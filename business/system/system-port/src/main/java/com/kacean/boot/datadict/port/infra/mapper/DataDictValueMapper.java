package com.kacean.boot.datadict.port.infra.mapper;

import com.kacean.boot.common.infra.mapper.BaseMapper;
import com.kacean.boot.datadict.port.infra.entity.DataDictValueEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据字典值表 MyBatis-Plus 数据访问接口。
 *
 * @author kacean-agent
 */
@Mapper
public interface DataDictValueMapper extends BaseMapper<DataDictValueEntity> {

}
