package com.middle.wcs.produce.service;

import com.middle.wcs.produce.entity.dto.BatchDetailDTO;

/**
 * 生产批次 Service 接口
 */
public interface ProduceBatchService {

    /**
     * 根据货物 UID 查询批次详情（批次 + 托盘 + 货物嵌套）
     *
     * @param uid 货物唯一码
     * @return BatchDetailDTO，若不存在则返回 null
     */
    BatchDetailDTO getByGoodsUid(String uid);

    /**
     * 保存完整批次（批次 + 托盘 + 货物），事务保证原子性
     *
     * @param dto BatchDetailDTO
     * @return 保存后的 BatchDetailDTO（含生成的 ID）
     */
    BatchDetailDTO save(BatchDetailDTO dto);

    /**
     * 获取当前正在执行的批次（status=1已确认 或 2生产中），若不存在则返回 null
     *
     * @return BatchDetailDTO，含完整托盘和货物
     */
    BatchDetailDTO getCurrentExecuting();

    /**
     * 确认批次，将状态更新为已确认（status=1）。
     * 若已有其他批次正在执行（status=1或2），则抛出业务异常，限定同一时间只能执行一个批次。
     *
     * @param batchId 批次 ID
     */
    void confirm(Long batchId);

    /**
     * 取消执行批次，将状态从已确认/执行中（status=1或2）回退为待确认（status=0）
     *
     * @param batchId 批次 ID
     */
    void cancel(Long batchId);
}
