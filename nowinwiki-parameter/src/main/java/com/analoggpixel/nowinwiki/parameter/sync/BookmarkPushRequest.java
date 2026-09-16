package com.analoggpixel.nowinwiki.parameter.sync;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BookmarkPushRequest {

    private String requestId;

    @Valid
    private List<BookmarkFolderSyncDTO> folders = new ArrayList<>();

    @Valid
    private List<BookmarkSyncDTO> bookmarks = new ArrayList<>();
}
