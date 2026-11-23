package com.kacean.boot.common.app.query;

import java.util.Objects;

/**
 * 分页查询：组合业务查询参数、页码与页大小，构造时完成分页参数合法性校验。
 *
 * @author kacean-agent
 * @param query 业务查询参数，非空
 * @param pageNum 页码，从 1 开始
 * @param pageSize 每页记录数，取值范围为 1 到 {@link #MAX_PAGE_SIZE}
 * @param <Q> 业务查询参数类型
 */
public record PageQuery<Q extends Query>(Q query, int pageNum, int pageSize) {

    /**
     * 每页记录数上限，防止调用方单次拉取过多数据
     */
    public static final int MAX_PAGE_SIZE = 200;

    public PageQuery {
        Objects.requireNonNull(query, "查询条件 query 不能为空");
        if (pageNum < 1) {
            throw new IllegalArgumentException("pageNum 必须大于等于 1，当前值: " + pageNum);
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "pageSize 必须在 1 到 " + MAX_PAGE_SIZE + " 之间，当前值: " + pageSize);
        }
    }
}
