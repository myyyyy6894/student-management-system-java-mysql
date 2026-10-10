package com.sms.dao.impl;

// 导入 ScoreDao 接口，ScoreDaoImpl 是它的 JDBC 实现。
import com.sms.dao.ScoreDao;
// 导入成绩查询参数对象。
import com.sms.model.dto.ScoreQueryDTO;
// 导入成绩实体类。
import com.sms.model.entity.Score;
// 导入成绩明细视图对象。
import com.sms.model.vo.ScoreDetailVO;
// 导入成绩统计视图对象。
import com.sms.model.vo.ScoreStatsVO;
// 导入数据库工具类。
import com.sms.util.DBUtil;
// 导入分页参数工具类，统一页码与每页条数的合法范围。
import com.sms.util.PageUtil;
// 导入数据库连接对象。
import java.sql.Connection;
// 导入 SQL 日期类，用于把 JDBC 日期转换成 LocalDate。
import java.sql.Date;
// 导入 SQL 预编译语句对象。
import java.sql.PreparedStatement;
// 导入 SQL 结果集对象。
import java.sql.ResultSet;
// 导入 SQL 异常。
import java.sql.SQLException;
// 导入 Statement，主要用于获取插入后的自增主键。
import java.sql.Statement;
// 导入 SQL 时间戳类，用于把 DATETIME 转换成 LocalDateTime。
import java.sql.Timestamp;
// 导入 JDBC 类型常量，用于把空值显式设置为数据库 NULL。
import java.sql.Types;
// 导入 List 和 ArrayList，用于保存查询结果。
import java.util.ArrayList;
import java.util.List;

/**
 * ScoreDao 的 JDBC 实现类。
 * <p>这里只负责执行 SQL、映射实体和视图对象，不处理“分数是否在 0~100”等业务规则。</p>
 */
public class ScoreDaoImpl implements ScoreDao {

    // 成绩表的基础查询字段，供单表查询使用。
    private static final String BASE_SELECT =
            "SELECT id, student_id, course_id, score, exam_date, remarks, created_at, updated_at FROM score";

    // 成绩明细查询的基础 SQL，三表 JOIN 后供明细分页查询使用。
    private static final String BASE_DETAIL_SELECT =
            "SELECT sc.id, sc.student_id, st.student_no, st.name AS student_name, st.class_name, "
                    + "sc.course_id, c.course_no, c.name AS course_name, c.credit, "
                    + "sc.score, sc.exam_date, sc.remarks "
                    + "FROM score sc "
                    + "JOIN student st ON st.id = sc.student_id "
                    + "JOIN course c ON c.id = sc.course_id";

    /**
     * 根据主键 ID 查询成绩。
     *
     * @param id 成绩物理主键 ID
     * @return 成绩实体；查不到时返回 null
     * @throws SQLException 数据库访问异常
     */
    @Override
    public Score findById(Long id) throws SQLException {
        // 主键为空时没有查询意义。
        if (id == null) {
            return null;
        }

        // 定义 SQL，使用主键等值查询。
        String sql = BASE_SELECT + " WHERE id = ?";

        // try-with-resources 自动关闭连接和语句。
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // 绑定成绩主键 ID。
            statement.setLong(1, id);

            // 执行查询。
            try (ResultSet resultSet = statement.executeQuery()) {
                // 如果有下一行，说明成绩存在。
                if (resultSet.next()) {
                    return mapScore(resultSet);
                }

                // 查不到指定成绩。
                return null;
            }
        }
    }

    /**
     * 查询特定学生在特定课程下的成绩。
     *
     * @param studentId 学生物理主键 ID
     * @param courseId  课程物理主键 ID
     * @return 成绩实体；查不到时返回 null
     * @throws SQLException 数据库访问异常
     */
    @Override
    public Score findByStudentAndCourse(Long studentId, Long courseId) throws SQLException {
        // 学生或课程 ID 为空时无法查询。
        if (studentId == null || courseId == null) {
            return null;
        }

        // 学生和课程组合字段有唯一索引，这里用两个字段等值查询。
        String sql = BASE_SELECT + " WHERE student_id = ? AND course_id = ?";

        // try-with-resources 自动关闭资源。
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // 绑定学生 ID。
            statement.setLong(1, studentId);
            // 绑定课程 ID。
            statement.setLong(2, courseId);

            // 执行查询。
            try (ResultSet resultSet = statement.executeQuery()) {
                // 如果有下一行，说明该学生该课程已有成绩。
                if (resultSet.next()) {
                    return mapScore(resultSet);
                }

                // 查不到表示还没有录入该组合的成绩。
                return null;
            }
        }
    }

    /**
     * 新增一条成绩记录。
     *
     * @param score 要插入的成绩实体
     * @return 数据库自动生成的成绩主键 ID
     * @throws SQLException 数据库插入异常
     */
    @Override
    public Long insert(Score score) throws SQLException {
        // 参数为空属于程序性错误，业务层通常已经校验，这里也做兜底拦截。
        if (score == null) {
            throw new IllegalArgumentException("score 不能为空");
        }

        // 定义插入 SQL；id、created_at、updated_at 由数据库自动维护，这里不手动插入。
        String sql =
                "INSERT INTO score (student_id, course_id, score, exam_date, remarks) VALUES (?, ?, ?, ?, ?)";

        // 使用 try-with-resources 自动关闭资源。
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // 1：学生 ID。
            statement.setLong(1, score.getStudentId());
            // 2：课程 ID。
            statement.setLong(2, score.getCourseId());
            // 3：考试成绩。
            statement.setBigDecimal(3, score.getScore());
            // 4：考试日期，可以为 null。
            setDateParameter(statement, 4, score.getExamDate());
            // 5：备注。
            statement.setString(5, score.getRemarks());

            // 执行 INSERT，并获取受影响行数。
            int affectedRows = statement.executeUpdate();

            // 正常插入成绩应该只影响 1 行。
            if (affectedRows != 1) {
                throw new SQLException("新增成绩失败，受影响行数：" + affectedRows);
            }

            // 读取数据库生成的自增主键。
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                // 如果存在生成的主键，就返回第一列。
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }

                // 没有拿到主键，说明数据库或驱动配置可能有问题。
                throw new SQLException("新增成绩成功但未获取到自增主键 ID");
            }
        }
    }

    /**
     * 根据主键 ID 更新成绩信息。
     * <p>这里不更新 student_id 和 course_id，避免破坏学生和课程的关联关系。</p>
     *
     * @param score 要更新的成绩实体
     * @return 数据库中受影响的行数
     * @throws SQLException 数据库更新异常
     */
    @Override
    public int update(Score score) throws SQLException {
        // 参数或主键为空时无法定位要更新的记录。
        if (score == null || score.getId() == null) {
            throw new IllegalArgumentException("更新成绩时 score 或 score.id 不能为空");
        }

        // 只更新分数、考试日期和备注；学生和课程关系保持不变。
        String sql = "UPDATE score SET score = ?, exam_date = ?, remarks = ? WHERE id = ?";

        // try-with-resources 自动关闭资源。
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // 1：考试成绩。
            statement.setBigDecimal(1, score.getScore());
            // 2：考试日期，可以为 null。
            setDateParameter(statement, 2, score.getExamDate());
            // 3：备注。
            statement.setString(3, score.getRemarks());
            // 4：WHERE 条件中的成绩主键 ID。
            statement.setLong(4, score.getId());

            // 执行 UPDATE，并返回受影响的行数。
            return statement.executeUpdate();
        }
    }

    /**
     * 根据主键 ID 删除成绩记录。
     *
     * @param id 成绩物理主键 ID
     * @return 数据库中受影响的行数
     * @throws SQLException 数据库删除异常
     */
    @Override
    public int deleteById(Long id) throws SQLException {
        // 主键为空无法删除，提前抛出程序性异常。
        if (id == null) {
            throw new IllegalArgumentException("删除成绩时 id 不能为空");
        }

        // 定义 DELETE SQL。
        String sql = "DELETE FROM score WHERE id = ?";

        // try-with-resources 自动关闭资源。
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // 绑定成绩主键 ID。
            statement.setLong(1, id);

            // 执行 DELETE，并返回受影响的行数。
            return statement.executeUpdate();
        }
    }

    /**
     * 统计某个学生名下的成绩记录总数。
     *
     * @param studentId 学生物理主键 ID
     * @return 成绩记录总数
     * @throws SQLException 数据库统计异常
     */
    @Override
    public long countByStudentId(Long studentId) throws SQLException {
        // 学生 ID 为空时没有统计意义，直接返回 0。
        if (studentId == null) {
            return 0L;
        }

        // 定义统计 SQL。
        String sql = "SELECT COUNT(*) FROM score WHERE student_id = ?";

        // try-with-resources 自动关闭资源。
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // 绑定学生 ID。
            statement.setLong(1, studentId);

            // 执行统计查询。
            try (ResultSet resultSet = statement.executeQuery()) {
                // COUNT(*) 一定会返回一行。
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }

                // 理论上不会走到这里，但为了类型完整仍然返回 0。
                return 0L;
            }
        }
    }

    /**
     * 统计某门课程关联的成绩记录总数。
     *
     * @param courseId 课程物理主键 ID
     * @return 成绩记录总数
     * @throws SQLException 数据库统计异常
     */
    @Override
    public long countByCourseId(Long courseId) throws SQLException {
        // 课程 ID 为空时没有统计意义，直接返回 0。
        if (courseId == null) {
            return 0L;
        }

        // 定义统计 SQL。
        String sql = "SELECT COUNT(*) FROM score WHERE course_id = ?";

        // try-with-resources 自动关闭资源。
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // 绑定课程 ID。
            statement.setLong(1, courseId);

            // 执行统计查询。
            try (ResultSet resultSet = statement.executeQuery()) {
                // COUNT(*) 一定会返回一行。
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }

                // 理论上不会走到这里，但为了类型完整仍然返回 0。
                return 0L;
            }
        }
    }

    /**
     * 多表联查分页获取成绩明细。
     *
     * @param queryDTO 查询条件、页码和每页条数
     * @return 当前页成绩明细集合
     * @throws SQLException 数据库查询异常
     */
    @Override
    public List<ScoreDetailVO> findDetailByPage(ScoreQueryDTO queryDTO) throws SQLException {
        // 查询参数为空属于程序性错误。
        if (queryDTO == null) {
            throw new IllegalArgumentException("queryDTO 不能为空");
        }

        // 先根据 DTO 构建动态 WHERE 条件，并把条件值保存到 parameters 中。
        // parameters 的顺序必须和 SQL 中占位符 ? 的出现顺序完全一致。
        StringBuilder whereSql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();
        buildDetailWhere(whereSql, parameters, queryDTO);

        // 拼接基础三表 JOIN 查询、动态条件、稳定排序和分页参数。
        String sql = BASE_DETAIL_SELECT + whereSql + " ORDER BY sc.id ASC LIMIT ? OFFSET ?";

        // 计算 offset：第 1 页从第 0 条开始，第 2 页跳过第一页的数据。
        // 统一使用 PageUtil 规范化参数，保证与 Service 层回显值一致。
        int page = PageUtil.normalizePage(queryDTO.getPage());
        int pageSize = PageUtil.normalizePageSize(queryDTO.getPageSize());

        // 用 long 计算偏移量，避免超大页码导致 int 溢出成负数。
        long offset = (long) (page - 1) * pageSize;

        // try-with-resources 自动关闭资源。
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // parameterIndex 表示当前要填充的占位符位置。
            int parameterIndex = 1;

            // 先填充查询条件参数。
            for (Object parameter : parameters) {
                statement.setObject(parameterIndex, parameter);
                parameterIndex++;
            }

            // 绑定 LIMIT：当前页最多返回多少条记录。
            statement.setInt(parameterIndex++, pageSize);
            // 绑定 OFFSET：当前页之前需要跳过多少条记录（用 long 接收 int 型占位符）。
            statement.setLong(parameterIndex, offset);

            // 执行分页查询。
            try (ResultSet resultSet = statement.executeQuery()) {
                // 创建结果集合，保存当前页所有成绩明细。
                List<ScoreDetailVO> details = new ArrayList<>();

                // 循环遍历结果集，每一行都映射成 ScoreDetailVO。
                while (resultSet.next()) {
                    details.add(mapDetail(resultSet));
                }

                // 返回当前页数据；没有数据时是空 List，而不是 null。
                return details;
            }
        }
    }

    /**
     * 根据综合多条件统计成绩明细总记录数。
     *
     * @param queryDTO 查询条件
     * @return 符合条件的总记录数
     * @throws SQLException 数据库统计异常
     */
    @Override
    public long countDetailByCondition(ScoreQueryDTO queryDTO) throws SQLException {
        // 查询参数为空属于程序性错误。
        if (queryDTO == null) {
            throw new IllegalArgumentException("queryDTO 不能为空");
        }

        // 构建和 findDetailByPage 完全一致的 WHERE 条件，保证查询结果和统计总数对应。
        StringBuilder whereSql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();
        buildDetailWhere(whereSql, parameters, queryDTO);

        // 定义 COUNT SQL；使用同一张三表 JOIN，保证统计范围一致。
        String sql = "SELECT COUNT(*) FROM score sc "
                + "JOIN student st ON st.id = sc.student_id "
                + "JOIN course c ON c.id = sc.course_id" + whereSql;

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
                // COUNT(*) 一定会返回一行数据。
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }

                // 理论上不会走到这里，但为了类型完整仍然返回 0。
                return 0L;
            }
        }
    }

    /**
     * 针对指定课程或全部课程统计成绩。
     *
     * @param courseId 课程 ID；传入 null 表示统计全部课程
     * @return 成绩统计结果
     * @throws SQLException 数据库统计异常
     */
    @Override
    public ScoreStatsVO calculateStatistics(Long courseId) throws SQLException {
        // 根据是否传入课程 ID 决定统计单门课程还是全部课程。
        String sql;
        boolean isCourseSpecific = courseId != null;

        if (isCourseSpecific) {
            // 统计单门课程时，从 course 表出发 LEFT JOIN score，这样没有成绩的课程也能返回 0 指标。
            sql = "SELECT c.id AS course_id, c.name AS course_name, "
                    + "COUNT(s.id) AS total_students, "
                    + "COALESCE(ROUND(AVG(s.score), 2), 0) AS average_score, "
                    + "COALESCE(MAX(s.score), 0) AS max_score, "
                    + "COALESCE(MIN(s.score), 0) AS min_score, "
                    + "COALESCE(ROUND(SUM(CASE WHEN s.score >= 60 THEN 1 ELSE 0 END) / COUNT(s.id) * 100, 2), 0) AS pass_rate "
                    + "FROM course c LEFT JOIN score s ON s.course_id = c.id "
                    + "WHERE c.id = ? "
                    + "GROUP BY c.id, c.name";
        } else {
            // 统计全部课程时，不需要展示具体课程 ID 和课程名称。
            sql = "SELECT NULL AS course_id, NULL AS course_name, "
                    + "COUNT(*) AS total_students, "
                    + "COALESCE(ROUND(AVG(score), 2), 0) AS average_score, "
                    + "COALESCE(MAX(score), 0) AS max_score, "
                    + "COALESCE(MIN(score), 0) AS min_score, "
                    + "COALESCE(ROUND(SUM(CASE WHEN score >= 60 THEN 1 ELSE 0 END) / COUNT(*) * 100, 2), 0) AS pass_rate "
                    + "FROM score";
        }

        // try-with-resources 自动关闭资源。
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // 如果是单课程统计，就绑定课程 ID。
            if (isCourseSpecific) {
                statement.setLong(1, courseId);
            }

            // 执行统计查询。
            try (ResultSet resultSet = statement.executeQuery()) {
                // 如果有统计结果行，映射成统计对象。
                if (resultSet.next()) {
                    return mapStats(resultSet);
                }

                // 如果课程不存在且没有结果，返回全 0 统计对象，避免上层空指针。
                ScoreStatsVO stats = new ScoreStatsVO();
                stats.setCourseId(courseId);
                stats.setTotalStudents(0);
                stats.setAverageScore(java.math.BigDecimal.ZERO);
                stats.setMaxScore(java.math.BigDecimal.ZERO);
                stats.setMinScore(java.math.BigDecimal.ZERO);
                stats.setPassRate(java.math.BigDecimal.ZERO);
                return stats;
            }
        }
    }

    /**
     * 构建成绩明细查询的动态 WHERE 条件。
     * <p>所有条件值都放入 parameters，由 PreparedStatement 安全绑定，不直接拼接到 SQL 文本中。</p>
     *
     * @param whereSql   动态条件 SQL 的拼接缓冲区
     * @param parameters 与占位符一一对应的参数列表
     * @param queryDTO   查询条件
     */
    private void buildDetailWhere(StringBuilder whereSql, List<Object> parameters, ScoreQueryDTO queryDTO) {
        // 1. 学生 ID 精确筛选。
        if (queryDTO.getStudentId() != null) {
            appendCondition(whereSql);
            whereSql.append("sc.student_id = ?");
            parameters.add(queryDTO.getStudentId());
        }

        // 2. 课程 ID 精确筛选。
        if (queryDTO.getCourseId() != null) {
            appendCondition(whereSql);
            whereSql.append("sc.course_id = ?");
            parameters.add(queryDTO.getCourseId());
        }

        // 3. 学生姓名模糊筛选。
        if (queryDTO.getStudentName() != null && !queryDTO.getStudentName().isBlank()) {
            appendCondition(whereSql);
            whereSql.append("st.name LIKE ?");
            parameters.add("%" + queryDTO.getStudentName().trim() + "%");
        }

        // 4. 最低分数筛选。
        if (queryDTO.getMinScore() != null) {
            appendCondition(whereSql);
            whereSql.append("sc.score >= ?");
            parameters.add(queryDTO.getMinScore());
        }

        // 5. 最高分数筛选。
        if (queryDTO.getMaxScore() != null) {
            appendCondition(whereSql);
            whereSql.append("sc.score <= ?");
            parameters.add(queryDTO.getMaxScore());
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
     * <p>因为 exam_date 允许为 NULL，所以这里不能直接用 setDate(null)，要显式设置 NULL 类型。</p>
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
     * 把 ResultSet 中的一行数据映射成 Score 实体。
     *
     * @param resultSet 当前数据行
     * @return 成绩实体对象
     * @throws SQLException 结果集读取异常
     */
    private Score mapScore(ResultSet resultSet) throws SQLException {
        // 创建空成绩实体对象。
        Score score = new Score();

        // 映射成绩主键。
        score.setId(resultSet.getLong("id"));
        // 映射学生 ID。
        score.setStudentId(resultSet.getLong("student_id"));
        // 映射课程 ID。
        score.setCourseId(resultSet.getLong("course_id"));
        // 映射考试成绩。
        score.setScore(resultSet.getBigDecimal("score"));

        // exam_date 可能为 NULL，先读取 java.sql.Date 再转换为 LocalDate。
        Date examDate = resultSet.getDate("exam_date");
        score.setExamDate(examDate == null ? null : examDate.toLocalDate());

        // 映射成绩备注。
        score.setRemarks(resultSet.getString("remarks"));

        // created_at 可能为 NULL，先读取 Timestamp 再转换为 LocalDateTime。
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        score.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());

        // updated_at 可能为 NULL，先读取 Timestamp 再转换为 LocalDateTime。
        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        score.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());

        // 返回映射完成的成绩对象。
        return score;
    }

    /**
     * 把 ResultSet 中的一行数据映射成成绩明细视图对象。
     *
     * @param resultSet 当前数据行
     * @return 成绩明细视图对象
     * @throws SQLException 结果集读取异常
     */
    private ScoreDetailVO mapDetail(ResultSet resultSet) throws SQLException {
        // 创建空成绩明细对象。
        ScoreDetailVO detail = new ScoreDetailVO();

        // 映射成绩主键。
        detail.setId(resultSet.getLong("id"));
        // 映射学生 ID。
        detail.setStudentId(resultSet.getLong("student_id"));
        // 映射学号。
        detail.setStudentNo(resultSet.getString("student_no"));
        // 映射学生姓名。
        detail.setStudentName(resultSet.getString("student_name"));
        // 映射班级名称。
        detail.setClassName(resultSet.getString("class_name"));
        // 映射课程 ID。
        detail.setCourseId(resultSet.getLong("course_id"));
        // 映射课程编号。
        detail.setCourseNo(resultSet.getString("course_no"));
        // 映射课程名称。
        detail.setCourseName(resultSet.getString("course_name"));
        // 映射课程学分。
        detail.setCredit(resultSet.getBigDecimal("credit"));
        // 映射考试成绩。
        detail.setScore(resultSet.getBigDecimal("score"));

        // 考试日期可能为 NULL，需要先判断再转换。
        Date examDate = resultSet.getDate("exam_date");
        detail.setExamDate(examDate == null ? null : examDate.toLocalDate());

        // 映射成绩备注。
        detail.setRemarks(resultSet.getString("remarks"));

        // 返回映射完成的明细对象。
        return detail;
    }

    /**
     * 把 ResultSet 中的一行统计结果映射成 ScoreStatsVO。
     *
     * @param resultSet 当前统计结果行
     * @return 成绩统计对象
     * @throws SQLException 结果集读取异常
     */
    private ScoreStatsVO mapStats(ResultSet resultSet) throws SQLException {
        // 创建空统计对象。
        ScoreStatsVO stats = new ScoreStatsVO();

        // course_id 可能为 NULL，表示统计的是全部课程。
        long courseId = resultSet.getLong("course_id");
        stats.setCourseId(resultSet.wasNull() ? null : courseId);

        // 映射课程名称。
        stats.setCourseName(resultSet.getString("course_name"));
        // 映射参加考试总人数。
        stats.setTotalStudents((int) resultSet.getLong("total_students"));
        // 映射平均分。
        stats.setAverageScore(resultSet.getBigDecimal("average_score"));
        // 映射最高分。
        stats.setMaxScore(resultSet.getBigDecimal("max_score"));
        // 映射最低分。
        stats.setMinScore(resultSet.getBigDecimal("min_score"));
        // 映射及格率。
        stats.setPassRate(resultSet.getBigDecimal("pass_rate"));

        // 返回统计对象。
        return stats;
    }
}
