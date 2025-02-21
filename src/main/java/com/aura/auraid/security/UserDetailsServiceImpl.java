package com.aura.auraid.security;

import com.aura.auraid.model.User;
import com.aura.auraid.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user details for username: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found with username: {}", username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });

        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                .collect(Collectors.toSet());
                
        log.debug("Found user: {}, with authorities: {}", username, authorities);

        return new CustomUserDetails(
            user.getId(),
            user.getUsername(),
            user.getPassword(),
            authorities,
            !user.getStatus().equals(com.aura.auraid.enums.UserStatus.INACTIVE),  // enabled
            true,  // accountNonExpired
            true,  // credentialsNonExpired
            !user.getStatus().equals(com.aura.auraid.enums.UserStatus.BLOCKED)  // accountNonLocked
        );
    }
} 