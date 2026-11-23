package com.kacean.boot.datadict.port.infra.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kacean.boot.common.infra.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;


/**
 * 数据字典值表持久化对象，仅用于 MyBatis-Plus 数据映射，不承载领域行为。
 *
 * @author kacean-agent
 */
@TableName("t_data_dict_value")
@Getter
@Setter
public class DataDictValueEntity extends BaseEntity {

    /**
     * 数据字典主表ID
     */
    @TableField("dict_id")
    private Long dictId;

    /**
     * 字典编码
     */
    @TableField("dict_code")
    private String dictCode;

    /**
     * 字典中文值
     */
    @TableField("dict_value")
    private String dictValue;

    @Override
    public String toString() {
        return "DataDictValueEntity{" +
            "dictId = " + dictId +
            ", dictCode = " + dictCode +
            ", dictValue = " + dictValue +
            "}";
    }
}
