package com.kacean.boot.datadict.port.infra.mapper;

import com.kacean.boot.common.infra.mapper.BaseMapper;
import com.kacean.boot.datadict.port.infra.entity.DataDictEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据字典表 MyBatis-Plus 数据访问接口。
 *
 * @author kacean-agent
 */
@Mapper
public interface DataDictMapper extends BaseMapper<DataDictEntity> {

}
