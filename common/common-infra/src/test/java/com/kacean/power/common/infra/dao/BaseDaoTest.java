package com.kacean.power.common.infra.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kacean.power.common.app.identity.ID;
import com.kacean.power.common.app.identity.IDCollection;
import com.kacean.power.common.app.query.PageQuery;
import com.kacean.power.common.app.query.PageResult;
import com.kacean.power.common.app.query.Query;
import com.kacean.power.common.infra.entity.BaseEntity;
import com.kacean.power.common.infra.mapper.BaseMapper;
import com.kacean.power.common.infra.query.Operator;
import com.kacean.power.common.infra.query.WrapperBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.DirectFieldAccessor;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 基础 DAO 测试：固定单标识二选一、外部编号条件、批量 OR 查询与删除、空集合短路，
 * 以及业务查询参数经条件转换后的列表与分页查询语义。
 *
 * @author kacean-agent
 */
class BaseDaoTest {

    @Test
    void 单标识查询_主键与外部编号使用各自条件() {
        TestDao dao = new TestDao();
        TestEntity byId = new TestEntity();
        TestEntity byCode = new TestEntity();
        dao.byId = byId;
        dao.byCode = byCode;

        assertSame(byId, dao.findById(new ID(1L, null)));
        assertEquals(1L, dao.selectedId);
        assertNull(dao.query);

        assertSame(byCode, dao.findById(new ID(null, "EXT-1")));
        assertTrue(dao.query.getSqlSegment().contains("external_no"));
        assertTrue(dao.query.getParamNameValuePairs().containsValue("EXT-1"));
    }

    @Test
    void 单标识查询_未命中时返回null() {
        TestDao dao = new TestDao();

        assertAll(
                () -> assertNull(dao.findById(new ID(1L, null))),
                () -> assertNull(dao.findById(new ID(null, "EXT-1"))));
    }

    @Test
    void 批量标识查询_主键与外部编号使用OR组合() {
        TestDao dao = new TestDao();
        TestEntity entity = new TestEntity();
        dao.entities = List.of(entity);

        List<TestEntity> result = dao.findByIds(new IDCollection(List.of(1L, 2L), List.of("EXT-1", "EXT-2")));

        assertEquals(List.of(entity), result);
        assertIdsOrCodesCondition(dao.query);
    }

    @Test
    void 批量标识删除_主键与外部编号使用OR组合() {
        TestDao dao = new TestDao();

        assertTrue(dao.removeById(new IDCollection(List.of(1L, 2L), List.of("EXT-1", "EXT-2"))));
        assertEquals(1, dao.removeCalls);
        assertIdsOrCodesCondition(dao.deletedQuery);
    }

    @Test
    void 空批量标识_查询与删除均不访问Mapper() {
        TestDao dao = new TestDao();
        IDCollection empty = new IDCollection(List.of(), List.of());

        assertTrue(dao.findByIds(empty).isEmpty());
        assertFalse(dao.removeById(empty));

        assertAll(
                () -> assertEquals(0, dao.listCalls),
                () -> assertEquals(0, dao.removeCalls));
    }

    @Test
    void 删除_主键走单条删除而外部编号走条件删除() {
        TestDao dao = new TestDao();

        assertTrue(dao.removeById(new ID(3L, null)));
        assertEquals(3L, dao.deletedId);
        assertNull(dao.deletedQuery);

        assertTrue(dao.removeById(new ID(null, "EXT-3")));
        assertTrue(dao.deletedQuery.getSqlSegment().contains("external_no"));
        assertTrue(dao.deletedQuery.getParamNameValuePairs().containsValue("EXT-3"));
    }

    @Test
    void 业务查询列表_经条件转换后查询() {
        TestDao dao = new TestDao();
        TestEntity entity = new TestEntity();
        dao.entities = List.of(entity);

        List<TestEntity> result = dao.list(new TestQuery("EXT-1"));

        assertEquals(List.of(entity), result);
        assertTrue(dao.query.getSqlSegment().contains("external_no ="));
        assertTrue(dao.query.getParamNameValuePairs().containsValue("EXT-1"));
    }

    @Test
    void 分页查询_按页码页大小查询并组装分页结果() {
        TestDao dao = new TestDao();
        TestEntity entity = new TestEntity();
        dao.entities = List.of(entity);
        dao.pageTotal = 21L;

        PageResult<TestEntity> result = dao.page(new PageQuery<>(new TestQuery("EXT-1"), 2, 10));

        assertAll(
                () -> assertEquals(List.of(entity), result.records()),
                () -> assertEquals(2, result.pageNum()),
                () -> assertEquals(10, result.pageSize()),
                () -> assertEquals(21L, result.total()),
                () -> assertEquals(3L, result.totalPages()),
                () -> assertEquals(2L, dao.pageRequest.getCurrent()),
                () -> assertEquals(10L, dao.pageRequest.getSize()),
                () -> assertTrue(dao.query.getSqlSegment().contains("external_no =")),
                () -> assertTrue(dao.query.getParamNameValuePairs().containsValue("EXT-1")));
    }

    @Test
    void 未覆写条件钩子_普通QP字段默认EQ查询() {
        TestDao dao = new DefaultQueryDao();

        dao.list(new TestQuery("EXT-1"));
        dao.page(new PageQuery<>(new TestQuery("EXT-1"), 1, 10));

        assertAll(
                () -> assertTrue(dao.query.getSqlSegment().contains("external_no =")),
                () -> assertTrue(dao.query.getParamNameValuePairs().containsValue("EXT-1")));
    }

    private static void assertIdsOrCodesCondition(QueryWrapper<TestEntity> query) {
        String sqlSegment = query.getSqlSegment();
        assertAll(
                () -> assertTrue(sqlSegment.contains("id IN")),
                () -> assertTrue(sqlSegment.contains("external_no IN")),
                () -> assertTrue(sqlSegment.contains("OR")),
                () -> assertTrue(query.getParamNameValuePairs().values().containsAll(
                        List.of(1L, 2L, "EXT-1", "EXT-2"))));
    }

    private static final class TestQuery extends Query {

        private final String externalNo;

        private TestQuery(String externalNo) {
            this.externalNo = externalNo;
        }

        private String getExternalNo() {
            return externalNo;
        }
    }

    private static TestMapper emptyMapper() {
        // Mapper 仅为填充父类注入字段的空代理，测试通过覆写 DAO 访问方法拦截全部调用
        return (TestMapper) Proxy.newProxyInstance(TestMapper.class.getClassLoader(),
                new Class<?>[]{TestMapper.class}, (proxy, method, args) -> null);
    }

    private static class TestDao extends BaseDaoImpl<TestMapper, TestEntity> {

        private TestDao() {
            // 父类已改为字段注入，测试通过反射设置父类的 baseMapper 字段
            new DirectFieldAccessor(this).setPropertyValue("baseMapper", emptyMapper());
        }

        private Serializable selectedId;
        private Serializable deletedId;
        private QueryWrapper<TestEntity> query;
        private QueryWrapper<TestEntity> deletedQuery;
        private IPage<TestEntity> pageRequest;
        private TestEntity byId;
        private TestEntity byCode;
        private List<TestEntity> entities = List.of();
        private long pageTotal;
        private int listCalls;
        private int removeCalls;

        @Override
        public Class<TestEntity> getEntityClass() {
            return TestEntity.class;
        }

        @Override
        public void declareWheres(Query query, WrapperBuilder<TestEntity> builder) {
            TestQuery typedQuery = builder.query(TestQuery.class);
            builder.addWhere("externalNo", Operator.EQ, typedQuery.getExternalNo());
        }

        @Override
        public Optional<TestEntity> getOptById(Serializable id) {
            selectedId = id;
            return Optional.ofNullable(byId);
        }

        @Override
        public Optional<TestEntity> getOneOpt(Wrapper<TestEntity> query, boolean throwEx) {
            this.query = (QueryWrapper<TestEntity>) query;
            return Optional.ofNullable(byCode);
        }

        @Override
        public List<TestEntity> list(Wrapper<TestEntity> query) {
            listCalls++;
            this.query = (QueryWrapper<TestEntity>) query;
            return entities;
        }

        @Override
        public <P extends IPage<TestEntity>> P page(P page, Wrapper<TestEntity> query) {
            this.query = (QueryWrapper<TestEntity>) query;
            this.pageRequest = page;
            page.setRecords(entities);
            page.setTotal(pageTotal);
            return page;
        }

        @Override
        public boolean removeById(Serializable id) {
            deletedId = id;
            return true;
        }

        @Override
        public boolean remove(Wrapper<TestEntity> query) {
            removeCalls++;
            deletedQuery = (QueryWrapper<TestEntity>) query;
            return true;
        }
    }

    private static final class DefaultQueryDao extends TestDao {

        private DefaultQueryDao() {
        }

        @Override
        public void declareWheres(Query query, WrapperBuilder<TestEntity> builder) {
            // 不声明特殊条件，验证未处理字段自动 EQ
        }
    }

    private interface TestMapper extends BaseMapper<TestEntity> {
    }

    private static final class TestEntity extends BaseEntity {

        private static final long serialVersionUID = 1L;
    }
}
