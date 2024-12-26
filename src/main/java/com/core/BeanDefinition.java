package com.core;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BeanDefinition {
    Class clazz; // 很关键
    String scope; // 作用域：singleton、其他
}
