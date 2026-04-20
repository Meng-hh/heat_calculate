## ADDED Requirements

### Requirement: 单步视觉识别与热量估算
系统 SHALL 通过单次调用通义千问-VL（qwen-vl-max）模型，识别图片中所有食物的种类、估算份量，并计算各食物及总热量区间。

#### Scenario: 成功识别并返回热量
- **WHEN** 合法食物图片上传后进入识别流程
- **THEN** 系统 SHALL 返回 HTTP 200，响应体包含食物列表，每项含名称、估算重量范围、热量区间（低/中/高），以及总热量区间

#### Scenario: 图片中无可识别食物
- **WHEN** 上传的图片不包含食物（如风景、人物）
- **THEN** 系统 SHALL 返回 HTTP 200，foods 列表为空，并附带说明信息

#### Scenario: 模型调用失败
- **WHEN** 通义千问 API 超时或返回错误
- **THEN** 系统 SHALL 返回 HTTP 502，错误信息提示模型服务暂时不可用

### Requirement: Prompt 内置餐具尺寸锚点
系统 SHALL 在 System Prompt 中固化标准中餐餐具尺寸参照，用于辅助模型估算食物份量。

#### Scenario: 图片含标准餐具
- **WHEN** 图片中出现饭碗、餐盘、筷子等标准餐具
- **THEN** 模型 SHALL 以内置尺寸参照（饭碗直径12cm、餐盘直径24cm、筷子长24cm等）作为份量估算依据

### Requirement: 结构化响应输出
系统 SHALL 返回固定结构的 JSON 响应，字段包括：`foods`（食物列表）、`totalCalories`（总热量区间）、`disclaimer`（估算免责说明）。

#### Scenario: 正常响应结构
- **WHEN** 识别成功
- **THEN** 响应 SHALL 符合以下结构：
  ```json
  {
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
