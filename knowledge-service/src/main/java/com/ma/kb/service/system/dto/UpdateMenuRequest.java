package com.ma.kb.service.system.dto;

/**
 * 更新菜单请求
 */
public record UpdateMenuRequest(
        Long parentId,
        String menuName,
        String menuType,
        String path,
        String component,
        String icon,
        String permissionCode,
        Integer sort,
        Integer visible,
        String status
) {
}
