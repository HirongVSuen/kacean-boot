package com.kacean.boot.common.web.assembler;

import com.kacean.boot.common.app.query.PageResult;
import com.kacean.boot.common.infra.entity.BaseEntity;
import com.kacean.boot.common.web.response.BaseDTO;
import org.junit.jupiter.api.Test;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CurdAssembler MapStruct 生成验证。
 *
 * @author kacean-agent
 */
class CurdAssemblerTest {

    /**
     * 简单转换器只依赖 DTO 与持久化对象，支持单值、集合与分页转换。
     */
    @Test
    void 简单转换器支持DTO与持久化对象互转() {
        SimpleTestAssembler assembler = Mappers.getMapper(SimpleTestAssembler.class);
        SimpleEntity entity = new SimpleEntity();
        entity.setId(1L);
        entity.setName("简单模式");

        SimpleDTO dto = assembler.fromEntity(entity);
        assertEquals(1L, dto.getId());
        assertEquals("简单模式", dto.getName());
        assertEquals("简单模式", assembler.toEntity(dto).getName());
        assertEquals(List.of("简单模式"), assembler.fromEntity(List.of(entity)).stream()
                .map(SimpleDTO::getName).toList());

        PageResult<SimpleDTO> page = assembler.fromEntity(new PageResult<>(List.of(entity), 2, 3, 10));
        assertEquals(List.of("简单模式"), page.records().stream().map(SimpleDTO::getName).toList());
        assertEquals(2, page.pageNum());
        assertEquals(3, page.pageSize());
        assertEquals(10, page.total());
    }

    /**
     * 简单 DTO 测试模型。
     *
     * @author kacean-agent
     */
    public static class SimpleDTO extends BaseDTO {
        private Long id;
        private String name;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * 简单持久化测试模型。
     *
     * @author kacean-agent
     */
    public static class SimpleEntity extends BaseEntity {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * 简单转换器测试契约。
     *
     * @author kacean-agent
     */
    @Mapper
    interface SimpleTestAssembler extends CurdAssembler<SimpleDTO, SimpleEntity> {
    }
}
