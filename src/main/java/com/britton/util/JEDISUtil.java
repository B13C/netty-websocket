package com.britton.util;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.db.nosql.redis.RedisDS;
import cn.hutool.setting.Setting;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

import java.nio.charset.StandardCharsets;

@Data
@Slf4j
public class JEDISUtil {
    private static final Jedis jedis;

    private static final RedisDS redisDS;

    static {
        final Setting setting = new Setting(ResourceUtil.getResourceObj("classpath:/config/redis.setting").getUrl(), StandardCharsets.UTF_8, true);
        redisDS = RedisDS.create(setting, "custom");
        jedis = redisDS.getJedis();
    }

    public static Jedis getJedis(int index) {
        jedis.select(index);
        return jedis;
    }

    public static Jedis getJedis() {
        return getJedis(0);
    }

    public static long addSet(String setName, String value) {
        long i = 0L;
        try {
            Jedis jedis = getJedis();
            i = jedis.sadd(setName, value);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            redisDS.close();
        }
        return i;
    }

    public static void publishMsg(String channelName, String msg) {
        try {
            getJedis().publish(channelName, msg);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            redisDS.close();
        }
    }

    public static void main(String[] args) {
        System.out.println(JEDISUtil.addSet("macro", "XXXXXMmmm"));
    }
}
