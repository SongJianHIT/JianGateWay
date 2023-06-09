/**
 * @projectName JianGateWay
 * @package tech.songjian.core
 * @className tech.songjian.core.LifeCycle
 */
package tech.songjian.core;

/**
 * LifeCycle
 * @description 组件的生命周期接口
 * @author SongJian
 * @date 2023/6/9 08:18
 * @version
 */
public interface LifeCycle {

    /**
     * 初始化
     */
    void init();

    /**
     * 启动
     */
    void start();

    /**
     * 关闭
     */
    void shutdown();
}
