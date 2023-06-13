/**
 * @projectName JianGateWay
 * @package tech.songjian.core
 * @className tech.songjian.core.Config
 */
package tech.songjian.core;

import lombok.Data;

/**
 * Config
 * @description 配置
 * @author SongJian
 * @date 2023/6/8 23:11
 * @version
 */
@Data
public class Config {
    /**
     * 端口
     */
    private int port = 8888;

    /**
     * 普罗帕米修斯端口
     */
    private int prometheusPort = 18000;

    /**
     * 应用名
     */
    private String applicationName = "api-gateway";

    /**
     * 配置中心地址
     */
    private String registryAddress = "127.0.0.1:8848";

    /**
     * 开发环境
     */
    private String env = "dev";

    /**
     * netty 的 bossGroup 数量
     */
    private int eventLoopGroupBossNum = 1;

    /**
     * netty 的 workerGroup 数量
     */
    private int eventLoopGroupWorkerNum = Runtime.getRuntime().availableProcessors();
    //private int eventLoopGroupWorkerNum = 1;

    /**
     * 最大报文长度
     */
    private int maxContentLength = 64 * 1024 * 1024;

    /**
     * 默认单异步模式
     */
    private boolean whenComplete = true;

    // ----------------------------------------------	Http Async 参数选项：

    /**
     * 连接超时时间
     */
    private int httpConnectTimeout = 30 * 1000;

    /**
     * 请求超时时间
     */
    private int httpRequestTimeout = 30 * 1000;

    /**
     * 客户端请求重试次数
     */
    private int httpMaxRequestRetry = 2;

    /**
     * 客户端请求最大连接数
     */
    private int httpMaxConnections = 10000;

    /**
     * 客户端每个地址支持的最大连接数
     */
    private int httpConnectionsPerHost = 8000;

    /**
     * 客户端空闲连接超时时间, 默认60秒
     */
    private int httpPooledConnectionIdleTimeout = 60 * 1000;

    private String bufferType = "parallel";
}

