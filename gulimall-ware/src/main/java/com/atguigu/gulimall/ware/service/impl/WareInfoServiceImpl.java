package com.atguigu.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.ware.dao.WareInfoDao;
import com.atguigu.gulimall.ware.entity.WareInfoEntity;
import com.atguigu.gulimall.ware.feign.memberFeignService;
import com.atguigu.gulimall.ware.service.WareInfoService;
import com.atguigu.gulimall.ware.vo.FareVo;
import com.atguigu.gulimall.ware.vo.MemberAdressVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {

    @Autowired
    memberFeignService memberFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params),
                new QueryWrapper<WareInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<WareInfoEntity> wrapper = new QueryWrapper<>();

        /**
         * key
         */

        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.eq("id",key).or().like("name",key).or().like("address",key).or().like("areacode",key);
        }


        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public FareVo getFare(Long addrId) {
        //获取收货信息  远程调用  gulimall-member
        R info = memberFeignService.info(addrId);
        FareVo fireVo = new FareVo();
        MemberAdressVo data = info.getData("memberReceiveAddress",new TypeReference<MemberAdressVo>() {});
        if(data!=null){
            fireVo.setMemberAdressVo(data);
            //获取手机号截取长度当做运费
            String phone = data.getPhone();
            String substring = phone.substring(phone.length() - 2, phone.length());
            fireVo.setFare(new BigDecimal(substring));
            return fireVo;
        }else{
            return null;
        }


    }

}