package tech.songjian.core.filter.gray;

import lombok.extern.slf4j.Slf4j;
import tech.songjian.core.context.GatewayContext;
import tech.songjian.core.filter.Filter;
import tech.songjian.core.filter.FilterAspect;

import static tech.songjian.common.constants.FilterConst.*;

/**
 * Created by IntelliJ IDEA.
 * tech.songjian.core.filter.gray
 *
 * @Author: SongJian
 * @Create: 2023/6/13 12:01
 * @Version:
 * @Describe:
 */
@Slf4j
@FilterAspect(id = GRAY_FILTER_ID, name = GRAY_FILTER_NAME, order = GRAY_FILTER_ORDER)
public class GrayFilter implements Filter {

    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        // 测试时有用
        String gray = ctx.getRequest().getHeaders().get("gray");
        if ("true".equals(gray)) {
            ctx.setGray(true);
        }

        String clientIp = ctx.getRequest().getClientIp();
        // 对 1024 取模
        int res = clientIp.hashCode() & (1024 - 1);
        if (res == 1) {
            // 设置为灰度的情况，自定义
            ctx.setGray(true);
        }
    }
}
