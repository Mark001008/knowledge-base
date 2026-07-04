package com.ma.kb.manager.auth.converter;

import com.ma.kb.dal.model.auth.MenuDO;
import com.ma.kb.manager.auth.bo.MenuBO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MenuDO -> MenuBO 转换器
 */
@Mapper(componentModel = "spring")
public interface MenuConverter {

    /**
     * MenuDO 转 MenuBO
     * children 字段需要单独构建，忽略此字段
     */
    @Mapping(target = "children", ignore = true)
    MenuBO toBO(MenuDO menuDO);

    /**
     * 批量转换
     */
    List<MenuBO> toBOList(List<MenuDO> menuDOList);
}
