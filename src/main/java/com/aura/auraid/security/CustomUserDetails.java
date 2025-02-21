package com.aura.auraid.security;

import org.springframework.security.core.GrantedAuthority;
import java.util.Collection;

public class CustomUserDetails extends org.springframework.security.core.userdetails.User {
    private final Long id;

    public CustomUserDetails(Long id, String username, String password, 
                           Collection<? extends GrantedAuthority> authorities,
                           boolean accountNonLocked, boolean accountNonExpired,
                           boolean credentialsNonExpired, boolean enabled) {
        super(username, password, enabled, accountNonExpired, 
              credentialsNonExpired, accountNonLocked, authorities);
        this.id = id;
    }

    public Long getId() {
        return id;
    }
} 