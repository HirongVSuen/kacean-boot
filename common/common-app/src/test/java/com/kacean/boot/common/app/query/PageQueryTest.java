package com.kacean.boot.common.app.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 分页查询测试，固定查询参数非空与页码、页大小的构造校验规则。
 *
 * @author kacean-agent
 */
class PageQueryTest {

    private static final TestQuery QUERY = new TestQuery();

    @Test
    void 查询参数为空_拒绝构造() {
        assertThrows(NullPointerException.class, () -> new PageQuery<>(null, 1, 10));
    }

    @Test
    void 页码小于1_拒绝构造() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new PageQuery<>(QUERY, 0, 10)),
                () -> assertThrows(IllegalArgumentException.class, () -> new PageQuery<>(QUERY, -1, 10)));
    }

    @Test
    void 页大小超出允许范围_拒绝构造() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new PageQuery<>(QUERY, 1, 0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new PageQuery<>(QUERY, 1, PageQuery.MAX_PAGE_SIZE + 1)));
    }

    @Test
    void 边界值合法_保留查询参数与分页参数() {
        PageQuery<TestQuery> query = new PageQuery<>(QUERY, 1, PageQuery.MAX_PAGE_SIZE);

        assertAll(
                () -> assertSame(QUERY, query.query()),
                () -> assertEquals(1, query.pageNum()),
                () -> assertEquals(PageQuery.MAX_PAGE_SIZE, query.pageSize()));
    }

    private static final class TestQuery extends Query {
    }
}
