package com.analoggpixel.nowinwiki.parameter.vo;

import lombok.Data;

@Data
public class LoginVO {

    private String accessToken;
    private String refreshToken;

    /**
     * Backward-compatible alias of {@link #accessToken}.
     */
    private String token;

    private UserProfileVO user;
}
