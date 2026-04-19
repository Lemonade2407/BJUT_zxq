# 构建阶段
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# 第一步：复制 pom.xml 文件并下载依赖（这一层会被缓存）
COPY pom.xml .
COPY common/pom.xml common/
COPY pojo/pom.xml pojo/
COPY server/pom.xml server/

# 下载依赖（利用 Docker 缓存，只有 pom.xml 变化时才会重新执行）
RUN mvn dependency:go-offline -B

# 第二步：复制源代码并编译（代码变化时才重新执行）
COPY common/src common/src
COPY pojo/src pojo/src
COPY server/src server/src

# 编译打包（跳过测试加快速度）
RUN mvn clean package -DskipTests -pl server -am

# 运行阶段
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 创建非root用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 从构建阶段复制jar包
COPY --from=builder /app/server/target/*.jar app.jar

# 创建日志目录
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

# 切换到非root用户
USER appuser

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
