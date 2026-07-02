CREATE DATABASE IF NOT EXISTS kb_base_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE kb_base_db;

-- ============================================================
-- 1. 系统用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user (
  id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
  username      VARCHAR(64)  NOT NULL UNIQUE COMMENT '用户名',
  password_hash VARCHAR(255) NOT NULL        COMMENT '密码哈希',
  display_name  VARCHAR(64)  NOT NULL        COMMENT '显示名称',
  email         VARCHAR(128)                 COMMENT '邮箱',
  status        VARCHAR(32)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED/DISABLED',
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- ============================================================
-- 2. 系统角色表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_role (
  id         BIGINT      PRIMARY KEY AUTO_INCREMENT,
  role_code  VARCHAR(64) NOT NULL UNIQUE COMMENT '角色编码',
  role_name  VARCHAR(64) NOT NULL        COMMENT '角色名称',
  created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

-- ============================================================
-- 3. 用户角色关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user_role (
  id      BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  UNIQUE KEY uk_user_role (user_id, role_id),
  INDEX idx_user_role_user (user_id),
  INDEX idx_user_role_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ============================================================
-- 4. 知识库表
-- ============================================================
CREATE TABLE IF NOT EXISTS kb_space (
  id                   BIGINT        PRIMARY KEY AUTO_INCREMENT,
  name                 VARCHAR(128)  NOT NULL        COMMENT '知识库名称',
  description          TEXT                           COMMENT '知识库描述',
  owner_id             BIGINT        NOT NULL        COMMENT '所有人ID',
  visibility           VARCHAR(32)   NOT NULL DEFAULT 'PRIVATE' COMMENT '可见范围: PRIVATE/INTERNAL',
  top_k                INT           NOT NULL DEFAULT 5 COMMENT '默认检索数量',
  similarity_threshold  DECIMAL(5,4)  NOT NULL DEFAULT 0.7000 COMMENT '相似度阈值',
  temperature          DECIMAL(4,3)  NOT NULL DEFAULT 0.200 COMMENT '模型温度',
  chunk_size           INT           NOT NULL DEFAULT 800 COMMENT '分片大小(tokens)',
  chunk_overlap        INT           NOT NULL DEFAULT 120 COMMENT '分片重叠(tokens)',
  created_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_space_owner (owner_id),
  INDEX idx_space_visibility (visibility)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';

-- ============================================================
-- 5. 知识库成员表
-- ============================================================
CREATE TABLE IF NOT EXISTS kb_space_member (
  id         BIGINT      PRIMARY KEY AUTO_INCREMENT,
  space_id   BIGINT      NOT NULL COMMENT '知识库ID',
  user_id    BIGINT      NOT NULL COMMENT '用户ID',
  role       VARCHAR(32) NOT NULL COMMENT '成员角色: OWNER/ADMIN/READER',
  created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_space_user (space_id, user_id),
  INDEX idx_member_space (space_id),
  INDEX idx_member_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库成员表';

-- ============================================================
-- 6. 文档表
-- ============================================================
CREATE TABLE IF NOT EXISTS kb_document (
  id               BIGINT        PRIMARY KEY AUTO_INCREMENT,
  space_id         BIGINT        NOT NULL        COMMENT '知识库ID',
  file_name        VARCHAR(255)  NOT NULL        COMMENT '原始文件名',
  file_type        VARCHAR(32)   NOT NULL        COMMENT '文件类型: PDF/TXT/MD/DOCX',
  file_size        BIGINT        NOT NULL        COMMENT '文件大小(字节)',
  storage_bucket   VARCHAR(128)  NOT NULL        COMMENT 'MinIO bucket',
  storage_object_key VARCHAR(512) NOT NULL       COMMENT 'MinIO object key',
  parse_status     VARCHAR(32)   NOT NULL DEFAULT 'PENDING' COMMENT '处理状态: PENDING/PARSING/INDEXING/COMPLETED/FAILED',
  error_message    TEXT                           COMMENT '失败原因',
  uploaded_by      BIGINT        NOT NULL        COMMENT '上传人ID',
  created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_document_space_status (space_id, parse_status),
  INDEX idx_document_uploaded_by (uploaded_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档表';

-- ============================================================
-- 7. 文档分片表
-- ============================================================
CREATE TABLE IF NOT EXISTS kb_document_chunk (
  id           BIGINT    PRIMARY KEY AUTO_INCREMENT,
  space_id     BIGINT    NOT NULL COMMENT '知识库ID',
  document_id  BIGINT    NOT NULL COMMENT '文档ID',
  chunk_index  INT       NOT NULL COMMENT '分片序号',
  content      TEXT      NOT NULL COMMENT '分片内容',
  page_number  INT                COMMENT '页码',
  token_count  INT                COMMENT 'Token数',
  vector_id    VARCHAR(128)       COMMENT '向量记录ID',
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_chunk_space_document (space_id, document_id),
  INDEX idx_chunk_vector_id (vector_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档分片表';

-- ============================================================
-- 8. 问答会话表
-- ============================================================
CREATE TABLE IF NOT EXISTS kb_chat_session (
  id         BIGINT       PRIMARY KEY AUTO_INCREMENT,
  space_id   BIGINT       NOT NULL COMMENT '知识库ID',
  user_id    BIGINT       NOT NULL COMMENT '用户ID',
  title      VARCHAR(128) NOT NULL COMMENT '会话标题',
  created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_session_space (space_id),
  INDEX idx_session_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问答会话表';

-- ============================================================
-- 9. 问答消息表
-- ============================================================
CREATE TABLE IF NOT EXISTS kb_chat_message (
  id                 BIGINT       PRIMARY KEY AUTO_INCREMENT,
  session_id         BIGINT       NOT NULL COMMENT '会话ID',
  role               VARCHAR(32)  NOT NULL COMMENT '角色: user/assistant',
  content            TEXT         NOT NULL COMMENT '消息内容',
  model_name         VARCHAR(128)          COMMENT '模型名称',
  prompt_tokens      INT                   COMMENT '输入Token数',
  completion_tokens  INT                   COMMENT '输出Token数',
  latency_ms         BIGINT                COMMENT '耗时(毫秒)',
  created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_message_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问答消息表';

-- ============================================================
-- 10. 回答引用表
-- ============================================================
CREATE TABLE IF NOT EXISTS kb_answer_citation (
  id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
  message_id   BIGINT       NOT NULL COMMENT 'assistant消息ID',
  document_id  BIGINT       NOT NULL COMMENT '文档ID',
  chunk_id     BIGINT       NOT NULL COMMENT '分片ID',
  score        DECIMAL(8,6) NOT NULL COMMENT '相似度分数',
  quote_text   TEXT         NOT NULL COMMENT '引用片段',
  created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_citation_message (message_id),
  INDEX idx_citation_document (document_id),
  INDEX idx_citation_chunk (chunk_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='回答引用表';

-- ============================================================
-- 11. 模型配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_model_config (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  provider        VARCHAR(64)  NOT NULL DEFAULT 'openai-compatible' COMMENT '模型供应商',
  base_url        VARCHAR(255) NOT NULL        COMMENT '模型服务地址',
  api_key_ref     VARCHAR(128) NOT NULL        COMMENT 'API Key环境变量名',
  chat_model      VARCHAR(128) NOT NULL        COMMENT '聊天模型',
  embedding_model VARCHAR(128) NOT NULL        COMMENT 'Embedding模型',
  enabled         BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '是否启用',
  created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型配置表';

-- ============================================================
-- 初始化数据
-- ============================================================

-- 初始化系统角色
INSERT INTO sys_role (role_code, role_name) VALUES
  ('SYSTEM_ADMIN', '系统管理员'),
  ('USER', '普通用户');

-- 初始化系统管理员 (密码: admin123)
INSERT INTO sys_user (username, password_hash, display_name, email, status) VALUES
  ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 'admin@example.com', 'ENABLED');

-- 关联管理员角色
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u, sys_role r
WHERE u.username = 'admin' AND r.role_code = 'SYSTEM_ADMIN';

-- 初始化默认模型配置
INSERT INTO sys_model_config (provider, base_url, api_key_ref, chat_model, embedding_model, enabled) VALUES
  ('openai-compatible', 'https://api.example.com/v1', 'AI_API_KEY', 'gpt-4o-mini', 'text-embedding-3-small', TRUE);
