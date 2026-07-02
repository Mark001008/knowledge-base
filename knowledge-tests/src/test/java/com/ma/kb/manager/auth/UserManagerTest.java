package com.ma.kb.manager.auth;

import com.ma.kb.dal.mapper.auth.UserMapper;
import com.ma.kb.dal.model.auth.UserDO;
import com.ma.kb.manager.auth.bo.UserBO;
import com.ma.kb.manager.auth.converter.UserConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagerTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserConverter userConverter;

    private UserManager userManager;

    @BeforeEach
    void setUp() {
        userManager = new UserManager(userMapper, userConverter);
    }

    @Test
    void getByUsernameFound() {
        UserDO userDO = buildUserDO(1L, "admin");
        UserBO userBO = buildUserBO(1L, "admin");

        when(userMapper.selectByUsername("admin")).thenReturn(userDO);
        when(userConverter.toBO(userDO)).thenReturn(userBO);
        when(userMapper.selectRoleCodesByUserId(1L)).thenReturn(List.of("ROLE_ADMIN"));

        UserBO result = userManager.getByUsername("admin");

        assertNotNull(result);
        assertEquals("admin", result.getUsername());
        assertEquals(List.of("ROLE_ADMIN"), result.getRoles());
    }

    @Test
    void getByUsernameNotFound() {
        when(userMapper.selectByUsername("unknown")).thenReturn(null);

        UserBO result = userManager.getByUsername("unknown");

        assertNull(result);
        verify(userConverter, never()).toBO(any());
    }

    @Test
    void getByIdFound() {
        UserDO userDO = buildUserDO(1L, "admin");
        UserBO userBO = buildUserBO(1L, "admin");

        when(userMapper.selectById(1L)).thenReturn(userDO);
        when(userConverter.toBO(userDO)).thenReturn(userBO);
        when(userMapper.selectRoleCodesByUserId(1L)).thenReturn(List.of("ROLE_ADMIN", "ROLE_USER"));

        UserBO result = userManager.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(2, result.getRoles().size());
    }

    @Test
    void getByIdNotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);

        UserBO result = userManager.getById(999L);

        assertNull(result);
    }

    private UserDO buildUserDO(Long id, String username) {
        UserDO userDO = new UserDO();
        userDO.setId(id);
        userDO.setUsername(username);
        userDO.setPasswordHash("$2a$10$encoded");
        userDO.setDisplayName("Test User");
        userDO.setEmail("test@example.com");
        userDO.setStatus("ENABLED");
        userDO.setCreatedAt(LocalDateTime.now());
        userDO.setUpdatedAt(LocalDateTime.now());
        return userDO;
    }

    private UserBO buildUserBO(Long id, String username) {
        UserBO userBO = new UserBO();
        userBO.setId(id);
        userBO.setUsername(username);
        userBO.setPasswordHash("$2a$10$encoded");
        userBO.setDisplayName("Test User");
        userBO.setEmail("test@example.com");
        userBO.setStatus("ENABLED");
        return userBO;
    }
}
