# 学生管理系统：企业级标准化开发顺序与实战执行指南

> **核心宗旨**：拒绝“面对海量文档无从下手”。  
> **开发策略**：遵循软件工程经典**“由底向上、核心先行、小步快跑、阶梯验证”**原则。每完成一步，都有明确的**自测手段**，确保上一层永远建立在已验证的坚实地基之上。

---

## 阶段全景路线图（Roadmap Overview）

```
[阶段 0：环境搭建与骨架准备]
   │  └── 建立 Maven 工程、配置 pom.xml 依赖、创建完整包路径
   ▼
[阶段 1：数据库落地与数据种子]
   │  └── 执行 schema.sql 与 data.sql，确保 MySQL 本地能查出测试数据
   ▼
[阶段 2：底层工具基石（Util）与通用响应]
   │  └── 编写 DBUtil、JsonUtil、CommonResult，写个 main 方法测试连通性
   ▼
[阶段 3：实体层（Model）与持久层（DAO）]
   │  └── 编写 Entity 映射，实现 StudentDao，用 PreparedStatement 查出数据
   ▼
[阶段 4：业务逻辑层（Service）与规则校验]
   │  └── 实现 StudentService、ScoreService，增加学号防重、分数 0~100 校验
   ▼
[阶段 5：网络接入层（HttpServer & Handler）]
   │  └── 启动原生 8080 服务器，编写 BaseHttpHandler，用 Postman/浏览器打通 API
   ▼
[阶段 6：前端集成与端到端联调（UI & AI 生成）]
   │  └── 静态资源托管，将 AI 生成的 index.html 放入 resources/static，页面联调
   ▼
[阶段 7：异常边界自测、代码清理与 GitHub 开源]
   │  └── 健壮性测试、编写清晰的 README.md、提交 GitHub
```

---

## 阶段 0：环境搭建与工程脚手架（耗时约 15~30 分钟）

### 目标
在 IDE（IntelliJ IDEA 或 Eclipse）中建立一个结构规整的纯 Java Maven 工程，准备好所有空包与配置文件。

### 具体执行步骤
1. **新建 Maven 工程**：
   - 打开 IDEA $ightarrow$ `New Project` $ightarrow$ 选择 `Maven Archetype` 或纯 `Java` 工程，JDK 选择 8 或 17。
   - `GroupId`: `com.sms`，`ArtifactId`: `student-management-system`。
2. **配置 `pom.xml`**：
   - 绝不引入 Spring 全家桶，只引入最核心的两个基础库：
     - `mysql-connector-j`（MySQL 官方 JDBC 驱动）；
     - `gson`（轻量 JSON 解析库，避免手工拼串出错）。
3. **创建规范的包目录**：
   - 在 `src/main/java/com/sms/` 下建立：
     - `server`、`controller`、`service`、`service.impl`、`dao`、`dao.impl`、`model.entity`、`model.dto`、`model.vo`、`util`。
   - 在 `src/main/resources/` 下建立：
     - `db.properties`（数据库连接配置）；
     - `static/` 目录（预留给之后的前端 HTML）。
4. **根目录归档 SQL 脚本**：
   - 在项目根目录下创建 `sql/` 文件夹，将文档中的 `schema.sql` 和 `data.sql` 保存进去。

> **阶段自测标准（Check Point 0）**：
> - 点击 Maven `Reload`，依赖下载成功无红线；
> - 工程结构一目了然，符合规范文档中定义的目录树。

---

## 阶段 1：数据库建库建表与数据验证（耗时约 15 分钟）

### 目标
让 MySQL 数据库在本地真正运行起来，并预先存在结构与数据。

### 具体执行步骤
1. **创建并导入数据库**：
   - 打开 MySQL 客户端（Navicat、DataGrip、DBeaver 或命令行）；
   - 执行 `sql/schema.sql`：创建 `student_db` 数据库及 `student`、`course`、`score` 三张表；
   - 执行 `sql/data.sql`：插入内置的 5 名测试学生、4 门课程和若干初始成绩。
2. **验证数据与约束**：
   - 运行简单的查询命令确认数据存在：
     ```sql
     SELECT * FROM student;
     SELECT * FROM course;
     SELECT s.name, c.name, sc.score 
     FROM score sc 
     JOIN student s ON sc.student_id = s.id 
     JOIN course c ON sc.course_id = c.id;
     ```

> **阶段自测标准（Check Point 1）**：
> - 数据库连接正常，能在客户端中查出 5 条学生记录，确认字符集为 `utf8mb4`。

---

## 阶段 2：通用基础设施与工具类打底（耗时约 30~45 分钟）

### 目标
把数据库连接、JSON 转换、统一数据格式这三个最核心的“水暖管道”铺设好。

### 具体执行步骤
1. **配置 `resources/db.properties`**：
   - 写入本地 MySQL 的连接地址、端口、账号和密码。
2. **实现 `DBUtil.java`**：
   - 读取配置文件，加载 `com.mysql.cj.jdbc.Driver` 驱动；
   - 提供 `getConnection()` 与 `close(rs, ps, conn)` 安全释放方法。
3. **实现 `CommonResult<T>.java` 与 `PageResult<T>.java`**：
   - 严格按照规范文档定义 `{ code, message, data }` 包装类；
   - 编写 `success()` 和 `error()` 静态便利方法。
4. **实现 `JsonUtil.java`**：
   - 使用 Gson 封装通用的 `toJson(obj)` 和 `fromJson(jsonStr, Class<T>)` 方法。

> **阶段自测标准（Check Point 2）**：
> - 在 `test` 目录或临时在 `Main.java` 中写一个 `public static void main(String[] args)`：
>   ```java
>   Connection conn = DBUtil.getConnection();
>   System.out.println("数据库连接成功: " + !conn.isClosed());
>   conn.close();
>   ```
> - 运行通过且控制台打印 `true`，证明 JDBC 底层通路彻底畅通！

---

## 阶段 3：领域实体与持久层（DAO）开发（耗时约 1~1.5 小时）

### 目标
让 Java 代码能够通过 SQL 从数据库中增删改查实体对象。

### 开发推进顺序（从简单到复杂）：
1. **第 1 步：编写 Entity 实体类**
   - 纯粹的 Java Bean：创建 `Student.java`、`Course.java`、`Score.java`；
   - 确保属性类型与数据库列类型完全对应（如 `BIGINT` 对应 `Long`，`DATETIME` 对应 `LocalDateTime`，`DECIMAL` 对应 `BigDecimal`）；
   - 生成对应的 Getter/Setter 与全参/无参构造器。
2. **第 2 步：实现核心接口 `StudentDao` 与 `StudentDaoImpl`**
   - 先写接口 `StudentDao`，声明 5 个最基础的方法：`findById`、`insert`、`update`、`deleteById`、`findByPage`；
   - 在 `StudentDaoImpl` 中用原生 JDBC 编写 SQL：
     - 重点：使用 `PreparedStatement` 填充参数，禁止字符串拼接 SQL；
     - 处理 `ResultSet` 结果集映射为 `Student` 对象。
3. **第 3 步：实现 `CourseDao` 与 `ScoreDao`**
   - `CourseDaoImpl` 相对简单（主要是列表查询与基础增改）；
   - `ScoreDaoImpl` 包含重点：编写多表联查 SQL（`JOIN student` 和 `JOIN course`），将结果映射为 `ScoreDetailVO`。

> **阶段自测标准（Check Point 3）**：
> - 编写一个简单的单测或 Main 方法：
>   ```java
>   StudentDao dao = new StudentDaoImpl();
>   Student s = dao.findById(1L);
>   System.out.println("查到学生：" + s.getName()); // 应输出：张三
>   ```
> - 能在控制台打印出张三，证明 DAO 层与数据库映射完美成功。

---

## 阶段 4：业务逻辑层（Service）开发（耗时约 45~60 分钟）

### 目标
增加业务规则研判、数据校验、异常拦截与多表联动业务。

### 具体执行步骤
1. **实现 `StudentServiceImpl`**：
   - **新增前校验**：先调用 `studentDao.findByStudentNo(student.getStudentNo())`，若不为空，抛出自定义业务异常或 `IllegalArgumentException("学号已存在")`；
   - **删除前安全检查**：调用 `scoreDao.countByStudentId(id)`，若该学生已有成绩，则阻断删除或提示“不可删除已有成绩档案的学生”；
   - **分页封装**：结合 `countByCondition()` 与 `findByPage()`，组装为规范的 `PageResult<Student>`。
2. **实现 `ScoreServiceImpl`**：
   - **分数范围校验**：校验 `score.compareTo(BigDecimal.ZERO) >= 0 && score.compareTo(BigDecimal.valueOf(100)) <= 0`；
   - **重复录入校验**：校验该学生该门课程是否已有成绩；
   - **统计报表业务**：调用 `scoreDao.calculateStatistics(courseId)` 计算平均分、最高分与及格率。

> **阶段自测标准（Check Point 4）**：
> - 测试学号防重逻辑：尝试 `studentService.addStudent()` 一个已存在的学号（如 20240101），程序必须能够按预期拦截并抛出提示，而不是让数据库抛出原生 SQL 唯一键冲突崩溃。

---

## 阶段 5：原生 HttpServer 与控制器层（Handler）打通（耗时约 1~1.5 小时）

### 目标
无需 Tomcat，完全用 JDK 原生 `HttpServer` 提供标准的 RESTful JSON 接口。

### 具体执行步骤
1. **编写 `BaseHttpHandler.java`**：
   - 提取公共逻辑：CORS 跨域头设置、处理 OPTIONS 预检请求、解析 URL Query 参数为 Map、从 RequestBody 读取 JSON 字符串、封装统一的 JSON 输出流回写。
2. **编写 `StudentHandler.java`**：
   - 继承 `BaseHttpHandler`；
   - 根据 `exchange.getRequestMethod()` 进行多路分发：
     - `GET /api/students` $ightarrow$ 分页列表；
     - `GET /api/students/1` $ightarrow$ 获取详情；
     - `POST /api/students` $ightarrow$ 读取 Body 反序列化为 Student，调用 Service 新增；
     - `PUT /api/students/1` $ightarrow$ 修改；
     - `DELETE /api/students/1` $ightarrow$ 删除。
   - 调用结果均用 `CommonResult.success()` 包装后转为 JSON 字符串发送。
3. **编写 `CourseHandler.java` 与 `ScoreHandler.java`**：
   - 依此类推，挂载对应的业务接口。
4. **配置统一启动类 `AppHttpServer.java` 与 `Main.java`**：
   - `HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);`
   - 绑定各个路由上下文：`server.createContext("/api/students", new StudentHandler());` 等；
   - `server.setExecutor(Executors.newFixedThreadPool(10));`
   - `server.start();` 启动服务。

> **阶段自测标准（Check Point 5）**：
> - 运行 `Main.java`，控制台提示启动成功；
> - 打开 Chrome 浏览器或 Postman 访问：`http://localhost:8080/api/students`；
> - 屏幕上能清晰看到格式化完好的 JSON 字符串返回：`{"code":200,"message":"查询成功","data":{...}}`。此时后端 API 已全部成功就绪！

---

## 阶段 6：静态资源托管与前端 AI 生成联调（耗时约 30~45 分钟）

### 目标
把 AI 生成的 HTML 页面挂载到后端同一端口，实现完整可见的图形化交互界面。

### 具体执行步骤
1. **编写 `StaticFileHandler.java`**：
   - 监听根路径 `/`；
   - 从 `src/main/resources/static/` 读取对应的文件流（如 `index.html`）；
   - 根据请求后缀返回 `text/html; charset=UTF-8` 或 `text/css`；
   - 在 `AppHttpServer` 中注册：`server.createContext("/", new StaticFileHandler());`。
2. **利用 AI 生成 `index.html`**：
   - 直接使用文档第 6.2 节准备好的 **前端 AI 生成 Prompt** 发送给大模型；
   - 获取完整的单文件 `index.html`（内含 Bootstrap 5 CDN 样式与 Fetch API 逻辑）；
   - 将该文件保存至 `src/main/resources/static/index.html`。
3. **全链路端到端联调测试**：
   - 重启 `Main.java`；
   - 浏览器直接访问 `http://localhost:8080/`；
   - 测试点击流程：
     1. 学生列表是否正常分页展示；
     2. 点击“新增学生”，输入张三丰，刷新看是否入库；
     3. 切换至“成绩管理”，选择课程与学生，录入成绩 85 分；
     4. 切换至“统计看板”，查看该课程的平均分、及格率卡片是否动态更新。

> **阶段自测标准（Check Point 6）**：
> - 纯浏览器界面操作流畅无报错，控制台 Network 面板中所有 API 状态均为 200，新增/修改的数据即时在页面和数据库中呈现。

---

## 阶段 7：异常边界测试、代码优化与 GitHub 开源交付（耗时约 30 分钟）

### 目标
打磨项目细节，消除潜在 Bug，撰写精美的开源文档，发布至 GitHub。

### 具体执行步骤
1. **边界鲁棒性自测（故意找茬）**：
   - 录入成绩时输入 `150` 分或 `-5` 分，检查是否友好提示“分数必须在 0~100 之间”；
   - 新增已存在的学号，检查是否提示“学号已存在”；
   - 删除一个名下已有成绩的学生，检查外键拦截是否生效。
2. **代码重构与清理**：
   - 检查所有 DAO 层中的数据库连接，确保每一个 `Connection`、`PreparedStatement`、`ResultSet` 都在 `finally` 块中被安全关闭，防止连接泄露；
   - 清理所有临时的 `System.out.println`，代码格式化排版整齐。
3. **撰写高水准的 `README.md`**：
   - 项目简介与技术选型（突出纯原生 Java，适合学习与课设）；
   - 系统架构图与目录结构；
   - 本地 3 步快速运行指南（导入 SQL $ightarrow$ 改 db.properties $ightarrow$ 运行 Main）；
   - 截取 2~3 张运行成功的界面截图放在文档里；
   - 附上开源协议（MIT License）。
4. **Git 提交并推送**：
   - 创建 `.gitignore`（过滤 `.idea/`、`target/`、`*.iml`）；
   - 初始化本地仓库 `git init` $ightarrow$ 提交 `git commit` $ightarrow$ 关联 GitHub 远程仓库并 Push。

---

## 总结：每天推进 1~2 个阶段的心得

| 建议执行节奏 | 当日专注核心任务 | 验收标志 |
| :--- | :--- | :--- |
| **Day 1** | 阶段 0 + 阶段 1 + 阶段 2 | 数据库建好，`DBUtil` 测试连接通过 |
| **Day 2** | 阶段 3 + 阶段 4 | DAO 与 Service 逻辑写完，单测数据增删改查正常 |
| **Day 3** | 阶段 5 + 阶段 6 | 8080 服务打通，AI 前端接入，页面可操作 |
| **Day 4** | 阶段 7 | 边界自测、写 README、推送到 GitHub 开源 |

按照本执行路线，**不跨层、不越级，每一步都有明确的正向反馈与测试结果**，整个项目就能非常顺畅地自然成型！
