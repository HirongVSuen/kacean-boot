package com.kacean.boot.common.app.identity;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * 实体唯一标识集合：{@code ids} 与 {@code extendNo} 是独立的查询条件，批量查询和删除时取并集，不按位置配对。
 * {@code null} 列表归一为空列表；各列表内部按首次出现顺序去重，元素不得为 {@code null}，外部编号不得为空白。
 * 空集合合法；去重后的两类标识总数不得超过 {@link #MAX_BATCH_SIZE}。
 *
 * @author kacean-agent
 * @param ids 主键集合，可为 {@code null}
 * @param extendNo 外部编号集合，可为 {@code null}
 */
public record IDCollection(List<Long> ids, List<String> extendNo) {

    /**
     * 单次批量操作允许的最大标识数，按去重后的主键与外部编号总数计算。
     */
    public static final int MAX_BATCH_SIZE = 1000;

    public IDCollection {
        ids = normalizeIds(ids);
        extendNo = normalizeCodes(extendNo);
        if (ids.size() + extendNo.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException(
                    "批量标识总数不能超过 " + MAX_BATCH_SIZE + "，当前值: " + (ids.size() + extendNo.size()));
        }
    }

    /**
     * 返回去重后的主键与外部编号总数。
     *
     * @return 批量标识总数
     */
    public int size() {
        return ids.size() + extendNo.size();
    }

    /**
     * 判断是否不包含任何标识。
     *
     * @return 两类标识均为空时返回 {@code true}
     */
    public boolean isEmpty() {
        return isIdsEmpty() && isExternalNoEmpty();
    }

    /**
     * 判断主键集合是否不包含任何标识。
     *
     * @return 仅当 {@code ids} 为空时返回 {@code true}
     */
    public boolean isIdsEmpty() {
        return ids == null || ids.isEmpty();
    }

    /**
     * 判断外部编号集合是否不包含任何标识。
     *
     * @return 仅当 {@code extendNo} 为空时返回 {@code true}
     */
    public boolean isExternalNoEmpty() {
        return extendNo == null || extendNo.isEmpty();
    }
    private static List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id == null) {
                throw new IllegalArgumentException("ids 不得包含 null 元素");
            }
            uniqueIds.add(id);
        }
        return List.copyOf(uniqueIds);
    }

    private static List<String> normalizeCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> uniqueCodes = new LinkedHashSet<>();
        for (String code : codes) {
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("extendNo 不得包含 null 或空白元素");
            }
            uniqueCodes.add(code);
        }
        return List.copyOf(uniqueCodes);
    }
}
