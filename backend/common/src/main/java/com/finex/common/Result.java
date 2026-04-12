package com.finex.common;

import lombok.Data;

import java.io.Serializable;

// 这里定义 Result 的统一返回结构。
// Controller 通常会用它把成功或失败结果包装后返回给前端。
// 如果改错，最容易影响整体接口的统一判断逻辑。

/**
 * 这是 Result 通用返回对象。
 * 它统一封装 code、message、data、timestamp 等字段。
 * 前后端通常依赖这个结构来解析接口结果。
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务状态码。
     */
    private Integer code;

    /**
     * 返回提示信息。
     */
    private String message;

    /**
     * 具体业务数据。
     */
    private T data;

    /**
     * 当前返回的时间戳。
     */
    private Long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 返回不带数据的成功结果。
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    /**
     * 返回带数据的成功结果。
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 返回带自定义提示语的成功结果。
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /**
     * 返回默认失败结果。
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    /**
     * 返回带自定义 code 的失败结果。
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 返回参数错误结果。
     */
    public static <T> Result<T> badRequest(String message) {
        return new Result<>(400, message, null);
    }

    /**
     * 返回未授权结果。
     */
    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(401, message, null);
    }

    /**
     * 返回禁止访问结果。
     */
    public static <T> Result<T> forbidden(String message) {
        return new Result<>(403, message, null);
    }

    /**
     * 返回资源不存在结果。
     */
    public static <T> Result<T> notFound(String message) {
        return new Result<>(404, message, null);
    }
}
