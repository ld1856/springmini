package com.yld.demo.controller;

import com.yld.demo.service.IDemoService;
import com.yld.spring.annotations.Autowrite;
import com.yld.spring.annotations.Controller;
import com.yld.spring.annotations.RequestMapping;
import com.yld.spring.annotations.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

@Controller
@RequestMapping("demo")
public class DemoController {

    @Autowrite
    IDemoService demoService;

    @RequestMapping("name")
    public void name(HttpServletRequest request, HttpServletResponse response, @RequestParam("name") String name) {
        response.setContentType("text/html;charset=utf-8");
        try {
            response.getWriter().write("my name is " + name);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping("service")
    public void service(HttpServletRequest request, HttpServletResponse response, @RequestParam("name") String name) {
        response.setContentType("text/html;charset=utf-8");
        try {
            response.getWriter().write(demoService.getData(name));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping("add")
    public Integer add(@RequestParam("i")Integer i, @RequestParam("j")Integer j){
        return i + j;
    }

    @RequestMapping("test")
    public void test(HttpServletResponse resp){
        resp.setContentType("text/html;charset=utf-8");
        try {
            resp.getWriter().write("test");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
