package com.middle.wcs.produce.service;

import com.middle.wcs.produce.entity.dto.PalletDetailDTO;

import java.util.List;

/**
 * 生产托盘 Service 接口
 */
public interface ProducePalletService {

    /**
     * 查询批次下所有托盘列表（含扫码状态、发送状态等完整字段）
     */
    List<PalletDetailDTO> listByBatchId(Long batchId);

    /**
     * 为托盘分配虚拟ID（同步标记上货状态），要求托盘归属的批次处于运行状态
     *
     * @param palletId  托盘ID
     * @param virtualId 虚拟ID（PC端生成的时间戳ID）
     * @return 更新后的托盘详情（含货物列表）
     */
    PalletDetailDTO assignVirtualId(Long palletId, String virtualId);

    /**
     * 发送目的地：
     * 前端已找到目标托盘（palletId + virtualId），后端负责：
     * 1. palletId + virtualId 双条件定位托盘（校验一致性）
     * 2. 将传入的barcodes对应的货物标记为01006已扫码（可选）
     * 3. 汇总该托盘下 goods 扫码状态，全部扫码完成(tray_status=2)才允许发送
     * 4. 非全扫抛异常（前端写999），全扫以1/2交替确定后缀
     * 5. 写入发送目的地编码（destinationCode + 后缀）
     * 6. 标记 send_status=1（已发送）
     *
     * @param palletId        前端找到的托盘ID
     * @param virtualId       前端找到的托盘虚拟ID（双条件校验）
     * @param destinationCode 前端传入的当前激活目的地编码
     * @param barcodes        01006扫码缓存的条码列表
     * @return 更新后的托盘详情
     */
    PalletDetailDTO sendDestination(Long palletId, String virtualId, String destinationCode, List<String> barcodes);

    /**
     * 根据扫描条码匹配托盘并分配虚拟ID（原子操作）
     * 1. 在指定批次的托盘中，查找任一货物的uid存在于barcodes中的、且尚未分配虚拟ID的托盘
     * 2. 匹配成功：查询该批次已分配的最大虚拟ID，递增1生成新虚拟ID，持久化到数据库
     * 3. 匹配成功后，批量更新所有barcodes对应货物的01002扫码状态（scan_status=1）
     * 4. 匹配失败：返回null
     *
     * @param batchId  当前批次ID
     * @param barcodes 扫描到的条码列表
     * @return 匹配成功返回托盘详情，匹配失败返回null
     */
    PalletDetailDTO matchAndAssignVirtualId(Long batchId, List<String> barcodes);

    /**
     * 故障托盘重新发送目的地（999托盘修正为正确目的地编码）
     * 仅允许 sendDestinationCode='999' 的托盘重新发送
     * 1. 定位托盘，校验为999异常托盘
     * 2. 计算1/2后缀（复用全扫后缀交替逻辑）
     * 3. 更新 sendDestinationCode 为正确目的地编码
     *
     * @param palletId        托盘ID
     * @param virtualId       托盘虚拟ID（双条件校验）
     * @param destinationCode 当前激活的目的地编码
     * @return 更新后的托盘详情
     */
    PalletDetailDTO resendDestination(Long palletId, String virtualId, String destinationCode);
}
