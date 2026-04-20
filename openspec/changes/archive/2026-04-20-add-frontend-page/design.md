## Context

后端已提供食物热量识别 API (`POST /api/v1/calories/analyze`)，接收图片文件返回热量估算结果。现在需要为健身爱好者用户群开发一个移动端友好的前端界面。

用户画像：健身爱好者，主要使用手机，需要快速查看食物热量，无需历史记录功能。

## Goals / Non-Goals

**Goals:**
- 提供移动端优先的响应式界面
- 实现简洁可爱的视觉风格
- 支持拍照和相册选择图片
- 清晰展示热量分析结果
- 打包后集成到 Spring Boot 静态资源

**Non-Goals:**
- 用户系统、登录、历史记录
- 多语言支持
- 离线使用
- 复杂的数据可视化图表

## Decisions

### 决策1：技术栈选择 React + Vite
选择 React 18 + Vite 而非 Vue 或其他框架。理由：React 生态成熟，Vite 构建快速，两者配合适合小型单页面应用。不选择 Next.js 是因为不需要 SSR，保持简单。

### 决策2：状态管理使用 useState
使用 React 内置的 useState 和 useReducer 管理三状态（上传/分析中/结果），不引入 Redux 或 Zustand。理由：应用状态简单，内置方案足够，减少依赖。

### 决策3：样式使用 CSS Modules
使用 CSS Modules 而非 Tailwind 或 styled-components。理由：样式量不大，CSS Modules 足够且无需额外依赖，构建产物更小。

### 决策4：构建产物放入 Spring Boot static
前端构建后输出到 `src/main/resources/static/`，由 Spring Boot 直接服务。理由：简化部署，无需单独前端服务器，适合小型项目。

### 决策5：图片压缩在前端处理
上传前在前端进行简单图片压缩（如果文件过大）。理由：减少上传时间和带宽，提升用户体验。

### 决策6：API 调用封装
创建独立的 `api.js` 模块封装后端接口调用。理由：统一错误处理、请求配置，便于维护。使用原生 `fetch` API，不引入 axios，减少依赖。

## API 接口设计

### 后端接口
```
POST /api/v1/calories/analyze
Content-Type: multipart/form-data

请求参数:
- image: File (required) - 图片文件 (JPG/PNG/WEBP, max 10MB)
- note: string (optional) - 备注信息

响应: 200 OK
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

错误响应:
- 400: 图片格式错误或大小超过限制
- 502: 模型服务不可用
- 500: 系统内部错误
```

### 前端 API 模块
```javascript
// src/api.js
export async function analyzeFood(image, note = '') {
  const formData = new FormData();
  formData.append('image', image);
  if (note) formData.append('note', note);
  
  const response = await fetch('/api/v1/calories/analyze', {
    method: 'POST',
    body: formData
  });
  
  if (!response.ok) {
    throw new Error(getErrorMessage(response.status));
  }
  
  return response.json();
}
```

## Risks / Trade-offs

- [浏览器兼容性] 部分旧版浏览器可能不支持较新的 CSS 特性 → 使用渐进增强，核心功能保证可用
- [图片大小] 用户可能上传超大图片导致上传慢 → 前端压缩 + 后端 10MB 限制双重保护
- [API 可用性] 依赖后端服务，网络不稳定时体验差 → 添加友好的错误提示和重试机制

## 架构图

```
┌─────────────────────────────────────────────┐
│              用户手机浏览器                   │
│  ┌─────────────────────────────────────┐    │
│  │           React 前端页面             │    │
│  │  ┌─────────┐  ┌─────────┐ ┌──────┐  │    │
│  │  │ 上传页  │→│ 分析中  │→│结果页│  │    │
│  │  └─────────┘  └─────────┘ └──────┘  │    │
│  └─────────────────────────────────────┘    │
│                    │ POST multipart/form-data│
└────────────────────┼────────────────────────┘
                     ▼
┌─────────────────────────────────────────────┐
│           Spring Boot 后端服务               │
│              (端口 8080)                     │
│  ┌─────────────────────────────────────┐    │
│  │    /api/v1/calories/analyze         │    │
│  └─────────────────────────────────────┘    │
└─────────────────────────────────────────────┘
```

## 项目结构

```
frontend/                          # 前端源码目录
├── index.html
├── package.json
├── vite.config.js
└── src/
    ├── main.jsx                   # 应用入口
    ├── App.jsx                    # 主组件，状态管理
    ├── api.js                     # API 接口封装
    ├── components/
    │   ├── UploadPage/            # 上传页面
    │   ├── LoadingPage/           # 分析中页面
    │   └── ResultPage/            # 结果展示页面
    └── styles/
        └── global.css             # 全局样式、CSS 变量
```

构建后输出到 `src/main/resources/static/`
