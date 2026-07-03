# AI 知识库平台需求文档

## 1. 项目背景

企业内部通常存在大量非结构化资料，例如 PDF、Word、Markdown、TXT、网页文档、制度文件、产品手册、技术文档、客服话术和项目资料。传统目录式文档管理依赖人工查找，检索效率低，且难以直接回答具体问题。

本项目拟建设一个基于 Java、Spring Boot、MySQL、MinIO、可选 Milvus 和 OpenAI 兼容模型接口的 AI 知识库平台，支持文档上传、解析、可选向量化、检索增强生成（RAG）问答、引用溯源、权限控制和问答记录管理，帮助用户通过自然语言快速获取可信答案。

## 2. 建设目标

### 2.1 核心目标

- 支持用户创建和管理多个知识库。
- 支持上传 PDF、Word、TXT、Markdown 等知识资料。
- 支持自动解析文档内容，按语义切片保存分片，并在启用 Milvus 时生成向量索引。
- 支持用户基于知识库进行自然语言问答。
- 支持回答结果附带引用来源，便于用户核验。
- 支持基础用户、角色和知识库权限控制。
- 支持问答历史、文档状态和知识库使用情况管理。

### 2.2 非目标

第一阶段不包含以下能力：

- 复杂知识图谱构建。
- 多智能体自动工作流。
- 大规模多租户商业化计费体系。
- 全量办公套件协同编辑。
- 自研大模型训练或微调平台。

## 3. 用户角色

| 角色 | 说明 | 主要权限 |
| --- | --- | --- |
| 系统管理员 | 平台最高权限用户 | 用户管理、知识库管理、系统配置、日志查看 |
| 知识库管理员 | 某个知识库的负责人 | 文档上传、删除、重建索引、成员管理 |
| 普通用户 | 使用知识库问答的用户 | 查看有权限的知识库、发起问答、查看历史 |
| 只读用户 | 仅查看知识库内容和问答结果 | 查询问答、查看引用，不可上传或删除文档 |

## 4. 业务流程

### 4.1 文档入库流程

```text
用户上传文档
  -> 保存原始文件
  -> 创建文档记录
  -> 提交解析任务
  -> 提取文档文本
  -> 文本清洗
  -> 文本切片
  -> 写入文档分片
  -> 如果启用 Milvus，则调用 Embedding 模型生成向量并写入 Milvus
  -> 如果未启用 Milvus，则跳过向量写入
  -> 更新文档索引状态
```

### 4.2 知识库问答流程

```text
用户选择知识库并提问
  -> 校验用户权限
  -> 如启用向量能力，则对问题生成查询向量
  -> 从向量库检索相关文档片段
  -> 构造 RAG Prompt
  -> 调用 Chat Model 生成答案
  -> 返回答案、引用来源、相关片段
  -> 保存问答记录
```

## 5. 功能需求

### 5.1 用户与认证

- 支持用户登录、退出。
- 支持 JWT Token 认证。
- 支持用户启用、禁用。
- 支持角色分配。
- 支持用户基本信息维护。

### 5.2 知识库管理

- 支持创建知识库。
- 支持编辑知识库名称、描述、可见范围。
- 支持删除知识库。
- 支持查看知识库文档数量、索引状态、最近更新时间。
- 支持配置知识库默认模型参数，例如 TopK、相似度阈值、温度参数。

### 5.3 文档管理

- 支持上传 PDF、DOCX、TXT、MD 文件。
- 支持记录文档名称、类型、大小、上传人、上传时间。
- 支持文档解析状态展示：
  - 待处理
  - 解析中
  - 已完成
  - 失败
- 支持查看文档解析失败原因。
- 支持删除文档及其向量索引。
- 支持重新解析和重建索引。

### 5.4 文档解析与切片

- 支持提取文档正文内容。
- 支持去除空白行、重复空格和无效字符。
- 支持按固定长度和重叠窗口进行文本切片。
- 支持保留文档元数据：
  - 文档 ID
  - 知识库 ID
  - 文件名
  - 页码
  - 分片序号
  - 上传人
- 支持后续扩展不同文档解析器。

### 5.5 向量索引

- 支持调用 OpenAI 兼容 Embedding 接口生成文本向量。
- 支持使用 Milvus 存储和检索向量，Milvus 未启用时跳过向量写入和向量检索。
- 第一阶段默认使用 MySQL 8.0 保存业务数据和文档分片；Milvus 通过 `VECTOR_MILVUS_ENABLED` 控制是否启用。
- 当前 2c2g 部署默认关闭 Milvus，后续单独 Milvus 服务器就绪后再启用。
- 支持按知识库 ID 过滤检索范围。
- 支持按权限过滤检索范围。
- 支持配置检索 TopK 和相似度阈值。

### 5.6 AI 问答

- 支持用户对指定知识库发起问题。
- 支持多轮会话。
- 支持基于检索片段生成回答。
- 支持回答中返回引用来源。
- 支持当知识库无相关资料时明确提示无法回答。
- 支持流式输出回答内容。
- 支持保存问题、回答、引用片段、模型名称、耗时和 Token 消耗。

### 5.7 引用溯源

- 每条回答应返回引用来源列表。
- 引用来源包含：
  - 文档名称
  - 页码或分片位置
  - 文档片段摘要
  - 相似度分数
- 用户可点击引用查看相关原文片段。

### 5.8 后台管理

- 支持用户管理。
- 支持知识库管理。
- 支持文档处理状态管理。
- 支持问答日志查询。
- 支持模型配置管理。
- 支持系统运行状态查看。

## 6. 非功能需求

### 6.1 性能

- 单个普通文档上传后，应异步处理，不阻塞用户操作。
- 普通问答接口首字响应时间目标小于 3 秒。
- 文档检索耗时目标小于 1 秒。
- 支持后续通过消息队列和任务队列扩展文档处理能力。

### 6.2 安全

- 所有接口需进行身份认证。
- 用户只能访问有权限的知识库。
- 向量检索必须带上权限过滤条件。
- 原始文件下载需校验权限。
- 敏感配置如 API Key 不得写入代码仓库。

### 6.3 可观测性

- 记录文档解析日志。
- 记录问答请求日志。
- 记录模型调用耗时和错误信息。
- 记录向量检索 TopK、相似度分数和命中文档。
- 支持后续接入 Prometheus、Grafana 和日志平台。

### 6.4 可扩展性

- 模型供应商可替换。
- 向量库可替换。
- 文件存储可替换。
- 文档解析器可扩展。
- 问答 Prompt 模板可配置。

## 7. 技术架构

### 7.1 推荐技术栈

| 层级 | 技术 |
| --- | --- |
| 后端框架 | Java 21、Spring Boot 3.x |
| AI 接入 | 当前直接对接 OpenAI 兼容 HTTP 接口，后续可接 Spring AI |
| 大模型 | OpenAI、Azure OpenAI、DeepSeek、Ollama 可选 |
| Embedding | OpenAI Embedding、Ollama Embedding 或兼容模型 |
| 数据库 | MySQL 8.0 |
| 向量库 | Milvus，可配置关闭 |
| 文件存储 | MinIO 或本地文件存储 |
| 权限认证 | Spring Security + JWT |
| 数据访问 | Spring Data JPA 或 MyBatis Plus |
| 异步任务 | Spring Task，后续可扩展 RabbitMQ |
| 前端 | Vue 3 或 React |
| 部署 | Docker Compose |

### 7.2 后端工程分层

后端采用按工程职责分层的单体模块化结构，业务能力在各层内按领域包继续拆分。

```text
start          应用启动和组装层，包含 Spring Boot 启动类、配置文件、健康检查静态页、HTTP Controller、打包插件等
trigger        触发入口层，包含消息消费、定时任务、Job 入口，只做触发接入、参数校验和入口转发
service        服务接口和实现层，包含对外服务接口、DTO、服务实现、统一异常边界和对 core 层的调用编排
core           核心业务层，承载业务规则、业务校验、业务流程编排和内部对象转换，可调用 integration 和 common
manager        数据管理层，封装 DAL 层 mapper，提供数据库表操作的原子逻辑，完成 DO 到 BO 的转换
dal            数据库持久层，包含 mapper 接口、mapper.xml 和 DO 数据模型，仅允许单表操作
integration    服务集成层，负责外部 HTTP/RPC 调用、外部 DTO、超时/失败/幂等处理和外部响应转换
common         通用能力层，包含工具类、常量、枚举、异常、配置属性、过滤器、切面等
tests          测试层，包含集成测试、冒烟测试、部署验证类测试，不作为业务运行层
```

业务领域包括：

```text
auth           用户认证、JWT、权限控制
user           用户、角色、成员管理
space          知识库空间管理
document       文档上传、解析、状态管理
index          文本切片、Embedding、向量索引
chat           RAG 问答、多轮会话、引用来源
admin          系统配置、日志、模型配置
```

分层约束：

- `core` 可调用 `manager`、`integration` 和 `common`。
- `manager` 可调用 `dal`，可访问 Redis、Elasticsearch 等非关系型数据库，不开启数据库事务，非特殊情况不可捕获异常。
- `dal` 只能单表操作，不能出现联表操作。
- 业务对象以 `***BO` 命名，DAL 数据模型对象以 `***DO` 命名。
- DAL 增删改查接口方法必须以 `insert`、`delete`、`update`、`select` 开头。
- `mapper.xml` 必须按照标准格式书写，每个方法必须加注释，表名必须起别名，尽量避免大量动态条件。
- 无意义 ID 在 insert 操作中不需要先获取序列，直接写在 SQL 中，减少数据库交互。

### 7.3 核心组件

- `ModelClient`：用于调用 OpenAI 兼容聊天模型生成回答。
- `EmbeddingClient`：用于生成文档片段和用户问题的向量。
- `VectorSearchService`：用于在 Milvus 启用时写入和检索向量数据。
- 自定义 RAG 流程：用于组合检索上下文和用户问题。
- 文档 Reader、Transformer、Splitter：用于文档解析和切片。

## 8. 数据模型设计

### 8.1 用户表 `sys_user`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| username | varchar | 用户名 |
| password_hash | varchar | 密码哈希 |
| display_name | varchar | 显示名称 |
| email | varchar | 邮箱 |
| status | varchar | 状态 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

### 8.2 知识库表 `kb_space`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| name | varchar | 知识库名称 |
| description | text | 描述 |
| owner_id | bigint | 所有人 |
| visibility | varchar | 可见范围 |
| top_k | int | 默认检索数量 |
| similarity_threshold | decimal | 相似度阈值 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

### 8.3 文档表 `kb_document`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| space_id | bigint | 知识库 ID |
| file_name | varchar | 文件名 |
| file_type | varchar | 文件类型 |
| file_size | bigint | 文件大小 |
| storage_path | varchar | 文件存储路径 |
| parse_status | varchar | 解析状态 |
| error_message | text | 失败原因 |
| uploaded_by | bigint | 上传人 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

### 8.4 文档分片表 `kb_document_chunk`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| space_id | bigint | 知识库 ID |
| document_id | bigint | 文档 ID |
| chunk_index | int | 分片序号 |
| content | text | 分片内容 |
| page_number | int | 页码 |
| token_count | int | Token 数 |
| vector_id | varchar | 向量记录 ID |
| created_at | timestamp | 创建时间 |

### 8.5 问答会话表 `kb_chat_session`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| space_id | bigint | 知识库 ID |
| user_id | bigint | 用户 ID |
| title | varchar | 会话标题 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

### 8.6 问答消息表 `kb_chat_message`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| session_id | bigint | 会话 ID |
| role | varchar | user 或 assistant |
| content | text | 消息内容 |
| model_name | varchar | 模型名称 |
| prompt_tokens | int | 输入 Token |
| completion_tokens | int | 输出 Token |
| latency_ms | bigint | 耗时 |
| created_at | timestamp | 创建时间 |

### 8.7 回答引用表 `kb_answer_citation`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| message_id | bigint | 回答消息 ID |
| document_id | bigint | 文档 ID |
| chunk_id | bigint | 分片 ID |
| score | decimal | 相似度分数 |
| quote_text | text | 引用片段 |
| created_at | timestamp | 创建时间 |

## 9. API 需求

### 9.1 认证接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/login` | 用户登录 |
| POST | `/api/auth/logout` | 用户退出 |
| GET | `/api/auth/me` | 当前用户信息 |

### 9.2 知识库接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/spaces` | 创建知识库 |
| GET | `/api/spaces` | 查询知识库列表 |
| GET | `/api/spaces/{id}` | 查询知识库详情 |
| PUT | `/api/spaces/{id}` | 更新知识库 |
| DELETE | `/api/spaces/{id}` | 删除知识库 |

### 9.3 文档接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/spaces/{spaceId}/documents` | 上传文档 |
| GET | `/api/spaces/{spaceId}/documents` | 查询文档列表 |
| GET | `/api/documents/{id}` | 查询文档详情 |
| DELETE | `/api/documents/{id}` | 删除文档 |
| POST | `/api/documents/{id}/reindex` | 重建索引 |

### 9.4 问答接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/spaces/{spaceId}/chat/sessions` | 创建会话 |
| GET | `/api/spaces/{spaceId}/chat/sessions` | 查询会话列表 |
| POST | `/api/chat/sessions/{sessionId}/messages` | 发送问题 |
| POST | `/api/chat/sessions/{sessionId}/messages/stream` | 流式问答 |
| GET | `/api/chat/sessions/{sessionId}/messages` | 查询会话消息 |

## 10. 页面需求

### 10.1 登录页

- 用户名和密码登录。
- 登录失败提示。
- 登录成功后进入知识库列表。

### 10.2 知识库列表页

- 展示用户可访问的知识库。
- 支持创建知识库。
- 支持按名称搜索。
- 展示文档数量和更新时间。

### 10.3 知识库详情页

- 展示知识库基本信息。
- 展示文档列表。
- 支持上传文档。
- 支持文档删除和重建索引。
- 支持进入问答页面。

### 10.4 问答页

- 左侧展示会话列表。
- 中间展示聊天内容。
- 底部输入问题。
- 支持流式展示回答。
- 回答下方展示引用来源。
- 支持点击引用查看原文片段。

### 10.5 后台管理页

- 用户管理。
- 知识库管理。
- 文档处理任务管理。
- 模型配置管理。
- 问答日志查询。

## 11. Prompt 要求

系统 Prompt 应满足：

- 只基于给定知识库上下文回答。
- 如果上下文不足，应明确说明无法从知识库中找到答案。
- 不编造来源。
- 回答应简洁、准确、结构清晰。
- 必须返回引用来源。

示例：

```text
你是企业知识库助手。请只根据提供的上下文回答用户问题。
如果上下文中没有答案，请回答“当前知识库中未找到相关信息”。
回答后请列出引用来源。
```

## 12. MVP 版本范围

第一阶段建议交付以下内容：

- 用户登录。
- 知识库创建和列表。
- 文档上传。
- PDF、TXT、Markdown 解析。
- 文档切片。
- Embedding 生成。
- Milvus 向量入库能力，当前部署可通过配置关闭。
- 知识库问答。
- 答案引用来源。
- 问答历史记录。
- Docker Compose 本地部署。

## 13. 后续规划

### 13.1 第二阶段

- Word 文档解析增强。
- Excel、PPT 文档解析。
- 网页 URL 抓取入库。
- 多轮会话记忆优化。
- 问答质量评价。
- 文档权限精细化。
- 模型调用统计。

### 13.2 第三阶段

- 多租户。
- 团队空间。
- 混合检索：关键词检索 + 向量检索。
- Rerank 重排序。
- 知识库自动更新。
- 企业微信、钉钉、飞书集成。
- 私有化模型部署。

## 14. 验收标准

- 用户可以成功登录系统。
- 用户可以创建知识库。
- 用户可以上传文档并看到处理状态。
- 文档处理完成后可以进行知识库问答。
- 问答结果能基于上传文档生成。
- 回答中包含引用来源。
- 无权限用户无法访问对应知识库。
- 文档删除后，对应内容不再参与检索。
- 系统支持通过 Docker Compose 在本地启动。

## 15. 关键风险

| 风险 | 说明 | 应对策略 |
| --- | --- | --- |
| 文档解析质量不稳定 | PDF 扫描件、复杂表格、图片内容难以解析 | 第一阶段限定文档类型，后续接入 OCR |
| 回答幻觉 | 模型可能生成知识库外内容 | 强化 Prompt、相似度阈值、引用校验 |
| 检索不准确 | 切片策略或 Embedding 模型影响召回 | 调整 chunk size、overlap、TopK，引入 Rerank |
| 成本不可控 | 模型调用和 Embedding 会产生成本 | 记录 Token，限制上传大小和调用频率 |
| 权限泄露 | 向量检索绕过业务权限 | 检索层强制加入 space_id 和权限过滤 |
