package com.analoggpixel.nowinwiki.controller;

import com.analoggpixel.nowinwiki.parameter.dto.Result;
import com.analoggpixel.nowinwiki.parameter.sync.PreferencePushRequest;
import com.analoggpixel.nowinwiki.parameter.sync.PreferencePushResponse;
import com.analoggpixel.nowinwiki.parameter.sync.PreferenceSnapshotVO;
import com.analoggpixel.nowinwiki.service.PreferenceSyncService;
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

@Tag(name = "Preference Sync")
@RestController
@RequestMapping("/api/v1/sync/preferences")
@RequiredArgsConstructor
public class PreferenceSyncController {

    private final PreferenceSyncService preferenceSyncService;

    @Operation(summary = "用户设置全量快照")
    @GetMapping("/snapshot")
    public Result<PreferenceSnapshotVO> snapshot() {
        return preferenceSyncService.snapshot();
    }

    @Operation(summary = "用户设置增量拉取")
    @GetMapping("/pull")
    public Result<PreferenceSnapshotVO> pull(@RequestParam("since") String since) {
        return preferenceSyncService.pull(since);
    }

    @Operation(summary = "用户设置上行同步")
    @PostMapping("/push")
    public Result<PreferencePushResponse> push(
            @Valid @RequestBody PreferencePushRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        return preferenceSyncService.push(request, requestId);
    }
}
