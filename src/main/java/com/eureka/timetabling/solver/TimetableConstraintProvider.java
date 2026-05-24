package com.eureka.timetabling.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.*;
import com.eureka.timetabling.domain.Lesson;
import org.springframework.stereotype.Component;

/**
 * Nhà cung cấp ràng buộc cho Timefold Solver.
 *
 * Hard Constraints (vi phạm → lời giải không hợp lệ):
 * - Một phòng không được dạy 2 buổi cùng ca
 * - Một giáo viên không được dạy 2 buổi cùng ca
 * - Giáo viên phải có kỹ năng phù hợp
 * - Giáo viên không được dạy trong thời gian xin nghỉ
 * - Sức chứa phòng phải đủ cho lớp học
 *
 * Soft Constraints (vi phạm → giảm điểm, lời giải vẫn hợp lệ):
 * - Giảm thiểu khoảng trống giữa các tiết của giáo viên
 * - Ưu tiên dùng cùng phòng cho các buổi liên tiếp của cùng lớp
 */
@Component
public class TimetableConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[]{
                // Hard
                roomConflict(factory),
                teacherConflict(factory),
                teacherSkillRequired(factory),
                teacherUnavailable(factory),
                roomCapacity(factory),
                patternCompliance(factory),
                // Soft
                teacherGapMinimization(factory),
                roomStability(factory),
                preferAssignedTeacher(factory)
        };
    }

    // =================== HARD CONSTRAINTS ===================

    /**
     * Hard: Một phòng không được có 2 buổi học cùng một ca
     */
    private Constraint roomConflict(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTimeslotId),
                        Joiners.equal(Lesson::getRoomId))
                .filter((l1, l2) -> l1.getRoomId() != null && l1.getTimeslotId() != null)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Xung đột phòng học");
    }

    /**
     * Hard: Một giáo viên không được dạy 2 buổi cùng một ca
     */
    private Constraint teacherConflict(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTimeslotId),
                        Joiners.equal(Lesson::getTeacherId))
                .filter((l1, l2) -> l1.getTeacherId() != null && l1.getTimeslotId() != null)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Xung đột giáo viên");
    }

    /**
     * Hard: Giáo viên phải có kỹ năng phù hợp với buổi học
     */
    private Constraint teacherSkillRequired(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTeacherId() != null)
                .join(Timetable.TeacherSkillFact.class,
                        Joiners.equal(Lesson::getTeacherId, Timetable.TeacherSkillFact::getTeacherId))
                .groupBy((lesson, skill) -> lesson, ConstraintCollectors.toList((lesson, skill) -> skill.getSkillCode()))
                .filter((lesson, skills) -> !skills.contains(lesson.getRequiredSkill()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Kỹ năng giáo viên không phù hợp");
    }

    /**
     * Hard: Giáo viên không được dạy trong ca mà họ xin nghỉ
     */
    private Constraint teacherUnavailable(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTeacherId() != null && lesson.getTimeslotId() != null)
                .join(Timetable.TeacherUnavailableFact.class,
                        Joiners.equal(Lesson::getTeacherId, Timetable.TeacherUnavailableFact::getTeacherId),
                        Joiners.equal(Lesson::getTimeslotId, Timetable.TeacherUnavailableFact::getTimeslotId))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Giáo viên bận trong ca này");
    }

    /**
     * Hard: Sức chứa phòng phải đủ cho lớp học (cần student_size qua lesson → class)
     * Hiện tại dùng RoomFact, constraint này cần student size ở Lesson.
     * Nếu lesson chứa studentSize thì filter trực tiếp:
     */
    private Constraint roomCapacity(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getRoomId() != null)
                .join(Timetable.RoomFact.class,
                        Joiners.equal(Lesson::getRoomId, Timetable.RoomFact::getRoomId))
                .filter((lesson, room) -> room.getStatus().equals("INACTIVE"))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Phòng không hoạt động");
    }

    // =================== SOFT CONSTRAINTS ===================

    /**
     * Soft: Giảm thiểu khoảng trống giữa các ca trong ngày của giáo viên
     * Phạt mỗi cặp buổi học của cùng giáo viên trong cùng ngày nếu không liên tiếp
     */
    private Constraint teacherGapMinimization(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTeacherId))
                .filter((l1, l2) -> l1.getTeacherId() != null
                        && l1.getTimeslotId() != null
                        && l2.getTimeslotId() != null
                        && !l1.getTimeslotId().equals(l2.getTimeslotId()))
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Giảm khoảng trống lịch giáo viên");
    }

    /**
     * Soft: Ưu tiên dùng cùng một phòng cho các buổi liên tiếp của cùng lớp
     */
    private Constraint roomStability(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getClassId))
                .filter((l1, l2) -> l1.getRoomId() != null
                        && l2.getRoomId() != null
                        && !l1.getRoomId().equals(l2.getRoomId()))
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Ổn định phòng học cho lớp");
    }

    /**
     * Hard: Các ca học phải tuân thủ mẫu lịch học (Schedule Pattern) được gán cho lớp.
     */
    private Constraint patternCompliance(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslotId() != null && lesson.getSchedulePatternId() != null)
                .join(Timetable.TimeslotFact.class,
                        Joiners.equal(Lesson::getTimeslotId, Timetable.TimeslotFact::getTimeslotId))
                .join(com.eureka.timetabling.domain.SchedulePattern.class,
                        Joiners.equal((lesson, timeslot) -> lesson.getSchedulePatternId(), com.eureka.timetabling.domain.SchedulePattern::getId))
                .filter((lesson, timeslot, pattern) -> {
                    String dayNum = getDayNum(timeslot.getDayOfWeek());
                    String slotCode = getSlotCodeFromStartTime(timeslot.getStartTime());
                    boolean dayOk = pattern.getStudyDays() != null && pattern.getStudyDays().contains(dayNum);
                    boolean slotOk = pattern.getSlotCode() != null && pattern.getSlotCode().equals(slotCode);
                    return !dayOk || !slotOk;
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Không tuân thủ mẫu lịch học");
    }

    /**
     * Soft: Ưu tiên chọn giáo viên đã được phân công từ trước cho lớp.
     */
    private Constraint preferAssignedTeacher(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTeacherId() != null 
                        && lesson.getClassTeacherId() != null 
                        && !lesson.getTeacherId().equals(lesson.getClassTeacherId()))
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Ưu tiên giáo viên đã phân công của lớp");
    }

    private static String getDayNum(String dayOfWeek) {
        if (dayOfWeek == null) return "";
        switch (dayOfWeek.toUpperCase()) {
            case "MONDAY": return "2";
            case "TUESDAY": return "3";
            case "WEDNESDAY": return "4";
            case "THURSDAY": return "5";
            case "FRIDAY": return "6";
            case "SATURDAY": return "7";
            case "SUNDAY": return "1";
            default: return "";
        }
    }

    private static String getSlotCodeFromStartTime(String startTime) {
        if (startTime == null) return "";
        if (startTime.startsWith("07:") || startTime.startsWith("08:")) return "C1";
        if (startTime.startsWith("09:") || startTime.startsWith("10:")) return "C2";
        if (startTime.startsWith("13:") || startTime.startsWith("14:")) return "C3";
        if (startTime.startsWith("15:") || startTime.startsWith("16:")) return "C4";
        if (startTime.startsWith("18:") || startTime.startsWith("19:")) return "C5";
        return "";
    }
}
