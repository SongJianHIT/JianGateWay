/**
 * @projectName JianGateWay
 * @package tech.songjian.gateway.client.support
 * @className tech.songjian.gateway.client.support.AbstractClientRegisterManager
 */
package tech.songjian.gateway.client.support;

import lombok.extern.slf4j.Slf4j;
import tech.songjian.common.config.ServiceDefinition;
import tech.songjian.common.config.ServiceInstance;
import tech.songjian.gateway.client.core.ApiProperties;
import tech.songjian.gateway.register.center.api.RegisterCenter;

import java.util.ServiceLoader;

/**
 * AbstractClientRegisterManager
 * @description 抽象注册管理器
 * @author SongJian
 * @date 2023/6/10 11:59
 * @version
 */
@Slf4j
public abstract class AbstractClientRegisterManager {

    /**
     * 配置
     */
    private ApiProperties apiProperties;

    /**
     * 注册中心客户端
     */
    private RegisterCenter registerCenter;

    protected AbstractClientRegisterManager (ApiProperties apiProperties) {
        this.apiProperties = apiProperties;

        // 初始化注册中心对象
        ServiceLoader<RegisterCenter> serviceLoader = ServiceLoader.load(RegisterCenter.class);
        serviceLoader.findFirst().orElseThrow(() -> {
            log.error("not find RegisterCenter impl");
            return new RuntimeException("not find RegisterCenter impl");
        });
    }

    /**
     * 服务注册
     * @param serviceDefinition
     * @param serviceInstance
     */
    protected void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        registerCenter.register(serviceDefinition, serviceInstance);
    }
}

