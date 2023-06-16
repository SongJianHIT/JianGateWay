# 【JianGateWay-4】Netty与核心容器

接下来，我们实现网关的核心功能：**接收请求，发送请求**。这步骤是通过 Netty 实现。

## 网关组件的生命周期

首先定义一个生命周期的接口，所有网关组件都必须实现该接口！

```JAVA
public interface LifeCycle {

    /**
     * 初始化
     */
    void init();

    /**
     * 启动
     */
    void start();

    /**
     * 关闭
     */
    void shutdown();
}
```

## Netty 服务端

服务端类实现比较简单，就是主要需要完成两个功能：

1. 与用户建立连接，接收原始请求
2. **处理原始请求：直接丢给过滤器链即可！**

那么在 `init()` 操作中，初始化好启动助手 serverBootstrap 并设置 Boss 和 Worker 线程组的参数。

在 `start()` 中，添加自定义的业务处理器 Handler。

```JAVA
@Slf4j
public class NettyHttpServer implements LifeCycle {
    /**
     * 配置文件
     */
    private final Config config;

    /**
     * 启动助手
     */
    private ServerBootstrap serverBootstrap;

    /**
     * boss 
     */
    private EventLoopGroup bossEventLoopGroup;
    /**
     * worker 线程组需要对外暴露，给 client 一起用
     */
    @Getter
    private EventLoopGroup workerEventLoopGroup;

    /**
     * NettyProcessor 核心处理器
     */
    private final NettyProcessor nettyProcessor;

    public NettyHttpServer (Config config, NettyProcessor nettyProcessor) {
        this.config = config;
        this.nettyProcessor = nettyProcessor;
        init();
    }

    @Override
    public void init() {
        this.serverBootstrap = new ServerBootstrap();
        log.info("使用 netty worker 线程数为：{}", config.getEventLoopGroupWorkerNum());
        if (useEpoll()) {
            this.bossEventLoopGroup = new EpollEventLoopGroup(
                    config.getEventLoopGroupBossNum(),
                    new DefaultThreadFactory("netty-boss-nio")
            );
            this.workerEventLoopGroup = new EpollEventLoopGroup(
                    config.getEventLoopGroupWorkerNum(),
                    new DefaultThreadFactory("netty-worker-nio")
            );
        } else {
            this.bossEventLoopGroup = new NioEventLoopGroup(
                    config.getEventLoopGroupBossNum(),
                    new DefaultThreadFactory("netty-boss-nio")
            );
            this.workerEventLoopGroup = new NioEventLoopGroup(
                    config.getEventLoopGroupWorkerNum(),
                    new DefaultThreadFactory("netty-worker-nio")
            );
        }
    }

    /**
     * 系统是否支持 epoll
     * @return
     */
    public boolean useEpoll() {
        return RemotingUtil.isLinuxPlatform() && Epoll.isAvailable();
    }

    @Override
    public void start() {
        this.serverBootstrap
                .group(bossEventLoopGroup, workerEventLoopGroup)
                .channel(useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(config.getPort()))
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        ChannelPipeline pipeline = channel.pipeline();
                        // HTTP 编解码器
                        pipeline.addLast(new HttpServerCodec());
                        /**
                         * 如果只是单纯的用 HttpServerCodec 是无法完全的解析 Http POST 请求的，
                         * 因为 HttpServerCodec 只能获取 uri 中参数
                         *
                         * HttpObjectAggregator 是 Netty 提供的 HTTP 消息聚合器，通过它可以把 HttpMessage
                         * 和 HttpContent 聚合成一个 FullHttpRequest 或者 FullHttpResponse(取决于是处理请求还是响应）
                         */
                        pipeline.addLast(new HttpObjectAggregator(config.getMaxContentLength()));
                        // 自定义，打印 Channel 的生命周期
                        pipeline.addLast(new NettyServerConnectManagerHandler());
                        // 自定义，业务处理
                        pipeline.addLast(new NettyHttpServerHandler(nettyProcessor));
                    }
                });
        try {
            this.serverBootstrap.bind().sync();
            log.info("server startup on port {}", config.getPort());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void shutdown() {
        if (bossEventLoopGroup != null) {
            bossEventLoopGroup.shutdownGracefully();
        }
        if (workerEventLoopGroup != null) {
            workerEventLoopGroup.shutdownGracefully();
        }
    }
}
```

没什么难度，创建 Netty 服务端基本上都这个格式。重点在于两个自定义业务处理类的实现。

### NettyServerConnectManagerHandler

我们继承 `ChannelDuplexHandler` 来实现对 **进站** 和 **出站** 数据的监听获取。

```JAVA
public class ChannelDuplexHandler extends ChannelInboundHandlerAdapter implements ChannelOutboundHandler {}
```

重写里面的方法，监听：

- `channelRegistered` ：当 Channel 注册到它的 EventLoop 并且能够处理 I/O 时调用
- `channelUnregistered` ：当 Channel 从它的 EventLoop 中注销并且无法处理任何 I/O 时调用
- `channelActive`  ：当 Channel 处理于活动状态时被调用，可以接收与发送数据
- `channelInactive` ：当 Channel 不再是活动状态且不再连接它的远程节点时被调用
- `userEventTriggered` ： 当ChannelInboundHandler.fireUserEventTriggered()方法被调用时触发
- `exceptionCaught` ：当 ChannelHandler 在处理过程中出现异常时调用

```JAVA
public class NettyServerConnectManagerHandler extends ChannelDuplexHandler {

    /**
     * 当 Channel 注册到它的 EventLoop 并且能够处理 I/O 时调用
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        final String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
        log.debug("NETTY SERVER PIPLINE: channelRegistered {}", remoteAddr);
        super.channelRegistered(ctx);
    }

    /**
     * 当 Channel 从它的 EventLoop 中注销并且无法处理任何 I/O 时调用
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        final String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
        log.debug("NETTY SERVER PIPLINE: channelUnregistered {}", remoteAddr);
        super.channelUnregistered(ctx);
    }

    /**
     * 当 Channel 处理于活动状态时被调用，可以接收与发送数据
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        final String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
        log.debug("NETTY SERVER PIPLINE: channelActive {}", remoteAddr);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //不再是活动状态且不再连接它的远程节点时被调用
        final String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
        log.debug("NETTY SERVER PIPLINE: channelInactive {}", remoteAddr);
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 当ChannelInboundHandler.fireUserEventTriggered()方法被调用时触发
        if(evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent)evt;
            if(event.state().equals(IdleState.ALL_IDLE)) { //有一段时间没有收到或发送任何数据
                final String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
                log.warn("NETTY SERVER PIPLINE: userEventTriggered: IDLE {}", remoteAddr);
                ctx.channel().close();
            }
        }
        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        //当ChannelHandler在处理过程中出现异常时调用
        final String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
        log.warn("NETTY SERVER PIPLINE: remoteAddr： {}, exceptionCaught {}", remoteAddr, cause);
        ctx.channel().close();
    }
}
```

### NettyHttpServerHandler

关键的来了，虽然关键，但也不难。

NettyHttpServerHandler 实现的业务逻辑很简单：

- 监听到 **进站** 请求后，进入该处理类
- 封装 HttpRequestWrapper 对象
- 将这个对象交给 NettyProcessor 核心处理类去处理

```JAVA
public class NettyHttpServerHandler extends ChannelInboundHandlerAdapter {

    private final NettyProcessor nettyProcessor;

    public NettyHttpServerHandler(NettyProcessor nettyProcessor) {
        this.nettyProcessor = nettyProcessor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // HttpObjectAggregator 帮我们实现的
        FullHttpRequest request = (FullHttpRequest) msg;

        HttpRequestWrapper httpRequestWrapper = new HttpRequestWrapper();
        httpRequestWrapper.setCtx(ctx);
        httpRequestWrapper.setRequest(request);

        // 封装好后，把核心业务逻辑委托给 nettyProcessor 进行处理
        nettyProcessor.process(httpRequestWrapper);
    }
}

```

看看这个 HttpRequestWrapper 对象是什么内容：

```JAVA
@Data
public class HttpRequestWrapper {
    private FullHttpRequest request;
    private ChannelHandlerContext ctx;
}
```

很简单，就是将完整的 HTTP 请求和 Netty 的 ChannelHandlerContext 进行一个封装。

那问题的关键就在核心处理类 NettyProcessor  的实现上了。

### NettyProcessor

面向接口编程，定义一个接口：

```JAVA
public interface NettyProcessor {

    /**
     * 处理过程
     * @param httpRequestWrapper
     */
    void process(HttpRequestWrapper httpRequestWrapper);

    /**
     * 启动
     */
    void start();

    /**
     * 销毁
     */
    void shutdown();
}
```

### NettyCoreProcessor

上述接口的实现类，重点在于 process() 的逻辑编写。其实也很简单，就是：

- **将接收到的 HttpRequestWrapper 封装成网关上下文（上下文第一次出现咯）**
- **将上下文丢到过滤器链中，执行过滤逻辑**

```JAVA
@Slf4j
public class NettyCoreProcessor implements NettyProcessor{

    private FilterFactory filterFactory = GatewayFilterChainFactory.getInstance();

    @Override
    public void process(HttpRequestWrapper httpRequestWrapper) {
        // 拿到具体的参数
        FullHttpRequest request = httpRequestWrapper.getRequest();
        ChannelHandlerContext ctx = httpRequestWrapper.getCtx();

        try {
            GatewayContext gatewayContext = RequestHelper.doContext(request, ctx);

            // 执行过滤器逻辑
            filterFactory.buildFilterChain(gatewayContext).doFilter(gatewayContext);

        } catch (BaseException e) {
            // 自定义异常
            log.error("process error {} {}", e.getCode(), e.getMessage());
            FullHttpResponse response = ResponseHelper.getHttpResponse(e.getCode());
            doWriteAndRelease(ctx, request, response);
        } catch (Throwable t) {
            log.error("process unknown error", t);
            FullHttpResponse response = ResponseHelper.getHttpResponse(ResponseCode.INTERNAL_ERROR);
            doWriteAndRelease(ctx, request, response);
        }

    }

    @Override
    public void start() {}

    @Override
    public void shutdown() {}

    /**
     * 回写数据并释放资源
     * @param ctx
     * @param request
     * @param response
     */
    private void doWriteAndRelease(ChannelHandlerContext ctx, FullHttpRequest request, FullHttpResponse response) {
        ctx.writeAndFlush(response)
                // 添加关闭 channel 的监听者，在释放资源后关闭 channel
                .addListener(ChannelFutureListener.CLOSE);
        ReferenceCountUtil.release(request);
    }
}
```

## Netty 客户端

接下来我们看 Netty 的客户端如何实现。

也不难，使用了开源的 `AsyncHttpClient` 来实现。

```JAVA
@Slf4j
public class NettyHttpClient implements LifeCycle {

    /**
     * 配置文件
     */
    private final Config config;

    /**
     * 线程组
     */
    private final EventLoopGroup workerEventLoopGroup;

    /**
     * 异步非阻塞 Http 客户端
     */
    private AsyncHttpClient asyncHttpClient;

    public NettyHttpClient(Config config, EventLoopGroup workerEventLoopGroup) {
        this.config = config;
        this.workerEventLoopGroup = workerEventLoopGroup;
        init();
    }

    @Override
    public void init() {
        DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder()
                .setEventLoopGroup(workerEventLoopGroup)
                // 超时时间
                .setConnectTimeout(config.getHttpConnectTimeout())
                .setRequestTimeout(config.getHttpRequestTimeout())
                // 重试次数
                .setMaxRedirects(config.getHttpMaxRequestRetry())
                // 池化 ByteBuffer 分配器 提升性能
                .setAllocator(PooledByteBufAllocator.DEFAULT)
                .setCompressionEnforced(true)
                .setMaxConnections(config.getHttpMaxConnections())
                .setMaxConnectionsPerHost(config.getHttpConnectionsPerHost())
                .setPooledConnectionIdleTimeout(config.getHttpPooledConnectionIdleTimeout());
        // 初始化 asyncHttpClient
        this.asyncHttpClient = new DefaultAsyncHttpClient(builder.build());
    }

    @Override
    public void start() {
        // 这里调用辅助类 AsyncHttpHelper，构造一个单例 asyncHttpClient
        AsyncHttpHelper.getInstance().initialized(asyncHttpClient);
    }

    @Override
    public void shutdown() {
        if (asyncHttpClient != null) {
            try {
                asyncHttpClient.close();
            } catch (IOException e) {
                log.error("NettyHttpClient shutdown error", e);
            }
        }
    }
}
```

其中，`AsyncHttpHelper` 为单例的客户端操作方法，对外提供了发送请求的方法 `executeRequest()`。

### AsyncHttpHelper	

```JAVA
public class AsyncHttpHelper {

	private static final class SingletonHolder {
		private static final AsyncHttpHelper INSTANCE = new AsyncHttpHelper();
	}

	private AsyncHttpHelper() {

	}

	public static AsyncHttpHelper getInstance() {
		return SingletonHolder.INSTANCE;
	}

	private AsyncHttpClient asyncHttpClient;

	public void initialized(AsyncHttpClient asyncHttpClient) {
		this.asyncHttpClient = asyncHttpClient;
	}

	public CompletableFuture<Response> executeRequest(Request request) {
		ListenableFuture<Response> future = asyncHttpClient.executeRequest(request);
		return future.toCompletableFuture();
	}

	public <T> CompletableFuture<T> executeRequest(Request request, AsyncHandler<T> handler) {
		ListenableFuture<T> future = asyncHttpClient.executeRequest(request, handler);
		return future.toCompletableFuture();
	}
}
```

## 核心容器

核心容器完成对 Netty 客户端和服务端的整合！

```JAVA
/**
 * Container
 * @description 核心容器，用于整合Netty相关组件
 * @author SongJian
 * @date 2023/6/9 10:05
 * @version
 */
@Slf4j
public class Container implements LifeCycle {

    /**
     * 配置文件
     */
    private final Config config;

    private NettyHttpClient nettyHttpClient;

    private NettyHttpServer nettyHttpServer;

    private NettyProcessor nettyProcessor;

    public Container(Config config) {
        this.config = config;
        init();
    }

    @Override
    public void init() {
        NettyCoreProcessor nettyCoreProcessor = new NettyCoreProcessor();
        if (BUFFER_TYPE_PARALLEL.equals(config.getBufferType())) {
            this.nettyProcessor = new DisruptorNettyCoreProcessor(config, nettyCoreProcessor);
        } else {
            this.nettyProcessor = nettyCoreProcessor;
        }
        this.nettyHttpServer = new NettyHttpServer(config, nettyProcessor);
        this.nettyHttpClient = new NettyHttpClient(config, nettyHttpServer.getWorkerEventLoopGroup());
    }

    @Override
    public void start() {
        nettyProcessor.start();
        nettyHttpServer.start();
        nettyHttpClient.start();
        log.info("api gateway started!");
    }

    @Override
    public void shutdown() {
        nettyProcessor.shutdown();
        nettyHttpServer.shutdown();
        nettyHttpClient.shutdown();
        log.info("api gateway shutdown!");
    }
}
```

