package com.sms.util;

// 导入数据库连接对象，测试 DBUtil.getConnection() 时需要使用。
import java.sql.Connection;
// 导入 SQL 异常，用于捕获数据库连接或查询过程中的错误。
import java.sql.SQLException;
// 导入预编译语句对象，用来执行一个极简的 SELECT 测试语句。
import java.sql.PreparedStatement;
// 导入结果集对象，用来读取 SELECT 语句的返回值。
import java.sql.ResultSet;

/**
 * DBUtil 的临时手工测试类。
 * <p>当前 pom.xml 还没有 JUnit 依赖，所以这里提供 main 方法，方便直接运行验证。</p>
 */
public class DBUtilTest {

    /**
     * Java 程序入口，右键运行后会依次执行数据库连接测试和资源关闭测试。
     *
     * @param args 命令行参数，当前测试不需要传入任何参数
     */
    public static void main(String[] args) {
        // 输出测试开始提示，便于在控制台确认程序已经启动。
        System.out.println("===== DBUtil 连接测试开始 =====");

        // 声明三个 JDBC 资源变量，统一放到方法外层，方便最后通过 DBUtil.close() 释放。
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            // 调用 DBUtil 的静态方法，根据 db.properties 创建一个新的数据库连接。
            connection = DBUtil.getConnection();

            // isClosed() 返回 false 表示连接对象当前处于打开状态。
            // 这里用 !connection.isClosed() 判断连接确实还没有关闭。
            if (connection == null || connection.isClosed()) {
                // 如果连接为 null 或已经关闭，说明连接获取失败。
                throw new IllegalStateException("数据库连接获取失败或已关闭");
            }

            // 输出连接成功提示。
            System.out.println("【1】数据库连接成功：" + !connection.isClosed());

            // 预编译一个最简单的 SQL：查询常量 1，并给它取一个别名叫 test_result。
            preparedStatement = connection.prepareStatement("SELECT 1 AS test_result");

            // executeQuery() 执行 SELECT 语句，并把查询结果保存到结果集中。
            resultSet = preparedStatement.executeQuery();

            // next() 先把游标移动到第一行；如果有数据则返回 true。
            if (!resultSet.next()) {
                // 正常情况下 SELECT 1 一定会返回一行，如果没有返回则说明数据库响应异常。
                throw new IllegalStateException("SELECT 1 查询结果异常");
            }

            // getInt("test_result") 读取刚才查询出来的常量值。
            int result = resultSet.getInt("test_result");

            // SELECT 1 的结果应该正好等于 1。
            if (result != 1) {
                // 如果结果不是 1，说明结果映射或数据库执行出现了异常。
                throw new IllegalStateException("SELECT 1 返回值不是 1，实际值：" + result);
            }

            // 输出 SQL 查询成功的结果。
            System.out.println("【2】SELECT 1 查询成功，结果：" + result);

            // 调用 DBUtil 的安全关闭方法，按 ResultSet -> PreparedStatement -> Connection 的顺序释放资源。
            DBUtil.close(resultSet, preparedStatement, connection);

            // 关闭成功后，把三个本地变量重新设置为 null，避免后续误用已关闭资源。
            resultSet = null;
            preparedStatement = null;
            connection = null;

            // 再次输出提示，确认资源已经通过 close 方法释放。
            System.out.println("【3】JDBC 资源关闭成功");
        } catch (SQLException exception) {
            // 数据库连接失败、SQL 执行失败等都会进入这里。
            System.err.println("数据库测试失败：" + exception.getMessage());
            // 打印完整异常堆栈，便于定位具体问题。
            exception.printStackTrace();
        } finally {
            // finally 中的兜底关闭逻辑用于处理中途抛异常的情况。
            // 如果 try 中已经关闭并置为 null，这里会直接跳过。
            DBUtil.close(resultSet, preparedStatement, connection);
        }

        // 输出测试结束提示。
        System.out.println("===== DBUtil 连接测试结束 =====");
    }
}
