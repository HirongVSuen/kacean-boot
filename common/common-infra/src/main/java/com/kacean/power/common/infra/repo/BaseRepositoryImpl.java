package com.kacean.power.common.infra.repo;

import com.kacean.power.common.app.domain.BaseDomain;
import com.kacean.power.common.app.identity.ID;
import com.kacean.power.common.app.identity.IDCollection;
import com.kacean.power.common.app.repo.BaseRepository;
import com.kacean.power.common.infra.dao.BaseDao;
import com.kacean.power.common.infra.entity.BaseEntity;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * MyBatis-Plus 基础仓储实现：通过基础 DAO 完成通用读写，通过 MapStruct 转换器隔离聚合与持久化对象。
 * DAO 与转换器由 Spring 按子类泛型进行 {@code @Autowired} 字段注入，子类无需声明构造器。
 * 查询允许聚合不存在，按标识未命中时返回 {@code null} 或不包含该标识的列表；
 * 空标识集合的批量查询与删除直接短路，不访问 DAO；批量写入与删除不默认保证原子性。
 *
 * @param <DOMAIN> 聚合类型
 * @param <E> 持久化对象类型
 * @author kacean-agent
 */
public class BaseRepositoryImpl<DOMAIN extends BaseDomain, E extends BaseEntity, DAO extends BaseDao<E>> implements BaseRepository<DOMAIN> {

    /**
     * 当前仓储使用的基础 DAO，由 Spring 按子类泛型注入。
     */
    @Autowired
    private DAO dao;

    /**
     * 当前仓储使用的基础转换器，由 Spring 按子类泛型注入。
     */
    @Autowired
    private BaseConvert<DOMAIN, E> converter;

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
    protected final BaseConvert<DOMAIN, E> converter() {
        return converter;
    }

    /**
     * 转换待保存聚合，聚合为 {@code null} 或转换结果为 {@code null} 时直接拒绝，避免把空对象交给 DAO。
     *
     * @param aggregate 待保存聚合
     * @return 持久化对象，非空
     */
    private E fromDomainRequired(DOMAIN aggregate) {
        Objects.requireNonNull(aggregate, "待保存聚合不能包含 null");
        return Objects.requireNonNull(converter.fromDomain(aggregate), "持久化对象转换结果不能为 null");
    }

    /**
     * 转换 DAO 返回的持久化对象，转换结果为 {@code null} 时视为转换器配置错误。
     *
     * @param entity 持久化对象，非空
     * @return 聚合，非空
     */
    private DOMAIN toDomainRequired(E entity) {
        return Objects.requireNonNull(converter.toDomain(entity), "聚合转换结果不能为 null");
    }

    /**
     * 批量转换 DAO 返回的持久化对象，保持 DAO 返回顺序。
     *
     * @param entities 持久化对象列表，非空
     * @return 聚合列表，非空
     */
    private List<DOMAIN> toDomainRequired(List<E> entities) {
        return Objects.requireNonNull(converter.toDomain(entities), "聚合转换结果不能为 null");
    }

    /**
     * 校验 DAO 写入结果，写入未生效时抛出异常，由调用方决定事务回滚。
     *
     * @param saved DAO 写入结果
     */
    private static void requireSaved(boolean saved) {
        if (!saved) {
            throw new IllegalStateException("保存聚合失败：记录不存在、版本冲突或数据库未接受写入");
        }
    }

    @Override
    public DOMAIN findById(ID id) {
        E entity = dao.findById(Objects.requireNonNull(id, "聚合标识 id 不能为空"));
        return entity == null ? null : toDomainRequired(entity);
    }

    @Override
    public List<DOMAIN> findByIds(IDCollection ids) {
        Objects.requireNonNull(ids, "聚合标识集合 ids 不能为空");
        if (ids.isEmpty()) {
            return List.of();
        }
        return toDomainRequired(dao.findByIds(ids));
    }

    @Override
    public DOMAIN save(DOMAIN aggregate) {
        E entity = fromDomainRequired(aggregate);
        requireSaved(dao.saveOrUpdate(entity));
        return toDomainRequired(entity);
    }

    @Override
    public List<DOMAIN> saveAll(Collection<DOMAIN> aggregates) {
        Objects.requireNonNull(aggregates, "待保存聚合集合 aggregates 不能为空");
        if (aggregates.isEmpty()) {
            return List.of();
        }
        if (aggregates.size() > IDCollection.MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("批量保存聚合数量不能超过 " + IDCollection.MAX_BATCH_SIZE
                    + "，当前值: " + aggregates.size());
        }
        List<E> entities = aggregates.stream().map(this::fromDomainRequired).toList();
        requireSaved(dao.saveOrUpdateBatch(entities));
        return toDomainRequired(entities);
    }

    @Override
    public void removeById(ID id) {
        dao.removeById(Objects.requireNonNull(id, "聚合标识 id 不能为空"));
    }

    @Override
    public void removeByIds(IDCollection ids) {
        Objects.requireNonNull(ids, "聚合标识集合 ids 不能为空");
        if (ids.isEmpty()) {
            return;
        }
        dao.removeById(ids);
    }
}
