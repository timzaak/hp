# 介绍
微服务目前分为两类，一类是RPC框架增强，例如Spring Cloud，一类是Service Mesh，例如K8S + Istio。

## 开发套件选型
此两类中国内主流采用的技术框架是第一类： Spring Cloud/Spring Cloud Alibaba。 目前Spring Cloud Ailibaba 技术套件都在奔着支持K8S去，例如 dubbo 支持 Service Mesh  

本文中采用国内流行的 Spring Cloud Alibaba 套件。引入中间件：
1. Nacos：服务发现以及配置分发
2. RocketMQ：异步消息（延迟消息、事务消息）
3. Dubbo：RPC 框架，3.x 支持GRPC，多语言支持
4. Seata：事务处理

关于这套中间件，需要着重澄清一下：

服务发现及配置分发：这个即使不用Nacos，也需要解决：
1. 密钥管理，多配置复用。只用 K8S configmap+env 维护成本上不一定低。国外出名的有 Consul 可以比对着用。
2. 服务发现：这个需要和RPC做适配，但 Dubbo 首要支持的就是 Nacos。

RPC 框架选择 Dubbo + protobuf：
1. 若是选择GRPC，还是需要解决GRPC和服务发现服务融合的问题。
2. 采用 Protobuf 协议是因为通用性好，性能也可以，多语言支持, Dubbo 3 推荐此方案。
3. 服务发现支持 K8S，方便以后迁移。

队列选择 RocketMQ：
1. 内置支持事务消息、延迟消息，方便用，性能也够业务开发。
（但由于本机测试问题，先不引入）

分布式事务处理 Seata：
1. AT、XA 模式显著降低程序员写rollback 逻辑错误的可能性。
2. 目前尚未发现其它好的替代品。

## 微服务治理选型

### Trace + Metric
SkyWalking 国内用的多，OpenTelemetry 多语言框架支持性好，这个最终看所部署的云平台支持哪个好用哪个。若自建，则需要基于运维工程师的经验来选。

### Log
云平台都有集成方案，自建图便宜走Lokki，想快走ELK，数据量太大走 Clickhouse 二开。
