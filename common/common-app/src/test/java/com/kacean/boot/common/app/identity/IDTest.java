package com.kacean.boot.common.app.identity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 实体唯一标识测试，固定主键与外部编号的二选一及空白校验规则。
 *
 * @author kacean-agent
 */
class IDTest {

    @Test
    void 主键与外部编号必须且只能提供一个() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new ID(null, null)),
                () -> assertThrows(IllegalArgumentException.class, () -> new ID(1L, "EXT-1")));
    }

    @Test
    void 外部编号为空白_拒绝构造() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new ID(null, "")),
                () -> assertThrows(IllegalArgumentException.class, () -> new ID(null, " ")),
                () -> assertThrows(IllegalArgumentException.class, () -> new ID(1L, " ")));
    }

    @Test
    void 仅提供一种有效标识_构造成功() {
        ID byId = new ID(1L, null);
        ID byCode = new ID(null, "EXT-1");

        assertAll(
                () -> assertEquals(1L, byId.id()),
                () -> assertNull(byId.code()),
                () -> assertNull(byCode.id()),
                () -> assertEquals("EXT-1", byCode.code()));
    }
}
