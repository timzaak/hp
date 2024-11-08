# 正式环境性能测试

性能测试代码未找到打包方案，故在测试机器上 git clone 代码，并编译。脚本如下：

## 单实例测试
在 UCloud 上海B区跑，原因是UCloud上有钱。

### 运行环境
| 服务       | 配置        |
|------------|-------------|
| PostgreSQL | 2C4G 20G RSSD 16.4 高可用版|
| Backend    | Ubuntu 4C8G |
| Redis      | 版本7.0 和backend同一台机器，docker部署|
| Gatling |Ubuntu 4C8G|

> backend镜像 redis host 配置无法生效，依旧指向本地，其余配置可生效，故Redis采用自建。
### 部署服务
```sh
docker run -d --network=host --name=backend \
-v $(pwd)/application.yml:/server/config/application.yml \
ghcr.io/timzaak/hp-backend:0.0.1

# 国内可以用 ghcr.nju.edu.cn/timzaak/hp-backend:0.0.1 加速
```

### 测试结果
| 测试内容                                 | TPS/QPS | 75%响应时常（ms） | 错误率 | 备注                               |
|------------------------------------------|:---------:|:--------:|:--------:|------------------------------------|
|1. 请求基准测试（300用户100次）|     6000(cpu 高峰 30%)   |55   |   0  | 1 redis session check + 1 db query |
|2.请求基准测试（100用户100次）|     5000   |18   |   0  | 1 redis session check + 1 db query |
|3.下单基准测试 （100用户，500次）|      3125  | 39      | 0       | 数据库纯插入，无事务               |
|4.下单纯数据库事务（10用户1000次）         |   3571.43|  40   |    0.28    | 用户只购买一款产品，类似秒杀       |
|5.下单纯数据库事务（100用户500次）         |  3333.33    |    39    |    0.52    | 用户只购买一款产品，类似秒杀       |
|6.下单纯数据库事务（100用户500次）         |     148.37    |   1029   |  0.25    | 用户只购买1～3款产品      |(毛刺严重)
|7.拆分事务（100用户500次）                 |      97.47	   |     2016       |   0.23     | 用户只购买1～3款产品|(毛刺严重)
|8.库存以内存为准，异步落库（100用户500次） |     3571.43	    |     37    |    0.06    | 用户只购买1～3款产品  |


> 依据基准测试来看，除了6，7外，其余相差系数和本地测试大致相同，错误率下降很多。
>
> 至于7为何比6还低，和本地测试结果相反，暂无结论。 
>
> 在测试中，数据库、后台服务器 CPU 均未超过 35%，参数100用户数还很保守，除了6、7外其它还未测出其上限。“库存冲突影响性能”的结论不变。



## 微服务
### 运行环境
| 服务       | 配置        |
|------------|-------------|
| PostgreSQL | 4C8G 40G RSSD 16.4 高可用版|
| 微服务+Redis | Ubuntu 8C16G |
| Redis      | 版本7.0 1G 主从 (只给 Seata 用)|
| Gatling |Ubuntu 4C8G|
|Nacos、Seata 2.2.0| Ubuntu 8C16G， 采用redis 作为数据备份｜

### 测试结果
| 测试内容                                 | TPS/QPS | 75%响应时常（ms） | 错误率 | 备注                               |
|------------------------------------------|:---------:|:--------:|:--------:|------------------------------------|
|1. RPC 基本测试（200用户100次）|     4000   |49   |   0  | 1 redis session check + 1 db query |
|2. RPC 基本测试（500用户100次）|     6250   |91   |   0  | 1 redis session check + 1 db query |
|3. RPC 基本测试（1000用户100次）|     8333.33	 |126   |   0  | 1 redis session check + 1 db query |
|4. RPC 基本测试（1500用户100次）|     8823.53	|176  |   0  | 1 redis session check + 1 db query |
|5. 事务基准测试 （200用户，100次）|    1538.46  | 138   | 0       | 只打印日志，无事务   |
|6. 事务基准测试 （1000用户，100次）|    2777.78  | 398   | 0       | 只打印日志，无事务   |
|7. 事务基准测试 （1500用户，100次）|    2777.78  | 596   | 0       | 只打印日志，无事务   |
|8. 库存以内存为准，异步落库（200用户100次） |     1818.18		|     108    |    0    | 用户只购买1～3款产品  |
|9. 库存以内存为准，异步落库（1000用户100次） |     2083.33    |     611	 |    0    | 用户只购买1～3款产品  |出现波峰波谷
|10. 库存以内存为准，异步落库（1500用户100次） |     1973.68	    |     988    |    0    | 用户只购买1～3款产品  | 波峰波谷变平缓

> 在以上测试，数据库最高使用率在4%， 最高TPS 3432。 Seata 机器 CPU 最高达到48%。 微服务的服务器达到81%。
>
> 该轮测试存在的问题：
> 1. 使用的服务器配置还是较低，在实际生产中，一般会提升到20核40G。
> 2. 未能用到微服务的LB能力。相比于单实例版本，多增加了RPC和分布式事务开销，TPS降低接近一半。

### 部署服务
#### 配置文件

后台服务配置文件：
```yml
dubbo:
  application:
    logger: slf4j
    name: ${APP_NAME}
    qos-enable: false
    check-serializable: false
  registry:
    address: nacos://${NACOS_ADDRESS}:8848?username=nacos&password=nacos
  protocol:
    port: ${DUBBO_PORT}
    name: tri

seata:
  registry:
    type: nacos
    nacos:
      server-addr: ${SEATA_ADDRESS}:8848
      group: 'SEATA_GROUP'
      namespace: ''
      username: 'nacos'
      password: 'nacos'
      application: seata-server
  tx-service-group: my_tx_group
  service:
    vgroup-mapping:
      my_tx_group: default
    disable-global-transaction: false
  enable-auto-data-source-proxy: false

spring:
  application:
    name: ${APP_NAME}
  datasource:
    url: jdbc:postgresql://${PG_ADDRESS}:5432/db
    username: root
    password: postgres_t
    driver-class-name: org.postgresql.Driver
  data:
    redis:
      host: ${REDIS_ADDRESS}
      jedis:
        pool:
          enabled: true
server:
  port: ${WEB_PORT}          
```




## 测试环境搭建

```shell
# Linux 安装docker，参考官网：
# 1. https://docs.docker.com/engine/install/ubuntu/#install-using-the-repository 安装docker
# 2. https://docs.docker.com/engine/install/linux-postinstall/ user 权限
# 测试环境Linux系统统一为 ubuntu 

sudo apt install zip unzip
curl -s "https://get.sdkman.io" | bash
git clone --depth=1 https://github.com/timzaak/hp.git
sdk install java 17.0.13-tem
sdk install sbt
sdk use java 17.0.13-tem
cd hp/benchmark
sbt compile
## 修改 pg、redis、api server 参数
## vim src/main/resources/application.conf 

## ubuntu install docker
sudo apt-get update
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
sudo mkdir -p /etc/apt/sources.list.d
# Add the repository to Apt sources:
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update

sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

sudo groupadd docker
sudo usermod -aG docker $USER
newgrp docker
dokcer ps
```

### 微服务部署脚本
```shell
# Nacos
docker run -d --name=nacos  --network=host \
-e PREFER_HOST_MODE=hostname \
-e MODE=standalone \
-e NACOS_AUTH_IDENTITY_KEY=serverIdentity \
-e NACOS_AUTH_IDENTITY_VALUE=security \
-e NACOS_AUTH_TOKEN=SecretKey012345678901234567890123456789012345678901234567890123456789 \
nacos/nacos-server:v2.4.3

# Seata
docker run -d --name seata --network=host \
-e SEATA_IP=10.23.84.252 \
-v $(pwd)/seata.application.yml:/seata-server/resources/application.yml \
apache/seata-server:2.2.0
# -v $(pwd)/seata.logback.xml:/seata-server/resources/logback-spring.xml \

docker run -d --network=host --name=order \
-e APP_NAME=order \
-e PG_ADDRESS=10.23.175.165 \
-e SEATA_ADDRESS=10.23.84.252 \
-e NACOS_ADDRESS=10.23.84.252 \
-e REDIS_ADDRESS=127.0.0.1 \
-e DUBBO_PORT=20883 \
-e WEB_PORT=8080 \
-v $(pwd)/application.yml:/server/config/application.yml \
ghcr.io/timzaak/hp-cloud-order:0.0.5

docker run -d --network=host --name=product \
-e APP_NAME=product \
-e PG_ADDRESS=10.23.175.165 \
-e SEATA_ADDRESS=10.23.84.252 \
-e NACOS_ADDRESS=10.23.84.252 \
-e REDIS_ADDRESS=10.23.129.74 \
-e DUBBO_PORT=20882 \
-e WEB_PORT=8081 \
-v $(pwd)/application.yml:/server/config/application.yml \
ghcr.io/timzaak/hp-cloud-product:0.0.5

docker run -d --network=host --name=others \
-e APP_NAME=others \
-e PG_ADDRESS=10.23.175.165 \
-e SEATA_ADDRESS=10.23.84.252 \
-e NACOS_ADDRESS=10.23.84.252 \
-e REDIS_ADDRESS=10.23.129.74 \
-e DUBBO_PORT=20881 \
-e WEB_PORT=8082 \
-v $(pwd)/application.yml:/server/config/application.yml \
ghcr.io/timzaak/hp-cloud-others:0.0.5

```
