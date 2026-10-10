package com.sms.service;

// 导入学生查询参数 DTO，用于条件分页查询。
import com.sms.model.dto.StudentQueryDTO;
// 导入学生实体类。
import com.sms.model.entity.Student;
// 导入分页结果包装类。
import com.sms.model.vo.PageResult;

/**
 * 学生业务逻辑接口。
 * <p>Service 层负责业务校验和规则研判，具体数据库操作仍然委托给 DAO 层。</p>
 */
public interface StudentService {

    /**
     * 获取学生详情。
     *
     * @param id 学生物理主键 ID
     * @return 学生实体；如果不存在则返回 null
     */
    Student getStudentById(Long id);

    /**
     * 新增学生档案。
     * <p>内置学号重复校验。</p>
     *
     * @param student 要新增的学生实体
     */
    void addStudent(Student student);

    /**
     * 修改学生档案。
     * <p>不允许修改唯一学号 studentNo。</p>
     *
     * @param student 要修改的学生实体
     */
    void updateStudent(Student student);

    /**
     * 删除学生档案。
     * <p>如果学生名下已有成绩记录，则业务层拦截删除。</p>
     *
     * @param id 学生物理主键 ID
     */
    void deleteStudent(Long id);

    /**
     * 条件分页查询学生。
     *
     * @param queryDTO 查询条件、页码和每页条数
     * @return 分页结果
     */
    PageResult<Student> queryStudents(StudentQueryDTO queryDTO);
}
