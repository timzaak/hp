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
> 在测试中，数据库、后台服务器 CPU 均未超过 35%，也就是给的100用户数还很保守，除了6、7外其它还未测出其上限。“库存冲突影响性能”的结论不变。


### 环境搭建脚本
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

## 微服务测试
TODO：
