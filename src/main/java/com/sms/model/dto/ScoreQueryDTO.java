package com.sms.model.dto;

/**
 * 成绩分页查询参数对象。
 * <p>用于把前端或业务层传入的搜索条件传递到 DAO 层。</p>
 */
public class ScoreQueryDTO {

    // 学生物理主键 ID，用于精确筛选某个学生的成绩。
    private Long studentId;

    // 课程物理主键 ID，用于精确筛选某门课程的成绩。
    private Long courseId;

    // 学生姓名模糊搜索条件。
    private String studentName;

    // 最低分数限制，包含边界值。
    private Double minScore;

    // 最高分数限制，包含边界值。
    private Double maxScore;

    // 当前页码，默认从第 1 页开始。
    private int page = 1;

    // 每页最大条数，默认 10 条。
    private int pageSize = 10;

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
     * 获取学生姓名搜索条件。
     *
     * @return 学生姓名关键字
     */
    public String getStudentName() {
        return studentName;
    }

    /**
     * 设置学生姓名搜索条件。
     *
     * @param studentName 学生姓名关键字
     */
    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    /**
     * 获取最低分数。
     *
     * @return 最低分数
     */
    public Double getMinScore() {
        return minScore;
    }

    /**
     * 设置最低分数。
     *
     * @param minScore 最低分数
     */
    public void setMinScore(Double minScore) {
        this.minScore = minScore;
    }

    /**
     * 获取最高分数。
     *
     * @return 最高分数
     */
    public Double getMaxScore() {
        return maxScore;
    }

    /**
     * 设置最高分数。
     *
     * @param maxScore 最高分数
     */
    public void setMaxScore(Double maxScore) {
        this.maxScore = maxScore;
    }

    /**
     * 获取当前页码。
     *
     * @return 当前页码
     */
    public int getPage() {
        return page;
    }

    /**
     * 设置当前页码。
     *
     * @param page 当前页码
     */
    public void setPage(int page) {
        this.page = page;
    }

    /**
     * 获取每页条数。
     *
     * @return 每页条数
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * 设置每页条数。
     *
     * @param pageSize 每页条数
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
