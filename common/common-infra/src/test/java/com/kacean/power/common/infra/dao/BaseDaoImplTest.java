package com.kacean.power.common.infra.dao;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kacean.power.common.infra.po.BaseEntity;
import com.kacean.power.common.infra.query.QueryConditionBuilder;
import com.kacean.power.common.infra.mapper.BaseMapper;
import org.junit.jupiter.api.Test;

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
    void 条件构建器查询_列表与统计均使用构建后的Wrapper() {
        AtomicReference<QueryWrapper<TestEntity>> listWrapper = new AtomicReference<>();
        AtomicReference<QueryWrapper<TestEntity>> countWrapper = new AtomicReference<>();
        TestDao dao = new TestDao(mapper((proxy, method, args) -> {
            switch (method.getName()) {
                case "selectList" -> {
                    listWrapper.set(castWrapper(args[0]));
                    return List.of();
                }
                case "selectCount" -> {
                    countWrapper.set(castWrapper(args[0]));
                    return 1L;
                }
                default -> {
                    return null;
                }
            }
        }));
        QueryConditionBuilder<TestEntity> builder = new QueryConditionBuilder<>() {
            @Override
            protected void addProperty() {
                wrapper().select("id");
            }

            @Override
            protected void addWhere() {
                wrapper().eq("external_no", "EXT-1");
            }

            @Override
            protected void addOrder() {
                wrapper().orderByAsc("id");
            }
        };

        assertEquals(List.of(), dao.list(builder));
        assertEquals(1L, dao.count(builder));
        assertAll(
                () -> assertEquals("id", listWrapper.get().getSqlSelect()),
                () -> assertTrue(listWrapper.get().getSqlSegment().contains("ORDER BY")),
                () -> assertSame(listWrapper.get(), countWrapper.get()),
                () -> assertTrue(countWrapper.get().getSqlSegment().contains("external_no =")));
    }

    @SuppressWarnings("unchecked")
    private static QueryWrapper<TestEntity> castWrapper(Object arg) {
        return (QueryWrapper<TestEntity>) arg;
    }

    private static TestMapper mapper(InvocationHandler handler) {
        return (TestMapper) Proxy.newProxyInstance(TestMapper.class.getClassLoader(),
                new Class<?>[]{TestMapper.class}, handler);
    }

    private static final class TestDao extends BaseDaoImpl<TestMapper, TestEntity> {

        private TestDao(TestMapper baseMapper) {
            super(baseMapper);
        }
    }

    private interface TestMapper extends BaseMapper<TestEntity> {
    }

    private static final class TestEntity extends BaseEntity {

        private static final long serialVersionUID = 1L;
    }
}
