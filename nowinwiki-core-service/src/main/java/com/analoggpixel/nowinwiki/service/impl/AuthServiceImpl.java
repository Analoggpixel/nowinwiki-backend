package com.analoggpixel.nowinwiki.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.analoggpixel.nowinwiki.common.constant.RedisConstants;
import com.analoggpixel.nowinwiki.common.constant.SystemConstants;
import com.analoggpixel.nowinwiki.common.exception.BusinessException;
import com.analoggpixel.nowinwiki.common.id.SnowflakeIdGenerator;
import com.analoggpixel.nowinwiki.common.util.RegexUtils;
import com.analoggpixel.nowinwiki.entity.Bookmark;
import com.analoggpixel.nowinwiki.entity.BookmarkFolder;
import com.analoggpixel.nowinwiki.entity.ReadHistory;
import com.analoggpixel.nowinwiki.entity.User;
import com.analoggpixel.nowinwiki.entity.UserPreference;
import com.analoggpixel.nowinwiki.mapper.BookmarkFolderMapper;
import com.analoggpixel.nowinwiki.mapper.BookmarkMapper;
import com.analoggpixel.nowinwiki.mapper.ReadHistoryMapper;
import com.analoggpixel.nowinwiki.mapper.UserMapper;
import com.analoggpixel.nowinwiki.mapper.UserPreferenceMapper;
import com.analoggpixel.nowinwiki.parameter.dto.LoginFormDTO;
import com.analoggpixel.nowinwiki.parameter.dto.RefreshTokenFormDTO;
import com.analoggpixel.nowinwiki.parameter.dto.Result;
import com.analoggpixel.nowinwiki.parameter.dto.UserDTO;
import com.analoggpixel.nowinwiki.parameter.vo.LoginVO;
import com.analoggpixel.nowinwiki.parameter.vo.TokenPairVO;
import com.analoggpixel.nowinwiki.parameter.vo.UserProfileVO;
import com.analoggpixel.nowinwiki.security.UserHolder;
import com.analoggpixel.nowinwiki.service.AuthService;
import com.analoggpixel.nowinwiki.service.AuthSessionService;
import com.analoggpixel.nowinwiki.sms.SmsSender;
import com.analoggpixel.nowinwiki.support.AuthContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl extends ServiceImpl<UserMapper, User> implements AuthService {

    private final StringRedisTemplate stringRedisTemplate;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final SmsSender smsSender;
    private final BookmarkMapper bookmarkMapper;
    private final BookmarkFolderMapper bookmarkFolderMapper;
    private final ReadHistoryMapper readHistoryMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final AuthSessionService authSessionService;

    @Override
    public Result<Void> sendCode(String phone) {
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }

        String rateLimitKey = RedisConstants.SMS_RATE_LIMIT_KEY + phone;
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(
                rateLimitKey,
                "1",
                RedisConstants.SMS_RATE_LIMIT_TTL_SECONDS,
                TimeUnit.SECONDS
        );
        if (Boolean.FALSE.equals(acquired)) {
            return Result.fail("发送过于频繁，请稍后再试");
        }

        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set(
                RedisConstants.LOGIN_CODE_KEY + phone,
                code,
                RedisConstants.LOGIN_CODE_TTL_MINUTES,
                TimeUnit.MINUTES
        );
        smsSender.sendLoginCode(phone, code);
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<LoginVO> login(LoginFormDTO loginForm) {
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        if (RegexUtils.isCodeInvalid(loginForm.getCode())) {
            return Result.fail("验证码格式错误");
        }

        String cacheCode = stringRedisTemplate.opsForValue().get(
                RedisConstants.LOGIN_CODE_KEY + phone
        );
        if (cacheCode == null || !cacheCode.equals(loginForm.getCode())) {
            return Result.fail("验证码错误或已过期");
        }
        stringRedisTemplate.delete(RedisConstants.LOGIN_CODE_KEY + phone);

        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) {
            user = createUser(phone);
        } else {
            ensureDefaultBookmarkFolder(user.getId());
        }

        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        TokenPairVO tokenPair = authSessionService.createSession(userDTO);

        LoginVO loginVO = new LoginVO();
        loginVO.setAccessToken(tokenPair.getAccessToken());
        loginVO.setRefreshToken(tokenPair.getRefreshToken());
        loginVO.setToken(tokenPair.getAccessToken());
        loginVO.setUser(BeanUtil.copyProperties(userDTO, UserProfileVO.class));
        return Result.ok(loginVO);
    }

    @Override
    public Result<TokenPairVO> refresh(RefreshTokenFormDTO refreshForm) {
        return Result.ok(authSessionService.refresh(refreshForm.getRefreshToken()));
    }

    @Override
    public Result<Void> logout(String accessToken) {
        authSessionService.revokeByAccessToken(accessToken);
        return Result.ok();
    }

    @Override
    public Result<UserDTO> currentUser() {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            throw new BusinessException("未登录");
        }
        return Result.ok(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteAccount(String accessToken) {
        Long userId = AuthContext.requireUserId();
        bookmarkMapper.delete(new LambdaQueryWrapper<Bookmark>().eq(Bookmark::getUserId, userId));
        bookmarkFolderMapper.delete(
                new LambdaQueryWrapper<BookmarkFolder>().eq(BookmarkFolder::getUserId, userId)
        );
        readHistoryMapper.delete(new LambdaQueryWrapper<ReadHistory>().eq(ReadHistory::getUserId, userId));
        userPreferenceMapper.delete(
                new LambdaQueryWrapper<UserPreference>().eq(UserPreference::getUserId, userId)
        );
        if (!removeById(userId)) {
            throw new BusinessException("账号删除失败");
        }
        authSessionService.revokeAllForUser(userId);
        authSessionService.revokeByAccessToken(accessToken);
        return Result.ok();
    }

    private User createUser(String phone) {
        User user = new User();
        user.setId(snowflakeIdGenerator.nextId());
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(8));
        if (!save(user)) {
            throw new BusinessException("用户创建失败");
        }
        createDefaultBookmarkFolder(user.getId());
        return user;
    }

    /**
     * Ensures the account has an active default folder (for legacy users created before this rule).
     */
    private void ensureDefaultBookmarkFolder(Long userId) {
        Long count = bookmarkFolderMapper.selectCount(
                new LambdaQueryWrapper<BookmarkFolder>()
                        .eq(BookmarkFolder::getUserId, userId)
                        .eq(BookmarkFolder::getIsDefault, true)
                        .isNull(BookmarkFolder::getDeletedAt)
        );
        if (count != null && count > 0) {
            return;
        }
        createDefaultBookmarkFolder(userId);
    }

    private void createDefaultBookmarkFolder(Long userId) {
        BookmarkFolder folder = new BookmarkFolder();
        folder.setId(snowflakeIdGenerator.nextId());
        folder.setUserId(userId);
        folder.setName(SystemConstants.DEFAULT_BOOKMARK_FOLDER_NAME);
        folder.setDescription(null);
        folder.setSortOrder(0);
        folder.setIsDefault(true);
        folder.setUpdatedAt(java.time.LocalDateTime.now());
        folder.setDeletedAt(null);
        if (bookmarkFolderMapper.insert(folder) <= 0) {
            throw new BusinessException("默认收藏夹创建失败");
        }
    }
}
