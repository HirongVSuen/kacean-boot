package ${webPackage}.assembler;

import ${appPackage}.domain.${businessName};
import ${infrastructurePackage}.entity.${table.entityName};
import ${webPackage}.dto.${businessName}DTO;
import com.kacean.boot.common.web.assembler.DddAssembler;
import org.mapstruct.Mapper;

/**
 * ${table.comment!"业务数据"}DTO、领域模型与持久化对象转换器。
 *
 * @author ${author}
 */
@Mapper(componentModel = "spring")
public interface ${businessName}Assembler extends DddAssembler<${businessName}DTO, ${businessName}, ${table.entityName}> {
}
