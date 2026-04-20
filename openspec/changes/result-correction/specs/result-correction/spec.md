## ADDED Requirements

### Requirement: 用户可纠正食物识别结果
系统 SHALL 允许用户在结果页面修改任意食物的名称和重量，并触发模型基于修正信息重新计算热量。

#### Scenario: 用户修改食物名称和重量后重新计算
- **WHEN** 用户修改一项或多项食物的名称或重量，点击"重新计算"
- **THEN** 系统 SHALL 将修正信息追加到会话上下文，调用模型重新推理，返回更新后的热量结果

#### Scenario: 用户仅补充备注后重新计算
- **WHEN** 用户不修改食物列表，仅在备注框输入补充说明，点击"重新计算"
- **THEN** 系统 SHALL 将备注追加到会话上下文，调用模型重新推理，返回更新后的热量结果

#### Scenario: 会话已过期时尝试纠正
- **WHEN** 用户在结果返回 3 分钟后点击"重新计算"
- **THEN** 系统 SHALL 返回 HTTP 404，前端提示用户重新上传图片

### Requirement: 纠正操作复用会话上下文
系统 SHALL 在纠正时将修正信息追加到原有 chatMemory，使模型能看到完整对话历史（含原始图片识别过程）进行重新推理。

#### Scenario: 纠正消息追加到 chatMemory
- **WHEN** 纠正请求到达后端
- **THEN** 系统 SHALL 构建包含修正内容和补充备注的 UserMessage，追加到对应会话的 chatMemory，再调用模型

### Requirement: 粗略模式识别结果附带 sessionId
系统 SHALL 在粗略模式的识别响应中包含 sessionId，以支持后续纠正操作。

#### Scenario: 粗略模式返回 sessionId
- **WHEN** 粗略模式识别成功
- **THEN** 响应 SHALL 包含 sessionId 字段，前端存储该值用于后续纠正请求
