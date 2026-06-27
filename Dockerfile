# Giai đoạn 1: Build project (Sử dụng Maven và JDK)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
# Copy file pom và download các dependency trước để tận dụng cache
COPY pom.xml .
RUN mvn dependency:go-offline
# Copy source code và build
COPY src ./src
RUN mvn clean package -DskipTests

# Giai đoạn 2: Tạo image chạy ứng dụng (Chỉ chứa JRE để nhẹ)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copy file JAR từ giai đoạn build
COPY --from=build /app/target/timetabling-1.0.0.jar app.jar

# Thiết lập biến môi trường mặc định (Render sẽ ghi đè lên các giá trị này)
ENV PORT=8080
EXPOSE 8080

# Chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]