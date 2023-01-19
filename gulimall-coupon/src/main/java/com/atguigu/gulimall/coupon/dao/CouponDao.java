package com.atguigu.gulimall.coupon.dao;

import com.atguigu.gulimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author lida
 * @email sunlightcs@gmail.com
 * @date 2021-03-13 12:47:05
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
