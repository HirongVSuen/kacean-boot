package com.kacean.power.loginaccount.port.web.dto;

import com.kacean.power.common.web.response.BaseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 登录账号表Web 传输对象。
 *
 * @author kacean-agent
 */
@Getter
@Setter
public class LoginAccountDTO extends BaseDTO {

    /**
     * 主键ID，外部传入
     */
    @Schema(description = "主键ID，外部传入")
    private Long id;

    /**
     * 外部编号
     */
    @Schema(description = "外部编号")
    private String externalNo;

    /**
     * 创建人
     */
    @Schema(description = "创建人")
    private String createBy;

    /**
     * 修改人
     */
    @Schema(description = "修改人")
    private String modifyBy;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createAt;

    /**
     * 修改时间
     */
    @Schema(description = "修改时间", example = "2026-01-01 12:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime modifyAt;

    /**
     * 乐观锁版本号
     */
    @Schema(description = "乐观锁版本号")
    private Long version;

    /**
     * 登录账号
     */
    @Schema(description = "登录账号")
    private String loginAccount;

    /**
     * 登录密码哈希，禁止存储明文
     */
    @Schema(description = "登录密码哈希，禁止存储明文")
    private String loginPassword;

    /**
     * 账号状态：0-禁用，1-启用
     */
    @Schema(description = "账号状态：0-禁用，1-启用")
    private Byte accountStatus;

    @Override
    public String toString() {
        return "LoginAccountDTO{" +
            "id = " + id +
            ", externalNo = " + externalNo +
            ", createBy = " + createBy +
            ", modifyBy = " + modifyBy +
            ", createAt = " + createAt +
            ", modifyAt = " + modifyAt +
            ", version = " + version +
            ", loginAccount = " + loginAccount +
            ", loginPassword = " + loginPassword +
            ", accountStatus = " + accountStatus +
            "}";
    }
}
