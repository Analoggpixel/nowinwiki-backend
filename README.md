# NowInWiki Backend

NowInWiki Backend 为 Android 客户端提供账号、收藏夹、收藏内容、浏览历史和用户偏好同步接口。

当前服务只用于本地开发和联调，默认使用 Mock 短信服务，不会发送真实短信。

## 运行前准备

- JDK 17 或更高版本
- Maven
- Docker Desktop
- 可用的 `3307`、`6379` 和 `8088` 端口

## 启动服务

### 1. 启动 MySQL 和 Redis

在仓库根目录执行：

```bash
docker compose up -d
```

Docker Compose 会启动：

| 服务 | 宿主机端口 | 容器端口 |
| --- | ---: | ---: |
| MySQL 8 | 3307 | 3306 |
| Redis 7 | 6379 | 6379 |

### 2. 启动 Spring Boot

```bash
mvn -pl nowinwiki-core-service -am spring-boot:run
```

服务默认监听 `8088` 端口。

启动后可以访问：

- 健康检查：<http://localhost:8088/actuator/health>
- Knife4j 文档：<http://localhost:8088/doc.html>
- OpenAPI 文档：<http://localhost:8088/v3/api-docs>

### 3. 构建项目

```bash
mvn clean package
```

## 默认配置

默认配置位于：

```text
nowinwiki-core-service/src/main/resources/application.yml
```

默认连接信息如下：

```text
MySQL: 127.0.0.1:3307
Redis: 127.0.0.1:6379
HTTP: 0.0.0.0:8088
```

可以通过环境变量覆盖默认值：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `MYSQL_HOST` | `127.0.0.1` | MySQL 地址 |
| `MYSQL_PORT` | `3307` | MySQL 宿主机端口 |
| `MYSQL_USER` | `root` | MySQL 用户名 |
| `MYSQL_PASSWORD` | `root` | MySQL 密码 |
| `REDIS_HOST` | `127.0.0.1` | Redis 地址 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `REDIS_PASSWORD` | 空 | Redis 密码 |
| `REDIS_DB` | `0` | Redis 数据库编号 |

Flyway 会在应用启动时自动执行 `nowinwiki-core-service/src/main/resources/db/migration` 下的数据库迁移。

## 短信验证码

默认配置为 Mock 模式：

```yaml
nowinwiki:
  sms:
    mode: mock
```

调用发送验证码接口后，验证码会打印在 Spring Boot 日志中：

```text
[mock-sms] phone=13800138000 code=123456
```

如果需要接入阿里云短信，将短信模式改为 `aliyun`，并配置以下环境变量：

```text
ALIYUN_SMS_ACCESS_KEY_ID
ALIYUN_SMS_ACCESS_KEY_SECRET
ALIYUN_SMS_SIGN_NAME
ALIYUN_SMS_TEMPLATE_CODE
```

不要把真实 AccessKey、密码或其他密钥写入配置文件或提交到 Git。

## 接口概览

需要登录的接口使用请求头：

```text
authorization: <accessToken>
```

主要接口包括：

| 功能 | 接口 |
| --- | --- |
| 发送验证码 | `POST /api/v1/auth/code?phone=` |
| 登录 | `POST /api/v1/auth/login` |
| 刷新 Token | `POST /api/v1/auth/refresh` |
| 当前用户 | `GET /api/v1/users/me` |
| 收藏同步 | `/api/v1/sync/bookmarks/*` |
| 浏览历史同步 | `/api/v1/sync/history/*` |
| 用户偏好同步 | `/api/v1/sync/preferences/*` |

完整请求参数、返回结构和错误示例见 [`docs/client-api.md`](docs/client-api.md)。

## 项目结构

```text
nowinwiki-common/          通用常量和工具
nowinwiki-parameter/       请求参数、响应对象和校验模型
nowinwiki-core-service/    Spring Boot 启动模块、Controller、Service 和数据访问
docker-compose.yml         MySQL 和 Redis 本地开发环境
```

## 测试

运行后端单元测试：

```bash
mvn test
```

如果启动失败，先检查 Docker 容器状态：

```bash
docker compose ps
```

## 常见问题

### 端口 3306 被占用

本项目不会把 MySQL 映射到宿主机 `3306`，默认使用 `3307`。如果修改了 Docker 端口映射，也要同步修改 `MYSQL_PORT`。

### Android 无法连接后端

确认 Spring Boot 已监听 `8088`，并根据设备类型配置 Android 的 `BACKEND_URL`：模拟器使用 `10.0.2.2:8088`，真机使用电脑局域网 IP 或 `adb reverse`。

### 看不到验证码

验证码只会在发送验证码请求成功后打印，不会在服务启动时自动打印。请查看处理请求的 Spring Boot Run 窗口。

### 数据库表不存在

确认 MySQL 容器已启动，并检查 Spring Boot 启动日志中的 Flyway 迁移结果。

## 相关项目

- Android 客户端：[NowInWiki](https://github.com/Analoggpixel/NowInWiki)
- Wikipedia REST API：<https://en.wikipedia.org/api/rest_v1/>
