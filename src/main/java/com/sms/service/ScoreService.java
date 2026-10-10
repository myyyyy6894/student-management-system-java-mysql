package com.sms.service;

// 导入成绩查询参数对象，用于明细分页查询。
import com.sms.model.dto.ScoreQueryDTO;
// 导入成绩实体类。
import com.sms.model.entity.Score;
// 导入通用分页结果对象。
import com.sms.model.vo.PageResult;
// 导入成绩明细视图对象。
import com.sms.model.vo.ScoreDetailVO;
// 导入成绩统计视图对象。
import com.sms.model.vo.ScoreStatsVO;

/**
 * 成绩业务逻辑接口。
 * <p>Service 层负责业务校验和规则研判，具体数据库操作仍然委托给 DAO 层。</p>
 */
public interface ScoreService {

    /**
     * 获取成绩详情。
     *
     * @param id 成绩物理主键 ID
     * @return 成绩实体；如果不存在则返回 null
     */
    Score getScoreById(Long id);

    /**
     * 录入单科成绩。
     * <p>内置学生存在性、课程存在性、分数范围和重复录入校验。</p>
     *
     * @param score 要录入的成绩实体
     */
    void addScore(Score score);

    /**
     * 修改成绩。
     * <p>内置分数范围和成绩存在性校验。</p>
     *
     * @param score 要修改的成绩实体
     */
    void updateScore(Score score);

    /**
     * 删除成绩。
     *
     * @param id 成绩物理主键 ID
     */
    void deleteScore(Long id);

    /**
     * 分页查询成绩联合明细。
     *
     * @param queryDTO 查询条件、页码和每页条数
     * @return 分页结果
     */
    PageResult<ScoreDetailVO> queryScoreDetails(ScoreQueryDTO queryDTO);

    /**
     * 获取成绩统计信息。
     *
     * @param courseId 课程 ID；传入 null 表示统计全部课程
     * @return 成绩统计结果
     */
    ScoreStatsVO getScoreStatistics(Long courseId);
}
