package com.http200ok.finbuddy.auth.repository;

import com.http200ok.finbuddy.auth.domain.RefreshToken;
import com.http200ok.finbuddy.member.domain.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByMember(Member member);
}
