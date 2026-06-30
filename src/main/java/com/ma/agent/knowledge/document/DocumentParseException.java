package com.ma.agent.knowledge.document;

/**
 * 文档解析异常
 */
public class DocumentParseException extends RuntimeException {

    public DocumentParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
