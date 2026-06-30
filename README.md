# Enterprise Knowledge Base

企业智能知识库系统 — 基于 RAG 检索增强生成的 AI 知识问答平台。

## 项目定位

面向企业的智能知识库管理系统，支持多格式文档上传、语义检索、AI 问答，帮助员工快速获取所需知识。

### 核心能力

- **多格式文档解析** — 支持 .txt, .pdf, .docx, .xlsx, .md 格式
- **知识库管理** — 创建多个独立知识库，按业务领域分类
- **语义检索** — 基于向量嵌入的语义搜索，理解自然语言查询
- **AI 问答** — RAG 检索增强生成，基于文档内容回答问题并引用来源
- **流式对话** — SSE 实时流式返回答案，提升用户体验

## 项目分层

```
com.ma.agent/
├── controller/         # REST API 层
│   ├── KnowledgeBaseController   # 知识库管理
│   ├── KnowledgeQAController     # 知识库问答
│   ├── DocumentController        # 文档管理
│   ├── KnowledgeController       # 关键词检索
│   ├── RagController             # RAG 检索
│   └── HealthController          # 健康检查
├── agent/              # 问答服务层
│   ├── KnowledgeQAService        # 知识库问答接口
│   └── KnowledgeQAServiceImpl    # RAG + LLM 问答实现
├── knowledge/          # 知识库核心
│   ├── base/           # 知识库管理
│   ├── document/       # 文档上传、多格式解析
│   ├── chunk/          # 文档分块
│   ├── embedding/      # 向量嵌入
│   ├── vector/         # 向量存储
│   ├── rag/            # RAG 管道
│   ├── store/          # 文档存储
│   └── search/         # 知识检索
├── model/              # 模型提供商适配层
├── cache/              # Redis 缓存
├── entity/             # 数据库实体
├── mapper/             # MyBatis-Plus Mapper
└── shared/             # 公共基础（异常处理、日志标记）
```

## API 端点

### 知识库管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/knowledge-bases` | 创建知识库 |
| GET | `/api/knowledge-bases` | 知识库列表 |
| GET | `/api/knowledge-bases/{kbId}` | 知识库详情 |
| PUT | `/api/knowledge-bases/{kbId}` | 更新知识库 |
| DELETE | `/api/knowledge-bases/{kbId}` | 删除知识库 |

### 文档管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/documents/upload?kbId=xxx` | 上传文档（支持多格式） |
| GET | `/api/documents?kbId=xxx` | 文档列表 |
| GET | `/api/documents/{id}/content` | 文档内容 |
| PUT | `/api/documents/{id}/category` | 更新文档分类 |
| DELETE | `/api/documents/{id}` | 删除文档 |

### 知识库问答

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/qa/ask` | 知识库问答（同步） |
| POST | `/api/qa/ask/stream` | 知识库问答（SSE 流式） |
| GET | `/api/qa/history/{conversationId}` | 对话历史 |

### 底层检索

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/knowledge/query` | 关键词检索 |
| POST | `/api/rag/index` | 索引文档到向量库 |
| POST | `/api/rag/query` | 语义向量检索 |
| POST | `/api/rag/query-with-context` | 检索 + 构建增强 Prompt |
| GET | `/api/rag/stats` | 向量库统计 |

### 系统

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/health` | 健康检查 |

## Smoke Test

```bash
# 健康检查
curl -s http://localhost:8080/health

# 创建知识库
curl -s -X POST http://localhost:8080/api/knowledge-bases \
  -H 'Content-Type: application/json' \
  -d '{"name":"产品文档库","description":"产品相关文档"}'

# 上传文档（支持 .txt, .pdf, .docx, .xlsx, .md）
curl -s -X POST "http://localhost:8080/api/documents/upload?kbId=YOUR_KB_ID" \
  -F "file=@/path/to/document.pdf"

# 知识库问答（同步）
curl -s -X POST http://localhost:8080/api/qa/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"产品有哪些功能？","kbId":"YOUR_KB_ID"}'

# 知识库问答（流式）
curl -N -X POST http://localhost:8080/api/qa/ask/stream \
  -H 'Content-Type: application/json' \
  -d '{"question":"产品有哪些功能？","kbId":"YOUR_KB_ID"}'

# 关键词检索
curl -s -X POST http://localhost:8080/api/knowledge/query \
  -H 'Content-Type: application/json' \
  -d '{"question":"关键词"}'
```

## 运行

本机 JDK 环境为 Oracle JDK 21（`/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home`），
Maven 配置使用项目专用 settings（`/Users/mahengchao/workspace/environment/apache-maven-3.9.14/conf/settings-self.xml`）。

```bash
# 编译
./scripts/mvn-jdk21.sh compile

# 启动（跳过测试编译）
./scripts/mvn-jdk21.sh spring-boot:run -Dmaven.test.skip=true

# 运行测试
./scripts/mvn-jdk21.sh test
```

## 敏感配置

不要把真实数据库、Redis、模型 API Key、OSS 密钥写进 `application.yml`。本项目通过环境变量注入敏感配置：

```bash
export DB_URL='jdbc:mysql://HOST:3306/agent_platform?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true'
export DB_USERNAME='agent'
export DB_PASSWORD='your-db-password'
export REDIS_HOST='HOST'
export REDIS_PORT='6379'
export REDIS_PASSWORD='your-redis-password'
export MODEL_PROVIDER='xiaomi-mimo'
export MODEL_API_KEY='your-model-api-key'
export DOCUMENT_PROVIDER='oss'
export OSS_ENDPOINT='http://HOST:9000'
export OSS_ACCESS_KEY='your-oss-access-key'
export OSS_SECRET_KEY='your-oss-secret-key'
```

本机私有配置也可以放在 `src/main/resources/application-local.yml`，该文件已被 `.gitignore` 忽略。

## Provider 切换

默认使用 mock 模型，避免本地启动时强依赖外部 API。可通过配置切换：

```yaml
# application.yml
agent:
  model:
    provider: xiaomi-mimo    # 或 mock
  knowledge:
    provider: keyword         # 或 mock
  document:
    provider: local           # 或 oss / mock
```

命令行指定 provider：

```bash
# 切换到 mock provider
./scripts/mvn-jdk21.sh spring-boot:run -Dmaven.test.skip=true \
  -Dspring-boot.run.arguments='--agent.model.provider=mock'

# 小米 MiMo
export MODEL_PROVIDER=xiaomi-mimo
export MODEL_API_KEY=your-key
./scripts/mvn-jdk21.sh spring-boot:run -Dmaven.test.skip=true \
  -Dspring-boot.run.arguments='--agent.model.provider=xiaomi-mimo'
```

## 支持的文档格式

| 格式 | 扩展名 | 说明 |
|------|--------|------|
| 纯文本 | .txt | UTF-8 编码文本文件 |
| PDF | .pdf | 使用 Apache PDFBox 解析 |
| Word | .docx | 使用 Apache POI 解析 |
| Excel | .xlsx | 使用 Apache POI 解析 |
| Markdown | .md | 使用 Flexmark 解析 |

## 数据库表

| 表名 | 说明 |
|------|------|
| knowledge_bases | 知识库元信息 |
| documents | 文档元信息和内容 |
| conversations | 对话历史记录 |

## Roadmap

- [x] 多格式文档解析（PDF, Word, Excel, Markdown）
- [x] 知识库管理（CRUD）
- [x] RAG 语义检索 + AI 问答
- [ ] 向量嵌入模型集成（替换 Mock Embedding）
- [ ] 文档权限管理
- [ ] 搜索分析和使用统计
- [ ] 问答质量评估
