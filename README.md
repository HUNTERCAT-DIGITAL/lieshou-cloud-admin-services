# lieshou-cloud-admin-services · 开源后端服务:admin

> 自 lieshou-cloud-core/services/admin 拆分(2026-08 细粒度)。
> 组合:services/common → lieshou-cloud-common。

依赖源由仓内 `.mvn/settings.xml` 固定在**内网 Nexus**（`maven-public`），经
`.mvn/maven.config` 自动加载，本地/CI/Docker 一致 —— 不需要任何 `-s` 参数。

```bash
# ⚠️ 必须在仓库根目录构建：.mvn/maven.config 里的 -s 以相对路径解析，
#    进 services/ 再跑会报 "user settings file does not exist"。
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -B -ntp -f services/pom.xml -DskipTests package
```
