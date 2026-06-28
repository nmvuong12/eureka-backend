# Eureka English Center - Timetabling System (Backend Core)

Đây là mã nguồn Backend của **Hệ thống Xếp lịch Thông minh Eureka English Center**. Hệ thống cung cấp các API RESTful, quản lý xác thực bảo mật, xử lý logic nghiệp vụ và tích hợp bộ giải tối ưu hóa ràng buộc tự động **Timefold Solver**.

---

## ☕ 1. Công Nghệ & Thư Viện Sử Dụng

Backend được xây dựng trên nền tảng Spring Boot hiện đại, chú trọng vào tính ổn định và hiệu năng cao:

*   **Ngôn ngữ & Runtime:** Java 21 (Eclipse Temurin JDK 21 khuyên dùng).
*   **Framework:** Spring Boot 3.2.x.
*   **Constraint Optimization:** Timefold Solver 1.13.x (Bộ giải thuật toán xếp lịch tự động tối ưu hóa ràng buộc cứng/mềm).
*   **Database & Migration:** MySQL 8.0+, kết hợp Flyway Database Migration để quản lý phiên bản database và tự động nạp dữ liệu mẫu (Seed Data).
*   **Data Access Layer:** NamedParameterJdbcTemplate (thực hiện truy vấn SQL thuần tối ưu hóa hiệu năng, xây dựng câu truy vấn động, tránh hoàn toàn SQL Injection).
*   **Security:** Spring Security & JWT (JSON Web Token) cho cơ chế Role-based Access Control (ADMIN, STAFF, TEACHER).
*   **API Documentation:** Swagger UI / OpenAPI 3.

---

## 📂 2. Cấu Trúc Thư Mục & Các Gói Chính (Packages)

Mã nguồn được tổ chức theo kiến trúc phân lớp hướng Domain (`com.eureka.timetabling`):

*   [`config`](file:///d:/eureka_system/backend/src/main/java/com/eureka/timetabling/config): Cấu hình Spring Boot, JWT Security, Cors, và OpenAPI Swagger.
*   [`controller`](file:///d:/eureka_system/backend/src/main/java/com/eureka/timetabling/controller): Tiếp nhận request, xử lý validate đầu vào và định nghĩa endpoints API.
*   [`domain`](file:///d:/eureka_system/backend/src/main/java/com/eureka/timetabling/domain): Khai báo thực thể nghiệp vụ (Domain Entities) như `Teacher`, `SchoolClass`, `Lesson`, `Timeslot`, `Room`, `LeaveRequest`...
*   [`dto`](file:///d:/eureka_system/backend/src/main/java/com/eureka/timetabling/dto): Các đối tượng vận chuyển dữ liệu (Data Transfer Objects) phục vụ Request và Response.
*   [`repository`](file:///d:/eureka_system/backend/src/main/java/com/eureka/timetabling/repository): Chứa các lớp truy vấn cơ sở dữ liệu sử dụng JDBC Template.
*   [`service`](file:///d:/eureka_system/backend/src/main/java/com/eureka/timetabling/service): Triển khai nghiệp vụ cốt lõi, bao gồm ClassPlanning (Rolling Scheduling), Auth, LeaveRequest, và Timetable.
*   [`solver`](file:///d:/eureka_system/backend/src/main/java/com/eureka/timetabling/solver): Chứa cấu hình Timefold:
    *   `Timetable`: Định nghĩa bài toán tối ưu (Planning Solution).
    *   `TimetableConstraintProvider`: Khai báo 6 ràng buộc cứng (Hard Constraints) và 10 ràng buộc mềm (Soft Constraints) bằng Java Constraint Streams API.

---

## ⚙️ 3. Hướng Dẫn Cài Đặt & Chạy Ứng Dụng (Local Setup)

### 3.1 Yêu cầu tiên quyết
*   **JDK 21** trở lên.
*   **MySQL Server** (8.0+).
*   **Maven** (Có thể dùng công cụ đóng gói đi kèm `./mvnw`).

### 3.2 Các bước cấu hình & khởi chạy
1.  **Tạo Database trống trong MySQL:**
    ```sql
    CREATE DATABASE eureka_timetabling CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    ```
2.  **Thiết lập File cấu hình môi trường Local:**
    *   Tại thư mục `backend/src/main/resources/`, sao chép file mẫu `application-local.yml.example` thành file `application-local.yml`:
        ```bash
        # PowerShell
        copy-item src/main/resources/application-local.yml.example src/main/resources/application-local.yml
        ```
    *   Mở file `application-local.yml` và điền cấu hình MySQL của máy bạn:
        ```yaml
        spring:
          datasource:
            url: jdbc:mysql://localhost:3306/eureka_timetabling?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
            username: root
            password: MAT_KHAU_MYSQL_CUA_BAN
        ```
3.  **Biên dịch & Khởi động Backend:**
    *   Mở Terminal tại thư mục `backend` và chạy lệnh:
        ```bash
        # Windows (PowerShell)
        mvn spring-boot:run -Dspring-boot.run.profiles=local
        
        # Windows (cmd)
        mvn spring-boot:run "-Dspring-boot.run.profiles=local"
        ```
    *   *Sau khi khởi động thành công, hệ thống Flyway Migration sẽ tự chạy để sinh bảng và nạp dữ liệu mẫu vào DB.*

### 3.3 Kiểm tra ứng dụng
*   API Root Endpoint: `http://localhost:8080/api`
*   Swagger UI API Docs: `http://localhost:8080/api/swagger-ui.html`
