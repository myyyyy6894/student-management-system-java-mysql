package com.sms.service.impl;

// 导入学生 DAO 接口，Service 只依赖接口，不直接依赖 JDBC 实现细节。
import com.sms.dao.StudentDao;
// 导入成绩 DAO 接口，用于删除学生前检查是否已有成绩。
import com.sms.dao.ScoreDao;
// 导入学生查询参数 DTO。
import com.sms.model.dto.StudentQueryDTO;
// 导入学生实体类。
import com.sms.model.entity.Student;
// 导入分页结果包装类。
import com.sms.model.vo.PageResult;
// 导入学生业务接口。
import com.sms.service.StudentService;
// 导入 SQL 异常；Service 方法对外不暴露 SQLException，所以这里要在内部统一转换。
import java.sql.SQLException;

/**
 * StudentService 的默认实现类。
 * <p>负责学生业务规则校验，并把 DAO 抛出的数据库异常转换成运行时异常。</p>
 */
public class StudentServiceImpl implements StudentService {

    // 学生数据访问接口，负责具体的数据库 CRUD。
    private final StudentDao studentDao;

    // 成绩数据访问接口，用于删除学生前判断是否已有成绩。
    private final ScoreDao scoreDao;

    /**
     * 无参构造器。
     * <p>普通项目中直接使用默认 DAO 实现。</p>
     */
    public StudentServiceImpl() {
        // 这里只依赖接口，运行时传入具体实现类。
        this(new com.sms.dao.impl.StudentDaoImpl(), new com.sms.dao.impl.ScoreDaoImpl());
    }

    /**
     * 支持依赖注入的构造器。
     * <p>测试时可以传入自定义 DAO 实现，业务层不需要知道数据库实现细节。</p>
     *
     * @param studentDao 学生 DAO 接口
     * @param scoreDao   成绩 DAO 接口
     */
    public StudentServiceImpl(StudentDao studentDao, ScoreDao scoreDao) {
        // 校验依赖不能为空，避免后续调用时空指针。
        if (studentDao == null) {
            throw new IllegalArgumentException("studentDao 不能为空");
        }

        // 校验依赖不能为空，避免后续调用时空指针。
        if (scoreDao == null) {
            throw new IllegalArgumentException("scoreDao 不能为空");
        }

        // 保存学生 DAO 依赖。
        this.studentDao = studentDao;

        // 保存成绩 DAO 依赖。
        this.scoreDao = scoreDao;
    }

    /**
     * 根据主键 ID 查询学生详情。
     *
     * @param id 学生物理主键 ID
     * @return 学生实体；如果不存在则返回 null
     */
    @Override
    public Student getStudentById(Long id) {
        // 主键 ID 是定位数据的最小条件，不能为空。
        requireId(id);

        try {
            // 直接委托 DAO 查询。
            return studentDao.findById(id);
        } catch (SQLException exception) {
            // 把受检异常转换成运行时异常，避免上层接口被迫声明 SQLException。
            throw wrapException("查询学生详情失败", exception);
        }
    }

    /**
     * 新增学生档案。
     *
     * @param student 要新增的学生实体
     */
    @Override
    public void addStudent(Student student) {
        // 先做基本参数校验。
        validateStudent(student);

        try {
            // 新增前先根据学号查询，防止数据库唯一索引报错后前端看到底层 SQL 异常。
            Student existingStudent = studentDao.findByStudentNo(student.getStudentNo());

            // 如果已经有相同学号的学生，业务层必须拦截。
            if (existingStudent != null) {
                throw new IllegalArgumentException("学号已存在");
            }

            // 调用 DAO 插入学生，并拿到数据库生成的自增主键。
            Long generatedId = studentDao.insert(student);

            // 回填主键，方便 Controller 或测试代码直接拿到新学生的 ID。
            student.setId(generatedId);
        } catch (SQLException exception) {
            // 数据库访问异常统一转成运行时异常。
            throw wrapException("新增学生失败", exception);
        }
    }

    /**
     * 修改学生档案。
     * <p>规范要求不允许直接修改学号，因为成绩表外键依赖学生记录。</p>
     *
     * @param student 要修改的学生实体
     */
    @Override
    public void updateStudent(Student student) {
        // 先做基本参数校验。
        validateStudent(student);

        // 修改操作必须携带主键 ID。
        requireId(student.getId());

        try {
            // 先查询旧数据，确认学生存在，并校验学号是否被修改。
            Student existingStudent = studentDao.findById(student.getId());

            // 如果学生不存在，返回 404 前应该在业务层明确拦截。
            if (existingStudent == null) {
                throw new IllegalArgumentException("学生不存在");
            }

            // 如果前端修改了学号，直接拒绝。
            if (!existingStudent.getStudentNo().equals(student.getStudentNo())) {
                throw new IllegalArgumentException("不允许修改学号");
            }

            // 调用 DAO 更新学生基本信息。
            studentDao.update(student);
        } catch (SQLException exception) {
            // 数据库访问异常统一转成运行时异常。
            throw wrapException("修改学生失败", exception);
        }
    }

    /**
     * 删除学生档案。
     * <p>如果学生名下已经有成绩，直接拦截，避免破坏成绩数据完整性。</p>
     *
     * @param id 学生物理主键 ID
     */
    @Override
    public void deleteStudent(Long id) {
        // 删除时必须提供主键 ID。
        requireId(id);

        try {
            // 先统计该学生名下的成绩数量。
            long scoreCount = scoreDao.countByStudentId(id);

            // 如果已有成绩记录，业务规则要求先清理成绩，不允许直接删除学生。
            if (scoreCount > 0) {
                throw new IllegalArgumentException("该学生已有考试成绩，请先清理成绩记录或注销学籍");
            }

            // 没有成绩依赖时，允许删除学生。
            studentDao.deleteById(id);
        } catch (SQLException exception) {
            // 数据库访问异常统一转成运行时异常。
            throw wrapException("删除学生失败", exception);
        }
    }

    /**
     * 条件分页查询学生。
     *
     * @param queryDTO 查询条件、页码和每页条数
     * @return 分页结果
     */
    @Override
    public PageResult<Student> queryStudents(StudentQueryDTO queryDTO) {
        // 查询参数不能为空。
        if (queryDTO == null) {
            throw new IllegalArgumentException("queryDTO 不能为空");
        }

        try {
            // 先统计符合条件的总记录数，用于前端分页器。
            long total = studentDao.countByCondition(queryDTO);

            // 再查询当前页数据。
            java.util.List<Student> students = studentDao.findByPage(queryDTO);

            // 把总数、页码、每页条数和当前页数据组装成统一分页对象。
            return new PageResult<>(total, queryDTO.getPage(), queryDTO.getPageSize(), students);
        } catch (SQLException exception) {
            // 数据库访问异常统一转成运行时异常。
            throw wrapException("分页查询学生失败", exception);
        }
    }

    /**
     * 校验学生新增/修改时的必要字段。
     *
     * @param student 学生实体
     */
    private void validateStudent(Student student) {
        // 学生对象本身不能为空。
        if (student == null) {
            throw new IllegalArgumentException("student 不能为空");
        }

        // 学号是业务唯一标识，必须填写。
        requireText(student.getStudentNo(), "学号不能为空");

        // 姓名是学生档案的核心字段，必须填写。
        requireText(student.getName(), "姓名不能为空");

        // 专业是必填字段。
        requireText(student.getMajor(), "专业不能为空");

        // 班级是必填字段。
        requireText(student.getClassName(), "班级不能为空");
    }

    /**
     * 校验主键 ID。
     *
     * @param id 主键 ID
     */
    private void requireId(Long id) {
        // 主键不能为空，并且必须是正数。
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("学生 ID 不能为空");
        }
    }

    /**
     * 校验字符串字段不能为空。
     *
     * @param value     字符串值
     * @param message   错误提示
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
