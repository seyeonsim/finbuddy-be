package com.http200ok.finbuddy.security;

import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails {
    // memberId를 반환하는 Getter 추가
    @Getter
    private final Long memberId;
    private final String username;
    private final String password;

    public CustomUserDetails(Long memberId, String username, String password) {
        this.memberId = memberId;
        this.username = username;
        this.password = password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null; // 권한이 필요하면 설정
    }

    @Override
    public String getPassword() {
        return password; //  사용자 비밀번호 반환
    }

    @Override
    public String getUsername() {
        return username; //  사용자 이름 반환
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; //  계정 만료 여부 (true면 만료되지 않음)
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; //  계정 잠김 여부 (true면 잠기지 않음)
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; //  비밀번호 만료 여부 (true면 만료되지 않음)
    }

    @Override
    public boolean isEnabled() {
        return true; // ✅ 계정 활성화 여부 (true면 활성화됨)
    }
}
