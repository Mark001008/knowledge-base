package com.ma.kb.start.controller.space;

import com.ma.kb.common.response.ApiResponse;
import com.ma.kb.core.auth.JwtService;
import com.ma.kb.service.space.SpaceService;
import com.ma.kb.service.space.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库控制器
 */
@RestController
@RequestMapping("/api/spaces")
public class SpaceController {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final SpaceService spaceService;
    private final JwtService jwtService;

    public SpaceController(SpaceService spaceService, JwtService jwtService) {
        this.spaceService = spaceService;
        this.jwtService = jwtService;
    }

    /**
     * 创建知识库
     */
    @PostMapping
    public ApiResponse<SpaceVO> create(HttpServletRequest request,
                                       @RequestBody SpaceCreateRequest body) {
        Long userId = getCurrentUserId(request);
        SpaceVO space = spaceService.create(userId, body);
        return ApiResponse.success(space);
    }

    /**
     * 查询知识库列表
     */
    @GetMapping
    public ApiResponse<List<SpaceVO>> list(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        List<SpaceVO> spaces = spaceService.listAccessible(userId);
        return ApiResponse.success(spaces);
    }

    /**
     * 查询知识库详情
     */
    @GetMapping("/{id}")
    public ApiResponse<SpaceVO> getById(HttpServletRequest request, @PathVariable Long id) {
        Long userId = getCurrentUserId(request);
        SpaceVO space = spaceService.getById(userId, id);
        return ApiResponse.success(space);
    }

    /**
     * 更新知识库
     */
    @PutMapping("/{id}")
    public ApiResponse<SpaceVO> update(HttpServletRequest request, @PathVariable Long id,
                                       @RequestBody SpaceUpdateRequest body) {
        Long userId = getCurrentUserId(request);
        SpaceVO space = spaceService.update(userId, id, body);
        return ApiResponse.success(space);
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(HttpServletRequest request, @PathVariable Long id) {
        Long userId = getCurrentUserId(request);
        spaceService.delete(userId, id);
        return ApiResponse.success();
    }

    /**
     * 添加知识库成员
     */
    @PostMapping("/{id}/members")
    public ApiResponse<Void> addMember(HttpServletRequest request, @PathVariable Long id,
                                       @RequestBody SpaceMemberRequest body) {
        Long userId = getCurrentUserId(request);
        spaceService.addMember(userId, id, body);
        return ApiResponse.success();
    }

    /**
     * 查询知识库成员列表
     */
    @GetMapping("/{id}/members")
    public ApiResponse<List<SpaceMemberVO>> listMembers(HttpServletRequest request,
                                                        @PathVariable Long id) {
        Long userId = getCurrentUserId(request);
        List<SpaceMemberVO> members = spaceService.listMembers(userId, id);
        return ApiResponse.success(members);
    }

    /**
     * 移除知识库成员
     */
    @DeleteMapping("/{id}/members/{userId}")
    public ApiResponse<Void> removeMember(HttpServletRequest request,
                                          @PathVariable Long id,
                                          @PathVariable Long userId) {
        Long currentUserId = getCurrentUserId(request);
        spaceService.removeMember(currentUserId, id, userId);
        return ApiResponse.success();
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            String token = bearerToken.substring(BEARER_PREFIX.length());
            return jwtService.getUserId(token);
        }
        return null;
    }
}
