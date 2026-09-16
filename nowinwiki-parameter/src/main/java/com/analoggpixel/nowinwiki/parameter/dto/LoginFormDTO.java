package com.analoggpixel.nowinwiki.parameter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginFormDTO {

    @NotBlank
    private String phone;

    @NotBlank
    private String code;
}
