package com.sms.controller;

// 导入 JDK 内置 HttpServer，用于启动临时测试服务器。
import com.sun.net.httpserver.HttpServer;
// 导入 DAO 实现，用于准备和清理测试数据。
import com.sms.dao.impl.CourseDaoImpl;
import com.sms.dao.impl.ScoreDaoImpl;
import com.sms.dao.impl.StudentDaoImpl;
// 导入实体类，用于构造测试数据。
import com.sms.model.entity.Course;
import com.sms.model.entity.Score;
import com.sms.model.entity.Student;
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
// 导入精确小数类型，用于设置成绩和学分。
import java.math.BigDecimal;
// 导入日期类型，用于设置考试日期。
import java.time.LocalDate;
// 导入集合类型，用于检查成绩明细列表。
import java.util.List;
// 导入 Map，用于解析响应数据。
import java.util.Map;

/**
 * ScoreHandler 的真实 HTTP 集成测试类。
 * <p>当前项目还没有 JUnit 依赖，所以这里提供 main 方法直接运行。</p>
 * <p>测试会启动一个随机端口的临时 HttpServer，并验证成绩接口的录入、修改、查询、统计和删除流程。</p>
 */
public class ScoreHandlerTest {

    // 测试学生学号；固定值方便测试前后清理遗留数据。
    private static final String TEST_STUDENT_NO = "TEST-HTTP-SCORE-STU999";

    // 测试课程编号；固定值方便测试前后清理遗留数据。
    private static final String TEST_COURSE_NO = "TEST-HTTP-SCORE-COURSE999";

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
        System.out.println("===== ScoreHandler HTTP 测试开始 =====");

        // 准备真实 DAO，用于测试前后的数据清理。
        StudentDaoImpl studentDao = new StudentDaoImpl();
        CourseDaoImpl courseDao = new CourseDaoImpl();
        ScoreDaoImpl scoreDao = new ScoreDaoImpl();

        HttpServer server = null;

        try {
            // 启动临时 HttpServer，绑定 127.0.0.1 的随机端口。
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

            // 把 ScoreHandler 注册到 /api/scores 路径。
            server.createContext("/api/scores", new ScoreHandler());

            // 测试程序是单线程顺序执行，不需要线程池。
            server.setExecutor(null);

            // 启动服务器。
            server.start();

            // 读取系统分配的随机端口，构造基础请求地址。
            baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/api/scores";

            // 输出测试服务器地址。
            System.out.println("测试服务器已启动：" + baseUrl);

            // 先清理可能遗留的测试成绩、课程和学生。
            cleanupTestData(studentDao, courseDao, scoreDao);

            // 创建测试学生和测试课程，因为成绩接口依赖这两个外键。
            Long studentId = insertTestStudent(studentDao);
            Long courseId = insertTestCourse(courseDao);

            // 输出依赖数据创建结果。
            System.out.println("【0】测试学生和课程创建成功：" + studentId + " / " + courseId);

            // 测试非法分数请求。
            testCreateInvalidScore(studentId, courseId);

            // 测试正常录入成绩接口。
            Long scoreId = testCreate(studentId, courseId);

            // 测试成绩详情接口。
            testDetail(scoreId);

            // 测试成绩明细分页接口。
            testList(courseId);

            // 测试修改成绩接口。
            testUpdate(scoreId);

            // 测试成绩统计接口。
            testStatistics(courseId);

            // 测试删除成绩接口。
            testDelete(scoreId);

            // 测试删除后的 404 详情查询。
            testDetailNotFound(scoreId);

            // 停止临时服务器，释放端口。
            server.stop(0);

            // 输出整体成功提示。
            System.out.println("===== ScoreHandler HTTP 测试全部成功 =====");
        } catch (Exception exception) {
            // 打印完整异常堆栈，便于定位问题。
            exception.printStackTrace();

            // 测试失败时兜底清理测试数据。
            cleanupTestData(studentDao, courseDao, scoreDao);

            // 确保临时服务器关闭。
            if (server != null) {
                server.stop(0);
            }

            // 让进程以失败状态退出。
            System.exit(1);
        } finally {
            // 正常执行结束后清理测试数据。
            cleanupTestData(studentDao, courseDao, scoreDao);
        }
    }

    /**
     * 创建测试学生，并返回新生成的学生主键 ID。
     *
     * @param studentDao 学生 DAO 对象
     * @return 测试学生主键 ID
     * @throws Exception 数据库访问异常
     */
    private static Long insertTestStudent(StudentDaoImpl studentDao) throws Exception {
        // 创建测试学生对象。
        Student student = new Student();

        // 设置固定测试学号。
        student.setStudentNo(TEST_STUDENT_NO);

        // 设置测试姓名。
        student.setName("HTTP成绩测试学生");

        // 设置性别：1-男。
        student.setGender(1);

        // 设置出生日期。
        student.setBirthDate(LocalDate.of(2004, 5, 20));

        // 设置联系电话。
        student.setPhone("13800009111");

        // 设置电子邮箱。
        student.setEmail("http-score-test@campus.edu.cn");

        // 设置所属专业。
        student.setMajor("计算机科学与技术");

        // 设置班级名称。
        student.setClassName("计科2401班");

        // 设置学籍状态：1-在读。
        student.setStatus(1);

        // 插入测试学生。
        return studentDao.insert(student);
    }

    /**
     * 创建测试课程，并返回新生成的课程主键 ID。
     *
     * @param courseDao 课程 DAO 对象
     * @return 测试课程主键 ID
     * @throws Exception 数据库访问异常
     */
    private static Long insertTestCourse(CourseDaoImpl courseDao) throws Exception {
        // 创建测试课程对象。
        Course course = new Course();

        // 设置固定测试课程编号。
        course.setCourseNo(TEST_COURSE_NO);

        // 设置课程名称。
        course.setName("HTTP成绩测试课程");

        // 设置课程学分。
        course.setCredit(new BigDecimal("3.0"));

        // 设置任课教师。
        course.setTeacher("HTTP测试老师");

        // 插入测试课程。
        return courseDao.insert(course);
    }

    /**
     * 测试 POST /api/scores 录入非法分数。
     *
     * @param studentId 测试学生主键 ID
     * @param courseId  测试课程主键 ID
     * @throws Exception HTTP 请求失败时抛出
     */
    private static void testCreateInvalidScore(Long studentId, Long courseId) throws Exception {
        // 构造非法成绩 JSON。
        String requestBody = """
                {
                  "studentId": %d,
                  "courseId": %d,
                  "score": 150,
                  "examDate": "2026-06-20",
                  "remarks": "HTTP非法分数"
                }
                """.formatted(studentId, courseId);

        // 构造 POST 请求。
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        // 发送请求并读取响应。
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // 校验 HTTP 状态码为 400。
        expectStatus(response, 400, "录入非法分数");

        // 解析统一响应结构。
        CommonResult<Object> result = parseResult(response);

        // 校验业务 code 为 400。
        expectCode(result, 400, "录入非法分数");

        // 输出非法分数测试成功提示。
        System.out.println("【1】POST /api/scores 非法分数拦截成功");
    }

    /**
     * 测试 POST /api/scores 正常录入成绩。
     *
     * @param studentId 测试学生主键 ID
     * @param courseId  测试课程主键 ID
     * @return 新增成绩的主键 ID
     * @throws Exception HTTP 请求或响应解析失败时抛出
     */
    private static Long testCreate(Long studentId, Long courseId) throws Exception {
        // 构造正常成绩 JSON。
        String requestBody = """
                {
                  "studentId": %d,
                  "courseId": %d,
                  "score": 85.50,
                  "examDate": "2026-06-20",
                  "remarks": "HTTP测试成绩"
                }
                """.formatted(studentId, courseId);

        // 构造 POST 请求。
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        // 发送请求并读取响应。
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // 校验 HTTP 状态码为 200。
        expectStatus(response, 200, "录入成绩");

        // 解析统一响应结构。
        CommonResult<Object> result = parseResult(response);

        // 校验业务 code 为 200。
        expectCode(result, 200, "录入成绩");

        // 从 data 中读取新增 ID。
        Long scoreId = readId(result.getData(), "录入成绩响应");

        // 输出成功提示。
        System.out.println("【2】POST /api/scores 录入成功，ID：" + scoreId);

        // 返回新增成绩 ID。
        return scoreId;
    }

    /**
     * 测试 GET /api/scores/{id} 成绩详情接口。
     *
     * @param scoreId 成绩 ID
     * @throws Exception HTTP 请求或响应解析失败时抛出
     */
    private static void testDetail(Long scoreId) throws Exception {
        // 构造详情请求地址。
        URI uri = URI.create(baseUrl + "/" + scoreId);

        // 构造 GET 请求。
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();

        // 发送请求并读取响应。
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // 校验 HTTP 状态码为 200。
        expectStatus(response, 200, "查询成绩详情");

        // 解析统一响应结构。
        CommonResult<Object> result = parseResult(response);

        // 校验业务 code 为 200。
        expectCode(result, 200, "查询成绩详情");

        // 校验响应体包含成绩 ID。
        if (!result.getData().toString().contains(String.valueOf(scoreId))) {
            // 如果没有成绩 ID，说明查询到了错误记录。
            throw new IllegalStateException("成绩详情未返回指定成绩：" + result.getData());
        }

        // 输出详情查询成功提示。
        System.out.println("【3】GET /api/scores/" + scoreId + " 详情查询成功");
    }

    /**
     * 测试 GET /api/scores 明细分页接口。
     *
     * @param courseId 测试课程主键 ID
     * @throws Exception HTTP 请求或响应解析失败时抛出
     */
    private static void testList(Long courseId) throws Exception {
        // 构造明细分页查询 URL。
        String url = baseUrl + "?page=1&pageSize=10&courseId=" + courseId;

        // 构造 GET 请求。
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();

        // 发送请求并读取响应。
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // 校验 HTTP 状态码为 200。
        expectStatus(response, 200, "查询成绩明细");

        // 解析统一响应结构。
        CommonResult<Object> result = parseResult(response);

        // 校验业务 code 为 200。
        expectCode(result, 200, "查询成绩明细");

        // 校验 data 是分页对象。
        Object data = result.getData();
        if (!(data instanceof Map)) {
            // 如果 data 不是对象，说明响应结构不符合约定。
            throw new IllegalStateException("成绩明细分页响应 data 应该是对象，实际值：" + data);
        }

        // 将 data 转成 Map。
        @SuppressWarnings("unchecked")
        Map<String, Object> page = (Map<String, Object>) data;

        // 读取分页 list。
        Object listValue = page.get("list");

        // list 必须存在且是集合。
        if (!(listValue instanceof List)) {
            // 如果没有 list，说明响应结构不符合约定。
            throw new IllegalStateException("成绩明细分页响应缺少 list 字段");
        }

        // 将 list 转成集合。
        List<?> details = (List<?>) listValue;

        // 校验至少能查到刚录入的成绩。
        if (details.isEmpty()) {
            // 如果没有查到，说明明细查询或条件筛选有问题。
            throw new IllegalStateException("成绩明细查询没有找到测试课程成绩：" + courseId);
        }

        // 输出明细分页查询成功提示。
        System.out.println("【4】GET /api/scores 明细分页查询成功，条数：" + details.size());
    }

    /**
     * 测试 PUT /api/scores/{id} 修改成绩接口。
     *
     * @param scoreId 成绩 ID
     * @throws Exception HTTP 请求或响应解析失败时抛出
     */
    private static void testUpdate(Long scoreId) throws Exception {
        // 构造修改成绩 JSON。
        String requestBody = """
                {
                  "score": 92.50,
                  "examDate": "2026-06-22",
                  "remarks": "HTTP测试成绩-已更新"
                }
                """;

        // 构造 PUT 请求。
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/" + scoreId))
                .header("Content-Type", "application/json; charset=UTF-8")
                .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        // 发送请求并读取响应。
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // 校验 HTTP 状态码为 200。
        expectStatus(response, 200, "修改成绩");

        // 解析统一响应结构。
        CommonResult<Object> result = parseResult(response);

        // 校验业务 code 为 200。
        expectCode(result, 200, "修改成绩");

        // 输出修改成功提示。
        System.out.println("【5】PUT /api/scores/" + scoreId + " 修改成功");
    }

    /**
     * 测试 GET /api/scores/statistics 统计接口。
     *
     * @param courseId 测试课程主键 ID
     * @throws Exception HTTP 请求或响应解析失败时抛出
     */
    private static void testStatistics(Long courseId) throws Exception {
        // 构造统计查询 URL。
        String url = baseUrl + "/statistics?courseId=" + courseId;

        // 构造 GET 请求。
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();

        // 发送请求并读取响应。
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // 校验 HTTP 状态码为 200。
        expectStatus(response, 200, "查询成绩统计");

        // 解析统一响应结构。
        CommonResult<Object> result = parseResult(response);

        // 校验业务 code 为 200。
        expectCode(result, 200, "查询成绩统计");

        // 校验 data 包含课程 ID。
        if (!result.getData().toString().contains(String.valueOf(courseId))) {
            // 如果没有课程 ID，说明统计到了错误课程。
            throw new IllegalStateException("成绩统计未返回指定课程：" + result.getData());
        }

        // 输出统计查询成功提示。
        System.out.println("【6】GET /api/scores/statistics 统计成功");
    }

    /**
     * 测试 DELETE /api/scores/{id} 删除成绩接口。
     *
     * @param scoreId 成绩 ID
     * @throws Exception HTTP 请求或响应解析失败时抛出
     */
    private static void testDelete(Long scoreId) throws Exception {
        // 构造 DELETE 请求。
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/" + scoreId))
                .DELETE()
                .build();

        // 发送请求并读取响应。
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // 校验 HTTP 状态码为 200。
        expectStatus(response, 200, "删除成绩");

        // 解析统一响应结构。
        CommonResult<Object> result = parseResult(response);

        // 校验业务 code 为 200。
        expectCode(result, 200, "删除成绩");

        // 输出删除成功提示。
        System.out.println("【7】DELETE /api/scores/" + scoreId + " 删除成功");
    }

    /**
     * 测试删除后再查询成绩详情返回 404。
     *
     * @param scoreId 已删除的成绩 ID
     * @throws Exception HTTP 请求失败时抛出
     */
    private static void testDetailNotFound(Long scoreId) throws Exception {
        // 构造详情请求地址。
        URI uri = URI.create(baseUrl + "/" + scoreId);

        // 构造 GET 请求。
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();

        // 发送请求并读取响应。
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // 校验 HTTP 状态码为 404。
        expectStatus(response, 404, "删除后查询成绩详情");

        // 解析统一响应结构。
        CommonResult<Object> result = parseResult(response);

        // 校验业务 code 为 404。
        expectCode(result, 404, "删除后查询成绩详情");

        // 输出 404 测试成功提示。
        System.out.println("【8】删除后成绩详情查询返回 404 成功");
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
     * 清理测试学生、课程和成绩数据。
     * <p>注意：必须先删除成绩，再删除课程和学生，否则外键约束可能导致清理失败。</p>
     *
     * @param studentDao 学生 DAO 对象
     * @param courseDao  课程 DAO 对象
     * @param scoreDao   成绩 DAO 对象
     */
    private static void cleanupTestData(
            StudentDaoImpl studentDao,
            CourseDaoImpl courseDao,
            ScoreDaoImpl scoreDao
    ) {
        try {
            // 根据固定测试学号查找学生。
            Student existingStudent = studentDao.findByStudentNo(TEST_STUDENT_NO);

            // 根据固定测试课程编号查找课程。
            Course existingCourse = courseDao.findByCourseNo(TEST_COURSE_NO);

            // 如果学生和课程都存在，先删除它们之间的成绩记录。
            if (existingStudent != null && existingCourse != null) {
                // 查询该学生该课程下的成绩。
                Score existingScore = scoreDao.findByStudentAndCourse(existingStudent.getId(), existingCourse.getId());

                // 如果成绩存在，先删除成绩。
                if (existingScore != null) {
                    // 删除成绩。
                    scoreDao.deleteById(existingScore.getId());

                    // 输出清理提示。
                    System.out.println("已清理遗留测试成绩：" + existingScore.getId());
                }
            }

            // 如果课程存在，删除课程。
            if (existingCourse != null) {
                // 删除测试课程。
                courseDao.deleteById(existingCourse.getId());

                // 输出清理提示。
                System.out.println("已清理遗留测试课程：" + TEST_COURSE_NO);
            }

            // 如果学生存在，删除学生。
            if (existingStudent != null) {
                // 删除测试学生。
                studentDao.deleteById(existingStudent.getId());

                // 输出清理提示。
                System.out.println("已清理遗留测试学生：" + TEST_STUDENT_NO);
            }
        } catch (Exception exception) {
            // 清理失败时提示，避免影响主流程排查。
            System.err.println("清理测试数据失败：" + exception.getMessage());
        }
    }
}
