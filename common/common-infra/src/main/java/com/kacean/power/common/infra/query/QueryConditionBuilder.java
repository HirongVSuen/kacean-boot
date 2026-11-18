package com.kacean.power.common.infra.query;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kacean.power.common.infra.po.BaseEntity;

/**
 * 查询条件构建器：把业务查询条件组装为 MyBatis-Plus {@link QueryWrapper} 的扩展基类。
 * 每个实现按需覆写 {@link #addProperty()}（SELECT 返回字段）、{@link #addWhere()}（WHERE 过滤条件）
 * 与 {@link #addOrder()}（ORDER BY 排序），通过 {@link #wrapper()} 使用 QueryWrapper 全部能力。
 * 首次调用 {@link #toWrapper()} 时按返回字段、过滤条件、排序顺序构建，重复调用不重复追加条件。
 *
 * @author kacean-agent
 * @param <E> 持久化对象类型
 */
public class QueryConditionBuilder<E extends BaseEntity> {

    private final QueryWrapper<E> wrapper = Wrappers.query();

    private boolean built;

    /**
     * 定义 SELECT 返回字段，默认不限制返回字段。
     */
    protected void addProperty() {
    }

    /**
     * 定义 WHERE 过滤条件，默认不施加过滤条件。
     */
    protected void addWhere() {
    }

    /**
     * 定义 ORDER BY 排序，默认不指定排序。
     */
    protected void addOrder() {
    }

    /**
     * 输出查询构造器，供 DAO 查询使用；构建只执行一次，多次调用返回同一实例。
     *
     * @return 构建完成的 QueryWrapper
     */
    public final QueryWrapper<E> toWrapper() {
        if (!built) {
            built = true;
            addProperty();
            addWhere();
            addOrder();
        }
        return wrapper;
    }

    /**
     * 提供 QueryWrapper 供实现类组装返回字段、条件与排序。
     *
     * @return 当前构建器持有的 QueryWrapper
     */
    protected final QueryWrapper<E> wrapper() {
        return wrapper;
    }
}
