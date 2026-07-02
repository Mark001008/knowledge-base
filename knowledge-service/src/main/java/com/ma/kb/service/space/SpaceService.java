package com.ma.kb.service.space;

import com.ma.kb.service.space.dto.*;

import java.util.List;

/**
 * 知识库服务接口
 */
public interface SpaceService {

    /**
     * 创建知识库
     */
    SpaceVO create(Long userId, SpaceCreateRequest request);

    /**
     * 查询知识库详情
     */
    SpaceVO getById(Long userId, Long spaceId);

    /**
     * 查询用户可访问的知识库列表
     */
    List<SpaceVO> listAccessible(Long userId);

    /**
     * 更新知识库
     */
    SpaceVO update(Long userId, Long spaceId, SpaceUpdateRequest request);

    /**
     * 删除知识库
     */
    void delete(Long userId, Long spaceId);

    /**
     * 添加知识库成员
     */
    void addMember(Long userId, Long spaceId, SpaceMemberRequest request);

    /**
     * 查询知识库成员列表
     */
    List<SpaceMemberVO> listMembers(Long userId, Long spaceId);

    /**
     * 移除知识库成员
     */
    void removeMember(Long userId, Long spaceId, Long targetUserId);
}
