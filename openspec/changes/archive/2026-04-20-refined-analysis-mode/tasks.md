## 1. 后端数据模型

- [x] 1.1 创建 `AnalysisSession.java`，包含 sessionId、status、chatMemory、currentQuestion、roundCount、createdAt、lastResult
- [x] 1.2 创建 `RefinedAnalysisResponse.java`，包含 sessionId、status、question、partialResult、result
- [x] 1.3 定义 `SessionStatus` 枚举：WAITING_INPUT、COMPLETE、EXPIRED

## 2. 后端会话存储

- [x] 2.1 创建 `SessionStore.java`，使用 ConcurrentHashMap 存储会话
- [x] 2.2 实现 `put(sessionId, session)` 方法
- [x] 2.3 实现 `get(sessionId)` 方法，包含懒清理逻辑（检查 createdAt + 3分钟）
- [x] 2.4 实现 `remove(sessionId)` 方法

## 3. 后端精细分析服务

- [x] 3.1 创建 `RefinedAnalysisService.java`，注入 ChatLanguageModel、ImageValidatorService、SessionStore
- [x] 3.2 实现 `startRefinedAnalysis(image, note)` 方法
  - [x] 3.2.1 图片校验和 Base64 编码（复用 CalorieService 逻辑）
  - [x] 3.2.2 创建 MessageWindowChatMemory（maxMessages=20）
  - [x] 3.2.3 构建首轮 UserMessage（图片 + 文本）
  - [x] 3.2.4 调用模型获取首轮识别结果
  - [x] 3.2.5 判断是否需要追问（totalCalories.high - low > 200）
  - [x] 3.2.6 如需追问：生成 sessionId，创建 AnalysisSession，存入 SessionStore，返回 need_input
  - [x] 3.2.7 如不需追问：直接返回 complete 状态
- [x] 3.3 实现 `continueRefinedAnalysis(sessionId, answer)` 方法
  - [x] 3.3.1 从 SessionStore 获取会话，处理过期/不存在情况
  - [x] 3.3.2 检查 roundCount 是否达到 5，达到则返回 lastResult
  - [x] 3.3.3 将用户回答追加到 chatMemory
  - [x] 3.3.4 调用模型重新推理
  - [x] 3.3.5 更新 lastResult 和 roundCount
  - [x] 3.3.6 判断是否继续追问或返回最终结果
- [x] 3.4 实现 `shouldAskClarification(result)` 私有方法（判断热量区间宽度）
- [x] 3.5 实现 `parseResponse(aiResponse)` 私有方法（复用 CalorieService 的 JSON 解析逻辑）

## 4. 后端 Controller

- [x] 4.1 创建 `RefinedAnalysisController.java`，注入 RefinedAnalysisService
- [x] 4.2 实现 `POST /api/v1/calories/analyze/refined` 端点
  - [x] 4.2.1 接收 MultipartFile image 和 String note
  - [x] 4.2.2 调用 service.startRefinedAnalysis
  - [x] 4.2.3 返回 RefinedAnalysisResponse
- [x] 4.3 实现 `POST /api/v1/calories/analyze/refined/{sessionId}/continue` 端点
  - [x] 4.3.1 接收 sessionId 和 request body { answer }
  - [x] 4.3.2 调用 service.continueRefinedAnalysis
  - [x] 4.3.3 处理 404 异常（会话不存在或过期）
  - [x] 4.3.4 返回 RefinedAnalysisResponse
- [x] 4.4 添加全局异常处理（SessionExpiredException → 404）

## 5. 后端单元测试

- [x] 5.1 测试 SessionStore 的懒清理逻辑（过期会话被正确移除）
- [x] 5.2 测试 RefinedAnalysisService.shouldAskClarification（区间判断）
- [x] 5.3 测试 roundCount 达到 5 时强制返回结果
- [x] 5.4 Mock ChatLanguageModel，测试多轮对话流程

## 6. 前端 API 封装

- [x] 6.1 在 `api.js` 中新增 `startRefinedAnalysis(image, note)` 函数
  - [x] 6.1.1 构建 FormData，POST 到 /api/v1/calories/analyze/refined
  - [x] 6.1.2 处理响应，返回 { sessionId, status, question?, result? }
- [x] 6.2 在 `api.js` 中新增 `continueRefinedAnalysis(sessionId, answer)` 函数
  - [x] 6.2.1 POST JSON body 到 /api/v1/calories/analyze/refined/{sessionId}/continue
  - [x] 6.2.2 处理 404 错误（会话过期），返回友好提示
  - [x] 6.2.3 返回响应数据

## 7. 前端 QuestionPage 组件

- [x] 7.1 创建 `components/QuestionPage/index.jsx`
- [x] 7.2 接收 props: question、onSubmit、partialResult（可选展示）
- [x] 7.3 实现文字输入框（textarea）
- [x] 7.4 实现"确认"按钮，点击后调用 onSubmit(answer)
- [x] 7.5 添加输入验证（不能为空）
- [x] 7.6 创建 `QuestionPage.module.css`，样式与现有页面保持一致

## 8. 前端 UploadPage 改动

- [x] 8.1 在 UploadPage 顶部增加模式切换开关（粗略/精细）
- [x] 8.2 使用 useState 管理当前选择的模式
- [x] 8.3 将模式传递给 onUpload 回调：`onUpload(file, note, mode)`
- [x] 8.4 样式调整：开关使用 toggle 或 radio button

## 9. 前端 App.jsx 状态扩展

- [x] 9.1 新增 QUESTIONING 状态到 STATES 常量
- [x] 9.2 新增 state: sessionId、currentQuestion、mode
- [x] 9.3 修改 handleUpload 函数
  - [x] 9.3.1 接收 mode 参数
  - [x] 9.3.2 如果是粗略模式，调用现有 analyzeFood（不变）
  - [x] 9.3.3 如果是精细模式，调用 startRefinedAnalysis
  - [x] 9.3.4 根据响应 status 切换到 RESULT 或 QUESTIONING
- [x] 9.4 新增 handleAnswerSubmit 函数
  - [x] 9.4.1 调用 continueRefinedAnalysis(sessionId, answer)
  - [x] 9.4.2 切换到 LOADING 状态
  - [x] 9.4.3 根据响应 status 切换到 RESULT 或 QUESTIONING
  - [x] 9.4.4 处理 404 错误（会话过期），切回 UPLOAD 并显示错误
- [x] 9.5 在 renderContent 中增加 QUESTIONING 分支，渲染 QuestionPage

## 10. 前端集成测试

- [x] 10.1 测试粗略模式流程（确保未受影响）
- [x] 10.2 测试精细模式首轮直接完成（区间窄）
- [x] 10.3 测试精细模式多轮追问流程（2~3 轮）
- [x] 10.4 测试会话过期场景（等待 3 分钟后 continue）
- [x] 10.5 测试达到 5 轮上限后返回结果

## 11. 端到端验证

- [x] 11.1 启动后端服务，验证两个新端点可访问
- [x] 11.2 前端构建并集成到 Spring Boot static
- [x] 11.3 使用真实图片测试完整精细模式流程
- [x] 11.4 验证粗略模式未受影响
- [x] 11.5 验证会话过期和上限逻辑
