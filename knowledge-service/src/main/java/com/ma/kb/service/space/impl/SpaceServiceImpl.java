package com.ma.kb.service.space.impl;

import com.ma.kb.common.enums.SpaceRoleEnum;
import com.ma.kb.common.enums.SpaceVisibilityEnum;
import com.ma.kb.common.exception.BusinessException;
import com.ma.kb.common.response.ErrorCode;
import com.ma.kb.manager.auth.UserManager;
import com.ma.kb.manager.auth.bo.UserBO;
import com.ma.kb.manager.space.SpaceManager;
import com.ma.kb.manager.space.bo.SpaceBO;
import com.ma.kb.manager.space.bo.SpaceMemberBO;
import com.ma.kb.service.space.SpaceService;
import com.ma.kb.service.space.converter.SpaceDTOConverter;
import com.ma.kb.service.space.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库服务实现
 */
@Service
public class SpaceServiceImpl implements SpaceService {

    private static final Logger log = LoggerFactory.getLogger(SpaceServiceImpl.class);

    private final SpaceManager spaceManager;
    private final UserManager userManager;
    private final SpaceDTOConverter spaceDTOConverter;

    public SpaceServiceImpl(SpaceManager spaceManager, UserManager userManager,
                            SpaceDTOConverter spaceDTOConverter) {
        this.spaceManager = spaceManager;
        this.userManager = userManager;
        this.spaceDTOConverter = spaceDTOConverter;
    }

    @Override
    public SpaceVO create(Long userId, SpaceCreateRequest request) {
        SpaceVisibilityEnum.fromCode(request.visibility());

        SpaceBO spaceBO = spaceDTOConverter.toBO(request, userId);

        SpaceBO created = spaceManager.create(spaceBO);
        spaceManager.addMember(created.getId(), userId, SpaceRoleEnum.OWNER.getCode());

        log.info("知识库创建成功: id={}, name={}, owner={}", created.getId(), created.getName(), userId);
        return toVO(created, null);
    }

    @Override
    public SpaceVO getById(Long userId, Long spaceId) {
        SpaceBO space = getAndCheckAccess(userId, spaceId);
        return toVO(space, null);
    }

    @Override
    public List<SpaceVO> listAccessible(Long userId) {
        List<SpaceBO> spaces = spaceManager.listAccessible(userId);
        return spaces.stream().map(s -> toVO(s, null)).toList();
    }

    @Override
    public SpaceVO update(Long userId, Long spaceId, SpaceUpdateRequest request) {
        getAndCheckAccess(userId, spaceId);
        checkRole(userId, spaceId, SpaceRoleEnum.ADMIN);

        if (request.visibility() != null) {
            SpaceVisibilityEnum.fromCode(request.visibility());
        }

        SpaceBO spaceBO = spaceManager.getById(spaceId);
        spaceDTOConverter.updateBO(request, spaceBO);

        spaceManager.update(spaceBO);
        log.info("知识库更新成功: id={}", spaceId);

        SpaceBO updated = spaceManager.getById(spaceId);
        return toVO(updated, null);
    }

    @Override
    public void delete(Long userId, Long spaceId) {
        getAndCheckAccess(userId, spaceId);
        String role = spaceManager.getMemberRole(spaceId, userId);
        if (!SpaceRoleEnum.OWNER.getCode().equals(role)) {
            throw new BusinessException(ErrorCode.SPACE_ACCESS_DENIED, "仅知识库所有者可删除");
        }

        spaceManager.deleteById(spaceId);
        log.info("知识库删除成功: id={}", spaceId);
    }

    @Override
    public void addMember(Long userId, Long spaceId, SpaceMemberRequest request) {
        checkRole(userId, spaceId, SpaceRoleEnum.OWNER);
        SpaceRoleEnum.fromCode(request.role());

        UserBO user = userManager.getById(request.userId());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        String existingRole = spaceManager.getMemberRole(spaceId, request.userId());
        if (existingRole != null) {
            throw new BusinessException(ErrorCode.SPACE_MEMBER_ALREADY_EXISTS);
        }

        spaceManager.addMember(spaceId, request.userId(), request.role());
        log.info("知识库成员添加成功: spaceId={}, userId={}, role={}", spaceId, request.userId(), request.role());
    }

    @Override
    public List<SpaceMemberVO> listMembers(Long userId, Long spaceId) {
        getAndCheckAccess(userId, spaceId);

        List<SpaceMemberBO> members = spaceManager.listMembers(spaceId);
        List<SpaceMemberVO> result = new ArrayList<>();
        for (SpaceMemberBO member : members) {
            UserBO user = userManager.getById(member.getUserId());
            if (user != null) {
                result.add(new SpaceMemberVO(
                        user.getId(), user.getUsername(), user.getDisplayName(),
                        member.getRole(), member.getCreatedAt()
                ));
            }
        }
        return result;
    }

    @Override
    public void removeMember(Long userId, Long spaceId, Long targetUserId) {
        checkRole(userId, spaceId, SpaceRoleEnum.OWNER);

        String targetRole = spaceManager.getMemberRole(spaceId, targetUserId);
        if (targetRole == null) {
            throw new BusinessException(ErrorCode.SPACE_MEMBER_NOT_FOUND);
        }
        if (SpaceRoleEnum.OWNER.getCode().equals(targetRole)) {
            throw new BusinessException(ErrorCode.SPACE_OWNER_CANNOT_LEAVE);
        }

        spaceManager.removeMember(spaceId, targetUserId);
        log.info("知识库成员移除成功: spaceId={}, userId={}", spaceId, targetUserId);
    }

    // ==================== 私有方法 ====================

    private SpaceBO getAndCheckAccess(Long userId, Long spaceId) {
        SpaceBO space = spaceManager.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.SPACE_NOT_FOUND);
        }

        String role = spaceManager.getMemberRole(spaceId, userId);
        if (role == null && !SpaceVisibilityEnum.INTERNAL.getCode().equals(space.getVisibility())) {
            throw new BusinessException(ErrorCode.SPACE_ACCESS_DENIED);
        }

        return space;
    }

    private void checkRole(Long userId, Long spaceId, SpaceRoleEnum minRole) {
        String role = spaceManager.getMemberRole(spaceId, userId);
        if (role == null) {
            throw new BusinessException(ErrorCode.SPACE_ACCESS_DENIED);
        }

        SpaceRoleEnum userRole = SpaceRoleEnum.fromCode(role);
        if (userRole.ordinal() > minRole.ordinal()) {
            throw new BusinessException(ErrorCode.SPACE_ACCESS_DENIED);
        }
    }

    private SpaceVO toVO(SpaceBO space, Integer documentCount) {
        return new SpaceVO(
                space.getId(), space.getName(), space.getDescription(),
                space.getOwnerId(), null, space.getVisibility(),
                space.getTopK(), space.getSimilarityThreshold(), space.getTemperature(),
                space.getChunkSize(), space.getChunkOverlap(),
                documentCount, space.getCreatedAt(), space.getUpdatedAt()
        );
    }
}
