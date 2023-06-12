/**
 * @projectName JianGateWay
 * @package tech.songjian.common.constants
 * @className tech.songjian.common.constants.FilterConst
 */
package tech.songjian.common.constants;

/**
 * FilterConst
 * @description 负载均衡过滤器常量
 * @author SongJian
 * @date 2023/6/10 23:48
 * @version
 */
public interface FilterConst {
    /**
     * ---------------- 负载均衡过滤器 --------------------------
     */
    String LOAD_BALANCE_FILTER_ID = "load_balancer_filter";

    String LOAD_BALANCE_FILTER_NAME = "load_balancer_filter";

    int LOAD_BALANCE_FILTER_ORDER = 100;

    String LOAD_BALANCE_KEY = "load_balancer";

    String LOAD_BALANCE_STRATEGY_RANDOM = "random";

    String LOAD_BALANCE_STRATEGY_ROUND_ROBIN = "RoundRobin";

    /**
     * ---------------- 路由过滤器 --------------------------
     */
    String ROUTER_FILTER_ID = "router_filter";

    String ROUTER_FILTER_NAME = "router_filter";

    int ROUTER_FILTER_ORDER = Integer.MAX_VALUE;
}

