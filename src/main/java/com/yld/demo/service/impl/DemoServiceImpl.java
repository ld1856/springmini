package com.yld.demo.service.impl;

import com.yld.demo.service.IDemoService;
import com.yld.spring.annotations.Service;

@Service
public class DemoServiceImpl implements IDemoService {


    @Override
    public String getData(String name) {
        return "Demoservice: my name is " + name;
    }
}
