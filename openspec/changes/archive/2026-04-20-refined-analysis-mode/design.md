## Context

在现有的粗略模式（单次识别）基础上，新增精细模式。精细模式通过多轮对话让模型追问用户，缩小热量估算区间。

前端已有三状态结构（upload / loading / result），需要扩展为支持精细模式的多轮对话流程。后端新增会话管理和多轮推理服务，现有粗略模式代码完全不动。

## Goals / Non-Goals

**Goals:**
- 前端 UploadPage 增加模式切换，用户可选择粗略或精细
- 精细模式下支持多轮追问交互（文字输入回答）
- 后端会话内存存储，懒清理过期会话
- 粗略模式代码零改动

**Non-Goals:**
- 持久化会话存储
- 追问快捷选项按钮（只支持打字）
- 改动粗略模式任何逻辑

## Decisions

### 决策1：前端新增 QUESTIONING 状态
App.jsx 的状态机从 3 个状态扩展为 4 个：

```
UPLOAD → LOADING → RESULT（粗略模式，不变）

UPLOAD → LOADING → QUESTIONING → LOADING → QUESTIONING → ... → RESULT（精细模式）
```

新增 `QuestionPage` 组件处理追问交互，复用现有 `LoadingPage`。

### 决策2：精细模式会话 ID 存前端
`sessionId` 由后端首次返回，前端存在 React state 中，后续 continue 请求携带。不存 localStorage，刷新即丢失（符合 3 分钟内存会话的定位）。

### 决策3：后端 Bean 类型用 ChatMemory per session
每个 `AnalysisSession` 持有独立的 `MessageWindowChatMemory`（最多 20 条消息），不共享。会话过期后 GC 自动回收。

### 决策4：追问由模型生成，应用层只判断是否追问
应用层只做一件事：判断 `totalCalories.high - totalCalories.low > 200`。追问的具体问题由模型在 prompt 中自行生成，不在应用层硬编码问题模板。

### 决策5：懒清理
`SessionStore.get(sessionId)` 时检查 `createdAt + 3分钟 < now`，过期则移除并返回 empty。不引入定时任务。

## API 接口设计

### 新增后端接口

```
// 开始精细分析
POST /api/v1/calories/analyze/refined
Content-Type: multipart/form-data
参数: image (File), note (String, optional)

响应 - 需要追问:
{
  "sessionId": "uuid",
  "status": "need_input",
  "question": "这碗面是素面还是加了肉？",
  "partialResult": { "foods": [...], "totalCalories": {...} }
}

响应 - 直接完成（首轮区间已足够窄）:
{
  "sessionId": "uuid",
  "status": "complete",
  "result": { "foods": [...], "totalCalories": {...}, "disclaimer": "..." }
}

// 继续精细分析
POST /api/v1/calories/analyze/refined/{sessionId}/continue
Content-Type: application/json
{ "answer": "是素面" }

响应格式同上，status 可能是 need_input 或 complete
错误: 404（会话不存在或已过期，提示重新上传）
```

### 前端新增 API 函数

```javascript
// api.js 新增
export async function startRefinedAnalysis(image, note = '') { ... }
export async function continueRefinedAnalysis(sessionId, answer) { ... }
```

## 架构图

```
前端状态机（精细模式）

  UploadPage
  [模式开关: 粗略●  精细○]
       │ 选择精细模式后上传
       ▼
  LoadingPage（复用）
       │
       ├─ status=complete ──────────────────▶ ResultPage
       │
       └─ status=need_input
              │
              ▼
         QuestionPage
         ┌─────────────────────────────┐
         │  "这碗面是素面还是加了肉？"   │
         │  ┌─────────────────────┐    │
         │  │ 请输入你的回答...    │    │
         │  └─────────────────────┘    │
         │  [确认]                      │
         └─────────────────────────────┘
              │ 提交回答
              ▼
         LoadingPage（复用）
              │
              └─ 循环，最多 5 轮
```

```
后端组件关系

RefinedAnalysisController
       │
       ▼
RefinedAnalysisService
  ├── SessionStore（ConcurrentHashMap<String, AnalysisSession>）
  │       └── 懒清理：get 时判断过期
  └── ChatLanguageModel（复用现有 Bean）
          └── 每个 Session 持有独立 MessageWindowChatMemory
```

## 项目结构变更

```
后端新增:
src/main/java/com/example/heatcalculate/
├── controller/
│   └── RefinedAnalysisController.java   # 新增
├── service/
│   ├── RefinedAnalysisService.java      # 新增
│   └── SessionStore.java                # 新增
└── model/
    ├── AnalysisSession.java             # 新增
    └── RefinedAnalysisResponse.java     # 新增（API 响应 DTO）

前端新增/改动:
frontend/src/
├── App.jsx                              # 改动：新增 QUESTIONING 状态
├── api.js                               # 改动：新增两个函数
└── components/
    ├── UploadPage/index.jsx             # 改动：增加模式切换开关
    └── QuestionPage/                    # 新增
        ├── index.jsx
        └── QuestionPage.module.css
```

## Risks / Trade-offs

- [内存泄漏] 用户开始精细分析后不回答，会话永久驻留内存 → 懒清理在下次访问时回收，低频场景可接受
- [并发] 多用户同时使用精细模式 → ConcurrentHashMap 保证线程安全
- [模型追问质量] 模型生成的问题可能不够精准 → prompt 中明确要求"针对热量影响最大的不确定因素提问"
