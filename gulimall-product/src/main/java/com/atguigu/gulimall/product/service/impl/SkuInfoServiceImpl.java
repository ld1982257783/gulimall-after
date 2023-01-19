package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.VO.SeckillInfoVo;
import com.atguigu.gulimall.product.VO.SkuItemSaleAttrVo;
import com.atguigu.gulimall.product.VO.SkuItemVo;
import com.atguigu.gulimall.product.VO.SpuItemAttrGroupVo;
import com.atguigu.gulimall.product.dao.SkuInfoDao;
import com.atguigu.gulimall.product.dao.SpuInfoDescDao;
import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import com.atguigu.gulimall.product.feign.SeckillFeignService;
import com.atguigu.gulimall.product.service.SkuImagesService;
import com.atguigu.gulimall.product.service.SkuInfoService;
import com.atguigu.gulimall.product.service.SkuSaleAttrValueService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {



    @Autowired
    SkuImagesService skuImagesService;

    @Autowired
    SpuInfoDescDao spuInfoDescDao;

    @Autowired
    AttrGroupServiceImpl attrGroupService;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    //注入线程池
    @Autowired
    ThreadPoolExecutor executor;

    @Autowired
    SeckillFeignService seckillFeignService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveSkuInfo(SkuInfoEntity skuInfoEntity) {

        this.baseMapper.insert(skuInfoEntity);
    }

    @Override
    public PageUtils queryPageBycondition(Map<String, Object> params) {
        QueryWrapper<SkuInfoEntity> wrapper = new QueryWrapper<>();

        /**
         * key
         * catelogId
         * brandId
         * min
         * max
         *
         */

        String key = (String) params.get("key");
        if(!StringUtils.isEmpty("key")){
            wrapper.and(w -> {
               w.eq("sku_id",key).or().like("sku_name",key);
            });
        }

        String catelogId = (String) params.get("catelogId");
        if(!StringUtils.isEmpty("catelogId") && !"0".equalsIgnoreCase(catelogId)){
            wrapper.eq("catalog_id",catelogId);
        }

        String brandId = (String) params.get("brandId");
        if(!StringUtils.isEmpty("brandId") && !"0".equalsIgnoreCase(brandId)){
            wrapper.eq("brand_id",brandId);
        }

        String min = (String) params.get("min");
        if(!StringUtils.isEmpty("min")){
            //gte  大于    ge 大于等于
            wrapper.ge("price",min);
        }

        String max = (String) params.get("max");
        if(!StringUtils.isEmpty("max")){
            try {
                BigDecimal bigDecimal = new BigDecimal(max);
                if(bigDecimal.compareTo(new BigDecimal("0")) ==1){
                    wrapper.le("price",max);
                }
            }catch (Exception e){

            }
        }


        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public List<SkuInfoEntity> getSkuBySpuId(Long spuId) {
        List<SkuInfoEntity> list = this.list(new QueryWrapper<SkuInfoEntity>().eq("spu_id", spuId));
        return list;
    }

    @Override
    public SkuItemVo item(Long skuId) throws ExecutionException, InterruptedException {
        SkuItemVo skuItemVo = new SkuItemVo();

        //开始异步编排
        CompletableFuture<SkuInfoEntity> infoFuture = CompletableFuture.supplyAsync(() -> {
            //1  sku基本信息的获取  pms_sku_info
            SkuInfoEntity skuInfoEntity = this.baseMapper.selectById(skuId);
            if (skuInfoEntity != null) {
                skuItemVo.setSkuInfoEntity(skuInfoEntity);
            }
            return skuInfoEntity;
        }, executor);


        CompletableFuture<Void> attrFuture = infoFuture.thenAcceptAsync((res) -> {
            //3  获取spu的所有销售属性组合
            List<SkuItemSaleAttrVo> saleAttrVos = skuSaleAttrValueService.getSaleAttrsBySpuId(res.getSpuId());
            skuItemVo.setSaleAttr(saleAttrVos);
        }, executor);


        CompletableFuture<Void> descFuture = infoFuture.thenAcceptAsync((res) -> {
            //4  获取spu的介绍
            SpuInfoDescEntity spuInfoDescEntity = spuInfoDescDao.selectById(res.getSpuId());
            if (spuInfoDescEntity != null) {
                skuItemVo.setSpuInfoDescEntity(spuInfoDescEntity);
            }
        }, executor);

        CompletableFuture<Void> baseAttrFuture = infoFuture.thenAcceptAsync((res) -> {

            //5  获取规格参数组及组下的规格参数
            List<SpuItemAttrGroupVo> spuItemAttrGroupVos = attrGroupService.getAttrGroupWithAttrsBySpuId(res.getSpuId(), res.getCatalogId());
            skuItemVo.setGroupAttrVos(spuItemAttrGroupVos);
        }, executor);

        //查询当前sku是否参与秒杀优惠
        CompletableFuture<Void> seckillFuture = CompletableFuture.runAsync(() -> {
            R r = seckillFeignService.skuSeckillInfo(skuId);
            if (r.getCode() == 0) {
                SeckillInfoVo seckillVo = r.getData(new TypeReference<SeckillInfoVo>() {});
                skuItemVo.setSeckillInfoVo(seckillVo);
                System.out.println(seckillVo);
            }
        }, executor);

        CompletableFuture<Void> imageFuture = CompletableFuture.runAsync(() -> {
            //2  获取sku图片信息  pms_sku_images
            BaseMapper<SkuImagesEntity> baseMapper = skuImagesService.getBaseMapper();
            List<SkuImagesEntity> skuImagesEntities = baseMapper.selectList(new QueryWrapper<SkuImagesEntity>().eq("sku_id", skuId));

            if (skuImagesEntities != null) {
                skuItemVo.setImagesEntities(skuImagesEntities);
            }
            skuItemVo.setHasStock(true);
        }, executor);





        //等到所有任务都完成   get全部运行完成
//        try {
//            CompletableFuture.anyOf(infoFuture,attrFuture,descFuture,baseAttrFuture,imageFuture,seckillFuture).get();
            CompletableFuture.allOf(infoFuture,attrFuture,descFuture,baseAttrFuture,imageFuture,seckillFuture).get();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        }

        //判断有货

        //System.out.println(skuItemVo);

        return skuItemVo;
    }


}