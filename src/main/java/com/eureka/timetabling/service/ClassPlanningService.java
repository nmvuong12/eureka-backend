package com.eureka.timetabling.service;

import com.eureka.timetabling.domain.*;
import com.eureka.timetabling.dto.response.CapacityDashboardItem;
import com.eureka.timetabling.dto.response.MergeResult;
import com.eureka.timetabling.dto.response.RebalanceResult;
import com.eureka.timetabling.dto.request.ClassUpdateRequest;
import com.eureka.timetabling.exception.BusinessException;
import com.eureka.timetabling.exception.ResourceNotFoundException;
import com.eureka.timetabling.repository.ClassRepository;
import com.eureka.timetabling.repository.CourseBatchRepository;
import com.eureka.timetabling.repository.CourseRepository;
import com.eureka.timetabling.repository.SchedulePatternRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service cốt lõi điều phối toàn bộ Workflow Lập kế hoạch mở lớp (Rolling Scheduling).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassPlanningService {

    private final ClassRepository classRepository;
    private final CourseRepository courseRepository;
    private final CourseBatchRepository courseBatchRepository;
    private final SchedulePatternRepository schedulePatternRepository;
    private final CapacityCalculationService capacityCalculationService;
    private final ClassStateMachineService classStateMachineService;

    /**
     * BƯỚC 4: Validate và sinh lớp học DRAFT hàng loạt.
     *
     * @param batchId   ID kế hoạch khai giảng
     * @param patternId ID của schedule pattern
     * @param count     Số lượng lớp muốn tạo
     * @return Danh sách các lớp DRAFT đã tạo
     */
    @Transactional
    public List<SchoolClass> generateDraftClasses(Long batchId, Long patternId, int count) {
        CourseBatch batch = courseBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Kế hoạch khai giảng", batchId));

        // 1. Chỉ cho phép sinh lớp từ đợt có trạng thái PLANNING
        if (batch.getStatus() != CourseBatchStatus.PLANNING) {
            throw new com.eureka.timetabling.exception.BusinessException("Kế hoạch chưa đủ điều kiện lập kế hoạch mở lớp");
        }

        Course course = courseRepository.findById(batch.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Khóa học", batch.getCourseId()));

        SchedulePattern pattern = schedulePatternRepository.findById(patternId)
                .orElseThrow(() -> new ResourceNotFoundException("Mẫu lịch học", patternId));

        // 2. Tính toán capacity còn lại của pattern này
        CapacityDashboardItem capacityItem = capacityCalculationService.calculateForPattern(patternId, course.getId());
        int remaining = capacityItem.getRemaining();
        if (remaining <= 0) {
            throw new com.eureka.timetabling.exception.BusinessException("Không đủ dữ liệu để lập kế hoạch mở lớp");
        }

        // Tự động giảm số lượng lớp sinh ra phù hợp với công suất thực tế còn lại
        if (count > remaining) {
            count = remaining;
        }

        // 3. Tìm suffix lớn nhất hiện tại của batch này
        String codePrefix = course.getCode() + "_";
        int maxSuffix = classRepository.findMaxSuffixForBatch(batchId, codePrefix);

        List<SchoolClass> createdClasses = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            char suffix = (char) ('A' + maxSuffix + i);
            String classCode = course.getCode() + "_" + suffix;

            LocalDate expectedOpening = batch.getExpectedOpeningDate();
            LocalDate fallbackStartDate = expectedOpening != null ? expectedOpening : LocalDate.now();

            SchoolClass clazz = SchoolClass.builder()
                    .classCode(classCode)
                    .batchId(batchId)
                    .schedulePatternId(patternId)
                    .courseId(course.getId())
                    .name(classCode) // Đồng bộ legacy field
                    .studentCount(0)
                    .studentSize(0)  // Đồng bộ legacy field
                    .status(ClassStatus.DRAFT)
                    .startDate(fallbackStartDate)
                    .actualOpeningDate(expectedOpening)
                    .build();

            Long classId = classRepository.savePlanningClass(clazz);
            clazz.setId(classId);

            // Ghi audit trail
            classStateMachineService.transition(classId, ClassStatus.DRAFT, "CREATED", "Khởi tạo lớp nháp từ lịch học " + pattern.getDisplayLabel());

            // Tự động tạo danh sách buổi học (lessons)
            String skill = course.getRequiredSkillCode() != null ? course.getRequiredSkillCode() : "GENERAL";
            int totalSessions = course.getTotalSessions() != null ? course.getTotalSessions() : (course.getTotalLessons() != null ? course.getTotalLessons() : 24);
            
            List<Lesson> lessons = new ArrayList<>();
            for (int j = 1; j <= totalSessions; j++) {
                lessons.add(Lesson.builder()
                        .classId(classId)
                        .lessonIndex(j)
                        .requiredSkill(skill)
                        .build());
            }
            classRepository.saveLessons(lessons);

            createdClasses.add(clazz);
        }

        // Tự động chuyển trạng thái đợt từ PLANNING sang GENERATED sau khi tạo thành công
        courseBatchRepository.updateStatus(batchId, CourseBatchStatus.GENERATED.name());

        log.info("Đã tạo {} lớp DRAFT cho batch: {} với pattern: {}", count, batch.getBatchName(), pattern.getCode());
        return createdClasses;
    }

    /**
     * BƯỚC 4b: Lập kế hoạch Tự động (Phù hợp nhất)
     * Dựa trên quy mô dự kiến (forecastScale), sĩ số tối đa (maxStudents), phòng học, giáo viên rảnh và các lịch học hiện có.
     * Thuật toán: Phân phối tham lam (greedy) các lớp cần mở vào các pattern có capacity còn lại cao nhất.
     */
    @Transactional
    public List<SchoolClass> smartGenerate(Long batchId) {
        CourseBatch batch = courseBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Kế hoạch khai giảng", batchId));

        // 1. Chỉ cho phép sinh lớp từ đợt có trạng thái PLANNING
        if (batch.getStatus() != CourseBatchStatus.PLANNING) {
            throw new BusinessException("Kế hoạch chưa đủ điều kiện lập kế hoạch mở lớp");
        }

        int forecastScale = batch.getForecastScale() != null ? batch.getForecastScale() : 0;
        if (forecastScale <= 0) {
            throw new BusinessException("Quy mô dự kiến của kế hoạch khai giảng phải lớn hơn 0");
        }

        Course course = courseRepository.findById(batch.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Khóa học", batch.getCourseId()));

        int maxStudents = course.getMaxStudents() != null && course.getMaxStudents() > 0 ? course.getMaxStudents() : 25;

        // 2. Tính số lượng lớp học cần sinh ra: N = ceil(forecastScale / maxStudents)
        int classesNeeded = (int) Math.ceil((double) forecastScale / maxStudents);

        // 3. Lấy capacity dashboard cho tất cả active patterns của khóa học này
        List<CapacityDashboardItem> dashboard = capacityCalculationService.calculateDashboard(course.getId());
        List<CapacityDashboardItem> availableItems = new ArrayList<>();
        for (CapacityDashboardItem item : dashboard) {
            if (item.getRemaining() > 0) {
                availableItems.add(item);
            }
        }

        if (availableItems.isEmpty()) {
            throw new BusinessException("Không đủ phòng học và giáo viên rảnh để lập kế hoạch mở lớp cho bất kỳ lịch học nào");
        }

        // 4. Phân phối tham lam (greedy): mỗi lần lấy pattern có remaining lớn nhất
        List<Long> patternIdsToGenerate = new ArrayList<>();
        for (int i = 0; i < classesNeeded; i++) {
            availableItems.sort((a, b) -> Integer.compare(b.getRemaining(), a.getRemaining()));
            CapacityDashboardItem bestPattern = availableItems.get(0);
            if (bestPattern.getRemaining() <= 0) {
                break; // Hết công suất nguồn lực
            }
            patternIdsToGenerate.add(bestPattern.getPatternId());
            bestPattern.setRemaining(bestPattern.getRemaining() - 1);
        }

        if (patternIdsToGenerate.isEmpty()) {
            throw new BusinessException("Không đủ phòng học và giáo viên rảnh để lập kế hoạch mở lớp cho bất kỳ lịch học nào");
        }

        // 5. Sinh các lớp nháp DRAFT cho danh sách pattern đã phân phối
        String codePrefix = course.getCode() + "_";
        int maxSuffix = classRepository.findMaxSuffixForBatch(batchId, codePrefix);

        List<SchoolClass> createdClasses = new ArrayList<>();
        for (int i = 0; i < patternIdsToGenerate.size(); i++) {
            Long patternId = patternIdsToGenerate.get(i);
            char suffix = (char) ('A' + maxSuffix + i);
            String classCode = course.getCode() + "_" + suffix;

            LocalDate expectedOpening = batch.getExpectedOpeningDate();
            LocalDate fallbackStartDate = expectedOpening != null ? expectedOpening : LocalDate.now();

            SchoolClass clazz = SchoolClass.builder()
                    .classCode(classCode)
                    .batchId(batchId)
                    .schedulePatternId(patternId)
                    .courseId(course.getId())
                    .name(classCode) // Đồng bộ legacy field
                    .studentCount(0)
                    .studentSize(0)  // Đồng bộ legacy field
                    .status(ClassStatus.DRAFT)
                    .startDate(fallbackStartDate)
                    .actualOpeningDate(expectedOpening)
                    .build();

            Long classId = classRepository.savePlanningClass(clazz);
            clazz.setId(classId);

            // Ghi audit trail
            SchedulePattern pattern = schedulePatternRepository.findById(patternId)
                    .orElseThrow(() -> new ResourceNotFoundException("Mẫu lịch học", patternId));
            classStateMachineService.transition(classId, ClassStatus.DRAFT, "CREATED", 
                    "Khởi tạo lớp nháp tự động từ lịch học " + pattern.getDisplayLabel());

            // Tự động tạo danh sách buổi học (lessons)
            String skill = course.getRequiredSkillCode() != null ? course.getRequiredSkillCode() : "GENERAL";
            int totalSessions = course.getTotalSessions() != null ? course.getTotalSessions() : (course.getTotalLessons() != null ? course.getTotalLessons() : 24);
            
            List<Lesson> lessons = new ArrayList<>();
            for (int j = 1; j <= totalSessions; j++) {
                lessons.add(Lesson.builder()
                        .classId(classId)
                        .lessonIndex(j)
                        .requiredSkill(skill)
                        .build());
            }
            classRepository.saveLessons(lessons);

            createdClasses.add(clazz);
        }

        // Tự động chuyển trạng thái đợt từ PLANNING sang GENERATED sau khi tạo thành công
        courseBatchRepository.updateStatus(batchId, CourseBatchStatus.GENERATED.name());

        log.info("Đã lập kế hoạch tự động và tạo {} lớp DRAFT cho batch: {}", createdClasses.size(), batch.getBatchName());
        return createdClasses;
    }

    /**
     * BƯỚC 5: Chuyển toàn bộ lớp DRAFT của Batch sang trạng thái tuyển sinh ENROLLING.
     * Đồng thời tự động cập nhật trạng thái CourseBatch sang ENROLLING.
     */
    @Transactional
    public void activateEnrollment(Long batchId) {
        CourseBatch batch = courseBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Kế hoạch khai giảng", batchId));

        List<SchoolClass> classes = classRepository.findByBatchId(batchId);
        int activatedCount = 0;

        for (SchoolClass clazz : classes) {
            if (clazz.getStatus() == ClassStatus.DRAFT) {
                classStateMachineService.transition(clazz.getId(), ClassStatus.ENROLLING, "STATUS_CHANGED", "Bắt đầu tuyển sinh");
                activatedCount++;
            }
        }

        // Tự động cập nhật trạng thái kế hoạch khai giảng sang ENROLLING
        if (batch.getStatus() == CourseBatchStatus.PLANNING || batch.getStatus() == CourseBatchStatus.GENERATED) {
            courseBatchRepository.updateStatus(batchId, CourseBatchStatus.ENROLLING.name());
        }

        log.info("Đã kích hoạt tuyển sinh cho {} lớp của batch: {}", activatedCount, batch.getBatchName());
    }

    /**
     * BƯỚC 6: Cập nhật sĩ số học viên tuyển sinh thực tế cho lớp.
     */
    @Transactional
    public void updateStudentCount(Long classId, int studentCount) {
        classRepository.updateStudentCount(classId, studentCount);
        classStateMachineService.logAction(classId, "REBALANCED", "Cập nhật sĩ số học viên thực tế: " + studentCount);
    }

    /**
     * BƯỚC 7: Cân bằng sĩ số (Rebalance) cho toàn bộ lớp thuộc cùng batch + schedule pattern.
     */
    @Transactional
    public RebalanceResult rebalanceClasses(Long batchId, Long patternId) {
        CourseBatch batch = courseBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Kế hoạch khai giảng", batchId));

        Course course = courseRepository.findById(batch.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Khóa học", batch.getCourseId()));

        SchedulePattern pattern = schedulePatternRepository.findById(patternId)
                .orElseThrow(() -> new ResourceNotFoundException("Mẫu lịch học", patternId));

        // Lấy danh sách lớp
        List<SchoolClass> allPatternClasses = classRepository.findByFilters(batchId, null, patternId, null);
        
        // Chỉ rebalance các lớp đang ở ENROLLING hoặc REBALANCING
        List<SchoolClass> targets = allPatternClasses.stream()
                .filter(c -> c.getStatus() == ClassStatus.ENROLLING || c.getStatus() == ClassStatus.REBALANCING)
                .toList();

        if (targets.isEmpty()) {
            return RebalanceResult.builder()
                    .action("NO_CLASSES")
                    .message("Không có lớp học nào đang tuyển sinh hoặc cần cân bằng của lịch học này.")
                    .classes(new ArrayList<>())
                    .build();
        }

        int n = targets.size();
        int totalStudents = targets.stream().mapToInt(SchoolClass::getStudentCount).sum();
        int minStudents = course.getMinStudents();

        int avgStudents = totalStudents / n;
        int remainder = totalStudents % n;

        List<SchoolClass> updatedClasses = new ArrayList<>();

        if (avgStudents >= minStudents) {
            // Trường hợp 1: Sĩ số trung bình đạt chuẩn tối thiểu -> Tái phân bổ đều và chuyển OPEN luôn
            for (int i = 0; i < n; i++) {
                SchoolClass clazz = targets.get(i);
                int studentAllocated = avgStudents + (i < remainder ? 1 : 0);

                // Cập nhật sĩ số
                classRepository.updateStudentCount(clazz.getId(), studentAllocated);
                
                // Chuyển sang OPEN
                classStateMachineService.transition(clazz.getId(), ClassStatus.OPEN, "STATUS_CHANGED", 
                        "Tái phân bổ sĩ số đều: " + studentAllocated + " học viên. Đạt sĩ số tối thiểu (" + minStudents + "). Chuyển sang OPEN.");

                // Load lại bản ghi mới cập nhật
                SchoolClass updated = classRepository.findById(clazz.getId()).orElse(clazz);
                updatedClasses.add(updated);
            }

            return RebalanceResult.builder()
                    .action("REDISTRIBUTED")
                    .classes(updatedClasses)
                    .message("Cân bằng thành công cho " + n + " lớp. Sĩ số trung bình đạt " + avgStudents 
                            + " học viên (tối thiểu yêu cầu: " + minStudents + "). Tất cả lớp đã chuyển sang trạng thái OPEN.")
                    .build();

        } else if (totalStudents >= minStudents) {
            // Trường hợp 2: Trung bình không đủ nhưng tổng đạt chuẩn -> Cần gộp lớp (Suggest Merge)
            for (SchoolClass clazz : targets) {
                // Chuyển sang trạng thái REBALANCING để chờ thao tác gộp
                if (clazz.getStatus() != ClassStatus.REBALANCING) {
                    classStateMachineService.transition(clazz.getId(), ClassStatus.REBALANCING, "STATUS_CHANGED",
                            "Sĩ số tuyển sinh trung bình (" + avgStudents + ") không đạt tối thiểu (" + minStudents + "). Chuyển sang REBALANCING.");
                }
                updatedClasses.add(classRepository.findById(clazz.getId()).orElse(clazz));
            }

            return RebalanceResult.builder()
                    .action("SUGGEST_MERGE")
                    .classes(updatedClasses)
                    .message("Sĩ số tuyển sinh trung bình (" + avgStudents + " học viên) không đạt tối thiểu " + minStudents 
                            + " để duy trì " + n + " lớp. Tổng sĩ số đạt " + totalStudents 
                            + ". Vui lòng thực hiện GỘP LỚP để đảm bảo chất lượng mở lớp.")
                    .build();

        } else {
            // Trường hợp 3: Tổng sĩ số cũng không đạt tối thiểu -> Đề xuất hủy hoặc mở cưỡng bức (Suggest Cancel)
            for (SchoolClass clazz : targets) {
                if (clazz.getStatus() != ClassStatus.REBALANCING) {
                    classStateMachineService.transition(clazz.getId(), ClassStatus.REBALANCING, "STATUS_CHANGED",
                            "Tổng sĩ số tuyển sinh (" + totalStudents + ") dưới mức tối thiểu mở lớp (" + minStudents + "). Chuyển sang REBALANCING.");
                }
                updatedClasses.add(classRepository.findById(clazz.getId()).orElse(clazz));
            }

            return RebalanceResult.builder()
                    .action("SUGGEST_CANCEL")
                    .classes(updatedClasses)
                    .message("Tổng sĩ số thực tế tuyển sinh của lịch học này là " + totalStudents 
                            + " học viên, không đạt sĩ số tối thiểu mở lớp (" + minStudents 
                            + "). Đề xuất hủy lớp hoặc mở cưỡng bức (Force Open).")
                    .build();
        }
    }

    /**
     * Gộp nhiều lớp học nguồn (sẽ bị hủy) vào một lớp học đích.
     */
    @Transactional
    public MergeResult mergeClasses(List<Long> sourceClassIds, Long targetClassId) {
        SchoolClass targetClass = classRepository.findById(targetClassId)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học đích", targetClassId));

        if (targetClass.getStatus() != ClassStatus.ENROLLING && targetClass.getStatus() != ClassStatus.REBALANCING) {
            throw new IllegalStateException("Lớp học đích phải ở trạng thái ENROLLING hoặc REBALANCING.");
        }

        int targetStudentCount = targetClass.getStudentCount();
        List<Long> cancelledIds = new ArrayList<>();

        for (Long sourceId : sourceClassIds) {
            if (sourceId.equals(targetClassId)) {
                continue;
            }
            SchoolClass src = classRepository.findById(sourceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lớp học nguồn", sourceId));

            if (src.getStatus() != ClassStatus.ENROLLING && src.getStatus() != ClassStatus.REBALANCING) {
                throw new IllegalStateException("Lớp học nguồn '" + src.getClassCode() + "' phải ở trạng thái ENROLLING hoặc REBALANCING.");
            }

            targetStudentCount += src.getStudentCount();

            // Hủy lớp nguồn
            classStateMachineService.transition(sourceId, ClassStatus.CANCELLED, "MERGED", 
                    "Đã gộp toàn bộ học viên sang lớp đích " + targetClass.getClassCode() + ". Hủy lớp.");
            
            cancelledIds.add(sourceId);
        }

        // Cập nhật sĩ số mới cho lớp đích
        classRepository.updateStudentCount(targetClassId, targetStudentCount);

        // Chuyển lớp đích sang OPEN
        classStateMachineService.transition(targetClassId, ClassStatus.OPEN, "MERGED",
                "Đã nhận học viên gộp từ các lớp học khác. Sĩ số mới: " + targetStudentCount + ". Chuyển trạng thái sang OPEN.");

        SchoolClass updatedTarget = classRepository.findById(targetClassId).orElse(targetClass);

        return MergeResult.builder()
                .targetClass(updatedTarget)
                .cancelledClassIds(cancelledIds)
                .build();
    }

    /**
     * Mở lớp học thủ công.
     */
    @Transactional
    public void openClass(Long classId) {
        classStateMachineService.transition(classId, ClassStatus.OPEN, "STATUS_CHANGED", "Mở lớp thủ công.");
    }

    /**
     * Mở lớp học cưỡng bức (bỏ qua sĩ số tối thiểu).
     */
    @Transactional
    public void forceOpenClass(Long classId) {
        SchoolClass clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học", classId));
        
        classStateMachineService.transition(classId, ClassStatus.OPEN, "FORCE_OPENED", 
                "Mở lớp cưỡng bức (Sĩ số hiện tại: " + clazz.getStudentCount() + "). Chuyển sang OPEN.");
    }

    /**
     * Hủy lớp học kèm lý do.
     */
    @Transactional
    public void cancelClass(Long classId, String reason) {
        classStateMachineService.transition(classId, ClassStatus.CANCELLED, "CANCELLED", 
                "Hủy lớp học. Lý do: " + (reason != null && !reason.isBlank() ? reason : "Không có lý do"));
    }

    /**
     * Cập nhật thông tin chi tiết lớp học.
     */
    @Transactional
    public SchoolClass updateClass(Long id, ClassUpdateRequest request) {
        SchoolClass clazz = classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học", id));

        // 1. Chỉ cho phép cập nhật khi lớp khác FINISHED hoặc CANCELLED
        if (clazz.getStatus() == ClassStatus.FINISHED || clazz.getStatus() == ClassStatus.CANCELLED) {
            throw new BusinessException("Không thể chỉnh sửa lớp học ở trạng thái hiện tại");
        }

        // 2. Không cho phép cập nhật Sĩ số tối đa nhỏ hơn sĩ số học sinh hiện tại
        if (request.getStudentSize() != null && request.getStudentSize() < clazz.getStudentCount()) {
            throw new BusinessException("Sĩ số tối đa không được nhỏ hơn số học viên hiện tại");
        }

        // 3. Giới hạn cập nhật giáo viên phụ trách khi chưa xếp lịch chính thức
        if (request.getTeacherId() != null && !request.getTeacherId().equals(clazz.getTeacherId())) {
            // Lớp học chưa xếp lịch chính thức có trạng thái: DRAFT, ENROLLING, REBALANCING, PENDING
            boolean isUnscheduledStatus = clazz.getStatus() == ClassStatus.DRAFT 
                    || clazz.getStatus() == ClassStatus.ENROLLING 
                    || clazz.getStatus() == ClassStatus.REBALANCING 
                    || clazz.getStatus() == ClassStatus.PENDING;
            
            // Đồng thời chưa phát sinh buổi học nào có phân công (gán teacher_id hoặc room_id hoặc timeslot_id)
            List<Lesson> lessons = classRepository.findLessonsByClassId(id);
            boolean hasAssignments = lessons.stream().anyMatch(l -> 
                    l.getTeacherId() != null || l.getRoomId() != null || l.getTimeslotId() != null);

            if (!isUnscheduledStatus || hasAssignments) {
                throw new BusinessException("Không thể cập nhật giáo viên phụ trách cho lớp học đã xếp lịch chính thức");
            }
        }

        clazz.setName(request.getName());
        clazz.setClassCode(request.getName()); // name = classCode đồng bộ
        clazz.setStudentSize(request.getStudentSize());
        clazz.setTeacherId(request.getTeacherId());
        clazz.setNote(request.getNote());

        classRepository.updateClassDetails(clazz);
        log.info("Đã cập nhật thông tin lớp học ID: {}", id);
        return classRepository.findById(id).orElse(clazz);
    }

    /**
     * Xóa lớp học.
     */
    @Transactional
    public void deleteClass(Long id) {
        SchoolClass clazz = classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học", id));

        // 1. Chỉ cho phép xóa khi lớp ở trạng thái DRAFT hoặc CANCELLED
        if (clazz.getStatus() != ClassStatus.DRAFT && clazz.getStatus() != ClassStatus.CANCELLED) {
            throw new BusinessException("Không thể xóa lớp học ở trạng thái hiện tại");
        }

        // 2. Chặn xóa nếu lớp đã có lịch giảng dạy / phân công giáo viên
        List<Lesson> lessons = classRepository.findLessonsByClassId(id);
        boolean hasAssignments = lessons.stream().anyMatch(l -> 
                l.getTeacherId() != null || l.getRoomId() != null || l.getTimeslotId() != null);
        if (clazz.getTeacherId() != null || hasAssignments) {
            throw new BusinessException("Lớp học đã phát sinh lịch giảng dạy, không thể xóa");
        }

        // 3. Chặn xóa nếu đã có học viên đăng ký
        if (clazz.getStudentCount() != null && clazz.getStudentCount() > 0) {
            throw new BusinessException("Lớp học đã phát sinh dữ liệu học viên, không thể xóa");
        }

        classRepository.deleteClass(id);
        log.info("Đã xóa mềm lớp học ID: {}", id);
    }
}
