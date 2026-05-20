package com.eureka.timetabling.service.impl;

import com.eureka.timetabling.domain.*;
import com.eureka.timetabling.dto.request.TeacherRequest;
import com.eureka.timetabling.dto.request.TeacherAvailabilityRequest;
import com.eureka.timetabling.dto.response.TeacherResponse;
import com.eureka.timetabling.dto.response.PageResponse;
import com.eureka.timetabling.exception.BusinessException;
import com.eureka.timetabling.exception.ResourceNotFoundException;
import com.eureka.timetabling.repository.TeacherRepository;
import com.eureka.timetabling.repository.TeacherAvailabilityRepository;
import com.eureka.timetabling.service.TeacherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Lớp triển khai Service quản lý Giáo viên (TeacherServiceImpl)
 * Triển khai đầy đủ nghiệp vụ theo yêu cầu chặt chẽ, tối ưu hiệu năng và kiểm soát lỗi toàn diện.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final TeacherRepository teacherRepository;
    private final TeacherAvailabilityRepository teacherAvailabilityRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TeacherResponse> findAll(String status) {
        log.info("Lấy toàn bộ danh sách giáo viên chưa xóa, lọc trạng thái: {}", status);
        return teacherRepository.findAll(status).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TeacherResponse> search(String teacherCode, String fullName, String skill,
                                                 String teacherType, String workingStatus,
                                                 int page, int size, String sortBy, String sortDir) {
        log.info("Tìm kiếm giáo viên nâng cao: code={}, name={}, skill={}, type={}, status={}, page={}, size={}",
                teacherCode, fullName, skill, teacherType, workingStatus, page, size);
        
        List<TeacherResponse> data = teacherRepository.search(teacherCode, fullName, skill, teacherType, 
                        workingStatus, page, size, sortBy, sortDir)
                .stream()
                .map(this::toResponse)
                .toList();

        long total = teacherRepository.countSearch(teacherCode, fullName, skill, teacherType, workingStatus);
        
        return PageResponse.of(data, page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherResponse findById(Long id) {
        log.info("Lấy chi tiết giáo viên với ID: {}", id);
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Giáo viên", id));
        return toResponse(teacher);
    }

    @Override
    @Transactional
    public TeacherResponse create(TeacherRequest request) {
        log.info("Tiến hành thêm mới giáo viên với Email: {}", request.getEmail());

        // 1. Kiểm tra ngày sinh không lớn hơn ngày hiện tại
        if (request.getDateOfBirth() != null && request.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new BusinessException("Ngày sinh không được lớn hơn ngày hiện tại");
        }

        // 2. Kiểm tra duy nhất Email
        if (teacherRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email '" + request.getEmail() + "' đã được sử dụng trong hệ thống");
        }

        // 3. Kiểm tra duy nhất Số điện thoại
        if (teacherRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("Số điện thoại '" + request.getPhone() + "' đã được sử dụng trong hệ thống");
        }

        // 4. Tự động sinh mã giáo viên dạng GVxxxx
        String teacherCode = generateNextTeacherCode();
        log.info("Sinh mã giáo viên tự động thành công: {}", teacherCode);

        // 5. Build thực thể Teacher và lưu cơ sở dữ liệu
        Teacher teacher = Teacher.builder()
                .teacherCode(teacherCode)
                .teacherType(TeacherType.valueOf(request.getTeacherType()))
                .fullName(request.getFullName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(Gender.valueOf(request.getGender()))
                .address(request.getAddress())
                .email(request.getEmail())
                .phone(request.getPhone())
                .skills(request.getSkills())
                .certificateFile(request.getCertificateFile())
                .profileFile(request.getProfileFile())
                .workingStatus(WorkingStatus.valueOf(request.getWorkingStatus()))
                .build();

        Long newId = teacherRepository.save(teacher);
        log.info("Đã lưu thông tin giáo viên vào Database với ID: {}", newId);

        // 6. Đồng bộ hóa với bảng liên kết kỹ năng cũ để giữ tương thích ngược
        synchronizeSkills(newId, request.getSkills());

        return findById(newId);
    }

    @Override
    @Transactional
    public TeacherResponse update(Long id, TeacherRequest request) {
        log.info("Tiến hành cập nhật giáo viên ID: {}", id);

        // 1. Kiểm tra giáo viên tồn tại
        Teacher existing = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Giáo viên", id));

        // 2. Kiểm tra ngày sinh hợp lệ
        if (request.getDateOfBirth() != null && request.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new BusinessException("Ngày sinh không được lớn hơn ngày hiện tại");
        }

        // 3. Kiểm tra duy nhất Email (loại trừ ID hiện tại)
        if (teacherRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new BusinessException("Email '" + request.getEmail() + "' đã được sử dụng bởi giáo viên khác");
        }

        // 4. Kiểm tra duy nhất Số điện thoại (loại trừ ID hiện tại)
        if (teacherRepository.existsByPhoneAndIdNot(request.getPhone(), id)) {
            throw new BusinessException("Số điện thoại '" + request.getPhone() + "' đã được sử dụng bởi giáo viên khác");
        }

        // 5. Cập nhật các trường thông tin thay đổi
        existing.setTeacherType(TeacherType.valueOf(request.getTeacherType()));
        existing.setFullName(request.getFullName());
        existing.setDateOfBirth(request.getDateOfBirth());
        existing.setGender(Gender.valueOf(request.getGender()));
        existing.setAddress(request.getAddress());
        existing.setEmail(request.getEmail());
        existing.setPhone(request.getPhone());
        existing.setSkills(request.getSkills());
        existing.setCertificateFile(request.getCertificateFile());
        existing.setProfileFile(request.getProfileFile());
        existing.setWorkingStatus(WorkingStatus.valueOf(request.getWorkingStatus()));

        teacherRepository.update(existing);
        log.info("Cập nhật thông tin giáo viên ID {} thành công", id);

        // 6. Đồng bộ bảng kỹ năng liên kết
        synchronizeSkills(id, request.getSkills());

        return findById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Tiến hành xóa giáo viên ID: {}", id);

        // 1. Kiểm tra tồn tại
        teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Giáo viên", id));

        // 2. Kiểm tra ràng buộc: Không cho phép xóa nếu đang có lịch dạy hoặc được phân công lớp
        if (teacherRepository.hasAssignments(id)) {
            throw new BusinessException("Không cho phép xóa giáo viên này vì đang có lịch giảng dạy hoặc đang được phân công lớp học");
        }

        // 3. Thực hiện Soft Delete (Xóa mềm)
        teacherRepository.deleteById(id);
        log.info("Đã thực hiện xóa mềm giáo viên ID: {}", id);
    }

    @Override
    @Transactional
    public void registerAvailability(TeacherAvailabilityRequest request) {
        log.info("Giáo viên ID {} đăng ký lịch rảnh vào {}", request.getTeacherId(), request.getDayOfWeek());

        // 1. Tìm kiếm giáo viên
        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Giáo viên", request.getTeacherId()));

        // 2. Ràng buộc: Chỉ giáo viên bán thời gian (PART_TIME) mới được đăng ký lịch rảnh
        if (teacher.getTeacherType() != TeacherType.PART_TIME) {
            throw new BusinessException("Chỉ giáo viên bán thời gian (PART_TIME) mới được đăng ký lịch rảnh");
        }

        // 3. Phân tích và kiểm tra ràng buộc giờ bắt đầu < giờ kết thúc
        LocalTime startTime = LocalTime.parse(request.getStartTime(), DateTimeFormatter.ofPattern("HH:mm"));
        LocalTime endTime = LocalTime.parse(request.getEndTime(), DateTimeFormatter.ofPattern("HH:mm"));

        if (!startTime.isBefore(endTime)) {
            throw new BusinessException("Thời gian bắt đầu rảnh (startTime) phải nhỏ hơn thời gian kết thúc (endTime)");
        }

        // 4. Kiểm tra xung đột thời gian (overlapping timeslot)
        boolean hasOverlap = teacherAvailabilityRepository.checkOverlap(
                request.getTeacherId(), request.getDayOfWeek(), startTime, endTime);
        
        if (hasOverlap) {
            throw new BusinessException("Lịch đăng ký bị trùng lặp thời gian với các đăng ký lịch rảnh khác trong cùng ngày của giáo viên này");
        }

        // 5. Lưu lịch rảnh vào DB
        TeacherAvailability availability = TeacherAvailability.builder()
                .teacherId(request.getTeacherId())
                .dayOfWeek(request.getDayOfWeek())
                .startTime(startTime)
                .endTime(endTime)
                .build();

        teacherAvailabilityRepository.save(availability);
        log.info("Đăng ký lịch rảnh thành công cho giáo viên ID {} [{} {}-{}]", 
                request.getTeacherId(), request.getDayOfWeek(), startTime, endTime);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherAvailability> getAvailabilities(Long teacherId) {
        log.info("Lấy danh sách lịch rảnh của giáo viên ID: {}", teacherId);
        // Kiểm tra giáo viên tồn tại
        teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Giáo viên", teacherId));
        return teacherAvailabilityRepository.findByTeacherId(teacherId);
    }

    @Override
    @Transactional
    public void deleteAvailability(Long id) {
        log.info("Xóa lịch rảnh ID: {}", id);
        int deletedRows = teacherAvailabilityRepository.deleteById(id);
        if (deletedRows == 0) {
            throw new ResourceNotFoundException("Lịch rảnh", id);
        }
    }

    /**
     * Thuật toán sinh mã giáo viên tự động dạng GVxxxx
     */
    private String generateNextTeacherCode() {
        Optional<String> maxCodeOpt = teacherRepository.findMaxTeacherCode();
        if (maxCodeOpt.isEmpty() || maxCodeOpt.get() == null || maxCodeOpt.get().isBlank()) {
            return "GV0001";
        }
        
        String maxCode = maxCodeOpt.get().trim();
        if (maxCode.startsWith("GV") && maxCode.length() > 2) {
            try {
                String numericPart = maxCode.substring(2);
                int currentNumber = Integer.parseInt(numericPart);
                int nextNumber = currentNumber + 1;
                return String.format("GV%04d", nextNumber);
            } catch (NumberFormatException e) {
                log.warn("Mã giáo viên lớn nhất trong DB '{}' không đúng chuẩn số, bắt đầu sinh mới từ GV0001", maxCode);
                return "GV0001";
            }
        }
        return "GV0001";
    }

    /**
     * Đồng bộ hóa chuỗi skills với bảng kỹ năng liên kết cũ để đảm bảo tính nhất quán dữ liệu toàn cục
     */
    private void synchronizeSkills(Long teacherId, String skillsString) {
        if (skillsString == null || skillsString.isBlank()) {
            teacherRepository.replaceSkills(teacherId, Collections.emptyList());
            return;
        }

        // Tách chuỗi bằng dấu phẩy, làm sạch khoảng trắng
        List<String> skillCodes = Arrays.stream(skillsString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        teacherRepository.replaceSkills(teacherId, skillCodes);
    }

    /**
     * Ánh xạ từ thực thể Domain Teacher sang DTO TeacherResponse
     */
    private TeacherResponse toResponse(Teacher t) {
        return TeacherResponse.builder()
                .id(t.getId())
                .teacherCode(t.getTeacherCode())
                .teacherType(t.getTeacherType().name())
                .fullName(t.getFullName())
                .dateOfBirth(t.getDateOfBirth())
                .gender(t.getGender().name())
                .address(t.getAddress())
                .email(t.getEmail())
                .phone(t.getPhone())
                .skills(t.getSkills())
                .certificateFile(t.getCertificateFile())
                .profileFile(t.getProfileFile())
                .workingStatus(t.getWorkingStatus().name())
                .createdDate(t.getCreatedDate())
                .modifiedDate(t.getModifiedDate())
                .build();
    }
}
