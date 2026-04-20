# BUG 修复记录：AI 服务不可用

## 问题描述

**现象：** 用户上传图片后，后端返回错误：
```json
{"code":502,"message":"模型服务暂时不可用，请稍后重试"}
```

**根本原因：** LangChain4j 的 DashScope 集成在处理 Base64 编码的图片时存在 bug，导致调用通义千问 API 时抛出异常。

## 错误日志

```
Caused by: com.alibaba.dashscope.exception.ApiException: 
{"statusCode":400,"message":"url error, please check url！
For details, see: https://help.aliyun.com/zh/model-studio/error-code#error-url",
"code":"InvalidParameter"}
```

## 问题分析

1. **LangChain4j DashScope 集成的限制**
   - LangChain4j 的 `QwenChatModel` 在传递 Base64 图片时使用错误的格式
   - 它期望图片以 URL 形式提供，但内部处理 Base64 data URL 时有 bug

2. **配置键名错误**
   - 原配置使用 `dash-scope`，但 LangChain4j 期望 `dashscope`

3. **图片格式问题**
   - DashScope API 要求图片使用特定格式：`data:image/jpeg;base64,<base64数据>`
   - LangChain4j 未能正确处理这种格式

## 解决方案

### 方案选择
放弃使用 LangChain4j 的 DashScope 集成，改为直接调用 DashScope REST API。

### 实施步骤

1. **删除 LangChain4j 相关代码**
   - 删除 `LangChain4jConfig.java`
   - 删除 `FoodCalorieAiService.java`

2. **重写 CalorieService**
   - 使用 `RestTemplate` 直接调用 DashScope API
   - 构建正确的请求体格式
   - 正确处理响应解析

3. **修复配置**
   - 将 `dash-scope` 改为 `dashscope`

### 关键代码变更

**请求体格式：**
```json
{
  "model": "qwen-vl-max",
  "input": {
    "messages": [{
      "role": "user",
      "content": [
        {"image": "data:image/jpeg;base64,<base64数据>"},
        {"text": "提示词"}
      ]
    }]
  },
  "parameters": {
    "result_format": "message"
  }
}
```

**API 端点：**
```
POST https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation
Authorization: Bearer <api-key>
Content-Type: application/json
```

## 验证结果

- [x] 服务启动正常
- [x] 图片上传成功
- [x] AI 服务调用成功
- [x] 返回正确的 JSON 格式响应

## 经验教训

1. **第三方库的限制**
   - LangChain4j 虽然提供了便利的抽象，但在特定场景下可能存在 bug
   - 当遇到底层 API 错误时，直接调用原生 API 是更可靠的选择

2. **调试技巧**
   - 查看详细的错误日志和堆栈跟踪
   - 对比官方 API 文档验证请求格式
   - 使用 curl/Postman 直接测试 API 以排除库的问题

3. **架构决策**
   - 对于关键功能，保持对底层 API 的直接访问能力
   - 抽象层应该易于替换，不应过度依赖特定库的实现细节

## 相关文件

- `src/main/java/com/example/heatcalculate/service/CalorieService.java`
- `src/main/resources/application.yml`
- ~~`src/main/java/com/example/heatcalculate/config/LangChain4jConfig.java`~~ (已删除)
- ~~`src/main/java/com/example/heatcalculate/ai/FoodCalorieAiService.java`~~ (已删除)

## 提交记录

```
commit 8da230d
fix: Resolve AI service unavailable error by replacing LangChain4j with direct DashScope API call
```

## 参考链接

- [DashScope 多模态 API 文档](https://help.aliyun.com/zh/dashscope/developer-reference/api-details)
- [LangChain4j DashScope 集成](https://docs.langchain4j.dev/integrations/language-models/dashscope/)
