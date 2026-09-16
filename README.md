# NowInWiki Backend

NowInWiki Backend 是 NowInWiki Android 客户端的用户数据服务，提供登录鉴权、收藏同步、浏览历史同步和用户偏好同步。

## 功能

- 手机号验证码登录
- Access Token 和 Refresh Token 管理
- 收藏夹与收藏内容同步
- 浏览历史同步
- 用户偏好同步
- Flyway 数据库迁移
- Knife4j / OpenAPI 接口文档
- Mock 和阿里云两种短信模式

## 技术栈

- Java 17
- Spring Boot 3.3
- MySQL 8
- Redis 7
- MyBatis-Plus
- Flyway
- Knife4j
- Maven

## 环境要求

- JDK 17 或更高版本
- Maven
- MySQL 8
- Redis 7

仓库提供了 `docker-compose.yml`，可用于启动本地 MySQL 和 Redis。

## 启动项目

### 1. 启动 MySQL 和 Redis

使用项目提供的 Docker Compose 配置：

```bash
docker compose up -d
```

如果已经有可用的 MySQL 和 Redis，可以跳过这一步，并通过环境变量配置连接信息。

### 2. 启动后端

```bash
mvn -pl nowinwiki-core-service -am spring-boot:run
```

服务启动后默认监听 `8088` 端口：

- 健康检查：<http://localhost:8088/actuator/health>
- Knife4j：<http://localhost:8088/doc.html>
- OpenAPI：<http://localhost:8088/v3/api-docs>

### 3. 构建项目

```bash
mvn clean package
```

## 配置

主要配置位于：

```text
nowinwiki-core-service/src/main/resources/application.yml
```

项目默认配置：

| 配置项 | 默认值 |
| --- | --- |
| HTTP 端口 | `8088` |
| MySQL 地址 | `127.0.0.1:3307` |
| MySQL 数据库 | `nowinwiki` |
| MySQL 用户名 | `root` |
| MySQL 密码 | `root` |
| Redis 地址 | `127.0.0.1:6379` |
| Redis 数据库 | `0` |

可以使用以下环境变量覆盖默认连接信息：

```text
MYSQL_HOST
MYSQL_PORT
MYSQL_USER
MYSQL_PASSWORD
REDIS_HOST
REDIS_PORT
REDIS_PASSWORD
REDIS_DB
```

Flyway 会在应用启动时自动执行 `nowinwiki-core-service/src/main/resources/db/migration` 下的数据库迁移。

## 短信验证码

默认使用 Mock 短信模式。调用发送验证码接口后，可以在 Spring Boot 日志中找到验证码：

```text
[mock-sms] phone=13800138000 code=123456
```

阿里云短信需要配置：

```text
ALIYUN_SMS_ACCESS_KEY_ID
ALIYUN_SMS_ACCESS_KEY_SECRET
ALIYUN_SMS_SIGN_NAME
ALIYUN_SMS_TEMPLATE_CODE
```

密钥和生产环境密码应通过环境变量提供，不要提交到 Git。

## 接口文档

需要登录的接口使用以下请求头：

```text
authorization: <accessToken>
```

完整的接口地址、请求参数、返回结构和错误示例见 [`docs/client-api.md`](docs/client-api.md)。服务启动后也可以通过 Knife4j 查看和调试接口。

## 项目结构

```text
nowinwiki-common/          通用常量和工具
nowinwiki-parameter/       请求参数、响应模型和校验对象
nowinwiki-core-service/    Controller、Service、数据访问和应用入口
docs/                      接口与设计文档
docker-compose.yml         本地 MySQL 和 Redis 配置
```

## 测试

```bash
mvn test
```

## 常见问题

### 后端无法连接 MySQL 或 Redis

检查服务是否已经启动，并确认环境变量中的地址、端口、用户名和密码与实际配置一致。

### 端口被占用

修改 `docker-compose.yml` 中的端口映射后，同步修改对应的环境变量。HTTP 端口由 `application.yml` 中的 `server.port` 配置。

### 看不到 Mock 验证码

验证码只会在发送验证码请求成功后写入后端日志。先确认手机号格式正确，并检查 Spring Boot 的运行窗口。

### 数据库表不存在

检查 MySQL 连接和 Flyway 启动日志，确认数据库迁移已经成功执行。

## Android 客户端

[NowInWiki Android](https://github.com/Analoggpixel/NowInWiki)
