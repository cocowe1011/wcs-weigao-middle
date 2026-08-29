package com.middle.wcs.produce.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.middle.wcs.produce.entity.po.ProducePallet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 生产托盘 Mapper
 */
@Mapper
public interface ProducePalletMapper extends BaseMapper<ProducePallet> {

    /**
     * 根据批次ID查询托盘列表
     */
    List<ProducePallet> selectByBatchId(Long batchId);

    /**
     * 为指定托盘分配虚拟ID，同步更新上货状态和上货时间
     */
    int assignVirtualId(@Param("palletId") Long palletId, @Param("virtualId") String virtualId);

    /**
     * 查询批次中第一个未发送目的地（send_status=0）且已上货（load_status=1）的托盘
     */
    ProducePallet selectFirstUnsentByBatchId(@Param("batchId") Long batchId);

    /**
     * 用 palletId + virtualId 两个条件同时定位托盘
     */
    ProducePallet selectByIdAndVirtualId(@Param("palletId") Long palletId, @Param("virtualId") String virtualId);

    /**
     * 回写托盘扫码汇总状态 + 目的地发送信息（palletId + virtualId 双条件更新）
     */
    int sendDestination(@Param("palletId") Long palletId,
                        @Param("virtualId") String virtualId,
                        @Param("trayStatus") String trayStatus,
                        @Param("sendDestinationCode") String sendDestinationCode);

    /**
     * 查询批次中所有已发送目的地的托盘（send_status=1），
     * 按 send_time DESC 排序，用于追溯后缀交替逻辑（1/2/3）
     */
    List<ProducePallet> selectSentByBatchIdDesc(@Param("batchId") Long batchId);

    /**
     * 根据条码列表匹配批次中尚未分配虚拟ID的托盘
     * 只要任一货物的uid在barcodes列表中即命中，返回第一个匹配的托盘
     */
    ProducePallet selectUnassignedByBarcodes(@Param("batchId") Long batchId, @Param("barcodes") List<String> barcodes);

    /**
     * 查询当日已分配的虚拟ID列表（10000-29999范围内，按 load_time 当天）
     * 用于生成当日不重复的下一个虚拟ID
     */
    List<Integer> selectUsedVirtualIdsToday();
    /**
     * 真删：按ID删除托盘
     */
    int deletePalletById(@Param("palletId") Long palletId);

    /**
     * 真删：按批次ID删除其下所有托盘（作废批次用）
     */
    int deleteByBatchId(@Param("batchId") Long batchId);
}
