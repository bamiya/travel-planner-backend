package com.example.travel_planner.service;

import com.example.travel_planner.config.JwtTokenProvider;
import com.example.travel_planner.config.StatusCode;
import com.example.travel_planner.entity.Comments;
import com.example.travel_planner.entity.Likes;
import com.example.travel_planner.entity.Users;
import com.example.travel_planner.repository.CommentRepository;
import com.example.travel_planner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CommentService {
    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    public ResponseEntity addComment(String token, Map<String, String> data){
        String tokenFilter = token.split(" ")[1];
        JwtTokenProvider jwtTokenProvider = this.jwtTokenProvider;
        if(jwtTokenProvider.validateAccessToken(tokenFilter)){
            String getCommentFromToken = jwtTokenProvider.getUserEmailFromToken(tokenFilter);//요고 집어넣음
            Optional<Users> user = userRepository.findById(getCommentFromToken);

            Comments comments = Comments.builder() //이중에서 날짜데타를 내가 없앤것같음 요 내용도 한번 고쳐봐야할듯?
                    .id(data.get("id"))
                    .content(data.get("content"))
                    .type(data.get("type"))
                    .date(LocalDate.now())
                    .email(user.get())
                    .build();

            commentRepository.save(comments);
            return new StatusCode(HttpStatus.OK, "댓글 추가 성공").sendResponse();
        }else{
            return new StatusCode(HttpStatus.UNAUTHORIZED, "만료된 토큰").sendResponse();
        }
    }

    public ResponseEntity getComment(String id){
        List<Comments> comments = commentRepository.findById(id);
        return new StatusCode(HttpStatus.OK, comments, "관광지댓글 조회성공").sendResponse();
    }
    @Transactional
    public ResponseEntity getMyPage(String token){ //토큰 마이페이지 사용할 리스트출력
        String tokenFilter = token.split(" ")[1];
        JwtTokenProvider jwtTokenProvider = this.jwtTokenProvider;
        if(jwtTokenProvider.validateAccessToken(tokenFilter)){
            String getCommentFromToken = jwtTokenProvider.getUserEmailFromToken(tokenFilter);
            Optional<Users> user = userRepository.findById(getCommentFromToken);
            return new StatusCode(HttpStatus.OK, user, "댓글 조회 성공").sendResponse();
        }else{
            return new StatusCode(HttpStatus.UNAUTHORIZED, "만료된 토큰").sendResponse();
        }
    }

    public ResponseEntity getMyComments(String token){ // 내가 쓴 댓글 목록
        String tokenFilter = token.split(" ")[1];
        if(jwtTokenProvider.validateAccessToken(tokenFilter)){
            String email = jwtTokenProvider.getUserEmailFromToken(tokenFilter);
            List<Comments> comments = commentRepository.findByEmail(email);
            return new StatusCode(HttpStatus.OK, comments, "내 댓글 조회 성공").sendResponse();
        }else{
            return new StatusCode(HttpStatus.UNAUTHORIZED, "만료된 토큰").sendResponse();
        }
    }
}
