package ${package.Entity};

<#list importEntityFrameworkPackages as pkg>
import ${pkg};
</#list>
import lombok.Getter;
import lombok.Setter;

<#list importEntityJavaPackages as pkg>
import ${pkg};
</#list>

/**
 * ${table.comment!"数据库表"}持久化对象，仅用于 MyBatis-Plus 数据映射，不承载领域行为。
 *
 * @author ${author}
 */
<#list entityClassAnnotations as an>
${an.displayName}
</#list>
@Getter
@Setter
<#if superEntityClass??>
public class ${entity} extends ${superEntityClass}<#if activeRecord><${entity}></#if> {
<#elseif activeRecord>
public class ${entity} extends Model<${entity}> {
<#else>
public class ${entity} {
</#if>
<#list table.fields as field>

    <#if field.comment!?length gt 0>
    /**
     * ${field.comment}
     */
    </#if>
    <#list field.annotationAttributesList as an>
    ${an.displayName}
    </#list>
    private ${field.propertyType} ${field.propertyName};
</#list>
<#if entityToString>

    @Override
    public String toString() {
        return "${entity}{" +
    <#list table.fields as field>
        <#if field_index==0>
            "${field.propertyName} = " + ${field.propertyName} +
        <#else>
            ", ${field.propertyName} = " + ${field.propertyName} +
        </#if>
    </#list>
            "}";
    }
</#if>
}
