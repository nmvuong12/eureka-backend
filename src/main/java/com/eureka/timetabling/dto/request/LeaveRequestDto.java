package com.eureka.timetabling.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

/** Request DTO - Đơn xin nghỉ */
@Data
public class LeaveRequestDto {
    @NotNull
    private LocalDate fromDate;

    @NotNull
    private LocalDate toDate;

    private String reason;
}
