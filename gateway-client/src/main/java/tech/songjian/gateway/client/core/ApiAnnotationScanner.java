package tech.songjian.gateway.client.core;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.spring.ServiceBean;
import tech.songjian.common.config.DubboServiceInvoker;
import tech.songjian.common.config.HttpServiceInvoker;
import tech.songjian.common.config.ServiceDefinition;
import tech.songjian.common.config.ServiceInvoker;
import tech.songjian.common.constants.BasicConst;
import tech.songjian.gateway.client.support.dubbo.DubboConstants;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 注解扫描类
 */
public class ApiAnnotationScanner {

    private ApiAnnotationScanner() {
    }

    private static class SingletonHolder {
        static final ApiAnnotationScanner INSTANCE = new ApiAnnotationScanner();
    }

    public static ApiAnnotationScanner getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * 扫描传入的 bean 对象，最终返回一个服务定义
     * @param bean
     * @param args
     * @return
     */
    public ServiceDefinition scanner(Object bean, Object... args) {
        Class<?> aClass = bean.getClass();
        // 判断是否加了 ApiService 注解
        if (!aClass.isAnnotationPresent(ApiService.class)) {
            return null;
        }

        // 拿到服务的注解数据
        ApiService apiService = aClass.getAnnotation(ApiService.class);
        String serviceId = apiService.serviceId();
        ApiProtocol protocol = apiService.protocol();
        String patternPath = apiService.patternPath();
        String version = apiService.version();

        Map<String, ServiceInvoker> invokerMap = new HashMap<>();
        // 扫描目标类上的所有方法
        Method[] methods = aClass.getMethods();
        if (methods != null && methods.length > 0) {
            for (Method method : methods) {
                // 尝试拿到 ApiInvoker 注解
                ApiInvoker apiInvoker = method.getAnnotation(ApiInvoker.class);
                if (apiInvoker == null) {
                    // 如果方法上没有加 ApiInvoker，说明不是暴露的服务方法
                    continue;
                }
                // 否则，处理服务方法
                // 根据注解，获取方法的访问路径
                String path = apiInvoker.path();
                // 判断服务协议
                switch (protocol) {
                    // 根据访问路径，构造最最最后的访问路径
                    case HTTP:
                        HttpServiceInvoker httpServiceInvoker = createHttpServiceInvoker(path);
                        invokerMap.put(path, httpServiceInvoker);
                        break;
                    case DUBBO:
                        ServiceBean<?> serviceBean = (ServiceBean<?>) args[0];
                        DubboServiceInvoker dubboServiceInvoker = createDubboServiceInvoker(path, serviceBean, method);

                        String dubboVersion = dubboServiceInvoker.getVersion();
                        if (!StringUtils.isBlank(dubboVersion)) {
                            version = dubboVersion;
                        }
                        invokerMap.put(path, dubboServiceInvoker);
                        break;
                    default:
                        break;
                }
            }

            // 构造返回对象
            ServiceDefinition serviceDefinition = new ServiceDefinition();
            serviceDefinition.setUniqueId(serviceId + BasicConst.COLON_SEPARATOR + version);
            serviceDefinition.setServiceId(serviceId);
            serviceDefinition.setVersion(version);
            serviceDefinition.setProtocol(protocol.getCode());
            serviceDefinition.setPatternPath(patternPath);
            serviceDefinition.setEnable(true);
            serviceDefinition.setInvokerMap(invokerMap);

            return serviceDefinition;
        }

        return null;
    }

    /**
     * 构建 HttpServiceInvoker 对象
     */
    private HttpServiceInvoker createHttpServiceInvoker(String path) {
        HttpServiceInvoker httpServiceInvoker = new HttpServiceInvoker();
        httpServiceInvoker.setInvokerPath(path);
        return httpServiceInvoker;
    }

    /**
     * 构建DubboServiceInvoker对象
     */
    private DubboServiceInvoker createDubboServiceInvoker (String path, ServiceBean<?> serviceBean, Method method) {
        DubboServiceInvoker dubboServiceInvoker = new DubboServiceInvoker();
        dubboServiceInvoker.setInvokerPath(path);

        String methodName = method.getName();
        String registerAddress = serviceBean.getRegistry().getAddress();
        String interfaceClass = serviceBean.getInterface();

        dubboServiceInvoker.setRegisterAddress(registerAddress);
        dubboServiceInvoker.setMethodName(methodName);
        dubboServiceInvoker.setInterfaceClass(interfaceClass);

        String[] parameterTypes = new String[method.getParameterCount()];
        Class<?>[] classes = method.getParameterTypes();
        for (int i = 0; i < classes.length; i++) {
            parameterTypes[i] = classes[i].getName();
        }
        dubboServiceInvoker.setParameterTypes(parameterTypes);

        Integer seriveTimeout = serviceBean.getTimeout();
        if (seriveTimeout == null || seriveTimeout.intValue() == 0) {
            ProviderConfig providerConfig = serviceBean.getProvider();
            if (providerConfig != null) {
                Integer providerTimeout = providerConfig.getTimeout();
                if (providerTimeout == null || providerTimeout.intValue() == 0) {
                    seriveTimeout = DubboConstants.DUBBO_TIMEOUT;
                } else {
                    seriveTimeout = providerTimeout;
                }
            }
        }
        dubboServiceInvoker.setTimeout(seriveTimeout);

        String dubboVersion = serviceBean.getVersion();
        dubboServiceInvoker.setVersion(dubboVersion);

        return dubboServiceInvoker;
    }
}
