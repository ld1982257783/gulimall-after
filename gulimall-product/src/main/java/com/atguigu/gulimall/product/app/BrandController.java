package com.atguigu.gulimall.product.app;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;
import com.atguigu.common.vaild.AddGroup;
import com.atguigu.common.vaild.UpdateGroup;
import com.atguigu.common.vaild.UpdateStatus;
import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;



/**
 * 品牌
 *
 * @author lida
 * @email sunlightcs@gmail.com
 * @date 2021-03-13 12:18:52
 *
 * 给校验结果后紧跟bindresult  就可以获取校验结果
 */
@RestController
@RequestMapping("product/brand")
public class BrandController {

    @Autowired
    private BrandService brandService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:brand:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = brandService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{brandId}")
    //@RequiresPermissions("product:brand:info")
    public R info(@PathVariable("brandId") Long brandId){
		BrandEntity brand = brandService.getById(brandId);

        return R.ok().put("brand", brand);
    }


    @RequestMapping("/info")
    //@RequiresPermissions("product:brand:info")
    public R brandInfo(@RequestParam List<Long> brandEntitys){
        List<BrandEntity> brands = brandService.getBrands(brandEntitys);

        return R.ok().put("brand", brands);
    }


    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:brand:save")
    public R save(@Validated({AddGroup.class}) @RequestBody BrandEntity brand){  //@Validated  可以进行分组校验
//		if(result.hasErrors()){
//		    Map<String,String> map = new HashMap<>();
//		    //获取校验结果
//            result.getFieldErrors().forEach((item) -> {
//                String defaultMessage = item.getDefaultMessage();
//                String field = item.getField();
//                map.put(field,defaultMessage);
//            });
//            return R.error(400,"提交的数据不合法").put("data",map);
//        }else{
//
//        }
            brandService.save(brand);

        return R.ok();

    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:brand:update")
    public R update(@Validated(UpdateGroup.class) @RequestBody BrandEntity brand){
		brandService.updateDetail(brand);

        return R.ok();
    }



    @RequestMapping("/update/status")
    //@RequiresPermissions("product:brand:update")
    public R updateStatus(@Validated(UpdateStatus.class) @RequestBody BrandEntity brand){
        brandService.updateById(brand);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:brand:delete")
    public R delete(@RequestBody Long[] brandIds){
		brandService.removeByIds(Arrays.asList(brandIds));

        return R.ok();
    }

}
