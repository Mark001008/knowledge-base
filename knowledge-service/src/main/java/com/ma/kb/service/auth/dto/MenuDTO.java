package com.ma.kb.service.auth.dto;

import java.util.List;

/**
 * 菜单 DTO
 */
public record MenuDTO(
        Long id,
        Long parentId,
        String menuName,
        String menuType,
        String path,
        String component,
        String icon,
        String permissionCode,
        Integer sort,
        Integer visible,
        List<MenuDTO> children
) {
}
