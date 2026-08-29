package com.middle.wcs.produce.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.middle.wcs.produce.entity.po.ProduceBatchDestination;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 批次目的地设置流水 Mapper
 */
@Mapper
public interface ProduceBatchDestinationMapper extends BaseMapper<ProduceBatchDestination> {

    /**
     * 查询批次当前激活的目的地设置（status=0），取最新一条
     *
     * @param batchId 批次ID
     * @return 激活的目的地记录，不存在则返回 null
     */
    ProduceBatchDestination selectActiveByBatchId(@Param("batchId") Long batchId);

    /**
     * 真删：按批次ID删除其所有目的地设置记录（作废批次用）
     *
     * @param batchId 批次ID
     * @return 删除行数
     */
    int deleteByBatchId(@Param("batchId") Long batchId);
}
