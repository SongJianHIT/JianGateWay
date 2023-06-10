/**
 * @projectName JianGateWay
 * @package tech.songjian.core.filter
 * @className tech.songjian.core.filter.GatewayFilterChain
 */
package tech.songjian.core.filter;

import lombok.extern.slf4j.Slf4j;
import tech.songjian.core.context.GatewayContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GatewayFilterChain
 * @description 过滤器链条类
 * @author SongJian
 * @date 2023/6/10 22:10
 * @version
 */
@Slf4j
public class GatewayFilterChain {

    /**
     * 过滤器集合
     */
    private List<Filter> filters = null;

    /**
     * 向过滤器链中添加过滤器
     * @param filter
     * @return
     */
    public GatewayFilterChain addFilter(Filter filter) {
        filters.add(filter);
        return this;
    }

    /**
     * 向过滤器链中添加过滤器
     * @param filters
     * @return
     */
    public GatewayFilterChain addFilterList (List<Filter> filters) {
        filters.addAll(filters);
        return this;
    }

    /**
     * 过滤
     * @param ctx
     * @return
     * @throws Throwable
     */
    public GatewayContext doFilter(GatewayContext ctx) {
        if (filters.isEmpty()) {
            return ctx;
        }
        try {
            for (Filter filter : filters) {
                filter.doFilter(ctx);
            }
        } catch (Exception e) {
            // 发生异常后处理
            log.error("filter chain doFilter error {}", e.getMessage());
        }
        return ctx;
    }
}

