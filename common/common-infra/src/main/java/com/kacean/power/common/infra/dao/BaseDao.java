package com.kacean.power.common.infra.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.kacean.power.common.app.identity.ID;
import com.kacean.power.common.app.identity.IDCollection;
import com.kacean.power.common.infra.po.BaseEntity;
import com.kacean.power.common.infra.query.QueryConditionBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 基础 DAO 接口：基于 MyBatis-Plus {@link IRepository} 扩展，提供主键与外部编号的统一访问语义。
 * 通用 CRUD、Wrapper 条件查询能力由 IRepository 提供，开发时可传入 QueryWrapper、LambdaQueryWrapper
 * 或 {@link QueryConditionBuilder}；不引入 mybatis-plus-spring 的 IService。
 *
 * @param <E> 持久化对象类型
 * @author hirong
 */
public interface BaseDao<E extends BaseEntity> extends IRepository<E> {

    /**
     * 批量插入
     *
     * @param entityList 实体对象集合
     * @return 插入结果
     */
    @Transactional(rollbackFor = Exception.class)
    default boolean saveBatch(Collection<E> entityList) {
        return saveBatch(entityList, DEFAULT_BATCH_SIZE);
    }

    /**
     * 批量修改插入
     *
     * @param entityList 实体对象集合
     * @return 插入结果
     */
    @Transactional(rollbackFor = Exception.class)
    default boolean saveOrUpdateBatch(Collection<E> entityList) {
        return saveOrUpdateBatch(entityList, DEFAULT_BATCH_SIZE);
    }

    /**
     * 根据ID 批量更新
     *
     * @param entityList 实体对象集合
     * @return 更新结果
     */
    @Transactional(rollbackFor = Exception.class)
    default boolean updateBatchById(Collection<E> entityList) {
        return updateBatchById(entityList, DEFAULT_BATCH_SIZE);
    }

    /**
     * 根据ID 批量删除
     *
     * @param ids 主键ID集合
     * @return 删除结果
     */
    @Transactional(rollbackFor = Exception.class)
    default boolean removeById(IDCollection ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        LambdaQueryWrapper<E> queryWrapper = new LambdaQueryWrapper<>();
        if (!ids.isIdsEmpty()) {
            queryWrapper.in(E::getId, ids.ids());
        }
        if (!ids.isExternalNoEmpty()) {
            queryWrapper.in(E::getExternalNo, ids.extendNo());
        }
        return remove(queryWrapper);
    }

    /**
     * 根据ID 删除
     *
     * @param id 主键ID
     * @return 删除结果
     */
    default boolean removeById(ID id) {
        return false;
    }

    /**
     * 根据ID 查询
     *
     * @param id 主键ID
     * @return 实体对象
     */
    default E findById(ID id) {
        return null;
    }

    /**
     * 根据ID 查询
     *
     * @param idCollection 主键ID集合
     * @return 实体对象
     */
    default List<E> findByIds(IDCollection idCollection) {
        return List.of();
    }

}
