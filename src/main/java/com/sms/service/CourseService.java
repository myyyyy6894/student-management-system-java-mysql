package com.sms.service;

// 导入课程实体类。
import com.sms.model.entity.Course;
// 导入 List，用于返回全部课程。
import java.util.List;

/**
 * 课程业务逻辑接口。
 * <p>Service 层负责课程业务校验，具体数据库操作委托给 CourseDao。</p>
 */
public interface CourseService {

    /**
     * 查询全部课程。
     *
     * @return 课程集合；没有数据时返回空集合
     */
    List<Course> listAllCourses();

    /**
     * 根据主键 ID 查询课程详情。
     *
     * @param id 课程物理主键 ID
     * @return 课程实体；如果不存在则返回 null
     */
    Course getCourseById(Long id);

    /**
     * 新增课程。
     * <p>内置课程编号重复和学分范围校验。</p>
     *
     * @param course 要新增的课程实体
     */
    void addCourse(Course course);

    /**
     * 修改课程。
     * <p>内置课程编号重复和学分范围校验。</p>
     *
     * @param course 要修改的课程实体
     */
    void updateCourse(Course course);

    /**
     * 删除课程。
     * <p>如果课程已关联成绩，业务层会拦截删除。</p>
     *
     * @param id 课程物理主键 ID
     */
    void deleteCourse(Long id);
}
