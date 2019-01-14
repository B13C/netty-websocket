package com.britton.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Slf4j
public class JEDISUtil {
    private static final String CONFIG_FILE_PATH = "/mpi.properties";
    private static final int REDIS_PORT = Integer.parseInt(PropertiesUtil.getValue(CONFIG_FILE_PATH, "redis_port"));
    private static final String REDIS_IP = PropertiesUtil.getValue(CONFIG_FILE_PATH, "redis_ip");
    private static final String REDIS_PWD = PropertiesUtil.getValue(CONFIG_FILE_PATH, "redis_pwd");
    private static final String MAX_WAIT_MILLIS = PropertiesUtil.getValue(CONFIG_FILE_PATH, "max_wait_millis");
    private static final String TEST_ON_BORROW = PropertiesUtil.getValue(CONFIG_FILE_PATH, "test_on_borrow");
    private static final String MAX_TOTAL = PropertiesUtil.getValue(CONFIG_FILE_PATH, "max_total");
    private static final String MAX_IDLE = PropertiesUtil.getValue(CONFIG_FILE_PATH, "max_idle");

    private static JedisPool pool;
    private static JedisPoolConfig config;

    static {
        config = new JedisPoolConfig();
        config.setMaxTotal(Integer.parseInt(MAX_TOTAL));
        config.setMaxIdle(Integer.parseInt(MAX_IDLE));
        config.setTestOnBorrow(Boolean.getBoolean(TEST_ON_BORROW));
        config.setMaxWaitMillis(Long.parseLong(MAX_WAIT_MILLIS));
        config.setTestOnReturn(true);
        pool = new JedisPool(config, REDIS_IP, REDIS_PORT, 10000);
    }

    public static JedisPool getPool() {
        return pool;
    }

    public static synchronized Jedis getJedis(int index) {
        log.info("JedisUtil redis数据库连接活跃数--<" + pool.getNumActive() + ">--空闲连接数--<" + pool.getNumIdle() + ">--等待连接数--<" + pool.getNumWaiters() + ">");
        try {
            Jedis jedis = pool.getResource();
            jedis.select(index);
            return jedis;
        } catch (Exception e) {
            pool.destroy();
            pool = new JedisPool(config, REDIS_IP, REDIS_PORT, 10000, REDIS_PWD);
            log.error("错误", e);
        }
        return null;
    }

    public static synchronized Jedis getJedis() {
        try {
            if (pool != null) {
                return pool.getResource();
            }
            return null;
        } catch (Exception e) {
            log.error("错误", e);
        }
        return null;
    }

    private static void close(Jedis jedis) {
        if (jedis != null) {
            jedis.close();
        }
    }

    public static List<String> lrange(int index, String key) {
        Jedis jedis = null;
        try {
            jedis = getJedis(index);
            return jedis.lrange(key, 0L, -1L);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            jedis.close();
        }
        return null;
    }

    public static long llen(int index, String key) {
        Jedis jedis = null;
        try {
            jedis = getJedis(index);
            return jedis.llen(key);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            jedis.close();
        }
        return 0L;
    }

    public static long lrem(int index, String key, String value) {
        Jedis jedis = null;
        try {
            jedis = getJedis(index);
            return jedis.lrem(key, 0L, value);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            jedis.close();
        }
        return 0L;
    }

    public static long lpush(int index, String key, String value) {
        Jedis jedis = null;
        try {
            jedis = getJedis(index);
            return jedis.lpush(key, new String[]{value});
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            jedis.close();
        }
        return 0L;
    }

    public static String rpop(int index, String key) {
        Jedis jedis = null;
        try {
            jedis = getJedis(index);
            return jedis.rpop(key);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            jedis.close();
        }
        return null;
    }

    public static String set(int index, String key, String value) {
        Jedis jedis = null;
        try {
            jedis = getJedis(index);
            return jedis.set(key, value);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            jedis.close();
        }
        return null;
    }

    public static String get(String key) {
        String value = null;
        Jedis jedis = null;
        try {
            jedis = getJedis();
            value = jedis.get(key);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return value;
    }

    public static String get(String key, int dbIndex) {
        String value = null;
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            value = jedis.get(key);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return value;
    }

    public static boolean set(String key, String value) {
        Jedis jedis = null;
        boolean bool = false;
        try {
            jedis = getJedis();
            jedis.set(key, value);
            bool = true;
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return bool;
    }

    public static boolean set(String key, String value, int dbIndex) {
        Jedis jedis = null;
        boolean bool = false;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            jedis.set(key, value);
            bool = true;
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return bool;
    }

    public static long append(String key, String value, int dbIndex) {
        Jedis jedis = null;
        long i = 0L;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            i = jedis.append(key, value);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return i;
    }

    public static long incr(String key, int dbIndex) {
        Jedis jedis = null;
        long i = 0L;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            i = jedis.incr(key);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return i;
    }

    public static String mset(int dbIndex, String... keysvalues) {
        Jedis jedis = null;
        String value = null;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            value = jedis.mset(keysvalues);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return value;
    }

    public static void del(String key) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.del(key);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
    }

    public static void del(String key, int dbIndex) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            jedis.del(key);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
    }

    public static String hget(String key, String value) {
        String val = null;
        Jedis jedis = null;
        try {
            jedis = getJedis();
            val = jedis.hget(key, value);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return val;
    }

    public static String hget(String key, String value, int dbIndex) {
        String val = null;
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            val = jedis.hget(key, value);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return val;
    }

    public static void hset(String key, String field, String value) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.hset(key, field, value);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
    }

    public static void hset(String key, String field, String value, int dbIndex) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            jedis.hset(key, field, value);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
    }

    public static List<String> hmget(String key, int dbIndex, String... keysValues) {
        java.util.List<String> val = null;
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            val = jedis.hmget(key, keysValues);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return val;
    }

    public static void hdel(String key, String fields) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.hdel(key, new String[]{fields});
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
    }

    public static void hdel(String key, String fields, int dbIndex) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            jedis.hdel(key, new String[]{fields});
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
    }

    public static String hmset(String key, Map<String, String> kv) {
        String val = null;
        Jedis jedis = null;
        try {
            jedis = getJedis();
            val = jedis.hmset(key, kv);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return val;
    }

    public static String hmset(String key, Map<String, String> kv, int dbIndex) {
        String val = null;
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            val = jedis.hmset(key, kv);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return val;
    }

    public static long lpush(String key, int dbIndex, String... keysvalues) {
        Jedis jedis = null;
        long i = 0L;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            i = jedis.lpush(key, keysvalues);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return i;
    }

    public static String lindex(String key, int index, int dbIndex) {
        Jedis jedis = null;
        String val = null;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            val = jedis.lindex(key, index);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return val;
    }

    public static Set<String> zrange(String key, int start, int end) {
        Set<String> zange = null;
        Jedis jedis = null;
        try {
            jedis = getJedis();
            zange = jedis.zrange(key, start, end);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return zange;
    }

    public static Set<String> zrevrange(String key, int start, int end) {
        Set<String> zange = null;
        Jedis jedis = null;
        try {
            jedis = getJedis();
            zange = jedis.zrevrange(key, start, end);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return zange;
    }

    public static void zadd(String key, double score, String member) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.zadd(key, score, member);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
    }

    public static void zremrangeByScore(String key, double start, double end) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.zremrangeByScore(key, start, end);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
    }

    public static Set<Tuple> zrangeWithScores(String key, int start, int end) {
        Set<Tuple> tuple = null;
        Jedis jedis = null;
        try {
            jedis = getJedis();
            tuple = jedis.zrangeWithScores(key, start, end);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return tuple;
    }

    public static Set<Tuple> zrevrangeWithScores(String key, int start, int end) {
        Set<Tuple> tuple = null;
        Jedis jedis = null;
        try {
            jedis = getJedis();
            tuple = jedis.zrevrangeWithScores(key, start, end);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return tuple;
    }

    public static long countSet(String setName) {
        long i = 0L;
        Jedis jedis = null;
        try {
            jedis = getJedis();
            i = jedis.scard(setName).longValue();
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return i;
    }

    public static long countSet(String setName, int dbIndex) {
        long i = 0L;
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            i = jedis.scard(setName).longValue();
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return i;
    }

    public static Set<String> setContainValue(String setName, int dbIndex) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            return jedis.smembers(setName);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return null;
    }

    public static boolean setContainValue(String setName, String value, int dbIndex) {
        boolean i = false;
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            i = jedis.sismember(setName, value);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return i;
    }

    public static boolean setContainValue(String setName, String value) {
        boolean i = false;
        Jedis jedis = null;
        try {
            jedis = getJedis();
            i = jedis.sismember(setName, value);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return i;
    }

    public static long addSet(String setName, String value) {
        long i = 0L;
        Jedis jedis = null;
        try {
            jedis = getJedis();
            i = jedis.sadd(setName, new String[]{value});
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return i;
    }

    public static long addSet(String setName, String value, int dbIndex) {
        long i = 0L;
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            i = jedis.sadd(setName, new String[]{value});
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return i;
    }

    public static long delSet(String setName, String value) {
        long i = 0L;
        Jedis jedis = null;
        try {
            jedis = getJedis();
            i = jedis.srem(setName, new String[]{value});
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return i;
    }

    public static long delSet(String setName, String value, int dbIndex) {
        long i = 0L;
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            i = jedis.srem(setName, new String[]{value});
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return i;
    }

    public static void subscribe(BinaryJedisPubSub binaryJedisPubSub, byte[] channelName) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.subscribe(binaryJedisPubSub, new byte[][]{channelName});
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
    }

    public static boolean unsubscribe(BinaryJedisPubSub binaryJedisPubSub) {
        boolean bool = false;
        try {
            binaryJedisPubSub.unsubscribe();
            bool = true;
        } catch (Exception e) {
            log.error("错误", e);
        }
        return bool;
    }

    public static boolean unsubscribeWithChannelName(BinaryJedisPubSub binaryJedisPubSub, String[] params) {
        boolean bool = false;
        byte[][] bytes = new byte[params.length][];
        int i = 0;
        try {
            String[] arr$ = params;
            int len$ = arr$.length;
            for (int i$ = 0; i$ < len$; i$++) {
                String param = arr$[i$];
                bytes[i] = param.getBytes();
                i++;
            }
            binaryJedisPubSub.punsubscribe(bytes);
            bool = true;
        } catch (Exception e) {
            log.error("错误", e);
        }
        return bool;
    }

    public static boolean publishMsg(byte[] channelName, byte[] msg) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.publish(channelName, msg);
            return true;
        } catch (Exception e) {
            log.error("错误", e);
            return false;
        } finally {
            close(jedis);
        }
    }

    public static boolean publishMsg(String channelName, String msg, int dbIndex) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            jedis.publish(channelName, msg);
            return true;
        } catch (Exception e) {
            log.error("错误", e);
            return false;
        } finally {
            close(jedis);
        }
    }

    public static boolean flushDB() {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.flushDB();
            return true;
        } catch (Exception e) {
            log.error("错误", e);
            return false;
        } finally {
            close(jedis);
        }
    }


    public static boolean setExpire(String key, String value, int seconds) {
        Jedis jedis = null;
        boolean bool = false;
        try {
            jedis = getJedis();
            jedis.select(0);
            jedis.set(key, value);
            jedis.expire(key, seconds);
            bool = true;
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return bool;
    }

    public static boolean exists(String key, int dbIndex) {
        Jedis jedis = null;
        boolean bool = false;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            bool = jedis.exists(key);
        } catch (Exception e) {
            log.error("错误", e);
        } finally {
            close(jedis);
        }
        return bool;
    }

    public static void main(String[] args) {
        System.out.println(JEDISUtil.addSet("macro", "123456789"));
    }
}
