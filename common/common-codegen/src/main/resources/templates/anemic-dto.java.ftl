package ${webPackage}.dto;

import com.kacean.boot.common.web.response.BaseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
<#if dtoJsonFormatRequired>
import com.fasterxml.jackson.annotation.JsonFormat;
</#if>
import lombok.Getter;
import lombok.Setter;
<#list modelImportPackages as packageName>
import ${packageName};
</#list>

/**
 * ${table.comment!"业务数据"}Web 传输对象。
 *
 * @author ${author}
 */
@Getter
@Setter
public class ${businessName}DTO extends BaseDTO {

<#include "model-members.java.ftl">
<@fieldDeclarations table.commonFields true/>
<@fieldDeclarations table.fields true/>
    @Override
    public String toString() {
        return "${businessName}DTO{" +
<#assign first = true>
<#list table.commonFields + table.fields as field>
<#if field.propertyName != "deleted">
<#if first>
            "${field.propertyName} = " + ${field.propertyName} +
<#assign first = false>
<#else>
            ", ${field.propertyName} = " + ${field.propertyName} +
</#if>
</#if>
</#list>
            "}";
    }
}
