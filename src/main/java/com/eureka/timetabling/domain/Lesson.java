package com.eureka.timetabling.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import lombok.*;

/**
 * Thực thể lập kế hoạch cho Timefold Solver.
 * Đại diện cho một buổi học cần được phân công giáo viên, phòng và ca học.
 */
@PlanningEntity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Lesson {

    @PlanningId
    @EqualsAndHashCode.Include
    private Long id;

    private Long classId;
    private Integer lessonIndex;
    private String requiredSkill;

    // Biến lập kế hoạch - sẽ được Timefold gán giá trị
    @PlanningVariable(valueRangeProviderRefs = "teacherRange")
    private Long teacherId;

    @PlanningVariable(valueRangeProviderRefs = "roomRange")
    private Long roomId;

    @PlanningVariable(valueRangeProviderRefs = "timeslotRange")
    private Long timeslotId;

    // Có thể ghim (không thay đổi trong quá trình giải)
    @PlanningPin
    private Boolean pinned;

    // Problem facts bổ sung cho Rolling Scheduling
    private Long schedulePatternId;
    private Long classTeacherId;
    private java.time.LocalDate sessionDate;

    // Backup fields for original schedule when rescheduled (dạy bù/dạy thay)
    private java.time.LocalDate originalSessionDate;
    private Long originalTimeslotId;
    private Long originalRoomId;
    private String rescheduleReason;
    private Long leaveRequestId;

    // Transient fields for UI and individual leave request choices
    private String classCode;
    private String makeupOption;
    private java.time.LocalDate makeupDate;
    private Long makeupTimeslotId;
    private String teacherName;
    private String feasibilityNote;
}



