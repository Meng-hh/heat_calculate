## MODIFIED Requirements

### Requirement: 结构化响应输出
系统 SHALL 返回固定结构的 JSON 响应，字段包括：`sessionId`（会话标识）、`foods`（食物列表）、`totalCalories`（总热量区间）、`disclaimer`（估算免责说明）。

#### Scenario: 正常响应结构
- **WHEN** 识别成功
- **THEN** 响应 SHALL 符合以下结构：
  ```json
  {
    "sessionId": "uuid",
    "foods": [
      {
        "name": "红烧肉",
        "estimatedWeight": "80-120g",
        "calories": { "low": 280, "mid": 380, "high": 480 }
      }
    ],
    "totalCalories": { "low": 280, "mid": 380, "high": 480 },
    "disclaimer": "热量数据为估算值，实际值因食材和烹饪方式而异"
  }
  ```

#### Scenario: 模型输出解析失败
- **WHEN** 模型返回无法解析为预期结构的内容
- **THEN** 系统 SHALL 返回 HTTP 500，并记录原始模型输出到日志
