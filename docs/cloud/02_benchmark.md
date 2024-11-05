# 性能测试
测试环境和运行测试脚本方法参考 [单机基准测试](../one/00_base_benchmark.md)。

本地开发机器测试结果仅是用来验证正确性，参考意义不大。微服务架构需要构建真实环境去测试才合理。

## 本地开发机器测试
### RPC 基准测试
```sh
sbt 'Gatling/testOnly com.timzaak.cloud.BaseRPCBenchmark' -Dusers=200 -Drepeat=100
```
包含一次redis + 一次 dubbo rpc 调用，QPS约`1300`,75%响应`182ms`。

### 事务基准测试(Seata)
由于机器资源有限，在测试过程中，Seata 会崩溃，具体数据可参考 [生产环境性能测试](../benchmark.md)
