package com.kacean.boot.common.infra.mapper;

import com.kacean.boot.common.infra.entity.BaseEntity;

/**
 * MyBatis 基础 Mapper：统一业务持久化 Mapper 的父接口，直接继承 MyBatis-Plus 原生 BaseMapper 能力。
 * 业务 Mapper 继承本接口后即可使用 QueryWrapper、LambdaQueryWrapper 等条件查询与通用单表读写；
 * 本包只承载 MyBatis 持久化 Mapper，通用 MapStruct 转换契约位于 {@code infra.repo} 包，二者不得混用。
 *
 * @author kacean-agent
 * @param <E> 持久化对象类型
 */
public interface BaseMapper<E extends BaseEntity> extends com.baomidou.mybatisplus.core.mapper.BaseMapper<E> {
}
