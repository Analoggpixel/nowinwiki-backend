package com.analoggpixel.nowinwiki.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.analoggpixel.nowinwiki.common.auth.RefreshTokenStatus;
import com.analoggpixel.nowinwiki.common.auth.SessionFamilyStatus;
import com.analoggpixel.nowinwiki.common.constant.RedisConstants;
import com.analoggpixel.nowinwiki.common.exception.AuthException;
import com.analoggpixel.nowinwiki.config.AuthProperties;
import com.analoggpixel.nowinwiki.entity.User;
import com.analoggpixel.nowinwiki.mapper.UserMapper;
import com.analoggpixel.nowinwiki.parameter.dto.UserDTO;
import com.analoggpixel.nowinwiki.parameter.vo.TokenPairVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private static final String FIELD_FAMILY_ID = "familyId";
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_CURRENT_REFRESH = "currentRefresh";
    private static final String FIELD_ROTATED_AT = "rotatedAt";
    private static final String FIELD_SUCCESSOR_REFRESH = "successorRefresh";
    private static final String FIELD_SUCCESSOR_ACCESS = "successorAccess";

    private final StringRedisTemplate stringRedisTemplate;
    private final AuthProperties authProperties;
    private final UserMapper userMapper;

    public TokenPairVO createSession(UserDTO user) {
        String familyId = UUID.randomUUID().toString(true);
        String refreshToken = UUID.randomUUID().toString(true);
        String accessToken = UUID.randomUUID().toString(true);

        saveFamily(familyId, user.getId(), refreshToken, SessionFamilyStatus.ACTIVE);
        saveRefreshToken(refreshToken, familyId, user.getId(), RefreshTokenStatus.ACTIVE);
        saveAccessToken(accessToken, user, familyId);
        trackUserFamily(user.getId(), familyId);

        return toTokenPair(accessToken, refreshToken);
    }

    public TokenPairVO refresh(String refreshToken) {
        if (StrUtil.isBlank(refreshToken)) {
            throw new AuthException("refresh_invalid", "Refresh token 无效");
        }

        RefreshContext context = loadRefreshContext(refreshToken);
        if (context.refreshRecord.isEmpty()) {
            throw new AuthException("refresh_expired", "登录已过期，请重新登录");
        }

        Optional<TokenPairVO> graceReplay = tryGraceReplay(refreshToken, context);
        if (graceReplay.isPresent()) {
            return graceReplay.get();
        }

        if (RefreshTokenStatus.ROTATED.name().equals(context.refreshStatus)) {
            revokeFamily(context.familyId);
            throw new AuthException("refresh_reused", "登录状态异常，请重新登录");
        }
        if (!RefreshTokenStatus.ACTIVE.name().equals(context.refreshStatus)) {
            throw new AuthException("refresh_invalid", "Refresh token 无效");
        }

        if (!refreshToken.equals(context.currentRefresh)) {
            revokeFamily(context.familyId);
            throw new AuthException("refresh_reused", "登录状态异常，请重新登录");
        }

        String lockKey = RedisConstants.AUTH_REFRESH_LOCK_KEY + context.familyId;
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(
                lockKey,
                "1",
                5,
                TimeUnit.SECONDS
        );
        if (!Boolean.TRUE.equals(locked)) {
            throw new AuthException("refresh_conflict", "刷新过于频繁，请稍后重试");
        }

        try {
            context = loadRefreshContext(refreshToken);

            graceReplay = tryGraceReplay(refreshToken, context);
            if (graceReplay.isPresent()) {
                return graceReplay.get();
            }

            if (RefreshTokenStatus.ROTATED.name().equals(context.refreshStatus)) {
                revokeFamily(context.familyId);
                throw new AuthException("refresh_reused", "登录状态异常，请重新登录");
            }
            if (!RefreshTokenStatus.ACTIVE.name().equals(context.refreshStatus)) {
                throw new AuthException("refresh_invalid", "Refresh token 无效");
            }
            if (!refreshToken.equals(context.currentRefresh)) {
                revokeFamily(context.familyId);
                throw new AuthException("refresh_reused", "登录状态异常，请重新登录");
            }

            Long userId = parseUserId(context.refreshRecord.get(FIELD_USER_ID));
            User user = userMapper.selectById(userId);
            if (user == null) {
                revokeFamily(context.familyId);
                throw new AuthException("refresh_expired", "登录已过期，请重新登录");
            }
            UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);

            String newRefreshToken = UUID.randomUUID().toString(true);
            String newAccessToken = UUID.randomUUID().toString(true);
            markRefreshRotated(refreshToken, newRefreshToken, newAccessToken);
            saveRefreshToken(newRefreshToken, context.familyId, userId, RefreshTokenStatus.ACTIVE);
            saveAccessToken(newAccessToken, userDTO, context.familyId);
            updateFamilyCurrentRefresh(context.familyId, userId, newRefreshToken);

            return toTokenPair(newAccessToken, newRefreshToken);
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    public Optional<UserDTO> resolveAccessToken(String accessToken) {
        if (StrUtil.isBlank(accessToken)) {
            return Optional.empty();
        }
        String key = RedisConstants.AUTH_ACCESS_KEY + accessToken;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
        if (userMap.isEmpty()) {
            return Optional.empty();
        }
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        stringRedisTemplate.expire(key, authProperties.getAccessTokenTtlMinutes(), TimeUnit.MINUTES);
        return Optional.of(userDTO);
    }

    public void revokeByAccessToken(String accessToken) {
        if (StrUtil.isBlank(accessToken)) {
            return;
        }
        String key = RedisConstants.AUTH_ACCESS_KEY + accessToken;
        String familyId = string(stringRedisTemplate.opsForHash().get(key, FIELD_FAMILY_ID));
        stringRedisTemplate.delete(key);
        if (StrUtil.isNotBlank(familyId)) {
            revokeFamily(familyId);
        }
    }

    public void revokeAllForUser(Long userId) {
        if (userId == null) {
            return;
        }
        String userFamiliesKey = RedisConstants.AUTH_USER_FAMILIES_KEY + userId;
        Set<String> familyIds = stringRedisTemplate.opsForSet().members(userFamiliesKey);
        if (familyIds == null || familyIds.isEmpty()) {
            return;
        }
        for (String familyId : familyIds) {
            revokeFamily(familyId);
        }
        stringRedisTemplate.delete(userFamiliesKey);
    }

    private Optional<TokenPairVO> tryGraceReplay(String refreshToken, RefreshContext context) {
        if (!RefreshTokenStatus.ROTATED.name().equals(context.refreshStatus)) {
            return Optional.empty();
        }
        if (context.familyRecord.isEmpty()
                || SessionFamilyStatus.REVOKED.name().equals(string(context.familyRecord.get(FIELD_STATUS)))) {
            return Optional.empty();
        }

        long rotatedAt = parseLong(context.refreshRecord.get(FIELD_ROTATED_AT));
        if (rotatedAt <= 0L) {
            return Optional.empty();
        }
        long graceMillis = authProperties.getRefreshGracePeriodSeconds() * 1000L;
        if (System.currentTimeMillis() - rotatedAt > graceMillis) {
            return Optional.empty();
        }

        String successorRefresh = string(context.refreshRecord.get(FIELD_SUCCESSOR_REFRESH));
        String successorAccess = string(context.refreshRecord.get(FIELD_SUCCESSOR_ACCESS));
        if (StrUtil.hasBlank(successorRefresh, successorAccess)) {
            return Optional.empty();
        }
        if (!successorRefresh.equals(context.currentRefresh)) {
            return Optional.empty();
        }

        Map<Object, Object> successorRecord = stringRedisTemplate.opsForHash()
                .entries(RedisConstants.AUTH_REFRESH_KEY + successorRefresh);
        if (successorRecord.isEmpty()
                || !RefreshTokenStatus.ACTIVE.name().equals(string(successorRecord.get(FIELD_STATUS)))) {
            return Optional.empty();
        }

        ensureAccessTokenAvailable(successorAccess, context.familyId, parseUserId(context.refreshRecord.get(FIELD_USER_ID)));
        return Optional.of(toTokenPair(successorAccess, successorRefresh));
    }

    private void ensureAccessTokenAvailable(String accessToken, String familyId, Long userId) {
        String accessKey = RedisConstants.AUTH_ACCESS_KEY + accessToken;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(accessKey))) {
            stringRedisTemplate.expire(accessKey, authProperties.getAccessTokenTtlMinutes(), TimeUnit.MINUTES);
            return;
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }
        saveAccessToken(accessToken, BeanUtil.copyProperties(user, UserDTO.class), familyId);
    }

    private RefreshContext loadRefreshContext(String refreshToken) {
        Map<Object, Object> refreshRecord = stringRedisTemplate.opsForHash()
                .entries(RedisConstants.AUTH_REFRESH_KEY + refreshToken);
        String familyId = string(refreshRecord.get(FIELD_FAMILY_ID));
        String refreshStatus = string(refreshRecord.get(FIELD_STATUS));

        Map<Object, Object> familyRecord = Map.of();
        String currentRefresh = null;
        if (StrUtil.isNotBlank(familyId)) {
            familyRecord = stringRedisTemplate.opsForHash().entries(RedisConstants.AUTH_FAMILY_KEY + familyId);
            currentRefresh = string(familyRecord.get(FIELD_CURRENT_REFRESH));
        }

        if (familyRecord.isEmpty()
                || SessionFamilyStatus.REVOKED.name().equals(string(familyRecord.get(FIELD_STATUS)))) {
            throw new AuthException("refresh_expired", "登录已过期，请重新登录");
        }
        if (StrUtil.isBlank(familyId)) {
            throw new AuthException("refresh_invalid", "Refresh token 无效");
        }

        return new RefreshContext(familyId, refreshStatus, currentRefresh, refreshRecord, familyRecord);
    }

    private void revokeFamily(String familyId) {
        Map<Object, Object> familyRecord = stringRedisTemplate.opsForHash()
                .entries(RedisConstants.AUTH_FAMILY_KEY + familyId);
        if (familyRecord.isEmpty()) {
            return;
        }

        Long userId = parseUserId(familyRecord.get(FIELD_USER_ID));
        String currentRefresh = string(familyRecord.get(FIELD_CURRENT_REFRESH));

        Map<String, String> revokedFamily = new HashMap<>();
        revokedFamily.put(FIELD_USER_ID, String.valueOf(userId));
        revokedFamily.put(FIELD_STATUS, SessionFamilyStatus.REVOKED.name());
        if (StrUtil.isNotBlank(currentRefresh)) {
            revokedFamily.put(FIELD_CURRENT_REFRESH, currentRefresh);
            markRefreshRevoked(currentRefresh);
        }
        stringRedisTemplate.opsForHash().putAll(RedisConstants.AUTH_FAMILY_KEY + familyId, revokedFamily);
        stringRedisTemplate.expire(
                RedisConstants.AUTH_FAMILY_KEY + familyId,
                authProperties.getRefreshTokenTtlDays(),
                TimeUnit.DAYS
        );

        if (userId != null) {
            stringRedisTemplate.opsForSet().remove(
                    RedisConstants.AUTH_USER_FAMILIES_KEY + userId,
                    familyId
            );
        }
    }

    private void saveFamily(String familyId, Long userId, String currentRefresh, SessionFamilyStatus status) {
        Map<String, String> family = new HashMap<>();
        family.put(FIELD_USER_ID, String.valueOf(userId));
        family.put(FIELD_CURRENT_REFRESH, currentRefresh);
        family.put(FIELD_STATUS, status.name());
        String key = RedisConstants.AUTH_FAMILY_KEY + familyId;
        stringRedisTemplate.opsForHash().putAll(key, family);
        stringRedisTemplate.expire(key, authProperties.getRefreshTokenTtlDays(), TimeUnit.DAYS);
    }

    private void updateFamilyCurrentRefresh(String familyId, Long userId, String currentRefresh) {
        Map<String, String> family = new HashMap<>();
        family.put(FIELD_USER_ID, String.valueOf(userId));
        family.put(FIELD_CURRENT_REFRESH, currentRefresh);
        family.put(FIELD_STATUS, SessionFamilyStatus.ACTIVE.name());
        String key = RedisConstants.AUTH_FAMILY_KEY + familyId;
        stringRedisTemplate.opsForHash().putAll(key, family);
        stringRedisTemplate.expire(key, authProperties.getRefreshTokenTtlDays(), TimeUnit.DAYS);
    }

    private void saveRefreshToken(
            String refreshToken,
            String familyId,
            Long userId,
            RefreshTokenStatus status
    ) {
        Map<String, String> refresh = new HashMap<>();
        refresh.put(FIELD_FAMILY_ID, familyId);
        refresh.put(FIELD_USER_ID, String.valueOf(userId));
        refresh.put(FIELD_STATUS, status.name());
        String key = RedisConstants.AUTH_REFRESH_KEY + refreshToken;
        stringRedisTemplate.opsForHash().putAll(key, refresh);
        stringRedisTemplate.expire(key, authProperties.getRefreshTokenTtlDays(), TimeUnit.DAYS);
    }

    private void markRefreshRotated(String refreshToken, String successorRefresh, String successorAccess) {
        String key = RedisConstants.AUTH_REFRESH_KEY + refreshToken;
        Map<String, String> rotated = new HashMap<>();
        rotated.put(FIELD_STATUS, RefreshTokenStatus.ROTATED.name());
        rotated.put(FIELD_ROTATED_AT, String.valueOf(System.currentTimeMillis()));
        rotated.put(FIELD_SUCCESSOR_REFRESH, successorRefresh);
        rotated.put(FIELD_SUCCESSOR_ACCESS, successorAccess);
        stringRedisTemplate.opsForHash().putAll(key, rotated);
        stringRedisTemplate.expire(key, authProperties.getRefreshTokenTtlDays(), TimeUnit.DAYS);
    }

    private void markRefreshRevoked(String refreshToken) {
        String key = RedisConstants.AUTH_REFRESH_KEY + refreshToken;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            stringRedisTemplate.opsForHash().put(key, FIELD_STATUS, RefreshTokenStatus.REVOKED.name());
            stringRedisTemplate.expire(key, authProperties.getRefreshTokenTtlDays(), TimeUnit.DAYS);
        }
    }

    private void saveAccessToken(String accessToken, UserDTO user, String familyId) {
        Map<String, String> access = new HashMap<>();
        access.put("id", String.valueOf(user.getId()));
        access.put("phone", user.getPhone());
        access.put("nickName", user.getNickName());
        if (user.getIcon() != null) {
            access.put("icon", user.getIcon());
        }
        access.put(FIELD_FAMILY_ID, familyId);
        String key = RedisConstants.AUTH_ACCESS_KEY + accessToken;
        stringRedisTemplate.opsForHash().putAll(key, access);
        stringRedisTemplate.expire(key, authProperties.getAccessTokenTtlMinutes(), TimeUnit.MINUTES);
    }

    private void trackUserFamily(Long userId, String familyId) {
        String key = RedisConstants.AUTH_USER_FAMILIES_KEY + userId;
        stringRedisTemplate.opsForSet().add(key, familyId);
        stringRedisTemplate.expire(key, authProperties.getRefreshTokenTtlDays(), TimeUnit.DAYS);
    }

    private TokenPairVO toTokenPair(String accessToken, String refreshToken) {
        TokenPairVO pair = new TokenPairVO();
        pair.setAccessToken(accessToken);
        pair.setRefreshToken(refreshToken);
        pair.setToken(accessToken);
        return pair;
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }

    private static Long parseUserId(Object value) {
        if (value == null) {
            return null;
        }
        return Long.parseLong(value.toString());
    }

    private static long parseLong(Object value) {
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private record RefreshContext(
            String familyId,
            String refreshStatus,
            String currentRefresh,
            Map<Object, Object> refreshRecord,
            Map<Object, Object> familyRecord
    ) {
    }
}
