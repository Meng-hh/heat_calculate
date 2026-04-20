package com.example.heatcalculate.service;

import com.example.heatcalculate.model.AnalysisSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SessionStore 单元测试
 */
class SessionStoreTest {

    private SessionStore sessionStore;

    @BeforeEach
    void setUp() {
        sessionStore = new SessionStore();
    }

    @Test
    void put_and_get_should_work() {
        AnalysisSession session = new AnalysisSession("test-id");
        sessionStore.put("test-id", session);

        Optional<AnalysisSession> result = sessionStore.get("test-id");
        assertTrue(result.isPresent());
        assertEquals("test-id", result.get().getSessionId());
    }

    @Test
    void get_nonexistent_should_return_empty() {
        Optional<AnalysisSession> result = sessionStore.get("nonexistent");
        assertTrue(result.isEmpty());
    }

    @Test
    void get_expired_session_should_return_empty_and_remove() {
        AnalysisSession session = new AnalysisSession("expired-id");
        // 设置创建时间为 4 分钟前（超过 3 分钟过期时间）
        session.setCreatedAt(Instant.now().minusSeconds(240));
        sessionStore.put("expired-id", session);

        Optional<AnalysisSession> result = sessionStore.get("expired-id");
        assertTrue(result.isEmpty());
        // 验证已被移除
        assertEquals(0, sessionStore.size());
    }

    @Test
    void remove_should_work() {
        AnalysisSession session = new AnalysisSession("remove-id");
        sessionStore.put("remove-id", session);
        assertEquals(1, sessionStore.size());

        sessionStore.remove("remove-id");
        assertEquals(0, sessionStore.size());
    }
}
