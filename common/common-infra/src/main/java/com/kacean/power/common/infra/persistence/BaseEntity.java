package com.kacean.power.common.infra.persistence;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * MyBatis-Plus 持久化对象基类：统一主键、外部编号、审计字段、逻辑删除与乐观锁映射。
 * <p>本基类仅供基础设施层持久化对象使用，与 {@code common-app} 中不绑定持久化框架的领域基类相互独立。
 * 主键不在此处绑定 {@code @TableId} 策略，由全局 {@code GlobalConfig.DbConfig} 决定；
 * 个别实体需要覆盖时，可在子类重新声明 {@code id} 并添加 {@code @TableId}。</p>
 *
 * @author kacean-agent
 */
@Data
public abstract class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID，由 MyBatis-Plus 按字段名识别，默认使用全局主键策略。
     */
    protected Long id;

    /**
     * 外部编号：跨系统关联键，由运行模块配置的 MetaObjectHandler 在插入时生成，长度不超过 40 字符。
     */
    @TableField(value = "external_no", fill = FieldFill.INSERT)
    protected String externalNo;

    /**
     * 逻辑删除标记：0 表示未删除，1 表示已删除。
     */
    @TableLogic
    @TableField(value = "deleted", fill = FieldFill.INSERT)
    protected Integer deleted = 0;

    /**
     * 创建时间。
     */
    @TableField(value = "create_at", fill = FieldFill.INSERT)
    protected LocalDateTime createAt;

    /**
     * 创建人。
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    protected String createBy;

    /**
     * 修改时间。
     */
    @TableField(value = "modify_at", fill = FieldFill.INSERT_UPDATE)
    protected LocalDateTime modifyAt;

    /**
     * 修改人。
     */
    @TableField(value = "modify_by", fill = FieldFill.INSERT_UPDATE)
    protected String modifyBy;

    /**
     * 乐观锁版本号。
     */
    @Version
    @TableField(value = "version", fill = FieldFill.INSERT)
    protected Long version = 0L;
}
