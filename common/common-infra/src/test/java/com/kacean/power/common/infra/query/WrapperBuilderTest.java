package com.kacean.power.common.infra.query;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kacean.power.common.app.query.Query;
import com.kacean.power.common.app.query.Sort;
import com.kacean.power.common.app.query.SortDirection;
import com.kacean.power.common.infra.entity.BaseEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 通用 Wrapper 构建器测试，固定特殊条件声明、默认 EQ、公共关键字、动态排序与 QP 类型校验。
 *
 * @author kacean-agent
 */
class WrapperBuilderTest {

    @Test
    void 完整模板_特殊条件与默认关键字排序协同构建() {
        TestQuery query = new TestQuery("EXT-1", 2L, List.of("ENABLED", "DISABLED"));
        query.setKeyword("ticket");
        query.setSorts(List.of(new Sort("createAt", SortDirection.DESC)));

        QueryWrapper<TestEntity> wrapper = buildWrapper(query);
        String sqlSegment = wrapper.getSqlSegment();

        assertAll(
                () -> assertEquals("id,external_no", wrapper.getSqlSelect().replace(" ", "")),
                () -> assertTrue(sqlSegment.contains("external_no =")),
                () -> assertTrue(sqlSegment.contains("version >=")),
                () -> assertTrue(sqlSegment.contains("status IN")),
                () -> assertTrue(sqlSegment.contains("(name LIKE")),
                () -> assertTrue(sqlSegment.contains("OR external_no LIKE")),
                () -> assertTrue(sqlSegment.contains("ORDER BY create_at DESC,id ASC")),
                () -> assertTrue(wrapper.getParamNameValuePairs().containsValue("EXT-1")),
                () -> assertTrue(wrapper.getParamNameValuePairs().containsValue(2L)),
                () -> assertTrue(wrapper.getParamNameValuePairs().containsValue("%ticket%")));
    }

    @Test
    void 空值与空集合_忽略对应条件且每次查询使用独立Wrapper() {
        TestQuery query = new TestQuery(" ", null, List.of());
        query.setKeyword(" ");

        QueryWrapper<TestEntity> first = buildWrapper(query);
        QueryWrapper<TestEntity> second = buildWrapper(query);

        assertAll(
                () -> assertNotSame(first, second),
                () -> assertFalse(first.getSqlSegment().contains("WHERE")),
                () -> assertTrue(first.getSqlSegment().contains("ORDER BY id ASC")),
                () -> assertTrue(first.getParamNameValuePairs().isEmpty()));
    }

    @Test
    void 未声明字段_非空值默认EQ且空白字段忽略() {
        DefaultQuery query = new DefaultQuery("EXT-2", 3L, " ");

        QueryWrapper<TestEntity> wrapper = new WrapperBuilder<>(query, TestEntity.class)
                .processDefaultFields()
                .build();
        String sqlSegment = wrapper.getSqlSegment();

        assertAll(
                () -> assertTrue(sqlSegment.contains("external_no =")),
                () -> assertTrue(sqlSegment.contains("version =")),
                () -> assertFalse(sqlSegment.contains("name =")),
                () -> assertTrue(wrapper.getParamNameValuePairs().containsValue("EXT-2")),
                () -> assertTrue(wrapper.getParamNameValuePairs().containsValue(3L)));
    }

    @Test
    void 特殊范围条件_BETWEEN应用两个非空边界并跳过默认EQ() {
        RangeQuery query = new RangeQuery(List.of(1L, 5L));
        WrapperBuilder<TestEntity> builder = new WrapperBuilder<>(query, TestEntity.class);

        QueryWrapper<TestEntity> wrapper = builder.addWhere("version", Operator.BETWEEN, query.getVersion())
                .processDefaultFields()
                .build();

        assertAll(
                () -> assertTrue(wrapper.getSqlSegment().contains("version BETWEEN")),
                () -> assertFalse(wrapper.getSqlSegment().contains("version =")),
                () -> assertTrue(wrapper.getParamNameValuePairs().values().containsAll(List.of(1L, 5L))));
    }

    @Test
    void 非持久化排序字段_拒绝构建避免任意列排序() {
        TestQuery query = new TestQuery(null, null, List.of());
        query.setSorts(List.of(new Sort("unknown", SortDirection.ASC)));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> buildWrapper(query));

        assertTrue(exception.getMessage().contains("unknown"));
    }

    @Test
    void 查询参数类型与DAO方法不匹配_拒绝构建() {
        OtherQuery query = new OtherQuery();

        assertThrows(IllegalArgumentException.class, () -> buildWrapper(query));
    }

    private QueryWrapper<TestEntity> buildWrapper(Query query) {
        WrapperBuilder<TestEntity> builder = new WrapperBuilder<>(query, TestEntity.class);
        declareProperties(builder);
        declareWheres(builder);
        builder.processDefaultFields();
        builder.processKeyword();
        builder.processOrder();
        return builder.build();
    }

    private void declareProperties(WrapperBuilder<TestEntity> builder) {
        builder.select("id", "externalNo")
                .keywordLike("name", "externalNo")
                .addOrder("id", SortDirection.ASC);
    }

    private void declareWheres(WrapperBuilder<TestEntity> builder) {
        TestQuery query = builder.query(TestQuery.class);
        builder.markProcessed("minVersion", "statuses")
                .addWhere("externalNo", Operator.EQ, query.getExternalNo())
                .addWhere("version", Operator.GE, query.getMinVersion())
                .addWhere("status", Operator.IN, query.getStatuses());
    }

    private static class TestQuery extends Query {

        private final String externalNo;
        private final Long minVersion;
        private final List<String> statuses;

        private TestQuery(String externalNo, Long minVersion, List<String> statuses) {
            this.externalNo = externalNo;
            this.minVersion = minVersion;
            this.statuses = statuses;
        }

        private String getExternalNo() {
            return externalNo;
        }

        private Long getMinVersion() {
            return minVersion;
        }

        private List<String> getStatuses() {
            return statuses;
        }
    }

    private static class DefaultBaseQuery extends Query {

        private final String externalNo;

        private DefaultBaseQuery(String externalNo) {
            this.externalNo = externalNo;
        }
    }

    private static final class DefaultQuery extends DefaultBaseQuery {

        private final Long version;
        private final String name;

        private DefaultQuery(String externalNo, Long version, String name) {
            super(externalNo);
            this.version = version;
            this.name = name;
        }
    }

    private static final class RangeQuery extends Query {

        private final List<Long> version;

        private RangeQuery(List<Long> version) {
            this.version = version;
        }

        private List<Long> getVersion() {
            return version;
        }
    }

    private static final class OtherQuery extends Query {
    }

    private static final class TestEntity extends BaseEntity {

        private static final long serialVersionUID = 1L;

        private String name;
        private String status;
    }
}
