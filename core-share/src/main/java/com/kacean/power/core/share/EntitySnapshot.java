package com.kacean.power.core.share;

import java.util.Objects;

/**
 * 跨系统冗余快照，用于在引用方保留名称、内部系统编码和 ID。
 * <p>快照是辅助信息，不要求与引用的主标识相同，也不作为引用身份的判断依据。</p>
 *
 * @author hirong
 * @param id 冗余 ID
 * @param internalCode 内部系统编码
 * @param name 名称
 */
public record EntitySnapshot(String id, String internalCode, String name, String refType) {

    public EntitySnapshot {
        id = requireText(id, "id");
        internalCode = requireText(internalCode, "internalCode");
        name = requireText(name, "name");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " 不能为空");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 不能为空白");
        }
        return value;
    }
}
