package com.kacean.power.common.app.query;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 查询参数契约测试，固定默认查询属性与业务 record 实现契约的方式。
 *
 * @author kacean-agent
 */
class QueryTest {

    @Test
    void 未覆盖通用查询属性_使用默认空值() {
        Query query = new EmptyQuery();

        assertAll(
                () -> assertNull(query.keyword()),
                () -> assertTrue(query.sorts().isEmpty()));
    }

    @Test
    void 业务查询参数实现契约_排序列表使用不可变快照() {
        List<Sort> sorts = new ArrayList<>(List.of(new Sort("name", SortDirection.ASC)));
        TicketQueryParam query = new TicketQueryParam("EXT-1", "keyword", sorts);

        sorts.add(new Sort("code", SortDirection.DESC));

        assertAll(
                () -> assertEquals("EXT-1", query.externalNo()),
                () -> assertEquals("keyword", query.keyword()),
                () -> assertEquals(1, query.sorts().size()),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> query.sorts().add(new Sort("code", SortDirection.DESC))));
    }

    private record EmptyQuery() implements Query {
    }

    private record TicketQueryParam(String externalNo, String keyword, List<Sort> sorts) implements Query {

        private TicketQueryParam {
            sorts = sorts == null ? List.of() : List.copyOf(sorts);
        }
    }
}
