package com.kacean.boot.common.infra.repo;

import com.kacean.boot.common.app.domain.BaseDomain;
import com.kacean.boot.common.app.query.PageResult;
import com.kacean.boot.common.infra.entity.BaseEntity;

import java.util.List;

/**
 * 基础设施层 MapStruct 转换契约：统一定义领域模型与持久化对象之间的双向转换。
 * 独立于 MyBatis Mapper（{@code infra.mapper} 包），具体业务子接口使用 {@code @Mapper} 注解，由 MapStruct 生成实现。
 *
 * @author hirong
 * @param <DOMAIN> 领域模型类型
 * @param <ENTITY> 持久化对象类型
 */
public interface BaseConvert<DOMAIN extends BaseDomain, ENTITY extends BaseEntity> {

    /**
     * 将持久化对象转换为领域模型。
     *
     * @param entity 持久化对象
     * @return 领域模型
     */
    DOMAIN toDomain(ENTITY entity);

    /**
     * 将领域模型转换为持久化对象。
     *
     * @param domain 领域模型
     * @return 持久化对象
     */
    ENTITY fromDomain(DOMAIN domain);

    /**
     * 将持久化对象集合转换为领域模型集合。
     *
     * @param entityList 持久化对象集合
     * @return 领域模型集合
     */
    List<DOMAIN> toDomain(List<ENTITY> entityList);

    /**
     * 将领域模型集合转换为持久化对象集合。
     *
     * @param domainList 领域模型集合
     * @return 持久化对象集合
     */
    List<ENTITY> fromDomain(List<DOMAIN> domainList);

    /**
     * 将持久化对象分页结果转换为领域模型分页结果，并保留分页元数据。
     *
     * @param pageResult 持久化对象分页结果
     * @return 领域模型分页结果
     */
    PageResult<DOMAIN> toDomain(PageResult<ENTITY> pageResult);

    /**
     * 将领域模型分页结果转换为持久化对象分页结果，并保留分页元数据。
     *
     * @param pageResult 领域模型分页结果
     * @return 持久化对象分页结果
     */
    PageResult<ENTITY> fromDomain(PageResult<DOMAIN> pageResult);
}
