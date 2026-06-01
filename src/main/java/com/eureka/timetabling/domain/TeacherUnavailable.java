package com.eureka.timetabling.domain;

import lombok.*;

/** Domain entity - Lịch bận cố định của giáo viên */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TeacherUnavailable {
    private Long id;
    private Long teacherId;
    private Long timeslotId;
    private String reason;
}
