package com.kacean.power.common.app.query;

import java.util.List;

/**
 * 查询参数契约：统一业务查询参数的关键字与排序访问方式，业务过滤字段由实现类型自行声明。
 * 业务按自身查询场景定义实现类型（推荐 record），由基础设施层的 QueryParamStruct 实现转换为查询条件构建器；
 * 实现类型应保证排序列表非空且不可变，公共层不假设任何持久化结构。
 *
 * @author kacean-agent
 */
public interface Query {

    /**
     * 获取查询关键字，匹配字段、模糊匹配与转义规则由具体业务仓储决定。
     *
     * @return 查询关键字，可为空表示不做关键字匹配
     */
    default String keyword() {
        return null;
    }

    /**
     * 获取排序规则，按列表顺序依次排序。
     *
     * @return 非空且不可变的排序规则列表，为空表示不指定排序
     */
    default List<Sort> sorts() {
        return List.of();
    }
}
