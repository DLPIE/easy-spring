package com.dl.service;

import com.core.BeanPostProcessor;
import com.core.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@Component("myPostProcessor")
public class MyPostProcessor implements BeanPostProcessor {

    @Override
    public void postProcessBeforeInitializing() {
        System.out.println("BeanPostProcessor前置处理...");
    }

    @Override
    public Object postProcessAfterInitializing(Object bean, String beanName) {
        System.out.println("BeanPostProcessor后置处理...");
        // 假如userService用了AOP
        if(beanName.equals("userService")){
            // 创建代理对象，实现方法增强
            Object proxyInstance = Proxy.newProxyInstance(MyPostProcessor.class.getClassLoader(), bean.getClass().getInterfaces(), new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    System.out.println("我是代理对象，我先执行非核心逻辑...");
                    return method.invoke(bean,args); // 执行原方法
                }
            });
            return proxyInstance;
        }
        return bean;
    }
}
