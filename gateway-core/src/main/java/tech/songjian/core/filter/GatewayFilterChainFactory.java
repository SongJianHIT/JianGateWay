/**
 * @projectName JianGateWay
 * @package tech.songjian.core.filter
 * @className tech.songjian.core.filter.GatewayFilterChainFactory
 */
package tech.songjian.core.filter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import tech.songjian.common.config.Rule;
import tech.songjian.common.constants.FilterConst;
import tech.songjian.core.context.GatewayContext;
import tech.songjian.core.filter.router.RouterFilter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GatewayFilterChainFactory
 * @description 过滤器链条工厂类
 * @author SongJian
 * @date 2023/6/10 22:21
 * @version
 */
@Slf4j
public class GatewayFilterChainFactory implements FilterFactory{

    /**
     * 将单例对象的定义放在静态内部类 SingletonInstance 中
     * 静态内部类并不会随着外部类的加载一起加载，只有在使用时才会加载；
     * 类加载的过程则直接保证了线程安全性，保证实例对象的唯一。
     */
    private static class SingletonInstance {
        private static final GatewayFilterChainFactory INSTANCE = new GatewayFilterChainFactory();
    }

    private GatewayFilterChainFactory() {
        ServiceLoader<Filter>  serviceLoader = ServiceLoader.load(Filter.class);
        serviceLoader.stream().forEach(filterProvider -> {
            Filter filter = filterProvider.get();
            FilterAspect annotation = filter.getClass().getAnnotation(FilterAspect.class);
            log.info("load filter success:{},{},{},{}",filter.getClass(),
                    annotation.id(),annotation.name(),annotation.order());
            if (annotation != null) {
                // 添加到过滤集合
                String filterId = annotation.id();
                if(StringUtils.isEmpty(filterId)){
                    filterId = filter.getClass().getName();
                }
                processorFilterIdMap.putIfAbsent(filterId, filter);
            }
        });
    }

    public static GatewayFilterChainFactory getInstance() {
        return SingletonInstance.INSTANCE;
    }

    public Map<String /* filterId */, Filter> processorFilterIdMap = new ConcurrentHashMap<>();

    @Override
    public GatewayFilterChain buildFilterChain(GatewayContext ctx) throws Exception {
        GatewayFilterChain chain = new GatewayFilterChain();
        ArrayList<Filter> filters = new ArrayList<>();
        // 添加灰度发布过滤器
        filters.add(getFilterInfo(FilterConst.GRAY_FILTER_ID));
        // 添加监控过滤器
        filters.add(getFilterInfo(FilterConst.MONITOR_FILTER_ID));
        filters.add(getFilterInfo(FilterConst.MONITOR_END_FILTER_ID));
        // 通过规则 rule 构建
        Rule rule = ctx.getRule();
        if (rule != null) {
            Set<Rule.FilterConfig> filterConfigs = rule.getFilterConfigs();
            Iterator iterator = filterConfigs.iterator();
            Rule.FilterConfig filterConfig;
            while (iterator.hasNext()) {
                filterConfig = (Rule.FilterConfig) iterator.next();
                if (filterConfig == null) {
                    continue;
                }
                String filterId = filterConfig.getId();
                if (StringUtils.isNotEmpty(filterId) && getFilterInfo(filterId) != null) {
                    // 根据 filterId 获取对应的 filter，并添加
                    Filter filter = getFilterInfo(filterId);
                    filters.add(filter);
                }
            }
        }
        // 最后一个过滤器：添加路由过滤器
        filters.add(new RouterFilter());
        // 排序
        filters.sort(Comparator.comparingInt(Filter::getOrder));
        // 添加到链中
        chain.addFilterList(filters);
        return chain;
    }

    @Override
    public Filter getFilterInfo(String filterId) throws Exception {
        return processorFilterIdMap.get(filterId);
    }
}

