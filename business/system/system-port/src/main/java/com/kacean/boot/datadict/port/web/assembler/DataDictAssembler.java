package com.kacean.boot.datadict.port.web.assembler;

import com.kacean.boot.datadict.port.infra.entity.DataDictEntity;
import com.kacean.boot.datadict.port.web.dto.DataDictDTO;
import com.kacean.boot.common.web.assembler.CurdAssembler;
import org.mapstruct.Mapper;

/**
 * 数据字典表DTO 与持久化对象转换器。
 *
 * @author kacean-agent
 */
@Mapper(componentModel = "spring")
public interface DataDictAssembler extends CurdAssembler<DataDictDTO, DataDictEntity> {
}
