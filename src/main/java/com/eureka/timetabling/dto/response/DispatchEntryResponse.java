package com.eureka.timetabling.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchEntryResponse {
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
    private LocalDate sessionDate;
    private boolean pinned;
}
