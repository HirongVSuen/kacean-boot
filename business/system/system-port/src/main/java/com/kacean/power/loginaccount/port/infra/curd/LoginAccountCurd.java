package com.kacean.power.loginaccount.port.infra.curd;

import com.kacean.power.loginaccount.port.infra.dao.LoginAccountDao;
import com.kacean.power.loginaccount.port.infra.entity.LoginAccountEntity;
import com.kacean.power.common.infra.curd.BaseCurd;
import org.springframework.stereotype.Component;

/**
 * 登录账号表通用读写用例。
 *
 * @author kacean-agent
 */
@Component
public class LoginAccountCurd extends BaseCurd<LoginAccountEntity, LoginAccountDao> {
}
