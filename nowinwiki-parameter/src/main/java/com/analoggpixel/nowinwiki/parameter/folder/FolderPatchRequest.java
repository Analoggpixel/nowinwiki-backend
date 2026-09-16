package com.analoggpixel.nowinwiki.parameter.folder;

import lombok.Data;

@Data
public class FolderPatchRequest {

    private String name;

    private String description;

    private Integer sortOrder;
}
