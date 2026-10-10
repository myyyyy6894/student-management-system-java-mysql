package com.sms.controller;

// 导入 JDK 原生 HttpHandler，StaticFileHandler 直接实现它。
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
// 导入统一响应对象和 JSON 工具，用于返回格式化的 404/405 响应。
import com.sms.model.vo.CommonResult;
import com.sms.util.JsonUtil;
// 导入 IO 和字符集工具。
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
// 导入 Map，用于根据文件后缀选择 Content-Type。
import java.util.HashMap;
import java.util.Map;

/**
 * 静态资源处理器。
 * <p>负责从 classpath:static 目录读取 HTML / CSS / JS / 图片等前端静态文件。</p>
 * <p>例如请求 / 会返回 static/index.html，请求 /css/app.css 会返回 static/css/app.css。</p>
 */
public class StaticFileHandler implements HttpHandler {

    // 静态资源统一放在 classpath 根目录下的 static 目录。
    private static final String STATIC_RESOURCE_DIR = "static/";

    // 默认首页文件；访问 / 时会返回这个文件。
    private static final String INDEX_PAGE = "index.html";

    // 保存常见文件后缀对应的 Content-Type。
    private static final Map<String, String> CONTENT_TYPES = new HashMap<>();

    // 静态初始化块只执行一次，用于填充文件后缀映射表。
    static {
        // HTML 页面必须显式指定 UTF-8，避免中文乱码。
        CONTENT_TYPES.put("html", "text/html; charset=UTF-8");
        CONTENT_TYPES.put("htm", "text/html; charset=UTF-8");

        // CSS 样式文件。
        CONTENT_TYPES.put("css", "text/css; charset=UTF-8");

        // JavaScript 文件。
        CONTENT_TYPES.put("js", "application/javascript; charset=UTF-8");

        // JSON 文件。
        CONTENT_TYPES.put("json", "application/json; charset=UTF-8");

        // 常见文本文件。
        CONTENT_TYPES.put("txt", "text/plain; charset=UTF-8");
        CONTENT_TYPES.put("md", "text/plain; charset=UTF-8");

        // 常见图片文件。
        CONTENT_TYPES.put("svg", "image/svg+xml");
        CONTENT_TYPES.put("png", "image/png");
        CONTENT_TYPES.put("jpg", "image/jpeg");
        CONTENT_TYPES.put("jpeg", "image/jpeg");
        CONTENT_TYPES.put("gif", "image/gif");
        CONTENT_TYPES.put("webp", "image/webp");
        CONTENT_TYPES.put("ico", "image/x-icon");

        // 字体文件，前端如果引入本地字体时可用。
        CONTENT_TYPES.put("woff", "font/woff");
        CONTENT_TYPES.put("woff2", "font/woff2");
    }

    /**
     * 处理静态资源请求。
     *
     * @param exchange 当前 HTTP 请求对象
     * @throws IOException 读取资源或写出响应失败时抛出
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 读取请求方法并统一转为大写。
        String method = exchange.getRequestMethod().toUpperCase();

        // 静态资源支持 GET 与 HEAD；HEAD 只返回响应头，用于浏览器预检、健康检查和爬虫。
        boolean headOnly = "HEAD".equals(method);

        // 其余方法（POST/PUT/DELETE 等）一律拒绝。
        if (!"GET".equals(method) && !headOnly) {
            // 返回 405，提示当前接口只允许 GET / HEAD。
            sendJson(exchange, 405, CommonResult.error(405, "静态资源只支持 GET / HEAD 请求"), false);
            return;
        }

        // 获取请求路径；例如 /、/index.html、/css/app.css。
        String requestPath = exchange.getRequestURI().getPath();

        // 如果请求根路径，则返回默认首页。
        if (requestPath == null || requestPath.isBlank() || "/".equals(requestPath)) {
            serveResource(exchange, INDEX_PAGE, headOnly);
            return;
        }

        // 去掉开头的 /，得到 static 目录下的相对路径。
        String relativePath = requestPath.substring(1);

        // 如果路径为空，也回落到首页。
        if (relativePath.isBlank()) {
            serveResource(exchange, INDEX_PAGE, headOnly);
            return;
        }

        // 阻止相对路径穿越攻击，例如 /../pom.xml。
        // 不能通过 URL 读取 static 目录以外的项目文件。
        if (relativePath.contains("..") || relativePath.contains("\\")) {
            sendJson(exchange, 403, CommonResult.error(403, "非法资源路径"), headOnly);
            return;
        }

        // 尝试按请求路径读取静态资源。
        serveResource(exchange, relativePath, headOnly);
    }

    /**
     * 从 classpath:static 目录读取指定文件并返回。
     *
     * @param exchange     当前 HTTP 请求对象
     * @param relativePath static 目录下的相对路径
     * @param headOnly     是否为 HEAD 请求（只回响应头，不写响应体）
     * @throws IOException 响应写出失败时抛出
     */
    private void serveResource(HttpExchange exchange, String relativePath, boolean headOnly) throws IOException {
        // 如果请求的是目录形式，例如 /css/，尝试读取该目录下的 index.html。
        if (relativePath.endsWith("/")) {
            relativePath = relativePath + INDEX_PAGE;
        }

        // 通过类加载器读取静态资源。
        // Maven 编译后 src/main/resources 下的文件会进入 classpath 根路径。
        InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(STATIC_RESOURCE_DIR + relativePath);

        // 如果资源不存在，返回 404。
        if (inputStream == null) {
            sendJson(exchange, 404, CommonResult.error(404, "静态资源不存在"), headOnly);
            return;
        }

        // try-with-resources 会自动关闭资源输入流。
        try (inputStream) {
            // 将静态资源读入字节数组；本地课程项目文件通常不大，直接读入内存足够简单可靠。
            byte[] bytes = inputStream.readAllBytes();

            // 根据文件后缀设置 Content-Type。
            exchange.getResponseHeaders().set("Content-Type", resolveContentType(relativePath));

            // 开发阶段禁用缓存，前端修改 HTML/CSS/JS 后刷新浏览器即可生效。
            exchange.getResponseHeaders().set("Cache-Control", "no-store");

            // 声明响应体长度（HEAD 请求也声明长度，只是不写响应体）。
            exchange.sendResponseHeaders(200, headOnly ? -1 : bytes.length);

            // HEAD 请求到此结束，不写响应体。
            if (headOnly) {
                return;
            }

            // try-with-resources 自动关闭响应输出流。
            try (OutputStream outputStream = exchange.getResponseBody()) {
                // 写出静态文件内容。
                outputStream.write(bytes);

                // 立即刷新。
                outputStream.flush();
            }
        }
    }

    /**
     * 根据文件后缀解析 Content-Type。
     *
     * @param relativePath 静态资源相对路径
     * @return HTTP Content-Type
     */
    private String resolveContentType(String relativePath) {
        // 找到最后一个点的位置。
        int dotIndex = relativePath.lastIndexOf('.');

        // 没有后缀时返回通用二进制类型。
        if (dotIndex < 0 || dotIndex == relativePath.length() - 1) {
            return "application/octet-stream";
        }

        // 提取后缀并统一转小写。
        String extension = relativePath.substring(dotIndex + 1).toLowerCase();

        // 从映射表中获取 Content-Type；未知类型按二进制流处理。
        return CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");
    }

    /**
     * 输出统一 JSON 错误响应。
     *
     * @param exchange 当前 HTTP 请求对象
     * @param status   HTTP 状态码
     * @param result   统一响应对象
     * @param headOnly 是否为 HEAD 请求（只回响应头，不写响应体）
     * @throws IOException 响应写出失败时抛出
     */
    private void sendJson(HttpExchange exchange, int status, CommonResult<Object> result, boolean headOnly) throws IOException {
        // 设置 JSON 响应类型。
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

        // 将统一响应对象转换成 JSON。
        byte[] bytes = JsonUtil.toJson(result).getBytes(StandardCharsets.UTF_8);

        // 声明响应体长度（HEAD 请求不写响应体）。
        exchange.sendResponseHeaders(status, headOnly ? -1 : bytes.length);

        // HEAD 请求到此结束。
        if (headOnly) {
            return;
        }

        // 写出 JSON 内容。
        try (OutputStream outputStream = exchange.getResponseBody()) {
            // 写出字节数组。
            outputStream.write(bytes);

            // 刷新响应流。
            outputStream.flush();
        }
    }
}
