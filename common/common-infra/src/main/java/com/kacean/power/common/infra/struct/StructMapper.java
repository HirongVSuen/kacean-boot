package com.kacean.power.common.infra.struct;

/**
 * 基础设施层 MapStruct 转换契约：统一定义领域模型与持久化对象之间的双向转换。
 * 独立于 MyBatis Mapper（{@code infra.mapper} 包），具体业务子接口使用 {@code @Mapper} 注解，由 MapStruct 生成实现。
 *
 * @author hirong
 * @param <D> 领域模型类型
 * @param <E> 持久化对象类型
 */
public interface StructMapper<D, E> {

    /**
     * 将领域模型转换为持久化对象。
     *
     * @param domain 领域模型
     * @return 持久化对象
     */
    E toDo(D domain);

    /**
     * 将持久化对象转换为领域模型。
     *
     * @param dataObject 持久化对象
     * @return 领域模型
     */
    D toDomain(E dataObject);
}
