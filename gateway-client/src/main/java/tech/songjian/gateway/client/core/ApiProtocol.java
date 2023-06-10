/**
 * @projectName JianGateWay
 * @package tech.songjian.gateway.client.core
 * @className tech.songjian.gateway.client.core.ApiProtocol
 */
package tech.songjian.gateway.client.core;

/**
 * ApiProtocol
 * @description 协议
 * @author SongJian
 * @date 2023/6/10 11:40
 * @version
 */

public enum ApiProtocol {

    /**
     * HTTP 协议
     */
    HTTP("http", "http协议"),

    /**
     * DUBBO 协议
     */
    DUBBO("dubbo", "dubbo协议");

    private String code;

    private String desc;

    ApiProtocol(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}

