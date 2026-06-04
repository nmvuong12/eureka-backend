package com.eureka.timetabling.dto.request;

import lombok.Data;
import java.util.List;

/** Request DTO - Gửi yêu cầu dạy thay FCFS hàng loạt */
@Data
public class SubstituteDispatchRequest {
    private Long leaveRequestId;
    private Long lessonId;
    private List<Long> teacherIds;
}
