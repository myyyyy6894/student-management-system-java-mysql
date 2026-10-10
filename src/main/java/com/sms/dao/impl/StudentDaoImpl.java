package com.sms.dao.impl;

// 导入 StudentDao 接口，StudentDaoImpl 是它的 JDBC 实现。
import com.sms.dao.StudentDao;
// 导入学生查询参数对象。
import com.sms.model.dto.StudentQueryDTO;
// 导入学生实体类。
import com.sms.model.entity.Student;
// 导入数据库工具类，用于获取数据库连接。
import com.sms.util.DBUtil;
// 导入数据库连接对象。
import java.sql.Connection;
// 导入 SQL Date 类，用于把 JDBC 日期转换成 LocalDate。
import java.sql.Date;
// 导入 SQL 预编译语句对象，所有 SQL 参数都使用占位符 ?，避免 SQL 注入。
import java.sql.PreparedStatement;
// 导入 SQL 结果集对象，用于把数据库行映射成 Student 实体。
import java.sql.ResultSet;
// 导入 SQL 异常。
import java.sql.SQLException;
// 导入 Statement，主要用于获取插入后的自增主键。
import java.sql.Statement;
// 导入 SQL 时间戳类，用于把 DATETIME 转换成 LocalDateTime。
import java.sql.Timestamp;
// 导入 JDBC 类型常量，用于把空值显式设置为数据库 NULL。
import java.sql.Types;
// 导入 List 和 ArrayList，用于保存分页查询结果。
import java.util.ArrayList;
import java.util.List;

/**
 * StudentDao 的 JDBC 实现类。
 * <p>这里只负责执行 SQL 和映射结果，不处理“学号是否重复”“是否能删除学生”等业务规则。</p>
 */
public class StudentDaoImpl implements StudentDao {

    // 学生表的基础查询字段，避免多个查询方法重复写一遍 SELECT 字段。
    private static final String BASE_SELECT =
            "SELECT id, student_no, name, gender, birth_date, phone, email, major, class_name, status, created_at, updated_at "
                    + "FROM student";

    /**
     * 根据主键 ID 查询一个学生。
     *
     * @param id 学生物理主键 ID
     * @return 学生实体；查不到时返回 null
     * @throws SQLException 数据库访问异常
     */
    @Override
    public Student findById(Long id) throws SQLException {
        // 参数为空说明调用方传入了非法 ID，这里提前返回 null，避免生成无效 SQL。
        if (id == null) {
            return null;
        }

        // 定义查询 SQL，student_id 使用占位符 ?，由 PreparedStatement 安全填充。
        String sql = BASE_SELECT + " WHERE id = ?";

        // try-with-resources 会自动关闭 Connection 和 PreparedStatement，避免连接和语句泄漏。
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // 把第一个占位符 ? 绑定为主键 id。
            statement.setLong(1, id);

            // executeQuery() 执行 SELECT，并把查询结果保存到 ResultSet 中。
            try (ResultSet resultSet = statement.executeQuery()) {
                // 如果结果集有下一行，说明找到了学生，就把它映射成实体对象。
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }

                // 如果结果集没有数据，说明数据库中不存在这个 ID。
                return null;
            }
        }
    }

    /**
     * 根据唯一学号查询一个学生。
     *
     * @param studentNo 学号
     * @return 学生实体；查不到时返回 null
     * @throws SQLException 数据库访问异常
     */
    @Override
    public Student findByStudentNo(String studentNo) throws SQLException {
        // 学号为空或空白时没有查询意义，直接返回 null。
        if (studentNo == null || studentNo.isBlank()) {
            return null;
        }

        // 定义 SQL，student_no 是唯一索引字段，这里用等值查询。
        String sql = BASE_SELECT + " WHERE student_no = ?";

        // try-with-resources 自动关闭连接和语句。
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // 将学号绑定到 SQL 的第一个 ?。
            statement.setString(1, studentNo);

            // 执行查询。
            try (ResultSet resultSet = statement.executeQuery()) {
                // student_no 有唯一索引，理论上最多只有一条记录。
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }

                // 查不到学号对应的学生。
                return null;
            }
        }
    }

    /**
     * 插入一条学生记录。
     *
     * @param student 要插入的学生实体
     * @return 数据库自动生成的学生主键 ID
     * @throws SQLException 数据库插入异常
     */
    @Override
    public Long insert(Student student) throws SQLException {
        // 参数为空是程序性错误，业务层通常已经校验，这里也要兜底拦截。
        if (student == null) {
            throw new IllegalArgumentException("student 不能为空");
        }

        // 定义插入 SQL；id、created_at、updated_at 由数据库自动维护，这里不手动插入。
        String sql =
                "INSERT INTO student (student_no, name, gender, birth_date, phone, email, major, class_name, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        // 使用 try-with-resources 自动关闭连接和语句。
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // 1：学号。
            statement.setString(1, student.getStudentNo());
            // 2：姓名。
            statement.setString(2, student.getName());
            // 3：性别。
            statement.setObject(3, student.getGender());
            // 4：出生日期，可以为 null。
            setDateParameter(statement, 4, student.getBirthDate());
            // 5：联系电话。
            statement.setString(5, student.getPhone());
            // 6：电子邮箱。
            statement.setString(6, student.getEmail());
            // 7：专业。
            statement.setString(7, student.getMajor());
            // 8：班级名称。
            statement.setString(8, student.getClassName());
            // 9：学籍状态。
            statement.setObject(9, student.getStatus());

            // executeUpdate() 执行 INSERT，返回受影响的行数。
            int affectedRows = statement.executeUpdate();

            // 正常 INSERT 应该只影响 1 行；如果不是 1 行，说明数据库写入异常。
            if (affectedRows != 1) {
                throw new SQLException("新增学生失败，受影响行数：" + affectedRows);
            }

            // 获取数据库生成的自增主键结果集。
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                // 游标移动到第一行，表示存在生成的主键。
                if (generatedKeys.next()) {
                    // 读取第一列的自增 ID 并返回。
                    return generatedKeys.getLong(1);
                }

                // 如果数据库没有返回主键，说明驱动或表配置可能有问题。
                throw new SQLException("新增学生成功但未获取到自增主键 ID");
            }
        }
    }

    /**
     * 根据主键 ID 更新学生基本信息。
     * <p>这里不更新 student_no，因为学号是业务唯一标识，规范禁止直接修改。</p>
     *
     * @param student 要更新的学生实体
     * @return 数据库中受影响的行数
     * @throws SQLException 数据库更新异常
     */
    @Override
    public int update(Student student) throws SQLException {
        // 参数或主键为空时无法定位要更新的记录，直接抛出程序性异常。
        if (student == null || student.getId() == null) {
            throw new IllegalArgumentException("更新学生时 student 或 student.id 不能为空");
        }

        // 定义更新 SQL；注意这里没有 student_no 字段，updated_at 由数据库自动更新。
        String sql =
                "UPDATE student SET name = ?, gender = ?, birth_date = ?, phone = ?, email = ?, major = ?, "
                        + "class_name = ?, status = ? WHERE id = ?";

        // try-with-resources 自动关闭资源。
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // 1：姓名。
            statement.setString(1, student.getName());
            // 2：性别。
            statement.setObject(2, student.getGender());
            // 3：出生日期，可以为 null。
            setDateParameter(statement, 3, student.getBirthDate());
            // 4：联系电话。
            statement.setString(4, student.getPhone());
            // 5：电子邮箱。
            statement.setString(5, student.getEmail());
            // 6：专业。
            statement.setString(6, student.getMajor());
            // 7：班级名称。
            statement.setString(7, student.getClassName());
            // 8：学籍状态。
            statement.setObject(8, student.getStatus());
            // 9：WHERE 条件中的学生主键 ID。
            statement.setLong(9, student.getId());

            // 执行 UPDATE，并返回数据库实际更新的行数。
            return statement.executeUpdate();
        }
    }

    /**
     * 根据主键 ID 删除学生记录。
     *
     * @param id 学生物理主键 ID
     * @return 数据库中受影响的行数
     * @throws SQLException 数据库删除异常
     */
    @Override
    public int deleteById(Long id) throws SQLException {
        // 主键为空无法删除，提前抛出程序性异常。
        if (id == null) {
            throw new IllegalArgumentException("删除学生时 id 不能为空");
        }

        // 定义 DELETE SQL，使用主键条件保证只删除一条记录。
        String sql = "DELETE FROM student WHERE id = ?";

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
     * 根据综合条件分页查询学生列表。
     *
     * @param queryDTO 查询条件、页码和每页条数
     * @return 当前页的学生集合
     * @throws SQLException 数据库查询异常
     */
    @Override
    public List<Student> findByPage(StudentQueryDTO queryDTO) throws SQLException {
        // 查询参数为空属于程序性错误，无法生成安全的分页 SQL。
        if (queryDTO == null) {
            throw new IllegalArgumentException("queryDTO 不能为空");
        }

        // 先根据 DTO 构建动态 WHERE 条件，并把条件值保存到 params 中。
        // params 的顺序必须和 SQL 中占位符 ? 的出现顺序完全一致。
        StringBuilder whereSql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();
        buildWhere(whereSql, parameters, queryDTO);

        // 拼接基础查询、动态条件、稳定排序和分页参数。
        String sql = BASE_SELECT + whereSql + " ORDER BY id ASC LIMIT ? OFFSET ?";

        // 计算 offset：第 1 页从第 0 条开始，第 2 页跳过第一页的数据。
        int page = Math.max(1, queryDTO.getPage());
        int pageSize = Math.max(1, queryDTO.getPageSize());
        int offset = (page - 1) * pageSize;

        // try-with-resources 自动关闭资源。
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // parameterIndex 表示当前要填充的占位符位置。
            int parameterIndex = 1;

            // 先填充查询条件参数。
            for (Object parameter : parameters) {
                // setObject 会根据参数的实际类型选择合适的 JDBC 类型。
                statement.setObject(parameterIndex, parameter);
                // 每绑定一个参数，位置下标加 1。
                parameterIndex++;
            }

            // 绑定 LIMIT：当前页最多返回多少条记录。
            statement.setInt(parameterIndex++, pageSize);
            // 绑定 OFFSET：当前页之前需要跳过多少条记录。
            statement.setInt(parameterIndex, offset);

            // 执行分页查询。
            try (ResultSet resultSet = statement.executeQuery()) {
                // 创建结果集合，保存当前页所有学生。
                List<Student> students = new ArrayList<>();

                // 循环遍历结果集，每一行都映射成一个 Student 实体。
                while (resultSet.next()) {
                    students.add(mapRow(resultSet));
                }

                // 返回当前页数据；没有数据时是空 List，而不是 null。
                return students;
            }
        }
    }

    /**
     * 根据综合条件统计学生总记录数。
     *
     * @param queryDTO 查询条件
     * @return 符合条件的总记录数
     * @throws SQLException 数据库统计异常
     */
    @Override
    public long countByCondition(StudentQueryDTO queryDTO) throws SQLException {
        // 查询参数为空属于程序性错误。
        if (queryDTO == null) {
            throw new IllegalArgumentException("queryDTO 不能为空");
        }

        // 构建和 findByPage 完全一致的 WHERE 条件，保证查询结果和统计总数对应。
        StringBuilder whereSql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();
        buildWhere(whereSql, parameters, queryDTO);

        // 定义 COUNT SQL。
        String sql = "SELECT COUNT(*) FROM student" + whereSql;

        // try-with-resources 自动关闭资源。
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // 按顺序填充条件参数。
            int parameterIndex = 1;
            for (Object parameter : parameters) {
                statement.setObject(parameterIndex, parameter);
                parameterIndex++;
            }

            // 执行统计查询。
            try (ResultSet resultSet = statement.executeQuery()) {
                // COUNT(*) 一定会返回一行数据；读取第一列的总数。
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }

                // 理论上不会走到这里，但为了类型完整仍然返回 0。
                return 0L;
            }
        }
    }

    /**
     * 构建学生列表的动态 WHERE 条件。
     * <p>所有条件值都放入 parameters，由 PreparedStatement 安全绑定，不直接拼接到 SQL 文本中。</p>
     *
     * @param whereSql    动态条件 SQL 的拼接缓冲区
     * @param parameters  与占位符一一对应的参数列表
     * @param queryDTO    查询条件
     */
    private void buildWhere(StringBuilder whereSql, List<Object> parameters, StudentQueryDTO queryDTO) {
        // 1. 综合关键字：模糊匹配姓名或学号。
        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isBlank()) {
            // 如果已有前面的条件，则继续用 AND 连接。
            appendCondition(whereSql);
            // 写入条件 SQL，这里只写入占位符，不写入用户输入。
            whereSql.append("(name LIKE ? OR student_no LIKE ?)");
            // 构造 LIKE 参数，左右各加 %，实现包含匹配。
            String keyword = "%" + queryDTO.getKeyword().trim() + "%";
            // 第一个 ? 对应 name LIKE。
            parameters.add(keyword);
            // 第二个 ? 对应 student_no LIKE。
            parameters.add(keyword);
        }

        // 2. 专业精准筛选。
        if (queryDTO.getMajor() != null && !queryDTO.getMajor().isBlank()) {
            appendCondition(whereSql);
            whereSql.append("major = ?");
            parameters.add(queryDTO.getMajor().trim());
        }

        // 3. 班级精准筛选。
        if (queryDTO.getClassName() != null && !queryDTO.getClassName().isBlank()) {
            appendCondition(whereSql);
            whereSql.append("class_name = ?");
            parameters.add(queryDTO.getClassName().trim());
        }

        // 4. 学籍状态精准筛选。
        if (queryDTO.getStatus() != null) {
            appendCondition(whereSql);
            whereSql.append("status = ?");
            parameters.add(queryDTO.getStatus());
        }
    }

    /**
     * 在动态 SQL 中追加条件连接符。
     *
     * @param whereSql 条件拼接缓冲区
     */
    private void appendCondition(StringBuilder whereSql) {
        // 第一次追加条件时使用 WHERE。
        if (whereSql.isEmpty()) {
            whereSql.append(" WHERE ");
        } else {
            // 后续条件都用 AND 连接。
            whereSql.append(" AND ");
        }
    }

    /**
     * 设置 DATE 类型的 JDBC 参数。
     * <p>因为 birth_date 允许为 NULL，所以这里不能直接用 setDate(null)，要显式设置 NULL 类型。</p>
     *
     * @param statement 预编译语句
     * @param index     占位符位置
     * @param value     Java 日期值
     * @throws SQLException 参数设置异常
     */
    private void setDateParameter(PreparedStatement statement, int index, java.time.LocalDate value) throws SQLException {
        // 如果业务值为 null，就显式写入数据库 NULL。
        if (value == null) {
            statement.setNull(index, Types.DATE);
        } else {
            // 否则把 LocalDate 转换成 JDBC 的 java.sql.Date。
            statement.setDate(index, Date.valueOf(value));
        }
    }

    /**
     * 把 ResultSet 中的一行数据映射成 Student 实体。
     *
     * @param resultSet 当前数据行
     * @return 学生实体对象
     * @throws SQLException 结果集读取异常
     */
    private Student mapRow(ResultSet resultSet) throws SQLException {
        // 创建空的学生实体对象。
        Student student = new Student();

        // 映射物理主键。
        student.setId(resultSet.getLong("id"));
        // 映射学号。
        student.setStudentNo(resultSet.getString("student_no"));
        // 映射姓名。
        student.setName(resultSet.getString("name"));
        // 映射性别。
        student.setGender(resultSet.getInt("gender"));

        // birth_date 可能为 NULL，先读取 java.sql.Date 再转换为 LocalDate。
        Date birthDate = resultSet.getDate("birth_date");
        student.setBirthDate(birthDate == null ? null : birthDate.toLocalDate());

        // 映射联系电话。
        student.setPhone(resultSet.getString("phone"));
        // 映射电子邮箱。
        student.setEmail(resultSet.getString("email"));
        // 映射专业。
        student.setMajor(resultSet.getString("major"));
        // 映射班级名称。
        student.setClassName(resultSet.getString("class_name"));
        // 映射学籍状态。
        student.setStatus(resultSet.getInt("status"));

        // created_at 可能为 NULL，先读取 Timestamp 再转换为 LocalDateTime。
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        student.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());

        // updated_at 可能为 NULL，先读取 Timestamp 再转换为 LocalDateTime。
        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        student.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());

        // 返回映射完成的学生对象。
        return student;
    }
}
