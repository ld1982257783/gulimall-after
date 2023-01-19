package com.atguigu.gulimall.search.controller;

import com.atguigu.gulimall.search.service.MailSearchService;
import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class SearchController {

    @Autowired
    MailSearchService mailSearchService;


    /**
     * 自动将页面提交的所有请求查询参数封装为指定的对象
     * @param searchParam
     * @return
     */
    @GetMapping("/list.html")
    public String listPage(SearchParam searchParam, Model model, HttpServletRequest request){

        //获取url ?后的所有信息
        String queryString = request.getQueryString();
        searchParam.set_queryString(queryString);
        //1  根据传递来的页面的查询参数  去es中检索商品
        SearchResult result = mailSearchService.search(searchParam);
        model.addAttribute("result",result);
        return "list";
    }



}
