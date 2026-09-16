package com.analoggpixel.nowinwiki.parameter.folder;

import lombok.Data;

@Data
public class FolderVO {

    private Long id;
    private String name;
    private String description;
    private Integer sortOrder;
    private Boolean isDefault;
    private String updatedAt;
    private boolean deleted;
}
