package com.kacean.boot.datadict.port.infra.dao;

import com.kacean.boot.datadict.port.infra.entity.DataDictEntity;
import com.kacean.boot.datadict.port.infra.mapper.DataDictMapper;
import com.kacean.boot.common.infra.dao.BaseDaoImpl;
import org.springframework.stereotype.Repository;

/**
 * 数据字典表DAO 的 MyBatis-Plus 实现。
 *
 * @author kacean-agent
 */
@Repository
public class DataDictDaoImpl extends BaseDaoImpl<DataDictMapper, DataDictEntity>
        implements DataDictDao {
}
