# 单服务到微服务

微服务相比于单服务，是以并行提升响应速度、以多服务协同提升吞吐。但也因此引入了很多问题，本文简单描述为何要转移到微服务架构以及微服务架构需要解决的问题。

## 为什么要转变为微服务架构
1. 某些特定需求当前项目选型无法满足，例如 java 项目中的一些算法需要加密，即使混淆也不好处理。
2. 根据成员结构，进行拆分项目，以便各自更高效的开发。
3. 程序中的某一块，对特定资源依赖严重，拆分出去。方便提升硬件利用率，例如算法服务。
4. 核心程序，对其它员工保密。
5. 编译耗时、bug越来越难排查、上线次数一天超过n次、新人无法快速承担任务等等影响研发效率的事情。

至于这类的问题：
> 数据库、Redis、队列等已经需要构建多个实例，单服务需要维护多个链接线程池。

其本质都是路由问题，可通过中间代理层解决。


## 微服务架构带来的影响
微服务需要解决的问题有如下：
### 单机开发变困难
依赖服务要么mock，要么RPC至测试环境，但多人用同一套开发环境需要注意解决冲突问题。

### 微服务治理
1. 请求链路要可观测

一个请求完成可能涉及多个部门的微服务，需要引入跟踪机制。请求出问题时，可确认问题出现在哪个微服务上。

2. 服务编排部署

一般是要走 k8s 或 Ansible 编排脚本和监控工具，当然单实例也需要部署，只是复杂度会低很多。


### 分布式事务
若是单机+DB代理架构，补充上流水，然后基于流水定时扫描，执行回滚（幂等）、重试即可。

但在微服务架构下，原本的一个事务会拆分成多个微服务中事务，这就需要提取出一个事务仲裁方，告知每个参与事务的微服务执行 or 回滚， 或者将依据实际业务偏向（good case 占据总请求90%），先默认都会执行成功，再复核，若不成功，再走补偿，确保数据最终一致性。

目前国内比较流行的是 Seata 分布式事务框架 和 RocketMQ 事务消息，我们依据此来了解分布式事务的常见解决方案。

#### Seata
:::warning
在使用 Seata 进入生产之前，请做好性能测试，确认是否能满足业务需求。
:::

Seata 框架可充当事务仲裁方的解决方案。我们简述一下Seata支持的事务模式。
##### AT、XA
AT代理 JDBC，第一阶段先写参数+rollback SQL，再异步化提交所有微服务的SQL，若有失败，则根据 rollback SQL 反向补偿，从而实现最终一致性。

XA是某组织定义的分布式事务处理标准，在数据库内部实现，强一致性，用法上和 AT 大致相同。

::: warning
注意以上都是代理JDBC来处理，若有其它数据源例如 Redis，则不可用。
:::
##### TCC
Try、Confirm、Cancel的缩写，需要业务方自行实现第一阶段 Try 和第二阶段 Confirm、Cancel 的业务逻辑。

:::warning
Seata TCC模式使用注意事项：
1. `@GlobalTransactional` 必须在 `@Service` 下用，在 Controller 层不生效。
2. 执行微服务需要用到 `@LocalTCC`，这个负责当前微服务的事务提交、回滚。
3. preare、commit、rollback 必须要写在 interface 内，然后用类实现，要么无法代理。
4. prepare 失败，当前事务绑定的 rollback 会被调用。
5. rollback、commit 返回 false，会触发该函数再次调用，即使重启服务也依旧会有。 
6. prepare 和 rollback 执行先后顺序不是固定的，需要预防，可以通过 useTCCFence(需要建数据库表) 来简单解决。
:::

##### Saga
TODO：
#### RocketMQ
RocketMQ 支持事务消息：发起方先给队列发送一条消息，再做业务逻辑，处理完毕后再给队列发此消息确认指令，队列此时才会将消息投送给消费者。

此方案除了削峰填谷作用外，也能回溯，方便做对账逻辑。


### 分布式锁
解决微服务之间修改特定信息具有排他性的问题，可将锁信息放置在数据库 or redis。 数据库有悲观、乐观一说，redis 有可重入锁、超时一说。

在实际情况中，最麻烦的点是锁的颗粒度大小调整，大则性能低，小则需要预防死锁，回滚逻辑也多。

### 服务编排
一般是上K8S，但它会有很多额外的性能开销：
1. k8s 的网络若无法接入物理网关，则会有一层虚拟网络开销。
2. 目前云厂商流量入口的方案是 云LB（四层） + Ingress（四层、七层），多了一层转发。
3. 相比于服务发现直接传递IP，k8s 一般会采用 dns 来做服务发现，也会有 dns lookup 开销。
5. 容器性能开销。

最新版本的k8s 引入api gateway，大的云厂商也都支持网络接入物理网关。很多问题都在优化中，若在云厂商部署微服务，建议直接用他们的套件+k8s编排，可省掉巨量的运维工作。若自建机房，则慎重考虑是否上k8s。