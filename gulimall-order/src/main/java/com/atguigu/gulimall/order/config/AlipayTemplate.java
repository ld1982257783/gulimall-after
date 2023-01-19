package com.atguigu.gulimall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gulimall.order.Vo.PayVo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class  AlipayTemplate {

    //在支付宝创建的应用的id
    private   String app_id = "2021000118678224";

    // 商户私钥，您的PKCS8格式RSA2私钥
    public static String merchant_private_key = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDNX6LT8SgGcEFYwJg2lAYvsMNky/54VIGutDQtQIuz4JaMYV4qIH5dALvUqp4987QuFMX4B7OYLJz4h0Nz4BY7N7jOIc5FPPrMkzhL8xxm9Vt2ptpmlnHFRJ4uSjJOBcM1tTnPEKBjGc11dSTKdn81PvSSwY5xuzUO09SrnLLEPVKORALXn8RKv916slYbhgfCBQmus93OarCthrXcCEunxKZvXY5EThpwWWZ+JhXVIno+8fJkLoJ4EJlY71MXTaecqtBEkDcipEAKiT1UGGIvKdv5Jt6yFanl4jpmviAY8V8S1mNn1DqaeT4gUWzGRfbVUGN8cyVdrMJfTV36UTvfAgMBAAECggEAAkQC1Nm9Okz0u5jsVPXPF6mYjj0d4o/0GkxMow8qO2LH5maQlmBjZH1ElTUIp+BPZY6/HFxhY9ViKe+8E8QuGpsjw8plryfcEHOzVEsrTgt45dczY5xXRvjZ0eLpqSixHZ+RMrKZQl6fgKM5M66Y+qpmIlRWUPTiwfYeS4JgCoApoGsI7bVglYnki3WI09OewE68Z4vNRyfhMFOjIQIVYj4vqFSm5KsBWsB39PGSsNKiD+Is8SLTgYbTh+8Ez1lmFsgrvc3pTfmm0tx4hUmiBoFq3q17PPkbQtRy1181uZYxmxeT0kP/Ms1+DXvdoHYuQfr5/NHk1vmxJYLt7C72IQKBgQDrOwDPHzKIsiVGqTe3V1xyp5RK6E+9lFHSsilAzEH3DTlkynlC380Zpz0HtbC6PLJN0HkfZ7xclFgtaX/Swdvl9sC7/3ONblOL0YE/I6J74VpERVjJvjW6TyD9soxzroyNV2dmt+A8ff+B9rzMdnhAHiY0eBemX67cZ6GfbFyA0QKBgQDfgcRRjdQqoUl5qLtQvaFpORKpbibuq7N6aYuuMbDyaGq3Xqyf54yuBE4LbaBUulCzgdy8kZiD8y6p+/zevi2eLRS80iipvplclVeGmIF5Mr52vT2lnEt+TkVs7B7feaXm4DFoYh6sCRxPHTd2xLfcMOKrubA94z9cKIHPKAWdrwKBgQDXQ2mcIYqXOZ+PKHAKYfICIoU8f5fpf3/Zdpz15XZAZWOubvPFVRVWcosRMR4HcGAjgbI2ITJo7oA7Hp6Q+kMIEWWTJVRTizD9dL34T93zy+hVLbjw9hYo5xzjza9mdce92MyRXZfhA9T81BU2bvvggTapMXVDFbDplhR71ihY4QKBgFqZucDtYyHcu3ILidLS68lBj2UK99Er3Nc0TaDRF89LsGaghA1VyVsI97H+QE3YMLpzW808n7xtKW9SbFDGeqtxzsQz1LPqRTSYfSBcVa+ReE+dqo2la5zJka7zkBg6sZuDKcQMci5+ivEvALquR3GEh33hCaYsphRvfx9AM/aVAoGBALphftyyXp2B8fywzxN6UsjFy1Dg0XnQjenAhPvqUXxDT/EZUhdlMcfiD8HwozUdQ+6Etx0borhrjxYb7Ebvuq8Yh8ALTphDbtuXTOh5TxGLZJN0YqoAndA10jO+6vPULhRnK7Ycgiv2YEHgKgbnpNQTJKENG2l5wzxrFKnlndbF";
    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    public static String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApnQoWgIF1pKqO/U1cpckHa+cmsf4AHWPGj/UFnE4knvdYMhscqsh3yem50n3USurrMuFGy/vlUhBpOKWGyhh/nTl6ab7WKt3pjnp8oTvpKaCYYEASM4Tm0NkM2OfYpNdDB9CCmNwamHy8+8rM4O03EFAtP0HdarBAVbhCdexX53iD2vrwmJ/l2T1JYkdLQ0tDdabd9Bil5TA8Ka2G/MH2fa4LBoyB/XAajI5QvSoumnkPDUsU0PZCOthd7XkN2nyptchD0+Y2bsY0YpUOogeP/N1FIO+SVfgs38YGmAIDedsNpIi9AtlkrUknFiycKl96NI4AkF8MOsVxXdsb4d6VwIDAQAB";
    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    public static String notify_url = "http://w46b971349.zicp.vip/payed/notify";

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    public static String return_url = "http://member.gulimall.com/memberOrder.html";

    // 签名方式
    private  String sign_type = "RSA2";

    // 字符编码格式
    private  String charset = "utf-8";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    private  String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";

    //设置超时时间
    private String timout = "30m";

    public  String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"timeout_express\":\""+ timout +"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应："+result);

        return result;

    }
}
