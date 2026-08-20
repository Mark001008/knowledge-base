package com.ma.kb.service.space.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 添加知识库成员请求
 */
public record SpaceMemberRequest(
        @NotNull(message = "用户ID不能为空") Long userId,
        @NotBlank(message = "角色不能为空") String role
) {
}
