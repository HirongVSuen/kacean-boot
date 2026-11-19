package com.kacean.power.tickettype.port.web.assembler;

import com.kacean.power.tickettype.port.infra.entity.TicketTypeEntity;
import com.kacean.power.tickettype.port.web.dto.TicketTypeDTO;
import com.kacean.power.common.web.assembler.CurdAssembler;
import org.mapstruct.Mapper;

/**
 * 票种类型表DTO 与持久化对象转换器。
 *
 * @author kacean-agent
 */
@Mapper(componentModel = "spring")
public interface TicketTypeAssembler extends CurdAssembler<TicketTypeDTO, TicketTypeEntity> {
}
