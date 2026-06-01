package com.eureka.timetabling.service;

import java.util.List;

/** Service quản lý lịch bận cố định của giáo viên */
public interface TeacherUnavailableService {
    
    /** Lấy danh sách ID các ca học bận của giáo viên */
    List<Long> getUnavailableTimeslots(Long teacherId);
    
    /** Cập nhật hàng loạt danh sách các ca học bận của giáo viên */
    void updateUnavailableTimeslots(Long teacherId, List<Long> timeslotIds);
}
