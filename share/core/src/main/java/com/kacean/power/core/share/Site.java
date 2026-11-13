package com.kacean.power.core.share;

import java.util.Objects;

/**
 * 场站或设施位置信息。
 * <p>不包含独立身份标识和层级关系，按场站类型、名称和内部编码判断相等。</p>
 *
 * @author hirong
 *
 * @param siteType       场站类型
 * @param bizCode        场站业务编码，可为 {@code null}
 * @param entitySnapshot 实体快照，可为 {@code null}
 *
 */
public record Site(SiteType siteType, String bizCode, EntitySnapshot entitySnapshot) {

}
