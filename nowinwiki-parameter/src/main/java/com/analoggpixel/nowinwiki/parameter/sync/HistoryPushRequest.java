package com.analoggpixel.nowinwiki.parameter.sync;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class HistoryPushRequest {

    private String requestId;

    @Valid
    private List<ReadHistorySyncDTO> items = new ArrayList<>();
}
