package com.analoggpixel.nowinwiki.service;

import com.analoggpixel.nowinwiki.parameter.dto.Result;
import com.analoggpixel.nowinwiki.parameter.sync.BookmarkPushRequest;
import com.analoggpixel.nowinwiki.parameter.sync.BookmarkPushResponse;
import com.analoggpixel.nowinwiki.parameter.sync.BookmarkSnapshotVO;

public interface BookmarkSyncService {

    Result<BookmarkSnapshotVO> snapshot();

    Result<BookmarkSnapshotVO> pull(String since);

    Result<BookmarkPushResponse> push(BookmarkPushRequest request, String requestIdHeader);
}
