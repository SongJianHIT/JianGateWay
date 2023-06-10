/**
 * @projectName JianGateWay
 * @package tech.songjian.core.netty.processor
 * @className tech.songjian.core.netty.processor.NettyCoreProcess
 */
package tech.songjian.core.netty.processor;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import tech.songjian.common.enums.ResponseCode;
import tech.songjian.common.exception.BaseException;
import tech.songjian.common.exception.ConnectException;
import tech.songjian.common.exception.ResponseException;
import tech.songjian.core.ConfigLoader;
import tech.songjian.core.context.GatewayContext;
import tech.songjian.core.filter.FilterFactory;
import tech.songjian.core.filter.GatewayFilterChainFactory;
import tech.songjian.core.helper.AsyncHttpHelper;
import tech.songjian.core.helper.RequestHelper;
import tech.songjian.core.helper.ResponseHelper;
import tech.songjian.core.request.HttpRequestWrapper;
import tech.songjian.core.response.GatewayResponse;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * NettyCoreProcess
 * @description 核心处理方法具体实现
 * @author SongJian
 * @date 2023/6/9 09:01
 * @version
 */
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

            // 进行路由
            route(gatewayContext);
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

    /**
     * 路由函数，请求转发
     * @param gatewayContext
     */
    private void route(GatewayContext gatewayContext) {
        Request request = gatewayContext.getRequest().build();
        // 发起请求
        CompletableFuture<Response> future = AsyncHttpHelper.getInstance().executeRequest(request);

        boolean whenComplete = ConfigLoader.getConfig().isWhenComplete();
        if (whenComplete) {
            // 单异步模式
            future.whenComplete((response, throwable) -> {
                complete(request, response, throwable, gatewayContext);
            });
        } else {
            // 双异步模式
            future.whenCompleteAsync((response, throwable) -> {
                complete(request, response, throwable, gatewayContext);
            });
        }
    }

    private void complete(Request request, Response response, Throwable throwable, GatewayContext gatewayContext) {
        // 释放请求资源
        gatewayContext.releaseRequest();

        try {
            // 判断有没有异常
            if (Objects.nonNull(throwable)) {
                String url = request.getUrl();
                if (throwable instanceof TimeoutException) {
                    log.warn("complete time out {}", url);
                    gatewayContext.setThrowable(new ResponseException(ResponseCode.REQUEST_TIMEOUT));
                } else {
                    gatewayContext.setThrowable(new ConnectException(throwable,
                            gatewayContext.getUniqueId(),
                            url, ResponseCode.HTTP_RESPONSE_ERROR));
                }
            } else {
                // 没有异常，正常响应结果
                gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(response));
            }
        } catch (Throwable t) {
            gatewayContext.setThrowable(new ResponseException(ResponseCode.INTERNAL_ERROR));
            log.error("complete error", t);
        } finally {
            // 改变 context 状态
            gatewayContext.setWritten();
            // 写回数据
            ResponseHelper.writeResponse(gatewayContext);
        }
    }
}

