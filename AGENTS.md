# AGENTS.md

> 本文件面向 AI 编码助手（Claude Code、Cursor 等），提供开发上下文和约束。人类可读文档见 README.md。

## 项目概述

企业智能知识库系统 — 基于 Spring Boot 3.4 + JDK 21 的 RAG 知识问答平台。自研分层架构（未使用 Spring AI 框架依赖），通过接口抽象实现模型提供商、文档存储、知识检索的可插拔切换。

## 技术栈

- Java 21, Spring Boot 3.4.4, Maven
- HTTP 客户端: Spring RestClient（同步）+ JDK HttpClient（SSE 流式）
- 对象存储: MinIO（S3 兼容）
- 文档解析: Apache PDFBox, Apache POI, Flexmark
- 构建脚本: `./scripts/mvn-jdk21.sh`（封装了 JDK 和 Maven settings 路径）

## 编译与运行

```bash
# 编译
./scripts/mvn-jdk21.sh compile

# 启动（跳过测试）
./scripts/mvn-jdk21.sh spring-boot:run -Dmaven.test.skip=true

# 运行测试
./scripts/mvn-jdk21.sh test
```

服务端口: 8080

## 分层架构

```
com.ma.agent/
├── controller/              # REST API 入口
│   ├── KnowledgeBaseController   # 知识库管理 CRUD
│   ├── KnowledgeQAController     # 知识库问答（同步 + 流式）
│   ├── DocumentController        # 文档管理（上传、列表、删除）
│   ├── KnowledgeController       # 关键词检索
│   ├── RagController             # RAG 语义检索
│   └── HealthController          # 健康检查
├── agent/                   # 问答服务层
│   ├── KnowledgeQAService        # 知识库问答接口
│   ├── KnowledgeQAServiceImpl    # RAG + LLM 问答实现
│   └── dto/                      # KnowledgeQAResponse, ChatStreamChunk
├── knowledge/               # 知识库核心
│   ├── base/               # 知识库管理（KnowledgeBaseService）
│   ├── document/           # 文档上传、多格式解析（DocumentParserFactory）
│   ├── chunk/              # 文档分块（RecursiveChunker）
│   ├── embedding/          # 向量嵌入（EmbeddingService）
│   ├── vector/             # 向量存储（VectorStore）
│   ├── rag/                # RAG 管道（RagPipeline）
│   ├── store/              # 文档存储（InMemoryDocumentStore）
│   └── search/             # 知识检索（KnowledgeService）
├── model/                   # 模型提供商适配（ModelGateway 接口）
├── cache/                   # Redis 缓存服务
├── entity/                  # 数据库实体（KnowledgeBase, Document, Conversation）
├── mapper/                  # MyBatis-Plus Mapper
└── shared/                  # 公共基础
    ├── LogMarkers           # 日志 Marker（API / BIZ / DATA）
    ├── GlobalExceptionHandler
    └── ErrorResponse
```

## 核心接口与扩展点

新增功能时优先实现对应接口，通过 `@ConditionalOnProperty` 切换：

| 接口 | 配置键 | 已有实现 |
|------|--------|----------|
| `ModelGateway` | `agent.model.provider` | `xiaomi-mimo`, `mock` |
| `DocumentService` | `agent.document.provider` | `local`, `oss`, `mock` |
| `KnowledgeService` | `agent.knowledge.provider` | `keyword`, `mock` |
| `FileParser` | 自动注册 | `txt`, `pdf`, `docx`, `xlsx`, `md` |

### 新增模型提供商

1. 实现 `ModelGateway` 接口（`chat` + `stream` 方法）
2. 添加 `@ConditionalOnProperty(prefix = "agent.model", name = "provider", havingValue = "your-provider")`
3. 在 `application.yml` 的 `agent.model` 下添加所需配置

### 新增文档格式解析器

1. 实现 `FileParser` 接口（`supportedExtension` + `parse` 方法）
2. 添加 `@Component`
3. `DocumentParserFactory` 自动收集，无需手动注册

### 新增知识库

1. 调用 `POST /api/knowledge-bases` 创建知识库
2. 上传文档时指定 `kbId` 参数
3. 问答时指定 `kbId` 参数，系统自动从该知识库检索

## API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/health` | 健康检查 |
| POST | `/api/knowledge-bases` | 创建知识库 |
| GET | `/api/knowledge-bases` | 知识库列表 |
| GET | `/api/knowledge-bases/{kbId}` | 知识库详情 |
| PUT | `/api/knowledge-bases/{kbId}` | 更新知识库 |
| DELETE | `/api/knowledge-bases/{kbId}` | 删除知识库 |
| POST | `/api/documents/upload?kbId=xxx` | 上传文档（多格式） |
| GET | `/api/documents?kbId=xxx` | 文档列表 |
| GET | `/api/documents/{id}/content` | 文档内容 |
| PUT | `/api/documents/{id}/category` | 更新文档分类 |
| DELETE | `/api/documents/{id}` | 删除文档 |
| POST | `/api/qa/ask` | 知识库问答（同步） |
| POST | `/api/qa/ask/stream` | 知识库问答（SSE 流式） |
| GET | `/api/qa/history/{conversationId}` | 对话历史 |
| POST | `/api/knowledge/query` | 关键词检索 |
| POST | `/api/rag/index` | 索引文档到向量库 |
| POST | `/api/rag/query` | 语义向量检索 |
| GET | `/api/rag/stats` | 向量库统计 |

## Provider 切换

```yaml
agent:
  model:
    provider: xiaomi-mimo    # 或 mock
  document:
    provider: local           # 或 oss / mock
  knowledge:
    provider: keyword         # 或 mock
```

命令行覆盖: `-Dspring-boot.run.arguments='--agent.model.provider=mock'`

## 日志体系

基于 Marker 的四级日志分离（logback-spring.xml）：

| Marker | 文件 | 职责 |
|--------|------|------|
| `API` | `logs/api.log` | Controller 层请求/响应 |
| `BIZ` | `logs/biz.log` | Service 层业务操作 |
| `DATA` | `logs/data.log` | 外部调用（模型 API、OSS） |
| 无 | `logs/sys.log` | 框架/通用日志 |

使用方式: `log.info(LogMarkers.API, "msg={}", value)`

## 关键设计决策

- **自研而非 Spring AI**: 不依赖 `spring-ai-*` 的 starter，通过自定义 `ModelGateway` 接口直接对接模型 API，保持最小依赖和最大控制
- **条件 Bean 切换**: 所有可插拔组件使用 `@ConditionalOnProperty` 而非 Profile，支持运行时通过配置文件或命令行参数切换
- **多格式解析器工厂**: `DocumentParserFactory` 自动收集所有 `FileParser` Bean，根据文件扩展名分发到对应解析器
- **RAG 管道**: 文档上传时自动分块、向量化、存储；问答时检索相关片段并构建增强 Prompt
- **SSE 流式**: 流式问答使用 `SseEmitter`，60 秒超时，独立线程池处理

## 编码约定

- Record 优先于 POJO 用于 DTO 和不可变数据
- 接口方法不加 `public` 修饰符
- 包级可见类只对外暴露接口
- 异常使用 `GlobalExceptionHandler` 统一处理，不在 Controller 中捕获
- 日志必须使用 `LogMarkers` 中的 Marker 分级

## 项目进度

| 阶段 | 状态 |
|------|------|
| 多格式文档解析 | 已完成 |
| 知识库管理 | 已完成 |
| RAG 语义检索 | 已完成 |
| AI 问答（RAG + LLM） | 已完成 |
| 向量嵌入模型集成 | 未开始 |
| 文档权限管理 | 未开始 |
| 搜索分析和使用统计 | 未开始 |
