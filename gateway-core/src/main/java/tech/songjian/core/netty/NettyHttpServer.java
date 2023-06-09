/**
 * @projectName JianGateWay
 * @package tech.songjian.core.netty
 * @className tech.songjian.core.netty.NettyHttpServer
 */
package tech.songjian.core.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import tech.songjian.common.utils.RemotingUtil;
import tech.songjian.core.Config;
import tech.songjian.core.LifeCycle;
import tech.songjian.core.netty.processor.NettyProcessor;

import java.net.InetSocketAddress;

/**
 * NettyHttpServer
 * @description Netty服务端
 * @author SongJian
 * @date 2023/6/9 08:26
 * @version
 */
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
     * boss 和 worker 线程组
     */
    private EventLoopGroup bossEventLoopGroup;
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
        if (useEpoll()) {
            this.serverBootstrap = new ServerBootstrap();
            this.bossEventLoopGroup = new EpollEventLoopGroup(
                    config.getEventLoopGroupBossNum(),
                    new DefaultThreadFactory("netty-boss-nio")
            );
            this.workerEventLoopGroup = new EpollEventLoopGroup(
                    config.getEventLoopGroupWorkerNum(),
                    new DefaultThreadFactory("netty-worker-nio")
            );
        } else {
            this.serverBootstrap = new ServerBootstrap();
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

