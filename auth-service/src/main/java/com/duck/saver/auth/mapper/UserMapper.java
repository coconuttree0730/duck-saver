package com.duck.saver.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.duck.saver.auth.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
