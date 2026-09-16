package com.analoggpixel.nowinwiki.parameter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenFormDTO {

    @NotBlank
    private String refreshToken;
}
