package com.eureka.timetabling.dto.request;

import lombok.Data;

/** Request DTO - Huỷ lớp học kèm lý do */
@Data
public class CancelClassRequest {
    private String reason;
}
