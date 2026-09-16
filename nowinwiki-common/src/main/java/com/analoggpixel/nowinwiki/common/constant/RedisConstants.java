package com.analoggpixel.nowinwiki.common.constant;

public final class RedisConstants {

    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final long LOGIN_CODE_TTL_MINUTES = 5L;

    /** @deprecated Replaced by {@link #AUTH_ACCESS_KEY} + refresh/family keys. */
    @Deprecated
    public static final String LOGIN_USER_KEY = "login:token:";

    public static final String AUTH_ACCESS_KEY = "auth:access:";
    public static final String AUTH_REFRESH_KEY = "auth:refresh:";
    public static final String AUTH_FAMILY_KEY = "auth:family:";
    public static final String AUTH_USER_FAMILIES_KEY = "auth:user:families:";
    public static final String AUTH_REFRESH_LOCK_KEY = "auth:refresh:lock:";

    public static final String SMS_RATE_LIMIT_KEY = "sms:rate:";
    public static final long SMS_RATE_LIMIT_TTL_SECONDS = 45L;

    private RedisConstants() {
    }
}
