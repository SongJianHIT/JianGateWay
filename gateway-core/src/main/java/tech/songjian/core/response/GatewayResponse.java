/**
 * @projectName JianGateWay
 * @package tech.songjian.core.response
 * @className tech.songjian.core.response.GatewayResponse
 */
package tech.songjian.core.response;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.handler.codec.http.*;
import lombok.Data;

import org.asynchttpclient.Response;
import tech.songjian.common.enums.ResponseCode;
import tech.songjian.common.utils.JSONUtil;

/**
 * GatewayResponse
 * @description 网关回复消息对象
 * @author SongJian
 * @date 2023/6/4 16:24
 * @version
 */
@Data
public class GatewayResponse {

    /**
     * 响应头
     */
    private HttpHeaders responseHeaders = new DefaultHttpHeaders();

    /**
     * 额外的响应头，可自定义
     */
    private HttpHeaders extraResponseHeaders = new DefaultHttpHeaders();

    /**
     * 响应内容
     */
    private String content;

    /**
     * 返回响应状态码
     */
    private HttpResponseStatus httpResponseStatus;

    /**
     * 异步返回对象
     */
    private Response futureResponse;

    public GatewayResponse() {
    }

    /**
     * 设置响应头信息
     * @param key
     * @param value
     */
    public void putHeader(CharSequence key, CharSequence value) {
        responseHeaders.add(key, value);
    }

    /**
     * 构建异步响应对象
     * @param futureResponse
     * @return
     */
    public static GatewayResponse buildGatewayResponse (Response futureResponse) {
        GatewayResponse response = new GatewayResponse();
        response.setFutureResponse(futureResponse);
        response.setHttpResponseStatus(HttpResponseStatus.valueOf(futureResponse.getStatusCode()));
        return response;
    }

    /**
     * 返回一个 JSON 类型的响应信息，失败时使用
     * @return
     */
    public static GatewayResponse buildGatewayResponse (ResponseCode code, Object...args) {
        // TODO common
        ObjectNode objectNode = JSONUtil.createObjectNode();
        // 状态码
        objectNode.put(JSONUtil.STATUS, code.getStatus().code());
        objectNode.put(JSONUtil.CODE, code.getCode());
        objectNode.put(JSONUtil.MESSAGE, code.getMessage());

        GatewayResponse response = new GatewayResponse();
        response.setHttpResponseStatus(code.getStatus());
        response.putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + ";charset=utf-8");
        response.setContent(JSONUtil.toJSONString(objectNode));
        return response;
    }

    /**
     * 返回一个 JSON 类型的响应信息，成功时使用
     * @return
     */
    public static GatewayResponse buildGatewayResponse (Object data) {
        // TODO common
        ObjectNode objectNode = JSONUtil.createObjectNode();
        // 状态码
        objectNode.put(JSONUtil.STATUS, ResponseCode.SUCCESS.getStatus().code());
        objectNode.put(JSONUtil.CODE, ResponseCode.SUCCESS.getStatus().code());
        objectNode.putPOJO(JSONUtil.DATA, data);

        GatewayResponse response = new GatewayResponse();
        response.setHttpResponseStatus(ResponseCode.SUCCESS.getStatus());
        response.putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + ";charset=utf-8");
        response.setContent(JSONUtil.toJSONString(objectNode));
        return response;
    }
}

