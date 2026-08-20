package com.ma.kb.service.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 在线文档创建/更新请求。
 */
public record OnlineDocumentRequest(
        @NotBlank(message = "文档标题不能为空") @Size(max = 200, message = "文档标题长度不能超过200字符") String title,
        @NotBlank(message = "文档内容不能为空") @Size(max = 500000, message = "文档内容长度不能超过500000字符") String content
) {
}
