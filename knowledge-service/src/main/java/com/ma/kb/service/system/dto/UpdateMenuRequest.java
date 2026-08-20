package com.ma.kb.service.system.dto;

import jakarta.validation.constraints.Size;

/**
 * 更新菜单请求
 */
public record UpdateMenuRequest(
        Long parentId,
        @Size(max = 100, message = "菜单名称长度不能超过100") String menuName,
        @Size(max = 20, message = "菜单类型长度不能超过20") String menuType,
        @Size(max = 200, message = "路径长度不能超过200") String path,
        @Size(max = 200, message = "组件路径长度不能超过200") String component,
        @Size(max = 100, message = "图标长度不能超过100") String icon,
        @Size(max = 100, message = "权限编码长度不能超过100") String permissionCode,
        Integer sort,
        Integer visible,
        @Size(max = 20, message = "状态值长度不能超过20") String status
) {
}
