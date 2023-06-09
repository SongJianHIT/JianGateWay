package tech.songjian.core.helper;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.apache.commons.lang3.StringUtils;


import tech.songjian.common.config.HttpServiceInvoker;
import tech.songjian.common.config.Rule;
import tech.songjian.common.config.ServiceDefinition;
import tech.songjian.common.config.ServiceInvoker;
import tech.songjian.common.constants.BasicConst;
import tech.songjian.common.constants.GatewayConst;
import tech.songjian.common.constants.GatewayProtocol;
import tech.songjian.core.context.GatewayContext;
import tech.songjian.core.request.GatewayRequest;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;


public class RequestHelper {

	public static GatewayContext doContext(FullHttpRequest request, ChannelHandlerContext ctx) {

		//	构建请求对象 GatewayRequest
		GatewayRequest gateWayRequest = doRequest(request, ctx);

		//	根据请求对象里的 uniqueId，获取资源服务信息(也就是服务定义信息)
		// TODO 先写死
		ServiceDefinition serviceDefinition = ServiceDefinition.builder()
				.serviceId("demo")
				.enable(true)
				.version("v1")
				.patternPath("**")
				.envType("dev")
				.protocol(GatewayProtocol.HTTP)
				.build();


		//	根据请求对象获取服务定义对应的方法调用，然后获取对应的规则
		ServiceInvoker serviceInvoker = new HttpServiceInvoker();
		serviceInvoker.setInvokerPath(gateWayRequest.getPath());
		serviceInvoker.setTimeout(500);


		//	构建我们而定 GateWayContext 对象
		GatewayContext gatewayContext = new GatewayContext(
				serviceDefinition.getProtocol(),
				ctx,
				HttpUtil.isKeepAlive(request),
				gateWayRequest,
				new Rule());


		// 后续服务发现做完，这里都要改成动态的
		gatewayContext.getRequest().setModifyHost("127.0.0.1:8080");

		return gatewayContext;
	}

	/**
	 *构建 Request 请求对象
	 */
	private static GatewayRequest doRequest(FullHttpRequest fullHttpRequest, ChannelHandlerContext ctx) {

		HttpHeaders headers = fullHttpRequest.headers();
		//	从 header 头获取必须要传入的关键属性 uniqueId
		String uniqueId = headers.get(GatewayConst.UNIQUE_ID);

		String host = headers.get(HttpHeaderNames.HOST);
		HttpMethod method = fullHttpRequest.method();
		String uri = fullHttpRequest.uri();
		String clientIp = getClientIp(ctx, fullHttpRequest);
		String contentType = HttpUtil.getMimeType(fullHttpRequest) == null ? null : HttpUtil.getMimeType(fullHttpRequest).toString();
		Charset charset = HttpUtil.getCharset(fullHttpRequest, StandardCharsets.UTF_8);

		GatewayRequest gatewayRequest = new GatewayRequest(uniqueId,
				charset,
				clientIp,
				host,
				uri,
				method,
				contentType,
				headers,
				fullHttpRequest);

		return gatewayRequest;
	}

	/**
	 * 获取客户端ip
	 */
	private static String getClientIp(ChannelHandlerContext ctx, FullHttpRequest request) {
		String xForwardedValue = request.headers().get(BasicConst.HTTP_FORWARD_SEPARATOR);

		String clientIp = null;
		if(StringUtils.isNotEmpty(xForwardedValue)) {
			List<String> values = Arrays.asList(xForwardedValue.split(", "));
			if(values.size() >= 1 && StringUtils.isNotBlank(values.get(0))) {
				clientIp = values.get(0);
			}
		}
		if(clientIp == null) {
			InetSocketAddress inetSocketAddress = (InetSocketAddress)ctx.channel().remoteAddress();
			clientIp = inetSocketAddress.getAddress().getHostAddress();
		}
		return clientIp;
	}


}
