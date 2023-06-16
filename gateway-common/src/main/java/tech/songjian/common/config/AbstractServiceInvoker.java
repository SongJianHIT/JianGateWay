package tech.songjian.common.config;

/**
 * 抽象的服务调用接口实现类
 */
public class AbstractServiceInvoker implements ServiceInvoker {

	/**
	 * 真正服务调用的全路径
	 */
	protected String invokerPath;

	/**
	 * 超时事件，默认 5s
	 */
	protected int timeout = 5000;

	@Override
	public String getInvokerPath() {
		return invokerPath;
	}

	@Override
	public void setInvokerPath(String invokerPath) {
		this.invokerPath = invokerPath;
	}

	@Override
	public int getTimeout() {
		return timeout;
	}

	@Override
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

}
