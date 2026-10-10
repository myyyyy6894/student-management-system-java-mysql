package com.sms.service.impl;

// 导入内存版 DAO。
import com.sms.support.InMemoryDaos;
// 导入业务异常类型。
import com.sms.exception.ConflictException;
import com.sms.exception.NotFoundException;
// 导入 DTO 与实体。
import com.sms.model.dto.StudentQueryDTO;
import com.sms.model.entity.Student;
import com.sms.model.vo.PageResult;
// 导入分页工具，用于校验回显值。
import com.sms.util.PageUtil;
// 导入 JUnit 5。
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * StudentServiceImpl 业务规则单元测试（不依赖数据库）。
 */
class StudentServiceRulesTest {

    private InMemoryDaos.InMemoryStudentDao studentDao;
    private InMemoryDaos.InMemoryScoreDao scoreDao;
    private StudentServiceImpl service;

    @BeforeEach
    void setUp() {
        studentDao = new InMemoryDaos.InMemoryStudentDao();
        scoreDao = new InMemoryDaos.InMemoryScoreDao();
        service = new StudentServiceImpl(studentDao, scoreDao);
    }

    @Test
    @DisplayName("性别/学籍状态缺省时自动补齐默认值，避免写入 NULL")
    void addStudent_fillsDefaultGenderAndStatus() {
        Student student = validStudent();
        student.setGender(null);
        student.setStatus(null);

        service.addStudent(student);

        assertEquals(1, student.getGender());
        assertEquals(1, student.getStatus());
    }

    @Test
    @DisplayName("重复学号抛出 ConflictException(409)")
    void addStudent_duplicate_throwsConflict() {
        service.addStudent(validStudent());

        ConflictException exception = assertThrows(ConflictException.class, () -> {
            Student duplicate = validStudent();
            duplicate.setName("另一个人");
            service.addStudent(duplicate);
        });

        assertEquals(409, exception.getCode());
        assertEquals("学号已存在", exception.getMessage());
    }

    @Test
    @DisplayName("手机号格式非法时抛出 400")
    void addStudent_invalidPhone_throws() {
        Student student = validStudent();
        student.setPhone("12345");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.addStudent(student));
        assertEquals("联系电话格式不正确", exception.getMessage());
    }

    @Test
    @DisplayName("邮箱格式非法时抛出 400")
    void addStudent_invalidEmail_throws() {
        Student student = validStudent();
        student.setEmail("not-an-email");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.addStudent(student));
        assertEquals("电子邮箱格式不正确", exception.getMessage());
    }

    @Test
    @DisplayName("姓名超过 50 字符时抛出 400")
    void addStudent_nameTooLong_throws() {
        Student student = validStudent();
        student.setName("张".repeat(51));

        assertThrows(IllegalArgumentException.class, () -> service.addStudent(student));
    }

    @Test
    @DisplayName("性别取值不在 0/1/2 时抛出 400")
    void addStudent_invalidGender_throws() {
        Student student = validStudent();
        student.setGender(9);

        assertThrows(IllegalArgumentException.class, () -> service.addStudent(student));
    }

    @Test
    @DisplayName("修改不存在的学生抛出 NotFoundException(404)")
    void updateStudent_notFound_throwsNotFound() {
        Student student = validStudent();
        student.setId(999L);

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> service.updateStudent(student));
        assertEquals(404, exception.getCode());
    }

    @Test
    @DisplayName("修改时缺省字段沿用库中旧值，不会把性别/状态重置为默认值")
    void updateStudent_backfillsMissingNotNullFields() {
        Student student = validStudent();
        student.setGender(2);
        student.setStatus(4);
        service.addStudent(student);

        Long id = student.getId();
        Student patch = new Student();
        patch.setId(id);
        patch.setName("改名后");
        patch.setMajor("软件工程");
        patch.setClassName("软工2409班");
        // 刻意不传 gender / status / studentNo

        service.updateStudent(patch);

        Student saved = studentDao.findById(id);
        assertEquals(2, saved.getGender());
        assertEquals(4, saved.getStatus());
        assertEquals("20249999", saved.getStudentNo());
        assertEquals("改名后", saved.getName());
    }

    @Test
    @DisplayName("不允许修改学号")
    void updateStudent_changeStudentNo_throws() {
        Student student = validStudent();
        service.addStudent(student);

        Student changed = validStudent();
        changed.setId(student.getId());
        changed.setStudentNo("20240000");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.updateStudent(changed));
        assertEquals("不允许修改学号", exception.getMessage());
    }

    @Test
    @DisplayName("学生名下有成绩时禁止删除，返回 409")
    void deleteStudent_withScores_throwsConflict() {
        Student student = validStudent();
        service.addStudent(student);

        com.sms.model.entity.Score score = new com.sms.model.entity.Score();
        score.setStudentId(student.getId());
        score.setCourseId(1L);
        score.setScore(new java.math.BigDecimal("88"));
        scoreDao.insert(score);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> service.deleteStudent(student.getId()));
        assertEquals(409, exception.getCode());
    }

    @Test
    @DisplayName("删除不存在的学生返回 404")
    void deleteStudent_notFound_throwsNotFound() {
        assertThrows(NotFoundException.class, () -> service.deleteStudent(123L));
    }

    @Test
    @DisplayName("分页参数非法时回显值被规范化，与 SQL 实际使用的值一致")
    void queryStudents_normalizesPagination() {
        for (int i = 0; i < 3; i++) {
            Student student = validStudent();
            student.setStudentNo("2024000" + i);
            service.addStudent(student);
        }

        StudentQueryDTO queryDTO = new StudentQueryDTO();
        queryDTO.setPage(0);
        queryDTO.setPageSize(-1);

        PageResult<Student> result = service.queryStudents(queryDTO);

        assertEquals(PageUtil.DEFAULT_PAGE, result.getPage());
        assertEquals(PageUtil.DEFAULT_PAGE_SIZE, result.getPageSize());
        assertEquals(3, result.getTotal());
        assertTrue(result.getList().size() <= PageUtil.DEFAULT_PAGE_SIZE);
    }

    @Test
    @DisplayName("出生日期晚于今天时抛出 400")
    void addStudent_futureBirthDate_throws() {
        Student student = validStudent();
        student.setBirthDate(LocalDate.now().plusDays(1));

        assertThrows(IllegalArgumentException.class, () -> service.addStudent(student));
    }

    private Student validStudent() {
        Student student = new Student();
        student.setStudentNo("20249999");
        student.setName("张三");
        student.setGender(1);
        student.setBirthDate(LocalDate.of(2004, 3, 15));
        student.setPhone("13800009999");
        student.setEmail("zhangsan@campus.edu.cn");
        student.setMajor("计算机科学与技术");
        student.setClassName("计科2401班");
        student.setStatus(1);
        return student;
    }
}
