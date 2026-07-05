package com.middle.wcs.order.service;

import com.middle.wcs.order.entity.po.QueueInfo;
import org.springframework.validation.annotation.Validated;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * (QueueInfo)表服务接口
 *
 * @author makejava
 * @since 2025-01-01 12:44:45
 */
@Validated
public interface QueueInfoService {


    /**
     * 修改数据
     *
     * @param entity 实例对象
     * @return 成功为1，失败为0
     */
    int update(@NotNull QueueInfo entity);

    /**
     * 查询队列信息列表
     * @return 出参
     */
    List<QueueInfo> queryQueueList();

    QueueInfo getQueueInfoById(Long id);

    /**
     * 根据预热柜/灭菌柜编号（队列名，如 Y3201~Y3215 / 3201~3215）查询该队列当前托盘对应的真实订单信息。
     * 出参 data 结构严格参考对外接口文档：{ queueNum, pallet_list:[{ sterilization_order_no,
     * process_plan_name_code, pallet_code, to_warehouse, material_details:[...] }] }。
     *
     * @param queueCode 预热柜/灭菌柜编号（队列名）
     * @return data 数据体
     */
    Map<String, Object> getProductInfo(String queueCode);
}
