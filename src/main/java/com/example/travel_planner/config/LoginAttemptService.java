package com.example.travel_planner.config;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// 로그인 무차별 대입 공격 방지용 - 같은 이메일로 짧은 시간 안에 여러 번 실패하면
// 잠시 잠근다. 별도 인프라(Redis 등) 없이 단일 서버 규모에 맞춘 최소 구현이라
// 서버 재시작 시 초기화되고, 여러 인스턴스로 확장하면 인스턴스별로 따로 센다.
@Component
public class LoginAttemptService {
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 5 * 60 * 1000; // 5분

    private static class Attempt {
        final AtomicInteger count = new AtomicInteger(0);
        volatile long lockedUntil = 0;
    }

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    public boolean isLocked(String key) {
        Attempt a = attempts.get(key);
        return a != null && a.lockedUntil > System.currentTimeMillis();
    }

    public long getLockRemainingSeconds(String key) {
        Attempt a = attempts.get(key);
        if (a == null) return 0;
        return Math.max(0, (a.lockedUntil - System.currentTimeMillis()) / 1000);
    }

    public void loginFailed(String key) {
        Attempt a = attempts.computeIfAbsent(key, k -> new Attempt());
        if (a.count.incrementAndGet() >= MAX_ATTEMPTS) {
            a.lockedUntil = System.currentTimeMillis() + LOCKOUT_DURATION_MS;
            a.count.set(0);
        }
    }

    public void loginSucceeded(String key) {
        attempts.remove(key);
    }
}
