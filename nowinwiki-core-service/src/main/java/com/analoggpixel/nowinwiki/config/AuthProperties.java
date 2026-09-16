package com.analoggpixel.nowinwiki.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "nowinwiki.auth")
public class AuthProperties {

    /**
     * Access token TTL used for API authorization header.
     */
    private long accessTokenTtlMinutes = 30L;

    /**
     * Refresh token TTL; also drives session family lifetime.
     */
    private long refreshTokenTtlDays = 30L;

    /**
     * After rotation, the previous refresh token may be replayed once within this window
     * (e.g. network retry) and receive the same successor token pair.
     */
    private long refreshGracePeriodSeconds = 10L;
}
