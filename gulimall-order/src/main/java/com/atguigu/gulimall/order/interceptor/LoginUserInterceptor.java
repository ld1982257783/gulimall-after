package com.atguigu.gulimall.order.interceptor;

import com.atguigu.common.VO.MemberEntity;
import com.atguigu.common.constant.AuthServerConstand;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Component   //注册拦截器  然后还要写一个配置类添加到spring容器
public class LoginUserInterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberEntity> loginUser = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String uri = request.getRequestURI();
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        boolean match1 = antPathMatcher.match("/payed/notify", uri);
        boolean match = antPathMatcher.match("/order/order/status/**", uri);
        if(match || match1){
            return true;
        }


        HttpSession session = request.getSession();
        MemberEntity attribute = (MemberEntity) session.getAttribute(AuthServerConstand.LOGIN_USER);
        if(attribute!=null){
            loginUser.set(attribute);
            return true;
        }else{
            //没登录 就去登录
            request.getSession().setAttribute("msg","请先进行登录");
            response.sendRedirect("http://127.0.0.1:20000/login.html");
            return false;
        }
    }

}
