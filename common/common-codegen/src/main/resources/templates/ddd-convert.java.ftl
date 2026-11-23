package ${infrastructurePackage}.convert;

import ${appPackage}.domain.${businessName};
import ${infrastructurePackage}.entity.${table.entityName};
import com.kacean.boot.common.infra.repo.BaseConvert;
import org.mapstruct.Mapper;

/**
 * ${table.comment!"业务数据"}领域模型与持久化对象转换器。
 *
 * @author ${author}
 */
@Mapper(componentModel = "spring")
public interface ${businessName}Convert extends BaseConvert<${businessName}, ${table.entityName}> {
}
