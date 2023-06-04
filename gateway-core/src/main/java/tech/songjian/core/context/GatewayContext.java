/**
 * @projectName JianGateWay
 * @package tech.songjian.core.context
 * @className tech.songjian.core.context.GatewayContext
 */
package tech.songjian.core.context;

import io.netty.channel.ChannelHandlerContext;
import tech.songjian.core.request.GatewayRequest;

/**
 * GatewayContext
 * @description JianGateway上下文，对 BasicContext 进行扩展
 * @author SongJian
 * @date 2023/6/4 14:46
 * @version
 */
public class GatewayContext extends BasicContext{

    /**
     * 请求体
     */
    public GatewayRequest request;

    /**
     * 响应体
     */
    public GatewayResponse response;

    /**
     * 规则
     */
    public Rule rule;

    public GatewayContext(String protocol, ChannelHandlerContext nettyCtx, boolean keepAlive) {
        super(protocol, nettyCtx, keepAlive);
    }
}

