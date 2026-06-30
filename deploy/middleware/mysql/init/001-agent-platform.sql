CREATE DATABASE IF NOT EXISTS agent_platform
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE agent_platform;

CREATE TABLE IF NOT EXISTS knowledge_bases (
  kb_id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  document_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS documents (
  document_id VARCHAR(64) PRIMARY KEY,
  kb_id VARCHAR(64) NOT NULL DEFAULT 'default',
  filename VARCHAR(255) NOT NULL,
  content LONGTEXT,
  char_count BIGINT,
  file_type VARCHAR(32),
  file_size BIGINT,
  category VARCHAR(255) DEFAULT '',
  status VARCHAR(32) NOT NULL DEFAULT 'pending',
  uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_documents_kb_id (kb_id),
  INDEX idx_documents_uploaded_at (uploaded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS conversations (
  id VARCHAR(64) PRIMARY KEY,
  conversation_id VARCHAR(64) NOT NULL,
  kb_id VARCHAR(64) DEFAULT 'default',
  role VARCHAR(32) NOT NULL,
  content LONGTEXT,
  model VARCHAR(100),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_conversations_conversation_id (conversation_id),
  INDEX idx_conversations_kb_id (kb_id),
  INDEX idx_conversations_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
