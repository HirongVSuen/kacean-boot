package com.kacean.power.common.infra.struct;

import org.junit.jupiter.api.Test;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 基础设施层 MapStruct 转换契约测试：验证具体子接口能够生成双向转换实现。
 *
 * @author kacean-agent
 */
class StructMapperTest {

    @Test
    void 具体转换器生成成功_支持领域模型与持久化对象互转() {
        TestMapper mapper = Mappers.getMapper(TestMapper.class);

        assertEquals(new TestDo("do"), mapper.toDo(new TestDomain("do")));
        assertEquals(new TestDomain("domain"), mapper.toDomain(new TestDo("domain")));
    }

    @Mapper
    interface TestMapper extends StructMapper<TestDomain, TestDo> {
    }

    record TestDomain(String value) {
    }

    record TestDo(String value) {
    }
}
