package com.analoggpixel.nowinwiki.parameter.sync;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookmarkSyncDTO {

    private Long id;

    private Long folderId;

    /** Used when [folderId] is not yet known on the client. */
    private Long clientFolderId;

    @NotBlank
    private String title;

    @NotBlank
    private String language;

    @NotNull
    private Long bookmarkedAt;

    private String description;

    private String thumbnailUrl;

    @NotBlank
    private String updatedAt;

    private boolean deleted;
}
