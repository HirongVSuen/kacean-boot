package com.kacean.boot.datadict.port.web.assembler;

import com.kacean.boot.datadict.port.infra.entity.DataDictValueEntity;
import com.kacean.boot.datadict.port.web.dto.DataDictValueDTO;
import com.kacean.boot.common.web.assembler.CurdAssembler;
import org.mapstruct.Mapper;

/**
 * 数据字典值表DTO 与持久化对象转换器。
 *
 * @author kacean-agent
 */
@Mapper(componentModel = "spring")
public interface DataDictValueAssembler extends CurdAssembler<DataDictValueDTO, DataDictValueEntity> {
}
