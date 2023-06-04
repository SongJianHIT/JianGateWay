/**
 * @projectName JianGateWay
 * @package tech.songjian.common.rule
 * @className tech.songjian.common.rule.Rule
 */
package tech.songjian.common.rule;

import lombok.Data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Rule
 * @description 规则对象
 * @author SongJian
 * @date 2023/6/4 16:58
 * @version
 */
@Data
public class Rule implements Comparable<Rule>, Serializable {

    /**
     * 全局唯一规则id
     */
    private String id;

    /**
     * 规则名称
     */
    private String name;

    /**
     * 规则对应的协议
     */
    private String protocol;

    /**
     * 规则优先级
     */
    private Integer order;

    /**
     * 规则配置
     */
    private Set<FilterConfig> filterConfigs = new HashSet<>();

    public Rule() {
        super();
    }

    public Rule(String id, String name, String protocol, Integer order, Set<FilterConfig> filterConfigs) {
        super();
        this.id = id;
        this.name = name;
        this.protocol = protocol;
        this.order = order;
        this.filterConfigs = filterConfigs;
    }

    /**
     * 规则配置类
     */
    @Data
    public static class FilterConfig {

        /**
         * 规则配置id
         */
        private String id;

        /**
         * 配置信息
         */
        private String config;

        /**
         * 重写 equals 必须重写 hashcode
         * @param o
         * @return
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            FilterConfig that = (FilterConfig) o;
            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    /**
     * 向规则中添加配置
     * @param filterConfig
     * @return
     */
    public boolean addFilterConfig(FilterConfig filterConfig) {
        return filterConfigs.add(filterConfig);
    }

    /**
     * 根据指定的 filter id 获取对应的配置信息
     * @param id
     * @return
     */
    public FilterConfig getFilterConfig (String id) {
        for (FilterConfig filterConfig : filterConfigs) {
            if (filterConfig.getId().equalsIgnoreCase(id)) {
                return filterConfig;
            }
        }
        return null;
    }

    /**
     * 根据指定的 filter id 判断配置信息是否存在
     * @param id
     * @return
     */
    public boolean hashId (String id) {
        for (FilterConfig filterConfig : filterConfigs) {
            if (filterConfig.getId().equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断两个规则的优先级
     * @param o the object to be compared.
     * @return
     */
    @Override
    public int compareTo(Rule o) {
        int orderCompare = Integer.compare(getOrder(), o.getOrder());
        if (orderCompare == 0) {
            return getId().compareTo(o.getId());
        }
        return orderCompare;
    }

    /**
     * 重写 equals 必须重写 hashcode
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Rule that = (Rule) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

