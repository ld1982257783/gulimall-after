package com.atguigu.gulimall.ware.service;

import com.atguigu.common.mq.OrderTo;
import com.atguigu.common.mq.StockLockedTo;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.atguigu.gulimall.ware.vo.SkuHasStockVo;
import com.atguigu.gulimall.ware.vo.WareSkuLockVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author lida
 * @email sunlightcs@gmail.com
 * @date 2021-03-13 12:54:52
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPageByCondition(Map<String, Object> params);

    void addSock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds);

    Boolean orderLockSkuVo(WareSkuLockVo vo);


    void unlockStock(StockLockedTo stockLockedTo);

    void unlockStock(OrderTo orderTo);
}

