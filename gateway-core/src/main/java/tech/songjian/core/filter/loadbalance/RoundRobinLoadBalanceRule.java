/**
 * @projectName JianGateWay
 * @package tech.songjian.core.filter.loadbalance
 * @className tech.songjian.core.filter.loadbalance.RoundRobinLoadBalanceRule
 */
package tech.songjian.core.filter.loadbalance;

import lombok.extern.slf4j.Slf4j;
import tech.songjian.common.config.DynamicConfigManager;
import tech.songjian.common.config.ServiceInstance;
import tech.songjian.common.enums.ResponseCode;
import tech.songjian.common.exception.NotFoundException;
import tech.songjian.core.context.GatewayContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RoundRobinLoadBalanceRule
 * @description 负载均衡-轮询
 * @author SongJian
 * @date 2023/6/11 00:06
 * @version
 */
@Slf4j
public class RoundRobinLoadBalanceRule implements IGatewayLoadBalanceRule{

    /**
     * 当前轮询到的位置，需要保证线程安全
     */
    private AtomicInteger position = new AtomicInteger(1);

    private final String serviceId;

    public RoundRobinLoadBalanceRule(String serviceId) {
        this.serviceId = serviceId;
    }

    private static ConcurrentHashMap<String, RoundRobinLoadBalanceRule> serviceMap = new ConcurrentHashMap<>();

    public static RoundRobinLoadBalanceRule getInstance(String serviceId) {
        // 先尝试从 serviceMap 中拿
        RoundRobinLoadBalanceRule loadBalanceRule = serviceMap.get(serviceId);
        if (loadBalanceRule == null) {
            // 没有，才进行创建，此时 position = 1
            loadBalanceRule = new RoundRobinLoadBalanceRule(serviceId);
            serviceMap.put(serviceId, loadBalanceRule);
        }
        return loadBalanceRule;
    }

    @Override
    public ServiceInstance choose(GatewayContext context) {
        String serviceId = context.getUniqueId();
        return choose(serviceId, context.isGray());
    }

    @Override
    public ServiceInstance choose(String serviceId, boolean gray) {
        Set<ServiceInstance> serviceInstanceSet =
                DynamicConfigManager.getInstance().getServiceInstanceByUniqueId(serviceId, gray);
        if (serviceInstanceSet.isEmpty()) {
            log.warn("No instance available for: {}", serviceId);
            throw new NotFoundException(ResponseCode.SERVICE_INSTANCE_NOT_FOUND);
        }
        List<ServiceInstance> instances = new ArrayList<>(serviceInstanceSet);
        int pos = Math.abs(this.position.incrementAndGet());
        return instances.get(pos % instances.size());
    }
}

