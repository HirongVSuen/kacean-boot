package com.kacean.power.common.infra.repo;

import com.kacean.power.common.app.identity.ID;
import com.kacean.power.common.app.identity.IDCollection;
import com.kacean.power.common.app.repo.BaseRepository;
import com.kacean.power.common.infra.dao.BaseDao;
import com.kacean.power.common.infra.po.BaseEntity;
import com.kacean.power.common.infra.struct.StructMapper;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * MyBatis-Plus 基础仓储实现：通过基础 DAO 完成通用读写，通过 MapStruct 转换器隔离聚合与持久化对象。
 *
 * @param <DOMAIN> 聚合类型
 * @param <E> 持久化对象类型
 * @author kacean-agent
 */
public class BaseRepositoryImpl<DOMAIN, E extends BaseEntity, DAO extends BaseDao<E>> implements BaseRepository<DOMAIN> {

    /**
     * 当前仓储使用的基础 DAO。
     */
    private final DAO dao;

    private final StructMapper<DOMAIN, E> structMapper;

    /**
     * 注入当前仓储使用的基础 DAO 与转换器。
     *
     * @param dao          DAO，非空
     * @param structMapper 聚合与持久化对象转换器，非空
     */
    protected BaseRepositoryImpl(DAO dao, StructMapper<DOMAIN, E> structMapper) {
        this.dao = Objects.requireNonNull(dao, "dao 不能为空");
        this.structMapper = Objects.requireNonNull(structMapper, "MapStruct 转换器 structMapper 不能为空");
    }


    /**
     * 提供基础 DAO 供业务仓储处理特有查询。
     *
     * @return 当前仓储使用的 DAO
     */
    protected final DAO dao() {
        return dao;
    }

    /**
     * 提供转换器供业务仓储处理特有查询结果。
     *
     * @return 聚合与持久化对象转换器
     */
    protected final StructMapper<DOMAIN, E> structMapper() {
        return structMapper;
    }

    private E toDoRequired(DOMAIN aggregate) {
        Objects.requireNonNull(aggregate, "待保存聚合不能包含 null");
        return Objects.requireNonNull(structMapper.toDo(aggregate), "持久化对象转换结果不能为 null");
    }

    private DOMAIN toDomainRequired(E entity) {
        return Objects.requireNonNull(structMapper.toDomain(entity), "聚合转换结果不能为 null");
    }

    private static void requireSaved(boolean saved) {
        if (!saved) {
            throw new IllegalStateException("保存聚合失败：记录不存在、版本冲突或数据库未接受写入");
        }
    }

    @Override
    public DOMAIN findById(ID id) {
        E entity = dao.findById(id);
        return toDomainRequired(entity);
    }

    @Override
    public List<DOMAIN> findByIds(IDCollection ids) {
        List<E> entities = dao.findByIds(ids);
        return null;
//        return toDomainRequired(entities);
    }

    @Override
    public DOMAIN save(DOMAIN aggregate) {
        E entity = toDoRequired(aggregate);
        boolean saved = dao.save(entity);
        requireSaved(saved);
        return toDomainRequired(entity);
    }

    @Override
    public List<DOMAIN> saveAll(Collection<DOMAIN> aggregates) {
//        boolean saved = dao.saveAll(entities);
        return null;
    }

    @Override
    public void removeById(ID id) {
        boolean removed = dao.removeById(id);
    }

    @Override
    public void removeByIds(IDCollection ids) {
        boolean removed = dao.removeById(ids);
    }
}
