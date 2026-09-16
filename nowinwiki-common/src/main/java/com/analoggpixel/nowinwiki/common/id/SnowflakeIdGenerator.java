package com.analoggpixel.nowinwiki.common.id;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.stereotype.Component;

@Component
public class SnowflakeIdGenerator {

    private final Snowflake snowflake = IdUtil.getSnowflake(1, 1);

    public long nextId() {
        return snowflake.nextId();
    }
}
