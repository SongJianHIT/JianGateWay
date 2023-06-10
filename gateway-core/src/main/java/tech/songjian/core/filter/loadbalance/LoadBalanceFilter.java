/**
 * @projectName JianGateWay
 * @package tech.songjian.core.filter
 * @className tech.songjian.core.filter.loadbalance.LoadBalanceFilter
 */
package tech.songjian.core.filter.loadbalance;


import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import tech.songjian.common.config.Rule;
import tech.songjian.core.context.GatewayContext;
import tech.songjian.core.filter.Filter;
import tech.songjian.core.filter.FilterAspect;
import tech.songjian.core.request.IGatewayRequest;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static tech.songjian.common.constants.LoadBalanceFilterConst.*;

/**
 * LoadBalanceFilter
 * @description 负载均衡过滤器
 * @author SongJian
 * @date 2023/6/10 23:47
 * @version
 */
@Slf4j
@FilterAspect(id = LOAD_BALANCE_FILTER_ID, name = LOAD_BALANCE_FILTER_NAME, order = LOAD_BALANCE_FILTER_ORDER)
public class LoadBalanceFilter implements Filter {
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        String serviceId = ctx.getUniqueId();
        IGatewayLoadBalanceRule gatewayLoadBalanceRule = getLoadBalanceRule(ctx);
    }

    /**
     * 根据上下文获取对应的负载均衡算法
     * @param ctx
     * @return
     */
    private IGatewayLoadBalanceRule getLoadBalanceRule(GatewayContext ctx) {
        IGatewayLoadBalanceRule loadBalanceRule = null;
        Rule configRule = ctx.getRule();
        if (configRule != null) {
            Set<Rule.FilterConfig> filterConfigs = configRule.getFilterConfigs();
            Iterator iterator = filterConfigs.iterator();
            Rule.FilterConfig filterConfig;
            while (iterator.hasNext()) {
                filterConfig = (Rule.FilterConfig) iterator.next();
                if (filterConfig == null) {
                    continue;
                }
                String filterId = filterConfig.getId();
                if (LOAD_BALANCE_FILTER_ID.equals(filterId)) {
                    // 如果匹配上，直接拿
                    String config = filterConfig.getConfig();
                    // 默认是随机
                    String strategy = LOAD_BALANCE_STRATEGY_RANDOM;
                    if (StringUtils.isNotEmpty(config)) {
                        Map<String, String> mapTypeMap = JSON.parseObject(config, Map.class);
                        strategy = mapTypeMap.get(LOAD_BALANCE_KEY);
                    }
                    switch (strategy) {
                        case LOAD_BALANCE_STRATEGY_RANDOM:
                            loadBalanceRule = new RandomLoadBalanceRule(ctx.getUniqueId());
                            break;
                        case LOAD_BALANCE_STRATEGY_ROUND_ROBIN:
                            loadBalanceRule = new RoundRobinLoadBalanceRule(new AtomicInteger(1), ctx.getUniqueId());
                            break;
                        default:
                            log.warn("No loadBalance strategy for service: {}", strategy);
                            loadBalanceRule = new RandomLoadBalanceRule(ctx.getUniqueId());
                            break;
                    }
                }
            }
        }
        return loadBalanceRule;
    }
}

