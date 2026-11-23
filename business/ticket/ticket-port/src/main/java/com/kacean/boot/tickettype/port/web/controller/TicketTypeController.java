package com.kacean.boot.tickettype.port.web.controller;

import com.kacean.boot.tickettype.port.infra.curd.TicketTypeCurd;
import com.kacean.boot.tickettype.port.infra.entity.TicketTypeEntity;
import com.kacean.boot.tickettype.port.web.dto.TicketTypeDTO;
import com.kacean.boot.common.web.controller.CurdController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 票种类型表控制器。
 *
 * @author kacean-agent
 */
@RestController
@RequestMapping("/ticket-type")
public class TicketTypeController extends CurdController<TicketTypeDTO, TicketTypeEntity, TicketTypeCurd> {
}
