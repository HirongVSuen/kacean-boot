package com.kacean.boot.tickettype.port.infra.dao;

import com.kacean.boot.tickettype.port.infra.entity.TicketTypeEntity;
import com.kacean.boot.tickettype.port.infra.mapper.TicketTypeMapper;
import com.kacean.boot.common.infra.dao.BaseDaoImpl;
import org.springframework.stereotype.Repository;

/**
 * 票种类型表DAO 的 MyBatis-Plus 实现。
 *
 * @author kacean-agent
 */
@Repository
public class TicketTypeDaoImpl extends BaseDaoImpl<TicketTypeMapper, TicketTypeEntity>
        implements TicketTypeDao {
}
