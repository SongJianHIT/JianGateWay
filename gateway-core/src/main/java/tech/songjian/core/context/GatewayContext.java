/**
 * @projectName JianGateWay
 * @package tech.songjian.core.context
 * @className tech.songjian.core.context.GatewayContext
 */
package tech.songjian.core.context;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.ReferenceCountUtil;
import lombok.Setter;
import tech.songjian.common.config.Rule;
import tech.songjian.common.utils.AssertUtil;
import tech.songjian.core.request.GatewayRequest;
import tech.songjian.core.response.GatewayResponse;


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

    public GatewayContext(String protocol, ChannelHandlerContext nettyCtx,
                          boolean keepAlive, GatewayRequest request, Rule rule) {
        super(protocol, nettyCtx, keepAlive);
        this.request = request;
        this.rule = rule;
    }

    /**
     * 构造者模式
     */
    public static class Builder {
        private String protocol;
        private ChannelHandlerContext nettyCtx;
        private GatewayRequest request;
        private Rule rule;
        private boolean keepAlive;

        public Builder() {
        }

        public Builder setProtocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder setNettyCtx(ChannelHandlerContext nettyCtx) {
            this.nettyCtx = nettyCtx;
            return this;
        }

        public Builder setRequest(GatewayRequest request) {
            this.request = request;
            return this;
        }

        public Builder setRule(Rule rule) {
            this.rule = rule;
            return this;
        }

        public Builder setKeepAlive(boolean keepAlive) {
            this.keepAlive = keepAlive;
            return this;
        }

        public GatewayContext build () {
            AssertUtil.notNull (protocol, "protocol 不能为空！");
            AssertUtil.notNull (nettyCtx, "nettyCtx 不能为空！");
            AssertUtil.notNull (rule, "rule 不能为空！");
            AssertUtil.notNull (request, "request 不能为空！");
            // AssertUtil.notNull (keepAlive, "setKeepAlive 不能为空！");
            return new GatewayContext(protocol, nettyCtx, keepAlive, request, rule);
        }
    }

    /**
     * 获取必要上下文参数
     * @param key
     * @return
     * @param <T>
     */
//    public <T> T getRequireAttribute (String key) {
//        T value = getAttribute(key);
//        AssertUtil.notNull (value, "缺乏必要参数！");
//        return value;
//    }

    /**
     * 获取指定 key 的上下文参数，如果没有，则返回默认值
     * @param key
     * @return
     * @param <T>
     */
    public <T> T getRequireAttribute (String key, T defaultValue) {
        return (T) attributes.getOrDefault(key, defaultValue);
    }

    /**
     * 获取指定过滤器信息
     * @param filterId
     * @return
     */
    public Rule.FilterConfig getFilterConfig (String filterId) {
        return rule.getFilterConfig(filterId);
    }

    /**
     * 获取服务id
     * @return
     */
    public String getUniqueId() {
        return request.getUniqueId();
    }

    /**
     * 重写父类，释放资源
     */
    public boolean releaseRequest() {
        // 使用 CAS 判断
        if (requestReleased.compareAndSet(false, true)) {
            ReferenceCountUtil.release(request.getFullHttpRequest());
        }
        return true;
    }

    /**
     * 获取原始请求对象
     * @return
     */
    public GatewayRequest getOriginRequest() {
        return request;
    }

    @Override
    public GatewayRequest getRequest() {
        return request;
    }

    public void setRequest(GatewayRequest request) {
        this.request = request;
    }

    @Override
    public GatewayResponse getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = (GatewayResponse) response;
    }

    public Rule getRule() {
        return rule;
    }

    public void setRule(Rule rule) {
        this.rule = rule;
    }
}

