package com.kacean.power.common.app.query;

import java.util.List;
import java.util.Objects;

/**
 * 分页结果：仅表达响应，总页数由记录总数与页大小派生计算，不由调用方传入。
 *
 * @author kacean-agent
 * @param records 当前页记录列表，非空，构造时做防御性复制
 * @param pageNum 页码，从 1 开始
 * @param pageSize 每页记录数，大于等于 1
 * @param total 符合查询条件的记录总数，大于等于 0
 */
public record PageResult<T>(List<T> records, int pageNum, int pageSize, long total) {

    public PageResult {
        Objects.requireNonNull(records, "记录列表 records 不能为空");
        records = List.copyOf(records);
        if (pageNum < 1) {
            throw new IllegalArgumentException("pageNum 必须大于等于 1，当前值: " + pageNum);
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize 必须大于等于 1，当前值: " + pageSize);
        }
        if (total < 0) {
            throw new IllegalArgumentException("total 必须大于等于 0，当前值: " + total);
        }
    }

    /**
     * 总页数按记录总数与页大小向上取整，无记录时为 0。
     *
     * @return 总页数
     */
    public long totalPages() {
        return total == 0 ? 0 : 1 + (total - 1) / pageSize;
    }
}
