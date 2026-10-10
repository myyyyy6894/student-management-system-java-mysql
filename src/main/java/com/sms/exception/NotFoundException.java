package com.sms.exception;

/**
 * 资源不存在异常。
 * <p>用于查询或操作不存在的学生、课程、成绩记录。对应业务状态码 404（NOT_FOUND）。</p>
 */
public class NotFoundException extends BusinessException {

    /**
     * 创建资源不存在异常。
     *
     * @param message 提示信息
     */
    public NotFoundException(String message) {
        // 资源不存在的业务状态码固定为 404。
        super(404, message);
    }
}
