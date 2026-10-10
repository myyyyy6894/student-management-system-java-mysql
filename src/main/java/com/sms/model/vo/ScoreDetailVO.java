package com.sms.model.vo;

// 导入精确小数类型，用于承载学分和成绩。
import java.math.BigDecimal;
// 导入日期类，用于承载考试日期。
import java.time.LocalDate;

/**
 * 成绩多表联查明细视图对象。
 * <p>把 score、student、course 三张表的数据合并成前端表格可以直接使用的结构。</p>
 */
public class ScoreDetailVO {

    // 成绩记录 ID，对应 score.id。
    private Long id;

    // 学生物理主键 ID，对应 score.student_id。
    private Long studentId;

    // 学号，来自 student.student_no。
    private String studentNo;

    // 学生姓名，来自 student.name。
    private String studentName;

    // 班级名称，来自 student.class_name。
    private String className;

    // 课程物理主键 ID，对应 score.course_id。
    private Long courseId;

    // 课程编号，来自 course.course_no。
    private String courseNo;

    // 课程名称，来自 course.name。
    private String courseName;

    // 课程学分，来自 course.credit。
    private BigDecimal credit;

    // 考试成绩，来自 score.score。
    private BigDecimal score;

    // 考试日期，来自 score.exam_date。
    private LocalDate examDate;

    // 成绩备注，来自 score.remarks。
    private String remarks;

    /**
     * 获取成绩记录 ID。
     *
     * @return 成绩记录 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置成绩记录 ID。
     *
     * @param id 成绩记录 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取学生 ID。
     *
     * @return 学生物理主键 ID
     */
    public Long getStudentId() {
        return studentId;
    }

    /**
     * 设置学生 ID。
     *
     * @param studentId 学生物理主键 ID
     */
    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    /**
     * 获取学号。
     *
     * @return 学号
     */
    public String getStudentNo() {
        return studentNo;
    }

    /**
     * 设置学号。
     *
     * @param studentNo 学号
     */
    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }

    /**
     * 获取学生姓名。
     *
     * @return 学生姓名
     */
    public String getStudentName() {
        return studentName;
    }

    /**
     * 设置学生姓名。
     *
     * @param studentName 学生姓名
     */
    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    /**
     * 获取班级名称。
     *
     * @return 班级名称
     */
    public String getClassName() {
        return className;
    }

    /**
     * 设置班级名称。
     *
     * @param className 班级名称
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * 获取课程 ID。
     *
     * @return 课程物理主键 ID
     */
    public Long getCourseId() {
        return courseId;
    }

    /**
     * 设置课程 ID。
     *
     * @param courseId 课程物理主键 ID
     */
    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    /**
     * 获取课程编号。
     *
     * @return 课程编号
     */
    public String getCourseNo() {
        return courseNo;
    }

    /**
     * 设置课程编号。
     *
     * @param courseNo 课程编号
     */
    public void setCourseNo(String courseNo) {
        this.courseNo = courseNo;
    }

    /**
     * 获取课程名称。
     *
     * @return 课程名称
     */
    public String getCourseName() {
        return courseName;
    }

    /**
     * 设置课程名称。
     *
     * @param courseName 课程名称
     */
    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    /**
     * 获取课程学分。
     *
     * @return 课程学分
     */
    public BigDecimal getCredit() {
        return credit;
    }

    /**
     * 设置课程学分。
     *
     * @param credit 课程学分
     */
    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    /**
     * 获取考试成绩。
     *
     * @return 考试成绩
     */
    public BigDecimal getScore() {
        return score;
    }

    /**
     * 设置考试成绩。
     *
     * @param score 考试成绩
     */
    public void setScore(BigDecimal score) {
        this.score = score;
    }

    /**
     * 获取考试日期。
     *
     * @return 考试日期
     */
    public LocalDate getExamDate() {
        return examDate;
    }

    /**
     * 设置考试日期。
     *
     * @param examDate 考试日期
     */
    public void setExamDate(LocalDate examDate) {
        this.examDate = examDate;
    }

    /**
     * 获取成绩备注。
     *
     * @return 成绩备注
     */
    public String getRemarks() {
        return remarks;
    }

    /**
     * 设置成绩备注。
     *
     * @param remarks 成绩备注
     */
    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
