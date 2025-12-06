package com.whatsapp.android.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * @author sunnoc
 * @date 2020-05-13 10:58
 */
@Component
public class SpringUtils implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (SpringUtils.applicationContext == null) {
            SpringUtils.applicationContext = applicationContext;
        }
    }

    /**
     * 获取applicationContext
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 通过name获取 Bean.
     */
    public static Object getBean(String name) {
        return getApplicationContext().getBean(name);
    }

    /**
     * 通过class获取Bean.
     */
    public static <T> T getBean(Class<T> clazz) {
        ApplicationContext applicationContext = getApplicationContext();
        if (applicationContext == null) {
            return null;
        }
        return applicationContext.getBean(clazz);
    }

    /**
     * 通过name,以及Clazz返回指定的Bean
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        return getApplicationContext().getBean(name, clazz);
    }

    /**
     * 获取运行环境
     *
     * @return 环境 0正式服，1预发布，2测试服，3开发环境，-1未知
     */
    public static int getEnvironment() {
        Environment environment = SpringUtils.getApplicationContext().getEnvironment();
        List<String> envList = Arrays.asList(environment.getActiveProfiles());
        if (envList.contains("prod")) {
            return 0;
        } else if (envList.contains("pre")) {
            return 1;
        } else if (envList.contains("test")) {
            return 2;
        } else if (envList.contains("dev")) {
            return 3;
        } else {
            return -1;
        }
    }
}