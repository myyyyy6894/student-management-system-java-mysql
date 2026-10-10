package com.sms.service.impl;

// 导入课程 DAO 接口。
import com.sms.dao.CourseDao;
// 导入成绩 DAO 接口，用于删除课程前检查是否已有成绩。
import com.sms.dao.ScoreDao;
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

            // 如果编号重复，业务层直接拦截。
            if (existingCourse != null) {
                throw new IllegalArgumentException("课程编号已存在");
            }

            // 调用 DAO 插入课程，并回填数据库生成的主键 ID。
            Long generatedId = courseDao.insert(course);
            course.setId(generatedId);
        } catch (SQLException exception) {
            // 数据库异常统一转成运行时异常。
            throw wrapException("新增课程失败", exception);
        }
    }

    /**
     * 修改课程。
     *
     * @param course 要修改的课程实体
     */
    @Override
    public void updateCourse(Course course) {
        // 校验课程对象、编号、名称和学分。
        validateCourse(course);

        // 修改时必须携带主键 ID。
        requireId(course.getId());

        try {
            // 先确认原课程存在。
            Course existingCourse = courseDao.findById(course.getId());

            // 如果课程不存在，业务层直接拦截。
            if (existingCourse == null) {
                throw new IllegalArgumentException("课程不存在");
            }

            // 如果修改了课程编号，需要检查新编号是否被其他课程占用。
            if (!existingCourse.getCourseNo().equals(course.getCourseNo())) {
                Course sameCourseNo = courseDao.findByCourseNo(course.getCourseNo());

                // 如果新编号已存在，且不是当前课程，则不能修改。
                if (sameCourseNo != null && !course.getId().equals(sameCourseNo.getId())) {
                    throw new IllegalArgumentException("课程编号已存在");
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
            // 先确认课程存在。
            Course existingCourse = courseDao.findById(id);

            // 如果课程不存在，业务层直接拦截。
            if (existingCourse == null) {
                throw new IllegalArgumentException("课程不存在");
            }

            // 统计课程下已有成绩数量。
            long scoreCount = scoreDao.countByCourseId(id);

            // 如果课程已被成绩引用，业务层禁止物理删除。
            if (scoreCount > 0) {
                throw new IllegalArgumentException("该课程已有成绩记录，不能删除");
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

        // 课程编号是业务唯一标识，必填。
        requireText(course.getCourseNo(), "课程编号不能为空");

        // 课程名称必填。
        requireText(course.getName(), "课程名称不能为空");

        // 学分不能为空。
        if (course.getCredit() == null) {
            throw new IllegalArgumentException("学分不能为空");
        }

        // 校验学分范围：规范文档要求 0.5 ~ 10.0。
        boolean creditValid = course.getCredit().compareTo(new BigDecimal("0.5")) >= 0
                && course.getCredit().compareTo(new BigDecimal("10.0")) <= 0;

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
