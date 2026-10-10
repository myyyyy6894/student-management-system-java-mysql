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