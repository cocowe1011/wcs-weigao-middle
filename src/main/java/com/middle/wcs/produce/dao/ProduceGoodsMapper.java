package com.middle.wcs.produce.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.middle.wcs.produce.entity.po.ProduceGoods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 生产货物 Mapper
 */
@Mapper
public interface ProduceGoodsMapper extends BaseMapper<ProduceGoods> {

    /**
     * 根据 uid 查询货物（含批次和托盘ID）
     */
    ProduceGoods selectByUid(@Param("uid") String uid);

    /**
     * 根据托盘ID查询货物列表
     */
    List<ProduceGoods> selectByPalletId(@Param("palletId") Long palletId);

    /**
     * 批量插入货物
     */
    int batchInsert(@Param("list") List<ProduceGoods> list);

    /**
     * 扫码回写：将指定 uid 的货物标记为已扫码，覆盖 scan_location 和 scan_time
     */
    int markScanned(@Param("uid") String uid, @Param("scanLocation") String scanLocation);
}
