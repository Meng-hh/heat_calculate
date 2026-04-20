## Why

模型识别结果不可能 100% 准确，用户需要一个途径在结果页面直接纠正食物名称、重量，并补充备注，让模型基于修正信息重新计算热量。两种模式（粗略/精细）均需支持，纠正时复用已有会话上下文。

## What Changes

- 粗略模式和精细模式的首次识别均返回 `sessionId`，会话在结果返回后继续保持有效
- 结果页面新增"编辑"入口，允许用户修改每项食物的名称和重量
- 结果页面新增补充备注输入框
- 新增"重新计算"操作，将用户修正信息追加到会话 chatMemory，调用模型重新推理
- 新增后端端点 `POST /api/v1/calories/analyze/sessions/{sessionId}/correct`

## Capabilities

### New Capabilities

- `result-correction`：用户在结果页面纠正食物识别内容（名称、重量）并补充备注，触发模型重新计算热量

### Modified Capabilities

- `food-calorie-recognition`：识别结果现在附带 sessionId，支持后续纠正操作

## Impact

- 后端：新增 `CorrectionController`、`CorrectionService`；`CalorieService` 和 `RefinedAnalysisService` 的响应均需携带 `sessionId`；`SessionStore` 需在结果返回后继续保持会话（不立即销毁）
- 前端：`ResultPage` 改造为可编辑状态；`api.js` 新增 `correctAnalysis` 函数；`App.jsx` 新增纠正流程状态
- 依赖：依赖 `refined-analysis-mode` change 中的 `SessionStore` 和 `AnalysisSession` 基础设施
