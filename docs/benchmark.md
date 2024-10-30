# 正式环境性能测试

性能测试代码未找到打包方案，故在测试机器上 git clone 代码，并编译。脚本如下：

```shell
# 测试环境Linux系统统一为 ubuntu
curl -s "https://get.sdkman.io" | bash
git clone --depth=1 https://github.com/timzaak/hp.git

sdk install java 17.0.13-tem
sdk install sbt
sdk use java 17.0.13-tem
cd hp/benchmark
sbt compile
## 修改 pg、redis、api server 参数
## vim src/main/resources/application.conf 

```
## 单实例测试

### 运行环境
| 服务       | 配置        |
|------------|-------------|
| Redis      | 1G          |
| PostgreSQL | 4C16G 100SSD       |
| Backend    | Ubuntu 4C8G |

### 部署服务
| 测试内容                                 | TPS/QPS | 75%响应时常（ms） | 错误率 | 备注                               |
|------------------------------------------|:---------:|:--------:|:--------:|------------------------------------|
| 请求基准测试                             |         |                   |        | 1 redis session check + 1 db query |
| 下单基准测试                             |         |                   |        | 数据库纯插入，无事务               |
| 下单纯数据库事务（10用户1000次）         |         |                   |        | 用户只购买一款产品，类似秒杀       |
| 下单纯数据库事务（100用户500次）         |         |                   |        | 用户只购买一款产品，类似秒杀       |
| 下单纯数据库事务（100用户500次）         |         |                   |        | 用户只购买1～3款产品               |
| 拆分事务（100用户500次）                 |         |                   |        | 用户只购买1～3款产品               |
| 库存以内存为准，异步落库（100用户500次） |         |                   |        | 用户只购买1～3款产品               |


### 测试结果


## 微服务测试
### 运行环境
直接基于阿里云的MSE来做，这样不用部署中间件，
| 服务       | 配置        |
|------------|-------------|
| nacos2.x专业版| 2C4G 3节点  |
| PostgreSQL | 4C16G 100SSD      |
| Backend    | Ubuntu 4C8G |


### 硬件环境