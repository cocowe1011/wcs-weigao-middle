package com.middle.wcs.produce.entity.dto;

import com.middle.wcs.produce.entity.po.ProduceBatch;
import lombok.Data;

import java.util.List;

/**
 * 批次详情 DTO（含托盘+货物嵌套结构）
 */
@Data
public class BatchDetailDTO {

    /** 批次基本信息 */
    private ProduceBatch batch;

    /** 托盘列表（每个托盘含货物列表） */
    private List<PalletDetailDTO> pallets;

    public static BatchDetailDTO of(ProduceBatch batch, List<PalletDetailDTO> pallets) {
        BatchDetailDTO dto = new BatchDetailDTO();
        dto.setBatch(batch);
        dto.setPallets(pallets);
        return dto;
    }
}
