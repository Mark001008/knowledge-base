package com.ma.kb.service.auth.converter;

import com.ma.kb.manager.auth.bo.UserBO;
import com.ma.kb.service.auth.dto.RoleDTO;
import com.ma.kb.service.auth.dto.UserInfoDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;

/**
 * UserBO -> UserInfoDTO 转换器
 */
@Mapper(componentModel = "spring")
public interface UserDTOConverter {

    /**
     * UserBO 转 UserInfoDTO
     * roles 字段需要手动转换，忽略此字段
     */
    @Mapping(target = "roles", ignore = true)
    UserInfoDTO toUserInfoDTO(UserBO userBO);

    /**
     * 手动转换 roles
     */
    default List<RoleDTO> mapRoles(List<String> roleCodes) {
        if (roleCodes == null) {
            return List.of();
        }
        return roleCodes.stream()
                .map(code -> new RoleDTO(null, code, code))
                .collect(Collectors.toList());
    }
}
