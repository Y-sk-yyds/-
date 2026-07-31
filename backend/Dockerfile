# ===== 第一阶段：Maven 构建 =====
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# ===== 第二阶段：运行镜像 =====
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/clinic-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
