package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.TO.SkuBoundTo;
import com.atguigu.common.TO.SkuReductionTo;
import com.atguigu.common.TO.es.SkuEsModel;
import com.atguigu.common.constant.StatusEnum;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.VO.*;
import com.atguigu.gulimall.product.dao.SpuInfoDao;
import com.atguigu.gulimall.product.entity.*;
import com.atguigu.gulimall.product.feign.CouponFeignService;
import com.atguigu.gulimall.product.feign.SearchFeignService;
import com.atguigu.gulimall.product.feign.WareFeignService;
import com.atguigu.gulimall.product.service.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {


    @Autowired
    SpuInfoDescService spuInfoDescService;

    @Autowired
    SpuImagesService spuImagesService;

    @Autowired
    AttrService attrService;

    @Autowired
    ProductAttrValueService productAttrValueService;

    @Autowired
    SkuImagesService skuImagesService;

    @Autowired
    SkuInfoService skuInfoService;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;


    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    BrandService brandService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    SearchFeignService searchFeignService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }


    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo vo) {
        //1 ??????spu??????????????? pms_spu_info
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo,spuInfoEntity);
        spuInfoEntity.setCreateTime(new Date());
        spuInfoEntity.setUpdateTime(new Date());
        spuInfoEntity.setCatalogId((long) vo.getCatalogId());
        spuInfoEntity.setBrandId((long) vo.getBrandId());
        spuInfoEntity.setWeight(BigDecimal.valueOf(vo.getWeight()));
        this.saveBaseSpuInfo(spuInfoEntity);


        //2 ??????SPU???????????????  pms_spu_info_desc
        List<String> decript = vo.getDecript();
        SpuInfoDescEntity spuInfoDescEntity = new SpuInfoDescEntity();
        spuInfoDescEntity.setSpuId(spuInfoEntity.getId());
        spuInfoDescEntity.setDecript(String.join(",",decript));
        spuInfoDescService.saveSpuInfoDesc(spuInfoDescEntity);


        //3  ??????spu????????????   pms_spu_images
        List<String> images = vo.getImages();
        spuImagesService.saveSpuImages(images,spuInfoEntity.getId());

        //4 ??????spu???????????????  pms_product_attr_value
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
        List<ProductAttrValueEntity> collect = baseAttrs.stream().map(attr -> {
            ProductAttrValueEntity ValueEntity = new ProductAttrValueEntity();
            ValueEntity.setAttrId((long) attr.getAttrId());
            AttrEntity byId = attrService.getById(attr.getAttrId());
            ValueEntity.setAttrName(byId.getAttrName());
            ValueEntity.setAttrValue(attr.getAttrValues());
            ValueEntity.setQuickShow(attr.getShowDesc());
            ValueEntity.setSpuId(spuInfoEntity.getId());
            return ValueEntity;
        }).collect(Collectors.toList());

        productAttrValueService.saveProductAttr(collect);

        //TODO
        //SPU????????????
        Bounds bounds = vo.getBounds();
        SkuBoundTo skuBoundTo = new SkuBoundTo();
        skuBoundTo.setSpuId(spuInfoEntity.getId());
        skuBoundTo.setBuyBounds(bounds.getBuyBounds());
        skuBoundTo.setGrowBounds(bounds.getGrowBounds());
        R r = couponFeignService.saveSpuBounds(skuBoundTo);
        if(r.getCode() != 0){
            log.error("??????????????????????????????");
        }


        //5 ??????spu???????????????  gulimall_sms -> sms_spu_bounds
        List<Skus> skus = vo.getSkus();
        if(skus != null && skus.size() > 0){
            for (Skus item : skus) {//??????????????????
                String defaultImg = "";
                for (Images image : item.getImages()) {
                    if (image.getDefaultImg() == 1) {
                        defaultImg = image.getImgUrl();
                    }
                }

                //5???????????????spu???????????????sku??????
                //5.1  sku???????????????  pms_sku_info
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(item, skuInfoEntity);
                skuInfoEntity.setBrandId(spuInfoEntity.getBrandId());
                skuInfoEntity.setCatalogId(spuInfoEntity.getCatalogId());
                skuInfoEntity.setSaleCount(0L);
                skuInfoEntity.setSpuId(spuInfoEntity.getId());
                skuInfoEntity.setSkuDefaultImg(defaultImg);
                skuInfoEntity.setPrice(item.getPrice());
                skuInfoService.saveSkuInfo(skuInfoEntity);

                //??????sku???????????????
                Long skuId = skuInfoEntity.getSkuId();


                //5.2  sku???????????????  pms_sku_images
                List<SkuImagesEntity> collect1 = item.getImages().stream().map(img -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setDefaultImg(img.getDefaultImg());
                    skuImagesEntity.setImgUrl(img.getImgUrl());
                    return skuImagesEntity;
                }).filter(entity -> {
                    return !StringUtils.isEmpty(entity.getImgUrl());
                }).collect(Collectors.toList());

                //TODO  ?????????????????????????????????
                skuImagesService.saveBatch(collect1);


                //5.3  sku?????????????????????  pms_sku_sale_attr_value
                List<Attr> attr = item.getAttr();
                List<SkuSaleAttrValueEntity> collect2 = attr.stream().map(a -> {
                    SkuSaleAttrValueEntity skuSaleAttrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(a, skuSaleAttrValueEntity);
                    skuSaleAttrValueEntity.setSkuId(skuId);

                    return skuSaleAttrValueEntity;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(collect2);


                //5.4  ??????sku??????????????????  SKU????????????
                //TODO
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(item,skuReductionTo);
                skuReductionTo.setFullCount(item.getFullCount());
                skuReductionTo.setDiscount(item.getDiscount());
                skuReductionTo.setFullPrice(item.getFullPrice());
                skuReductionTo.setReducePrice(item.getReducePrice());
                skuReductionTo.setSkuId(skuId);
                System.out.println("??????????????????  ????????????"+skuReductionTo.getFullCount()+
                        ","+skuReductionTo.getFullPrice()
                        +","+skuReductionTo.getDiscount()+
                        ","+skuReductionTo.getReducePrice());
                if(skuReductionTo.getFullCount() >=0 || skuReductionTo.getFullPrice() >= 0){
                    R r1 = couponFeignService.saveSkuReduction(skuReductionTo);
                    if(r1.getCode() != 0){
                        log.error("??????????????????????????????");
                    }
                }


            }
        }




    }

    @Override
    public void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity) {
            //??????????????????
            this.baseMapper.insert(spuInfoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {

        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();

        /**
         * status 2
         * key
         * catelogId
         * brandid
         *
         */
        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.and(w -> {
               w.eq("id",key).or().like("spu_name",key);
            });
        }
        // sattus=1 and (id=1 or spu_name like xxx)

        String status = (String) params.get("status");
        if(!StringUtils.isEmpty(status)){
            wrapper.eq("publish_status",status);
        }

        String brandId = (String) params.get("brandId");
        if(!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)){
            wrapper.eq("brand_id",brandId);
        }

        String catelogId = (String) params.get("catelogId");
        if(!StringUtils.isEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)){
            wrapper.eq("catalog_id",catelogId);
        }


        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
              wrapper
        );


        return new PageUtils(page);
    }

    @Override
    public void up(Long spuId) {

        //?????????????????????
        //1 ????????????spuid??????????????????????????? ???????????????
        List<SkuInfoEntity> skuInfoEntities = skuInfoService.getSkuBySpuId(spuId);
        List<Long> skuIdList = skuInfoEntities.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());

        //TODO ????????????sku?????????????????????
        List<ProductAttrValueEntity> baseAttrs = productAttrValueService.baseAttrlistforspu(spuId);
        List<Long> collect1 = baseAttrs.stream().map(attr -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());

        List<Long> SearchAttrId = attrService.selectSearchAttrsIds(collect1);
        Set<Long> set = new HashSet<>(SearchAttrId);

        List<SkuEsModel.Attrs> collect2 = baseAttrs.stream().filter(item -> {
            return set.contains(item.getAttrId());
        }).map(item -> {
            SkuEsModel.Attrs attrs1 = new SkuEsModel.Attrs();
            BeanUtils.copyProperties(item,attrs1);
            return attrs1;
        }).collect(Collectors.toList());

        //TODO ?????????????????? ??????????????????????????????
        Map<Long, Boolean> stockMap = null;
        try {
            R skuHasStock = wareFeignService.getSkuHasStock(skuIdList);
            TypeReference<List<SkuHasStockVo>> typeReference = new TypeReference<List<SkuHasStockVo>>() {
            };
            stockMap = skuHasStock.getData(typeReference).stream().collect(Collectors.toMap(SkuHasStockVo::getSkuId, item -> item.getHasStock()));

        }catch (Exception e){
            log.error("?????????????????????????????????{}",e);
        }

        //2 ????????????sku?????????
        Map<Long, Boolean> finalStockMap = stockMap;
        List<SkuEsModel> upProducts = skuInfoEntities.stream().map(sku -> {
            SkuEsModel esModel = new SkuEsModel();
            BeanUtils.copyProperties(sku, esModel);
            //skuPrice skuimg
            esModel.setSkuPrice(sku.getPrice());
            esModel.setSkuImg(sku.getSkuDefaultImg());
            //hasstock hotscore
            if(finalStockMap == null){
                esModel.setHasStock(true);
            }else{
                esModel.setHasStock(finalStockMap.get(sku.getSkuId()));
            }

            //TODO ???????????? 0
            esModel.setHotScore(0L);

            //TODO 3????????????????????????????????????
            BrandEntity byId = brandService.getById(esModel.getBrandId());
            esModel.setBrandName(byId.getName());
            esModel.setBrandImg(byId.getLogo());

            CategoryEntity byId1 = categoryService.getById(esModel.getCatalogId());
            esModel.setCatalogName(byId1.getName());

            //??????????????????
            esModel.setAttrs(collect2);

            return esModel;
        }).collect(Collectors.toList());

        //TODO  ??????es???????????? gulimall-search
        R r = searchFeignService.productStatusUp(upProducts);
        if(r.getCode() == 0){
            //??????????????????
            //TODO ????????????spu?????????
            baseMapper.updateSpuStatus(spuId, StatusEnum.SPU_UP.getCode());
        }else{
            //??????????????????
            //TODO ??????????????? ??????????????? ????????????  xxx
        }


    }

    @Override
    public SpuInfoEntity getSpuInfoBySkuId(Long skuId) {

        SkuInfoEntity byId = skuInfoService.getById(skuId);
        Long spuId = byId.getSpuId();
        SpuInfoEntity byId1 = getById(spuId);

        return byId1;
    }


}