package com.analoggpixel.nowinwiki;

import com.analoggpixel.nowinwiki.common.util.SyncTimeUtils;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncTimeUtilsTest {

    @Test
    void roundTrip() {
        LocalDateTime time = LocalDateTime.of(2026, 8, 24, 1, 30, 0);
        String formatted = SyncTimeUtils.format(time);
        assertEquals(time, SyncTimeUtils.parse(formatted));
    }

    @Test
    void compareForLww() {
        LocalDateTime older = SyncTimeUtils.parse("2026-08-24T01:00:00");
        LocalDateTime newer = SyncTimeUtils.parse("2026-08-24T02:00:00");
        assertTrue(newer.isAfter(older));
    }
}
