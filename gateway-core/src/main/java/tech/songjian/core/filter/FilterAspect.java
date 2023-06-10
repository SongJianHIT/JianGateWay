/**
 * @projectName JianGateWay
 * @package tech.songjian.core.filter
 * @className tech.songjian.core.filter.FilterAspect
 */
package tech.songjian.core.filter;

/**
 * FilterAspect
 * @description 过滤器注解类
 * @author SongJian
 * @date 2023/6/10 17:20
 * @version
 */
public @interface FilterAspect {

    /**
     * 过滤器 id
     * @return
     */
    String id();

    /**
     * 过滤器名称
     * @return
     */
    String name() default "";

    /**
     * 排序
     * @return
     */
    int order() default 0;
}
