package tech.songjian.core.filter.user;

import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultClaims;
import io.netty.handler.codec.http.cookie.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import tech.songjian.common.enums.ResponseCode;
import tech.songjian.common.exception.ResponseException;
import tech.songjian.core.context.GatewayContext;
import tech.songjian.core.filter.Filter;
import tech.songjian.core.filter.FilterAspect;

import static tech.songjian.common.constants.FilterConst.*;

/**
 * Created by IntelliJ IDEA.
 * tech.songjian.core.filter.user
 *
 * @Author: SongJian
 * @Create: 2023/6/13 10:36
 * @Version:
 * @Describe: 基于 JWT 的用户鉴权
 */
@Slf4j
@FilterAspect(id = USER_AUTH_FILTER_ID, name = USER_AUTH_FILTER_NAME, order = USER_AUTH_FILTER_ORDER)
public class UserAuthFilter implements Filter {

    /**
     * 密钥，不应该在代码中写死
     */
    private static final String SECRET_KEY = "";

    /**
     * 放入 cookie 的名称
     */
    private static final String COOKIE_NAME = "user-jwt";

    /**
     * 鉴权具体逻辑
     * @param ctx
     * @throws Exception
     */
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        if (ctx.getRule().getFilterConfig(USER_AUTH_FILTER_ID) == null) {
            // 如果没有配置鉴权，直接返回
            return;
        }
        String token = ctx.getRequest().getCookie(COOKIE_NAME).value();
        // 如果没有拿到 jwt，报错
        if (StringUtils.isBlank(token)) {
            throw new ResponseException(ResponseCode.UNAUTHORIZED);
        }
        try {
            // 解析
            long userId = pareseUserId(token);
            // 传递给下游，下游处理在 GateWayRequest 中的 build
            ctx.getRequest().setUserId(userId);
        } catch (Exception e) {
            throw new ResponseException(ResponseCode.UNAUTHORIZED);
        }
    }

    /**
     * 从 jwt 中解析用户 id
     * @param token
     * @return
     */
    private long pareseUserId(String token) {
        Jwt parse = Jwts.parser().setSigningKey(SECRET_KEY).parse(token);
        return Long.parseLong(((DefaultClaims) parse.getBody()).getSubject());
    }
}
