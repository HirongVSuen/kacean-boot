package com.kacean.power.loginaccount.port.infra.dao;

import com.kacean.power.loginaccount.port.infra.entity.LoginAccountEntity;
import com.kacean.power.loginaccount.port.infra.mapper.LoginAccountMapper;
import com.kacean.power.common.infra.dao.BaseDaoImpl;
import org.springframework.stereotype.Repository;

/**
 * 登录账号表DAO 的 MyBatis-Plus 实现。
 *
 * @author kacean-agent
 */
@Repository
public class LoginAccountDaoImpl extends BaseDaoImpl<LoginAccountMapper, LoginAccountEntity>
        implements LoginAccountDao {
}
