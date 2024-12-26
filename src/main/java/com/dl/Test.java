package com.dl;

import com.core.MyApplicationContext;
import com.dl.service.UserService;
import com.dl.service.UserServiceImpl;

public class Test {
    public static void main(String[] args) {
        MyApplicationContext context=new MyApplicationContext(AppConfig.class);
        UserService userService = (UserService) context.getBean("userService");
        userService.method();
    }
}
