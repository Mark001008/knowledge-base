package com.ma.kb.service.chat.converter;

import com.ma.kb.manager.chat.bo.AnswerCitationBO;
import com.ma.kb.manager.chat.bo.ChatMessageBO;
import com.ma.kb.manager.chat.bo.ChatSessionBO;
import com.ma.kb.integration.vector.SearchResult;
import com.ma.kb.service.chat.dto.ChatSessionCreateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Chat DTO -> BO 转换器
 */
@Mapper(componentModel = "spring")
public interface ChatDTOConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "spaceId", source = "spaceId")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "title", expression = "java(request.title() != null ? request.title() : \"新会话\")")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ChatSessionBO toSessionBO(ChatSessionCreateRequest request, Long spaceId, Long userId);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sessionId", source = "sessionId")
    @Mapping(target = "role", constant = "user")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "modelName", ignore = true)
    @Mapping(target = "promptTokens", ignore = true)
    @Mapping(target = "completionTokens", ignore = true)
    @Mapping(target = "latencyMs", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    ChatMessageBO toUserMessageBO(Long sessionId, String content);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sessionId", source = "sessionId")
    @Mapping(target = "role", constant = "assistant")
    @Mapping(target = "createdAt", ignore = true)
    ChatMessageBO toAssistantMessageBO(Long sessionId, String content, String modelName,
                                       int promptTokens, int completionTokens, long latencyMs);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "messageId", source = "messageId")
    @Mapping(target = "documentId", source = "searchResult.documentId")
    @Mapping(target = "chunkId", source = "searchResult.chunkId")
    @Mapping(target = "score", source = "searchResult.score")
    @Mapping(target = "quoteText", source = "searchResult.content")
    @Mapping(target = "documentName", ignore = true)
    @Mapping(target = "pageNumber", ignore = true)
    AnswerCitationBO toCitationBO(Long messageId, SearchResult searchResult);
}
