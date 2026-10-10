package com.sms.model.entity;

// 导入精确小数类型，用于映射数据库中的 score DECIMAL(5,2)。
import java.math.BigDecimal;
// 导入日期类，用于映射数据库中的 exam_date DATE。
import java.time.LocalDate;
// 导入日期时间类，用于映射数据库中的 created_at / updated_at DATETIME 类型。
import java.time.LocalDateTime;

/**
 * 学生成绩物理实体类。
 * <p>对应数据库表 score，记录学生和课程之间的成绩关系。</p>
 */
public class Score {

    // 成绩物理主键 ID，对应 score.id BIGINT UNSIGNED。
    private Long id;

    // 学生物理主键 ID，对应 score.student_id BIGINT UNSIGNED，关联 student.id。
    private Long studentId;

    // 课程物理主键 ID，对应 score.course_id BIGINT UNSIGNED，关联 course.id。
    private Long courseId;

    // 考试成绩，对应 score.score DECIMAL(5,2)，业务范围 0.00 ~ 100.00。
    private BigDecimal score;

    // 考核日期，对应 score.exam_date DATE。
    private LocalDate examDate;

    // 成绩备注，对应 score.remarks VARCHAR(255)。
    private String remarks;

    // 记录创建时间，对应 score.created_at DATETIME。
    private LocalDateTime createdAt;

    // 记录最新修改时间，对应 score.updated_at DATETIME。
    private LocalDateTime updatedAt;

    /**
     * 无参构造器，方便 JDBC 映射、JSON 反序列化和测试代码先创建空对象。
     */
    public Score() {
    }

    /**
     * 全参构造器，用于一次性构建完整成绩实体。
     *
     * @param id        成绩物理主键 ID
     * @param studentId 学生物理主键 ID
     * @param courseId  课程物理主键 ID
     * @param score     考试成绩
     * @param examDate  考核日期
     * @param remarks   成绩备注
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     */
    public Score(Long id,
                 Long studentId,
                 Long courseId,
                 BigDecimal score,
                 LocalDate examDate,
                 String remarks,
                 LocalDateTime createdAt,
                 LocalDateTime updatedAt) {
        // 将参数 id 赋值给成员变量 id。
        this.id = id;
        // 将参数 studentId 赋值给成员变量 studentId。
        this.studentId = studentId;
        // 将参数 courseId 赋值给成员变量 courseId。
        this.courseId = courseId;
        // 将参数 score 赋值给成员变量 score。
        this.score = score;
        // 将参数 examDate 赋值给成员变量 examDate。
        this.examDate = examDate;
        // 将参数 remarks 赋值给成员变量 remarks。
        this.remarks = remarks;
        // 将参数 createdAt 赋值给成员变量 createdAt。
        this.createdAt = createdAt;
        // 将参数 updatedAt 赋值给成员变量 updatedAt。
        this.updatedAt = updatedAt;
    }

    /**
     * 获取成绩物理主键 ID。
     *
     * @return 成绩物理主键 ID
     */
    public Long getId() {
        // 返回当前对象的 id。
        return id;
    }

    /**
     * 设置成绩物理主键 ID。
     *
     * @param id 成绩物理主键 ID
     */
    public void setId(Long id) {
        // 更新当前对象的 id。
        this.id = id;
    }

    /**
     * 获取学生物理主键 ID。
     *
     * @return 学生物理主键 ID
     */
    public Long getStudentId() {
        // 返回当前对象的 studentId。
        return studentId;
    }

    /**
     * 设置学生物理主键 ID。
     *
     * @param studentId 学生物理主键 ID
     */
    public void setStudentId(Long studentId) {
        // 更新当前对象的 studentId。
        this.studentId = studentId;
    }

    /**
     * 获取课程物理主键 ID。
     *
     * @return 课程物理主键 ID
     */
    public Long getCourseId() {
        // 返回当前对象的 courseId。
        return courseId;
    }

    /**
     * 设置课程物理主键 ID。
     *
     * @param courseId 课程物理主键 ID
     */
    public void setCourseId(Long courseId) {
        // 更新当前对象的 courseId。
        this.courseId = courseId;
    }

    /**
     * 获取考试成绩。
     *
     * @return 考试成绩，范围 0.00 ~ 100.00
     */
    public BigDecimal getScore() {
        // 返回当前对象的 score。
        return score;
    }

    /**
     * 设置考试成绩。
     *
     * @param score 考试成绩，范围 0.00 ~ 100.00
     */
    public void setScore(BigDecimal score) {
        // 更新当前对象的 score。
        this.score = score;
    }

    /**
     * 获取考核日期。
     *
     * @return 考核日期
     */
    public LocalDate getExamDate() {
        // 返回当前对象的 examDate。
        return examDate;
    }

    /**
     * 设置考核日期。
     *
     * @param examDate 考核日期
     */
    public void setExamDate(LocalDate examDate) {
        // 更新当前对象的 examDate。
        this.examDate = examDate;
    }

    /**
     * 获取成绩备注。
     *
     * @return 成绩备注
     */
    public String getRemarks() {
        // 返回当前对象的 remarks。
        return remarks;
    }

    /**
     * 设置成绩备注。
     *
     * @param remarks 成绩备注
     */
    public void setRemarks(String remarks) {
        // 更新当前对象的 remarks。
        this.remarks = remarks;
    }

    /**
     * 获取记录创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        // 返回当前对象的 createdAt。
        return createdAt;
    }

    /**
     * 设置记录创建时间。
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        // 更新当前对象的 createdAt。
        this.createdAt = createdAt;
    }

    /**
     * 获取记录最新修改时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdatedAt() {
        // 返回当前对象的 updatedAt。
        return updatedAt;
    }

    /**
     * 设置记录最新修改时间。
     *
     * @param updatedAt 更新时间
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        // 更新当前对象的 updatedAt。
        this.updatedAt = updatedAt;
    }
}
