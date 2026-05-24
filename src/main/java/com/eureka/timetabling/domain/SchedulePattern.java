package com.eureka.timetabling.domain;

import lombok.*;

/** Mẫu lịch học chuẩn (19 pattern) */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SchedulePattern {
    private Long id;
    /** Mã pattern (P001-P019) */
    private String code;
    /** Ngày học trong tuần, phân cách bởi dấu phẩy (VD: "2,4,6") */
    private String studyDays;
    /** Mã ca học (C1-C5) */
    private String slotCode;
    /** Giờ bắt đầu (VD: "08:00") */
    private String slotStart;
    /** Giờ kết thúc (VD: "10:00") */
    private String slotEnd;
    /** Số buổi mỗi tuần */
    private Integer sessionsPerWeek;
    /** Đang được sử dụng hay không */
    private Boolean active;

    /** Nhãn hiển thị (tính từ studyDays + slotCode) */
    public String getDisplayLabel() {
        return formatDays() + " " + slotCode + " (" + slotStart + "-" + slotEnd + ")";
    }

    /** Format ngày học từ dạng "2,4,6" sang "T2-T4-T6" */
    private String formatDays() {
        if (studyDays == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String d : studyDays.split(",")) {
            if (!sb.isEmpty()) sb.append("-");
            sb.append("T").append(d.trim());
        }
        return sb.toString();
    }
}
