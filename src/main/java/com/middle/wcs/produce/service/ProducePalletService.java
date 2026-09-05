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
     * 1. 收紧匹配：提交条码必须“全部”归属当前批次同一个尚未分配虚拟ID的托盘才命中；
     *    有任一条码不属于该托盘（错扫到别批次/混托）则不匹配、拒绝上货（允许漏扫，不要求扫全托盘所有货物）
     * 2. 匹配成功：按当日已分配虚拟ID递增生成（10000-29999，当日不重复，次日从10000重新开始），持久化到数据库
     * 3. 匹配成功后，批量更新所有barcodes对应货物的01002扫码状态（scan_status=1）
     * 4. 未匹配到托盘：逐条码归因（不属于当前批次/货物或托盘已作废/托盘已分配虚拟ID等），
     *    抛 BusinessException（code=201）把详细原因带回前端展示；barcodes 为空时返回 null
     *
     * @param dto 匹配分配虚拟ID请求 DTO
     * @return 匹配成功返回托盘详情；barcodes 为空返回 null；未匹配到托盘抛 BusinessException
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
     * 恢复建档状态：把托盘及其货物同步重置为刚建档时的状态（不删数据）
     * 1. 托盘：清空虚拟ID、上货状态/时间、扫码汇总状态、目的地发送信息
     * 2. 货物：扫码状态回到未扫，清空扫码位置与扫码时间，uid 等建档数据保留
     * 3. 批次：若已自动完结则回退为生产中并恢复激活目的地，保证不影响下一次上货
     *
     * @param po 托盘信息（需包含 id）
     * @return 恢复后的托盘详情（含货物列表）
     */
    PalletDetailDTO resetPallet(ProducePallet po);

    /**
     * 真删托盘：删除托盘及其下属所有货物
     *
     * @param po 托盘信息（需包含 id）
     */
    Integer deletePallet(ProducePallet po);
}
