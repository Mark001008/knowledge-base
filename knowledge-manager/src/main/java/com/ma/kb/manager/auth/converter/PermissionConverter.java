package com.ma.kb.manager.auth.converter;

import com.ma.kb.dal.model.auth.PermissionDO;
import com.ma.kb.manager.auth.bo.PermissionBO;
import org.mapstruct.Mapper;

/**
 * PermissionDO -> PermissionBO 转换器
 */
@Mapper(componentModel = "spring")
public interface PermissionConverter {

    /**
     * PermissionDO 转 PermissionBO
     */
    PermissionBO toBO(PermissionDO permissionDO);
}
