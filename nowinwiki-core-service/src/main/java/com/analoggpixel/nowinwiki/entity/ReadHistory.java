package com.analoggpixel.nowinwiki.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("read_history")
public class ReadHistory {

    private Long userId;

    private String language;

    private String title;

    private Long viewedAt;

    private String description;

    private String thumbnailUrl;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
