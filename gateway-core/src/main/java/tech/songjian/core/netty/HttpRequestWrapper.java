/**
 * @projectName JianGateWay
 * @package tech.songjian.core.netty
 * @className tech.songjian.core.netty.HttpRequestWrapper
 */
package tech.songjian.core.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.Data;

/**
 * HttpRequestWrapper
 * @description HTTP包装
 * @author SongJian
 * @date 2023/6/9 08:51
 * @version
 */
@Data
public class HttpRequestWrapper {
    private FullHttpRequest request;
    private ChannelHandlerContext ctx;
}

