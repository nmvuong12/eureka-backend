package com.eureka.timetabling.domain;

import lombok.*;

/** Domain entity - Ca học */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Timeslot {
    private Long id;
    private String dayOfWeek;
    private String startTime;
    private String endTime;
    private String label;
}
