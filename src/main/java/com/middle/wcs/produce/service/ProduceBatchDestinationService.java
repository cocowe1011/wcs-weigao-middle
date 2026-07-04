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
     * @param po 目的地设置信息（需包含 batchId 和 destinationCode）
     * @return 新建的目的地记录
     */
    ProduceBatchDestination set(ProduceBatchDestination po);

    /**
     * 取消当前批次的激活目的地
     *
     * @param po 目的地设置信息（需包含 batchId）
     */
    Integer cancel(ProduceBatchDestination po);
}
