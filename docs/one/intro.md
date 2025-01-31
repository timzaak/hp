# 介绍
本项目从单机开始探索 Java Web 并发负载能力，以国内最常见的B2C电商业务为应用场景。

所用框架为国内JAVA主流框架：SpringBoot、MyBatis，便于读者实验阅读。

在提升性能方面，主要以业务为出发点，通过**拆分数据库锁粒度、使用缓存**来提升性能，不会细扣Java语言的特殊语法、API，也不会调整数据库、缓存、Web框架性能相关配置，其它编程语言使用者也可拿来参考。

在开始之前，需要声明一下：
<br/>
1. **由于性能测试机器的硬件资源不同，文档中所列性能测试结果仅供参考。** 若需自己跑测，请跑测完基准测试，对机器性能有个大概了解，再去跑其它性能测试。
2. 代码结构不遵循 Controller、Service、Dao + Interface 常规Java编写范式，尽量简化，但会有部分重复性代码，主要是为了方便读者阅读。
3. 所有数据库操作尽量用SQL语句，方便非Java人士理解。
