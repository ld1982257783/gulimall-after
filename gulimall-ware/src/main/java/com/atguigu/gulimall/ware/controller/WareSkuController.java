package com.atguigu.gulimall.ware.controller;

import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.exception.NoStockException;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.atguigu.gulimall.ware.vo.SkuHasStockVo;
import com.atguigu.gulimall.ware.vo.WareSkuLockVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;



/**
 * 商品库存
 *
 * @author lida
 * @email sunlightcs@gmail.com
 * @date 2021-03-13 12:54:52
 */
@RestController
@RequestMapping("ware/waresku")
public class WareSkuController {
    @Autowired
    private WareSkuService wareSkuService;




    @PostMapping("/lock/order")
    public R orderLockStock(@RequestBody WareSkuLockVo vo){

//        int i = 1/0;
//        System.out.println(i);
//        return R.error(BizCodeEnume.NO_STOCK_EXCEPTION.getCode(),BizCodeEnume.NO_STOCK_EXCEPTION.getMessage());

        try {
            Boolean stock = wareSkuService.orderLockSkuVo(vo);
            return R.ok();
        }catch (NoStockException e){
            return R.error(BizCodeEnume.NO_STOCK_EXCEPTION.getCode(),BizCodeEnume.NO_STOCK_EXCEPTION.getMessage());
        }

    }


    /**
     *
     * 查询sku对应的商品是否拥有库存
     */
    @PostMapping("/hasstock")
    public R getSkuHasStock(@RequestBody List<Long> skuIds){
        List<SkuHasStockVo> vos = wareSkuService.getSkuHasStock(skuIds);
        return R.ok().setData(vos);
    }





    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("ware:waresku:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = wareSkuService.queryPageByCondition(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("ware:waresku:info")
    public R info(@PathVariable("id") Long id){
		WareSkuEntity wareSku = wareSkuService.getById(id);

        return R.ok().put("wareSku", wareSku);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("ware:waresku:save")
    public R save(@RequestBody WareSkuEntity wareSku){
		wareSkuService.save(wareSku);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("ware:waresku:update")
    public R update(@RequestBody WareSkuEntity wareSku){
		wareSkuService.updateById(wareSku);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("ware:waresku:delete")
    public R delete(@RequestBody Long[] ids){
		wareSkuService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
