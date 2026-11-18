package com.kacean.power.common.app.query;

import java.util.Objects;

/**
 * 排序条件：字段名仅为逻辑标识，不得直接拼接进 SQL，
 * 由业务仓储按允许字段白名单转换为实际排序列，防止注入与越权排序。
 *
 * @author kacean-agent
 * @param field 排序字段的逻辑名，非空白
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
