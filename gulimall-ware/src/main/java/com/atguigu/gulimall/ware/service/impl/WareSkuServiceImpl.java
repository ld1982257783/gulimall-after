package com.atguigu.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.exception.NoStockException;
import com.atguigu.common.mq.OrderTo;
import com.atguigu.common.mq.StockDetailTo;
import com.atguigu.common.mq.StockLockedTo;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.ware.dao.WareSkuDao;
import com.atguigu.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.atguigu.gulimall.ware.entity.WareOrderTaskEntity;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.atguigu.gulimall.ware.feign.orderFeignService;
import com.atguigu.gulimall.ware.feign.productFeignService;
import com.atguigu.gulimall.ware.service.WareOrderTaskDetailService;
import com.atguigu.gulimall.ware.service.WareOrderTaskService;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.atguigu.gulimall.ware.vo.OrderItemVo;
import com.atguigu.gulimall.ware.vo.OrderVo;
import com.atguigu.gulimall.ware.vo.SkuHasStockVo;
import com.atguigu.gulimall.ware.vo.WareSkuLockVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;



@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {


    @Autowired
    WareSkuDao wareSkuDao;

    @Autowired
    productFeignService productFeignService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired           //库存工作单
    WareOrderTaskService wareOrderTaskService;

    @Autowired           //库存锁定状态
    WareOrderTaskDetailService wareOrderTaskDetailService;

    @Autowired
    orderFeignService orderFeignService;


    /**
     * 下单成功  库存锁定成功
     * 两种情况
     * 1 库存锁定成功  但是之前的业务调用逻辑错误  导致订单回滚 之前锁定的库存就要自解锁
     * 2 库存锁定失败  这种情况会自动回滚  无须解锁
     */




    private void unLockStock(Long skuId,Long wareId,Integer num,Long taskDetailId){
        //库存解锁
        wareSkuDao.unlockStock(skuId,wareId,num);
        //更新库存工作单的状态
        WareOrderTaskDetailEntity entity = new WareOrderTaskDetailEntity();
        entity.setId(taskDetailId);
        entity.setLockStatus(2); //已解锁
        wareOrderTaskDetailService.updateById(entity);

    }




    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if(!StringUtils.isEmpty(skuId)){
            wrapper.eq("sku_id",skuId);
        }

        String wareId = (String) params.get("wareId");
        if(!StringUtils.isEmpty(wareId)){
            wrapper.eq("ware_id",wareId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void addSock(Long skuId, Long wareId, Integer skuNum) {

        //判断如果还没有这个记录
        List<WareSkuEntity> entities = wareSkuDao.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        if(entities == null || entities.size() == 0){
            WareSkuEntity skuEntity = new WareSkuEntity();
            skuEntity.setSkuId(skuId);
            skuEntity.setWareId(wareId);
            skuEntity.setStock(skuNum);
            skuEntity.setStockLocked(0);

            //TODO  远程查询Sku的名字  如果失败  整个事务不需要回滚
            R info = productFeignService.info(skuId);
            try {
                if(info.getCode() == 0){
                    Map<String,Object> skuInfo = (Map<String, Object>) info.get("skuInfo");
                    skuEntity.setSkuName((String) skuInfo.get("skuName"));
                }

            }catch (Exception e){

            }


            skuEntity.setSkuName("");

            wareSkuDao.insert(skuEntity);
        }else{
            wareSkuDao.addStock(skuId,wareId,skuNum);

        }

    }

    @Override
    public List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds) {

        List<SkuHasStockVo> collect = skuIds.stream().map(sku -> {
            SkuHasStockVo vo = new SkuHasStockVo();

            //查询当前sku的总库存量
            Long count = baseMapper.getSkuStock(sku);
            vo.setSkuId(sku);
            vo.setHasStock(count==null? false:count>0);

            return vo;
        }).collect(Collectors.toList());
        return collect;
    }

    /**
     * 为某个订单锁定库存
     * (rollbackFor = NoStockException.class)
     * 默认运行时异常都会回滚
     * @param vo
     * @return
     */
    @Transactional
    @Override
    public Boolean orderLockSkuVo(WareSkuLockVo vo) {

        //保存库存工作单的详情 追溯
        WareOrderTaskEntity wareOrderTaskEntity = new WareOrderTaskEntity();
        wareOrderTaskEntity.setOrderSn(vo.getOrderSn());
        wareOrderTaskService.save(wareOrderTaskEntity);


        //按照下单的收货地址  找到一个就近仓库 锁定库存

        //1 找到每个商品在哪都有库存

        List<OrderItemVo> locks = vo.getLocks();

        //收集每个商品在哪有库存
        List<SkuWareHasStock> collect = locks.stream().map(item -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());
            //查询这个商品在哪个仓库id有库存
            List<Long> wareIds = wareSkuDao.listWareIdHasStock(skuId);
            stock.setWareId(wareIds);
            return stock;
        }).collect(Collectors.toList());

        //2 锁定库存  收集每个商品在哪有库存
        for (SkuWareHasStock skuWareHasStock : collect) {
            Boolean skuStocked = false;
            Long skuId = skuWareHasStock.getSkuId();
            List<Long> wareIds = skuWareHasStock.getWareId();
            if(wareIds==null || wareIds.size()==0){
                //没有任何商品有库存 抛出异常
                throw new NoStockException(skuId);
            }

            //查询当前商品那些仓库有   有就设为true 没有就是false
            //1 如果每一个商品都锁定成功  将当前商品锁定了几件的记录发送给mq
            //2 如果锁定失败  前面保存的工作单信息就回滚了 发送出去的消息即使要解锁记录 去数据库找不到id

            for(Long wareId : wareIds){
                //成功就返回 1 否则是0  更新数据库
                Long count = wareSkuDao.lockSkuStock(skuId,wareId,skuWareHasStock.getNum());
                if(count==1){
                    //成功
                    skuStocked = true;

                    WareOrderTaskDetailEntity entity = new WareOrderTaskDetailEntity(null,skuId,null,skuWareHasStock.getNum(),wareOrderTaskEntity.getId(),wareId,1);
                    wareOrderTaskDetailService.save(entity);

                    //发给消息队列
                    StockLockedTo stockLockedTo = new StockLockedTo();
                    stockLockedTo.setId(wareOrderTaskEntity.getId());
                    //封装锁定的详细信息
                    StockDetailTo stockDetailTo = new StockDetailTo();
                    BeanUtils.copyProperties(entity,stockDetailTo);
                    stockLockedTo.setDetail(stockDetailTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange","stock.locked",stockLockedTo);


                    //锁成功  跳出循环
                    break;
                }else{
                    //失败

                }
            }
            if(skuStocked == false){
                //当前商品所有库存都没锁住 抛异常
                throw new NoStockException(skuId);
            }
        }

        //3  肯定所有商品都是锁定成功的


        return true;
    }



    @Override
    public void unlockStock(StockLockedTo stockLockedTo) {
            Long id = stockLockedTo.getId(); //库存工作单的id
            StockDetailTo detail = stockLockedTo.getDetail();
            //查询此id有没有锁定库存的信息
            WareOrderTaskDetailEntity byId = wareOrderTaskDetailService.getById(id);
            if(byId != null){
                //需要解锁  证明库存锁定成功 了  到底要不要解锁
                // 1 查询数据库 关于这个订单的锁定库存信息
                // 有 证明库存锁定成功了
                // 解锁 订单情况
                // 1    没有这个订单  必须解锁
                // 2    有这个订单  不是解锁库存
                //          查看订单状态  订单状态如果已取消 解锁库存
                //              没有取消  不能解锁库存
                Long id1 = stockLockedTo.getId();
                WareOrderTaskEntity byId1 = wareOrderTaskService.getById(id1);
                String orderSn = byId1.getOrderSn();
                //TODO  注意 此处远程调用  order服务的拦截器会拦截此请求 需要去order拦截器进行设置
                R r = orderFeignService.getOrderStatus(orderSn);
                if(r.getCode() == 0){
                    //订单数据返回成功
                    OrderVo data = r.getData(new TypeReference<OrderVo>() {});
                    if(data == null || data.getStatus() == 4){
                        //订单已经被取消了  才能解锁库存
                        if(byId.getLockStatus() == 1){
                            //当前库存状态为已锁定  但是未解锁  才可以解锁
                            unLockStock(detail.getSkuId(),detail.getWareId(),detail.getSkuNum(),id);
                        }else{

                        }
                    }
                }else{
                    //消息拒绝以后重新放到队列 让别人继续消费
                    throw new RuntimeException("远程调用失败");
                }
            }else{
                //无须解锁
            }
    }


    //防止订单服务卡顿  导致订单状态一直改不了  库存消息优先到期 查订单状态新建状态  什么都不做就走了
    //导致卡顿的订单永远不能解锁库存
    @Transactional
    @Override
    public void unlockStock(OrderTo orderTo) {
        String orderSn = orderTo.getOrderSn();
        //查一下订单的最新状态
        //R r = orderFeignService.getOrderStatus(orderSn);
        //查一下库存工作单的最新状态  放置重复解锁库存
        WareOrderTaskEntity task = wareOrderTaskService.getOrderTaskByOrderSn(orderSn);
        Long id = task.getId();
        //按照工作单 找到所有没有解锁的库存 进行解锁
        List<WareOrderTaskDetailEntity> entities = wareOrderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>().eq("task_id", id)
                .eq("lock_status", 1));

        for (WareOrderTaskDetailEntity entity : entities) {
            Long id1 = entity.getId();
            Long skuId = entity.getSkuId();
            Long wareId = entity.getWareId();
            Integer skuNum = entity.getSkuNum();
//            Long skuId,Long wareId,Integer num,Long taskDetailId
            unLockStock(skuId,wareId,skuNum,id1);

        }


    }


    @Data
    class SkuWareHasStock{

        private Long skuId;

        private Integer num;

        private List<Long> wareId;


    }


}