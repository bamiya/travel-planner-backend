package com.example.travel_planner.config;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

public class KakaoProvider {
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
