package com.middle.wcs.produce.service.impl;

import com.middle.wcs.produce.dao.ProduceBatchDestinationMapper;
import com.middle.wcs.produce.dao.ProduceBatchMapper;
import com.middle.wcs.produce.entity.po.ProduceBatch;
import com.middle.wcs.produce.entity.po.ProduceBatchDestination;
import com.middle.wcs.produce.service.ProduceBatchDestinationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 批次目的地设置 Service 实现
 */
@Service
public class ProduceBatchDestinationServiceImpl implements ProduceBatchDestinationService {

    @Resource
    private ProduceBatchDestinationMapper destinationMapper;

    @Resource
    private ProduceBatchMapper produceBatchMapper;

    @Override
    public ProduceBatchDestination getCurrentByBatchId(Long batchId) {
        return destinationMapper.selectActiveByBatchId(batchId);
    }

    @Override
    @Transactional
    public ProduceBatchDestination set(ProduceBatchDestination po) {
        Long batchId = po.getBatchId();
        String destinationCode = po.getDestinationCode();
        ProduceBatch batch = produceBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new RuntimeException("批次不存在: " + batchId);
        }
        if (!"1".equals(batch.getStatus()) && !"2".equals(batch.getStatus())) {
            throw new RuntimeException("批次未允许生产，无法设置目的地");
        }
        validateDestinationCode(destinationCode);

        // 已有激活目的地时拒绝再次设置，必须先取消
        ProduceBatchDestination existing = destinationMapper.selectActiveByBatchId(batchId);
        if (existing != null) {
            throw new RuntimeException("当前批次已设置目的地 " + existing.getDestinationCode() + "，请先取消后再重新设置");
        }

        // 新建激活记录
        ProduceBatchDestination dest = new ProduceBatchDestination();
        dest.setBatchId(batchId);
        dest.setDestinationCode(destinationCode);
        dest.setStatus("0");
        dest.setSetTime(new Date());
        dest.setInvalidFlag("0");
        dest.setCreatedAt(new Date());
        destinationMapper.insert(dest);
        return dest;
    }

    @Override
    @Transactional
    public Integer cancel(ProduceBatchDestination po) {
        Long batchId = po.getBatchId();
        ProduceBatchDestination existing = destinationMapper.selectActiveByBatchId(batchId);
        if (existing == null) {
            throw new RuntimeException("当前批次没有激活的目的地设置");
        }
        existing.setStatus("1");
        existing.setCancelTime(new Date());
        return destinationMapper.updateById(existing);
    }

    private void validateDestinationCode(String code) {
        try {
            int num = Integer.parseInt(code);
            if (num < 3201 || num > 3215) {
                throw new RuntimeException("目的地编码必须在 3201~3215 范围内");
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException("目的地编码格式错误: " + code);
        }
    }
}
