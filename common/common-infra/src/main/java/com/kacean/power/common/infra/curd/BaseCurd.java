package com.kacean.power.common.infra.curd;

import com.kacean.power.common.app.identity.ID;
import com.kacean.power.common.app.identity.IDCollection;
import com.kacean.power.common.app.query.PageQuery;
import com.kacean.power.common.app.query.PageResult;
import com.kacean.power.common.app.query.Query;
import com.kacean.power.common.infra.dao.BaseDao;
import com.kacean.power.common.infra.entity.BaseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

/**
 * 基础CURD操作类：通过 {@code @Autowired} 字段注入业务 DAO，依赖由 Spring 按子类泛型解析注入，子类无需声明构造器。
 *
 * @author hirong
 */
public class BaseCurd<E extends BaseEntity, D extends BaseDao<E>> {

    /**
     * 持久层对象，由 Spring 按子类泛型注入
     */
    @Autowired
    private D dao;

    /**
     * 获取持久层对象
     */
    protected D dao() {
        return dao;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(Collection<E> entityList) {
        return dao.saveBatch(entityList);
    }

    /**
     * 批量修改插入
     *
     * @param entityList 实体对象集合
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateBatch(Collection<E> entityList) {
        return dao.saveOrUpdateBatch(entityList);
    }

    /**
     * 批量删除(jdbc批量提交)
     *
     * @param list 主键ID或实体列表(主键ID类型必须与实体类型字段保持一致)
     * @return 删除结果
     * @since 3.5.0
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean removeBatchByIds(Collection<?> list) {
        return dao.removeByIds(list);
    }

    /**
     * 根据ID 批量更新
     *
     * @param entityList 实体对象集合
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBatchById(Collection<E> entityList) {
        return dao.updateBatchById(entityList);
    }

    /**
     * 根据ID 批量删除
     *
     * @param ids 主键ID集合
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(IDCollection ids) {
        return dao.removeById(ids);
    }

    /**
     * 根据ID 删除
     *
     * @param id 主键ID
     * @return 删除结果
     */
    public boolean removeById(ID id) {
        return dao.removeById(id);
    }

    /**
     * 根据ID 查询
     *
     * @param id 主键ID
     * @return 实体对象
     */
    public E findById(ID id) {
        return dao.findById(id);
    }

    /**
     * 根据ID 查询
     *
     * @param idCollection 主键ID集合
     * @return 实体对象
     */
    public List<E> findByIds(IDCollection idCollection) {
        return dao.findByIds(idCollection);
    }


    public boolean save(E entity) {
        return dao.save(entity);
    }

    /**
     * 根据 ID 删除
     *
     * @param id 主键ID
     */
    public boolean removeById(Serializable id) {
        return dao.removeById(id);
    }

    /**
     * 根据实体(ID)删除
     *
     * @param entity 实体
     * @since 3.4.4
     */
    public boolean removeById(E entity) {
        return dao.removeById(entity);
    }

    /**
     * 删除（根据ID 批量删除）
     *
     * @param list 主键ID或实体列表
     */
    public boolean removeByIds(Collection<?> list) {
        return dao.removeByIds(list);
    }


    /**
     * 根据 ID 选择修改
     *
     * @param entity 实体对象
     */
    public boolean updateById(E entity) {
        return dao.updateById(entity);
    }

    /**
     * TableId 注解存在更新记录，否插入一条记录
     *
     * @param entity 实体对象
     */
    public boolean saveOrUpdate(E entity) {
        return dao.saveOrUpdate(entity);
    }

    /**
     * 根据 ID 查询
     *
     * @param id 主键ID
     */
    public E getById(Serializable id) {
        return dao.getById(id);
    }

    /**
     * 根据 ID 查询，返回一个Option对象
     *
     * @param id 主键ID
     * @return {@link Optional}
     */
    public Optional<E> getOptById(Serializable id) {
        return dao.getOptById(id);
    }

    /**
     * 查询（根据ID 批量查询）
     *
     * @param idList 主键ID列表
     */
    public List<E> listByIds(Collection<? extends Serializable> idList) {
        return dao.listByIds(idList);
    }

    /**
     * 查询总记录数
     *
     */
    public long count() {
        return dao.count();
    }

    /**
     * 查询所有
     *
     */
    public List<E> list() {
        return dao.list();
    }

    /**
     * 查询所有列表
     *
     */
    public List<Map<String, Object>> listMaps() {
        return dao.listMaps();
    }

    /**
     * 查询全部记录
     */
    public <R> List<R> listObjs() {
        return dao.listObjs();
    }

    /**
     * 查询全部记录
     *
     * @param mapper 转换函数
     */
    public <V> List<V> listObjs(Function<? super Object, V> mapper) {
        return dao.listObjs(mapper);
    }

    /**
     * 根据分页查询
     *
     * @param pageQuery 分页查询对象
     */
    public PageResult<E> page(PageQuery<? extends Query> pageQuery) {
        return dao.page(pageQuery);
    }

    /**
     * 根据查询条件查询列表
     *
     * @param query 查询条件
     */
    public List<E> list(Query query) {
        return dao.list(query);
    }
}
