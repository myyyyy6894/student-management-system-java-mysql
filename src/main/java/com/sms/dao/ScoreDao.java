package com.sms.dao;

// 导入成绩查询参数对象。
import com.sms.model.dto.ScoreQueryDTO;
// 导入成绩实体类。
import com.sms.model.entity.Score;
// 导入成绩明细视图对象。
import com.sms.model.vo.ScoreDetailVO;
// 导入成绩统计视图对象。
import com.sms.model.vo.ScoreStatsVO;
// 导入 SQL 异常。
import java.sql.SQLException;
// 导入 List，用于返回多条成绩记录。
import java.util.List;

/**
 * 成绩数据访问接口。
 * <p>只声明数据库操作；复杂 JOIN 和聚合统计在实现类中完成。</p>
 */
public interface ScoreDao {

    /**
     * 根据主键 ID 查询单条成绩记录。
     *
     * @param id 成绩物理主键 ID
     * @return 成绩实体；如果不存在则返回 null
     * @throws SQLException 数据库访问失败时抛出
     */
    Score findById(Long id) throws SQLException;

    /**
     * 查询特定学生在特定课程下是否已经有成绩。
     *
     * @param studentId 学生物理主键 ID
     * @param courseId  课程物理主键 ID
     * @return 成绩实体；如果不存在则返回 null
     * @throws SQLException 数据库访问失败时抛出
     */
    Score findByStudentAndCourse(Long studentId, Long courseId) throws SQLException;

    /**
     * 新增成绩记录。
     *
     * @param score 要新增的成绩实体
     * @return 数据库生成的自增主键 ID
     * @throws SQLException 数据库写入失败时抛出
     */
    Long insert(Score score) throws SQLException;

    /**
     * 根据主键 ID 更新成绩信息。
     *
     * @param score 要更新的成绩实体，必须包含主键 id
     * @return 数据库中受影响的行数
     * @throws SQLException 数据库更新失败时抛出
     */
    int update(Score score) throws SQLException;

    /**
     * 根据主键 ID 删除成绩记录。
     *
     * @param id 成绩物理主键 ID
     * @return 数据库中受影响的行数
     * @throws SQLException 数据库删除失败时抛出
     */
    int deleteById(Long id) throws SQLException;

    /**
     * 统计某个学生名下的成绩记录总数。
     *
     * @param studentId 学生物理主键 ID
     * @return 成绩记录总数
     * @throws SQLException 数据库统计失败时抛出
     */
    long countByStudentId(Long studentId) throws SQLException;

    /**
     * 统计某门课程关联的成绩记录总数。
     *
     * @param courseId 课程物理主键 ID
     * @return 成绩记录总数
     * @throws SQLException 数据库统计失败时抛出
     */
    long countByCourseId(Long courseId) throws SQLException;

    /**
     * 多表联查分页获取成绩明细。
     *
     * @param queryDTO 查询条件、页码和每页条数
     * @return 当前页成绩明细集合
     * @throws SQLException 数据库查询失败时抛出
     */
    List<ScoreDetailVO> findDetailByPage(ScoreQueryDTO queryDTO) throws SQLException;

    /**
     * 根据综合多条件统计成绩明细总记录数。
     *
     * @param queryDTO 查询条件
     * @return 符合条件的总记录数
     * @throws SQLException 数据库统计失败时抛出
     */
    long countDetailByCondition(ScoreQueryDTO queryDTO) throws SQLException;

    /**
     * 针对指定课程或全部课程进行成绩统计。
     *
     * @param courseId 课程 ID；传入 null 表示统计全部课程
     * @return 成绩统计结果
     * @throws SQLException 数据库统计失败时抛出
     */
    ScoreStatsVO calculateStatistics(Long courseId) throws SQLException;
}
