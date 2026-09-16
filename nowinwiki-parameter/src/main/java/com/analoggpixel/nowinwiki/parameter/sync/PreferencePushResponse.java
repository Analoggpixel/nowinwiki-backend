package com.analoggpixel.nowinwiki.parameter.sync;

import lombok.Data;

@Data
public class PreferencePushResponse {

    private String serverTime;

    private boolean applied;

    private boolean skipped;
}
