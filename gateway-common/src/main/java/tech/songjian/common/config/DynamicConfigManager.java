package tech.songjian.common.config;

import org.checkerframework.checker.units.qual.A;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static tech.songjian.common.constants.BasicConst.DIT_SEPARATOR;

/**
 * 动态服务缓存配置管理类
 */
public class DynamicConfigManager {

	/**
	 * 服务的定义集合：uniqueId 代表服务的唯一标识
	 */
	private ConcurrentHashMap<String /* uniqueId */ , ServiceDefinition>  serviceDefinitionMap = new ConcurrentHashMap<>();

	/**
	 * 服务的实例集合：uniqueId 与一对服务实例对应
	 */
	private ConcurrentHashMap<String /* uniqueId */ , Set<ServiceInstance>>  serviceInstanceMap = new ConcurrentHashMap<>();

	/**
	 * 规则集合
	 */
	private ConcurrentHashMap<String /* ruleId */ , Rule>  ruleMap = new ConcurrentHashMap<>();

	/**
	 * 路径以及规则集合
	 */
	private ConcurrentHashMap<String /* 路径 */, Rule> pathRuleMap = new ConcurrentHashMap<>();

	/**
	 * 服务以及规则集合
	 */
	private ConcurrentHashMap<String /* 服务名 */, List<Rule>> serverRuleMap = new ConcurrentHashMap<>();

	private DynamicConfigManager() {
	}

	private static class SingletonHolder {
		private static final DynamicConfigManager INSTANCE = new DynamicConfigManager();
	}


	/***************** 	对服务定义缓存进行操作的系列方法 	***************/

	public static DynamicConfigManager getInstance() {
		return SingletonHolder.INSTANCE;
	}

	public void putServiceDefinition(String uniqueId,
			ServiceDefinition serviceDefinition) {

		serviceDefinitionMap.put(uniqueId, serviceDefinition);;
	}

	public ServiceDefinition getServiceDefinition(String uniqueId) {
		return serviceDefinitionMap.get(uniqueId);
	}

	public void removeServiceDefinition(String uniqueId) {
		serviceDefinitionMap.remove(uniqueId);
	}

	public ConcurrentHashMap<String, ServiceDefinition> getServiceDefinitionMap() {
		return serviceDefinitionMap;
	}

	/***************** 	对服务实例缓存进行操作的系列方法 	***************/

	public Set<ServiceInstance> getServiceInstanceByUniqueId(String uniqueId, boolean gray){
		Set<ServiceInstance> serviceInstances = serviceInstanceMap.get(uniqueId);
		if (CollectionUtils.isEmpty(serviceInstances)) {
			// 如果为空，直接返回
			return Collections.emptySet();
		}
		// 如果是灰度，则要过滤出灰度服务
		if (gray) {
			return serviceInstances.stream()
					.filter(ServiceInstance::isGray)
					.collect(Collectors.toSet());
		}
		// 否则，正常返回
		return serviceInstances;
	}

	public void addServiceInstance(String uniqueId, ServiceInstance serviceInstance) {
		Set<ServiceInstance> set = serviceInstanceMap.get(uniqueId);
		set.add(serviceInstance);
	}

	public void addServiceInstance(String uniqueId, Set<ServiceInstance> serviceInstanceSet) {
		serviceInstanceMap.put(uniqueId, serviceInstanceSet);
	}

	public void updateServiceInstance(String uniqueId, ServiceInstance serviceInstance) {
		Set<ServiceInstance> set = serviceInstanceMap.get(uniqueId);
		Iterator<ServiceInstance> it = set.iterator();
		while(it.hasNext()) {
			ServiceInstance is = it.next();
			if(is.getServiceInstanceId().equals(serviceInstance.getServiceInstanceId())) {
				it.remove();
				break;
			}
		}
		set.add(serviceInstance);
	}

	public void removeServiceInstance(String uniqueId, String serviceInstanceId) {
		Set<ServiceInstance> set = serviceInstanceMap.get(uniqueId);
		Iterator<ServiceInstance> it = set.iterator();
		while(it.hasNext()) {
			ServiceInstance is = it.next();
			if(is.getServiceInstanceId().equals(serviceInstanceId)) {
				it.remove();
				break;
			}
		}
	}

	public void removeServiceInstancesByUniqueId(String uniqueId) {
		serviceInstanceMap.remove(uniqueId);
	}


	/***************** 	对规则缓存进行操作的系列方法 	***************/

	public void putRule(String ruleId, Rule rule) {
		ruleMap.put(ruleId, rule);
	}

	public void putAllRule(List<Rule> ruleList) {
		ConcurrentHashMap<String, Rule> newRuleMap = new ConcurrentHashMap<>();
		ConcurrentHashMap<String, Rule> newPathMap = new ConcurrentHashMap<>();
		ConcurrentHashMap<String, List<Rule>> newServiceMap = new ConcurrentHashMap<>();

		for (Rule rule : ruleList) {
			newRuleMap.put(rule.getId(), rule);
			List<Rule> rules = newServiceMap.get(rule.getServiceId());
			if (rules == null) {
				rules = new ArrayList<>();
			}
			rules.add(rule);
			newServiceMap.put(rule.getServiceId(), rules);

			List<String> paths = rule.getPaths();
			for (String path : paths) {
				String key = rule.getServiceId() + DIT_SEPARATOR + path;
				newPathMap.put(key, rule);
			}
		}
		ruleMap = newRuleMap;
		pathRuleMap = newPathMap;
		serverRuleMap = newServiceMap;
	}

	public Rule getRule(String ruleId) {
		return ruleMap.get(ruleId);
	}

	public void removeRule(String ruleId) {
		ruleMap.remove(ruleId);
	}

	public ConcurrentHashMap<String, Rule> getRuleMap() {
		return ruleMap;
	}

	/**
	 * 根据路径获取规则
	 * @param path
	 * @return
	 */
	public Rule getRuleByPath (String path) {
		return pathRuleMap.get(path);
	}

	/**
	 * 根据服务id获取规则集合
	 * @param serviceId
	 * @return
	 */
	public List<Rule> getRuleByServiceId(String serviceId) {
		return serverRuleMap.get(serviceId);
	}
}
