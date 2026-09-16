package com.analoggpixel.nowinwiki.parameter.sync;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class BookmarkPushResponse {

    private String serverTime;

    private Map<String, Long> folderIdMappings = new LinkedHashMap<>();

    private int foldersApplied;

    private int bookmarksApplied;

    private int skipped;
}
