package tech.songjian.core.filter.router;

import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import tech.songjian.common.enums.ResponseCode;
import tech.songjian.common.exception.ConnectException;
import tech.songjian.common.exception.ResponseException;
import tech.songjian.core.ConfigLoader;
import tech.songjian.core.context.GatewayContext;
import tech.songjian.core.filter.Filter;
import tech.songjian.core.filter.FilterAspect;
import tech.songjian.core.helper.AsyncHttpHelper;
import tech.songjian.core.helper.ResponseHelper;
import tech.songjian.core.response.GatewayResponse;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static tech.songjian.common.constants.FilterConst.*;

/**
 * Created by IntelliJ IDEA.
 * tech.songjian.core.filter.router
 *
 * @Author: SongJian
 * @Create: 2023/6/12 14:37
 * @Version:
 * @Describe: 路由过滤器类
 */
@Slf4j
@FilterAspect(id = ROUTER_FILTER_ID, name = ROUTER_FILTER_NAME, order = ROUTER_FILTER_ORDER)
public class RouterFilter implements Filter {


    /**
     * 路由请求转发
     * @param gatewayContext
     * @throws Exception
     */
    @Override
    public void doFilter(GatewayContext gatewayContext) throws Exception {
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

    @Override
    public int getOrder() {
        return ROUTER_FILTER_ORDER;
    }
}
