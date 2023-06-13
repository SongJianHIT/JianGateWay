/**
 * @projectName JianGateWay
 * @package tech.songjian.core.filter.loadbalance
 * @className tech.songjian.core.filter.loadbalance.IGatewayLoadBalanceRule
 */
package tech.songjian.core.filter.loadbalance;

import tech.songjian.common.config.ServiceInstance;
import tech.songjian.core.context.GatewayContext;

/**
 * IGatewayLoadBalanceRule
 * @description 负载均衡顶级接口
 * @author SongJian
 * @date 2023/6/10 23:55
 * @version
 */
public interface IGatewayLoadBalanceRule {

    /**
     * 通过上下文参数 选择 对应的服务实例
     * @param context
     * @return
     */
    ServiceInstance choose (GatewayContext context);

    /**
     * 根据服务 id 选择 对应的服务实例
     * @param serviceId
     * @return
     */
    ServiceInstance choose (String serviceId, boolean gray);
}
