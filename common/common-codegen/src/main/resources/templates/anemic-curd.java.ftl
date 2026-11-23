package ${infrastructurePackage}.curd;

import ${infrastructurePackage}.dao.${businessName}Dao;
import ${infrastructurePackage}.entity.${table.entityName};
import com.kacean.boot.common.infra.curd.BaseCurd;
import org.springframework.stereotype.Component;

/**
 * ${table.comment!"业务数据"}通用读写用例。
 *
 * @author ${author}
 */
@Component
public class ${businessName}Curd extends BaseCurd<${table.entityName}, ${businessName}Dao> {
}
