# 正式环境性能测试

性能测试代码未找到打包方案，故在测试机器上 git clone 代码，并编译。脚本如下：

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
## 单实例测试
在UCloud 上海区跑，原因是UCloud上有钱。
### 运行环境
| 服务       | 配置        |
|------------|-------------|
| Redis      | 2G 主从 7.0版本        |
| PostgreSQL | 2C4G 20G RSSD 16.4|
| Backend    | Ubuntu 4C8G |
| Gatling |Ubuntu 4C8G|
### 部署服务
```sh
docker run -d --network=host --name=backend \
-v $(pwd)/application.yml:/server/config/application.yml \ 
ghcr.io/timzaak/hp-backend:0.0.1


```

```yml
spring:
  application:
    name: backend
  datasource:
    url: jdbc:postgresql://10.23.214.202:5432/testdb
    username: root
    password: postgres_
    driver-class-name: org.postgresql.Driver
  data:
    redis:
      host: 10.23.171.15
      jedis:
        pool:
          enabled: true
```



### 测试结果
| 测试内容                                 | TPS/QPS | 75%响应时常（ms） | 错误率 | 备注                               |
|------------------------------------------|:---------:|:--------:|:--------:|------------------------------------|
| 请求基准测试                             |         |                   |        | 1 redis session check + 1 db query |
| 下单基准测试                             |         |                   |        | 数据库纯插入，无事务               |
| 下单纯数据库事务（10用户1000次）         |         |                   |        | 用户只购买一款产品，类似秒杀       |
| 下单纯数据库事务（100用户500次）         |         |                   |        | 用户只购买一款产品，类似秒杀       |
| 下单纯数据库事务（100用户500次）         |         |                   |        | 用户只购买1～3款产品               |
| 拆分事务（100用户500次）                 |         |                   |        | 用户只购买1～3款产品               |
| 库存以内存为准，异步落库（100用户500次） |         |                   |        | 用户只购买1～3款产品               |


## 微服务测试
### 运行环境
直接基于阿里云的MSE来做，这样不用部署中间件，
| 服务       | 配置        |
|------------|-------------|
| nacos2.x专业版| 2C4G 3节点  |
| PostgreSQL | 4C16G 100SSD      |
| Backend    | Ubuntu 4C8G |


### 硬件环境