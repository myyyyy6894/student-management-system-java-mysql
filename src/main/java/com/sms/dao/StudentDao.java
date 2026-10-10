package com.sms.dao;

// 导入学生查询参数 DTO，用于条件分页查询和总记录数统计。
import com.sms.model.dto.StudentQueryDTO;
// 导入学生实体类，DAO 的查询结果和写入参数都使用它。
import com.sms.model.entity.Student;
// 导入 SQL 异常，数据库操作失败时抛给上层处理。
import java.sql.SQLException;
// 导入 List，用于返回多条学生记录。
import java.util.List;

/**
 * 学生数据访问接口。
 * <p>只声明数据库操作，不包含业务校验；具体 SQL 由 StudentDaoImpl 实现。</p>
 */
public interface StudentDao {

    /**
     * 根据主键 ID 查询单条学生信息。
     *
     * @param id 学生物理主键 ID
     * @return 学生实体；如果不存在则返回 null
     * @throws SQLException 数据库访问失败时抛出
     */
    Student findById(Long id) throws SQLException;

    /**
     * 根据唯一学号查询学生信息。
     *
     * @param studentNo 学号
     * @return 学生实体；如果不存在则返回 null
     * @throws SQLException 数据库访问失败时抛出
     */
    Student findByStudentNo(String studentNo) throws SQLException;

    /**
     * 新增学生记录。
     *
     * @param student 要新增的学生实体
     * @return 数据库生成的自增主键 ID
     * @throws SQLException 数据库写入失败时抛出
     */
    Long insert(Student student) throws SQLException;

    /**
     * 根据主键 ID 更新学生基本信息。
     * <p>规范要求不允许直接修改学号 student_no。</p>
     *
     * @param student 要更新的学生实体，必须包含主键 id
     * @return 数据库中受影响的行数
     * @throws SQLException 数据库更新失败时抛出
     */
    int update(Student student) throws SQLException;

    /**
     * 根据主键 ID 删除学生记录。
     *
     * @param id 学生物理主键 ID
     * @return 数据库中受影响的行数
     * @throws SQLException 数据库删除失败时抛出
     */
    int deleteById(Long id) throws SQLException;

    /**
     * 根据综合多条件分页查询学生列表。
     *
     * @param queryDTO 查询条件、页码和每页条数
     * @return 当前页学生列表
     * @throws SQLException 数据库查询失败时抛出
     */
    List<Student> findByPage(StudentQueryDTO queryDTO) throws SQLException;

    /**
     * 根据综合多条件统计匹配的学生总记录数。
     *
     * @param queryDTO 查询条件
     * @return 匹配条件的总记录数
     * @throws SQLException 数据库统计失败时抛出
     */
    long countByCondition(StudentQueryDTO queryDTO) throws SQLException;
}
