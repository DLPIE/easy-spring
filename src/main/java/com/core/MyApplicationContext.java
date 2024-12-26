package com.core;
import com.core.ComponentScan;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

// 容器
public class MyApplicationContext {
    Class configClass;

    ConcurrentHashMap<String,Object> singletonObjects=new ConcurrentHashMap<>(); // 单例池(beanName,bean) todo 为什么并发？？
    ConcurrentHashMap<String,BeanDefinition> beanDefinitions=new ConcurrentHashMap<>();// bean描述(beanName,beanDefinition)

    List<BeanPostProcessor> beanPostProcessorList=new ArrayList<>();
    public MyApplicationContext(Class configClass){
        this.configClass=configClass;
        // 1.扫描路径、创建beanDefinition
        scan(configClass);
        // 2.实例化bean
        for(String beanName:beanDefinitions.keySet()){
            BeanDefinition beanDefinition = beanDefinitions.get(beanName);
            if(beanDefinition.getScope().equals("singleton")){
                Object bean = createBean(beanDefinition);
                singletonObjects.put(beanName,bean);
            }
        }
        // 3.依赖注入、Aware注入
        DIAndAware();

    }

    /**
     * 创建bean
     * @param beanDefinition
     * @return
     */
    public Object createBean(BeanDefinition beanDefinition){
        Class clazz = beanDefinition.getClazz();
        try {
            Object object = clazz.getDeclaredConstructor().newInstance();
            return object;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 依赖注入、Aware注入
     */
    public void DIAndAware(){
        // 遍历单例池
        for(String beanName:singletonObjects.keySet()){
            Object bean = singletonObjects.get(beanName);
            Class clazz = beanDefinitions.get(beanName).getClazz();
            // 1.依赖注入：属性上有@Autowired注解，就从单例池寻找对应的bean注入(目前只有byName)、@Value todo
            for(Field field:clazz.getDeclaredFields()){
                if(field.isAnnotationPresent(Autowired.class)){
                    String name = field.getAnnotation(Autowired.class).value();
                    field.setAccessible(true);
                    try {
                        field.set(bean,singletonObjects.get(name)); // 其实要注意没找到的情况
                        System.out.println("依赖注入成功...");
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            // 3.检查bean有没有实现Aware接口，有就调用接口的方法
            if(bean instanceof BeanNameAware){
                ((BeanNameAware) bean).setBeanName(beanName);
            }
            // 4.BeanPostProcessor前置处理
            for(BeanPostProcessor b:beanPostProcessorList){
                b.postProcessBeforeInitializing();
            }
            // 5.检查bean有没有实现InitializingBean接口，有就调用接口的方法
            if(bean instanceof InitializingBean){
                ((InitializingBean) bean).afterPropertiesSet();
            }
            // 6.BeanPostProcessor后置处理
            for(BeanPostProcessor b:beanPostProcessorList){
                bean=b.postProcessAfterInitializing(bean,beanName); // 实现AOP：用代理对象覆盖原对象
                singletonObjects.put(beanName,bean);  // 关键，用代理对象覆盖原对象
            }

        }
    }

    /**
     * 扫描创建beanDefinitions
     * @param configClass
     */
    public void scan(Class configClass) {
        // 1.获取扫描路径
        ComponentScan componentScanAnnotation = (ComponentScan) configClass.getAnnotation(ComponentScan.class);
        String path = componentScanAnnotation.value(); // com.dl.service

        // 2.获取文件集合
        ClassLoader classLoader = MyApplicationContext.class.getClassLoader();
        URL resource = classLoader.getResource(path.replace(".", "/"));
        File[] files = new File(resource.getFile()).listFiles();

        // 3.遍历文件、加载到jvm、反射判断有@Component就创建beanDefinition
        for(File f:files){
            String absolutePath = f.getAbsolutePath(); // D:\workspace_idea\easy-spring\target\classes\com\dl\service\UserService.class
            String fileName=absolutePath.substring(absolutePath.indexOf("com"),absolutePath.indexOf(".class")); // com\dl\service\UserService
            fileName = fileName.replace("\\", "."); // com.dl.service.UserService
            try {
                Class<?> clazz = classLoader.loadClass(fileName); // 加载到jvm
                if(clazz.isAnnotationPresent(Component.class)){
                    // 如果clazz实现了BeanPostProcessor，就创建bean添加到list，便于后置处理
                    if(BeanPostProcessor.class.isAssignableFrom(clazz)){
                        BeanPostProcessor postProcessor=(BeanPostProcessor)clazz.getDeclaredConstructor().newInstance();
                        beanPostProcessorList.add(postProcessor);
                    }
                    Component componentAnnotation = clazz.getAnnotation(Component.class);
                    String beanName = componentAnnotation.value();
                    // 创建beanDefinition、存到map
                    BeanDefinition beanDefinition = new BeanDefinition();
                    beanDefinition.setClazz(clazz);
                    if(clazz.isAnnotationPresent(Scope.class)){
                        beanDefinition.setScope(clazz.getAnnotation(Scope.class).value());
                    }else{
                        beanDefinition.setScope("singleton"); // 默认单例
                    }
                    beanDefinitions.put(beanName,beanDefinition);
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Object getBean(String beanName){
        // 查询bean是否存在
        BeanDefinition beanDefinition = beanDefinitions.get(beanName);
        if(beanDefinition==null){
            // todo 创建bean（单例bean要DCL,防止2个线程同时发现为null，创建2个bean）
            throw new RuntimeException("您要找的bean不存在");
        }else{ // 单例和其他采取不同策略
            String scope = beanDefinition.getScope();
            if(scope.equals("singleton")){
                return singletonObjects.get(beanName);
            }else{
                return createBean(beanDefinition);
            }
        }
    }
}
