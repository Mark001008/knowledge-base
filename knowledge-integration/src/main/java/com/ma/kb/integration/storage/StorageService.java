package com.ma.kb.integration.storage;

import java.io.InputStream;

/**
 * 文件存储服务接口
 */
public interface StorageService {

    /**
     * 上传文件
     *
     * @param bucket    存储桶
     * @param objectKey 对象键
     * @param inputStream 文件流
     * @param fileSize  文件大小
     * @param contentType 内容类型
     * @return 存储路径
     */
    String upload(String bucket, String objectKey, InputStream inputStream,
                  long fileSize, String contentType);

    /**
     * 下载文件
     */
    InputStream download(String bucket, String objectKey);

    /**
     * 删除文件
     */
    void delete(String bucket, String objectKey);

    /**
     * 获取默认存储桶
     */
    String getDefaultBucket();
}
