# NowInWiki 用户后端 — 开发大纲（Living Document）

> **状态：** v0.3（Phase 0–4 后端 MVP 已完成，待 Android 接入）  
> **仓库：** `D:\Documents\GitHub\nowinwiki-backend`（与 Android 客户端分离）  
> **技术栈：** Java 17 + Spring Boot 3.3 + MyBatis-Plus + MySQL + Redis  
> **参考架构：** `hmdp-plus-master`（选择性复用，非全盘照搬）  
> **客户端：** NowInWiki Android（Kotlin / Retrofit / Room）  
> **不在范围内：** 维基正文代理（继续直连 MediaWiki / Wikipedia API）

---

## 0. 文档维护约定

| 字段 | 说明 |
|------|------|
| 更新时机 | 每完成一个 Phase、API 定稿、表结构变更时更新 |
| 勾选规则 | `[x]` 已完成，`[~]` 进行中，`[ ]` 未开始 |
| 决策记录 | 见文末 **附录 D — 决策日志** |

---

## 1. 目标与边界

### 1.1 要做什么

为 NowInWiki 提供**用户侧后端**，支撑：

1. **账号与鉴权**（手机号 + 验证码登录，或后续扩展 OAuth）
2. **用户数据云同步**（收藏夹 / 收藏条目 / 阅读历史）
3. **（可选 Phase 2+）** 设置同步、设备管理、推送订阅

### 1.2 不做什么（第一期）

- 不代理、不缓存 Wikipedia 正文 HTML
- 不上秒杀 / 分库分表 / Kafka 全链路（除非后期规模明确需要）
- 不做 Web 管理后台（优先服务 Android App）

### 1.3 与 App 现有模型对齐

| App 本地模型 | 后端对应 |
|--------------|----------|
| `WikiBookmarkFolder` | `bookmark_folder` |
| `WikiBookmark` | `bookmark`（`folder_id + language + title` 唯一） |
| `WikiHistoryEntity` | `read_history`（`language + title` 唯一，按 `viewed_at` 更新） |
| `UserData`（主题/字号等） | Phase 2 `user_preference` |

---

## 2. hmdp-plus 架构复用评估

### 2.1 模块对照

| hmdp-plus 模块 | 是否复用 | 用途映射 |
|----------------|----------|----------|
| `hmdp-common` | ✅ 部分 | 异常体系、`BaseCode`、常量、工具类 |
| `hmdp-parameter` | ✅ 推荐 | DTO / VO 独立模块（`nowinwiki-parameter`） |
| `hmdp-core-service` | ✅ 模式 | 业务单体 `nowinwiki-core-service` |
| `hmdp-redis-tool-framework` | ✅ 部分 | `RedisCache`、`RedisKeyBuild`、Key 规范 |
| `hmdp-redisson-framework` | ⚠️ 按需 | 分布式锁 / 幂等：同步冲突写路径可用 |
| `hmdp-repeat-execute-limit` | ✅ 推荐 | 同步接口防重复提交 |
| `hmdp-id-generator-framework` | ✅ 推荐 | 雪花 ID（用户、文件夹、书签） |
| `hmdp-redis-rate-limit` | ⚠️ 按需 | 登录 / 同步接口限流（可简化版） |
| `hmdp-mq-framework` (Kafka) | ❌ 一期不用 | 无异步重业务；后期推送可用 |
| `hmdp-sharding` | ❌ 一期不用 | 用户数据量级不需要 |
| 布隆过滤器 + 多层缓存 | ❌ 一期不用 | 读模型简单，无热点穿透场景 |
| Outbox + 对账 | ❌ 一期不用 | 无订单 / 库存一致性需求 |

### 2.2 可直接借鉴的模式

1. **统一响应** `Result<T>`（`success` / `errorMsg` / `data`）
2. **登录链路** `sendCode` → Redis 存码 → `login` → token → Redis Hash 存 `UserDTO`
3. **拦截器链** `RefreshTokenInterceptor`（order=0）+ `LoginInterceptor`（order=1）
4. **MyBatis-Plus** `ServiceImpl` + `lambdaQuery` 风格
5. **Redis Key 规范** `RedisKeyManage` + `RedisKeyBuild`
6. **Maven 多模块** + `flatten-maven-plugin` + Spring Boot 3.5

### 2.3 需要改造的点（相对 hmdp）

| 项 | hmdp 现状 | NowInWiki 改造 |
|----|-----------|----------------|
| 数据源 | ShardingSphere 分库分表 | 单库 MySQL 即可 |
| 登录返回 | token 在 header / 业务约定 | 明确 `Authorization: token` + App Retrofit Interceptor |
| 用户标识 | `UserHolder` ThreadLocal | 保留；补充 `device_id` 用于同步 |
| 验证码 | 日志打印（教学） | 生产不接短信一期可保留 mock；二期接阿里云等 |
| 包名 | `org.javaup` | `com.analoggpixel.nowinwiki`（与 `applicationId` 一致） |

---

## 3. 目标工程结构（新建仓库建议）

```
nowinwiki-backend/                     # 已实现（v0.1）
├── pom.xml
├── nowinwiki-common/                  # 常量、工具、雪花 ID、异常
├── nowinwiki-parameter/               # DTO / VO / Result
├── nowinwiki-core-service/            # 主应用
│   ├── sms/                           # MockSmsSender + AliyunSmsSender
│   ├── security/                      # 拦截器 + UserHolder
│   └── resources/db/migration/        # Flyway
├── docker-compose.yml
└── README.md
```

> **与初稿差异：** `nowinwiki-redis-framework` / `nowinwiki-id-generator` 暂未独立模块，Redis Key 与雪花 ID 放在 `common`；后续 Phase 2 可按需拆分。

**依赖中间件（一期）：** MySQL 8、Redis 6+  
**依赖中间件（二期+）：** Kafka / FCM 推送

---

## 4. 分阶段交付计划

### Phase 0 — 脚手架 [x]

- [x] 创建 Maven 多模块工程，JDK 17，Spring Boot 3.3
- [x] 从 hmdp 迁移：`Result`、异常基类、Redis 常量
- [x] 接入 MyBatis-Plus、Flyway、Knife4j（API 文档）
- [x] 本地 `docker-compose`：MySQL + Redis
- [x] 健康检查 `/actuator/health`

**验收：** `mvn clean package` 通过；启动后访问 `/doc.html`、`/actuator/health`。

---

### Phase 1 — 账号与鉴权 [x]（后端侧；App 未接）

#### API（已定稿）

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/api/v1/auth/code?phone=` | 否 | 发送验证码（45s 限流） |
| POST | `/api/v1/auth/login` | 否 | 登录，返回 `{ token, user }` |
| POST | `/api/v1/auth/logout` | 是 | 注销 token |
| GET | `/api/v1/users/me` | 是 | 当前用户信息 |

请求头：`authorization: <token>`（小写，与 hmdp 一致）

#### 短信双实现

| 模式 | 配置 | 行为 |
|------|------|------|
| mock（默认） | `nowinwiki.sms.mode=mock` | 验证码打印到日志 |
| aliyun | `spring.profiles.active=aliyun` + 环境变量 | 阿里云按量付费发短信 |

#### 表结构（已实现）

```sql
wiki_user: id, phone UNIQUE, nick_name, icon, create_time, update_time
```

> **与草案差异：** 未拆 `user_phone` 表，一期单表足够。

#### 任务

- [x] 迁移 hmdp 登录流程（`AuthServiceImpl`）
- [x] `WebMvcConfig` 拦截器 + 白名单
- [x] 验证码发送限流（Redis `setIfAbsent` 45s）
- [x] Mock + Aliyun 两套 `SmsSender`
- [ ] App 侧：`AuthInterceptor` 注入 `Authorization` header

**验收：** App 可登录，带 token 访问 `/users/me` 成功，无 token 401。

---

### Phase 2 — 收藏夹同步 [x]（后端）

#### 同步策略

- LWW：`updatedAt`（`ISO_LOCAL_DATE_TIME`）较大者胜出
- 软删除：`deleted=true` → `deleted_at` tombstone
- 首次 push 支持 `clientId` / `clientFolderId` 映射服务端 folder id
- 幂等：Redis `sync:push:bookmarks:{userId}:{requestId}`（`X-Request-Id` 或 body `requestId`）

#### API（已定稿）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/sync/bookmarks/snapshot` | 全量快照 |
| POST | `/api/v1/sync/bookmarks/push` | 上行变更 |
| GET | `/api/v1/sync/bookmarks/pull?since=` | 增量拉取 |

#### 表结构（已实现）

`bookmark_folder`、`bookmark`（Flyway `V2__bookmark_sync.sql`）

#### 任务

- [x] Entity / Mapper / Service / Controller
- [x] Push 幂等（Redis，非 hmdp `@RepeatExecuteLimit`）
- [ ] Room 侧 `SyncRepository` + WorkManager
- [ ] 未登录纯本地；登录后合并策略

---

### Phase 3 — 阅读历史同步 [x]（后端）

- [x] 表 `read_history`（`user_id, language, title` PK）
- [x] `/api/v1/sync/history/snapshot|pull|push`
- [x] 服务端每用户最多 200 条（`nowinwiki.sync.history-max-rows`）

---

### Phase 4 — 设置同步 [x]（后端）

- [x] `user_preference` JSON 列（`payload` + `updated_at`）
- [x] `/api/v1/sync/preferences/snapshot|pull|push`
- [x] `payloadJson`  opaque JSON，Android 侧对齐 `UserData` 序列化
- [x] `DELETE /api/v1/users/me` 注销账号并删除云端数据

---

### Phase 5 — 推送与运营（可选）[ ]

- [ ] FCM token 注册
- [ ] 关注词条变更通知（需 Wikipedia EventStreams 或轮询服务）
- [ ] 此时可考虑 Kafka + 延迟队列（可参考 hmdp 延迟通知）

---

## 5. 非功能需求

| 类别 | 一期目标 |
|------|----------|
| 安全 | HTTPS、token 过期刷新、验证码 TTL、接口限流 |
| 性能 | 同步接口 P99 < 300ms（单用户百级收藏） |
| 可观测 | 结构化日志、Micrometer 基础指标 |
| 部署 | 单机 Docker / 云服务器；无 K8s 要求 |
| 合规 | 注销账号 `DELETE /users/me`；导出待 Android 侧 |

---

## 6. Android 客户端改造清单（并行跟踪）

| 模块 | 改动 |
|------|------|
| `core/network` | 新增 `NowInWikiApi` Retrofit 接口，`BACKEND_URL` 配置 |
| `core/data` | `RemoteBookmarkRepository` + 本地优先合并 |
| `feature/settings` | 登录 / 登出 UI |
| `app` | `AuthInterceptor`、登录态 `DataStore` |
| 离线 | 未登录零影响；登录后后台同步 |

---

## 7. 风险与待定项

| # | 项 | 状态 | 备注 |
|---|-----|------|------|
| R1 | 短信服务商选型 | **已定：阿里云按量** | dev 用 mock；prod 用 `aliyun` profile |
| R2 | token 格式 | **已定：随机 UUID** | Redis Hash 存 UserDTO，沿用 hmdp |
| R3 | 本地数据合并策略 | 待定 | 登录时全并 / 选夹合并 |
| R4 | 是否独立仓库 | **已定：独立** | `D:\Documents\GitHub\nowinwiki-backend` |
| R5 | hmdp 代码 LICENSE | 需确认 | 借鉴模式，非直接拷贝 |

---

## 附录 A — 环境变量

```yaml
# application.yml 占位
MYSQL_URL: jdbc:mysql://localhost:3306/nowinwiki
REDIS_HOST: 127.0.0.1
REDIS_PASSWORD: ...
```

App `local.properties`:

```properties
BACKEND_URL=https://api.example.com/
```

---

## 附录 B — 从 hmdp 迁移文件清单（Phase 0/1）

| 源路径（hmdp） | 目标 | 改动量 |
|----------------|------|--------|
| `dto/Result.java` | `nowinwiki-parameter` | 小 |
| `utils/LoginInterceptor.java` | core-service | 小 |
| `utils/RefreshTokenInterceptor.java` | core-service | 小 |
| `config/MvcConfig.java` | core-service | 中（改白名单） |
| `service/impl/UserServiceImpl.java` | core-service | 中 |
| `redis/RedisCache*.java` | redis-framework | 小 |
| `SnowflakeIdGenerator` | id-generator | 小 |

---

## 附录 C — 决策日志（摘要）

> Android 仓完整决策台账：`nowinandroid/docs/nowinwiki-backend-decisions.md`（若本仓有拷贝则以 Android 仓为准）。

| 日期 | 决策 | 原因 |
|------|------|------|
| 2026-08-24 | 技术栈 Java + Spring Boot | 与 hmdp 复用、用户熟悉 |
| 2026-08-24 | 一期不用 Kafka / 分库分表 | 业务非高并发交易 |
| 2026-08-24 | 正文不走后端 | 合规与复杂度 |
| 2026-08-24 | 客户端与后端分仓 | Android `nowinandroid`，后端 `nowinwiki-backend` |
| 2026-08-24 | 短信 mock + aliyun 双实现 | dev 不打真实短信；prod 阿里云按量付费 |
| 2026-08-24 | Phase 2/3/4 后端完成 | 收藏/历史/设置同步 + 账号注销 |
| 2026-09 | 同步 LWW + 软删 + push 幂等 | 多端冲突与弱网重试 |
| 2026-09 | App：云端权威 / cacheThenNetwork / 乐观写+回滚 | 见 Android 决策文档 |

---

## 附录 D — 下一步（Immediate Actions）

1. [x] 独立仓库 `nowinwiki-backend`
2. [x] mock + 阿里云短信双实现
3. [x] Phase 0–4 后端 MVP
4. [ ] 本地启动验证：`docker compose up -d` + `mvn -pl nowinwiki-core-service spring-boot:run`
5. [ ] Android 接入（Retrofit、登录 UI、各 SyncRepository）
6. [ ] Phase 5：FCM 推送（可选）
