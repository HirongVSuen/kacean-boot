package com.kacean.power.common.web.assembler;

import com.kacean.power.common.app.domain.BaseDomain;
import com.kacean.power.common.app.query.PageResult;
import com.kacean.power.common.infra.entity.BaseEntity;
import com.kacean.power.common.web.response.BaseDTO;
import org.junit.jupiter.api.Test;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Web 层基础转换契约测试：验证具体 MapStruct 子接口能够生成四向转换实现。
 *
 * @author kacean-agent
 */
class DddAssemblerTest {

    @Test
    void 具体转换器生成成功_支持DTO与领域模型和持久化对象互转() {
        TestAssembler assembler = Mappers.getMapper(TestAssembler.class);

        assertEquals(new TestDomain("domain"), assembler.toDomain(new TestDto("domain")));
        assertEquals("entity", assembler.toEntity(new TestDto("entity")).getValue());
        assertEquals("domain", assembler.fromDomain(new TestDomain("domain")).getValue());
        assertEquals("entity", assembler.fromEntity(new TestEntity("entity")).getValue());
    }

    @Test
    void 具体转换器生成成功_支持DTO与领域模型和持久化对象集合互转() {
        TestAssembler assembler = Mappers.getMapper(TestAssembler.class);

        assertEquals(List.of(new TestDomain("domain")),
                assembler.toDomain(List.of(new TestDto("domain"))));
        assertEquals(List.of("entity"), assembler.toEntity(List.of(new TestDto("entity"))).stream()
                .map(TestEntity::getValue)
                .toList());
        assertEquals(List.of("domain"), assembler.fromDomain(List.of(new TestDomain("domain"))).stream()
                .map(TestDto::getValue)
                .toList());
        assertEquals(List.of("entity"), assembler.fromEntity(List.of(new TestEntity("entity"))).stream()
                .map(TestDto::getValue)
                .toList());
    }

    @Test
    void 具体转换器生成成功_支持DTO与领域模型和持久化对象分页结果互转() {
        TestAssembler assembler = Mappers.getMapper(TestAssembler.class);

        PageResult<TestDomain> domainResult = assembler.toDomain(
                new PageResult<>(List.of(new TestDto("domain")), 2, 10, 21));
        assertEquals(List.of(new TestDomain("domain")), domainResult.records());
        assertPageMetadata(domainResult);

        PageResult<TestEntity> entityResult = assembler.toEntity(
                new PageResult<>(List.of(new TestDto("entity")), 2, 10, 21));
        assertEquals(List.of("entity"), entityResult.records().stream()
                .map(TestEntity::getValue)
                .toList());
        assertPageMetadata(entityResult);

        PageResult<TestDto> fromDomainResult = assembler.fromDomain(
                new PageResult<>(List.of(new TestDomain("domain")), 2, 10, 21));
        assertEquals(List.of("domain"), fromDomainResult.records().stream()
                .map(TestDto::getValue)
                .toList());
        assertPageMetadata(fromDomainResult);

        PageResult<TestDto> fromEntityResult = assembler.fromEntity(
                new PageResult<>(List.of(new TestEntity("entity")), 2, 10, 21));
        assertEquals(List.of("entity"), fromEntityResult.records().stream()
                .map(TestDto::getValue)
                .toList());
        assertPageMetadata(fromEntityResult);
    }

    private static void assertPageMetadata(PageResult<?> pageResult) {
        assertEquals(2, pageResult.pageNum());
        assertEquals(10, pageResult.pageSize());
        assertEquals(21, pageResult.total());
    }

    @Mapper
    interface TestAssembler extends DddAssembler<TestDto, TestDomain, TestEntity> {
    }

    static class TestDto extends BaseDTO {
        private String value;

        TestDto() {
        }

        TestDto(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    record TestDomain(String value) implements BaseDomain {
    }

    static class TestEntity extends BaseEntity {
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
