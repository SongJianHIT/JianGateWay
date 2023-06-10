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
    private final AtomicInteger position;

    private final String serviceId;

    private Set<ServiceInstance> serviceInstanceSet;;

    public RoundRobinLoadBalanceRule(AtomicInteger position, String serviceId) {
        this.position = position;
        this.serviceId = serviceId;

        // 从注册中心中拿到服务实例集合
        this.serviceInstanceSet = DynamicConfigManager.getInstance().getServiceInstanceByUniqueId(serviceId);
    }


    @Override
    public ServiceInstance choose(GatewayContext context) {
        String serviceId = context.getUniqueId();
        return choose(serviceId);
    }

    @Override
    public ServiceInstance choose(String serviceId) {
        if (serviceInstanceSet.size() == 0) {
            serviceInstanceSet = DynamicConfigManager.getInstance().getServiceInstanceByUniqueId(serviceId);
        }
        if (serviceInstanceSet.size() == 0) {
            log.warn("No instance available for: {}", serviceId);
            throw new NotFoundException(ResponseCode.SERVICE_INSTANCE_NOT_FOUND);
        }
        List<ServiceInstance> instances = new ArrayList<>(serviceInstanceSet);
        int pos = Math.abs(this.position.incrementAndGet());
        return instances.get(pos % instances.size());
    }
}

