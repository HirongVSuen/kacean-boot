package com.kacean.power.logincontext.contract;

import lombok.Data;

/**
 * 登录上下文统一响应对象，由登录管理器创建并交给各个登录上下文提供者逐步填充。
 * <p>每次获取均创建独立实例，避免不同请求或执行环境之间共享可变状态。</p>
 *
 * @author kacean-agent
 */
@Data
public class LoginContext {

    /** 登录用户标识。 */
    private String userId;

    /** 登录用户名。 */
    private String username;
}
