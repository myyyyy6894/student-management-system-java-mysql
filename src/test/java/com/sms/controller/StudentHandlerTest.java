package com.sms.controller;

// 导入 JDK 内置 HttpServer，用来启动一个临时测试服务器。
import com.sun.net.httpserver.HttpServer;
// 导入学生 DAO 实现，用来准备和清理测试数据。
import com.sms.dao.impl.StudentDaoImpl;
// 导入学生实体，用来查询测试学生是否存在。
import com.sms.model.entity.Student;
// 导入 JSON 工具，用来解析响应结果。
import com.sms.util.JsonUtil;
// 导入统一响应结构，方便直接检查 code/message/data。
import com.sms.model.vo.CommonResult;
// 导入 IO 类，用来读取 HTTP 响应体。
import java.io.IOException;
// 导入网络地址类，用于绑定随机端口。
import java.net.InetSocketAddress;
// 导入网络类，用于发送 HTTP 请求。
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
// 导入时间类型，用于构造测试学生出生日期。
import java.time.LocalDate;
// 导入集合类型，用于处理服务端返回的数据。
import java.util.List;
// 导入 Map，用于解析分页数据。
import java.util.Map;

/**
 * StudentHandler 的真实 HTTP 集成测试类。
 * <p>当前项目还没有 JUnit 依赖，所以这里提供 main 方法直接运行。</p>
 * <p>测试会启动一个随机端口的临时 HttpServer，然后验证学生接口的增删改查流程。</p>
 */
public class StudentHandlerTest {

    // 测试学生学号；固定值方便测试前后清理遗留数据。
    private static final String TEST_STUDENT_NO = "TEST-HTTP-STU999";

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
        System.out.println("===== StudentHandler HTTP 测试开始 =====");

        // 准备真实 DAO，用于测试前后的数据清理。
        StudentDaoImpl studentDao = new StudentDaoImpl();

        try {
            // 启动临时 HttpServer，绑定 127.0.0.1 的随机端口。
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

            // 把 StudentHandler 注册到 /api/students 路径。
            server.createContext("/api/students", new StudentHandler());

            // 测试程序是单线程顺序执行，不需要线程池。
            server.setExecutor(null);

            // 启动服务器。
            server.start();

            // 读取系统分配的随机端口，构造基础请求地址。
            int port = server.getAddress().getPort();
            baseUrl = "http://127.0.0.1:" + port + "/api/students";

            // 输出测试服务器地址。
            System.out.println("测试服务器已启动：" + baseUrl);

            // 先清理可能遗留的测试学生。
            deleteTestStudentIfExists(studentDao, TEST_STUDENT_NO);

            // 测试新增学生接口。
            Long studentId = testCreate();

            // 测试分页查询接口。
            testList(studentId);

            // 测试详情查询接口。
            testDetail(studentId);

            // 测试修改接口。
            testUpdate(studentId);

            // 测试删除接口。
            testDelete(studentId);

            // 测试删除后的 404 详情查询。
            testDetailNotFound(studentId);

            // 停止临时服务器，释放端口。
            server.stop(0);

            // 输出整体成功提示。
            System.out.println("===== StudentHandler HTTP 测试全部成功 =====");
        } catch (Exception exception) {
            // 打印完整异常堆栈，便于定位问题。
            exception.printStackTrace();

            // 测试失败时兜底清理测试数据。
            deleteTestStudentIfExists(studentDao, TEST_STUDENT_NO);

            // 让进程以失败状态退出。
            System.exit(1);
        }
    }

    /**
     * 测试 POST /api/students 新增学生。
     *
     * @return 新增学生的主键 ID
     * @throws Exception HTTP 请求或响应解析失败时抛出
     */
    private static Long testCreate() throws Exception {
        // 构造新增学生 JSON，birth_date 使用 ISO 日期格式。
        String requestBody = """
                {
                  "studentNo": "TEST-HTTP-STU999",
                  "name": "HTTP测试张三",
                  "gender": 1,
                  "birthDate": "2004-05-10",
                  "phone": "13800009999",
                  "email": "http-test@campus.edu.cn",
                  "major": "计算机科学与技术",
                  "className": "计科2401班",
                  "status": 1
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
        expectStatus(response, 200, "新增学生");

        // 解析统一响应结构。
        CommonResult<Object> result = parseResult(response);

        // 校验业务 code 为 200。
        expectCode(result, 200, "新增学生");

        // 校验 data 是对象并读取新增 ID。
        Object data = result.getData();
        if (!(data instanceof Map)) {
            // 如果 data 不是对象，说明响应结构不符合约定。
            throw new IllegalStateException("新增响应 data 应该是对象，实际值：" + data);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> createData = (Map<String, Object>) data;
        Object idValue = createData.get("id");
        if (!(idValue instanceof Number)) {
            // 如果 id 不是数字，说明响应结构不符合约定。
            throw new IllegalStateException("新增学生响应缺少有效 id，实际值：" + idValue);
        }

        // 转换成 Long，方便后续查询和更新。
        Long studentId = ((Number) idValue).longValue();

        // 输出成功提示。
        System.out.println("【1】POST /api/students 新增成功，ID：" + studentId);

        // 返回新增学生 ID。
        return studentId;
    }

    /**
     * 测试 GET /api/students 分页列表接口。
     *
     * @param studentId 刚新增的学生 ID
     * @throws Exception HTTP 请求或响应解析失败时抛出
     */
    private static void testList(Long studentId) throws Exception {
        // 构造分页查询 URL，使用固定学号关键字，避免数据库中其他数据影响结果。
        String url = baseUrl + "?page=1&pageSize=10&keyword=" + TEST_STUDENT_NO;

        // 构造 GET 请求。
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();

        // 发送请求并读取响应。
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // 校验 HTTP 状态码为 200。
        expectStatus(response, 200, "分页查询学生");

        // 由于 CommonResult.data 是 PageResult，这里直接用 Map 解析，避免引入复杂泛型转换。
        CommonResult<Object> result = parseResult(response);

        // 校验业务 code 为 200。
        expectCode(result, 200, "分页查询学生");

        // 校验分页对象是 Map 结构。
        Object data = result.getData();
        if (!(data instanceof Map)) {
            // 如果 data 不是对象，说明响应结构不符合约定。
            throw new IllegalStateException("分页响应 data 应该是对象，实际值：" + data);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> page = (Map<String, Object>) data;

        // 校验分页对象存在。
        if (page == null || page.get("list") == null) {
            // 如果没有 list，说明响应结构不符合约定。
            throw new IllegalStateException("分页响应缺少 list 字段");
        }

        // 将 list 转成原始集合，判断是否包含当前学生。
        List<?> list = (List<?>) page.get("list");

        // 校验至少能查到刚新增的学生。
        if (list.isEmpty()) {
            // 如果没有查到，说明分页查询或关键字筛选有问题。
            throw new IllegalStateException("分页查询没有找到测试学生：" + TEST_STUDENT_NO);
        }

        // 输出分页查询成功提示。
        System.out.println("【2】GET /api/students 分页查询成功，条数：" + list.size());
    }

    /**
     * 测试 GET /api/students/{id} 详情接口。
     *
     * @param studentId 学生 ID
     * @throws Exception HTTP 请求或响应解析失败时抛出
     */
    private static void testDetail(Long studentId) throws Exception {
        // 构造详情请求地址。
        URI uri = URI.create(baseUrl + "/" + studentId);

        // 构造 GET 请求。
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();

        // 发送请求并读取响应。
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // 校验 HTTP 状态码为 200。
        expectStatus(response, 200, "查询学生详情");

        // 解析统一响应结构。
        CommonResult<Object> result = parseResult(response);

        // 校验业务 code 为 200。
        expectCode(result, 200, "查询学生详情");

        // 校验 data 存在。
        if (result.getData() == null) {
            // 如果 data 为空，说明详情查询失败。
            throw new IllegalStateException("详情查询返回的数据为空");
        }

        // 校验返回的学生 ID 正确。
        Object data = result.getData();
        if (!(data instanceof Map)) {
            // 如果 data 不是对象，说明响应结构不符合约定。
            throw new IllegalStateException("详情响应 data 应该是对象，实际值：" + data);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> studentData = (Map<String, Object>) data;
        Object idValue = studentData.get("id");
        if (!(idValue instanceof Number) || studentId.longValue() != ((Number) idValue).longValue()) {
            // 如果不一致，说明查询到了错误记录。
            throw new IllegalStateException("详情查询学生 ID 不正确，实际值：" + idValue);
        }

        // 输出详情查询成功提示。
        System.out.println("【3】GET /api/students/" + studentId + " 详情查询成功");
    }

    /**
     * 测试 PUT /api/students/{id} 修改接口。
     *
     * @param studentId 学生 ID
     * @throws Exception HTTP 请求或响应解析失败时抛出
     */
    private static void testUpdate(Long studentId) throws Exception {
        // 构造修改学生 JSON；接口允许不传学号，Handler 会回填数据库旧学号。
        String requestBody = """
                {
                  "name": "HTTP测试李四",
                  "gender": 2,
                  "birthDate": "2004-06-11",
                  "phone": "13800009888",
                  "email": "http-update@campus.edu.cn",
                  "major": "计算机科学与技术",
                  "className": "计科2401班",
                  "status": 2
                }
                """;

        // 构造 PUT 请求。
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/" + studentId))
                .header("Content-Type", "application/json; charset=UTF-8")
                .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        // 发送请求并读取响应。
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // 校验 HTTP 状态码为 200。
        expectStatus(response, 200, "修改学生");

        // 解析统一响应结构。
        CommonResult<Object> result = parseResult(response);

        // 校验业务 code 为 200。
        expectCode(result, 200, "修改学生");

        // 输出修改成功提示。
        System.out.println("【4】PUT /api/students/" + studentId + " 修改成功");
    }

    /**
     * 测试 DELETE /api/students/{id} 删除接口。
     *
     * @param studentId 学生 ID
     * @throws Exception HTTP 请求或响应解析失败时抛出
     */
    private static void testDelete(Long studentId) throws Exception {
        // 构造 DELETE 请求。
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/" + studentId))
                .DELETE()
                .build();

        // 发送请求并读取响应。
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // 校验 HTTP 状态码为 200。
        expectStatus(response, 200, "删除学生");

        // 解析统一响应结构。
        CommonResult<Object> result = parseResult(response);

        // 校验业务 code 为 200。
        expectCode(result, 200, "删除学生");

        // 输出删除成功提示。
        System.out.println("【5】DELETE /api/students/" + studentId + " 删除成功");
    }

    /**
     * 测试删除后再查询详情返回 404。
     *
     * @param studentId 已删除的学生 ID
     * @throws Exception HTTP 请求失败时抛出
     */
    private static void testDetailNotFound(Long studentId) throws Exception {
        // 构造详情请求地址。
        URI uri = URI.create(baseUrl + "/" + studentId);

        // 构造 GET 请求。
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();

        // 发送请求并读取响应。
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // 校验 HTTP 状态码为 404。
        expectStatus(response, 404, "删除后查询学生详情");

        // 解析统一响应结构。
        CommonResult<Object> result = parseResult(response);

        // 校验业务 code 为 404。
        expectCode(result, 404, "删除后查询学生详情");

        // 输出 404 测试成功提示。
        System.out.println("【6】删除后详情查询返回 404 成功");
    }

    /**
     * 发送请求并解析 CommonResult<T>。
     *
     * @param response HTTP 响应
     * @param dataType data 字段对应的 Java 类型
     * @param <T>      data 字段类型
     * @return CommonResult 对象
     */
    private static CommonResult<Object> parseResult(
            HttpResponse<String> response
    ) {
        // 获取响应体。
        String body = response.body();

        // 校验响应体不为空。
        if (body == null || body.isBlank()) {
            // 如果响应体为空，说明服务端没有按 JSON 格式输出。
            throw new IllegalStateException("HTTP 响应体为空");
        }

        // 解析统一响应结构；data 具体类型在测试方法里按需检查。
        CommonResult<Object> result = JsonUtil.fromJson(
                body,
                com.sms.model.vo.CommonResult.class
        );

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
    private static void expectStatus(
            HttpResponse<String> response,
            int expectedCode,
            String actionName
    ) {
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
    private static void expectCode(
            CommonResult<?> result,
            int expectedCode,
            String actionName
    ) {
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
     * 如果测试学生存在，则删除该学生。
     *
     * @param studentDao 学生 DAO
     * @param studentNo  测试学号
     */
    private static void deleteTestStudentIfExists(StudentDaoImpl studentDao, String studentNo) {
        try {
            // 按学号查询测试学生。
            Student existingStudent = studentDao.findByStudentNo(studentNo);

            // 如果学生存在，则删除。
            if (existingStudent != null) {
                // 按主键删除测试学生。
                studentDao.deleteById(existingStudent.getId());

                // 输出清理提示。
                System.out.println("已清理遗留测试学生：" + studentNo);
            }
        } catch (Exception exception) {
            // 清理失败时提示，避免影响主流程排查。
            System.err.println("清理测试学生失败：" + exception.getMessage());
        }
    }
}
