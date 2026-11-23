package com.kacean.boot.logincontext.contract;

/**
 * 登录上下文提供者 SPI，负责将当前认证方式或执行环境中的身份信息填充到统一登录上下文。
 * <p>实现可注册为 Spring Bean，也可按 Java SPI 约定在
 * {@code META-INF/services/com.kacean.boot.logincontext.contract.LoginContextProvider} 中注册。</p>
 *
 * @author kacean-agent
 */
public interface LoginContextProvider {

    /**
     * 填充本提供者负责的登录上下文内容；没有可填充内容时保持原上下文不变。
     * <p>同一实例会按提供者顺序依次传递，后续提供者可读取并补充已有内容。</p>
     *
     * @param context 登录管理器创建的统一登录上下文，不会为 {@code null}
     */
    void fill(LoginContext context);
}
