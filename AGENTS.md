# 协作指引 — lieshou-cloud-admin-services（L2 底座 · 管理服务）

你是 **lieshou-cloud-admin-services** 的 AI 协作伙伴。
技术栈：Java 21 · Spring Boot 3.5.7 · Spring Cloud 2023.0.5 · Spring Cloud Alibaba 2023.0.1.0 · Maven（聚合构建，
parent = `framework-parent`，从内网 Nexus `maven-public` 按坐标解析，**本仓无 submodule**）· JUnit 5 + JaCoCo + Spotless。

## 怎么读这个仓库（启动顺序）

```bash
cat README.md                              # 构建 / 镜像 / 制品 / 发版
cat .gitlab-ci.yml                         # 门 = mvn verify；deploy stage 仅 tag 触发
cat services/admin/pom.xml                 # 唯一的版本源（<parent> framework-parent）
git log --oneline -10                      # 最近改了什么
```

（本仓不设 `.ai/`：薄壳仓，长期事实就写在 README + 本文件里。）

## 架构速览

```
pom.xml（聚合器：仅 services/admin；上游 framework 不随仓、无 submodule）
└── services/admin/       Spring Boot 薄壳（端口 8082）
    ├── AdminApplication.java          启动 + @EnableFeignClients
    ├── web/AdminController.java       /api/admin/**（health / users/{id} / users/by-username）
    ├── feign/UserFeignClient.java     → user-service（Nacos 名 lieshoucloud-user，lb:// 软负载）
    │   └── UserFeignClientFallback     Resilience4j 熔断降级（不向调用方抛 5xx）
    └── resources/application*.yml     端口 / Nacos 注册与可选配置中心
```

- 上游解析：`framework-parent` / `framework-common` 从内网 Nexus `maven-public`
  （`https://nexus.ts.huntercat.cn/repository/maven-public/`）按坐标拉。
- 依赖方向：本仓 → `framework-*`（**不反向**）；跨服务只调 `user-service`（Feign，lb）。
- **无数据库**：无 Flyway / JPA / datasource。

## 协作边界

| 级别 | 动作 | 怎么做 |
| --- | --- | --- |
| 🟢 | 加测试、改 bug、补文档 | ✅ 直接 |
| 🟡 | 加依赖、改 `services/admin/pom.xml` / `application.yml` 结构 | ⚠️ 先告知影响 |
| 🟡 | 改 Feign 契约（`UserFeignClient`）、错误响应体 | ⚠️ 先告知影响（跨服务契约） |
| 🟡 | 升 `framework-parent` / `framework-*` 版本号 | ⚠️ 先告知影响（改的是上游版本线） |
| 🔴 | 直接改 `framework-*` 源码 | ❌ 不在本仓 —— 改上游 `lieshou-framework` 仓 → 发版到 Nexus → 本仓升版本号 |
| 🔴 | 打 `v*` tag / 触发 Nexus 发布、直接推 `main` | ❌ 必须先问人（发布点只增不改） |

## 常用命令

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -B -ntp verify        # 全量门（spotless + 单测 + jacoco）
mvn -B -ntp -DskipTests package                                        # 打包 → services/admin/target/lieshoucloud-admin.jar
mvn -B -ntp spotless:apply                                             # 格式化
docker build -f services/admin/Dockerfile .                            # 镜像（context = 仓根）
java -jar services/admin/target/lieshoucloud-admin.jar                 # 运行（ADMIN_PORT，默认 8082）
```

## 当前阶段

- 2026-09-15：上游改走内网 Nexus（**移除 `framework` submodule**），`framework-parent` 0.3.3 线；
  CI 增加 tag 触发的 `deploy`（发 `cn.huntercat:lieshoucloud-admin` 到 Nexus `maven-releases`）。

## 待办

- [ ] CD：镜像构建推 Harbor + 部署 job —— 未做（当前只有 GitLab CI 的 verify / deploy）
- [ ] 内网 CA 进 job 镜像，去掉各处 keytool 妥协（跨仓基础设施项）

## 关键决策

- 2026-09-15：**移除 `framework` submodule**，上游 `framework-*` 一律从内网 Nexus `maven-public` 按坐标解析
  —— 免去 job 里的 submodule 私钥/URL 重写，也消除"本地框架源码与制品版本不一致"的可能。
- 2026-09-15：制品只发本仓模块（`-pl services/admin`），`framework-*` 归上游仓发布 —— 避免同坐标重复发撞 Nexus 重发策略。
- 2026-08：管理业务核心上收 `lieshou-framework`（本仓只留 Controller + Feign 端口 + 装配）。
