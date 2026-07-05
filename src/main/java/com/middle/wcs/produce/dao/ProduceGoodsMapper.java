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
     * 根据多个托盘ID批量查询货物列表（避免循环内单条查询）
     */
    List<ProduceGoods> selectByPalletIds(@Param("palletIds") List<Long> palletIds);

    /**
     * 批量插入货物（一条 SQL 插入多行，避免逐条 insert 的大量网络往返）。
     * 注意：调用前需为每条记录设置好主键 id。
     */
    int insertBatch(@Param("list") List<ProduceGoods> list);

    /**
     * 扫码回写：将指定 uid 的货物标记为已扫码，覆盖 scan_location 和 scan_time
     */
    int markScanned(@Param("uid") String uid, @Param("scanLocation") String scanLocation);
    /**
     * 真删：按托盘ID删除其下所有货物
     */
    int deleteByPalletId(@Param("palletId") Long palletId);
}
