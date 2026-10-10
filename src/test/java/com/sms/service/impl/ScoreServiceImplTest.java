package com.sms.service.impl;

// 导入 DAO 实现类，用于准备和清理测试数据。
import com.sms.dao.impl.CourseDaoImpl;
import com.sms.dao.impl.ScoreDaoImpl;
import com.sms.dao.impl.StudentDaoImpl;
// 导入成绩查询参数对象，用于测试明细分页。
import com.sms.model.dto.ScoreQueryDTO;
// 导入实体类，用于构造测试数据。
import com.sms.model.entity.Course;
import com.sms.model.entity.Score;
import com.sms.model.entity.Student;
// 导入分页结果、明细视图和统计视图对象。
import com.sms.model.vo.PageResult;
import com.sms.model.vo.ScoreDetailVO;
import com.sms.model.vo.ScoreStatsVO;
// 导入 SQL 异常，用于捕获数据库访问错误。
import java.sql.SQLException;
// 导入精确小数类型，用于设置分数和学分。
import java.math.BigDecimal;
// 导入日期类型，用于设置考试日期和出生日期。
import java.time.LocalDate;
// 导入 List，用于检查分页结果。
import java.util.List;

/**
 * ScoreServiceImpl 的真实数据库手工测试类。
 * <p>当前项目还没有 JUnit 依赖，所以这里提供 main 方法，可以直接运行验证业务层规则。</p>
 * <p>测试重点：成绩录入、重复录入拦截、分数范围校验、学生/课程存在性校验、修改、明细分页、统计和删除。</p>
 */
public class ScoreServiceImplTest {

    // 测试学生学号；固定值方便测试前清理遗留数据。
    private static final String TEST_STUDENT_NO = "TEST-SVC-SCORE-STU999";

    // 测试课程编号；固定值方便测试前清理遗留数据。
    private static final String TEST_COURSE_NO = "TEST-SVC-SCORE-COURSE999";

    /**
     * 测试入口，右键运行后会按顺序执行成绩业务规则测试。
     *
     * @param args 命令行参数，当前测试不需要传入任何参数
     */
    public static void main(String[] args) {
        // 输出测试开始提示，方便在控制台确认测试已经启动。
        System.out.println("===== ScoreServiceImpl 测试开始 =====");

        // 准备真实 DAO 对象，用于测试数据准备和清理。
        StudentDaoImpl studentDao = new StudentDaoImpl();
        CourseDaoImpl courseDao = new CourseDaoImpl();
        ScoreDaoImpl scoreDao = new ScoreDaoImpl();

        // 创建被测 Service，默认构造器内部会使用真实 DAO 实现。
        ScoreServiceImpl service = new ScoreServiceImpl();

        // 保存测试数据 ID，方便 finally 中判断是否需要兜底清理。
        Long studentId = null;
        Long courseId = null;
        Long scoreId = null;

        try {
            // 先清理可能遗留的测试学生、课程和成绩，保证测试可以重复运行。
            cleanupTestData(studentDao, courseDao, scoreDao);

            // 创建测试学生，并拿到主键 ID。
            studentId = insertTestStudent(studentDao);

            // 创建测试课程，并拿到主键 ID。
            courseId = insertTestCourse(courseDao);

            // 输出测试依赖数据创建结果。
            System.out.println("【0】测试学生和课程创建成功：" + studentId + " / " + courseId);

            // 测试学生不存在时不能录入成绩。
            testAddScoreMissingStudent(service, courseId);

            // 测试课程不存在时不能录入成绩。
            testAddScoreMissingCourse(service, studentId);

            // 测试分数超出最大值时不能录入。
            testAddScoreOutOfRange(service, studentId, courseId, new BigDecimal("150"));

            // 测试分数低于最小值时不能录入。
            testAddScoreOutOfRange(service, studentId, courseId, new BigDecimal("-5"));

            // 正常录入成绩，返回新增成绩主键 ID。
            scoreId = testAddScore(service, studentId, courseId);

            // 测试同一学生同一课程重复录入会被拦截。
            testAddScoreDuplicate(service, studentId, courseId);

            // 测试按 ID 查询成绩。
            testGetScoreById(service, scoreId);

            // 测试正常修改成绩。
            testUpdateScore(service, scoreId);

            // 测试修改时分数超出范围会被拦截。
            testUpdateScoreOutOfRange(service, scoreId);

            // 测试三表 JOIN 明细分页查询和总数统计。
            testQueryScoreDetails(service, studentId, courseId);

            // 测试课程成绩统计报表。
            testScoreStatistics(service, courseId);

            // 测试删除成绩。
            testDeleteScore(service, scoreId);

            // 删除成功后清空 ID，避免 finally 中重复清理。
            scoreId = null;

            // 输出整体测试成功提示。
            System.out.println("===== ScoreServiceImpl 测试全部成功 =====");
        } catch (SQLException exception) {
            // 测试数据准备或数据库访问失败都会进入这里。
            System.err.println("ScoreServiceImpl 测试失败：" + exception.getMessage());
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
     * 创建测试学生。
     *
     * @param studentDao 学生 DAO 对象
     * @return 新增学生主键 ID
     * @throws SQLException 数据库访问异常
     */
    private static Long insertTestStudent(StudentDaoImpl studentDao) throws SQLException {
        // 创建测试学生对象。
        Student student = new Student();

        // 设置固定测试学号。
        student.setStudentNo(TEST_STUDENT_NO);

        // 设置测试姓名。
        student.setName("成绩服务测试学生");

        // 设置性别：1-男。
        student.setGender(1);

        // 设置出生日期。
        student.setBirthDate(LocalDate.of(2004, 4, 18));

        // 设置联系电话。
        student.setPhone("13800001111");

        // 设置电子邮箱。
        student.setEmail("score-service-test@campus.edu.cn");

        // 设置所属专业。
        student.setMajor("计算机科学与技术");

        // 设置班级名称。
        student.setClassName("计科2401班");

        // 设置学籍状态：1-在读。
        student.setStatus(1);

        // 插入测试学生。
        return studentDao.insert(student);
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
        course.setName("成绩服务测试课程");

        // 设置课程学分。
        course.setCredit(new BigDecimal("3.0"));

        // 设置任课教师。
        course.setTeacher("成绩服务测试老师");

        // 插入测试课程。
        return courseDao.insert(course);
    }

    /**
     * 测试学生不存在时不能录入成绩。
     *
     * @param service  被测业务对象
     * @param courseId 测试课程主键 ID
     */
    private static void testAddScoreMissingStudent(ScoreServiceImpl service, Long courseId) {
        // 创建成绩对象，学生 ID 使用一个大概率不存在的值。
        Score score = buildScore(999999999L, courseId, new BigDecimal("80"));

        // 期望业务层抛出“学生不存在”的异常。
        expectIllegalArgument(
                () -> service.addScore(score),
                "学生不存在",
                "录入学生不存在的成绩"
        );

        // 输出测试成功提示。
        System.out.println("【1】学生不存在拦截成功");
    }

    /**
     * 测试课程不存在时不能录入成绩。
     *
     * @param service   被测业务对象
     * @param studentId 测试学生主键 ID
     */
    private static void testAddScoreMissingCourse(ScoreServiceImpl service, Long studentId) {
        // 创建成绩对象，课程 ID 使用一个大概率不存在的值。
        Score score = buildScore(studentId, 999999999L, new BigDecimal("80"));

        // 期望业务层抛出“课程不存在”的异常。
        expectIllegalArgument(
                () -> service.addScore(score),
                "课程不存在",
                "录入课程不存在的成绩"
        );

        // 输出测试成功提示。
        System.out.println("【2】课程不存在拦截成功");
    }

    /**
     * 测试分数超出范围时不能录入成绩。
     *
     * @param service   被测业务对象
     * @param studentId 测试学生主键 ID
     * @param courseId  测试课程主键 ID
     * @param score     非法分数
     */
    private static void testAddScoreOutOfRange(
            ScoreServiceImpl service,
            Long studentId,
            Long courseId,
            BigDecimal score
    ) {
        // 构造分数不合法的成绩对象。
        Score invalidScore = buildScore(studentId, courseId, score);

        // 期望业务层抛出分数范围异常。
        expectIllegalArgument(
                () -> service.addScore(invalidScore),
                "分数必须在 0~100 之间",
                "录入非法分数成绩"
        );

        // 输出测试成功提示。
        System.out.println("【3】非法分数拦截成功：" + score);
    }

    /**
     * 正常录入成绩，并校验主键回填。
     *
     * @param service   被测业务对象
     * @param studentId 测试学生主键 ID
     * @param courseId  测试课程主键 ID
     * @return 新增成绩主键 ID
     */
    private static Long testAddScore(ScoreServiceImpl service, Long studentId, Long courseId) {
        // 构造合法成绩对象。
        Score score = buildScore(studentId, courseId, new BigDecimal("85.50"));

        // 调用业务层新增成绩。
        service.addScore(score);

        // 校验主键 ID 不为空。
        if (score.getId() == null || score.getId() <= 0) {
            // 如果主键无效，说明业务层没有正确回填新增结果。
            throw new IllegalStateException("新增成绩后没有回填有效主键 ID，实际值：" + score.getId());
        }

        // 输出新增成功提示。
        System.out.println("【4】成绩录入成功，ID：" + score.getId());

        // 返回测试成绩 ID，供后续方法继续使用。
        return score.getId();
    }

    /**
     * 测试同一学生同一课程重复录入会被拦截。
     *
     * @param service   被测业务对象
     * @param studentId 测试学生主键 ID
     * @param courseId  测试课程主键 ID
     */
    private static void testAddScoreDuplicate(ScoreServiceImpl service, Long studentId, Long courseId) {
        // 构造同学生同课程的重复成绩对象。
        Score duplicateScore = buildScore(studentId, courseId, new BigDecimal("78"));

        // 期望业务层抛出重复录入异常。
        expectIllegalArgument(
                () -> service.addScore(duplicateScore),
                "该学生本门课程成绩已存在，请走修改流程",
                "重复录入成绩"
        );

        // 输出重复录入拦截成功提示。
        System.out.println("【5】重复成绩录入拦截成功");
    }

    /**
     * 测试按 ID 查询成绩。
     *
     * @param service 被测业务对象
     * @param scoreId 测试成绩主键 ID
     */
    private static void testGetScoreById(ScoreServiceImpl service, Long scoreId) {
        // 调用业务层查询成绩。
        Score score = service.getScoreById(scoreId);

        // 校验成绩存在。
        if (score == null) {
            // 如果查不到，说明按 ID 查询失败。
            throw new IllegalStateException("按 ID 查询不到成绩：" + scoreId);
        }

        // 校验查询到的成绩 ID 正确。
        if (!scoreId.equals(score.getId())) {
            // 如果不一致，说明查询映射有问题。
            throw new IllegalStateException("查询到的成绩 ID 不正确，实际值：" + score.getId());
        }

        // 输出查询成功提示。
        System.out.println("【6】按 ID 查询成绩成功，分数：" + score.getScore());
    }

    /**
     * 正常修改成绩，并校验修改结果。
     *
     * @param service 被测业务对象
     * @param scoreId 测试成绩主键 ID
     */
    private static void testUpdateScore(ScoreServiceImpl service, Long scoreId) {
        // 先查询当前成绩。
        Score score = service.getScoreById(scoreId);

        // 校验成绩存在。
        if (score == null) {
            // 如果不存在，说明前置流程有问题。
            throw new IllegalStateException("修改测试前找不到成绩：" + scoreId);
        }

        // 修改考试分数。
        score.setScore(new BigDecimal("92.50"));

        // 修改考试日期。
        score.setExamDate(LocalDate.of(2026, 6, 22));

        // 修改备注。
        score.setRemarks("Service测试成绩-已更新");

        // 调用业务层修改成绩。
        service.updateScore(score);

        // 重新查询成绩。
        Score updatedScore = service.getScoreById(scoreId);

        // 校验更新后的成绩存在。
        if (updatedScore == null) {
            // 如果查不到，说明更新后查询失败。
            throw new IllegalStateException("更新后查询不到成绩：" + scoreId);
        }

        // 校验分数是否更新成功。
        if (updatedScore.getScore() == null || updatedScore.getScore().compareTo(new BigDecimal("92.50")) != 0) {
            // 如果分数没有变化，说明 update 流程有问题。
            throw new IllegalStateException("更新后的成绩不正确，实际值：" + updatedScore.getScore());
        }

        // 输出更新成功提示。
        System.out.println("【7】成绩修改成功，分数：" + updatedScore.getScore());
    }

    /**
     * 测试修改成绩时分数超出范围会被拦截。
     *
     * @param service 被测业务对象
     * @param scoreId 测试成绩主键 ID
     */
    private static void testUpdateScoreOutOfRange(ScoreServiceImpl service, Long scoreId) {
        // 先查询当前成绩。
        Score score = service.getScoreById(scoreId);

        // 校验成绩存在。
        if (score == null) {
            // 如果不存在，说明前置流程有问题。
            throw new IllegalStateException("更新范围测试前找不到成绩：" + scoreId);
        }

        // 设置非法分数。
        score.setScore(new BigDecimal("120"));

        // 期望业务层抛出分数范围异常。
        expectIllegalArgument(
                () -> service.updateScore(score),
                "分数必须在 0~100 之间",
                "修改为非法分数"
        );

        // 输出更新范围拦截成功提示。
        System.out.println("【8】修改非法分数拦截成功");
    }

    /**
     * 测试三表 JOIN 明细分页查询和总数统计。
     *
     * @param service   被测业务对象
     * @param studentId 测试学生主键 ID
     * @param courseId  测试课程主键 ID
     */
    private static void testQueryScoreDetails(ScoreServiceImpl service, Long studentId, Long courseId) {
        // 创建查询条件。
        ScoreQueryDTO queryDTO = new ScoreQueryDTO();

        // 使用学生 ID 精确筛选。
        queryDTO.setStudentId(studentId);

        // 使用课程 ID 精确筛选。
        queryDTO.setCourseId(courseId);

        // 设置查询第 1 页。
        queryDTO.setPage(1);

        // 设置每页最多 10 条数据。
        queryDTO.setPageSize(10);

        // 调用业务层分页查询。
        PageResult<ScoreDetailVO> pageResult = service.queryScoreDetails(queryDTO);

        // 校验分页结果不为空。
        if (pageResult == null) {
            // 如果为 null，说明 Service 没有正确封装分页对象。
            throw new IllegalStateException("queryScoreDetails 不应该返回 null");
        }

        // 校验总记录数为 1。
        if (pageResult.getTotal() != 1) {
            // 如果总数不是 1，说明条件查询或统计逻辑有问题。
            throw new IllegalStateException("明细分页总数不正确，实际值：" + pageResult.getTotal());
        }

        // 校验当前页数据不为空。
        if (pageResult.getList() == null || pageResult.getList().isEmpty()) {
            // 如果没有数据，说明分页查询有问题。
            throw new IllegalStateException("分页明细数据不应该为空");
        }

        // 获取第一条明细。
        ScoreDetailVO detail = pageResult.getList().get(0);

        // 校验学生 ID 正确。
        if (!studentId.equals(detail.getStudentId())) {
            // 如果不一致，说明 JOIN 映射有问题。
            throw new IllegalStateException("明细学生 ID 不正确，实际值：" + detail.getStudentId());
        }

        // 校验课程 ID 正确。
        if (!courseId.equals(detail.getCourseId())) {
            // 如果不一致，说明 JOIN 映射有问题。
            throw new IllegalStateException("明细课程 ID 不正确，实际值：" + detail.getCourseId());
        }

        // 输出明细查询成功提示。
        System.out.println("【9】成绩明细分页查询成功，总数：" + pageResult.getTotal());
    }

    /**
     * 测试课程成绩统计报表。
     *
     * @param service  被测业务对象
     * @param courseId 测试课程主键 ID
     */
    private static void testScoreStatistics(ScoreServiceImpl service, Long courseId) {
        // 调用业务层统计接口。
        ScoreStatsVO stats = service.getScoreStatistics(courseId);

        // 校验统计对象不为空。
        if (stats == null) {
            // 如果为空，说明统计查询没有返回结果。
            throw new IllegalStateException("成绩统计结果不应该为 null");
        }

        // 校验课程 ID 正确。
        if (!courseId.equals(stats.getCourseId())) {
            // 如果不一致，说明统计映射有问题。
            throw new IllegalStateException("统计课程 ID 不正确，实际值：" + stats.getCourseId());
        }

        // 校验参加考试总人数至少为 1。
        if (stats.getTotalStudents() < 1) {
            // 如果小于 1，说明统计没有包含测试成绩。
            throw new IllegalStateException("统计总人数不正确，实际值：" + stats.getTotalStudents());
        }

        // 校验平均分和测试分数一致。
        if (stats.getAverageScore() == null || stats.getAverageScore().compareTo(new BigDecimal("92.50")) != 0) {
            // 如果不一致，说明聚合查询或映射有问题。
            throw new IllegalStateException("平均分统计不正确，实际值：" + stats.getAverageScore());
        }

        // 校验及格率为 100%。
        if (stats.getPassRate() == null || stats.getPassRate().compareTo(new BigDecimal("100")) != 0) {
            // 如果不一致，说明及格率计算有问题。
            throw new IllegalStateException("及格率统计不正确，实际值：" + stats.getPassRate());
        }

        // 输出统计成功提示。
        System.out.println("【10】成绩统计成功，平均分：" + stats.getAverageScore() + "，及格率：" + stats.getPassRate());
    }

    /**
     * 测试删除成绩，并验证删除后查询不到。
     *
     * @param service 被测业务对象
     * @param scoreId 待删除的成绩主键 ID
     */
    private static void testDeleteScore(ScoreServiceImpl service, Long scoreId) {
        // 调用业务层删除成绩。
        service.deleteScore(scoreId);

        // 删除后再次查询。
        Score deletedScore = service.getScoreById(scoreId);

        // 校验删除后查询结果为 null。
        if (deletedScore != null) {
            // 如果还能查到，说明删除流程失败。
            throw new IllegalStateException("删除后仍然可以查询到成绩：" + scoreId);
        }

        // 输出删除成功提示。
        System.out.println("【11】删除成绩成功");
    }

    /**
     * 构造成绩测试对象。
     *
     * @param studentId 学生物理主键 ID
     * @param courseId  课程物理主键 ID
     * @param score     考试分数
     * @return 成绩实体
     */
    private static Score buildScore(Long studentId, Long courseId, BigDecimal score) {
        // 创建空成绩对象。
        Score result = new Score();

        // 设置学生 ID。
        result.setStudentId(studentId);

        // 设置课程 ID。
        result.setCourseId(courseId);

        // 设置考试成绩。
        result.setScore(score);

        // 设置考试日期。
        result.setExamDate(LocalDate.of(2026, 6, 20));

        // 设置成绩备注。
        result.setRemarks("Service测试成绩");

        // 返回测试成绩对象。
        return result;
    }

    /**
     * 清理测试学生、课程和成绩数据。
     * <p>注意：必须先删除成绩，再删除课程和学生，否则外键约束可能导致清理失败。</p>
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
