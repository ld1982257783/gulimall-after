package com.atguigu.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.member.dao.MemberDao;
import com.atguigu.gulimall.member.dao.MemberLevelDao;
import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.entity.MemberLevelEntity;
import com.atguigu.gulimall.member.exception.PhoneExistException;
import com.atguigu.gulimall.member.exception.UsernameExistException;
import com.atguigu.gulimall.member.service.MemberService;
import com.atguigu.gulimall.member.vo.MemberLoginVo;
import com.atguigu.gulimall.member.vo.MemberRegist;
import com.atguigu.gulimall.member.vo.SocialUser;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {


    @Autowired
    MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void regist(MemberRegist vo) {
        MemberDao memberDao = this.baseMapper;
        MemberEntity memberEntity = new MemberEntity();

        //设置默认等级  查询到默认等级
        MemberLevelEntity memberLevelEntity = memberLevelDao.getDefaultLevel();
        memberEntity.setLevelId(memberLevelEntity.getId());


        //设置手机号  判断手机号和用户名是否唯一
        checkPhoneUnique(vo.getPhone());
        checkUserName(vo.getUserName());


        memberEntity.setMobile(vo.getPhone());
        memberEntity.setUsername(vo.getUserName());

        memberEntity.setNickname(vo.getUserName());

        //密码要进行 加密存储
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(vo.getPassword());
        memberEntity.setPassword(encode);

        //保存数据
        memberDao.insert(memberEntity);

    }

    @Override
    public void checkPhoneUnique(String phone) throws PhoneExistException{
        Integer mobile = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if(mobile>0){
            throw new PhoneExistException();
        }
    }

    @Override
    public void checkUserName(String username) throws UsernameExistException{
        Integer count = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", username));
        if(count>0){
            throw  new UsernameExistException();
        }
    }

    @Override
    public MemberEntity login(MemberLoginVo vo) {

        String userName = vo.getUserName();
        String password = vo.getPassword();

        //去数据库查询
        MemberEntity memberEntity = this.baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("username", userName).or().eq("mobile", userName));
        if(memberEntity==null){
            //登录失败
            return  null;
        }else{
            //获取到数据库的password
            String password1 = memberEntity.getPassword();
            //密码校验
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            boolean matches = passwordEncoder.matches(password, password1);
            if(matches){
                return memberEntity;
            }else{
                return null;
            }


        }


    }


    /**
     * 创建socialUser
     * @param vo
     * @return
     */
    @Override
    public MemberEntity login(SocialUser vo) throws Exception {
        //登录和注册合并逻辑

        //判断当前用户是否已经登录过
        String uid = vo.getUid();
        //去数据库查找当前用户
        MemberDao memberDao = this.baseMapper;
        MemberEntity memberEntity = memberDao.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));
        if(memberEntity !=null){
            //已经登录过
            //更新新的过期时间和access_token
            MemberEntity update = new MemberEntity();
            update.setId(memberEntity.getId());
            update.setAccessToken(vo.getAccess_token());
            update.setExpiresIn(vo.getExpires_in());
            memberDao.updateById(update);

            memberEntity.setAccessToken(vo.getAccess_token());
            memberEntity.setExpiresIn(vo.getExpires_in());
            return memberEntity;
        }else{
            //2 没有查到当前用户对应的社交记录 我们就需要注册
            MemberEntity regist = new MemberEntity();
            try {

                //3 查询当前社交用户的社交账号信息  (昵称性别等)
                HashMap<String, String> query = new HashMap<>();
                query.put("access_token",vo.getAccess_token());
                query.put("uid",vo.getUid());
                HttpResponse response = HttpUtils.doGet("https://api.weibo.com", "/2/users/show.json", "get", new HashMap<String, String>(), query);
                if(response.getStatusLine().getStatusCode() == 200){
                    //查询成功
                    String json = EntityUtils.toString(response.getEntity());
                    JSONObject jsonObject = JSON.parseObject(json);
                    //昵称
                    String name = jsonObject.getString("name");
                    //性别
                    String gender = jsonObject.getString("gender");
                    regist.setNickname(name);
                    regist.setGender("m".equals(gender)?1:0);

                }
            }catch (Exception e){

            }
            regist.setSocialUid(vo.getUid());
            regist.setAccessToken(vo.getAccess_token());
            regist.setExpiresIn(vo.getExpires_in());

            //插入数据库注册
            memberDao.insert(regist);

            return regist;

        }

    }

}