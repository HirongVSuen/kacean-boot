package com.kacean.power.logincontext.contract;

/**
 * 登录上下文不可用异常，表示提供者完成填充后仍缺少当前登录用户的必需身份信息。
 *
 * @author kacean-agent
 */
public class LoginContextUnavailableException extends RuntimeException {

    public LoginContextUnavailableException() {
        super("当前不存在可用的登录上下文");
    }
}
