package com.example.heatcalculate.service;

import com.example.heatcalculate.exception.ImageValidationException;
import com.example.heatcalculate.exception.ModelServiceException;
import com.example.heatcalculate.model.CalorieResult;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * CalorieService 单元测试
 * 验证 LangChain4j ImageContent 传图方式的正确性
 */
@ExtendWith(MockitoExtension.class)
class CalorieServiceTest {

    @Mock
    private ChatLanguageModel chatModel;

    private ImageValidatorService imageValidatorService;
    private CalorieService calorieService;

    private static final String VALID_JSON =
        "{\"foods\":[{\"name\":\"米饭\",\"estimatedWeight\":\"200g\"," +
        "\"calories\":{\"low\":200,\"mid\":230,\"high\":260}}]," +
        "\"totalCalories\":{\"low\":200,\"mid\":230,\"high\":260}," +
        "\"disclaimer\":\"热量数据为估算值\"}";

    @BeforeEach
    void setUp() {
        imageValidatorService = new ImageValidatorService();
        calorieService = new CalorieService(imageValidatorService, chatModel);
    }

    // ─── parseResponse 单元测试（不依赖 mock）─────────────────────────────────

    @Test
    @DisplayName("parseResponse - 正常 JSON 解析成功")
    void parseResponse_ValidJson_ReturnsResult() {
        CalorieResult result = calorieService.parseResponse(VALID_JSON);

        assertNotNull(result);
        assertEquals(1, result.getFoods().size());
        assertEquals("米饭", result.getFoods().get(0).getName());
        assertEquals(230, result.getTotalCalories().getMid());
    }

    @Test
    @DisplayName("parseResponse - AI 返回 markdown 代码块时能正确解析")
    void parseResponse_MarkdownCodeBlock_StripsAndParses() {
        String withMarkdown = "```json\n" + VALID_JSON + "\n```";
        CalorieResult result = calorieService.parseResponse(withMarkdown);

        assertNotNull(result);
        assertEquals(1, result.getFoods().size());
        assertEquals("米饭", result.getFoods().get(0).getName());
    }

    @Test
    @DisplayName("parseResponse - AI 返回无语言标记的代码块时能正确解析")
    void parseResponse_PlainCodeBlock_StripsAndParses() {
        String withPlainBlock = "```\n" + VALID_JSON + "\n```";
        CalorieResult result = calorieService.parseResponse(withPlainBlock);

        assertNotNull(result);
        assertEquals(1, result.getFoods().size());
    }

    @Test
    @DisplayName("parseResponse - 无效 JSON 时返回降级结果")
    void parseResponse_InvalidJson_ReturnsFallback() {
        CalorieResult result = calorieService.parseResponse("这不是JSON");

        assertNotNull(result);
        assertNotNull(result.getFoods());
        assertTrue(result.getFoods().isEmpty());
        assertEquals(0, result.getTotalCalories().getMid());
        assertEquals("解析结果失败，请重试", result.getDisclaimer());
    }

    // ─── analyzeFood 集成测试（mock QwenChatModel）────────────────────────────

    @Test
    @DisplayName("analyzeFood - 正常流程：图片校验通过，AI 返回正确结果")
    void analyzeFood_HappyPath_ReturnsResult() {
        MultipartFile image = new MockMultipartFile("image", "food.jpg", "image/jpeg", new byte[1024]);
        Response<AiMessage> mockResponse = Response.from(AiMessage.from(VALID_JSON), new TokenUsage(10, 20));
        when(chatModel.generate(anyList())).thenReturn(mockResponse);

        CalorieResult result = calorieService.analyzeFood(image, null);

        assertNotNull(result);
        assertEquals(1, result.getFoods().size());
        assertEquals("米饭", result.getFoods().get(0).getName());
        verify(chatModel, times(1)).generate(anyList());
    }

    @Test
    @DisplayName("analyzeFood - 带备注时，备注内容出现在发送给 AI 的消息中")
    void analyzeFood_WithNote_NoteIncludedInMessage() {
        MultipartFile image = new MockMultipartFile("image", "food.jpg", "image/jpeg", new byte[1024]);
        Response<AiMessage> mockResponse = Response.from(AiMessage.from(VALID_JSON), new TokenUsage(10, 20));
        ArgumentCaptor<List<ChatMessage>> captor = ArgumentCaptor.forClass(List.class);
        when(chatModel.generate(captor.capture())).thenReturn(mockResponse);

        calorieService.analyzeFood(image, "这是午餐");

        List<ChatMessage> sentMessages = captor.getValue();
        UserMessage sent = (UserMessage) sentMessages.get(0);
        // 消息中应包含 TextContent，且文本中含有备注
        String fullText = sent.contents().stream()
            .filter(c -> c instanceof dev.langchain4j.data.message.TextContent)
            .map(c -> ((dev.langchain4j.data.message.TextContent) c).text())
            .findFirst().orElse("");
        assertTrue(fullText.contains("这是午餐"), "发送给 AI 的文本应包含用户备注");
    }

    @Test
    @DisplayName("analyzeFood - 消息中包含 ImageContent（正确传图方式）")
    void analyzeFood_ImageContentPresent_InUserMessage() {
        MultipartFile image = new MockMultipartFile("image", "food.jpg", "image/jpeg", new byte[1024]);
        Response<AiMessage> mockResponse = Response.from(AiMessage.from(VALID_JSON), new TokenUsage(10, 20));
        ArgumentCaptor<List<ChatMessage>> captor = ArgumentCaptor.forClass(List.class);
        when(chatModel.generate(captor.capture())).thenReturn(mockResponse);

        calorieService.analyzeFood(image, null);

        List<ChatMessage> sentMessages = captor.getValue();
        UserMessage sent = (UserMessage) sentMessages.get(0);
        boolean hasImageContent = sent.contents().stream()
            .anyMatch(c -> c instanceof dev.langchain4j.data.message.ImageContent);
        assertTrue(hasImageContent, "UserMessage 中必须包含 ImageContent，而非把 Base64 塞进文本");
    }

    @Test
    @DisplayName("analyzeFood - AI 调用抛出异常时，包装为 ModelServiceException")
    void analyzeFood_AiThrowsException_WrapsToModelServiceException() {
        MultipartFile image = new MockMultipartFile("image", "food.jpg", "image/jpeg", new byte[1024]);
        when(chatModel.generate(anyList())).thenThrow(new RuntimeException("网络超时"));

        ModelServiceException ex = assertThrows(
            ModelServiceException.class,
            () -> calorieService.analyzeFood(image, null)
        );
        assertTrue(ex.getMessage().contains("模型服务暂时不可用"));
    }

    @Test
    @DisplayName("analyzeFood - 图片格式不支持时，抛出 ImageValidationException")
    void analyzeFood_InvalidImageFormat_ThrowsImageValidationException() {
        MultipartFile image = new MockMultipartFile("image", "test.gif", "image/gif", new byte[1024]);

        assertThrows(ImageValidationException.class, () -> calorieService.analyzeFood(image, null));
        // AI 服务不应被调用
        verifyNoInteractions(chatModel);
    }

    @Test
    @DisplayName("analyzeFood - 空文件时，抛出 ImageValidationException")
    void analyzeFood_EmptyFile_ThrowsImageValidationException() {
        MultipartFile image = new MockMultipartFile("image", "empty.jpg", "image/jpeg", new byte[0]);

        assertThrows(ImageValidationException.class, () -> calorieService.analyzeFood(image, null));
        verifyNoInteractions(chatModel);
    }

    @Test
    @DisplayName("analyzeFood - AI 返回 markdown 代码块时，能正确解析")
    void analyzeFood_AiReturnsMarkdownBlock_ParsesCorrectly() {
        MultipartFile image = new MockMultipartFile("image", "food.jpg", "image/jpeg", new byte[1024]);
        String markdownResponse = "```json\n" + VALID_JSON + "\n```";
        Response<AiMessage> mockResponse = Response.from(AiMessage.from(markdownResponse), new TokenUsage(10, 20));
        when(chatModel.generate(anyList())).thenReturn(mockResponse);

        CalorieResult result = calorieService.analyzeFood(image, null);

        assertNotNull(result);
        assertEquals(1, result.getFoods().size());
        assertEquals("米饭", result.getFoods().get(0).getName());
    }
}
