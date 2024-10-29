# 下单接口性能优化
通过硬件升级提升软件性能我们先按下不表，通过上一章我们确认性能问题点在于**商品库存竞争**。为此我们能做的优化点有：
## 1.大事务变多个小事务

将扣减库存事务单独隔离出来，并做好补偿逻辑。

逻辑代码：<a v-bind:href="codeSrc + '/controller/Order2Controller.java'">Order2Controller.java</a> 中的 `/order2`。

测试代码：
1. <a v-bind:href="benchSrc + '/one/Order2Benchmark.scala'">Order2Benchmark.scala</a> ：模拟100用户下单500次,每次随机下单1～3款商品（共10款商品）。

```shell
 sbt 'Gatling/testOnly com.timzaak.one.Order2Benchmark'
```
测试结果：**TPS 约 `230`，75%响应时间低于`202ms`, `10%`失败（数据库 40001）**。

![order2_multiple_transaction](/img/order2_multi_transaction.png)

TPS从 `150` 提升至 `230`。但引入了*漏库存*的风险（扣减完库存后，停机不创建订单），这个可通过事后对账补偿来处理(`update product set stock = ... where ... returning stock`, 获取更改后的库存，并记录到订单中)

## 2.以内存为准，异步落库
将商品库存扣减移到 Redis 中，后续异步队列 batch merge 写数据库，并做好依据 订单+后台操作日志 回滚库存到 Redis 逻辑。

该优化点引了*队列*，国内的常见队列选型有：RocketMQ、Kafka、RabbitMQ ，但笔记本性能再负载个 RocketMQ 去做性能测试误差太大，先不写异步队列相关逻辑。

逻辑代码：<a v-bind:href="codeSrc + '/controller/Order2Controller.java'">Order2Controller.java</a> 中的 `/order2_redis`。 Redis Function脚本：<a v-bind:href="luaSrc + '/stock.lua'">stock.lua</a>（Redis7 支持 Function，低于此版本，需要使用 Script），该lua脚本会被性能测试脚本自动写入到 Redis 中。

测试结果：**TPS 约 `450`，75%响应时间低于`239ms`, `5.46%`失败（数据库 40001）**。
![order2_redis](/img/order2_redis.png)

TPS 和 `无事务（基准测试）` 大致相同，响应时间低于基准测试（没有写硬盘）。此提升也会引入*漏库存*的风险，事务失败后关机，库存回滚没有执行。


:::tip
漏库存的原因是事务回滚时，由于数据库没有记录Redis库存变更，所以无法将库存回滚掉。
此时的解决方案是引入数据库以外的事务处理引擎，记录此变更。

目前 Java世界里的是的出名事务处理引擎是 `Seata`, 读者可以借鉴其实现原理复用到自己的语言中。
:::
## 3.多数据库+第三方事务仲裁
多数据库+第三方事务仲裁可做到并行执行、共同回滚、最终一致。

虽然单实例通过 异步（Async SpringBoot）+ 多数据库(Database Proxy）+ 第三方事务中间件（Seata、RocketMQ）也能做，但目前市面上的常见解决方案是`微服务`，我们会在后续 `微服务` 做测试。

这里要多说一下异步，在我们的测试场景下异步并不能提升多少并发能力性能（性能瓶颈在数据库）但可以降低CPU开销（线程不用阻塞，数量也不会太多，减少CPU调度线程资源开销）。

有兴趣的话，可以自行实现并测试。

## 4. 创建数据库存储过程
存储过程能提升的性能点在于减少数据库与应用程序多次传递信息的时间，降低事务完成时间，从而提升性能。不过后面若再分库则存储过程不可用。

有兴趣的话，可自行实现并测试。

## 5. PG置优化
PG配置优化需要依据硬件性能处理，可依据[postgresqltuner](https://github.com/jfcoz/postgresqltuner) 配置一遍。我在 macOS docker 镜像里优化PG 并没有太多意义，故省略掉。

至于 Redis 、Web框架配置优化，在当前的测试场景下，提升空间有限。
