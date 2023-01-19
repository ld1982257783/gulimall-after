package com.atguigu.gulimall.coupon.service.impl;

import com.atguigu.common.TO.SkuReductionTo;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.coupon.dao.SkuFullReductionDao;
import com.atguigu.gulimall.coupon.entity.MemberPriceEntity;
import com.atguigu.gulimall.coupon.entity.SkuFullReductionEntity;
import com.atguigu.gulimall.coupon.entity.SkuLadderEntity;
import com.atguigu.gulimall.coupon.service.MemberPriceService;
import com.atguigu.gulimall.coupon.service.SkuFullReductionService;
import com.atguigu.gulimall.coupon.service.SkuLadderService;
import com.atguigu.gulimall.product.VO.MemberPrice;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("skuFullReductionService")
public class SkuFullReductionServiceImpl extends ServiceImpl<SkuFullReductionDao, SkuFullReductionEntity> implements SkuFullReductionService {


    @Autowired
    SkuLadderService skuLadderService;

    @Autowired
    SkuFullReductionService skuFullReductionService;

    @Autowired
    MemberPriceService memberPriceService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuFullReductionEntity> page = this.page(
                new Query<SkuFullReductionEntity>().getPage(params),
                new QueryWrapper<SkuFullReductionEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveReduction(SkuReductionTo skuReductionTo) {
        System.out.println("传过来的信息"+skuReductionTo.getFullPrice()+skuReductionTo.getReducePrice());
        //保存满减打折  会员价       gulimall_sms   ->  sms_sku_ladder\sms_sku_full_reduction
        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        skuLadderEntity.setSkuId(skuReductionTo.getSkuId());
        skuLadderEntity.setFullCount(skuReductionTo.getFullCount());
        skuLadderEntity.setDiscount(BigDecimal.valueOf(skuReductionTo.getDiscount()));
        skuLadderEntity.setAddOther(skuReductionTo.getCountStatus());
        System.out.println("是否封装信息"+skuLadderEntity.getDiscount()+skuLadderEntity.getFullCount());
        if(skuLadderEntity.getFullCount() > 0){
            skuLadderService.save(skuLadderEntity);
        }


        //2  sms_sku_full_reducion
        SkuFullReductionEntity skuFullReductionEntity = new SkuFullReductionEntity();
        skuFullReductionEntity.setSkuId(skuReductionTo.getSkuId());
        skuFullReductionEntity.setFullPrice(BigDecimal.valueOf(skuReductionTo.getFullPrice()));
        skuFullReductionEntity.setReducePrice(BigDecimal.valueOf(skuReductionTo.getReducePrice()));
        skuFullReductionEntity.setAddOther(skuReductionTo.getCountStatus());
        if(skuFullReductionEntity.getFullPrice().compareTo(new BigDecimal("0")) == 1){
            this.save(skuFullReductionEntity);
        }

        //3 sms_member_price

        List<MemberPrice> memberPrice = skuReductionTo.getMemberPrice();
        List<MemberPriceEntity> collect = memberPrice.stream().map(item -> {
            MemberPriceEntity PriceEntity = new MemberPriceEntity();
            PriceEntity.setSkuId(skuReductionTo.getSkuId());
            PriceEntity.setMemberLevelName(item.getName());
            PriceEntity.setMemberLevelId((long) item.getId());
            PriceEntity.setMemberPrice(BigDecimal.valueOf(item.getPrice()));
            PriceEntity.setAddOther(1);
            return PriceEntity;

        }).filter(entity -> {
            return entity.getMemberPrice().compareTo(new BigDecimal("0")) == 1;
        }).collect(Collectors.toList());
        memberPriceService.saveBatch(collect);



    }

}