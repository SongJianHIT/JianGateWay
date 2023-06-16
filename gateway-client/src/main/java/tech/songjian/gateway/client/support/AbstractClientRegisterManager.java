/**
 * @projectName JianGateWay
 * @package tech.songjian.gateway.client.support
 * @className tech.songjian.gateway.client.support.AbstractClientRegisterManager
 */
package tech.songjian.gateway.client.support;

import lombok.Getter;
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
     * 下游服务配置
     */
    @Getter
    private ApiProperties apiProperties;

    /**
     * 注册中心客户端
     */
    private RegisterCenter registerCenter;

    /**
     * 构造函数，初始化注册中心对象
     * @param apiProperties
     */
    protected AbstractClientRegisterManager (ApiProperties apiProperties) {
        this.apiProperties = apiProperties;
        // 初始化 注册中心 对象
        ServiceLoader<RegisterCenter> serviceLoader = ServiceLoader.load(RegisterCenter.class);
        registerCenter = serviceLoader.findFirst().orElseThrow(() -> {
            log.error("没有找到【注册中心】的具体实现类！");
            return new RuntimeException("没有找到【注册中心】的具体实现类！");
        });
        registerCenter.init(apiProperties.getRegisterAddress(), apiProperties.getEnv());
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

