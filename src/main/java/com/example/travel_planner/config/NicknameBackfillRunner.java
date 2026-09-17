package com.example.travel_planner.config;

import com.example.travel_planner.entity.Users;
import com.example.travel_planner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

// 닉네임 컬럼을 추가하기 전에 가입한 계정들은 nickname이 비어있다. 화면에는
// getDisplayNickname()이 이름으로 대체 표시해주지만, "닉네임 클릭 -> 그 닉네임으로
// 프로필 조회" 기능은 실제 DB의 nickname 값으로 찾기 때문에 비어있으면 못 찾는다.
// 서버 기동 시 한 번, 닉네임이 없는 계정에 한해 이름을 닉네임으로 채워 넣는다
// (이미 그 이름을 쓰는 닉네임이 있으면 겹치지 않게 뒤에 숫자를 붙인다).
@Component
public class NicknameBackfillRunner implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) {
        List<Users> targets = userRepository.findAll().stream()
                .filter(u -> u.getNickname() == null || u.getNickname().isBlank())
                .toList();
        for (Users user : targets) {
            String base = user.getName() != null ? user.getName() : "user" + user.getId();
            String candidate = base;
            int suffix = 1;
            while (userRepository.findByNickname(candidate).isPresent()) {
                candidate = base + suffix;
                suffix++;
            }
            user.setNickname(candidate);
            userRepository.save(user);
        }
    }
}
