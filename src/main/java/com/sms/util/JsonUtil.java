package com.sms.util;

// 导入 Gson 核心类，负责对象和 JSON 字符串之间的双向转换。
import com.google.gson.Gson;
// 导入 Gson 构建器，可以在创建 Gson 对象前注册自定义类型适配器。
import com.google.gson.GsonBuilder;
// 导入 Gson 自定义序列化器接口，用于控制 Java 对象如何转换成 JSON。
import com.google.gson.JsonSerializer;
// 导入 Gson 自定义反序列化器接口，用于控制 JSON 如何转换回 Java 对象。
import com.google.gson.JsonDeserializer;
// 导入 Gson 的 JsonElement 和 JsonPrimitive，用于读取 JSON 字段值。
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
// 导入 Java 8 日期类，数据库 DATE 和 DATETIME 字段在实体中使用这两种类型。
import java.time.LocalDate;
import java.time.LocalDateTime;
// 导入日期格式化器，统一 JSON 中日期和日期时间的输出格式。
import java.time.format.DateTimeFormatter;

/**
 * JSON 工具类。
 * <p>封装 Gson 的常用能力，对外只提供两个简单方法：</p>
 * <p>1. toJson：把 Java 对象转换成 JSON 字符串；</p>
 * <p>2. fromJson：把 JSON 字符串转换回指定类型的 Java 对象。</p>
 */
public final class JsonUtil {

    // 数据库 DATE 字段在 JSON 中统一输出为 yyyy-MM-dd。
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    // 数据库 DATETIME 字段在 JSON 中统一输出为 yyyy-MM-dd HH:mm:ss。
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 创建一个全局共用的 Gson 对象。
    // Gson 本身是线程安全的，声明为 static final 可以在程序中复用，避免每次都重复创建。
    // serializeNulls() 表示 JSON 序列化时保留值为 null 的字段，例如 data=null 也会输出成 "data":null。
    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(LocalDate.class, localDateSerializer())
            .registerTypeAdapter(LocalDate.class, localDateDeserializer())
            .registerTypeAdapter(LocalDateTime.class, localDateTimeSerializer())
            .registerTypeAdapter(LocalDateTime.class, localDateTimeDeserializer())
            .create();

    /**
     * 私有构造器，禁止外部创建 JsonUtil 对象。
     */
    private JsonUtil() {
        // 工具类只需要静态方法，不需要对象状态。
    }

    /**
     * 将 Java 对象序列化为 JSON 字符串。
     *
     * @param obj 要转换的对象，例如 CommonResult、PageResult、Student
     * @return JSON 字符串；如果 obj 为 null，则返回字符串 "null"
     */
    public static String toJson(Object obj) {
        // Gson.toJson 会根据对象属性生成 JSON 字符串。
        // 如果 obj 是 null，Gson 会返回 "null"，这也是 JSON 表示空值的标准格式。
        return GSON.toJson(obj);
    }

    /**
     * 将 JSON 字符串反序列化为指定类型的 Java 对象。
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型的 Class 对象，例如 CommonResult.class、Student.class
     * @param <T>   目标 Java 类型
     * @return 转换后的对象；如果 json 为 null 或空字符串，则返回 null
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        // 先判断输入的 JSON 字符串是否为 null 或空字符串。
        if (json == null || json.isBlank()) {
            // 空字符串没有可解析的数据，直接返回 null，避免交给 Gson 后产生歧义。
            return null;
        }

        // Gson.fromJson 会按照 clazz 指定的类型解析 JSON 字符串。
        // 例如 clazz 是 CommonResult.class，就会把 JSON 转换成 CommonResult 对象。
        return GSON.fromJson(json, clazz);
    }

    /**
     * 创建 LocalDate 序列化器。
     *
     * @return Gson 自定义序列化器
     */
    private static JsonSerializer<LocalDate> localDateSerializer() {
        // Gson 2.x 的 lambda 形式：第一个参数是被序列化对象，第二个是类型，第三个是上下文。
        return (source, typeOfSrc, context) -> new JsonPrimitive(DATE_FORMATTER.format(source));
    }

    /**
     * 创建 LocalDate 反序列化器。
     *
     * @return Gson 自定义反序列化器
     */
    private static JsonDeserializer<LocalDate> localDateDeserializer() {
        // Gson 2.x 的 lambda 形式：第一个参数是 JSON 元素，第二个是类型，第三个是上下文。
        return (json, typeOfJson, context) -> parseDate(json);
    }

    /**
     * 创建 LocalDateTime 序列化器。
     *
     * @return Gson 自定义序列化器
     */
    private static JsonSerializer<LocalDateTime> localDateTimeSerializer() {
        // 使用固定格式 yyyy-MM-dd HH:mm:ss，符合接口文档中的时间输出示例。
        return (source, typeOfSrc, context) -> new JsonPrimitive(DATE_TIME_FORMATTER.format(source));
    }

    /**
     * 创建 LocalDateTime 反序列化器。
     *
     * @return Gson 自定义反序列化器
     */
    private static JsonDeserializer<LocalDateTime> localDateTimeDeserializer() {
        // 使用固定格式 yyyy-MM-dd HH:mm:ss，兼容数据库时间字段在 JSON 中的常见格式。
        return (json, typeOfJson, context) -> parseDateTime(json);
    }

    /**
     * 将 JSON 元素解析为 LocalDate。
     *
     * @param json JSON 元素
     * @return LocalDate；JSON 值为 null 时返回 null
     */
    private static LocalDate parseDate(JsonElement json) {
        // 如果 JSON 中该字段为 null，直接返回 null。
        if (json == null || json.isJsonNull()) {
            return null;
        }

        // 将 JSON 字符串解析为 LocalDate。
        return LocalDate.parse(json.getAsString(), DATE_FORMATTER);
    }

    /**
     * 将 JSON 元素解析为 LocalDateTime。
     *
     * @param json JSON 元素
     * @return LocalDateTime；JSON 值为 null 时返回 null
     */
    private static LocalDateTime parseDateTime(JsonElement json) {
        // 如果 JSON 中该字段为 null，直接返回 null。
        if (json == null || json.isJsonNull()) {
            return null;
        }

        // 将 JSON 字符串解析为 LocalDateTime。
        return LocalDateTime.parse(json.getAsString(), DATE_TIME_FORMATTER);
    }
}
