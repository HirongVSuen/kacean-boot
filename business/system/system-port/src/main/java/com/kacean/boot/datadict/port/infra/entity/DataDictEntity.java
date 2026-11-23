package com.kacean.boot.datadict.port.infra.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kacean.boot.common.infra.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;


/**
 * 数据字典表持久化对象，仅用于 MyBatis-Plus 数据映射，不承载领域行为。
 *
 * @author kacean-agent
 */
@TableName("t_data_dict")
@Getter
@Setter
public class DataDictEntity extends BaseEntity {

    /**
     * 字典名
     */
    @TableField("dict_name")
    private String dictName;

    /**
     * 是否启用：0-禁用，1-启用
     */
    @TableField("is_enabled")
    private Byte isEnabled;

    @Override
    public String toString() {
        return "DataDictEntity{" +
            "dictName = " + dictName +
            ", isEnabled = " + isEnabled +
            "}";
    }
}
