package com.eureka.timetabling.service.impl;

import com.eureka.timetabling.domain.Lesson;
import com.eureka.timetabling.domain.SchoolClass;
import com.eureka.timetabling.dto.request.ClassRequest;
import com.eureka.timetabling.dto.request.LessonAssignmentRequest;
import com.eureka.timetabling.exception.ResourceNotFoundException;
import com.eureka.timetabling.repository.ClassRepository;
import com.eureka.timetabling.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** Service quản lý lớp học */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClassRepository classRepository;
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public List<SchoolClass> findAll() {
        return classRepository.findAll();
    }

    @Transactional(readOnly = true)
    public SchoolClass findById(Long id) {
        return classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học", id));
    }

    @Transactional
    public SchoolClass create(ClassRequest request) {
        var course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Khóa học", request.getCourseId()));

        SchoolClass clazz = SchoolClass.builder()
                .courseId(request.getCourseId())
                .name(request.getName())
                .studentSize(request.getStudentSize())
                .startDate(request.getStartDate())
                .status("PENDING")
                .build();
        Long classId = classRepository.saveClass(clazz);

        // Tự động tạo danh sách buổi học theo tổng số buổi của khóa học
        String skill = request.getRequiredSkill() != null
                ? request.getRequiredSkill()
                : course.getName().toUpperCase().replace(" ", "_");

        List<Lesson> lessons = new ArrayList<>();
        for (int i = 1; i <= course.getTotalLessons(); i++) {
            lessons.add(Lesson.builder()
                    .classId(classId)
                    .lessonIndex(i)
                    .requiredSkill(skill)
                    .build());
        }
        classRepository.saveLessons(lessons);
        log.info("Đã tạo lớp {} với {} buổi học", request.getName(), lessons.size());

        clazz.setId(classId);
        return clazz;
    }

    @Transactional(readOnly = true)
    public List<Lesson> getLessons(Long classId) {
        classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học", classId));
        return classRepository.findLessonsByClassId(classId);
    }

    @Transactional
    public void updateLessonAssignment(Long lessonId, LessonAssignmentRequest request) {
        classRepository.updateAssignment(lessonId,
                request.getTeacherId(), request.getRoomId(), request.getTimeslotId());
    }
}
