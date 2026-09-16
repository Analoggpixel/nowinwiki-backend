package com.analoggpixel.nowinwiki.parameter.sync;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookmarkFolderSyncDTO {

    /** Server id; null when the client creates a new folder. */
    private Long id;

    /** Local Room id for mapping in the same push batch. */
    private Long clientId;

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private Integer sortOrder;

    /**
     * True for the account's system default folder (server-owned; not deletable).
     * Boolean (not primitive) so Jackson serializes as {@code isDefault}.
     */
    private Boolean isDefault = false;

    /** ISO-8601 local datetime from client, e.g. 2026-08-24T01:00:00 */
    @NotBlank
    private String updatedAt;

    private boolean deleted;
}
