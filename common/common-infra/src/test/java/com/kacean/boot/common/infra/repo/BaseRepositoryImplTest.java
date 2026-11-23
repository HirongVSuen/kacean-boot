package com.kacean.boot.common.infra.repo;

import com.kacean.boot.common.app.domain.BaseDomain;
import com.kacean.boot.common.app.identity.ID;
import com.kacean.boot.common.app.identity.IDCollection;
import com.kacean.boot.common.app.query.PageResult;
import com.kacean.boot.common.infra.dao.BaseDaoImpl;
import com.kacean.boot.common.infra.entity.BaseEntity;
import com.kacean.boot.common.infra.mapper.BaseMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.DirectFieldAccessor;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.List;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 基础仓储实现测试：验证 DAO 委托、聚合与持久化对象转换、批量写入与失败保护。
 *
 * @author kacean-agent
 */
class BaseRepositoryImplTest {

    @Test
    void 查询与删除_委托基础DAO并转换领域对象() {
        TestDao dao = new TestDao();
        TestRepository repository = new TestRepository(dao);
        TestEntity entity = new TestEntity(1L, "one");
        ID id = new ID(1L, null);
        IDCollection ids = new IDCollection(List.of(1L), List.of("EXT-1"));
        dao.findResult = entity;
        dao.findResults = List.of(entity);

        TestDomain found = repository.findById(id);
        List<TestDomain> foundAll = repository.findByIds(ids);
        repository.removeById(id);
        repository.removeByIds(ids);

        assertAll(
                () -> assertEquals(new TestDomain(1L, "one"), found),
                () -> assertEquals(List.of(new TestDomain(1L, "one")), foundAll),
                () -> assertSame(id, dao.lastFindId),
                () -> assertSame(ids, dao.lastFindIds),
                () -> assertSame(id, dao.lastDeletedId),
                () -> assertSame(ids, dao.lastDeletedIds));
    }

    @Test
    void 查询未命中_返回null而不抛出异常() {
        TestDao dao = new TestDao();
        TestRepository repository = new TestRepository(dao);

        assertNull(repository.findById(new ID(1L, null)));
    }

    @Test
    void 保存与批量保存_写回持久化字段后重新转换() {
        TestDao dao = new TestDao();
        TestRepository repository = new TestRepository(dao);

        TestDomain saved = repository.save(new TestDomain(null, "one"));
        List<TestDomain> savedAll = repository.saveAll(List.of(
                new TestDomain(null, "two"), new TestDomain(9L, "three")));

        assertAll(
                () -> assertEquals(new TestDomain(100L, "one"), saved),
                () -> assertEquals(List.of(new TestDomain(101L, "two"), new TestDomain(9L, "three")), savedAll),
                () -> assertEquals(1, dao.saveBatchCalls),
                () -> assertEquals(1L, dao.lastSaved.getVersion()));
    }

    @Test
    void 空批量保存与写入失败_分别短路和抛出异常() {
        TestDao dao = new TestDao();
        TestRepository repository = new TestRepository(dao);

        assertTrue(repository.saveAll(List.of()).isEmpty());
        assertEquals(0, dao.saveBatchCalls);

        dao.writeResult = false;
        assertThrows(IllegalStateException.class, () -> repository.save(new TestDomain(null, "one")));
        assertThrows(IllegalStateException.class,
                () -> repository.saveAll(List.of(new TestDomain(null, "two"))));
    }

    @Test
    void 空标识集合_查询与删除均不访问DAO() {
        TestDao dao = new TestDao();
        TestRepository repository = new TestRepository(dao);
        IDCollection empty = new IDCollection(null, null);

        assertTrue(repository.findByIds(empty).isEmpty());
        repository.removeByIds(empty);

        assertAll(
                () -> assertNull(dao.lastFindIds),
                () -> assertNull(dao.lastDeletedIds));
    }

    @Test
    void 批量保存超过上限_在访问DAO前拒绝() {
        TestDao dao = new TestDao();
        TestRepository repository = new TestRepository(dao);
        List<TestDomain> aggregates = LongStream.rangeClosed(1, IDCollection.MAX_BATCH_SIZE + 1L)
                .mapToObj(id -> new TestDomain(id, "aggregate-" + id))
                .toList();

        assertThrows(IllegalArgumentException.class, () -> repository.saveAll(aggregates));
        assertEquals(0, dao.saveBatchCalls);
    }

    private static final class TestRepository extends BaseRepositoryImpl<TestDomain, TestEntity, TestDao> {

        private TestRepository(TestDao dao) {
            // 父类已改为字段注入，测试通过反射设置父类的 dao 与 converter 字段
            DirectFieldAccessor accessor = new DirectFieldAccessor(this);
            accessor.setPropertyValue("dao", dao);
            accessor.setPropertyValue("converter", new TestConverter());
        }
    }

    private static final class TestConverter implements BaseConvert<TestDomain, TestEntity> {

        @Override
        public TestDomain toDomain(TestEntity entity) {
            return new TestDomain(entity.getId(), entity.getName());
        }

        @Override
        public TestEntity fromDomain(TestDomain domain) {
            return new TestEntity(domain.id(), domain.name());
        }

        @Override
        public List<TestDomain> toDomain(List<TestEntity> entityList) {
            return entityList.stream().map(this::toDomain).toList();
        }

        @Override
        public List<TestEntity> fromDomain(List<TestDomain> domainList) {
            return domainList.stream().map(this::fromDomain).toList();
        }

        @Override
        public PageResult<TestDomain> toDomain(PageResult<TestEntity> pageResult) {
            return new PageResult<>(toDomain(pageResult.records()),
                    pageResult.pageNum(), pageResult.pageSize(), pageResult.total());
        }

        @Override
        public PageResult<TestEntity> fromDomain(PageResult<TestDomain> pageResult) {
            return new PageResult<>(fromDomain(pageResult.records()),
                    pageResult.pageNum(), pageResult.pageSize(), pageResult.total());
        }
    }

    private static final class TestDao extends BaseDaoImpl<TestMybatisMapper, TestEntity> {

        private TestDao() {
            // 本测试覆写全部 DAO 访问方法，Mapper 仅为填充父类注入字段的空代理
            new DirectFieldAccessor(this).setPropertyValue("baseMapper",
                    Proxy.newProxyInstance(TestMybatisMapper.class.getClassLoader(),
                            new Class<?>[]{TestMybatisMapper.class}, (proxy, method, args) -> null));
        }

        private TestEntity findResult;
        private List<TestEntity> findResults = List.of();
        private ID lastFindId;
        private IDCollection lastFindIds;
        private ID lastDeletedId;
        private IDCollection lastDeletedIds;
        private TestEntity lastSaved;
        private boolean writeResult = true;
        private long nextId = 100L;
        private int saveBatchCalls;

        @Override
        public TestEntity findById(ID id) {
            lastFindId = id;
            return findResult;
        }

        @Override
        public List<TestEntity> findByIds(IDCollection ids) {
            lastFindIds = ids;
            return findResults;
        }

        @Override
        public boolean saveOrUpdate(TestEntity entity) {
            lastSaved = entity;
            if (entity.getId() == null) {
                entity.setId(nextId++);
            }
            entity.setVersion(entity.getVersion() + 1);
            return writeResult;
        }

        @Override
        public boolean saveOrUpdateBatch(Collection<TestEntity> entityList) {
            saveBatchCalls++;
            entityList.forEach(this::saveOrUpdate);
            return writeResult;
        }

        @Override
        public boolean removeById(ID id) {
            lastDeletedId = id;
            return true;
        }

        @Override
        public boolean removeById(IDCollection ids) {
            lastDeletedIds = ids;
            return true;
        }
    }

    private interface TestMybatisMapper extends BaseMapper<TestEntity> {
    }

    private record TestDomain(Long id, String name) implements BaseDomain {
    }

    private static final class TestEntity extends BaseEntity {

        private static final long serialVersionUID = 1L;

        private String name;

        private TestEntity() {
        }

        private TestEntity(Long id, String name) {
            setId(id);
            this.name = name;
        }

        private String getName() {
            return name;
        }
    }
}
