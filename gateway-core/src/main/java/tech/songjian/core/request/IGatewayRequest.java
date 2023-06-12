/**
 * @projectName JianGateWay
 * @package tech.songjian.core.request
 * @className tech.songjian.core.request.IGatewayRequest
 */
package tech.songjian.core.request;

import org.asynchttpclient.cookie.Cookie;
import org.asynchttpclient.Request;

/**
 * IGatewayRequest
 * @description 提供可修改Request参数的操作接口
 * @author SongJian
 * @date 2023/6/4 14:49
 * @version
 */
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
