package com.example.heatcalculate.service;

import com.example.heatcalculate.exception.SessionExpiredException;
import com.example.heatcalculate.model.*;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * CorrectionService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class CorrectionServiceTest {

    @Mock
    private ChatLanguageModel chatModel;

    private SessionStore sessionStore;
    private CorrectionService correctionService;

    @BeforeEach
    void setUp() {
        sessionStore = new SessionStore();
        correctionService = new CorrectionService(sessionStore, chatModel);
    }

    private AnalysisSession createTestSession(String sessionId) {
        AnalysisSession session = new AnalysisSession(sessionId);
        CalorieResult lastResult = new CalorieResult();
        lastResult.setFoods(new ArrayList<>());
        CalorieRange range = new CalorieRange(300, 400, 500);
        lastResult.setTotalCalories(range);
        lastResult.setDisclaimer("测试");
        session.setLastResult(lastResult);

        List<Map<String, Object>> chatHistory = new ArrayList<>();
        chatHistory.add(Map.of("role", "user", "content", "分析图片",
            "hasImage", true, "base64Data", "dGVzdA==", "mimeType", "image/png"));
        chatHistory.add(Map.of("role", "assistant", "content",
            "{\"foods\":[{\"name\":\"米饭\",\"estimatedWeight\":\"200g\",\"calories\":{\"low\":200,\"mid\":250,\"high\":300}}],\"totalCalories\":{\"low\":200,\"mid\":250,\"high\":300},\"disclaimer\":\"测试\"}"));
        session.setChatHistory(chatHistory);

        sessionStore.put(sessionId, session);
        return session;
    }

    @Test
    @DisplayName("3.1 正常纠正流程 - mock ChatLanguageModel")
    void testCorrectNormalFlow() {
        String sessionId = "test-session-1";
        createTestSession(sessionId);

        String aiResponse = "{\"foods\":[{\"name\":\"梅菜扣肉\",\"estimatedWeight\":\"300g\",\"calories\":{\"low\":500,\"mid\":600,\"high\":700}}],\"totalCalories\":{\"low\":500,\"mid\":600,\"high\":700},\"disclaimer\":\"热量数据为估算值\"}";
        when(chatModel.generate(anyList())).thenReturn(new Response<>(AiMessage.from(aiResponse)));

        List<CorrectionRequest.CorrectionItem> corrections = new ArrayList<>();
        CorrectionRequest.CorrectionItem item = new CorrectionRequest.CorrectionItem();
        item.setIndex(0);
        item.setName("梅菜扣肉");
        item.setWeight("300g");
        corrections.add(item);

        CalorieResult result = correctionService.correct(sessionId, corrections, "少油少盐");

        assertNotNull(result);
        assertEquals(sessionId, result.getSessionId());
        assertNotNull(result.getFoods());
        assertEquals(1, result.getFoods().size());
        assertEquals("梅菜扣肉", result.getFoods().get(0).getName());
        assertEquals(600, result.getTotalCalories().getMid());

        verify(chatModel, times(1)).generate(anyList());
    }

    @Test
    @DisplayName("3.2 会话过期时抛出 SessionExpiredException")
    void testCorrectSessionExpired() {
        String sessionId = "non-existent-session";

        assertThrows(SessionExpiredException.class, () -> {
            correctionService.correct(sessionId, null, null);
        });
    }

    @Test
    @DisplayName("3.3 纠正消息正确追加到 chatMemory")
    @SuppressWarnings("unchecked")
    void testCorrectionMessageAppendedToChatMemory() {
        String sessionId = "test-session-2";
        AnalysisSession session = createTestSession(sessionId);
        int originalHistorySize = session.getChatHistory().size();

        String aiResponse = "{\"foods\":[],\"totalCalories\":{\"low\":0,\"mid\":0,\"high\":0},\"disclaimer\":\"测试\"}";
        when(chatModel.generate(anyList())).thenReturn(new Response<>(AiMessage.from(aiResponse)));

        List<CorrectionRequest.CorrectionItem> corrections = new ArrayList<>();
        CorrectionRequest.CorrectionItem item = new CorrectionRequest.CorrectionItem();
        item.setIndex(0);
        item.setName("红烧鱼");
        item.setWeight("200g");
        corrections.add(item);

        correctionService.correct(sessionId, corrections, "偏辣");

        // 验证 chatHistory 增加了 2 条（user + assistant）
        assertEquals(originalHistorySize + 2, session.getChatHistory().size());

        // 验证纠正消息内容
        Map<String, Object> userEntry = session.getChatHistory().get(originalHistorySize);
        String content = (String) userEntry.get("content");
        assertTrue(content.contains("红烧鱼"));
        assertTrue(content.contains("200g"));
        assertTrue(content.contains("偏辣"));

        // 验证发送给模型的消息中包含纠正内容
        ArgumentCaptor<List<ChatMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(chatModel).generate(captor.capture());
        List<ChatMessage> sentMessages = captor.getValue();

        // 最后一条消息应该是纠正 UserMessage
        String lastMessage = sentMessages.get(sentMessages.size() - 1).toString();
        assertTrue(lastMessage.contains("红烧鱼"));
    }

    @Test
    @DisplayName("构建纠正消息 - 包含修改项和备注")
    void testBuildCorrectionMessage() {
        List<CorrectionRequest.CorrectionItem> corrections = new ArrayList<>();
        CorrectionRequest.CorrectionItem item1 = new CorrectionRequest.CorrectionItem();
        item1.setIndex(0);
        item1.setName("梅菜扣肉");
        item1.setWeight("300g");
        corrections.add(item1);

        CorrectionRequest.CorrectionItem item2 = new CorrectionRequest.CorrectionItem();
        item2.setIndex(1);
        item2.setName("青菜");
        corrections.add(item2);

        String message = correctionService.buildCorrectionMessage(corrections, "少油少盐");

        assertTrue(message.contains("梅菜扣肉"));
        assertTrue(message.contains("300g"));
        assertTrue(message.contains("青菜"));
        assertTrue(message.contains("少油少盐"));
        assertTrue(message.contains("重新计算"));
    }
}
