package com.eureka.timetabling.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO - Yêu cầu xác thực xung đột phân công hàng loạt
 */
@Data
public class BatchValidateAssignmentRequest {
    @NotEmpty(message = "Danh sách ID buổi học không được rỗng")
    private List<Long> lessonIds;
    
    private Long teacherId;
    private Long roomId;
    private Long timeslotId;
    private LocalDate sessionDate;
}
