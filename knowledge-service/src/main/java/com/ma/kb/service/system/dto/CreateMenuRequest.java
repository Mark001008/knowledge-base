package com.ma.kb.service.system.dto;

/**
 * 创建菜单请求
 */
public record CreateMenuRequest(
        Long parentId,
        String menuName,
        String menuType,
        String path,
        String component,
        String icon,
        String permissionCode,
        Integer sort,
        Integer visible
) {
}
