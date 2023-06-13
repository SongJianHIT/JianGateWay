/**
 * @projectName JianGateWay
 * @package tech.songjian.core.netty.processor
 * @className tech.songjian.core.netty.processor.NettyCoreProcess
 */
package tech.songjian.core.netty.processor;

import com.lmax.disruptor.dsl.ProducerType;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import tech.songjian.common.enums.ResponseCode;
import tech.songjian.common.exception.BaseException;
import tech.songjian.core.Config;
import tech.songjian.core.context.GatewayContext;
import tech.songjian.core.disruptor.EventListener;
import tech.songjian.core.disruptor.ParallelQueueHandler;
import tech.songjian.core.filter.FilterFactory;
import tech.songjian.core.filter.GatewayFilterChainFactory;
import tech.songjian.core.helper.RequestHelper;
import tech.songjian.core.helper.ResponseHelper;
import tech.songjian.core.request.HttpRequestWrapper;

/**
 * NettyCoreProcess
 * @description disruptor 流程处理类
 * @author SongJian
 * @date 2023/6/9 09:01
 * @version
 */
@Slf4j
public class DisruptorNettyCoreProcessor implements NettyProcessor {

    private static final String THREAD_NAME_PREFIX = "gateway-queue-";

    private Config config;

    private NettyCoreProcessor nettyCoreProcessor;

    private ParallelQueueHandler<HttpRequestWrapper> parallelQueueHandler;

    public DisruptorNettyCoreProcessor(Config config, NettyCoreProcessor nettyCoreProcessor) {
        this.config = config;
        this.nettyCoreProcessor = nettyCoreProcessor;
        ParallelQueueHandler.Builder<HttpRequestWrapper> builder = new ParallelQueueHandler.Builder<HttpRequestWrapper>()
                .setBufferSize(config.getBufferSize())
                .setThreads(config.getProcessThread())
                .setProducerType(ProducerType.MULTI)
                .setNamePrefix(THREAD_NAME_PREFIX)
                .setWaitStrategy(config.getWaitStrategy());
        BatchEventListenerProcessor batchEventListenerProcessor = new BatchEventListenerProcessor();
        builder.setListener(batchEventListenerProcessor);
        this.parallelQueueHandler = builder.build();
    }

    @Override
    public void process(HttpRequestWrapper httpRequestWrapper) {
        this.parallelQueueHandler.add(httpRequestWrapper);
    }

    public class BatchEventListenerProcessor implements EventListener<HttpRequestWrapper> {

        @Override
        public void onEvent(HttpRequestWrapper event) {
            nettyCoreProcessor.process(event);
        }

        @Override
        public void onException(Throwable ex, long sequence, HttpRequestWrapper event) {
            HttpRequest request = event.getRequest();
            ChannelHandlerContext ctx = event.getCtx();
            try {
                log.error("BatchEventListenerProcessor onException 请求写回失败，request:{}，errormsg：{}", request, ex.getMessage(), ex);
                // 构建响应对象
                FullHttpResponse fullHttpResponse = ResponseHelper.getHttpResponse(ResponseCode.INTERNAL_ERROR);
                if (!HttpUtil.isKeepAlive(request)) {
                    ctx.writeAndFlush(fullHttpResponse).addListener(ChannelFutureListener.CLOSE);
                } else {
                    fullHttpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                    ctx.writeAndFlush(fullHttpResponse);
                }
            } catch (Exception e) {
                log.error("BatchEventListenerProcessor onException 请求写回失败，request:{}，errormsg：{}", request, e.getMessage(), e);
            }
        }
    }
}

