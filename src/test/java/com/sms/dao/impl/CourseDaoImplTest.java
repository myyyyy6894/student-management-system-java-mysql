package com.sms.dao.impl;

// 导入课程实体类，用来构造测试数据。
import com.sms.model.entity.Course;
// 导入 SQL 异常，用于捕获数据库访问错误。
import java.sql.SQLException;
// 导入精确小数类型，用于设置课程学分。
import java.math.BigDecimal;
// 导入 List，用于检查 findAll 查询结果。
import java.util.List;

/**
 * CourseDaoImpl 的真实数据库手工测试类。
 * <p>当前项目还没有 JUnit 依赖，所以这里提供 main 方法，可以直接运行验证 DAO 层。</p>
 * <p>测试流程覆盖：新增、按 ID 查询、按课程编号查询、更新、查询全部课程、删除和删除后查询。</p>
 */
public class CourseDaoImplTest {

    // 测试课程编号；固定值可以用于测试前后的清理，避免污染数据库。
    private static final String TEST_COURSE_NO = "TEST-CS999";

    /**
     * 测试入口，右键运行后会按顺序执行完整的课程增删改查流程。
     *
     * @param args 命令行参数，当前测试不需要传入任何参数
     */
    public static void main(String[] args) {
        // 输出测试开始提示，方便确认测试已经启动。
        System.out.println("===== CourseDaoImpl 测试开始 =====");

        // 创建被测 DAO 对象。
        CourseDaoImpl dao = new CourseDaoImpl();

        // 用于保存新增后的课程主键；初始为 null，方便 finally 中判断是否需要兜底清理。
        Long courseId = null;

        try {
            // 先清理可能遗留的测试课程，保证测试可以重复运行。
            deleteTestCourseIfExists(dao, TEST_COURSE_NO);

            // 执行新增和查询测试，返回新生成的课程主键 ID。
            courseId = testInsertAndFind(dao);

            // 执行更新测试。
            testUpdate(dao, courseId);

            // 执行查询全部课程测试。
            testFindAll(dao);

            // 执行删除测试。
            testDelete(dao, courseId);

            // 删除成功后把主键置为 null，避免 finally 中再次清理。
            courseId = null;

            // 输出整体测试成功提示。
            System.out.println("===== CourseDaoImpl 测试全部成功 =====");
        } catch (SQLException exception) {
            // 数据库连接、SQL 执行失败等都会进入这里。
            System.err.println("CourseDaoImpl 测试失败：" + exception.getMessage());
            // 打印完整异常堆栈，便于定位问题。
            exception.printStackTrace();
        } finally {
            // finally 中的兜底清理逻辑用于处理中途抛异常的情况。
            if (courseId != null) {
                try {
                    // 按课程编号清理测试数据，避免数据库留下脏数据。
                    deleteTestCourseIfExists(dao, TEST_COURSE_NO);
                } catch (SQLException cleanupException) {
                    // 清理失败也要提示，方便手动检查数据库。
                    System.err.println("测试课程清理失败：" + cleanupException.getMessage());
                }
            }
        }
    }

    /**
     * 测试新增课程，并分别验证 findById 和 findByCourseNo。
     *
     * @param dao 被测 DAO 对象
     * @return 新增课程的主键 ID
     * @throws SQLException 数据库访问异常
     */
    private static Long testInsertAndFind(CourseDaoImpl dao) throws SQLException {
        // 创建测试课程对象。
        Course course = new Course();

        // 设置固定测试课程编号。
        course.setCourseNo(TEST_COURSE_NO);

        // 设置课程名称。
        course.setName("DAO测试课程");

        // 设置课程学分。
        course.setCredit(new BigDecimal("3.5"));

        // 设置任课教师。
        course.setTeacher("测试老师");

        // 调用 DAO 的 insert 方法，并获取数据库生成的自增主键。
        Long courseId = dao.insert(course);

        // 校验主键 ID 不为 null。
        if (courseId == null || courseId <= 0) {
            // 如果主键无效，说明 insert 没有正确返回数据库生成的主键。
            throw new IllegalStateException("新增课程后没有返回有效主键 ID，实际值：" + courseId);
        }

        // 输出新增成功信息。
        System.out.println("【1】新增课程成功，ID：" + courseId);

        // 根据 ID 查询刚刚插入的课程。
        Course foundById = dao.findById(courseId);

        // 校验查询结果不为空。
        if (foundById == null) {
            // 如果为空，说明按 ID 查询失败。
            throw new IllegalStateException("按 ID 查询失败，找不到课程：" + courseId);
        }

        // 校验查询到的课程编号是否与新增时一致。
        if (!TEST_COURSE_NO.equals(foundById.getCourseNo())) {
            // 如果不一致，说明结果映射有问题。
            throw new IllegalStateException("按 ID 查询到的课程编号不正确，实际值：" + foundById.getCourseNo());
        }

        // 输出按 ID 查询成功信息。
        System.out.println("【2】按 ID 查询成功：" + foundById.getName());

        // 按课程编号查询同一个课程。
        Course foundByNo = dao.findByCourseNo(TEST_COURSE_NO);

        // 校验按课程编号查询结果不为空。
        if (foundByNo == null) {
            // 如果为空，说明按课程编号查询失败。
            throw new IllegalStateException("按课程编号查询失败，找不到课程：" + TEST_COURSE_NO);
        }

        // 校验两个查询方法返回的主键一致。
        if (!courseId.equals(foundByNo.getId())) {
            // 如果不一致，说明按课程编号查询到的不是同一条记录。
            throw new IllegalStateException("按课程编号查询到的主键 ID 不正确，实际值：" + foundByNo.getId());
        }

        // 输出按课程编号查询成功信息。
        System.out.println("【3】按课程编号查询成功：" + foundByNo.getName());

        // 返回测试课程 ID，供后续方法继续使用。
        return courseId;
    }

    /**
     * 测试更新课程信息。
     *
     * @param dao      被测 DAO 对象
     * @param courseId 待更新的课程主键 ID
     * @throws SQLException 数据库访问异常
     */
    private static void testUpdate(CourseDaoImpl dao, Long courseId) throws SQLException {
        // 先根据 ID 查询当前课程。
        Course course = dao.findById(courseId);

        // 校验课程存在。
        if (course == null) {
            // 如果不存在，说明前置查询或数据库状态有问题。
            throw new IllegalStateException("更新测试前找不到课程：" + courseId);
        }

        // 修改课程名称。
        course.setName("DAO测试课程-已更新");

        // 修改课程学分。
        course.setCredit(new BigDecimal("4.0"));

        // 修改任课教师。
        course.setTeacher("测试老师-已更新");

        // 调用 DAO 的 update 方法。
        int affectedRows = dao.update(course);

        // 校验数据库受影响行数是否为 1。
        if (affectedRows != 1) {
            // 如果不是 1，说明更新没有按预期执行。
            throw new IllegalStateException("更新课程失败，受影响行数：" + affectedRows);
        }

        // 重新根据 ID 查询更新后的课程。
        Course updatedCourse = dao.findById(courseId);

        // 校验更新后的课程仍然存在。
        if (updatedCourse == null) {
            // 如果不存在，说明更新后查询失败。
            throw new IllegalStateException("更新后查询不到课程：" + courseId);
        }

        // 校验课程名称是否更新成功。
        if (!"DAO测试课程-已更新".equals(updatedCourse.getName())) {
            // 如果名称没有变化，说明 update 映射或 SQL 有问题。
            throw new IllegalStateException("更新后的课程名称不正确，实际值：" + updatedCourse.getName());
        }

        // 校验学分是否更新成功。
        if (updatedCourse.getCredit() == null || updatedCourse.getCredit().compareTo(new BigDecimal("4.0")) != 0) {
            // 如果学分没有变化，说明 update 映射或 SQL 有问题。
            throw new IllegalStateException("更新后的课程学分不正确，实际值：" + updatedCourse.getCredit());
        }

        // 输出更新成功信息。
        System.out.println("【4】更新课程成功：" + updatedCourse.getName());
    }

    /**
     * 测试查询全部课程。
     *
     * @param dao 被测 DAO 对象
     * @throws SQLException 数据库访问异常
     */
    private static void testFindAll(CourseDaoImpl dao) throws SQLException {
        // 调用 findAll 查询全部课程。
        List<Course> courses = dao.findAll();

        // 校验结果集合不为 null。
        if (courses == null) {
            // 如果为 null，说明 DAO 没有按约定返回空集合。
            throw new IllegalStateException("findAll 查询结果不应该返回 null");
        }

        // 查找测试课程是否存在。
        boolean found = courses.stream().anyMatch(course -> TEST_COURSE_NO.equals(course.getCourseNo()));

        // 校验结果集合中包含测试课程。
        if (!found) {
            // 如果不存在，说明 findAll 查询或结果映射有问题。
            throw new IllegalStateException("findAll 查询结果中没有测试课程：" + TEST_COURSE_NO);
        }

        // 输出查询成功信息。
        System.out.println("【5】查询全部课程成功，总数：" + courses.size());
    }

    /**
     * 测试删除课程，并验证删除后查询不到。
     *
     * @param dao      被测 DAO 对象
     * @param courseId 待删除的课程主键 ID
     * @throws SQLException 数据库访问异常
     */
    private static void testDelete(CourseDaoImpl dao, Long courseId) throws SQLException {
        // 调用 DAO 的删除方法。
        int affectedRows = dao.deleteById(courseId);

        // 校验删除操作只影响 1 行。
        if (affectedRows != 1) {
            // 如果不是 1，说明删除没有按预期执行。
            throw new IllegalStateException("删除课程失败，受影响行数：" + affectedRows);
        }

        // 删除后再次查询。
        Course deletedCourse = dao.findById(courseId);

        // 校验删除后查询结果为 null。
        if (deletedCourse != null) {
            // 如果还能查到，说明删除失败或查询缓存有问题。
            throw new IllegalStateException("删除后仍然可以查询到课程：" + courseId);
        }

        // 输出删除成功信息。
        System.out.println("【6】删除课程成功");
    }

    /**
     * 如果测试课程已经存在，则删除该课程。
     * <p>这个方法用于测试开始前的重复数据清理，以及测试失败后的 finally 兜底清理。</p>
     *
     * @param dao      被测 DAO 对象
     * @param courseNo 测试课程编号
     * @throws SQLException 数据库访问异常
     */
    private static void deleteTestCourseIfExists(CourseDaoImpl dao, String courseNo) throws SQLException {
        // 先根据课程编号查询测试课程。
        Course existingCourse = dao.findByCourseNo(courseNo);

        // 如果测试课程不存在，直接返回。
        if (existingCourse == null) {
            return;
        }

        // 如果测试课程存在，则按主键删除。
        dao.deleteById(existingCourse.getId());

        // 输出清理提示，方便观察测试环境状态。
        System.out.println("已清理遗留测试课程：" + courseNo);
    }
}
