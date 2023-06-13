package tech.songjian.core.disruptor;

/**
 * Created by IntelliJ IDEA.
 * tech.songjian.core.disruptor
 *
 * @Author: SongJian
 * @Create: 2023/6/13 20:36
 * @Version:
 * @Describe: 多生产者、多消费者处理接口
 */
public interface ParallelQueue<E> {

    /**
     * 添加元素
     * @param event
     */
    void add (E event);

    /**
     * 添加元素
     * @param event
     */
    void add (E... event);

    /**
     * 添加元素，有返回值
     * @param event
     * @return
     */
    boolean tryAdd(E event);
    boolean tryAdd(E... event);

    /**
     * 启动
     */
    void start();

    /**
     * 销毁
     */
    void shutDown();

    /**
     * 判断是否已经被销毁
     * @return
     */
    boolean isShutDown();
}
