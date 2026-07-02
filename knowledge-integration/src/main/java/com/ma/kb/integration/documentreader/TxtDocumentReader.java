package com.ma.kb.integration.documentreader;

import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * TXT 文档读取器
 */
@Component
public class TxtDocumentReader implements DocumentReader {

    @Override
    public String read(InputStream inputStream, String fileName) {
        try {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("TXT 文档读取失败: " + e.getMessage(), e);
        }
    }
}
