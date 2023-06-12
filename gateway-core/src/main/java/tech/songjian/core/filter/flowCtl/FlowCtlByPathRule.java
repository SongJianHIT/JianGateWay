package tech.songjian.core.filter.flowCtl;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.common.utils.StringUtils;
import tech.songjian.common.config.Rule;
import tech.songjian.core.redis.JedisUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static tech.songjian.common.constants.BasicConst.DIT_SEPARATOR;
import static tech.songjian.common.constants.FilterConst.*;

/**
 * Created by IntelliJ IDEA.
 * tech.songjian.core.filter.flowCtl
 *
 * @Author: SongJian
 * @Create: 2023/6/12 20:05
 * @Version:
 * @Describe: 根据路径进行流控
 */
public class FlowCtlByPathRule implements IGatewayFlowCtlRule{

    private String serviceId;

    private String path;

    private RedisCountLimiter redisCountLimiter;

    private static final String LIMIT_MESSAGE = "您的请求过于频繁，请稍后重试！";

    public FlowCtlByPathRule(String serviceId, String path, RedisCountLimiter redisCountLimiter) {
        this.serviceId = serviceId;
        this.path = path;
        this.redisCountLimiter = redisCountLimiter;
    }

    private static ConcurrentHashMap<String, FlowCtlByPathRule> servicePathMap = new ConcurrentHashMap<>();

    public static FlowCtlByPathRule getInstance(String serviceId, String path) {
        StringBuffer buffer = new StringBuffer();
        // 拿到 key
        String key = buffer.append(serviceId).append(DIT_SEPARATOR).append(path).toString();
        // 获取
        FlowCtlByPathRule flowCtlByPathRule = servicePathMap.get(key);
        if (flowCtlByPathRule == null) {
            flowCtlByPathRule = new FlowCtlByPathRule(serviceId, path,
                    new RedisCountLimiter(new JedisUtil()));
            servicePathMap.putIfAbsent(key, flowCtlByPathRule);
        }
        return flowCtlByPathRule;
    }


    @Override
    public void doFlowCtlFilter(Rule.FlowCtlConfig flowCtlConfig, String serviceId) {
        if (flowCtlConfig == null || StringUtils.isEmpty(serviceId)
                || StringUtils.isEmpty(flowCtlConfig.getConfig())) {
            return;
        }
        Map<String, Integer> configMap = JSON.parseObject(flowCtlConfig.getConfig(), Map.class);
        if (!configMap.containsKey(FLOW_CTL_LIMIT_DURATION) ||
                !configMap.containsKey(FLOW_CTL_LIMIT_PERMITS)) {
            // 如果没有这两个关键参数，则不进行流控了
            return;
        }
        // 拿到 规定时间长度 与 限制次数
        double duration = configMap.get(FLOW_CTL_LIMIT_DURATION);
        double permits = configMap.get(FLOW_CTL_LIMIT_PERMITS);
        boolean flag = true;
        if (FLOW_CTL_MODEL_DISTRIBUTED.equalsIgnoreCase(flowCtlConfig.getModel())) {
            // 分布式架构
            StringBuffer buffer = new StringBuffer();
            String key = buffer.append(serviceId).append(DIT_SEPARATOR).append(path).toString();
            flag = redisCountLimiter.doFlowCtl(key, (int) permits, (int) duration);
        } else {
            // 单体架构的限流实现
            GuavaCountLimiter guavaCountLimiter = GuavaCountLimiter.getInstance(serviceId, flowCtlConfig);
            if (guavaCountLimiter == null) {
                throw new RuntimeException("获取单机限流工具类为空！");
            }
            // 计算每秒执行多少次
            int count = (int) Math.ceil(permits/duration);
            flag = guavaCountLimiter.acquire(count);
        }
        if (!flag) {
            throw new RuntimeException(LIMIT_MESSAGE);
        }
    }
}
