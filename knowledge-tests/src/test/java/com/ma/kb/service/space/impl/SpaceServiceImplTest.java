package com.ma.kb.service.space.impl;

import com.ma.kb.common.enums.SpaceRoleEnum;
import com.ma.kb.common.exception.BusinessException;
import com.ma.kb.common.response.ErrorCode;
import com.ma.kb.manager.auth.UserManager;
import com.ma.kb.manager.auth.bo.UserBO;
import com.ma.kb.manager.space.SpaceManager;
import com.ma.kb.manager.space.bo.SpaceBO;
import com.ma.kb.service.space.converter.SpaceDTOConverter;
import com.ma.kb.service.space.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpaceServiceImplTest {

    @Mock
    private SpaceManager spaceManager;
    @Mock
    private UserManager userManager;
    @Mock
    private SpaceDTOConverter spaceDTOConverter;

    private SpaceServiceImpl spaceService;

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long SPACE_ID = 100L;

    @BeforeEach
    void setUp() {
        spaceService = new SpaceServiceImpl(spaceManager, userManager, spaceDTOConverter);
    }

    @Test
    void createSuccess() {
        SpaceCreateRequest request = new SpaceCreateRequest(
                "测试知识库", "描述", "PRIVATE", 5,
                new BigDecimal("0.7"), new BigDecimal("0.2")
        );

        SpaceBO spaceBO = buildSpaceBO(SPACE_ID, "测试知识库", OWNER_ID);
        when(spaceDTOConverter.toBO(request, OWNER_ID)).thenReturn(spaceBO);
        when(spaceManager.create(any())).thenReturn(spaceBO);

        SpaceVO result = spaceService.create(OWNER_ID, request);

        assertNotNull(result);
        assertEquals(SPACE_ID, result.id());
        verify(spaceManager).addMember(SPACE_ID, OWNER_ID, SpaceRoleEnum.OWNER.getCode());
    }

    @Test
    void createWithInvalidVisibility() {
        SpaceCreateRequest request = new SpaceCreateRequest(
                "测试", "描述", "INVALID", 5,
                new BigDecimal("0.7"), new BigDecimal("0.2")
        );

        assertThrows(IllegalArgumentException.class,
                () -> spaceService.create(OWNER_ID, request));
    }

    @Test
    void getByIdSuccess() {
        SpaceBO spaceBO = buildSpaceBO(SPACE_ID, "知识库", OWNER_ID);
        when(spaceManager.getById(SPACE_ID)).thenReturn(spaceBO);
        when(spaceManager.getMemberRole(SPACE_ID, OWNER_ID)).thenReturn(SpaceRoleEnum.READER.getCode());

        SpaceVO result = spaceService.getById(OWNER_ID, SPACE_ID);

        assertNotNull(result);
        assertEquals(SPACE_ID, result.id());
    }

    @Test
    void getByIdNotFound() {
        when(spaceManager.getById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> spaceService.getById(OWNER_ID, 999L));
    }

    @Test
    void getByIdAccessDenied() {
        SpaceBO spaceBO = buildSpaceBO(SPACE_ID, "知识库", OWNER_ID);
        spaceBO.setVisibility("PRIVATE");
        when(spaceManager.getById(SPACE_ID)).thenReturn(spaceBO);
        when(spaceManager.getMemberRole(SPACE_ID, OTHER_USER_ID)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> spaceService.getById(OTHER_USER_ID, SPACE_ID));
        assertEquals(ErrorCode.SPACE_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    void deleteSuccess() {
        SpaceBO spaceBO = buildSpaceBO(SPACE_ID, "知识库", OWNER_ID);
        when(spaceManager.getById(SPACE_ID)).thenReturn(spaceBO);
        when(spaceManager.getMemberRole(SPACE_ID, OWNER_ID)).thenReturn(SpaceRoleEnum.OWNER.getCode());

        spaceService.delete(OWNER_ID, SPACE_ID);

        verify(spaceManager).deleteById(SPACE_ID);
    }

    @Test
    void deleteByNonOwner() {
        SpaceBO spaceBO = buildSpaceBO(SPACE_ID, "知识库", OWNER_ID);
        when(spaceManager.getById(SPACE_ID)).thenReturn(spaceBO);
        when(spaceManager.getMemberRole(SPACE_ID, OTHER_USER_ID)).thenReturn(SpaceRoleEnum.ADMIN.getCode());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> spaceService.delete(OTHER_USER_ID, SPACE_ID));
        assertEquals(ErrorCode.SPACE_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    void addMemberSuccess() {
        when(userManager.getById(OWNER_ID)).thenReturn(null);
        when(spaceManager.getMemberRole(SPACE_ID, OWNER_ID)).thenReturn(SpaceRoleEnum.OWNER.getCode());

        UserBO targetUser = new UserBO();
        targetUser.setId(OTHER_USER_ID);
        when(userManager.getById(OTHER_USER_ID)).thenReturn(targetUser);
        when(spaceManager.getMemberRole(SPACE_ID, OTHER_USER_ID)).thenReturn(null);

        SpaceMemberRequest request = new SpaceMemberRequest(OTHER_USER_ID, "READER");
        spaceService.addMember(OWNER_ID, SPACE_ID, request);

        verify(spaceManager).addMember(SPACE_ID, OTHER_USER_ID, "READER");
    }

    @Test
    void addMemberAlreadyExists() {
        when(userManager.getById(OWNER_ID)).thenReturn(null);
        when(spaceManager.getMemberRole(SPACE_ID, OWNER_ID)).thenReturn(SpaceRoleEnum.OWNER.getCode());

        UserBO targetUser = new UserBO();
        targetUser.setId(OTHER_USER_ID);
        when(userManager.getById(OTHER_USER_ID)).thenReturn(targetUser);
        when(spaceManager.getMemberRole(SPACE_ID, OTHER_USER_ID)).thenReturn(SpaceRoleEnum.READER.getCode());

        SpaceMemberRequest request = new SpaceMemberRequest(OTHER_USER_ID, "READER");
        assertThrows(BusinessException.class,
                () -> spaceService.addMember(OWNER_ID, SPACE_ID, request));
    }

    @Test
    void removeMemberSuccess() {
        when(spaceManager.getMemberRole(SPACE_ID, OWNER_ID)).thenReturn(SpaceRoleEnum.OWNER.getCode());
        when(spaceManager.getMemberRole(SPACE_ID, OTHER_USER_ID)).thenReturn(SpaceRoleEnum.READER.getCode());

        spaceService.removeMember(OWNER_ID, SPACE_ID, OTHER_USER_ID);

        verify(spaceManager).removeMember(SPACE_ID, OTHER_USER_ID);
    }

    @Test
    void removeOwnerShouldFail() {
        when(spaceManager.getMemberRole(SPACE_ID, OWNER_ID)).thenReturn(SpaceRoleEnum.OWNER.getCode());
        when(spaceManager.getMemberRole(SPACE_ID, OTHER_USER_ID)).thenReturn(SpaceRoleEnum.OWNER.getCode());

        assertThrows(BusinessException.class,
                () -> spaceService.removeMember(OWNER_ID, SPACE_ID, OTHER_USER_ID));
    }

    private SpaceBO buildSpaceBO(Long id, String name, Long ownerId) {
        SpaceBO bo = new SpaceBO();
        bo.setId(id);
        bo.setName(name);
        bo.setOwnerId(ownerId);
        bo.setVisibility("PRIVATE");
        bo.setTopK(5);
        bo.setSimilarityThreshold(new BigDecimal("0.7"));
        bo.setTemperature(new BigDecimal("0.2"));
        bo.setChunkSize(800);
        bo.setChunkOverlap(120);
        return bo;
    }
}
