package ${appPackage}.domain;

import com.kacean.boot.common.app.domain.BaseDomain;
import lombok.Getter;
import lombok.Setter;
<#list modelImportPackages as packageName>
import ${packageName};
</#list>

/**
 * ${table.comment!"业务数据"}领域模型。
 *
 * @author ${author}
 */
@Getter
@Setter
public class ${businessName} implements BaseDomain {

<#include "model-members.java.ftl">
<@fieldDeclarations table.commonFields/>
<@fieldDeclarations table.fields/>
}
