package com.ma.agent.knowledge.chunk;

import java.util.List;

/**
 * 文档分块器接口。
 *
 * 将长文档切分为多个小块（chunk），每块包含一段有意义的文本。
 * 分块是 RAG 的第一步，分块质量直接影响检索效果。
 */
public interface DocumentChunker {

    /**
     * 将文本内容切分为多个块。
     *
     * @param content   原始文本内容
     * @param metadata  文档元数据（文件名等），用于生成块的元信息
     * @return 分块结果列表
     */
    List<Chunk> chunk(String content, ChunkMetadata metadata);

    /**
     * 一个文本块。
     *
     * @param content    块的文本内容
     * @param metadata   块的元数据
     * @param index      块在文档中的序号（从 0 开始）
     */
    record Chunk(String content, ChunkMetadata metadata, int index) {

        /**
         * 块的唯一标识：{documentId}_chunk_{index}
         */
        public String chunkId() {
            return metadata.documentId() + "_chunk_" + index;
        }
    }

    /**
     * 块的元数据。
     */
    record ChunkMetadata(
            String documentId,
            String filename,
            int totalChunks
    ) {
    }
}
