package com.ma.kb.service.space.converter;

import com.ma.kb.manager.space.bo.SpaceBO;
import com.ma.kb.service.space.dto.SpaceCreateRequest;
import com.ma.kb.service.space.dto.SpaceUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Space DTO <-> BO 转换器
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SpaceDTOConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ownerId", source = "ownerId")
    @Mapping(target = "chunkSize", constant = "800")
    @Mapping(target = "chunkOverlap", constant = "120")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SpaceBO toBO(SpaceCreateRequest request, Long ownerId);

    void updateBO(SpaceUpdateRequest request, @MappingTarget SpaceBO spaceBO);
}
