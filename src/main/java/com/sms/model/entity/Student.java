package com.sms.model.entity;

// 导入日期类，用于映射数据库中的 birth_date DATE 类型。
import java.time.LocalDate;
// 导入日期时间类，用于映射数据库中的 created_at / updated_at DATETIME 类型。
import java.time.LocalDateTime;
// 导入精确小数类型，虽然学生表本身没有 DECIMAL 字段，但统一实体设计时可以保留一致的建模方式。
import java.math.BigDecimal;

/**
 * 学生物理实体类。
 * <p>对应数据库表 student，字段类型按照数据库列类型映射：</p>
 * <p>BIGINT -&gt; Long；VARCHAR -&gt; String；TINYINT -&gt; Integer；DATE -&gt; LocalDate；DATETIME -&gt; LocalDateTime。</p>
 */
public class Student {

    // 学生物理主键 ID，对应 student.id BIGINT UNSIGNED。
    private Long id;

    // 学号，对应 student.student_no VARCHAR(32)，业务上全局唯一。
    private String studentNo;

    // 学生真实姓名，对应 student.name VARCHAR(50)。
    private String name;

    // 性别，对应 student.gender TINYINT：1-男，2-女，0-未知。
    private Integer gender;

    // 出生日期，对应 student.birth_date DATE。
    private LocalDate birthDate;

    // 联系电话，对应 student.phone VARCHAR(20)。
    private String phone;

    // 电子邮箱，对应 student.email VARCHAR(100)。
    private String email;

    // 所属专业，对应 student.major VARCHAR(100)。
    private String major;

    // 班级名称，对应 student.class_name VARCHAR(50)。
    private String className;

    // 学籍状态，对应 student.status TINYINT：1-在读，2-休学，3-退学，4-毕业。
    private Integer status;

    // 记录创建时间，对应 student.created_at DATETIME。
    private LocalDateTime createdAt;

    // 记录最新修改时间，对应 student.updated_at DATETIME。
    private LocalDateTime updatedAt;

    /**
     * 无参构造器，方便 JDBC 映射、JSON 反序列化和测试代码先创建空对象。
     */
    public Student() {
    }

    /**
     * 全参构造器，用于一次性构建完整学生实体。
     *
     * @param id          学生物理主键 ID
     * @param studentNo   学号
     * @param name        学生姓名
     * @param gender      性别
     * @param birthDate   出生日期
     * @param phone       联系电话
     * @param email       电子邮箱
     * @param major       所属专业
     * @param className   班级名称
     * @param status      学籍状态
     * @param createdAt   创建时间
     * @param updatedAt   更新时间
     */
    public Student(Long id,
                   String studentNo,
                   String name,
                   Integer gender,
                   LocalDate birthDate,
                   String phone,
                   String email,
                   String major,
                   String className,
                   Integer status,
                   LocalDateTime createdAt,
                   LocalDateTime updatedAt) {
        // 将参数 id 赋值给成员变量 id。
        this.id = id;
        // 将参数 studentNo 赋值给成员变量 studentNo。
        this.studentNo = studentNo;
        // 将参数 name 赋值给成员变量 name。
        this.name = name;
        // 将参数 gender 赋值给成员变量 gender。
        this.gender = gender;
        // 将参数 birthDate 赋值给成员变量 birthDate。
        this.birthDate = birthDate;
        // 将参数 phone 赋值给成员变量 phone。
        this.phone = phone;
        // 将参数 email 赋值给成员变量 email。
        this.email = email;
        // 将参数 major 赋值给成员变量 major。
        this.major = major;
        // 将参数 className 赋值给成员变量 className。
        this.className = className;
        // 将参数 status 赋值给成员变量 status。
        this.status = status;
        // 将参数 createdAt 赋值给成员变量 createdAt。
        this.createdAt = createdAt;
        // 将参数 updatedAt 赋值给成员变量 updatedAt。
        this.updatedAt = updatedAt;
    }

    /**
     * 获取学生物理主键 ID。
     *
     * @return 学生物理主键 ID
     */
    public Long getId() {
        // 返回当前对象的 id。
        return id;
    }

    /**
     * 设置学生物理主键 ID。
     *
     * @param id 学生物理主键 ID
     */
    public void setId(Long id) {
        // 更新当前对象的 id。
        this.id = id;
    }

    /**
     * 获取学号。
     *
     * @return 学号
     */
    public String getStudentNo() {
        // 返回当前对象的 studentNo。
        return studentNo;
    }

    /**
     * 设置学号。
     *
     * @param studentNo 学号
     */
    public void setStudentNo(String studentNo) {
        // 更新当前对象的 studentNo。
        this.studentNo = studentNo;
    }

    /**
     * 获取学生姓名。
     *
     * @return 学生姓名
     */
    public String getName() {
        // 返回当前对象的 name。
        return name;
    }

    /**
     * 设置学生姓名。
     *
     * @param name 学生姓名
     */
    public void setName(String name) {
        // 更新当前对象的 name。
        this.name = name;
    }

    /**
     * 获取性别。
     *
     * @return 性别：1-男，2-女，0-未知
     */
    public Integer getGender() {
        // 返回当前对象的 gender。
        return gender;
    }

    /**
     * 设置性别。
     *
     * @param gender 性别：1-男，2-女，0-未知
     */
    public void setGender(Integer gender) {
        // 更新当前对象的 gender。
        this.gender = gender;
    }

    /**
     * 获取出生日期。
     *
     * @return 出生日期
     */
    public LocalDate getBirthDate() {
        // 返回当前对象的 birthDate。
        return birthDate;
    }

    /**
     * 设置出生日期。
     *
     * @param birthDate 出生日期
     */
    public void setBirthDate(LocalDate birthDate) {
        // 更新当前对象的 birthDate。
        this.birthDate = birthDate;
    }

    /**
     * 获取联系电话。
     *
     * @return 联系电话
     */
    public String getPhone() {
        // 返回当前对象的 phone。
        return phone;
    }

    /**
     * 设置联系电话。
     *
     * @param phone 联系电话
     */
    public void setPhone(String phone) {
        // 更新当前对象的 phone。
        this.phone = phone;
    }

    /**
     * 获取电子邮箱。
     *
     * @return 电子邮箱
     */
    public String getEmail() {
        // 返回当前对象的 email。
        return email;
    }

    /**
     * 设置电子邮箱。
     *
     * @param email 电子邮箱
     */
    public void setEmail(String email) {
        // 更新当前对象的 email。
        this.email = email;
    }

    /**
     * 获取所属专业。
     *
     * @return 所属专业
     */
    public String getMajor() {
        // 返回当前对象的 major。
        return major;
    }

    /**
     * 设置所属专业。
     *
     * @param major 所属专业
     */
    public void setMajor(String major) {
        // 更新当前对象的 major。
        this.major = major;
    }

    /**
     * 获取班级名称。
     *
     * @return 班级名称
     */
    public String getClassName() {
        // 返回当前对象的 className。
        return className;
    }

    /**
     * 设置班级名称。
     *
     * @param className 班级名称
     */
    public void setClassName(String className) {
        // 更新当前对象的 className。
        this.className = className;
    }

    /**
     * 获取学籍状态。
     *
     * @return 学籍状态：1-在读，2-休学，3-退学，4-毕业
     */
    public Integer getStatus() {
        // 返回当前对象的 status。
        return status;
    }

    /**
     * 设置学籍状态。
     *
     * @param status 学籍状态：1-在读，2-休学，3-退学，4-毕业
     */
    public void setStatus(Integer status) {
        // 更新当前对象的 status。
        this.status = status;
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
