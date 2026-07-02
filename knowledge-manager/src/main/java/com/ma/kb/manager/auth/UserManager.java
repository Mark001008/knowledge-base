package com.ma.kb.manager.auth;

import com.ma.kb.dal.mapper.auth.UserMapper;
import com.ma.kb.dal.model.auth.UserDO;
import com.ma.kb.manager.auth.bo.UserBO;
import com.ma.kb.manager.auth.converter.UserConverter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用户数据管理器
 */
@Component
public class UserManager {

    private final UserMapper userMapper;
    private final UserConverter userConverter;

    public UserManager(UserMapper userMapper, UserConverter userConverter) {
        this.userMapper = userMapper;
        this.userConverter = userConverter;
    }

    /**
     * 根据用户名查询用户（含角色）
     */
    public UserBO getByUsername(String username) {
        UserDO userDO = userMapper.selectByUsername(username);
        if (userDO == null) {
            return null;
        }
        return convertToBO(userDO);
    }

    /**
     * 根据用户ID查询用户（含角色）
     */
    public UserBO getById(Long userId) {
        UserDO userDO = userMapper.selectById(userId);
        if (userDO == null) {
            return null;
        }
        return convertToBO(userDO);
    }

    /**
     * DO 转 BO
     */
    private UserBO convertToBO(UserDO userDO) {
        UserBO userBO = userConverter.toBO(userDO);

        // 查询用户角色
        List<String> roleCodes = userMapper.selectRoleCodesByUserId(userDO.getId());
        userBO.setRoles(roleCodes);

        return userBO;
    }
}
