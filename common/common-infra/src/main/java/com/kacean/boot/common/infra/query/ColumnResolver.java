package com.kacean.boot.common.infra.query;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.kacean.boot.common.infra.entity.BaseEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * PO 属性与数据库列解析器：优先读取 MyBatis-Plus 注解，未显式配置列名时按驼峰转下划线处理。
 * 仅允许解析真实持久化字段，客户端传入的未知字段和非持久化字段会被拒绝。
 *
 * @author kacean-agent
 */
public final class ColumnResolver {

    private static final ClassValue<Map<String, String>> COLUMNS = new ClassValue<>() {
        @Override
        protected Map<String, String> computeValue(Class<?> type) {
            Map<String, String> columns = new LinkedHashMap<>();
            for (Class<?> current = type; current != null && current != Object.class;
                    current = current.getSuperclass()) {
                for (Field field : current.getDeclaredFields()) {
                    if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    TableField tableField = field.getAnnotation(TableField.class);
                    if (tableField != null && !tableField.exist()) {
                        continue;
                    }
                    columns.putIfAbsent(field.getName(), resolveColumn(field, tableField));
                }
            }
            return Collections.unmodifiableMap(columns);
        }
    };

    private ColumnResolver() {
    }

    /**
     * 将 PO 的 Java 属性名解析为数据库列名。
     *
     * @param entityClass PO 类型，非空
     * @param field PO Java 属性名，非空白
     * @return 数据库列名
     */
    public static String resolve(Class<? extends BaseEntity> entityClass, String field) {
        Objects.requireNonNull(entityClass, "持久化类型 entityClass 不能为空");
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("PO 属性 field 不能为空或空白");
        }
        String column = COLUMNS.get(entityClass).get(field);
        if (column == null) {
            throw new IllegalArgumentException("字段不是 " + entityClass.getName() + " 的持久化属性: " + field);
        }
        return column;
    }

    private static String resolveColumn(Field field, TableField tableField) {
        if (tableField != null && StringUtils.isNotBlank(tableField.value())) {
            return tableField.value();
        }
        TableId tableId = field.getAnnotation(TableId.class);
        if (tableId != null && StringUtils.isNotBlank(tableId.value())) {
            return tableId.value();
        }
        return StringUtils.camelToUnderline(field.getName());
    }
}
