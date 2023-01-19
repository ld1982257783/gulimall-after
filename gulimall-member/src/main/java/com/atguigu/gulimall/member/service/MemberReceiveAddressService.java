package com.atguigu.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.member.entity.MemberReceiveAddressEntity;

import java.util.List;
import java.util.Map;

/**
 * 会员收货地址
 *
 * @author lida
 * @email sunlightcs@gmail.com
 * @date 2021-03-13 12:52:10
 */
public interface MemberReceiveAddressService extends IService<MemberReceiveAddressEntity> {

    PageUtils queryPage(Map<String, Object> params);


    /**
     * 后去会员的所有收货地址列表
     * @param memberId
     * @return
     */
    List<MemberReceiveAddressEntity> getAdress(Long memberId);
}

