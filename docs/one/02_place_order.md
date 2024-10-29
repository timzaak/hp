# 下单详解
下单是大部分从0编写的电商项目最常见容易出问题的地方：超卖、漏卖、事务处理。这些问题解决方案已比较成熟，下文会描述常见下单流程，并给出代码实现及性能测试。

## 下单常见问题及如何解决
### 动态变化的信息
订单结算的关联信息很多处于动态变化中，例如：
1. 商品库存低于购买量、商品下架。
2. 商品价格变更。
3. 优惠券、活动失效（过期、禁用、取消等）。
4. 储值/积分不够抵扣。
5. 会员等级变更。

以上信息点任何一个在下单期间产生变化，都会造成容易造成订单生成失败。其中最容易被提及的是库存数对不齐，该问题后续会重点解决。至于优惠券、积分等出现概率相对较低，影响程度相对较小。可通过下述来进行解决：
> 前台传递影响订单价格的所有信息到后台，后台在锁完库存后进行校验，若产生变化，需要告知用户具体原因。
> 若简单点做，则前端只传递价格，后端算完价格进行比对。不一致，则告知用户，前端也重新获取价格或影响价格计算的相关信息。

### 秒杀
秒杀的核心问题在于：**缺货后，仍有很多请求**。这些无效请求要以尽量小的代价过滤掉（不占用数据库资源），一般解决方案是将库存同步至 redis 缓存，在缓存层过滤掉无效请求。
若此时性能仍不达标，可以看看是否能够禁用优惠券、积分等减少数据库写操作。

## 数据库建表
参考上一章 [电商业务梳理](./01_business_abstruct.md)，我们创建数据库表。建表SQL文件: <a v-bind:href='resourceSrc + "/b2c_mall_init.sql"'>在此</a>。

::: warning
为了方便开发测试，每一次运行 b2c_mall_init.sql 都会删除所有表并重建。

性能测试代码会自动执行 b2c_mall_init.sql，不需手动创建。
:::

由于我们目前只关注下单流程，做了很多简化和模块移除，
移除的模块有：
> 储值、会员、购物车、售后（退货、维修）、撤销订单申请等
 
简化部分如下并附带一些说明：
### 用户
不建用户表，直接在 Redis 里构建 sessionID(String) -> userID（Int）映射，实现用户认证逻辑。

### 商品
简化定义：
* 一个商品对应一个SKU、一个库存。
* 快递费全免。
* 无分类、无规格、无详情、无活动、无赠品。
* 商品只包含一下属性：商品名、库存、销量、价格、状态（上下架）。
除库存、销量以外的商品信息，还需添加*商品快照表*，用来解决：
1. 购物车等需要读取商品变更历史的业务逻辑。
2. 用户购买时看到的商品内容和订单商品快照保持一致。

### 优惠券
简化定义：
* 减钱方式只有折扣、满减。
* 无时间限制、无商品限制、无活动限制。
* 无优惠券模版、无优惠券分发。

### 积分
简化定义：
* 积分永久有效。
* 不做积分增减记录、积分冻结、积分分发。
* 1积分等于1分钱。
* 每个订单可使用全部积分。

### 订单
订单ID：使用PG自带的ID生成方案：`nextval(order_primary_index)`（注意事务回滚不会回滚nextval）。若是为了id 可读性，可通过构造`format(order_created_at, 'yyyymmdd') + hashIds(order_id)`来生成可读订单ID给到用户，但注意hashIds长度不固定，密钥配置泄漏后，订单号可被倒推出订单ID。后续做分布式时可选择用分布式ID生成策略。

我们只关注下单流程，所以订单表数据结构一切从简：*无支付及以后的流程字段*。

### 建表字段补充说明
1. 价格、总花费
订单的总价、商品价格用 PG `numerical(10,2)`类型：精确到小数点2位，最大值为：999999999.99（10亿-1分），若是需要考虑日本等汇率较低的国家，需要扩大此值或用 `decimal` 类型。
2. 时间戳
时间戳统一为 `timestamp with time zone`，很多人在跑线上代码的时候会忘设置服务、数据库时区，导致时区按照 utc+0 时区走，说的更高大上一些是为后续服务跨时间做准备。
3. 用户ID
用户ID 使用 serial 类型, 不需要用 bigserial，因为 serial 的取值范围为：(1 to 2,147,483,647)，除非你做到顶级中的顶级，否则完全够用。 java 程序中可用 int/long来表述，虽然 int 会出现负值，但不影响ID唯一性。
4. 手机号
mobile 手机号在 PG 数据库里，直接用`text`类型即可，但 MySQL 会麻烦一些，因为手机号不止11位，会有 +86 国际电话区号问题，需要适当延长长度；手机号还存在换号问题，要允许 null 值。



## 代码实现

![simple_place_order](/img/simple_place_order.jpg)

上图简要描述了完整的下单逻辑，我们以此以及上一章的订单业务梳理实现本代码：
### 无事务（基准测试）
逻辑代码：<a v-bind:href="codeSrc + '/controller/Order1Controller.java'">Order1Controller.java</a> `/order1_without_transaction`

测试代码：<a v-bind:href="benchSrc + '/one/Order1NoTransactionBenchmark.scala'">Order1NoTransactionBenchmark.scala</a>

```shell
sbt 'Gatling/testOnly com.timzaak.one.OrderNoTransactionBenchmark'
```

测试结果： **TPS 约 `450`，75%响应时间低于`265ms`**。
![order_with_transaction](/img/order1_no_transaction.jpg)

:::warning
`/order1_without_transaction` 有个假设： 库存可能失败，其余总价计算、积分等都一定成功。但在实际业务中不可能成立。所以这段代码主要是做为基准测试使用，用来确认最高能达到什么程度。

:::

### 数据库事务（Isolation SERIALIZABLE）
逻辑代码：<a v-bind:href="codeSrc + '/controller/Order1Controller.java'">Order1Controller.java</a> `/order1`

测试代码：
1. <a v-bind:href="benchSrc + '/one/Order1OneProductBenchmark.scala'">Order1OneProductBenchmark.scala</a> ：模拟用户只购买一款产品（共10款产品，类似秒杀）。

```shell
sbt 'Gatling/testOnly com.timzaak.one.Order1OneProductBenchmark -Duser=10 -Drepeat=1000'

sbt 'Gatling/testOnly com.timzaak.one.Order1OneProductBenchmark -Duser=100 -Drepeat=500'

```

2. <a v-bind:href="benchSrc + '/one/Order1Benchmark.scala'">Order1Benchmark.scala</a> ：模拟100用户下单500次,每次随机下单1～3款商品（共10款商品）。

```shell
 sbt 'Gatling/testOnly com.timzaak.one.Order1Benchmark'
```

### 结果

10人只买一款产品（各1000次）：**TPS 约 `470`, 75% 响应时间低于 `211ms`, `0.01%`失败（数据库 40001）。** （后来再测了几次，有约TPS 442，7.1% 失败率的结果。 ）
![order1_buy_oneProduct](/img/order1_buy_oneProduct.png)
PS：相比于无事务版本 TPS 高的原因是：商品从随机 1～3 款变为 1款。

100人只买一款产品（各500次）： **TPS 约 `410`, 75% 响应时间低于 `250ms`, `10.32%`失败（数据库 40001）。**
![buy one product 100](/img/order1_buy_one_product_100.png)
100人买1～3款产品（各500次）： **TPS 约 `150`，75%响应时间低于`252ms`，`4.3%` 失败（数据库 40001）**。
![order_with_transaction](/img/order1_transaction.jpg)

::: tip
40001 是数据库事务隔离级别为 `SERIALIZABLE` 的正常现象,一般需要添加重试机制解决，但在当前场景下只会加重数据库事务负担，重试机制并不是正向优化。

`sbt 'Gatling/testOnly com.timzaak.one.Order1RetryBenchmark'` 这个指令会测试带有重试事务下单，可以自行尝试一下。

另外事务级别在这个场景下可设置为 `Read Committed`，但考虑到这个更改和其它业务事务容易冲突，在后续排查错误数据时难以复现，故提升到最高级。
:::

根据以上测试结果可看出，若无数据竞争，数据库事务并不会影响多少性能，当涉及商品库存数据竞争时，尤其是多个商品时，TPS 会从 470 掉到 150，相差甚远。

**商品库存扣减冲突** 是下单接口性能瓶颈所在。

我们将在下一章进行优化。
