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
     * 2. 汇总该托盘下 goods 扫码状态，回写 tray_status（0未扫/1部分/2全扫）
     * 3. 写入发送目的地（全扫=destinationCode，否则=destinationCode+"3"）
     * 4. 标记 send_status=1（已发送）
     *
     * @param palletId        前端找到的托盘ID
     * @param virtualId       前端找到的托盘虚拟ID（双条件校验）
     * @param destinationCode 前端传入的当前激活目的地编码
     * @return 更新后的托盘详情
     */
    PalletDetailDTO sendDestination(Long palletId, String virtualId, String destinationCode);
}
