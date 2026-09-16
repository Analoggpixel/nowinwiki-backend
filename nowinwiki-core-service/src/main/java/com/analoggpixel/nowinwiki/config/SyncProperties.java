package com.analoggpixel.nowinwiki.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "nowinwiki.sync")
public class SyncProperties {

    /**
     * Max read-history rows kept per user on the server.
     */
    private int historyMaxRows = 200;

    /**
     * Idempotent push window in seconds.
     */
    private long pushIdempotentTtlSeconds = 300L;
}
