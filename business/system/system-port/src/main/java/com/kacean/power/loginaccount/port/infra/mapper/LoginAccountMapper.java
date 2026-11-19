package com.kacean.power.loginaccount.port.infra.mapper;

import com.kacean.power.common.infra.mapper.BaseMapper;
import com.kacean.power.loginaccount.port.infra.entity.LoginAccountEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录账号表 MyBatis-Plus 数据访问接口。
 *
 * @author kacean-agent
 */
@Mapper
public interface LoginAccountMapper extends BaseMapper<LoginAccountEntity> {

}
