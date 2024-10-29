# 微服务拆分
参考 [电商业务梳理](one/01_business_abstruct), 按照模块+使用者（运营后台、客户端）进行拆分，但拆分要有个先后顺序，一般如下：
1. 用户/账户（授权认证）模块
这一块需要尽早拆出去，方便集成进 API网关 做安全防护、无效流量拦截。

2. 积分、优惠券、活动
这个地方也容易产生业务变动，另外重要程度低一些，可以拿来给团队学习微服务架构，当然它也放在订单后做也没问题。

2. 商品（店铺装修、商品分类、库存）
这个地方业务变动会比较频繁，所以尽早独立出去。

3. 订单
订单牵扯大部分系统，也对准确性、完整性要求高，需要单独投资源进入。

4. 其它
非核心模块，就看团队情况来处理了。

## 性能测试拆分
我们为方便测试，拆分出三个微服务：
1. 积分、优惠券(others)
2. 订单(order)
3. 商品(product)

为了不引入API网关和复用老代码逻辑，**由订单服务提供`下单接口`，认证逻辑也在这里做**。

### 开发环境
PG 和  Redis docker 镜像部署参考 [单机基准测试](/one/00_base_benchmark)

```shell
# Nacos
# 浏览器访问 http://127.0.0.1:8848/nacos
docker run -d --name=nacos \
-e PREFER_HOST_MODE=hostname \
-e MODE=standalone \
-e NACOS_AUTH_IDENTITY_KEY=serverIdentity \
-e NACOS_AUTH_IDENTITY_VALUE=security \
-e NACOS_AUTH_TOKEN=SecretKey012345678901234567890123456789012345678901234567890123456789 \
-p 8848:8848 -p 9848:9848 \
nacos/nacos-server:v2.4.3

# Seata
# 注意 SEATA_IP 和 seata.applciation.yml 里的IP配置，需要都能互通
# 浏览器访问 http://127.0.0.1:7091, 默认账号密码：seata/seata
docker run -d --name seata -p 8091:8091 -p 7091:7091 \
-e SEATA_IP=192.168.31.146 \
seataio/seata-server:2.0.0
```
<a v-bind:href="cloud+'/seata.application.yml'">seata.application.yml</a>
*注意替换IP为自己的局域网IP*。

<a v-bind:href="cloud">代码目录入口</a>，其中 BaseController 里提供了 `/rpc` `/transaction` API接口，用来测试 dubbo 和 seata。

### 代码注意点
我们以 `库存以redis为准` 的版本来改造，会遇到如下问题：
> 1. TCC分布式事务 try、cancel 乱序 or try 不执行问题，useTCCFence 虽然能解决，但是挂在数据库上解决，库存变更逻辑需要改写 lua 脚本，解决上述问题。
> 2. 由于 Seata，写在 Controller 下单逻辑需要迁移至 Service 层。将数据库操作写在构建 TCCAction interface 实现类里。
