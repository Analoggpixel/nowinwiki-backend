package com.analoggpixel.nowinwiki.parameter.vo;

import lombok.Data;

@Data
public class TokenPairVO {

    private String accessToken;
    private String refreshToken;

    /**
     * Backward-compatible alias of {@link #accessToken} for existing clients.
     */
    private String token;
}
