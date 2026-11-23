package com.kacean.boot.common.app.identity;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 实体唯一标识集合测试，固定不可变性、去重、空元素及批量大小规则。
 *
 * @author kacean-agent
 */
class IDCollectionTest {

    @Test
    void null列表_归一为空列表且标识集合为空() {
        IDCollection identities = new IDCollection(null, null);

        assertAll(
                () -> assertEquals(List.of(), identities.ids()),
                () -> assertEquals(List.of(), identities.extendNo()),
                () -> assertEquals(0, identities.size()),
                () -> assertTrue(identities.isEmpty()));
    }

    @Test
    void 输入列表随后被修改_不影响已创建的不可变快照() {
        List<Long> ids = new ArrayList<>(List.of(2L, 1L, 2L));
        List<String> codes = new ArrayList<>(List.of("EXT-2", "EXT-1", "EXT-2"));
        IDCollection identities = new IDCollection(ids, codes);

        ids.clear();
        codes.clear();

        assertAll(
                () -> assertEquals(List.of(2L, 1L), identities.ids()),
                () -> assertEquals(List.of("EXT-2", "EXT-1"), identities.extendNo()),
                () -> assertEquals(4, identities.size()),
                () -> assertThrows(UnsupportedOperationException.class, () -> identities.ids().add(3L)),
                () -> assertThrows(UnsupportedOperationException.class, () -> identities.extendNo().add("EXT-3")));
    }

    @Test
    void 集合包含空元素或空白外部编号_拒绝构造() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new IDCollection(Arrays.asList(1L, null), null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new IDCollection(null, Arrays.asList("EXT-1", null))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new IDCollection(null, List.of("EXT-1", " "))));
    }

    @Test
    void 标识总数超过批量上限_拒绝构造() {
        List<Long> maxIds = LongStream.rangeClosed(1, IDCollection.MAX_BATCH_SIZE).boxed().toList();
        IDCollection maxIdentities = new IDCollection(maxIds, null);
        List<Long> tooManyIds = LongStream.rangeClosed(1, IDCollection.MAX_BATCH_SIZE + 1L).boxed().toList();

        assertAll(
                () -> assertEquals(IDCollection.MAX_BATCH_SIZE, maxIdentities.size()),
                () -> assertThrows(IllegalArgumentException.class, () -> new IDCollection(tooManyIds, null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new IDCollection(maxIds, List.of("EXT-1"))));
    }
}
