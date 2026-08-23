package com.duck.saver.statistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.duck.saver.statistics.entity.ProcessedEventEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProcessedEventMapper extends BaseMapper<ProcessedEventEntity> {
}
