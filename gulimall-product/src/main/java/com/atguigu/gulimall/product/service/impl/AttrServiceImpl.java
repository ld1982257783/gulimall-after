package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.constant.AttrEnum;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.product.VO.AttrGroupRelationVo;
import com.atguigu.gulimall.product.VO.AttrRespVo;
import com.atguigu.gulimall.product.VO.AttrVo;
import com.atguigu.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.atguigu.gulimall.product.dao.AttrDao;
import com.atguigu.gulimall.product.dao.AttrGroupDao;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.*;
import com.atguigu.gulimall.product.service.AttrService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    @Autowired
    AttrAttrgroupRelationDao relationDao;


    @Autowired
    AttrGroupDao attrGroupDao;

    @Autowired
    CategoryDao categoryDao;

    @Autowired
    CategoryService categoryService;



    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void saveAttr(AttrVo attr) {
        System.out.println("attr对象"+attr);
        AttrEntity attrentiy = new AttrEntity();
        BeanUtils.copyProperties(attr,attrentiy);
        //保存基本数据
        this.save(attrentiy);

        if(attr.getAttrType() == AttrEnum.ATTR_TYPE_BASE.getCode() && attr.getAttrGroupId() != null){
            //保存基本属性 保存关联关系
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            relationEntity.setAttrId(attrentiy.getAttrId());
            relationDao.insert(relationEntity);
        }



    }

    @Override
    public PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId,String type) {

        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<AttrEntity>().eq("attr_type","base".equalsIgnoreCase(type)?AttrEnum.ATTR_TYPE_BASE.getCode():AttrEnum.ATTR_TYPE_SALE.getCode());

        if(catelogId!=0){
            queryWrapper.eq("catelog_id",catelogId);
        }

        String key = (String) params.get("key");
        if(StringUtils.isEmpty(key)){
            queryWrapper.and((Wrapper) -> {
                Wrapper.eq("attr_id",key).or().like("attr_name",key);

            });
        }


        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                queryWrapper
        );

        List<AttrEntity> records = page.getRecords();
        List<AttrRespVo> respVos = records.stream().map((attrEntity) -> {
            AttrRespVo attrRespVo = new AttrRespVo();
            BeanUtils.copyProperties(attrEntity, attrRespVo);

            //设置分类和分组的名字
            if("base".equalsIgnoreCase(type)){
                AttrAttrgroupRelationEntity attrId = relationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId()));
                if (attrId != null && attrId.getAttrGroupId() != null) {
                    AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrId.getAttrGroupId());
                    attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }


            CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatelogId());

            if (categoryEntity != null) {
                attrRespVo.setCatelogName(categoryEntity.getName());
            }

            return attrRespVo;
        }).collect(Collectors.toList());


        PageUtils pageUtils = new PageUtils(page);

        pageUtils.setList(respVos);

        return pageUtils;
    }


    //根据属性id  获取所有属性信息
    @Cacheable(value = "attr",key = "'attrinfo'+#root.args[0]")
    @Override
    public AttrRespVo getAttrInfo(Long attrId) {
        AttrRespVo respVo = new AttrRespVo();
        AttrEntity byId = this.getById(attrId);
        BeanUtils.copyProperties(byId,respVo);

        if(byId.getAttrType() == AttrEnum.ATTR_TYPE_BASE.getCode()){
            //设置分组信息
            AttrAttrgroupRelationEntity attr_id = relationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", byId.getAttrId()));
            System.out.println(attr_id);
            if(attr_id!=null){
                respVo.setAttrGroupId(attr_id.getAttrGroupId());
            }

            Long attrGroupId = attr_id.getAttrGroupId();
            System.out.println("数值为"+attrGroupId);
            if(attrGroupId!=0){
                AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attr_id.getAttrGroupId());
                if(attrGroupEntity!=null){
                    respVo.setGroupName(attrGroupEntity.getAttrGroupName());

                }
            }
        }




        //设置分类信息
        Long catelogId = byId.getCatelogId();
        Long[] catelogPath = categoryService.findCatelogPath(catelogId);
        respVo.setCatelogPath(catelogPath);

        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);
        if(categoryEntity!=null){
            respVo.setCatelogName(categoryEntity.getName());
        }


//        respVo.setCatelogPath();


        return respVo;
    }

    @Transactional
    @Override
    public void updateAttr(AttrRespVo attr) {

        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr,attrEntity);


        if(attrEntity.getAttrType() == AttrEnum.ATTR_TYPE_BASE.getCode()) {
            //修改分组关联
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrId(attr.getAttrId());
            relationEntity.setAttrGroupId(attr.getAttrGroupId());

            Integer attr_id = relationDao.selectCount(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attr.getAttrId()));

            if(attr_id>0){
                relationDao.update(relationEntity,new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id",attr.getAttrId()));
            }else{
                relationDao.insert(relationEntity);
            }

        }




    }


    //根据分组ID查找关联的所有属性
    @Override
    public List<AttrEntity> getRelationAttr(Long attrgroupId) {
        List<AttrAttrgroupRelationEntity> entities = relationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", attrgroupId));

        List<Long> attrIds = entities.stream().map((item) -> {
            return item.getAttrId();
        }).collect(Collectors.toList());

        if(attrIds == null || attrIds.size() == 0){
            return null;
        }

        List<AttrEntity> entities1 = this.listByIds(attrIds);

        return entities1;
    }

    @Override
    public void deleteRelation(AttrGroupRelationVo[] vos) {

        List<AttrAttrgroupRelationEntity> collect = Arrays.asList(vos).stream().map((item) -> {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(item, relationEntity);
            return relationEntity;
        }).collect(Collectors.toList());

        //批量删除关联关系
        relationDao.deleteBatchRelation(collect);
    }


    /**
     * 获取当前分组没有关联的所有属性
     * @param params
     * @param attrgroupId
     * @return
     */
    @Override
    public PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId) {
        //1当前分组只能关联自己所属的分类的所有属性
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupId);  //获取当前分组对象
        Long catelogId = attrGroupEntity.getCatelogId();     //获取当前分类

        //2当前分组只能关联别的分组没有引用的属性
        //2.1 获取当前分类下的所有分组
        List<AttrGroupEntity> group = attrGroupDao.selectList(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));
        List<Long> collect = group.stream().map((item) -> {
            return item.getAttrGroupId();
        }).collect(Collectors.toList());

        //2.2 这些分类关联的属性   先查出所有关联数据
        List<AttrAttrgroupRelationEntity> groupId = relationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().in("attr_group_id", collect));
        List<Long> attrIds = groupId.stream().map((item) -> {
            return item.getAttrId();
        }).collect(Collectors.toList());

        //2.3从当前分类的所有属性中移除这些属性
        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<AttrEntity>()
                .eq("catelog_id",catelogId).eq("attr_type",AttrEnum.ATTR_TYPE_BASE.getCode());
        if(attrIds != null && attrIds.size() > 0){  //如果不为空 代表其他分组关联此数据 则拼装
            wrapper.notIn("attr_id",attrIds);   //不包含其他分组关联的属性
        }
        String key = (String) params.get("key");

        if(!StringUtils.isEmpty("key")){
            wrapper.and((w) -> {
               w.eq("attr_id",key).or().like("attr_name",key);
            });
        }

        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), wrapper);

        PageUtils pageUtils = new PageUtils(page);


        //从当前分类的所有属性中移除这些属

        return pageUtils;
    }

    @Override
    public List<Long> selectSearchAttrsIds(List<Long> collect1) {

        return baseMapper.selectSearchAttrIds(collect1);
    }



}