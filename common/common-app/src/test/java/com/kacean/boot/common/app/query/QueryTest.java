package com.kacean.boot.common.app.query;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 查询参数基类测试，固定 QP 继承方式、公共属性默认值与排序列表不可变快照语义。
 *
 * @author kacean-agent
 */
class QueryTest {

    @Test
    void 未设置通用查询属性_使用默认空值() {
        Query query = new EmptyQuery();

        assertAll(
                () -> assertNull(query.keyword()),
                () -> assertTrue(query.sorts().isEmpty()));
    }

    @Test
    void 业务查询参数继承基类_公共属性与业务字段均可访问() {
        List<Sort> sorts = new ArrayList<>(List.of(new Sort("name", SortDirection.ASC)));
        TicketQueryParam query = new TicketQueryParam("EXT-1", "keyword", sorts);

        sorts.add(new Sort("code", SortDirection.DESC));

        assertAll(
                () -> assertEquals("EXT-1", query.getExternalNo()),
                () -> assertEquals("keyword", query.getKeyword()),
                () -> assertEquals("keyword", query.keyword()),
                () -> assertEquals(1, query.sorts().size()),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> query.getSorts().add(new Sort("code", SortDirection.DESC))));
    }

    @Test
    void 设置排序列表_保存不可变快照并将null归一为空列表() {
        TicketQueryParam query = new TicketQueryParam("EXT-1", null, null);
        List<Sort> sorts = new ArrayList<>(List.of(new Sort("name", SortDirection.ASC)));

        query.setSorts(sorts);
        sorts.clear();

        assertEquals(1, query.sorts().size());
        query.setSorts(null);
        assertTrue(query.sorts().isEmpty());
    }

    private static final class EmptyQuery extends Query {
    }

    private static final class TicketQueryParam extends Query {

        private final String externalNo;

        private TicketQueryParam(String externalNo, String keyword, List<Sort> sorts) {
            super(keyword, sorts);
            this.externalNo = externalNo;
        }

        private String getExternalNo() {
            return externalNo;
        }
    }
}
