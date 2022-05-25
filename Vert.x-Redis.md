# Vert.x-Redis

> 翻译: 白石(https://github.com/wjw465150/Vert.x-Core-Manual)

Vert.x-redis 是与 Vert.x 一起使用的 redis 客户端。

该模块允许在 Redis 中保存、检索、搜索和删除数据。 Redis 是一个开源的高级键值存储。 它通常被称为数据结构服务器，因为键可以包含字符串、散列、列表、集合和排序集合。 要使用此模块，您必须在网络上运行 Redis 服务器实例。

Redis 有丰富的 API，可以分为以下几组：

- 集群 - 与集群管理相关的命令，请注意，使用这些命令中的大多数，您需要一个版本 >=3.0.0 的 redis 服务器
- 连接 - 允许您切换数据库、连接、断开连接和对服务器进行身份验证的命令。
- 哈希 - 允许对哈希进行操作的命令。
- HyperLogLog - 用于近似多重集中不同元素数量的命令，即 HyperLogLog。
- 键 - 使用键的命令。
- 列表 - 使用列表的命令。
- 发布/订阅 - 创建队列和发布/订阅客户端的命令。
- 脚本 - 在 redis 中运行 Lua 脚本的命令。
- 服务器 - 管理和获取服务器配置的命令。
- 集 - 使用无序集的命令。
- 排序集 - 使用排序集的命令。
- 字符串 - 使用字符串的命令。
- 交易 - 处理交易生命周期的命令。
- 流 - 处理流的命令。

## 使用 Vert.x-Redis

要使用 Vert.x Redis 客户端，请将以下依赖项添加到构建描述符的 *dependencies* 部分：

- Maven (在你的 `pom.xml`):

```xml
<dependency>
<groupId>io.vertx</groupId>
<artifactId>vertx-redis-client</artifactId>
<version>4.3.0</version>
</dependency>
```

- Gradle (在你的 `build.gradle` file):

```groovy
compile 'io.vertx:vertx-redis-client:4.3.0'
```

## 连接到 Redis

Redis 客户端可以在 4 种不同的模式下运行：

- 简单的客户端（可能是大多数用户需要的）。
- 哨兵（在高可用性模式下使用 Redis 时）。
- 集群（在集群模式下使用 Redis 时）。
- 复制（单个分片，一个节点写入，多个读取）。

连接方式由Redis接口上的工厂方法选择。 无论采用何种模式，客户端都可以使用`RedisOptions`数据对象进行配置。 默认情况下，一些配置值使用以下值初始化：

- `netClientOptions`：默认为 `TcpKeepAlive: true`, `TcpNoDelay: true`
- `endpoint`：默认为 `redis://localhost:6379`
- `masterName`：默认为`mymaster`
- `role` 默认是 `MASTER`
- `useReplicas` 默认为 `NEVER`

为了获得连接，请使用以下代码：

```java
Redis.createClient(vertx)
  .connect()
  .onSuccess(conn -> {
    // use the connection
  });
```

在包含`password` 和/或 `select` 数据库的配置中，一旦成功连接到服务器，这两个命令将自动执行。

```java
Redis.createClient(
  vertx,
  // The client handles REDIS URLs. The select database as per spec is the
  // numerical path of the URL and the password is the password field of
  // the URL authority
  "redis://:abracadabra@localhost:6379/1")
  .connect()
  .onSuccess(conn -> {
    // use the connection
  });
```

## 连接字符串

客户端将识别以下表达式的地址：

```
redis://[:password@]host[:port][/db-number]
```

或者

```
unix://[:password@]/domain/docker.sock[?select=db-number]
```

当指定密码或数据库时，这些命令总是在连接开始时执行。

## 运行命令

鉴于 redis 客户端已连接到服务器，现在可以使用此模块执行所有命令。 该模块提供了一个干净的 API 来执行命令，而无需手动编写命令本身，例如，如果想要获取键的值，可以这样做：

```java
RedisAPI redis = RedisAPI.api(client);

redis
  .get("mykey")
  .onSuccess(value -> {
    // do something...
  });
```

响应对象是一种通用类型，允许从基本的 redis 类型转换为您的语言类型。 例如，如果您的响应是`INTEGER`类型，那么您可以将值作为任何数字原始类型`int`、`long`等...

或者你可以执行更复杂的任务，比如将响应作为迭代器处理:

```java
if (response.type() == ResponseType.MULTI) {
  for (Response item : response) {
    // do something with item...
  }
}
```

## 高可用性(哨兵) 模式

要使用高可用性模式，连接创建非常相似：

```java
Redis.createClient(
  vertx,
  new RedisOptions()
    .setType(RedisClientType.SENTINEL)
    .addConnectionString("redis://127.0.0.1:5000")
    .addConnectionString("redis://127.0.0.1:5001")
    .addConnectionString("redis://127.0.0.1:5002")
    .setMasterName("sentinel7000")
    .setRole(RedisRole.MASTER))
  .connect()
  .onSuccess(conn -> {
    conn.send(Request.cmd(Command.INFO))
      .onSuccess(info -> {
        // do something...
      });
  });
```

需要注意的重要一点是，在这种模式下，会与服务器建立额外的连接，并且在幕后，客户端将侦听来自哨兵的事件。 当哨兵通知我们切换了主控时，就会向客户端发送一个异常，您可以决定下一步该做什么。

## 集群 模式

要使用集群，连接创建非常相似：

```java
final RedisOptions options = new RedisOptions()
  .addConnectionString("redis://127.0.0.1:7000")
  .addConnectionString("redis://127.0.0.1:7001")
  .addConnectionString("redis://127.0.0.1:7002")
  .addConnectionString("redis://127.0.0.1:7003")
  .addConnectionString("redis://127.0.0.1:7004")
  .addConnectionString("redis://127.0.0.1:7005");
```

在这种情况下，配置需要知道集群的多个成员之一。 此列表将用于向集群询问当前配置，这意味着如果列出的任何成员不可用，它将被跳过。

在集群模式下，每个节点都建立了连接，执行命令时需要特别小心。 建议阅读 redis 手册以了解集群的工作原理。 在这种模式下运行的客户端将尽最大努力识别所执行的命令使用了哪个槽，以便在正确的节点上执行它。 在某些情况下，可能无法识别这一点，在这种情况下，将尽可能在一个随机节点上运行该命令。

## 复制 模式

使用复制对客户端是透明的。 获取连接是一项昂贵的操作。 客户端将循环提供的端点，直到找到主节点。 一旦识别出主节点（这是将执行所有写入命令的节点），就会尽最大努力连接到所有副本节点（读取节点）。

有了所有节点知识，客户端现在将过滤对正确节点类型执行读取或写入的操作。 请注意，`useReplica` 配置会影响此选择。就像集群一样，当配置声明副本节点的使用是 `ALWAYS` 时，任何读取操作都将在副本节点上执行，`SHARED` 将在 master 和 replicas 之间随机共享读取，最后 `NEVER` 意味着 永远不会使用副本。

The recommended usage of this mode, given the connection acquisition cost, is to re-use the connection as long as the application may need it.

```java
Redis.createClient(
  vertx,
  new RedisOptions()
    .setType(RedisClientType.REPLICATION)
    .addConnectionString("redis://localhost:7000")
    .setMaxPoolSize(4)
    .setMaxPoolWaiting(16))
  .connect()
  .onSuccess(conn -> {
    // this is a replication client.
    // write operations will end up in the master node
    conn.send(Request.cmd(Command.SET).arg("key").arg("value"));
    // and read operations will end up in the replica nodes if available
    conn.send(Request.cmd(Command.GET).arg("key"));
  });
```

## 发布/订阅 模式

Redis 支持队列和发布/订阅模式，在此模式下操作时，一旦连接调用订阅者模式，则它不能用于运行除离开该模式的命令之外的其他命令。

要启动一个订阅者，首先要做的是:

```java
Redis.createClient(vertx, new RedisOptions())
  .connect()
  .onSuccess(conn -> {
    conn.handler(message -> {
      // do whatever you need to do with your message
    });
  });
```

然后从代码的另一个位置向队列发布消息:

```java
redis.send(Request.cmd(Command.PUBLISH).arg("channel1").arg("Hello World!"))
  .onSuccess(res -> {
    // published!
  });
```

> **🏷注意:** 重要的是要记住命令 `SUBSCRIBE`、`UNSUBSCRIBE`、`PSUBSCRIBE` 和 `PUNSUBSCRIBE` 是 `void`。 这意味着成功的结果是 `null` 而不是响应实例。 然后，所有消息都通过客户端上的处理程序进行路由。

## 域套接字

大多数例子显示了连接到TCP套接字，但是也可以使用Redis连接到UNIX域docket:

```java
Redis.createClient(vertx, "unix:///tmp/redis.sock")
  .connect()
  .onSuccess(conn -> {
    // so something...
  });
```

请注意，HA和集群模式总是在TCP地址上报告服务器地址，而不是在域套接字上。所以两者结合是不可能的。不是因为这个客端户，而是因为Redis的工作方式。

## 连接池

所有客户端变体都由连接池支持。 默认情况下，配置将池大小设置为 1，这意味着它就像单个连接一样运行。 池有 5个可调参数：

- `maxPoolSize` 池中的最大连接数（默认为 `6`）
- `maxPoolWaiting` 池中等待连接的最大请求数。（默认为 `24`）
- `maxWaitingHandlers` backlog()多少积压)。客户端愿意排队的处理程序的数量。（默认为 `2048`）
- `poolCleanerInterval` 连接清除的间隔默认为 `-1`（禁用）
- `poolRecycleTimeout` 超时以保持池上打开的连接等待然后关闭（默认为 `30_000` ）

池是非常有用的，以避免自定义连接管理，例如，你可以使用:

```java
Redis.createClient(vertx, "redis://localhost:7006")
  .send(Request.cmd(Command.PING))
  .onSuccess(res -> {
    // Should have received a pong...
  });
```

务必注意，没有获得或返回任何连接，所有连接都由池处理。但是，当超过1个并发请求试图从池中获取连接时，可能会出现一些可伸缩性问题，为了克服这个问题，我们需要调优池。一个常见的配置是将池的最大大小设置为**可用CPU核数**，并允许请求从池中获得连接到队列:

```java
Redis.createClient(
  vertx,
  new RedisOptions()
    .setConnectionString("redis://localhost:7006")
    // allow at max 8 connections to redis
    .setMaxPoolSize(8)
    // allow 32 connection requests to queue waiting
    // for a connection to be available.
    .setMaxWaitingHandlers(32))
  .send(Request.cmd(Command.PING))
  .onSuccess(res -> {
    // Should have received a pong...
  });
```

> **🏷注意:** 池化与 `SUBSCRIBE`、`UNSUBSCRIBE`、`PSUBSCRIBE` 或 `PUNSUBSCRIBE` 不兼容，因为这些命令会修改连接的操作方式，并且无法重用连接。

## 实现错误重连

虽然连接池非常有用，但为了提高性能，连接不应该是自动管理的，而是由您控制的。 在这种情况下，您将需要处理连接恢复、错误处理和重新连接。

一个典型的场景是，每当发生错误时，用户都希望重新连接到服务器。 自动重新连接不是 redis 客户端的一部分，因为它会强制执行可能不符合用户期望的行为，例如：

1. 当前正在进行的请求应该如何处理？
2. 是否应该调用异常处理程序？
3. 如果重试也会失败怎么办？
4. 是否应该恢复之前的状态（db、authentication、subscriptions）？
5. 等等……

为了给用户充分的灵活性，这个决定不应该由客户来执行。 但是，可以按如下方式实现带有回退超时的简单重新连接：

```java
class RedisVerticle extends AbstractVerticle {

  private static final int MAX_RECONNECT_RETRIES = 16;

  private final RedisOptions options = new RedisOptions();
  private RedisConnection client;
  private final AtomicBoolean CONNECTING = new AtomicBoolean();

  @Override
  public void start() {
    createRedisClient()
      .onSuccess(conn -> {
        // connected to redis!
      });
  }

  /**
   * Will create a redis client and setup a reconnect handler when there is
   * an exception in the connection.
   */
  private Future<RedisConnection> createRedisClient() {
    Promise<RedisConnection> promise = Promise.promise();

    if (CONNECTING.compareAndSet(false, true)) {
      Redis.createClient(vertx, options)
        .connect()
        .onSuccess(conn -> {

          // make sure to invalidate old connection if present
          if (client != null) {
            client.close();
          }

          // make sure the client is reconnected on error
          conn.exceptionHandler(e -> {
            // attempt to reconnect,
            // if there is an unrecoverable error
            attemptReconnect(0);
          });

          // allow further processing
          promise.complete(conn);
          CONNECTING.set(false);
        }).onFailure(t -> {
          promise.fail(t);
          CONNECTING.set(false);
        });
    } else {
      promise.complete();  //@白石注释: 标记为完成,如果已经有了结果,那就还是沿用以前的结果值,否则结果值被赋值成`NULL` 对象.
    }

    return promise.future();
  }

  /**
   * Attempt to reconnect up to MAX_RECONNECT_RETRIES
   */
  private void attemptReconnect(int retry) {
    if (retry > MAX_RECONNECT_RETRIES) {
      // we should stop now, as there's nothing we can do.
      CONNECTING.set(false);
    } else {
      // retry with backoff up to 10240 ms
      long backoff = (long) (Math.pow(2, Math.min(retry, 10)) * 10);

      vertx.setTimer(backoff, timer -> {
        createRedisClient()
          .onFailure(t -> attemptReconnect(retry + 1));
      });
    }
  }
}
```

在本例中，客户机对象将在重新连接时被替换，应用程序将重试最多16次，回退时间最长为1280ms。通过丢弃客户端，我们确保所有旧的flight响应都将丢失，而所有新的响应将在新的连接上。

重要的是要注意，重新连接将创建一个新的连接对象，因此不应该每次都缓存和计算这些对象引用。

## 协议解析器

这个客户端同时支持' RESP2 '和' RESP3 '协议，在连接握手时，客户端会自动检测服务器支持哪个版本并使用它。

解析器在内部从服务器接收的所有块中创建一个“无限”可读缓冲区，为了避免在内存收集方面创建太多垃圾，可在 JVM 启动时配置一个可调水印值。 系统属性 `io.vertx.redis.parser.watermark` 定义了在这个可读缓冲区被丢弃之前保留了多少数据。 默认情况下，此值为 512Kb。这意味着到服务器的每个连接都将使用至少这个数量的内存。 由于客户端工作在流水线模式，保持低连接数提供最佳结果，这意味着将使用 `512Kb * nconn` 内存。 如果应用程序需要大量连接，则建议将水印值减小到更小的值，甚至完全禁用它。

