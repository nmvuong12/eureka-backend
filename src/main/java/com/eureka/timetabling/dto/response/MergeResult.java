package com.eureka.timetabling.dto.response;

import com.eureka.timetabling.domain.SchoolClass;
import lombok.*;
import java.util.List;

/** DTO kết quả gộp lớp học */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MergeResult {
    /** Lớp đích sau khi gộp */
    private SchoolClass targetClass;
    /** Danh sách ID lớp bị huỷ sau khi gộp */
    private List<Long> cancelledClassIds;
}
