package com.analoggpixel.nowinwiki.parameter.sync;

import lombok.Data;

@Data
public class HistoryPushResponse {

    private String serverTime;

    private int itemsApplied;

    private int skipped;

    private int trimmed;
}
