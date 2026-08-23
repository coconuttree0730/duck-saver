package com.duck.saver.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.duck.saver.account.entity.AccountEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountMapper extends BaseMapper<AccountEntity> {
}
