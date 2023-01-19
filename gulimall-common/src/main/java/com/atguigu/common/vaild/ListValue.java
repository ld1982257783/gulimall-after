package com.atguigu.common.vaild;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = {ListValueConstraintValidator.class })    //使用哪个校验器进行校验   自己写的校验器
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })   //可以标注在哪些位置
@Retention(RUNTIME)
public @interface ListValue {
    //鼻血使用如下三个属性           还需要在common resources properties文件配置相关的对应内容
    String message() default "{com.atguigu.common.vaild.ListValue.message}";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };


    //标注注解  后显示的值类型
    int [] vals() default {} ;

}
