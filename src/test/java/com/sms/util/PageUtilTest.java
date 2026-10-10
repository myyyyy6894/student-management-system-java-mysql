package com.sms.util;

// 导入 JUnit 5 断言与注解。
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PageUtil 分页参数规范化的单元测试。
 */
class PageUtilTest {

    @Test
    @DisplayName("页码小于 1 时回退到第 1 页")
    void normalizePage_fallsBackToOne() {
        assertEquals(1, PageUtil.normalizePage(0));
        assertEquals(1, PageUtil.normalizePage(-5));
        assertEquals(3, PageUtil.normalizePage(3));
    }

    @Test
    @DisplayName("每页条数非法时回退默认值，超大时截断到上限")
    void normalizePageSize_clampsRange() {
        assertEquals(PageUtil.DEFAULT_PAGE_SIZE, PageUtil.normalizePageSize(0));
        assertEquals(PageUtil.DEFAULT_PAGE_SIZE, PageUtil.normalizePageSize(-1));
        assertEquals(20, PageUtil.normalizePageSize(20));
        assertEquals(PageUtil.MAX_PAGE_SIZE, PageUtil.normalizePageSize(1_000_000));
    }
}
