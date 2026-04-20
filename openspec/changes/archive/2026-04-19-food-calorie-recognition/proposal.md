## Why

用户希望通过拍摄食物图片，快速获取图片中食物的热量信息，辅助日常饮食管理。借助大模型视觉能力，无需维护食物数据库即可实现智能识别与热量估算。

## What Changes

- 新增图片上传接口，接收食物图片及可选备注
- 集成通义千问-VL 视觉大模型，通过单步 Prompt 识别食物并估算热量
- Prompt 内置标准餐具尺寸作为份量估算锚点，提升准确率
- 返回结构化结果：食物列表、各项热量区间、总热量区间
- 无持久化层、无缓存层，保持系统最小化

## Capabilities

### New Capabilities

- `image-upload`: 接收用户上传的食物图片（支持 JPG/PNG/WEBP），进行格式与大小校验
- `food-calorie-recognition`: 调用通义千问-VL，单步识别图片中食物种类、份量并估算热量区间

### Modified Capabilities

（无）

## Impact

- 新增依赖：LangChain4j（通义千问集成）、Spring Boot Web、SpringDoc OpenAPI
- 对外暴露 REST 接口：`POST /api/v1/calories/analyze`
- 无数据库、无 Redis，无外部存储依赖
- 需配置通义千问 API Key（环境变量注入）
