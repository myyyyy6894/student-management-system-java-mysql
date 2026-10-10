package com.sms.controller;

// 导入 JDK 内置 HttpServer 与 HttpClient。
import com.sun.net.httpserver.HttpServer;
// 导入被装配的真实 Service 实现。
import com.sms.service.impl.CourseServiceImpl;
import com.sms.service.impl.ScoreServiceImpl;
import com.sms.service.impl.StudentServiceImpl;
// 导入内存版 DAO。
import com.sms.support.InMemoryDaos;
// 导入实体。
import com.sms.model.entity.Student;
// 导入 JUnit 5。
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Handler 路由与状态码的端到端测试。
 * <p>测试会启动一个随机端口的临时 HttpServer，并用内存 DAO 装配真实 Service，
 * 因此不需要 MySQL 即可验证以下关键行为：</p>
 * <ul>
 *   <li>非法路径 ID 返回 400，而不是静默返回列表；</li>
 *   <li>非法日期返回 400，而不是 500；</li>
 *   <li>唯一性冲突返回 409；</li>
 *   <li>资源不存在返回 404；</li>
 *   <li>静态资源支持 HEAD 请求。</li>
 * </ul>
 */
class HandlerRoutingTest {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private static HttpServer server;
    private static String baseUrl;

    private static InMemoryDaos.InMemoryStudentDao studentDao;
    private static InMemoryDaos.InMemoryCourseDao courseDao;
    private static InMemoryDaos.InMemoryScoreDao scoreDao;

    @BeforeAll
    static void startServer() throws Exception {
        studentDao = new InMemoryDaos.InMemoryStudentDao();
        courseDao = new InMemoryDaos.InMemoryCourseDao();
        scoreDao = new InMemoryDaos.InMemoryScoreDao();
        scoreDao.studentDao = studentDao;
        scoreDao.courseDao = courseDao;

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/students", new StudentHandler(new StudentServiceImpl(studentDao, scoreDao)));
        server.createContext("/api/courses", new CourseHandler(new CourseServiceImpl(courseDao, scoreDao)));
        server.createContext("/api/scores", new ScoreHandler(new ScoreServiceImpl(studentDao, courseDao, scoreDao)));
        server.createContext("/", new StaticFileHandler());
        server.setExecutor(null);
        server.start();

        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @BeforeEach
    void clearData() {
        studentDao.table.clear();
        studentDao.sequence.set(0);
        courseDao.table.clear();
        courseDao.sequence.set(0);
        scoreDao.table.clear();
        scoreDao.sequence.set(0);
    }

    @Test
    @DisplayName("新增学生成功后返回新主键 ID")
    void createStudent_returnsId() throws Exception {
        HttpResponse<String> response = post("/api/students", studentJson("20240001"));

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"id\":1"), response.body());
    }

    @Test
    @DisplayName("出生日期非法时返回 400，而不是 500")
    void createStudent_invalidBirthDate_returns400() throws Exception {
        String body = """
                {"studentNo":"20240001","name":"张三","major":"计算机科学与技术",
                 "className":"计科2401班","birthDate":"2024-13-45"}
                """;

        HttpResponse<String> response = post("/api/students", body);

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("\"code\":400"), response.body());
    }

    @Test
    @DisplayName("学号重复时返回 409 CONFLICT")
    void createStudent_duplicate_returns409() throws Exception {
        assertEquals(200, post("/api/students", studentJson("20240001")).statusCode());

        HttpResponse<String> response = post("/api/students", studentJson("20240001"));

        assertEquals(409, response.statusCode());
        assertTrue(response.body().contains("学号已存在"), response.body());
    }

    @Test
    @DisplayName("路径 ID 不是数字时返回 400，而不是把列表返回给调用方")
    void getStudent_nonNumericId_returns400() throws Exception {
        // 先放入一条数据，如果实现有缺陷就会把它当作列表查询返回。
        post("/api/students", studentJson("20240001"));

        HttpResponse<String> response = get("/api/students/abc");

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("\"code\":400"), response.body());
    }

    @Test
    @DisplayName("路径多出一段时返回 400")
    void getStudent_extraSegment_returns400() throws Exception {
        HttpResponse<String> response = get("/api/students/1/2");

        assertEquals(400, response.statusCode());
    }

    @Test
    @DisplayName("查询不存在的学生返回 404")
    void getStudent_notFound_returns404() throws Exception {
        HttpResponse<String> response = get("/api/students/999");

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("\"code\":404"), response.body());
    }

    @Test
    @DisplayName("不支持的请求方法返回 405")
    void unsupportedMethod_returns405() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/students"))
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();

        assertEquals(405, HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    @Test
    @DisplayName("不存在的课程统计返回 404")
    void statistics_unknownCourse_returns404() throws Exception {
        HttpResponse<String> response = get("/api/scores/statistics?courseId=999");

        assertEquals(404, response.statusCode());
    }

    @Test
    @DisplayName("静态资源支持 HEAD 请求")
    void staticResource_supportsHead() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/"))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("text/html"),
                "HEAD 响应应带有 Content-Type");
    }

    @Test
    @DisplayName("静态资源对非 GET/HEAD 方法返回 405")
    void staticResource_rejectsPost() throws Exception {
        HttpResponse<String> response = post("/", "{}");

        assertEquals(405, response.statusCode());
    }

    @Test
    @DisplayName("请求分页参数超限时被截断，返回的 pageSize 不超过上限")
    void listStudents_clampsPageSize() throws Exception {
        post("/api/students", studentJson("20240001"));

        HttpResponse<String> response = get("/api/students?page=1&pageSize=100000");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"pageSize\":100"), response.body());
    }

    private String studentJson(String studentNo) {
        Student student = new Student();
        student.setStudentNo(studentNo);
        student.setName("张三");
        student.setGender(1);
        student.setMajor("计算机科学与技术");
        student.setClassName("计科2401班");
        student.setStatus(1);
        student.setPhone("13800009999");
        student.setEmail("zhangsan@campus.edu.cn");
        return com.sms.util.JsonUtil.toJson(student);
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build();
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
