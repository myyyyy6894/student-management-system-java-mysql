package com.sms.controller;

// 导入 JDK 内置 HttpServer 的请求交换对象，封装一次 HTTP 请求和响应。
import com.sun.net.httpserver.HttpExchange;
// 导入 HTTP 处理器接口，BaseHttpHandler 的子类最终都会被注册到 HttpServer。
import com.sun.net.httpserver.HttpHandler;
// 导入统一响应包装类，用于生成 code/message/data 格式的 JSON。
import com.sms.model.vo.CommonResult;
// 导入 JSON 工具类，避免手工拼接 JSON 字符串。
import com.sms.util.JsonUtil;
// 导入 IO 异常，读写请求体和响应体时可能抛出。
import java.io.IOException;
// 导入输出流，用于向客户端回写响应数据。
import java.io.OutputStream;
// 导入字符集类，统一使用 UTF-8 处理请求和响应。
import java.nio.charset.StandardCharsets;
// 导入 List 和 ArrayList，用于拆分 Query 字符串。
import java.util.ArrayList;
import java.util.List;
// 导入 Map 和 LinkedHashMap，保存解析后的 Query 参数并保持原始顺序。
import java.util.LinkedHashMap;
import java.util.Map;
// 导入 URLDecoder，用于解码 URL 中的特殊字符。
import java.net.URLDecoder;

/**
 * 通用 HTTP 处理器基类。
 * <p>负责所有 Controller 都需要的通用能力：CORS、OPTIONS 预检、异常统一返回、请求体读取、Query 解析、路径 ID 提取和 JSON 输出。</p>
 * <p>具体业务路由由子类实现 {@link #processRequest(HttpExchange)}。</p>
 */
public abstract class BaseHttpHandler implements HttpHandler {

    /**
     * HTTP 请求统一入口。
     * <p>这是模板方法：先处理所有请求都需要的通用逻辑，再把具体分发交给子类。</p>
     *
     * @param exchange 当前 HTTP 请求对象
     * @throws IOException 当响应写出失败时抛出
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 设置允许跨域的来源；本地开发阶段使用 * 最简单。
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

        // 设置允许跨域使用的 HTTP 方法，覆盖本项目全部 RESTful 操作。
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");

        // 设置允许跨域携带的请求头，尤其是前端 POST/PUT 常用的 Content-Type。
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        // 浏览器跨域复杂请求前会先发送 OPTIONS 预检请求，这里直接响应成功即可。
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            // 204 表示无内容；第二个参数 -1 明确告诉 HttpServer 该响应没有响应体。
            exchange.sendResponseHeaders(204, -1);

            // 预检请求处理完成，不需要继续进入业务路由。
            return;
        }

        // 业务处理可能抛出参数异常或其他运行时异常，这里统一捕获并转换成 JSON。
        try {
            // 交给子类完成具体路由分发和业务调用。
            processRequest(exchange);
        } catch (IllegalArgumentException exception) {
            // IllegalArgumentException 通常表示请求参数错误或业务校验失败。
            // 统一映射为 HTTP 400，并保留异常提示信息。
            CommonResult<Object> result = CommonResult.error(400, safeMessage(exception, "请求参数错误"));
            sendJsonResponse(exchange, 400, JsonUtil.toJson(result));
        } catch (Throwable throwable) {
            // 这里必须捕获 Throwable，而不能只捕获 Exception。
            // 例如 DBUtil 静态初始化失败时会抛出 ExceptionInInitializerError，它属于 Error。
            // 如果只捕获 Exception，这类异常会导致响应头未发送，浏览器就可能看到 stream closed。
            throwable.printStackTrace();
            CommonResult<Object> result = CommonResult.error(500, safeMessage(throwable, "服务器内部错误"));
            sendJsonResponse(exchange, 500, JsonUtil.toJson(result));
        }
    }

    /**
     * 子类必须实现的路由处理方法。
     * <p>例如 StudentHandler 会在这里分发 GET / POST / PUT / DELETE 请求。</p>
     *
     * @param exchange 当前 HTTP 请求对象
     * @throws Exception 子类处理过程中可能抛出的异常，由基类统一捕获
     */
    protected abstract void processRequest(HttpExchange exchange) throws Exception;

    /**
     * 读取 HTTP 请求体，并转换为 UTF-8 字符串。
     * <p>POST/PUT 请求通常会携带 JSON 格式的请求体。</p>
     *
     * @param exchange 当前 HTTP 请求对象
     * @return 请求体字符串；没有请求体时返回空字符串
     * @throws IOException 读取请求体失败时抛出
     */
    protected String readRequestBody(HttpExchange exchange) throws IOException {
        // 获取请求体输入流。
        try (java.io.InputStream inputStream = exchange.getRequestBody()) {
            // readAllBytes 会阻塞直到读到 EOF，也就是客户端请求体发送完毕。
            byte[] bytes = inputStream.readAllBytes();

            // HTTP 请求体本质是字节流，必须按 UTF-8 转成字符串，避免中文乱码。
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    /**
     * 解析 URL Query 参数。
     * <p>例如 /api/students?page=1&pageSize=10&keyword=张 会解析成 Map。</p>
     *
     * @param exchange 当前 HTTP 请求对象
     * @return Query 参数集合；没有 Query 参数时返回空 Map
     */
    protected Map<String, String> parseQueryParams(HttpExchange exchange) {
        // 使用 LinkedHashMap 保留查询参数在 URL 中出现的顺序。
        Map<String, String> parameters = new LinkedHashMap<>();

        // getQuery() 只返回 ? 后面的部分，例如 page=1&pageSize=10。
        String query = exchange.getRequestURI().getQuery();

        // 如果 URL 没有 ? 或 ? 后面没有内容，直接返回空 Map。
        if (query == null || query.isBlank()) {
            return parameters;
        }

        // 多个 Query 参数用 & 分隔。
        String[] pairs = query.split("&");

        // 逐个处理 key=value 片段。
        for (String pair : pairs) {
            // 忽略连续 & 造成的空片段。
            if (pair == null || pair.isBlank()) {
                continue;
            }

            // limit=2 表示最多拆成 key 和 value 两部分，避免 value 中出现 = 被错误拆开。
            String[] keyValue = pair.split("=", 2);

            // URL 中的中文、空格、& 等字符通常经过了 URL 编码，这里必须解码。
            String key = urlDecode(keyValue[0]);

            // 如果只有 key 没有 =，value 视为空字符串。
            String value = keyValue.length > 1 ? urlDecode(keyValue[1]) : "";

            // 保存解析后的参数。
            parameters.put(key, value);
        }

        // 返回完整的 Query 参数集合。
        return parameters;
    }

    /**
     * 从请求路径末尾提取数字 ID。
     * <p>例如 basePath 为 /api/students，请求路径 /api/students/5 会返回 5。</p>
     *
     * @param exchange 当前 HTTP 请求对象
     * @param basePath 资源基础路径，例如 /api/students
     * @return 路径中的资源 ID；路径无效或 ID 不是数字时返回 null
     */
    protected Long extractPathId(HttpExchange exchange, String basePath) {
        // 基础路径不能为空。
        if (basePath == null || basePath.isBlank()) {
            return null;
        }

        // getRequestURI().getPath() 返回不包含 Query 的路径部分。
        String path = exchange.getRequestURI().getPath();

        // 路径为空或不以基础路径开头时，无法提取 ID。
        if (path == null || !path.startsWith(basePath)) {
            return null;
        }

        // 去掉基础路径，得到剩余部分，例如 /api/students/5 -> /5。
        String remainder = path.substring(basePath.length());

        // 去掉开头的 /，得到 5。
        if (remainder.startsWith("/")) {
            remainder = remainder.substring(1);
        }

        // 允许路径末尾带 /，例如 /api/students/5/。
        if (remainder.endsWith("/")) {
            remainder = remainder.substring(0, remainder.length() - 1);
        }

        // 剩余部分为空说明请求的是集合路径，而不是详情路径。
        if (remainder.isEmpty()) {
            return null;
        }

        // 本项目的详情路径只支持一个数字 ID，不支持 /api/students/5/other 这种形式。
        if (remainder.contains("/")) {
            return null;
        }

        try {
            // 将路径片段转换为 Long 类型的资源主键。
            return Long.parseLong(remainder);
        } catch (NumberFormatException exception) {
            // ID 不是数字时返回 null，让子类决定是返回 404 还是 400。
            return null;
        }
    }

    /**
     * 向客户端输出统一 JSON 响应。
     *
     * @param exchange   当前 HTTP 请求对象
     * @param httpStatus HTTP 状态码，例如 200、400、404、500
     * @param jsonString 已经序列化好的 JSON 字符串
     * @throws IOException 响应写出失败时抛出
     */
    protected void sendJsonResponse(HttpExchange exchange, int httpStatus, String jsonString) throws IOException {
        // 设置响应内容类型为 JSON，并显式指定 UTF-8。
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

        // JSON 字符串必须转成 UTF-8 字节数组后再写出，避免中文乱码。
        byte[] bytes = jsonString.getBytes(StandardCharsets.UTF_8);

        // sendResponseHeaders 的第二个参数是响应体长度；先声明长度，再写入实际字节。
        exchange.sendResponseHeaders(httpStatus, bytes.length);

        // try-with-resources 会自动关闭响应输出流，保证请求正常结束。
        try (OutputStream outputStream = exchange.getResponseBody()) {
            // 将 JSON 字节写入响应流。
            outputStream.write(bytes);

            // 立即刷新缓冲区，让客户端尽快收到响应。
            outputStream.flush();
        }
    }

    /**
     * 对 URL 片段执行解码。
     *
     * @param value 编码后的 URL 片段
     * @return 解码后的字符串；解码失败时返回原始值
     */
    private String urlDecode(String value) {
        try {
            // URLDecoder 默认会处理 %xx 和 +。
            // 注意：现代 URL 中空格通常编码为 %20，这里也兼容表单风格的 +。
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            // 如果客户端传了非法的百分号编码，直接返回原值比让整个请求失败更友好。
            return value;
        }
    }

    /**
     * 获取安全的异常提示信息。
     *
     * @param exception     原始异常
     * @param defaultText   异常没有 message 时使用的默认提示
     * @return 可用于 JSON message 字段的非空文本
     */
    private String safeMessage(Throwable throwable, String defaultText) {
        // 先读取异常自带的 message。
        String message = throwable.getMessage();

        // 如果 message 为空或空白，则返回调用方指定的默认提示。
        if (message == null || message.isBlank()) {
            return defaultText;
        }

        // 返回原始异常提示，便于前端定位问题。
        return message;
    }
}
