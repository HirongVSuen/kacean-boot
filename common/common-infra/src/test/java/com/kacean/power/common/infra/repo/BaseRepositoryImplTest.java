package com.kacean.power.common.infra.repo;

import com.kacean.power.common.app.identity.ID;
import com.kacean.power.common.app.identity.IDCollection;
import com.kacean.power.common.app.repo.BaseRepository;
import com.kacean.power.common.infra.dao.BaseDaoImpl;
import com.kacean.power.common.infra.po.BaseEntity;
import com.kacean.power.common.infra.mapper.BaseMapper;
import com.kacean.power.common.infra.struct.StructMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 基础仓储实现测试：验证 DAO 委托、MapStruct 转换、批量写入与失败保护。
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

        Optional<TestDomain> found = repository.findById(id);
        List<TestDomain> foundAll = repository.findByIds(ids);
        repository.deleteById(id);
        repository.deleteByIds(ids);

        assertAll(
                () -> assertEquals(new TestDomain(1L, "one"), found.orElseThrow()),
                () -> assertEquals(List.of(new TestDomain(1L, "one")), foundAll),
                () -> assertSame(id, dao.lastFindId),
                () -> assertSame(ids, dao.lastFindIds),
                () -> assertSame(id, dao.lastDeletedId),
                () -> assertSame(ids, dao.lastDeletedIds));
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
        repository.deleteByIds(empty);

        assertAll(
                () -> assertNull(dao.lastFindIds),
                () -> assertNull(dao.lastDeletedIds));
    }

    @Test
    void 批量保存超过上限_在访问DAO前拒绝() {
        TestDao dao = new TestDao();
        TestRepository repository = new TestRepository(dao);
        List<TestDomain> aggregates = LongStream.rangeClosed(1, BaseRepository.MAX_BATCH_SIZE + 1L)
                .mapToObj(id -> new TestDomain(id, "aggregate-" + id))
                .toList();

        assertThrows(IllegalArgumentException.class, () -> repository.saveAll(aggregates));
        assertEquals(0, dao.saveBatchCalls);
    }

    private static final class TestRepository extends BaseRepositoryImpl<TestDomain, TestEntity> {

        private TestRepository(TestDao baseDao) {
            super(baseDao, new TestConverter());
        }
    }

    private static final class TestConverter implements StructMapper<TestDomain, TestEntity> {

        @Override
        public TestEntity toDo(TestDomain domain) {
            return new TestEntity(domain.id(), domain.name());
        }

        @Override
        public TestDomain toDomain(TestEntity dataObject) {
            return new TestDomain(dataObject.getId(), dataObject.getName());
        }
    }

    private static final class TestDao extends BaseDaoImpl<TestMybatisMapper, TestEntity> {

        private TestDao() {
            // 本测试覆写全部 DAO 访问方法，Mapper 仅为满足构造器非空约束的空代理
            super((TestMybatisMapper) Proxy.newProxyInstance(TestMybatisMapper.class.getClassLoader(),
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
        public Optional<TestEntity> findById(ID identity) {
            lastFindId = identity;
            return Optional.ofNullable(findResult);
        }

        @Override
        public List<TestEntity> findByIds(IDCollection identities) {
            lastFindIds = identities;
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
        public void deleteById(ID identity) {
            lastDeletedId = identity;
        }

        @Override
        public void deleteByIds(IDCollection identities) {
            lastDeletedIds = identities;
        }
    }

    private interface TestMybatisMapper extends BaseMapper<TestEntity> {
    }

    private record TestDomain(Long id, String name) {
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
