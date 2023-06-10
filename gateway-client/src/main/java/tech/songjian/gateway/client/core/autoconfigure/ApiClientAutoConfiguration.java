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

import javax.servlet.Servlet;

/**
 * ApiClientAutoConfiguration
 * @description 自动装配
 * @author SongJian
 * @date 2023/6/10 13:33
 * @version
 */
@Configuration
@EnableConfigurationProperties(ApiProperties.class)
@ConditionalOnProperty(prefix = "api", name = {"registerAddress"})
public class ApiClientAutoConfiguration {

    @Autowired
    private ApiProperties apiProperties;

    @Bean
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

