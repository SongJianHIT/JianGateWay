# 【JianGateWay-2】网关上下文定义

## 定义网关上下文接口

网关上下文是在网关内部流转的，因此需要维护：

1. **网关上下文的生命周期**：正常运行、异常写回、成功写回、请求结束
2. **获取请求协议**
3. **获取请求对象**
4. **获取、设置响应对象**
5. **获取、设置异常信息**
6. **获取 Netty 上下文**
7. **释放请求资源**
8. **设置写回接收回调函数**

```JAVA
public interface IContext {

    /**
     * 生命周期：运行中状态
     */
    int Running = 1;

    /**
     * 生命周期：运行过程中发生错误，对其进行标记，请求已经结束，需要返回客户端
     */
    int Written = 0;

    /**
     * 生命周期：标记写回成功，防止并发情况下多次写回
     */
    int Completed = 1;

    /**
     * 生命周期：网关请求结束
     */
    int Terminated = 2;

    /**
     * 设置上下文状态为运行中
     */
    void setRunning();

    /**
     * 设置上下文状态为标记写回
     */
    void setWritten();

    /**
     * 设置上下文状态为标记写回成功
     */
    void setCompleted();

    /**
     * 设置上下文状态为请求结束
     */
    void setTerminated();

    /**
     * 判断网关状态
     * @return
     */
    boolean isRunning();

    /**
     * 判断网关状态
     * @return
     */
    boolean isWritten();

    /**
     * 判断网关状态
     * @return
     */
    boolean isCompleted();

    /**
     * 判断网关状态
     * @return
     */
    boolean isTerminated();

    /**
     * 获取协议
     * @return
     */
    String getProtocol();

    /**
     * 获取请求对象
     * @return
     */
    Object getRequest();

    /**
     * 获取返回对象
     * @return
     */
    Object getResponse();

    /**
     * 设置返回对象
     */
    void setResponse(Object response);

    /**
     * 获取异常信息
     * @return
     */
    Throwable getThrowable();

    /**
     * 设置异常信息
     * @param throwable
     */
    void setThrowable(Throwable throwable);

    /**
     * 获取 Netty 上下文
     * @return
     */
    ChannelHandlerContext getNettyCtx();

    /**
     * 判断是否长连接
     * @return
     */
    boolean isKeepAlive();

    /**
     * 释放请求资源
     * @return
     */
    boolean releaseRequest();

    /**
     * 设置写回接收回调函数
     * @param consumer
     */
    void setCompletedCallBack(Consumer<IContext> consumer);

    /**
     * 执行写回接收回调函数
     */
    void invokeCompletedCallBack();
}
```

## 完成网关上下文基础实现

是  `IContext` 的最基础实现，后续所有网关上下文都可以直接在这个 `BasicContext` 上进行扩展功能。

```JAVA
public class BasicContext implements IContext{

    /**
     * 转发协议
     */
    protected final String protocol;

    /**
     * 网关状态
     * volatile：多线程情况下做到线程之间的可见性与防止指令重排
     */
    protected volatile int stats = IContext.Running;

    /**
     * Netty 上下文
     */
    protected final ChannelHandlerContext nettyCtx;

    /**
     * 上下文参数
     */
    protected final Map<String, Object> attributes = new HashMap<String, Object>();

    /**
     * 请求过程中发生的异常
     */
    protected Throwable throwable;

    /**
     * 是否保持长连接
     */
    protected final boolean keepAlive;

    /**
     * 存放回调函数集合
     */
    protected List<Consumer<IContext>> completedCallBacks;

    /**
     * 定义是否已经释放资源
     */
    protected final AtomicBoolean requestReleased = new AtomicBoolean(false);

    public BasicContext(String protocol, ChannelHandlerContext nettyCtx, boolean keepAlive) {
        this.protocol = protocol;
        this.nettyCtx = nettyCtx;
        this.keepAlive = keepAlive;
    }

    // getter setter...

    @Override
    public void setCompletedCallBack(Consumer<IContext> consumer) {
        if (completedCallBacks == null) {
            completedCallBacks = new ArrayList<>();
        }
        completedCallBacks.add(consumer);
    }

    @Override
    public void invokeCompletedCallBack() {
        // 回调函数不为空，则遍历回调函数集合，并执行
        if (completedCallBacks != null) {
            completedCallBacks.forEach(call->call.accept(this));
        }
    }
}
```

## 网关上下文的具体实现

继承自 `BasicContext`，对其进行扩展，包括：

- **请求对象 GatewayRequest**
- **响应对象 GatewayResponse**
- **规则 Rule**
- **重试次数**：用于熔断限流
- **灰度标识**：用户标记服务是否是灰度发布

```JAVA
public class GatewayContext extends BasicContext{

    /**
     * 请求对象
     */
    public GatewayRequest request;

    /**
     * 响应对象
     */
    public GatewayResponse response;

    /**
     * 规则
     */
    public Rule rule;

    /**
     * 当前重试次数
     */
    private int currentRetryTimes;

    /**
     * 灰度标识
     */
    @Setter
    @Getter
    private boolean gray;

    /**
     * 统计过滤器时间
     **/
    @Setter
    @Getter
    private Timer.Sample timerSample;

    public GatewayContext(String protocol, ChannelHandlerContext nettyCtx,
                          boolean keepAlive, GatewayRequest request, Rule rule, int currentRetryTimes) {
        super(protocol, nettyCtx, keepAlive);
        this.request = request;
        this.rule = rule;
        this.currentRetryTimes = currentRetryTimes;
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
            return new GatewayContext(protocol, nettyCtx, keepAlive, request, rule, 0);
        }
    }

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
}
```

