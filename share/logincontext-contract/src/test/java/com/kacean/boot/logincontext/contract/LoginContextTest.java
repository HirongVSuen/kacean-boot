package com.kacean.boot.logincontext.contract;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 登录上下文测试，固定统一响应对象的可填充性与实例隔离规则。
 *
 * @author kacean-agent
 */
class LoginContextTest {

    @Test
    void 新建上下文_可由提供者逐字段填充() {
        LoginContext context = new LoginContext();

        assertAll(
                () -> assertNull(context.getUserId()),
                () -> assertNull(context.getUsername()));

        context.setUserId("user-1");
        context.setUsername("operator");

        assertAll(
                () -> assertEquals("user-1", context.getUserId()),
                () -> assertEquals("operator", context.getUsername()));
    }

    @Test
    void 分别创建上下文_可变状态互不影响() {
        LoginContext first = new LoginContext();
        LoginContext second = new LoginContext();

        first.setUserId("user-1");
        first.setUsername("operator");

        assertAll(
                () -> assertNull(second.getUserId()),
                () -> assertNull(second.getUsername()));
    }
}
