package com.kacean.boot.common.web.assembler;

import com.kacean.boot.common.app.query.PageResult;
import com.kacean.boot.common.infra.entity.BaseEntity;
import com.kacean.boot.common.web.response.BaseDTO;

import java.util.List;

/**
 * CURD Web 转换契约：只定义 DTO 与持久化对象之间的双向转换，供贫血模式直接复用。
 * 具体业务子接口使用 {@code @Mapper(componentModel = "spring")}，由 MapStruct 生成 Spring 组件。
 *
 * @author kacean-agent
 * @param <DTO> DTO 类型
 * @param <ENTITY> 持久化对象类型
 */
public interface CurdAssembler<DTO extends BaseDTO, ENTITY extends BaseEntity> {

    /**
     * 将 DTO 转换为持久化对象。
     *
     * @param dto Web 层 DTO
     * @return 持久化对象
     */
    ENTITY toEntity(DTO dto);

    /**
     * 将持久化对象转换为 DTO。
     *
     * @param entity 持久化对象
     * @return Web 层 DTO
     */
    DTO fromEntity(ENTITY entity);

    /**
     * 将 DTO 集合转换为持久化对象集合。
     *
     * @param dtoList Web 层 DTO 集合
     * @return 持久化对象集合
     */
    List<ENTITY> toEntity(List<DTO> dtoList);

    /**
     * 将持久化对象集合转换为 DTO 集合。
     *
     * @param entityList 持久化对象集合
     * @return Web 层 DTO 集合
     */
    List<DTO> fromEntity(List<ENTITY> entityList);

    /**
     * 将 DTO 分页结果转换为持久化对象分页结果，并保留分页元数据。
     *
     * @param pageResult DTO 分页结果
     * @return 持久化对象分页结果
     */
    PageResult<ENTITY> toEntity(PageResult<DTO> pageResult);

    /**
     * 将持久化对象分页结果转换为 DTO 分页结果，并保留分页元数据。
     *
     * @param pageResult 持久化对象分页结果
     * @return DTO 分页结果
     */
    PageResult<DTO> fromEntity(PageResult<ENTITY> pageResult);
}
