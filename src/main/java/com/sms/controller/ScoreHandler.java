package com.sms.controller;

// 导入 HTTP 请求对象。
import com.sun.net.httpserver.HttpExchange;
// 导入成绩查询参数对象。
import com.sms.model.dto.ScoreQueryDTO;
// 导入成绩实体类。
import com.sms.model.entity.Score;
// 导入成绩明细视图对象。
import com.sms.model.vo.ScoreDetailVO;
// 导入成绩统计视图对象。
import com.sms.model.vo.ScoreStatsVO;
// 导入分页结果对象。
import com.sms.model.vo.PageResult;
// 导入统一响应包装类。
import com.sms.model.vo.CommonResult;
// 导入成绩业务接口。
import com.sms.service.ScoreService;
// 导入成绩业务默认实现类。
import com.sms.service.impl.ScoreServiceImpl;
// 导入 JSON 工具类。
import com.sms.util.JsonUtil;
// 导入 Gson 的 JSON 语法异常。
import com.google.gson.JsonSyntaxException;
// 导入 IO 异常。
import java.io.IOException;
// 导入 Map 和 LinkedHashMap，用于解析 Query 参数和构造响应数据。
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 成绩 HTTP 控制器。
 * <p>负责成绩相关 HTTP 请求的路由分发和统一 JSON 输出。</p>
 */
public class ScoreHandler extends BaseHttpHandler {

    // 成绩资源基础路径。
    private static final String BASE_PATH = "/api/scores";

    // 成绩统计接口路径。
    private static final String STATISTICS_PATH = BASE_PATH + "/statistics";

    // 成绩业务接口。
    private final ScoreService scoreService;

    /**
     * 无参构造器，使用默认成绩业务实现。
     */
    public ScoreHandler() {
        // 默认使用真实的成绩业务实现。
        this(new ScoreServiceImpl());
    }

    /**
     * 支持依赖注入的构造器。
     *
     * @param scoreService 成绩业务接口
     */
    public ScoreHandler(ScoreService scoreService) {
        // Service 不能为空。
        if (scoreService == null) {
            throw new IllegalArgumentException("scoreService 不能为空");
        }

        // 保存业务对象。
        this.scoreService = scoreService;
    }

    /**
     * 根据 HTTP 方法分发成绩请求。
     *
     * @param exchange 当前 HTTP 请求对象
     * @throws Exception 业务处理失败时抛出
     */
    @Override
    protected void processRequest(HttpExchange exchange) throws Exception {
        // 读取请求方法并统一转为大写。
        String method = exchange.getRequestMethod().toUpperCase();

        // 统计接口路径需要优先判断，避免被当成详情路径处理。
        if (isStatistics(exchange)) {
            if ("GET".equals(method)) {
                doStatistics(exchange);
            } else {
                sendJsonResponse(
                        exchange,
                        405,
                        JsonUtil.toJson(CommonResult.error(405, "不支持的请求方法"))
                );
            }
            return;
        }

        // 根据 RESTful 语义分发普通成绩请求。
        switch (method) {
            // GET 可能是成绩明细分页查询，也可能是单条成绩详情。
            case "GET" -> doListOrDetail(exchange);
            // POST 表示录入成绩。
            case "POST" -> doCreate(exchange);
            // PUT 表示修改成绩。
            case "PUT" -> doUpdate(exchange);
            // DELETE 表示删除成绩。
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
     * 判断当前请求是否是成绩统计接口。
     *
     * @param exchange 当前 HTTP 请求对象
     * @return true 表示统计接口
     */
    private boolean isStatistics(HttpExchange exchange) {
        // 使用请求路径精确匹配统计接口。
        return STATISTICS_PATH.equals(exchange.getRequestURI().getPath());
    }

    /**
     * 处理 GET 请求：没有路径 ID 时查询成绩明细；有路径 ID 时查询单条成绩。
     *
     * @param exchange 当前 HTTP 请求对象
     * @throws Exception 业务处理失败时抛出
     */
    private void doListOrDetail(HttpExchange exchange) throws Exception {
        // 从路径末尾提取成绩 ID。
        Long scoreId = extractPathId(exchange, BASE_PATH);

        // 如果没有 ID，说明当前请求的是成绩明细列表。
        if (scoreId == null) {
            doList(exchange);
        } else {
            // 如果有 ID，说明当前请求的是单条成绩详情。
            doDetail(exchange, scoreId);
        }
    }

    /**
     * 处理成绩明细分页查询。
     *
     * @param exchange 当前 HTTP 请求对象
     * @throws Exception 业务处理失败时抛出
     */
    private void doList(HttpExchange exchange) throws Exception {
        // 解析 URL 中的查询参数。
        Map<String, String> queryParams = parseQueryParams(exchange);

        // 创建查询参数对象，并填充 Service/DAO 需要的条件字段。
        ScoreQueryDTO queryDTO = new ScoreQueryDTO();
        queryDTO.setPage(parseInt(queryParams.get("page"), 1));
        queryDTO.setPageSize(parseInt(queryParams.get("pageSize"), 10));
        queryDTO.setStudentId(parseLongOrNull(queryParams.get("studentId")));
        queryDTO.setCourseId(parseLongOrNull(queryParams.get("courseId")));
        queryDTO.setStudentName(cleanText(queryParams.get("studentName")));
        queryDTO.setMinScore(parseDoubleOrNull(queryParams.get("minScore")));
        queryDTO.setMaxScore(parseDoubleOrNull(queryParams.get("maxScore")));

        // 调用业务层完成成绩明细分页查询。
        PageResult<ScoreDetailVO> pageResult = scoreService.queryScoreDetails(queryDTO);

        // 输出统一 JSON 响应。
        sendJsonResponse(
                exchange,
                200,
                JsonUtil.toJson(CommonResult.success("查询成功", pageResult))
        );
    }

    /**
     * 处理单条成绩详情查询。
     *
     * @param exchange 当前 HTTP 请求对象
     * @param scoreId  成绩主键 ID
     * @throws Exception 业务处理失败时抛出
     */
    private void doDetail(HttpExchange exchange, Long scoreId) throws Exception {
        // 调用业务层查询单条成绩。
        Score score = scoreService.getScoreById(scoreId);

        // 如果成绩不存在，返回 404。
        if (score == null) {
            sendJsonResponse(
                    exchange,
                    404,
                    JsonUtil.toJson(CommonResult.error(404, "成绩不存在"))
            );
            return;
        }

        // 输出成绩详情。
        sendJsonResponse(
                exchange,
                200,
                JsonUtil.toJson(CommonResult.success("查询成功", score))
        );
    }

    /**
     * 处理成绩统计接口。
     *
     * @param exchange 当前 HTTP 请求对象
     * @throws Exception 业务处理失败时抛出
     */
    private void doStatistics(HttpExchange exchange) throws Exception {
        // 解析 URL 中的查询参数。
        Map<String, String> queryParams = parseQueryParams(exchange);

        // courseId 可以为空，表示统计全部课程。
        Long courseId = parseLongOrNull(queryParams.get("courseId"));

        // 调用业务层统计成绩。
        ScoreStatsVO stats = scoreService.getScoreStatistics(courseId);

        // 输出统一 JSON 响应。
        sendJsonResponse(
                exchange,
                200,
                JsonUtil.toJson(CommonResult.success("统计计算成功", stats))
        );
    }

    /**
     * 处理成绩录入请求。
     *
     * @param exchange 当前 HTTP 请求对象
     * @throws Exception 业务处理失败时抛出
     */
    private void doCreate(HttpExchange exchange) throws Exception {
        // 读取并解析成绩 JSON 请求体。
        Score score = parseScoreBody(exchange);

        // 调用业务层录入成绩；Service 会校验学生、课程、分数范围和重复录入。
        scoreService.addScore(score);

        // 按接口风格返回新生成的成绩 ID。
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", score.getId());

        // 输出统一 JSON 响应。
        sendJsonResponse(
                exchange,
                200,
                JsonUtil.toJson(CommonResult.success("成绩录入成功", data))
        );
    }

    /**
     * 处理成绩修改请求。
     *
     * @param exchange 当前 HTTP 请求对象
     * @throws Exception 业务处理失败时抛出
     */
    private void doUpdate(HttpExchange exchange) throws Exception {
        // 修改接口必须携带路径 ID。
        Long scoreId = extractPathId(exchange, BASE_PATH);

        // ID 为空说明请求路径不是 /api/scores/{id}。
        if (scoreId == null) {
            sendJsonResponse(
                    exchange,
                    400,
                    JsonUtil.toJson(CommonResult.error(400, "成绩 ID 不能为空"))
            );
            return;
        }

        // 读取并解析成绩 JSON 请求体。
        Score score = parseScoreBody(exchange);

        // 修改记录的 ID 以路径参数为准，不能信任请求体中的 id。
        score.setId(scoreId);

        // 先确认成绩存在，避免 Service 的业务异常导致无法区分 404。
        Score existingScore = scoreService.getScoreById(scoreId);

        // 如果成绩不存在，返回 404。
        if (existingScore == null) {
            sendJsonResponse(
                    exchange,
                    404,
                    JsonUtil.toJson(CommonResult.error(404, "成绩不存在"))
            );
            return;
        }

        // 调用业务层修改成绩。
        scoreService.updateScore(score);

        // 输出统一 JSON 响应。
        sendJsonResponse(
                exchange,
                200,
                JsonUtil.toJson(CommonResult.success("成绩更新成功", true))
        );
    }

    /**
     * 处理成绩删除请求。
     *
     * @param exchange 当前 HTTP 请求对象
     * @throws Exception 业务处理失败时抛出
     */
    private void doDelete(HttpExchange exchange) throws Exception {
        // 删除接口必须携带路径 ID。
        Long scoreId = extractPathId(exchange, BASE_PATH);

        // ID 为空说明请求路径不是 /api/scores/{id}。
        if (scoreId == null) {
            sendJsonResponse(
                    exchange,
                    400,
                    JsonUtil.toJson(CommonResult.error(400, "成绩 ID 不能为空"))
            );
            return;
        }

        // 先确认成绩存在，避免 Service 的业务异常导致无法区分 404。
        Score existingScore = scoreService.getScoreById(scoreId);

        // 如果成绩不存在，返回 404。
        if (existingScore == null) {
            sendJsonResponse(
                    exchange,
                    404,
                    JsonUtil.toJson(CommonResult.error(404, "成绩不存在"))
            );
            return;
        }

        // 调用业务层删除成绩。
        scoreService.deleteScore(scoreId);

        // 输出统一 JSON 响应。
        sendJsonResponse(
                exchange,
                200,
                JsonUtil.toJson(CommonResult.success("成绩删除成功", true))
        );
    }

    /**
     * 读取并解析 Score JSON 请求体。
     *
     * @param exchange 当前 HTTP 请求对象
     * @return 成绩实体对象
     * @throws IOException 读取请求体失败时抛出
     */
    private Score parseScoreBody(HttpExchange exchange) throws IOException {
        // 读取 POST/PUT 的 JSON 字符串。
        String requestBody = readRequestBody(exchange);

        // 空请求体无法反序列化成 Score，属于请求参数错误。
        if (requestBody == null || requestBody.isBlank()) {
            throw new IllegalArgumentException("请求体不能为空");
        }

        try {
            // 使用 JsonUtil 将 JSON 转换成 Score 实体。
            Score score = JsonUtil.fromJson(requestBody, Score.class);

            // 如果 JSON 内容是 null，也视为参数错误。
            if (score == null) {
                throw new IllegalArgumentException("请求体不能为空");
            }

            // 返回解析完成的成绩对象。
            return score;
        } catch (JsonSyntaxException exception) {
            // JSON 语法非法时统一返回业务参数错误。
            throw new IllegalArgumentException("请求体 JSON 格式错误");
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
     * 解析 Query 参数中的 Long 值。
     *
     * @param value 原始字符串
     * @return Long 值；参数不存在时返回 null
     */
    private Long parseLongOrNull(String value) {
        // 参数不存在时保持 null，表示不按该字段筛选。
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            // 将字符串转换为 Long。
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            // 参数格式非法时返回 400。
            throw new IllegalArgumentException("ID 参数必须是数字");
        }
    }

    /**
     * 解析 Query 参数中的 Double 值。
     *
     * @param value 原始字符串
     * @return Double 值；参数不存在时返回 null
     */
    private Double parseDoubleOrNull(String value) {
        // 参数不存在时保持 null，表示不按该字段筛选。
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            // 将字符串转换为 Double。
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            // 参数格式非法时返回 400。
            throw new IllegalArgumentException("分数参数必须是数字");
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
