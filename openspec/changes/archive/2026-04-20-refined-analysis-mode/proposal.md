# Proposal: 精细分析模式（多轮追问）

## 背景

当前应用只有单次识别的"粗略模式"。对于认真记录饮食的用户，热量区间过宽时缺乏进一步精确的手段。

## 目标

新增"精细模式"，允许模型在识别结果不确定时主动追问用户，通过 1~5 轮对话缩小热量估算区间。粗略模式保持不变。

**前端改动**：在上传页面增加模式切换开关（粗略/精细），精细模式下支持多轮对话交互。

## 非目标

- 不引入持久化存储（会话仅存内存）
- 不改造粗略模式的任何逻辑
- 不引入 RAG、向量数据库等额外基础设施

## 方案设计

### 双模式入口

```
粗略模式（现有）：POST /api/v1/calories/analyze
精细模式（新增）：POST /api/v1/calories/analyze/refined
继续精细分析：   POST /api/v1/calories/analyze/refined/{sessionId}/continue
```

### 会话约束

| 约束 | 值 |
|---|---|
| 存储方式 | JVM 内存（ConcurrentHashMap） |
| 会话过期时间 | 3 分钟（从创建时起） |
| 最大追问轮数 | 5 轮 |
| 过期后行为 | 返回 404，提示用户重新上传 |
| 达到上限后行为 | 返回当前最佳估算结果 |

### 追问触发规则

模型输出热量区间后，由应用层判断是否追问：

- 总热量区间（high - low）> 200kcal → 触发追问
- 达到 5 轮上限 → 强制返回当前最佳估算

### 核心数据结构

```java
class AnalysisSession {
    String sessionId;
    SessionStatus status;       // WAITING_INPUT | COMPLETE | EXPIRED
    ChatMemory chatMemory;      // LangChain4j MessageWindowChatMemory
    String currentQuestion;     // 当前追问内容
    int roundCount;             // 已追问次数
    Instant createdAt;          // 用于过期判断
    CalorieResult lastResult;   // 每轮最新估算（兜底用）
}
```

### API 响应格式

```json
// 需要追问时
{
  "sessionId": "uuid",
  "status": "need_input",
  "question": "这碗面是素面还是加了肉？",
  "partialResult": { ... }
}

// 完成时
{
  "sessionId": "uuid",
  "status": "complete",
  "result": { "foods": [...], "totalCalories": {...}, "disclaimer": "..." }
}
```

### LangChain4j 使用方式

使用 `MessageWindowChatMemory` 维护每个会话的对话历史，每轮追问时将用户回答追加到 memory，模型能看到完整上下文后重新推理。

## 影响范围

**后端新增：**
- `AnalysisSession.java`、`SessionStore.java`、`RefinedAnalysisService.java`
- `RefinedAnalysisController.java`（新端点）
- 不改动：`CalorieService.java`、`CalorieController.java`

**前端改动：**
- `UploadPage`：增加粗略/精细模式切换开关
- `App.jsx`：新增精细模式的多轮对话状态（`QUESTIONING`）和流程
- `api.js`：新增精细模式的两个 API 调用函数
- 新增 `QuestionPage` 组件：展示追问内容 + 文字输入框

## 技术决策

### 会话清理策略

采用**懒清理**：访问会话时判断是否过期，过期则返回 404。不引入定时任务，避免额外的线程开销。
