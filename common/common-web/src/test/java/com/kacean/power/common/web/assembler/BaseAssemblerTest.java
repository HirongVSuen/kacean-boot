package com.kacean.power.common.web.assembler;

import org.junit.jupiter.api.Test;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Web 层基础转换契约测试：验证具体 MapStruct 子接口能够生成四向转换实现。
 *
 * @author kacean-agent
 */
class BaseAssemblerTest {

    @Test
    void 具体转换器生成成功_支持DTO与领域模型和持久化对象互转() {
        TestAssembler assembler = Mappers.getMapper(TestAssembler.class);

        assertEquals(new TestDomain("domain"), assembler.toDomain(new TestDto("domain")));
        assertEquals(new TestDo("do"), assembler.toDo(new TestDto("do")));
        assertEquals(new TestDto("domain"), assembler.fromDomain(new TestDomain("domain")));
        assertEquals(new TestDto("do"), assembler.fromDo(new TestDo("do")));
    }

    @Mapper
    interface TestAssembler extends BaseAssembler<TestDto, TestDomain, TestDo> {
    }

    record TestDto(String value) {
    }

    record TestDomain(String value) {
    }

    record TestDo(String value) {
    }
}
