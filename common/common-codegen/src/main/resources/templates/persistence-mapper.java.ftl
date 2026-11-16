package ${package.Mapper};

<#list importMapperFrameworkPackages as pkg>
import ${pkg};
</#list>
<#list importMapperJavaPackages as pkg>
import ${pkg};
</#list>

/**
 * ${table.comment!"数据库表"} MyBatis-Plus 数据访问接口。
 *
 * @author ${author}
 */
<#if mapperAnnotationClass??>
@${mapperAnnotationClass.simpleName}
</#if>
public interface ${table.mapperName} extends ${superMapperClass}<${entity}> {

<#list mapperMethodList as m>
    /**
     * 按数据库索引生成的查询方法。
     *
    <#list m.tableFieldList as f>
     * @param ${f.propertyName} ${f.comment}
    </#list>
     * @return 查询结果
     */
    ${m.method}
</#list>
}
