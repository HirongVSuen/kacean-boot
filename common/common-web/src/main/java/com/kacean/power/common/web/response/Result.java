package com.kacean.power.common.web.response;

/**
 * 统一响应体：成功码固定为 {@code 0}，业务错误码必须非空白且不得复用成功码。
 * 参数校验失败统一使用 {@code VALIDATION_ERROR}，此时 {@code data} 可携带具体错误详情。
 *
 * @author kacean-agent
 * @param code 响应码，非空白
 * @param message 响应说明，非空白
 * @param data 响应数据，无数据时为 {@code null}
 */
public record Result<T>(String code, String message, T data) {

    /** 成功响应码，禁止用于失败响应。 */
    public static final String SUCCESS_CODE = "0";

    /** 参数校验失败响应码，具体错误详情通过 {@code data} 返回。 */
    public static final String VALIDATION_ERROR_CODE = "VALIDATION_ERROR";

    private static final String SUCCESS_MESSAGE = "success";

    public Result {
        code = requireText(code, "code").trim();
        message = requireText(message, "message");
    }

    /**
     * 创建携带数据的成功响应。
     *
     * @param data 响应数据，可为 {@code null}
     * @param <T> 数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

    /**
     * 创建无数据的成功响应。
     *
     * @return {@code data} 为 {@code null} 的成功响应
     */
    public static Result<Void> success() {
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, null);
    }

    /**
     * 创建失败响应，参数校验失败时可传入具体错误详情作为数据。
     *
     * @param code 业务错误码，非空白且不得为 {@value #SUCCESS_CODE}
     * @param message 错误说明，非空白
     * @param data 错误详情，可为 {@code null}
     * @param <T> 错误详情类型
     * @return 失败响应
     */
    public static <T> Result<T> failure(String code, String message, T data) {
        if (code != null && SUCCESS_CODE.equals(code.trim())) {
            throw new IllegalArgumentException("业务错误码不能使用成功码");
        }
        return new Result<>(code, message, data);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value;
    }
}
