package com.atguigu.gulimall.product;

import com.atguigu.gulimall.product.VO.SkuItemSaleAttrVo;
import com.atguigu.gulimall.product.VO.SpuItemAttrGroupVo;
import com.atguigu.gulimall.product.dao.SkuSaleAttrValueDao;
import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.service.BrandService;
import com.atguigu.gulimall.product.service.impl.AttrGroupServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.UUID;

@SpringBootTest
class GulimallProductApplicationTests {

    @Autowired
    BrandService brandService;

//    @Autowired
//    OSSClient ossClient;

//    测试redis
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    AttrGroupServiceImpl attrGroupService;

    @Autowired
    SkuSaleAttrValueDao skuSaleAttrValueDao;

    @Test
    void contextLoads() {
//        BrandEntity brandEntity = new BrandEntity();
//        brandEntity.setName("华为");
//        brandEntity.setLogo("华为");
//        brandService.save(brandEntity);


//        BrandEntity brandEntity = new BrandEntity();
//        brandEntity.setBrandId(1L);
//        brandEntity.setName("苹果");
//        boolean b = brandService.updateById(brandEntity);


        List<BrandEntity> brand_id = brandService.list(new QueryWrapper<BrandEntity>().eq("brand_id", 1L));
        brand_id.forEach((item)->{
            System.out.println(item);
        });


    }

    @Test
    void OSSTest() throws FileNotFoundException {
        System.out.println("上传成功");
    }


    @Test
    void redistext1(){
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        ops.set("hello","word"+ UUID.randomUUID().toString());
        String hello = ops.get("hello");
        System.out.println("查询到的值为"+hello);
    }



    @Test
    void text3(){
        List<SpuItemAttrGroupVo> attrGroupWithAttrsBySpuId = attrGroupService.getAttrGroupWithAttrsBySpuId(3L, 225L);
        System.out.println(attrGroupWithAttrsBySpuId);
    }


    @Test
    void text4(){
        List<SkuItemSaleAttrVo> saleAttrsBySpuuId = skuSaleAttrValueDao.getSaleAttrsBySpuuId(3L);
        System.out.println(saleAttrsBySpuuId);
    }
}
