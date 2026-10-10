package com.sms.service.impl;

// 导入内存版 DAO。
import com.sms.support.InMemoryDaos;
// 导入业务异常类型。
import com.sms.exception.ConflictException;
import com.sms.exception.NotFoundException;
// 导入 DTO 与实体。
import com.sms.model.dto.ScoreQueryDTO;
import com.sms.model.entity.Course;
import com.sms.model.entity.Score;
import com.sms.model.entity.Student;
import com.sms.model.vo.PageResult;
import com.sms.model.vo.ScoreDetailVO;
import com.sms.model.vo.ScoreStatsVO;
// 导入 JUnit 5。
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * ScoreServiceImpl 业务规则单元测试（不依赖数据库）。
 */
class ScoreServiceRulesTest {

    private InMemoryDaos.InMemoryStudentDao studentDao;
    private InMemoryDaos.InMemoryCourseDao courseDao;
    private InMemoryDaos.InMemoryScoreDao scoreDao;
    private ScoreServiceImpl service;

    private Long studentId;
    private Long courseId;

    @BeforeEach
    void setUp() {
        studentDao = new InMemoryDaos.InMemoryStudentDao();
        courseDao = new InMemoryDaos.InMemoryCourseDao();
        scoreDao = new InMemoryDaos.InMemoryScoreDao();
        scoreDao.studentDao = studentDao;
        scoreDao.courseDao = courseDao;
        service = new ScoreServiceImpl(studentDao, courseDao, scoreDao);

        studentId = studentDao.insert(buildStudent());
        courseId = courseDao.insert(buildCourse());
    }

    @Test
    @DisplayName("分数低于 0 或高于 100 时抛出 400")
    void addScore_outOfRange_throws() {
        Score low = buildScore(-1);
        assertEquals("分数必须在 0~100 之间",
                assertThrows(IllegalArgumentException.class, () -> service.addScore(low)).getMessage());

        Score high = buildScore(101);
        assertEquals("分数必须在 0~100 之间",
                assertThrows(IllegalArgumentException.class, () -> service.addScore(high)).getMessage());
    }

    @Test
    @DisplayName("学生不存在时返回 404")
    void addScore_studentNotFound_throwsNotFound() {
        Score score = buildScore(80);
        score.setStudentId(999L);

        NotFoundException exception = assertThrows(NotFoundException.class, () -> service.addScore(score));
        assertEquals(404, exception.getCode());
    }

    @Test
    @DisplayName("课程不存在时返回 404")
    void addScore_courseNotFound_throwsNotFound() {
        Score score = buildScore(80);
        score.setCourseId(999L);

        NotFoundException exception = assertThrows(NotFoundException.class, () -> service.addScore(score));
        assertEquals(404, exception.getCode());
    }

    @Test
    @DisplayName("同一学生同一课程重复录入时返回 409")
    void addScore_duplicate_throwsConflict() {
        service.addScore(buildScore(80));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> service.addScore(buildScore(90)));
        assertEquals(409, exception.getCode());
        assertEquals("该学生本门课程成绩已存在，请走修改流程", exception.getMessage());
    }

    @Test
    @DisplayName("备注超过 255 字符时抛出 400")
    void addScore_remarksTooLong_throws() {
        Score score = buildScore(80);
        score.setRemarks("备".repeat(256));

        assertThrows(IllegalArgumentException.class, () -> service.addScore(score));
    }

    @Test
    @DisplayName("统计不存在的课程返回 404，而不是全 0 指标")
    void statistics_unknownCourse_throwsNotFound() {
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> service.getScoreStatistics(999L));
        assertEquals(404, exception.getCode());
    }

    @Test
    @DisplayName("统计存在的课程返回正确的平均分与及格率")
    void statistics_calculatesMetrics() {
        // 同一学生同一课程只能有一条成绩，因此第二名同学需要单独创建。
        Score first = buildScore(90);
        service.addScore(first);

        Student secondStudent = buildStudent();
        secondStudent.setStudentNo("20240002");
        secondStudent.setName("李四");
        Long secondStudentId = studentDao.insert(secondStudent);

        Score second = buildScore(50);
        second.setStudentId(secondStudentId);
        service.addScore(second);

        ScoreStatsVO stats = service.getScoreStatistics(courseId);

        assertEquals(2, stats.getTotalStudents());
        assertEquals(new BigDecimal("70.00"), stats.getAverageScore());
        assertEquals(new BigDecimal("50.00"), stats.getPassRate());
        assertEquals("Java语言程序设计", stats.getCourseName());
    }

    @Test
    @DisplayName("分数区间非法时抛出 400")
    void queryDetails_invalidInterval_throws() {
        ScoreQueryDTO queryDTO = new ScoreQueryDTO();
        queryDTO.setMinScore(90.0);
        queryDTO.setMaxScore(60.0);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.queryScoreDetails(queryDTO));
        assertEquals("最低分不能大于最高分", exception.getMessage());
    }

    @Test
    @DisplayName("分数区间端点超出 0~100 时抛出 400")
    void queryDetails_outOfRangeInterval_throws() {
        ScoreQueryDTO queryDTO = new ScoreQueryDTO();
        queryDTO.setMinScore(-1.0);

        assertThrows(IllegalArgumentException.class, () -> service.queryScoreDetails(queryDTO));
    }

    @Test
    @DisplayName("分页查询明细返回联查后的学生与课程信息，并规范化分页参数")
    void queryDetails_returnsJoinedFields() {
        service.addScore(buildScore(88.5));

        ScoreQueryDTO queryDTO = new ScoreQueryDTO();
        queryDTO.setPage(0);
        queryDTO.setPageSize(0);

        PageResult<ScoreDetailVO> result = service.queryScoreDetails(queryDTO);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getPage());
        assertEquals(10, result.getPageSize());
        assertEquals("张三", result.getList().get(0).getStudentName());
        assertEquals("Java语言程序设计", result.getList().get(0).getCourseName());
    }

    @Test
    @DisplayName("修改不存在的成绩返回 404")
    void updateScore_notFound_throwsNotFound() {
        Score score = buildScore(80);
        score.setId(999L);

        assertThrows(NotFoundException.class, () -> service.updateScore(score));
    }

    private Student buildStudent() {
        Student student = new Student();
        student.setStudentNo("20249999");
        student.setName("张三");
        student.setGender(1);
        student.setMajor("计算机科学与技术");
        student.setClassName("计科2401班");
        student.setStatus(1);
        return student;
    }

    private Course buildCourse() {
        Course course = new Course();
        course.setCourseNo("CS101");
        course.setName("Java语言程序设计");
        course.setCredit(new BigDecimal("3.5"));
        course.setTeacher("高老师");
        return course;
    }

    private Score buildScore(double value) {
        Score score = new Score();
        score.setStudentId(studentId);
        score.setCourseId(courseId);
        score.setScore(BigDecimal.valueOf(value));
        score.setExamDate(LocalDate.of(2026, 6, 15));
        score.setRemarks("期末正考");
        return score;
    }
}
