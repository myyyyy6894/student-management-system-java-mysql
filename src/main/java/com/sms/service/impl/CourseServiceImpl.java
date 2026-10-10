package com.sms.service.impl;

// 导入课程 DAO 接口。
import com.sms.dao.CourseDao;
// 导入成绩 DAO 接口，用于删除课程前检查是否已有成绩。
import com.sms.dao.ScoreDao;
// 导入业务冲突异常（409）。
import com.sms.exception.ConflictException;
// 导入资源不存在异常（404）。
import com.sms.exception.NotFoundException;
// 导入课程实体类。
import com.sms.model.entity.Course;
// 导入课程业务接口。
import com.sms.service.CourseService;
// 导入 List，用于返回课程集合。
import java.util.List;
// 导入精确小数类型，用于学分范围校验。
import java.math.BigDecimal;
// 导入 SQL 异常；Service 方法对外不暴露 SQLException。
import java.sql.SQLException;

/**
 * CourseService 的默认实现类。
 * <p>负责课程业务规则校验，并把 DAO 抛出的数据库异常转换成运行时异常。</p>
 */
public class CourseServiceImpl implements CourseService {

    // 学分下限。
    private static final BigDecimal MIN_CREDIT = new BigDecimal("0.5");

    // 学分上限。
    private static final BigDecimal MAX_CREDIT = new BigDecimal("10.0");

    // 课程编号最大长度，对应 course.course_no VARCHAR(32)。
    private static final int MAX_COURSE_NO_LENGTH = 32;

    // 课程名称最大长度，对应 course.name VARCHAR(100)。
    private static final int MAX_NAME_LENGTH = 100;

    // 任课教师最大长度，对应 course.teacher VARCHAR(50)。
    private static final int MAX_TEACHER_LENGTH = 50;

    // 课程数据访问接口。
    private final CourseDao courseDao;

    // 成绩数据访问接口，用于判断课程是否已被成绩记录引用。
    private final ScoreDao scoreDao;

    /**
     * 无参构造器，普通项目中使用默认 DAO 实现。
     */
    public CourseServiceImpl() {
        // 这里只依赖接口，运行时传入具体实现类。
        this(new com.sms.dao.impl.CourseDaoImpl(), new com.sms.dao.impl.ScoreDaoImpl());
    }

    /**
     * 支持依赖注入的构造器。
     *
     * @param courseDao 课程 DAO 接口
     * @param scoreDao  成绩 DAO 接口
     */
    public CourseServiceImpl(CourseDao courseDao, ScoreDao scoreDao) {
        // 课程 DAO 不能为空。
        if (courseDao == null) {
            throw new IllegalArgumentException("courseDao 不能为空");
        }

        // 成绩 DAO 不能为空。
        if (scoreDao == null) {
            throw new IllegalArgumentException("scoreDao 不能为空");
        }

        // 保存依赖。
        this.courseDao = courseDao;
        this.scoreDao = scoreDao;
    }

    /**
     * 查询全部课程。
     *
     * @return 课程集合；没有数据时返回空集合
     */
    @Override
    public List<Course> listAllCourses() {
        try {
            // 直接委托 DAO 查询全部课程。
            return courseDao.findAll();
        } catch (SQLException exception) {
            // 数据库异常统一转成运行时异常。
            throw wrapException("查询课程失败", exception);
        }
    }

    /**
     * 根据主键 ID 查询课程详情。
     *
     * @param id 课程物理主键 ID
     * @return 课程实体；如果不存在则返回 null
     */
    @Override
    public Course getCourseById(Long id) {
        // 主键不能为空。
        requireId(id);

        try {
            // 直接委托 DAO 查询。
            return courseDao.findById(id);
        } catch (SQLException exception) {
            // 数据库异常统一转成运行时异常。
            throw wrapException("查询课程详情失败", exception);
        }
    }

    /**
     * 新增课程。
     *
     * @param course 要新增的课程实体
     */
    @Override
    public void addCourse(Course course) {
        // 校验课程对象、编号、名称和学分。
        validateCourse(course);

        try {
            // 新增前检查课程编号是否已经存在。
            Course existingCourse = courseDao.findByCourseNo(course.getCourseNo());

            // 如果编号重复，业务层直接拦截，返回 409。
            if (existingCourse != null) {
                throw new ConflictException("课程编号已存在");
            }

            // 调用 DAO 插入课程，并回填数据库生成的主键 ID。
            Long generatedId = courseDao.insert(course);
            course.setId(generatedId);
        } catch (SQLException exception) {
            // 并发场景下唯一索引仍可能被触发，这里统一转换成 409。
            throw wrapInsertException("新增课程失败", "课程编号已存在", exception);
        }
    }

    /**
     * 修改课程。
     *
     * @param course 要修改的课程实体
     */
    @Override
    public void updateCourse(Course course) {
        // 校验课程对象本身与主键。
        if (course == null) {
            throw new IllegalArgumentException("course 不能为空");
        }
        requireId(course.getId());

        try {
            // 先确认课程存在，并拿到旧数据。
            Course existingCourse = courseDao.findById(course.getId());

            // 如果课程不存在，返回 404。
            if (existingCourse == null) {
                throw new NotFoundException("课程不存在");
            }

            // 请求体没填课程编号时沿用旧值。
            if (course.getCourseNo() == null || course.getCourseNo().isBlank()) {
                course.setCourseNo(existingCourse.getCourseNo());
            }

            // 校验补齐后的完整字段。
            validateCourse(course);

            // 如果修改了课程编号，需要检查新编号是否被其他课程占用。
            if (!existingCourse.getCourseNo().equals(course.getCourseNo())) {
                Course sameCourseNo = courseDao.findByCourseNo(course.getCourseNo());

                // 如果新编号已存在，且不是当前课程，则不能修改。
                if (sameCourseNo != null && !course.getId().equals(sameCourseNo.getId())) {
                    throw new ConflictException("课程编号已存在");
                }
            }

            // 调用 DAO 更新课程。
            courseDao.update(course);
        } catch (SQLException exception) {
            // 数据库异常统一转成运行时异常。
            throw wrapException("修改课程失败", exception);
        }
    }

    /**
     * 删除课程。
     *
     * @param id 课程物理主键 ID
     */
    @Override
    public void deleteCourse(Long id) {
        // 主键不能为空。
        requireId(id);

        try {
            // 先确认课程存在，不存在时返回 404。
            Course existingCourse = courseDao.findById(id);

            // 如果课程不存在，业务层直接拦截。
            if (existingCourse == null) {
                throw new NotFoundException("课程不存在");
            }

            // 统计课程下已有成绩数量。
            long scoreCount = scoreDao.countByCourseId(id);

            // 如果课程已被成绩引用，业务层禁止物理删除。
            if (scoreCount > 0) {
                throw new ConflictException("该课程已有成绩记录，不能删除");
            }

            // 没有成绩依赖时允许删除。
            courseDao.deleteById(id);
        } catch (SQLException exception) {
            // 数据库异常统一转成运行时异常。
            throw wrapException("删除课程失败", exception);
        }
    }

    /**
     * 校验课程新增/修改字段。
     *
     * @param course 课程实体
     */
    private void validateCourse(Course course) {
        // 课程对象本身不能为空。
        if (course == null) {
            throw new IllegalArgumentException("course 不能为空");
        }

        // 课程编号是业务唯一标识，必填且不能超过数据库列长度。
        requireText(course.getCourseNo(), "课程编号不能为空");
        requireMaxLength(course.getCourseNo(), MAX_COURSE_NO_LENGTH, "课程编号长度不能超过 32 个字符");

        // 课程名称必填。
        requireText(course.getName(), "课程名称不能为空");
        requireMaxLength(course.getName(), MAX_NAME_LENGTH, "课程名称长度不能超过 100 个字符");

        // 任课教师可选，但填写后不能超过数据库列长度。
        requireMaxLength(course.getTeacher(), MAX_TEACHER_LENGTH, "任课教师长度不能超过 50 个字符");

        // 学分不能为空。
        if (course.getCredit() == null) {
            throw new IllegalArgumentException("学分不能为空");
        }

        // 校验学分范围：规范文档要求 0.5 ~ 10.0。
        boolean creditValid = course.getCredit().compareTo(MIN_CREDIT) >= 0
                && course.getCredit().compareTo(MAX_CREDIT) <= 0;

        // 如果范围非法，直接拦截。
        if (!creditValid) {
            throw new IllegalArgumentException("学分必须在 0.5~10.0 之间");
        }
    }

    /**
     * 校验主键 ID。
     *
     * @param id 主键 ID
     */
    private void requireId(Long id) {
        // 主键不能为空，并且必须是正数。
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("课程 ID 不能为空");
        }
    }

    /**
     * 校验字符串字段不能为空。
     *
     * @param value   字符串值
     * @param message 错误提示
     */
    private void requireText(String value, String message) {
        // null、空字符串和纯空格都视为无效。
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 校验字符串长度上限。
     *
     * @param value     字符串值
     * @param maxLength 允许的最大长度
     * @param message   错误提示
     */
    private void requireMaxLength(String value, int maxLength, String message) {
        // 超过数据库列长度时提前拦截，避免数据库报错。
        if (value != null && value.length() > maxLength) {
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

    /**
     * 把新增场景的 SQLException 转换成运行时异常。
     *
     * @param message         默认错误提示
     * @param conflictMessage 唯一性冲突提示
     * @param exception       原始数据库异常
     * @return 运行时异常
     */
    private RuntimeException wrapInsertException(String message, String conflictMessage, SQLException exception) {
        // 唯一键冲突转换为 409。
        if ("23000".equals(exception.getSQLState()) || exception.getErrorCode() == 1062) {
            return new ConflictException(conflictMessage);
        }

        // 其他数据库异常保留原始异常链。
        return wrapException(message, exception);
    }
}
