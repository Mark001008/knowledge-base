package com.ma.kb.manager.space.converter;

import com.ma.kb.dal.model.space.SpaceDO;
import com.ma.kb.dal.model.space.SpaceMemberDO;
import com.ma.kb.manager.space.bo.SpaceBO;
import com.ma.kb.manager.space.bo.SpaceMemberBO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Space DO <-> BO 转换器
 */
@Mapper(componentModel = "spring")
public interface SpaceConverter {

    SpaceBO toBO(SpaceDO spaceDO);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SpaceDO toDO(SpaceBO spaceBO);

    SpaceMemberBO toMemberBO(SpaceMemberDO memberDO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    SpaceMemberDO toMemberDO(SpaceMemberBO memberBO);
}
