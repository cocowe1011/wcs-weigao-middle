package com.middle.wcs.produce.service;

import com.middle.wcs.produce.entity.dto.MatchAndAssignDTO;
import com.middle.wcs.produce.entity.dto.PalletDetailDTO;
import com.middle.wcs.produce.entity.dto.SendDestinationDTO;
import com.middle.wcs.produce.entity.po.ProducePallet;

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
     * @param po 托盘信息（需包含 id 和 virtualId）
     * @return 更新后的托盘详情（含货物列表）
     */
    PalletDetailDTO assignVirtualId(ProducePallet po);

    /**
     * 发送目的地：
     * 前端已找到目标托盘（palletId + virtualId），后端负责：
     * 1. palletId + virtualId 双条件定位托盘（校验一致性）
     * 2. 将传入的barcodes对应的货物标记为01006已扫码（可选）
     * 3. 汇总该托盘下 goods 扫码状态，全部扫码完成(tray_status=2)才允许发送
     * 4. 非全扫抛异常（前端写999），全扫以1/2交替确定后缀
     * 5. 写入发送目的地编码（destinationCode + 后缀）
     * 6. 标记 send_status=1（已发送）
     * skipScanCheck=true时跳过步骤3-4的扫码判断，直接赋值目的地编码+后缀
     *
     * @param dto 发送目的地请求 DTO
     * @return 更新后的托盘详情
     */
    PalletDetailDTO sendDestination(SendDestinationDTO dto);

    /**
     * 根据扫描条码匹配托盘并分配虚拟ID（原子操作）
     * 1. 在指定批次的托盘中，查找任一货物的uid存在于barcodes中的、且尚未分配虚拟ID的托盘
     * 2. 匹配成功：查询该批次已分配的最大虚拟ID，递增1生成新虚拟ID，持久化到数据库
     * 3. 匹配成功后，批量更新所有barcodes对应货物的01002扫码状态（scan_status=1）
     * 4. 匹配失败：返回null
     *
     * @param dto 匹配分配虚拟ID请求 DTO
     * @return 匹配成功返回托盘详情，匹配失败返回null
     */
    PalletDetailDTO matchAndAssignVirtualId(MatchAndAssignDTO dto);

    /**
     * 根据货物UID查询所属托盘信息（简化接口）
     * 仅返回托盘详情（托盘基本信息 + 货物列表），不包含批次完整数据
     *
     * @param uid 货物唯一码
     * @return 托盘详情 DTO，若不存在则返回 null
     */
    PalletDetailDTO getByGoodsUid(String uid);

    /**
     * 故障托盘重新发送目的地（999托盘修正为正确目的地编码）
     * 仅允许 sendDestinationCode='999' 的托盘重新发送
     * 1. 定位托盘，校验为999异常托盘
     * 2. 计算1/2后缀（复用全扫后缀交替逻辑）
     * 3. 更新 sendDestinationCode 为正确目的地编码
     *
     * @param dto 重新发送目的地请求 DTO
     * @return 更新后的托盘详情
     */
    PalletDetailDTO resendDestination(SendDestinationDTO dto);

    /**
     * 真删托盘：删除托盘及其下属所有货物
     *
     * @param po 托盘信息（需包含 id）
     */
    Integer deletePallet(ProducePallet po);
}
