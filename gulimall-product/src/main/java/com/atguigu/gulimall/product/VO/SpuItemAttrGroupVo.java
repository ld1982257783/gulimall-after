package com.atguigu.gulimall.product.VO;

import lombok.Data;
import lombok.ToString;

import java.util.List;

@ToString
@Data
public class SpuItemAttrGroupVo {

    //属性分组  属性名字
    private String groupName;
    private List<SpuBaseAttrVo> attrVos;
}
