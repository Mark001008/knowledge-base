package com.ma.kb.service.chat;

import com.ma.kb.service.chat.dto.ChatMessageResponse;

/**
 * 问答流式输出接收器。
 */
public interface ChatStreamSink {

    void status(String message);

    void delta(String content);

    void complete(ChatMessageResponse response);

    void error(String message);
}
