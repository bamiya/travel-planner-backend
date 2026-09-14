package com.example.travel_planner.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenProvider {
    @Value("${app.jwt.secret-refresh}")
    private String JWT_SECRET_REFRESH;
    @Value("${app.jwt.secret-access}")
    private String JWT_SECRET_ACCESS;
    @Value("${app.jwt.secret-reset}")
    private String JWT_SECRET_RESET;

    static final int JWT_EXPIRATION_REFRESH = 604800000; // 리프레쉬 (7d)
    static final int JWT_EXPIRATION_ACCESS = 1080000; // 액세스 (3h)
    static final int JWT_EXPIRATION_RESET = 600000; // 비밀번호 재설정 (10m)

    public Map<String, String> generateToken(String email){
        Map<String, String> result = new HashMap<>();
        // 토큰 유효시간

        String refresh_token = Jwts.builder()
                .setSubject(email) // 사용자
                .setIssuedAt(new Date()) // 현재 시간 기반으로 생성
                .setExpiration(new Date(new Date().getTime() + JWT_EXPIRATION_REFRESH)) // 만료 시간 세팅
                .signWith(SignatureAlgorithm.HS256, JWT_SECRET_REFRESH.getBytes()) // 사용할 암호화 알고리즘, signature에 들어갈 secret 값 세팅
                .compact();
        String access_token = Jwts.builder()
                .setSubject(email) // 사용자
                .setIssuedAt(new Date()) // 현재 시간 기반으로 생성
                .setExpiration(new Date(new Date().getTime() + JWT_EXPIRATION_ACCESS)) // 만료 시간 세팅
                .signWith(SignatureAlgorithm.HS256, JWT_SECRET_ACCESS.getBytes()) // 사용할 암호화 알고리즘, signature에 들어갈 secret 값 세팅
                .compact();

        result.put("refresh_token", refresh_token);
        result.put("access_token", access_token);
        return result;
    }

    public Map<String, String> generateAccessToken(String refreshToken){
        Map<String, String> result = new HashMap<>();
        // 토큰 유효시간
        String access_token = null;
        try {
            Claims claims = Jwts.parser().setSigningKey(JWT_SECRET_REFRESH.getBytes()).parseClaimsJws(refreshToken).getBody(); // 리프레쉬 토큰이 만료인지 아닌지 and 이메일 가져오기
            access_token = Jwts.builder()
                    .setSubject((String) claims.get("sub")) // 사용자
                    .setIssuedAt(new Date()) // 현재 시간 기반으로 생성
                    .setExpiration(new Date(new Date().getTime() + JWT_EXPIRATION_ACCESS)) // 만료 시간 세팅
                    .signWith(SignatureAlgorithm.HS256, JWT_SECRET_ACCESS.getBytes()) // 사용할 암호화 알고리즘, signature에 들어갈 secret 값 세팅
                    .compact();
        } catch (Exception e) {
            System.out.println(e);
            result.put("access_token", null);
            return result;
        }

        result.put("access_token", access_token);
        return result;
    }

    // 토큰이 만료가 되었는지 안됐는지 확인하는 함수
    public boolean validateAccessToken(String token) {
        try {
            Claims claims = Jwts.parser().setSigningKey(JWT_SECRET_ACCESS.getBytes()).parseClaimsJws(token).getBody();
            return true;
        } catch (Exception e) {
            System.out.println(e);
        }
        return false;
    }

    // 토큰 안에 있는 사용자의 이메일을 주는 함수
    public String getUserEmailFromToken(String token){
        try {
            Claims claims = Jwts.parser().setSigningKey(JWT_SECRET_ACCESS.getBytes()).parseClaimsJws(token).getBody();
            System.out.println((String) claims.get("sub"));
            return (String) claims.get("sub");
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    // 비밀번호 재설정용 단기 토큰 발급 (이메일 인증코드 확인 후에만 호출되어야 함)
    public String generateResetToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + JWT_EXPIRATION_RESET))
                .signWith(SignatureAlgorithm.HS256, JWT_SECRET_RESET.getBytes())
                .compact();
    }

    // 비밀번호 재설정 토큰에서 이메일을 꺼내며, 위변조/만료 시 null 반환
    public String getEmailFromResetToken(String token) {
        try {
            Claims claims = Jwts.parser().setSigningKey(JWT_SECRET_RESET.getBytes()).parseClaimsJws(token).getBody();
            return (String) claims.get("sub");
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }
}
