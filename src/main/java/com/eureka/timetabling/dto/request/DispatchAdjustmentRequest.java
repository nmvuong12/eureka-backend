package com.eureka.timetabling.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class DispatchAdjustmentRequest {
    private Long teacherId;
    private Long roomId;
    private Long timeslotId;
    private LocalDate sessionDate;
}
