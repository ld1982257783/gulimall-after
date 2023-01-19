package com.atguigu.gulimall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.AuthServerConstand;
import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.gulimall.auth.feign.ThirdPartFeignService;
import com.atguigu.common.VO.MemberEntity;
import com.atguigu.gulimall.auth.vo.UserLoginVo;
import com.atguigu.gulimall.auth.vo.UserRegistVo;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class loginController {

    @Autowired
    ThirdPartFeignService thirdPartFeignService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    MemberFeignService memberFeignService;

    /**
     * 发送一个请求直接跳转到一个页面
     * springmvc viewcontroller；将请求和页面映射过来
     * @param phone
     * @return
     */
    @GetMapping("/sms/sendCode")
    @ResponseBody
    public R sendCode(@RequestParam("phone") String phone){

        //  TODO 接口防刷

        //防止验证码60s再次发送
        String s = stringRedisTemplate.opsForValue().get(AuthServerConstand.SMS_CODE_CACHE_PREFIX + phone);
        if(!Strings.isEmpty(s)){
            long l = Long.parseLong(s.split("_")[1]);
            if(System.currentTimeMillis()-l<60000){

                //返回异常
                return R.error(BizCodeEnume.SMS_CODE_EXCEPTION.getCode(),BizCodeEnume.SMS_CODE_EXCEPTION.getMessage());
            }

        }

        //reallypss就是要发送的验证码
        String reallypss = UUID.randomUUID().toString().substring(0, 5);
        String substring = reallypss+"_"+System.currentTimeMillis();
        //redis缓存验证码
        stringRedisTemplate.opsForValue().set(AuthServerConstand.SMS_CODE_CACHE_PREFIX+phone,substring,10, TimeUnit.MINUTES);
        //调用远程服务发送验证码到指定手机号
        thirdPartFeignService.sendCode(phone, reallypss);
        return R.ok();
    }




    //进行数据校验
    //重定向携带数据  利用session原理 将数据放在session中  只要跳到下一个页面取出这个数据以后 session里面数据就会删掉
    //TODO  分布式下的session问题
    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo vo, BindingResult result, RedirectAttributes redirectAttributes){
        if(result.hasErrors()){

            // post请求不支持 forward 转发get请求  所以不支持  变为 模板渲染和重定向即可

            //校验出错转发到注册页
            Map<String, String> collect = result.getFieldErrors().stream().collect(Collectors.toMap(fieldError -> {
                return fieldError.getField();
            }, fieldError -> {
                return fieldError.getDefaultMessage();
            }));
            //RedirectAttributes  只能缓存一次数据
            redirectAttributes.addFlashAttribute("errors",collect);
            return "redirect:http://auth.gulimall.com/reg.html";
        }

        //真正注册   调用远程服务进行注册
        String code = vo.getCode();
        //获取缓存真正的验证码
        String s = stringRedisTemplate.opsForValue().get(AuthServerConstand.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if(!Strings.isEmpty(s)){
            //验证验证码是否正确
            if(code.equals(s.split("_")[0])){
                //删除验证码
                stringRedisTemplate.delete(AuthServerConstand.SMS_CODE_CACHE_PREFIX+ vo.getPhone());
                //进行校验  校验成功  调用远程服务
                //验证码通过  真正注册  调用远程服务器进行注册  会员服务
                R r = memberFeignService.regist(vo);
                if(r.getCode() == 0){
                    //成功

                    return "redirect:http://auth.gulimall.com/login.html";

                }else{
                    HashMap<String, String> errors = new HashMap<>();
                    errors.put("msg",r.getData("msg",new TypeReference<String>(){}));
                    redirectAttributes.addFlashAttribute("errors",errors);
                    //失败
                    return "redirect:http://auth.gulimall.com/reg.html";

                }


            }else{
                Map<String, String> errors = new HashMap<>();
                errors.put("code","验证码错误");
                redirectAttributes.addFlashAttribute("errors",errors);
                return "redirect:http://auth.gulimall.com/reg.html";
            }

        }else{
            Map<String, String> errors = new HashMap<>();
            errors.put("code","验证码错误");
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gulimall.com/reg.html";
        }


    }




    @PostMapping("/logins")
    public String login(UserLoginVo vo, RedirectAttributes redirectAttributes, HttpSession session, HttpServletResponse servletResponse, HttpServletRequest servletRequest){
        //发送远程登录请求

        //远程登录
        R login = memberFeignService.login(vo);
        if(login.getCode()==0){
            //成功
            MemberEntity data = login.getData("data", new TypeReference<MemberEntity>() {});
            //session 只在一次会话生效  不能跨域名共享  需要使用子域
            //1  第一次使用session  浏览器会保存卡号 JSSESSIONID=cookie
            //2  以后浏览器访问那个网站就会带上这个网站的cookie
            //3  子域之间  gulimall.com  auth.gulimall.com  order.gulimall.com
            System.out.println("----------------"+data);

            //会把session存到redis
            session.setAttribute(AuthServerConstand.LOGIN_USER,data);
//            Cookie cookie = new Cookie("JSSESSIONID", "data");
//            cookie.setDomain(".gulimall.com");
//            servletResponse.addCookie(cookie)

            return "redirect:http://gulimall.com";

        }else{
            //失败
            HashMap<String, String> errors = new HashMap<>();
            errors.put("msg",login.getData("msg",new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gulimall.com/login.html";

        }

    }





    @GetMapping("/login.html")
    public String loginPage(HttpSession session){
        Object attribute = session.getAttribute(AuthServerConstand.LOGIN_USER);
        if(attribute==null){
            //没有登录
            return "login";
        }else{
            //已经登录
            return "redirect:http://gulimall.com";
        }

    }





}
