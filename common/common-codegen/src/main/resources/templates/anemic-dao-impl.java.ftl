package ${infrastructurePackage}.dao;

import ${infrastructurePackage}.entity.${table.entityName};
import ${infrastructurePackage}.mapper.${table.mapperName};
import com.kacean.power.common.infra.dao.BaseDaoImpl;
import org.springframework.stereotype.Repository;

/**
 * ${table.comment!"业务数据"}DAO 的 MyBatis-Plus 实现。
 *
 * @author ${author}
 */
@Repository
public class ${businessName}DaoImpl extends BaseDaoImpl<${table.mapperName}, ${table.entityName}>
        implements ${businessName}Dao {
}
