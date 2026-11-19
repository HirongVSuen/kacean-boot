package com.kacean.power.common.infra.dao;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kacean.power.common.infra.entity.BaseEntity;
import com.kacean.power.common.app.query.Query;
import com.kacean.power.common.app.query.Sort;
import com.kacean.power.common.app.query.SortDirection;
import com.kacean.power.common.infra.query.Operator;
import com.kacean.power.common.infra.query.WrapperBuilder;
import com.kacean.power.common.infra.mapper.BaseMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.DirectFieldAccessor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 基础 DAO 实现测试：通过 Mapper 动态代理验证通用 CRUD 委托、保存分支与空集合短路语义。
 *
 * @author kacean-agent
 */
class BaseDaoImplTest {

    @Test
    void 增删改_委托Mapper并以受影响行数判定结果() {
        List<String> invoked = new ArrayList<>();
        AtomicInteger rows = new AtomicInteger(1);
        TestDao dao = new TestDao(mapper((proxy, method, args) -> {
            invoked.add(method.getName());
            return rows.get();
        }));
        TestEntity entity = new TestEntity();

        assertAll(
                () -> assertTrue(dao.save(entity)),
                () -> assertTrue(dao.updateById(entity)),
                () -> assertTrue(dao.update(entity, Wrappers.emptyWrapper())),
                () -> assertTrue(dao.removeById(1L)),
                () -> assertTrue(dao.removeByIds(List.of(1L, 2L))),
                () -> assertTrue(dao.remove(Wrappers.emptyWrapper())));
        assertEquals(List.of("insert", "updateById", "update", "deleteById", "deleteByIds", "delete"), invoked);

        rows.set(0);
        assertAll(
                () -> assertFalse(dao.save(entity)),
                () -> assertFalse(dao.updateById(entity)),
                () -> assertFalse(dao.removeById(1L)));
    }

    @Test
    void 查询_委托Mapper对应查询方法() {
        TestEntity entity = new TestEntity();
        TestDao dao = new TestDao(mapper((proxy, method, args) -> switch (method.getName()) {
            case "selectById", "selectOne" -> entity;
            case "selectByIds", "selectList" -> List.of(entity);
            case "selectCount" -> 2L;
            case "exists" -> true;
            default -> null;
        }));

        assertAll(
                () -> assertSame(entity, dao.getById(1L)),
                () -> assertEquals(Optional.of(entity), dao.getOptById(1L)),
                () -> assertSame(entity, dao.getOne(Wrappers.emptyWrapper())),
                () -> assertEquals(Optional.of(entity), dao.getOneOpt(Wrappers.emptyWrapper())),
                () -> assertEquals(List.of(entity), dao.listByIds(List.of(1L))),
                () -> assertEquals(List.of(entity), dao.list()),
                () -> assertEquals(2L, dao.count()),
                () -> assertTrue(dao.exists(Wrappers.emptyWrapper())));
    }

    @Test
    void 保存或更新_按主键与记录存在性选择插入或更新() {
        List<String> invoked = new ArrayList<>();
        AtomicReference<Object> selectResult = new AtomicReference<>();
        TestDao dao = new TestDao(mapper((proxy, method, args) -> {
            invoked.add(method.getName());
            return switch (method.getName()) {
                case "insert", "updateById" -> 1;
                case "selectById" -> selectResult.get();
                default -> null;
            };
        }));

        assertTrue(dao.saveOrUpdate(new TestEntity()));
        assertEquals(List.of("insert"), invoked);

        invoked.clear();
        TestEntity update = new TestEntity();
        update.setId(1L);
        selectResult.set(new TestEntity());
        assertTrue(dao.saveOrUpdate(update));
        assertEquals(List.of("selectById", "updateById"), invoked);

        invoked.clear();
        selectResult.set(null);
        assertTrue(dao.saveOrUpdate(update));
        assertEquals(List.of("selectById", "insert"), invoked);
    }

    @Test
    void 批量保存_逐条委托且任一失败时返回false() {
        AtomicInteger insertCalls = new AtomicInteger();
        TestDao dao = new TestDao(mapper((proxy, method, args) ->
                insertCalls.incrementAndGet() == 1 ? 1 : 0));

        assertFalse(dao.saveBatch(List.of(new TestEntity(), new TestEntity())));
        assertEquals(2, insertCalls.get());
    }

    @Test
    void 批量按主键更新_逐条委托且任一失败时返回false() {
        AtomicInteger updateCalls = new AtomicInteger();
        TestDao dao = new TestDao(mapper((proxy, method, args) ->
                updateCalls.incrementAndGet() == 1 ? 1 : 0));
        TestEntity first = new TestEntity();
        first.setId(1L);
        TestEntity second = new TestEntity();
        second.setId(2L);

        assertFalse(dao.updateBatchById(List.of(first, second), 100));
        assertEquals(2, updateCalls.get());
    }

    @Test
    void 空集合_批量方法与主键集合操作均不访问Mapper() {
        AtomicInteger calls = new AtomicInteger();
        TestDao dao = new TestDao(mapper((proxy, method, args) -> {
            calls.incrementAndGet();
            return null;
        }));

        assertAll(
                () -> assertTrue(dao.saveBatch(List.of())),
                () -> assertTrue(dao.saveOrUpdateBatch(List.of())),
                () -> assertTrue(dao.removeByIds(List.of())),
                () -> assertEquals(List.of(), dao.listByIds(List.of())));
        assertEquals(0, calls.get());
    }

    @Test
    void 声明式查询_列表使用自动构建的Wrapper() {
        AtomicReference<QueryWrapper<TestEntity>> listWrapper = new AtomicReference<>();
        TestDao dao = new TestDao(mapper((proxy, method, args) -> {
            if ("selectList".equals(method.getName())) {
                listWrapper.set(castWrapper(args[0]));
                return List.of();
            }
            return null;
        }));
        TestQuery query = new TestQuery("EXT-1");
        query.setSorts(List.of(new Sort("id", SortDirection.ASC)));

        assertEquals(List.of(), dao.list(query));
        assertAll(
                () -> assertEquals("id", listWrapper.get().getSqlSelect()),
                () -> assertTrue(listWrapper.get().getSqlSegment().contains("ORDER BY id ASC")),
                () -> assertTrue(listWrapper.get().getSqlSegment().contains("external_no =")),
                () -> assertTrue(listWrapper.get().getParamNameValuePairs().containsValue("EXT-1")));
    }

    @SuppressWarnings("unchecked")
    private static QueryWrapper<TestEntity> castWrapper(Object arg) {
        return (QueryWrapper<TestEntity>) arg;
    }

    private static TestMapper mapper(InvocationHandler handler) {
        return (TestMapper) Proxy.newProxyInstance(TestMapper.class.getClassLoader(),
                new Class<?>[]{TestMapper.class}, handler);
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

    private static final class TestDao extends BaseDaoImpl<TestMapper, TestEntity> {

        private TestDao(TestMapper baseMapper) {
            // 父类已改为字段注入，测试通过反射设置父类的 baseMapper 字段
            new DirectFieldAccessor(this).setPropertyValue("baseMapper", baseMapper);
        }

        @Override
        public Class<TestEntity> getEntityClass() {
            return TestEntity.class;
        }

        @Override
        public void declareProperties(Query query, WrapperBuilder<TestEntity> builder) {
            builder.select("id");
        }

        @Override
        public void declareWheres(Query query, WrapperBuilder<TestEntity> builder) {
            TestQuery typedQuery = builder.query(TestQuery.class);
            builder.addWhere("externalNo", Operator.EQ, typedQuery.getExternalNo());
        }
    }

    private interface TestMapper extends BaseMapper<TestEntity> {
    }

    private static final class TestEntity extends BaseEntity {

        private static final long serialVersionUID = 1L;
    }
}
