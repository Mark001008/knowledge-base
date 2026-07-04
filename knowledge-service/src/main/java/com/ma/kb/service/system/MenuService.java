package com.ma.kb.service.system;

import com.ma.kb.service.auth.dto.MenuDTO;
import com.ma.kb.service.system.dto.CreateMenuRequest;
import com.ma.kb.service.system.dto.UpdateMenuRequest;

import java.util.List;

/**
 * 菜单服务接口
 */
public interface MenuService {

    /**
     * 获取当前用户菜单树
     */
    List<MenuDTO> getCurrentUserMenus(Long userId);

    /**
     * 获取所有菜单树
     */
    List<MenuDTO> getAllMenus();

    /**
     * 根据ID获取菜单
     */
    MenuDTO getMenuById(Long id);

    /**
     * 创建菜单
     */
    MenuDTO createMenu(CreateMenuRequest request);

    /**
     * 更新菜单
     */
    MenuDTO updateMenu(Long id, UpdateMenuRequest request);

    /**
     * 删除菜单
     */
    void deleteMenu(Long id);
}
