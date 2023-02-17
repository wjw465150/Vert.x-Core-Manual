# Vert.x-Redis

> 翻译: 白石(https://github.com/wjw465150/Vert.x-Core-Manual)

Vert.x-redis 是与 Vert.x 一起使用的 Redis 客户端。

该模块允许在 Redis 中保存、查询、搜索和删除数据。Redis 是一个开源的、先进的键值存储数据库。 它通常被称为数据结构服务器，因为 Redis 的键可以存储字符串、散列、列表、集合和排序集合。 要使用此模块，您的网络上必须运行一个 Redis 服务器实例。

Redis 有着丰富的 API，可以分成以下几组：

- 集群 Cluster - 与集群管理相关的命令, 使用这些命令需要注意 redis server 版本 >=3.0.0 。
- 连接 Connection - 切换数据库，连接，断开连接以及身份认证的命令。
- 哈希 Hashes - 对哈希进行操作的命令。
- 基数统计 HyperLogLog - 对可重复集合中统计不重复元素的命令。
- 键 Keys - 使用 key 相关的命令。
- 列表 List - 使用 list 相关的命令。
- 发布/订阅 Pub/Sub - 创建队列和发布/订阅客户端的命令。
- 脚本 Scripting - 在 Redis 中运行 Lua 脚本的命令。
- 服务器 Server - 管理和获取服务器配置的命令。
- 集合 Sets - 处理无序集合的命令。
- 有序集合 Sorted Sets - 处理有序集合的命令。
- 字符串 Strings - 处理字符串的命令。
- 事务 Transactions - 处理事务生命周期的命令。
- 流 Streams - 处理流的命令。

## 使用 Vert.x-Redis

要使用 Vert.x Redis 客户端，请将以下依赖项添加到项目的 *dependencies* 中：

- Maven（在您的 `pom.xml` 中）：

```xml
<dependency>
<groupId>io.vertx</groupId>
<artifactId>vertx-redis-client</artifactId>
<version>4.3.5</version>
</dependency>
```

- Gradle（在您的 `build.gradle` 文件中）：

```groovy
compile 'io.vertx:vertx-redis-client:4.3.5'
```

## 连接到 Redis

Redis 客户端可以在四种模式下操作：

- 简易客户端 (大多数用户需要的)。
- 哨兵 (在高可用模式下使用 Redis)。
- 集群 (在集群模式下使用 Redis)。
- 分片 (单节点共享， 单节点写入，多节点读取)。

连接方式由 Redis 接口的工厂方法决定。无论客户端是哪种模式， 都可以通过 `RedisOptions` 数据对象来配置。 默认情况下，一些配置项按下面的值初始化：

- `netClientOptions`: 默认为 `TcpKeepAlive: true`, `TcpNoDelay: true`
- `endpoint`: 默认为 `redis://localhost:6379`
- `masterName`: 默认为 `mymaster`
- `role`: 默认为 `MASTER`
- `useReplicas`: 默认为 `NEVER`

用以下代码获得连接：

```java
Redis.createClient(vertx)
  .connect()
  .onSuccess(conn -> {
    // 使用 connection
  });
```

配置中包含 `password` 且/或 `select` 数据库， 在成功建立连接后，这两个命令会自动执行。

```java
Redis.createClient(
  vertx,
  // 客户端处理 REDIS URLs。
  // 规范的数据库URL，密码是 URL 授权的密码字段。
  "redis://:abracadabra@localhost:6379/1")
  .connect()
  .onSuccess(conn -> {
    // use the connection
  });
```

## 连接字符串

客户端会识别表达式上的地址：

```
redis://[:password@]host[:port][/db-number]
```

或

```
unix://[:password@]/domain/docker.sock[?select=db-number]
```

当指定密码或数据库时，这些命令会在连接启动时执行。

## 执行命令

Redis 客户端已连接到服务器，现在可以使用此模块执行所有命令。 例如，该模块提供了一个简洁的 API 来执行命令，而不需要自己手写命令。 如果想要获取键的值，可以这样做：

```java
RedisAPI redis = RedisAPI.api(client);

redis
  .get("mykey")
  .onSuccess(value -> {
    // do something...
  });
```

返回的对象是泛型类型，它允许从基本的 redis 类型转换为您的编程语言类型。 例如，如果返回对象类型为 `INTEGER` ，则可以通过任意数值基本类型获取该值，如 `int`、`long` 等等。

或者，可以执行更复杂的任务，例如将返回的值作为迭代器处理：

```java
if (response.type() == ResponseType.MULTI) {
  for (Response item : response) {
    // do something with item...
  }
}
```

## 高可用模式

在高可用性模式下使用，创建连接的过程非常相似：

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

需要注意的是，在此模式下，将建立额外连接到服务器。 客户端将在后台监听哨兵的事件。当哨兵通知我们切换了主机时， 就会向客户端发送一个异常，您可以决定下一步做什么。

## 集群模式

在集群模式下使用，创建连接的过程也非常相似：

```java
final RedisOptions options = new RedisOptions()
  .addConnectionString("redis://127.0.0.1:7000")
  .addConnectionString("redis://127.0.0.1:7001")
  .addConnectionString("redis://127.0.0.1:7002")
  .addConnectionString("redis://127.0.0.1:7003")
  .addConnectionString("redis://127.0.0.1:7004")
  .addConnectionString("redis://127.0.0.1:7005");
```

在这种情况下，需要配置一个或多个集群成员。 此成员列表用于向集群请求当前配置，这意味着列表中不可用的成员将被跳过。

在集群模式下将建立到每个节点的连接。 在执行命令时需要特别小心，建议阅读Redis手册以了解集群如何工作。 在此模式下操作的客户端会尽量识别执行的命令使用哪个槽（slot），以便在正确的节点上执行它。 如果出现无法识别的情况，最好在随机节点上运行该命令。

## 分片模式

服务器是否使用分片模式运行对客户端来说是透明的。获取一个连接是耗费很大的操作。客户端会遍历所有的节点，直到找到主节点。 一旦找到主节点 (所有的写命令都可以在主节点上执行)，那么客户端会尽力去连接到所有的分片节点 (读取节点)。

当获取到所有的节点后，客户端现在会过滤所有的操作，并在恰当的节点上执行读操作或写操作。注意，由 `useReplica` 配置项控制节点的选择。就像集群模式一样，当分片节点的配置项状态是 `ALWAYS` 时，所有的读操作都会在该节点上执行，当状态是 `SHARED` 时，读操作会在主节点和分片节点上随机执行，而当状态是 `NEVER` 时，读操作不会在该节点上执行。

考虑到获取连接的开销是很大的，因此如果您需要使用分片模式，您的应用需要尽可能地考虑重用数据库连接。

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

## 发布/订阅模式

Redis 支持队列和发布/订阅模式。 在此模式下操作时，当一连接调用订阅模式，则它不能用于运行除退出该模式之外的其他命令。

要启动订阅者，需要执行以下操作：

```java
Redis.createClient(vertx, new RedisOptions())
  .connect()
  .onSuccess(conn -> {
    conn.handler(message -> {
      // do whatever you need to do with your message
    });
  });
```

其他位置的代码将消息发布到队列：

```java
redis.send(Request.cmd(Command.PUBLISH).arg("channel1").arg("Hello World!"))
  .onSuccess(res -> {
    // published!
  });
```

注意: `SUBSCRIBE`, `UNSUBSCRIBE`, `PSUBSCRIBE`, `PUNSUBSCRIBE` 这些命令返回值是 `void`。 这意味着成功的结果是 `null`，而不是响应的实例。所有消息都通过客户端上的 handler 进行路由。

## 域套接字

大部分例子展示连接到 TCP 套接字，但也可以用 Redis 连接到 UNIX 域套接字。

```java
Redis.createClient(vertx, "unix:///tmp/redis.sock")
  .connect()
  .onSuccess(conn -> {
    // so something...
  });
```

请注意，高可用模式和集群模式报告的服务器地址始终位于 TCP 地址上，而不是域套接字上。 这是 Redis 的原因而不是客户端的原因，因此混合使用是不行的。

## 连接池

所有的客户端都有一个连接池。默认配置连接池大小为 1，这意味着操作和单个连接一样。连接池有四个可调项：

- `maxPoolSize` 最大连接数 (默认为 `6`)
- `maxPoolWaiting` 在队列上获取连接的最大等待处理程序数 (默认值为 `24`)
- `poolCleanerInterval` 清除连接的时间间隔 默认为 `-1` (禁用)
- `poolRecycleTimeout` 连接池中打开的连接保持等待到关闭的超时时间 (默认 `15_000`)

连接池非常有用，无需自己管理连接，例如，您只需要：

```java
Redis.createClient(vertx, "redis://localhost:7006")
  .send(Request.cmd(Command.PING))
  .onSuccess(res -> {
    // Should have received a pong...
  });
```

需要注意的是，连接不需要手动获取或者归还，所有连接都由连接池处理。 但是超过 1 个尝试从连接池中获取连接的并发请求可能会出现一些可伸缩性问题。 为了克服这个问题，我们需要对连接池进行调优。 常见的配置是将连接池的最大大小设置为可用CPU核心数，并允许排队从连接池里面获取连接。

```java
Redis.createClient(
  vertx,
  new RedisOptions()
    .setConnectionString("redis://localhost:7006")
    // 允许最多有 8 个连接到 redis
    .setMaxPoolSize(8)
    // 允许 32 个连接请求排队等待连接可用
    .setMaxWaitingHandlers(32))
  .send(Request.cmd(Command.PING))
  .onSuccess(res -> {
    // Should have received a pong...
  });
```

注意：连接池不支持 `SUBSCRIBE`, `UNSUBSCRIBE`, `PSUBSCRIBE`, `PUNSUBSCRIBE` 这些命令。 因为这些命令将修改连接的操作方式，而且连接不能重复使用。

## 出错时重连

虽然连接池非常有用，但为了提高性能，连接不应自动管理，而应该由您控制。 因此您需要处理连接恢复、错误处理和重新连接。

典型的情况是，每当发生错误时，用户都希望重新连接到服务器。 自动重新连接不是 Redis 客户端的一部分，因为它将强制执行可能不符合用户预期的行为，例如：

1. 如何处理当前执行的请求？
2. 是否调用异常处理程序？
3. 如果重试也将失败，该怎么办？
4. 是否应恢复以前的状态（数据库、身份验证、订阅）？
5. 等等等等。

为了给用户充分的灵活性，我们决定不应由客户端执行。 但是，对于超时的简单重新连接可以按如下方式实现：

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
        // 连接到 redis!
      });
  }

  /**
   * 当连接中出现异常时，将创建一个 Redis客 户端并设置重新连接处理程序。
   */
  private Future<RedisConnection> createRedisClient() {
    Promise<RedisConnection> promise = Promise.promise();

    if (CONNECTING.compareAndSet(false, true)) {
      Redis.createClient(vertx, options)
        .connect()
        .onSuccess(conn -> {

          // 关闭旧的连接
          if (client != null) {
            client.close();
          }

          // 确保客户端在报错时重连
          conn.exceptionHandler(e -> {
            // 有无法恢复错误时
            // 尝试重连
            attemptReconnect(0);
          });

          // 进一步处理
          promise.complete(conn);
          CONNECTING.set(false);
        }).onFailure(t -> {
          promise.fail(t);
          CONNECTING.set(false);
        });
    } else {
      promise.complete();
    }

    return promise.future();
  }

  /**
   * 尝试重新连接次数最多到 MAX_RECONNECT_RETRIES 次
   */
  private void attemptReconnect(int retry) {
    if (retry > MAX_RECONNECT_RETRIES) {
      // 现在应该停下来，因为我们无能为力。
      CONNECTING.set(false);
    } else {
      // 最长回退重试 10240 ms
      long backoff = (long) (Math.pow(2, Math.min(retry, 10)) * 10);

      vertx.setTimer(backoff, timer -> {
        createRedisClient()
          .onFailure(t -> attemptReconnect(retry + 1));
      });
    }
  }
}
```

在本例中，客户端对象将在重新连接时被替换，应用程序将重试最多 16 次，回退时间最长可达 1280 ms。 通过弃用旧客户端，我们可以确保所有没有处理的响应都被抛弃。

需要注意，重新连接将创建一个新的连接对象，因此不会每次都缓存和执行这些对象的引用。

## 协议解析器

这个客户端同时支持 `RESP2` 和 `RESP3` 协议，在连接握手阶段， 客户端会自动检测服务器支持的版本，并使用之。

解析器隐式地为从服务器接收到的数据块创建"无限"可读缓冲区， 考虑到内存容量，为了避免产生过多的内存垃圾，在JVM启动的时候，可以配置一个可调优的watermark值。 系统参数 `io.vertx.redis.parser.watermark` 定义了一个缓冲区被废弃之前，可以存储可读数据的数量。 默认情况下，这个大小是512Kb。这意味着每个服务器的连接都会消耗至少512Kb的内存。 客户端以 pipeline 模式运行，他会保持较低的连接数同时提供最佳效果， 这意味着会消耗 `512Kb * n连接数` 大小的内存。 如果应用需要大量连接，那么我们建议将watermark值调小或者直接禁用之。
