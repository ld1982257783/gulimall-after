package com.atguigu.gulimall.product.VO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Catelog2Vo {
    private String catalog1Id;  //以及分类ID
    private List<Catelog3Vo> catalog3List ;  //三级子分类
    private String id;
    private String name;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Catelog3Vo{

        private String catalog2Id;  //父分类  2级分类Id
        private String id;
        private String name;


    }

}
