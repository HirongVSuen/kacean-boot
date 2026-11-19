package com.kacean.power.runner.logincontext;

import com.kacean.power.logincontext.contract.LoginContext;
import com.kacean.power.logincontext.contract.LoginContextProvider;
import com.kacean.power.logincontext.contract.LoginContextUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 登录管理器测试，固定提供者填充顺序、完整性校验、Spring 装配与 Java SPI 发现行为。
 *
 * @author kacean-agent
 */
class LoginManagerTest {

    @Test
    void 多个提供者_依次填充同一个LoginContext() {
        List<LoginContext> receivedContexts = new ArrayList<>();
        LoginManager manager = new LoginManager(List.of(
                context -> {
                    receivedContexts.add(context);
                    context.setUserId("user-2");
                },
                context -> {
                    receivedContexts.add(context);
                    context.setUsername(context.getUserId() + "@power");
                }));

        LoginContext actual = manager.getRequired();

        assertAll(
                () -> assertEquals("user-2", actual.getUserId()),
                () -> assertEquals("user-2@power", actual.getUsername()),
                () -> assertSame(actual, receivedContexts.get(0)),
                () -> assertSame(actual, receivedContexts.get(1)));
    }

    @Test
    void 填充后缺少必需身份信息_抛出明确异常() {
        LoginManager withoutProviders = new LoginManager(List.of());
        LoginManager withIncompleteProvider = new LoginManager(List.of(
                context -> context.setUserId("user-1")));

        assertThrows(LoginContextUnavailableException.class, withoutProviders::getRequired);
        assertThrows(LoginContextUnavailableException.class, withIncompleteProvider::getRequired);
    }

    @Test
    void 每次获取上下文_均重新创建并填充() {
        LoginManager manager = new LoginManager(List.of(context -> {
            context.setUserId("user-1");
            context.setUsername("operator");
        }));

        LoginContext first = manager.getRequired();
        LoginContext second = manager.getRequired();

        assertAll(
                () -> assertEquals(first.getUserId(), second.getUserId()),
                () -> assertEquals(first.getUsername(), second.getUsername()),
                () -> assertNotSame(first, second));
    }

    @Test
    void 通过JavaSPI发现提供者_填充并返回统一登录上下文() {
        LoginContext context = LoginManager.fromServiceLoader().getRequired();

        assertAll(
                () -> assertEquals("spi-user", context.getUserId()),
                () -> assertEquals("spi-operator", context.getUsername()));
    }

    @Test
    void Spring容器没有提供者_管理器仍可装配() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(LoginManager.class);
            context.refresh();

            LoginManager manager = context.getBean(LoginManager.class);
            assertThrows(LoginContextUnavailableException.class, manager::getRequired);
        }
    }

    /** Java SPI 测试登录上下文提供者。 */
    public static class SpiLoginContextProvider implements LoginContextProvider {

        @Override
        public void fill(LoginContext context) {
            context.setUserId("spi-user");
            context.setUsername("spi-operator");
        }
    }
}
