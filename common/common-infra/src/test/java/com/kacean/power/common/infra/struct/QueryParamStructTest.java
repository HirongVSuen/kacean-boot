package com.kacean.power.common.infra.struct;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kacean.power.common.app.query.Query;
import com.kacean.power.common.infra.po.BaseEntity;
import com.kacean.power.common.infra.query.QueryConditionBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 查询参数转换契约测试：验证业务查询参数经实现类转换为带条件的查询条件构建器。
 *
 * @author kacean-agent
 */
class QueryParamStructTest {

    @Test
    void 业务查询参数_经Struct转换为带条件的构建器() {
        TicketQueryParamStruct struct = new TicketQueryParamStruct();

        QueryConditionBuilder<TicketEntity> builder =
                struct.toConditionBuilder(new TicketQueryParam("EXT-1", 2));
        QueryWrapper<TicketEntity> wrapper = builder.toWrapper();

        assertAll(
                () -> assertEquals("id,external_no", wrapper.getSqlSelect().replace(" ", "")),
                () -> assertTrue(wrapper.getSqlSegment().contains("external_no =")),
                () -> assertTrue(wrapper.getSqlSegment().contains("version >=")),
                () -> assertTrue(wrapper.getSqlSegment().contains("ORDER BY create_at DESC")),
                () -> assertTrue(wrapper.getParamNameValuePairs().values().containsAll(List.of("EXT-1", 2))));
    }

    private record TicketQueryParam(String externalNo, Integer minVersion) implements Query {
    }

    private static final class TicketQueryParamStruct implements QueryParamStruct<TicketQueryParam, TicketEntity> {

        @Override
        public QueryConditionBuilder<TicketEntity> toConditionBuilder(TicketQueryParam queryParam) {
            Objects.requireNonNull(queryParam, "查询参数 queryParam 不能为空");
            return new QueryConditionBuilder<>() {
                @Override
                protected void addProperty() {
                    wrapper().select("id", "external_no");
                }

                @Override
                protected void addWhere() {
                    wrapper().eq(queryParam.externalNo() != null, "external_no", queryParam.externalNo())
                            .ge(queryParam.minVersion() != null, "version", queryParam.minVersion());
                }

                @Override
                protected void addOrder() {
                    wrapper().orderByDesc("create_at");
                }
            };
        }
    }

    private static final class TicketEntity extends BaseEntity {

        private static final long serialVersionUID = 1L;
    }
}
