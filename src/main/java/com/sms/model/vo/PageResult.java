package com.sms.model.vo;

// 导入 List，用来承载当前页查询出来的多条数据。
import java.util.List;

/**
 * 通用分页结果类。
 * <p>用于把总数、页码、每页条数和当前页数据组装成一个统一结构，方便前端渲染表格和分页器。</p>
 *
 * @param <T> 当前页单条数据的类型，例如 Student、Course 或 ScoreDetailVO
 */
public class PageResult<T> {

    // 总记录条数：符合查询条件的所有记录数量，而不是当前页的数量。
    private long total;

    // 当前页码，业务约定从 1 开始。
    private int page;

    // 每页最大条数。
    private int pageSize;

    // 当前页的数据集合；如果没有数据，应该返回空集合而不是 null。
    private List<T> list;

    /**
     * 无参构造器。
     * <p>方便 JSON 序列化框架或测试代码先创建空对象，再逐步设置字段。</p>
     */
    public PageResult() {
    }

    /**
     * 全参构造器，用于一次性组装完整分页结果。
     *
     * @param total    符合条件的总记录数
     * @param page     当前页码，从 1 开始
     * @param pageSize 每页条数
     * @param list     当前页数据集合
     */
    public PageResult(long total, int page, int pageSize, List<T> list) {
        // 将参数 total 赋值给成员变量 total。
        this.total = total;
        // 将参数 page 赋值给成员变量 page。
        this.page = page;
        // 将参数 pageSize 赋值给成员变量 pageSize。
        this.pageSize = pageSize;
        // 将参数 list 赋值给成员变量 list。
        this.list = list;
    }

    /**
     * 获取总记录数。
     *
     * @return 符合查询条件的记录总数
     */
    public long getTotal() {
        // 返回当前对象的 total。
        return total;
    }

    /**
     * 设置总记录数。
     *
     * @param total 符合查询条件的记录总数
     */
    public void setTotal(long total) {
        // 更新当前对象的 total。
        this.total = total;
    }

    /**
     * 获取当前页码。
     *
     * @return 当前页码，从 1 开始
     */
    public int getPage() {
        // 返回当前对象的 page。
        return page;
    }

    /**
     * 设置当前页码。
     *
     * @param page 当前页码，从 1 开始
     */
    public void setPage(int page) {
        // 更新当前对象的 page。
        this.page = page;
    }

    /**
     * 获取每页条数。
     *
     * @return 每页最大数据条数
     */
    public int getPageSize() {
        // 返回当前对象的 pageSize。
        return pageSize;
    }

    /**
     * 设置每页条数。
     *
     * @param pageSize 每页最大数据条数
     */
    public void setPageSize(int pageSize) {
        // 更新当前对象的 pageSize。
        this.pageSize = pageSize;
    }

    /**
     * 获取当前页数据集合。
     *
     * @return 当前页数据；无数据时可能是空集合
     */
    public List<T> getList() {
        // 返回当前对象的 list。
        return list;
    }

    /**
     * 设置当前页数据集合。
     *
     * @param list 当前页数据集合
     */
    public void setList(List<T> list) {
        // 更新当前对象的 list。
        this.list = list;
    }
}
