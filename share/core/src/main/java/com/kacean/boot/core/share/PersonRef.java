package com.kacean.boot.core.share;

import java.util.Objects;

/**
 * 人员引用，携带人员主标识、业务角色以及可选的跨系统冗余快照。
 * <p>人员标识按原样保存；引用身份仅由 {@code personId + roleType} 决定，快照不参与相等性判断。</p>
 *
 * @author hirong
 * @param roleType 人员角色
 * @param snapshot 名称、内部系统编码和 ID 快照，可为 {@code null}
 */
public record PersonRef(RoleType roleType, EntitySnapshot snapshot) {

}
