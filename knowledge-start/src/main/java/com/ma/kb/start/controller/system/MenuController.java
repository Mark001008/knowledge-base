package com.ma.kb.start.controller.system;

import com.ma.kb.common.response.ApiResponse;
import com.ma.kb.service.auth.dto.MenuDTO;
import com.ma.kb.service.system.MenuService;
import com.ma.kb.service.system.dto.CreateMenuRequest;
import com.ma.kb.service.system.dto.UpdateMenuRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜单管理控制器
 */
@RestController
@RequestMapping("/api/system/menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    /**
     * 获取所有菜单树
     */
    @GetMapping
    public ApiResponse<List<MenuDTO>> getAllMenus() {
        List<MenuDTO> menus = menuService.getAllMenus();
        return ApiResponse.success(menus);
    }

    /**
     * 根据ID获取菜单
     */
    @GetMapping("/{id}")
    public ApiResponse<MenuDTO> getMenuById(@PathVariable Long id) {
        MenuDTO menu = menuService.getMenuById(id);
        return ApiResponse.success(menu);
    }

    /**
     * 创建菜单
     */
    @PostMapping
    public ApiResponse<MenuDTO> createMenu(@RequestBody CreateMenuRequest request) {
        MenuDTO menu = menuService.createMenu(request);
        return ApiResponse.success(menu);
    }

    /**
     * 更新菜单
     */
    @PutMapping("/{id}")
    public ApiResponse<MenuDTO> updateMenu(@PathVariable Long id, @RequestBody UpdateMenuRequest request) {
        MenuDTO menu = menuService.updateMenu(id, request);
        return ApiResponse.success(menu);
    }

    /**
     * 删除菜单
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteMenu(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return ApiResponse.success(null);
    }

    /**
     * 获取当前用户菜单树
     */
    @GetMapping("/current")
    public ApiResponse<List<MenuDTO>> getCurrentUserMenus() {
        // TODO: 从SecurityContext获取当前用户ID
        // 暂时返回所有菜单
        List<MenuDTO> menus = menuService.getAllMenus();
        return ApiResponse.success(menus);
    }
}
