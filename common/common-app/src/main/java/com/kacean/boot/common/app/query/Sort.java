package com.kacean.boot.common.app.query;

import java.util.Objects;

/**
 * 排序条件：字段名使用 PO 的 Java 持久化属性，不得直接拼接进 SQL，
 * 由基础设施层解析为真实持久化字段对应的数据库列，防止注入与越权排序。
 *
 * @author kacean-agent
 * @param field PO 的 Java 持久化属性名，非空白
 * @param direction 排序方向，非空
 */
public record Sort(String field, SortDirection direction) {

    public Sort {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("排序字段 field 不能为空或空白");
        }
        Objects.requireNonNull(direction, "排序方向 direction 不能为空");
    }
}
