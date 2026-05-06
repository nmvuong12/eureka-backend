package com.eureka.timetabling.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
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
    @PlanningVariable
    private Long teacherId;

    @PlanningVariable
    private Long roomId;

    @PlanningVariable
    private Long timeslotId;

    // Có thể ghim (không thay đổi trong quá trình giải)
    private Boolean pinned;
}
