package ${webPackage}.controller;

import ${appPackage}.domain.${businessName};
import ${infrastructurePackage}.curd.${businessName}Curd;
import ${infrastructurePackage}.entity.${table.entityName};
import ${webPackage}.dto.${businessName}DTO;
import com.kacean.power.common.web.controller.DddController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ${table.comment!"业务数据"}DDD 控制器。
 *
 * @author ${author}
 */
@RestController
@RequestMapping("${requestPath}")
public class ${businessName}Controller
        extends DddController<${businessName}DTO, ${businessName}, ${table.entityName}, ${businessName}Curd> {
}
