package com.kacean.boot.common.web.response;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 统一响应体测试，固定不可变结构、响应码规则与参数校验响应码契约。
 *
 * @author kacean-agent
 */
class ResultTest {

    @Test
    void 响应体是不可变record_仅包含三个核心字段() {
        assertTrue(Result.class.isRecord());
        assertArrayEquals(new String[]{"code", "message", "data"}, Arrays.stream(Result.class.getRecordComponents())
                .map(component -> component.getAccessor().getName())
                .toArray());
    }

    @Test
    void 成功响应_统一成功码并支持有无数据() {
        Result<String> withData = Result.success("内容");
        Result<Void> withoutData = Result.success();

        assertAll(
                () -> assertEquals(Result.SUCCESS_CODE, withData.code()),
                () -> assertEquals("success", withData.message()),
                () -> assertEquals("内容", withData.data()),
                () -> assertEquals(Result.SUCCESS_CODE, withoutData.code()),
                () -> assertEquals("success", withoutData.message()),
                () -> assertNull(withoutData.data()));
    }

    @Test
    void 失败响应_保留业务错误码消息和数据() {
        Result<String> result = Result.failure("TICKET_TYPE_DISABLED", "票种已停用", "ticket-type-1");

        assertAll(
                () -> assertEquals("TICKET_TYPE_DISABLED", result.code()),
                () -> assertEquals("票种已停用", result.message()),
                () -> assertEquals("ticket-type-1", result.data()));
    }

    @Test
    void 参数校验失败_使用统一响应码并保留错误详情() {
        Result<String> result = Result.failure(
                Result.VALIDATION_ERROR_CODE, "参数校验失败", "name 不能为空");

        assertAll(
                () -> assertEquals("VALIDATION_ERROR", result.code()),
                () -> assertEquals("参数校验失败", result.message()),
                () -> assertEquals("name 不能为空", result.data()));
    }

    @Test
    void 响应码和消息不符合规则_拒绝创建() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> Result.failure(Result.SUCCESS_CODE, "错误码冲突", null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> Result.failure(" 0 ", "错误码冲突", null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> Result.failure(" ", "错误码为空", null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> Result.failure("BIZ_ERROR", " ", null)));
    }
}
