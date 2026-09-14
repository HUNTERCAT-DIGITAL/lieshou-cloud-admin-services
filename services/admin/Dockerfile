# syntax=docker/dockerfile:1.7
# LieShouCloud · Admin Service (port 8082) multi-stage
# docker build context = 仓库根(含 framework submodule + services/,mvn -f pom.xml 一次聚合构建)

FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /workspace
COPY . /workspace/

# 依赖源由仓内 .mvn/ 固定为内网 Nexus（.mvn/maven.config 自动加载，与本地/CI 一致）。
# 旧写法 COPY services/.m2-settings.xml → /root/.m2/settings.xml 其实从未生效：
# 下面那行 --mount=type=cache 会把 /root/.m2 整个遮住。
# Nexus 由 Caddy 内部 CA 签发，容器 JDK cacerts 里没有 → 先导入（.ci/caddy-root.crt 是公开根证书，非私钥）。
RUN keytool -importcert -noprompt -trustcacerts -alias caddy-root-2026 \
      -file .ci/caddy-root.crt -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -Dmaven.test.skip=true -f pom.xml -pl services/admin -am clean package

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

COPY --from=builder /workspace/services/admin/target/lieshoucloud-admin.jar /app/app.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Dfile.encoding=UTF-8"
USER app

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -Y off -qO- http://127.0.0.1:8082/actuator/health >/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
