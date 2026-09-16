package com.analoggpixel.nowinwiki.service;

import com.analoggpixel.nowinwiki.parameter.dto.Result;
import com.analoggpixel.nowinwiki.parameter.sync.HistoryPushRequest;
import com.analoggpixel.nowinwiki.parameter.sync.HistoryPushResponse;
import com.analoggpixel.nowinwiki.parameter.sync.HistorySnapshotVO;

public interface HistorySyncService {

    Result<HistorySnapshotVO> snapshot();

    Result<HistorySnapshotVO> pull(String since);

    Result<HistoryPushResponse> push(HistoryPushRequest request, String requestIdHeader);
}
