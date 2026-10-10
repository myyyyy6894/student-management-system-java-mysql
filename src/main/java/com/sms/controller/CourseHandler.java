package com.sms.controller;

// 导入 HTTP 请求对象。
import com.sun.net.httpserver.HttpExchange;
// 导入课程实体类。
import com.sms.model.entity.Course;
// 导入统一响应包装类。
import com.sms.model.vo.CommonResult;
// 导入课程业务接口。
import com.sms.service.CourseService;
// 导入课程业务默认实现类。
import com.sms.service.impl.CourseServiceImpl;
// 导入 JSON 工具类。
import com.sms.util.JsonUtil;
// 导入 Gson 的 JSON 语法异常，用于转换非法请求体。
import com.google.gson.JsonSyntaxException;
// 导入日期解析异常，用于把非法日期字符串转换成 400 参数错误。
import java.time.format.DateTimeParseException;
// 导入 IO 异常。
import java.io.IOException;
// 导入 Map 和 LinkedHashMap，用于构造响应数据。
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

/**
 * 课程 HTTP 控制器。
 * <p>负责课程相关 HTTP 请求的路由分发和统一 JSON 输出。</p>
 */
public class CourseHandler extends BaseHttpHandler {

    // 课程资源基础路径。
    private static final String BASE_PATH = "/api/courses";

    // 课程业务接口。
    private final CourseService courseService;

    /**
     * 无参构造器，使用默认课程业务实现。
     */
    public CourseHandler() {
        // 默认使用真实的课程业务实现。
        this(new CourseServiceImpl());
    }

    /**
     * 支持依赖注入的构造器。
     *
     * @param courseService 课程业务接口
     */
    public CourseHandler(CourseService courseService) {
        // Service 不能为空。
        if (courseService == null) {
            throw new IllegalArgumentException("courseService 不能为空");
        }

        // 保存业务对象。
        this.courseService = courseService;
    }

    /**
     * 根据 HTTP 方法分发课程请求。
     *
     * @param exchange 当前 HTTP 请求对象
     * @throws Exception 业务处理失败时抛出
     */
    @Override
    protected void processRequest(HttpExchange exchange) throws Exception {
        // 读取请求方法并统一转为大写。
        String method = exchange.getRequestMethod().toUpperCase();

        // 根据 RESTful 语义分发。
        switch (method) {
            // GET 可能是课程列表，也可能是课程详情。
            case "GET" -> doListOrDetail(exchange);
            // POST 表示新增课程。
            case "POST" -> doCreate(exchange);
            // PUT 表示修改课程。
            case "PUT" -> doUpdate(exchange);
            // DELETE 表示删除课程。
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
     * 处理 GET 请求：没有路径 ID 时查询课程列表；有路径 ID 时查询课程详情。
     *
     * @param exchange 当前 HTTP 请求对象
     * @throws Exception 业务处理失败时抛出
     */
    private void doListOrDetail(HttpExchange exchange) throws Exception {
        // 从路径末尾提取课程 ID。
        Long courseId = extractPathId(exchange, BASE_PATH);

        // 如果没有 ID，说明当前请求的是课程列表。
        if (courseId == null) {
            doList(exchange);
        } else {
            // 如果有 ID，说明当前请求的是课程详情。
            doDetail(exchange, courseId);
        }
    }

    /**
     * 处理课程列表查询。
     *
     * @param exchange 当前 HTTP 请求对象
     * @throws Exception 业务处理失败时抛出
     */
    private void doList(HttpExchange exchange) throws Exception {
        // 调用业务层查询全部课程。
        List<Course> courses = courseService.listAllCourses();

        // 输出统一 JSON 响应。
        sendJsonResponse(
                exchange,
                200,
                JsonUtil.toJson(CommonResult.success("查询成功", courses))
        );
    }

    /**
     * 处理课程详情查询。
     *
     * @param exchange 当前 HTTP 请求对象
     * @param courseId 课程主键 ID
     * @throws Exception 业务处理失败时抛出
     */
    private void doDetail(HttpExchange exchange, Long courseId) throws Exception {
        // 调用业务层查询课程。
        Course course = courseService.getCourseById(courseId);

        // 如果课程不存在，返回 404。
        if (course == null) {
            sendJsonResponse(
                    exchange,
                    404,
                    JsonUtil.toJson(CommonResult.error(404, "课程不存在"))
            );
            return;
        }

        // 输出课程详情。
        sendJsonResponse(
                exchange,
                200,
                JsonUtil.toJson(CommonResult.success("查询成功", course))
        );
    }

    /**
     * 处理新增课程请求。
     *
     * @param exchange 当前 HTTP 请求对象
     * @throws Exception 业务处理失败时抛出
     */
    private void doCreate(HttpExchange exchange) throws Exception {
        // 读取并解析课程 JSON 请求体。
        Course course = parseCourseBody(exchange);

        // 调用业务层新增课程；Service 会校验必填字段、学分范围和编号重复。
        courseService.addCourse(course);

        // 按接口风格返回新生成的课程 ID。
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", course.getId());

        // 输出统一 JSON 响应。
        sendJsonResponse(
                exchange,
                200,
                JsonUtil.toJson(CommonResult.success("课程添加成功", data))
        );
    }

    /**
     * 处理修改课程请求。
     *
     * @param exchange 当前 HTTP 请求对象
     * @throws Exception 业务处理失败时抛出
     */
    private void doUpdate(HttpExchange exchange) throws Exception {
        // 修改接口必须携带路径 ID。
        Long courseId = extractPathId(exchange, BASE_PATH);

        // ID 为空说明请求路径不是 /api/courses/{id}。
        if (courseId == null) {
            sendJsonResponse(
                    exchange,
                    400,
                    JsonUtil.toJson(CommonResult.error(400, "课程 ID 不能为空"))
            );
            return;
        }

        // 读取并解析课程 JSON 请求体。
        Course course = parseCourseBody(exchange);

        // 修改记录的 ID 以路径参数为准，不能信任请求体中的 id。
        course.setId(courseId);

        // 先查询旧数据，确认课程存在并处理未填写的编号。
        Course existingCourse = courseService.getCourseById(courseId);

        // 如果课程不存在，返回 404。
        if (existingCourse == null) {
            sendJsonResponse(
                    exchange,
                    404,
                    JsonUtil.toJson(CommonResult.error(404, "课程不存在"))
            );
            return;
        }

        // 如果 Body 中没有课程编号，则继续使用旧编号。
        if (course.getCourseNo() == null || course.getCourseNo().isBlank()) {
            course.setCourseNo(existingCourse.getCourseNo());
        }

        // 调用业务层修改课程。
        courseService.updateCourse(course);

        // 输出统一 JSON 响应。
        sendJsonResponse(
                exchange,
                200,
                JsonUtil.toJson(CommonResult.success("课程修改成功", true))
        );
    }

    /**
     * 处理删除课程请求。
     *
     * @param exchange 当前 HTTP 请求对象
     * @throws Exception 业务处理失败时抛出
     */
    private void doDelete(HttpExchange exchange) throws Exception {
        // 删除接口必须携带路径 ID。
        Long courseId = extractPathId(exchange, BASE_PATH);

        // ID 为空说明请求路径不是 /api/courses/{id}。
        if (courseId == null) {
            sendJsonResponse(
                    exchange,
                    400,
                    JsonUtil.toJson(CommonResult.error(400, "课程 ID 不能为空"))
            );
            return;
        }

        // 先确认课程存在，避免 Service 的业务异常导致无法区分 404。
        Course existingCourse = courseService.getCourseById(courseId);

        // 如果课程不存在，返回 404。
        if (existingCourse == null) {
            sendJsonResponse(
                    exchange,
                    404,
                    JsonUtil.toJson(CommonResult.error(404, "课程不存在"))
            );
            return;
        }

        // 调用业务层删除；如果课程已有成绩，Service 会拦截。
        courseService.deleteCourse(courseId);

        // 输出统一 JSON 响应。
        sendJsonResponse(
                exchange,
                200,
                JsonUtil.toJson(CommonResult.success("课程删除成功", true))
        );
    }

    /**
     * 读取并解析 Course JSON 请求体。
     *
     * @param exchange 当前 HTTP 请求对象
     * @return 课程实体对象
     * @throws IOException 读取请求体失败时抛出
     */
    private Course parseCourseBody(HttpExchange exchange) throws IOException {
        // 读取 POST/PUT 的 JSON 字符串。
        String requestBody = readRequestBody(exchange);

        // 空请求体无法反序列化成 Course，属于请求参数错误。
        if (requestBody == null || requestBody.isBlank()) {
            throw new IllegalArgumentException("请求体不能为空");
        }

        try {
            // 使用 JsonUtil 将 JSON 转换成 Course 实体。
            Course course = JsonUtil.fromJson(requestBody, Course.class);

            // 如果 JSON 内容是 null，也视为参数错误。
            if (course == null) {
                throw new IllegalArgumentException("请求体不能为空");
            }

            // 返回解析完成的课程对象。
            return course;
        } catch (JsonSyntaxException exception) {
            // JSON 语法非法时统一返回业务参数错误。
            throw new IllegalArgumentException("请求体 JSON 格式错误");
        } catch (DateTimeParseException exception) {
            // 日期字符串无法解析（例如 2024-13-45）属于 400 参数错误，而不是 500 服务器错误。
            // 必须单独捕获，因为 DateTimeParseException 并不是 JsonSyntaxException 的子类。
            throw new IllegalArgumentException("日期格式不正确，日期应使用 yyyy-MM-dd，时间应使用 yyyy-MM-dd HH:mm:ss");
        }
    }
}
