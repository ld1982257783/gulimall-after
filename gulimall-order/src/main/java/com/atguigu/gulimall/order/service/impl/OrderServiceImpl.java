package com.atguigu.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.VO.MemberEntity;
import com.atguigu.common.exception.NoStockException;
import com.atguigu.common.mq.OrderTo;
import com.atguigu.common.mq.SeckillOrderTo;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.order.To.OrderCreateTo;
import com.atguigu.gulimall.order.Vo.*;
import com.atguigu.gulimall.order.constant.OrderConstant;
import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.dao.OrderItemDao;
import com.atguigu.gulimall.order.entity.*;
import com.atguigu.gulimall.order.enume.OrderStatusEnum;
import com.atguigu.gulimall.order.feign.CartFeignService;
import com.atguigu.gulimall.order.feign.MemberFeignService;
import com.atguigu.gulimall.order.feign.ProductFeignService;
import com.atguigu.gulimall.order.feign.WmsFeignService;
import com.atguigu.gulimall.order.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.order.service.OrderItemService;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.service.PaymentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@RabbitListener(queues = {"hello-java-queue"})
@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    private ThreadLocal<OrderSubmitVo> threadLocal = new ThreadLocal<>();


    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    CartFeignService cartFeignService;

    //注入线程池  开始异步编排
    @Autowired
    ThreadPoolExecutor executor;

    //注入库存远程服务
    @Autowired
    WmsFeignService wmsFeignService;

    //注入redis模板 进行相关操作  接口幂等性
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    OrderDao orderDao;

    @Autowired
    OrderItemDao orderItemDao;

    @Autowired
    OrderItemService orderItemService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    PaymentInfoService paymentInfoService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }



    //监听接收消息
    //监听那个队列
    /**
     * 参数设置
     * 1 Message 原生的详细信息 头+体
     * 2 T<设置接收消息的类型> OrderReturnReasonEntity content
     * 3 Channel channel 当前传输数据的通道
     *
     * Queue可以有很多人来监听 只要收到消息  队列删除信息 而且只有一个能接收到此消息
     *      1)订单服务启动多个 同一个消息 只能有一个客户端收到
     *      2)只有一个完全处理完成  才可以接受下一个消息
     * @param message
     * @param content
     */
    @RabbitHandler
    public void recieveMessage(Message message, OrderReturnReasonEntity content, Channel channel) throws InterruptedException {
        //获取消息体  还需要转换
        byte[] body = message.getBody();
        //获取消息头内容
        MessageProperties messageProperties = message.getMessageProperties();
//        Thread.sleep(2000);
        System.out.println("接收到消息"+message+"类型"+content.getName());

        //channel 内自增的
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        System.out.println("deliverTag["+deliveryTag+"]");

        //签收货物   false代表非批量模式  只签收自己
        try {
            if(deliveryTag%2==0){
                channel.basicAck(deliveryTag,false);
                System.out.println("签收了货物..."+deliveryTag);
            }else{
                //long deliveryTag, boolean multiple  批量操作 批量拒收, boolean requeue  退回队列
                channel.basicNack(deliveryTag,false,true);
//                channel.basicReject();  效果一样
                System.out.println("没有签收到货物"+deliveryTag);
            }
        } catch (IOException e) {
            //网络中断
            e.printStackTrace();
        }

    }


    @RabbitHandler
    public void recieveMessage(Message message, OrderEntity content, Channel channel) throws InterruptedException {
        //获取消息体  还需要转换
        byte[] body = message.getBody();
        //获取消息头内容
        MessageProperties messageProperties = message.getMessageProperties();
//        Thread.sleep(2000);
        System.out.println("接收到消息"+message+"类型"+content);
    }


    /**
     * 订单确认页返回所需要的数据
     * @return
     */
    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        //拦截器 获取用户会员信息
        MemberEntity memberEntity = LoginUserInterceptor.loginUser.get();
        System.out.println("主线程"+Thread.currentThread().getId());
        //远程查询所有的地址列表
        //利用会员id获取会员所有地址

        //TODO  异步模式下又会造成请求头的丢失   解决办法  全局上下文请求中再获取请求
        //获取老请求
        //TODO  RequestContextHolder只要线程不一样里面的数据就不一样  在主线程里面拿到数据  再到副线程共享
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<MemberAdressVo> address = memberFeignService.getAddress(memberEntity.getId());
            confirmVo.setAdress(address);
            System.out.println("1号副线程"+Thread.currentThread().getId());
        }, executor);


        //远程查询购物车所有选中的购物项
        //查询购物车所有数据  购物车服务的拦截器会拦截到当前用户的购物车id  根据id获取数据
        // TODO  注意feign的远程调用会丢失请求头信息  需要配置拦截器将旧请求的请求体放进新请求的请求头
        CompletableFuture<Void> getCartItemFuture = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> items = cartFeignService.getCurrentUserCartItem();
            confirmVo.setItems(items);
            System.out.println("2号副线程"+Thread.currentThread().getId());
        }, executor).thenRunAsync(()->{
            //批量查询每一个商品的库存信息
            List<OrderItemVo> items = confirmVo.getItems();
            //收集所有的商品id
            List<Long> collect = items.stream().map(item -> item.getSkuId()).collect(Collectors.toList());
            // TODO  一定要启动库存服务  否则页面查不出
            R hasStock = wmsFeignService.getSkuHasStock(collect);
            //获取每一个库存的状态信息
            List<SkuStockVo> data = hasStock.getData(new TypeReference<List<SkuStockVo>>(){});
            if(data!=null){
                //重新加工  获取一个map
                Map<Long, Boolean> map = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                System.out.println("库存信息为："+map);
                confirmVo.setStocks(map);
            }



        },executor);


        //3 查询用户积分
        Integer integration = memberEntity.getIntegration();
        confirmVo.setIntegeration(integration);

        //自动计算其他数据

        //TODO 防重复令牌  幂等性  redis
        String token = UUID.randomUUID().toString().replace("-", "");
        confirmVo.setOrderToken(token);  //放入传进页面
        //给redis里面存储一个
        stringRedisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX+ memberEntity.getId(),token,30, TimeUnit.MINUTES);



        //所有异步任务完成才能开始下一步
        CompletableFuture.allOf(getAddressFuture,getCartItemFuture).get();


        return confirmVo;
    }

    /**
     * //下单成功去创建订单，校验令牌 校验价格  锁库存
     * (isolation = Isolation.DEFAULT) 设置隔离级别
     * @param vo
     * @return
     */
    //TODO 本地事务 在分布式系统中 只能控制住自己的回滚 控制不了其他服务的回滚
    //分布式事务 最大原因 网络问题+分布式机器
    // 库存解锁的场景
    // 1） 下订单成功  订单过期没有支付  被系统自动取消 被用户手动取消
    // 2） 下订单成功  库存锁定成功 接下来的业务调用失败  导致订单回滚
//            之前锁定的库存要自动解锁

//    @GlobalTransactional
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
        //TODO 封装返回数据
        SubmitOrderResponseVo responseVo = new SubmitOrderResponseVo();
        responseVo.setCode(0);
        //1  验证令牌是否合法
        //2  验证令牌[令牌的对比和删除必须是原子性的 保证原子性]
        String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        MemberEntity memberEntity = LoginUserInterceptor.loginUser.get();
        threadLocal.set(vo);
        String orderToken = vo.getOrderToken();
        // 验证和删除锁
        Long result = stringRedisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberEntity.getId()), orderToken);
        if(result == 1){
            //验证成功
            //TODO  1下单去创建订单
            OrderCreateTo order = createOrder();
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice1 = vo.getPayPrice();
            //TODO 2 比价
            if(Math.abs(payAmount.subtract(payPrice1).doubleValue()) < 0.01){
                //金额对比 成功
                //TODO 3 保存订单
                saveOrder(order);
                //3 库存锁定 只要又异常 回滚订单数据
                //订单号  所有订单项 (skuId skuNum,num)
                WareSkuLockVo lockVo = new WareSkuLockVo();
                lockVo.setOrderSn(order.getOrder().getOrderSn());
                List<OrderItemVo> locks = order.getOrderItems().stream().map(item -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    orderItemVo.setSkuId(item.getSkuId());  //订单项目id
                    orderItemVo.setCount(item.getSkuQuantity());  //订单数量
                    orderItemVo.setTittle(item.getSkuName());   //订单名字
                    return orderItemVo;
                }).collect(Collectors.toList());
                lockVo.setLocks(locks);
                //TODO 远程调用库存服务进行锁库存
                R r = wmsFeignService.orderLockStock(lockVo);
                if(r.getCode() == 0){
                    //封装订单返回
                    responseVo.setOrder(order.getOrder());
                    //TODO 远程扣减积分出现异常
//                    int i = 10/0;  //订单回滚  库存不回滚

                    //订单创建成功  发送消息给mq
                    rabbitTemplate.convertAndSend("order-event-exchange","order.create.order",order.getOrder());

                    return responseVo;
                }else{
                    //锁定失败
                    String msg = (String) r.get("msg");
                    throw new NoStockException(msg);
                }


            }else{
                responseVo.setCode(1);  //金额对比失败
                return responseVo;
            }
        }else{
            //TODO  防重复验证失败
            responseVo.setCode(1);
            return responseVo;
        }

       // String redisToken = stringRedisTemplate.opsForValue().get(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberEntiy.getId());
    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        OrderEntity order_sn = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        return order_sn;
    }



    @Override
    public void closeOrder(OrderEntity orderEntity) {
        //查询当前订单的最新状态
        OrderEntity order_sn = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderEntity.getOrderSn()));
        if(orderEntity.getStatus() == OrderStatusEnum.CREATE_NEW.getCode()){
            //关单
            OrderEntity orderEntity1 = new OrderEntity();
            orderEntity1.setId(orderEntity.getId());
            orderEntity1.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(orderEntity1);
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(order_sn,orderTo);

            //接下来 再把解锁信息发给mq
            rabbitTemplate.convertAndSend("order-event-exchange","order.release.other",orderTo);
        }




    }


    /**
     * 获取当前订单的支付信息
     * @param orderSn
     * @return
     */
    @Override
    public PayVo getOrderPay(String orderSn) {
        PayVo payVo = new PayVo();
        OrderEntity orderByOrderSn = this.getOrderByOrderSn(orderSn);
        BigDecimal bigDecimal = orderByOrderSn.getPayAmount().setScale(2, BigDecimal.ROUND_UP);
        //设置订单号
        payVo.setOut_trade_no(orderByOrderSn.getOrderSn());
        //设置金额
        payVo.setTotal_amount(bigDecimal.toString());
        //设置标题
        List<OrderItemEntity> orderItemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderByOrderSn.getOrderSn()));
        String skuName = orderItemEntities.get(0).getSkuName();
        payVo.setSubject(skuName);
        //设置
        payVo.setBody(orderItemEntities.get(0).getSkuAttrsVals());

        return payVo;
    }

    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {
        //1 从拦截器获取挡墙用户
        MemberEntity memberEntity = LoginUserInterceptor.loginUser.get();

        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id",memberEntity.getId()).orderByDesc("id")
        );

        //给当前每个订单设置订单项
        List<OrderEntity> order_sn1 = page.getRecords().stream().map(order -> {
            //查询当前订单号对应的所有订单项
            List<OrderItemEntity> order_sn = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", order.getOrderSn()));
            order.setOrderItemEntities(order_sn);
            return order;
        }).collect(Collectors.toList());
        page.setRecords(order_sn1);
        return new PageUtils(page);

    }

    @Override
    public String handlePayResult(PayAsyncVo vo) {
        //1 保存支付流水信息
        PaymentInfoEntity infoEntity = new PaymentInfoEntity();
        //封装支付宝的交易流水号
        infoEntity.setAlipayTradeNo(vo.getTrade_no());
        //哪个订单
        infoEntity.setOrderSn(vo.getOut_trade_no());
        //交易状态
        infoEntity.setPaymentStatus(vo.getTrade_status());
        //回调时间
        infoEntity.setCallbackTime(vo.getNotify_time());
        //保存交易流水
        paymentInfoService.save(infoEntity);
        //修改订单的状态信息
        if(vo.getTrade_status().equals("TRADE_SUCCESS")||vo.getTrade_status().equals("TRADE_FINISHED")){
            //支付成功状态
            String outTradeNo = vo.getOut_trade_no();
            this.baseMapper.updateOrderStatus(outTradeNo,OrderStatusEnum.PAYED.getCode());
        }

        return "success";
    }


    //创建秒杀哦订单相关信息
    @Override
    public void createSeckillOrder(SeckillOrderTo seckillOrderTo) {
        //保存订单信息
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(seckillOrderTo.getOrderSn());
        orderEntity.setMemberId(seckillOrderTo.getMemberId());
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        //价格 秒杀价格乘以数量
        BigDecimal multiply = seckillOrderTo.getSeckillPrice().multiply(new BigDecimal(seckillOrderTo.getNum() + ""));
        orderEntity.setPayAmount(multiply);
        //保存订单
        this.save(orderEntity);

        //保存订单项信息
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        orderItemEntity.setOrderSn(seckillOrderTo.getOrderSn());
        orderItemEntity.setRealAmount(multiply);
        //TODO  获取当前sku的详细信息进行设置
        orderItemEntity.setSkuQuantity(seckillOrderTo.getNum());
        orderItemService.save(orderItemEntity);

    }


    private void saveOrder(OrderCreateTo order) {
        //保存订单数据
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        boolean save = this.save(orderEntity);
        if(save){
            System.out.println("订单保存成功");
        }else{
            System.out.println("订单保存失败");
        }

        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);
    }




    private OrderCreateTo createOrder(){
        OrderCreateTo orderCreateTo = new OrderCreateTo();
        //生成订单号
        String orderSn = IdWorker.getTimeId();
        //创建订单
        OrderEntity entity = buildOrder(orderSn);
        orderCreateTo.setOrder(entity);
        //获取所有订单项
        List<OrderItemEntity> orderItemEntities = buildOrderItems(orderSn);
        orderCreateTo.setOrderItems(orderItemEntities);
        // 3 计算价格相关校验价
        computePrice(entity,orderItemEntities);





        return orderCreateTo;


    }

    private void computePrice(OrderEntity entity, List<OrderItemEntity> orderItemEntities) {
        //订单的总额  每一个订单项的累加
        BigDecimal bigDecimal = new BigDecimal("0.0");
        //打折总共优惠
        BigDecimal bigDecimal1 = new BigDecimal("0.0");
        //优惠券优惠总额
        BigDecimal bigDecimal2 = new BigDecimal("0.0");
        //积分优惠总额
        BigDecimal bigDecimal3 = new BigDecimal("0.0");
        //总共获取多少积分
        BigDecimal gifts = new BigDecimal("0.0");
        //总共获取多少成长值
        BigDecimal growths = new BigDecimal("0.0");


        for (OrderItemEntity orderItemEntity : orderItemEntities) {
            BigDecimal realAmount = orderItemEntity.getRealAmount();
            bigDecimal = bigDecimal.add(realAmount);
            bigDecimal1 = bigDecimal1.add(orderItemEntity.getPromotionAmount());
            bigDecimal2 = bigDecimal2.add(orderItemEntity.getCouponAmount());
            bigDecimal3 = bigDecimal3.add(orderItemEntity.getIntegrationAmount());
            gifts = gifts.add(new BigDecimal(orderItemEntity.getGiftIntegration().toString()));
            growths = growths.add(new BigDecimal(orderItemEntity.getGiftGrowth().toString()));

        }
        //总金额
        entity.setTotalAmount(bigDecimal);
        //应付金额  +运费
        entity.setPayAmount(bigDecimal.add(entity.getFreightAmount()));
        //设置打折优惠总金额
        entity.setPromotionAmount(bigDecimal1);
        //设置优惠券优惠总金额
        entity.setCouponAmount(bigDecimal2);
        //设置积分优惠总金额
        entity.setIntegrationAmount(bigDecimal3);

        //设置可获得多少积分
        entity.setIntegration(gifts.intValue());
        //设置可以获得多少成长值
        entity.setGrowth(growths.intValue());

        //设置默认删除状态 false
        entity.setDeleteStatus(0);   //未删除





    }

    /**
     * 创建订单
     * @param orderSn
     * @return
     */
    private OrderEntity buildOrder(String orderSn) {
        MemberEntity memberEntity = LoginUserInterceptor.loginUser.get();
        OrderEntity entity = new OrderEntity();
        entity.setOrderSn(orderSn);
        entity.setMemberId(memberEntity.getId());
        OrderSubmitVo orderSubmitVo = threadLocal.get();
        //获取收货地址信息
        R facre = wmsFeignService.getFacre(orderSubmitVo.getAddrId());
        FareVo data = facre.getData(new TypeReference<FareVo>() {});
        BigDecimal fare = data.getFare();
        //设置运费信息
        entity.setFreightAmount(fare);
        //设置收货人信息
        entity.setReceiverCity(data.getMemberAdressVo().getCity());
        entity.setReceiverDetailAddress(data.getMemberAdressVo().getDetailAddress());
        entity.setReceiverName(data.getMemberAdressVo().getName());
        entity.setReceiverPhone(data.getMemberAdressVo().getPhone());
        entity.setReceiverPostCode(data.getMemberAdressVo().getPostCode());
        entity.setReceiverProvince(data.getMemberAdressVo().getProvince());
        entity.setReceiverRegion(data.getMemberAdressVo().getRegion());

        //设置订单的相关状态信息
        entity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        //自动确认时间  7天
        entity.setAutoConfirmDay(7);

        return entity;
    }


    /**
     * 构建所有订单项数据
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {

        //获取到所有的订单项   获取当前用户的购物车数据
        List<OrderItemVo> currentUserCartItem = cartFeignService.getCurrentUserCartItem();
        if(currentUserCartItem!=null && currentUserCartItem.size()>0){
            List<OrderItemEntity> itemEntities = currentUserCartItem.stream().map(cartItem -> {
                OrderItemEntity itemEntity = buildOrderItem(cartItem);
                //给每个订单项设置是哪个订单
                itemEntity.setOrderSn(orderSn);
                return itemEntity;
            }).collect(Collectors.toList());
            return itemEntities;
        }else {
            return null;
        }

    }

    /**
     * 构建某一个订单项的信息
     * @param cartItem
     * @return
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity itemEntity = new OrderItemEntity();
        //1 订单信息  订单号
        //2 商品的spu信息
        R spuInfoBySkuId = productFeignService.getSpuInfoBySkuId(cartItem.getSkuId());
        SpuInfoVo data = spuInfoBySkuId.getData(new TypeReference<SpuInfoVo>() {});
        itemEntity.setSpuId(data.getId());
        itemEntity.setSpuName(data.getSpuName());
        itemEntity.setSpuBrand(data.getBrandId().toString());
        itemEntity.setCategoryId(data.getCatalogId());

        //3  商品的sku信息
        itemEntity.setSkuId(cartItem.getSkuId());
        itemEntity.setSkuName(cartItem.getTittle());
        itemEntity.setSkuPic(cartItem.getImage());
        itemEntity.setSkuPrice(cartItem.getPrice());
        itemEntity.setSkuAttrsVals(StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(),";"));
        itemEntity.setSkuQuantity(cartItem.getCount());
        //4  优惠信息
        //5 积分信息
        itemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        itemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        //设置打折信息
        itemEntity.setPromotionAmount(new BigDecimal("0"));
        //设置优惠券信息
        itemEntity.setCouponAmount(new BigDecimal("0"));
        //积分优惠
        itemEntity.setIntegrationAmount(new BigDecimal("0"));
        //当前订单项的总价   单价*数量
        BigDecimal multiply = itemEntity.getSkuPrice().multiply(new BigDecimal(itemEntity.getSkuQuantity().toString()));
        //订单实际金额减去优惠 打折信息
        BigDecimal subtract = multiply.subtract(itemEntity.getPromotionAmount()).subtract(itemEntity.getCouponAmount()).subtract(itemEntity.getIntegrationAmount());
        itemEntity.setRealAmount(subtract);





        return itemEntity;


    }


}