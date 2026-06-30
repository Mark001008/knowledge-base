package com.ma.agent.knowledge.document;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ma.agent.entity.DocumentEntity;
import com.ma.agent.knowledge.dto.DocumentUploadResponse;
import com.ma.agent.mapper.DocumentMapper;
import com.ma.agent.shared.LogMarkers;
import io.minio.*;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * OSS 文档服务 - 使用 MinIO 存储文件，MySQL 存储元数据
 * <p>支持多格式文档解析（txt, pdf, docx, xlsx, md）</p>
 */
@Service
@ConditionalOnProperty(prefix = "agent.document", name = "provider", havingValue = "oss")
class OssDocumentService implements DocumentService {

    private static final Logger log = LoggerFactory.getLogger(OssDocumentService.class);

    private final MinioClient minioClient;
    private final OssProperties ossProperties;
    private final DocumentMapper documentMapper;
    private final DocumentParser parser;

    OssDocumentService(OssProperties ossProperties, DocumentMapper documentMapper, DocumentParser parser) {
        this.ossProperties = ossProperties;
        this.documentMapper = documentMapper;
        this.parser = parser;

        // 配置 OkHttp 客户端，绕过代理连接 MinIO
        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS);

        // 获取代理配置
        String proxyHost = getProxyHost();
        String proxyPort = getProxyPort();

        // 如果配置了代理，且 MinIO 地址不是本地地址，则使用代理
        if (proxyHost != null && proxyPort != null && !isLocalAddress(ossProperties.endpoint())) {
            log.info("MinIO client using proxy: {}:{}", proxyHost, proxyPort);
            okHttpClientBuilder.proxy(new Proxy(
                    Proxy.Type.HTTP,
                    new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort))
            ));
        } else {
            // MinIO 是本地或远程服务器，不使用代理
            log.info("MinIO client connecting directly to: {}", ossProperties.endpoint());
            okHttpClientBuilder.proxy(Proxy.NO_PROXY);
        }

        OkHttpClient okHttpClient = okHttpClientBuilder.build();

        this.minioClient = MinioClient.builder()
                .endpoint(ossProperties.endpoint())
                .credentials(ossProperties.accessKey(), ossProperties.secretKey())
                .httpClient(okHttpClient)
                .build();
    }

    @Override
    public DocumentUploadResponse upload(MultipartFile file, String kbId) {
        String documentId = java.util.UUID.randomUUID().toString();
        String originalFilename = file.getOriginalFilename();
        String objectName = documentId + "-" + originalFilename;
        String fileType = extractExtension(originalFilename);

        try {
            ensureBucket();

            // 上传文件到 MinIO
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(ossProperties.bucket())
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            // 解析文件内容（支持多格式）
            ParsedDocument parsed = parser.parse(documentId, file);

            // 保存元数据到 MySQL
            DocumentEntity entity = new DocumentEntity();
            entity.setDocumentId(documentId);
            entity.setKbId(kbId != null ? kbId : "default");
            entity.setFilename(originalFilename);
            entity.setContent(parsed.content());
            entity.setCharCount(parsed.charCount());
            entity.setFileType(fileType);
            entity.setFileSize(file.getSize());
            entity.setCategory("");
            entity.setStatus("indexed");
            entity.setUploadedAt(LocalDateTime.now());
            documentMapper.insert(entity);

            log.info(LogMarkers.DATA, "Document uploaded: bucket={}, object={}, type={}, size={}",
                    ossProperties.bucket(), objectName, fileType, file.getSize());

            return new DocumentUploadResponse(documentId, originalFilename, "stored");
        } catch (Exception e) {
            log.error(LogMarkers.DATA, "Failed to upload document: {}", objectName, e);
            throw new OssUploadException("Failed to upload document", e);
        }
    }

    @Override
    public List<DocumentInfo> listDocuments() {
        return documentMapper.selectList(null).stream()
                .map(this::toDocumentInfo)
                .toList();
    }

    @Override
    public List<DocumentInfo> listDocumentsByKbId(String kbId) {
        return documentMapper.selectList(
                new LambdaQueryWrapper<DocumentEntity>()
                        .eq(DocumentEntity::getKbId, kbId)
                        .orderByDesc(DocumentEntity::getUploadedAt)
        ).stream().map(this::toDocumentInfo).toList();
    }

    @Override
    public Optional<String> getContent(String documentId) {
        DocumentEntity entity = documentMapper.selectById(documentId);
        return entity != null ? Optional.of(entity.getContent()) : Optional.empty();
    }

    @Override
    public void delete(String documentId) {
        documentMapper.deleteById(documentId);
        log.info(LogMarkers.BIZ, "文档删除: documentId={}", documentId);
    }

    @Override
    public void updateCategory(String documentId, String category) {
        DocumentEntity entity = documentMapper.selectById(documentId);
        if (entity != null) {
            entity.setCategory(category);
            documentMapper.updateById(entity);
            log.info(LogMarkers.BIZ, "文档分类更新: documentId={} category={}", documentId, category);
        }
    }

    private DocumentInfo toDocumentInfo(DocumentEntity entity) {
        return new DocumentInfo(
                entity.getDocumentId(),
                entity.getKbId(),
                entity.getFilename(),
                entity.getCharCount(),
                entity.getFileType(),
                entity.getFileSize(),
                entity.getCategory(),
                entity.getStatus(),
                entity.getUploadedAt() != null ? entity.getUploadedAt().toString() : ""
        );
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(ossProperties.bucket()).build());
        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(ossProperties.bucket()).build());
            log.info(LogMarkers.DATA, "Bucket created: {}", ossProperties.bucket());
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "txt";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * 获取代理主机
     */
    private String getProxyHost() {
        String proxy = System.getenv("HTTPS_PROXY");
        if (proxy == null || proxy.isBlank()) {
            proxy = System.getenv("HTTP_PROXY");
        }
        if (proxy == null || proxy.isBlank()) {
            return null;
        }
        try {
            String withoutProtocol = proxy.replaceAll("^https?://", "");
            return withoutProtocol.split(":")[0];
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取代理端口
     */
    private String getProxyPort() {
        String proxy = System.getenv("HTTPS_PROXY");
        if (proxy == null || proxy.isBlank()) {
            proxy = System.getenv("HTTP_PROXY");
        }
        if (proxy == null || proxy.isBlank()) {
            return null;
        }
        try {
            String withoutProtocol = proxy.replaceAll("^https?://", "");
            String[] parts = withoutProtocol.split(":");
            return parts.length > 1 ? parts[1] : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查是否是本地地址
     */
    private boolean isLocalAddress(String endpoint) {
        if (endpoint == null) {
            return false;
        }
        String lower = endpoint.toLowerCase();
        return lower.contains("localhost") ||
               lower.contains("127.0.0.1") ||
               lower.contains("0.0.0.0");
    }

    static class OssUploadException extends RuntimeException {
        OssUploadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
