package com.ma.kb.manager.auth.converter;

import com.ma.kb.dal.model.auth.UserDO;
import com.ma.kb.manager.auth.bo.UserBO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * UserDO -> UserBO 转换器
 */
@Mapper(componentModel = "spring")
public interface UserConverter {

    /**
     * UserDO 转 UserBO
     * roles 字段需要单独查询设置，忽略此字段
     */
    @Mapping(target = "roles", ignore = true)
    UserBO toBO(UserDO userDO);
}
