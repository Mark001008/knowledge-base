# AGENTS.md

本项目是 AI 知识库后端，Spring Boot 3.4 + Java 21，Maven 多模块分层架构。开发时按模块分层落代码，不要把业务实现放回 `knowledge-start`。

## 命令

```bash
./mvnw compile
./mvnw test
./mvnw -pl knowledge-start -am -DskipTests package
java -jar knowledge-start/target/knowledge-start-0.0.1-SNAPSHOT.jar
```

> 项目使用 Maven Wrapper（`./mvnw`），已在 `.mvn/jvm.config` 中绑定 Java 21。系统默认 `mvn` 仍为 Java 8，项目内统一用 `./mvnw`。

## 模块职责

- `knowledge-start`：Spring Boot 启动类、Controller、全局异常处理、Security 配置、JWT 过滤器。
- `knowledge-trigger`：消息消费、定时任务、Job 入口，只做触发转发。
- `knowledge-service`：服务接口、DTO、服务实现，编排调用 `core`。
- `knowledge-core`：业务规则、业务校验、业务流程编排、JWT 服务、Spring Security 集成。
- `knowledge-manager`：封装 DAL mapper、非关系型数据访问、DO 到 BO 转换，不开启事务。
- `knowledge-dal`：mapper、mapper.xml、DO，单表持久化操作。
- `knowledge-integration`：外部 HTTP/RPC/SDK 调用和外部 DTO 转换。
- `knowledge-common`：统一响应（`ApiResponse`）、异常（`BusinessException`/`ErrorCode`）、枚举、工具。
- `knowledge-tests`：所有单元测试统一存放，依赖 `spring-boot-starter-test` + Mockito。

## 依赖方向

```text
start -> trigger/service
trigger -> service
service -> core
core -> manager/integration
manager -> dal
common 被各层依赖
tests -> start
```

## 当前能力

- 认证：`POST /api/auth/login`、`GET /api/auth/me`（JWT Bearer Token）
- 健康检查：`GET /api/system/health`、`GET /health.html`
- 统一响应封装：`ApiResponse`
- 统一异常处理：`BusinessException` / `ErrorCode` / `GlobalExceptionHandler`
- 用户体系：`UserDO` / `RoleDO` / `UserRoleDO`、`UserMapper`、`UserManager`、`AuthService`
- JWT：`JwtService`（生成/解析/校验 Token）
- 测试：50 个单元测试，覆盖 common / core / manager / service / start 各层

## 测试规范

- 所有测试类放在 `knowledge-tests` 模块，包路径与被测类保持一致。
- 使用 JUnit 5 + Mockito，纯单元测试不依赖数据库和 Spring 容器。
- Controller 层测试等配好测试数据库后再补充。

## 数据访问约束

- 业务对象以 `***BO` 命名。
- DAL 数据模型对象以 `***DO` 命名。
- DAL 方法名以 `insert`、`delete`、`update`、`select` 开头。
- DAL 只能单表操作，不能出现联表操作。
- `mapper.xml` 每个方法必须加注释，表名必须起别名。
- `mapper.xml` 尽量避免大量动态条件。
