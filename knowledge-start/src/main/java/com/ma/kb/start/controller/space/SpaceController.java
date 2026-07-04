package com.ma.kb.start.controller.space;

import com.ma.kb.common.response.ApiResponse;
import com.ma.kb.core.auth.JwtService;
import com.ma.kb.core.auth.RequirePermission;
import com.ma.kb.core.auth.SecurityUtils;
import com.ma.kb.service.space.SpaceService;
import com.ma.kb.service.space.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库控制器
 */
@RestController
@RequestMapping("/api/spaces")
public class SpaceController {

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
    @RequirePermission("space:create")
    public ApiResponse<SpaceVO> create(HttpServletRequest request,
                                       @RequestBody SpaceCreateRequest body) {
        Long userId = SecurityUtils.getCurrentUserId(request.getHeader("Authorization"), jwtService);
        SpaceVO space = spaceService.create(userId, body);
        return ApiResponse.success(space);
    }

    /**
     * 查询知识库列表
     */
    @GetMapping
    @RequirePermission("space:view")
    public ApiResponse<List<SpaceVO>> list(HttpServletRequest request) {
        Long userId = SecurityUtils.getCurrentUserId(request.getHeader("Authorization"), jwtService);
        List<SpaceVO> spaces = spaceService.listAccessible(userId);
        return ApiResponse.success(spaces);
    }

    /**
     * 查询知识库详情
     */
    @GetMapping("/{id}")
    @RequirePermission("space:view")
    public ApiResponse<SpaceVO> getById(HttpServletRequest request, @PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId(request.getHeader("Authorization"), jwtService);
        SpaceVO space = spaceService.getById(userId, id);
        return ApiResponse.success(space);
    }

    /**
     * 更新知识库
     */
    @PutMapping("/{id}")
    @RequirePermission("space:update")
    public ApiResponse<SpaceVO> update(HttpServletRequest request, @PathVariable Long id,
                                       @RequestBody SpaceUpdateRequest body) {
        Long userId = SecurityUtils.getCurrentUserId(request.getHeader("Authorization"), jwtService);
        SpaceVO space = spaceService.update(userId, id, body);
        return ApiResponse.success(space);
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/{id}")
    @RequirePermission("space:delete")
    public ApiResponse<Void> delete(HttpServletRequest request, @PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId(request.getHeader("Authorization"), jwtService);
        spaceService.delete(userId, id);
        return ApiResponse.success();
    }

    /**
     * 添加知识库成员
     */
    @PostMapping("/{id}/members")
    @RequirePermission("member:add")
    public ApiResponse<Void> addMember(HttpServletRequest request, @PathVariable Long id,
                                       @RequestBody SpaceMemberRequest body) {
        Long userId = SecurityUtils.getCurrentUserId(request.getHeader("Authorization"), jwtService);
        spaceService.addMember(userId, id, body);
        return ApiResponse.success();
    }

    /**
     * 查询知识库成员列表
     */
    @GetMapping("/{id}/members")
    @RequirePermission("member:view")
    public ApiResponse<List<SpaceMemberVO>> listMembers(HttpServletRequest request,
                                                        @PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId(request.getHeader("Authorization"), jwtService);
        List<SpaceMemberVO> members = spaceService.listMembers(userId, id);
        return ApiResponse.success(members);
    }

    /**
     * 移除知识库成员
     */
    @DeleteMapping("/{id}/members/{memberId}")
    @RequirePermission("member:remove")
    public ApiResponse<Void> removeMember(HttpServletRequest request,
                                          @PathVariable Long id,
                                          @PathVariable Long memberId) {
        Long userId = SecurityUtils.getCurrentUserId(request.getHeader("Authorization"), jwtService);
        spaceService.removeMember(userId, id, memberId);
        return ApiResponse.success();
    }
}
