package tech.songjian.core.filter.flowCtl;

import tech.songjian.common.config.Rule;
import tech.songjian.core.context.GatewayContext;

/**
 * Created by IntelliJ IDEA.
 * tech.songjian.core.filter.flowCtl
 *
 * @Author: SongJian
 * @Create: 2023/6/12 20:03
 * @Version:
 * @Describe: 执行限流的接口
 */
public interface IGatewayFlowCtlRule {

    /**
     * 执行限流的具体方式
     * @param ctx
     */
    void doFlowCtlFilter(Rule.FlowCtlConfig flowCtlConfig, String serviceId);
}
