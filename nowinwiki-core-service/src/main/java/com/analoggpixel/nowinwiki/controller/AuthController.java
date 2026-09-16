package com.analoggpixel.nowinwiki.controller;

import com.analoggpixel.nowinwiki.parameter.dto.LoginFormDTO;
import com.analoggpixel.nowinwiki.parameter.dto.RefreshTokenFormDTO;
import com.analoggpixel.nowinwiki.parameter.dto.Result;
import com.analoggpixel.nowinwiki.parameter.dto.UserDTO;
import com.analoggpixel.nowinwiki.parameter.vo.LoginVO;
import com.analoggpixel.nowinwiki.parameter.vo.TokenPairVO;
import com.analoggpixel.nowinwiki.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "发送登录验证码")
    @PostMapping("/auth/code")
    public Result<Void> sendCode(@RequestParam("phone") String phone) {
        return authService.sendCode(phone);
    }

    @Operation(summary = "手机号验证码登录")
    @PostMapping("/auth/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginFormDTO loginForm) {
        return authService.login(loginForm);
    }

    @Operation(summary = "刷新访问令牌")
    @PostMapping("/auth/refresh")
    public Result<TokenPairVO> refresh(@Valid @RequestBody RefreshTokenFormDTO refreshForm) {
        return authService.refresh(refreshForm);
    }

    @Operation(summary = "登出")
    @PostMapping("/auth/logout")
    public Result<Void> logout(@RequestHeader(value = "authorization", required = false) String token) {
        return authService.logout(token);
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/users/me")
    public Result<UserDTO> me() {
        return authService.currentUser();
    }

    @Operation(summary = "注销账号并删除云端数据")
    @DeleteMapping("/users/me")
    public Result<Void> deleteAccount(
            @RequestHeader(value = "authorization", required = false) String token
    ) {
        return authService.deleteAccount(token);
    }
}
