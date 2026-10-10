package com.sms.service.impl;

// 导入学生 DAO 接口，用于校验学生是否存在。
import com.sms.dao.StudentDao;
// 导入课程 DAO 接口，用于校验课程是否存在。
import com.sms.dao.CourseDao;
// 导入成绩 DAO 接口，用于成绩的增删改查、明细查询和统计。
import com.sms.dao.ScoreDao;
// 导入成绩查询参数对象。
import com.sms.model.dto.ScoreQueryDTO;
// 导入成绩实体类。
import com.sms.model.entity.Score;
// 导入分页结果对象。
import com.sms.model.vo.PageResult;
// 导入成绩明细视图对象。
import com.sms.model.vo.ScoreDetailVO;
// 导入成绩统计视图对象。
import com.sms.model.vo.ScoreStatsVO;
// 导入成绩业务接口。
import com.sms.service.ScoreService;
// 导入 List，用于保存分页明细结果。
import java.util.List;
// 导入精确小数类型，用于分数范围校验。
import java.math.BigDecimal;
// 导入 SQL 异常；Service 方法对外不暴露 SQLException，所以这里要在内部统一转换。
import java.sql.SQLException;

/**
 * ScoreService 的默认实现类。
 * <p>负责成绩业务规则校验，并把 DAO 抛出的数据库异常转换成运行时异常。</p>
 */
public class ScoreServiceImpl implements ScoreService {

    // 学生数据访问接口，用于校验学生是否存在。
    private final StudentDao studentDao;

    // 课程数据访问接口，用于校验课程是否存在。
    private final CourseDao courseDao;

    // 成绩数据访问接口，负责具体数据库操作。
    private final ScoreDao scoreDao;

    /**
     * 无参构造器。
     * <p>普通项目中直接使用默认 DAO 实现。</p>
     */
    public ScoreServiceImpl() {
        // 这里只依赖接口，运行时传入具体实现类。
        this(
                new com.sms.dao.impl.StudentDaoImpl(),
                new com.sms.dao.impl.CourseDaoImpl(),
                new com.sms.dao.impl.ScoreDaoImpl()
        );
    }

    /**
     * 支持依赖注入的构造器。
     * <p>测试时可以传入自定义 DAO 实现，业务层不需要知道数据库实现细节。</p>
     *
     * @param studentDao 学生 DAO 接口
     * @param courseDao  课程 DAO 接口
     * @param scoreDao   成绩 DAO 接口
     */
    public ScoreServiceImpl(StudentDao studentDao, CourseDao courseDao, ScoreDao scoreDao) {
        // 校验依赖不能为空，避免后续调用时空指针。
        if (studentDao == null) {
            throw new IllegalArgumentException("studentDao 不能为空");
        }

        // 校验依赖不能为空。
        if (courseDao == null) {
            throw new IllegalArgumentException("courseDao 不能为空");
        }

        // 校验依赖不能为空。
        if (scoreDao == null) {
            throw new IllegalArgumentException("scoreDao 不能为空");
        }

        // 保存学生 DAO 依赖。
        this.studentDao = studentDao;

        // 保存课程 DAO 依赖。
        this.courseDao = courseDao;

        // 保存成绩 DAO 依赖。
        this.scoreDao = scoreDao;
    }

    /**
     * 根据主键 ID 查询成绩详情。
     *
     * @param id 成绩物理主键 ID
     * @return 成绩实体；如果不存在则返回 null
     */
    @Override
    public Score getScoreById(Long id) {
        // 成绩 ID 是定位数据的最小条件，不能为空。
        requireId(id, "成绩 ID 不能为空");

        try {
            // 直接委托 DAO 查询。
            return scoreDao.findById(id);
        } catch (SQLException exception) {
            // 把受检异常转换成运行时异常，避免上层接口被迫声明 SQLException。
            throw wrapException("查询成绩失败", exception);
        }
    }

    /**
     * 录入单科成绩。
     *
     * @param score 要录入的成绩实体
     */
    @Override
    public void addScore(Score score) {
        // 先校验成绩对象、关联 ID 和分数范围。
        validateScore(score);

        try {
            // 校验学生是否存在，避免直接向数据库写入无效外键。
            if (studentDao.findById(score.getStudentId()) == null) {
                throw new IllegalArgumentException("学生不存在");
            }

            // 校验课程是否存在。
            if (courseDao.findById(score.getCourseId()) == null) {
                throw new IllegalArgumentException("课程不存在");
            }

            // 校验该学生该课程是否已经有成绩，避免触发数据库唯一索引错误。
            Score existingScore = scoreDao.findByStudentAndCourse(score.getStudentId(), score.getCourseId());

            // 如果已经存在，业务层直接拦截，提示前端走修改流程。
            if (existingScore != null) {
                throw new IllegalArgumentException("该学生本门课程成绩已存在，请走修改流程");
            }

            // 调用 DAO 插入成绩，并拿到数据库生成的自增主键。
            Long generatedId = scoreDao.insert(score);

            // 回填主键，方便 Controller 或测试代码直接拿到新成绩的 ID。
            score.setId(generatedId);
        } catch (SQLException exception) {
            // 数据库访问异常统一转成运行时异常。
            throw wrapException("录入成绩失败", exception);
        }
    }

    /**
     * 修改成绩。
     *
     * @param score 要修改的成绩实体
     */
    @Override
    public void updateScore(Score score) {
        // 先校验成绩对象、主键 ID 和分数范围。
        validateScoreForUpdate(score);

        try {
            // 先确认原成绩存在，避免更新 0 行时上层无法得到明确业务提示。
            Score existingScore = scoreDao.findById(score.getId());

            // 如果成绩不存在，业务层直接拦截。
            if (existingScore == null) {
                throw new IllegalArgumentException("成绩不存在");
            }

            // 只更新分数、考试日期和备注；学生与课程关系不允许在修改成绩时变更。
            scoreDao.update(score);
        } catch (SQLException exception) {
            // 数据库访问异常统一转成运行时异常。
            throw wrapException("修改成绩失败", exception);
        }
    }

    /**
     * 删除成绩。
     *
     * @param id 成绩物理主键 ID
     */
    @Override
    public void deleteScore(Long id) {
        // 删除时必须提供成绩 ID。
        requireId(id, "成绩 ID 不能为空");

        try {
            // 先确认成绩存在，给调用方更明确的业务提示。
            Score existingScore = scoreDao.findById(id);

            // 如果不存在，业务层直接拦截。
            if (existingScore == null) {
                throw new IllegalArgumentException("成绩不存在");
            }

            // 调用 DAO 删除成绩。
            scoreDao.deleteById(id);
        } catch (SQLException exception) {
            // 数据库访问异常统一转成运行时异常。
            throw wrapException("删除成绩失败", exception);
        }
    }

    /**
     * 分页查询成绩联合明细。
     *
     * @param queryDTO 查询条件、页码和每页条数
     * @return 分页结果
     */
    @Override
    public PageResult<ScoreDetailVO> queryScoreDetails(ScoreQueryDTO queryDTO) {
        // 查询参数不能为空。
        if (queryDTO == null) {
            throw new IllegalArgumentException("queryDTO 不能为空");
        }

        try {
            // 先统计符合条件的总记录数，用于前端分页器。
            long total = scoreDao.countDetailByCondition(queryDTO);

            // 再查询当前页数据。
            List<ScoreDetailVO> details = scoreDao.findDetailByPage(queryDTO);

            // 把总数、页码、每页条数和当前页数据组装成统一分页对象。
            return new PageResult<>(total, queryDTO.getPage(), queryDTO.getPageSize(), details);
        } catch (SQLException exception) {
            // 数据库访问异常统一转成运行时异常。
            throw wrapException("分页查询成绩明细失败", exception);
        }
    }

    /**
     * 获取成绩统计信息。
     *
     * @param courseId 课程 ID；传入 null 表示统计全部课程
     * @return 成绩统计结果
     */
    @Override
    public ScoreStatsVO getScoreStatistics(Long courseId) {
        // 如果指定了课程 ID，则必须是一个有效正数；null 表示统计全部课程。
        if (courseId != null && courseId <= 0) {
            throw new IllegalArgumentException("课程 ID 不能为空");
        }

        try {
            // 直接委托 DAO 统计平均分、最高分、最低分和及格率。
            return scoreDao.calculateStatistics(courseId);
        } catch (SQLException exception) {
            // 数据库访问异常统一转成运行时异常。
            throw wrapException("统计成绩失败", exception);
        }
    }

    /**
     * 校验新增成绩的必要字段和业务规则。
     *
     * @param score 成绩实体
     */
    private void validateScore(Score score) {
        // 成绩对象本身不能为空。
        if (score == null) {
            throw new IllegalArgumentException("score 不能为空");
        }

        // 学生 ID 是外键关联条件，不能为空。
        requireId(score.getStudentId(), "学生 ID 不能为空");

        // 课程 ID 是外键关联条件，不能为空。
        requireId(score.getCourseId(), "课程 ID 不能为空");

        // 校验分数必须在 0~100 之间。
        validateScoreRange(score.getScore());
    }

    /**
     * 校验修改成绩的必要字段和业务规则。
     *
     * @param score 成绩实体
     */
    private void validateScoreForUpdate(Score score) {
        // 成绩对象本身不能为空。
        if (score == null) {
            throw new IllegalArgumentException("score 不能为空");
        }

        // 修改时必须携带成绩主键。
        requireId(score.getId(), "成绩 ID 不能为空");

        // 修改时也要确保分数合法。
        validateScoreRange(score.getScore());
    }

    /**
     * 校验分数范围。
     *
     * @param score 考试分数
     */
    private void validateScoreRange(BigDecimal score) {
        // 分数不能为空。
        if (score == null) {
            throw new IllegalArgumentException("分数不能为空");
        }

        // compareTo 避免受 BigDecimal 精度影响；0 <= score <= 100 才允许保存。
        boolean valid = score.compareTo(BigDecimal.ZERO) >= 0
                && score.compareTo(BigDecimal.valueOf(100)) <= 0;

        // 如果分数不合法，直接拦截。
        if (!valid) {
            throw new IllegalArgumentException("分数必须在 0~100 之间");
        }
    }

    /**
     * 校验主键 ID。
     *
     * @param id      主键 ID
     * @param message 错误提示
     */
    private void requireId(Long id, String message) {
        // 主键不能为空，并且必须是正数。
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 把 SQLException 包装成运行时异常。
     *
     * @param message   错误提示
     * @param exception 原始数据库异常
     * @return 运行时异常
     */
    private RuntimeException wrapException(String message, SQLException exception) {
        // 保留原始异常链，方便后续排查数据库问题。
        return new RuntimeException(message, exception);
    }
}
