package com.kacean.boot.logincontext.contract;

/**
 * 登录上下文管理契约，屏蔽不同认证方式与登录上下文提供者的差异。
 *
 * @author kacean-agent
 */
public interface LoginContextManager {

    /**
     * 创建统一登录上下文，并由所有登录上下文提供者依次填充。
     *
     * @return 填充完整的登录上下文
     * @throws LoginContextUnavailableException 填充结束后仍缺少必需身份信息时抛出
     */
    LoginContext getRequired();
}
