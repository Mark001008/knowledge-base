package com.ma.kb.start.config;

import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceWarmupConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceWarmupConfig.class);

    @Bean
    public ApplicationRunner dataSourceWarmupRunner(DataSource dataSource) {
        return args -> {
            long startedAt = System.currentTimeMillis();
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1");
                log.info("数据库连接预热完成，耗时 {} ms", System.currentTimeMillis() - startedAt);
            } catch (Exception e) {
                log.warn("数据库连接预热失败，后续请求会继续尝试连接: {}", e.getMessage());
            }
        };
    }
}
