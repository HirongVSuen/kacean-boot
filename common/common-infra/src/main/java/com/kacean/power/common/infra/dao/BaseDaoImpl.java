package com.kacean.power.common.infra.dao;

import com.baomidou.mybatisplus.extension.repository.AbstractRepository;
import com.kacean.power.common.infra.po.BaseEntity;
import com.kacean.power.common.infra.mapper.BaseMapper;

import java.util.Objects;

/**
 * 基础 DAO 实现：继承 MyBatis-Plus {@link AbstractRepository}，构造器注入业务 MyBatis Mapper。
 * 通用 CRUD 由 IRepository 默认方法与 AbstractRepository 提供，子类可通过 {@link #getBaseMapper()}
 * 使用 QueryWrapper 或 LambdaQueryWrapper 扩展特有查询。
 * 批量方法逐条实现，不依赖 mybatis-plus-spring 的 SqlSession 批处理，也不保证原子性，
 * 需要整体提交或回滚时由调用方建立事务边界。
 *
 * @param <M> 业务 Mapper 类型
 * @param <E> 持久化对象类型
 * @author kacean-agent
 */
public abstract class BaseDaoImpl<M extends BaseMapper<E>, E extends BaseEntity>
        extends AbstractRepository<M, E> implements BaseDao<E> {

    private final M baseMapper;

    /**
     * 注入当前 DAO 使用的业务 Mapper。
     *
     * @param baseMapper 业务 Mapper，非空
     */
    protected BaseDaoImpl(M baseMapper) {
        this.baseMapper = Objects.requireNonNull(baseMapper, "基础 Mapper baseMapper 不能为空");
    }

    @Override
    public M getBaseMapper() {
        return baseMapper;
    }
}
