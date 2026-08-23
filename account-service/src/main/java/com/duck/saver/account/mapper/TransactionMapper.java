package com.duck.saver.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.duck.saver.account.entity.TransactionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TransactionMapper extends BaseMapper<TransactionEntity> {
}
