# NowInWiki 客户端 API 对接文档

> **版本：** v1.0（对应后端 Phase 0–4 + 双 Token 鉴权）  
> **仓库：** `nowinwiki-backend`  
> **默认 Base URL：** `http://<host>:8088`  
> **在线文档：** `http://<host>:8088/doc.html`（Knife4j）

本文档面向 **Android 客户端** 开发者，说明如何对接账号、鉴权与云端同步接口。词条内容仍由客户端直连维基百科，**不经过本后端**。

---

## 1. 通用约定

### 1.1 响应包装 `Result<T>`

所有 JSON API 均返回统一结构：

```json
{
  "success": true,
  "errorMsg": null,
  "data": { }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `success` | boolean | `true` 表示业务成功 |
| `errorMsg` | string \| null | 失败时的可读错误信息 |
| `data` | T \| null | 成功时的业务数据 |

客户端应先判断 `success`，再读 `data`。不要仅依赖 HTTP 状态码解析业务错误（部分错误 HTTP 200 + `success: false`）。

### 1.2 HTTP 状态码

| 状态码 | 场景 |
|--------|------|
| `200` | 正常返回（含 `success: false` 的业务失败） |
| `401` | 未登录 / Access 无效；Refresh 接口的鉴权失败（见 §3.4） |
| `400` | 参数校验失败、业务异常（`BusinessException`） |
| `500` | 服务端未捕获异常 |

### 1.3 请求头

| Header | 必填 | 说明 |
|--------|------|------|
| `Content-Type` | POST JSON 时 | `application/json` |
| `authorization` | 需登录接口 | **Access Token**（小写 header 名，与后端一致） |
| `X-Request-Id` | 可选 | Push 幂等 ID，可与 body 中 `requestId` 二选一 |

> 注意：header 名为 `authorization`（全小写），值为 token 字符串本身，**不需要** `Bearer ` 前缀。

### 1.4 时间格式

同步相关字段 `updatedAt`、`serverTime` 使用 **ISO-8601 本地时间**（无时区后缀）：

```text
2026-08-24T12:00:00
```

解析/生成请与 `java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME` 对齐。

### 1.5 冲突策略（同步）

- 采用 **LWW（Last-Write-Wins）**：比较 `updatedAt`，仅当客户端版本 **严格更新** 时才写入。
- 较旧版本会被 **跳过**（`skipped` 计数增加），不报错。
- 删除为 **软删除**：`deleted: true`，服务端记录 `deletedAt`。

---

## 2. 鉴权模型（双 Token）

### 2.1 凭证说明

| 凭证 | 默认有效期 | 用途 |
|------|------------|------|
| **Access Token** | 30 分钟（滑动续期） | 所有需登录 API 的 `authorization` 头 |
| **Refresh Token** | 30 天 | 仅用于 `POST /api/v1/auth/refresh` |

登录成功返回：

```json
{
  "success": true,
  "data": {
    "accessToken": "...",
    "refreshToken": "...",
    "token": "...",
    "user": {
      "id": 123456789,
      "phone": "13800138000",
      "nickName": "wiki_abc123",
      "icon": null
    }
  }
}
```

`token` 与 `accessToken` **相同**，为兼容字段，新客户端请使用 `accessToken` + `refreshToken`。

### 2.2 Access 滑动续期

任意带有效 `authorization` 的请求成功后，服务端会 **延长该 Access Token 的 Redis TTL**（默认再续 30 分钟）。  
客户端在活跃使用期间可不频繁刷新。

### 2.3 Refresh 轮换与重用检测

- 每次调用 `/auth/refresh` 会签发 **新的** Access + Refresh，**旧 Refresh 立即作废**（标记为 `ROTATED`）。
- 若在宽限期（默认 **10 秒**）内用旧 Refresh 重试（如网络丢包），服务端 **幂等返回** 上一轮已签发的新双 Token，不视为攻击。
- 超过宽限期再次使用已轮换的 Refresh → `refresh_reused` → **整次登录会话（family）吊销**，需重新登录。

### 2.4 客户端推荐实现

```text
1. 登录成功 → 持久化 accessToken、refreshToken、user
2. 每个 API 请求 → Interceptor 附加 authorization: accessToken
3. 收到 HTTP 401（或业务失败且为 token 过期）
   → 单飞（Mutex）调用 /auth/refresh
   → 成功：更新本地双 Token，重试原请求
   → 失败：清空会话，跳转登录页
4. 登出 → 调 /auth/logout + 清本地
```

**必须**对 Refresh 做 **并发单飞**，避免多个 401 同时触发多次刷新导致误触发重用检测。

### 2.5 Refresh 错误码（`errorMsg` 前缀）

Refresh 失败时 HTTP **401**，`errorMsg` 形如 `code: 中文说明`：

| code | 含义 | 客户端处理 |
|------|------|------------|
| `refresh_expired` | Refresh 过期或会话已注销 | 清会话，去登录 |
| `refresh_reused` | 检测到重用/会话异常 | 清会话，去登录（可提示「请重新登录」） |
| `refresh_invalid` | Refresh 无效 | 清会话，去登录 |
| `refresh_conflict` | 并发刷新冲突 | 短暂退避后重试一次 refresh |

---

## 3. 账号与鉴权 API

### 3.1 发送验证码

```http
POST /api/v1/auth/code?phone=13800138000
```

- **无需登录**
- 手机号：大陆 11 位（`1` 开头）
- 限流：同一手机号 **45 秒** 内只能发一次
- 验证码有效期：**5 分钟**
- 开发环境（`mock` SMS）：验证码打印在后端日志，不会真发短信

**成功：** `success: true`，`data` 为 `null`

**失败示例：** `errorMsg: "发送过于频繁，请稍后再试"`

---

### 3.2 登录

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "phone": "13800138000",
  "code": "123456"
}
```

- **无需登录**
- 验证码正确后：无用户则自动注册（雪花 ID + 随机昵称 `wiki_` 前缀）
- 返回双 Token + 用户信息（见 §2.1）

**失败示例：** `errorMsg: "验证码错误或已过期"`

---

### 3.3 刷新 Token

```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "<refresh_token>"
}
```

- **无需** `authorization` 头
- **成功 `data`：**

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "token": "..."
}
```

客户端收到后应 **整体替换** 本地 access + refresh（不要继续保留旧 refresh）。

---

### 3.4 登出

```http
POST /api/v1/auth/logout
authorization: <accessToken>
```

- 吊销当前 Access 所属会话（family）
- 成功：`success: true`

---

### 3.5 当前用户

```http
GET /api/v1/users/me
authorization: <accessToken>
```

**成功 `data`：**

```json
{
  "id": 123456789,
  "phone": "13800138000",
  "nickName": "wiki_abc123",
  "icon": null
}
```

无有效 token 时 HTTP **401**（无 JSON body 或空 body，由拦截器直接返回）。

---

### 3.6 注销账号

```http
DELETE /api/v1/users/me
authorization: <accessToken>
```

- 删除该用户云端：收藏夹、收藏、阅读历史、用户设置
- 删除 `wiki_user` 记录
- 吊销所有会话
- **不可恢复**

---

## 4. 收藏同步 API

Base path：`/api/v1/sync/bookmarks`  
**均需登录。**

### 4.1 全量快照

```http
GET /api/v1/sync/bookmarks/snapshot
authorization: <accessToken>
```

**`data` 结构：**

```json
{
  "serverTime": "2026-08-24T12:00:00",
  "folders": [ /* BookmarkFolderSyncDTO[] */ ],
  "bookmarks": [ /* BookmarkSyncDTO[] */ ]
}
```

适用于：首次登录后全量拉取、本地无游标时的重建。

---

### 4.4 收藏夹 CRUD（交互写）

Base path：`/api/v1/folders`  
**均需登录。** 多端对齐仍走 snapshot / pull；点星仍走 bookmark push。

#### 新建

```http
POST /api/v1/folders
authorization: <accessToken>
Content-Type: application/json

{
  "name": "Travel",
  "description": null,
  "sortOrder": null
}
```

`sortOrder` 可空，服务端追加到末尾。返回 `FolderVO`（含服务端 `id`、`updatedAt`、`isDefault`）。

#### 改名 / 改描述

```http
PATCH /api/v1/folders/{id}
authorization: <accessToken>
Content-Type: application/json

{
  "name": "Trips"
}
```

只提交要改的字段。

#### 删除

```http
DELETE /api/v1/folders/{id}
authorization: <accessToken>
```

软删夹及夹内书签。默认夹返回业务错误「默认收藏夹不可删除」。

**`FolderVO`：**

```json
{
  "id": 123,
  "name": "Travel",
  "description": null,
  "sortOrder": 1,
  "isDefault": false,
  "updatedAt": "2026-09-14T16:00:00",
  "deleted": false
}
```

---

### 4.2 增量拉取

```http
GET /api/v1/sync/bookmarks/pull?since=2026-08-24T11:00:00
authorization: <accessToken>
```

- `since`：上次成功同步保存的 `serverTime`（或本地已知的最大 `updatedAt`）
- 返回 `updatedAt > since` **或** `deletedAt > since` 的文件夹与书签
- 响应结构与 snapshot 相同

---

### 4.3 上行 Push

```http
POST /api/v1/sync/bookmarks/push
authorization: <accessToken>
X-Request-Id: optional-uuid
Content-Type: application/json

{
  "requestId": "optional-uuid",
  "folders": [ /* BookmarkFolderSyncDTO[] */ ],
  "bookmarks": [ /* BookmarkSyncDTO[] */ ]
}
```

**`BookmarkFolderSyncDTO`**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | long \| null | 服务端 ID；新建时 null |
| `clientId` | long \| null | 客户端本地文件夹 ID，用于同批次映射 |
| `name` | string | 必填 |
| `description` | string \| null | |
| `sortOrder` | int | 必填 |
| `updatedAt` | string | 必填，ISO 本地时间 |
| `deleted` | boolean | 软删除 |

**`BookmarkSyncDTO`**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | long \| null | 服务端 ID；新建时 null |
| `folderId` | long \| null | 已知服务端文件夹 ID |
| `clientFolderId` | long \| null | 同 push 批次内新建文件夹时用 `clientId` 映射 |
| `title` | string | 必填 |
| `language` | string | 必填，如 `zh`、`en` |
| `bookmarkedAt` | long | 必填，epoch 毫秒 |
| `description` | string \| null | |
| `thumbnailUrl` | string \| null | |
| `updatedAt` | string | 必填 |
| `deleted` | boolean | |

**同批次新建文件夹 + 书签示例：**

```json
{
  "folders": [
    {
      "clientId": 1001,
      "name": "默认",
      "sortOrder": 0,
      "updatedAt": "2026-08-24T12:00:00",
      "deleted": false
    }
  ],
  "bookmarks": [
    {
      "clientFolderId": 1001,
      "title": "Kotlin",
      "language": "zh",
      "bookmarkedAt": 1724486400000,
      "updatedAt": "2026-08-24T12:00:00",
      "deleted": false
    }
  ]
}
```

**成功 `data`：**

```json
{
  "serverTime": "2026-08-24T12:00:01",
  "folderIdMappings": {
    "1001": 9876543210987654321
  },
  "foldersApplied": 1,
  "bookmarksApplied": 1,
  "skipped": 0
}
```

| 字段 | 说明 |
|------|------|
| `folderIdMappings` | `clientId` → 服务端 `folderId` |
| `foldersApplied` / `bookmarksApplied` | 实际写入条数 |
| `skipped` | 因 LWW 过旧而跳过的条数 |

**幂等：** `requestId` 或 `X-Request-Id` 在 **300 秒** 内重复提交会返回 `400`：`重复提交，请稍后重试`。

---

## 5. 阅读历史同步 API

Base path：`/api/v1/sync/history`  
**均需登录。**

### 5.1 全量 / 增量

```http
GET /api/v1/sync/history/snapshot
GET /api/v1/sync/history/pull?since=2026-08-24T11:00:00
```

**`data`：**

```json
{
  "serverTime": "2026-08-24T12:00:00",
  "items": [ /* ReadHistorySyncDTO[] */ ]
}
```

**`ReadHistorySyncDTO`**

| 字段 | 类型 | 说明 |
|------|------|------|
| `title` | string | 必填；与 `language` 组成服务端唯一键 |
| `language` | string | 必填 |
| `viewedAt` | long | 必填，epoch 毫秒 |
| `description` | string \| null | |
| `thumbnailUrl` | string \| null | |
| `updatedAt` | string | 必填 |
| `deleted` | boolean | |

服务端每用户最多保留 **200** 条（超出时 push 响应 `trimmed > 0`）。

### 5.2 Push

```http
POST /api/v1/sync/history/push
authorization: <accessToken>
Content-Type: application/json

{
  "requestId": "optional-uuid",
  "items": [ /* ReadHistorySyncDTO[] */ ]
}
```

**成功 `data`：**

```json
{
  "serverTime": "2026-08-24T12:00:01",
  "itemsApplied": 3,
  "skipped": 0,
  "trimmed": 0
}
```

---

## 6. 用户设置同步 API

Base path：`/api/v1/sync/preferences`  
**均需登录。**

每用户 **一条** 设置记录；`payloadJson` 为不透明 JSON 字符串，建议与 Android `UserData` 序列化格式一致。

### 6.1 全量 / 增量

```http
GET /api/v1/sync/preferences/snapshot
GET /api/v1/sync/preferences/pull?since=2026-08-24T11:00:00
```

**`data`：**

```json
{
  "serverTime": "2026-08-24T12:00:00",
  "preference": {
    "payloadJson": "{\"themeBrand\":\"DEFAULT\",...}",
    "updatedAt": "2026-08-24T12:00:00",
    "deleted": false
  }
}
```

无记录时 `preference` 可能为 `null`。

### 6.2 Push

```http
POST /api/v1/sync/preferences/push
authorization: <accessToken>
Content-Type: application/json

{
  "requestId": "optional-uuid",
  "preference": {
    "payloadJson": "{...}",
    "updatedAt": "2026-08-24T12:00:00",
    "deleted": false
  }
}
```

**成功 `data`：**

```json
{
  "serverTime": "2026-08-24T12:00:01",
  "applied": true,
  "skipped": false
}
```

---

## 7. 推荐同步流程（客户端）

### 7.1 登录后首次同步

```text
1. POST /auth/login → 保存双 Token
2. GET /sync/bookmarks/snapshot
3. GET /sync/history/snapshot
4. GET /sync/preferences/snapshot
5. 与本地数据合并（LWW），保存各模块 serverTime
```

### 7.2 周期性 / 前台恢复

```text
1. 若有本地待上传变更 → push（带 requestId）
2. pull?since=<lastServerTime> 拉增量
3. 合并并更新 lastServerTime
```

### 7.3 多设备

- 以 `updatedAt` LWW 为准，无设备向量时钟。
- 后写入且时间戳更新的版本胜出。
- 客户端时钟不准会导致异常覆盖，建议用 **单调时钟** 或 NTP 校准后的时间。

---

## 8. 本地开发

### 8.1 启动后端

```bash
cd nowinwiki-backend
docker compose up -d
mvn -pl nowinwiki-core-service -am spring-boot:run
```

### 8.2 Android 模拟器访问本机

| 环境 | Base URL |
|------|----------|
| Android Emulator | `http://10.0.2.2:8088` |
| 真机（同 WiFi） | `http://<电脑局域网IP>:8088` |

### 8.3 Mock 验证码

默认 `nowinwiki.sms.mode=mock`，发送验证码后查看后端控制台日志中的 6 位数字。

### 8.4 健康检查

```http
GET /actuator/health
```

---

## 9. 不在本后端范围内的能力

| 能力 | 说明 |
|------|------|
| 维基词条搜索/正文 | 客户端直连 Wikipedia / MediaWiki API |
| 推送通知（FCM） | Phase 5，尚未实现 |
| 第三方登录（微信/QQ） | 未实现 |
| 修改密码 | 仅手机号验证码登录，无密码 |

---

## 10. 接口一览

| Method | Path | 登录 |
|--------|------|------|
| POST | `/api/v1/auth/code` | 否 |
| POST | `/api/v1/auth/login` | 否 |
| POST | `/api/v1/auth/refresh` | 否 |
| POST | `/api/v1/auth/logout` | 是 |
| GET | `/api/v1/users/me` | 是 |
| DELETE | `/api/v1/users/me` | 是 |
| GET | `/api/v1/sync/bookmarks/snapshot` | 是 |
| GET | `/api/v1/sync/bookmarks/pull` | 是 |
| POST | `/api/v1/sync/bookmarks/push` | 是 |
| POST | `/api/v1/folders` | 是 |
| PATCH | `/api/v1/folders/{id}` | 是 |
| DELETE | `/api/v1/folders/{id}` | 是 |
| GET | `/api/v1/sync/history/snapshot` | 是 |
| GET | `/api/v1/sync/history/pull` | 是 |
| POST | `/api/v1/sync/history/push` | 是 |
| GET | `/api/v1/sync/preferences/snapshot` | 是 |
| GET | `/api/v1/sync/preferences/pull` | 是 |
| POST | `/api/v1/sync/preferences/push` | 是 |

---

## 11. 变更记录

| 日期 | 说明 |
|------|------|
| 2026-08-25 | 初版：双 Token + family + Grace Period；收藏/历史/设置同步 |
| 2026-09-14 | 收藏夹交互 CRUD：`POST/PATCH/DELETE /api/v1/folders` |

如有接口变更，以 `nowinwiki-backend` 仓库与本文件为准；Swagger 文档为辅助参考。
