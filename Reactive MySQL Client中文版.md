# Reactive MySQL Client中文版

> 翻译: 白石(https://github.com/wjw465150/Vert.x-Core-Manual)

Reactive MySQL Client 是 MySQL 的一个客户端，它有一个简单的 API，专注于可伸缩性和低开销。

**特性**

- 事件驱动
- 轻量级的
- 内置连接池
- 准备好的查询缓存
- 光标支持
- 行流式传输
- RxJava API
- 直接内存到对象，没有不必要的副本
- 完整的数据类型支持
- 存储过程支持
- TLS/SSL 支持
- MySQL 实用程序命令支持
- 支持 MySQL 和 MariaDB
- 丰富的排序规则和字符集支持
- Unix域套接字

## 用法

要使用 Reactive MySQL Client，请将以下依赖项添加到构建描述符的 *dependencies* 部分：

- Maven (在你的 `pom.xml`):

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-mysql-client</artifactId>
 <version>4.2.6</version>
</dependency>
```

- Gradle (在你的 `build.gradle` ):

```groovy
dependencies {
 compile 'io.vertx:vertx-mysql-client:4.2.6'
}
```

## 开始使用

这是连接、查询和断开连接的最简单方法

```java
MySQLConnectOptions connectOptions = new MySQLConnectOptions()
  .setPort(3306)
  .setHost("the-host")
  .setDatabase("the-db")
  .setUser("user")
  .setPassword("secret");

// Pool options
PoolOptions poolOptions = new PoolOptions()
  .setMaxSize(5);

// Create the client pool
MySQLPool client = MySQLPool.pool(connectOptions, poolOptions);

// A simple query
client
  .query("SELECT * FROM users WHERE id='julien'")
  .execute(ar -> {
  if (ar.succeeded()) {
    RowSet<Row> result = ar.result();
    System.out.println("Got " + result.size() + " rows ");
  } else {
    System.out.println("Failure: " + ar.cause().getMessage());
  }

  // Now close the pool
  client.close();
});
```

## 连接到 MySQL

大多数情况下，您将使用池连接到 MySQL：

```java
MySQLConnectOptions connectOptions = new MySQLConnectOptions()
  .setPort(3306)
  .setHost("the-host")
  .setDatabase("the-db")
  .setUser("user")
  .setPassword("secret");

// Pool options
PoolOptions poolOptions = new PoolOptions()
  .setMaxSize(5);

// Create the pooled client
MySQLPool client = MySQLPool.pool(connectOptions, poolOptions);
```

池化客户端使用一个连接池，任何操作都会从池中借用一个连接来执行操作并将其释放到池中。

如果您使用 Vert.x 运行，则可以将您的 Vertx 实例传递给它：

```java
MySQLConnectOptions connectOptions = new MySQLConnectOptions()
  .setPort(3306)
  .setHost("the-host")
  .setDatabase("the-db")
  .setUser("user")
  .setPassword("secret");

// Pool options
PoolOptions poolOptions = new PoolOptions()
  .setMaxSize(5);
// Create the pooled client
MySQLPool client = MySQLPool.pool(vertx, connectOptions, poolOptions);
```

当你不再需要它时，你需要释放它：

```java
pool.close();
```

当你需要在同一个连接上执行多个操作时，你需要使用一个客户端`connection`。

您可以轻松地从池中获得一个：

```java
MySQLConnectOptions connectOptions = new MySQLConnectOptions()
  .setPort(3306)
  .setHost("the-host")
  .setDatabase("the-db")
  .setUser("user")
  .setPassword("secret");

// Pool options
PoolOptions poolOptions = new PoolOptions()
  .setMaxSize(5);

// Create the pooled client
MySQLPool client = MySQLPool.pool(vertx, connectOptions, poolOptions);

// Get a connection from the pool
client.getConnection().compose(conn -> {
  System.out.println("Got a connection from the pool");

  // All operations execute on the same connection
  return conn
    .query("SELECT * FROM users WHERE id='julien'")
    .execute()
    .compose(res -> conn
      .query("SELECT * FROM users WHERE id='emad'")
      .execute())
    .onComplete(ar -> {
      // Release the connection to the pool
      conn.close();
    });
}).onComplete(ar -> {
  if (ar.succeeded()) {

    System.out.println("Done");
  } else {
    System.out.println("Something went wrong " + ar.cause().getMessage());
  }
});
```

完成连接后，您必须关闭它以将其释放到池中，以便可以重用它。

## 池共享

您可以在多个 Verticle 或同一 Verticle的多个实例之间共享一个池。 这样的池应该在 Verticle 之外创建，否则当创建它的 Verticle 被取消部署时它将被关闭

```java
MySQLPool pool = MySQLPool.pool(database, new PoolOptions().setMaxSize(maxSize));
vertx.deployVerticle(() -> new AbstractVerticle() {
  @Override
  public void start() throws Exception {
    // Use the pool
  }
}, new DeploymentOptions().setInstances(4));
```

您还可以在每个verticle中创建一个共享池：

```java
vertx.deployVerticle(() -> new AbstractVerticle() {
  MySQLPool pool;
  @Override
  public void start() {
    // Get or create a shared pool
    // this actually creates a lease to the pool
    // when the verticle is undeployed, the lease will be released automaticaly
    pool = MySQLPool.pool(database, new PoolOptions()
      .setMaxSize(maxSize)
      .setShared(true)
      .setName("my-pool"));
  }
}, new DeploymentOptions().setInstances(4));
```

第一次创建共享池时，它将为该池创建资源。 后续调用将重用此池并为此池创建租约。 资源在所有租约结束后被关闭。

默认情况下，池在需要创建 TCP 连接时会重用当前的事件循环。 因此，共享池将随机使用使用它的 Verticle 的事件循环。

你可以分配多个事件循环，一个池将独立使用它的上下文

```java
MySQLPool pool = MySQLPool.pool(database, new PoolOptions()
  .setMaxSize(maxSize)
  .setShared(true)
  .setName("my-pool")
  .setEventLoopSize(4));
```

### Unix 域套接字

有时出于简单、安全或性能原因，需要通过 [Unix 域套接字](https://dev.mysql.com/doc/refman/8.0/en/server-system-variables.html#sysvar_socket) 进行连接 .

由于 JVM 不支持域套接字，因此您首先必须将本机传输扩展添加到您的项目中。

- Maven (在你的 `pom.xml`):

```xml
<dependency>
 <groupId>io.netty</groupId>
 <artifactId>netty-transport-native-epoll</artifactId>
 <version>${netty.version}</version>
 <classifier>linux-x86_64</classifier>
</dependency>
```

- Gradle 在你的 `build.gradle` ):

```groovy
dependencies {
 compile 'io.netty:netty-transport-native-epoll:${netty.version}:linux-x86_64'
}
```

> **🏷注意:** 对 ARM64 的原生 `epoll` 支持也可以通过分类器 `linux-aarch64` 添加。

> **🏷注意:** 如果您的团队中有 Mac 用户，请添加带有分类器`osx-x86_64`的`netty-transport-native-kqueue`。

然后在 `MySQLConnectOptions#setHost` 中设置域套接字的路径：

```java
MySQLConnectOptions connectOptions = new MySQLConnectOptions()
  .setHost("/var/run/mysqld/mysqld.sock")
  .setDatabase("the-db");

// Pool options
PoolOptions poolOptions = new PoolOptions()
  .setMaxSize(5);

// Create the pooled client
MySQLPool client = MySQLPool.pool(connectOptions, poolOptions);

// Create the pooled client with a vertx instance
// Make sure the vertx instance has enabled native transports
// vertxOptions.setPreferNativeTransport(true);
MySQLPool client2 = MySQLPool.pool(vertx, connectOptions, poolOptions);
```

有关本机传输的更多信息，请参阅 [Vert.x 文档](https://vertx.io/docs/vertx-core/java/#_native_transports)。

## 配置

您可以通过多种方式配置客户端。

### 数据对象

配置客户端的一种简单方法是指定一个 `MySQLConnectOptions` 数据对象。

```java
MySQLConnectOptions connectOptions = new MySQLConnectOptions()
  .setPort(3306)
  .setHost("the-host")
  .setDatabase("the-db")
  .setUser("user")
  .setPassword("secret");

// Pool Options
PoolOptions poolOptions = new PoolOptions().setMaxSize(5);

// Create the pool from the data object
MySQLPool pool = MySQLPool.pool(vertx, connectOptions, poolOptions);

pool.getConnection(ar -> {
  // Handling your connection
});
```

#### 排序规则和字符集

Reactive MySQL 客户端支持配置排序规则或字符集并将它们映射到相关的 `java.nio.charset.Charset`。 例如，您可以为连接指定字符集，例如

```java
MySQLConnectOptions connectOptions = new MySQLConnectOptions();

// set connection character set to utf8 instead of the default charset utf8mb4
connectOptions.setCharset("utf8");
```

反应式 MySQL 客户端将 `utf8mb4` 作为默认字符集。 密码和错误消息等字符串值始终以 `UTF-8` 字符集解码。

`characterEncoding` 选项用于确定将使用哪个 Java charset 对 String 值进行编码，例如查询字符串和参数值，charset 默认为 `UTF-8`，如果设置为 `null`，则客户端将使用默认的 Java 字符集。

您还可以为连接指定排序规则，例如

```java
MySQLConnectOptions connectOptions = new MySQLConnectOptions();

// set connection collation to utf8_general_ci instead of the default collation utf8mb4_general_ci
// setting a collation will override the charset option
connectOptions.setCharset("gbk");
connectOptions.setCollation("utf8_general_ci");
```

注意在数据对象上设置排序规则将覆盖 **charset** 和 **characterEncoding** 选项。

您可以执行 SQL `SHOW COLLATION;` 或 `SHOW CHARACTER SET;` 来获取服务器支持的排序规则和字符集。

有关 MySQL 字符集和排序规则的更多信息可以在 [MySQL 参考手册](https://dev.mysql.com/doc/refman/8.0/en/charset.html) 中找到。

#### 连接属性

您还可以使用 `setProperties` 或 `addProperty` 方法配置连接属性。 注意 `setProperties` 将覆盖默认客户端属性。

```java
MySQLConnectOptions connectOptions = new MySQLConnectOptions();

// Add a connection attribute
connectOptions.addProperty("_java_version", "1.8.0_212");

// Override the attributes
Map<String, String> attributes = new HashMap<>();
attributes.put("_client_name", "myapp");
attributes.put("_client_version", "1.0.0");
connectOptions.setProperties(attributes);
```

关于客户端连接属性的更多信息可以在【MySQL 参考手册】（https://dev.mysql.com/doc/refman/8.0/en/performance-schema-connection-attribute-tables.html）中找到。

#### useAffectedRows

你可以配置`useAffectedRows`选项来决定是否在连接到服务器时设置`CLIENT_FOUND_ROWS`标志。如果指定了`LIENT_FOUND_ROWS`标志，则受影响的行数是找到的行数的数值，而不是受影响的行数。

有关这方面的更多信息，请参阅 [MySQL 参考手册](https://dev.mysql.com/doc/refman/8.0/en/mysql-affected-rows.html)

### 连接 URI

除了使用 `MySQLConnectOptions` 数据对象进行配置外，我们还为您提供了另一种连接方式，以便您使用连接 URI 进行配置：

```java
String connectionUri = "mysql://dbuser:secretpassword@database.server.com:3211/mydb";

// Create the pool from the connection URI
MySQLPool pool = MySQLPool.pool(connectionUri);

// Create the connection from the connection URI
MySQLConnection.connect(vertx, connectionUri, res -> {
  // Handling your connection
});
```

更多关于连接字符串格式的信息可以在【MySQL 参考手册】(https://dev.mysql.com/doc/refman/8.0/en/connecting-using-uri-or-key-value-pairs.html #connecting-using-uri)。

目前，客户端支持在连接uri中使用以下参数关键字（关键字不区分大小写）：

- host
- port
- user
- password
- schema
- socket
- useAffectedRows

## Connect 重试

您可以将客户端配置为在建立连接失败时重试。

```java
options
  .setReconnectAttempts(2)
  .setReconnectInterval(1000);
```

## 运行查询

当您不需要事务或运行单个查询时，您可以直接在池上运行查询； 池将使用其连接之一运行查询并将结果返回给您。

以下是运行简单查询的方法：

```java
client
  .query("SELECT * FROM users WHERE id='julien'")
  .execute(ar -> {
  if (ar.succeeded()) {
    RowSet<Row> result = ar.result();
    System.out.println("Got " + result.size() + " rows ");
  } else {
    System.out.println("Failure: " + ar.cause().getMessage());
  }
});
```

### 准备好的查询

您可以对准备好的查询执行相同的操作。

The SQL string can refer to parameters by position, using the database syntax `?`

```java
client
  .preparedQuery("SELECT * FROM users WHERE id=?")
  .execute(Tuple.of("julien"), ar -> {
  if (ar.succeeded()) {
    RowSet<Row> rows = ar.result();
    System.out.println("Got " + rows.size() + " rows ");
  } else {
    System.out.println("Failure: " + ar.cause().getMessage());
  }
});
```

查询方法提供了适用于 *SELECT* 查询的异步 `RowSet` 实例

```java
client
  .preparedQuery("SELECT first_name, last_name FROM users")
  .execute(ar -> {
  if (ar.succeeded()) {
    RowSet<Row> rows = ar.result();
    for (Row row : rows) {
      System.out.println("User " + row.getString(0) + " " + row.getString(1));
    }
  } else {
    System.out.println("Failure: " + ar.cause().getMessage());
  }
});
```

或者 *UPDATE*/*INSERT* 查询:

```java
client
  .preparedQuery("INSERT INTO users (first_name, last_name) VALUES (?, ?)")
  .execute(Tuple.of("Julien", "Viet"), ar -> {
  if (ar.succeeded()) {
    RowSet<Row> rows = ar.result();
    System.out.println(rows.rowCount());
  } else {
    System.out.println("Failure: " + ar.cause().getMessage());
  }
});
```

`Row` 让您可以按索引访问数据

```java
System.out.println("User " + row.getString(0) + " " + row.getString(1));
```

或者按名称

```java
System.out.println("User " + row.getString("first_name") + " " + row.getString("last_name"));
```

客户端不会在这里做任何魔术，并且无论您的 SQL 文本如何，列名都由表中的名称标识。

您可以访问多种类型

```java
String firstName = row.getString("first_name");
Boolean male = row.getBoolean("male");
Integer age = row.getInteger("age");
```

您可以使用缓存的准备好的语句来执行一次性准备好的查询：

```java
connectOptions.setCachePreparedStatements(true);
client
  .preparedQuery("SELECT * FROM users WHERE id = ?")
  .execute(Tuple.of("julien"), ar -> {
    if (ar.succeeded()) {
      RowSet<Row> rows = ar.result();
      System.out.println("Got " + rows.size() + " rows ");
    } else {
      System.out.println("Failure: " + ar.cause().getMessage());
    }
  });
```

您可以创建一个`PreparedStatement`并自行管理生命周期。

```java
sqlConnection
  .prepare("SELECT * FROM users WHERE id = ?", ar -> {
    if (ar.succeeded()) {
      PreparedStatement preparedStatement = ar.result();
      preparedStatement.query()
        .execute(Tuple.of("julien"), ar2 -> {
          if (ar2.succeeded()) {
            RowSet<Row> rows = ar2.result();
            System.out.println("Got " + rows.size() + " rows ");
            preparedStatement.close();
          } else {
            System.out.println("Failure: " + ar2.cause().getMessage());
          }
        });
    } else {
      System.out.println("Failure: " + ar.cause().getMessage());
    }
  });
```

### 批处理

您可以执行准备好的批处理

```java
List<Tuple> batch = new ArrayList<>();
batch.add(Tuple.of("julien", "Julien Viet"));
batch.add(Tuple.of("emad", "Emad Alblueshi"));

// Execute the prepared batch
client
  .preparedQuery("INSERT INTO USERS (id, name) VALUES (?, ?)")
  .executeBatch(batch, res -> {
  if (res.succeeded()) {

    // Process rows
    RowSet<Row> rows = res.result();
  } else {
    System.out.println("Batch failed " + res.cause());
  }
});
```

## MySQL LAST_INSERT_ID

如果将记录插入表中，则可以获得自动递增的值。

```java
client
  .query("INSERT INTO test(val) VALUES ('v1')")
  .execute(ar -> {
    if (ar.succeeded()) {
      RowSet<Row> rows = ar.result();
      long lastInsertId = rows.property(MySQLClient.LAST_INSERTED_ID);
      System.out.println("Last inserted id is: " + lastInsertId);
    } else {
      System.out.println("Failure: " + ar.cause().getMessage());
    }
  });
```

更多信息可以在[如何获取最后插入行的唯一 ID](https://dev.mysql.com/doc/refman/8.0/en/getting-unique-id.html) 中找到。

## 使用连接

### 建立连接

当您需要执行顺序查询（没有事务）时，您可以创建一个新连接或从池中借用一个。 请记住，在从池中获取连接并将其返回到池之间，您应该注意连接，因为它可能由于某种原因（例如空闲超时）被服务器关闭。

```java
pool
  .getConnection()
  .compose(connection ->
    connection
      .preparedQuery("INSERT INTO Users (first_name,last_name) VALUES (?, ?)")
      .executeBatch(Arrays.asList(
        Tuple.of("Julien", "Viet"),
        Tuple.of("Emad", "Alblueshi")
      ))
      .compose(res -> connection
        // Do something with rows
        .query("SELECT COUNT(*) FROM Users")
        .execute()
        .map(rows -> rows.iterator().next().getInteger(0)))
      // Return the connection to the pool
      .eventually(v -> connection.close())
  ).onSuccess(count -> {
  System.out.println("Insert users, now the number of users is " + count);
});
```

可以创建准备好的查询：

```java
connection
  .prepare("SELECT * FROM users WHERE first_name LIKE ?")
  .compose(pq ->
    pq.query()
      .execute(Tuple.of("Julien"))
      .eventually(v -> pq.close())
  ).onSuccess(rows -> {
  // All rows
});
```

### 简化的连接 API

当你使用一个池时，你可以调用 `withConnection` 来传递一个在连接中执行的函数。

它从池中借用一个连接并用这个连接调用函数。

函数必须返回任意结果的future。

在future完成后，连接被返回到池，并提供总体结果。

```java
pool.withConnection(connection ->
  connection
    .preparedQuery("INSERT INTO Users (first_name,last_name) VALUES (?, ?)")
    .executeBatch(Arrays.asList(
      Tuple.of("Julien", "Viet"),
      Tuple.of("Emad", "Alblueshi")
    ))
    .compose(res -> connection
      // Do something with rows
      .query("SELECT COUNT(*) FROM Users")
      .execute()
      .map(rows -> rows.iterator().next().getInteger(0)))
).onSuccess(count -> {
  System.out.println("Insert users, now the number of users is " + count);
});
```

## 使用事务

### 有连接的事务

您可以使用 SQL `BEGIN`/`COMMIT`/`ROLLBACK` 执行事务，如果这样做，您必须使用 `SqlConnection` 并自己管理它。

或者你可以使用`SqlConnection`的事务API：

```java
pool.getConnection()
  // Transaction must use a connection
  .onSuccess(conn -> {
    // Begin the transaction
    conn.begin()
      .compose(tx -> conn
        // Various statements
        .query("INSERT INTO Users (first_name,last_name) VALUES ('Julien','Viet')")
        .execute()
        .compose(res2 -> conn
          .query("INSERT INTO Users (first_name,last_name) VALUES ('Emad','Alblueshi')")
          .execute())
        // Commit the transaction
        .compose(res3 -> tx.commit()))
      // Return the connection to the pool
      .eventually(v -> conn.close())
      .onSuccess(v -> System.out.println("Transaction succeeded"))
      .onFailure(err -> System.out.println("Transaction failed: " + err.getMessage()));
  });
```

当数据库服务器报告当前事务失败时（例如，臭名昭著的*当前事务被中止，命令在事务块结束前被忽略*），事务被回滚，`completion` future 因`TransactionRollbackException`而失败：

```java
tx.completion()
  .onFailure(err -> {
    System.out.println("Transaction failed => rolled back");
  });
```

### 简化的事务 API

当你使用一个池时，你可以调用`withTransaction`向它传递一个在事务中执行的函数。

它从池中借用一个连接，开始事务并调用函数，客户端执行此事务范围内的所有操作。

该函数必须返回任意结果的future：

- 当future成功时，客户端将提交事务
- 当future失败时，客户端将回滚事务

事务完成后，将连接返回到池中并提供整体结果。

```java
pool.withTransaction(client -> client
  .query("INSERT INTO Users (first_name,last_name) VALUES ('Julien','Viet')")
  .execute()
  .flatMap(res -> client
    .query("INSERT INTO Users (first_name,last_name) VALUES ('Emad','Alblueshi')")
    .execute()
    // Map to a message result
    .map("Users inserted")))
  .onSuccess(v -> System.out.println("Transaction succeeded"))
  .onFailure(err -> System.out.println("Transaction failed: " + err.getMessage()));
```

## 游标 和 流式传输

默认情况下，准备好的查询执行会获取所有行，您可以使用“游标”来控制要读取的行数：

```java
connection.prepare("SELECT * FROM users WHERE age > ?", ar1 -> {
  if (ar1.succeeded()) {
    PreparedStatement pq = ar1.result();

    // Create a cursor
    Cursor cursor = pq.cursor(Tuple.of(18));

    // Read 50 rows
    cursor.read(50, ar2 -> {
      if (ar2.succeeded()) {
        RowSet<Row> rows = ar2.result();

        // Check for more ?
        if (cursor.hasMore()) {
          // Repeat the process...
        } else {
          // No more rows - close the cursor
          cursor.close();
        }
      }
    });
  }
});
```

提前释放游标时应关闭游标:

```java
cursor.read(50, ar2 -> {
  if (ar2.succeeded()) {
    // Close the cursor
    cursor.close();
  }
});
```

流 API 也可用于游标，这可以更方便，特别是 Rxified 版本。

```java
connection.prepare("SELECT * FROM users WHERE age > ?", ar1 -> {
  if (ar1.succeeded()) {
    PreparedStatement pq = ar1.result();

    // Fetch 50 rows at a time
    RowStream<Row> stream = pq.createStream(50, Tuple.of(18));

    // Use the stream
    stream.exceptionHandler(err -> {
      System.out.println("Error: " + err.getMessage());
    });
    stream.endHandler(v -> {
      System.out.println("End of stream");
    });
    stream.handler(row -> {
      System.out.println("User: " + row.getString("last_name"));
    });
  }
});
```

流逐批读取“50”行并将其流化，当行被传递给处理程序时，将读取一批新的“50”行，以此类推。

流可以被恢复或暂停，加载的行将保持在内存中，直到它们被交付，游标将停止迭代。

## 跟踪查询

当 Vert.x 启用跟踪时，SQL 客户端可以跟踪查询执行。

客户端报告以下 *client* 跨度：

- `Query` 操作名称
- 标签
- `db.user`：数据库用户名
- `db.instance`：数据库实例
- `db.statement`：SQL 查询
- `db.type`: *sql*

默认跟踪策略是 `PROPAGATE(传播)`，客户端仅在参与活动跟踪时才会创建跨度。

您可以使用 `setTracingPolicy` 更改客户端策略，例如，您可以将 `ALWAYS` 设置为始终报告跨度：

```java
options.setTracingPolicy(TracingPolicy.ALWAYS);
```

## MySQL 类型映射

目前客户端支持以下 MySQL 类型

- BOOL,BOOLEAN (`java.lang.Byte`)
- TINYINT (`java.lang.Byte`)
- TINYINT UNSIGNED(`java.lang.Short`)
- SMALLINT (`java.lang.Short`)
- SMALLINT UNSIGNED(`java.lang.Integer`)
- MEDIUMINT (`java.lang.Integer`)
- MEDIUMINT UNSIGNED(`java.lang.Integer`)
- INT,INTEGER (`java.lang.Integer`)
- INTEGER UNSIGNED(`java.lang.Long`)
- BIGINT (`java.lang.Long`)
- BIGINT UNSIGNED(`io.vertx.sqlclient.data.Numeric`)
- FLOAT (`java.lang.Float`)
- FLOAT UNSIGNED(`java.lang.Float`)
- DOUBLE (`java.lang.Double`)
- DOUBLE UNSIGNED(`java.lang.Double`)
- BIT (`java.lang.Long`)
- NUMERIC (`io.vertx.sqlclient.data.Numeric`)
- NUMERIC UNSIGNED(`io.vertx.sqlclient.data.Numeric`)
- DATE (`java.time.LocalDate`)
- DATETIME (`java.time.LocalDateTime`)
- TIME (`java.time.Duration`)
- TIMESTAMP (`java.time.LocalDateTime`)
- YEAR (`java.lang.Short`)
- CHAR (`java.lang.String`)
- VARCHAR (`java.lang.String`)
- BINARY (`io.vertx.core.buffer.Buffer`)
- VARBINARY (`io.vertx.core.buffer.Buffer`)
- TINYBLOB (`io.vertx.core.buffer.Buffer`)
- TINYTEXT (`java.lang.String`)
- BLOB (`io.vertx.core.buffer.Buffer`)
- TEXT (`java.lang.String`)
- MEDIUMBLOB (`io.vertx.core.buffer.Buffer`)
- MEDIUMTEXT (`java.lang.String`)
- LONGBLOB (`io.vertx.core.buffer.Buffer`)
- LONGTEXT (`java.lang.String`)
- ENUM (`java.lang.String`)
- SET (`java.lang.String`)
- JSON (`io.vertx.core.json.JsonObject`, `io.vertx.core.json.JsonArray`, `Number`, `Boolean`, `String`, `io.vertx.sqlclient.Tuple#JSON_NULL`)
- GEOMETRY(`io.vertx.mysqlclient.data.spatial.*`)

元组解码在存储值时使用上述类型

> **🏷注意:** 在 Java 中，无符号数值没有特定的表示，因此该客户端会将无符号值转换为相关的 Java 类型。

### 隐式类型转换

Reactive MySQL Client 在执行准备好的语句时支持隐式类型转换。 假设您的表中有一个`TIME`列，下面的两个示例都可以在这里使用。

```java
client
  .preparedQuery("SELECT * FROM students WHERE updated_time = ?")
  .execute(Tuple.of(LocalTime.of(19, 10, 25)), ar -> {
  // handle the results
});
// this will also work with implicit type conversion
client
  .preparedQuery("SELECT * FROM students WHERE updated_time = ?")
  .execute(Tuple.of("19:10:25"), ar -> {
  // handle the results
});
```

用于编码的 MySQL 数据类型将从参数值中推断出来，这里是类型映射

|              参数值类型               |   编码 MySQL 类型    |
| ----------------------------------- | ------------------- |
| null                                | MYSQL_TYPE_NULL     |
| java.lang.Byte                      | MYSQL_TYPE_TINY     |
| java.lang.Boolean                   | MYSQL_TYPE_TINY     |
| java.lang.Short                     | MYSQL_TYPE_SHORT    |
| java.lang.Integer                   | MYSQL_TYPE_LONG     |
| java.lang.Long                      | MYSQL_TYPE_LONGLONG |
| java.lang.Double                    | MYSQL_TYPE_DOUBLE   |
| java.lang.Float                     | MYSQL_TYPE_FLOAT    |
| java.time.LocalDate                 | MYSQL_TYPE_DATE     |
| java.time.Duration                  | MYSQL_TYPE_TIME     |
| java.time.LocalTime                 | MYSQL_TYPE_TIME     |
| io.vertx.core.buffer.Buffer         | MYSQL_TYPE_BLOB     |
| java.time.LocalDateTime             | MYSQL_TYPE_DATETIME |
| io.vertx.mysqlclient.data.spatial.* | MYSQL_TYPE_BLOB     |
| default                             | MYSQL_TYPE_STRING   |

### 处理 布尔值

在 MySQL 中，`BOOLEAN` 和 `BOOL` 数据类型是 `TINYINT(1)` 的同义词。 零值被认为是假的，非零值被认为是真。 `BOOLEAN` 数据类型值以`java.lang.Byte` 类型存储在`Row` 或`Tuple` 中，您可以调用`Row#getValue` 将其检索为`java.lang.Byte` 值，或者 您可以调用 `Row#getBoolean` 将其检索为 `java.lang.Boolean` 值。

```java
client
  .query("SELECT graduated FROM students WHERE id = 0")
  .execute(ar -> {
  if (ar.succeeded()) {
    RowSet<Row> rowSet = ar.result();
    for (Row row : rowSet) {
      int pos = row.getColumnIndex("graduated");
      Byte value = row.get(Byte.class, pos);
      Boolean graduated = row.getBoolean("graduated");
    }
  } else {
    System.out.println("Failure: " + ar.cause().getMessage());
  }
});
```

当您想要执行带有 `BOOLEAN` 值的参数的准备好的语句时，只需将 `java.lang.Boolean` 值添加到参数列表中即可。

```java
client
  .preparedQuery("UPDATE students SET graduated = ? WHERE id = 0")
  .execute(Tuple.of(true), ar -> {
  if (ar.succeeded()) {
    System.out.println("Updated with the boolean value");
  } else {
    System.out.println("Failure: " + ar.cause().getMessage());
  }
});
```

### 处理 JSON

MySQL `JSON` 数据类型由以下 Java 类型表示：

- `String`
- `Number`
- `Boolean`
- `io.vertx.core.json.JsonObject`
- `io.vertx.core.json.JsonArray`
- `io.vertx.sqlclient.Tuple#JSON_NULL` 用于表示 JSON 空字面量

```java
Tuple tuple = Tuple.of(
  Tuple.JSON_NULL,
  new JsonObject().put("foo", "bar"),
  3);

// Retrieving json
Object value = tuple.getValue(0); // Expect JSON_NULL

//
value = tuple.get(JsonObject.class, 1); // Expect JSON object

//
value = tuple.get(Integer.class, 2); // Expect 3
value = tuple.getInteger(2); // Expect 3
```

### 处理 BIT

`BIT` 数据类型映射到 `java.lang.Long` 类型，但是 Java 没有无符号数值的概念，所以如果要插入或更新最大值为 `BIT(64)` 的记录， 你可以做一些技巧将参数设置为`-1L`。

### 处理 TIME

MySQL `TIME` 数据类型可用于表示一天中的时间或时间间隔，范围从 `-838:59:59` 到 `838:59:59`。 在 Reactive MySQL 客户端中，`TIME` 数据类型本机映射到`java.time.Duration`，但您也可以通过`Row#getLocalTime` 访问器将其作为`java.time.LocalTime` 检索。

### 处理 NUMERIC

`Numeric` Java 类型用于表示 MySQL `NUMERIC` 类型。

```java
Numeric numeric = row.get(Numeric.class, 0);
if (numeric.isNaN()) {
  // Handle NaN
} else {
  BigDecimal value = numeric.bigDecimalValue();
}
```

### 处理 ENUM

MySQL 支持 ENUM 数据类型，客户端将这些类型检索为 String 数据类型。

您可以像这样将 Java 枚举编码为 String：

```java
client
  .preparedQuery("INSERT INTO colors VALUES (?)")
  .execute(Tuple.of(Color.red),  res -> {
    // ...
  });
```

您可以像这样检索 ENUM 列作为 Java 枚举：

```java
client
  .preparedQuery("SELECT color FROM colors")
  .execute()
  .onComplete(res -> {
  if (res.succeeded()) {
    RowSet<Row> rows = res.result();
    for (Row row : rows) {
      System.out.println(row.get(Color.class, "color"));
    }
  }
});
```

### 处理 GEOMETRY

MYSQL 的 `GEOMETRY` 数据类型也被支持，这里有一些例子展示了你可以处理的几何数据在众所周知的文本(WKT)格式或众所周知的二进制(WKB)格式，数据被解码为MYSQL文本或BLOB数据类型。有许多很棒的第三方库可以处理这种格式的数据。

你可以获取WKT格式的空间数据:

```java
client
  .query("SELECT ST_AsText(g) FROM geom;")
  .execute(ar -> {
  if (ar.succeeded()) {
    // Fetch the spatial data in WKT format
    RowSet<Row> result = ar.result();
    for (Row row : result) {
      String wktString = row.getString(0);
    }
  } else {
    System.out.println("Failure: " + ar.cause().getMessage());
  }
});
```

或者你可以获取WKB格式的空间数据:

```java
client
  .query("SELECT ST_AsBinary(g) FROM geom;")
  .execute(ar -> {
  if (ar.succeeded()) {
    // Fetch the spatial data in WKB format
    RowSet<Row> result = ar.result();
    for (Row row : result) {
      Buffer wkbValue = row.getBuffer(0);
    }
  } else {
    System.out.println("Failure: " + ar.cause().getMessage());
  }
});
```

我们还为您提供了一种在 Reactive MySQL Client 中处理几何数据类型的简单方法。

您可以将几何数据检索为 Vert.x 数据对象：

```java
client
  .query("SELECT g FROM geom;")
  .execute(ar -> {
  if (ar.succeeded()) {
    // Fetch the spatial data as a Vert.x Data Object
    RowSet<Row> result = ar.result();
    for (Row row : result) {
      Point point = row.get(Point.class, 0);
      System.out.println("Point x: " + point.getX());
      System.out.println("Point y: " + point.getY());
    }
  } else {
    System.out.println("Failure: " + ar.cause().getMessage());
  }
});
```

您还可以将其作为 WKB 表示中的准备好的语句参数。

```java
Point point = new Point(0, 1.5, 1.5);
// Send as a WKB representation
client
  .preparedQuery("INSERT INTO geom VALUES (ST_GeomFromWKB(?))")
  .execute(Tuple.of(point), ar -> {
  if (ar.succeeded()) {
    System.out.println("Success");
  } else {
    System.out.println("Failure: " + ar.cause().getMessage());
  }
});
```

## Collector 查询

您可以将 Java Collector与查询 API 一起使用：

```java
Collector<Row, ?, Map<Long, String>> collector = Collectors.toMap(
  row -> row.getLong("id"),
  row -> row.getString("last_name"));

// Run the query with the collector
client.query("SELECT * FROM users").collecting(collector).execute(ar -> {
    if (ar.succeeded()) {
      SqlResult<Map<Long, String>> result = ar.result();

      // Get the map created by the collector
      Map<Long, String> map = result.value();
      System.out.println("Got " + map);
    } else {
      System.out.println("Failure: " + ar.cause().getMessage());
    }
  });
```

collector处理不能保留对`Row` 的引用，因为有一个行用于处理整个集合。

Java `Collectors` 提供了许多有趣的预定义收集器，例如，您可以轻松地从行集中直接创建一个字符串：

```java
Collector<Row, ?, String> collector = Collectors.mapping(
  row -> row.getString("last_name"),
  Collectors.joining(",", "(", ")")
);

// Run the query with the collector
client.query("SELECT * FROM users").collecting(collector).execute(ar -> {
    if (ar.succeeded()) {
      SqlResult<String> result = ar.result();

      // Get the string created by the collector
      String list = result.value();
      System.out.println("Got " + list);
    } else {
      System.out.println("Failure: " + ar.cause().getMessage());
    }
  });
```

## MySQL 存储过程

您可以在查询中运行存储过程。 结果将按照 [MySQL 协议](https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_command_phase_sp.html) 从服务器检索，这里没有任何魔法。

```java
client.query("CREATE PROCEDURE multi() BEGIN\n" +
  "  SELECT 1;\n" +
  "  SELECT 1;\n" +
  "  INSERT INTO ins VALUES (1);\n" +
  "  INSERT INTO ins VALUES (2);\n" +
  "END;").execute(ar1 -> {
  if (ar1.succeeded()) {
    // create stored procedure success
    client
      .query("CALL multi();")
      .execute(ar2 -> {
      if (ar2.succeeded()) {
        // handle the result
        RowSet<Row> result1 = ar2.result();
        Row row1 = result1.iterator().next();
        System.out.println("First result: " + row1.getInteger(0));

        RowSet<Row> result2 = result1.next();
        Row row2 = result2.iterator().next();
        System.out.println("Second result: " + row2.getInteger(0));

        RowSet<Row> result3 = result2.next();
        System.out.println("Affected rows: " + result3.rowCount());
      } else {
        System.out.println("Failure: " + ar2.cause().getMessage());
      }
    });
  } else {
    System.out.println("Failure: " + ar1.cause().getMessage());
  }
});
```

> **🏷注意:** 目前不支持绑定 OUT 参数的预处理语句。

## MySQL 本地INFILE

此客户端支持处理 LOCAL INFILE 请求，如果您想从本地文件加载数据到服务器，您可以使用查询 `LOAD DATA LOCAL INFILE '<filename>' INTO TABLE <table>;`。 更多信息可以在 [MySQL 参考手册](https://dev.mysql.com/doc/refman/8.0/en/load-data.html) 中找到。

## 认证

### 默认身份验证插件

此客户端支持指定在连接开始时使用的默认身份验证插件。 目前支持以下插件：

- mysql_native_password
- caching_sha2_password
- mysql_clear_password

```java
MySQLConnectOptions options = new MySQLConnectOptions()
  .setPort(3306)
  .setHost("the-host")
  .setDatabase("the-db")
  .setUser("user")
  .setPassword("secret")
  .setAuthenticationPlugin(MySQLAuthenticationPlugin.MYSQL_NATIVE_PASSWORD); // set the default authentication plugin
```

### MySQL 8 中引入的新身份验证方法

MySQL 8.0 引入了一种名为`caching_sha2_password`的新身份验证方法，它是默认的身份验证方法。 为了使用这种新的身份验证方法连接到服务器，您需要使用安全连接（即启用 TLS/SSL）或使用 RSA 密钥对交换加密密码以避免密码泄漏。 RSA 密钥对在通信过程中自动交换，但服务器 RSA 公钥可能在此过程中被黑客入侵，因为它是在不安全的连接上传输的。 因此，如果您处于不安全的连接上并希望避免暴露服务器 RSA 公钥的风险，您可以像这样设置服务器 RSA 公钥：

```java
MySQLConnectOptions options1 = new MySQLConnectOptions()
  .setPort(3306)
  .setHost("the-host")
  .setDatabase("the-db")
  .setUser("user")
  .setPassword("secret")
  .setServerRsaPublicKeyPath("tls/files/public_key.pem"); // configure with path of the public key

MySQLConnectOptions options2 = new MySQLConnectOptions()
  .setPort(3306)
  .setHost("the-host")
  .setDatabase("the-db")
  .setUser("user")
  .setPassword("secret")
  .setServerRsaPublicKeyValue(Buffer.buffer("-----BEGIN PUBLIC KEY-----\n" +
    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA3yvG5s0qrV7jxVlp0sMj\n" +
    "xP0a6BuLKCMjb0o88hDsJ3xz7PpHNKazuEAfPxiRFVAV3edqfSiXoQw+lJf4haEG\n" +
    "HQe12Nfhs+UhcAeTKXRlZP/JNmI+BGoBduQ1rCId9bKYbXn4pvyS/a1ft7SwFkhx\n" +
    "aogCur7iIB0WUWvwkQ0fEj/Mlhw93lLVyx7hcGFq4FOAKFYr3A0xrHP1IdgnD8QZ\n" +
    "0fUbgGLWWLOossKrbUP5HWko1ghLPIbfmU6o890oj1ZWQewj1Rs9Er92/UDj/JXx\n" +
    "7ha1P+ZOgPBlV037KDQMS6cUh9vTablEHsMLhDZanymXzzjBkL+wH/b9cdL16LkQ\n" +
    "5QIDAQAB\n" +
    "-----END PUBLIC KEY-----\n")); // configure with buffer of the public key
```

关于 `caching_sha2_password` 认证方法的更多信息可以在 [MySQL 参考手册](https://dev.mysql.com/doc/refman/8.0/en/caching-sha2-pluggable-authentication.html) 中找到。

## 使用 SSL/TLS

要将客户端配置为使用 SSL 连接，您可以像 Vert.x `NetClient` 一样配置`MySQLConnectOptions`。 支持所有 [SSL 模式](https://dev.mysql.com/doc/refman/8.0/en/connection-options.html#option_general_ssl-mode)，您可以配置 `sslmode`。 默认情况下，客户端处于 `DISABLED` SSL 模式。 `ssl` 参数仅作为设置 `sslmode` 的快捷方式。 `setSsl(true)` 等价于 `setSslMode(VERIFY_CA)` 和 `setSsl(false)` 等价于 `setSslMode(DISABLED)`。

```java
MySQLConnectOptions options = new MySQLConnectOptions()
  .setPort(3306)
  .setHost("the-host")
  .setDatabase("the-db")
  .setUser("user")
  .setPassword("secret")
  .setSslMode(SslMode.VERIFY_CA)
  .setPemTrustOptions(new PemTrustOptions().addCertPath("/path/to/cert.pem"));

MySQLConnection.connect(vertx, options, res -> {
  if (res.succeeded()) {
    // Connected with SSL
  } else {
    System.out.println("Could not connect " + res.cause());
  }
});
```

更多信息可以在 [Vert.x 文档](https://vertx.io/docs/vertx-core/java/#ssl) 中找到。

## MySQL 实用程序命令

有时您想使用 MySQL 实用程序命令，我们为此提供支持。 更多信息可以在 [MySQL 实用程序命令](https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_command_phase_utility.html) 中找到。

### COM_PING

您可以使用 `COM_PING` 命令检查服务器是否处于活动状态。 如果服务器响应 PING，将通知处理程序，否则将永远不会调用处理程序。

```java
connection.ping(ar -> {
  System.out.println("The server has responded to the PING");
});
```

### COM_RESET_CONNECTION

您可以使用`COM_RESET_CONNECTION`命令重置会话状态，这将重置连接状态，例如： - 用户变量 - 临时表 - 准备好的语句

```java
connection.resetConnection(ar -> {
  if (ar.succeeded()) {
    System.out.println("Connection has been reset now");
  } else {
    System.out.println("Failure: " + ar.cause().getMessage());
  }
});
```

### COM_CHANGE_USER

您可以更改当前连接的用户，这将执行重新身份验证并重置连接状态，如`COM_RESET_CONNECTION`。

```java
MySQLAuthOptions authenticationOptions = new MySQLAuthOptions()
  .setUser("newuser")
  .setPassword("newpassword")
  .setDatabase("newdatabase");
connection.changeUser(authenticationOptions, ar -> {
  if (ar.succeeded()) {
    System.out.println("User of current connection has been changed.");
  } else {
    System.out.println("Failure: " + ar.cause().getMessage());
  }
});
```

### COM_INIT_DB

您可以使用 `COM_INIT_DB` 命令更改连接的默认模式。

```java
connection.specifySchema("newschema", ar -> {
  if (ar.succeeded()) {
    System.out.println("Default schema changed to newschema");
  } else {
    System.out.println("Failure: " + ar.cause().getMessage());
  }
});
```

### COM_STATISTICS

您可以使用`COM_STATISTICS`命令获取 MySQL 服务器中一些内部状态变量的人类可读字符串。

```java
connection.getInternalStatistics(ar -> {
  if (ar.succeeded()) {
    System.out.println("Statistics: " + ar.result());
  } else {
    System.out.println("Failure: " + ar.cause().getMessage());
  }
});
```

### COM_DEBUG

您可以使用`COM_DEBUG`命令将调试信息转储到 MySQL 服务器的 STDOUT。

```java
connection.debug(ar -> {
  if (ar.succeeded()) {
    System.out.println("Debug info dumped to server's STDOUT");
  } else {
    System.out.println("Failure: " + ar.cause().getMessage());
  }
});
```

### COM_SET_OPTION

您可以使用 `COM_SET_OPTION` 命令设置当前连接的选项。 目前只能设置`CLIENT_MULTI_STATEMENTS`。

For example, you can disable `CLIENT_MULTI_STATEMENTS` with this command.

```java
connection.setOption(MySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_OFF, ar -> {
  if (ar.succeeded()) {
    System.out.println("CLIENT_MULTI_STATEMENTS is off now");
  } else {
    System.out.println("Failure: " + ar.cause().getMessage());
  }
});
```

## MySQL 和 MariaDB 版本支持矩阵

| MySQL |        | MariaDB |        |
| ----- | ------ | ------- | ------ |
| 版本   | 支持的 | 版本    | 支持的 |
| `5.5` | ✔     | `10.1`  | ✔     |
| `5.6` | ✔     | `10.2`  | ✔     |
| `5.7` | ✔     | `10.3`  | ✔     |
| `8.0` | ✔     | `10.4`  | ✔     |

已知的问题：

- 重置连接实用程序命令在 MySQL 5.5、5.6 和 MariaDB 10.1 中不起作用
- MariaDB 10.2 和 10.3 不支持更改用户实用程序命令

## 陷阱 & 良好实践

在使用 Reactive MySQL Client 时，这里有一些好的做法可以帮助您避免常见的陷阱。

### 准备好的语句计数限制

有时你可能会遇到臭名昭著的错误`Can't create more than max_prepared_stmt_count statements (current value: 16382)`，这是因为服务器已经达到了prepared statement的总数限制。

您可以调整服务器系统变量`max_prepared_stmt_count`，但它有一个上限，因此您无法通过这种方式消除错误。

缓解这种情况的最佳方法是启用预准备语句缓存，因此可以重复使用具有相同 SQL 字符串的预准备语句，并且客户端不必为每个请求创建全新的预准备语句。 准备好的语句将在语句执行后自动关闭。 这样虽然不能完全消除，但达到极限的几率会大大降低。

您也可以通过 `SqlConnection#prepare` 接口创建一个 `PreparedStatement` 对象来手动管理prepared statements的生命周期，这样您就可以选择何时释放语句句柄，甚至可以使用[SQL syntax prepared statement](https://dev.mysql.com/doc/refman/8.0/en/sql-prepared-statements.html).

### 揭开准备好的批次的神秘面纱

有时你想批量插入数据到数据库中，你可以使用`PreparedQuery#executeBatch`，它提供了一个简单的API来处理这个。 请记住，MySQL 本身并不支持批处理协议，因此 API 只是一个通过一个接一个地执行准备好的语句的糖，这意味着与通过执行一个包含值列表的准备好的语句插入多行相比，需要更多的网络往返 .

### 棘手的日期和时间数据类型

处理 MYSQL DATE 和 TIME 数据类型，尤其是使用时区是很棘手的，因此 Reactive MySQL Client 不会对这些值进行魔法转换。

- MySQL DATETIME 数据类型不包含时区信息，因此无论当前会话中的时区是什么，您得到的都与您设置的相同。
- MySQL TIMESTAMP 数据类型包含时区信息，因此当您设置或获取值时，它总是由服务器转换为当前会话中设置的时区。

## 高级池配置

### 服务器负载均衡

您可以使用服务器列表而不是单个服务器来配置池。

```java
MySQLPool pool = MySQLPool.pool(Arrays.asList(server1, server2, server3), options);
```

当创建连接以选择不同的服务器时，池使用循环负载平衡。

> **🏷注意:** 这在创建连接时提供负载平衡，而不是在从池中借用连接时提供负载平衡。

### 池连接初始化

您可以在连接创建之后和将其插入池之前使用 `connectHandler` 与它进行交互。

```java
pool.connectHandler(conn -> {
  conn.query(sql).execute().onSuccess(res -> {
    // Release the connection to the pool, ready to be used by the application
    conn.close();
  });
});
```

完成连接后，您应该简单地关闭它以向池发出使用它的信号。