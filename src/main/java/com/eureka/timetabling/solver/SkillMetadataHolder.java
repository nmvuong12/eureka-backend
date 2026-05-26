package com.eureka.timetabling.solver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holder quản lý siêu dữ liệu kỹ năng (Skill Metadata Cache)
 * phục vụ đối chiếu phân cấp trình độ (Hierarchical Skill Matching) trong bộ giải Timefold Solver.
 */
public class SkillMetadataHolder {

    public static class SkillInfo {
        private final String group;
        private final int rank;

        public SkillInfo(String group, int rank) {
            this.group = group;
            this.rank = rank;
        }

        public String getGroup() {
            return group;
        }

        public int getRank() {
            return rank;
        }
    }

    private static final Map<String, SkillInfo> skillMap = new ConcurrentHashMap<>();

    /**
     * Đồng bộ hóa danh mục kỹ năng từ database vào solver
     */
    public static void setSkills(Map<String, SkillInfo> newSkills) {
        skillMap.clear();
        if (newSkills != null) {
            skillMap.putAll(newSkills);
        }
    }

    /**
     * Kiểm tra tính tương thích giữa kỹ năng của giáo viên và kỹ năng yêu cầu của lớp
     */
    public static boolean isCompatible(String teacherSkillCode, String requiredSkillCode) {
        if (requiredSkillCode == null) {
            return true;
        }
        if (teacherSkillCode == null) {
            return false;
        }
        if (teacherSkillCode.equalsIgnoreCase(requiredSkillCode)) {
            return true;
        }

        SkillInfo req = skillMap.get(requiredSkillCode);
        SkillInfo teach = skillMap.get(teacherSkillCode);

        // Nếu cả hai thuộc cùng một nhóm phân cấp, giáo viên có rank cao hơn hoặc bằng sẽ đủ chuẩn dạy
        if (req != null && teach != null 
                && req.getGroup() != null && req.getGroup().equalsIgnoreCase(teach.getGroup())) {
            return teach.getRank() >= req.getRank();
        }

        return false;
    }

    /**
     * Tính toán độ chênh lệch rank tối thiểu giữa các kỹ năng tương thích của giáo viên và kỹ năng yêu cầu.
     * Dùng để phạt soft nhằm ưu tiên giáo viên có trình độ vừa khít nhất.
     */
    public static int getMinRankDifference(java.util.List<String> teacherSkillCodes, String requiredSkillCode) {
        if (requiredSkillCode == null || teacherSkillCodes == null || teacherSkillCodes.isEmpty()) {
            return 0;
        }
        SkillInfo req = skillMap.get(requiredSkillCode);
        if (req == null) {
            return 0;
        }

        int minDiff = Integer.MAX_VALUE;
        boolean foundCompatible = false;

        for (String teachSkillCode : teacherSkillCodes) {
            if (teachSkillCode.equalsIgnoreCase(requiredSkillCode)) {
                return 0; // Vừa khít hoàn hảo
            }
            SkillInfo teach = skillMap.get(teachSkillCode);
            if (teach != null && req.getGroup() != null && req.getGroup().equalsIgnoreCase(teach.getGroup())) {
                int diff = teach.getRank() - req.getRank();
                if (diff >= 0 && diff < minDiff) {
                    minDiff = diff;
                    foundCompatible = true;
                }
            }
        }

        return foundCompatible ? minDiff : 0;
    }
}
