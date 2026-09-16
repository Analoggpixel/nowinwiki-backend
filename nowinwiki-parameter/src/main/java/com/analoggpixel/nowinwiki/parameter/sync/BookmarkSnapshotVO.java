package com.analoggpixel.nowinwiki.parameter.sync;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BookmarkSnapshotVO {

    private String serverTime;

    private List<BookmarkFolderSyncDTO> folders = new ArrayList<>();

    private List<BookmarkSyncDTO> bookmarks = new ArrayList<>();
}
