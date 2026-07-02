package com.ma.kb.integration.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 本地文件存储实现（MVP 阶段）
 */
@Service
public class LocalStorageServiceImpl implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageServiceImpl.class);

    @Value("${storage.local.base-path:./storage}")
    private String basePath;

    @Value("${storage.local.bucket:kb-documents}")
    private String defaultBucket;

    @Override
    public String upload(String bucket, String objectKey, InputStream inputStream,
                         long fileSize, String contentType) {
        try {
            Path dir = Paths.get(basePath, bucket);
            Files.createDirectories(dir);

            Path filePath = dir.resolve(objectKey);
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("文件上传成功: bucket={}, objectKey={}, size={}", bucket, objectKey, fileSize);
            return filePath.toString();
        } catch (IOException e) {
            log.error("文件上传失败: bucket={}, objectKey={}", bucket, objectKey, e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream download(String bucket, String objectKey) {
        try {
            Path filePath = Paths.get(basePath, bucket, objectKey);
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            log.error("文件下载失败: bucket={}, objectKey={}", bucket, objectKey, e);
            throw new RuntimeException("文件下载失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String bucket, String objectKey) {
        try {
            Path filePath = Paths.get(basePath, bucket, objectKey);
            Files.deleteIfExists(filePath);
            log.info("文件删除成功: bucket={}, objectKey={}", bucket, objectKey);
        } catch (IOException e) {
            log.error("文件删除失败: bucket={}, objectKey={}", bucket, objectKey, e);
        }
    }

    @Override
    public String getDefaultBucket() {
        return defaultBucket;
    }
}
