package com.middle.wcs.produce.service;

import com.middle.wcs.produce.entity.dto.BatchDetailDTO;
import com.middle.wcs.produce.entity.dto.BatchHistoryQueryDTO;
import com.middle.wcs.produce.entity.dto.PalletDetailDTO;
import com.middle.wcs.produce.entity.po.ProduceBatch;
import com.middle.wcs.produce.entity.po.ProduceGoods;
import com.middle.wcs.produce.entity.po.ProducePallet;
import com.github.pagehelper.PageInfo;

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
     * 根据批次ID查询批次详情（批次 + 托盘 + 货物嵌套）
     *
     * @param batchId 批次ID
     * @return BatchDetailDTO，若不存在则返回 null
     */
    BatchDetailDTO getBatchDetailById(Long batchId);

    /**
     * 历史批次分页查询（PageHelper）
     *
     * @param query 查询条件
     * @return PageInfo
     */
    PageInfo<ProduceBatch> selectListByPage(BatchHistoryQueryDTO query);

    /**
     * 确认批次，将状态更新为已确认（status=1）。
     * 若已有其他批次正在执行（status=1或2），则抛出业务异常，限定同一时间只能执行一个批次。
     *
     * @param po 批次信息
     */
    Integer confirm(ProduceBatch po);

    /**
     * 取消执行批次，将状态从已确认/执行中（status=1或2）回退为待确认（status=0）
     *
     * @param po 批次信息
     */
    Integer cancel(ProduceBatch po);

    /**
     * 向指定批次添加一个空托盘，托盘号自动生成
     *
     * @param po 托盘信息（需包含 batchId）
     * @return 新托盘的 PalletDetailDTO（含空货物列表）
     */
    PalletDetailDTO addPallet(ProducePallet po);

    /**
     * 向指定托盘添加一件货物（仅 UID 为真实扫码，品名/规格使用模拟数据）
     *
     * @param po 货物信息（需包含 batchId、palletId、uid）
     * @return 添加后的 ProduceGoods
     */
    ProduceGoods addGoods(ProduceGoods po);
}
