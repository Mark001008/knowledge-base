# Knowledge Base Backend

AI 知识库后端。基于 Spring Boot 3.4 + Java 21 构建，采用 Maven 多模块分层架构。

## 模块结构

```text
knowledge-base-parent
├── knowledge-start        # 应用启动和组装层：启动类、HTTP Controller、配置、健康页
├── knowledge-trigger      # 触发入口层：消息消费、定时任务、Job 入口
├── knowledge-service      # 服务接口和实现层：DTO、服务编排、异常边界
├── knowledge-core         # 核心业务层：业务规则、校验、流程编排
├── knowledge-manager      # 数据管理层：封装 DAL mapper、DO 到 BO 转换、非关系型数据访问
├── knowledge-dal          # 数据库持久层：mapper、mapper.xml、DO 单表操作
├── knowledge-integration  # 外部系统集成层：模型、向量库、对象存储、文档解析等
├── knowledge-common       # 通用能力层：响应、异常、配置属性、工具、枚举
└── knowledge-tests        # 单元测试、集成测试（统一存放所有测试类）
```

依赖方向：

```text
start -> trigger/service
trigger -> service
service -> core
core -> manager/integration
manager -> dal
common 被各层依赖
tests -> start
```

## 当前接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/login` | 用户登录 |
| GET | `/api/auth/me` | 获取当前用户信息（需 Bearer Token） |
| GET | `/api/system/health` | 系统健康检查 |
| GET | `/health.html` | 静态健康页 |

## 技术栈

- Java 21 + Spring Boot 3.4.4
- Spring Security + JWT（jjwt 0.12.6）
- MyBatis-Plus 3.5.9
- MapStruct 1.6.3
- Maven Wrapper（项目内置，无需额外安装 Maven）

## 运行

```bash
# 编译
./mvnw compile

# 测试
./mvnw test

# 打包
./mvnw -pl knowledge-start -am -DskipTests package

# 启动
java -jar knowledge-start/target/knowledge-start-0.0.1-SNAPSHOT.jar
```

> IDEA 中将 Project SDK 设置为 Java 21 即可，终端默认 `mvn` 仍使用 Java 8，项目内统一使用 `./mvnw`。

本机私有配置可放在 `knowledge-start/src/main/resources/application-local.yml`，该文件已被 `.gitignore` 忽略。

## 开发约束

- `start` 只放启动、配置、Controller 和协议适配。
- `trigger` 只做触发接入、参数校验和入口转发。
- `service` 放对外服务接口、DTO、服务实现和对 `core` 的调用编排。
- `core` 承载业务规则、业务校验和业务流程编排。
- `manager` 封装 DAL mapper，不开启事务，非特殊情况不捕获异常。
- `dal` 只做单表操作，方法以 `insert`、`delete`、`update`、`select` 开头。
- `integration` 只做外部系统调用和响应转换，不承载业务决策。
- `common` 不包含具体业务流程。
- 测试类统一放在 `knowledge-tests` 模块，不在各业务模块中写测试。
