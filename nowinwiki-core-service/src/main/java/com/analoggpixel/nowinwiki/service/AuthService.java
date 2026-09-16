package com.analoggpixel.nowinwiki.service;

import com.analoggpixel.nowinwiki.parameter.dto.LoginFormDTO;
import com.analoggpixel.nowinwiki.parameter.dto.RefreshTokenFormDTO;
import com.analoggpixel.nowinwiki.parameter.dto.Result;
import com.analoggpixel.nowinwiki.parameter.dto.UserDTO;
import com.analoggpixel.nowinwiki.parameter.vo.LoginVO;
import com.analoggpixel.nowinwiki.parameter.vo.TokenPairVO;

public interface AuthService {

    Result<Void> sendCode(String phone);

    Result<LoginVO> login(LoginFormDTO loginForm);

    Result<TokenPairVO> refresh(RefreshTokenFormDTO refreshForm);

    Result<Void> logout(String accessToken);

    Result<UserDTO> currentUser();

    Result<Void> deleteAccount(String accessToken);
}
