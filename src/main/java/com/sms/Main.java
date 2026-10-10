package com.sms;

// 导入统一 HTTP 服务启动类。
import com.sms.server.AppHttpServer;
// 导入 IO 异常，端口被占用时需要捕获。
import java.io.IOException;

/**
 * 学生管理系统统一启动入口。
 * <p>运行 Main.main() 即可启动所有 API 路由。</p>
 */
public final class Main {

    /**
     * 私有构造器，Main 是程序入口类，不需要创建对象。
     */
    private Main() {
    }

    /**
     * Java 程序入口。
     *
     * @param args 可选的第一个参数表示 HTTP 端口；不传时默认使用 8080。
     */
    public static void main(String[] args) {
        // 默认使用规范文档中的 8080 端口。
        int port = AppHttpServer.DEFAULT_PORT;

        // 如果用户通过启动参数传入端口，则使用用户指定的端口。
        if (args.length > 0) {
            try {
                // 第一个启动参数作为端口号。
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException exception) {
                // 端口参数必须是数字。
                System.err.println("启动参数必须是数字端口，当前值：" + args[0]);
                System.exit(1);
                return;
            }
        }

        // 创建统一 HTTP 服务对象。
        AppHttpServer appHttpServer = new AppHttpServer(port);

        // JVM 正常退出、Ctrl+C 或 IDE 停止按钮触发 shutdown hook，确保释放端口和线程池。
        Runtime.getRuntime().addShutdownHook(new Thread(appHttpServer::stop, "app-http-shutdown"));

        try {
            // 启动 HTTP 服务并注册所有业务路由。
            appHttpServer.start();
        } catch (IOException exception) {
            // 最常见原因是 8080 端口已经被其他程序占用。
            System.err.println("HTTP 服务启动失败：" + exception.getMessage());
            exception.printStackTrace();

            // 启动失败时返回非 0 退出码，方便脚本和 IDE 识别失败状态。
            System.exit(1);
        }
    }
}
