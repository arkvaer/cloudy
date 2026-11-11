# Spring Cloud Alibaba 学习计划（2026-11）

> 适用仓库：`cloudy`  
> 当前基线：Spring Boot 4.1.2 / Spring Cloud 2025.1.4 / Spring Cloud Alibaba 2025.1.0.0 / Java 25  
> 建议周期：6 周，每周 10–12 小时；时间紧可压缩为 4 周。  
> 目标：能独立搭建、排错并讲清楚 Nacos + OpenFeign + Gateway + Sentinel + Seata 的微服务系统。

---

## 一、先回答：Nacos 3.3.0 能学吗？

**能学，而且和你的技术栈匹配；但要接受“版本很新、教程滞后”，并注意 3.x 的破坏性变化。**

事实依据：

- Nacos 3.3.0 发布于 **2026-10-29**，截至本文写作（2026-11-10）仅 12 天，是当前最新版。
- Spring Cloud Alibaba 2025.1.0.0 管理的 `nacos-client` 是 **3.1.1**（不是 3.3.0）。
- Nacos 官方升级手册：3.3 服务端兼容 **2.x / 3.x 客户端**；1.x 客户端不再兼容（需要 `nacos-api-legacy-adapter`）。
- 所以推荐组合是：**Nacos Server 3.3.0 + SCA 自带的 nacos-client 3.1.1**。不要手动把 `nacos-client` 覆盖成 3.3.0，除非你明确要验证 3.3 新客户端特性。
- 3.3 的亮点偏 AI/MCP 资源管理（Agent、MCP、Skill、Prompt）、DNS 发现、分布式锁等，**微服务学习核心（注册/配置）用不到**；这也意味着 3.3 的教程会很少。
- 如果希望“教程多、少踩坑”，可以退到 Nacos **3.2.4** 服务端（客户端仍是 3.1.1），或整套退到 Boot 3.5 + SCA 2023.0.3.4 + Nacos 3.1。但你已经搭好 Boot 4 的架子，没必要倒退。
- **生产环境**：3.3.0 太新，建议等 3.3.x 补丁版；学习环境直接用 3.3.0 没问题。Nacos 2.5.5 与 3.3.0 同日发布，说明 2.x 仍在维护。

### 3.x 必须知道的 6 个变化

1. **端口**：控制台 `8080`，服务端 `8848`，客户端 gRPC `9848`；集群再加 `9849`。不要把 8080 当服务端口。
2. **鉴权默认开启**：必须设置 `NACOS_AUTH_TOKEN`（Base64，解码后 ≥32 字节）、`NACOS_AUTH_IDENTITY_KEY/VALUE`，客户端配置 username/password。
3. **默认命名空间**：3.x 客户端默认 `public`；2.x 是空字符串，迁移时注意。
4. **1.x 客户端不再兼容**（3.3 起）；老项目升级要先换客户端。
5. **3.3 移除了 2.x 配置数据迁移逻辑**；全新环境无感，老环境要先经过 3.0–3.2。
6. **控制台与服务端分离部署**；容器要同时映射 8080 / 8848 / 9848。

---

## 二、版本矩阵

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 25.0.3 | 项目 target 25；SCA 自身按 17 编译 |
| Spring Boot | 4.1.2 | 当前根 POM |
| Spring Cloud | 2025.1.4 | 管理 Gateway 5.0.4 / OpenFeign 5.0.4 |
| Spring Cloud Alibaba | 2025.1.0.0 | 管理 Nacos client 3.1.1 / Sentinel 1.8.9 / Seata 2.5.0 |
| Nacos Server | 3.3.0 | 最新版；学习可用，生产建议等补丁 |
| nacos-client | 3.1.1 | 由 SCA BOM 管理，不要手改 |
| Spring Cloud Gateway | 5.0.4 | 注意新的 starter 与配置前缀 |
| OpenFeign | 5.0.4 | Spring Cloud BOM 管理 |
| Sentinel | 1.8.9 | Dashboard 也用 1.8.9（或 1.8.10） |
| Seata | 2.5.0 | 客户端与服务端保持一致 |
| MySQL | 8.4 | Seata 业务库 + undo_log |

核对实际依赖版本：

```bash
mvn dependency:tree -Dincludes=com.alibaba.nacos:nacos-client
mvn dependency:tree -Dincludes=com.alibaba.csp:sentinel-core
mvn dependency:tree -Dincludes=org.apache.seata:seata-spring-boot-starter
```

---

## 三、开工前：当前仓库需要修的 4 件事

### 1. JDK 版本不对（最优先）

当前 shell 的 `java` 和 `mvn` 都跑在 **Java 8**，而项目是 Java 25，直接构建会失败。

```bash
sdk use java 25.0.3-oracle
# 建议直接设为默认：
# sdk default java 25.0.3-oracle

java -version
mvn -version
```

IntelliJ IDEA 里同时检查：

- Project Structure → Project SDK = 25
- Settings → Build Tools → Maven → Runner JRE = 25

### 2. Nacos 密码是明文

`nacos-provider/src/main/resources/application.yaml` 里写了明文密码。改为：

```yaml
username: ${NACOS_USERNAME:nacos}
password: ${NACOS_PASSWORD}
```

在 IDE Run Configuration、shell 或未提交的 `.env` 中提供 `NACOS_PASSWORD`；`.gitignore` 加 `.env`。不要把这个密码提交到仓库。

### 3. `management` 配置不完整

当前是：

```yaml
management:
  endpoint:
    web
```

建议改成：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

### 4. `main` 方法建议加 `public`

```java
public static void main(String[] args) {
    SpringApplication.run(NacosProviderApplication.class, args);
}
```

Java 25 虽然支持更宽松的 main 方法写法，但 Spring Boot 插件/反射调用按 `public static void main` 最稳。

### 5. 端口规划（避免冲突）

Nacos 3 控制台占 **8080**，Sentinel Dashboard 默认也是 **8080**，必须错开：

| 服务 | 端口 |
|---|---|
| cloudy-gateway | 8000 |
| nacos-provider（两实例） | 9001 / 9002 |
| order-service | 9101 |
| inventory-service | 9102 |
| account-service | 9103 |
| Nacos 控制台 | 8080 |
| Nacos 服务端 | 8848 |
| Nacos client gRPC | 9848 |
| Sentinel Dashboard | 8858 |
| Seata 控制台 / 服务 | 7091 / 8091 |
| MySQL | 3306 |

---

## 四、六周计划

### 第 0 周：环境与基线（0.5–1 天）

**目标**：Nacos、Sentinel Dashboard、MySQL 跑起来，现有 provider 能注册成功。

1. 用 Docker/Podman 启动 Nacos 3.3.0：

```bash
docker run -d --name nacos \
  -e MODE=standalone \
  -e NACOS_AUTH_ENABLE=true \
  -e NACOS_AUTH_TOKEN="$(openssl rand -base64 32)" \
  -e NACOS_AUTH_IDENTITY_KEY=serverIdentity \
  -e NACOS_AUTH_IDENTITY_VALUE=security \
  -p 8080:8080 -p 8848:8848 -p 9848:9848 \
  nacos/nacos-server:v3.3.0
```

> 当前机器的 `docker` 是 Podman 模拟；若 `docker run` 不工作，直接用 `podman run`，参数相同。  
> 生产/长期环境请把 token 和 identity 固定到未提交的 `.env`，不要每次随机生成。

2. 打开控制台 `http://127.0.0.1:8080`，登录后确认命名空间、服务列表可用。
3. 启动 Sentinel Dashboard（**用 JDK 17 跑**，1.8.9 基于 Spring Boot 2.5，JDK 25 容易出反射/兼容问题）：

```bash
JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.0-open" \
  java -jar sentinel-dashboard-1.8.9.jar --server.port=8858
```

4. 启动 MySQL 8.4（第 4 周才用，先准备好）：

```bash
docker run -d --name mysql8 \
  -e MYSQL_ROOT_PASSWORD=root \
  -p 3306:3306 mysql:8.4
```

**验收**：Nacos 控制台能登录；provider 启动后出现在服务列表；`curl localhost:9001/actuator/health` 返回 UP。

**常见坑**：JDK 仍是 8；Nacos 只映射了 8848 没映射 9848；密码用默认 `nacos/nacos` 但服务端开了鉴权。

---

### 第 1 周：Nacos 配置中心 + 注册中心

**目标**：理解 namespace / group / dataId，配置能热更新，服务能注册发现。

**实操**

1. 在 Nacos 控制台新建配置：`nacos-provider.yaml`（DEFAULT_GROUP），内容放一个业务开关，例如：

```yaml
demo:
  welcome: hello-nacos
  feature-enabled: true
```

2. provider 增加：

```yaml
spring:
  config:
    import: optional:nacos:${spring.application.name}.yaml
  cloud:
    nacos:
      config:
        server-addr: 127.0.0.1:8848
        username: ${NACOS_USERNAME:nacos}
        password: ${NACOS_PASSWORD}
        refresh-enabled: true
      discovery:
        server-addr: 127.0.0.1:8848
        username: ${NACOS_USERNAME:nacos}
        password: ${NACOS_PASSWORD}
```

3. 用 `@ConfigurationProperties(prefix = "demo")` 绑定配置；用 `@RefreshScope` 只包需要 `@Value` 的 bean。优先 `@ConfigurationProperties`，Spring Cloud 会自动 rebind。
4. 再启动一个实例：`--server.port=9002`，给两个实例打不同 metadata：

```yaml
spring.cloud.nacos.discovery.metadata.version: v1   # 9001
spring.cloud.nacos.discovery.metadata.version: v2   # 9002
```

5. 用 `DiscoveryClient` 打印实例列表；在控制台手动下线一个实例，观察列表变化。

**验收**

- 修改 Nacos 里的 `demo.welcome`，不重启服务，接口返回值变化。
- 两个实例都出现在 `nacos-provider` 服务下。
- 新建一个 namespace，把配置放进去，用 `namespace: <ID>`（填 ID，不是显示名）能隔离读取。

**常见坑**

- 只配了 `spring.cloud.nacos.config` 但没写 `spring.config.import`，配置不会加载。
- namespace 填成显示名；group 写错；dataId 少了 `.yaml`。
- 3.x 默认 namespace 是 `public`，不是空字符串。
- 改完配置没刷新：`@Value` 没加 `@RefreshScope`，或 `refresh-enabled` 被关掉。

---

### 第 2 周：OpenFeign + LoadBalancer + Gateway

**目标**：gateway → order → provider 全链路跑通，理解负载均衡与超时。

**实操**

1. 新建 `order-service`（9101），调用 `nacos-provider`：

```java
@FeignClient(name = "nacos-provider", path = "/provider")
public interface ProviderClient {
    @GetMapping("/hello")
    String hello(@RequestParam String name);
}
```

2. 配置 Feign 超时、日志、错误解码：

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connect-timeout: 2000
            read-timeout: 3000
            logger-level: basic
```

3. 写 `RequestInterceptor` 透传 `Authorization` / `X-Request-Id`；写 `ErrorDecoder` 把下游错误转成统一异常。
4. 新建 `cloudy-gateway`（8000），依赖用 **Gateway 5.0 的新 starter**：

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-gateway-server-webflux</artifactId>
</dependency>
```

5. 路由配置注意 **5.0 的新前缀**：

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: provider
              uri: lb://nacos-provider
              predicates:
                - Path=/api/provider/**
              filters:
                - StripPrefix=2
```

6. 给 Gateway 加 CORS、全局 TraceId filter（生成/透传 `X-Request-Id`）。

**验收**

- `curl localhost:8000/api/provider/hello?name=vaer` 能到 provider。
- 停掉 9001，请求仍能打到 9002，无报错。
- 下游 sleep 5s，Feign 在 3s 超时并返回统一错误。

**常见坑（老教程重灾区）**

- 还在用 `spring-cloud-starter-gateway`：5.0 已拆成 `-server-webflux` / `-server-webmvc`。
- 还在用 `spring.cloud.gateway.routes`：5.0 是 `spring.cloud.gateway.server.webflux.routes`。
- Gateway 模块误引 `spring-boot-starter-web`，导致 WebFlux 不生效。
- Gateway 跑在 8080，和 Nacos 控制台冲突。
- Feign 默认超时很长，不配会“卡住像死机”。

---

### 第 3 周：Sentinel 流控、熔断、降级

**目标**：能在 Dashboard 配规则，能解释 QPS/线程数/慢调用比例，规则能持久化到 Nacos。

**实操**

1. 引入：

```xml
<dependency>
  <groupId>com.alibaba.cloud</groupId>
  <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
```

```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: 127.0.0.1:8858
        port: 8719
      eager: true
```

2. 在 Dashboard 配流控：QPS=2、线程数、Warm Up、匀速排队；观察快速失败/排队等待的区别。
3. 熔断：慢调用比例（RT > 500ms 且比例 > 50%）、异常比例；用 sleep 接口触发。
4. 热点参数：对 `userId` 限流；系统规则：LOAD / RT / 线程数。
5. 代码里用 `@SentinelResource(value = "hello", blockHandler = "blocked", fallback = "fallback")`，注意 blockHandler 与 fallback 的签名区别。
6. Feign 整合：

```yaml
feign:
  sentinel:
    enabled: true
```

7. Gateway 整合：加 `spring-cloud-alibaba-sentinel-gateway`，在 Dashboard 给路由配限流/降级。
8. 规则持久化：用 Nacos datasource 把流控/熔断规则写进 Nacos，重启后规则还在。

**验收**

- 压测超过 QPS 阈值时返回自定义 block JSON，不是 500。
- 慢调用触发熔断，后续请求直接 fallback。
- 重启服务后，Nacos 中的规则仍生效。

**常见坑**

- Dashboard 没启动、端口冲突、`eager` 没开导致首次不显示。
- 只写 `@SentinelResource` 但方法不是 Spring bean，AOP 不生效。
- blockHandler 参数没加 `BlockException`，签名不对。
- 规则只存在内存，重启丢失；一定要做 Nacos 持久化。

---

### 第 4 周：Seata 分布式事务（AT 模式）

**目标**：order → inventory → account 跨三个库，异常时全局回滚。

**准备**

1. 启动 Seata Server 2.5.0（与客户端版本一致）：

```bash
docker run -d --name seata-server \
  -p 7091:7091 -p 8091:8091 \
  apache/seata-server:2.5.0.jdk17
```

2. 建库：`seata`（服务端存事务日志）、`cloudy_order`、`cloudy_inventory`、`cloudy_account`。
3. Seata 服务端库执行 `global_table`、`branch_table`、`lock_table`、`distributed_lock` 建表脚本。
4. 每个业务库加 `undo_log` 表（AT 模式必需）。

**客户端配置（order-service）**

```yaml
seata:
  enabled: true
  application-id: order-service
  tx-service-group: cloudy_tx_group
  service:
    vgroup-mapping:
      cloudy_tx_group: default
  registry:
    type: nacos
    nacos:
      server-addr: 127.0.0.1:8848
      namespace: public
      group: SEATA_GROUP
      application: seata-server
      username: ${NACOS_USERNAME:nacos}
      password: ${NACOS_PASSWORD}
  config:
    type: nacos
    nacos:
      server-addr: 127.0.0.1:8848
      namespace: public
      group: SEATA_GROUP
      username: ${NACOS_USERNAME:nacos}
      password: ${NACOS_PASSWORD}
```

**实操**

1. order-service 的“创建订单”入口加 `@GlobalTransactional`。
2. 通过 Feign 依次调用 inventory（扣库存）和 account（扣余额）。
3. 在 account 扣款后抛异常，观察三库回滚。
4. 制造库存不足，观察 order / account 的回滚。
5. 重复提交同一请求，验证幂等（业务唯一键 + 状态机）。

**验收**

- 正常下单：订单、库存、账户三方一致。
- account 抛异常：order 和 inventory 都回滚，`undo_log` 有记录。
- inventory 库存不足：全局回滚。
- 能画出 AT 模式两阶段提交（TC/TM/RM）的时序图。

**常见坑**

- Seata 客户端 2.5.0 配 2.7.0 服务端（或反过来），版本不匹配。
- 业务库漏建 `undo_log`，报 `Table 'undo_log' doesn't exist`。
- `@GlobalTransactional` 加在内部方法/同类调用上，AOP 不生效。
- 调用链中间用了本地方法而不是 Feign/RPC，XID 没传播。
- AT 模式依赖主键和可回滚 SQL；复杂 SQL 先看 `undo_log` 里的 rollback_info。

---

### 第 5 周：可观测性与生产化

**目标**：能定位“慢在哪、错在哪”，并给系统加上基本防护。

**实操**

1. 引入 Actuator + Micrometer Tracing（OTLP 或 Zipkin），采样率 1.0；Gateway/order/provider 全链路 traceId。
2. 日志 pattern 加 `traceId/spanId`：

```yaml
logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

3. Prometheus + Grafana：暴露 `/actuator/prometheus`，看 QPS、RT、错误率、JVM。
4. Gateway 限流：Redis + `RequestRateLimiter`（令牌桶），或 Sentinel Gateway 规则。
5. 优雅停机、健康检查、Nacos 心跳；K8s readiness/liveness 预留。
6. 故障演练：停 Nacos（本地缓存是否继续工作）、停下游（熔断）、慢 SQL（RT 熔断）。

**验收**

- 一次请求在 Zipkin/OTLP 里能看到 gateway → order → provider 的完整 span。
- Grafana 能看到服务 QPS/RT/错误率。
- 停掉下游后，Sentinel 熔断生效，错误不雪崩。

**常见坑**

- 只加 actuator 不加 tracing bridge，traceId 为空。
- 采样率 0 或日志 pattern 写错。
- Gateway 是 WebFlux，Servlet 的 MDC filter 不生效，要用 Reactor 上下文传播。

---

### 第 6 周：综合实战与复盘

**目标**：把六周内容合成一个能演示、能讲清楚的项目。

**综合项目：cloudy-mall**

```text
client
  |
  v
cloudy-gateway :8000
  |  lb://order-service
  v
order-service :9101  --Feign--> inventory-service :9102
  |                                   |
  +-------------Feign---------------> account-service :9103
  |
  +--- Seata @GlobalTransactional (XID)

所有服务：Nacos 注册/配置 + Sentinel 规则 + Micrometer Tracing
```

**交付物**

- 多模块工程：`cloudy-common`、`cloudy-gateway`、`order-service`、`inventory-service`、`account-service`。
- Nacos 配置清单（dataId/group/namespace）与导入脚本。
- Sentinel 规则（Nacos 持久化）。
- 数据库脚本（业务表 + undo_log + Seata 服务端表）。
- README：架构图、启动顺序、端口表、故障演练步骤。
- 一份压测/故障报告：正常、库存不足、账户异常、下游超时、Nacos 宕机。

**故障演练清单**

| 场景 | 预期 |
|---|---|
| inventory 扣减后抛异常 | order + inventory + account 全部回滚 |
| account 余额不足 | 全局回滚，订单不落库 |
| provider 一个实例挂掉 | Gateway/Feign 自动打到另一实例 |
| provider 全部挂掉 | Sentinel 熔断 + fallback，不雪崩 |
| Nacos 配置改错 | 配置回滚，服务不重启恢复 |
| Nacos 服务端重启 | 客户端本地缓存继续工作，恢复后重新注册 |
| 重复下单 | 幂等，不重复扣款/扣库存 |

---

## 五、全家桶案例仓库

### 优先：官方 SCA Examples（版本最匹配）

- 仓库：<https://github.com/alibaba/spring-cloud-alibaba/tree/2025.1.x/spring-cloud-alibaba-examples>
- 关键模块：
  - `nacos-example/`：`nacos-discovery-example`、`nacos-config-example`、`nacos-gateway-example`
  - `sentinel-example/`：`sentinel-openfeign-example`、`sentinel-spring-cloud-gateway-example`、`sentinel-circuitbreaker-example`
  - `seata-example/`：`business-service`、`order-service`、`storage-service`、`account-service`
  - `integrated-example/`：**全家桶综合案例**，整合 Gateway + Nacos + Sentinel + Seata + RocketMQ + Docker/K8s
- 官方文档：<https://github.com/alibaba/spring-cloud-alibaba/tree/2025.1.x/spring-cloud-alibaba-examples/integrated-example/docs/en/readme.md>
- 建议顺序：先跑 `nacos-example`，再跑 `sentinel-example`、`seata-example`，最后跑 `integrated-example`。不要一上来就跑 integrated，依赖多、排错难。

### 其他官方/社区仓库

| 仓库 | 用途 | 注意 |
|---|---|---|
| <https://github.com/nacos-group/nacos-examples> | Nacos 原生 SDK、Spring Boot、Dubbo、SCG、Sentinel 集成 | 最后更新 2024，Nacos 2.x 时代；改端口/鉴权后食用 |
| <https://github.com/apache/incubator-seata-samples> | AT / TCC / Saga / XA 官方样例 | 部分模块 Spring Boot 版本较旧，按模块适配 |
| <https://github.com/alibaba/Sentinel/tree/1.8/sentinel-demo> | Sentinel 原生 demo（注解、Nacos datasource、热点参数等） | 偏底层；Spring Cloud 整合优先看 SCA examples |
| <https://github.com/spring-cloud/spring-cloud-gateway/tree/main/spring-cloud-gateway-sample> | Gateway 5 官方 sample | 注意 `spring.cloud.gateway.server.webflux` 新前缀 |
| <https://github.com/macrozheng/mall-swarm> | 完整电商微服务（Boot 3.5 + SCA） | 体量大，适合看架构，不适合入门照抄 |
| <https://github.com/macrozheng/springcloud-learning> | 结构化 Spring Cloud 教程 | 最后更新 2024，版本偏旧 |
| <https://github.com/zhoutaoo/SpringCloud> | Spring Cloud 2023 微服务平台 | 含 RBAC、网关、监控，适合进阶参考 |
| <https://github.com/YunaiV/yudao-cloud> | 大型 Cloud 平台 | 非常庞大，适合后期读架构 |

**使用原则**：官方 examples 用来“跑通 + 对照版本”；社区项目用来“看架构 + 学工程化”。不要直接复制社区代码进 `cloudy`，先理解再重写。

---

## 六、学习资源

- Spring Cloud Alibaba 官网：<https://sca.aliyun.com/>
- Nacos 文档：<https://nacos.io/docs/latest/>
- Nacos 升级手册（客户端兼容性）：<https://nacos.io/docs/latest/manual/admin/upgrading/>
- Nacos Docker 快速开始：<https://nacos.io/docs/latest/quickstart/quick-start-docker/>
- Seata 官网：<https://seata.apache.org/>
- Sentinel 官网 / Wiki：<https://sentinelguard.io/>
- Spring Cloud Gateway 文档：<https://docs.spring.io/spring-cloud-gateway/reference/>
- Spring Cloud OpenFeign 文档：<https://docs.spring.io/spring-cloud-openfeign/reference/>

版本以 Maven BOM 为准，不轻信博客：

```bash
# 看 SCA 2025.1.0.0 实际管理的版本
curl -s https://repo1.maven.org/maven2/com/alibaba/cloud/spring-cloud-alibaba-dependencies/2025.1.0.0/spring-cloud-alibaba-dependencies-2025.1.0.0.pom
```

---

## 七、每周检查清单

- [ ] 第 0 周：JDK 25 + Nacos 3.3.0 + Sentinel Dashboard + MySQL 跑通
- [ ] 第 1 周：配置热更新、双实例注册、namespace 隔离
- [ ] 第 2 周：Gateway → Order → Provider 链路、负载均衡、Feign 超时
- [ ] 第 3 周：Sentinel 流控/熔断/热点、规则持久化 Nacos
- [ ] 第 4 周：Seata AT 全局回滚、undo_log、幂等
- [ ] 第 5 周：Tracing + Prometheus + Gateway 限流 + 故障演练
- [ ] 第 6 周：综合项目 + README + 故障报告 + 代码复盘

---

## 八、建议的节奏

- 每周至少写一篇笔记：这周解决了什么问题、为什么这样配、踩了什么坑。
- 每周结束做一次“脱稿讲解”：能否不看资料画出注册/配置/调用/回滚的时序图。
- 每完成一个组件，就在 `cloudy` 里加一个可运行的模块，而不是只跑官方 demo。
- 遇到版本问题，优先查官方升级手册和 BOM，不看二手博客的“最新版本对应表”。