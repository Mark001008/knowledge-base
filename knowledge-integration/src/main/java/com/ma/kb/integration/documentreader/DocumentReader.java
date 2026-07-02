package com.ma.kb.integration.documentreader;

import java.io.InputStream;

/**
 * 文档读取器接口
 */
public interface DocumentReader {

    /**
     * 读取文档内容
     *
     * @param inputStream 文件流
     * @param fileName    文件名
     * @return 文档文本内容
     */
    String read(InputStream inputStream, String fileName);
}
