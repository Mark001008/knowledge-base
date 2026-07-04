package com.ma.kb.manager.auth;

import com.ma.kb.dal.mapper.auth.MenuMapper;
import com.ma.kb.dal.model.auth.MenuDO;
import com.ma.kb.manager.auth.bo.MenuBO;
import com.ma.kb.manager.auth.converter.MenuConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单数据管理器
 */
@Component
public class MenuManager {

    private final MenuMapper menuMapper;
    private final MenuConverter menuConverter;

    public MenuManager(MenuMapper menuMapper, MenuConverter menuConverter) {
        this.menuMapper = menuMapper;
        this.menuConverter = menuConverter;
    }

    /**
     * 根据用户ID查询菜单树
     */
    public List<MenuBO> getMenuTreeByUserId(Long userId) {
        List<MenuDO> menuDOList = menuMapper.selectByUserId(userId);
        List<MenuBO> menuBOList = menuConverter.toBOList(menuDOList);
        return buildMenuTree(menuBOList);
    }

    /**
     * 查询所有菜单树
     */
    public List<MenuBO> getAllMenuTree() {
        List<MenuDO> menuDOList = menuMapper.selectAllMenus();
        List<MenuBO> menuBOList = menuConverter.toBOList(menuDOList);
        return buildMenuTree(menuBOList);
    }

    /**
     * 构建菜单树
     */
    private List<MenuBO> buildMenuTree(List<MenuBO> menuList) {
        // 按parentId分组
        Map<Long, List<MenuBO>> parentMap = menuList.stream()
                .filter(menu -> menu.getParentId() != null)
                .collect(Collectors.groupingBy(MenuBO::getParentId));

        // 设置子菜单
        for (MenuBO menu : menuList) {
            menu.setChildren(parentMap.getOrDefault(menu.getId(), new ArrayList<>()));
        }

        // 返回顶级菜单
        return menuList.stream()
                .filter(menu -> menu.getParentId() != null && menu.getParentId() == 0)
                .collect(Collectors.toList());
    }
}
