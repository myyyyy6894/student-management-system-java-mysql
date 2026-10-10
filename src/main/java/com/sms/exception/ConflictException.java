package com.sms.exception;

/**
 * 业务冲突异常。
 * <p>用于唯一性冲突、状态冲突等场景，例如学号重复、同一学生重复录入同一门课程成绩。
 * 对应业务状态码 409（CONFLICT）。</p>
 */
public class ConflictException extends BusinessException {

    /**
     * 创建业务冲突异常。
     *
     * @param message 冲突提示信息
     */
    public ConflictException(String message) {
        // 冲突场景的业务状态码固定为 409。
        super(409, message);
    }
}
