package com.sms.model.entity;

// 导入精确小数类型，用于映射数据库中的 credit DECIMAL(3,1)。
import java.math.BigDecimal;
// 导入日期时间类，用于映射数据库中的 created_at / updated_at DATETIME 类型。
import java.time.LocalDateTime;

/**
 * 课程物理实体类。
 * <p>对应数据库表 course，字段类型按照数据库列类型映射。</p>
 */
public class Course {

    // 课程物理主键 ID，对应 course.id BIGINT UNSIGNED。
    private Long id;

    // 课程编号，对应 course.course_no VARCHAR(32)，业务上全局唯一。
    private String courseNo;

    // 课程名称，对应 course.name VARCHAR(100)。
    private String name;

    // 课程学分，对应 course.credit DECIMAL(3,1)，使用 BigDecimal 避免精度丢失。
    private BigDecimal credit;

    // 任课教师姓名，对应 course.teacher VARCHAR(50)。
    private String teacher;

    // 记录创建时间，对应 course.created_at DATETIME。
    private LocalDateTime createdAt;

    // 记录最新修改时间，对应 course.updated_at DATETIME。
    private LocalDateTime updatedAt;

    /**
     * 无参构造器，方便 JDBC 映射、JSON 反序列化和测试代码先创建空对象。
     */
    public Course() {
    }

    /**
     * 全参构造器，用于一次性构建完整课程实体。
     *
     * @param id        课程物理主键 ID
     * @param courseNo  课程编号
     * @param name      课程名称
     * @param credit    课程学分
     * @param teacher   任课教师
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     */
    public Course(Long id,
                  String courseNo,
                  String name,
                  BigDecimal credit,
                  String teacher,
                  LocalDateTime createdAt,
                  LocalDateTime updatedAt) {
        // 将参数 id 赋值给成员变量 id。
        this.id = id;
        // 将参数 courseNo 赋值给成员变量 courseNo。
        this.courseNo = courseNo;
        // 将参数 name 赋值给成员变量 name。
        this.name = name;
        // 将参数 credit 赋值给成员变量 credit。
        this.credit = credit;
        // 将参数 teacher 赋值给成员变量 teacher。
        this.teacher = teacher;
        // 将参数 createdAt 赋值给成员变量 createdAt。
        this.createdAt = createdAt;
        // 将参数 updatedAt 赋值给成员变量 updatedAt。
        this.updatedAt = updatedAt;
    }

    /**
     * 获取课程物理主键 ID。
     *
     * @return 课程物理主键 ID
     */
    public Long getId() {
        // 返回当前对象的 id。
        return id;
    }

    /**
     * 设置课程物理主键 ID。
     *
     * @param id 课程物理主键 ID
     */
    public void setId(Long id) {
        // 更新当前对象的 id。
        this.id = id;
    }

    /**
     * 获取课程编号。
     *
     * @return 课程编号
     */
    public String getCourseNo() {
        // 返回当前对象的 courseNo。
        return courseNo;
    }

    /**
     * 设置课程编号。
     *
     * @param courseNo 课程编号
     */
    public void setCourseNo(String courseNo) {
        // 更新当前对象的 courseNo。
        this.courseNo = courseNo;
    }

    /**
     * 获取课程名称。
     *
     * @return 课程名称
     */
    public String getName() {
        // 返回当前对象的 name。
        return name;
    }

    /**
     * 设置课程名称。
     *
     * @param name 课程名称
     */
    public void setName(String name) {
        // 更新当前对象的 name。
        this.name = name;
    }

    /**
     * 获取课程学分。
     *
     * @return 课程学分
     */
    public BigDecimal getCredit() {
        // 返回当前对象的 credit。
        return credit;
    }

    /**
     * 设置课程学分。
     *
     * @param credit 课程学分
     */
    public void setCredit(BigDecimal credit) {
        // 更新当前对象的 credit。
        this.credit = credit;
    }

    /**
     * 获取任课教师姓名。
     *
     * @return 任课教师姓名
     */
    public String getTeacher() {
        // 返回当前对象的 teacher。
        return teacher;
    }

    /**
     * 设置任课教师姓名。
     *
     * @param teacher 任课教师姓名
     */
    public void setTeacher(String teacher) {
        // 更新当前对象的 teacher。
        this.teacher = teacher;
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
