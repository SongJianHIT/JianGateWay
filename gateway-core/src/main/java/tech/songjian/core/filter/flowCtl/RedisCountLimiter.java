package tech.songjian.core.filter.flowCtl;

import lombok.extern.slf4j.Slf4j;
import tech.songjian.core.redis.JedisUtil;

/**
 * Created by IntelliJ IDEA.
 * tech.songjian.core.filter.flowCtl
 *
 * @Author: SongJian
 * @Create: 2023/6/12 20:28
 * @Version:
 * @Describe: 使用 Redis 实现分布式限流
 */
@Slf4j
public class RedisCountLimiter {

    /**
     * 封装好的 redis 工具类
     */
    protected JedisUtil jedisUtil;

    public RedisCountLimiter (JedisUtil jedisUtil) {
        this.jedisUtil = jedisUtil;
    }

    private static final int SUCCESS_RESULT = 1;

    private static final int FAILED_RESULT = 0;

    /**
     * 执行限流
     * @param key
     * @param limit
     * @param expireTime
     * @return
     */
    public boolean doFlowCtl (String key, int limit, int expireTime) {
        try {
            // 构造、执行lua脚本
            Object object = jedisUtil.executeScript(key, limit, expireTime);
            if (object == null) {
                // 没有限制，限流通过
                return true;
            }
            Long result = Long.valueOf(object.toString());
            if (FAILED_RESULT == result) {
                return false;
            }
        } catch (Exception e) {
            throw new RuntimeException("分布式限流发生错误！");
        }
        return true;
    }
}
