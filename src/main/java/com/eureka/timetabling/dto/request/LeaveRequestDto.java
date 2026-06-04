package com.eureka.timetabling.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/** Request DTO - Đơn xin nghỉ */
@Data
public class LeaveRequestDto {
    @NotNull
    private LocalDate fromDate;

    @NotNull
    private LocalDate toDate;

    private String reason;

    private String sessionType; // MORNING, AFTERNOON, ALL_DAY (optional now)

    private String makeupOption; // MAKEUP, NO_MAKEUP (optional now)

    private LocalDate makeupDate;

    private Long makeupTimeslotId;

    private List<LessonOptionDto> lessonOptions;

    private List<DayConfigDto> dayConfigs;

    @Data
    public static class DayConfigDto {
        @NotNull
        private LocalDate date;
        @NotBlank
        private String sessionType; // MORNING, AFTERNOON, ALL_DAY
    }

    @Data
    public static class LessonOptionDto {
        @NotNull
        private Long lessonId;
        @NotBlank
        private String makeupOption; // MAKEUP, NO_MAKEUP
        private LocalDate makeupDate;
        private Long makeupTimeslotId;
    }
}

