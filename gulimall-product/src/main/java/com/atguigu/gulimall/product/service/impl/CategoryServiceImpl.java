package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.product.VO.Catelog2Vo;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    CategoryDao categoryDao;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedissonClient redisson;


    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //查出所有分类
        //组装成父子的分类结构
        List<CategoryEntity> entities = categoryDao.selectList(null);

        //组长成父子的树形结构
        //（1） 找到所有的一级分类
        entities.stream().filter(CategoryEntity ->
             CategoryEntity.getParentCid() == 0
        ).map((menu) ->{
            menu.setChildren(getChildren(menu,entities));
            return menu;
        }).sorted((menu1,menu2) -> {
            return (menu1.getSort() == null ? 0: menu1.getSort()) - (menu2.getSort() == null? 0: menu2.getSort());
        }).collect(Collectors.toList());




        return entities;
    }

    @Override
    public void removeMenusByIds(List<Long> asList) {
        //TODO 批量删除  检查当前删除的菜单  是否被别的地方引用
        //逻辑删除  并不删除数据库的真是内容
        baseMapper.deleteBatchIds(asList);
    }



    //传入当前菜单  和从哪里获取当前菜单的子菜单  递归查找所有菜单的子菜单
    private List<CategoryEntity> getChildren(CategoryEntity root,List<CategoryEntity> all){

        //依次过滤总菜单
        List<CategoryEntity> chlidren = all.stream().filter((categoryEntity) -> {
            return categoryEntity.getParentCid() == root.getCatId();
        }).map((categoryEntity) -> {
            // 1 找到子菜单
            categoryEntity.setChildren(getChildren(categoryEntity,all));
            return categoryEntity;
        }).sorted((menu1,menu2) -> {
            // 2 菜单的
            return (menu1.getSort() == null ? 0: menu1.getSort()) - (menu2.getSort() == null? 0: menu2.getSort());
        }).collect(Collectors.toList());


        return chlidren;


    }


    //[2,25,255]
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> longs = morefindCatelogid(catelogId, paths);

        Collections.reverse(longs);

        return  longs.toArray(new Long[longs.size()]);
    }


    //更新所有关联的数据   失效模式  调用方法完毕删除缓存
//    @CachePut  双写模式   修改就更新缓存
    //两种方法  删除多个缓存   @CacheEvict(value = "category",allEntries = true)
    @Caching(evict = {
            @CacheEvict(value = "category",key = "'getLevel1Categorys'"),
            @CacheEvict(value = "category",key = "'getCatelogJson'"),
    })
    @CacheEvict(value = "category",allEntries = true)
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());

    }


    //key  spel表达式  需要加个单引号
    //每一个需要缓存的数据都需要制定放到那个名字的缓存    相当于缓存的分区
    @Cacheable(value = {"category"},key = "#root.method.name")    //代表此方法的结果被缓存  如果缓存中有   方法不用调用  如果缓存中没有，会调用方法将方法的结果放入缓存
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        System.out.println("缓存一条数据");
        List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return categoryEntities;

    }


    @Cacheable(value = {"category"},key = "#root.method.name")
    @Override
    public Map<String, List<Catelog2Vo>> getCatelogJson() {

        System.out.println("缓存没有，开始查询数据库");
        //先查出所有的分类
        List<CategoryEntity> selectLists = this.baseMapper.selectList(null);
        //先查出所有的一级分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectLists,0L);
        //封装数据
        Map<String, List<Catelog2Vo>> collect1 = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //1 每一个的一级分类 查到这个一级分类的二级分类
            List<CategoryEntity> categoryEntities = getParent_cid(selectLists,v.getCatId());
            //2 封装上面的结果
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    //2查找当前二级分类的三级分类 封装成vo
                    List<CategoryEntity> level3Catelog = getParent_cid(selectLists,l2.getCatId());
                    if (level3Catelog != null) {
                        List<Catelog2Vo.Catelog3Vo> collect = level3Catelog.stream().map(l3 -> {
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(collect);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));
        return collect1;
    }


















    //注入redis
    //TODO 测试期间会产生堆外内存溢出的错误

    // 1）springboot2.0以后默认使用lettuce作为redis的客户端   使用netty进行网络通信
    // 2  lettuce的bug导致netty堆外内存溢出  -Xmx300m
    // 3 解决方案
    //  1) 升级 lettuce  2）切换使用jedis
    public Map<String, List<Catelog2Vo>> getCatelogJson2() {
        /**
         * redis 缓存会出现的问题
         * 1 、 空结果缓存  解决缓存穿透
         * 2 、 设置过期时间(加随机值)  解决缓存雪崩
         * 3 、 加锁  解决缓存击穿 
         */
        //给缓存中json字符串  拿出的json还需要能逆转为对象  序列化  反序列化的过程
        String catalogJSON = stringRedisTemplate.opsForValue().get("getCatelogJson");
        if(StringUtils.isEmpty(catalogJSON)){
            //缓存中没有  去数据库查找
            Map<String, List<Catelog2Vo>> catelogJsonFormDb = getCatelogJsonFormDbWithRedisLock();
            return catelogJsonFormDb;
        }
        //转为指定的数据
        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON,new TypeReference<Map<String, List<Catelog2Vo>>>(){});
//        加入缓存逻辑
        System.out.println("查询完毕.....");
        return result;
    }




    //分布式锁
    public Map<String, List<Catelog2Vo>> getCatelogJsonFormDbWithRedissonLock() {

        RLock lock = redisson.getLock("CatalogJson-lock");
        lock.lock();
        Map<String, List<Catelog2Vo>> catelogJsonFormDb = null;

        try {
            catelogJsonFormDb = getCatelogJsonFormDb();
        }catch (Exception e){

        }finally {
            lock.unlock();
        }

            return catelogJsonFormDb;

    }


    //    从数据库获取真正的数据并封装整个分类数据

    //分布式锁
    public Map<String, List<Catelog2Vo>> getCatelogJsonFormDbWithRedisLock() {
        //占分布式锁，去redis占坑   NX  没有则放  有则不放
        //令牌机制
        String uuid = UUID.randomUUID().toString();
        //setIfAbsent  只有在key并不存在的情况下才进行设置  存在的情况下则返回false
        Boolean loack = stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid,300,TimeUnit.SECONDS);

        if(loack){
            System.out.println("获取分布式锁成功");
            //占锁成功
            //设置锁的过期时间   必须是原子的  加锁和设置过期时间
            //stringRedisTemplate.expire("lock",30,TimeUnit.SECONDS);
            //不管业务是否超时 ，或者出现什么样的异常  都需要删除锁
            Map<String, List<Catelog2Vo>> catelogJsonFormDb = null;
            try {
                catelogJsonFormDb = getCatelogJsonFormDb();
            }finally {
                //删除锁  核心 先比对是否自己占的锁  如果是删除  原子操作
                String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
                Long lock1 = stringRedisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                        Arrays.asList("lock"), uuid);
            }
            return catelogJsonFormDb;
        }else{
            System.out.println("获取分布式锁失败，等待重试");
            //加锁重试  重试  synchronized ()
            //睡眠一会  避免重复调用  内存溢出
            try {
                Thread.sleep(200);
            }catch (Exception e){
                e.printStackTrace();
            }
            return getCatelogJsonFormDbWithRedisLock();
        }


    }


    //本地锁
    public Map<String, List<Catelog2Vo>> getCatelogJsonFormDbWithLocalLock() {

//        加同步锁
        synchronized (this){
//            再一次确认缓存中有没有数据
            String catalogJSON = stringRedisTemplate.opsForValue().get("catalogJSON");
            if(!StringUtils.isEmpty(catalogJSON)){
                Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON,new TypeReference<Map<String, List<Catelog2Vo>>>(){});
                return result;
            }

            System.out.println("缓存中没有，开始查询数据库");


            //先查出所有的分类
            List<CategoryEntity> selectLists = this.baseMapper.selectList(null);

            //先查出所有的一级分类
            List<CategoryEntity> level1Categorys = getParent_cid(selectLists,0L);
            //封装数据
            Map<String, List<Catelog2Vo>> collect1 = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                //1 每一个的一级分类 查到这个一级分类的二级分类
                List<CategoryEntity> categoryEntities = getParent_cid(selectLists,v.getCatId());
                //2 封装上面的结果
                List<Catelog2Vo> catelog2Vos = null;
                if (categoryEntities != null) {
                    catelog2Vos = categoryEntities.stream().map(l2 -> {
                        Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                        //2查找当前二级分类的三级分类 封装成vo
                        List<CategoryEntity> level3Catelog = getParent_cid(selectLists,l2.getCatId());
                        if (level3Catelog != null) {
                            List<Catelog2Vo.Catelog3Vo> collect = level3Catelog.stream().map(l3 -> {
                                Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                                return catelog3Vo;
                            }).collect(Collectors.toList());
                            catelog2Vo.setCatalog3List(collect);
                        }
                        return catelog2Vo;
                    }).collect(Collectors.toList());
                }
                return catelog2Vos;
            }));

            //查到的对象逆转为json放入redis
            String s = JSON.toJSONString(collect1);
            stringRedisTemplate.opsForValue().set("catalogJSON",s,1, TimeUnit.DAYS);

            return collect1;
        }


    }

    public Map<String, List<Catelog2Vo>> getCatelogJsonFormDb() {

        //   再一次确认缓存中有没有数据
        String catalogJSON = stringRedisTemplate.opsForValue().get("catalogJSON");
        if(!StringUtils.isEmpty(catalogJSON)){
            Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON,new TypeReference<Map<String, List<Catelog2Vo>>>(){});
            System.out.println("缓存命中，直接返回");
            return result;
        }

        System.out.println("缓存没有，开始查询数据库");
            //先查出所有的分类
            List<CategoryEntity> selectLists = this.baseMapper.selectList(null);

            //先查出所有的一级分类
            List<CategoryEntity> level1Categorys = getParent_cid(selectLists,0L);
            //封装数据
            Map<String, List<Catelog2Vo>> collect1 = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                //1 每一个的一级分类 查到这个一级分类的二级分类
                List<CategoryEntity> categoryEntities = getParent_cid(selectLists,v.getCatId());
                //2 封装上面的结果
                List<Catelog2Vo> catelog2Vos = null;
                if (categoryEntities != null) {
                    catelog2Vos = categoryEntities.stream().map(l2 -> {
                        Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                        //2查找当前二级分类的三级分类 封装成vo
                        List<CategoryEntity> level3Catelog = getParent_cid(selectLists,l2.getCatId());
                        if (level3Catelog != null) {
                            List<Catelog2Vo.Catelog3Vo> collect = level3Catelog.stream().map(l3 -> {
                                Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                                return catelog3Vo;
                            }).collect(Collectors.toList());
                            catelog2Vo.setCatalog3List(collect);
                        }
                        return catelog2Vo;
                    }).collect(Collectors.toList());
                }
                return catelog2Vos;
            }));

            //查到的对象逆转为json放入redis
            String s = JSON.toJSONString(collect1);
            stringRedisTemplate.opsForValue().set("catalogJSON",s,1, TimeUnit.DAYS);

            return collect1;

    }

    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectLists,Long cid) {
        List<CategoryEntity> collect = selectLists.stream().filter(item -> {
            return item.getParentCid() == cid;
        }).collect(Collectors.toList());
        return collect;
    }


    //递归获取父分类ID
    private List<Long> morefindCatelogid(Long id,List<Long> list){
        list.add(id);
        CategoryEntity byId = this.getById(id);
        if(byId.getParentCid()!=0){
            morefindCatelogid(byId.getParentCid(),list);
        }
        return list;
    }

}