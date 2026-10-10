# 学生管理系统（Student Management System）系统设计与开发规范文档

> **项目定位**：计算机科学与技术本科课程设计级别（Java SE + MySQL + 原生 HTTP 服务）  
> **设计目标**：无 Spring Boot 等重型框架依赖、纯 Java 原生轻量化实现、三层架构解耦、代码结构规范清晰、适合 GitHub 开源分享及初学者学习参考。

---

## 目录
1. [系统总体架构设计](#1-系统总体架构设计)
   - 1.1 技术选型与设计原则
   - 1.2 系统分层架构（三层 MVC 模式）
   - 1.3 项目工程目录结构规范
   - 1.4 原生 HttpServer 与静态资源托管方案
2. [系统功能需求说明书](#2-系统功能需求说明书)
   - 2.1 功能模块划分
   - 2.2 学生信息管理业务用例与校验规则
   - 2.3 课程信息管理业务用例与校验规则
   - 2.4 学生成绩管理业务用例与统计规则
   - 2.5 统一异常处理与状态码规范
3. [数据库设计与 DDL 规范](#3-数据库设计与-ddl-规范)
   - 3.1 数据库全局设计规范
   - 3.2 数据表结构定义（列级详述）
   - 3.3 数据库完整 DDL 建表脚本
   - 3.4 初始测试数据 SQL 脚本
4. [Java 后端详细设计规范](#4-java-后端详细设计规范)
   - 4.1 包结构规划与职责划分
   - 4.2 实体类（Entity）与模型设计
   - 4.3 数据传输对象（DTO / VO）设计
   - 4.4 数据访问层（DAO）接口与实现类设计
   - 4.5 业务逻辑层（Service）接口与实现类设计
   - 4.6 控制层（Controller / Handler）与路由设计
   - 4.7 核心工具类（Util）设计
5. [系统 RESTful 接口文档（API Specification）](#5-系统-restful-接口文档api-specification)
   - 5.1 统一交互协议与数据包裹格式
   - 5.2 学生信息管理接口
   - 5.3 课程信息管理接口
   - 5.4 学生成绩管理接口
   - 5.5 成绩统计分析接口
6. [前端 AI 提示词与联调指南](#6-前端-ai-提示词与联调指南)
   - 6.1 前端单页应用（SPA）设计要求
   - 6.2 供 AI 直接生成前端页面的 Prompt 模板
   - 6.3 静态文件与前后端联调测试步骤

---

## 1. 系统总体架构设计

### 1.1 技术选型与设计原则

本项目摒弃了 Spring/Spring Boot、MyBatis 等复杂第三方框架，完全基于 **Java SE 标准类库** 与 **MySQL 关系型数据库** 构建，旨在夯实面向对象设计（OOP）、多线程网络编程、JDBC 底层操作与 HTTP 协议基础。

| 维度 | 选型技术 | 选型考量与说明 |
| :--- | :--- | :--- |
| **开发语言** | Java SE 8 或更高版本 (推荐 JDK 17) | 纯原生 Java，语法严谨，零框架开箱即用。 |
| **持久层数据库** | MySQL 8.0+ (支持 InnoDB 引擎) | 事务安全、成熟稳定、通用性强。 |
| **数据库连接** | 原生 JDBC (`mysql-connector-j:8.x`) | 深入理解 `Connection`、`PreparedStatement`、`ResultSet` 生命周期。 |
| **连接池管理** | 自定义简易连接池（基于 `BlockingQueue`）或原生直连包装 | 兼顾并发安全与资源复用，展示底层池化思想。 |
| **Web 服务器** | JDK 内置 `com.sun.net.httpserver.HttpServer` | 原生内置组件，监听本地 `8080` 端口，无需额外安装 Tomcat。 |
| **数据交换格式** | JSON (`application/json; charset=UTF-8`) | 前后端数据交互标准。可采用轻量 JSON 库（如 `Gson` 或手工简易工具）。 |
| **前端交互技术** | HTML5 + CSS3 + 原生 JavaScript (Fetch API) | 纯静态单页面或轻量前端，由 AI 快速生成，直接被后端服务器托管。 |

### 1.2 系统分层架构（三层 MVC 模式）

系统遵循软件工程中的**高内聚、低耦合**原则，划分为典型的三层架构：

```
+-------------------------------------------------------------------+
|                        Web 前端 (UI / HTML5)                       |
|               (由 AI 快速生成，通过 HTTP 8080 加载展示)               |
+---------------------------------+---------------------------------+
                                  | HTTP 请求 (GET / POST / PUT / DELETE)
                                  v
+-------------------------------------------------------------------+
|               Controller / Handler 控制层 (HTTP 路由与处理)         |
|      解析 Request 路径与 JSON 参数，调用 Service，封装统一统一响应结构       |
+---------------------------------+---------------------------------+
                                  | 业务接口调用 (Java Interface)
                                  v
+-------------------------------------------------------------------+
|                   Service 业务逻辑层 (业务规则校验与事务管理)         |
|      学号唯一性校验、外键依赖校验、及格率与平均分计算、事务控制管理           |
+---------------------------------+---------------------------------+
                                  | 数据操作调用 (Java Interface)
                                  v
+-------------------------------------------------------------------+
|                    DAO 数据访问层 (Data Access Object)             |
|       纯 JDBC 原生 SQL 拼接与 PreparedStatement 执行，实现实体映射       |
+---------------------------------+---------------------------------+
                                  | JDBC / SQL
                                  v
+-------------------------------------------------------------------+
|                       MySQL 8.0 物理存储层                         |
|                 (student / course / score 数据表)                  |
+-------------------------------------------------------------------+
```

### 1.3 项目工程目录结构规范

采用标准 Maven/IDE 通用工程布局，方便其他开发者 Clone 到本地后在 IntelliJ IDEA 或 Eclipse 中一键打开：

```text
student-management-system/
│
├── .gitignore                      # Git 忽略文件（忽略 .class、target、.idea 等）
├── README.md                       # 开源仓库说明书（安装运行指南）
├── pom.xml                         # Maven 依赖配置文件（仅引入 mysql-connector 与可选 gson）
│
├── sql/
│   ├── schema.sql                  # 数据库表结构初始化脚本
│   └── data.sql                    # 初始测试演示数据脚本
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── sms/
│   │   │           ├── Main.java               # 应用程序统一启动入口
│   │   │           │
│   │   │           ├── server/                 # HTTP 服务启动与路由配置
│   │   │           │   ├── AppHttpServer.java  # HttpServer 初始化与端口绑定
│   │   │           │   └── RouteDispatcher.java# 统一请求分发器
│   │   │           │
│   │   │           ├── controller/             # 请求控制层（Handler）
│   │   │           │   ├── BaseHttpHandler.java# 通用 HTTP 基类（封装 JSON 解析与输出）
│   │   │           │   ├── StudentHandler.java # 学生相关 HTTP 接口处理
│   │   │           │   ├── CourseHandler.java  # 课程相关 HTTP 接口处理
│   │   │           │   ├── ScoreHandler.java   # 成绩相关 HTTP 接口处理
│   │   │           │   └── StaticFileHandler.java # 本地 HTML/JS/CSS 静态托管处理
│   │   │           │
│   │   │           ├── service/                # 业务逻辑层接口
│   │   │           │   ├── StudentService.java
│   │   │           │   ├── CourseService.java
│   │   │           │   ├── ScoreService.java
│   │   │           │   └── impl/               # 业务逻辑层实现类
│   │   │           │       ├── StudentServiceImpl.java
│   │   │           │       ├── CourseServiceImpl.java
│   │   │           │       └── ScoreServiceImpl.java
│   │   │           │
│   │   │           ├── dao/                    # 数据访问层接口
│   │   │           │   ├── StudentDao.java
│   │   │           │   ├── CourseDao.java
│   │   │           │   ├── ScoreDao.java
│   │   │           │   └── impl/               # 数据访问层 JDBC 实现类
│   │   │           │       ├── StudentDaoImpl.java
│   │   │           │       ├── CourseDaoImpl.java
│   │   │           │       └── ScoreDaoImpl.java
│   │   │           │
│   │   │           ├── model/                  # 领域数据模型
│   │   │           │   ├── entity/             # 数据库映射实体对象 (POJO)
│   │   │           │   │   ├── Student.java
│   │   │           │   │   ├── Course.java
│   │   │           │   │   └── Score.java
│   │   │           │   ├── dto/                # 接收前端请求的查询与入参对象
│   │   │           │   │   ├── StudentQueryDTO.java
│   │   │           │   │   └── ScoreQueryDTO.java
│   │   │           │   └── vo/                 # 响应给前端的视图对象
│   │   │           │       ├── CommonResult.java   # 统一 API 响应包装
│   │   │           │       ├── PageResult.java     # 分页结果包装
│   │   │           │       ├── ScoreDetailVO.java  # 学生姓名+课程名称+成绩详情
│   │   │           │       └── ScoreStatsVO.java   # 平均分、最高分等统计 VO
│   │   │           │
│   │   │           └── util/                   # 基础设施与通用工具类
│   │   │               ├── DBUtil.java         # JDBC 连接管理与事务工具
│   │   │               ├── SimpleConnectionPool.java # 轻量化简易连接池实现
│   │   │               ├── JsonUtil.java       # JSON 序列化与反序列化工具
│   │   │               ├── HttpUtil.java       # HTTP 参数提取、路径解析工具
│   │   │               └── StringUtil.java     # 字符串判空与基础校验工具
│   │   │
│   │   └── resources/
│   │       ├── db.properties                   # 数据库连接参数配置文件
│   │       └── static/                         # 前端静态 Web 页面目录
│   │           ├── index.html                  # 主界面（学生/成绩管理 SPA）
│   │           ├── css/
│   │           │   └── style.css               # 样式表
│   │           └── js/
│   │               └── app.js                  # 前端请求逻辑
```

### 1.4 原生 HttpServer 与静态资源托管方案

系统无需额外安装外置 Tomcat 或部署 WAR 包，通过 Java 原生 `com.sun.net.httpserver.HttpServer` 在 `Main.java` 中创建单机 HTTP 监听器：
1. **监听端口**：`localhost:8080`；
2. **线程池配置**：设置 `server.setExecutor(Executors.newFixedThreadPool(10))` 实现多线程并发处理 HTTP 请求；
3. **路由分发机制**：
   - 静态路由上下文：绑定 `/`，映射至 `StaticFileHandler`。当访问 `http://localhost:8080/` 时，读取 `static/index.html` 并回写 `Content-Type: text/html; charset=UTF-8`；
   - API 数据路由上下文：绑定 `/api/students`、`/api/courses`、`/api/scores` 等路由，分配至对应的 Handler。

---

## 2. 系统功能需求说明书

### 2.1 功能模块划分

系统主要涵盖两大核心业务模块与一个辅助支撑模块：
1. **学生信息管理模块（Student Info Module）**：学生档案生命周期管理；
2. **学生成绩管理模块（Student Score Module）**：各门课程的分数录入、维护与报表统计；
3. **课程信息支撑模块（Course Info Module）**：为成绩录入提供标准的课程元数据。

### 2.2 学生信息管理业务用例与校验规则

| 功能点 | 业务规则与数据校验 | 异常处理分支 |
| :--- | :--- | :--- |
| **新增学生** | 1. 学号（`student_no`）必填，全局唯一。<br>2. 姓名（`name`）必填，长度 1-50 字符。<br>3. 性别（`gender`）取值 [1-男, 2-女, 0-未知]。<br>4. 联系电话与邮箱符合正则格式校验。 | 若学号已存在，中断并返回错误码 `409 (Conflict)`；若必填项缺失，返回 `400 (Bad Request)`。 |
| **修改学生** | 1. 根据主键 `id` 修改个人基本信息（姓名、联系方式、专业、班级、状态）。<br>2. 不允许直接修改唯一学号 `student_no`（避免成绩外键关联产生孤儿数据）。 | 若指定 `id` 不存在，返回 `404 (Not Found)`。 |
| **删除学生** | 1. 根据主键 `id` 删除学生档案。<br>2. **级联安全规则**：删除前需检查该学生是否已有成绩记录。 | 若存在关联成绩，阻断删除并提示“该学生已有考试成绩，请先清理成绩记录或注销学籍”；或事务内联动删除其所有成绩。 |
| **学生列表与分页查询** | 1. 支持按学生姓名模糊查询、按学号精确查询、按专业/班级筛选。<br>2. 支持分页参数：`page`（从 1 开始，默认 1），`pageSize`（每页条数，默认 10）。 | 若无符合条件记录，返回空列表 `[]` 与总数 `total: 0`。 |
| **学生详情查询** | 根据主键 `id` 获取学生完整档案详情。 | 若记录不存在，返回 `404 (Not Found)`。 |

### 2.3 课程信息管理业务用例与校验规则

1. **课程编号（`course_no`）**：必填且唯一（如 `CS101`）；
2. **课程名称（`name`）**：必填，长度 1-100；
3. **学分（`credit`）**：浮点型，范围必须在 `0.5 ~ 10.0` 之间；
4. **删除校验**：若课程已被录入过成绩，禁止物理删除，防止破坏历史成绩数据完整性。

### 2.4 学生成绩管理业务用例与统计规则

| 功能点 | 业务规则与数据校验 | 异常处理分支 |
| :--- | :--- | :--- |
| **成绩录入** | 1. 关联的学生 `student_id` 与课程 `course_id` 必须真实有效。<br>2. **同一名学生在同一门课程下只能有一条有效成绩记录**（复合唯一索引）。<br>3. 成绩分数 `score` 取值区间为 `[0.00, 100.00]`。 | 若重复录入，返回 `409 (该学生本门课程成绩已存在，请走修改流程)`；若学生或课程不存在，返回 `404`。 |
| **成绩修改** | 根据成绩主键 `id` 修改考试得分 `score` 与考试备注。 | 若分数不在 0~100 范围内，返回 `400` 参数错误。 |
| **成绩删除** | 根据成绩主键 `id` 删除成绩记录。 | 若记录不存在，返回 `404`。 |
| **综合多条件查询** | 支持通过学生姓名、学号、课程名称、分数区间（`minScore` ~ `maxScore`）联合分页检索。查询结果包含学生与课程的关联字段。 | 动态拼接 SQL，防范 SQL 注入（必须使用 `PreparedStatement` 占位符）。 |
| **成绩统计分析** | 针对指定课程（或全部课程）计算：<br>1. 参加考试总人数（`totalStudents`）；<br>2. 平均分（`averageScore`，保留 2 位小数）；<br>3. 最高分（`maxScore`）与最低分（`minScore`）；<br>4. 及格率（`passRate`，得分 >= 60 的比例百分比）。 | 若指定课程暂无成绩，统计结果指标全置为 0，不抛出空指针异常。 |

### 2.5 统一异常处理与状态码规范

系统向前端返回标准统一的 JSON 数据结构，包含业务状态码（`code`）、提示消息（`message`）与数据载荷（`data`）：

| 状态码 (`code`) | 英文定义 | 中文语义说明 | 典型触发场景 |
| :--- | :--- | :--- | :--- |
| **200** | SUCCESS | 请求执行成功 | 增删改查正常完成 |
| **400** | BAD_REQUEST | 请求参数格式或范围错误 | 必填项为空、分数超出 0~100、参数类型转换失败 |
| **404** | NOT_FOUND | 请求的资源不存在 | 查询或操作不存在的学生 ID、成绩 ID |
| **409** | CONFLICT | 业务状态冲突/唯一性冲突 | 学号重复、同课程重复录入成绩 |
| **500** | INTERNAL_SERVER_ERROR | 服务器内部异常 | 数据库连接丢失、SQL 语法错误、空指针异常 |

---

## 3. 数据库设计与 DDL 规范

### 3.1 数据库全局设计规范
- **数据库名**：`student_db`
- **存储引擎**：`InnoDB`（支持事务处理、行级锁与外键约束）
- **默认字符集**：`utf8mb4`（完全支持中文生僻字及特殊符号）
- **字符校验规则**：`utf8mb4_unicode_ci`
- **命名规范**：表名、字段名全小写，多单词以下划线 `_` 分隔。

### 3.2 数据表结构定义（列级详述）

#### 表 1：学生信息表（`student`）
存储学生个人档案基本属性。

| 字段名称 | 物理数据类型 | 主键/索引 | 允许为空 | 默认值 | 业务含义与注释 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | `BIGINT UNSIGNED` | PRIMARY KEY | NO | 自增 (AUTO_INCREMENT) | 学生物理主键 ID |
| `student_no` | `VARCHAR(32)` | UNIQUE KEY (`uk_student_no`) | NO | 无 | 学号（业务唯一标识，如 202401001） |
| `name` | `VARCHAR(50)` | INDEX (`idx_name`) | NO | 无 | 学生真实姓名 |
| `gender` | `TINYINT` | 无 | NO | `1` | 性别：`1`-男, `2`-女, `0`-未知 |
| `birth_date` | `DATE` | 无 | YES | NULL | 出生日期（YYYY-MM-DD） |
| `phone` | `VARCHAR(20)` | 无 | YES | NULL | 联系电话 |
| `email` | `VARCHAR(100)` | 无 | YES | NULL | 电子邮箱 |
| `major` | `VARCHAR(100)` | INDEX (`idx_major`) | NO | 无 | 所属专业（如：计算机科学与技术） |
| `class_name` | `VARCHAR(50)` | 无 | NO | 无 | 班级名称（如：计科2401班） |
| `status` | `TINYINT` | 无 | NO | `1` | 学籍状态：`1`-在读, `2`-休学, `3`-退学, `4`-毕业 |
| `created_at` | `DATETIME` | 无 | NO | `CURRENT_TIMESTAMP` | 记录创建时间 |
| `updated_at` | `DATETIME` | 无 | NO | `CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 记录最新修改时间 |

#### 表 2：课程信息表（`course`）
存储课程元数据，为选课及成绩提供课程支持。

| 字段名称 | 物理数据类型 | 主键/索引 | 允许为空 | 默认值 | 业务含义与注释 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | `BIGINT UNSIGNED` | PRIMARY KEY | NO | 自增 (AUTO_INCREMENT) | 课程物理主键 ID |
| `course_no` | `VARCHAR(32)` | UNIQUE KEY (`uk_course_no`) | NO | 无 | 课程编号（如 CS101） |
| `name` | `VARCHAR(100)` | 无 | NO | 无 | 课程名称（如 Java程序设计） |
| `credit` | `DECIMAL(3,1)` | 无 | NO | `1.0` | 课程学分（如 3.0） |
| `teacher` | `VARCHAR(50)` | 无 | YES | NULL | 任课教师姓名 |
| `created_at` | `DATETIME` | 无 | NO | `CURRENT_TIMESTAMP` | 记录创建时间 |
| `updated_at` | `DATETIME` | 无 | NO | `CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 记录最新修改时间 |

#### 表 3：学生成绩表（`score`）
记录各学生对应课程的考核成绩。

| 字段名称 | 物理数据类型 | 主键/索引 | 允许为空 | 默认值 | 业务含义与注释 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | `BIGINT UNSIGNED` | PRIMARY KEY | NO | 自增 (AUTO_INCREMENT) | 成绩物理主键 ID |
| `student_id` | `BIGINT UNSIGNED` | INDEX (`idx_student_id`) | NO | 无 | 关联学生表主键 `student.id` |
| `course_id` | `BIGINT UNSIGNED` | INDEX (`idx_course_id`) | NO | 无 | 关联课程表主键 `course.id` |
| `score` | `DECIMAL(5,2)` | 无 | NO | `0.00` | 考试得分（0.00 ~ 100.00） |
| `exam_date` | `DATE` | 无 | YES | NULL | 考核日期 |
| `remarks` | `VARCHAR(255)` | 无 | YES | NULL | 成绩备注说明（如补考、重修等） |
| `created_at` | `DATETIME` | 无 | NO | `CURRENT_TIMESTAMP` | 记录创建时间 |
| `updated_at` | `DATETIME` | 无 | NO | `CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 记录最新修改时间 |

- **复合唯一约束**：`UNIQUE KEY uk_student_course (student_id, course_id)`，确保单名学生同一门课程不可重复录入多条成绩。
- **外键约束考量**：在课程设计规范中，推荐通过数据库外键或业务代码层维护关联；为保证数据严谨性，在 DDL 中建立物理外键约束：
  - `fk_score_student`: `FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE`
  - `fk_score_course`: `FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE RESTRICT`

### 3.3 数据库完整 DDL 建表脚本

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS `student_db` 
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_unicode_ci;

USE `student_db`;

-- 1. 创建学生信息表
DROP TABLE IF EXISTS `score`;
DROP TABLE IF EXISTS `course`;
DROP TABLE IF EXISTS `student`;

CREATE TABLE `student` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '学生物理主键 ID',
  `student_no` VARCHAR(32) NOT NULL COMMENT '学号（业务唯一）',
  `name` VARCHAR(50) NOT NULL COMMENT '学生真实姓名',
  `gender` TINYINT NOT NULL DEFAULT 1 COMMENT '性别: 1-男, 2-女, 0-未知',
  `birth_date` DATE DEFAULT NULL COMMENT '出生日期',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '电子邮箱',
  `major` VARCHAR(100) NOT NULL COMMENT '所属专业',
  `class_name` VARCHAR(50) NOT NULL COMMENT '班级名称',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '学籍状态: 1-在读, 2-休学, 3-退学, 4-毕业',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_no` (`student_no`),
  KEY `idx_name` (`name`),
  KEY `idx_major` (`major`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生信息档案表';

-- 2. 创建课程信息表
CREATE TABLE `course` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '课程物理主键 ID',
  `course_no` VARCHAR(32) NOT NULL COMMENT '课程编号（业务唯一）',
  `name` VARCHAR(100) NOT NULL COMMENT '课程名称',
  `credit` DECIMAL(3,1) NOT NULL DEFAULT 1.0 COMMENT '课程学分',
  `teacher` VARCHAR(50) DEFAULT NULL COMMENT '任课教师姓名',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_no` (`course_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程信息表';

-- 3. 创建学生成绩表
CREATE TABLE `score` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '成绩主键 ID',
  `student_id` BIGINT UNSIGNED NOT NULL COMMENT '关联学生表主键 ID',
  `course_id` BIGINT UNSIGNED NOT NULL COMMENT '关联课程表主键 ID',
  `score` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '考试成绩（0.00 ~ 100.00）',
  `exam_date` DATE DEFAULT NULL COMMENT '考核日期',
  `remarks` VARCHAR(255) DEFAULT NULL COMMENT '成绩备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_course` (`student_id`, `course_id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_course_id` (`course_id`),
  CONSTRAINT `fk_score_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_score_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生课程成绩表';
```

### 3.4 初始测试数据 SQL 脚本

```sql
-- 插入测试学生数据
INSERT INTO `student` (`student_no`, `name`, `gender`, `birth_date`, `phone`, `email`, `major`, `class_name`, `status`) VALUES
('20240101', '张三', 1, '2004-03-15', '13800000001', 'zhangsan@campus.edu.cn', '计算机科学与技术', '计科2401班', 1),
('20240102', '李四', 1, '2004-06-20', '13800000002', 'lisi@campus.edu.cn', '计算机科学与技术', '计科2401班', 1),
('20240103', '王芳', 2, '2004-09-10', '13800000003', 'wangfang@campus.edu.cn', '软件工程', '软工2402班', 1),
('20240104', '赵雷', 1, '2003-11-25', '13800000004', 'zhaolei@campus.edu.cn', '人工智能', '智科2401班', 1),
('20240105', '刘敏', 2, '2004-01-08', '13800000005', 'liumin@campus.edu.cn', '软件工程', '软工2401班', 1);

-- 插入测试课程数据
INSERT INTO `course` (`course_no`, `name`, `credit`, `teacher`) VALUES
('CS101', 'Java语言程序设计', 3.5, '高老师'),
('CS102', '关系型数据库系统概论', 3.0, '陈老师'),
('CS103', '数据结构与算法分析', 4.0, '林老师'),
('CS104', '计算机网络', 3.0, '黄老师');

-- 插入测试成绩数据
INSERT INTO `score` (`student_id`, `course_id`, `score`, `exam_date`, `remarks`) VALUES
(1, 1, 88.50, '2026-06-15', '期末正考'),
(1, 2, 92.00, '2026-06-18', '期末正考'),
(2, 1, 58.00, '2026-06-15', '期末正考不及格'),
(2, 2, 75.50, '2026-06-18', '期末正考'),
(3, 1, 95.00, '2026-06-15', '期末正考优秀'),
(3, 3, 89.00, '2026-06-22', '期末正考'),
(4, 1, 62.50, '2026-06-15', '期末正考');
```

---

## 4. Java 后端详细设计规范

### 4.1 包结构规划与职责划分

```text
com.sms
├── Main.java               # 进程主启动入口
├── server                  # HTTP 监听与路由分派核心
├── controller              # 协议接入与控制器层
├── service                 # 业务逻辑接口层
│   └── impl                # 业务逻辑实现层
├── dao                     # 数据持久化接口层
│   └── impl                # JDBC 数据持久化实现层
├── model                   # 实体与数据承载模型
│   ├── entity              # PO 表映射实体
│   ├── dto                 # 入参传输对象
│   └── vo                  # 接口返回视图对象
└── util                    # 基础连接、响应、解析工具库
```

---

### 4.2 实体类（Entity）设计

#### 1. `com.sms.model.entity.Student`
对应表 `student`。

```java
package com.sms.model.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Student {
    // 成员属性
    private Long id;                  // 物理主键 ID
    private String studentNo;         // 学号
    private String name;              // 姓名
    private Integer gender;           // 性别 (1-男, 2-女, 0-未知)
    private LocalDate birthDate;      // 出生日期
    private String phone;             // 手机号码
    private String email;             // 邮箱
    private String major;             // 专业
    private String className;         // 班级
    private Integer status;           // 学籍状态 (1-在读, 2-休学, 3-退学, 4-毕业)
    private LocalDateTime createdAt;  // 创建时间
    private LocalDateTime updatedAt;  // 更新时间

    // 构造方法
    public Student() {}
    public Student(Long id, String studentNo, String name, Integer gender, 
                   LocalDate birthDate, String phone, String email, 
                   String major, String className, Integer status) {
        // 全参构造赋值
    }

    // Getter & Setter 方法（所有字段一一对应规范声明）
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Student{" + "id=" + id + ", studentNo='" + studentNo + ''' + ", name='" + name + ''' + '}';
    }
}
```

#### 2. `com.sms.model.entity.Course`
对应表 `course`。

```java
package com.sms.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Course {
    private Long id;                  // 课程主键 ID
    private String courseNo;          // 课程编号
    private String name;              // 课程名称
    private BigDecimal credit;        // 学分 (例如 3.0)
    private String teacher;           // 任课教师
    private LocalDateTime createdAt;  // 创建时间
    private LocalDateTime updatedAt;  // 更新时间

    public Course() {}
    public Course(Long id, String courseNo, String name, BigDecimal credit, String teacher) {
        this.id = id;
        this.courseNo = courseNo;
        this.name = name;
        this.credit = credit;
        this.teacher = teacher;
    }

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCourseNo() { return courseNo; }
    public void setCourseNo(String courseNo) { this.courseNo = courseNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getCredit() { return credit; }
    public void setCredit(BigDecimal credit) { this.credit = credit; }
    public String getTeacher() { return teacher; }
    public void setTeacher(String teacher) { this.teacher = teacher; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

#### 3. `com.sms.model.entity.Score`
对应表 `score`。

```java
package com.sms.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Score {
    private Long id;                  // 成绩主键 ID
    private Long studentId;           // 关联学生 ID
    private Long courseId;            // 关联课程 ID
    private BigDecimal score;         // 成绩得分
    private LocalDate examDate;       // 考试日期
    private String remarks;           // 备注
    private LocalDateTime createdAt;  // 创建时间
    private LocalDateTime updatedAt;  // 更新时间

    public Score() {}
    public Score(Long id, Long studentId, Long courseId, BigDecimal score, LocalDate examDate, String remarks) {
        this.id = id;
        this.studentId = studentId;
        this.courseId = courseId;
        this.score = score;
        this.examDate = examDate;
        this.remarks = remarks;
    }

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

---

### 4.3 数据传输对象（DTO / VO）设计

#### 1. 通用统一响应包装：`com.sms.model.vo.CommonResult<T>`
```java
package com.sms.model.vo;

public class CommonResult<T> {
    private int code;       // 状态码：200 成功，400/404/409/500 等异常
    private String message; // 提示信息
    private T data;         // 数据载荷

    public CommonResult() {}
    public CommonResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 静态便捷构建工厂方法
    public static <T> CommonResult<T> success(T data) {
        return new CommonResult<>(200, "操作成功", data);
    }
    public static <T> CommonResult<T> success(String message, T data) {
        return new CommonResult<>(200, message, data);
    }
    public static <T> CommonResult<T> error(int code, String message) {
        return new CommonResult<>(code, message, null);
    }

    // Getter & Setter
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
```

#### 2. 通用分页数据包装：`com.sms.model.vo.PageResult<T>`
```java
package com.sms.model.vo;

import java.util.List;

public class PageResult<T> {
    private long total;       // 总记录条数
    private int page;         // 当前页码 (从 1 开始)
    private int pageSize;     // 每页条数
    private List<T> list;     // 当前页数据集合

    public PageResult() {}
    public PageResult(long total, int page, int pageSize, List<T> list) {
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.list = list;
    }

    // Getter & Setter
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public List<T> getList() { return list; }
    public void setList(List<T> list) { this.list = list; }
}
```

#### 3. 成绩多表联查视图对象：`com.sms.model.vo.ScoreDetailVO`
将学生、课程与成绩信息拼接输出至前端展示：
```java
package com.sms.model.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ScoreDetailVO {
    private Long id;              // 成绩记录 ID
    private Long studentId;       // 学生 ID
    private String studentNo;     // 学号
    private String studentName;   // 学生姓名
    private String className;     // 班级
    private Long courseId;        // 课程 ID
    private String courseNo;      // 课程编号
    private String courseName;    // 课程名称
    private BigDecimal credit;    // 学分
    private BigDecimal score;     // 成绩
    private LocalDate examDate;   // 考试日期
    private String remarks;       // 备注

    // Getter & Setter...
}
```

#### 4. 成绩统计视图对象：`com.sms.model.vo.ScoreStatsVO`
```java
package com.sms.model.vo;

import java.math.BigDecimal;

public class ScoreStatsVO {
    private Long courseId;            // 统计课程 ID（若统计全局则为 null）
    private String courseName;        // 课程名称
    private int totalStudents;        // 参加考试总人数
    private BigDecimal averageScore;  // 平均分
    private BigDecimal maxScore;      // 最高分
    private BigDecimal minScore;      // 最低分
    private BigDecimal passRate;      // 及格率 (百分比，如 85.50 表示 85.5%)

    // Getter & Setter...
}
```

#### 5. 查询参数传输对象：`StudentQueryDTO` 与 `ScoreQueryDTO`
```java
package com.sms.model.dto;

public class StudentQueryDTO {
    private String keyword;    // 综合关键字（姓名或学号模糊匹配）
    private String major;      // 专业精准筛选
    private String className;  // 班级精准筛选
    private Integer status;    // 学籍状态
    private int page = 1;      // 当前页（默认 1）
    private int pageSize = 10; // 每页条数（默认 10）
    // Getter & Setter...
}

public class ScoreQueryDTO {
    private Long studentId;       // 学生 ID 筛选
    private Long courseId;        // 课程 ID 筛选
    private String studentName;   // 学生姓名模糊搜索
    private Double minScore;      // 最低分限制
    private Double maxScore;      // 最高分限制
    private int page = 1;
    private int pageSize = 10;
    // Getter & Setter...
}
```

---

### 4.4 数据访问层（DAO）设计

DAO 层遵循接口驱动开发，严格使用 `PreparedStatement` 避免 SQL 注入。

#### 1. `com.sms.dao.StudentDao` 接口
```java
package com.sms.dao;

import com.sms.model.dto.StudentQueryDTO;
import com.sms.model.entity.Student;
import java.sql.SQLException;
import java.util.List;

public interface StudentDao {
    // 根据主键 ID 查询单条学生信息
    Student findById(Long id) throws SQLException;

    // 根据唯一学号查询学生信息
    Student findByStudentNo(String studentNo) throws SQLException;

    // 新增学生，返回插入后自增生成的物理主键 ID
    Long insert(Student student) throws SQLException;

    // 根据主键 ID 更新学生信息，返回受影响的行数
    int update(Student student) throws SQLException;

    // 根据主键 ID 删除学生，返回受影响的行数
    int deleteById(Long id) throws SQLException;

    // 根据综合多条件分页查询学生列表
    List<Student> findByPage(StudentQueryDTO queryDTO) throws SQLException;

    // 根据综合多条件统计匹配的学生总记录数
    long countByCondition(StudentQueryDTO queryDTO) throws SQLException;
}
```

#### 2. `com.sms.dao.CourseDao` 接口
```java
package com.sms.dao;

import com.sms.model.entity.Course;
import java.sql.SQLException;
import java.util.List;

public interface CourseDao {
    Course findById(Long id) throws SQLException;
    Course findByCourseNo(String courseNo) throws SQLException;
    Long insert(Course course) throws SQLException;
    int update(Course course) throws SQLException;
    int deleteById(Long id) throws SQLException;
    List<Course> findAll() throws SQLException;
}
```

#### 3. `com.sms.dao.ScoreDao` 接口
```java
package com.sms.dao;

import com.sms.model.dto.ScoreQueryDTO;
import com.sms.model.entity.Score;
import com.sms.model.vo.ScoreDetailVO;
import com.sms.model.vo.ScoreStatsVO;
import java.sql.SQLException;
import java.util.List;

public interface ScoreDao {
    Score findById(Long id) throws SQLException;
    
    // 校验特定学生是否已录入某门课程成绩
    Score findByStudentAndCourse(Long studentId, Long courseId) throws SQLException;

    Long insert(Score score) throws SQLException;
    int update(Score score) throws SQLException;
    int deleteById(Long id) throws SQLException;

    // 统计某个学生名下拥有的成绩记录总数（用于删除学生前的外键依赖安全检查）
    long countByStudentId(Long studentId) throws SQLException;

    // 统计某门课程关联的成绩总数（用于删除课程前的安全检查）
    long countByCourseId(Long courseId) throws SQLException;

    // 多表联查（JOIN student 与 course）分页获取成绩详情
    List<ScoreDetailVO> findDetailByPage(ScoreQueryDTO queryDTO) throws SQLException;
    long countDetailByCondition(ScoreQueryDTO queryDTO) throws SQLException;

    // 针对指定课程（或全部）进行聚合统计（AVG, MAX, MIN, 及格人数）
    ScoreStatsVO calculateStatistics(Long courseId) throws SQLException;
}
```

---

### 4.5 业务逻辑层（Service）设计

Service 负责参数完整性前置校验、业务逻辑合法性研判与事务管控。

#### 1. `com.sms.service.StudentService` 接口
```java
package com.sms.service;

import com.sms.model.dto.StudentQueryDTO;
import com.sms.model.entity.Student;
import com.sms.model.vo.PageResult;

public interface StudentService {
    // 获取学生详情
    Student getStudentById(Long id);

    // 新增学生档案（内置学号重复校验）
    void addStudent(Student student);

    // 修改学生档案（不允许修改 studentNo）
    void updateStudent(Student student);

    // 删除学生（安全检查：若已有成绩记录则拦截或支持事务联动）
    void deleteStudent(Long id);

    // 条件分页查询学生
    PageResult<Student> queryStudents(StudentQueryDTO queryDTO);
}
```

#### 2. `com.sms.service.CourseService` 接口
```java
package com.sms.service;

import com.sms.model.entity.Course;
import java.util.List;

public interface CourseService {
    List<Course> listAllCourses();
    Course getCourseById(Long id);
    void addCourse(Course course);
    void updateCourse(Course course);
    void deleteCourse(Long id);
}
```

#### 3. `com.sms.service.ScoreService` 接口
```java
package com.sms.service;

import com.sms.model.dto.ScoreQueryDTO;
import com.sms.model.entity.Score;
import com.sms.model.vo.PageResult;
import com.sms.model.vo.ScoreDetailVO;
import com.sms.model.vo.ScoreStatsVO;

public interface ScoreService {
    Score getScoreById(Long id);

    // 录入成绩（校验学生、课程存在性，校验 0~100 分数区间，防重复录入）
    void addScore(Score score);

    // 更新成绩得分与备注
    void updateScore(Score score);

    // 删除成绩
    void deleteScore(Long id);

    // 分页查询联合明细
    PageResult<ScoreDetailVO> queryScoreDetails(ScoreQueryDTO queryDTO);

    // 获取成绩统计信息
    ScoreStatsVO getScoreStatistics(Long courseId);
}
```

---

### 4.6 控制层（Controller / Handler）与路由设计

#### 1. 通用 HTTP 基类 `com.sms.controller.BaseHttpHandler`
负责统一封装 JSON 请求体读取、URL 查询参数解析、跨域 CORS 头设置与 JSON 响应回写。

```java
package com.sms.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public abstract class BaseHttpHandler implements HttpHandler {

    // 统一处理跨域（CORS）与请求分发模板方法
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 设置通用 CORS 响应头，允许本地前端直接 Fetch
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            processRequest(exchange);
        } catch (IllegalArgumentException e) {
            sendJsonResponse(exchange, 400, "{"code":400,"message":"" + e.getMessage() + "","data":null}");
        } catch (Exception e) {
            e.printStackTrace();
            sendJsonResponse(exchange, 500, "{"code":500,"message":"服务器内部错误：" + e.getMessage() + "","data":null}");
        }
    }

    // 由各子类实现具体的 REST 路由分发
    protected abstract void processRequest(HttpExchange exchange) throws Exception;

    // 工具方法：读取 HTTP 请求体为 String
    protected String readRequestBody(HttpExchange exchange) throws IOException {
        // 读取 exchange.getRequestBody() 并转为 UTF-8 字符串
    }

    // 工具方法：解析 URL Query 参数为 Map<String, String>
    protected Map<String, String> parseQueryParams(HttpExchange exchange) {
        // 从 exchange.getRequestURI().getQuery() 解析 key=value
    }

    // 工具方法：从路径抽取末尾 ID（例如 /api/students/5 -> 5）
    protected Long extractPathId(HttpExchange exchange, String basePath) {
        // 截取路径并转换为 Long
    }

    // 核心输出：向客户端发送 JSON 响应
    protected void sendJsonResponse(HttpExchange exchange, int httpStatus, String jsonString) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = jsonString.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(httpStatus, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
            os.flush();
        }
    }
}
```

#### 2. 各业务控制器类与方法声明
- `StudentHandler`：继承 `BaseHttpHandler`，匹配 `exchange.getRequestMethod()` 分别路由到 `doList()`、`doGet()`、`doCreate()`、`doUpdate()`、`doDelete()`；
- `CourseHandler`：实现对课程元数据的增删改查；
- `ScoreHandler`：实现成绩的录入、维护、联合分页与统计报表触发；
- `StaticFileHandler`：监听根路径 `/`，自动读取类路径下的 `static/index.html`、`style.css`、`app.js` 等并根据文件后缀返回 `text/html`、`text/css`、`application/javascript` 响应。

---

### 4.7 核心工具类（Util）设计

#### 1. `com.sms.util.DBUtil`
负责管理 JDBC 驱动加载、数据库连接获取与释放、事务控制。

```java
package com.sms.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public class DBUtil {
    private static String url;
    private static String username;
    private static String password;

    // 静态代码块加载 resources/db.properties 配置文件
    static {
        try (InputStream in = DBUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            Properties props = new Properties();
            props.load(in);
            url = props.getProperty("jdbc.url");
            username = props.getProperty("jdbc.username");
            password = props.getProperty("jdbc.password");
            Class.forName(props.getProperty("jdbc.driverClassName"));
        } catch (Exception e) {
            throw new RuntimeException("初始化数据库驱动或配置失败", e);
        }
    }

    // 获取原生数据库连接
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    // 安全关闭 JDBC 资源
    public static void close(ResultSet rs, PreparedStatement ps, Connection conn) {
        if (rs != null) { try { rs.close(); } catch (SQLException ignored) {} }
        if (ps != null) { try { ps.close(); } catch (SQLException ignored) {} }
        if (conn != null) { try { conn.close(); } catch (SQLException ignored) {} }
    }
}
```

#### 2. `com.sms.util.JsonUtil`
负责对象与 JSON 字符串的双向转换。可封装 `com.google.gson.Gson` 或纯手工简易序列化，对外暴露统一静态方法：
- `public static String toJson(Object obj)`
- `public static <T> T fromJson(String json, Class<T> clazz)`

---

## 5. 系统 RESTful 接口文档（API Specification）

### 5.1 统一交互协议与数据包裹格式

所有数据接口均使用 JSON 进行数据交互。

**标准成功响应示例 (HTTP 200)**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

**标准失败响应示例 (HTTP 400 / 404 / 409 / 500)**：
```json
{
  "code": 409,
  "message": "学号 20240101 已存在，请勿重复添加",
  "data": null
}
```

---

### 5.2 学生信息管理接口

#### 接口 1：分页条件检索学生列表
- **请求方法**：`GET`
- **请求 URL**：`/api/students`
- **请求 Query 参数**：
  | 参数名 | 类型 | 必填 | 默认值 | 示例值 | 说明 |
  | :--- | :--- | :--- | :--- | :--- | :--- |
  | `page` | Integer | 否 | 1 | 1 | 当前页码 |
  | `pageSize` | Integer | 否 | 10 | 10 | 每页显示条数 |
  | `keyword` | String | 否 | 无 | 张 | 姓名或学号模糊匹配 |
  | `major` | String | 否 | 无 | 计算机科学与技术 | 专业筛选 |
  | `className`| String | 否 | 无 | 计科2401班 | 班级筛选 |

- **成功响应示例 (HTTP 200)**：
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 1,
    "page": 1,
    "pageSize": 10,
    "list": [
      {
        "id": 1,
        "studentNo": "20240101",
        "name": "张三",
        "gender": 1,
        "birthDate": "2004-03-15",
        "phone": "13800000001",
        "email": "zhangsan@campus.edu.cn",
        "major": "计算机科学与技术",
        "className": "计科2401班",
        "status": 1,
        "createdAt": "2026-10-09 21:00:00"
      }
    ]
  }
}
```

---

#### 接口 2：获取学生个人详细档案
- **请求方法**：`GET`
- **请求 URL**：`/api/students/{id}`（例如 `/api/students/1`）
- **路径参数**：`id` (Long, 必填) - 学生主键 ID
- **成功响应示例 (HTTP 200)**：
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "id": 1,
    "studentNo": "20240101",
    "name": "张三",
    "gender": 1,
    "birthDate": "2004-03-15",
    "phone": "13800000001",
    "email": "zhangsan@campus.edu.cn",
    "major": "计算机科学与技术",
    "className": "计科2401班",
    "status": 1
  }
}
```

---

#### 接口 3：新增学生档案
- **请求方法**：`POST`
- **请求 URL**：`/api/students`
- **请求头**：`Content-Type: application/json`
- **请求 Body (JSON)**：
```json
{
  "studentNo": "20240106",
  "name": "孙立",
  "gender": 1,
  "birthDate": "2004-05-12",
  "phone": "13911112222",
  "email": "sunli@campus.edu.cn",
  "major": "物联网工程",
  "className": "物联2401班",
  "status": 1
}
```
- **成功响应示例 (HTTP 200)**：
```json
{
  "code": 200,
  "message": "学生添加成功",
  "data": {
    "id": 6
  }
}
```

---

#### 接口 4：更新学生基本信息
- **请求方法**：`PUT`
- **请求 URL**：`/api/students/{id}`（例如 `/api/students/1`）
- **请求 Body (JSON)**：
```json
{
  "name": "张三丰",
  "gender": 1,
  "birthDate": "2004-03-15",
  "phone": "13899998888",
  "email": "zhangsanfeng@campus.edu.cn",
  "major": "计算机科学与技术",
  "className": "计科2401班",
  "status": 1
}
```
- **成功响应示例 (HTTP 200)**：
```json
{
  "code": 200,
  "message": "学生档案修改成功",
  "data": true
}
```

---

#### 接口 5：删除指定学生档案
- **请求方法**：`DELETE`
- **请求 URL**：`/api/students/{id}`（例如 `/api/students/6`）
- **成功响应示例 (HTTP 200)**：
```json
{
  "code": 200,
  "message": "学生删除成功",
  "data": true
}
```

---

### 5.3 课程信息管理接口

#### 接口 1：获取全部课程下拉列表
- **请求方法**：`GET`
- **请求 URL**：`/api/courses`
- **成功响应示例 (HTTP 200)**：
```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "id": 1,
      "courseNo": "CS101",
      "name": "Java语言程序设计",
      "credit": 3.5,
      "teacher": "高老师"
    },
    {
      "id": 2,
      "courseNo": "CS102",
      "name": "关系型数据库系统概论",
      "credit": 3.0,
      "teacher": "陈老师"
    }
  ]
}
```

---

### 5.4 学生成绩管理接口

#### 接口 1：分页多条件查询学生成绩明细
- **请求方法**：`GET`
- **请求 URL**：`/api/scores`
- **请求 Query 参数**：
  | 参数名 | 类型 | 必填 | 默认值 | 示例值 | 说明 |
  | :--- | :--- | :--- | :--- | :--- | :--- |
  | `page` | Integer | 否 | 1 | 1 | 当前页码 |
  | `pageSize` | Integer | 否 | 10 | 10 | 每页条数 |
  | `studentId` | Long | 否 | 无 | 1 | 指定学生物理 ID |
  | `courseId` | Long | 否 | 无 | 1 | 指定课程 ID |
  | `studentName`| String | 否 | 无 | 张三 | 学生姓名模糊搜索 |
  | `minScore` | Double | 否 | 无 | 60.0 | 最低分数筛选 |
  | `maxScore` | Double | 否 | 无 | 100.0 | 最高分数筛选 |

- **成功响应示例 (HTTP 200)**：
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 1,
    "page": 1,
    "pageSize": 10,
    "list": [
      {
        "id": 1,
        "studentId": 1,
        "studentNo": "20240101",
        "studentName": "张三",
        "className": "计科2401班",
        "courseId": 1,
        "courseNo": "CS101",
        "courseName": "Java语言程序设计",
        "credit": 3.5,
        "score": 88.50,
        "examDate": "2026-06-15",
        "remarks": "期末正考"
      }
    ]
  }
}
```

---

#### 接口 2：录入单科成绩
- **请求方法**：`POST`
- **请求 URL**：`/api/scores`
- **请求 Body (JSON)**：
```json
{
  "studentId": 4,
  "courseId": 2,
  "score": 82.00,
  "examDate": "2026-06-18",
  "remarks": "期末正考"
}
```
- **成功响应示例 (HTTP 200)**：
```json
{
  "code": 200,
  "message": "成绩录入成功",
  "data": {
    "id": 8
  }
}
```

---

#### 接口 3：修改已录入的成绩
- **请求方法**：`PUT`
- **请求 URL**：`/api/scores/{id}`（例如 `/api/scores/1`）
- **请求 Body (JSON)**：
```json
{
  "score": 91.50,
  "examDate": "2026-06-15",
  "remarks": "卷面复核重新核分"
}
```
- **成功响应示例 (HTTP 200)**：
```json
{
  "code": 200,
  "message": "成绩更新成功",
  "data": true
}
```

---

#### 接口 4：删除单条成绩记录
- **请求方法**：`DELETE`
- **请求 URL**：`/api/scores/{id}`（例如 `/api/scores/8`）
- **成功响应示例 (HTTP 200)**：
```json
{
  "code": 200,
  "message": "成绩删除成功",
  "data": true
}
```

---

### 5.5 成绩统计分析接口

#### 接口 1：获取课程成绩统计报表指标
- **请求方法**：`GET`
- **请求 URL**：`/api/scores/statistics`
- **请求 Query 参数**：
  | 参数名 | 类型 | 必填 | 默认值 | 示例值 | 说明 |
  | :--- | :--- | :--- | :--- | :--- | :--- |
  | `courseId` | Long | 否 | 无 | 1 | 统计指定课程；不传则统计全校综合成绩 |

- **成功响应示例 (HTTP 200)**：
```json
{
  "code": 200,
  "message": "统计计算成功",
  "data": {
    "courseId": 1,
    "courseName": "Java语言程序设计",
    "totalStudents": 4,
    "averageScore": 76.00,
    "maxScore": 95.00,
    "minScore": 58.00,
    "passRate": 75.00
  }
}
```

---

## 6. 前端 AI 提示词与联调指南

### 6.1 前端单页应用（SPA）设计要求
- **单文件交付**：直接生成单个 `index.html`（内嵌 CSS 与原生 JS），或者将 CSS/JS 分别存放于 `src/main/resources/static/` 目录下；
- **免构建与开箱即用**：采用 CDN 引入 Bootstrap 5 或 Tailwind CSS，无 Node.js、npm 等复杂打包依赖；
- **Tab 标签页切换**：
  - 标签页 1：**学生档案管理**（查询表单、表格展示、分页器、新增/修改 Modal 弹窗、删除二次确认）；
  - 标签页 2：**成绩录入与管理**（多条件搜索、学生及课程下拉框动态回显、成绩维护表格）；
  - 标签页 3：**成绩统计分析看板**（课程下拉筛选、统计卡片展示总人数、平均分、最高分、及格率）。

### 6.2 供 AI 直接生成前端页面的 Prompt 模板

你可以直接复制以下这段精心设计的 Prompt 发送给任意主流大模型，让其为你一键生成前端代码：

> **前端生成 Prompt 模板**：
> 
> ```markdown
> 请帮我编写一个单文件形式的轻量现代前端页面 `index.html`，用于“学生与成绩管理系统”。
> 要求：
> 1. 技术栈：HTML5 + Bootstrap 5 (CDN) + 原生 JavaScript (Fetch API)，不需要任何 npm 或构建工具，直接双击或由后端 8080 端口静态托管即可运行。
> 2. 页面布局：顶部为导航栏（标题“学生管理系统”），主体分为三个 Tab 切换页：
>    - Tab 1: 学生信息管理（支持姓名/学号关键字搜索、专业筛选；列表表格展示；支持分页器；右上角有“新增学生”按钮弹出模态框；操作列有“编辑”、“删除”按钮）。
>    - Tab 2: 成绩记录管理（支持按课程下拉筛选、最低分/最高分筛选；表格展示学号、姓名、课程、学分、成绩、备注；支持“录入成绩”模态框，需能动态从 `/api/courses` 拉取课程列表；支持编辑分数和删除）。
>    - Tab 3: 成绩统计看板（顶部为课程选择下拉框；下方有 4 个精致指标卡片：考查总人数、平均分、最高分/最低分、及格率百分比）。
> 3. 后端 API 规范约定（请求都在同源 http://localhost:8080 下）：
>    - 获取学生列表：GET /api/students?page=1&pageSize=10&keyword=...
>    - 新增学生：POST /api/students，Body 为 JSON {studentNo, name, gender, birthDate, phone, email, major, className}
>    - 修改学生：PUT /api/students/{id}
>    - 删除学生：DELETE /api/students/{id}
>    - 课程列表下拉：GET /api/courses
>    - 成绩列表：GET /api/scores?page=1&pageSize=10&courseId=...
>    - 录入成绩：POST /api/scores，Body 为 JSON {studentId, courseId, score, examDate, remarks}
>    - 修改成绩：PUT /api/scores/{id}，Body 为 JSON {score, examDate, remarks}
>    - 删除成绩：DELETE /api/scores/{id}
>    - 统计接口：GET /api/scores/statistics?courseId=...
> 4. 交互细节：所有接口返回结构均为 `{code: 200, message: "...", data: ...}`。当 code !== 200 时，使用友好 alert 或 toast 提示错误 message。
> 请给出完整可用、UI 美观大方、带有中文注释的 `index.html` 源码。
> ```

### 6.3 静态文件与前后端联调测试步骤

1. **导入数据库**：
   - 打开 MySQL 终端或 Navicat / DataGrip，执行 `sql/schema.sql` 建立库与表结构；
   - 执行 `sql/data.sql` 写入预置演示测试数据。
2. **配置连接参数**：
   - 检查 `src/main/resources/db.properties`，确保用户名、密码与本地 MySQL 一致。
3. **放置前端文件**：
   - 将 AI 生成的 `index.html` 放置到 `src/main/resources/static/index.html`。
4. **编译与启动**：
   - 运行 `com.sms.Main` 主函数；
   - 控制台打印：`[INFO] 学生管理系统 HTTP 服务已在本地启动: http://localhost:8080/`。
5. **浏览器访问与验证**：
   - 打开 Chrome 浏览器，访问 `http://localhost:8080/`；
   - 静态页面自动加载，并立即触发 `/api/students` 加载首屏数据；
   - 进行增删改查测试与统计报表查看，并在控制台观察网络请求流转。

---

## 7. 架构可扩展性与 GitHub 开源建议

1. **接口与实现解耦**：DAO 和 Service 均定义为 Interface，未来若升级引入数据库连接池（如 HikariCP）或引入 Redis 缓存层，只需实现新类，无需改动上层业务逻辑；
2. **零框架轻量化亮点**：在 GitHub 的 `README.md` 中突出“基于 Java 原生标准库实现简易 Web 容器与三层架构”，适合作为大学生期末课设、Java 核心语法进阶、JDBC 基础学习的标准模范范例；
3. **扩展功能路线图（Roadmap）**：
   - [ ] 进阶功能一：增加简易 Session/Token 用户登录认证与权限拦截；
   - [ ] 进阶功能二：集成 Apache POI 原生实现学生与成绩 Excel 表格一键导入与导出；
   - [ ] 进阶功能三：前端统计图表引入 ECharts 可视化直方图与饼图。
