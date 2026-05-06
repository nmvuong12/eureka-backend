package com.eureka.timetabling.controller;

import com.eureka.timetabling.domain.Room;
import com.eureka.timetabling.domain.Timeslot;
import com.eureka.timetabling.dto.request.RoomRequest;
import com.eureka.timetabling.dto.request.TimeslotRequest;
import com.eureka.timetabling.dto.response.ApiResponse;
import com.eureka.timetabling.exception.ResourceNotFoundException;
import com.eureka.timetabling.repository.RoomRepository;
import com.eureka.timetabling.repository.TimeslotRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API quản lý phòng học và ca học
 */
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class RoomTimeslotController {

    private final RoomRepository roomRepository;
    private final TimeslotRepository timeslotRepository;

    // ===== Phòng học =====

    @GetMapping("/rooms")
    @Tag(name = "Phòng học")
    @Operation(summary = "Danh sách phòng học")
    public ResponseEntity<ApiResponse<List<Room>>> getRooms() {
        return ResponseEntity.ok(ApiResponse.success(roomRepository.findAll()));
    }

    @PostMapping("/rooms")
    @Tag(name = "Phòng học")
    @Operation(summary = "Tạo phòng học mới")
    public ResponseEntity<ApiResponse<Room>> createRoom(@Valid @RequestBody RoomRequest request) {
        Room room = Room.builder()
                .name(request.getName())
                .capacity(request.getCapacity())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .build();
        Long id = roomRepository.save(room);
        room.setId(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo phòng thành công", room));
    }

    @PutMapping("/rooms/{id}")
    @Tag(name = "Phòng học")
    @Operation(summary = "Cập nhật phòng học")
    public ResponseEntity<ApiResponse<Room>> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phòng học", id));
        room.setName(request.getName());
        room.setCapacity(request.getCapacity());
        if (request.getStatus() != null) room.setStatus(request.getStatus());
        roomRepository.update(room);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật phòng thành công", room));
    }

    @DeleteMapping("/rooms/{id}")
    @Tag(name = "Phòng học")
    @Operation(summary = "Xóa phòng học")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable Long id) {
        roomRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa phòng thành công", null));
    }

    // ===== Ca học =====

    @GetMapping("/timeslots")
    @Tag(name = "Ca học")
    @Operation(summary = "Danh sách ca học")
    public ResponseEntity<ApiResponse<List<Timeslot>>> getTimeslots() {
        return ResponseEntity.ok(ApiResponse.success(timeslotRepository.findAll()));
    }

    @PostMapping("/timeslots")
    @Tag(name = "Ca học")
    @Operation(summary = "Tạo ca học mới")
    public ResponseEntity<ApiResponse<Timeslot>> createTimeslot(@Valid @RequestBody TimeslotRequest request) {
        Timeslot timeslot = Timeslot.builder()
                .dayOfWeek(request.getDayOfWeek())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .label(request.getLabel())
                .build();
        Long id = timeslotRepository.save(timeslot);
        timeslot.setId(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo ca học thành công", timeslot));
    }

    @DeleteMapping("/timeslots/{id}")
    @Tag(name = "Ca học")
    @Operation(summary = "Xóa ca học")
    public ResponseEntity<ApiResponse<Void>> deleteTimeslot(@PathVariable Long id) {
        timeslotRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa ca học thành công", null));
    }
}
