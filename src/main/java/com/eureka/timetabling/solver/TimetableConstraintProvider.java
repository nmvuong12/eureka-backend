package com.eureka.timetabling.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.*;
import com.eureka.timetabling.domain.Lesson;
import com.eureka.timetabling.domain.ConstraintConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nhà cung cấp ràng buộc cho Timefold Solver.
 * Đã được tối ưu hóa vượt bậc:
 * - Tránh giới hạn join stream của Timefold (QuadStream limit) bằng cách sử dụng bộ nhớ đệm O(1) tĩnh ở solver startup.
 * - Cho phép bật/tắt và tinh chỉnh trọng số động từ Database/UI.
 */
public class TimetableConstraintProvider implements ConstraintProvider {

    private static final Map<Long, TimeslotInfo> timeslotMap = new ConcurrentHashMap<>();
    private static final Map<Long, String> teacherTypeMap = new ConcurrentHashMap<>();
    private static final Map<String, ConstraintConfig> configMap = new ConcurrentHashMap<>();

    // Timefold requires a public no-arg constructor
    public TimetableConstraintProvider() {
    }

    /**
     * Khởi tạo dữ liệu hỗ trợ tối ưu hóa hiệu năng và trọng số phạt động trước khi bắt đầu xếp lịch.
     */
    public static void initSolverData(
            List<ConstraintConfig> configs,
            List<TimeslotInfo> timeslots,
            Map<Long, String> teacherTypes
    ) {
        configMap.clear();
        if (configs != null) {
            for (ConstraintConfig c : configs) {
                configMap.put(c.getConstraintKey(), c);
            }
        }

        timeslotMap.clear();
        if (timeslots != null) {
            for (TimeslotInfo ts : timeslots) {
                timeslotMap.put(ts.getId(), ts);
            }
        }

        teacherTypeMap.clear();
        if (teacherTypes != null) {
            teacherTypeMap.putAll(teacherTypes);
        }
    }

    private static int getDynamicWeight(String key) {
        ConstraintConfig config = configMap.get(key);
        if (config == null || !config.isEnabled()) {
            return 0;
        }
        return config.getWeight() != null ? config.getWeight() : 0;
    }

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        List<Constraint> constraintList = new ArrayList<>();

        // --- HARD CONSTRAINTS (Luôn hoạt động) ---
        constraintList.add(roomConflict(factory));
        constraintList.add(teacherConflict(factory));
        constraintList.add(teacherSkillRequired(factory));
        constraintList.add(teacherNoSkills(factory));
        constraintList.add(teacherUnavailable(factory));
        constraintList.add(roomCapacity(factory));
        constraintList.add(patternCompliance(factory));

        // --- DYNAMIC SOFT CONSTRAINTS (Trọng số phạt động O(1) Lookup) ---
        constraintList.add(teacherGapMinimization(factory));
        constraintList.add(roomStability(factory));
        constraintList.add(preferAssignedTeacher(factory));
        constraintList.add(teacherStability(factory));
        constraintList.add(preferExactSkillLevel(factory));
        constraintList.add(teacherRoomStability(factory));
        constraintList.add(teacherMaxConsecutive(factory));
        constraintList.add(teacherMaxDailyLoad(factory));
        constraintList.add(teacherTypePreference(factory));
        constraintList.add(teacherLoadBalance(factory));

        return constraintList.toArray(new Constraint[0]);
    }

    // =================== HARD CONSTRAINTS ===================

    private Constraint roomConflict(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTimeslotId),
                        Joiners.equal(Lesson::getRoomId))
                .filter((l1, l2) -> l1.getRoomId() != null && l1.getTimeslotId() != null)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Xung đột phòng học");
    }

    private Constraint teacherConflict(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTimeslotId),
                        Joiners.equal(Lesson::getTeacherId))
                .filter((l1, l2) -> l1.getTeacherId() != null && l1.getTimeslotId() != null)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Xung đột giáo viên");
    }

    private Constraint teacherSkillRequired(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTeacherId() != null && lesson.getRequiredSkill() != null)
                .join(Timetable.TeacherSkillFact.class,
                        Joiners.equal(Lesson::getTeacherId, Timetable.TeacherSkillFact::getTeacherId))
                .groupBy((lesson, skill) -> lesson, ConstraintCollectors.toList((lesson, skill) -> skill.getSkillCode()))
                .filter((lesson, skills) -> {
                    return skills.stream().noneMatch(skillCode ->
                            SkillMetadataHolder.isCompatible(skillCode, lesson.getRequiredSkill()));
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Kỹ năng giáo viên không phù hợp");
    }

    private Constraint teacherNoSkills(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTeacherId() != null)
                .ifNotExists(Timetable.TeacherSkillFact.class,
                        Joiners.equal(Lesson::getTeacherId, Timetable.TeacherSkillFact::getTeacherId))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Giáo viên không có bất kỳ kỹ năng nào");
    }

    private Constraint teacherUnavailable(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTeacherId() != null && lesson.getTimeslotId() != null)
                .join(Timetable.TeacherUnavailableFact.class,
                        Joiners.equal(Lesson::getTeacherId, Timetable.TeacherUnavailableFact::getTeacherId),
                        Joiners.equal(Lesson::getTimeslotId, Timetable.TeacherUnavailableFact::getTimeslotId))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Giáo viên bận trong ca này");
    }

    private Constraint roomCapacity(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getRoomId() != null)
                .join(Timetable.RoomFact.class,
                        Joiners.equal(Lesson::getRoomId, Timetable.RoomFact::getRoomId))
                .filter((lesson, room) -> room.getStatus().equals("INACTIVE"))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Phòng không hoạt động");
    }

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

    // =================== DYNAMIC SOFT CONSTRAINTS ===================

    private Constraint teacherGapMinimization(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTeacherId),
                        Joiners.equal(Lesson::getSessionDate))
                .filter((l1, l2) -> {
                    if (l1.getTeacherId() == null || l1.getTimeslotId() == null || l2.getTimeslotId() == null) return false;
                    TimeslotInfo ts1 = timeslotMap.get(l1.getTimeslotId());
                    TimeslotInfo ts2 = timeslotMap.get(l2.getTimeslotId());
                    if (ts1 == null || ts2 == null) return false;
                    return Math.abs(ts1.getSlotIndex() - ts2.getSlotIndex()) > 1;
                })
                .penalize(HardSoftScore.ONE_SOFT, (l1, l2) -> getDynamicWeight("teacher_gap"))
                .asConstraint("Giảm khoảng trống lịch giáo viên");
    }

    private Constraint roomStability(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getClassId))
                .filter((l1, l2) -> l1.getRoomId() != null
                        && l2.getRoomId() != null
                        && !l1.getRoomId().equals(l2.getRoomId()))
                .penalize(HardSoftScore.ONE_SOFT, (l1, l2) -> getDynamicWeight("room_stability"))
                .asConstraint("Ổn định phòng học cho lớp");
    }

    private Constraint preferAssignedTeacher(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTeacherId() != null 
                        && lesson.getClassTeacherId() != null 
                        && !lesson.getTeacherId().equals(lesson.getClassTeacherId()))
                .penalize(HardSoftScore.ONE_SOFT, lesson -> getDynamicWeight("prefer_assigned_teacher"))
                .asConstraint("Ưu tiên giáo viên đã phân công của lớp");
    }

    private Constraint teacherStability(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getClassId))
                .filter((l1, l2) -> l1.getTeacherId() != null
                        && l2.getTeacherId() != null
                        && !l1.getTeacherId().equals(l2.getTeacherId()))
                .penalize(HardSoftScore.ONE_SOFT, (l1, l2) -> getDynamicWeight("teacher_stability"))
                .asConstraint("Ổn định giáo viên cho lớp");
    }

    private Constraint preferExactSkillLevel(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTeacherId() != null && lesson.getRequiredSkill() != null)
                .join(Timetable.TeacherSkillFact.class,
                        Joiners.equal(Lesson::getTeacherId, Timetable.TeacherSkillFact::getTeacherId))
                .groupBy((lesson, skill) -> lesson, ConstraintCollectors.toList((lesson, skill) -> skill.getSkillCode()))
                .penalize(HardSoftScore.ONE_SOFT, (lesson, skills) -> {
                    return SkillMetadataHolder.getMinRankDifference(skills, lesson.getRequiredSkill()) * getDynamicWeight("prefer_exact_skill_level");
                })
                .asConstraint("Ưu tiên giáo viên trình độ vừa khít");
    }

    private Constraint teacherRoomStability(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTeacherId),
                        Joiners.equal(Lesson::getSessionDate))
                .filter((l1, l2) -> {
                    if (l1.getTeacherId() == null || l1.getRoomId() == null || l2.getRoomId() == null) return false;
                    if (l1.getRoomId().equals(l2.getRoomId())) return false;
                    TimeslotInfo ts1 = timeslotMap.get(l1.getTimeslotId());
                    TimeslotInfo ts2 = timeslotMap.get(l2.getTimeslotId());
                    if (ts1 == null || ts2 == null) return false;
                    return Math.abs(ts1.getSlotIndex() - ts2.getSlotIndex()) == 1;
                })
                .penalize(HardSoftScore.ONE_SOFT, (l1, l2) -> getDynamicWeight("teacher_room_stability"))
                .asConstraint("Giữ cố định phòng dạy cho Giáo viên");
    }

    private Constraint teacherMaxConsecutive(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTeacherId),
                        Joiners.equal(Lesson::getSessionDate))
                .join(Lesson.class,
                        Joiners.equal((l1, l2) -> l1.getTeacherId(), Lesson::getTeacherId),
                        Joiners.equal((l1, l2) -> l1.getSessionDate(), Lesson::getSessionDate))
                .filter((l1, l2, l3) -> l1.getId() < l2.getId() && l2.getId() < l3.getId())
                .filter((l1, l2, l3) -> {
                    if (l1.getTimeslotId() == null || l2.getTimeslotId() == null || l3.getTimeslotId() == null) return false;
                    TimeslotInfo ts1 = timeslotMap.get(l1.getTimeslotId());
                    TimeslotInfo ts2 = timeslotMap.get(l2.getTimeslotId());
                    TimeslotInfo ts3 = timeslotMap.get(l3.getTimeslotId());
                    if (ts1 == null || ts2 == null || ts3 == null) return false;
                    int v1 = ts1.getSlotIndex();
                    int v2 = ts2.getSlotIndex();
                    int v3 = ts3.getSlotIndex();
                    int min = Math.min(v1, Math.min(v2, v3));
                    int max = Math.max(v1, Math.max(v2, v3));
                    int mid = v1 + v2 + v3 - min - max;
                    return (mid - min == 1) && (max - mid == 1);
                })
                .penalize(HardSoftScore.ONE_SOFT, (l1, l2, l3) -> getDynamicWeight("teacher_max_consecutive"))
                .asConstraint("Hạn chế dạy liên tiếp không nghỉ");
    }

    private Constraint teacherMaxDailyLoad(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(l -> l.getTeacherId() != null && l.getSessionDate() != null)
                .groupBy(Lesson::getTeacherId, Lesson::getSessionDate, ConstraintCollectors.count())
                .filter((teacherId, date, count) -> count > 3)
                .penalize(HardSoftScore.ONE_SOFT, (teacherId, date, count) -> (count - 3) * getDynamicWeight("teacher_max_daily_load"))
                .asConstraint("Giới hạn số ca dạy tối đa trong ngày");
    }

    private Constraint teacherTypePreference(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(l -> {
                    if (l.getTeacherId() == null || l.getTimeslotId() == null) return false;
                    TimeslotInfo ts = timeslotMap.get(l.getTimeslotId());
                    String type = teacherTypeMap.get(l.getTeacherId());
                    if (ts == null || type == null) return false;
                    
                    int slot = ts.getSlotIndex();
                    if ("FULL_TIME".equals(type) && slot == 5) {
                        return true;
                    }
                    if ("PART_TIME".equals(type) && (slot == 1 || slot == 2 || slot == 3)) {
                        return true;
                    }
                    return false;
                })
                .penalize(HardSoftScore.ONE_SOFT, lesson -> getDynamicWeight("teacher_type_preference"))
                .asConstraint("Ưu tiên ca dạy theo hợp đồng giáo viên");
    }

    private Constraint teacherLoadBalance(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(l -> l.getTeacherId() != null)
                .groupBy(Lesson::getTeacherId, ConstraintCollectors.count())
                .penalize(HardSoftScore.ONE_SOFT, (teacherId, count) -> count * count * getDynamicWeight("teacher_load_balance"))
                .asConstraint("Cân bằng tải dạy giữa các giáo viên");
    }

    // =================== HELPER METHODS ===================

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
        if (startTime.startsWith("15:") || startTime.startsWith("16:") || startTime.startsWith("17:")) return "C4";
        if (startTime.startsWith("18:") || startTime.startsWith("19:")) return "C5";
        return "";
    }

    public static int getSlotIndex(String startTime) {
        if (startTime == null) return 0;
        if (startTime.startsWith("07:") || startTime.startsWith("08:")) return 1;
        if (startTime.startsWith("09:") || startTime.startsWith("10:")) return 2;
        if (startTime.startsWith("13:") || startTime.startsWith("14:")) return 3;
        if (startTime.startsWith("15:") || startTime.startsWith("16:") || startTime.startsWith("17:")) return 4;
        if (startTime.startsWith("18:") || startTime.startsWith("19:")) return 5;
        return 0;
    }

    // ===== Helper Timeslot Fact class for O(1) Cache Lookup =====
    public static class TimeslotInfo {
        private final Long id;
        private final String dayOfWeek;
        private final int slotIndex;
        private final String startTime;

        public TimeslotInfo(Long id, String dayOfWeek, String startTime) {
            this.id = id;
            this.dayOfWeek = dayOfWeek;
            this.startTime = startTime;
            this.slotIndex = TimetableConstraintProvider.getSlotIndex(startTime);
        }

        public Long getId() { return id; }
        public String getDayOfWeek() { return dayOfWeek; }
        public int getSlotIndex() { return slotIndex; }
        public String getStartTime() { return startTime; }
    }
}
