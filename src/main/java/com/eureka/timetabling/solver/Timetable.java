package com.eureka.timetabling.solver;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import com.eureka.timetabling.domain.Lesson;
import com.eureka.timetabling.domain.Room;
import com.eureka.timetabling.domain.Teacher;
import com.eureka.timetabling.domain.Timeslot;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Lời giải lập kế hoạch (Planning Solution) cho Timefold Solver.
 * Chứa danh sách các buổi học cần xếp lịch, và tập hợp giá trị có thể gán.
 */
@PlanningSolution
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Timetable {

    private String name;

    // Tập hợp giá trị cho các biến lập kế hoạch
    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<Long> teacherIds;

    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<Long> roomIds;

    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<Long> timeslotIds;

    // Thông tin phụ trợ để kiểm tra ràng buộc
    @ProblemFactCollectionProperty
    private List<TeacherFact> teacherFacts;

    @ProblemFactCollectionProperty
    private List<RoomFact> roomFacts;

    @ProblemFactCollectionProperty
    private List<TimeslotFact> timeslotFacts;

    @ProblemFactCollectionProperty
    private List<TeacherUnavailableFact> teacherUnavailableFacts;

    @ProblemFactCollectionProperty
    private List<TeacherSkillFact> teacherSkillFacts;

    // Thực thể lập kế hoạch - Timefold sẽ gán giá trị cho các biến
    @PlanningEntityCollectionProperty
    private List<Lesson> lessons;

    // Điểm số đánh giá lời giải
    @PlanningScore
    private HardSoftScore score;

    // ===== Inner Problem Fact Classes =====

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class TeacherFact {
        private Long teacherId;
        private String status;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RoomFact {
        private Long roomId;
        private Integer capacity;
        private String status;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class TimeslotFact {
        private Long timeslotId;
        private String dayOfWeek;
        private String startTime;
        private String endTime;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class TeacherUnavailableFact {
        private Long teacherId;
        private Long timeslotId;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class TeacherSkillFact {
        private Long teacherId;
        private String skillCode;
    }
}
