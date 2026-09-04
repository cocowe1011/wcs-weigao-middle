package com.middle.wcs.produce.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.middle.wcs.produce.entity.dto.BatchHistoryQueryDTO;
import com.middle.wcs.produce.entity.po.ProduceBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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

    /**
     * 历史批次列表（配合 PageHelper 分页，SQL 本身不分页）
     */
    List<ProduceBatch> selectListByPage(BatchHistoryQueryDTO query);

    /**
     * 重新开线：将已完成（status=3）的批次回退为生产中（status=2）并清空完成时间
     *
     * @param batchId 批次ID
     * @return 影响行数（0 表示批次不处于完成态）
     */
    int reopen(@Param("batchId") Long batchId);
}
