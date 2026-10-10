package com.sms.model.vo;

// 导入精确小数类型，用于承载平均分、最高分、最低分和及格率。
import java.math.BigDecimal;

/**
 * 成绩统计视图对象。
 * <p>用于返回某门课程或全部课程的成绩统计指标。</p>
 */
public class ScoreStatsVO {

    // 统计课程 ID；如果统计全部课程，则为 null。
    private Long courseId;

    // 课程名称；如果统计全部课程，可以为 null。
    private String courseName;

    // 参加考试总人数。
    private int totalStudents;

    // 平均分，保留两位小数。
    private BigDecimal averageScore;

    // 最高分。
    private BigDecimal maxScore;

    // 最低分。
    private BigDecimal minScore;

    // 及格率，单位是百分比，例如 85.50 表示 85.5%。
    private BigDecimal passRate;

    /**
     * 获取统计课程 ID。
     *
     * @return 统计课程 ID
     */
    public Long getCourseId() {
        return courseId;
    }

    /**
     * 设置统计课程 ID。
     *
     * @param courseId 统计课程 ID
     */
    public void setCourseId(Long courseId) {
        this.courseId = courseId;
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
     * 获取参加考试总人数。
     *
     * @return 参加考试总人数
     */
    public int getTotalStudents() {
        return totalStudents;
    }

    /**
     * 设置参加考试总人数。
     *
     * @param totalStudents 参加考试总人数
     */
    public void setTotalStudents(int totalStudents) {
        this.totalStudents = totalStudents;
    }

    /**
     * 获取平均分。
     *
     * @return 平均分
     */
    public BigDecimal getAverageScore() {
        return averageScore;
    }

    /**
     * 设置平均分。
     *
     * @param averageScore 平均分
     */
    public void setAverageScore(BigDecimal averageScore) {
        this.averageScore = averageScore;
    }

    /**
     * 获取最高分。
     *
     * @return 最高分
     */
    public BigDecimal getMaxScore() {
        return maxScore;
    }

    /**
     * 设置最高分。
     *
     * @param maxScore 最高分
     */
    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    /**
     * 获取最低分。
     *
     * @return 最低分
     */
    public BigDecimal getMinScore() {
        return minScore;
    }

    /**
     * 设置最低分。
     *
     * @param minScore 最低分
     */
    public void setMinScore(BigDecimal minScore) {
        this.minScore = minScore;
    }

    /**
     * 获取及格率。
     *
     * @return 及格率百分比
     */
    public BigDecimal getPassRate() {
        return passRate;
    }

    /**
     * 设置及格率。
     *
     * @param passRate 及格率百分比
     */
    public void setPassRate(BigDecimal passRate) {
        this.passRate = passRate;
    }
}
