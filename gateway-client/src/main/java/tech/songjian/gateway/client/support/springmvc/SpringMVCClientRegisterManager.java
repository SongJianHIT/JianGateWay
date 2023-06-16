/**
 * @projectName JianGateWay
 * @package tech.songjian.gateway.client.support.springmvc
 * @className tech.songjian.gateway.client.support.springmvc.SpringMVCClientRegisterManager
 */
package tech.songjian.gateway.client.support.springmvc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tech.songjian.common.config.ServiceDefinition;
import tech.songjian.common.config.ServiceInstance;
import tech.songjian.common.utils.NetUtils;
import tech.songjian.common.utils.TimeUtil;
import tech.songjian.gateway.client.core.ApiAnnotationScanner;
import tech.songjian.gateway.client.core.ApiProperties;
import tech.songjian.gateway.client.support.AbstractClientRegisterManager;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static tech.songjian.common.constants.BasicConst.COLON_SEPARATOR;
import static tech.songjian.common.constants.GatewayConst.DEFAULT_WEIGHT;

/**
 * SpringMVCClientRegisterManager
 * @description
 * @author SongJian
 * @date 2023/6/10 12:56
 * @version
 */
@Slf4j
public class SpringMVCClientRegisterManager
        extends AbstractClientRegisterManager
        implements ApplicationListener<ApplicationEvent>, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Autowired
    private ServerProperties serverProperties;

    private Set<Object> set = new HashSet<>();

    public SpringMVCClientRegisterManager(ApiProperties apiProperties) {
        super(apiProperties);
    }

    /**
     * 设置 SpringMVC 上下文
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    /**
     * SpringMVC 启动事件执行
     * @param applicationEvent
     */
    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ApplicationStartedEvent) {
            // 如果是应用启动事件，则执行注册
            try {
                doRegisterSpringMvc();
            } catch (Exception e) {
                log.info("下游【SpringMVC服务】注册失败！");
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 注册具体逻辑：
     *      1、从 applicationContext 获取所有 RequestMappingHandlerMapping 的 bean 对象
     *      2、从而获取对应的 Controller 类
     *      3、注解解析，构造服务定义和服务实例
     *      4、调用注册中心的注册接口，完成服务注册
     */
    private void doRegisterSpringMvc() {
        // BeanFactoryUtils.beansOfTypeIncludingAncestors 方法可以
        // 从指定的 beanFactory 中获取指定类型的所有 bean，包括在祖先 bean 工厂中定义的 bean
        // 这里就是想要获取所有 RequestMappingHandlerMapping 的 bean 对象
        Map<String, RequestMappingHandlerMapping> allRequestMapping =
                BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext,
                RequestMappingHandlerMapping.class, true, false);

        for (RequestMappingHandlerMapping handlerMapping : allRequestMapping.values()) {
            // 拿到所有的方法
            Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();

            for (Map.Entry<RequestMappingInfo, HandlerMethod> me : handlerMethods.entrySet()) {
                HandlerMethod handlerMethod = me.getValue();
                Class<?> clazz = handlerMethod.getBeanType();

                // 重点在这：获取 Controller 的实例bean
                Object bean = applicationContext.getBean(clazz);
                if (set.contains(bean)) {
                    // 如果已经处理过了，则跳过
                    continue;
                }

                // 传入 bean 对象，并扫描其上面的注解，返回服务定义
                ServiceDefinition serviceDefinition = ApiAnnotationScanner.getInstance().scanner(bean);
                if (serviceDefinition == null) {
                    // 如果拿不到服务定义，跳过
                    continue;
                }
                serviceDefinition.setEnvType(getApiProperties().getEnv());

                // 服务实例
                ServiceInstance serviceInstance = new ServiceInstance();
                String localIp = NetUtils.getLocalIp();
                int port = serverProperties.getPort();
                String serviceInstanceId = localIp + COLON_SEPARATOR + port;
                String uniqueId = serviceDefinition.getUniqueId();
                String version = serviceDefinition.getVersion();

                serviceInstance.setServiceInstanceId(serviceInstanceId);
                serviceInstance.setUniqueId(uniqueId);
                serviceInstance.setVersion(version);
                serviceInstance.setIp(localIp);
                serviceInstance.setPort(port);
                serviceInstance.setRegisterTime(TimeUtil.currentTimeMillis());
                serviceInstance.setWeight(DEFAULT_WEIGHT);

                if (getApiProperties().isGray()) {
                    // 如果是灰度服务
                    serviceInstance.setGray(true);
                }

                // 注册
                register(serviceDefinition, serviceInstance);
            }
        }
    }
}

