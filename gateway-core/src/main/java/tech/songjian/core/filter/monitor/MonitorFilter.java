package tech.songjian.core.filter.monitor;

import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import tech.songjian.core.context.GatewayContext;
import tech.songjian.core.filter.Filter;
import tech.songjian.core.filter.FilterAspect;

import static tech.songjian.common.constants.FilterConst.*;

/**
 * Created by IntelliJ IDEA.
 * tech.songjian.core.filter.monitor
 *
 * @Author: SongJian
 * @Create: 2023/6/13 13:04
 * @Version:
 * @Describe:
 */
@Slf4j
@FilterAspect(id = MONITOR_FILTER_ID, name = MONITOR_FILTER_NAME, order = MONITOR_FILTER_ORDER)
public class MonitorFilter implements Filter {
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        ctx.setTimerSample(Timer.start());
    }
}
