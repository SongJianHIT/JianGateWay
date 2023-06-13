package tech.songjian.core.filter.monitor;

import com.alibaba.nacos.client.naming.utils.RandomUtils;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;

import tech.songjian.core.ConfigLoader;
import tech.songjian.core.context.GatewayContext;
import tech.songjian.core.filter.Filter;
import tech.songjian.core.filter.FilterAspect;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static tech.songjian.common.constants.FilterConst.*;

/**
 * Created by IntelliJ IDEA.
 * tech.songjian.core.filter.monitor
 *
 * @Author: SongJian
 * @Create: 2023/6/13 13:04
 * @Version:
 * @Describe:
 */
@Slf4j
@FilterAspect(id = MONITOR_END_FILTER_ID, name = MONITOR_END_FILTER_NAME, order = MONITOR_END_FILTER_ORDER)
public class MonitorEndFilter implements Filter {

    // 普罗米修斯的注册表
    private final PrometheusMeterRegistry prometheusMeterRegistry;

    public MonitorEndFilter() {
        this.prometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        // 暴露接口给普罗米修斯拉去数据
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(ConfigLoader.getConfig().getPrometheusPort()),0);
            server.createContext("/prometheus", exchange -> {
                // 该 api 用于获取指标数据的文本内容
                String scrape = prometheusMeterRegistry.scrape();
                // 指标数据返回
                exchange.sendResponseHeaders(200, scrape.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(scrape.getBytes());
                }
            });
            new Thread((server::start)).start();

        } catch (IOException e) {
            log.error("prometheus http server start error! ", e);
            throw new RuntimeException(e);
        }
        log.info("prometheus 启动成功！");

        // mock
        Executors.newScheduledThreadPool(1000).scheduleAtFixedRate(() -> {
            Timer.Sample sample = Timer.start();
            try {
                Thread.sleep(RandomUtils.nextInt(100));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            Timer timer = prometheusMeterRegistry.timer("gateway_request",
                    "uniqueId", "backend-http-server:1.0.0",
                    "protocol", "http",
                    "path", "/http-server/ping" + RandomUtils.nextInt(10));
            sample.stop(timer);
        },200, 100, TimeUnit.MILLISECONDS);
    }

    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        Timer timer = prometheusMeterRegistry.timer("gateway_request",
                "uniqueId", ctx.getUniqueId(),
                "protocol", ctx.getProtocol(),
                "path", ctx.getRequest().getPath());
        ctx.getTimerSample().stop(timer);
    }
}
