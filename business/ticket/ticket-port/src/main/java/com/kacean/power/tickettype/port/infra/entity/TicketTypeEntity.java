package com.kacean.power.tickettype.port.infra.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kacean.power.common.infra.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;


/**
 * 票种类型表持久化对象，仅用于 MyBatis-Plus 数据映射，不承载领域行为。
 *
 * @author kacean-agent
 */
@TableName("t_ticket_type")
@Getter
@Setter
public class TicketTypeEntity extends BaseEntity {

    /**
     * 票种类型名称
     */
    @TableField("ticket_type_name")
    private String ticketTypeName;

    /**
     * 类目编号
     */
    @TableField("catalog_no")
    private String catalogNo;

    @Override
    public String toString() {
        return "TicketTypeEntity{" +
            "ticketTypeName = " + ticketTypeName +
            ", catalogNo = " + catalogNo +
            "}";
    }
}
