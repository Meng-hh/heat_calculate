package com.example.heatcalculate.service;

import com.example.heatcalculate.model.*;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * RefinedAnalysisService 单元测试
 */
class RefinedAnalysisServiceTest {

    private RefinedAnalysisService service;
    private ChatLanguageModel mockChatModel;
    private SessionStore sessionStore;
    private ImageValidatorService imageValidatorService;

    @BeforeEach
    void setUp() {
        mockChatModel = mock(ChatLanguageModel.class);
        sessionStore = new SessionStore();
        imageValidatorService = new ImageValidatorService();
        service = new RefinedAnalysisService(imageValidatorService, mockChatModel, sessionStore);
    }

    @Test
    void shouldAskClarification_wide_range_returns_true() {
        CalorieResult result = new CalorieResult();
        CalorieRange range = new CalorieRange(200, 400, 500);
        result.setTotalCalories(range);

        assertTrue(service.shouldAskClarification(result));
    }

    @Test
    void shouldAskClarification_narrow_range_returns_false() {
        CalorieResult result = new CalorieResult();
        CalorieRange range = new CalorieRange(300, 380, 450);
        result.setTotalCalories(range);

        assertFalse(service.shouldAskClarification(result));
    }

    @Test
    void shouldAskClarification_null_result_returns_false() {
        assertFalse(service.shouldAskClarification(null));
    }

    @Test
    void startRefinedAnalysis_complete_without_question() {
        // 模拟 AI 返回窄区间（不需要追问）
        String aiJson = "{\"foods\":[{\"name\":\"白米饭\",\"estimatedWeight\":\"150g\",\"calories\":{\"low\":180,\"mid\":200,\"high\":220}}]," +
            "\"totalCalories\":{\"low\":180,\"mid\":200,\"high\":220},\"disclaimer\":\"估算值\"}";

        when(mockChatModel.generate(anyList()))
            .thenReturn(new Response<>(AiMessage.from(aiJson)));

        MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[100]);
        RefinedAnalysisResponse response = service.startRefinedAnalysis(image, null);

        assertEquals("complete", response.getStatus());
        assertNotNull(response.getResult());
        assertNull(response.getQuestion());
    }

    @Test
    void startRefinedAnalysis_need_input_with_question() {
        // 模拟 AI 返回宽区间 + 追问
        String aiJson = "{\"foods\":[{\"name\":\"面条\",\"estimatedWeight\":\"200-300g\",\"calories\":{\"low\":300,\"mid\":450,\"high\":600}}]," +
            "\"totalCalories\":{\"low\":300,\"mid\":450,\"high\":600},\"disclaimer\":\"估算值\"}\n" +
            "QUESTION:这碗面是素面还是加了肉？";

        when(mockChatModel.generate(anyList()))
            .thenReturn(new Response<>(AiMessage.from(aiJson)));

        MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[100]);
        RefinedAnalysisResponse response = service.startRefinedAnalysis(image, null);

        assertEquals("need_input", response.getStatus());
        assertNotNull(response.getSessionId());
        assertEquals("这碗面是素面还是加了肉？", response.getQuestion());
        assertNotNull(response.getPartialResult());
    }

    @Test
    void continueRefinedAnalysis_max_rounds_returns_result() {
        // 准备一个已经达到 4 轮的会话（下一轮是第 5 轮 = MAX_ROUNDS）
        String sessionId = "test-session";
        AnalysisSession session = new AnalysisSession(sessionId);
        session.setRoundCount(5); // 已达最大轮数
        CalorieResult lastResult = new CalorieResult();
        lastResult.setFoods(new ArrayList<>());
        lastResult.setTotalCalories(new CalorieRange(300, 400, 500));
        lastResult.setDisclaimer("test");
        session.setLastResult(lastResult);
        sessionStore.put(sessionId, session);

        RefinedAnalysisResponse response = service.continueRefinedAnalysis(sessionId, "一些回答");

        assertEquals("complete", response.getStatus());
        assertNotNull(response.getResult());
        // 验证会话被移除
        assertTrue(sessionStore.get(sessionId).isEmpty());
    }

    @Test
    void continueRefinedAnalysis_expired_session_returns_null() {
        // 不存在的会话
        RefinedAnalysisResponse response = service.continueRefinedAnalysis("nonexistent", "answer");
        assertNull(response);
    }
}
