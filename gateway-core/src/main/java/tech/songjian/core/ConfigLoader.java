/**
 * @projectName JianGateWay
 * @package tech.songjian.core
 * @className tech.songjian.core.ConfigLoader
 */
package tech.songjian.core;

import lombok.extern.slf4j.Slf4j;
import tech.songjian.common.utils.PropertiesUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * ConfigLoader
 * @description
 * @author SongJian
 * @date 2023/6/8 23:17
 * @version
 */
@Slf4j
public class ConfigLoader {

    private static final String CONFIG_FILE = "gateway.properties";

    private static final String ENV_PREFIX = "GATEWAY_";

    private static final String JVM_PREFIX = "gateway.";

    /**
     * ConfigLoader 使用 单例设计模式 构造
     */
    private static final ConfigLoader INSTANCE = new ConfigLoader();

    /**
     * 存放配置
     */
    private Config config;

    private ConfigLoader() {}

    public static ConfigLoader getInstance() {
        return INSTANCE;
    }

    /**
     * 获取配置
     * @return
     */
    public static Config getConfig() {
        return INSTANCE.config;
    }

    /**
     * 优先级高的会覆盖优先级低的
     * 运行参数 -- jvm 参数 -- 环境变量 -- 配置文件 -- 配置对象默认值
     * @param args
     * @return
     */
    public Config load(String[] args) {
        // 默认值
        config = new Config();
        // 配置文件
        loadFromConfigFile();
        // 环境变量
        loadFromEnv();
        // jvm 参数
        loadFromJvm();
        // 运行参数
        loadFromArgs(args);
        return config;
    }

    /**
     * 从运行参数中读取配置
     */
    private void loadFromArgs(String[] args) {
        if (args != null && args.length > 0) {
            Properties properties = new Properties();
            for (String arg : args) {
                if (arg.startsWith("--") && arg.contains("=")) {
                    properties.put(arg.substring(2, arg.indexOf("=")), arg.substring(arg.indexOf("=") + 1));
                }
            }
            PropertiesUtils.properties2Object(properties, config);
        }
    }

    /**
     * 从 JVM 参数中读取配置
     */
    private void loadFromJvm() {
        Properties properties = System.getProperties();
        PropertiesUtils.properties2Object(properties, config, JVM_PREFIX);
    }

    /**
     * 从环境变量中加载配置
     */
    private void loadFromEnv() {
        Map<String, String> env = System.getenv();
        Properties properties = new Properties();
        properties.putAll(env);
        PropertiesUtils.properties2Object(properties, config, ENV_PREFIX);
    }

    /**
     * 从配置文件中加载配置
     * @return
     */
    private void loadFromConfigFile() {
        InputStream inputStream = ConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
        if (inputStream != null) {
            Properties properties = new Properties();
            try {
                properties.load(inputStream);
                // 把属性复制到 config 对象中
                PropertiesUtils.properties2Object(properties, config);
            } catch (IOException e) {
                log.warn("load config file {} error", CONFIG_FILE, e);
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    //
                }
            }
        }
    }
}

