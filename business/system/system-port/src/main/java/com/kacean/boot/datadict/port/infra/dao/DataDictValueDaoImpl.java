package com.kacean.boot.datadict.port.infra.dao;

import com.kacean.boot.datadict.port.infra.entity.DataDictValueEntity;
import com.kacean.boot.datadict.port.infra.mapper.DataDictValueMapper;
import com.kacean.boot.common.infra.dao.BaseDaoImpl;
import org.springframework.stereotype.Repository;

/**
 * 数据字典值表DAO 的 MyBatis-Plus 实现。
 *
 * @author kacean-agent
 */
@Repository
public class DataDictValueDaoImpl extends BaseDaoImpl<DataDictValueMapper, DataDictValueEntity>
        implements DataDictValueDao {
}
