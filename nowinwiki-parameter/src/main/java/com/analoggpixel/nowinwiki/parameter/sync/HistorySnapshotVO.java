package com.analoggpixel.nowinwiki.parameter.sync;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class HistorySnapshotVO {

    private String serverTime;

    private List<ReadHistorySyncDTO> items = new ArrayList<>();
}
