package com.sms.exception;

/**
 * 业务异常基类。
 * <p>携带统一的业务状态码 code，Controller 层据此决定 HTTP 状态码与响应体 code，
 * 从而把「参数错误(400)」「资源不存在(404)」「业务冲突(409)」区分开。</p>
 * <p>本类继承 {@link IllegalArgumentException}，一方面语义上都属于「调用方输入不合法」，
 * 另一方面兼容既有的业务校验约定，避免调用方需要修改捕获类型。</p>
 */
public class BusinessException extends IllegalArgumentException {

    // 业务状态码，例如 400、404、409。
    private final int code;

    /**
     * 创建带业务状态码的业务异常。
     *
     * @param code    业务状态码
     * @param message 提示信息
     */
    public BusinessException(int code, String message) {
        // 提示信息交给父类保存，业务状态码由本类保存。
        super(message);

        // 保存业务状态码。
        this.code = code;
    }

    /**
     * 获取业务状态码。
     *
     * @return 业务状态码
     */
    public int getCode() {
        // 返回业务状态码。
        return code;
    }
}
