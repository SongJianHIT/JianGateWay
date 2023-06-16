/**
 * @projectName JianGateWay
 * @package tech.songjian.gateway.client.core.autoconfigure
 * @className tech.songjian.gateway.client.core.autoconfigure.ApiClientAutoConfiguration
 */
package tech.songjian.gateway.client.core.autoconfigure;

import org.apache.dubbo.config.spring.ServiceBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tech.songjian.gateway.client.core.ApiProperties;
import tech.songjian.gateway.client.support.dubbo.Dubbo27ClientRegisterManager;
import tech.songjian.gateway.client.support.springmvc.SpringMVCClientRegisterManager;

import javax.annotation.Resource;
import javax.servlet.Servlet;

/**
 * ApiClientAutoConfiguration
 * @description 自动装配，只有满足特定的情况才会装配
 * @author SongJian
 * @date 2023/6/10 13:33
 * @version
 */
// 该注解表明这是一个配置类，Spring 将会读取该类中的配置信息并应用它们。
@Configuration
// 该注解用于启用配置属性绑定。它告诉 Spring Boot 将 ApiProperties 类的实例与配置属性进行绑定
@EnableConfigurationProperties(ApiProperties.class)
// 该注解表示只有当名为 api.registerAddress 的属性存在于配置文件中时，才会应用该自动配置类
@ConditionalOnProperty(prefix = "api", name = {"registerAddress"})
public class ApiClientAutoConfiguration {

    @Resource
    private ApiProperties apiProperties;

    @Bean
    // 该注解表示只有当类路径中存在 Servlet、DispatcherServlet 和 WebMvcConfigurer 类时，
    // 才会创建该 Bean。这样可以确保该 Bean 只在 Spring MVC 环境下生效。
    @ConditionalOnClass({Servlet.class, DispatcherServlet.class, WebMvcConfigurer.class})
    @ConditionalOnMissingBean
    public SpringMVCClientRegisterManager springMVCClientRegisterManager() {
        return new SpringMVCClientRegisterManager(apiProperties);
    }

    @Bean
    @ConditionalOnClass({ServiceBean.class})
    @ConditionalOnMissingBean
    public Dubbo27ClientRegisterManager dubbo27ClientRegisterManager() {
        return new Dubbo27ClientRegisterManager(apiProperties);
    }
}

