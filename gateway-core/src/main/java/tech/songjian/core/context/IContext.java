/**
 * @projectName JianGateWay
 * @package tech.songjian.core.context
 * @className tech.songjian.core.context.IContext
 */
package tech.songjian.core.context;

import io.netty.channel.ChannelHandlerContext;

import java.util.function.Consumer;

/**
 * IContext
 * @description 上下文接口：
 *                  1、网关的生命周期状态设计、获取
 *                  2、获取协议
 *                  3、获取请求对象
 *                  4、获取、设置返回对象
 *                  5、获取、设置异常信息
 *                  6、获取 Netty 上下文
 *                  7、释放请求资源
 *                  8、设置写回接收回调函数
 * @author SongJian
 * @date 2023/6/4 13:13
 * @version
 */
public interface IContext {

    /**
     * 上下文生命周期：运行中状态
     */
    int Running = 1;

    /**
     * 上下文生命周期：运行过程中发生错误，对其进行标记，请求已经结束，需要返回客户端
     */
    int Written = 0;

    /**
     * 上下文生命周期：标记写回成功，防止并发情况下多次写回
     */
    int Completed = 1;

    /**
     * 上下文生命周期：网关请求结束
     */
    int Terminated = 2;

    /**
     * 设置上下文状态为运行中
     */
    void setRunning();

    /**
     * 设置上下文状态为标记写回
     */
    void setWritten();

    /**
     * 设置上下文状态为标记写回成功
     */
    void setCompleted();

    /**
     * 设置上下文状态为请求结束
     */
    void setTerminated();

    /**
     * 判断网关状态
     * @return
     */
    boolean isRunning();

    /**
     * 判断网关状态
     * @return
     */
    boolean isWritten();

    /**
     * 判断网关状态
     * @return
     */
    boolean isCompleted();

    /**
     * 判断网关状态
     * @return
     */
    boolean isTerminated();

    /**
     * 获取协议
     * @return
     */
    String getProtocol();

    /**
     * 获取请求对象
     * @return
     */
    Object getRequest();

    /**
     * 获取返回对象
     * @return
     */
    Object getResponse();

    /**
     * 设置返回对象
     */
    void setResponse(Object response);

    /**
     * 获取异常信息
     * @return
     */
    Throwable getThrowable();

    /**
     * 设置异常信息
     * @param throwable
     */
    void setThrowable(Throwable throwable);

    /**
     * 获取 Netty 上下文
     * @return
     */
    ChannelHandlerContext getNettyCtx();

    /**
     * 判断是否长连接
     * @return
     */
    boolean isKeepAlive();

    /**
     * 释放请求资源
     * @return
     */
    boolean releaseRequest();

    /**
     * 设置写回接收回调函数
     * @param consumer
     */
    void setCompletedCallBack(Consumer<IContext> consumer);

    /**
     * 执行写回接收回调函数
     */
    void invokeCompletedCallBack();
}
