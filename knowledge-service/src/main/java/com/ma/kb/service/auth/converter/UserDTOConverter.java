package com.ma.kb.service.auth.converter;

import com.ma.kb.manager.auth.bo.UserBO;
import com.ma.kb.service.auth.dto.UserInfoDTO;
import org.mapstruct.Mapper;

/**
 * UserBO -> UserInfoDTO 转换器
 */
@Mapper(componentModel = "spring")
public interface UserDTOConverter {

    /**
     * UserBO 转 UserInfoDTO
     */
    UserInfoDTO toUserInfoDTO(UserBO userBO);
}
