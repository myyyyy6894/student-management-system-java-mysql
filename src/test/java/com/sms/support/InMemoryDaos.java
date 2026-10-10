package com.sms.support;

// 导入三个 DAO 接口。
import com.sms.dao.CourseDao;
import com.sms.dao.ScoreDao;
import com.sms.dao.StudentDao;
// 导入 DTO 与实体。
import com.sms.model.dto.ScoreQueryDTO;
import com.sms.model.dto.StudentQueryDTO;
import com.sms.model.entity.Course;
import com.sms.model.entity.Score;
import com.sms.model.entity.Student;
import com.sms.model.vo.ScoreDetailVO;
import com.sms.model.vo.ScoreStatsVO;
// 导入集合与并发工具。
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存版 DAO 实现集合，供单元测试使用。
 * <p>目的是让 Service / Controller 层的规则测试完全不依赖真实 MySQL，
 * 从而可以在 {@code mvn test} 中自动、快速地执行。</p>
 * <p>这里只实现测试需要的行为，查询条件只做最小可用支持。</p>
 */
public final class InMemoryDaos {

    /**
     * 私有构造器，避免创建工具类实例。
     */
    private InMemoryDaos() {
    }

    /**
     * 内存版学生 DAO。
     */
    public static final class InMemoryStudentDao implements StudentDao {

        // 用有序 Map 模拟数据表。
        public final Map<Long, Student> table = new LinkedHashMap<>();

        // 自增主键序列。
        public final AtomicLong sequence = new AtomicLong(0);

        @Override
        public Student findById(Long id) {
            return id == null ? null : table.get(id);
        }

        @Override
        public Student findByStudentNo(String studentNo) {
            if (studentNo == null) {
                return null;
            }
            return table.values().stream()
                    .filter(item -> studentNo.equals(item.getStudentNo()))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public Long insert(Student student) {
            long id = sequence.incrementAndGet();
            student.setId(id);
            table.put(id, student);
            return id;
        }

        @Override
        public int update(Student student) {
            if (!table.containsKey(student.getId())) {
                return 0;
            }
            table.put(student.getId(), student);
            return 1;
        }

        @Override
        public int deleteById(Long id) {
            return table.remove(id) == null ? 0 : 1;
        }

        @Override
        public List<Student> findByPage(StudentQueryDTO queryDTO) {
            List<Student> matched = filter(queryDTO);
            int page = Math.max(1, queryDTO.getPage());
            int pageSize = Math.max(1, queryDTO.getPageSize());
            int from = Math.min((page - 1) * pageSize, matched.size());
            int to = Math.min(from + pageSize, matched.size());
            return new ArrayList<>(matched.subList(from, to));
        }

        @Override
        public long countByCondition(StudentQueryDTO queryDTO) {
            return filter(queryDTO).size();
        }

        private List<Student> filter(StudentQueryDTO queryDTO) {
            List<Student> matched = new ArrayList<>();
            for (Student student : table.values()) {
                if (queryDTO.getStatus() != null && !queryDTO.getStatus().equals(student.getStatus())) {
                    continue;
                }
                if (queryDTO.getMajor() != null && !queryDTO.getMajor().equals(student.getMajor())) {
                    continue;
                }
                if (queryDTO.getClassName() != null && !queryDTO.getClassName().equals(student.getClassName())) {
                    continue;
                }
                if (queryDTO.getKeyword() != null) {
                    String keyword = queryDTO.getKeyword();
                    boolean hit = student.getName().contains(keyword) || student.getStudentNo().contains(keyword);
                    if (!hit) {
                        continue;
                    }
                }
                matched.add(student);
            }
            return matched;
        }
    }

    /**
     * 内存版课程 DAO。
     */
    public static final class InMemoryCourseDao implements CourseDao {

        // 用有序 Map 模拟数据表。
        public final Map<Long, Course> table = new LinkedHashMap<>();

        // 自增主键序列。
        public final AtomicLong sequence = new AtomicLong(0);

        @Override
        public Course findById(Long id) {
            return id == null ? null : table.get(id);
        }

        @Override
        public Course findByCourseNo(String courseNo) {
            if (courseNo == null) {
                return null;
            }
            return table.values().stream()
                    .filter(item -> courseNo.equals(item.getCourseNo()))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public Long insert(Course course) {
            long id = sequence.incrementAndGet();
            course.setId(id);
            table.put(id, course);
            return id;
        }

        @Override
        public int update(Course course) {
            if (!table.containsKey(course.getId())) {
                return 0;
            }
            table.put(course.getId(), course);
            return 1;
        }

        @Override
        public int deleteById(Long id) {
            return table.remove(id) == null ? 0 : 1;
        }

        @Override
        public List<Course> findAll() {
            return new ArrayList<>(table.values());
        }
    }

    /**
     * 内存版成绩 DAO。
     */
    public static final class InMemoryScoreDao implements ScoreDao {

        // 用有序 Map 模拟数据表。
        public final Map<Long, Score> table = new LinkedHashMap<>();

        // 自增主键序列。
        public final AtomicLong sequence = new AtomicLong(0);

        // 统计时用于关联查询学生与课程名称。
        public InMemoryStudentDao studentDao;

        // 统计时用于关联查询课程名称。
        public InMemoryCourseDao courseDao;

        @Override
        public Score findById(Long id) {
            return id == null ? null : table.get(id);
        }

        @Override
        public Score findByStudentAndCourse(Long studentId, Long courseId) {
            if (studentId == null || courseId == null) {
                return null;
            }
            return table.values().stream()
                    .filter(item -> studentId.equals(item.getStudentId()) && courseId.equals(item.getCourseId()))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public Long insert(Score score) {
            long id = sequence.incrementAndGet();
            score.setId(id);
            table.put(id, score);
            return id;
        }

        @Override
        public int update(Score score) {
            if (!table.containsKey(score.getId())) {
                return 0;
            }
            table.put(score.getId(), score);
            return 1;
        }

        @Override
        public int deleteById(Long id) {
            return table.remove(id) == null ? 0 : 1;
        }

        @Override
        public long countByStudentId(Long studentId) {
            if (studentId == null) {
                return 0;
            }
            return table.values().stream()
                    .filter(item -> studentId.equals(item.getStudentId()))
                    .count();
        }

        @Override
        public long countByCourseId(Long courseId) {
            if (courseId == null) {
                return 0;
            }
            return table.values().stream()
                    .filter(item -> courseId.equals(item.getCourseId()))
                    .count();
        }

        @Override
        public List<ScoreDetailVO> findDetailByPage(ScoreQueryDTO queryDTO) {
            List<ScoreDetailVO> matched = details(queryDTO);
            int page = Math.max(1, queryDTO.getPage());
            int pageSize = Math.max(1, queryDTO.getPageSize());
            int from = Math.min((page - 1) * pageSize, matched.size());
            int to = Math.min(from + pageSize, matched.size());
            return new ArrayList<>(matched.subList(from, to));
        }

        @Override
        public long countDetailByCondition(ScoreQueryDTO queryDTO) {
            return details(queryDTO).size();
        }

        @Override
        public ScoreStatsVO calculateStatistics(Long courseId) {
            List<Score> scoped = new ArrayList<>();
            for (Score score : table.values()) {
                if (courseId == null || courseId.equals(score.getCourseId())) {
                    scoped.add(score);
                }
            }

            ScoreStatsVO stats = new ScoreStatsVO();
            if (courseId != null && courseDao != null && courseDao.findById(courseId) != null) {
                stats.setCourseId(courseId);
                stats.setCourseName(courseDao.findById(courseId).getName());
            }

            stats.setTotalStudents(scoped.size());
            if (scoped.isEmpty()) {
                stats.setAverageScore(BigDecimal.ZERO);
                stats.setMaxScore(BigDecimal.ZERO);
                stats.setMinScore(BigDecimal.ZERO);
                stats.setPassRate(BigDecimal.ZERO);
                return stats;
            }

            BigDecimal sum = BigDecimal.ZERO;
            BigDecimal max = null;
            BigDecimal min = null;
            long passCount = 0;
            for (Score score : scoped) {
                BigDecimal value = score.getScore();
                sum = sum.add(value);
                max = max == null || value.compareTo(max) > 0 ? value : max;
                min = min == null || value.compareTo(min) < 0 ? value : min;
                if (value.compareTo(BigDecimal.valueOf(60)) >= 0) {
                    passCount++;
                }
            }

            stats.setAverageScore(sum.divide(BigDecimal.valueOf(scoped.size()), 2, RoundingMode.HALF_UP));
            stats.setMaxScore(max);
            stats.setMinScore(min);
            stats.setPassRate(BigDecimal.valueOf(passCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(scoped.size()), 2, RoundingMode.HALF_UP));
            return stats;
        }

        private List<ScoreDetailVO> details(ScoreQueryDTO queryDTO) {
            List<ScoreDetailVO> matched = new ArrayList<>();
            for (Score score : table.values()) {
                if (queryDTO.getStudentId() != null && !queryDTO.getStudentId().equals(score.getStudentId())) {
                    continue;
                }
                if (queryDTO.getCourseId() != null && !queryDTO.getCourseId().equals(score.getCourseId())) {
                    continue;
                }
                if (queryDTO.getMinScore() != null
                        && score.getScore().doubleValue() < queryDTO.getMinScore()) {
                    continue;
                }
                if (queryDTO.getMaxScore() != null
                        && score.getScore().doubleValue() > queryDTO.getMaxScore()) {
                    continue;
                }

                Student student = studentDao == null ? null : studentDao.findById(score.getStudentId());
                if (queryDTO.getStudentName() != null
                        && (student == null || !student.getName().contains(queryDTO.getStudentName()))) {
                    continue;
                }

                Course course = courseDao == null ? null : courseDao.findById(score.getCourseId());

                ScoreDetailVO detail = new ScoreDetailVO();
                detail.setId(score.getId());
                detail.setStudentId(score.getStudentId());
                detail.setStudentNo(student == null ? null : student.getStudentNo());
                detail.setStudentName(student == null ? null : student.getName());
                detail.setClassName(student == null ? null : student.getClassName());
                detail.setCourseId(score.getCourseId());
                detail.setCourseNo(course == null ? null : course.getCourseNo());
                detail.setCourseName(course == null ? null : course.getName());
                detail.setCredit(course == null ? null : course.getCredit());
                detail.setScore(score.getScore());
                detail.setExamDate(score.getExamDate());
                detail.setRemarks(score.getRemarks());
                matched.add(detail);
            }
            return matched;
        }
    }
}
