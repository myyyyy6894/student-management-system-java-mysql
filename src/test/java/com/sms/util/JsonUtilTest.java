package com.sms.util;

/**
 * JsonUtil 的临时手工测试类。
 * <p>当前项目还没有引入 JUnit，所以先用 main 方法直接运行验证。</p>
 */
public class JsonUtilTest {

    /**
     * 用于测试 JSON 转换的简单内部类。
     * <p>这里故意包含一个 email 字段，用来验证 null 字段是否也能被序列化。</p>
     */
    public static class TestData {
        // 学生姓名，用来验证普通字符串字段的序列化和反序列化。
        private String name;

        // 学生年龄，用来验证基础数值字段。
        private Integer age;

        // 学生邮箱，测试时设置为 null，用来验证 Gson 保留 null 字段。
        private String email;

        /**
         * 获取姓名。
         *
         * @return 姓名
         */
        public String getName() {
            return name;
        }

        /**
         * 设置姓名。
         *
         * @param name 姓名
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * 获取年龄。
         *
         * @return 年龄
         */
        public Integer getAge() {
            return age;
        }

        /**
         * 设置年龄。
         *
         * @param age 年龄
         */
        public void setAge(Integer age) {
            this.age = age;
        }

        /**
         * 获取邮箱。
         *
         * @return 邮箱
         */
        public String getEmail() {
            return email;
        }

        /**
         * 设置邮箱。
         *
         * @param email 邮箱
         */
        public void setEmail(String email) {
            this.email = email;
        }
    }

    /**
     * 测试入口，直接运行后会依次验证 JSON 序列化、反序列化和空字符串处理。
     *
     * @param args 命令行参数，当前测试不需要传入任何参数
     */
    public static void main(String[] args) {
        // 输出测试开始提示，方便在控制台确认测试已经执行。
        System.out.println("===== JsonUtil 测试开始 =====");

        // 创建一个测试对象，模拟后面项目中会被 JSON 序列化的实体或 VO。
        TestData testData = new TestData();

        // 设置普通字符串字段。
        testData.setName("张三");

        // 设置基础数值字段。
        testData.setAge(20);

        // 不设置 email，让它的值保持 null，用来验证 serializeNulls() 配置。
        testData.setEmail(null);

        // 调用 toJson，把 Java 对象转换成 JSON 字符串。
        String json = JsonUtil.toJson(testData);

        // 输出序列化后的 JSON，方便人工检查。
        System.out.println("【1】对象转 JSON：" + json);

        // 校验序列化结果不为空。
        if (json == null || json.isBlank()) {
            // 如果 JSON 为空，说明 toJson 没有正常工作。
            throw new IllegalStateException("toJson 序列化结果为空");
        }

        // 校验普通字段 name 是否正确输出。
        if (!json.contains("\"name\":\"张三\"")) {
            // 如果不包含 name 字段，说明字段序列化失败。
            throw new IllegalStateException("toJson 没有正确序列化 name 字段");
        }

        // 校验 email=null 是否被保留输出为 "email":null。
        if (!json.contains("\"email\":null")) {
            // 如果没有输出 null 字段，说明 serializeNulls() 配置没有生效。
            throw new IllegalStateException("toJson 没有保留 email=null 字段");
        }

        // 调用 fromJson，把刚才生成的 JSON 字符串转换回 TestData 对象。
        TestData parsedData = JsonUtil.fromJson(json, TestData.class);

        // 校验反序列化结果不是 null。
        if (parsedData == null) {
            // 如果对象为 null，说明 JSON 反序列化失败。
            throw new IllegalStateException("fromJson 反序列化结果为 null");
        }

        // 校验 name 字段是否成功还原。
        if (!"张三".equals(parsedData.getName())) {
            // 如果名字不一致，说明 JSON 到对象的字段映射失败。
            throw new IllegalStateException("fromJson 没有正确还原 name 字段，实际值：" + parsedData.getName());
        }

        // 校验 age 字段是否成功还原。
        if (!Integer.valueOf(20).equals(parsedData.getAge())) {
            // 如果年龄不一致，说明基础类型字段映射失败。
            throw new IllegalStateException("fromJson 没有正确还原 age 字段，实际值：" + parsedData.getAge());
        }

        // 输出反序列化成功提示。
        System.out.println("【2】JSON 转对象成功：" + parsedData.getName() + " / " + parsedData.getAge());

        // 测试空字符串场景：fromJson 应该安全地返回 null，而不是抛出解析异常。
        TestData blankResult = JsonUtil.fromJson("", TestData.class);

        // 校验空字符串确实返回 null。
        if (blankResult != null) {
            // 如果不是 null，说明空字符串处理逻辑不符合预期。
            throw new IllegalStateException("空 JSON 字符串应该返回 null，实际返回：" + blankResult);
        }

        // 输出空字符串测试成功提示。
        System.out.println("【3】空 JSON 字符串安全返回 null");

        // 输出测试结束提示。
        System.out.println("===== JsonUtil 测试结束 =====");
    }
}
