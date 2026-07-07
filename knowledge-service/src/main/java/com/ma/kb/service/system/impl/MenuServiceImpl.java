package com.ma.kb.service.system.impl;

import com.ma.kb.common.exception.BusinessException;
import com.ma.kb.common.response.ErrorCode;
import com.ma.kb.dal.mapper.auth.MenuMapper;
import com.ma.kb.dal.mapper.auth.RoleMenuMapper;
import com.ma.kb.dal.model.auth.MenuDO;
import com.ma.kb.manager.auth.MenuManager;
import com.ma.kb.manager.auth.bo.MenuBO;
import com.ma.kb.service.auth.dto.MenuDTO;
import com.ma.kb.service.system.MenuService;
import com.ma.kb.service.system.dto.CreateMenuRequest;
import com.ma.kb.service.system.dto.UpdateMenuRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单服务实现
 */
@Service
public class MenuServiceImpl implements MenuService {

    private static final Logger log = LoggerFactory.getLogger(MenuServiceImpl.class);

    private final MenuManager menuManager;
    private final MenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;

    public MenuServiceImpl(MenuManager menuManager, MenuMapper menuMapper, RoleMenuMapper roleMenuMapper) {
        this.menuManager = menuManager;
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    @Override
    public List<MenuDTO> getCurrentUserMenus(Long userId) {
        List<MenuBO> menuBOList = menuManager.getMenuTreeByUserId(userId);
        return convertToDTOList(menuBOList);
    }

    @Override
    public List<MenuDTO> getAllMenus() {
        List<MenuBO> menuBOList = menuManager.getAllMenuTree();
        return convertToDTOList(menuBOList);
    }

    @Override
    public MenuDTO getMenuById(Long id) {
        MenuDO menuDO = menuMapper.selectById(id);
        if (menuDO == null) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        }
        return convertToDTO(menuDO);
    }

    @Override
    public MenuDTO createMenu(CreateMenuRequest request) {
        MenuDO menuDO = new MenuDO();
        menuDO.setParentId(request.parentId());
        menuDO.setMenuName(request.menuName());
        menuDO.setMenuType(request.menuType());
        menuDO.setPath(request.path());
        menuDO.setComponent(request.component());
        menuDO.setIcon(request.icon());
        menuDO.setPermissionCode(request.permissionCode());
        menuDO.setSort(request.sort());
        menuDO.setVisible(request.visible());
        menuDO.setStatus("ENABLED");

        menuMapper.insert(menuDO);
        log.info("创建菜单成功: {}", request.menuName());

        return convertToDTO(menuDO);
    }

    @Override
    public MenuDTO updateMenu(Long id, UpdateMenuRequest request) {
        MenuDO menuDO = menuMapper.selectById(id);
        if (menuDO == null) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        }

        menuDO.setParentId(request.parentId());
        menuDO.setMenuName(request.menuName());
        menuDO.setMenuType(request.menuType());
        menuDO.setPath(request.path());
        menuDO.setComponent(request.component());
        menuDO.setIcon(request.icon());
        menuDO.setPermissionCode(request.permissionCode());
        menuDO.setSort(request.sort());
        menuDO.setVisible(request.visible());
        menuDO.setStatus(request.status());

        menuMapper.updateById(menuDO);
        log.info("更新菜单成功: {}", id);

        return convertToDTO(menuDO);
    }

    @Override
    public void deleteMenu(Long id) {
        MenuDO menuDO = menuMapper.selectById(id);
        if (menuDO == null) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        }
        if (roleMenuMapper.countByMenuId(id) > 0) {
            throw new BusinessException(ErrorCode.MENU_HAS_ROLES);
        }

        menuMapper.deleteById(id);
        log.info("删除菜单成功: {}", id);
    }

    private List<MenuDTO> convertToDTOList(List<MenuBO> menuBOList) {
        return menuBOList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private MenuDTO convertToDTO(MenuBO menuBO) {
        if (menuBO == null) {
            return null;
        }
        List<MenuDTO> children = menuBO.getChildren() != null ?
                menuBO.getChildren().stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList()) :
                List.of();

        return new MenuDTO(
                menuBO.getId(),
                menuBO.getParentId(),
                menuBO.getMenuName(),
                menuBO.getMenuType(),
                menuBO.getPath(),
                menuBO.getComponent(),
                menuBO.getIcon(),
                menuBO.getPermissionCode(),
                menuBO.getSort(),
                menuBO.getVisible(),
                children
        );
    }

    private MenuDTO convertToDTO(MenuDO menuDO) {
        if (menuDO == null) {
            return null;
        }
        return new MenuDTO(
                menuDO.getId(),
                menuDO.getParentId(),
                menuDO.getMenuName(),
                menuDO.getMenuType(),
                menuDO.getPath(),
                menuDO.getComponent(),
                menuDO.getIcon(),
                menuDO.getPermissionCode(),
                menuDO.getSort(),
                menuDO.getVisible(),
                List.of()
        );
    }
}
