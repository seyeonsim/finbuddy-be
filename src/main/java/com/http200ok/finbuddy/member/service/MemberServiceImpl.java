package com.http200ok.finbuddy.member.service;

import com.http200ok.finbuddy.member.domain.Member;
import com.http200ok.finbuddy.member.dto.SignUpRequest;
import com.http200ok.finbuddy.member.repository.MemberRepository;
import com.http200ok.finbuddy.security.CustomUserDetails;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signUp(SignUpRequest request) {
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        Member member = Member.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(encodedPassword)
                .birthDate(request.getBirthDate())
                .sex(request.getSex())
                .job(request.getJob())
                .income(request.getIncome())
                .build();

        memberRepository.save(member);
    }


    public ResponseEntity<?> getCurrentUser(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 정보 없음");
        }

        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // ✅ 응답 데이터 맵 생성
        Map<String, Object> response = new HashMap<>();
        response.put("id", member.getId());
        response.put("email", member.getEmail());
        response.put("name", member.getName());

        return ResponseEntity.ok(response);
    }

}