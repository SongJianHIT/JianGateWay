package tech.songjian.core.filter.flowCtl;

import lombok.extern.slf4j.Slf4j;
import tech.songjian.common.config.Rule;
import tech.songjian.core.context.GatewayContext;
import tech.songjian.core.filter.Filter;
import tech.songjian.core.filter.FilterAspect;

import java.util.Iterator;
import java.util.Set;

import static tech.songjian.common.constants.FilterConst.*;

/**
 * Created by IntelliJ IDEA.
 * tech.songjian.core.filter.flowCtl
 *
 * @Author: SongJian
 * @Create: 2023/6/12 19:47
 * @Version:
 * @Describe: 限流流控过滤器
 */
@Slf4j
@FilterAspect(id = FLOW_CTL_FILTER_ID, name = FLOW_CTL_FILTER_NAME, order = FLOW_CTL_FILTER_ORDER)
public class FlowCtlFilter implements Filter {

    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        Rule rule = ctx.getRule();
        if (rule != null) {
            // 先拿到流控的规则
            Set<Rule.FlowCtlConfig> flowCtlConfigs = rule.getFlowCtlConfigs();
            Iterator iterator = flowCtlConfigs.iterator();
            Rule.FlowCtlConfig flowCtlConfig;
            while (iterator.hasNext()) {
                IGatewayFlowCtlRule flowCtlRule = null;
                flowCtlConfig = (Rule.FlowCtlConfig) iterator.next();
                if (flowCtlConfig == null) {
                    continue;
                }
                String path = ctx.getRequest().getPath();
                // 如果是按照路径进行流控
                if (flowCtlConfig.getType().equalsIgnoreCase(FLOW_CTL_TYPE_PATH) &&
                        path.equals(flowCtlConfig.getValue())) {
                    flowCtlRule = FlowCtlByPathRule.getInstance(rule.getServiceId(), path);
                } else if (flowCtlConfig.getType().equalsIgnoreCase(FLOW_CTL_TYPE_SERVICE)){
                    // TODO 如果是根据 服务名 进行限流
                }
                if (flowCtlRule != null) {
                    flowCtlRule.doFlowCtlFilter(flowCtlConfig, rule.getServiceId());
                }
            }
        }
    }

    @Override
    public int getOrder() {
        return Filter.super.getOrder();
    }
}
