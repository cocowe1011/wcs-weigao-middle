package com.middle.wcs.produce.service;

/**
 * 生产货物 Service 接口
 */
public interface ProduceGoodsService {

    /**
     * 扫码回写：将指定 uid 的货物标记为已扫码（scan_status=1），
     * 同时覆盖更新 scan_location 和 scan_time。
     *
     * @param uid          货物唯一码
     * @param scanLocation 扫码位置（01002 或 01006）
     */
    void markScanned(String uid, String scanLocation);
}
