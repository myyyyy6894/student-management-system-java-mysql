package com.sms.model.vo;

/**
 * 统一响应结果类。
 * <p>前端所有接口的返回值都包装成：code + message + data，方便前端统一判断请求结果。</p>
 *
 * @param <T> data 字段要承载的具体数据类型，例如 Student、Course 或 List&lt;Student&gt;
 */
public class CommonResult<T> {

    // 业务状态码：200 表示成功，400/404/409/500 表示不同类型的业务或系统错误。
    private int code;

    // 提示信息，用于告诉前端本次请求成功或失败的原因。
    private String message;

    // 真正的业务数据；如果失败，通常为 null。
    private T data;

    /**
     * 无参构造器。
     * <p>部分 JSON 库反序列化对象时需要一个公共无参构造器。</p>
     */
    public CommonResult() {
    }

    /**
     * 全参构造器，用于手动组装一个完整响应对象。
     *
     * @param code    业务状态码
     * @param message 提示信息
     * @param data    业务数据
     */
    public CommonResult(int code, String message, T data) {
        // 将参数 code 赋值给当前对象的成员变量 code。
        this.code = code;
        // 将参数 message 赋值给当前对象的成员变量 message。
        this.message = message;
        // 将参数 data 赋值给当前对象的成员变量 data。
        this.data = data;
    }

    /**
     * 创建成功响应，使用默认提示“操作成功”。
     *
     * @param data 成功时要返回给前端的数据
     * @param <T>  数据类型
     * @return 统一响应对象
     */
    public static <T> CommonResult<T> success(T data) {
        // 调用另一个重载 success 方法，复用默认成功提示。
        return success("操作成功", data);
    }

    /**
     * 创建成功响应，并允许调用方自定义提示信息。
     *
     * @param message 成功提示，例如“查询成功”“新增成功”
     * @param data    成功时要返回给前端的数据
     * @param <T>     数据类型
     * @return 统一响应对象
     */
    public static <T> CommonResult<T> success(String message, T data) {
        // 成功状态码固定为 200。
        return new CommonResult<>(200, message, data);
    }

    /**
     * 创建失败响应。
     *
     * @param code    业务状态码，例如 400、404、409、500
     * @param message 失败提示，例如“学号已存在”
     * @param <T>     数据类型；失败响应通常没有数据，因此 T 一般会自动推断为 Object
     * @return 统一响应对象
     */
    public static <T> CommonResult<T> error(int code, String message) {
        // 失败时不返回业务数据，第三个参数传 null。
        return new CommonResult<>(code, message, null);
    }

    /**
     * 获取业务状态码。
     *
     * @return 业务状态码
     */
    public int getCode() {
        // 返回当前对象的 code。
        return code;
    }

    /**
     * 设置业务状态码。
     *
     * @param code 业务状态码
     */
    public void setCode(int code) {
        // 更新当前对象的 code。
        this.code = code;
    }

    /**
     * 获取提示信息。
     *
     * @return 提示信息
     */
    public String getMessage() {
        // 返回当前对象的 message。
        return message;
    }

    /**
     * 设置提示信息。
     *
     * @param message 提示信息
     */
    public void setMessage(String message) {
        // 更新当前对象的 message。
        this.message = message;
    }

    /**
     * 获取业务数据。
     *
     * @return 业务数据；失败时通常为 null
     */
    public T getData() {
        // 返回当前对象的 data。
        return data;
    }

    /**
     * 设置业务数据。
     *
     * @param data 业务数据
     */
    public void setData(T data) {
        // 更新当前对象的 data。
        this.data = data;
    }
}
