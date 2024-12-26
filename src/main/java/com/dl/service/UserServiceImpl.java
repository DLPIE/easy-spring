package com.dl.service;

import com.core.*;
import lombok.Data;

@Component("userService")
@Data
// @Scope("xxx") // 没有这个注解，默认单例
public class UserServiceImpl implements BeanNameAware, InitializingBean,UserService {

    String beanName;

    @Autowired("orderService")
    public OrderService orderService;

    @Override
    public void setBeanName(String beanName) {
        this.beanName=beanName;
        System.out.println("执行Aware接口的方法...");
    }

    @Override
    public void method() {
        System.out.println("执行核心逻辑...");
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("执行InitializingBean接口的方法...");
    }
}
