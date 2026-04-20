## Context

基于 Spring Boot 3.x + LangChain4j 构建的食物热量识别服务。用户上传食物图片，系统调用通义千问-VL 视觉模型，通过单步 Prompt 返回结构化热量估算结果。无数据库、无缓存，纯无状态服务。

## Goals / Non-Goals

**Goals:**
- 提供单一 REST 接口，接收图片返回热量估算
- 通过 Prompt 内置餐具尺寸锚点提升份量估算准确率
- 返回热量区间（低/中/高）而非单一数值，体现估算的不确定性
- 系统保持最小依赖，快速可部署

**Non-Goals:**
- 用户系统、登录、历史记录
- 食物营养数据库维护
- 多步 Chain 或 RAG 增强
- 移动端 SDK

## Decisions

### 决策1：单步 Prompt vs 多步 Chain
选择单步 Prompt。理由：减少 API 调用次数（成本与延迟减半），通义千问-VL 视觉能力足以在单次调用中完成识别+估算。代价是 Prompt 复杂度略高，可接受。

### 决策2：Prompt 内置餐具锚点
将标准餐具尺寸固化在 System Prompt 中，而非要求用户输入。理由：中餐场景餐具尺寸相对标准，固定锚点比无锚点估算准确，且不增加用户操作负担。
```
标准参照：
- 饭碗：直径12cm，盛满约200g米饭
- 餐盘：直径24cm
- 汤碗：直径16cm，容量约500ml
- 筷子：长约24cm（图片中天然比例尺）
```

### 决策3：结构化输出
使用 LangChain4j `@AiServices` + 返回 Java 对象，让框架处理 JSON 解析，避免手动解析模型输出。

### 决策4：无持久化层
初版不引入数据库和 Redis。理由：无用户系统，无需存储任何状态，引入会增加部署复杂度而无收益。

### 决策5：图片传输方式
图片以 Base64 编码通过 multipart/form-data 上传，在内存中处理后直接传给模型，不落盘。

## 架构图

```
Client
  │  POST /api/v1/calories/analyze
  │  (multipart: image + optional note)
  ▼
CalorieController
  │
  ▼
ImageValidatorService  ──→ 校验格式(JPG/PNG/WEBP)、大小(≤10MB)
  │
  ▼
CalorieService
  │
  ▼
FoodCalorieAiService (LangChain4j @AiServices)
  │  System Prompt: 餐具锚点 + 输出格式规范
  │  User Input: 图片(Base64) + 备注
  ▼
通义千问-VL API (qwen-vl-max)
  │
  ▼
CalorieResult (结构化 JSON 响应)
```

## 项目结构

```
src/main/java/com/example/heatcalculate/
├── controller/
│   └── CalorieController.java
├── service/
│   ├── CalorieService.java
│   └── ImageValidatorService.java
├── ai/
│   └── FoodCalorieAiService.java
├── model/
│   ├── AnalysisRequest.java
│   ├── FoodItem.java
│   └── CalorieResult.java
└── config/
    └── LangChain4jConfig.java
```

## Risks / Trade-offs

- [模型估算误差] 份量估算依赖图片质量和角度，误差可达 ±30% → 缓解：返回区间而非精确值，并在响应中附加免责说明
- [API 可用性] 依赖通义千问外部服务 → 缓解：接口层统一处理超时和错误，返回友好错误信息
- [图片大小] 大图片 Base64 编码后体积增大约 33%，影响请求延迟 → 缓解：限制上传大小 ≤10MB，服务端压缩后再传模型
- [Prompt 漂移] 模型更新可能导致输出格式变化 → 缓解：LangChain4j 结构化输出有解析兜底，字段缺失时返回默认值
