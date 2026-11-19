package com.kacean.power.common.app.identity;

/**
 * 实体唯一标识：主键 {@code id} 与外部编号 {@code code} 必须且只能提供一个。
 *
 * @author kacean-agent
 * @param id 主键，与 {@code code} 二选一
 * @param code 外部编号，提供时不得为空白，与 {@code id} 二选一
 */
public record ID(Long id, String code) {

    public ID {
        if ((id == null) == (code == null)) {
            throw new IllegalArgumentException("id 与 code 必须且只能提供一个");
        }
        if (code != null && code.isBlank()) {
            throw new IllegalArgumentException("code 不得为空白");
        }
    }
}
