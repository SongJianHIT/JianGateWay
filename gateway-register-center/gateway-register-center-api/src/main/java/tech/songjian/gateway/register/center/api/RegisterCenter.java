/**
 * @projectName JianGateWay
 * @package tech.songjian.gateway.register.center.api
 * @className tech.songjian.gateway.register.center.api.RegisterCenter
 */
package tech.songjian.gateway.register.center.api;

import tech.songjian.common.config.ServiceDefinition;
import tech.songjian.common.config.ServiceInstance;

/**
 * RegisterCenter
 * @description 注册中心管理接口
 * @author SongJian
 * @date 2023/6/9 19:24
 * @version
 */
public interface RegisterCenter {

    /**
     * 初始化
     * @param regsiterAddress
     * @param env
     */
    void init(String regsiterAddress, String env);

    /**
     * 注册
     * @param serviceDefinition
     * @param serviceInstance
     */
    void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance);

    /**
     * 注销
     * @param serviceDefinition
     * @param serviceInstance
     */
    void deregister(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance);

    /**
     * 订阅所有服务的变更
     * @param registerCenterListener
     */
    void subscribeAllServices(RegisterCenterListener registerCenterListener);

}
