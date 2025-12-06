package com.whatsapp.android.config;

import cn.hutool.core.lang.ClassScanner;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.service.ApiStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 扫描组装所有api
 *
 * @author sunnoc
 * @date 2020-10-19 9:36
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiContext {
    private final ApplicationContext applicationContext;
    /**
     * 策略容器
     */
    private Map<String, Class> allApi = new HashMap<>(100);

    public ApiStrategy getStrategyInstance(String type) {
        Class<?> clazz = getApi(type);
        return ((ApiStrategy) applicationContext.getBean(clazz));
    }

    private Class getApi(String type) {
        return allApi.get(type);
    }

    public void initAllApi(String packageName) {
        Set<Class<?>> classes = ClassScanner.scanPackageByAnnotation(packageName, ApiType.class);
        for (Class clazz : classes) {
            // 提取策略名
            ApiType annotation = (ApiType) clazz.getAnnotation(ApiType.class);
            // 将扫描到的策略放入容器
            allApi.put(annotation.value(), clazz);
        }
    }

}
