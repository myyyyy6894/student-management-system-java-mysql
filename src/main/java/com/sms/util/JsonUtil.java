package com.sms.util;

// 导入 Gson 核心类，负责对象和 JSON 字符串之间的双向转换。
import com.google.gson.Gson;
// 导入 Gson 构建器，可以在创建 Gson 对象前进行一些配置，例如是否输出 null 字段。
import com.google.gson.GsonBuilder;

/**
 * JSON 工具类。
 * <p>封装 Gson 的常用能力，对外只提供两个简单方法：</p>
 * <p>1. toJson：把 Java 对象转换成 JSON 字符串；</p>
 * <p>2. fromJson：把 JSON 字符串转换回指定类型的 Java 对象。</p>
 */
public final class JsonUtil {

    // 创建一个全局共用的 Gson 对象。
    // Gson 本身是线程安全的，声明为 static final 可以在程序中复用，避免每次都重复创建。
    // serializeNulls() 表示 JSON 序列化时保留值为 null 的字段，例如 data=null 也会输出成 "data":null。
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

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
}
