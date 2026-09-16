package com.analoggpixel.nowinwiki.parameter.sync;

import lombok.Data;

@Data
public class PreferenceSnapshotVO {

    private String serverTime;

    private UserPreferenceSyncDTO preference;
}
