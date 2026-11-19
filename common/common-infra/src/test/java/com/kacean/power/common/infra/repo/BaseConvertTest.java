package com.kacean.power.common.infra.repo;

import com.kacean.power.common.app.domain.BaseDomain;
import com.kacean.power.common.app.query.PageResult;
import com.kacean.power.common.infra.entity.BaseEntity;
import org.junit.jupiter.api.Test;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 基础设施层转换契约测试：验证具体 MapStruct 子接口能够生成领域模型与持久化对象的双向转换实现。
 *
 * @author kacean-agent
 */
class BaseConvertTest {

    @Test
    void 具体转换器生成成功_支持领域模型与持久化对象互转() {
        TestConverter converter = Mappers.getMapper(TestConverter.class);

        assertEquals(new TestDomain("domain"), converter.toDomain(new TestEntity("domain")));
        assertEquals("entity", converter.fromDomain(new TestDomain("entity")).getValue());
    }

    @Test
    void 具体转换器生成成功_支持领域模型与持久化对象集合互转() {
        TestConverter converter = Mappers.getMapper(TestConverter.class);

        assertEquals(List.of(new TestDomain("domain")), converter.toDomain(List.of(new TestEntity("domain"))));
        assertEquals(List.of("entity"), converter.fromDomain(List.of(new TestDomain("entity"))).stream()
                .map(TestEntity::getValue)
                .toList());
    }

    @Test
    void 具体转换器生成成功_分页结果互转并保留分页元数据() {
        TestConverter converter = Mappers.getMapper(TestConverter.class);

        PageResult<TestDomain> domainResult = converter.toDomain(
                new PageResult<>(List.of(new TestEntity("domain")), 2, 10, 21));
        assertEquals(List.of(new TestDomain("domain")), domainResult.records());
        assertPageMetadata(domainResult);

        PageResult<TestEntity> entityResult = converter.fromDomain(
                new PageResult<>(List.of(new TestDomain("entity")), 2, 10, 21));
        assertEquals(List.of("entity"), entityResult.records().stream()
                .map(TestEntity::getValue)
                .toList());
        assertPageMetadata(entityResult);
    }

    private static void assertPageMetadata(PageResult<?> pageResult) {
        assertEquals(2, pageResult.pageNum());
        assertEquals(10, pageResult.pageSize());
        assertEquals(21, pageResult.total());
    }

    @Mapper
    interface TestConverter extends BaseConvert<TestDomain, TestEntity> {
    }

    record TestDomain(String value) implements BaseDomain {
    }

    static class TestEntity extends BaseEntity {

        private static final long serialVersionUID = 1L;

        private String value;

        TestEntity() {
        }

        TestEntity(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
