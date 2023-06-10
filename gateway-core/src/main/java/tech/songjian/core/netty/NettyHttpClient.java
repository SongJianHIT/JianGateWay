/**
 * @projectName JianGateWay
 * @package tech.songjian.core.netty
 * @className tech.songjian.core.netty.NettyHttpClient
 */
package tech.songjian.core.netty;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import tech.songjian.core.Config;
import tech.songjian.core.LifeCycle;
import tech.songjian.core.helper.AsyncHttpHelper;

import java.io.IOException;

/**
 * NettyHttpClient
 * @description
 * @author SongJian
 * @date 2023/6/9 09:49
 * @version
 */
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

