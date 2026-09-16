package com.analoggpixel.nowinwiki.security;

import com.analoggpixel.nowinwiki.parameter.dto.UserDTO;

public final class UserHolder {

    private static final ThreadLocal<UserDTO> HOLDER = new ThreadLocal<>();

    public static void saveUser(UserDTO user) {
        HOLDER.set(user);
    }

    public static UserDTO getUser() {
        return HOLDER.get();
    }

    public static void removeUser() {
        HOLDER.remove();
    }

    private UserHolder() {
    }
}
