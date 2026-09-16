package com.analoggpixel.nowinwiki.parameter.folder;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FolderCreateRequest {

    @NotBlank
    private String name;

    private String description;

    /** Optional; server appends at end when null. */
    private Integer sortOrder;
}
