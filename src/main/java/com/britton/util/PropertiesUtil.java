package com.britton.util;

import cn.hutool.core.io.resource.ClassPathResource;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Properties;

@Slf4j
public class PropertiesUtil {
    public static String getValue(String fileName, String key) {
        try {
            log.info(System.getProperty("user.dir"));
            ClassPathResource resource = new ClassPathResource(fileName);
            Properties properties = new Properties();
            properties.load(resource.getStream());
            return properties.getProperty(key, "");
        } catch (IOException e) {
            log.error("获取属性失败", e);
        }
        return "";
    }
}
