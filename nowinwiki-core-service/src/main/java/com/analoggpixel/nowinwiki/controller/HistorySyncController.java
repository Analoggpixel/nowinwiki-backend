package com.analoggpixel.nowinwiki.controller;

import com.analoggpixel.nowinwiki.parameter.dto.Result;
import com.analoggpixel.nowinwiki.parameter.sync.HistoryPushRequest;
import com.analoggpixel.nowinwiki.parameter.sync.HistoryPushResponse;
import com.analoggpixel.nowinwiki.parameter.sync.HistorySnapshotVO;
import com.analoggpixel.nowinwiki.service.HistorySyncService;
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

@Tag(name = "History Sync")
@RestController
@RequestMapping("/api/v1/sync/history")
@RequiredArgsConstructor
public class HistorySyncController {

    private final HistorySyncService historySyncService;

    @Operation(summary = "阅读历史全量快照")
    @GetMapping("/snapshot")
    public Result<HistorySnapshotVO> snapshot() {
        return historySyncService.snapshot();
    }

    @Operation(summary = "阅读历史增量拉取")
    @GetMapping("/pull")
    public Result<HistorySnapshotVO> pull(@RequestParam("since") String since) {
        return historySyncService.pull(since);
    }

    @Operation(summary = "阅读历史上行同步")
    @PostMapping("/push")
    public Result<HistoryPushResponse> push(
            @Valid @RequestBody HistoryPushRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        return historySyncService.push(request, requestId);
    }
}
