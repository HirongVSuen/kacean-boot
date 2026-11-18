package com.kacean.power.common.infra.struct;

import com.kacean.power.common.app.query.Query;
import com.kacean.power.common.infra.po.BaseEntity;
import com.kacean.power.common.infra.query.QueryConditionBuilder;

/**
 * 查询参数转换契约：把 app 层业务查询参数转换为 infra 查询条件构建器。
 * 每个实现根据自身查询参数字段定义返回字段、过滤条件与排序，
 * 通常返回覆写 addProperty / addWhere / addOrder 的 QueryConditionBuilder 实现。
 *
 * @author kacean-agent
 * @param <QP> 业务查询参数类型
 * @param <E> 持久化对象类型
 */
public interface QueryParamStruct<QP extends Query, E extends BaseEntity> {

    /**
     * 将业务查询参数转换为查询条件构建器。
     *
     * @param queryParam 业务查询参数，非空
     * @return 查询条件构建器，非空
     */
    QueryConditionBuilder<E> toConditionBuilder(QP queryParam);
}
