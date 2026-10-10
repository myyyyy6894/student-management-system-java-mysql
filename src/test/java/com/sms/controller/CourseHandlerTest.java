package com.sms.controller;

// 导入 JDK 内置 HttpServer，用于启动临时测试服务器。
import com.sun.net.httpserver.HttpServer;
// 导入课程 DAO，用于准备和清理测试数据。
import com.sms.dao.impl.CourseDaoImpl;
// 导入课程实体，用于查询测试课程是否存在。
import com.sms.model.entity.Course;
// 导入统一响应结构。
import com.sms.model.vo.CommonResult;
// 导入 JSON 工具。
import com.sms.util.JsonUtil;
// 导入网络地址类。
import java.net.InetSocketAddress;
// 导入 HTTP 客户端和请求响应类。
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
// 导入集合类型，用于检查课程列表。
import java.util.List;
// 导入 Map，用于解析响应数据。
import java.util.Map;

/**
 * CourseHandler 的真实 HTTP 集成测试类。
 * <p>当前项目还没有 JUnit 依赖，所以这里提供 main 方法直接运行。</p>
 * <p>测试会启动一个随机端口的临时 HttpServer，并验证课程接口的增删改查流程。</p>
 */
public class CourseHandlerTest {

    // 测试课程编号；固定值方便测试前后清理遗留数据。
    private static final String TEST_COURSE_NO = "TEST-HTTP-COURSE999";

    // JDK HttpClient 是线程安全的，整个测试可以复用一个客户端对象。
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    // 临时测试服务器地址，测试启动后生成。
    private static String baseUrl;

    /**
     * 测试入口。
     *
     * @param args 命令行参数，当前测试不需要传入任何参数
     * @throws Exception 服务器启动或 HTTP 请求失败时抛出
     */
    public static void main(String[] args) throws Exception {
        // 输出测试开始提示，方便观察执行流程。
        System.out.println("===== CourseHandler HTTP 测试开始 =====");

        // 准备真实 DAO，用于测试前后的数据清理。
        CourseDaoImpl courseDao = new CourseDaoImpl();

        HttpServer server = null;

        try {
            // 启动临时 HttpServer，绑定 127.0.0.1 的随机端口。
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

            // 把 CourseHandler 注册到 /api/courses 路径。
            server.createContext("/api/courses", new CourseHandler());

            // 测试程序是单线程顺序执行，不需要线程池。
            server.setExecutor(null);

            // 启动服务器。
            server.start();

            // 读取系统分配的随机端口，构造基础请求地址。
            baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/api/courses";

            // 输出测试服务器地址。
            System.out.println("测试服务器已启动：" + baseUrl);

            // 先清理可能遗留的测试课程。
            deleteTestCourseIfExists(courseDao, TEST_COURSE_NO);

            // 测试新增课程接口。
            Long courseId = testCreate();

            // 测试查询全部课程接口。
            testList();

            // 测试课程详情接口。
            testDetail(courseId);

            // 测试修改课程接口。
            testUpdate(courseId);

            // 测试删除课程接口。
            testDelete(courseId);

            // 测试删除后的 404 详情查询。
            testDetailNotFound(courseId);

            // 停止临时服务器，释放端口。
            server.stop(0);

            // 输出整体成功提示。
            System.out.println("===== CourseHandler HTTP 测试全部成功 =====");
        } catch (Exception exception) {
            // 打印完整异常堆栈，便于定位问题。
            exception.printStackTrace();

            // 测试失败时兜底清理测试数据。
            deleteTestCourseIfExists(courseDao, TEST_COURSE_NO);

            // 确保临时服务器关闭。
            if (server != null) {
                server.stop(0);
            }

            // 让进程以失败状态退出。
            System.exit(1);
        }
    }

    /**
     * 测试 POST /api/courses 新增课程。
     *
     * @return 新增课程的主键 ID
     * @throws Exception HTTP 请求或响应解析失败时抛出
     */
    private static Long testCreate() throws Exception {
        // 构造新增课程 JSON。
        String requestBody = """
                {
                  "courseNo": "TEST-HTTP-COURSE999",
                  "name": "HTTP测试课程",
                  "credit": 3.5,
                  "teacher": "HTTP测试老师"
                }
                """;

        // 构造 POST 请求。
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        // 发送请求并读取响应。
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // 校验 HTTP 状态码为 200。
        expectStatus(response, 200, "新增课程");

        // 解析统一响应结构。
        CommonResult<Object> result = parseResult(response);

        // 校验业务 code 为 200。
        expectCode(result, 200, "新增课程");

        // 校验 data 中包含新增 ID。
        Long courseId = readId(result.getData(), "新增课程响应");

        // 输出成功提示。
        System.out.println("【1】POST /api/courses 新增成功，ID：" + courseId);

        // 返回新增课程 ID。
        return courseId;
    }

    /**
     * 测试 GET /api/courses 列表接口。
     *
     * @throws Exception HTTP 请求或响应解析失败时抛出
     */
    private static void testList() throws Exception {
        // 构造 GET 请求。
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl)).GET().build();

        // 发送请求并读取响应。
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // 校验 HTTP 状态码为 200。
        expectStatus(response, 200, "查询课程列表");

        // 解析统一响应结构。
        CommonResult<Object> result = parseResult(response);

        // 校验业务 code 为 200。
        expectCode(result, 200, "查询课程列表");

        // 校验 data 是集合。
        if (!(result.getData() instanceof List)) {
            // 如果 data 不是数组，说明响应结构不符合约定。
            throw new IllegalStateException("课程列表响应 data 应该是集合，实际值：" + result.getData());
        }

        // 将 data 转成课程集合。
        List<?> courses = (List<?>) result.getData();

        // 校验列表中包含测试课程。
        boolean found = courses.stream().anyMatch(item -> item.toString().contains(TEST_COURSE_NO));

        // 如果没有找到测试课程，说明列表查询有问题。
        if (!found) {
            throw new IllegalStateException("课程列表中没有找到测试课程：" + TEST_COURSE_NO);
        }

        // 输出列表查询成功提示。
        System.out.println("【2】GET /api/courses 列表查询成功，条数：" + courses.size());
    }

    /**
     * 测试 GET /api/courses/{id} 详情接口。
     *
     * @param courseId 课程 ID
     * @throws Exception HTTP 请求或响应解析失败时抛出
     */
    private static void testDetail(Long courseId) throws Exception {
        // 构造详情请求地址。
        URI uri = URI.create(baseUrl + "/" + courseId);

        // 构造 GET 请求。
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();

        // 发送请求并读取响应。
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // 校验 HTTP 状态码为 200。
        expectStatus(response, 200, "查询课程详情");

        // 解析统一响应结构。
        CommonResult<Object> result = parseResult(response);

        // 校验业务 code 为 200。
        expectCode(result, 200, "查询课程详情");

        // 校验 data 包含课程编号。
        String body = JsonUtil.toJson(result.getData());
        if (body == null || !body.contains(TEST_COURSE_NO)) {
            // 如果没有课程编号，说明详情查询到了错误记录。
            throw new IllegalStateException("详情查询未返回测试课程：" + body);
        }

        // 输出详情查询成功提示。
        System.out.println("【3】GET /api/courses/" + courseId + " 详情查询成功");
    }

    /**
     * 测试 PUT /api/courses/{id} 修改接口。
     *
     * @param courseId 课程 ID
     * @throws Exception HTTP 请求或响应解析失败时抛出
     */
    private static void testUpdate(Long courseId) throws Exception {
        // 构造修改课程 JSON；课程编号使用旧值。
        String requestBody = """
                {
                  "courseNo": "TEST-HTTP-COURSE999",
                  "name": "HTTP测试课程-已更新",
                  "credit": 4.0,
                  "teacher": "HTTP测试老师-已更新"
                }
                """;

        // 构造 PUT 请求。
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/" + courseId))
                .header("Content-Type", "application/json; charset=UTF-8")
                .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        // 发送请求并读取响应。
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // 校验 HTTP 状态码为 200。
        expectStatus(response, 200, "修改课程");

        // 解析统一响应结构。
        CommonResult<Object> result = parseResult(response);

        // 校验业务 code 为 200。
        expectCode(result, 200, "修改课程");

        // 输出修改成功提示。
        System.out.println("【4】PUT /api/courses/" + courseId + " 修改成功");
    }

    /**
     * 测试 DELETE /api/courses/{id} 删除接口。
     *
     * @param courseId 课程 ID
     * @throws Exception HTTP 请求或响应解析失败时抛出
     */
    private static void testDelete(Long courseId) throws Exception {
        // 构造 DELETE 请求。
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/" + courseId))
                .DELETE()
                .build();

        // 发送请求并读取响应。
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // 校验 HTTP 状态码为 200。
        expectStatus(response, 200, "删除课程");

        // 解析统一响应结构。
        CommonResult<Object> result = parseResult(response);

        // 校验业务 code 为 200。
        expectCode(result, 200, "删除课程");

        // 输出删除成功提示。
        System.out.println("【5】DELETE /api/courses/" + courseId + " 删除成功");
    }

    /**
     * 测试删除后再查询详情返回 404。
     *
     * @param courseId 已删除的课程 ID
     * @throws Exception HTTP 请求失败时抛出
     */
    private static void testDetailNotFound(Long courseId) throws Exception {
        // 构造详情请求地址。
        URI uri = URI.create(baseUrl + "/" + courseId);

        // 构造 GET 请求。
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();

        // 发送请求并读取响应。
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // 校验 HTTP 状态码为 404。
        expectStatus(response, 404, "删除后查询课程详情");

        // 解析统一响应结构。
        CommonResult<Object> result = parseResult(response);

        // 校验业务 code 为 404。
        expectCode(result, 404, "删除后查询课程详情");

        // 输出 404 测试成功提示。
        System.out.println("【6】删除后课程详情查询返回 404 成功");
    }

    /**
     * 从 data 对象中读取 id 字段。
     *
     * @param data       响应数据
     * @param actionName 测试动作名称
     * @return 数字 ID
     */
    private static Long readId(Object data, String actionName) {
        // data 必须是对象。
        if (!(data instanceof Map)) {
            // 如果不是对象，说明响应结构不符合约定。
            throw new IllegalStateException(actionName + " data 应该是对象，实际值：" + data);
        }

        // 将 data 转成 Map。
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) data;

        // 读取 id 字段。
        Object idValue = map.get("id");

        // id 必须是数字。
        if (!(idValue instanceof Number)) {
            // 如果不是数字，说明响应结构不符合约定。
            throw new IllegalStateException(actionName + " 缺少有效 id，实际值：" + idValue);
        }

        // 转换成 Long。
        return ((Number) idValue).longValue();
    }

    /**
     * 发送请求并解析 CommonResult。
     *
     * @param response HTTP 响应
     * @return CommonResult 对象
     */
    private static CommonResult<Object> parseResult(HttpResponse<String> response) {
        // 获取响应体。
        String body = response.body();

        // 校验响应体不为空。
        if (body == null || body.isBlank()) {
            // 如果响应体为空，说明服务端没有按 JSON 格式输出。
            throw new IllegalStateException("HTTP 响应体为空");
        }

        // 解析统一响应结构。
        CommonResult<Object> result = JsonUtil.fromJson(body, CommonResult.class);

        // 校验反序列化结果不为空。
        if (result == null) {
            // 如果为 null，说明 JSON 解析失败。
            throw new IllegalStateException("HTTP 响应体无法解析为 CommonResult：" + body);
        }

        // 返回解析结果。
        return result;
    }

    /**
     * 校验 HTTP 状态码。
     *
     * @param response     HTTP 响应
     * @param expectedCode 期望状态码
     * @param actionName   测试动作名称
     */
    private static void expectStatus(HttpResponse<String> response, int expectedCode, String actionName) {
        // 如果状态码不一致，直接失败并输出响应体，方便定位问题。
        if (response.statusCode() != expectedCode) {
            throw new IllegalStateException(
                    actionName + " 的 HTTP 状态码错误，期望：" + expectedCode
                            + "，实际：" + response.statusCode()
                            + "，响应体：" + response.body()
            );
        }
    }

    /**
     * 校验 CommonResult 的业务 code。
     *
     * @param result       统一响应对象
     * @param expectedCode 期望业务 code
     * @param actionName   测试动作名称
     */
    private static void expectCode(CommonResult<?> result, int expectedCode, String actionName) {
        // 如果业务 code 不一致，直接失败。
        if (result.getCode() != expectedCode) {
            throw new IllegalStateException(
                    actionName + " 的业务 code 错误，期望：" + expectedCode
                            + "，实际：" + result.getCode()
                            + "，message：" + result.getMessage()
            );
        }
    }

    /**
     * 如果测试课程存在，则删除该课程。
     *
     * @param courseDao 课程 DAO
     * @param courseNo  测试课程编号
     */
    private static void deleteTestCourseIfExists(CourseDaoImpl courseDao, String courseNo) {
        try {
            // 按课程编号查询测试课程。
            Course existingCourse = courseDao.findByCourseNo(courseNo);

            // 如果课程存在，则删除。
            if (existingCourse != null) {
                // 按主键删除测试课程。
                courseDao.deleteById(existingCourse.getId());

                // 输出清理提示。
                System.out.println("已清理遗留测试课程：" + courseNo);
            }
        } catch (Exception exception) {
            // 清理失败时提示，避免影响主流程排查。
            System.err.println("清理测试课程失败：" + exception.getMessage());
        }
    }
}
