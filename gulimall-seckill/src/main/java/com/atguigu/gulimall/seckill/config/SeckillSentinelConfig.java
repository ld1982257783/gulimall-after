package com.atguigu.gulimall.seckill.config;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import com.alibaba.fastjson.JSON;
import com.atguigu.gulimall.seckill.vo.MyResponse;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class SeckillSentinelConfig implements BlockExceptionHandler {


    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, BlockException e) throws Exception {
        response.setContentType("application/json;charset=utf-8");
        MyResponse data = null;
        if(e instanceof FlowException){
            data = new MyResponse(-1,"限流了");
        }else if(e instanceof DegradeException){
            data = new MyResponse(-2,"降级了");
        }else if(e instanceof ParamFlowException){
            data = new MyResponse(-3,"参数限流了");
        }else if(e instanceof SystemBlockException){
            data = new MyResponse(-4,"系统负载异常了");
        }else if(e instanceof AuthorityException){
            data = new MyResponse(-5,"授权异常");
        }
        response.getWriter().write(JSON.toJSONString(data));
    }
}
