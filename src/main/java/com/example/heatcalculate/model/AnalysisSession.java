package com.example.heatcalculate.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 精细分析会话
 * 存储多轮对话的状态和历史消息
 */
public class AnalysisSession {

    private String sessionId;
    private SessionStatus status;
    private List<Map<String, Object>> chatHistory; // 对话历史（用于多轮 API 调用）
    private String currentQuestion;
    private int roundCount;
    private Instant createdAt;
    private CalorieResult lastResult;

    public AnalysisSession() {
        this.chatHistory = new ArrayList<>();
        this.roundCount = 0;
        this.createdAt = Instant.now();
        this.status = SessionStatus.WAITING_INPUT;
    }

    public AnalysisSession(String sessionId) {
        this();
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public List<Map<String, Object>> getChatHistory() {
        return chatHistory;
    }

    public void setChatHistory(List<Map<String, Object>> chatHistory) {
        this.chatHistory = chatHistory;
    }

    public String getCurrentQuestion() {
        return currentQuestion;
    }

    public void setCurrentQuestion(String currentQuestion) {
        this.currentQuestion = currentQuestion;
    }

    public int getRoundCount() {
        return roundCount;
    }

    public void setRoundCount(int roundCount) {
        this.roundCount = roundCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public CalorieResult getLastResult() {
        return lastResult;
    }

    public void setLastResult(CalorieResult lastResult) {
        this.lastResult = lastResult;
    }

    /**
     * 判断会话是否已过期（3 分钟）
     */
    public boolean isExpired() {
        return Instant.now().isAfter(createdAt.plusSeconds(180));
    }
}
