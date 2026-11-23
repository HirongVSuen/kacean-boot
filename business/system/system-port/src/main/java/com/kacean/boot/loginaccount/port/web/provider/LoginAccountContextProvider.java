package com.kacean.boot.loginaccount.port.web.provider;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kacean.boot.loginaccount.port.infra.dao.LoginAccountDao;
import com.kacean.boot.loginaccount.port.infra.entity.LoginAccountEntity;
import com.kacean.boot.logincontext.contract.LoginContext;
import com.kacean.boot.logincontext.contract.LoginContextProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 登录账号上下文提供者：loginaccount 子域作为身份来源，向统一登录上下文填充当前登录账号。
 * <p>尚未接入认证设施，当前从请求头 {@code X-Login-Account} 读取登录账号并查询启用账号，
 * 作为开发期模拟登录的占位实现；后续接入认证时仅需替换账号名来源，填充逻辑保持不变。
 * 非 Web 请求、请求头缺失或启用账号未命中时保持上下文不变，由登录管理器统一校验完整性。</p>
 *
 * @author kacean-agent
 */
@Component
public class LoginAccountContextProvider implements LoginContextProvider {

    /** 开发期模拟登录请求头，值为登录账号。 */
    public static final String LOGIN_ACCOUNT_HEADER = "X-Login-Account";

    /** 账号状态：启用。 */
    private static final byte ACCOUNT_STATUS_ENABLED = 1;

    private final LoginAccountDao loginAccountDao;

    /**
     * 使用登录账号 DAO 创建提供者。
     *
     * @param loginAccountDao 登录账号表 DAO
     */
    public LoginAccountContextProvider(LoginAccountDao loginAccountDao) {
        this.loginAccountDao = loginAccountDao;
    }

    @Override
    public void fill(LoginContext context) {
        String loginAccount = currentLoginAccount();
        if (loginAccount == null || loginAccount.isBlank()) {
            return;
        }
        LoginAccountEntity entity = loginAccountDao.getOneOpt(new LambdaQueryWrapper<LoginAccountEntity>()
                        .eq(LoginAccountEntity::getLoginAccount, loginAccount)
                        .eq(LoginAccountEntity::getAccountStatus, ACCOUNT_STATUS_ENABLED), false)
                .orElse(null);
        if (entity == null) {
            return;
        }
        context.setUserId(String.valueOf(entity.getId()));
        context.setUsername(entity.getLoginAccount());
    }

    /**
     * 从当前 Web 请求头读取登录账号，非 Web 请求环境返回 {@code null}。
     *
     * @return 登录账号，无法获取时为 {@code null}
     */
    private static String currentLoginAccount() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getHeader(LOGIN_ACCOUNT_HEADER);
        }
        return null;
    }
}
