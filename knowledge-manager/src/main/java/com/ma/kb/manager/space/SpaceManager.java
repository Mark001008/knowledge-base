package com.ma.kb.manager.space;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ma.kb.dal.mapper.space.SpaceMapper;
import com.ma.kb.dal.mapper.space.SpaceMemberMapper;
import com.ma.kb.dal.model.space.SpaceDO;
import com.ma.kb.dal.model.space.SpaceMemberDO;
import com.ma.kb.manager.space.bo.SpaceBO;
import com.ma.kb.manager.space.bo.SpaceMemberBO;
import com.ma.kb.manager.space.converter.SpaceConverter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识库数据管理器
 */
@Component
public class SpaceManager {

    private final SpaceMapper spaceMapper;
    private final SpaceMemberMapper spaceMemberMapper;
    private final SpaceConverter spaceConverter;

    public SpaceManager(SpaceMapper spaceMapper, SpaceMemberMapper spaceMemberMapper,
                        SpaceConverter spaceConverter) {
        this.spaceMapper = spaceMapper;
        this.spaceMemberMapper = spaceMemberMapper;
        this.spaceConverter = spaceConverter;
    }

    public SpaceBO create(SpaceBO spaceBO) {
        SpaceDO spaceDO = spaceConverter.toDO(spaceBO);
        spaceMapper.insert(spaceDO);
        spaceBO.setId(spaceDO.getId());
        return spaceBO;
    }

    public SpaceBO getById(Long id) {
        SpaceDO spaceDO = spaceMapper.selectById(id);
        return spaceDO != null ? spaceConverter.toBO(spaceDO) : null;
    }

    public void update(SpaceBO spaceBO) {
        SpaceDO spaceDO = spaceConverter.toDO(spaceBO);
        spaceMapper.updateById(spaceDO);
    }

    public void deleteById(Long id) {
        spaceMapper.deleteById(id);
    }

    public List<SpaceBO> listAccessible(Long userId) {
        List<Long> spaceIds = spaceMapper.selectAccessibleSpaceIds(userId);
        if (spaceIds.isEmpty()) {
            return List.of();
        }
        List<SpaceDO> spaces = spaceMapper.selectBatchIds(spaceIds);
        return spaces.stream().map(spaceConverter::toBO).toList();
    }

    public List<SpaceBO> listAll() {
        List<SpaceDO> spaces = spaceMapper.selectList(
                new LambdaQueryWrapper<SpaceDO>()
                        .orderByDesc(SpaceDO::getUpdatedAt)
        );
        return spaces.stream().map(spaceConverter::toBO).toList();
    }

    public void addMember(Long spaceId, Long userId, String role) {
        SpaceMemberBO memberBO = new SpaceMemberBO();
        memberBO.setSpaceId(spaceId);
        memberBO.setUserId(userId);
        memberBO.setRole(role);
        SpaceMemberDO memberDO = spaceConverter.toMemberDO(memberBO);
        spaceMemberMapper.insert(memberDO);
    }

    public String getMemberRole(Long spaceId, Long userId) {
        return spaceMemberMapper.selectRoleBySpaceIdAndUserId(spaceId, userId);
    }

    public void removeMember(Long spaceId, Long userId) {
        spaceMemberMapper.selectList(
                new LambdaQueryWrapper<SpaceMemberDO>()
                        .eq(SpaceMemberDO::getSpaceId, spaceId)
                        .eq(SpaceMemberDO::getUserId, userId)
        ).forEach(m -> spaceMemberMapper.deleteById(m.getId()));
    }

    public List<SpaceMemberBO> listMembers(Long spaceId) {
        List<SpaceMemberDO> members = spaceMemberMapper.selectList(
                new LambdaQueryWrapper<SpaceMemberDO>()
                        .eq(SpaceMemberDO::getSpaceId, spaceId)
        );
        return members.stream().map(spaceConverter::toMemberBO).toList();
    }

    public List<SpaceBO> listByOwner(Long ownerId) {
        List<SpaceDO> spaces = spaceMapper.selectList(
                new LambdaQueryWrapper<SpaceDO>()
                        .eq(SpaceDO::getOwnerId, ownerId)
        );
        return spaces.stream().map(spaceConverter::toBO).toList();
    }
}
