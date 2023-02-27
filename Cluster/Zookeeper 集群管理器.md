# Zookeeper 集群管理器

这是使用 [Zookeeper](https://zookeeper.apache.org/) 的 Vert.x 集群管理器实现。

它完全实现了vert.x集群的接口。 因此，如果需要，您可以使用它来代替 vertx-hazelcast。 这个实现被打包在里面：

```xml
<dependency>
  <groupId>io.vertx</groupId>
  <artifactId>vertx-zookeeper</artifactId>
  <version>4.2.7</version>
</dependency>
```

在 Vert.x 中，集群管理器用于各种功能，包括：

- 集群中 Vert.x 节点的发现和组成员身份
- 维护集群范围的主题订阅者列表（因此我们知道哪些节点对哪些事件总线地址感兴趣）
- 分布式Map支持
- 分布式锁
- 分布式计数器

<mark>**集群管理器不处理事件总线节点间传输，这由 Vert.x 通过 TCP 连接直接完成。**</mark>

## 如何工作

我们正在使用 [Apache Curator](https://curator.apache.org/) 框架而不是直接使用 zookeeper 客户端，因此我们对 Curator 中使用的库有依赖关系，例如`guava`、`slf4j`，当然还有`zookeeper `。

由于 ZK 使用树字典来存储数据，我们可以将根路径作为命名空间,在 `default-zookeeper.json` 中默认根路径是 `io.vertx`。 在 vert.x 集群管理器中还有另外 5 个子路径用于记录功能的其他信息，您可以更改的路径是`根路径`。

你可以在`/io.vertx/cluster/nodes/`的路径中找到所有的vert.x节点信息.
`/io.vertx/asyncMap/$name/`记录你用`io.vertx.core.shareddata.AsyncMap` 接口创建的所有`AsyncMap`。 
`/io.vertx/asyncMultiMap/$name/`记录你用`io.vertx.core.spi.cluster.AsyncMultiMap`接口创建的所有`AsyncMultiMap`。 
`/io.vertx/locks/`记录分布式Locks信息。 
`/io.vertx/counters/`记录分布式Count信息。

## 使用此集群管理器

如果您从命令行使用 Vert.x，则与该集群管理器对应的 jar（它将被命名为 `vertx-zookeeper-4.2.7.jar` ）应该在 Vert.x 安装的 `lib` 目录中 .

如果你想在你的 Vert.x Maven 或 Gradle 项目中使用这个集群管理器进行集群，那么只需在你的项目中添加一个依赖项到工件：`io.vertx:vertx-zookeeper:${version}`。

> **🏷注意:** 如果 jar 如上所述在您的类路径中，那么 Vert.x 将自动检测到它并将其用作集群管理器。 请确保您的类路径中没有任何其他集群管理器，否则 Vert.x 可能会选择错误的。

如果要嵌入 Vert.x，还可以通过在创建 Vert.x 实例时在选项上指定集群管理器，以编程方式指定集群管理器，例如：

```java
ClusterManager mgr = new ZookeeperClusterManager();
VertxOptions options = new VertxOptions().setClusterManager(mgr);
Vertx.clusteredVertx(options, res -> {
  if (res.succeeded()) {
    Vertx vertx = res.result();
  } else {
    // failed!
  }
});
```

## 配置此集群管理器

通常集群管理器由一个文件 [default-zookeeper.json](https://github.com/vert-x3/vertx-zookeeper/blob/master/src/main/resources/default-zookeeper.json) 配置，该文件包装在jar里。

`default-zookeeper.json`文件内容:
```json
{
  "zookeeperHosts":"127.0.0.1",
  "sessionTimeout":20000,
  "connectTimeout":3000,
  "rootPath":"io.vertx",
  "retry": {
    "initialSleepTime":100,
    "intervalTimes":10000,
    "maxTimes":5
  }
}
```

如果你想覆盖这个配置，你可以在你的类路径中提供一个名为`zookeeper.json`的文件，这个文件将被使用。 如果要将`zookeeper.json`文件嵌入到 fat jar 中，它必须位于 fat jar 的根目录下。 如果是外部文件，则必须将包含该文件的*目录**添加到类路径中。 例如，如果您使用 Vert.x 中的 *launcher* 类，则可以按如下方式进行类路径增强：

```bash
# If the zookeeper.json is in the current directory:
java -jar ... -cp . -cluster
vertx run MyVerticle -cp . -cluster

# If the zookeeper.json is in the conf directory
java -jar ... -cp conf -cluster
```

另一种覆盖配置的方法是为系统属性`vertx.zookeeper.conf`提供一个位置：

```bash
# Use a cluster configuration located in an external file
java -Dvertx.zookeeper.config=./config/my-zookeeper-conf.json -jar ... -cluster

# Or use a custom configuration from the classpath
java -Dvertx.zookeeper.config=classpath:my/package/config/my-cluster-config.json -jar ... -cluster
```
> <mark>**🔔重要:**</mark> 翻译者白石发现不能加`classpath:`前缀

`vertx.zookeeper.config` 系统属性，如果存在，会覆盖类路径中的任何 `zookeeper.json`，但如果从该系统属性加载失败，则加载回退到 `zookeeper.json` 或 Zookeeper的 默认配置 .

配置文件在 `default-zookeeper.json 的注释中有详细描述。

如果要嵌入，也可以通过编程方式指定配置:

```java
JsonObject zkConfig = new JsonObject();
zkConfig.put("zookeeperHosts", "127.0.0.1");
zkConfig.put("rootPath", "io.vertx");
zkConfig.put("retry", new JsonObject()
    .put("initialSleepTime", 3000)
    .put("maxTimes", 3));


ClusterManager mgr = new ZookeeperClusterManager(zkConfig);
VertxOptions options = new VertxOptions().setClusterManager(mgr);

Vertx.clusteredVertx(options, res -> {
  if (res.succeeded()) {
    Vertx vertx = res.result();
  } else {
    // failed!
  }
});
```

> **⚠重要:** 您还可以使用 `vertx.zookeeper.hosts` 系统属性配置 zookeeper 主机。

### 启用日志记录

当使用 Zookeeper 解决集群问题时，从 Zookeeper 获取一些日志输出以查看它是否正确形成集群通常很有用。 您可以通过在类路径中添加一个名为 `vertx-default-jul-logging.properties` 的文件来执行此操作（使用默认的 JUL 日志记录时）。 这是一个标准的 java.util.logging (JUL) 配置文件。 里面设置：

```properties
org.apache.zookeeper.level=INFO
```

以及

```properties
java.util.logging.ConsoleHandler.level=INFO
java.util.logging.FileHandler.level=INFO
```

## 关于 Zookeeper 版本

我们使用 Curator 4.3.0，因为 Zookeeper 最新的稳定版是 3.4.8，所以我们不支持 3.5.x 的任何特性.

