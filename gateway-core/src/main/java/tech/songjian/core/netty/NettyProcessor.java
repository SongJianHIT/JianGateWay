/**
 * @projectName JianGateWay
 * @package tech.songjian.core.netty
 * @className tech.songjian.core.netty.NettyProcessor
 */
package tech.songjian.core.netty;

/**
 * NettyProcessor
 * @description 核心处理器
 * @author SongJian
 * @date 2023/6/9 08:53
 * @version
 */
public interface NettyProcessor {

    /**
     * 处理过程
     * @param httpRequestWrapper
     */
    void process(HttpRequestWrapper httpRequestWrapper);
}

