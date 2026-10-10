package com.sms.service.impl;

// 导入学生 DAO 接口，Service 只依赖接口，不直接依赖 JDBC 实现细节。
import com.sms.dao.StudentDao;
// 导入成绩 DAO 接口，用于删除学生前检查是否已有成绩。
import com.sms.dao.ScoreDao;
// 导入业务冲突异常（409）。
import com.sms.exception.ConflictException;
// 导入资源不存在异常（404）。
import com.sms.exception.NotFoundException;
// 导入学生查询参数 DTO。
import com.sms.model.dto.StudentQueryDTO;
// 导入学生实体类。
import com.sms.model.entity.Student;
// 导入分页结果包装类。
import com.sms.model.vo.PageResult;
// 导入学生业务接口。
import com.sms.service.StudentService;
// 导入分页参数工具类，统一页码与每页条数。
import com.sms.util.PageUtil;
// 导入日期类型，用于出生日期校验。
import java.time.LocalDate;
// 导入 SQL 异常；Service 方法对外不暴露 SQLException，所以这里要在内部统一转换。
import java.sql.SQLException;
// 导入正则表达式工具，用于手机号与邮箱格式校验。
import java.util.regex.Pattern;

/**
 * StudentService 的默认实现类。
 * <p>负责学生业务规则校验，并把 DAO 抛出的数据库异常转换成运行时异常。</p>
 */
public class StudentServiceImpl implements StudentService {

    // 中国大陆手机号正则：1 开头，第二位 3~9，共 11 位。
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    // 通用邮箱正则，覆盖常见的 local@domain.tld 形式。
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // 学号最大长度，对应 student.student_no VARCHAR(32)。
    private static final int MAX_STUDENT_NO_LENGTH = 32;

    // 姓名最大长度，对应 student.name VARCHAR(50)。
    private static final int MAX_NAME_LENGTH = 50;

    // 专业最大长度，对应 student.major VARCHAR(100)。
    private static final int MAX_MAJOR_LENGTH = 100;

    // 班级最大长度，对应 student.class_name VARCHAR(50)。
    private static final int MAX_CLASS_NAME_LENGTH = 50;

    // 电话最大长度，对应 student.phone VARCHAR(20)。
    private static final int MAX_PHONE_LENGTH = 20;

    // 邮箱最大长度，对应 student.email VARCHAR(100)。
    private static final int MAX_EMAIL_LENGTH = 100;

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
        // 先补齐可缺省字段（性别、学籍状态），再执行完整校验。
        applyDefaults(student);

        // 执行业务校验。
        validateStudent(student);

        try {
            // 新增前先根据学号查询，防止数据库唯一索引报错后前端看到底层 SQL 异常。
            Student existingStudent = studentDao.findByStudentNo(student.getStudentNo());

            // 如果已经有相同学号的学生，业务层必须拦截。
            if (existingStudent != null) {
                throw new ConflictException("学号已存在");
            }

            // 调用 DAO 插入学生，并拿到数据库生成的自增主键。
            Long generatedId = studentDao.insert(student);

            // 回填主键，方便 Controller 或测试代码直接拿到新学生的 ID。
            student.setId(generatedId);
        } catch (SQLException exception) {
            // 并发场景下唯一索引仍可能被触发，这里统一转换成 409。
            throw wrapInsertException("新增学生失败", "学号已存在", exception);
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
        // 学生对象本身不能为空。
        if (student == null) {
            throw new IllegalArgumentException("student 不能为空");
        }

        // 修改操作必须携带主键 ID。
        requireId(student.getId());

        try {
            // 先查询旧数据，确认学生存在，并用于后续回填缺省字段。
            Student existingStudent = studentDao.findById(student.getId());

            // 如果学生不存在，返回 404。
            if (existingStudent == null) {
                throw new NotFoundException("学生不存在");
            }

            // 学号不允许修改：请求体没填学号时沿用旧值，填了且不同则直接拒绝。
            if (student.getStudentNo() == null || student.getStudentNo().isBlank()) {
                student.setStudentNo(existingStudent.getStudentNo());
            }

            // 请求体没填性别/学籍状态时沿用旧值，避免把 NOT NULL 字段写成 NULL。
            if (student.getGender() == null) {
                student.setGender(existingStudent.getGender());
            }

            // 请求体没填学籍状态时沿用旧值。
            if (student.getStatus() == null) {
                student.setStatus(existingStudent.getStatus());
            }

            // 校验补齐后的完整字段。
            validateStudent(student);

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
            // 先确认学生存在，不存在时返回 404。
            if (studentDao.findById(id) == null) {
                throw new NotFoundException("学生不存在");
            }

            // 再统计该学生名下的成绩数量。
            long scoreCount = scoreDao.countByStudentId(id);

            // 如果已有成绩记录，业务规则要求先清理成绩，不允许直接删除学生。
            if (scoreCount > 0) {
                throw new ConflictException("该学生已有考试成绩，请先清理成绩记录或注销学籍");
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

        // 规范化页码与每页条数，保证分页回显值和 DAO 实际执行的 SQL 一致。
        queryDTO.setPage(PageUtil.normalizePage(queryDTO.getPage()));
        queryDTO.setPageSize(PageUtil.normalizePageSize(queryDTO.getPageSize()));

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
     * 为可缺省字段填充数据库默认值。
     * <p>gender、status 在数据库中是 NOT NULL 且有默认值，但显式写入 NULL 会覆盖默认值导致报错，
     * 因此在业务层先补齐，保证行为可预期。</p>
     *
     * @param student 学生实体，可能为 null
     */
    private void applyDefaults(Student student) {
        // 学生对象为空时交给后续校验统一报错。
        if (student == null) {
            return;
        }

        // 性别缺省时默认为 1（男），与数据库默认值保持一致。
        if (student.getGender() == null) {
            student.setGender(1);
        }

        // 学籍状态缺省时默认为 1（在读），与数据库默认值保持一致。
        if (student.getStatus() == null) {
            student.setStatus(1);
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

        // 学号是业务唯一标识，必须填写且不能超过数据库列长度。
        requireText(student.getStudentNo(), "学号不能为空");
        requireMaxLength(student.getStudentNo(), MAX_STUDENT_NO_LENGTH, "学号长度不能超过 32 个字符");

        // 姓名是学生档案的核心字段，必须填写且长度 1~50。
        requireText(student.getName(), "姓名不能为空");
        requireMaxLength(student.getName(), MAX_NAME_LENGTH, "姓名长度不能超过 50 个字符");

        // 专业是必填字段。
        requireText(student.getMajor(), "专业不能为空");
        requireMaxLength(student.getMajor(), MAX_MAJOR_LENGTH, "专业长度不能超过 100 个字符");

        // 班级是必填字段。
        requireText(student.getClassName(), "班级不能为空");
        requireMaxLength(student.getClassName(), MAX_CLASS_NAME_LENGTH, "班级长度不能超过 50 个字符");

        // 性别取值范围必须是 0-未知 / 1-男 / 2-女。
        Integer gender = student.getGender();
        if (gender == null || (gender != 0 && gender != 1 && gender != 2)) {
            throw new IllegalArgumentException("性别只能是 0-未知、1-男、2-女");
        }

        // 学籍状态取值范围必须是 1~4。
        Integer status = student.getStatus();
        if (status == null || status < 1 || status > 4) {
            throw new IllegalArgumentException("学籍状态只能是 1-在读、2-休学、3-退学、4-毕业");
        }

        // 出生日期可以为空，但不能晚于今天。
        LocalDate birthDate = student.getBirthDate();
        if (birthDate != null && birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("出生日期不能晚于今天");
        }

        // 联系电话可选，但填写后必须是合法的 11 位手机号。
        String phone = student.getPhone();
        if (phone != null && !phone.isBlank()) {
            requireMaxLength(phone, MAX_PHONE_LENGTH, "联系电话长度不能超过 20 个字符");
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                throw new IllegalArgumentException("联系电话格式不正确");
            }
        }

        // 电子邮箱可选，但填写后必须符合邮箱格式。
        String email = student.getEmail();
        if (email != null && !email.isBlank()) {
            requireMaxLength(email, MAX_EMAIL_LENGTH, "电子邮箱长度不能超过 100 个字符");
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new IllegalArgumentException("电子邮箱格式不正确");
            }
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
            throw new IllegalArgumentException("学生 ID 不能为空");
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
     * <p>并发写入时唯一索引仍可能被触发，此时转换为 409 冲突响应。</p>
     *
     * @param message         默认错误提示
     * @param conflictMessage 唯一性冲突提示
     * @param exception       原始数据库异常
     * @return 运行时异常
     */
    private RuntimeException wrapInsertException(String message, String conflictMessage, SQLException exception) {
        // SQLState 23000 或 MySQL 错误码 1062 表示唯一键冲突。
        if (isDuplicateKey(exception)) {
            return new ConflictException(conflictMessage);
        }

        // 其他数据库异常保留原始异常链。
        return wrapException(message, exception);
    }

    /**
     * 判断数据库异常是否为唯一键冲突。
     *
     * @param exception 数据库异常
     * @return true 表示唯一键冲突
     */
    private boolean isDuplicateKey(SQLException exception) {
        // 同时兼容 SQL 标准 SQLState 和 MySQL 专有错误码。
        return "23000".equals(exception.getSQLState()) || exception.getErrorCode() == 1062;
    }
}
