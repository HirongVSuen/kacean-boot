package com.kacean.boot.common.web.assembler;

import com.kacean.boot.common.app.domain.BaseDomain;
import com.kacean.boot.common.app.query.PageResult;
import com.kacean.boot.common.infra.entity.BaseEntity;
import com.kacean.boot.common.web.response.BaseDTO;

import java.util.List;

/**
 * DDD Web 转换契约：扩展 CURD 转换，增加 DTO 与领域模型的双向转换。
 * 具体业务子接口使用 {@code @Mapper(componentModel = "spring")}，由 MapStruct 生成 Spring 组件。
 *
 * @author kacean-agent
 * @param <DTO> Web 层 DTO 类型
 * @param <DOMAIN> 领域模型类型
 * @param <ENTITY> 持久化对象类型
 */
public interface DddAssembler<DTO extends BaseDTO, DOMAIN extends BaseDomain, ENTITY extends BaseEntity>
        extends CurdAssembler<DTO, ENTITY> {

    /**
     * 将 DTO 转换为领域模型。
     *
     * @param dto Web 层 DTO
     * @return 领域模型
     */
    DOMAIN toDomain(DTO dto);

    /**
     * 将领域模型转换为 DTO。
     *
     * @param domain 领域模型
     * @return Web 层 DTO
     */
    DTO fromDomain(DOMAIN domain);

    /**
     * 将 DTO 集合转换为领域模型集合。
     *
     * @param dtoList Web 层 DTO 集合
     * @return 领域模型集合
     */
    List<DOMAIN> toDomain(List<DTO> dtoList);

    /**
     * 将领域模型集合转换为 DTO 集合。
     *
     * @param domainList 领域模型集合
     * @return Web 层 DTO 集合
     */
    List<DTO> fromDomain(List<DOMAIN> domainList);

    /**
     * 将 DTO 分页结果转换为领域模型分页结果，并保留分页元数据。
     *
     * @param pageResult DTO 分页结果
     * @return 领域模型分页结果
     */
    PageResult<DOMAIN> toDomain(PageResult<DTO> pageResult);

    /**
     * 将领域模型分页结果转换为 DTO 分页结果，并保留分页元数据。
     *
     * @param pageResult 领域模型分页结果
     * @return DTO 分页结果
     */
    PageResult<DTO> fromDomain(PageResult<DOMAIN> pageResult);
}
