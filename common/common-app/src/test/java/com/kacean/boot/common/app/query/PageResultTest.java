package com.kacean.boot.common.app.query;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 分页结果测试，固定总页数派生计算与记录列表防御性复制契约。
 *
 * @author kacean-agent
 */
class PageResultTest {

    @Test
    void 总页数按记录总数与页大小向上取整() {
        assertAll(
                () -> assertEquals(0, new PageResult<>(List.of(), 1, 10, 0).totalPages()),
                () -> assertEquals(1, new PageResult<>(List.of("a"), 1, 10, 1).totalPages()),
                () -> assertEquals(2, new PageResult<>(List.of("a"), 1, 10, 20).totalPages()),
                () -> assertEquals(3, new PageResult<>(List.of("a"), 1, 10, 21).totalPages()));
    }

    @Test
    void 记录总数为Long最大值_总页数计算不溢出() {
        assertAll(
                () -> assertEquals(922337203685477581L,
                        new PageResult<>(List.of("a"), 1, 10, Long.MAX_VALUE).totalPages()),
                () -> assertEquals(Long.MAX_VALUE,
                        new PageResult<>(List.of("a"), 1, 1, Long.MAX_VALUE).totalPages()));
    }

    @Test
    void 记录列表防御性复制_外部修改不影响分页结果() {
        List<String> records = new ArrayList<>(List.of("a", "b"));
        PageResult<String> result = new PageResult<>(records, 1, 10, 2);

        records.add("c");

        assertAll(
                () -> assertEquals(List.of("a", "b"), result.records()),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> result.records().add("d")));
    }

    @Test
    void 非法分页参数_拒绝构造() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new PageResult<>(List.of(), 0, 10, 0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new PageResult<>(List.of(), 1, 0, 0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new PageResult<>(List.of(), 1, 10, -1)));
    }
}
