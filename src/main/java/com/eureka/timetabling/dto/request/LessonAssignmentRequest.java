package com.eureka.timetabling.dto.request;

import lombok.Data;

/** Request DTO - Chỉnh sửa thủ công phân công buổi học */
@Data
public class LessonAssignmentRequest {
    private Long teacherId;
    private Long roomId;
    private Long timeslotId;
    private Boolean pinned;
}
