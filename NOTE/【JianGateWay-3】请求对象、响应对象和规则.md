# 【JianGateWay-3】请求对象、响应对象和规则

## 请求对象

一个网关的请求对象需要：

- **接受原始请求**：
  - 获取目标服务的主机
  - 获取目标服务的路径
- **通过网关一系列操作，对原始请求进行自定义改造**
  - 修改目标服务的主机
  - 修改目标服务的路径
  - 设置请求头内容
  - 添加请求头内容
  - 添加表单请求
  - 添加或替换Cookie
  - 设置超时时间
- **最终构造出实际的请求地址**

```JAVA
public interface IGatewayRequest {

    /**
     * 修改目标服务主机
     * @param host
     */
    void setModifyHost(String host);

    /**
     * 获取目标服务主机
     * @return
     */
    String getModifyHost();

    /**
     * 设置目标服务路径
     * @param path
     */
    void setModifyPath(String path);

    /**
     * 获取目标服务路径
     * @return
     */
    String getModifyPath();

    /**
     * 添加请求头
     * @param name
     * @param value
     */
    void addHeader(CharSequence name, String value);

    /**
     * 设置请求头
     * @param name
     * @param value
     */
    void setHeader(CharSequence name, String value);

    /**
     * 添加请求参数（GET 请求）
     * @param name
     * @param value
     */
    void addQueryParam(String name, String value);

    /**
     * 添加表单请求参数（POST 请求）
     * @param name
     * @param value
     */
    void addFormParam(String name, String value);

    /**
     * 添加或者替换 Cookie
     * @param cookie
     */
    void addOrReplaceCookie(Cookie cookie);

    /**
     * 设置超时时间
     * @param requestTimeout
     */
    void setRequestTimeout(int requestTimeout);

    /**
     * 获取最终请求路径，包含请求参数
     * 如：http://localhost:8081/api/admin?name=songjian
     * @return
     */
    String getFinalURL();

    /**
     * 请求构建
     * @return
     */
    Request build();
}
```

我们实现这个接口，定义完整的 GatewayRequest ：

- **服务唯一 id** ：用于标识这个请求是请求到哪个服务的
- **进入网关时间**：用于统计请求整个时间
- **字符集**：用于后续解码所使用，一般设置为 UTF-8
- **客户端 Ip**
- **服务端主机**
- **请求路径**，如 XXX/XX/XX
- **请求的统一资源表示符**，如 XXX/XX/XX?attr1=1&attr2=2
- **请求方式**
- **请求体格式**
- **请求体内容**
- **请求头**
- **Netty 的参数解析器**：用于解析 path
- **FullHttpRequest**：是Netty框架中的一个类，用于表示一个完整的 HTTP 请求。它包含了请求的方法、URI、HTTP 版本、请求头、请求体等信息。

```JAVA
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

    /**
     * 可修改的 Scheme，默认为 http://
     */
    private String modifyScheme;

    /********************* 可修改的请求变量 *********************/

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

    public GatewayRequest(String uniquedId, Charset charset, String clientIp, String host, String uri, HttpMethod method, String contentType, HttpHeaders headers, FullHttpRequest fullHttpRequest) {
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
        this.queryStringDecoder = new QueryStringDecoder(uri,charset);
        this.path  = queryStringDecoder.path();
        this.modifyHost = host;
        this.modifyPath = path;

        this.modifyScheme = BasicConst.HTTP_PREFIX_SEPARATOR;
        this.requestBuilder = new RequestBuilder();
        this.requestBuilder.setMethod(getMethod().name());
        this.requestBuilder.setHeaders(getHeaders());
        this.requestBuilder.setQueryParams(queryStringDecoder.parameters());

        ByteBuf contentBuffer = fullHttpRequest.content();
        if(Objects.nonNull(contentBuffer)){
            this.requestBuilder.setBody(contentBuffer.nioBuffer());
        }
    }

    /**
     * 获取请求体方法
     * @return
     */
    public String getBody() {
        if (StringUtils.isEmpty(body)) {
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
```

## 响应对象

响应对象比较简单：

- 响应状态码
- 响应头
- 额外的响应结果
- 响应内容
- 异步返回对象

```JAVA
@Data
public class GatewayResponse {

    /**
     * 响应头
     */
    private HttpHeaders responseHeaders = new DefaultHttpHeaders();

    /**
     * 额外的响应结果
     */
    private final HttpHeaders extraResponseHeaders = new DefaultHttpHeaders();
    /**
     * 响应内容
     */
    private String content;

    /**
     * 异步返回对象
     */
    private Response futureResponse;

    /**
     * 响应返回码
     */
    private HttpResponseStatus httpResponseStatus;


    public GatewayResponse(){

    }

    /**
     * 设置响应头信息
     * @param key
     * @param val
     */
    public void putHeader(CharSequence key, CharSequence val){
        responseHeaders.add(key, val);
    }

    /**
     * 构建异步响应对象
     * @param futureResponse
     * @return
     */
    public static GatewayResponse buildGatewayResponse(Response futureResponse){
        GatewayResponse response = new GatewayResponse();
        response.setFutureResponse(futureResponse);
        response.setHttpResponseStatus(HttpResponseStatus.valueOf(futureResponse.getStatusCode()));
        return response;
    }

    /**
     * 处理返回json对象，失败时调用
     * @param code
     * @param args
     * @return
     */
    public static GatewayResponse buildGatewayResponse(ResponseCode code,Object...args){
        ObjectNode objectNode = JSONUtil.createObjectNode();
        objectNode.put(JSONUtil.STATUS,code.getStatus().code());
        objectNode.put(JSONUtil.CODE,code.getCode());
        objectNode.put(JSONUtil.MESSAGE,code.getMessage());

        GatewayResponse response = new GatewayResponse();
        response.setHttpResponseStatus(code.getStatus());
        response.putHeader(HttpHeaderNames.CONTENT_TYPE,HttpHeaderValues.APPLICATION_JSON+";charset=utf-8");
        response.setContent(JSONUtil.toJSONString(objectNode));
        return response;
    }

    /**
     * 处理返回json对象，成功时调用
     * @param data
     * @return
     */
    public static GatewayResponse buildGatewayResponse(Object data){
        ObjectNode objectNode = JSONUtil.createObjectNode();
        objectNode.put(JSONUtil.STATUS, ResponseCode.SUCCESS.getStatus().code());
        objectNode.put(JSONUtil.CODE, ResponseCode.SUCCESS.getCode());
        objectNode.putPOJO(JSONUtil.DATA, data);

        GatewayResponse response = new GatewayResponse();
        response.setHttpResponseStatus(ResponseCode.SUCCESS.getStatus());
        response.putHeader(HttpHeaderNames.CONTENT_TYPE,HttpHeaderValues.APPLICATION_JSON+";charset=utf-8");
        response.setContent(JSONUtil.toJSONString(objectNode));
        return response;
    }
}
```

## 规则

规则说明了一个请求进到网关后的处理逻辑，是否需要限流？熔断？如何转发？

```YAML
{
    "rules": [
        {
            "id":"user-private",
            "name":"user-private",
            "protocol":"http",
            "serviceId":"backend-http-server",
            "prefix":"/user/private",
            "paths": [
            ],
            "filterConfigs":[
                {
                    "id":"load_balancer_filter",
                    "config":{
                        "load_balancer": "Random"
                    }
                }, 
                {
                    "id": "user_auth_filter"
                }
            ]
        }, 
        {
            "id":"user",
            "name":"user",
            "protocol":"http",
            "serviceId":"backend-user-server",
            "prefix":"/user",
            "paths": [
            ],
            "filterConfigs":[
                {
                    "id":"load_balancer_filter",
                    "config": {
                        "load_balancer": "Random"
                    }
                }
            ]
        },
        {
            "id":"http-server",
            "name":"http-server",
            "protocol":"http",
            "serviceId":"backend-http-server",
            "prefix":"/http-server",
            "paths": [
                "/http-server/ping"
            ],
            "filterConfigs":[
                {
                    "id":"load_balancer_filter",
                    "config": {
                        "load_balancer": "Random"
                    }
                }
            ]
        }
    ]
}
```

一个网关可以有多个规则，可以根据请求地址的不同（或者其他情况）走不同的规则。

- **规则id**
- **规则名称**
- **请求协议**
- **规则排序**：一个路径符合多个规则的要求，走优先级高的规则
- **后端服务id**
- **请求前缀**
- **规则对应的路径集合**
- **过滤器的配置类**
- **所有过滤器的配置集合**

```JAVA
public class Rule implements Comparable<Rule>, Serializable {

    /**
     * 规则ID，全局唯一
     */
    private String id;

    /**
     * 规则名称
     */
    private String name;

    /**
     * 协议
     */
    private String protocol;

    /**
     * 规则排序，对应场景：一个路径对应多条规则，然后只执行一条规则的情况
     */
    private Integer order;

    /**
     * 后端服务id
     */
    private String serviceId;

    /**
     * 请求前缀
     */
    private String prefix;

    /**
     * 路径集合
     */
    private List<String> paths;

    private Set<FilterConfig> filterConfigs =new HashSet<>();

    private RetryConfig retryConfig = new RetryConfig();

    private Set<HystrixConfig> hystrixConfigs = new HashSet<>();

    /**
     * 限流规则配置
     */
    private Set<FlowCtlConfig> flowCtlConfigs =new HashSet<>();

    // GET SET...
    
    public Rule(){
        super();
    }

    public Rule(String id, String name, String protocol,
                Integer order, String serviceId, String prefix,
                List<String> paths, Set<FilterConfig> filterConfigs) {
        this.id = id;
        this.name = name;
        this.protocol = protocol;
        this.order = order;
        this.serviceId = serviceId;
        this.prefix = prefix;
        this.paths = paths;
        this.filterConfigs = filterConfigs;
    }

    /**
     * --------------------------------------  过滤器配置内部类  -----------------
     */
    public static class FilterConfig {

        /**
         * 过滤器唯一ID
         */
        private String id;
        /**
         * 过滤器规则描述，{"timeOut":500,"balance":random}
         */
        private String config;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getConfig() {
            return config;
        }

        public void setConfig(String config) {
            this.config = config;
        }

        @Override
        public  boolean equals(Object o){
            if (this == o) {
                return  true;
            }

            if ((o== null) || getClass() != o.getClass()) {
                return false;
            }

            FilterConfig that =(FilterConfig)o;
            return id.equals(that.id);
        }

        @Override
        public  int hashCode (){
            return Objects.hash(id);
        }
    }

    /**
     * 向规则里面添加过滤器
     * @param filterConfig
     * @return
     */
     public boolean addFilterConfig (FilterConfig filterConfig){
            return filterConfigs.add(filterConfig);
     }

    /**
     * 通过一个指定的FilterID获取FilterConfig
     * @param id
     * @return
     */
     public FilterConfig getFilterConfig (String id) {
         for (FilterConfig config:filterConfigs) {
             if (config.getId().equalsIgnoreCase(id)) {
                return config;
             }
         }
         return null;
     }

    /**
     * 根据filterID判断当前Rule是否存在
     * @return
     */
    public boolean hashId() {
        for(FilterConfig config:filterConfigs){
            if(config.getId().equalsIgnoreCase(id)){
                return true;
            }
        }
        return false;
    }


    @Override
    public int compareTo(Rule o) {
        int orderCompare = Integer.compare(getOrder(),o.getOrder());
        if(orderCompare == 0){
          return getId().compareTo(o.getId());
        }
        return orderCompare;
    }

    @Override
    public  boolean equals(Object o){
        if (this == o) return  true;

        if((o== null) || getClass() != o.getClass()){
            return false;
        }

        FilterConfig that =(FilterConfig)o;
        return id.equals(that.id);
    }

    @Override
    public  int hashCode(){
        return Objects.hash(id);
    }

    /**
     * --------------------------------------  重试机制内部类  -----------------
     */
    public static class RetryConfig {
        /**
         * 重试次数
         */
        private int times;

        public int getTimes() {
            return times;
        }

        public void setTimes(int times) {
            this.times = times;
        }
    }

    public RetryConfig getRetryConfig() {
        return retryConfig;
    }

    public void setRetryConfig(RetryConfig retryConfig) {
        this.retryConfig = retryConfig;
    }

    /**
     * --------------------------------------  限流流控配置内部类  -----------------
     */
    public static class FlowCtlConfig {
        /**
         * 限流类型：可对 path，ip 或服务
         */
        private String type;

        /**
         * 限流对象的值
         */
        private String value;

        /**
         * 限流模式：单机 or 分布式
         */
        private String model;

        /**
         * 限流规则
         */
        private String config;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getConfig() {
            return config;
        }

        public void setConfig(String config) {
            this.config = config;
        }
    }

    /**
     * --------------------------------------  熔断限流配置内部类  -----------------
     */
    @Data
    public static class HystrixConfig {
        private String path;
        private int timeoutInMilliseconds;
        private int threadCoreSize;
        private String fallbackResponse;
    }
}
```

