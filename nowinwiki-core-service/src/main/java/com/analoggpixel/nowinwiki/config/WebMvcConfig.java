package com.analoggpixel.nowinwiki.config;

import com.analoggpixel.nowinwiki.security.LoginInterceptor;
import com.analoggpixel.nowinwiki.security.RefreshTokenInterceptor;
import com.analoggpixel.nowinwiki.service.AuthSessionService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthSessionService authSessionService;

    public WebMvcConfig(AuthSessionService authSessionService) {
        this.authSessionService = authSessionService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RefreshTokenInterceptor(authSessionService))
                .addPathPatterns("/**")
                .order(0);
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(
                        "/api/v1/auth/code",
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/actuator/**",
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**"
                )
                .order(1);
    }
}
