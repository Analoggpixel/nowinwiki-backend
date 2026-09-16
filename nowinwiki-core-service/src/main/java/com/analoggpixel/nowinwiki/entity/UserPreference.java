package com.analoggpixel.nowinwiki.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_preference")
public class UserPreference {

    private Long userId;

    private String payload;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
