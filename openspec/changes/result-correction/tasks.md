## 1. 后端：粗略模式支持会话

- [x] 1.1 修改 `CalorieService`，注入 `SessionStore`（依赖 `refined-analysis-mode` change 中的 SessionStore）
- [x] 1.2 修改 `analyzeFood` 方法：识别完成后创建 `AnalysisSession`，将对话历史存入 chatMemory，返回结果时附带 sessionId
- [x] 1.3 修改 `CalorieResult` 模型类，新增 `sessionId` 字段
- [x] 1.4 修改 `CalorieController`，响应中透传 sessionId

## 2. 后端：纠正服务

- [x] 2.1 创建 `CorrectionRequest.java`，包含 `corrections`（List，每项含 index、name、weight）和 `additionalNote`
- [x] 2.2 创建 `CorrectionService.java`，注入 `SessionStore` 和 `ChatLanguageModel`
- [x] 2.3 实现 `correct(sessionId, corrections, additionalNote)` 方法
  - [x] 2.3.1 从 SessionStore 获取会话，处理不存在/过期情况（抛 SessionExpiredException）
  - [x] 2.3.2 构建纠正 UserMessage（包含修改项和备注）
  - [x] 2.3.3 追加到 chatMemory，调用模型重新推理
  - [x] 2.3.4 解析响应，更新 lastResult，返回新的 CalorieResult（含 sessionId）
- [x] 2.4 创建 `CorrectionController.java`，实现 `POST /api/v1/calories/analyze/sessions/{sessionId}/correct`
- [x] 2.5 确认全局异常处理已覆盖 `SessionExpiredException → 404`

## 3. 后端单元测试

- [x] 3.1 测试 `CorrectionService.correct`：正常纠正流程，mock ChatLanguageModel
- [x] 3.2 测试会话过期时抛出 SessionExpiredException
- [x] 3.3 测试纠正消息正确追加到 chatMemory（验证消息内容包含修改项和备注）
- [x] 3.4 测试粗略模式响应包含 sessionId

## 4. 前端 API 封装

- [x] 4.1 在 `api.js` 新增 `correctAnalysis(sessionId, corrections, additionalNote)` 函数
- [x] 4.2 处理 404 响应（会话过期），返回友好错误信息

## 5. 前端 ResultPage 改造

- [x] 5.1 ResultPage 新增 props：`sessionId`、`onCorrect(sessionId, corrections, note)`
- [x] 5.2 每行食物新增"编辑"按钮，点击后该行切换为内联编辑状态（name input + weight input）
- [x] 5.3 编辑状态下显示"确认"和"取消"按钮，确认后记录修改，取消还原
- [x] 5.4 ResultPage 底部新增补充备注 textarea（可选填）
- [x] 5.5 新增"重新计算"按钮，收集所有已修改项和备注，调用 `onCorrect`
- [x] 5.6 "重新计算"按钮在无任何修改且备注为空时禁用
- [x] 5.7 更新 `ResultPage.module.css`，添加编辑状态样式

## 6. 前端 App.jsx 改造

- [x] 6.1 新增 state：`sessionId`
- [x] 6.2 粗略模式的 `handleUpload` 从响应中提取并存储 sessionId
- [x] 6.3 精细模式的 `handleUpload` 和 `handleAnswerSubmit` 同样存储 sessionId
- [x] 6.4 新增 `handleCorrect(sessionId, corrections, note)` 函数
  - [x] 6.4.1 切换到 LOADING 状态
  - [x] 6.4.2 调用 `correctAnalysis`
  - [x] 6.4.3 成功后更新 result 和 sessionId，切回 RESULT 状态
  - [x] 6.4.4 404 错误时切回 UPLOAD 并显示"会话已过期，请重新上传"
- [x] 6.5 将 `sessionId` 和 `onCorrect` 传入 ResultPage

## 7. 端到端验证

- [x] 7.1 粗略模式：识别后响应包含 sessionId
- [x] 7.2 粗略模式：修改食物名称后重新计算，结果正确更新
- [x] 7.3 精细模式：多轮追问完成后，仍可纠正结果
- [x] 7.4 纠正后再次纠正（多次纠正同一会话）
- [x] 7.5 会话过期后纠正，前端显示正确错误提示
