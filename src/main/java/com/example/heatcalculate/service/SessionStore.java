package com.example.heatcalculate.service;

import com.example.heatcalculate.model.AnalysisSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 精细分析会话存储
 * 使用 ConcurrentHashMap 存储，支持懒清理过期会话
 */
@Component
public class SessionStore {

    private static final Logger log = LoggerFactory.getLogger(SessionStore.class);
    private final ConcurrentHashMap<String, AnalysisSession> sessions = new ConcurrentHashMap<>();

    /**
     * 存入会话
     */
    public void put(String sessionId, AnalysisSession session) {
        sessions.put(sessionId, session);
        log.debug("Session stored: {}, total sessions: {}", sessionId, sessions.size());
    }

    /**
     * 获取会话，包含懒清理逻辑（检查 createdAt + 3分钟）
     * 过期会话将被移除并返回 empty
     */
    public Optional<AnalysisSession> get(String sessionId) {
        AnalysisSession session = sessions.get(sessionId);
        if (session == null) {
            return Optional.empty();
        }
        if (session.isExpired()) {
            log.info("Session expired and removed: {}", sessionId);
            sessions.remove(sessionId);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    /**
     * 移除会话
     */
    public void remove(String sessionId) {
        sessions.remove(sessionId);
        log.debug("Session removed: {}", sessionId);
    }

    /**
     * 当前活跃会话数（用于监控）
     */
    public int size() {
        return sessions.size();
    }
}
