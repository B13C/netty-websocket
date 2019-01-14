package com.britton.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.PropertyConfigurator;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class PropertiesUtil {
    public static final String URL = "/mpi.properties";

    public static String getValue(String fileName, String key) {
        log.info(System.getProperty("user.dir"));
        InputStream in = PropertiesUtil.class.getResourceAsStream(fileName);
        if (in == null) {
            PropertyConfigurator.configure(System.getProperty("user.dir") + "/config/log4j.properties");
            String filePath = System.getProperty("user.dir") + "/config" + fileName;
            try {
                in = new FileInputStream(filePath);
            } catch (FileNotFoundException e) {
                log.error("错误", e);
            }
        }
        Properties p = new Properties();
        try {
            p.load(in);
            return p.getProperty(key);
        } catch (IOException e) {
            log.error("错误", e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                log.error("错误", e);
            }
        }
        return "";
    }
}
