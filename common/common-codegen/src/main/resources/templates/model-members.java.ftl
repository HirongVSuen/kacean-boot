<#-- DTO 与领域对象共用字段声明：逻辑删除字段 deleted 由持久层管理，不对外生成；webAnnotations=true 时（仅 DTO）输出 @Schema 文档注解，并为 LocalDateTime 增加 @JsonFormat 与日期示例 -->
<#macro fieldDeclarations fields webAnnotations=false>
<#list fields as field>
<#if field.propertyName != "deleted">
<#assign fieldComment = field.comment?has_content?then(field.comment, "业务字段")>
    /**
     * ${fieldComment}
     */
<#if webAnnotations>
    @Schema(description = "${fieldComment?j_string}"<#if field.propertyType == "LocalDateTime">, example = "2026-01-01 12:00:00"</#if>)
</#if>
<#if webAnnotations && field.propertyType == "LocalDateTime">
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
</#if>
    private ${field.propertyType} ${field.propertyName};

</#if>
</#list>
</#macro>
