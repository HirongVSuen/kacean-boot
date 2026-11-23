package com.kacean.boot.tickettype.port.web.assembler;

import com.kacean.boot.tickettype.port.infra.entity.TicketTypeEntity;
import com.kacean.boot.tickettype.port.web.dto.TicketTypeDTO;
import com.kacean.boot.common.web.assembler.CurdAssembler;
import org.mapstruct.Mapper;

/**
 * 票种类型表DTO 与持久化对象转换器。
 *
 * @author kacean-agent
 */
@Mapper(componentModel = "spring")
public interface TicketTypeAssembler extends CurdAssembler<TicketTypeDTO, TicketTypeEntity> {
}
