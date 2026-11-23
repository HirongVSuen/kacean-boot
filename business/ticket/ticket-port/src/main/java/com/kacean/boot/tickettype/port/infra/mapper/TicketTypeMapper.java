package com.kacean.boot.tickettype.port.infra.mapper;

import com.kacean.boot.common.infra.mapper.BaseMapper;
import com.kacean.boot.tickettype.port.infra.entity.TicketTypeEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 票种类型表 MyBatis-Plus 数据访问接口。
 *
 * @author kacean-agent
 */
@Mapper
public interface TicketTypeMapper extends BaseMapper<TicketTypeEntity> {

}
