package com.eureka.timetabling.dto.response;

import com.eureka.timetabling.domain.SchoolClass;
import lombok.*;
import java.util.List;

/** DTO kết quả rebalance lớp học */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RebalanceResult {
    /** Hành động thực hiện: REDISTRIBUTED, SUGGEST_MERGE, SUGGEST_CANCEL */
    private String action;
    /** Danh sách lớp sau khi tái phân bổ */
    private List<SchoolClass> classes;
    /** Thông điệp giải thích kết quả */
    private String message;
}
