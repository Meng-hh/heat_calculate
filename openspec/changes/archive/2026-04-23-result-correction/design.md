## Context

现有的粗略模式是无状态的单次调用，精细模式（`refined-analysis-mode` change）引入了 `SessionStore` 和 `AnalysisSession`，但会话在返回最终结果后即完成使命。

本 change 在此基础上延伸：结果返回后会话不销毁，用户可在结果页面纠正识别内容并触发重新计算。粗略模式也需要补充会话支持，以便纠正时有上下文可用。

## Goals / Non-Goals

**Goals:**
- 粗略模式首次识别也创建会话并返回 sessionId
- ResultPage 支持编辑每项食物的名称和重量
- ResultPage 新增补充备注输入框
- 纠正信息追加到 chatMemory，模型重新推理
- 两种模式共用同一个纠正端点

**Non-Goals:**
- 纠正历史记录（只保留最新一次结果）
- 纠正后再次追问（纠正是一次性操作，不进入精细追问循环）
- 持久化会话

## Decisions

### 决策1：粗略模式也创建会话
粗略模式的 `CalorieService.analyzeFood` 改为也创建 `AnalysisSession`，将图片识别的对话历史存入 chatMemory，返回结果时附带 sessionId。这样纠正端点可以统一处理两种模式，无需区分来源。

### 决策2：会话在结果返回后继续存活
`AnalysisSession` 的 3 分钟过期从**创建时**计算，结果返回后不重置计时器。用户有 3 分钟窗口可以纠正，超时后纠正请求返回 404，提示重新上传。这与精细模式的过期策略保持一致，不引入新的状态。

### 决策3：纠正是追加消息，不是替换
纠正时将用户修改构建为一条新的 UserMessage 追加到 chatMemory：
```
"用户纠正：第1项食物名称改为「梅菜扣肉」，重量改为300g。
 补充说明：少油少盐。请基于以上修正重新计算所有食物的热量。"
```
模型能看到完整对话历史（包括原始图片识别过程），重新推理更准确。

### 决策4：纠正端点独立，不复用精细模式的 continue 端点
`/sessions/{sessionId}/correct` 与 `/analyze/refined/{sessionId}/continue` 语义不同：前者是用户主动纠正已有结果，后者是回答模型的追问。分开可以避免混淆，也方便后续独立演进。

### 决策5：前端编辑状态内联，不弹窗
ResultPage 中每行食物直接变为可编辑的 input，不弹出 modal。减少交互层级，移动端体验更好。

## API 设计

```
POST /api/v1/calories/analyze/sessions/{sessionId}/correct
Content-Type: application/json
{
  "corrections": [
    { "index": 0, "name": "梅菜扣肉", "weight": "300g" }
  ],
  "additionalNote": "少油少盐"
}

响应 200:
{ "foods": [...], "totalCalories": {...}, "disclaimer": "..." }

响应 404: 会话不存在或已过期
```

粗略模式响应格式变更（新增 sessionId）：
```
POST /api/v1/calories/analyze
响应: { "sessionId": "uuid", "foods": [...], "totalCalories": {...}, "disclaimer": "..." }
```

## 前端状态机变更

```
现有：UPLOAD → LOADING → RESULT

新增：RESULT 内部新增编辑子状态
  RESULT[viewing] → RESULT[editing] → LOADING(correction) → RESULT[viewing]
```

ResultPage 接收新 props：`sessionId`、`onCorrect(sessionId, corrections, note)`

## Risks / Trade-offs

- [会话过期] 用户看完结果后超过 3 分钟才纠正 → 返回 404，提示重新上传，可接受
- [粗略模式改动] CalorieService 需要注入 SessionStore，增加了依赖 → 影响范围小，仅改 service 层
- [并发纠正] 同一 sessionId 被多次纠正 → chatMemory 追加多条消息，模型能处理，不需要加锁
