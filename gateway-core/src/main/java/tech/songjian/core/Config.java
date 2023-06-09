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
     * 应用名
     */
    private String applicationName = "api-gateway";

    /**
     * 配置中心地址
     */
    private String registryAddr = "127.0.0.1:8848";

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

    /**
     * 最大报文长度
     */
    private int maxContentLength = 64 * 1024 * 1024;

    /**
     * 默认单异步模式
     */
    private boolean whenComplete = true;
}

