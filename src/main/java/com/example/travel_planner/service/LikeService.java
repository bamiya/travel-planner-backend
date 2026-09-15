package com.example.travel_planner.service;

import com.example.travel_planner.config.JwtTokenProvider;
import com.example.travel_planner.config.StatusCode;
import com.example.travel_planner.entity.Likes;
import com.example.travel_planner.entity.Users;
import com.example.travel_planner.repository.LikeRepository;
import com.example.travel_planner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class LikeService {
    @Autowired
    private LikeRepository likeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    public ResponseEntity getLikes(String token){
        String tokenFilter = token.split(" ")[1];
        JwtTokenProvider jwtTokenProvider = this.jwtTokenProvider;
        if(jwtTokenProvider.validateAccessToken(tokenFilter)){
            List<Likes> likes = likeRepository.selectLikeByEmail(jwtTokenProvider.getUserEmailFromToken(tokenFilter));
            return new StatusCode(HttpStatus.OK, likes, "좋아요 조회 성공").sendResponse();
        }else{
            return new StatusCode(HttpStatus.UNAUTHORIZED, "만료된 토큰").sendResponse();
        }
    }

    public ResponseEntity addLikes(String token, Map<String, String> data){
        String tokenFilter = token.split(" ")[1];
        JwtTokenProvider jwtTokenProvider = this.jwtTokenProvider;
        if(jwtTokenProvider.validateAccessToken(tokenFilter)){
            Optional<Users> user = userRepository.findById(jwtTokenProvider.getUserEmailFromToken(tokenFilter));
            Likes likes = Likes.builder()
                    .id(data.get("id"))
                    .type(data.get("type"))
                    .email(user.get())
                    .build();
            likeRepository.save(likes);
            return new StatusCode(HttpStatus.OK, "좋아요 추가 성공").sendResponse();
        }else{
            return new StatusCode(HttpStatus.UNAUTHORIZED, "만료된 토큰").sendResponse();
        }
    }

    @Transactional
    public ResponseEntity removeLikes(String token, String id){
        String tokenFilter = token.split(" ")[1];
        JwtTokenProvider jwtTokenProvider = this.jwtTokenProvider;
        if(jwtTokenProvider.validateAccessToken(tokenFilter)){
            Likes likes = likeRepository.findByIdAndEmail(jwtTokenProvider.getUserEmailFromToken(tokenFilter), id);
            likeRepository.deleteByIdx(likes.getLikeIdx());
            return new StatusCode(HttpStatus.OK, "좋아요 삭제 성공").sendResponse();
        }else{
            return new StatusCode(HttpStatus.UNAUTHORIZED, "토큰 만료").sendResponse();
        }
    }

    public ResponseEntity getLikeCount(String id){
        int cnt = likeRepository.selectLikeCount(Integer.parseInt(id));
        return new StatusCode(HttpStatus.OK, cnt, "좋아요 수 불러오기 완료").sendResponse();
    }
}
