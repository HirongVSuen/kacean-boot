package com.kacean.power.common.infra.dao;

import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.repository.AbstractRepository;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.kacean.power.common.infra.entity.BaseEntity;
import com.kacean.power.common.infra.mapper.BaseMapper;
import org.apache.ibatis.binding.MapperMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 基础 DAO 实现：继承 MyBatis-Plus {@link AbstractRepository}，通过 {@code @Autowired} 字段注入业务 MyBatis Mapper。
 * 依赖由 Spring 按子类泛型解析注入，子类无需声明构造器。
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

    /**
     * 当前 DAO 使用的业务 Mapper，由 Spring 按子类泛型注入。
     */
    @Autowired
    private M baseMapper;

    @Override
    public M getBaseMapper() {
        return baseMapper;
    }

    /**
     * 逐条保存，任一记录保存失败即返回 {@code false}；空集合不访问 Mapper 并返回 {@code true}。
     * 不使用 SqlSession 批处理，{@code batchSize} 仅为遵守契约而保留。
     *
     * @param entityList 实体对象集合，非空
     * @param batchSize  批量大小，逐条实现不使用
     * @return 全部保存成功时返回 {@code true}
     */
    @Override
    public boolean saveBatch(Collection<E> entityList, int batchSize) {
        String sqlStatement = getSqlStatement(SqlMethod.INSERT_ONE);
        return executeBatch(entityList, batchSize, (sqlSession, entity) -> sqlSession.insert(sqlStatement, entity));
    }


    /**
     * 逐条按主键更新，任一记录更新失败即返回 {@code false}；空集合不访问 Mapper 并返回 {@code true}。
     * 不使用 SqlSession 批处理，{@code batchSize} 仅为遵守契约而保留。
     *
     * @param entityList 实体对象集合，非空
     * @param batchSize  批量大小，逐条实现不使用
     * @return 全部更新成功时返回 {@code true}
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateBatchById(Collection<E> entityList, int batchSize) {
        String sqlStatement = getSqlStatement(SqlMethod.UPDATE_BY_ID);
        return executeBatch(entityList, batchSize, (sqlSession, entity) -> {
            MapperMethod.ParamMap<E> param = new MapperMethod.ParamMap<>();
            param.put(Constants.ENTITY, entity);
            return sqlSession.update(sqlStatement, param);
        });
    }

    /**
     * 按 {@link BaseEntity} 的 Long 主键判定插入或更新：主键为空直接插入；
     * 主键非空时记录存在则更新，不存在则插入，不依赖 Mapper 的 insertOrUpdate。
     *
     * @param entity 实体对象，非空
     * @return 写入成功时返回 {@code true}
     */
    @Override
    public boolean saveOrUpdate(E entity) {
        Objects.requireNonNull(entity, "实体 entity 不能为空");
        if (entity.getId() == null) {
            return save(entity);
        }
        return getById(entity.getId()) != null ? updateById(entity) : save(entity);
    }

    /**
     * 批量按主键删除，空集合短路不访问 Mapper 并返回 {@code true}。
     *
     * @param list 主键ID或实体列表
     * @return 空集合或删除生效时返回 {@code true}
     */
    @Override
    public boolean removeByIds(Collection<?> list) {
        if (list == null || list.isEmpty()) {
            return true;
        }
        return getBaseMapper().deleteByIds(list) > 0;
    }

    /**
     * 批量按主键查询，空集合短路不访问 Mapper 并返回空列表。
     *
     * @param idList 主键ID列表
     * @return 实体对象列表
     */
    @Override
    public List<E> listByIds(Collection<? extends Serializable> idList) {
        if (idList == null || idList.isEmpty()) {
            return List.of();
        }
        return getBaseMapper().selectByIds(idList);
    }

    /**
     * 获取 SQL 语句。
     *
     * @param sqlMethod SQL 方法
     * @return SQL 语句
     */
    protected String getSqlStatement(SqlMethod sqlMethod) {
        return SqlHelper.getSqlStatement(this.getMapperClass(), sqlMethod);
    }

    /**
     * 逐条保存或更新，任一记录写入失败即返回 {@code false}；空集合不访问 Mapper 并返回 {@code true}。
     * 不使用 SqlSession 批处理，{@code batchSize} 仅为遵守契约而保留。
     *
     * @param entityList 实体对象集合，非空
     * @param batchSize  批量大小，逐条实现不使用
     * @return 全部写入成功时返回 {@code true}
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveOrUpdateBatch(Collection<E> entityList, int batchSize) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(this.getEntityClass());
        Assert.notNull(tableInfo, "error: can not execute. because can not find cache of TableInfo for entity!");
        String keyProperty = tableInfo.getKeyProperty();
        Assert.notEmpty(keyProperty, "error: can not execute. because can not find column for id from entity!");
        return SqlHelper.saveOrUpdateBatch(
                getSqlSessionFactory(), this.getMapperClass(), this.log, entityList, batchSize,
                (sqlSession, entity) -> {
                    Object idVal = tableInfo.getPropertyValue(entity, keyProperty);
                    return StringUtils.checkValNull(idVal) || CollectionUtils
                            .isEmpty(sqlSession.selectList(getSqlStatement(SqlMethod.SELECT_BY_ID), idVal));
                }, (sqlSession, entity) -> {
                    MapperMethod.ParamMap<E> param = new MapperMethod.ParamMap<>();
                    param.put(Constants.ENTITY, entity);
                    return sqlSession.update(getSqlStatement(SqlMethod.UPDATE_BY_ID), param);
                });
    }
}
