/**
 * @projectName JianGateWay
 * @package tech.songjian.core
 * @className tech.songjian.core.Bootstrap
 */
package tech.songjian.core;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import tech.songjian.common.config.DynamicConfigManager;
import tech.songjian.common.config.Rule;
import tech.songjian.common.config.ServiceDefinition;
import tech.songjian.common.config.ServiceInstance;
import tech.songjian.common.utils.NetUtils;
import tech.songjian.common.utils.TimeUtil;
import tech.songjian.gateway.config.center.api.ConfigCenter;
import tech.songjian.gateway.config.center.api.RulesChangeListener;
import tech.songjian.gateway.register.center.api.RegisterCenter;
import tech.songjian.gateway.register.center.api.RegisterCenterListener;


import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import static tech.songjian.common.constants.BasicConst.COLON_SEPARATOR;

/**
 * Bootstrap
 * @description API网关启动类
 * @author SongJian
 * @date 2023/6/4 11:13
 * @version
 */
@Slf4j
public class Bootstrap {

    public static void main(String[] args) {
        // 1、加载网关的核心静态配置
        Config config = ConfigLoader.getInstance().load(args);

        // 2、插件初始化

        // 3、配置中心管理器初始化：连接配置中心，监听配置中心的新增、修改、删除
        configCenterInitAndSubscribe(config);

        // 4、启动容器
        Container container = new Container(config);
        container.start();

        // 5、连接注册中心，将注册中心的实例加载到本地
        final RegisterCenter registerCenter = registerAndSubscribe(config);

        // 6、服务优雅关机
        // 进程收到 kill 信号的时候调用
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                registerCenter.deregister(buildGatewayServiceDefinition(config), buildGatewayServiceInstance(config));
                container.shutdown();
            }
        });
    }

    private static void configCenterInitAndSubscribe(Config config) {

        // ServiceLoader 它用来实现 SPI(Service Provider Interface)，一种服务发现机制，很多框架用它来做来做服务的扩展发现。
        ServiceLoader<ConfigCenter> serviceLoader = ServiceLoader.load(ConfigCenter.class);
        final ConfigCenter configCenter = serviceLoader.findFirst().orElseThrow(() -> {
            log.error("没有找到【注册中心】的具体实现类！");
            return new RuntimeException("没有找到【注册中心】的具体实现类！");
        });
        configCenter.init(config.getRegistryAddress(), config.getEnv());
        log.info("【配置中心】初始化完成：{}", configCenter);
        configCenter.subscribeRulesChange(rules -> DynamicConfigManager.getInstance().putAllRule(rules));
        log.info("【配置中心】订阅规则完成：{}", configCenter);
    }

    /**
     * 注册与订阅
     * @param config
     * @return
     */
    private static RegisterCenter registerAndSubscribe(Config config) {

        // SPI 机制，通过线程上下文类加载器加载实现类
        ServiceLoader<RegisterCenter> serviceLoader = ServiceLoader.load(RegisterCenter.class);
        final RegisterCenter registerCenter = serviceLoader.findFirst().orElseThrow(() -> {
            log.error("没有找到【注册中心】的具体实现类！");
            return new RuntimeException("没有找到【注册中心】的具体实现类！");
        });

        // 注册中心初始化
        registerCenter.init(config.getRegistryAddress(), config.getEnv());
        log.info("【注册中心】初始化完成：{}", registerCenter);

        // 5.1、构造网关服务定义和服务实例
        ServiceDefinition serviceDefinition = buildGatewayServiceDefinition(config);
        ServiceInstance serviceInstance = buildGatewayServiceInstance(config);

        // 5.2、注册网关服务
        registerCenter.register(serviceDefinition, serviceInstance);

        // 5.3 订阅所有服务
        registerCenter.subscribeAllServices(new RegisterCenterListener() {
            @Override
            public void onChange(ServiceDefinition serviceDefinition, Set<ServiceInstance> serviceInstanceSet) {
                log.info("【注册中心】更新服务与实例: {} {}", serviceDefinition.getUniqueId(), JSON.toJSON(serviceInstanceSet));
                // 由 DynamicConfigManager 根据传入的 服务定义 和 对应服务的实例集合 进行缓存！
                DynamicConfigManager manager = DynamicConfigManager.getInstance();
                manager.addServiceInstance(serviceDefinition.getUniqueId(), serviceInstanceSet);
                manager.putServiceDefinition(serviceDefinition.getUniqueId(), serviceDefinition);
            }
        });
        log.info("【注册中心】订阅服务完成：{}", registerCenter);

        return registerCenter;
    }

    /**
     * 构造网关的服务实例
     * @param config
     * @return
     */
    private static ServiceInstance buildGatewayServiceInstance(Config config) {
        String localIp = NetUtils.getLocalIp();
        int port = config.getPort();
        ServiceInstance serviceInstance = new ServiceInstance();
        serviceInstance.setServiceInstanceId(localIp + COLON_SEPARATOR + port);
        serviceInstance.setIp(localIp);
        serviceInstance.setPort(port);
        serviceInstance.setRegisterTime(TimeUtil.currentTimeMillis());
        return serviceInstance;
    }

    /**
     * 构造网关的服务定义
     * @param config
     * @return
     */
    private static ServiceDefinition buildGatewayServiceDefinition(Config config) {
        ServiceDefinition serviceDefinition = new ServiceDefinition();
        serviceDefinition.setInvokerMap(Map.of());
        serviceDefinition.setUniqueId(config.getApplicationName());
        serviceDefinition.setServiceId(config.getApplicationName());
        serviceDefinition.setEnvType(config.getEnv());
        return serviceDefinition;
    }
}

