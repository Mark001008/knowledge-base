package com.ma.kb.core.auth;

import com.ma.kb.common.enums.UserStatusEnum;
import com.ma.kb.manager.auth.UserManager;
import com.ma.kb.manager.auth.bo.UserBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserManager userManager;

    private UserDetailsServiceImpl userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new UserDetailsServiceImpl(userManager);
    }

    @Test
    void loadUserByUsernameSuccess() {
        UserBO userBO = new UserBO();
        userBO.setId(1L);
        userBO.setUsername("admin");
        userBO.setPasswordHash("$2a$10$encodedPassword");
        userBO.setStatus(UserStatusEnum.ENABLED.getCode());
        userBO.setRoles(List.of("ROLE_ADMIN", "ROLE_USER"));

        when(userManager.getByUsername("admin")).thenReturn(userBO);

        UserDetails userDetails = userDetailsService.loadUserByUsername("admin");

        assertEquals("admin", userDetails.getUsername());
        assertEquals("$2a$10$encodedPassword", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertEquals(2, userDetails.getAuthorities().size());
    }

    @Test
    void loadUserByUsernameNotFound() {
        when(userManager.getByUsername("unknown")).thenReturn(null);

        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("unknown"));
    }

    @Test
    void loadUserByUsernameDisabledUser() {
        UserBO userBO = new UserBO();
        userBO.setId(2L);
        userBO.setUsername("disabled");
        userBO.setPasswordHash("$2a$10$encodedPassword");
        userBO.setStatus(UserStatusEnum.DISABLED.getCode());
        userBO.setRoles(List.of("ROLE_USER"));

        when(userManager.getByUsername("disabled")).thenReturn(userBO);

        UserDetails userDetails = userDetailsService.loadUserByUsername("disabled");

        assertFalse(userDetails.isEnabled());
    }
}
