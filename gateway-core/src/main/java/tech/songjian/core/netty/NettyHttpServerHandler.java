/**
 * @projectName JianGateWay
 * @package tech.songjian.core.netty
 * @className tech.songjian.core.netty.NettyHttpServerHandler
 */
package tech.songjian.core.netty;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * NettyHttpServerHandler
 * @description 服务端业务处理类
 * @author SongJian
 * @date 2023/6/9 08:41
 * @version
 */
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

