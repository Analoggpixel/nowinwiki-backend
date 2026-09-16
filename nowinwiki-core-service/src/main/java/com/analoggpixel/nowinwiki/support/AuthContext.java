package com.analoggpixel.nowinwiki.support;

import com.analoggpixel.nowinwiki.common.exception.BusinessException;
import com.analoggpixel.nowinwiki.parameter.dto.UserDTO;
import com.analoggpixel.nowinwiki.security.UserHolder;

public final class AuthContext {

    public static Long requireUserId() {
        UserDTO user = UserHolder.getUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException("未登录");
        }
        return user.getId();
    }

    private AuthContext() {
    }
}
