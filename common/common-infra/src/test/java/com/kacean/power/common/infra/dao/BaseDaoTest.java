package com.kacean.power.common.infra.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kacean.power.common.app.identity.ID;
import com.kacean.power.common.app.identity.IDCollection;
import com.kacean.power.common.infra.po.BaseEntity;
import com.kacean.power.common.infra.mapper.BaseMapper;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 基础 DAO 测试：固定单标识二选一、外部编号条件、批量 OR 查询与空集合短路语义。
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

        assertSame(byId, dao.findById(new ID(1L, null)).orElseThrow());
        assertEquals(1L, dao.selectedId);
        assertNull(dao.query);

        assertSame(byCode, dao.findById(new ID(null, "EXT-1")).orElseThrow());
        assertTrue(dao.query.getSqlSegment().contains("external_no"));
        assertTrue(dao.query.getParamNameValuePairs().containsValue("EXT-1"));
    }

    @Test
    void 批量标识查询_主键与外部编号使用OR组合() {
        TestDao dao = new TestDao();
        TestEntity entity = new TestEntity();
        dao.entities = List.of(entity);

        List<TestEntity> result = dao.findByIds(new IDCollection(List.of(1L, 2L), List.of("EXT-1", "EXT-2")));

        assertEquals(List.of(entity), result);
        String sqlSegment = dao.query.getSqlSegment();
        assertAll(
                () -> assertTrue(sqlSegment.contains("id IN")),
                () -> assertTrue(sqlSegment.contains("external_no IN")),
                () -> assertTrue(sqlSegment.contains("OR")),
                () -> assertTrue(dao.query.getParamNameValuePairs().values().containsAll(
                        List.of(1L, 2L, "EXT-1", "EXT-2"))));
    }

    @Test
    void 空批量标识_查询与删除均不访问Mapper() {
        TestDao dao = new TestDao();
        IDCollection empty = new IDCollection(List.of(), List.of());

        assertTrue(dao.findByIds(empty).isEmpty());
        dao.deleteByIds(empty);

        assertAll(
                () -> assertEquals(0, dao.listCalls),
                () -> assertEquals(0, dao.removeCalls));
    }

    @Test
    void 删除_主键走单条删除而外部编号走条件删除() {
        TestDao dao = new TestDao();

        dao.deleteById(new ID(3L, null));
        assertEquals(3L, dao.deletedId);
        assertNull(dao.deletedQuery);

        dao.deleteById(new ID(null, "EXT-3"));
        assertTrue(dao.deletedQuery.getSqlSegment().contains("external_no"));
        assertTrue(dao.deletedQuery.getParamNameValuePairs().containsValue("EXT-3"));
    }

    private static final class TestDao extends BaseDaoImpl<TestMapper, TestEntity> {

        private TestDao() {
            // 本测试覆写全部 DAO 访问方法，Mapper 仅为满足构造器非空约束的空代理
            super((TestMapper) Proxy.newProxyInstance(TestMapper.class.getClassLoader(),
                    new Class<?>[]{TestMapper.class}, (proxy, method, args) -> null));
        }

        private Serializable selectedId;
        private Serializable deletedId;
        private QueryWrapper<TestEntity> query;
        private QueryWrapper<TestEntity> deletedQuery;
        private TestEntity byId;
        private TestEntity byCode;
        private List<TestEntity> entities = List.of();
        private int listCalls;
        private int removeCalls;

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

    private interface TestMapper extends BaseMapper<TestEntity> {
    }

    private static final class TestEntity extends BaseEntity {

        private static final long serialVersionUID = 1L;
    }
}
