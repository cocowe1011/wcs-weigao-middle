package com.middle.wcs.produce.service;

import com.middle.wcs.produce.entity.po.ProduceGoods;

/**
 * 生产货物 Service 接口
 */
public interface ProduceGoodsService {

    /**
     * 扫码回写：将指定 uid 的货物标记为已扫码（scan_status=1），
     * 同时覆盖更新 scan_location 和 scan_time。
     *
     * @param po 货物信息（需包含 uid 和 scanLocation）
     */
    Integer markScanned(ProduceGoods po);
    /**
     * 真删：按主键ID删除单条货物
     *
     * @param po 货物信息（需包含 id）
     */
    Integer deleteById(ProduceGoods po);
}
