package ${infrastructurePackage}.repo;

import ${appPackage}.domain.${businessName};
import ${appPackage}.repo.${businessName}Repository;
import ${infrastructurePackage}.dao.${businessName}Dao;
import ${infrastructurePackage}.entity.${table.entityName};
import com.kacean.power.common.infra.repo.BaseRepositoryImpl;
import org.springframework.stereotype.Repository;

/**
 * ${table.comment!"业务数据"}仓储的 MyBatis-Plus 适配实现。
 *
 * @author ${author}
 */
@Repository
public class ${businessName}RepositoryImpl
        extends BaseRepositoryImpl<${businessName}, ${table.entityName}, ${businessName}Dao>
        implements ${businessName}Repository {
}
