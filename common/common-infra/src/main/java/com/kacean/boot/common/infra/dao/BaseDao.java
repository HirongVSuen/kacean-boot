package com.kacean.boot.common.infra.dao;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.kacean.boot.common.app.identity.ID;
import com.kacean.boot.common.app.identity.IDCollection;
import com.kacean.boot.common.app.query.PageQuery;
import com.kacean.boot.common.app.query.PageResult;
import com.kacean.boot.common.app.query.Query;
import com.kacean.boot.common.infra.entity.BaseEntity;
import com.kacean.boot.common.infra.query.WrapperBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 基础 DAO 接口：基于 MyBatis-Plus {@link IRepository} 扩展，提供主键与外部编号的统一访问语义。
 * 通用 CRUD、Wrapper 条件查询能力由 IRepository 提供，开发时可传入 QueryWrapper 或 LambdaQueryWrapper，
 * 不引入 mybatis-plus-spring 的 IService。
 * 按业务查询参数 {@link Query} 的列表与分页查询通过 declareProperties 与 declareWheres 两个钩子声明特殊规则，
 * 再由 {@link WrapperBuilder} 默认处理未声明字段、公共关键字、动态排序与 QueryWrapper 构建。
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
     * 根据ID 批量删除，主键与外部编号条件取并集（OR 组合），空集合短路不访问 Mapper
     *
     * @param ids 主键ID集合
     * @return 删除结果
     */
    @Transactional(rollbackFor = Exception.class)
    default boolean removeById(IDCollection ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return remove(idCollectionWrapper(ids));
    }

    /**
     * 根据ID 删除：提供主键时走单条主键删除，提供外部编号时走条件删除
     *
     * @param id 主键ID，非空
     * @return 删除结果
     */
    default boolean removeById(ID id) {
        Objects.requireNonNull(id, "标识 id 不能为空");
        if (id.id() != null) {
            return removeById(id.id());
        }
        return remove(new QueryWrapper<E>().eq("external_no", id.code()));
    }

    /**
     * 根据ID 查询：提供主键时按主键查询，提供外部编号时按外部编号查询单条记录
     *
     * @param id 主键ID，非空
     * @return 实体对象，不存在时返回 {@code null}
     */
    default E findById(ID id) {
        Objects.requireNonNull(id, "标识 id 不能为空");
        if (id.id() != null) {
            return getOptById(id.id()).orElse(null);
        }
        return getOneOpt(new QueryWrapper<E>().eq("external_no", id.code()), false).orElse(null);
    }

    /**
     * 根据ID 批量查询，主键与外部编号条件取并集（OR 组合），空集合短路不访问 Mapper
     *
     * @param idCollection 主键ID集合
     * @return 实体对象列表，未命中的标识不出现在结果中
     */
    default List<E> findByIds(IDCollection idCollection) {
        if (idCollection == null || idCollection.isEmpty()) {
            return List.of();
        }
        return list(idCollectionWrapper(idCollection));
    }

    /**
     * 根据分页查询：业务查询参数经 DAO 查询方法与 {@link WrapperBuilder} 转换后执行分页，
     * 分页元数据以请求页码、页大小与实际记录总数组装
     *
     * @param pageQuery 分页查询对象，非空
     * @return 分页查询结果
     */
    default PageResult<E> page(PageQuery<? extends Query> pageQuery) {
        Objects.requireNonNull(pageQuery, "分页查询 pageQuery 不能为空");
        QueryWrapper<E> wrapper = toWrapper(pageQuery.query());
        Page<E> page = page(new Page<>(pageQuery.pageNum(), pageQuery.pageSize()), wrapper);
        return new PageResult<>(page.getRecords(), pageQuery.pageNum(), pageQuery.pageSize(), page.getTotal());
    }

    /**
     * 根据查询条件查询列表：业务查询参数经 DAO 查询方法与 {@link WrapperBuilder} 转换后查询
     *
     * @param query 查询条件，非空
     * @return 实体对象列表
     */
    default List<E> list(Query query) {
        return list(toWrapper(query));
    }

    /**
     * 声明特殊查询属性，如 SELECT 返回字段、公共关键字匹配列与固定排序；默认不声明。
     *
     * @param query 当前业务查询参数
     * @param builder 携带当前 QP 与 PO 类型的 Wrapper 构建器
     */
    default void declareProperties(Query query, WrapperBuilder<E> builder) {
        // 默认不做任何声明
    }

    /**
     * 声明 LIKE、IN、BETWEEN 等特殊 WHERE 条件；默认不声明，普通字段由 Builder 自动按 EQ 处理。
     *
     * @param query 当前业务查询参数
     * @param builder 携带当前 QP 与 PO 类型的 Wrapper 构建器
     */
    default void declareWheres(Query query, WrapperBuilder<E> builder) {
        // 默认不做任何声明
    }

    /**
     * 核心查询模板：先执行 DAO 钩子，再默认处理普通字段、公共关键字和动态排序。
     *
     * @param query 业务查询参数，非空
     * @return 查询条件
     */
    default QueryWrapper<E> toWrapper(Query query) {
        WrapperBuilder<E> builder = new WrapperBuilder<>(query, getEntityClass());
        declareProperties(query, builder);
        declareWheres(query, builder);
        builder.processDefaultFields();
        builder.processKeyword();
        builder.processOrder();
        return builder.build();
    }

    /**
     * 组装主键与外部编号的并集条件：两类标识都存在时用 OR 连接，只有一类时不拼接多余 OR
     *
     * @param ids 非空标识集合
     * @return 并集查询条件
     */
    private QueryWrapper<E> idCollectionWrapper(IDCollection ids) {
        QueryWrapper<E> wrapper = new QueryWrapper<>();
        boolean hasIds = !ids.isIdsEmpty();
        if (hasIds) {
            wrapper.in("id", ids.ids());
        }
        if (!ids.isExternalNoEmpty()) {
            if (hasIds) {
                wrapper.or();
            }
            wrapper.in("external_no", ids.extendNo());
        }
        return wrapper;
    }
}
