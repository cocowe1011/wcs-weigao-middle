package com.middle.wcs.produce.service;

import com.middle.wcs.produce.entity.po.ProduceBatchDestination;

/**
 * 批次目的地设置 Service 接口
 */
public interface ProduceBatchDestinationService {

    /**
     * 查询批次当前激活的目的地，不存在则返回 null
     *
     * @param batchId 批次ID
     * @return 激活的目的地记录
     */
    ProduceBatchDestination getCurrentByBatchId(Long batchId);

    /**
     * 设置目的地：关闭旧的激活记录，新建一条激活记录
     * 要求批次状态为已确认(1)或生产中(2)，否则抛出异常
     *
     * @param batchId         批次ID
     * @param destinationCode 目的地编码（3201~3215）
     * @return 新建的目的地记录
     */
    ProduceBatchDestination set(Long batchId, String destinationCode);

    /**
     * 取消当前批次的激活目的地
     *
     * @param batchId 批次ID
     */
    void cancel(Long batchId);
}
