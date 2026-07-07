package com.ma.kb.service.space.impl;

import com.ma.kb.common.enums.SpaceRoleEnum;
import com.ma.kb.common.enums.SpaceVisibilityEnum;
import com.ma.kb.common.exception.BusinessException;
import com.ma.kb.common.response.ErrorCode;
import com.ma.kb.integration.storage.StorageService;
import com.ma.kb.integration.vector.VectorSearchService;
import com.ma.kb.manager.auth.UserManager;
import com.ma.kb.manager.auth.bo.UserBO;
import com.ma.kb.manager.chat.ChatManager;
import com.ma.kb.manager.document.DocumentManager;
import com.ma.kb.manager.document.bo.DocumentBO;
import com.ma.kb.manager.space.SpaceManager;
import com.ma.kb.manager.space.bo.SpaceBO;
import com.ma.kb.manager.space.bo.SpaceMemberBO;
import com.ma.kb.service.chat.dto.IndexHealthDTO;
import com.ma.kb.service.space.SpaceService;
import com.ma.kb.service.space.converter.SpaceDTOConverter;
import com.ma.kb.service.space.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库服务实现
 */
@Service
public class SpaceServiceImpl implements SpaceService {

    private static final Logger log = LoggerFactory.getLogger(SpaceServiceImpl.class);
    private static final String SYSTEM_ADMIN_ROLE = "SYSTEM_ADMIN";
    private static final String KB_ADMIN_ROLE = "KB_ADMIN";
    private static final int USER_MAX_SPACES = 20;

    private final SpaceManager spaceManager;
    private final UserManager userManager;
    private final DocumentManager documentManager;
    private final ChatManager chatManager;
    private final StorageService storageService;
    private final VectorSearchService vectorSearchService;
    private final SpaceDTOConverter spaceDTOConverter;

    public SpaceServiceImpl(SpaceManager spaceManager, UserManager userManager,
                            DocumentManager documentManager, ChatManager chatManager,
                            StorageService storageService, VectorSearchService vectorSearchService,
                            SpaceDTOConverter spaceDTOConverter) {
        this.spaceManager = spaceManager;
        this.userManager = userManager;
        this.documentManager = documentManager;
        this.chatManager = chatManager;
        this.storageService = storageService;
        this.vectorSearchService = vectorSearchService;
        this.spaceDTOConverter = spaceDTOConverter;
    }

    @Override
    public SpaceVO create(Long userId, SpaceCreateRequest request) {
        SpaceVisibilityEnum.fromCode(request.visibility());

        // 普通用户保留合理上限，避免演示级限制影响团队真实使用。
        if (!isSystemAdmin(userId) && !isKbAdmin(userId)) {
            List<SpaceBO> userSpaces = spaceManager.listByOwner(userId);
            if (userSpaces.size() >= USER_MAX_SPACES) {
                throw new BusinessException(ErrorCode.SPACE_LIMIT_EXCEEDED, "普通用户最多创建" + USER_MAX_SPACES + "个知识库");
            }
        }

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
        List<SpaceBO> spaces = isSystemAdmin(userId)
                ? spaceManager.listAll()
                : spaceManager.listAccessible(userId);
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
    @Transactional
    public void delete(Long userId, Long spaceId) {
        getAndCheckAccess(userId, spaceId);
        String role = spaceManager.getMemberRole(spaceId, userId);
        if (!SpaceRoleEnum.OWNER.getCode().equals(role)) {
            throw new BusinessException(ErrorCode.SPACE_ACCESS_DENIED, "仅知识库所有者可删除");
        }

        List<DocumentBO> documents = documentManager.listBySpaceId(spaceId);
        for (DocumentBO document : documents) {
            deleteDocumentCascade(document);
        }
        chatManager.deleteSessionsBySpaceId(spaceId);
        spaceManager.deleteMembersBySpaceId(spaceId);
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

        if (isSystemAdmin(userId)) {
            return space;
        }

        String role = spaceManager.getMemberRole(spaceId, userId);
        if (role == null && !SpaceVisibilityEnum.INTERNAL.getCode().equals(space.getVisibility())) {
            throw new BusinessException(ErrorCode.SPACE_ACCESS_DENIED);
        }

        return space;
    }

    private void deleteDocumentCascade(DocumentBO document) {
        try {
            storageService.delete(document.getStorageBucket(), document.getStorageObjectKey());
        } catch (Exception e) {
            log.warn("删除知识库时清理文档原文件失败，继续删除业务数据: documentId={}", document.getId(), e);
        }
        try {
            vectorSearchService.deleteByDocumentId(document.getId());
        } catch (Exception e) {
            log.warn("删除知识库时清理文档向量失败，继续删除业务数据: documentId={}", document.getId(), e);
        }
        documentManager.deleteChunksByDocumentId(document.getId());
        documentManager.deleteById(document.getId());
    }

    private void checkRole(Long userId, Long spaceId, SpaceRoleEnum minRole) {
        if (isSystemAdmin(userId)) {
            return;
        }

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
        IndexHealthDTO indexHealth = buildIndexHealth(space.getId());
        return new SpaceVO(
                space.getId(), space.getName(), space.getDescription(),
                space.getOwnerId(), null, space.getVisibility(),
                space.getTopK(), space.getSimilarityThreshold(), space.getTemperature(),
                space.getChunkSize(), space.getChunkOverlap(),
                documentCount, indexHealth, space.getCreatedAt(), space.getUpdatedAt()
        );
    }

    private IndexHealthDTO buildIndexHealth(Long spaceId) {
        List<DocumentBO> documents = documentManager.listBySpaceId(spaceId);
        int completed = 0;
        int processing = 0;
        int failed = 0;
        java.time.LocalDateTime lastIndexedAt = null;
        for (DocumentBO document : documents) {
            String status = document.getParseStatus();
            if ("COMPLETED".equals(status)) {
                completed++;
                if (document.getUpdatedAt() != null &&
                        (lastIndexedAt == null || document.getUpdatedAt().isAfter(lastIndexedAt))) {
                    lastIndexedAt = document.getUpdatedAt();
                }
            } else if ("FAILED".equals(status)) {
                failed++;
            } else {
                processing++;
            }
        }
        return new IndexHealthDTO(
                documents.size(),
                completed,
                processing,
                failed,
                documentManager.countChunksBySpaceId(spaceId),
                vectorSearchService.isEnabled(),
                lastIndexedAt
        );
    }

    private boolean isSystemAdmin(Long userId) {
        UserBO user = userManager.getById(userId);
        return user != null && user.getRoles() != null && user.getRoles().contains(SYSTEM_ADMIN_ROLE);
    }

    private boolean isKbAdmin(Long userId) {
        UserBO user = userManager.getById(userId);
        return user != null && user.getRoles() != null && user.getRoles().contains(KB_ADMIN_ROLE);
    }
}
