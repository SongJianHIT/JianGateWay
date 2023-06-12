package tech.songjian.core.filter.flowCtl;

import com.google.common.util.concurrent.RateLimiter;

import org.apache.commons.lang3.StringUtils;
import tech.songjian.common.config.Rule;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static tech.songjian.common.constants.BasicConst.DIT_SEPARATOR;

/**
 * Created by IntelliJ IDEA.
 * tech.songjian.core.filter.flowCtl
 *
 * @Author: SongJian
 * @Create: 2023/6/12 20:29
 * @Version:
 * @Describe: 单机使用 Guava 限流
 */
public class GuavaCountLimiter {

    /**
     * Guava 库中的一个限流器
     */
    private RateLimiter rateLimiter;

    /**
     * 最大请求数
     */
    private double maxPermits;

    public static ConcurrentHashMap<String, GuavaCountLimiter> resourceRateLimiterMap = new ConcurrentHashMap<>();

    public GuavaCountLimiter(double maxPermits) {
        this.maxPermits = maxPermits;
        rateLimiter = RateLimiter.create(maxPermits);
    }

    public GuavaCountLimiter(double maxPermits, long warmUpPeriodAsSecond) {
        this.maxPermits = maxPermits;
        rateLimiter = RateLimiter.create(maxPermits, warmUpPeriodAsSecond, TimeUnit.SECONDS);
    }

    public static GuavaCountLimiter getInstance(String serviceId, Rule.FlowCtlConfig flowCtlConfig) {
        if (StringUtils.isEmpty(serviceId) || flowCtlConfig == null
                || StringUtils.isEmpty(flowCtlConfig.getValue())
                || StringUtils.isEmpty(flowCtlConfig.getConfig())
                || StringUtils.isEmpty(flowCtlConfig.getType())) {
            return null;
        }
        StringBuffer buffer = new StringBuffer();
        String key = buffer.append(serviceId).append(DIT_SEPARATOR).append(flowCtlConfig.getValue()).toString();
        GuavaCountLimiter countLimiter = resourceRateLimiterMap.get(key);
        if (countLimiter == null) {
            countLimiter = new GuavaCountLimiter(50);
            resourceRateLimiterMap.putIfAbsent(key, countLimiter);
        }
        return countLimiter;
    }

    public boolean acquire(int permits) {
        boolean success =  rateLimiter.tryAcquire(permits);
        if (success) {
            return true;
        }
        return false;
    }
}
