package com.kacean.boot.datadict.port.web.dto;

import com.kacean.boot.common.web.response.BaseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 数据字典表Web 传输对象。
 *
 * @author kacean-agent
 */
@Getter
@Setter
public class DataDictDTO extends BaseDTO {

    /**
     * 主键ID，外部传入
     */
    @Schema(description = "主键ID，外部传入")
    private Long id;

    /**
     * 外部编号
     */
    @Schema(description = "外部编号")
    private String externalNo;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createAt;

    /**
     * 创建人
     */
    @Schema(description = "创建人")
    private String createBy;

    /**
     * 修改时间
     */
    @Schema(description = "修改时间", example = "2026-01-01 12:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime modifyAt;

    /**
     * 修改人
     */
    @Schema(description = "修改人")
    private String modifyBy;

    /**
     * 乐观锁版本号
     */
    @Schema(description = "乐观锁版本号")
    private Long version;

    /**
     * 字典名
     */
    @Schema(description = "字典名")
    private String dictName;

    /**
     * 是否启用：0-禁用，1-启用
     */
    @Schema(description = "是否启用：0-禁用，1-启用")
    private Byte isEnabled;

    @Override
    public String toString() {
        return "DataDictDTO{" +
            "id = " + id +
            ", externalNo = " + externalNo +
            ", createAt = " + createAt +
            ", createBy = " + createBy +
            ", modifyAt = " + modifyAt +
            ", modifyBy = " + modifyBy +
            ", version = " + version +
            ", dictName = " + dictName +
            ", isEnabled = " + isEnabled +
            "}";
    }
}
