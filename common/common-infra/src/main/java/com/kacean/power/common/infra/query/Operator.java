package com.kacean.power.common.infra.query;

/**
 * 查询条件操作符：用于 DAO 声明特殊 WHERE 条件，未声明的 QP 字段默认使用 {@link #EQ}。
 *
 * @author kacean-agent
 */
public enum Operator {
    /**
     * 等于
     */
    EQ,
    /**
     * 不等于
     */
    NE,
    /**
     * 大于
     */
    GT,
    /**
     * 大等于
     */
    GE,
    /**
     * 小于
     */
    LT,
    /**
     * 小于等于
     */
    LE,
    /**
     * 模糊查询
     */
    LIKE,
    LIKE_LEFT,
    /**
     * 右模糊查询
     */
    LIKE_RIGHT,
    /**
     * 在...内
     */
    IN,
    /**
     * 不在...内
     */
    NOT_IN,
    /**
     * 在...之间
     */
    BETWEEN,
    /**
     * 不在...之间
     */
    NOT_BETWEEN
}
