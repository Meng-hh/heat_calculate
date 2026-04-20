# BUG 修复记录：AI 服务不可用

## 问题描述

**现象：** 用户上传图片后，后端返回错误：
```json
{"code":502,"message":"模型服务暂时不可用，请稍后重试"}
```

**根本原因：** 两个 bug 叠加导致：
1. `application.yml` 配置键名错误（`dash-scope` 应为 `dashscope`），导致 `apiKey` 为 null
2. `FoodCalorieAiService` 通过 `@AiService` 注解将 Base64 图片数据作为普通文本传递，DashScope 收到的是一段字符串而非图片内容，报 `url error`

## 错误日志

```
Caused by: com.alibaba.dashscope.exception.ApiException: 
{"statusCode":400,"message":"url error, please check url！
For details, see: https://help.aliyun.com/zh/model-studio/error-code#error-url",
"code":"InvalidParameter"}
```

## 问题分析

1. **配置键名错误**
   - 原配置使用 `langchain4j.dash-scope`，LangChain4j 期望的是 `langchain4j.dashscope`（无连字符）
   - 导致 `@Value("${langchain4j.dash-scope.api-key}")` 注入失败，`apiKey` 为 null

2. **图片传递方式错误**
   - `FoodCalorieAiService` 使用 `@AiService` + `@UserMessage` 注解方式，把 Base64 字符串拼进提示词文本
   - LangChain4j 不会将其识别为图片内容，DashScope 收到的是纯文本，无法解析为图片 URL

3. **LangChain4j 版本过低**
   - 0.25.0 尚未提供 `ImageContent` API，无法通过正确方式传递 Base64 图片

## 解决方案

### 方案选择

保留 LangChain4j 架构，升级版本并使用正确的多模态消息 API，而非绕过 LangChain4j 直接调用 REST API。

### 实施步骤

1. **升级 LangChain4j 到 0.36.2**
   - `ImageContent` API 在 0.27+ 引入，0.36.2 是 0.x 系列最后一个稳定版

2. **修复配置键名**
   - `application.yml`：`dash-scope` → `dashscope`
   - `LangChain4jConfig.java`：同步修正 `@Value` 注解中的键名

3. **重构 CalorieService，使用 `ImageContent` 正确传图**
   - 删除 `FoodCalorieAiService`（`@AiService` 注解方式无法传图）
   - 改用 `UserMessage.from(ImageContent.from(...), TextContent.from(...))` 构建多模态消息
   - 通过 `ChatLanguageModel.generate(List<ChatMessage>)` 调用模型

4. **将 Bean 类型改为 `ChatLanguageModel` 接口**
   - `LangChain4jConfig` 返回接口而非具体类，便于单元测试 mock

### 关键代码变更

**修复前（错误方式）：**
```java
// FoodCalorieAiService.java
@AiService
public interface FoodCalorieAiService {
    @UserMessage("分析图片：{{imageBase64}}")  // Base64 被当成普通文本
    CalorieResult analyze(@V("imageBase64") String imageBase64);
}
```

**修复后（正确方式）：**
```java
// CalorieService.java
UserMessage userMessage = UserMessage.from(
    ImageContent.from(base64Data, mimeType),   // 正确传递图片内容
    TextContent.from(SYSTEM_PROMPT + "\n\n" + userText)
);
Response<AiMessage> response = chatModel.generate(List.of(userMessage));
```

**配置修复：**
```yaml
# 修复前
langchain4j:
  dash-scope:
    api-key: sk-xxx

# 修复后
langchain4j:
  dashscope:
    api-key: sk-xxx
```

## 验证结果

### 单元测试（22 个，全部通过）

| 测试用例 | 结果 |
|---|---|
| 正常 JSON 解析 | ✅ |
| AI 返回 markdown 代码块时正确解析 | ✅ |
| 无效 JSON 时返回降级结果 | ✅ |
| 正常流程返回正确结果 | ✅ |
| 带备注时备注出现在 AI 消息中 | ✅ |
| UserMessage 中包含 ImageContent（核心验证） | ✅ |
| AI 调用异常时包装为 ModelServiceException | ✅ |
| 图片格式不支持时抛出 ImageValidationException | ✅ |
| 空文件时抛出 ImageValidationException | ✅ |

### 集成测试

- [x] 服务启动正常（0.957s）
- [x] 图片校验通过
- [x] AI 服务调用成功（不再报 `url error`）
- [x] 真实食物图片返回正确 JSON，HTTP 200

**真实请求响应示例：**
```json
{
  "foods": [{"name": "紫米", "estimatedWeight": "150-200g", "calories": {"low": 630, "mid": 720, "high": 810}}],
  "totalCalories": {"low": 630, "mid": 720, "high": 810},
  "disclaimer": "热量数据为估算值，实际值因食材和烹饪方式而异"
}
```

## 经验教训

1. **配置键名要与库的期望严格一致**，连字符和驼峰的细微差异会导致注入失败且不报明显错误

2. **多模态 API 需要使用专用的内容类型**，不能把图片数据拼进文本字符串

3. **第三方库版本要与所用 API 匹配**，`ImageContent` 等多模态 API 是后续版本才引入的

4. **将 Bean 声明为接口类型**（`ChatLanguageModel` 而非 `QwenChatModel`），可以在不启动 Spring 容器的情况下 mock，大幅降低测试成本

## 相关文件变更

| 文件 | 变更类型 |
|---|---|
| `src/main/java/com/example/heatcalculate/service/CalorieService.java` | 重构：改用 `ImageContent` 传图 |
| `src/main/java/com/example/heatcalculate/config/LangChain4jConfig.java` | 修复：键名 + 返回接口类型 |
| `src/main/resources/application.yml` | 修复：`dash-scope` → `dashscope` |
| `src/main/java/com/example/heatcalculate/ai/FoodCalorieAiService.java` | 删除：`@AiService` 方式无法传图 |
| `src/test/java/com/example/heatcalculate/service/CalorieServiceTest.java` | 新增：9 个单元测试用例 |
| `pom.xml` | 升级：LangChain4j `0.25.0` → `0.36.2` |

## 提交记录

```
commit 8da230d（已废弃方案：直接调用 REST API）
fix: Resolve AI service unavailable error by replacing LangChain4j with direct DashScope API call

当前方案（保留 LangChain4j 架构）：
fix: Fix AI service unavailable by using LangChain4j ImageContent API correctly
```
