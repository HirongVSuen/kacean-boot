package ${webPackage}.assembler;

import ${infrastructurePackage}.entity.${table.entityName};
import ${webPackage}.dto.${businessName}DTO;
import com.kacean.boot.common.web.assembler.CurdAssembler;
import org.mapstruct.Mapper;

/**
 * ${table.comment!"业务数据"}DTO 与持久化对象转换器。
 *
 * @author ${author}
 */
@Mapper(componentModel = "spring")
public interface ${businessName}Assembler extends CurdAssembler<${businessName}DTO, ${table.entityName}> {
}
