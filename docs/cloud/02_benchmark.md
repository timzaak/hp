# 性能测试
测试环境和运行测试脚本方法参考 [单机基准测试](../one/00_base_benchmark.md)。

本地开发机器测试结果仅是用来验证正确性，参考意义不大。微服务架构需要构建真实环境去测试才合理。

## 本地开发机器测试
### RPC 基准测试
```sh
sbt 'Gatling/testOnly com.timzaak.cloud.BaseRPCBenchmark' -Dusers=200 -Drepeat=100
```
包含一次redis + 一次 dubbo rpc 调用，QPS约`1300`,75%响应`182ms`。

### 分布式事务基准测试
```sh
sbt 'Gatling/testOnly com.timzaak.cloud.BaseTransactionBenchmark' -Dusers=200 -Drepeat=100
```
测试了两次，seata server 都崩溃，停止测试。

### 下单测试
```sh
sbt 'Gatling/testOnly com.timzaak.cloud.OrderBenchmark' -Dusers=200 -Drepeat=100
```
测试了两次，seata server 都崩溃，停止测试。


## 基于云平台测试

