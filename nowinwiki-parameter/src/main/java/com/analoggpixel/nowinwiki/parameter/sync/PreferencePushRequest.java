package com.analoggpixel.nowinwiki.parameter.sync;

import jakarta.validation.Valid;
import lombok.Data;

@Data
public class PreferencePushRequest {

    private String requestId;

    @Valid
    private UserPreferenceSyncDTO preference;
}
