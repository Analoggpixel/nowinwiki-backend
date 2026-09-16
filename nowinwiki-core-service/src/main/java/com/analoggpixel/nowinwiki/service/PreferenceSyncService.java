package com.analoggpixel.nowinwiki.service;

import com.analoggpixel.nowinwiki.parameter.dto.Result;
import com.analoggpixel.nowinwiki.parameter.sync.PreferencePushRequest;
import com.analoggpixel.nowinwiki.parameter.sync.PreferencePushResponse;
import com.analoggpixel.nowinwiki.parameter.sync.PreferenceSnapshotVO;

public interface PreferenceSyncService {

    Result<PreferenceSnapshotVO> snapshot();

    Result<PreferenceSnapshotVO> pull(String since);

    Result<PreferencePushResponse> push(PreferencePushRequest request, String requestIdHeader);
}
