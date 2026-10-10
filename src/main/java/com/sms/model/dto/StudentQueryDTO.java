package com.sms.model.dto;

/**
 * 学生分页查询参数对象。
 * <p>用于把前端或业务层传入的搜索条件传递到 DAO 层。</p>
 */
public class StudentQueryDTO {

    // 综合关键字：可以匹配学生姓名或学号。
    private String keyword;

    // 专业精准筛选条件。
    private String major;

    // 班级精准筛选条件。
    private String className;

    // 学籍状态筛选条件：1-在读，2-休学，3-退学，4-毕业。
    private Integer status;

    // 当前页码，默认从第 1 页开始。
    private int page = 1;

    // 每页最大条数，默认 10 条。
    private int pageSize = 10;

    /**
     * 获取综合关键字。
     *
     * @return 姓名/学号搜索关键字
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * 设置综合关键字。
     *
     * @param keyword 姓名/学号搜索关键字
     */
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    /**
     * 获取专业筛选条件。
     *
     * @return 专业名称
     */
    public String getMajor() {
        return major;
    }

    /**
     * 设置专业筛选条件。
     *
     * @param major 专业名称
     */
    public void setMajor(String major) {
        this.major = major;
    }

    /**
     * 获取班级筛选条件。
     *
     * @return 班级名称
     */
    public String getClassName() {
        return className;
    }

    /**
     * 设置班级筛选条件。
     *
     * @param className 班级名称
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * 获取学籍状态。
     *
     * @return 学籍状态
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * 设置学籍状态。
     *
     * @param status 学籍状态
     */
    public void setStatus(Integer status) {
        this.status = status;
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
