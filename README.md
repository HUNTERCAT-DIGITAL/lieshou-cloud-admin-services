# lieshou-cloud-admin-services · 开源后端服务:admin

> 自 lieshou-cloud-core/services/admin 拆分(2026-08 细粒度)。
> **上游全部走内网 Nexus**：本仓无 submodule；`framework-parent` / `framework-*` 由
> `https://nexus.ts.huntercat.cn/repository/maven-public/` 解析（framework 版本线 0.3.3）。
> 本仓只留 Controller + 装配 + 启动，业务在 `lieshou-framework`（上游同源唯一）。

## 构建

```bash
# 前提：~/.m2/settings.xml 已 mirror 到内网 Nexus（新机器：ssh mesh-dell 'cat /srv/nexus/client-setup.sh' | bash）
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -B -ntp -DskipTests -f pom.xml package
# 只构建 admin 服务（本仓只有这一个模块，-pl 只是显式化）：
mvn -B -ntp -DskipTests -f pom.xml -pl services/admin package
# 全量门（spotless + 单测 + jacoco）
mvn -B -ntp verify
```

镜像：`docker build -f services/admin/Dockerfile .`（context = 仓根；builder 阶段导入 `services/.ci/caddy-root.crt`）。
