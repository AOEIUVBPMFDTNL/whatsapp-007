package com.whatsapp.android.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
public class LocalCacheUtil {

    private static final LoadingCache<String, List<Object>> IP_CACHE = CacheBuilder
            .newBuilder()
            // 最大容量为 100 超过容量有对应的淘汰机制
            .maximumSize(10000)
            // 缓存项写入后多久过期
            .expireAfterWrite(60, TimeUnit.SECONDS)
            // 缓存写入自动刷新拿不到数据会调用load函数
            .build(new CacheLoader<String, List<Object>>() {
                // 加载缓存数据的方法
                @Override
                public List<Object> load(String key) {
                    RedisService redisService = SpringUtils.getBean(RedisService.class);
                    List<Object> ipPool = redisService.lGet2("ip_pool", 0L, -1L);
                    return ipPool;
                }
            });

    /**
     * 从Ip池里随机获取一个ip
     *
     * @return
     */
    public static Pair<String, Integer> getOneIp() {
        List<Object> ipPool;
        try {
            ipPool = IP_CACHE.get("ip_pool");
            String ip = ipPool.get(ThreadLocalRandom.current().nextInt(0, ipPool.size())).toString();
            String[] split = ip.split(":");
            String host = split[0];
            String port = split[1];
            return ImmutablePair.of(host, Integer.parseInt(port));
        } catch (Exception e) {
            log.error("获取ip失败：{}",e);
            throw new RuntimeException(e);
        }
    }

}
