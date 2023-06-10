/**
 * @projectName JianGateWay
 * @package tech.songjian.gateway.client.support.dubbo
 * @className tech.songjian.gateway.client.support.dubbo.Dubbo27ClientRegisterManager
 */
package tech.songjian.gateway.client.support.dubbo;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.config.spring.context.event.ServiceBeanExportedEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import tech.songjian.common.config.ServiceDefinition;
import tech.songjian.common.config.ServiceInstance;
import tech.songjian.common.utils.NetUtils;
import tech.songjian.common.utils.TimeUtil;
import tech.songjian.gateway.client.core.ApiAnnotationScanner;
import tech.songjian.gateway.client.core.ApiProperties;
import tech.songjian.gateway.client.support.AbstractClientRegisterManager;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static tech.songjian.common.constants.BasicConst.COLON_SEPARATOR;
import static tech.songjian.common.constants.GatewayConst.DEFAULT_WEIGHT;

/**
 * Dubbo27ClientRegisterManager
 * @description
 * @author SongJian
 * @date 2023/6/10 13:18
 * @version
 */
@Slf4j
public class Dubbo27ClientRegisterManager
        extends AbstractClientRegisterManager
        implements ApplicationListener<ApplicationEvent> {

    /**
     * 存储处理过的 bean
     */
    private Set<Objects> set = new HashSet<>();



    public Dubbo27ClientRegisterManager(ApiProperties apiProperties) {
        super(apiProperties);
    }

    /**
     * 事件回调
     * @param applicationEvent
     */
    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        // 监听的事件：ServiceBeanExportedEvent
        // 在一个 Service 暴露完之后，会产生此事件。因此在其暴露时，注册
        if (applicationEvent instanceof ServiceBeanExportedEvent) {
            try {
                ServiceBean serviceBean = ((ServiceBeanExportedEvent) applicationEvent).getServiceBean();
                doRegisterDubbo(serviceBean);
            } catch (Exception e) {
                log.info("doRegisterDubbo error");
                throw new RuntimeException(e);
            }
        } else if (applicationEvent instanceof ApplicationStartedEvent) {
            log.info("dubbo api started");
        }
    }

    private void doRegisterDubbo(ServiceBean serviceBean) {
        // 这个bean才是真正的bean
        Object bean = serviceBean.getRef();
        if (set.contains(bean)) {
            // 如果已经处理过
            return;
        }
        // 扫描
        ServiceDefinition serviceDefinition = ApiAnnotationScanner.getInstance().scanner(bean, serviceBean);
        if (serviceDefinition == null) {
            // 服务定义为空，跳过
            return;
        }
        serviceDefinition.setEnvType(getApiProperties().getEnv());

        // 服务实例
        ServiceInstance serviceInstance = new ServiceInstance();
        String localIp = NetUtils.getLocalIp();
        int port = serviceBean.getProtocol().getPort();
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

        // 注册
        register(serviceDefinition, serviceInstance);
    }
}

