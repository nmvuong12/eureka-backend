package com.eureka.timetabling.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

/** Request DTO cập nhật hàng loạt lịch bận cố định */
@Data
public class TeacherUnavailableRequest {
    @NotNull(message = "Danh sách ca học bận không được để trống")
    private List<Long> timeslotIds;
}
