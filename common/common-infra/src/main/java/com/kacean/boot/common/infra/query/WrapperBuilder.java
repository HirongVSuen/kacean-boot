package com.kacean.boot.common.infra.query;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kacean.boot.common.app.query.Query;
import com.kacean.boot.common.app.query.Sort;
import com.kacean.boot.common.app.query.SortDirection;
import com.kacean.boot.common.infra.entity.BaseEntity;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 通用 Wrapper 构建器：承载当前 QP 与 PO 类型，配合 DAO 模板方法分步构建查询。
 * DAO 可显式声明 SELECT、特殊 WHERE 和固定排序；未声明的 QP 非空字段默认按 EQ 处理，
 * 公共关键字使用括号包围的多列 OR，动态排序通过 PO 持久化字段解析防止任意列注入。
 *
 * @author kacean-agent
 * @param <E> 持久化对象类型
 */
public final class WrapperBuilder<E extends BaseEntity> {

    private static final ClassValue<List<Field>> QUERY_FIELDS = new ClassValue<>() {
        @Override
        protected List<Field> computeValue(Class<?> type) {
            List<Field> fields = new ArrayList<>();
            for (Class<?> current = type; current != null && current != Query.class && current != Object.class;
                    current = current.getSuperclass()) {
                for (Field field : current.getDeclaredFields()) {
                    if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    if (!field.trySetAccessible()) {
                        throw new IllegalStateException("无法访问查询参数字段: " + field);
                    }
                    fields.add(field);
                }
            }
            return List.copyOf(fields);
        }
    };

    private final Query query;
    private final Class<E> entityClass;
    private final QueryWrapper<E> wrapper;
    private final List<String> selectColumns = new ArrayList<>();
    private final Set<String> processedFields = new HashSet<>();
    private final List<String> keywordColumns = new ArrayList<>();
    private final List<Sort> defaultSorts = new ArrayList<>();
    private boolean keywordProcessed;
    private boolean orderProcessed;

    /**
     * 创建携带当前业务 QP 与 PO 类型的构建器，通常由 BaseDao 调用。
     *
     * @param query 业务查询参数，非空
     * @param entityClass 持久化对象类型，非空
     */
    public WrapperBuilder(Query query, Class<E> entityClass) {
        this.query = Objects.requireNonNull(query, "查询参数 query 不能为空");
        this.entityClass = Objects.requireNonNull(entityClass, "持久化类型 entityClass 不能为空");
        this.wrapper = Wrappers.query();
    }

    /**
     * 获取当前业务 QP。
     *
     * @return 业务查询参数
     */
    public Query query() {
        return query;
    }

    /**
     * 按业务 DAO 期望的 QP 类型获取查询参数，类型不匹配时拒绝构建。
     *
     * @param queryType 业务 QP 类型，非空
     * @return 类型安全的业务查询参数
     * @param <Q> 业务查询参数类型
     */
    public <Q extends Query> Q query(Class<Q> queryType) {
        Objects.requireNonNull(queryType, "查询参数类型 queryType 不能为空");
        if (!queryType.isInstance(query)) {
            throw new IllegalArgumentException("查询参数类型与 DAO 查询方法不匹配，期望 " + queryType.getName()
                    + "，实际 " + query.getClass().getName());
        }
        return queryType.cast(query);
    }

    /**
     * 声明 SELECT 返回的 PO 属性，可多次调用追加。
     *
     * @param fields PO Java 属性名
     * @return 当前构建器
     */
    public WrapperBuilder<E> select(String... fields) {
        Objects.requireNonNull(fields, "返回字段 fields 不能为空");
        for (String field : fields) {
            selectColumns.add(ColumnResolver.resolve(entityClass, field));
        }
        return this;
    }

    /**
     * 声明 QP 公共关键字匹配的 PO 属性，多列使用括号包围的 OR 条件。
     *
     * @param fields PO Java 属性名
     * @return 当前构建器
     */
    public WrapperBuilder<E> keywordLike(String... fields) {
        Objects.requireNonNull(fields, "关键字字段 fields 不能为空");
        for (String field : fields) {
            keywordColumns.add(ColumnResolver.resolve(entityClass, field));
        }
        return this;
    }

    /**
     * 追加固定排序，在客户端动态排序之后作为次级排序。
     *
     * @param field PO Java 属性名
     * @param direction 排序方向，非空
     * @return 当前构建器
     */
    public WrapperBuilder<E> addOrder(String field, SortDirection direction) {
        defaultSorts.add(new Sort(field, direction));
        return this;
    }

    /**
     * 声明特殊 WHERE 条件，并将同名 QP 字段标记为已处理，避免默认字段扫描重复添加。
     *
     * @param field QP 与 PO 共有的 Java 属性名
     * @param operator 查询操作符，非空
     * @param value 条件值，空值时忽略
     * @return 当前构建器
     */
    public WrapperBuilder<E> addWhere(String field, Operator operator, Object value) {
        requireText(field, "查询字段 field 不能为空或空白");
        Objects.requireNonNull(operator, "查询操作符 operator 不能为空");
        processedFields.add(field);
        if (!hasValue(value)) {
            return this;
        }
        String column = ColumnResolver.resolve(entityClass, field);
        applyCondition(wrapper, operator, column, value);
        return this;
    }

    /**
     * 标记已由特殊条件处理的 QP 字段，用于别名映射或自定义条件。
     *
     * @param fields 不再参与默认字段处理的 QP 属性名
     * @return 当前构建器
     */
    public WrapperBuilder<E> markProcessed(String... fields) {
        Objects.requireNonNull(fields, "已处理字段 fields 不能为空");
        for (String field : fields) {
            requireText(field, "已处理字段不能包含 null 或空白");
            processedFields.add(field);
        }
        return this;
    }

    public WrapperBuilder<E> eq(String field, Object value) {
        return addWhere(field, Operator.EQ, value);
    }

    public WrapperBuilder<E> ne(String field, Object value) {
        return addWhere(field, Operator.NE, value);
    }

    public WrapperBuilder<E> gt(String field, Object value) {
        return addWhere(field, Operator.GT, value);
    }

    public WrapperBuilder<E> ge(String field, Object value) {
        return addWhere(field, Operator.GE, value);
    }

    public WrapperBuilder<E> lt(String field, Object value) {
        return addWhere(field, Operator.LT, value);
    }

    public WrapperBuilder<E> le(String field, Object value) {
        return addWhere(field, Operator.LE, value);
    }

    public WrapperBuilder<E> like(String field, Object value) {
        return addWhere(field, Operator.LIKE, value);
    }

    public WrapperBuilder<E> likeLeft(String field, Object value) {
        return addWhere(field, Operator.LIKE_LEFT, value);
    }

    public WrapperBuilder<E> likeRight(String field, Object value) {
        return addWhere(field, Operator.LIKE_RIGHT, value);
    }

    /**
     * 声明集合 IN 条件，空集合不会生成永假的 IN 片段。
     *
     * @param field QP 与 PO 共有的 Java 属性名
     * @param value 条件值，空集合时忽略
     * @return 当前构建器
     */
    public WrapperBuilder<E> in(String field, Collection<?> value) {
        return addWhere(field, Operator.IN, value);
    }

    /**
     * 添加标准字段条件无法表达的自定义条件；相关 QP 字段应通过 markProcessed 排除默认处理。
     *
     * @param customizer 自定义 Wrapper 条件
     * @return 当前构建器
     */
    public WrapperBuilder<E> where(Consumer<QueryWrapper<E>> customizer) {
        Objects.requireNonNull(customizer, "自定义条件 customizer 不能为空").accept(wrapper);
        return this;
    }

    /**
     * 默认处理未声明的 QP 字段：跳过公共 Query 字段、static 字段和已处理字段，其余非空值使用 EQ。
     *
     * @return 当前构建器
     */
    public WrapperBuilder<E> processDefaultFields() {
        for (Field field : QUERY_FIELDS.get(query.getClass())) {
            if (processedFields.contains(field.getName())) {
                continue;
            }
            addWhere(field.getName(), getDefaultOperator(field), readValue(field));
        }
        return this;
    }

    /**
     * 默认处理 QP 公共关键字，重复调用不会重复追加条件。
     *
     * @return 当前构建器
     */
    public WrapperBuilder<E> processKeyword() {
        if (keywordProcessed) {
            return this;
        }
        keywordProcessed = true;
        String keyword = query.keyword();
        if (keyword == null || keyword.isBlank() || keywordColumns.isEmpty()) {
            return this;
        }
        wrapper.and(nested -> {
            for (int index = 0; index < keywordColumns.size(); index++) {
                if (index > 0) {
                    nested.or();
                }
                nested.like(keywordColumns.get(index), keyword);
            }
        });
        return this;
    }

    /**
     * 默认处理客户端动态排序，再追加 DAO 声明的固定排序；重复调用不会重复追加排序。
     *
     * @return 当前构建器
     */
    public WrapperBuilder<E> processOrder() {
        if (orderProcessed) {
            return this;
        }
        orderProcessed = true;
        for (Sort sort : query.sorts()) {
            applySort(sort);
        }
        for (Sort sort : defaultSorts) {
            applySort(sort);
        }
        return this;
    }

    /**
     * 应用已声明的 SELECT 并返回当前 Wrapper；每个查询应使用独立 Builder。
     *
     * @return 查询条件
     */
    public QueryWrapper<E> build() {
        if (!selectColumns.isEmpty()) {
            wrapper.select(selectColumns.toArray(String[]::new));
        }
        return wrapper;
    }

    private Operator getDefaultOperator(Field field) {
        return Operator.EQ;
    }

    private Object readValue(Field field) {
        try {
            return field.get(query);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("无法读取查询参数字段: " + field.getName(), exception);
        }
    }

    private void applySort(Sort sort) {
        String column = ColumnResolver.resolve(entityClass, sort.field());
        switch (sort.direction()) {
            case ASC -> wrapper.orderByAsc(column);
            case DESC -> wrapper.orderByDesc(column);
            default -> throw new IllegalArgumentException("不支持的排序方向: " + sort.direction());
        }
    }

    private static <E extends BaseEntity> void applyCondition(
            QueryWrapper<E> wrapper, Operator operator, String column, Object value) {
        switch (operator) {
            case EQ -> wrapper.eq(column, value);
            case NE -> wrapper.ne(column, value);
            case GT -> wrapper.gt(column, value);
            case GE -> wrapper.ge(column, value);
            case LT -> wrapper.lt(column, value);
            case LE -> wrapper.le(column, value);
            case LIKE -> wrapper.like(column, value);
            case LIKE_LEFT -> wrapper.likeLeft(column, value);
            case LIKE_RIGHT -> wrapper.likeRight(column, value);
            case IN -> wrapper.in(column, collectionValues(operator, value));
            case NOT_IN -> wrapper.notIn(column, collectionValues(operator, value));
            case BETWEEN -> {
                Object[] range = rangeValues(operator, value);
                wrapper.between(column, range[0], range[1]);
            }
            case NOT_BETWEEN -> {
                Object[] range = rangeValues(operator, value);
                wrapper.notBetween(column, range[0], range[1]);
            }
            default -> throw new IllegalArgumentException("不支持的查询操作符: " + operator);
        }
    }

    private static Collection<?> collectionValues(Operator operator, Object value) {
        if (value instanceof Collection<?> collection) {
            return collection;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> values = new ArrayList<>(length);
            for (int index = 0; index < length; index++) {
                values.add(Array.get(value, index));
            }
            return values;
        }
        throw new IllegalArgumentException(operator + " 条件值必须是集合或数组");
    }

    private static Object[] rangeValues(Operator operator, Object value) {
        Object[] range;
        if (value instanceof Collection<?> collection) {
            range = collection.toArray();
        } else if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            range = new Object[length];
            for (int index = 0; index < length; index++) {
                range[index] = Array.get(value, index);
            }
        } else {
            throw new IllegalArgumentException(operator + " 条件值必须是包含两个元素的集合或数组");
        }
        if (range.length != 2 || range[0] == null || range[1] == null) {
            throw new IllegalArgumentException(operator + " 条件值必须包含两个非空边界");
        }
        return range;
    }

    private static boolean hasValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String text) {
            return !text.isBlank();
        }
        if (value instanceof Collection<?> collection) {
            return !collection.isEmpty();
        }
        return !value.getClass().isArray() || Array.getLength(value) > 0;
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
