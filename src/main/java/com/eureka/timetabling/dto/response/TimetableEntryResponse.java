package com.eureka.timetabling.dto.response;

import lombok.*;

/** Response DTO - Thông tin timetable entry */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TimetableEntryResponse {
    private Long lessonId;
    private Long classId;
    private String className;
    private Integer lessonIndex;
    private String requiredSkill;

    private Long teacherId;
    private String teacherName;

    private Long roomId;
    private String roomName;

    private Long timeslotId;
    private String dayOfWeek;
    private String startTime;
    private String endTime;
    private String timeslotLabel;

    private Boolean pinned;
}
