package com.kacean.power.core.share;

/**
 * 设备引用，携带设备主标识以及可选的跨系统冗余快照。
 * <p>标识值按原样保存；引用身份仅由 {@code scheme + value} 决定，快照不参与相等性判断。</p>
 *
 * @author hirong
 * @param scheme 设备标识方案
 * @param value 设备标识值
 * @param snapshot 名称、内部系统编码和 ID 快照，可为 {@code null}
 */
public record DeviceRef(IdentifierScheme scheme, String value, EntitySnapshot snapshot) {
}
