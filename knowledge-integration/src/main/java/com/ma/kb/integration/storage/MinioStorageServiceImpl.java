package com.ma.kb.integration.storage;

import io.minio.*;
import io.minio.errors.MinioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;

/**
 * MinIO 对象存储实现
 */
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "minio")
public class MinioStorageServiceImpl implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageServiceImpl.class);

    @Value("${storage.minio.endpoint}")
    private String endpoint;

    @Value("${storage.minio.access-key}")
    private String accessKey;

    @Value("${storage.minio.secret-key}")
    private String secretKey;

    @Value("${storage.minio.bucket:kb-documents}")
    private String defaultBucket;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        ensureBucketExists(defaultBucket);
        log.info("MinIO 存储初始化完成: endpoint={}, bucket={}", endpoint, defaultBucket);
    }

    @Override
    public String upload(String bucket, String objectKey, InputStream inputStream,
                         long fileSize, String contentType) {
        try {
            ensureBucketExists(bucket);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(inputStream, fileSize, -1)
                    .contentType(contentType)
                    .build());
            log.info("MinIO 文件上传成功: bucket={}, objectKey={}, size={}", bucket, objectKey, fileSize);
            return bucket + "/" + objectKey;
        } catch (Exception e) {
            log.error("MinIO 文件上传失败: bucket={}, objectKey={}", bucket, objectKey, e);
            throw new RuntimeException("MinIO 文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream download(String bucket, String objectKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            log.error("MinIO 文件下载失败: bucket={}, objectKey={}", bucket, objectKey, e);
            throw new RuntimeException("MinIO 文件下载失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String bucket, String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
            log.info("MinIO 文件删除成功: bucket={}, objectKey={}", bucket, objectKey);
        } catch (Exception e) {
            log.error("MinIO 文件删除失败: bucket={}, objectKey={}", bucket, objectKey, e);
        }
    }

    @Override
    public String getDefaultBucket() {
        return defaultBucket;
    }

    private void ensureBucketExists(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucket)
                    .build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucket)
                        .build());
                log.info("MinIO 创建存储桶: {}", bucket);
            }
        } catch (Exception e) {
            throw new RuntimeException("MinIO 存储桶检查失败: " + e.getMessage(), e);
        }
    }
}
