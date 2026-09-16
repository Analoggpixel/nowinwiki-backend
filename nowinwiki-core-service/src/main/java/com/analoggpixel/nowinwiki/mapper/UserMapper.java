package com.analoggpixel.nowinwiki.mapper;

import com.analoggpixel.nowinwiki.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
