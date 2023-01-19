package com.atguigu.gulimall.product.Exception;

import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
  //统一处理异常的方法
@Slf4j   //日志记录
@RestControllerAdvice(basePackages = "com.atguigu.gulimall.product.controller")   //这个包下所有的异常都处理
public class GulimallExceptionControllerAdvice {


    @ExceptionHandler(value = MethodArgumentNotValidException.class)   //代表可以处理哪一类的异常
    public R handleVaildException(MethodArgumentNotValidException e){
        log.error("数据校验问题{}，异常类型：{}",e.getMessage(),e.getClass());
        BindingResult bindingResult = e.getBindingResult();    //获取异常返回的结果
        Map<String,String> map = new HashMap<>();
        bindingResult.getFieldErrors().forEach((item) -> {        //异常结果封装进集合里面
            map.put(item.getField(),item.getDefaultMessage());
        });
        return R.error(BizCodeEnume.VAILD_EXCEPTION.getCode(),BizCodeEnume.VAILD_EXCEPTION.getMessage()).put("data",map);
    }


    @ExceptionHandler(value = Throwable.class)   //处理任意类型的异常
    public R handleException(Throwable throwable){
        log.error("错误",throwable);
        return R.error(BizCodeEnume.UNKNOW_EXCEPTION.getCode(),BizCodeEnume.UNKNOW_EXCEPTION.getMessage());
    }
}
