## Why

后端 API 已完成食物热量识别功能，但缺少用户友好的前端界面。为了让健身爱好者能够方便地使用手机拍照并查看热量分析结果，需要开发一个移动端优先的独立前端页面。

## What Changes

- 新增独立前端页面，使用 React + Vite 构建
- 实现三状态交互流程：上传 → 分析中 → 结果展示
- 移动端优先设计，支持相机直接调用
- 可爱简洁的视觉风格，圆角卡片、柔和配色
- 打包为静态资源，集成到 Spring Boot 的 static 目录

## Capabilities

### New Capabilities
- `frontend-upload`: 图片上传界面，支持拍照和相册选择，文件格式和大小校验
- `frontend-result-display`: 热量结果展示，包括食物列表、热量区间可视化、总热量汇总

### Modified Capabilities
- （无）

## Impact

- 新增前端技术栈：React 18、Vite、现代 CSS
- 构建产物放入 `src/main/resources/static/`，由 Spring Boot 提供服务
- 无需修改后端 API，前端通过 HTTP 调用现有 `/api/v1/calories/analyze` 接口
- 无数据库、无状态、无历史记录，保持系统简单
