package com.ma.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 对话历史实体 - 持久化到 MySQL
 * <p>存储每次对话的消息记录，支持多轮对话上下文追溯</p>
 */
@TableName("conversations")
public class ConversationEntity {

    /** 消息ID（雪花算法生成） */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 会话ID，同一对话窗口的多条消息共享 */
    private String conversationId;

    /** 所属知识库ID */
    private String kbId;

    /** 消息角色：user-用户, assistant-助手, system-系统 */
    private String role;

    /** 消息内容 */
    private String content;

    /** 使用的模型名称 */
    private String model;

    /** 创建时间 */
    private LocalDateTime createdAt;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getKbId() {
        return kbId;
    }

    public void setKbId(String kbId) {
        this.kbId = kbId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
