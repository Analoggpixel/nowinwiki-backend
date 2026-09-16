package com.analoggpixel.nowinwiki.controller;

import com.analoggpixel.nowinwiki.parameter.dto.Result;
import com.analoggpixel.nowinwiki.parameter.sync.BookmarkPushRequest;
import com.analoggpixel.nowinwiki.parameter.sync.BookmarkPushResponse;
import com.analoggpixel.nowinwiki.parameter.sync.BookmarkSnapshotVO;
import com.analoggpixel.nowinwiki.service.BookmarkSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Bookmark Sync")
@RestController
@RequestMapping("/api/v1/sync/bookmarks")
@RequiredArgsConstructor
public class BookmarkSyncController {

    private final BookmarkSyncService bookmarkSyncService;

    @Operation(summary = "收藏夹全量快照")
    @GetMapping("/snapshot")
    public Result<BookmarkSnapshotVO> snapshot() {
        return bookmarkSyncService.snapshot();
    }

    @Operation(summary = "收藏夹增量拉取")
    @GetMapping("/pull")
    public Result<BookmarkSnapshotVO> pull(@RequestParam("since") String since) {
        return bookmarkSyncService.pull(since);
    }

    @Operation(summary = "收藏夹上行同步")
    @PostMapping("/push")
    public Result<BookmarkPushResponse> push(
            @Valid @RequestBody BookmarkPushRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        return bookmarkSyncService.push(request, requestId);
    }
}
