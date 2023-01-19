package com.atguigu.gulimall.product.entity;

import com.atguigu.common.vaild.AddGroup;
import com.atguigu.common.vaild.ListValue;
import com.atguigu.common.vaild.UpdateGroup;
import com.atguigu.common.vaild.UpdateStatus;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.*;
import java.io.Serializable;

/**
 * 品牌
 * 
 * @author lida
 * @email sunlightcs@gmail.com
 * @date 2021-03-13 12:18:52
 * 添加分组校验   解决多场景的分组校验
 */
@Data
@TableName("pms_brand")
public class BrandEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 品牌id
	 * 数字使用  notempty   string使用notblack
	 */
	@NotNull(message = "修改必须指定品牌ID",groups = {UpdateGroup.class})
	@Null(message = "新增不能指定ID",groups = {AddGroup.class})
	@TableId
	private Long brandId;
	/**
	 * 品牌名
	 */
	@NotBlank(message = "品牌名必须提交",groups = {AddGroup.class})
	private String name;
	/**
	 * 品牌logo地址
	 */
	@NotBlank(groups = {AddGroup.class})
	@URL(message = "logo必须是一个合法的URL地址",groups = {AddGroup.class,UpdateGroup.class})
	private String logo;
	/**
	 * 介绍
	 */
	private String descript;
	/**
	 * 显示状态[0-不显示；1-显示]
	 */
	//自定义校验注解  编写自定义校验器     编写自定义校验  在common下pom.xml引入vaildation-api  包
	@NotNull
	@ListValue(vals = {0,1},groups = {AddGroup.class, UpdateStatus.class})
	private Integer showStatus;
	/**
	 * 检索首字母
	 */
	@Pattern(regexp = "^[a-zA-Z]$",message = "检索首字母必须是一个字母",groups = {AddGroup.class,UpdateGroup.class})
	private String firstLetter;
	/**
	 * 排序
	 */
	@Min(value = 0,message = "排序必须大于等于0",groups = {AddGroup.class,UpdateGroup.class})
	private Integer sort;

}
