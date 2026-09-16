package com.analoggpixel.nowinwiki.parameter.sync;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserPreferenceSyncDTO {

    @NotBlank
    private String payloadJson;

    @NotBlank
    private String updatedAt;

    private boolean deleted;
}
