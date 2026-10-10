package com.sms.dao.impl;

// 导入学生查询参数对象，用来测试分页和条件统计。
import com.sms.model.dto.StudentQueryDTO;
// 导入学生实体类，用来构造新增和更新的数据。
import com.sms.model.entity.Student;
// 导入数据库工具类，用于测试前后的数据库连接和清理。
import com.sms.util.DBUtil;
// 导入 SQL 异常，用于捕获数据库访问错误。
import java.sql.SQLException;
// 导入日期类，用于构造学生出生日期。
import java.time.LocalDate;
// 导入 List，用于检查分页查询结果。
import java.util.List;

/**
 * StudentDaoImpl 的真实数据库手工测试类。
 * <p>当前项目还没有 JUnit 依赖，所以这里提供 main 方法，可以直接运行验证 DAO 层。</p>
 * <p>测试流程覆盖：新增、按 ID 查询、按学号查询、更新、分页查询、统计查询、删除和删除后查询。</p>
 */
public class StudentDaoImplTest {

    // 测试学生的固定学号；这个值会用于测试前后的清理，避免污染数据库。
    private static final String TEST_STUDENT_NO = "TEST20240999";

    /**
     * 测试入口，右键运行后会按顺序执行完整的增删改查流程。
     *
     * @param args 命令行参数，当前测试不需要传入任何参数
     */
    public static void main(String[] args) {
        // 输出测试开始提示，方便在控制台确认测试已经启动。
        System.out.println("===== StudentDaoImpl 测试开始 =====");

        // 创建被测 DAO 对象。
        StudentDaoImpl dao = new StudentDaoImpl();

        // 用于保存新增后的学生主键；初始为 null，方便 finally 中判断是否需要兜底清理。
        Long studentId = null;

        try {
            // 先清理可能遗留的测试学生，保证测试可以重复运行。
            deleteTestStudentIfExists(dao, TEST_STUDENT_NO);

            // 执行新增和查询测试，返回新生成的学生主键 ID。
            studentId = testInsertAndFind(dao);

            // 执行更新测试。
            testUpdate(dao, studentId);

            // 执行分页查询和总记录数统计测试。
            testPageAndCount(dao);

            // 执行删除测试。
            testDelete(dao, studentId);

            // 删除成功后把主键置为 null，避免 finally 中再次清理。
            studentId = null;

            // 输出整体测试成功提示。
            System.out.println("===== StudentDaoImpl 测试全部成功 =====");
        } catch (SQLException exception) {
            // 数据库连接、SQL 执行失败等都会进入这里。
            System.err.println("StudentDaoImpl 测试失败：" + exception.getMessage());
            // 打印完整异常堆栈，便于定位问题。
            exception.printStackTrace();
        } finally {
            // finally 中的兜底清理逻辑用于处理中途抛异常的情况。
            // 如果 studentId 不为 null，说明测试没有走到删除成功这一步。
            if (studentId != null) {
                try {
                    // 按学号清理测试数据，避免数据库留下脏数据。
                    deleteTestStudentIfExists(dao, TEST_STUDENT_NO);
                } catch (SQLException cleanupException) {
                    // 清理失败也要提示，方便手动检查数据库。
                    System.err.println("测试数据清理失败：" + cleanupException.getMessage());
                }
            }
        }
    }

    /**
     * 测试新增学生，并分别验证 findById 和 findByStudentNo。
     *
     * @param dao 被测 DAO 对象
     * @return 新增学生的主键 ID
     * @throws SQLException 数据库访问异常
     */
    private static Long testInsertAndFind(StudentDaoImpl dao) throws SQLException {
        // 创建测试学生对象。
        Student student = new Student();

        // 设置固定测试学号。
        student.setStudentNo(TEST_STUDENT_NO);

        // 设置测试姓名。
        student.setName("DAO测试张三");

        // 设置性别：1-男。
        student.setGender(1);

        // 设置出生日期。
        student.setBirthDate(LocalDate.of(2004, 1, 15));

        // 设置联系电话。
        student.setPhone("13900000099");

        // 设置电子邮箱。
        student.setEmail("dao-test-999@campus.edu.cn");

        // 设置所属专业。
        student.setMajor("计算机科学与技术");

        // 设置班级名称。
        student.setClassName("计科2401班");

        // 设置学籍状态：1-在读。
        student.setStatus(1);

        // 调用 DAO 的 insert 方法，并获取数据库生成的自增主键。
        Long studentId = dao.insert(student);

        // 校验主键 ID 不为 null。
        if (studentId == null || studentId <= 0) {
            // 如果主键无效，说明 insert 没有正确返回数据库生成的主键。
            throw new IllegalStateException("新增学生后没有返回有效主键 ID，实际值：" + studentId);
        }

        // 输出新增成功信息。
        System.out.println("【1】新增学生成功，ID：" + studentId);

        // 根据 ID 查询刚刚插入的学生。
        Student foundById = dao.findById(studentId);

        // 校验查询结果不为空。
        if (foundById == null) {
            // 如果为空，说明按 ID 查询失败。
            throw new IllegalStateException("按 ID 查询失败，找不到学生：" + studentId);
        }

        // 校验查询到的学号是否与新增时一致。
        if (!TEST_STUDENT_NO.equals(foundById.getStudentNo())) {
            // 如果不一致，说明结果映射有问题。
            throw new IllegalStateException("按 ID 查询到的学号不正确，实际值：" + foundById.getStudentNo());
        }

        // 输出按 ID 查询成功信息。
        System.out.println("【2】按 ID 查询成功：" + foundById.getName());

        // 按学号查询同一个学生。
        Student foundByNo = dao.findByStudentNo(TEST_STUDENT_NO);

        // 校验按学号查询结果不为空。
        if (foundByNo == null) {
            // 如果为空，说明按学号查询失败。
            throw new IllegalStateException("按学号查询失败，找不到学生：" + TEST_STUDENT_NO);
        }

        // 校验两个查询方法返回的主键一致。
        if (!studentId.equals(foundByNo.getId())) {
            // 如果不一致，说明按学号查询到的不是同一条记录。
            throw new IllegalStateException("按学号查询到的主键 ID 不正确，实际值：" + foundByNo.getId());
        }

        // 输出按学号查询成功信息。
        System.out.println("【3】按学号查询成功：" + foundByNo.getName());

        // 返回测试学生 ID，供后续方法继续使用。
        return studentId;
    }

    /**
     * 测试更新学生信息。
     *
     * @param dao       被测 DAO 对象
     * @param studentId 待更新的学生主键 ID
     * @throws SQLException 数据库访问异常
     */
    private static void testUpdate(StudentDaoImpl dao, Long studentId) throws SQLException {
        // 先根据 ID 查询当前学生。
        Student student = dao.findById(studentId);

        // 校验学生存在。
        if (student == null) {
            // 如果不存在，说明前置查询或数据库状态有问题。
            throw new IllegalStateException("更新测试前找不到学生：" + studentId);
        }

        // 修改测试姓名。
        student.setName("DAO测试李四");

        // 修改联系电话。
        student.setPhone("13900000088");

        // 修改学籍状态：2-休学。
        student.setStatus(2);

        // 调用 DAO 的 update 方法。
        int affectedRows = dao.update(student);

        // 校验数据库受影响行数是否为 1。
        if (affectedRows != 1) {
            // 如果不是 1，说明更新没有按预期执行。
            throw new IllegalStateException("更新学生失败，受影响行数：" + affectedRows);
        }

        // 重新根据 ID 查询更新后的学生。
        Student updatedStudent = dao.findById(studentId);

        // 校验更新后的学生仍然存在。
        if (updatedStudent == null) {
            // 如果不存在，说明更新后查询失败。
            throw new IllegalStateException("更新后查询不到学生：" + studentId);
        }

        // 校验姓名是否更新成功。
        if (!"DAO测试李四".equals(updatedStudent.getName())) {
            // 如果姓名没有变化，说明 update 映射或 SQL 有问题。
            throw new IllegalStateException("更新后的姓名不正确，实际值：" + updatedStudent.getName());
        }

        // 校验状态是否更新成功。
        if (!Integer.valueOf(2).equals(updatedStudent.getStatus())) {
            // 如果状态没有变化，说明 update 映射或 SQL 有问题。
            throw new IllegalStateException("更新后的状态不正确，实际值：" + updatedStudent.getStatus());
        }

        // 输出更新成功信息。
        System.out.println("【4】更新学生成功：" + updatedStudent.getName() + " / " + updatedStudent.getStatus());
    }

    /**
     * 测试分页查询和总记录数统计。
     *
     * @param dao 被测 DAO 对象
     * @throws SQLException 数据库访问异常
     */
    private static void testPageAndCount(StudentDaoImpl dao) throws SQLException {
        // 创建查询条件对象。
        StudentQueryDTO queryDTO = new StudentQueryDTO();

        // 使用测试学号作为关键字，因为它有唯一性，测试结果更稳定。
        queryDTO.setKeyword(TEST_STUDENT_NO);

        // 设置查询第 1 页。
        queryDTO.setPage(1);

        // 设置每页最多 10 条数据。
        queryDTO.setPageSize(10);

        // 调用分页查询方法。
        List<Student> students = dao.findByPage(queryDTO);

        // 校验分页结果不为 null。
        if (students == null) {
            // 如果为 null，说明 DAO 没有按约定返回空集合。
            throw new IllegalStateException("分页查询不应该返回 null");
        }

        // 校验分页结果中确实包含测试学生。
        if (students.isEmpty()) {
            // 如果集合为空，说明动态条件查询或分页查询有问题。
            throw new IllegalStateException("分页查询没有找到测试学生：" + TEST_STUDENT_NO);
        }

        // 校验查询出来的第一条记录学号正确。
        if (!TEST_STUDENT_NO.equals(students.get(0).getStudentNo())) {
            // 如果不一致，说明条件查询映射有问题。
            throw new IllegalStateException("分页查询结果学号不正确，实际值：" + students.get(0).getStudentNo());
        }

        // 输出分页查询成功信息。
        System.out.println("【5】分页查询成功，条数：" + students.size());

        // 调用总记录数统计方法。
        long total = dao.countByCondition(queryDTO);

        // 校验统计总数至少为 1。
        if (total < 1) {
            // 如果小于 1，说明统计 SQL 与分页查询条件不一致。
            throw new IllegalStateException("条件统计总数不正确，实际值：" + total);
        }

        // 输出统计查询成功信息。
        System.out.println("【6】条件统计成功，总数：" + total);
    }

    /**
     * 测试删除学生，并验证删除后查询不到。
     *
     * @param dao       被测 DAO 对象
     * @param studentId 待删除的学生主键 ID
     * @throws SQLException 数据库访问异常
     */
    private static void testDelete(StudentDaoImpl dao, Long studentId) throws SQLException {
        // 调用 DAO 的删除方法。
        int affectedRows = dao.deleteById(studentId);

        // 校验删除操作只影响 1 行。
        if (affectedRows != 1) {
            // 如果不是 1，说明删除没有按预期执行。
            throw new IllegalStateException("删除学生失败，受影响行数：" + affectedRows);
        }

        // 删除后再次查询。
        Student deletedStudent = dao.findById(studentId);

        // 校验删除后查询结果为 null。
        if (deletedStudent != null) {
            // 如果还能查到，说明删除失败或查询缓存有问题。
            throw new IllegalStateException("删除后仍然可以查询到学生：" + studentId);
        }

        // 输出删除成功信息。
        System.out.println("【7】删除学生成功");
    }

    /**
     * 如果测试学生已经存在，则删除该学生。
     * <p>这个方法用于测试开始前的重复数据清理，以及测试失败后的 finally 兜底清理。</p>
     *
     * @param dao       被测 DAO 对象
     * @param studentNo 测试学号
     * @throws SQLException 数据库访问异常
     */
    private static void deleteTestStudentIfExists(StudentDaoImpl dao, String studentNo) throws SQLException {
        // 先根据学号查询测试学生。
        Student existingStudent = dao.findByStudentNo(studentNo);

        // 如果测试学生不存在，直接返回。
        if (existingStudent == null) {
            return;
        }

        // 如果测试学生存在，则按主键删除。
        dao.deleteById(existingStudent.getId());

        // 输出清理提示，方便观察测试环境状态。
        System.out.println("已清理遗留测试学生：" + studentNo);
    }
}
