package com.sms.dao;

// 导入课程实体类。
import com.sms.model.entity.Course;
// 导入 SQL 异常。
import java.sql.SQLException;
// 导入 List，用于返回多条课程记录。
import java.util.List;

/**
 * 课程数据访问接口。
 * <p>只声明数据库操作，不包含业务校验；具体 SQL 由 CourseDaoImpl 实现。</p>
 */
public interface CourseDao {

    /**
     * 根据主键 ID 查询单条课程信息。
     *
     * @param id 课程物理主键 ID
     * @return 课程实体；如果不存在则返回 null
     * @throws SQLException 数据库访问失败时抛出
     */
    Course findById(Long id) throws SQLException;

    /**
     * 根据唯一课程编号查询课程信息。
     *
     * @param courseNo 课程编号
     * @return 课程实体；如果不存在则返回 null
     * @throws SQLException 数据库访问失败时抛出
     */
    Course findByCourseNo(String courseNo) throws SQLException;

    /**
     * 新增课程记录。
     *
     * @param course 要新增的课程实体
     * @return 数据库生成的自增主键 ID
     * @throws SQLException 数据库写入失败时抛出
     */
    Long insert(Course course) throws SQLException;

    /**
     * 根据主键 ID 更新课程信息。
     *
     * @param course 要更新的课程实体，必须包含主键 id
     * @return 数据库中受影响的行数
     * @throws SQLException 数据库更新失败时抛出
     */
    int update(Course course) throws SQLException;

    /**
     * 根据主键 ID 删除课程记录。
     *
     * @param id 课程物理主键 ID
     * @return 数据库中受影响的行数
     * @throws SQLException 数据库删除失败时抛出
     */
    int deleteById(Long id) throws SQLException;

    /**
     * 查询全部课程。
     *
     * @return 课程集合；没有数据时返回空集合
     * @throws SQLException 数据库查询失败时抛出
     */
    List<Course> findAll() throws SQLException;
}
