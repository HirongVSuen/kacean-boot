package com.kacean.power.loginaccount.port.web.provider;

import com.kacean.power.loginaccount.port.infra.dao.LoginAccountDao;
import com.kacean.power.loginaccount.port.infra.entity.LoginAccountEntity;
import com.kacean.power.logincontext.contract.LoginContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Proxy;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 登录账号上下文提供者测试，固定开发期请求头模拟登录的填充与各种不填充场景。
 * <p>不引入额外测试框架：DAO 与 HTTP 请求均以 JDK 动态代理打桩，
 * 未预期的方法调用会抛出异常，从而保证填充逻辑只走声明的交互路径。</p>
 *
 * @author kacean-agent
 */
class LoginAccountContextProviderTest {

    @AfterEach
    void 清理线程绑定的请求上下文() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void 请求头指定启用登录账号_填充账号主键与登录名() {
        LoginAccountEntity entity = new LoginAccountEntity();
        entity.setId(7L);
        entity.setLoginAccount("admin");
        LoginAccountContextProvider provider = new LoginAccountContextProvider(daoReturning(entity));
        bindRequest("admin");
        LoginContext context = new LoginContext();

        provider.fill(context);

        assertAll(
                () -> assertEquals("7", context.getUserId()),
                () -> assertEquals("admin", context.getUsername()));
    }

    @Test
    void 非Web请求环境_保持上下文不变() {
        LoginAccountContextProvider provider = new LoginAccountContextProvider(daoReturning(null));
        LoginContext context = new LoginContext();

        provider.fill(context);

        assertAll(
                () -> assertNull(context.getUserId()),
                () -> assertNull(context.getUsername()));
    }

    @Test
    void 请求头缺失或账号未命中_保持上下文不变() {
        LoginAccountContextProvider provider = new LoginAccountContextProvider(daoReturning(null));
        LoginContext context = new LoginContext();

        bindRequest(null);
        provider.fill(context);

        bindRequest("ghost");
        provider.fill(context);

        assertAll(
                () -> assertNull(context.getUserId()),
                () -> assertNull(context.getUsername()));
    }

    /** 打桩登录账号 DAO：仅响应 getOneOpt，返回固定账号，其余调用视为未预期交互。 */
    private static LoginAccountDao daoReturning(LoginAccountEntity entity) {
        return (LoginAccountDao) Proxy.newProxyInstance(
                LoginAccountDao.class.getClassLoader(),
                new Class<?>[]{LoginAccountDao.class},
                (proxy, method, args) -> {
                    if ("getOneOpt".equals(method.getName())) {
                        return Optional.ofNullable(entity);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    /** 绑定模拟 Web 请求：仅响应 getHeader，返回指定登录账号请求头。 */
    private static void bindRequest(String loginAccount) {
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    if ("getHeader".equals(method.getName())) {
                        return LoginAccountContextProvider.LOGIN_ACCOUNT_HEADER.equals(args[0])
                                ? loginAccount : null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
