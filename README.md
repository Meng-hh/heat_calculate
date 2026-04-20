# 食物卡路里识别服务

一个基于 Spring Boot + React 的智能食物卡路里识别应用，通过上传食物图片，AI 自动分析并返回食物名称和卡路里估算。

## 功能特性

- **智能识别**：集成通义千问 AI，自动识别食物图片
- **卡路里估算**：提供每份食物的卡路里范围和详细数据
- **移动端优先**：响应式设计，适配手机单手操作
- **图片压缩**：前端自动压缩图片，优化上传速度
- **简洁可爱风**：橙绿粉配色，适合健身爱好者

## 技术栈

### 后端
- Java 24
- Spring Boot 3.x
- LangChain4j (AI 集成)
- Tongyi Qianwen (通义千问)
- Maven

### 前端
- React 18
- Vite
- CSS Modules
- Mobile-first 设计

## 快速开始

### 环境要求
- JDK 24+
- Node.js 18+
- Maven 3.8+

### 1. 克隆项目

```bash
git clone https://github.com/Meng-hh/heat_calculate.git
cd heat_calculate
```

### 2. 配置 AI 密钥

编辑 `src/main/resources/application.yml`：

```yaml
langchain4j:
  dashscope:
    api-key: "你的通义千问API密钥"
```

### 3. 启动后端

```bash
mvn spring-boot:run
```

后端服务将在 http://localhost:8080 启动

### 4. 启动前端（开发模式）

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器将在 http://localhost:3000 启动

### 5. 构建生产版本

```bash
cd frontend
npm run build
```

构建后的文件将自动输出到 `src/main/resources/static/`，Spring Boot 会直接托管。

## API 接口

### 分析食物图片

```http
POST /api/v1/calories/analyze
Content-Type: multipart/form-data
```

**参数：**
- `image` (File, 必填): 食物图片，支持 JPG、PNG、WEBP，最大 10MB
- `note` (String, 可选): 补充说明，如"约200克"

**响应示例：**

```json
{
  "foods": [
    {
      "name": "白米饭",
      "caloriesPerServing": 200,
      "servingSize": "1碗(约150g)",
      "confidence": 0.95
    }
  ],
  "totalCalories": {
    "min": 180,
    "max": 220,
    "estimated": 200
  },
  "note": "估算基于标准份量"
}
```

## 项目结构

```
heat_calculate/
├── frontend/                 # React 前端
│   ├── src/
│   │   ├── components/      # 页面组件
│   │   │   ├── UploadPage/  # 上传页面
│   │   │   ├── LoadingPage/ # 加载页面
│   │   │   └── ResultPage/  # 结果页面
│   │   ├── api.js           # API 封装
│   │   └── App.jsx          # 主应用
│   └── vite.config.js       # Vite 配置
├── src/
│   └── main/
│       ├── java/            # Java 后端代码
│       │   └── com/example/heatcalculate/
│       │       ├── controller/   # REST 控制器
│       │       ├── service/      # 业务逻辑
│       │       ├── model/        # 数据模型
│       │       ├── ai/           # AI 服务
│       │       └── exception/    # 异常处理
│       └── resources/
│           ├── application.yml   # 配置文件
│           └── static/           # 前端静态资源
├── openspec/                # OpenSpec 变更管理
└── pom.xml                  # Maven 配置
```

## 开发规范

本项目使用 [OpenSpec](openspec/) 进行变更管理，所有功能开发遵循以下流程：

1. **探索** (`/opsx:explore`) - 澄清需求和设计
2. **提案** (`/opsx:propose`) - 生成设计文档和任务列表
3. **实现** (`/opsx:apply`) - 按任务列表执行开发
4. **归档** (`/opsx:archive`) - 完成并归档变更

## 浏览器支持

- Chrome 90+
- Safari 14+
- Firefox 88+
- Edge 90+

## 许可证

MIT License

## 致谢

- [LangChain4j](https://github.com/langchain4j/langchain4j) - Java LLM 集成框架
- [Tongyi Qianwen](https://tongyi.aliyun.com/) - 阿里云大语言模型
