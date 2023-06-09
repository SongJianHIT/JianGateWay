/**
 * @projectName JianGateWay
 * @package tech.songjian.core
 * @className tech.songjian.core.Container
 */
package tech.songjian.core;

import lombok.extern.slf4j.Slf4j;
import tech.songjian.core.netty.NettyHttpClient;
import tech.songjian.core.netty.NettyHttpServer;
import tech.songjian.core.netty.processor.NettyCoreProcessor;
import tech.songjian.core.netty.processor.NettyProcessor;

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
        this.nettyProcessor = new NettyCoreProcessor();
        this.nettyHttpServer = new NettyHttpServer(config, nettyProcessor);
        this.nettyHttpClient = new NettyHttpClient(config, nettyHttpServer.getWorkerEventLoopGroup());
    }

    @Override
    public void start() {
        nettyHttpServer.start();
        nettyHttpClient.start();
        log.info("api gateway started!");
    }

    @Override
    public void shutdown() {
        nettyHttpServer.shutdown();
        nettyHttpClient.shutdown();
        log.info("api gateway shutdown!");
    }
}

