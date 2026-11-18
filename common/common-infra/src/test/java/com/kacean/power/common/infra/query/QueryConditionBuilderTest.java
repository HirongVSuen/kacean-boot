package com.kacean.power.common.infra.query;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kacean.power.common.infra.po.BaseEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 查询条件构建器测试：固定 addProperty / addWhere / addOrder 的构建结果与单次构建语义。
 *
 * @author kacean-agent
 */
class QueryConditionBuilderTest {

    @Test
    void 构建_返回字段条件与排序全部生效且只构建一次() {
        QueryConditionBuilder<TestEntity> builder = new QueryConditionBuilder<>() {
            @Override
            protected void addProperty() {
                wrapper().select("id", "external_no");
            }

            @Override
            protected void addWhere() {
                wrapper().eq("external_no", "EXT-1").ge("version", 1);
            }

            @Override
            protected void addOrder() {
                wrapper().orderByDesc("create_at");
            }
        };

        QueryWrapper<TestEntity> first = builder.toWrapper();
        QueryWrapper<TestEntity> second = builder.toWrapper();

        String sqlSegment = first.getSqlSegment();
        assertAll(
                () -> assertSame(first, second),
                () -> assertEquals("id,external_no", first.getSqlSelect().replace(" ", "")),
                () -> assertTrue(sqlSegment.contains("external_no =")),
                () -> assertTrue(sqlSegment.contains("version >=")),
                () -> assertTrue(sqlSegment.contains("ORDER BY create_at DESC")),
                () -> assertEquals(sqlSegment.indexOf("external_no ="), sqlSegment.lastIndexOf("external_no =")),
                () -> assertTrue(first.getParamNameValuePairs().values().containsAll(List.of("EXT-1", 1))));
    }

    @Test
    void 默认实现_不施加返回字段限制条件与排序() {
        QueryConditionBuilder<TestEntity> builder = new QueryConditionBuilder<>() {
        };

        QueryWrapper<TestEntity> wrapper = builder.toWrapper();

        assertAll(
                () -> assertNull(wrapper.getSqlSelect()),
                () -> assertTrue(wrapper.getSqlSegment().isBlank()));
    }

    private static final class TestEntity extends BaseEntity {

        private static final long serialVersionUID = 1L;
    }
}
