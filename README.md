# NowInWiki Backend

Java backend for [NowInWiki](https://github.com/Analoggpixel/NowInWiki) user accounts and cloud sync.

## Stack

- Java 17, Spring Boot 3.3
- MySQL 8, Redis 7
- MyBatis-Plus, Flyway
- SMS: `mock` (default) or `aliyun` profile

## Quick start

```bash
docker compose up -d
mvn -pl nowinwiki-core-service -am spring-boot:run
```

The backend listens on port `8088` by default.

- API docs: http://localhost:8088/doc.html
- Health: http://localhost:8088/actuator/health

## SMS modes

| Profile / config | Behavior |
|------------------|----------|
| default (`nowinwiki.sms.mode=mock`) | Log verification code, no real SMS |
| `spring.profiles.active=aliyun` | Send via Aliyun SMS (pay-as-you-go) |

Aliyun env vars:

```bash
ALIYUN_SMS_ACCESS_KEY_ID=...
ALIYUN_SMS_ACCESS_KEY_SECRET=...
ALIYUN_SMS_SIGN_NAME=...
ALIYUN_SMS_TEMPLATE_CODE=...
```

## API overview

All sync APIs require header `authorization: <accessToken>`.

### Auth

| Method | Path | Auth |
|--------|------|------|
| POST | `/api/v1/auth/code?phone=` | No |
| POST | `/api/v1/auth/login` | No |
| POST | `/api/v1/auth/refresh` | No (body: `refreshToken`) |
| POST | `/api/v1/auth/logout` | Yes |
| GET | `/api/v1/users/me` | Yes |
| DELETE | `/api/v1/users/me` | Yes |

Login response: `{ accessToken, refreshToken, token, user }` where `token` is an alias of `accessToken`.
Refresh response: `{ accessToken, refreshToken, token }`.

Access token TTL defaults to 30 minutes (sliding on use). Refresh token TTL defaults to 30 days with rotation, family reuse detection, and a 10-second grace period for idempotent refresh retries.

### Bookmark sync

| Method | Path |
|--------|------|
| GET | `/api/v1/sync/bookmarks/snapshot` |
| GET | `/api/v1/sync/bookmarks/pull?since=` |
| POST | `/api/v1/sync/bookmarks/push` |

Push supports `clientId` / `clientFolderId` for first-time folder mapping. Optional idempotency: `X-Request-Id` header or `requestId` in body.

### History sync

| Method | Path |
|--------|------|
| GET | `/api/v1/sync/history/snapshot` |
| GET | `/api/v1/sync/history/pull?since=` |
| POST | `/api/v1/sync/history/push` |

Server keeps up to 200 history rows per user (`nowinwiki.sync.history-max-rows`).

### Preference sync

| Method | Path |
|--------|------|
| GET | `/api/v1/sync/preferences/snapshot` |
| GET | `/api/v1/sync/preferences/pull?since=` |
| POST | `/api/v1/sync/preferences/push` |

`payloadJson` is opaque JSON aligned with Android `UserData` serialization.

## Build

```bash
mvn clean package
```
