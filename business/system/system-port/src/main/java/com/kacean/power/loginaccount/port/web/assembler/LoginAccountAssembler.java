package com.kacean.power.loginaccount.port.web.assembler;

import com.kacean.power.loginaccount.port.infra.entity.LoginAccountEntity;
import com.kacean.power.loginaccount.port.web.dto.LoginAccountDTO;
import com.kacean.power.common.web.assembler.CurdAssembler;
import org.mapstruct.Mapper;

/**
 * 登录账号表DTO 与持久化对象转换器。
 *
 * @author kacean-agent
 */
@Mapper(componentModel = "spring")
public interface LoginAccountAssembler extends CurdAssembler<LoginAccountDTO, LoginAccountEntity> {
}
