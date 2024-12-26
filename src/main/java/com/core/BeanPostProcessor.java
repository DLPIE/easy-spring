package com.core;

public interface BeanPostProcessor {
    public void postProcessBeforeInitializing();
    public Object postProcessAfterInitializing(Object bean, String beanName);
}
