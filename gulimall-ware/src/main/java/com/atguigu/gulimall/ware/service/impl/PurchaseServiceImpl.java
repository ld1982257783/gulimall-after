package com.atguigu.gulimall.ware.service.impl;

import com.atguigu.common.constant.WareConstant;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.ware.dao.PurchaseDao;
import com.atguigu.gulimall.ware.entity.PurchaseDetailEntity;
import com.atguigu.gulimall.ware.entity.PurchaseEntity;
import com.atguigu.gulimall.ware.service.PurchaseDetailService;
import com.atguigu.gulimall.ware.service.PurchaseService;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.atguigu.gulimall.ware.vo.PurchaseDoneVo;
import com.atguigu.gulimall.ware.vo.PurchaseItemDoneVo;
import com.atguigu.gulimall.ware.vo.mergeVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {



    @Autowired
    PurchaseDetailService purchaseDetailService;


    @Autowired
    WareSkuService wareSkuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {



        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageUnreceivePurchase(Map<String, Object> params) {


        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>().eq("status",0).or().eq("status",1)
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void mergePurchase(mergeVo mergeVo) {
        //获取采购单的ID
        Long purchaseId = mergeVo.getPurchaseId();

        if(purchaseId == null){
            //如果没有  新建一个
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATE.getCode());
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            this.save(purchaseEntity);
            purchaseId = purchaseEntity.getId();

        }

        //TODO  确认采购单状态是 0或 1才可以合并

            //获取所有采购需求
            List<Long> items = mergeVo.getItems();
            Long finalPurchaseId = purchaseId;
            //获取当前采购单
            PurchaseEntity byId = this.getById(finalPurchaseId);
            if(byId.getStatus() == WareConstant.PurchaseStatusEnum.CREATE.getCode() ||
               byId.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode()){


                List<PurchaseDetailEntity> collect = items.stream().map(i -> {
                    PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
                    detailEntity.setId(i);
                    detailEntity.setPurchaseId(finalPurchaseId);
                    detailEntity.setStatus(WareConstant.PurchaseStatusEnum.ASSIGNED.getCode());
                    return detailEntity;
                }).collect(Collectors.toList());

                //整体批量修改
                purchaseDetailService.updateBatchById(collect);

                //更新采购单的日期

                PurchaseEntity purchaseEntity = new PurchaseEntity();
                purchaseEntity.setId(purchaseId);
                purchaseEntity.setUpdateTime(new Date());
                this.updateById(purchaseEntity);

            }




    }

    /**
     *
     * @param ids  采购单的ID
     */
    @Transactional
    @Override
    public void received(List<Long> ids) {
        //1 改变当前采购单是新建或者已分配状态
        List<PurchaseEntity> collect = ids.stream().map(id -> {
            //找到当前所有采购项
            PurchaseEntity byId = this.getById(id);
            return byId;
        }).filter(item -> {
            if (item.getStatus() == WareConstant.PurchaseStatusEnum.CREATE.getCode() ||
                    item.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode()) {
                return true;
            } else {
                return false;
            }

        }).map(item -> {
            item.setStatus(WareConstant.PurchaseStatusEnum.RECEIVE.getCode());
            return item;
        }).collect(Collectors.toList());


        //2 改变采购单的状态
        this.updateBatchById(collect);


        //3 改变采购项的状态
        collect.forEach(item -> {   //遍历每个采购单  返回所有的采购项目
            List<PurchaseDetailEntity> entities =  purchaseDetailService.listDetailByPuchaseId(item.getId());
            List<PurchaseDetailEntity> collect1 = entities.stream().map(entity -> {
                PurchaseDetailEntity entity1 = new PurchaseDetailEntity();
                entity1.setId(entity.getId());
                entity1.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode());
                return entity1;
            }).collect(Collectors.toList());
            purchaseDetailService.updateBatchById(collect1);
        });


    }

    @Transactional
    @Override
    public void done(PurchaseDoneVo doneVos) {

         Long id = doneVos.getId();

        //1  改变采购项的状态
        List<PurchaseItemDoneVo> items = doneVos.getItems();

        Boolean flag = true;
        List<PurchaseDetailEntity> updates = new ArrayList<>();
        for (PurchaseItemDoneVo item : items) {
            PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
            if(item.getStatus() == WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode()){
                flag = false;
                detailEntity.setStatus(item.getStatus());
            }else{
                detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.FINISH.getCode());
                //将采购成功的进行入库
                PurchaseDetailEntity byId = purchaseDetailService.getById(item.getItemId());
                wareSkuService.addSock(byId.getSkuId(),byId.getWareId(),byId.getSkuNum());


            }
            detailEntity.setId(item.getItemId());
            updates.add(detailEntity);
        }
            //更新所有采购项的状态
        purchaseDetailService.updateBatchById(updates);

        //2 改变采购单的状态
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(id);
        if(flag){
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.FINISH.getCode());
        }else{
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.HASERROR.getCode());
        }
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);

        //3 将采购成功的进行入库


    }

}