package com.sms.util;

// 导入文件输入流，用来读取 classpath 下的 db.properties 配置文件。
import java.io.InputStream;
// 导入 IO 异常，用于处理配置文件读取失败的情况。
import java.io.IOException;
// 导入数据库连接对象，代表 Java 程序与 MySQL 数据库之间的一次连接。
import java.sql.Connection;
// 导入驱动管理器，负责根据连接信息创建数据库连接。
import java.sql.DriverManager;
// 导入预编译 SQL 语句对象，后续 DAO 层会通过它安全地执行 SQL。
import java.sql.PreparedStatement;
// 导入 SQL 异常，表示连接或操作数据库时可能出现的问题。
import java.sql.SQLException;
// 导入结果集对象，用于保存查询语句返回的数据。
import java.sql.ResultSet;
// 导入 Properties，用来解析 key=value 形式的配置文件。
import java.util.Properties;

/**
 * 数据库工具类。
 * <p>本类只负责三件事：加载配置文件、加载 JDBC 驱动、获取并释放数据库连接。</p>
 * <p>类中的工具方法都声明为 static，调用时不需要先创建 DBUtil 对象。</p>
 */
public final class DBUtil {

    // 配置文件的相对路径：文件实际位于 src/main/resources/db.properties，编译后会出现在 classpath 根目录下。
    private static final String CONFIG_FILE = "db.properties";

    // 数据库连接地址，例如 jdbc:mysql://127.0.0.1:3306/student_db?...。
    private static String url;

    // 登录 MySQL 的用户名。
    private static String username;

    // 登录 MySQL 的密码。
    private static String password;

    /**
     * 静态初始化块。
     * <p>JVM 第一次加载 DBUtil 类时执行，且整个程序生命周期只执行一次。</p>
     */
    static {
        // try-with-resources 会在 try 块结束后自动关闭输入流，避免手动 close 造成资源泄漏。
        // DBUtil.class.getClassLoader() 先获取类加载器，再通过 getResourceAsStream 从 classpath 根路径读取配置文件。
        try (InputStream inputStream = DBUtil.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            // 如果返回 null，说明 classpath 下找不到 db.properties 文件。
            if (inputStream == null) {
                // 主动抛出异常，让程序在启动阶段立即失败，而不是等到获取数据库连接时才报错。
                throw new IllegalStateException("未在 classpath 根路径找到 db.properties 配置文件");
            }

            // 创建 Properties 对象，用于临时保存配置文件中的所有 key=value 数据。
            Properties properties = new Properties();
            // load(inputStream) 会读取输入流内容，并把里面的驱动、地址、账号、密码等配置加载到 properties 中。
            properties.load(inputStream);

            // 从配置对象中读取数据库连接地址。
            url = properties.getProperty("db.url");
            // 从配置对象中读取数据库用户名。
            username = properties.getProperty("db.username");
            // 从配置对象中读取数据库密码。
            password = properties.getProperty("db.password");
            // 从配置对象中读取 JDBC 驱动类的全限定名。
            String driverClassName = properties.getProperty("db.driver");

            // 校验关键配置是否为空或空白字符串，避免带着不完整配置继续运行。
            if (isBlank(url) || isBlank(username) || isBlank(password) || isBlank(driverClassName)) {
                // 配置不完整属于启动期错误，这里直接终止 DBUtil 的初始化。
                throw new IllegalStateException("db.properties 中的数据库配置不完整");
            }

            // Class.forName 通过反射加载 MySQL JDBC 驱动类，让 DriverManager 能够识别 jdbc:mysql 协议。
            Class.forName(driverClassName);
        } catch (IOException | ClassNotFoundException exception) {
            // IOException 表示配置文件读取失败；ClassNotFoundException 表示驱动类不存在。
            // 包装成运行时异常抛出，让调用方看到更清晰的初始化失败原因。
            throw new RuntimeException("初始化数据库配置或 JDBC 驱动失败", exception);
        }
    }

    /**
     * 私有构造器，禁止在外部创建 DBUtil 对象。
     */
    private DBUtil() {
        // 工具类不需要实例状态，保留空构造器即可。
    }

    /**
     * 获取一个新的数据库连接。
     *
     * @return JDBC 数据库连接对象
     * @throws SQLException 当数据库不可用、账号密码错误或连接参数不正确时抛出
     */
    public static Connection getConnection() throws SQLException {
        // DriverManager 会根据 url 选择合适的驱动，并使用用户名和密码建立数据库连接。
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * 安全释放查询操作涉及的三类 JDBC 资源。
     *
     * @param resultSet      查询结果集，可能为 null
     * @param preparedStatement SQL 执行对象，可能为 null
     * @param connection     数据库连接对象，可能为 null
     */
    public static void close(ResultSet resultSet, PreparedStatement preparedStatement, Connection connection) {
        // 先关闭 ResultSet，因为它是基于 Statement 执行后得到的数据对象。
        if (resultSet != null) {
            // close 也可能抛出 SQL 异常，这里单独捕获，避免影响后续资源的关闭。
            try {
                // 关闭结果集并释放其占用的 JDBC 资源。
                resultSet.close();
            } catch (SQLException exception) {
                // 打印简洁错误信息，方便定位资源释放问题。
                System.err.println("关闭 ResultSet 失败：" + exception.getMessage());
            }
        }

        // 再关闭 PreparedStatement。
        if (preparedStatement != null) {
            // 单独捕获 Statement 的关闭异常，保证即使这里失败也能继续关闭连接。
            try {
                // 关闭 SQL 语句对象，并释放数据库端和客户端占用的资源。
                preparedStatement.close();
            } catch (SQLException exception) {
                // 打印 Statement 关闭失败的原因。
                System.err.println("关闭 PreparedStatement 失败：" + exception.getMessage());
            }
        }

        // 最后关闭 Connection，因为它是最外层的数据库连接资源。
        if (connection != null) {
            // 单独捕获连接关闭异常，防止中断整个 close 方法。
            try {
                // 关闭数据库连接并归还给 MySQL 服务端处理。
                connection.close();
            } catch (SQLException exception) {
                // 打印 Connection 关闭失败的原因。
                System.err.println("关闭 Connection 失败：" + exception.getMessage());
            }
        }
    }

    /**
     * 关闭增删改操作中不需要使用 ResultSet 的资源。
     *
     * @param preparedStatement SQL 执行对象
     * @param connection        数据库连接对象
     */
    public static void close(PreparedStatement preparedStatement, Connection connection) {
        // 复用三参数 close 方法，把第一个 ResultSet 参数显式设置为 null。
        close(null, preparedStatement, connection);
    }

    /**
     * 单独关闭数据库连接。
     *
     * @param connection 数据库连接对象
     */
    public static void close(Connection connection) {
        // 复用三参数 close 方法，把 ResultSet 和 PreparedStatement 都显式设置为 null。
        close(null, null, connection);
    }

    /**
     * 判断字符串是否为 null、空字符串或空白字符串。
     *
     * @param value 待检查的字符串
     * @return true 表示无效；false 表示字符串有内容
     */
    private static boolean isBlank(String value) {
        // value == null 检查空引用；value.isBlank() 检查空串和纯空格字符串。
        return value == null || value.isBlank();
    }
}
