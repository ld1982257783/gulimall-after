package com.atguigu.gulimall.product.service;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.VO.AttrGroupRelationVo;
import com.atguigu.gulimall.product.VO.AttrRespVo;
import com.atguigu.gulimall.product.VO.AttrVo;
import com.atguigu.gulimall.product.entity.AttrEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 商品属性
 *
 * @author lida
 * @email sunlightcs@gmail.com
 * @date 2021-03-13 12:18:53
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveAttr(AttrVo attr);

    PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId,String type);

    AttrRespVo getAttrInfo(Long attrId);

    void updateAttr(AttrRespVo attr);

    List<AttrEntity> getRelationAttr(Long attrgroupId);

    void deleteRelation(AttrGroupRelationVo[] vos);

    PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId);

    List<Long> selectSearchAttrsIds(List<Long> collect1);

}

