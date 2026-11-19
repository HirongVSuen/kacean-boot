package com.kacean.power.tickettype.port.infra.curd;

import com.kacean.power.tickettype.port.infra.dao.TicketTypeDao;
import com.kacean.power.tickettype.port.infra.entity.TicketTypeEntity;
import com.kacean.power.common.infra.curd.BaseCurd;
import org.springframework.stereotype.Component;

/**
 * 票种类型表通用读写用例。
 *
 * @author kacean-agent
 */
@Component
public class TicketTypeCurd extends BaseCurd<TicketTypeEntity, TicketTypeDao> {
}
