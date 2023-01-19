package com.atguigu.gulimall.order.listener;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gulimall.order.config.AlipayTemplate;
import com.atguigu.gulimall.order.entity.PayAsyncVo;
import com.atguigu.gulimall.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *  此方法用来接收支付宝的异步通知
 */
@RestController
public class OrderPayedLinstener {

    //用于修改订单状态
    @Autowired
    OrderService orderService;

    @Autowired
    AlipayTemplate alipayTemplate;

    @PostMapping("/payed/notify")
    public String handleAlipayed(PayAsyncVo vo,HttpServletRequest request) throws AlipayApiException, UnsupportedEncodingException {
        //只要收到了支付宝给我们的异步的通知,告诉我们订单支付成功，返回success,支付宝就再也不通知
//        Map<String, String[]> parameterMap = request.getParameterMap();
//        for(String key : parameterMap.keySet()){
//            String parameter = request.getParameter(key);
//            System.out.println("参数名："+key+"===>参数值"+parameter);
//        }
//        System.out.println("支付阿伯通知到位了"+parameterMap);

        //核心操作  验签  验证是不是支付宝发送的请求
        //获取支付宝POST过来反馈信息
        Map<String,String> params = new HashMap<String,String>();
        Map<String,String[]> requestParams = request.getParameterMap();
        for (Iterator<String> iter = requestParams.keySet().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            //乱码解决，这段代码在出现乱码时使用
//            valueStr = new String(valueStr.getBytes("ISO-8859-1"), "utf-8");
            params.put(name, valueStr);
        }
        boolean signVerified = AlipaySignature.rsaCheckV1(params, alipayTemplate.alipay_public_key, alipayTemplate.getCharset(), alipayTemplate.getSign_type()); //调用SDK验证签名
        if(signVerified){
            //签名验证成功
            System.out.println("签名验证成功");
            //执行业务代码
            String result = orderService.handlePayResult(vo);
            return result;
        }else{
            System.out.println("签名验证失败");
            return "error";
        }



    }
}
