package com.sms.service.impl;

// 导入真实 DAO 实现，用于准备和清理测试数据。
import com.sms.dao.impl.CourseDaoImpl;
import com.sms.dao.impl.ScoreDaoImpl;
import com.sms.dao.impl.StudentDaoImpl;
// 导入课程和成绩实体，用于准备删除学生前的依赖数据。
import com.sms.model.entity.Course;
import com.sms.model.entity.Score;
// 导入学生实体和查询条件。
import com.sms.model.entity.Student;
import com.sms.model.dto.StudentQueryDTO;
// 导入分页结果，用于校验 Service 的分页封装。
import com.sms.model.vo.PageResult;
// 导入 SQL 异常，数据库准备或清理数据失败时使用。
import java.sql.SQLException;
// 导入精确小数和日期类型，用于构造测试数据。
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * StudentServiceImpl 的真实数据库手工测试类。
 * <p>当前项目还没有 JUnit 依赖，所以这里提供 main 方法，可以直接运行验证业务层规则。</p>
 * <p>测试重点：新增、学号防重、修改学号拦截、分页封装、删除成绩依赖拦截和正常删除。</p>
 */
public class StudentServiceImplTest {

    // 测试学生学号；固定值方便测试前清理遗留数据。
    private static final String TEST_STUDENT_NO = "TEST-SVC-STU999";

    // 测试课程编号；删除学生前需要先挂一条成绩，所以需要准备一门测试课程。
    private static final String TEST_COURSE_NO = "TEST-SVC-COURSE999";

    /**
     * 测试入口，右键运行后会按顺序执行学生业务规则测试。
     *
     * @param args 命令行参数，当前测试不需要传入任何参数
     */
    public static void main(String[] args) {
        // 输出测试开始提示，方便在控制台确认测试已经启动。
        System.out.println("===== StudentServiceImpl 测试开始 =====");

        // 准备真实 DAO 对象，用于测试数据准备和清理。
        StudentDaoImpl studentDao = new StudentDaoImpl();
        CourseDaoImpl courseDao = new CourseDaoImpl();
        ScoreDaoImpl scoreDao = new ScoreDaoImpl();

        // 创建被测 Service，默认构造器内部会使用真实 DAO 实现。
        StudentServiceImpl service = new StudentServiceImpl();

        // 保存测试数据 ID，方便 finally 中判断是否需要兜底清理。
        Long studentId = null;
        Long courseId = null;
        Long scoreId = null;

        try {
            // 先清理可能遗留的测试学生、课程和成绩，保证测试可以重复运行。
            cleanupTestData(studentDao, courseDao, scoreDao);

            // 创建测试学生对象。
            Student student = buildTestStudent();

            // 调用业务层新增学生。
            service.addStudent(student);

            // 校验 Service 把数据库生成的主键回填到了实体对象。
            studentId = student.getId();

            // 校验新增结果。
            testAddResult(service, studentId);

            // 测试相同学号不能重复新增。
            testAddDuplicate(service);

            // 测试不允许修改学号。
            testUpdateStudentNoBlocked(service, studentId);

            // 测试正常修改学生基本信息。
            testUpdate(service, studentId);

            // 测试分页查询结果封装。
            testQueryStudents(service);

            // 创建一门测试课程，用于测试“有成绩的学生不能删除”。
            courseId = insertTestCourse(courseDao);

            // 给测试学生录入一条成绩。
            scoreId = insertTestScore(scoreDao, studentId, courseId);

            // 测试删除学生时，如果已有成绩会被业务层拦截。
            testDeleteBlocked(service, studentId);

            // 删除测试成绩，解除学生删除依赖。
            scoreDao.deleteById(scoreId);
            scoreId = null;

            // 测试没有成绩依赖时可以正常删除学生。
            testDelete(service, studentId);

            // 删除成功后清空 ID，避免 finally 中重复清理。
            studentId = null;

            // 输出整体测试成功提示。
            System.out.println("===== StudentServiceImpl 测试全部成功 =====");
        } catch (SQLException exception) {
            // 测试数据准备或数据库访问失败都会进入这里。
            System.err.println("StudentServiceImpl 测试失败：" + exception.getMessage());
            // 打印完整异常堆栈，便于定位问题。
            exception.printStackTrace();
        } finally {
            // finally 兜底清理，避免测试中途失败后留下脏数据。
            try {
                // 按成绩、课程、学生的顺序清理。
                cleanupTestData(studentDao, courseDao, scoreDao);
            } catch (SQLException cleanupException) {
                // 清理失败也要提示，方便手动检查数据库。
                System.err.println("测试数据清理失败：" + cleanupException.getMessage());
            }
        }
    }

    /**
     * 构造测试学生对象。
     *
     * @return 学生实体
     */
    private static Student buildTestStudent() {
        // 创建空学生对象。
        Student student = new Student();

        // 设置固定测试学号。
        student.setStudentNo(TEST_STUDENT_NO);

        // 设置测试姓名。
        student.setName("Service测试张三");

        // 设置性别：1-男。
        student.setGender(1);

        // 设置出生日期。
        student.setBirthDate(LocalDate.of(2004, 2, 10));

        // 设置联系电话。
        student.setPhone("13800009999");

        // 设置电子邮箱。
        student.setEmail("service-test@campus.edu.cn");

        // 设置所属专业。
        student.setMajor("计算机科学与技术");

        // 设置班级名称。
        student.setClassName("计科2401班");

        // 设置学籍状态：1-在读。
        student.setStatus(1);

        // 返回测试学生。
        return student;
    }

    /**
     * 校验新增学生和按 ID 查询结果。
     *
     * @param service   被测业务对象
     * @param studentId 新增学生主键 ID
     */
    private static void testAddResult(StudentServiceImpl service, Long studentId) {
        // 校验 Service 新增后返回了有效主键。
        if (studentId == null || studentId <= 0) {
            // 如果主键无效，说明新增流程没有正确回填 ID。
            throw new IllegalStateException("新增学生后没有回填有效主键 ID，实际值：" + studentId);
        }

        // 按 ID 查询学生。
        Student foundStudent = service.getStudentById(studentId);

        // 校验学生存在。
        if (foundStudent == null) {
            // 如果查不到，说明新增或查询流程有问题。
            throw new IllegalStateException("新增后按 ID 查询不到学生：" + studentId);
        }

        // 校验学号一致。
        if (!TEST_STUDENT_NO.equals(foundStudent.getStudentNo())) {
            // 如果不一致，说明数据写入或映射有问题。
            throw new IllegalStateException("新增后学号不正确，实际值：" + foundStudent.getStudentNo());
        }

        // 输出新增和查询成功提示。
        System.out.println("【1】新增学生并按 ID 查询成功，ID：" + studentId);
    }

    /**
     * 测试相同学号不能重复新增。
     *
     * @param service 被测业务对象
     */
    private static void testAddDuplicate(StudentServiceImpl service) {
        // 构造一个和已存在学生学号相同的新对象。
        Student duplicateStudent = buildTestStudent();

        // 修改姓名，模拟两个不同学生使用了同一个学号。
        duplicateStudent.setName("Service测试重复学号");

        // 期望业务层抛出 IllegalArgumentException。
        expectIllegalArgument(
                () -> service.addStudent(duplicateStudent),
                "学号已存在",
                "新增重复学号学生"
        );

        // 输出学号防重测试成功提示。
        System.out.println("【2】重复学号新增拦截成功");
    }

    /**
     * 测试不允许修改学生学号。
     *
     * @param service   被测业务对象
     * @param studentId 测试学生主键 ID
     */
    private static void testUpdateStudentNoBlocked(StudentServiceImpl service, Long studentId) {
        // 先查询当前学生。
        Student student = service.getStudentById(studentId);

        // 校验学生存在。
        if (student == null) {
            // 如果不存在，说明测试环境或前置流程有问题。
            throw new IllegalStateException("测试修改学号前找不到学生：" + studentId);
        }

        // 修改学号，模拟前端非法提交。
        student.setStudentNo("TEST-SVC-STU999-CHANGED");

        // 期望业务层抛出 IllegalArgumentException。
        expectIllegalArgument(
                () -> service.updateStudent(student),
                "不允许修改学号",
                "修改学生学号"
        );

        // 输出修改学号拦截成功提示。
        System.out.println("【3】修改学号拦截成功");
    }

    /**
     * 测试正常修改学生基本信息。
     *
     * @param service   被测业务对象
     * @param studentId 测试学生主键 ID
     */
    private static void testUpdate(StudentServiceImpl service, Long studentId) {
        // 先查询当前学生。
        Student student = service.getStudentById(studentId);

        // 校验学生存在。
        if (student == null) {
            // 如果不存在，说明前置流程有问题。
            throw new IllegalStateException("更新测试前找不到学生：" + studentId);
        }

        // 修改姓名，学号保持不变。
        student.setName("Service测试李四");

        // 修改联系电话。
        student.setPhone("13800008888");

        // 修改学籍状态：2-休学。
        student.setStatus(2);

        // 调用业务层更新学生。
        service.updateStudent(student);

        // 更新后重新查询。
        Student updatedStudent = service.getStudentById(studentId);

        // 校验更新后的学生存在。
        if (updatedStudent == null) {
            // 如果查不到，说明更新后查询失败。
            throw new IllegalStateException("更新后查询不到学生：" + studentId);
        }

        // 校验姓名是否更新成功。
        if (!"Service测试李四".equals(updatedStudent.getName())) {
            // 如果姓名没变，说明 update 流程有问题。
            throw new IllegalStateException("更新后的姓名不正确，实际值：" + updatedStudent.getName());
        }

        // 校验学籍状态是否更新成功。
        if (!Integer.valueOf(2).equals(updatedStudent.getStatus())) {
            // 如果状态没变，说明 update 流程有问题。
            throw new IllegalStateException("更新后的学籍状态不正确，实际值：" + updatedStudent.getStatus());
        }

        // 输出更新成功提示。
        System.out.println("【4】正常修改学生成功：" + updatedStudent.getName());
    }

    /**
     * 测试分页查询结果封装。
     *
     * @param service 被测业务对象
     */
    private static void testQueryStudents(StudentServiceImpl service) {
        // 创建查询条件。
        StudentQueryDTO queryDTO = new StudentQueryDTO();

        // 用固定测试学号作为关键字，保证测试结果稳定。
        queryDTO.setKeyword(TEST_STUDENT_NO);

        // 设置第 1 页。
        queryDTO.setPage(1);

        // 设置每页 10 条。
        queryDTO.setPageSize(10);

        // 调用 Service 分页查询。
        PageResult<Student> pageResult = service.queryStudents(queryDTO);

        // 校验分页结果不为空。
        if (pageResult == null) {
            // 如果为 null，说明 Service 没有正确封装分页对象。
            throw new IllegalStateException("queryStudents 不应该返回 null");
        }

        // 校验总记录数为 1。
        if (pageResult.getTotal() != 1) {
            // 如果总数不是 1，说明条件查询或统计逻辑有问题。
            throw new IllegalStateException("分页总数不正确，实际值：" + pageResult.getTotal());
        }

        // 校验当前页数据不为空。
        if (pageResult.getList() == null || pageResult.getList().isEmpty()) {
            // 如果没有数据，说明分页查询有问题。
            throw new IllegalStateException("分页数据不应该为空");
        }

        // 校验第一页数据学号正确。
        if (!TEST_STUDENT_NO.equals(pageResult.getList().get(0).getStudentNo())) {
            // 如果不一致，说明查询映射有问题。
            throw new IllegalStateException(
                    "分页查询学号不正确，实际值：" + pageResult.getList().get(0).getStudentNo()
            );
        }

        // 输出分页查询成功提示。
        System.out.println("【5】分页查询封装成功，总数：" + pageResult.getTotal());
    }

    /**
     * 测试删除已有成绩的学生会被拦截。
     *
     * @param service   被测业务对象
     * @param studentId 测试学生主键 ID
     */
    private static void testDeleteBlocked(StudentServiceImpl service, Long studentId) {
        // 期望业务层抛出 IllegalArgumentException。
        expectIllegalArgument(
                () -> service.deleteStudent(studentId),
                "该学生已有考试成绩，请先清理成绩记录或注销学籍",
                "删除已有成绩的学生"
        );

        // 输出删除拦截成功提示。
        System.out.println("【6】已有成绩的学生删除拦截成功");
    }

    /**
     * 测试没有成绩依赖时可以正常删除学生。
     *
     * @param service   被测业务对象
     * @param studentId 测试学生主键 ID
     */
    private static void testDelete(StudentServiceImpl service, Long studentId) {
        // 调用业务层删除学生。
        service.deleteStudent(studentId);

        // 删除后再次查询。
        Student deletedStudent = service.getStudentById(studentId);

        // 校验删除后查询不到。
        if (deletedStudent != null) {
            // 如果还能查到，说明删除流程失败。
            throw new IllegalStateException("删除后仍然可以查询到学生：" + studentId);
        }

        // 输出删除成功提示。
        System.out.println("【7】正常删除学生成功");
    }

    /**
     * 创建测试课程。
     *
     * @param courseDao 课程 DAO 对象
     * @return 新增课程主键 ID
     * @throws SQLException 数据库访问异常
     */
    private static Long insertTestCourse(CourseDaoImpl courseDao) throws SQLException {
        // 创建测试课程对象。
        Course course = new Course();

        // 设置固定测试课程编号。
        course.setCourseNo(TEST_COURSE_NO);

        // 设置课程名称。
        course.setName("Service测试课程");

        // 设置课程学分。
        course.setCredit(new BigDecimal("3.0"));

        // 设置任课教师。
        course.setTeacher("Service测试老师");

        // 插入测试课程。
        return courseDao.insert(course);
    }

    /**
     * 给测试学生录入一条成绩。
     *
     * @param scoreDao  成绩 DAO 对象
     * @param studentId 测试学生主键 ID
     * @param courseId  测试课程主键 ID
     * @return 新增成绩主键 ID
     * @throws SQLException 数据库访问异常
     */
    private static Long insertTestScore(ScoreDaoImpl scoreDao, Long studentId, Long courseId) throws SQLException {
        // 创建测试成绩对象。
        Score score = new Score();

        // 关联测试学生。
        score.setStudentId(studentId);

        // 关联测试课程。
        score.setCourseId(courseId);

        // 设置测试成绩。
        score.setScore(new BigDecimal("85.00"));

        // 设置考试日期。
        score.setExamDate(LocalDate.of(2026, 6, 20));

        // 设置成绩备注。
        score.setRemarks("Service测试成绩");

        // 插入测试成绩。
        return scoreDao.insert(score);
    }

    /**
     * 清理测试学生、课程和成绩。
     * <p>注意：必须先删除成绩，再删除课程和学生，避免外键约束导致清理失败。</p>
     *
     * @param studentDao 学生 DAO 对象
     * @param courseDao  课程 DAO 对象
     * @param scoreDao   成绩 DAO 对象
     * @throws SQLException 数据库访问异常
     */
    private static void cleanupTestData(
            StudentDaoImpl studentDao,
            CourseDaoImpl courseDao,
            ScoreDaoImpl scoreDao
    ) throws SQLException {
        // 根据固定测试学号查找学生。
        Student existingStudent = studentDao.findByStudentNo(TEST_STUDENT_NO);

        // 根据固定测试课程编号查找课程。
        Course existingCourse = courseDao.findByCourseNo(TEST_COURSE_NO);

        // 如果学生和课程都存在，先删除它们之间的成绩记录。
        if (existingStudent != null && existingCourse != null) {
            // 查询该学生该课程下的成绩。
            Score existingScore = scoreDao.findByStudentAndCourse(
                    existingStudent.getId(),
                    existingCourse.getId()
            );

            // 如果成绩存在，先删除成绩。
            if (existingScore != null) {
                // 删除成绩。
                scoreDao.deleteById(existingScore.getId());

                // 输出清理提示。
                System.out.println("已清理遗留测试成绩：" + existingScore.getId());
            }
        }

        // 如果课程存在，删除课程。
        if (existingCourse != null) {
            // 删除测试课程。
            courseDao.deleteById(existingCourse.getId());

            // 输出清理提示。
            System.out.println("已清理遗留测试课程：" + TEST_COURSE_NO);
        }

        // 如果学生存在，删除学生。
        if (existingStudent != null) {
            // 删除测试学生。
            studentDao.deleteById(existingStudent.getId());

            // 输出清理提示。
            System.out.println("已清理遗留测试学生：" + TEST_STUDENT_NO);
        }
    }

    /**
     * 校验业务方法抛出指定 IllegalArgumentException。
     *
     * @param action          被测业务动作
     * @param expectedMessage 期望异常提示
     * @param actionName      测试动作名称
     */
    private static void expectIllegalArgument(
            Runnable action,
            String expectedMessage,
            String actionName
    ) {
        try {
            // 执行被测业务方法。
            action.run();
        } catch (IllegalArgumentException exception) {
            // 校验异常提示是否正确。
            if (!expectedMessage.equals(exception.getMessage())) {
                // 如果提示不匹配，说明业务校验结果不符合预期。
                throw new IllegalStateException(
                        actionName + "返回的异常提示不正确，实际值：" + exception.getMessage(),
                        exception
                );
            }

            // 如果异常提示正确，测试通过。
            return;
        }

        // 如果没有抛出异常，说明业务规则没有生效。
        throw new IllegalStateException(actionName + "应该抛出业务异常，但实际执行成功");
    }
}
