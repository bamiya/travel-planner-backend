package com.example.travel_planner.config;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

public class KakaoProvider {
    // 인증코드(code)를 카카오 액세스 토큰으로 교환한다. client_secret이 필요한 단계라
    // 반드시 서버 사이드(여기)에서만 호출해야 한다 - 프론트에 두면 비밀키가 번들에 노출됨.
    public String exchangeCodeForToken(String code, String clientId, String clientSecret, String redirectUri) {
        try {
            String url = "https://kauth.kakao.com/oauth/token";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("redirect_uri", redirectUri);
            params.add("code", code);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<Map<String, Object>> response = new RestTemplate().exchange(
                    url, HttpMethod.POST, request, new ParameterizedTypeReference<Map<String, Object>>() {});
            Object accessToken = response.getBody() != null ? response.getBody().get("access_token") : null;
            return accessToken != null ? accessToken.toString() : null;
        } catch (Exception e) {
            System.out.println(e.toString());
            return null;
        }
    }

    public Map<String, Object> getUserInfo(String token){
        try{
            String url = "https://kapi.kakao.com/v2/user/me";
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + token);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = new RestTemplate().exchange(
                    url, HttpMethod.POST, request, new ParameterizedTypeReference<Map<String, Object>>() {});
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) response.getBody().get("kakao_account");
            return result;
        } catch (Exception e){
            System.out.println(e.toString());
            return null;
        }
    }
}
