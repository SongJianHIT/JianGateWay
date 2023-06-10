/**
 * @projectName JianGateWay
 * @package tech.songjian.gateway.config.center.nacos
 * @className tech.songjian.gateway.config.center.nacos.NacosConfigCenter
 */
package tech.songjian.gateway.config.center.nacos;


import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.extern.slf4j.Slf4j;
import tech.songjian.common.config.Rule;
import tech.songjian.gateway.config.center.api.ConfigCenter;
import tech.songjian.gateway.config.center.api.RulesChangeListener;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * NacosConfigCenter
 * @description
 * @author SongJian
 * @date 2023/6/10 14:12
 * @version
 */
@Slf4j
public class NacosConfigCenter implements ConfigCenter {

    /**
     * 定义常量 DATA_ID
     */
    private static final String DATA_ID = "api-gateway";

    /**
     * 服务端地址
     */
    private String serverAddr;

    /**
     * 环境
     */
    private String env;

    /**
     * nacos 提供的
     */
    private ConfigService configService;

    @Override
    public void init(String serverAddr, String env) {
        this.serverAddr = serverAddr;
        this.env = env;

        try {
            configService  = NacosFactory.createConfigService(serverAddr);
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void subscribeRulesChange(RulesChangeListener listener) {
        try {
            // 初始化通知
            String config = configService.getConfig(DATA_ID, env, 5000);
            log.info("config from nacos: {}", config);
            // JSON 转化成 JAVA 对象
            List<Rule> rules = JSON.parseObject(config).getJSONArray("rules").toJavaList(Rule.class);
            listener.onRulesChange(rules);


            // 监听变化
            configService.addListener(DATA_ID, env, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("config from nacos: {}", configInfo);
                    List<Rule> rules = JSON.parseObject(configInfo).getJSONArray("rules").toJavaList(Rule.class);
                    listener.onRulesChange(rules);
                }
            });

        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }
}

