package com.eureka.timetabling.domain;

/** Enum trạng thái lớp học trong state machine */
public enum ClassStatus {
    DRAFT, ENROLLING, REBALANCING, OPEN, STUDYING, FINISHED, CANCELLED,
    PENDING, ACTIVE, COMPLETED
}
