package com.eureka.timetabling.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

/** Request DTO - Gộp nhiều lớp vào một lớp đích */
@Data
public class MergeClassRequest {
    /** Danh sách ID lớp nguồn (sẽ bị huỷ sau khi gộp) */
    @NotEmpty private List<Long> sourceClassIds;
    /** ID lớp đích (sẽ nhận học viên từ các lớp nguồn) */
    @NotNull  private Long targetClassId;
}
