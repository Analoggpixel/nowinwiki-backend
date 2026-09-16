package com.analoggpixel.nowinwiki.security;

import cn.hutool.core.util.StrUtil;
import com.analoggpixel.nowinwiki.parameter.dto.UserDTO;
import com.analoggpixel.nowinwiki.service.AuthSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class RefreshTokenInterceptor implements HandlerInterceptor {

    private final AuthSessionService authSessionService;

    public RefreshTokenInterceptor(AuthSessionService authSessionService) {
        this.authSessionService = authSessionService;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        String accessToken = request.getHeader("authorization");
        if (StrUtil.isBlank(accessToken)) {
            return true;
        }

        authSessionService.resolveAccessToken(accessToken)
                .ifPresent(UserHolder::saveUser);
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        UserHolder.removeUser();
    }
}
