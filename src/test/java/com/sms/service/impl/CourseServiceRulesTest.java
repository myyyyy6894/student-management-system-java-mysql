package com.sms.service.impl;

// 导入内存版 DAO。
import com.sms.support.InMemoryDaos;
// 导入业务异常类型。
import com.sms.exception.ConflictException;
import com.sms.exception.NotFoundException;
// 导入实体。
import com.sms.model.entity.Course;
import com.sms.model.entity.Score;
// 导入 JUnit 5。
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * CourseServiceImpl 业务规则单元测试（不依赖数据库）。
 */
class CourseServiceRulesTest {

    private InMemoryDaos.InMemoryCourseDao courseDao;
    private InMemoryDaos.InMemoryScoreDao scoreDao;
    private CourseServiceImpl service;

    @BeforeEach
    void setUp() {
        courseDao = new InMemoryDaos.InMemoryCourseDao();
        scoreDao = new InMemoryDaos.InMemoryScoreDao();
        service = new CourseServiceImpl(courseDao, scoreDao);
    }

    @Test
    @DisplayName("学分超出 0.5~10.0 时抛出 400")
    void addCourse_creditOutOfRange_throws() {
        Course course = buildCourse();
        course.setCredit(new BigDecimal("0.1"));
        assertThrows(IllegalArgumentException.class, () -> service.addCourse(course));

        Course tooLarge = buildCourse();
        tooLarge.setCredit(new BigDecimal("11.0"));
        assertThrows(IllegalArgumentException.class, () -> service.addCourse(tooLarge));
    }

    @Test
    @DisplayName("课程编号重复时返回 409")
    void addCourse_duplicate_throwsConflict() {
        service.addCourse(buildCourse());

        ConflictException exception = assertThrows(ConflictException.class,
                () -> service.addCourse(buildCourse()));
        assertEquals(409, exception.getCode());
        assertEquals("课程编号已存在", exception.getMessage());
    }

    @Test
    @DisplayName("课程已被成绩引用时禁止删除，返回 409")
    void deleteCourse_withScores_throwsConflict() {
        Course course = buildCourse();
        service.addCourse(course);

        Score score = new Score();
        score.setStudentId(1L);
        score.setCourseId(course.getId());
        score.setScore(new BigDecimal("88"));
        scoreDao.insert(score);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> service.deleteCourse(course.getId()));
        assertEquals(409, exception.getCode());
    }

    @Test
    @DisplayName("删除不存在的课程返回 404")
    void deleteCourse_notFound_throwsNotFound() {
        assertThrows(NotFoundException.class, () -> service.deleteCourse(999L));
    }

    @Test
    @DisplayName("修改时缺省课程编号会沿用旧值")
    void updateCourse_backfillsCourseNo() {
        Course course = buildCourse();
        service.addCourse(course);

        Course patch = new Course();
        patch.setId(course.getId());
        patch.setName("Java程序设计（进阶）");
        patch.setCredit(new BigDecimal("4.0"));
        patch.setTeacher("高老师");

        service.updateCourse(patch);

        assertEquals("CS101", courseDao.findById(course.getId()).getCourseNo());
        assertEquals("Java程序设计（进阶）", courseDao.findById(course.getId()).getName());
    }

    private Course buildCourse() {
        Course course = new Course();
        course.setCourseNo("CS101");
        course.setName("Java语言程序设计");
        course.setCredit(new BigDecimal("3.5"));
        course.setTeacher("高老师");
        return course;
    }
}
