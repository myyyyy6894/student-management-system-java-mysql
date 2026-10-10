package com.sms.util;

// 导入被测实体。
import com.sms.model.entity.Student;
// 导入 JUnit 5 断言与注解。
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JsonUtil 日期序列化 / 反序列化的单元测试。
 */
class JsonUtilUnitTest {

    @Test
    @DisplayName("LocalDate 序列化为 yyyy-MM-dd，并能反序列化回来")
    void localDate_roundTrip() {
        Student student = new Student();
        student.setStudentNo("20240101");
        student.setName("张三");
        student.setBirthDate(LocalDate.of(2004, 3, 15));

        String json = JsonUtil.toJson(student);
        assertTrue(json.contains("\"birthDate\":\"2004-03-15\""), "日期应输出为 yyyy-MM-dd，实际：" + json);

        Student parsed = JsonUtil.fromJson(json, Student.class);
        assertEquals(LocalDate.of(2004, 3, 15), parsed.getBirthDate());
    }

    @Test
    @DisplayName("null 或空白 JSON 返回 null")
    void fromJson_nullOrBlank_returnsNull() {
        assertNull(JsonUtil.fromJson(null, Student.class));
        assertNull(JsonUtil.fromJson("   ", Student.class));
    }

    @Test
    @DisplayName("非法日期字符串会抛出运行时异常，调用方必须转换为 400")
    void fromJson_invalidDate_throwsRuntimeException() {
        String json = "{\"studentNo\":\"1\",\"name\":\"x\",\"major\":\"m\",\"className\":\"c\","
                + "\"birthDate\":\"2024-13-45\"}";

        // 这里刻意断言运行时异常类型，提醒 Controller 必须捕获并转换成 400 参数错误。
        assertThrows(RuntimeException.class, () -> JsonUtil.fromJson(json, Student.class));
    }
}
