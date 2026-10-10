package com.sms.dao.impl;

// 导入 CourseDao 接口，CourseDaoImpl 是它的 JDBC 实现。
import com.sms.dao.CourseDao;
// 导入课程实体类。
import com.sms.model.entity.Course;
// 导入数据库工具类。
import com.sms.util.DBUtil;
// 导入数据库连接对象。
import java.sql.Connection;
// 导入 SQL 预编译语句对象，所有 SQL 参数都使用占位符 ?，避免 SQL 注入。
import java.sql.PreparedStatement;
// 导入 SQL 结果集对象，用于把数据库行映射成 Course 实体。
import java.sql.ResultSet;
// 导入 SQL 异常。
import java.sql.SQLException;
// 导入 Statement，主要用于获取插入后的自增主键。
import java.sql.Statement;
// 导入 SQL 时间戳类，用于把 DATETIME 转换成 LocalDateTime。
import java.sql.Timestamp;
// 导入 List 和 ArrayList，用于保存课程查询结果。
import java.util.ArrayList;
import java.util.List;

/**
 * CourseDao 的 JDBC 实现类。
 * <p>这里只负责执行 SQL 和映射结果，不处理“课程编号是否重复”“课程是否能删除”等业务规则。</p>
 */
public class CourseDaoImpl implements CourseDao {

    // 课程表的基础查询字段，避免多个查询方法重复写一遍 SELECT 字段。
    private static final String BASE_SELECT =
            "SELECT id, course_no, name, credit, teacher, created_at, updated_at FROM course";

    /**
     * 根据主键 ID 查询课程。
     *
     * @param id 课程物理主键 ID
     * @return 课程实体；查不到时返回 null
     * @throws SQLException 数据库访问异常
     */
    @Override
    public Course findById(Long id) throws SQLException {
        // 主键为空时没有查询意义，直接返回 null。
        if (id == null) {
            return null;
        }

        // 定义查询 SQL，课程主键使用占位符 ?。
        String sql = BASE_SELECT + " WHERE id = ?";

        // try-with-resources 会自动关闭 Connection 和 PreparedStatement。
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // 把第一个占位符绑定为主键 ID。
            statement.setLong(1, id);

            // 执行查询并遍历结果集。
            try (ResultSet resultSet = statement.executeQuery()) {
                // 如果有下一行，说明课程存在。
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }

                // 查不到指定 ID 的课程。
                return null;
            }
        }
    }

    /**
     * 根据课程编号查询课程。
     *
     * @param courseNo 课程编号
     * @return 课程实体；查不到时返回 null
     * @throws SQLException 数据库访问异常
     */
    @Override
    public Course findByCourseNo(String courseNo) throws SQLException {
        // 课程编号为空或空白时没有查询意义。
        if (courseNo == null || courseNo.isBlank()) {
            return null;
        }

        // course_no 是唯一索引字段，这里使用等值查询。
        String sql = BASE_SELECT + " WHERE course_no = ?";

        // try-with-resources 自动关闭资源。
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // 绑定课程编号。
            statement.setString(1, courseNo);

            // 执行查询。
            try (ResultSet resultSet = statement.executeQuery()) {
                // 唯一编号最多对应一条课程记录。
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }

                // 查不到课程。
                return null;
            }
        }
    }

    /**
     * 新增一条课程记录。
     *
     * @param course 要插入的课程实体
     * @return 数据库自动生成的课程主键 ID
     * @throws SQLException 数据库插入异常
     */
    @Override
    public Long insert(Course course) throws SQLException {
        // 参数为空属于程序性错误，业务层通常已经校验，这里也做兜底拦截。
        if (course == null) {
            throw new IllegalArgumentException("course 不能为空");
        }

        // 定义插入 SQL；id、created_at、updated_at 由数据库自动维护，这里不手动插入。
        String sql = "INSERT INTO course (course_no, name, credit, teacher) VALUES (?, ?, ?, ?)";

        // 使用 try-with-resources 自动关闭连接和语句。
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // 1：课程编号。
            statement.setString(1, course.getCourseNo());
            // 2：课程名称。
            statement.setString(2, course.getName());
            // 3：课程学分。
            statement.setBigDecimal(3, course.getCredit());
            // 4：任课教师。
            statement.setString(4, course.getTeacher());

            // 执行 INSERT，返回受影响的行数。
            int affectedRows = statement.executeUpdate();

            // 正常插入课程应该只影响 1 行。
            if (affectedRows != 1) {
                throw new SQLException("新增课程失败，受影响行数：" + affectedRows);
            }

            // 读取数据库生成的自增主键。
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                // 如果存在生成的主键，就返回第一列。
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }

                // 没有拿到主键，说明数据库或驱动配置可能有问题。
                throw new SQLException("新增课程成功但未获取到自增主键 ID");
            }
        }
    }

    /**
     * 根据主键 ID 更新课程信息。
     *
     * @param course 要更新的课程实体
     * @return 数据库中受影响的行数
     * @throws SQLException 数据库更新异常
     */
    @Override
    public int update(Course course) throws SQLException {
        // 参数或主键为空时无法定位要更新的记录。
        if (course == null || course.getId() == null) {
            throw new IllegalArgumentException("更新课程时 course 或 course.id 不能为空");
        }

        // 定义更新 SQL；updated_at 由数据库自动更新。
        String sql = "UPDATE course SET course_no = ?, name = ?, credit = ?, teacher = ? WHERE id = ?";

        // try-with-resources 自动关闭资源。
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // 1：课程编号。
            statement.setString(1, course.getCourseNo());
            // 2：课程名称。
            statement.setString(2, course.getName());
            // 3：课程学分。
            statement.setBigDecimal(3, course.getCredit());
            // 4：任课教师。
            statement.setString(4, course.getTeacher());
            // 5：WHERE 条件中的课程主键 ID。
            statement.setLong(5, course.getId());

            // 执行 UPDATE，并返回受影响的行数。
            return statement.executeUpdate();
        }
    }

    /**
     * 根据主键 ID 删除课程记录。
     *
     * @param id 课程物理主键 ID
     * @return 数据库中受影响的行数
     * @throws SQLException 数据库删除异常
     */
    @Override
    public int deleteById(Long id) throws SQLException {
        // 主键为空无法删除，提前抛出程序性异常。
        if (id == null) {
            throw new IllegalArgumentException("删除课程时 id 不能为空");
        }

        // 定义 DELETE SQL，使用主键条件保证只删除一条课程记录。
        String sql = "DELETE FROM course WHERE id = ?";

        // try-with-resources 自动关闭资源。
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // 把主键 ID 绑定到 WHERE 条件。
            statement.setLong(1, id);

            // 执行 DELETE，并返回受影响的行数。
            return statement.executeUpdate();
        }
    }

    /**
     * 查询全部课程。
     *
     * @return 课程集合；没有数据时是空集合，而不是 null
     * @throws SQLException 数据库查询异常
     */
    @Override
    public List<Course> findAll() throws SQLException {
        // 查询所有课程，并按 ID 升序排列，保证前端下拉框展示稳定。
        String sql = BASE_SELECT + " ORDER BY id ASC";

        // try-with-resources 自动关闭资源。
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            // 创建结果集合，保存全部课程。
            List<Course> courses = new ArrayList<>();

            // 逐行读取结果集。
            while (resultSet.next()) {
                courses.add(mapRow(resultSet));
            }

            // 返回全部课程；没有数据时返回空 List。
            return courses;
        }
    }

    /**
     * 把 ResultSet 中的一行数据映射成 Course 实体。
     *
     * @param resultSet 当前数据行
     * @return 课程实体对象
     * @throws SQLException 结果集读取异常
     */
    private Course mapRow(ResultSet resultSet) throws SQLException {
        // 创建空课程实体对象。
        Course course = new Course();

        // 映射课程主键。
        course.setId(resultSet.getLong("id"));
        // 映射课程编号。
        course.setCourseNo(resultSet.getString("course_no"));
        // 映射课程名称。
        course.setName(resultSet.getString("name"));
        // 映射课程学分。
        course.setCredit(resultSet.getBigDecimal("credit"));
        // 映射任课教师。
        course.setTeacher(resultSet.getString("teacher"));

        // created_at 可能为 NULL，先读取 Timestamp 再转换为 LocalDateTime。
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        course.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());

        // updated_at 可能为 NULL，先读取 Timestamp 再转换为 LocalDateTime。
        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        course.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());

        // 返回映射完成的课程对象。
        return course;
    }
}
