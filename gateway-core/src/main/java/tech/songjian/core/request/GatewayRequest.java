/**
 * @projectName JianGateWay
 * @package tech.songjian.core.request
 * @className tech.songjian.core.request.GatewayRequest
 */
package tech.songjian.core.request;

import com.google.common.collect.Lists;
import com.jayway.jsonpath.JsonPath;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import lombok.Getter;

import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import tech.songjian.common.constants.BasicConst;
import tech.songjian.common.utils.TimeUtil;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;

/**
 * GatewayRequest
 * @description JianGateway请求对象
 * @author SongJian
 * @date 2023/6/4 15:11
 * @version
 */
public class GatewayRequest implements IGatewayRequest{

    /**
     * 服务唯一id
     */
    @Getter
    private final String uniqueId;

    /**
     * 进入网关的开始时间
     */
    @Getter
    private final long beginTime;

    /**
     * 字符集
     */
    @Getter
    private final Charset charSet;

    /**
     * 客户端的IP
     */
    @Getter
    private final String clientIp;

    /**
     * 服务端主机名
     */
    @Getter
    private final String host;

    /**
     * 服务端的请求路径（不包含请求参数），如：XXX/XX/XX
     */
    @Getter
    private final String path;

    /**
     * 统一资源表示符，如 XXX/XX/XX?attr1=1&attr2=2
     */
    @Getter
    private final String uri;

    /**
     * 请求方式：POST/GET/PUT
     */
    @Getter
    private final HttpMethod method;

    /**
     * 请求格式
     */
    @Getter
    private final String contentType;

    /**
     * 请求头
     */
    @Getter
    private final HttpHeaders headers;

    /**
     * 参数解析器
     */
    @Getter
    private final QueryStringDecoder queryStringDecoder;

    /**
     * fullHttpRequest
     */
    @Getter
    private final FullHttpRequest fullHttpRequest;

    /**
     * 请求体
     */
    private String body;

    /**
     * JWT 解析出的用户 id
     */
    @Getter
    @Setter
    private long userId;

    /**
     * 一个请求可能分发到多个服务中去，可能存在多个cookie
     */
    private Map<String, Cookie> cookieMap;

    /**
     *  POST 请求参数
     */
    private Map<String, List<String>> postParameters;


    /************************************** 可修改的请求变量 ***************************************/

    /**
     * 可修改的 Scheme，默认为 http://
     */
    private String modifyScheme;

    /**
     * 可修改的主机名
     */
    private String modifyHost;

    /**
     * 可修改的请求路径
     */
    private String modifyPath;

    /**
     * 下游请求的 http 请求构建器
     */
    private final RequestBuilder requestBuilder;

    public GatewayRequest(String uniquedId, Charset charset, String clientIp,
                          String host, String uri, HttpMethod method, String contentType,
                          HttpHeaders headers, FullHttpRequest fullHttpRequest) {
        this.uniqueId = uniquedId;
        this.beginTime = TimeUtil.currentTimeMillis();
        this.charSet = charset;
        this.clientIp = clientIp;
        this.host = host;
        this.uri = uri;
        this.method = method;
        this.contentType = contentType;
        this.headers = headers;
        this.fullHttpRequest = fullHttpRequest;

        // 使用 netty 的 QueryStringDecoder 解析出路径
        this.queryStringDecoder = new QueryStringDecoder(uri, charset);
        this.path  = queryStringDecoder.path();

        this.modifyHost = host;
        this.modifyPath = path;
        this.modifyScheme = BasicConst.HTTP_PREFIX_SEPARATOR;

        this.requestBuilder = new RequestBuilder();
        this.requestBuilder.setMethod(getMethod().name());
        this.requestBuilder.setHeaders(getHeaders());
        this.requestBuilder.setQueryParams(queryStringDecoder.parameters());

        // 从完成的 HTTP 请求体中获取内容
        ByteBuf contentBuffer = fullHttpRequest.content();
        if(Objects.nonNull(contentBuffer)){
            // 传递给下游服务
            this.requestBuilder.setBody(contentBuffer.nioBuffer());
        }
    }

    /**
     * 获取请求体方法
     * @return
     */
    public String getBody() {
        if (StringUtils.isNotEmpty(body)) {
            body = fullHttpRequest.content().toString(charSet);
        }
        return body;
    }

    /**
     * 获取 Cookie 方法
     * @return
     */
    public Cookie getCookie(String name) {
        if (cookieMap == null) {
            cookieMap = new HashMap<String, Cookie>();
            // 从 header 中拿到 cookie
            String cookieStr = getHeaders().get(HttpHeaderNames.COOKIE);
            // 解析 cookie 成集合
            Set<Cookie> cookies = ServerCookieDecoder.STRICT.decode(cookieStr);
            for (Cookie cookie : cookies) {
                cookieMap.put(name, cookie);
            }
        }
        return cookieMap.get(name);
    }

    /**
     * 获取指定名称的参数值
     * @param name
     * @return
     */
    public List<String> getQueryParametersMultiple (String name) {
        return queryStringDecoder.parameters().get(name);
    }

    /**
     * 获取指定名称的参数值
     * @param name
     * @return
     */
    public List<String> getPostParametersMultiple (String name) {
        String body = getBody();
        if (isFormPost()) {
            // 如果是 form 表单的 POST
            if (postParameters == null) {
                QueryStringDecoder paramDecoder = new QueryStringDecoder(body, false);
                postParameters = paramDecoder.parameters();
            }
            if (postParameters == null || postParameters.isEmpty()) {
                return null;
            } else {
                return postParameters.get(name);
            }
        } else if (isJsonPost()) {
            // 如果是 JSON 的 POST
            return Lists.newArrayList(JsonPath.read(body, name).toString());
        }
        return null;
    }

    /**
     * 判断是否是 JSON POST
     * @return
     */
    private boolean isJsonPost() {
        return HttpMethod.POST.equals(method) &&
                contentType.startsWith(HttpHeaderValues.APPLICATION_JSON.toString());
    }

    /**
     * 判断是否是表单 POST
     * @return
     */
    public boolean isFormPost() {
        return HttpMethod.POST.equals(method) && (
                contentType.startsWith(HttpHeaderValues.FORM_DATA.toString()) ||
                contentType.startsWith(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString()));

    }



    @Override
    public void setModifyHost(String host) {
        this.modifyHost = host;
    }

    @Override
    public String getModifyHost() {
        return modifyHost;
    }

    @Override
    public void setModifyPath(String path) {
        this.modifyPath = path;
    }

    @Override
    public String getModifyPath() {
        return modifyPath;
    }

    @Override
    public void addHeader(CharSequence name, String value) {
        requestBuilder.addHeader(name, value);
    }

    @Override
    public void setHeader(CharSequence name, String value) {
        requestBuilder.setHeader(name, value);
    }

    @Override
    public void addQueryParam(String name, String value) {
        requestBuilder.addQueryParam(name, value);
    }

    @Override
    public void addFormParam(String name, String value) {
        if (isFormPost()) {
            requestBuilder.addFormParam(name, value);
        }
    }

    @Override
    public void addOrReplaceCookie(org.asynchttpclient.cookie.Cookie cookie) {
        requestBuilder.addOrReplaceCookie(cookie);
    }

    @Override
    public void setRequestTimeout(int requestTimeout) {
        requestBuilder.setRequestTimeout(requestTimeout);
    }

    @Override
    public String getFinalURL() {
        return modifyScheme + modifyHost + modifyPath;
    }



    /**
     * 转发之前执行的
     * 构造请求
     * @return
     */
    @Override
    public Request build() {
        requestBuilder.setUrl(getFinalURL());
        requestBuilder.setHeader("userId", String.valueOf(userId));
        return requestBuilder.build();
    }
}

