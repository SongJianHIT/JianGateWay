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
public interface LoadBalanceFilterConst {

    String LOAD_BALANCE_FILTER_ID = "load_balance_filter";

    String LOAD_BALANCE_FILTER_NAME = "load_balance_filter";

    int LOAD_BALANCE_FILTER_ORDER = 100;

    String LOAD_BALANCE_KEY = "load_balance";

    String LOAD_BALANCE_STRATEGY_RANDOM = "random";

    String LOAD_BALANCE_STRATEGY_ROUND_ROBIN = "RoundRobin";
}

