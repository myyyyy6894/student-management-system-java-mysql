package com.sms.dao.impl;

// 导入成绩查询参数对象，用来测试明细分页和统计条件。
import com.sms.model.dto.ScoreQueryDTO;
// 导入课程实体类，用来创建测试课程。
import com.sms.model.entity.Course;
// 导入成绩实体类，用来构造新增和更新的数据。
import com.sms.model.entity.Score;
// 导入学生实体类，用来创建测试学生。
import com.sms.model.entity.Student;
// 导入成绩明细视图对象，用来校验三表 JOIN 查询结果。
import com.sms.model.vo.ScoreDetailVO;
// 导入成绩统计视图对象，用来校验统计查询结果。
import com.sms.model.vo.ScoreStatsVO;
// 导入 SQL 异常，用于捕获数据库访问错误。
import java.sql.SQLException;
// 导入精确小数类型，用于设置成绩和学分。
import java.math.BigDecimal;
// 导入日期类，用于设置考试日期。
import java.time.LocalDate;
// 导入 List，用于检查分页查询结果。
import java.util.List;

/**
 * ScoreDaoImpl 的真实数据库手工测试类。
 * <p>当前项目还没有 JUnit 依赖，所以这里提供 main 方法，可以直接运行验证 DAO 层。</p>
 * <p>测试流程覆盖：新增、按 ID 查询、按学生和课程查询、更新、统计、明细分页、统计报表、删除和删除后查询。</p>
 */
public class ScoreDaoImplTest {

    // 测试学生学号；固定值可以用于测试前后的清理，避免污染数据库。
    private static final String TEST_STUDENT_NO = "TEST-STU999";

    // 测试课程编号；固定值可以用于测试前后的清理，避免污染数据库。
    private static final String TEST_COURSE_NO = "TEST-SCORE-CS999";

    /**
     * 测试入口，右键运行后会按顺序执行完整的成绩增删改查流程。
     *
     * @param args 命令行参数，当前测试不需要传入任何参数
     */
    public static void main(String[] args) {
        // 输出测试开始提示，方便在控制台确认测试已经启动。
        System.out.println("===== ScoreDaoImpl 测试开始 =====");

        // 创建测试依赖的 DAO 对象。
        StudentDaoImpl studentDao = new StudentDaoImpl();
        CourseDaoImpl courseDao = new CourseDaoImpl();
        ScoreDaoImpl scoreDao = new ScoreDaoImpl();

        // 用于保存测试学生、课程和成绩的主键，方便测试结束后清理。
        Long studentId = null;
        Long courseId = null;
        Long scoreId = null;

        try {
            // 先清理可能遗留的测试成绩、课程和学生，保证测试可以重复运行。
            cleanupTestData(studentDao, courseDao, scoreDao);

            // 创建测试学生和测试课程，因为成绩表依赖这两个外键。
            studentId = insertTestStudent(studentDao);
            courseId = insertTestCourse(courseDao);

            // 输出依赖数据创建结果。
            System.out.println("【0】测试学生和课程创建成功：" + studentId + " / " + courseId);

            // 执行新增和单表查询测试，返回新生成的成绩主键 ID。
            scoreId = testInsertAndFind(scoreDao, studentId, courseId);

            // 执行更新测试。
            testUpdate(scoreDao, scoreId);

            // 执行统计数量测试。
            testCount(scoreDao, studentId, courseId);

            // 执行三表 JOIN 分页查询测试。
            testDetailPage(scoreDao, studentId, courseId);

            // 执行成绩统计报表测试。
            testStatistics(scoreDao, courseId);

            // 执行删除测试。
            testDelete(scoreDao, scoreId);

            // 删除成功后把主键置为 null，避免 finally 中再次清理。
            scoreId = null;

            // 输出整体测试成功提示。
            System.out.println("===== ScoreDaoImpl 测试全部成功 =====");
        } catch (SQLException exception) {
            // 数据库连接、SQL 执行失败等都会进入这里。
            System.err.println("ScoreDaoImpl 测试失败：" + exception.getMessage());
            // 打印完整异常堆栈，便于定位问题。
            exception.printStackTrace();
        } finally {
            // finally 中的兜底清理逻辑用于处理中途抛异常的情况。
            try {
                // 按顺序清理成绩、课程和学生，避免外键依赖导致清理失败。
                cleanupTestData(studentDao, courseDao, scoreDao);
            } catch (SQLException cleanupException) {
                // 清理失败也要提示，方便手动检查数据库。
                System.err.println("测试数据清理失败：" + cleanupException.getMessage());
            }
        }
    }

    /**
     * 创建测试学生，并返回新生成的学生主键 ID。
     *
     * @param studentDao 学生 DAO 对象
     * @return 测试学生主键 ID
     * @throws SQLException 数据库访问异常
     */
    private static Long insertTestStudent(StudentDaoImpl studentDao) throws SQLException {
        // 创建测试学生对象。
        Student student = new Student();

        // 设置固定测试学号。
        student.setStudentNo(TEST_STUDENT_NO);

        // 设置测试姓名。
        student.setName("成绩测试学生");

        // 设置性别：1-男。
        student.setGender(1);

        // 设置出生日期。
        student.setBirthDate(LocalDate.of(2004, 3, 20));

        // 设置联系电话。
        student.setPhone("13900009999");

        // 设置电子邮箱。
        student.setEmail("score-test@campus.edu.cn");

        // 设置所属专业。
        student.setMajor("计算机科学与技术");

        // 设置班级名称。
        student.setClassName("计科2401班");

        // 设置学籍状态：1-在读。
        student.setStatus(1);

        // 插入测试学生并返回主键。
        return studentDao.insert(student);
    }

    /**
     * 创建测试课程，并返回新生成的课程主键 ID。
     *
     * @param courseDao 课程 DAO 对象
     * @return 测试课程主键 ID
     * @throws SQLException 数据库访问异常
     */
    private static Long insertTestCourse(CourseDaoImpl courseDao) throws SQLException {
        // 创建测试课程对象。
        Course course = new Course();

        // 设置固定测试课程编号。
        course.setCourseNo(TEST_COURSE_NO);

        // 设置课程名称。
        course.setName("成绩测试课程");

        // 设置课程学分。
        course.setCredit(new BigDecimal("3.0"));

        // 设置任课教师。
        course.setTeacher("测试老师");

        // 插入测试课程并返回主键。
        return courseDao.insert(course);
    }

    /**
     * 测试新增成绩，并分别验证 findById 和 findByStudentAndCourse。
     *
     * @param scoreDao  被测成绩 DAO 对象
     * @param studentId 测试学生主键 ID
     * @param courseId  测试课程主键 ID
     * @return 新增成绩的主键 ID
     * @throws SQLException 数据库访问异常
     */
    private static Long testInsertAndFind(ScoreDaoImpl scoreDao, Long studentId, Long courseId) throws SQLException {
        // 创建测试成绩对象。
        Score score = new Score();

        // 设置学生 ID。
        score.setStudentId(studentId);

        // 设置课程 ID。
        score.setCourseId(courseId);

        // 设置考试成绩。
        score.setScore(new BigDecimal("85.50"));

        // 设置考试日期。
        score.setExamDate(LocalDate.of(2026, 6, 20));

        // 设置成绩备注。
        score.setRemarks("DAO测试成绩");

        // 调用 DAO 的 insert 方法，并获取数据库生成的自增主键。
        Long scoreId = scoreDao.insert(score);

        // 校验主键 ID 不为 null。
        if (scoreId == null || scoreId <= 0) {
            // 如果主键无效，说明 insert 没有正确返回数据库生成的主键。
            throw new IllegalStateException("新增成绩后没有返回有效主键 ID，实际值：" + scoreId);
        }

        // 输出新增成功信息。
        System.out.println("【1】新增成绩成功，ID：" + scoreId);

        // 根据 ID 查询刚刚插入的成绩。
        Score foundById = scoreDao.findById(scoreId);

        // 校验查询结果不为空。
        if (foundById == null) {
            // 如果为空，说明按 ID 查询失败。
            throw new IllegalStateException("按 ID 查询失败，找不到成绩：" + scoreId);
        }

        // 校验查询到的学生 ID 是否正确。
        if (!studentId.equals(foundById.getStudentId())) {
            // 如果不一致，说明结果映射有问题。
            throw new IllegalStateException("按 ID 查询到的学生 ID 不正确，实际值：" + foundById.getStudentId());
        }

        // 输出按 ID 查询成功信息。
        System.out.println("【2】按 ID 查询成功，分数：" + foundById.getScore());

        // 按学生 ID 和课程 ID 查询同一份成绩。
        Score foundByStudentAndCourse = scoreDao.findByStudentAndCourse(studentId, courseId);

        // 校验按组合条件查询结果不为空。
        if (foundByStudentAndCourse == null) {
            // 如果为空，说明按学生和课程查询失败。
            throw new IllegalStateException("按学生和课程查询失败");
        }

        // 校验两个查询方法返回的主键一致。
        if (!scoreId.equals(foundByStudentAndCourse.getId())) {
            // 如果不一致，说明按组合条件查询到的不是同一条记录。
            throw new IllegalStateException("按学生和课程查询到的成绩 ID 不正确，实际值：" + foundByStudentAndCourse.getId());
        }

        // 输出按组合条件查询成功信息。
        System.out.println("【3】按学生和课程查询成功");

        // 返回测试成绩 ID，供后续方法继续使用。
        return scoreId;
    }

    /**
     * 测试更新成绩信息。
     *
     * @param scoreDao 被测 DAO 对象
     * @param scoreId  待更新的成绩主键 ID
     * @throws SQLException 数据库访问异常
     */
    private static void testUpdate(ScoreDaoImpl scoreDao, Long scoreId) throws SQLException {
        // 先根据 ID 查询当前成绩。
        Score score = scoreDao.findById(scoreId);

        // 校验成绩存在。
        if (score == null) {
            // 如果不存在，说明前置查询或数据库状态有问题。
            throw new IllegalStateException("更新测试前找不到成绩：" + scoreId);
        }

        // 修改考试成绩。
        score.setScore(new BigDecimal("92.50"));

        // 修改考试日期。
        score.setExamDate(LocalDate.of(2026, 6, 25));

        // 修改备注。
        score.setRemarks("DAO测试成绩-已更新");

        // 调用 DAO 的 update 方法。
        int affectedRows = scoreDao.update(score);

        // 校验数据库受影响行数是否为 1。
        if (affectedRows != 1) {
            // 如果不是 1，说明更新没有按预期执行。
            throw new IllegalStateException("更新成绩失败，受影响行数：" + affectedRows);
        }

        // 重新根据 ID 查询更新后的成绩。
        Score updatedScore = scoreDao.findById(scoreId);

        // 校验更新后的成绩仍然存在。
        if (updatedScore == null) {
            // 如果不存在，说明更新后查询失败。
            throw new IllegalStateException("更新后查询不到成绩：" + scoreId);
        }

        // 校验分数是否更新成功。
        if (updatedScore.getScore() == null || updatedScore.getScore().compareTo(new BigDecimal("92.50")) != 0) {
            // 如果分数没有变化，说明 update 映射或 SQL 有问题。
            throw new IllegalStateException("更新后的成绩不正确，实际值：" + updatedScore.getScore());
        }

        // 输出更新成功信息。
        System.out.println("【4】更新成绩成功，分数：" + updatedScore.getScore());
    }

    /**
     * 测试按学生和课程统计成绩记录数量。
     *
     * @param scoreDao  被测 DAO 对象
     * @param studentId 测试学生主键 ID
     * @param courseId  测试课程主键 ID
     * @throws SQLException 数据库访问异常
     */
    private static void testCount(ScoreDaoImpl scoreDao, Long studentId, Long courseId) throws SQLException {
        // 统计某个学生的成绩数量。
        long studentScoreCount = scoreDao.countByStudentId(studentId);

        // 校验该学生至少有一条成绩。
        if (studentScoreCount < 1) {
            // 如果小于 1，说明统计查询有问题。
            throw new IllegalStateException("学生成绩统计总数不正确，实际值：" + studentScoreCount);
        }

        // 输出学生成绩统计结果。
        System.out.println("【5】学生成绩统计成功，总数：" + studentScoreCount);

        // 统计某门课程的成绩数量。
        long courseScoreCount = scoreDao.countByCourseId(courseId);

        // 校验该课程至少有一条成绩。
        if (courseScoreCount < 1) {
            // 如果小于 1，说明统计查询有问题。
            throw new IllegalStateException("课程成绩统计总数不正确，实际值：" + courseScoreCount);
        }

        // 输出课程成绩统计结果。
        System.out.println("【6】课程成绩统计成功，总数：" + courseScoreCount);
    }

    /**
     * 测试成绩三表 JOIN 明细分页查询。
     *
     * @param scoreDao  被测 DAO 对象
     * @param studentId 测试学生主键 ID
     * @param courseId  测试课程主键 ID
     * @throws SQLException 数据库访问异常
     */
    private static void testDetailPage(ScoreDaoImpl scoreDao, Long studentId, Long courseId) throws SQLException {
        // 创建查询条件对象。
        ScoreQueryDTO queryDTO = new ScoreQueryDTO();

        // 使用学生 ID 精确筛选。
        queryDTO.setStudentId(studentId);

        // 使用课程 ID 精确筛选。
        queryDTO.setCourseId(courseId);

        // 设置查询第 1 页。
        queryDTO.setPage(1);

        // 设置每页最多 10 条数据。
        queryDTO.setPageSize(10);

        // 调用分页明细查询方法。
        List<ScoreDetailVO> details = scoreDao.findDetailByPage(queryDTO);

        // 校验分页结果不为 null。
        if (details == null) {
            // 如果为 null，说明 DAO 没有按约定返回空集合。
            throw new IllegalStateException("分页明细查询不应该返回 null");
        }

        // 校验分页结果中确实包含测试成绩。
        if (details.isEmpty()) {
            // 如果集合为空，说明三表 JOIN 或条件查询有问题。
            throw new IllegalStateException("分页明细查询没有找到测试成绩");
        }

        // 获取第一条明细。
        ScoreDetailVO firstDetail = details.get(0);

        // 校验学生 ID 是否正确。
        if (!studentId.equals(firstDetail.getStudentId())) {
            // 如果不一致，说明 JOIN 映射有问题。
            throw new IllegalStateException("明细查询学生 ID 不正确，实际值：" + firstDetail.getStudentId());
        }

        // 校验课程 ID 是否正确。
        if (!courseId.equals(firstDetail.getCourseId())) {
            // 如果不一致，说明 JOIN 映射有问题。
            throw new IllegalStateException("明细查询课程 ID 不正确，实际值：" + firstDetail.getCourseId());
        }

        // 输出明细查询成功信息。
        System.out.println("【7】成绩明细查询成功，条数：" + details.size());

        // 调用总记录数统计方法。
        long total = scoreDao.countDetailByCondition(queryDTO);

        // 校验统计总数至少为 1。
        if (total < 1) {
            // 如果小于 1，说明统计 SQL 与分页查询条件不一致。
            throw new IllegalStateException("明细条件统计总数不正确，实际值：" + total);
        }

        // 输出明细统计成功信息。
        System.out.println("【8】成绩明细统计成功，总数：" + total);
    }

    /**
     * 测试课程成绩统计报表。
     *
     * @param scoreDao 被测 DAO 对象
     * @param courseId 测试课程主键 ID
     * @throws SQLException 数据库访问异常
     */
    private static void testStatistics(ScoreDaoImpl scoreDao, Long courseId) throws SQLException {
        // 调用统计方法，查询测试课程的成绩统计。
        ScoreStatsVO stats = scoreDao.calculateStatistics(courseId);

        // 校验统计对象不为空。
        if (stats == null) {
            // 如果为空，说明统计查询没有返回结果。
            throw new IllegalStateException("成绩统计结果不应该为 null");
        }

        // 校验课程 ID 是否正确。
        if (!courseId.equals(stats.getCourseId())) {
            // 如果不一致，说明统计映射有问题。
            throw new IllegalStateException("成绩统计课程 ID 不正确，实际值：" + stats.getCourseId());
        }

        // 校验参加考试总人数至少为 1。
        if (stats.getTotalStudents() < 1) {
            // 如果小于 1，说明统计没有包含测试成绩。
            throw new IllegalStateException("成绩统计总人数不正确，实际值：" + stats.getTotalStudents());
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

        // 输出统计成功信息。
        System.out.println("【9】成绩统计成功，平均分：" + stats.getAverageScore() + "，及格率：" + stats.getPassRate());
    }

    /**
     * 测试删除成绩，并验证删除后查询不到。
     *
     * @param scoreDao 被测 DAO 对象
     * @param scoreId  待删除的成绩主键 ID
     * @throws SQLException 数据库访问异常
     */
    private static void testDelete(ScoreDaoImpl scoreDao, Long scoreId) throws SQLException {
        // 调用 DAO 的删除方法。
        int affectedRows = scoreDao.deleteById(scoreId);

        // 校验删除操作只影响 1 行。
        if (affectedRows != 1) {
            // 如果不是 1，说明删除没有按预期执行。
            throw new IllegalStateException("删除成绩失败，受影响行数：" + affectedRows);
        }

        // 删除后再次查询。
        Score deletedScore = scoreDao.findById(scoreId);

        // 校验删除后查询结果为 null。
        if (deletedScore != null) {
            // 如果还能查到，说明删除失败或查询缓存有问题。
            throw new IllegalStateException("删除后仍然可以查询到成绩：" + scoreId);
        }

        // 输出删除成功信息。
        System.out.println("【10】删除成绩成功");
    }

    /**
     * 清理测试学生、课程和成绩数据。
     * <p>注意：必须先删除成绩，再删除课程和学生，否则外键约束可能导致删除失败。</p>
     *
     * @param studentDao 学生 DAO 对象
     * @param courseDao  课程 DAO 对象
     * @param scoreDao   成绩 DAO 对象
     * @throws SQLException 数据库访问异常
     */
    private static void cleanupTestData(StudentDaoImpl studentDao, CourseDaoImpl courseDao, ScoreDaoImpl scoreDao) throws SQLException {
        // 先查找测试学生。
        Student existingStudent = studentDao.findByStudentNo(TEST_STUDENT_NO);

        // 再查找测试课程。
        Course existingCourse = courseDao.findByCourseNo(TEST_COURSE_NO);

        // 如果学生和课程都存在，就先删除它们之间的成绩记录。
        if (existingStudent != null && existingCourse != null) {
            // 查询该学生该课程下的测试成绩。
            Score existingScore = scoreDao.findByStudentAndCourse(existingStudent.getId(), existingCourse.getId());

            // 如果成绩存在，则删除。
            if (existingScore != null) {
                scoreDao.deleteById(existingScore.getId());
                // 输出清理提示。
                System.out.println("已清理遗留测试成绩：" + existingScore.getId());
            }
        }

        // 如果测试课程存在，则删除课程。
        if (existingCourse != null) {
            courseDao.deleteById(existingCourse.getId());
            // 输出清理提示。
            System.out.println("已清理遗留测试课程：" + TEST_COURSE_NO);
        }

        // 如果测试学生存在，则删除学生。
        if (existingStudent != null) {
            studentDao.deleteById(existingStudent.getId());
            // 输出清理提示。
            System.out.println("已清理遗留测试学生：" + TEST_STUDENT_NO);
        }
    }
}
