package com.sms.controller;

// 导入 Gson 的 JSON 语法异常，用于把非法请求体转换为业务参数错误。
import com.google.gson.JsonSyntaxException;
// 导入日期解析异常，用于把非法日期字符串转换成 400 参数错误。
import java.time.format.DateTimeParseException;
// 导入 HTTP 请求对象。
import com.sun.net.httpserver.HttpExchange;
// 导入学生查询参数对象，用于接收 URL Query 参数。
import com.sms.model.dto.StudentQueryDTO;
// 导入学生实体类，用于接收 JSON 请求体和返回查询结果。
import com.sms.model.entity.Student;
// 导入分页结果对象。
import com.sms.model.vo.PageResult;
// 导入统一响应包装类。
import com.sms.model.vo.CommonResult;
// 导入学生业务接口。
import com.sms.service.StudentService;
// 导入学生业务默认实现类，普通项目中由 Handler 创建。
import com.sms.service.impl.StudentServiceImpl;
// 导入 JSON 工具类。
import com.sms.util.JsonUtil;
// 导入 IO 异常。
import java.io.IOException;
// 导入 Map 和 LinkedHashMap，用于解析 Query 参数和构造新增响应数据。
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 学生 HTTP 控制器。
 * <p>负责把学生相关的 HTTP 请求分发到 StudentService，并把处理结果包装成统一 JSON 响应。</p>
 */
public class StudentHandler extends BaseHttpHandler {

    // 学生资源基础路径，用于解析 /api/students/{id} 中的 {id}。
    private static final String BASE_PATH = "/api/students";

    // 学生业务接口，StudentHandler 不直接依赖具体实现类。
    private final StudentService studentService;

    /**
     * 无参构造器，普通项目中使用默认 Service 实现。
     */
    public StudentHandler() {
        // 默认使用真实的学生业务实现。
        this(new StudentServiceImpl());
    }

    /**
     * 支持依赖注入的构造器，测试时可以替换业务实现。
     *
     * @param studentService 学生业务接口
     */
    public StudentHandler(StudentService studentService) {
        // Service 不能为空，否则路由请求时会出现空指针。
        if (studentService == null) {
            throw new IllegalArgumentException("studentService 不能为空");
        }

        // 保存业务对象。
        this.studentService = studentService;
    }

    /**
     * 根据请求方法分发到具体的业务方法。
     *
     * @param exchange 当前 HTTP 请求对象
     * @throws Exception 路由处理失败时抛出，由 BaseHttpHandler 统一捕获
     */
    @Override
    protected void processRequest(HttpExchange exchange) throws Exception {
        // 读取请求方法并统一转为大写，避免浏览器传来小写方法。
        String method = exchange.getRequestMethod().toUpperCase();

        // 根据 RESTful 语义分发。
        switch (method) {
            // GET 可能是列表查询，也可能是详情查询。
            case "GET" -> doListOrDetail(exchange);
            // POST 表示新增学生。
            case "POST" -> doCreate(exchange);
            // PUT 表示修改学生。
            case "PUT" -> doUpdate(exchange);
            // DELETE 表示删除学生。
            case "DELETE" -> doDelete(exchange);
            // 其他方法不符合接口约定。
            default -> sendJsonResponse(
                    exchange,
                    405,
                    JsonUtil.toJson(CommonResult.error(405, "不支持的请求方法"))
            );
        }
    }

    /**
     * 处理 GET 请求：没有路径 ID 时查询分页列表；有路径 ID 时查询学生详情。
     *
     * @param exchange 当前 HTTP 请求对象
     * @throws Exception 业务处理失败时抛出
     */
    private void doListOrDetail(HttpExchange exchange) throws Exception {
        // 从路径末尾提取数字 ID。
        Long studentId = extractPathId(exchange, BASE_PATH);

        // 如果没有 ID，说明当前请求的是集合路径 /api/students。
        if (studentId == null) {
            doList(exchange);
        } else {
            // 如果有 ID，说明请求的是详情路径 /api/students/{id}。
            doDetail(exchange, studentId);
        }
    }

    /**
     * 处理学生分页条件查询。
     *
     * @param exchange 当前 HTTP 请求对象
     * @throws Exception 业务处理失败时抛出
     */
    private void doList(HttpExchange exchange) throws Exception {
        // 解析 URL 中的查询参数。
        Map<String, String> queryParams = parseQueryParams(exchange);

        // 创建查询参数对象，并填充 Service/DAO 需要的条件字段。
        StudentQueryDTO queryDTO = new StudentQueryDTO();
        queryDTO.setPage(parseInt(queryParams.get("page"), 1));
        queryDTO.setPageSize(parseInt(queryParams.get("pageSize"), 10));
        queryDTO.setKeyword(cleanText(queryParams.get("keyword")));
        queryDTO.setMajor(cleanText(queryParams.get("major")));
        queryDTO.setClassName(cleanText(queryParams.get("className")));
        queryDTO.setStatus(parseIntOrNull(queryParams.get("status")));

        // 调用业务层完成条件分页查询。
        PageResult<Student> pageResult = studentService.queryStudents(queryDTO);

        // 输出统一 JSON 响应。
        sendJsonResponse(
                exchange,
                200,
                JsonUtil.toJson(CommonResult.success("查询成功", pageResult))
        );
    }

    /**
     * 处理学生详情查询。
     *
     * @param exchange  当前 HTTP 请求对象
     * @param studentId 学生主键 ID
     * @throws Exception 业务处理失败时抛出
     */
    private void doDetail(HttpExchange exchange, Long studentId) throws Exception {
        // 调用业务层查询学生。
        Student student = studentService.getStudentById(studentId);

        // 如果学生不存在，返回 404。
        if (student == null) {
            sendJsonResponse(
                    exchange,
                    404,
                    JsonUtil.toJson(CommonResult.error(404, "学生不存在"))
            );
            return;
        }

        // 输出学生详情。
        sendJsonResponse(
                exchange,
                200,
                JsonUtil.toJson(CommonResult.success("查询成功", student))
        );
    }

    /**
     * 处理新增学生请求。
     *
     * @param exchange 当前 HTTP 请求对象
     * @throws Exception 业务处理失败时抛出
     */
    private void doCreate(HttpExchange exchange) throws Exception {
        // 读取并反序列化请求体。
        Student student = parseStudentBody(exchange);

        // 调用业务层新增学生；Service 会校验必填字段和学号重复。
        studentService.addStudent(student);

        // 按接口文档要求，新增成功后只返回新生成的学生 ID。
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", student.getId());

        // 输出统一 JSON 响应。
        sendJsonResponse(
                exchange,
                200,
                JsonUtil.toJson(CommonResult.success("学生添加成功", data))
        );
    }

    /**
     * 处理修改学生请求。
     *
     * @param exchange 当前 HTTP 请求对象
     * @throws Exception 业务处理失败时抛出
     */
    private void doUpdate(HttpExchange exchange) throws Exception {
        // 修改接口必须携带路径 ID。
        Long studentId = extractPathId(exchange, BASE_PATH);

        // ID 为空说明请求路径不是 /api/students/{id}。
        if (studentId == null) {
            sendJsonResponse(
                    exchange,
                    400,
                    JsonUtil.toJson(CommonResult.error(400, "学生 ID 不能为空"))
            );
            return;
        }

        // 读取并反序列化请求体。
        Student student = parseStudentBody(exchange);

        // 修改记录的 ID 必须以路径参数为准，不能信任请求体中的 id。
        student.setId(studentId);

        // 先查询旧数据；业务层的“学生不存在”校验也需要主键存在。
        Student existingStudent = studentService.getStudentById(studentId);

        // 如果学生不存在，返回 404。
        if (existingStudent == null) {
            sendJsonResponse(
                    exchange,
                    404,
                    JsonUtil.toJson(CommonResult.error(404, "学生不存在"))
            );
            return;
        }

        // 接口文档规定修改 Body 通常不含学号。
        // 如果调用方没有传学号，就用数据库里的旧学号回填。
        if (student.getStudentNo() == null || student.getStudentNo().isBlank()) {
            student.setStudentNo(existingStudent.getStudentNo());
        }

        // 调用业务层修改；如果 Body 中传了不同的学号，Service 会拦截。
        studentService.updateStudent(student);

        // 输出统一 JSON 响应。
        sendJsonResponse(
                exchange,
                200,
                JsonUtil.toJson(CommonResult.success("学生档案修改成功", true))
        );
    }

    /**
     * 处理删除学生请求。
     *
     * @param exchange 当前 HTTP 请求对象
     * @throws Exception 业务处理失败时抛出
     */
    private void doDelete(HttpExchange exchange) throws Exception {
        // 删除接口必须携带路径 ID。
        Long studentId = extractPathId(exchange, BASE_PATH);

        // ID 为空说明请求路径不是 /api/students/{id}。
        if (studentId == null) {
            sendJsonResponse(
                    exchange,
                    400,
                    JsonUtil.toJson(CommonResult.error(400, "学生 ID 不能为空"))
            );
            return;
        }

        // 先确认学生存在，避免 Service 只给出 IllegalArgumentException 导致无法区分 404。
        Student existingStudent = studentService.getStudentById(studentId);

        // 如果学生不存在，返回 404。
        if (existingStudent == null) {
            sendJsonResponse(
                    exchange,
                    404,
                    JsonUtil.toJson(CommonResult.error(404, "学生不存在"))
            );
            return;
        }

        // 调用业务层删除；如果学生名下有成绩，Service 会拦截。
        studentService.deleteStudent(studentId);

        // 输出统一 JSON 响应。
        sendJsonResponse(
                exchange,
                200,
                JsonUtil.toJson(CommonResult.success("学生删除成功", true))
        );
    }

    /**
     * 读取并解析 Student JSON 请求体。
     *
     * @param exchange 当前 HTTP 请求对象
     * @return 学生实体对象
     * @throws IOException 读取请求体失败时抛出
     */
    private Student parseStudentBody(HttpExchange exchange) throws IOException {
        // 读取 POST/PUT 的 JSON 字符串。
        String requestBody = readRequestBody(exchange);

        // 空请求体无法反序列化成 Student，属于请求参数错误。
        if (requestBody == null || requestBody.isBlank()) {
            throw new IllegalArgumentException("请求体不能为空");
        }

        try {
            // 使用 JsonUtil 将 JSON 转换成 Student 实体。
            Student student = JsonUtil.fromJson(requestBody, Student.class);

            // 如果 JSON 内容是 null，也视为参数错误。
            if (student == null) {
                throw new IllegalArgumentException("请求体不能为空");
            }

            // 返回解析完成的学生对象。
            return student;
        } catch (JsonSyntaxException exception) {
            // JSON 语法非法时统一返回业务参数错误。
            throw new IllegalArgumentException("请求体 JSON 格式错误");
        } catch (DateTimeParseException exception) {
            // 日期字符串无法解析（例如 2024-13-45）属于 400 参数错误，而不是 500 服务器错误。
            // 必须单独捕获，因为 DateTimeParseException 并不是 JsonSyntaxException 的子类。
            throw new IllegalArgumentException("日期格式不正确，日期应使用 yyyy-MM-dd，时间应使用 yyyy-MM-dd HH:mm:ss");
        }
    }

    /**
     * 解析 Query 参数中的 int 值。
     *
     * @param value        原始字符串
     * @param defaultValue 参数不存在或为空时使用的默认值
     * @return int 值
     */
    private int parseInt(String value, int defaultValue) {
        // 参数不存在时使用默认值。
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            // 将字符串转换为 int。
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            // 参数格式非法时返回 400。
            throw new IllegalArgumentException("分页参数必须是数字");
        }
    }

    /**
     * 解析 Query 参数中的 Integer 值。
     *
     * @param value 原始字符串
     * @return Integer 值；参数不存在时返回 null
     */
    private Integer parseIntOrNull(String value) {
        // 参数不存在时保持 null，表示不按该字段筛选。
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            // 将字符串转换为 Integer。
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            // 参数格式非法时返回 400。
            throw new IllegalArgumentException("状态参数必须是数字");
        }
    }

    /**
     * 清理字符串参数。
     *
     * @param value 原始字符串
     * @return 去除首尾空白后的字符串；空串转换为 null
     */
    private String cleanText(String value) {
        // null 直接返回 null。
        if (value == null) {
            return null;
        }

        // 去除首尾空白。
        String trimmed = value.trim();

        // 空字符串视为没有筛选条件。
        return trimmed.isEmpty() ? null : trimmed;
    }
}
