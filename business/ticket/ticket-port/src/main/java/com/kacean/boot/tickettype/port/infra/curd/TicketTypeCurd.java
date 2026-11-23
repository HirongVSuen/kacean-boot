package com.kacean.boot.tickettype.port.infra.curd;

import com.kacean.boot.tickettype.port.infra.dao.TicketTypeDao;
import com.kacean.boot.tickettype.port.infra.entity.TicketTypeEntity;
import com.kacean.boot.common.infra.curd.BaseCurd;
import org.springframework.stereotype.Component;

/**
 * 票种类型表通用读写用例。
 *
 * @author kacean-agent
 */
@Component
public class TicketTypeCurd extends BaseCurd<TicketTypeEntity, TicketTypeDao> {
}
