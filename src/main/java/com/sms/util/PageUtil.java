package com.sms.util;

/**
 * 分页参数工具类。
 * <p>统一约束页码与每页条数的合法范围，避免超大 pageSize 拖垮数据库，
 * 也让 Service 组装的分页回显值与 DAO 实际执行的 SQL 完全一致。</p>
 */
public final class PageUtil {

    // 默认页码：从第 1 页开始。
    public static final int DEFAULT_PAGE = 1;

    // 默认每页条数。
    public static final int DEFAULT_PAGE_SIZE = 10;

    // 每页条数上限，防止客户端传入超大值导致全表扫描。
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * 私有构造器，工具类不需要创建对象。
     */
    private PageUtil() {
        // 工具类只需要静态方法。
    }

    /**
     * 规范化页码。
     *
     * @param page 原始页码
     * @return 合法页码，最小为 1
     */
    public static int normalizePage(int page) {
        // 小于 1 的页码一律回退到第 1 页。
        return page < DEFAULT_PAGE ? DEFAULT_PAGE : page;
    }

    /**
     * 规范化每页条数。
     *
     * @param pageSize 原始每页条数
     * @return 合法每页条数，范围 [1, MAX_PAGE_SIZE]
     */
    public static int normalizePageSize(int pageSize) {
        // 非正数回退到默认值。
        if (pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }

        // 超过上限时截断到上限。
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
