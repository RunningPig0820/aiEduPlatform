# Java 后端多阶段构建 (Java 21)
# 构建上下文 = aiEduPlatform 仓库根目录（docker-compose 里 context: ../）
# 原 deploy/Dockerfile 用 mvnw + temurin:17，项目无 maven wrapper 且为 Java 21，此处修复。
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY ai-edu-backend/ .
RUN mvn -q -pl ai-edu-interface -am clean package -Dmaven.test.skip=true

# 运行阶段
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/ai-edu-interface/target/*.jar app.jar
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms512m -Xmx1024m"
EXPOSE 9627
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
