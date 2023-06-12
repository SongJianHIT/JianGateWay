/**
 * @projectName JianGateWay
 * @package tech.songjian.core.filter.loadbalance
 * @className tech.songjian.core.filter.loadbalance.RandomLoadBalanceRule
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * RandomLoadBalanceRule
 * @description 负载均衡-随机
 * @author SongJian
 * @date 2023/6/10 23:57
 * @version
 */
@Slf4j
public class RandomLoadBalanceRule implements IGatewayLoadBalanceRule{

    private final String serviceId;

    private Set<ServiceInstance> serviceInstanceSet;

    public RandomLoadBalanceRule(String serviceId) {
        this.serviceId = serviceId;
    }

    private static ConcurrentHashMap<String, RandomLoadBalanceRule> serviceMap = new ConcurrentHashMap<>();

    public static RandomLoadBalanceRule getInstance(String serviceId) {
        // 先尝试从 serviceMap 中拿
        RandomLoadBalanceRule loadBalanceRule = serviceMap.get(serviceId);
        if (loadBalanceRule == null) {
            loadBalanceRule = new RandomLoadBalanceRule(serviceId);
            serviceMap.put(serviceId, loadBalanceRule);
        }
        return loadBalanceRule;
    }


    @Override
    public ServiceInstance choose(GatewayContext context) {
        String serviceId = context.getUniqueId();
        return choose(serviceId);
    }

    @Override
    public ServiceInstance choose(String serviceId) {
        Set<ServiceInstance> serviceInstanceSet = DynamicConfigManager.getInstance().getServiceInstanceByUniqueId(serviceId);
        if (serviceInstanceSet.isEmpty()) {
            log.warn("No instance available for: {}", serviceId);
            throw new NotFoundException(ResponseCode.SERVICE_INSTANCE_NOT_FOUND);
        }
        List<ServiceInstance> instances = new ArrayList<>(serviceInstanceSet);
        int index = ThreadLocalRandom.current().nextInt(instances.size());
        return instances.get(index);
    }
}

