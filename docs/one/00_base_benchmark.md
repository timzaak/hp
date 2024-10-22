# 基准测试
由于各自实际运行软硬件环境的差异，需要做一个基准测试来进行评估。本项目运行环境如下：
### 硬件
MacBook Pro (18年)
* 处理器: 2.9 GHz 六核Intel Core i9
* 内存: 16 GB 2400 MHz DDR4
* 硬盘: 512 SSD
### 软件
* JVM: Eclipse Temurin Java 17
* Postgres 17
* Redis 7.4.1
* Gatling (benchmark使用, 脚本采用Scala2 编写，需要sbt编译代码)
* SpringBoot 3.3.4

第三方基础服务由 docker 启动
```sh
# Postgres 数据卷需要挂载出来
docker run -d -v $(PWD):/var/lib/postgresql/data\
 -e POSTGRES_PASSWORD=postgres\
 -e POSTGRES_USER=postgres\
 -e POSTGRES_DB=db\
 -p 5432:5432 --name postgres postgres:17
# Redis
docker run -d --name redis -p 6379:6379 redis:7.4.1
```

以上主要是 Gatling 依赖的 sbt 很少人会装在电脑上，请参考 [sbt安装](https://www.scala-sbt.org/download/)，另外首次运行，需要安装很多依赖包，比较慢，科学上网会有较大提升。

## 基准测试用例

HTTP GET `/ping`：包含读取一次 redis（session 验证）和查询数据库一次。代码在：<a v-bind:href="codeSrc + '/controller/BaseController.java'">BaseController.java</a>。

Benchmark 在：<a v-bind:href="benchSrc + '/one/BaseBenchmark.scala'">BaseBenchmark.scala</a>。

测试步骤：
1. 在 backend 目录下，以 release 模式运行 Spring Boot 服务。
2. 在 benchmark 目录下，运行命令行 `
```shell
# users 代表并行数，模拟用户数
# repeat 代表每个并行不间断执行任务次数
# 以上两个参数根据硬件情况需要进行修改调整
sbt 'Gatling/testOnly com.timzaak.one.BaseBenchmark' -Dusers=100 -Drepeat=500
```
3. 打开测试结果页，观察测试结果。



### 测试结果
在上述软硬件环境下（开着微信、两个idea、chrome、vscode等），**QPS 约 `2400`，75%响应时间低于60ms**。

::: tip
测试数据低的原因主要在于：性能测试、后台服务、Redis、Postgres 都跑在一台笔记本电脑上，后续有机会，会在云机器上再测试一遍。
:::

## 小结
至此我们准备好了测试环境，也获得本机下的基础性能数据，下章开始讲解电商业务抽象。
