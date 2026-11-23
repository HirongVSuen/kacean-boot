package com.kacean.boot.loginaccount.port.web.controller;

import com.kacean.boot.loginaccount.port.infra.curd.LoginAccountCurd;
import com.kacean.boot.loginaccount.port.infra.entity.LoginAccountEntity;
import com.kacean.boot.loginaccount.port.web.dto.LoginAccountDTO;
import com.kacean.boot.common.web.controller.CurdController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录账号表控制器。
 *
 * @author kacean-agent
 */
@RestController
@RequestMapping("/login-account")
public class LoginAccountController extends CurdController<LoginAccountDTO, LoginAccountEntity, LoginAccountCurd> {
}
