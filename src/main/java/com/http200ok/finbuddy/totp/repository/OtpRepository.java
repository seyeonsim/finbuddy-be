package com.http200ok.finbuddy.totp.repository;

import com.http200ok.finbuddy.member.domain.Member;
import com.http200ok.finbuddy.totp.domain.Otp;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpRepository extends JpaRepository<Otp, Long> {
    boolean existsByMember(Member member);
    Optional<Otp> findByMember(Member member);
    void deleteByMember(Member member);
}
