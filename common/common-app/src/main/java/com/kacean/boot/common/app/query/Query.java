package com.kacean.boot.common.app.query;

import java.util.List;

/**
 * 业务查询参数基类：统一承载关键字与排序参数，具体 QP 通过继承声明自己的过滤字段。
 * 基础设施层由业务 DAO 的 declareProperties / declareWheres 钩子与通用 WrapperBuilder 转换为持久化条件，
 * 未声明的普通字段默认按 EQ 处理，公共层不依赖任何持久化技术。
 * 排序列表始终保存为不可变快照，避免请求对象被外部集合修改。
 *
 * @author kacean-agent
 */
public class Query {

    private String keyword;
    private List<Sort> sorts = List.of();

    protected Query() {
    }

    /**
     * 使用公共查询参数构造 QP。
     *
     * @param keyword 查询关键字，可为空
     * @param sorts 排序规则，为空时表示不指定排序
     */
    protected Query(String keyword, List<Sort> sorts) {
        this.keyword = keyword;
        setSorts(sorts);
    }

    /**
     * 获取查询关键字，匹配字段与模糊匹配规则由 infra 的查询声明决定。
     *
     * @return 查询关键字，可为空表示不做关键字匹配
     */
    public final String getKeyword() {
        return keyword;
    }

    /**
     * 设置查询关键字，供具体 QP 构造或 Web 反序列化使用。
     *
     * @param keyword 查询关键字，可为空
     */
    public final void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    /**
     * 获取查询关键字的简洁访问器。
     *
     * @return 查询关键字
     */
    public final String keyword() {
        return keyword;
    }

    /**
     * 获取排序规则，按列表顺序依次排序。
     *
     * @return 非空且不可变的排序规则列表，为空表示不指定排序
     */
    public final List<Sort> getSorts() {
        return sorts;
    }

    /**
     * 设置排序规则并保存不可变快照。
     *
     * @param sorts 排序规则，为空时归一化为空列表
     */
    public final void setSorts(List<Sort> sorts) {
        this.sorts = sorts == null ? List.of() : List.copyOf(sorts);
    }

    /**
     * 获取排序规则的简洁访问器。
     *
     * @return 非空且不可变的排序规则列表
     */
    public final List<Sort> sorts() {
        return sorts;
    }
}
