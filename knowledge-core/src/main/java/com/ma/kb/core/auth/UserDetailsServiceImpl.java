package com.ma.kb.core.auth;

import com.ma.kb.common.enums.UserStatusEnum;
import com.ma.kb.manager.auth.UserManager;
import com.ma.kb.manager.auth.bo.UserBO;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Security 用户详情服务
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserManager userManager;

    public UserDetailsServiceImpl(UserManager userManager) {
        this.userManager = userManager;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserBO userBO = userManager.getByUsername(username);
        if (userBO == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        List<SimpleGrantedAuthority> authorities = userBO.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        return new User(
                userBO.getUsername(),
                userBO.getPasswordHash(),
                UserStatusEnum.ENABLED.getCode().equals(userBO.getStatus()),
                true,
                true,
                true,
                authorities
        );
    }
}
