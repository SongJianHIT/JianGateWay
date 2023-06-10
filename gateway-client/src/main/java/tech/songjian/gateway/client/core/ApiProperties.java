/**
 * @projectName JianGateWay
 * @package tech.songjian.gateway.client.core
 * @className tech.songjian.gateway.client.core.ApiProperties
 */
package tech.songjian.gateway.client.core;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * ApiProperties
 * @description 配置，用于存放注册中心地址与环境
 * @author SongJian
 * @date 2023/6/10 12:00
 * @version
 */
@Data
@ConfigurationProperties(prefix = "api")
public class ApiProperties {

    /**
     * 注册中心地址
     */
    private String registerAddress;

    /**
     * 当前环境
     */
    private String env = "dev";
}

