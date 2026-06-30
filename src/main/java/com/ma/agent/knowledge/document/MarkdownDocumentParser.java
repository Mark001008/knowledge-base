package com.ma.agent.knowledge.document;

import com.ma.agent.shared.LogMarkers;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Markdown 文档解析器 - 使用 Flexmark
 * <p>直接提取 Markdown 源文本作为内容（保留格式标记以增强语义）</p>
 */
@Component
class MarkdownDocumentParser implements FileParser {

    private static final Logger log = LoggerFactory.getLogger(MarkdownDocumentParser.class);

    @Override
    public String supportedExtension() {
        return "md";
    }

    @Override
    public String parse(MultipartFile file) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);

            // 验证是合法的 Markdown（解析不报错即可）
            MutableDataSet options = new MutableDataSet();
            Parser parser = Parser.builder(options).build();
            Node document = parser.parse(content);
            HtmlRenderer renderer = HtmlRenderer.builder(options).build();
            renderer.render(document); // 验证解析成功

            log.info(LogMarkers.DATA, "Markdown parsed: filename={} chars={}",
                    file.getOriginalFilename(), content.length());
            return content;
        } catch (IOException e) {
            throw new DocumentParseException("Failed to parse Markdown: " + file.getOriginalFilename(), e);
        }
    }
}
