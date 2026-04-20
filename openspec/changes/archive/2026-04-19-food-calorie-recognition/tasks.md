## 1. 项目初始化

- [x] 1.1 使用 Spring Initializr 创建 Maven 项目，Java 17，GroupId: `com.example`，ArtifactId: `heat-calculate`
- [x] 1.2 在 `pom.xml` 中添加依赖：`spring-boot-starter-web`、`langchain4j-dashscope`（通义千问）、`springdoc-openapi-starter-webmvc-ui`
- [x] 1.3 配置 `application.yml`：服务端口、通义千问 API Key（从环境变量读取）、文件上传大小限制（10MB）

## 2. 数据模型

- [x] 2.1 创建 `FoodItem.java`：字段 `name`、`estimatedWeight`、`calories`（含 low/mid/high）
- [x] 2.2 创建 `CalorieResult.java`：字段 `foods`（List\<FoodItem\>）、`totalCalories`（含 low/mid/high）、`disclaimer`
- [x] 2.3 创建 `AnalysisRequest.java`：字段 `image`（MultipartFile）、`note`（String，可选）

## 3. 图片校验

- [x] 3.1 创建 `ImageValidatorService.java`，校验文件格式（JPG/PNG/WEBP）和大小（≤10MB）
- [x] 3.2 格式不合法时抛出自定义异常，大小超限时抛出自定义异常
- [x] 3.3 创建全局异常处理器 `GlobalExceptionHandler.java`，将校验异常映射为 HTTP 400 响应

## 4. AI 服务集成

- [x] 4.1 创建 `LangChain4jConfig.java`，配置通义千问-VL 模型（qwen-vl-max）Bean
- [x] 4.2 创建 `FoodCalorieAiService.java`（LangChain4j `@AiServices` 接口），定义 System Prompt（含餐具锚点）和方法签名
- [x] 4.3 System Prompt 内容：内置标准餐具尺寸（饭碗12cm/餐盘24cm/汤碗16cm/筷子24cm）、输出格式规范（JSON）、估算规则

## 5. 业务编排

- [x] 5.1 创建 `CalorieService.java`，编排：图片校验 → Base64 编码 → 调用 AI 服务 → 返回结果
- [x] 5.2 处理模型调用异常（超时/API 错误），包装为 HTTP 502 响应
- [x] 5.3 处理模型输出解析失败，记录原始输出到日志，返回 HTTP 500

## 6. REST 接口

- [x] 6.1 创建 `CalorieController.java`，暴露 `POST /api/v1/calories/analyze` 接口
- [x] 6.2 添加 SpringDoc 注解，完善接口文档（请求参数、响应结构、错误码说明）

## 7. 测试与验证

- [x] 7.1 编写 `ImageValidatorServiceTest.java`，覆盖格式校验和大小校验场景
- [x] 7.2 使用真实图片手动测试接口，验证热量识别结果合理性
- [x] 7.3 验证 Swagger UI 可访问（`/swagger-ui.html`）
