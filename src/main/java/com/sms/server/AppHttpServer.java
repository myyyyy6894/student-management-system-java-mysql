package com.sms.server;

// 导入 JDK 原生 HttpServer，用于创建本项目的轻量 HTTP 服务。
import com.sun.net.httpserver.HttpServer;
// 导入学生处理器。
import com.sms.controller.StudentHandler;
// 导入课程处理器。
import com.sms.controller.CourseHandler;
// 导入成绩处理器。
import com.sms.controller.ScoreHandler;
// 导入静态资源处理器。
import com.sms.controller.StaticFileHandler;
// 导入 IO 异常，端口绑定失败时抛出。
import java.io.IOException;
// 导入网络地址对象，用于指定监听地址和端口。
import java.net.InetSocketAddress;
// 导入线程池执行器。
import java.util.concurrent.ExecutorService;
// 导入固定线程池工厂。
import java.util.concurrent.Executors;

/**
 * 应用 HTTP 服务统一启动类。
 * <p>负责创建 HttpServer、配置线程池、注册 API 路由和启动/停止服务。</p>
 */
public class AppHttpServer {

    // 默认监听端口；规范文档要求本机服务运行在 8080 端口。
    public static final int DEFAULT_PORT = 8080;

    // 当前服务器监听端口。
    private final int port;

    // JDK 原生 HttpServer 对象，服务启动后赋值。
    private HttpServer server;

    // HTTP 请求处理线程池，服务启动后赋值。
    private ExecutorService executor;

    /**
     * 使用默认端口 8080 创建服务器对象。
     */
    public AppHttpServer() {
        // 复用默认端口构造器，减少重复代码。
        this(DEFAULT_PORT);
    }

    /**
     * 使用指定端口创建服务器对象。
     *
     * @param port 监听端口
     */
    public AppHttpServer(int port) {
        // 端口必须在合法范围内；0 表示系统随机分配端口，方便测试。
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("端口必须在 0~65535 之间");
        }

        // 保存监听端口。
        this.port = port;
    }

    /**
     * 启动 HTTP 服务。
     *
     * @throws IOException 端口被占用或网络绑定失败时抛出
     */
    public synchronized void start() throws IOException {
        // 防止重复启动同一个 AppHttpServer 对象。
        if (server != null) {
            return;
        }

        // 创建 HttpServer；backlog=0 表示使用系统默认排队长度。
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // 创建固定大小线程池；规范文档要求使用 10 个线程处理并发请求。
        executor = Executors.newFixedThreadPool(10);

        // 注册学生模块路由；StudentHandler 内部会同时处理列表、详情、新增、修改和删除。
        server.createContext("/api/students", new StudentHandler());

        // 注册课程模块路由；CourseHandler 内部会处理课程列表、详情、新增、修改和删除。
        server.createContext("/api/courses", new CourseHandler());

        // 注册成绩模块路由；ScoreHandler 内部会处理成绩录入、修改、删除、明细分页和统计接口。
        server.createContext("/api/scores", new ScoreHandler());

        // 注册静态资源路由；HttpServer 会优先匹配更长的 /api/* 路径。
        server.createContext("/", new StaticFileHandler());

        // 给 HttpServer 指定请求处理线程池。
        server.setExecutor(executor);

        // 启动服务器。
        server.start();

        // 打印启动成功信息，方便开发阶段确认服务已经可用。
        System.out.println("学生管理系统 HTTP 服务已启动: http://localhost:" + port + "/");
        System.out.println("学生接口: /api/students");
        System.out.println("课程接口: /api/courses");
        System.out.println("成绩接口: /api/scores");
        System.out.println("前端入口: http://localhost:" + port + "/");
    }

    /**
     * 停止 HTTP 服务。
     */
    public synchronized void stop() {
        // 如果服务尚未启动，则无需停止。
        if (server != null) {
            // stop(0) 表示立刻停止，不再等待当前请求处理完成。
            server.stop(0);

            // 清空 server 引用，表示当前对象已停止。
            server = null;
        }

        // 关闭线程池并释放线程资源。
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }

        // 打印停止信息。
        System.out.println("学生管理系统 HTTP 服务已停止");
    }

    /**
     * 获取配置的监听端口。
     *
     * @return 监听端口
     */
    public int getPort() {
        // 返回配置端口。
        return port;
    }
}
