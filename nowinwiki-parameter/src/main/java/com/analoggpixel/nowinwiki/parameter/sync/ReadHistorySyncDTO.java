package com.analoggpixel.nowinwiki.parameter.sync;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReadHistorySyncDTO {

    @NotBlank
    private String title;

    @NotBlank
    private String language;

    @NotNull
    private Long viewedAt;

    private String description;

    private String thumbnailUrl;

    @NotBlank
    private String updatedAt;

    private boolean deleted;
}
