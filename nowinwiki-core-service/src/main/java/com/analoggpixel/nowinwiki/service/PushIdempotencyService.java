package com.analoggpixel.nowinwiki.service;

import com.analoggpixel.nowinwiki.common.exception.BusinessException;
import com.analoggpixel.nowinwiki.config.SyncProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PushIdempotencyService {

    private static final String KEY_PREFIX = "sync:push:";

    private final StringRedisTemplate stringRedisTemplate;
    private final SyncProperties syncProperties;

    public void ensureFirstSeen(Long userId, String scope, String requestId) {
        if (!StringUtils.hasText(requestId)) {
            return;
        }
        String key = KEY_PREFIX + scope + ":" + userId + ":" + requestId;
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(
                key,
                "1",
                syncProperties.getPushIdempotentTtlSeconds(),
                TimeUnit.SECONDS
        );
        if (Boolean.FALSE.equals(ok)) {
            throw new BusinessException("重复提交，请稍后重试");
        }
    }
}
