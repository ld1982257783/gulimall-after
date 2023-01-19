package com.atguigu.common.vaild;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.Set;

public class ListValueConstraintValidator implements ConstraintValidator<ListValue,Integer> {


    private Set<Integer> set = new HashSet<>();

    //初始化方法
    @Override
    public void initialize(ListValue constraintAnnotation) {
        //获取提交默认值
        int[] vals = constraintAnnotation.vals();
        if (vals!=null){
            for (int val : vals) {
                set.add(val);
            }
        }
    }


    //判断是否校验成功
    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        return set.contains(value);
    }
}
