package com.middle.wcs.produce.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.middle.wcs.produce.entity.po.ProduceBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 生产批次 Mapper
 */
@Mapper
public interface ProduceBatchMapper extends BaseMapper<ProduceBatch> {

    /**
     * 查询当前正在执行的批次（status=1已确认 或 2生产中），取最新一条
     */
    ProduceBatch selectCurrentExecuting();

    /**
     * 统计正在执行的批次数量（排除指定批次ID）
     *
     * @param excludeBatchId 排除的批次ID，可为 null
     * @return 数量
     */
    int countExecutingExcludeBatch(@Param("excludeBatchId") Long excludeBatchId);
}
