package com.atguigu.gulimall.cart.interceptor;

import com.atguigu.common.VO.MemberEntity;
import com.atguigu.common.constant.AuthServerConstand;
import com.atguigu.common.constant.CartConstant;
import com.atguigu.gulimall.cart.vo.UserInfoTo;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

/**
 * 在执行方法之前  判断用户是临时用户还是登录用户
 * 并封装传递给controller
 *
 * 总而言之  临时用户是一定有的
 * 第一次正常页面登陆   封装了一个带有false的临时用户  然后拦截后正式创建一临时用户cookie  user-key
 * 第二次正常登录  页面 有user-key  所以不需要再次设置cookie
 *
 * 如果页面不是正常登录  页面 直接创建一个临时用户
 */
@Component
public class CartInterptor implements HandlerInterceptor {

    public static ThreadLocal<UserInfoTo> threadLocal = new ThreadLocal<>();

    //在目标方法执行之前拦截  false 和true 判断是否放行
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //拦截器获取上一步请求的session 判定是否登录过
        UserInfoTo userInfoTo = new UserInfoTo();
        HttpSession session = request.getSession();
        MemberEntity memberEntity = (MemberEntity) session.getAttribute(AuthServerConstand.LOGIN_USER);
        if(memberEntity != null){
            //登录  放入用户
            userInfoTo.setUserId(memberEntity.getId());
            Cookie[] cookies = request.getCookies();
            for (Cookie cookie : cookies) {
                //user-key
                String name = cookie.getName();
                if(name.equals(CartConstant.TEMP_USER_COOKIE_NAME)){
                    userInfoTo.setUserKey(cookie.getValue());
                    userInfoTo.setTempUser(true);
                }
            }

        }

        //如果没有临时用户  一定要分配一个临时用户
        if(Strings.isEmpty(userInfoTo.getUserKey())){
        String uuid = UUID.randomUUID().toString();
        userInfoTo.setUserKey(uuid);
        }

        //目标方法执行之前
        threadLocal.set(userInfoTo);
        return true;
    }


    /**
     * 业务执行之后
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     * @throws Exception
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        //如果页面中有临时用户信息
        UserInfoTo userInfoTo = threadLocal.get();
        if(!userInfoTo.getTempUser()){
            Cookie cookie = new Cookie(CartConstant.TEMP_USER_COOKIE_NAME, userInfoTo.getUserKey());
            //设置cookie的子域
            cookie.setDomain("gulimall.com");
            //设置cookie的最大有效期
            cookie.setMaxAge(CartConstant.TEMP_USER_COOKIE_TIMEOUT);

            response.addCookie(cookie);

        }

    }
}
