/**
 * @projectName JianGateWay
 * @package tech.songjian.gateway.client.core
 * @className tech.songjian.gateway.client.core.ApiService
 */
package tech.songjian.gateway.client.core;

import java.lang.annotation.*;

/**
 * ApiService
 * @description 注解，服务定义
 * @author SongJian
 * @date 2023/6/10 11:38
 * @version
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiService {
    /**
     * 服务id
     * @return
     */
    String serviceId();

    /**
     * 版本号
     * @return
     */
    String version() default "1.0.0";

    /**
     * 协议
     * @return
     */
    ApiProtocol protocol();

    /**
     * 匹配路径
     * @return
     */
    String patternPath();
}

