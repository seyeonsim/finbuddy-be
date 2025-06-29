package com.http200ok.finbuddy.security;

import com.http200ok.finbuddy.member.domain.Member;
import com.http200ok.finbuddy.member.repository.MemberRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.util.Optional;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
//        Optional<String> token = jwtTokenProvider.resolveToken(request);
        Optional<String> token = extractAccessTokenFromCookies(request);


        token.ifPresent(t -> {
            try {
                if (jwtTokenProvider.validateToken(t)) {
                    Long memberId = jwtTokenProvider.getMemberIdFromAccessToken(t);
                    Member member = memberRepository.findById(memberId) // ID 기반 조회
                            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + memberId));

                    UserDetails userDetails = userDetailsService.loadUserByUsername(member.getEmail());
//                    CustomUserDetails userDetails = new CustomUserDetails(member.getId(), member.getEmail(), member.getPassword());

                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(userDetails, null));
                }
//            } catch (ExpiredJwtException e) {
//                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//                response.setContentType("application/json");
//                response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Token expired\"}");
//                return;

            } catch (JwtException ignored) {
            }
        });

        chain.doFilter(request, response);
    }

    // 쿠키에서 액세스 토큰을 추출하는 메서드
    private Optional<String> extractAccessTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();

        for (Cookie cookie : request.getCookies()) {
            if ("accessToken".equals(cookie.getName())) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }
}
