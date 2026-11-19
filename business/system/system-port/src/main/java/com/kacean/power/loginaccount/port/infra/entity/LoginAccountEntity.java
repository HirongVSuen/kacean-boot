package com.kacean.power.loginaccount.port.infra.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kacean.power.common.infra.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;


/**
 * 登录账号表持久化对象，仅用于 MyBatis-Plus 数据映射，不承载领域行为。
 *
 * @author kacean-agent
 */
@TableName("t_login_account")
@Getter
@Setter
public class LoginAccountEntity extends BaseEntity {

    /**
     * 登录账号
     */
    @TableField("login_account")
    private String loginAccount;

    /**
     * 登录密码哈希，禁止存储明文
     */
    @TableField("login_password")
    private String loginPassword;

    /**
     * 账号状态：0-禁用，1-启用
     */
    @TableField("account_status")
    private Byte accountStatus;

    @Override
    public String toString() {
        return "LoginAccountEntity{" +
            "loginAccount = " + loginAccount +
            ", loginPassword = " + loginPassword +
            ", accountStatus = " + accountStatus +
            "}";
    }
}
