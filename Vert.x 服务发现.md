# Vert.x 服务发现

> 翻译: 白石(https://github.com/wjw465150/Vert.x-Core-Manual)

该组件提供了发布和发现各种资源的基础设施，例如服务代理、HTTP 端点、数据源……这些资源称为`services`。 `service` 是一个可发现的功能。 它可以通过其类型、元数据和位置来限定。 因此，`service`可以是数据库、服务代理、HTTP 端点和任何其他您可以想象的资源，只要您可以描述、发现并与之交互。 它不必是 vert.x 实体，但可以是任何东西。 每个服务都由一个`Record` 来描述。

服务发现实现了面向服务计算中定义的交互。 并且在某种程度上，还提供了动态的面向服务的计算交互。 因此，应用程序可以对服务的到达和离开做出反应。

服务提供商可以：

- 发布服务记录
- 取消发布已发布的记录
- 更新已发布服务的状态（关闭、停止服务……）

服务消费者可以：

- 查找服务
- 绑定到选定的服务（它得到一个`ServiceReference`）并使用它
- 一旦用户使用完服务，就释放它
- 收听服务的到达、离开和修改。

消费者将
1. 查找与他们需要匹配的服务记录，
2. 检索提供访问服务的`ServiceReference`，
3. 获得访问服务的服务对象，
4. 一旦完成释放服务对象。

这个过程可以通过使用“服务类型”来简化，如果你知道它是哪种类型，你可以直接检索服务对象(JDBC客户端，Http客户端……)。

如上所述，提供者和消费者共享的核心信息是`records`。

提供者和使用者必须创建他们自己的`ServiceDiscovery`实例。这些实例在后台协作(分布式结构)，以保持服务集同步。

服务发现支持从其他发现技术导入和导出服务的桥梁。

## 使用服务发现

要使用 Vert.x 服务发现，请将以下依赖项添加到构建描述符的 *dependencies* 部分：

- Maven (在你的 `pom.xml`):

```xml
<dependency>
<groupId>io.vertx</groupId>
<artifactId>vertx-service-discovery</artifactId>
<version>4.3.0</version>
</dependency>
```

- Gradle (在你的 `build.gradle`):

```groovy
compile 'io.vertx:vertx-service-discovery:4.3.0'
```

## 整体概念

发现机制基于本节解释的几个概念。

### 服务记录

服务 `Record` 是一个描述由服务提供者发布的服务的对象。它包含一个名称、一些元数据、一个位置对象(描述服务在哪里)。这条记录是提供者(已经发布了它)和使用者(在执行查找时检索它)共享的唯一对象。

元数据甚至位置格式取决于 [服务类型](#Service_Type)。

记录在提供者准备使用时发布，在服务提供者停止时撤销。

### 服务提供者和发布者

服务提供者是提供*服务*的实体。发布者负责发布描述提供者的记录。它可以是单个实体(提供者发布自身)，也可以是不同的实体。

### 服务消费者

服务消费者在服务发现中搜索服务。 每次查找都会检索 `0..n` 个 `Record`。 从这些记录中，消费者可以检索 `ServiceReference` ，代表消费者和提供者之间的绑定。 此引用允许消费者检索*服务对象*（以使用服务）并释放服务。

释放服务引用以清理对象和更新服务使用是很重要的。

### 服务对象

服务对象是提供对服务的访问权限的对象。 它可以以各种形式出现，例如代理、客户端，甚至对于某些服务类型可能不存在。 服务对象的性质取决于服务类型。

请注意，由于 Vert.x 的多语言特性，如果您从 Java、Groovy 或其他语言检索服务对象，它可能会有所不同。

### 服务类型

服务只是资源，有很多不同种类的服务。 它们可以是功能服务、数据库、REST API 等。 Vert.x 服务发现具有服务类型的概念来处理这种异构性。 每种类型定义：

- 服务的位置（URI、事件总线地址、IP / DNS...） - *location*
- 服务对象的性质（服务代理、HTTP 客户端、消息消费者……） - *client*

某些服务类型由服务发现组件实现和提供，但您可以添加自己的。

### 服务事件

每次发布或撤回服务提供者时，都会在事件总线上触发一个事件。 此事件包含已修改的记录。

此外，为了跟踪谁在使用谁，每次使用 `getReference` 检索引用或使用 `release` 释放引用时，都会在事件总线上发出事件以跟踪服务使用情况。

以下是有关这些事件的更多详细信息。

### 后端

服务发现使用 Vert.x 分布式数据结构来存储记录。 因此，集群的所有成员都可以访问所有记录。 这是默认的后端实现。 您可以通过实现 `ServiceDiscoveryBackend` SPI 来实现自己的。 例如，我们提供了一个基于 Redis 的实现。

请注意，发现不需要 Vert.x 集群。 在单节点模式下，结构是本地的。 它可以用 `ServiceImporter` 填充。 从 3.5.0 开始，即使在集群模式下，您也可以通过将系统属性 `vertx-service-discovery-backend-local` 设置为 `true`（或环境变量 `VERTX-SERVICE-DISCOVERY-BACKEND-LOCAL ` 到 `true`）。

## 创建服务发现实例

发布者和消费者必须创建自己的`ServiceDiscovery`实例才能使用发现基础设施：

```java
ServiceDiscovery discovery = ServiceDiscovery.create(vertx);

// Customize the configuration
discovery = ServiceDiscovery.create(vertx,
    new ServiceDiscoveryOptions()
        .setAnnounceAddress("service-announce")
        .setName("my-name"));

// Do something...

discovery.close();
```

默认情况下，通知地址(发送服务事件的事件总线地址是：`vertx.discovery.announce`)。您还可以配置用于服务使用的名称（请参阅有关服务使用的部分）。

当您不再需要服务发现对象时，不要忘记关闭它。 它关闭您已配置的不同发现导入器和导出器并释放服务引用。

> **🏷注意:** 您应该避免共享服务发现实例，这样服务使用将代表正确的“用法”。

## 发布服务

一旦你有了一个服务发现实例，你就可以发布服务了。 过程如下：

1. 为特定服务提供商创建记录
2. 发布此记录
3. 保留用于取消发布或修改服务的发布记录。

要创建记录，您可以使用 `Record`类，也可以使用服务类型中的便捷方法。

```java
Record record = new Record()
    .setType("eventbus-service-proxy")
    .setLocation(new JsonObject().put("endpoint", "the-service-address"))
    .setName("my-service")
    .setMetadata(new JsonObject().put("some-label", "some-value"));

discovery.publish(record, ar -> {
  if (ar.succeeded()) {
    // publication succeeded
    Record publishedRecord = ar.result();
  } else {
    // publication failed
  }
});

// Record creation from a type
record = HttpEndpoint.createRecord("some-rest-api", "localhost", 8080, "/api");
discovery.publish(record, ar -> {
  if (ar.succeeded()) {
    // publication succeeded
    Record publishedRecord = ar.result();
  } else {
    // publication failed
  }
});
```

在返回的记录上保留一个引用是很重要的，因为这个记录已经被一个`registration id`扩展了。

## 撤销(服务

撤销(取消发布)记录，使用:

```java
discovery.unpublish(record.getRegistration(), ar -> {
  if (ar.succeeded()) {
    // Ok
  } else {
    // cannot un-publish the service, may have already been removed, or the record is not published
  }
});
```

## 寻找服务

*本节介绍检索服务的低级过程，每种服务类型都提供了方便的方法来聚合不同的步骤。*

在消费者方面，首先要做的是查找记录。 您可以搜索单个记录或所有匹配的记录。 在第一种情况下，返回第一个匹配的记录。

消费者可以通过过滤器来选择服务。 有两种方式来描述过滤器：

1. 一个以 `Record` 作为参数并返回布尔值的函数（它是一个谓词）
2. 此过滤器是一个 JSON 对象。 根据记录检查给定过滤器的每个条目。 所有条目必须与记录完全匹配。 该条目可以使用特殊的 `*` 值来表示对键的要求，而不是对值的要求。

让我们看一个 JSON 过滤器的示例：

```
{ "name" = "a" } => matches records with name set to "a"
{ "color" = "*" } => matches records with "color" set
{ "color" = "red" } => only matches records with "color" set to "red"
{ "color" = "red", "name" = "a"} => only matches records with name set to "a", and color set to "red"
```

如果未设置 JSON 过滤器（`null` 或为空），它将接受所有记录。 使用函数时，要接受所有记录，无论记录如何，都必须返回 *true*。

这里有些例子：

```java
discovery.getRecord(r -> true, ar -> {
  if (ar.succeeded()) {
    if (ar.result() != null) {
      // we have a record
    } else {
      // the lookup succeeded, but no matching service
    }
  } else {
    // lookup failed
  }
});

discovery.getRecord((JsonObject) null, ar -> {
  if (ar.succeeded()) {
    if (ar.result() != null) {
      // we have a record
    } else {
      // the lookup succeeded, but no matching service
    }
  } else {
    // lookup failed
  }
});


// Get a record by name
discovery.getRecord(r -> r.getName().equals("some-name"), ar -> {
  if (ar.succeeded()) {
    if (ar.result() != null) {
      // we have a record
    } else {
      // the lookup succeeded, but no matching service
    }
  } else {
    // lookup failed
  }
});

discovery.getRecord(new JsonObject().put("name", "some-service"), ar -> {
  if (ar.succeeded()) {
    if (ar.result() != null) {
      // we have a record
    } else {
      // the lookup succeeded, but no matching service
    }
  } else {
    // lookup failed
  }
});

// Get all records matching the filter
discovery.getRecords(r -> "some-value".equals(r.getMetadata().getString("some-label")), ar -> {
  if (ar.succeeded()) {
    List<Record> results = ar.result();
    // If the list is not empty, we have matching record
    // Else, the lookup succeeded, but no matching service
  } else {
    // lookup failed
  }
});


discovery.getRecords(new JsonObject().put("some-label", "some-value"), ar -> {
  if (ar.succeeded()) {
    List<Record> results = ar.result();
    // If the list is not empty, we have matching record
    // Else, the lookup succeeded, but no matching service
  } else {
    // lookup failed
  }
});
```

您可以使用 `getRecords` 检索单个记录或所有匹配记录。 默认情况下，记录查找只包括`status`设置为`UP`的记录。 这可以被覆盖：

- 使用 JSON 过滤器时，只需将 `status` 设置为您想要的值（或 `*` 接受所有状态）
- 使用函数时，将 `getRecords` 中的 `includeOutOfService` 参数设置为 `true`。

## 检索服务引用

选择`Record`后，您可以检索`ServiceReference`，然后检索服务对象：

```java
ServiceReference reference1 = discovery.getReference(record1);
ServiceReference reference2 = discovery.getReference(record2);

// Then, gets the service object, the returned type depends on the service type:
// For http endpoint:
HttpClient client = reference1.getAs(HttpClient.class);
// For message source
MessageConsumer consumer = reference2.getAs(MessageConsumer.class);

// When done with the service
reference1.release();
reference2.release();
```

完成后不要忘记释放引用。

服务引用表示与服务提供者的绑定。

检索服务引用时，您可以传递用于配置服务对象的`JsonObject`。 它可以包含有关服务对象的各种数据。 有些服务类型不需要额外配置，有些需要配置（作为数据源）：

```java
ServiceReference reference = discovery.getReferenceWithConfiguration(record, conf);

// Then, gets the service object, the returned type depends on the service type:
// For http endpoint:
JDBCClient client = reference.getAs(JDBCClient.class);

// Do something with the client...

// When done with the service
reference.release();
```

在前面的示例中，代码使用了 `getAs`。 参数是您希望获得的对象的类型。 如果您使用的是 Java，则可以使用 `get`。 但是在另一种语言中，您必须传递预期的类型。

<a name="Service_Type"></a>
## 服务类型

如上所述，服务发现具有服务类型概念来管理不同服务的异构性。

这些类型是默认提供的：

- `HttpEndpoint` - 对于 REST API，服务对象是在主机和端口上配置的 `HttpClient`（位置是 url）。
- `EventBusService` - 对于服务代理，服务对象是一个代理。 它的类型是代理接口（位置是地址）。
- `MessageSource` - 对于消息源（发布者），服务对象是`MessageConsumer`（位置是地址）。
- `JDBCDataSource` - 对于 JDBC 数据源，服务对象是 `JDBCClient`（客户端的配置是根据位置、元数据和消费者配置计算得出的）。
- `RedisDataSource` - 对于 Redis 数据源，服务对象是一个 `Redis`（客户端的配置是根据位置、元数据和消费者配置计算得出的）。
- `MongoDataSource` - 对于 Mongo 数据源，服务对象是`MongoClient`（客户端的配置是根据位置、元数据和消费者配置计算的）。

本节概述了有关服务类型的详细信息，并描述了如何使用默认服务类型。

### 没有类型的服务

有些记录可能没有类型（`ServiceType.UNKNOWN`）。 无法检索这些记录的引用，但您可以从`Record`的`location`和`metadata`构建连接详细信息。

使用这些服务不会触发服务使用事件。

### 实现自己的服务类型

您可以通过实现 `ServiceType` SPI 创建自己的服务类型：

1. (可选) 创建一个扩展`ServiceType`的公共接口。 此接口仅用于提供帮助方法以简化类型的使用，例如 `createRecord` 方法、`getX`，其中 `X` 是您检索的服务对象的类型等。 例如，检查 `HttpEndpoint` 或 `MessageSource`
2. 创建一个实现 `ServiceType` 的类或您在步骤 1 中创建的接口。该类型有一个 `name`，以及为该类型创建 `ServiceReference` 的方法。 该名称必须与与您的类型关联的`Record`的`type`字段匹配。
3. 创建一个扩展 `io.vertx.ext.discovery.types.AbstractServiceReference` 的类。 您可以使用您要返回的服务对象的类型参数化该类。 您必须实现创建服务对象的`AbstractServiceReference#retrieve()`。 此方法只调用一次。 如果您的服务对象需要清理，请同时覆盖`AbstractServiceReference#onClose()`。
4. 创建一个打包在你的 jar 中的 `META-INF/services/io.vertx.servicediscovery.spi.ServiceType` 文件。 在此文件中，只需指明在步骤 2 中创建的类的完全限定名称。
5. 创建一个包含服务类型接口（步骤 1）、实现（步骤 2 和 3）和服务描述符文件（步骤 4）的 jar。 将此 jar 放在应用程序的类路径中。 在这里，您的服务类型现在可用。

### HTTP 端点

HTTP 端点代表 REST API 或使用 HTTP 请求可访问的服务。 HTTP 端点服务对象是配置了主机、端口和 ssl 的 `HttpClient`。

#### 发布HTTP端点

要发布HTTP端点，你需要一个`Record`。你可以使用 `HttpEndpoint.createRecord`创建记录。

下面的代码片段演示了如何从 `HttpEndpoint` 创建一个 `Record`:

```java
Record record1 = HttpEndpoint.createRecord(
  "some-http-service", // The service name
  "localhost", // The host
  8433, // the port
  "/api" // the root of the service
);

discovery.publish(record1, ar -> {
  // ...
});

Record record2 = HttpEndpoint.createRecord(
  "some-other-name", // the service name
  true, // whether or not the service requires HTTPs
  "localhost", // The host
  8433, // the port
  "/api", // the root of the service
  new JsonObject().put("some-metadata", "some value")
);
```

当您在容器或云上运行您的服务时，它可能不知道它的公共IP和公共端口，因此发布必须由拥有该信息的另一个实体完成。一般来说，它是一个bridge。

#### 使用HTTP端点

一旦发布了HTTP端点，使用者就可以检索它。服务对象是一个配置了端口和主机的 `HttpClient` :

```java
discovery.getRecord(new JsonObject().put("name", "some-http-service"), ar1 -> {
  if (ar1.succeeded() && ar1.result() != null) {
    // Retrieve the service reference
    ServiceReference reference = discovery.getReference(ar1.result());
    // Retrieve the service object
    HttpClient client = reference.getAs(HttpClient.class);

    // You need to path the complete path
    client.request(HttpMethod.GET, "/api/persons").compose(request ->
      request
        .send()
        .compose(HttpClientResponse::body))
      .onComplete(ar2 -> {
      // Dont' forget to release the service
      reference.release();
    });
  }
});
```

您还可以使用 `HttpEndpoint.getClient`方法将查找和服务检索结合在一个调用中:

```java
HttpEndpoint.getClient(discovery, new JsonObject().put("name", "some-http-service"), ar -> {
  if (ar.succeeded()) {
    HttpClient client = ar.result();

    // You need to path the complete path
    client.request(HttpMethod.GET, "/api/persons").compose(request ->
      request
        .send()
        .compose(HttpClientResponse::body))
      .onComplete(ar2 -> {
        // Dont' forget to release the service
        ServiceDiscovery.releaseServiceObject(discovery, client);
      });
  }
});
```

在第二个版本中，服务对象是使用 `ServiceDiscovery.releaseServiceObject` 释放的，因此您不需要保留服务引用。

从 Vert.x 3.4.0 开始，提供了另一个客户端。 这个名为`WebClient`的高级客户端往往更易于使用。 您可以使用以下方法检索 `WebClient` 实例：

```java
discovery.getRecord(new JsonObject().put("name", "some-http-service"), ar -> {
  if (ar.succeeded() && ar.result() != null) {
    // Retrieve the service reference
    ServiceReference reference = discovery.getReference(ar.result());
    // Retrieve the service object
    WebClient client = reference.getAs(WebClient.class);

    // You need to path the complete path
    client.get("/api/persons").send(
      response -> {

        // ...

        // Dont' forget to release the service
        reference.release();

      });
  }
});
```

而且，如果您更喜欢使用服务类型的方法：

```java
HttpEndpoint.getWebClient(discovery, new JsonObject().put("name", "some-http-service"), ar -> {
  if (ar.succeeded()) {
    WebClient client = ar.result();

    // You need to path the complete path
    client.get("/api/persons")
      .send(response -> {

        // ...

        // Dont' forget to release the service
        ServiceDiscovery.releaseServiceObject(discovery, client);

      });
  }
});
```

### 事件总线服务(RPC 服务)

事件总线服务是服务代理。 它们在事件总线之上实现异步 RPC 服务。 从事件总线服务中检索服务对象时，您将获得正确类型的服务代理。 您可以从 `EventBusService` 访问辅助方法。

请注意，服务代理（服务实现和服务接口）是用 Java 开发的。

#### 发布事件总线服务

要发布事件总线服务，您需要创建一个`Record`：

```java
Record record = EventBusService.createRecord(
    "some-eventbus-service", // The service name
    "address", // the service address,
    "examples.MyService", // the service interface as string
    new JsonObject()
        .put("some-metadata", "some value")
);

discovery.publish(record, ar -> {
  // ...
});
```

您还可以将服务接口作为类传递：

```java
Record record = EventBusService.createRecord(
"some-eventbus-service", // The service name
"address", // the service address,
MyService.class // the service interface
);

discovery.publish(record, ar -> {
// ...
});
```

#### 使用事件总线服务

要使用事件总线服务，您可以检索记录然后获取引用，或者使用在一次调用中组合这两个操作的 `EventBusService` 接口。

当使用引用时，你可以这样做:

```java
discovery.getRecord(new JsonObject().put("name", "some-eventbus-service"), ar -> {
if (ar.succeeded() && ar.result() != null) {
// Retrieve the service reference
ServiceReference reference = discovery.getReference(ar.result());
// Retrieve the service object
MyService service = reference.getAs(MyService.class);

// Dont' forget to release the service
reference.release();
}
});
```

使用 `EventBusService` 类，你可以如下所示获得代理:

```java
EventBusService.getProxy(discovery, MyService.class, ar -> {
if (ar.succeeded()) {
MyService service = ar.result();

// Dont' forget to release the service
ServiceDiscovery.releaseServiceObject(discovery, service);
}
});
```

### 消息源

消息源是在特定地址上的事件总线上发送消息的组件。 消息源客户端是`MessageConsumer`。

*location* 或消息源服务是发送消息的事件总线地址。

#### 发布消息源

对于其他服务类型，发布消息源需要两个步骤:

1. 使用 `MessageSource` 创建一个记录
2. 发布记录

```java
Record record = MessageSource.createRecord(
    "some-message-source-service", // The service name
    "some-address" // The event bus address
);

discovery.publish(record, ar -> {
  // ...
});

record = MessageSource.createRecord(
    "some-other-message-source-service", // The service name
    "some-address", // The event bus address
    "examples.MyData" // The payload type
);
```

在第二条记录中，还指出了有效载荷的类型。 此信息是可选的。

在java中，你可以使用`Class`参数：

```java
Record record1 = MessageSource.createRecord(
"some-message-source-service", // The service name
"some-address", // The event bus address
JsonObject.class // The message payload type
);

Record record2 = MessageSource.createRecord(
"some-other-message-source-service", // The service name
"some-address", // The event bus address
JsonObject.class, // The message payload type
new JsonObject().put("some-metadata", "some value")
);
```

#### 使用消息源

在消费者端，您可以检索记录和引用，或者使用 `MessageSource` 类来检索服务是一个调用。

使用第一种方法，代码如下：

```java
discovery.getRecord(new JsonObject().put("name", "some-message-source-service"), ar -> {
  if (ar.succeeded() && ar.result() != null) {
    // Retrieve the service reference
    ServiceReference reference = discovery.getReference(ar.result());
    // Retrieve the service object
    MessageConsumer<JsonObject> consumer = reference.getAs(MessageConsumer.class);

    // Attach a message handler on it
    consumer.handler(message -> {
      // message handler
      JsonObject payload = message.body();
    });
  }
});
```

当使用 `MessageSource` 时，它变为：

```java
MessageSource.<JsonObject>getConsumer(discovery, new JsonObject().put("name", "some-message-source-service"), ar -> {
  if (ar.succeeded()) {
    MessageConsumer<JsonObject> consumer = ar.result();

    // Attach a message handler on it
    consumer.handler(message -> {
      // message handler
      JsonObject payload = message.body();
    });
    // ...
  }
});
```

### JDBC 数据源

数据源代表数据库或数据存储。 JDBC 数据源是对使用 JDBC 驱动程序可访问的数据库的一种特殊化。 JDBC 数据源服务的客户端是一个`JDBCClient`。

#### 发布 JDBC 服务

至于其他服务类型，发布 JDBC 数据源是一个两步过程：

1. 使用 `JDBCDataSource` 创建一个记录
2. 发布记录

```java
Record record = JDBCDataSource.createRecord(
    "some-data-source-service", // The service name
    new JsonObject().put("url", "some jdbc url"), // The location
    new JsonObject().put("some-metadata", "some-value") // Some metadata
);

discovery.publish(record, ar -> {
  // ...
});
```

由于 JDBC 数据源可以代表各种各样的数据库，并且它们的访问方式通常不同，因此Record是相当非结构化的。 `location` 是一个简单的 JSON 对象，它应该提供访问数据源的字段（JDBC url、用户名……）。 字段集可能取决于数据库，也可能取决于前面使用的连接池。

#### 使用 JDBC 服务

如上一节所述，如何访问数据源取决于数据源本身。 要构建 `JDBCClient`，可以合并配置：record位置、元数据和消费者提供的 json 对象：

```java
discovery.getRecord(
    new JsonObject().put("name", "some-data-source-service"),
    ar -> {
      if (ar.succeeded() && ar.result() != null) {
        // Retrieve the service reference
        ServiceReference reference = discovery.getReferenceWithConfiguration(
            ar.result(), // The record
            new JsonObject().put("username", "clement").put("password", "*****")); // Some additional metadata

        // Retrieve the service object
        JDBCClient client = reference.getAs(JDBCClient.class);

        // ...

        // when done
        reference.release();
      }
    });
```

您还可以使用 `JDBCClient` 类在一次调用中进行查找和检索：

```java
JDBCDataSource.<JsonObject>getJDBCClient(discovery,
    new JsonObject().put("name", "some-data-source-service"),
    new JsonObject().put("username", "clement").put("password", "*****"), // Some additional metadata
    ar -> {
      if (ar.succeeded()) {
        JDBCClient client = ar.result();

        // ...

        // Dont' forget to release the service
        ServiceDiscovery.releaseServiceObject(discovery, client);

      }
    });
```

### Redis 数据源

Redis 数据源是 Redis 持久性数据库的一种特殊化。 Redis 数据源服务的客户端是一个`Redis`。

#### 发布 Redis 服务

发布 Redis 数据源分为两步：

1. 使用 `RedisDataSource` 创建记录
2. 发布记录

```java
Record record = RedisDataSource.createRecord(
  "some-redis-data-source-service", // The service name
  new JsonObject().put("url", "localhost"), // The location
  new JsonObject().put("some-metadata", "some-value") // Some metadata
);

discovery.publish(record, ar -> {
  // ...
});
```

`location` 是一个简单的 JSON 对象，它应该提供访问 Redis 数据源（url、端口……）的字段。

#### 使用 Redis 服务

如上一节所述，如何访问数据源取决于数据源本身。 要构建 Redis，您可以合并配置：record 位置、元数据和消费者提供的 json 对象：

```java
discovery.getRecord(
  new JsonObject().put("name", "some-redis-data-source-service"), ar -> {
    if (ar.succeeded() && ar.result() != null) {
      // Retrieve the service reference
      ServiceReference reference = discovery.getReference(ar.result());

      // Retrieve the service instance
      Redis client = reference.getAs(Redis.class);

      // ...

      // when done
      reference.release();
    }
  });
```

您还可以使用 `RedisDataSource` 类在一次调用中进行查找和检索：

```java
RedisDataSource.getRedisClient(discovery,
  new JsonObject().put("name", "some-redis-data-source-service"),
  ar -> {
    if (ar.succeeded()) {
      Redis client = ar.result();

      // ...

      // Dont' forget to release the service
      ServiceDiscovery.releaseServiceObject(discovery, client);

    }
  });
```

### Mongo 数据源

Mongo 数据源是 MongoDB 数据库的一种专门化。 Mongo 数据源服务的客户端是一个`MongoClient`。

#### 发布 Mongo 服务

发布 Mongo 数据源是一个两步过程：

1. 使用 `MongoDataSource` 创建一个记录
2. 发布记录

```java
Record record = MongoDataSource.createRecord(
  "some-data-source-service", // The service name
  new JsonObject().put("connection_string", "some mongo connection"), // The location
  new JsonObject().put("some-metadata", "some-value") // Some metadata
);

discovery.publish(record, ar -> {
  // ...
});
```

`location` 是一个简单的 JSON 对象，它应该提供访问 Redis 数据源（url、端口……）的字段。

#### 使用 Mongo 服务

如上一节所述，如何访问数据源取决于数据源本身。 要构建`MongoClient`，可以合并配置：record 位置、元数据和消费者提供的json对象：

```java
discovery.getRecord(
  new JsonObject().put("name", "some-data-source-service"),
  ar -> {
    if (ar.succeeded() && ar.result() != null) {
      // Retrieve the service reference
      ServiceReference reference = discovery.getReferenceWithConfiguration(
        ar.result(), // The record
        new JsonObject().put("username", "clement").put("password", "*****")); // Some additional metadata

      // Retrieve the service object
      MongoClient client = reference.get();

      // ...

      // when done
      reference.release();
    }
  });
```

您还可以使用 `MongoDataSource` 类在一次调用中进行查找和检索：

```java
MongoDataSource.<JsonObject>getMongoClient(discovery,
  new JsonObject().put("name", "some-data-source-service"),
  new JsonObject().put("username", "clement").put("password", "*****"), // Some additional metadata
  ar -> {
    if (ar.succeeded()) {
      MongoClient client = ar.result();

      // ...

      // Dont' forget to release the service
      ServiceDiscovery.releaseServiceObject(discovery, client);

    }
  });
```

## 监听服务到达和离开

每次发布或删除提供者时，都会在 `vertx.discovery.announce` 地址上发布一个事件。 此地址可从 `ServiceDiscoveryOptions` 配置。

接收到的记录有一个 `status` 字段指示记录的新状态：

- `UP` : 服务可用，可以开始使用
- `DOWN` : 该服务不再可用，你不应该再使用它
- `OUT_OF_SERVICE` : 服务没有运行，你不应该再使用它，但它可能会在稍后回来。

## 监听服务使用情况

每次检索（`bind`）或释放（`release`）服务引用时，都会在 `vertx .discovery.usage` 地址上发布一个事件。 此地址可从 `ServiceDiscoveryOptions` 配置。

它使您可以侦听服务使用情况并映射服务绑定。

收到的消息是一个 `JsonObject` 包含：

- `record` 字段中的记录
- `type` 字段中的事件类型。 它是`bind`或`release`
- `id` 字段中服务发现的 id（它的名称或节点 id）

这个 `id` 可以从 `ServiceDiscoveryOptions` 配置。 默认情况下，它在单节点配置上是 `localhost` ，在集群模式下是节点的 id。

您可以通过使用 `setUsageAddress` 将使用地址设置为 `null` 来禁用服务使用支持。

## 服务发现桥接器

桥接器允许您从/到其他发现机制（例如 Docker、Kubernetes、Consul）导入和导出服务……每个桥接器决定服务的导入和导出方式。 它不必是双向的。

您可以通过实现 `ServiceImporter` 接口来提供自己的桥接器，并使用 `registerServiceImporter` 注册它。

第二个参数可以为桥接器提供可选配置。

当桥被注册时，`start` 方法被调用。 它允许您配置网桥。 当网桥配置、准备就绪并导入/导出初始服务时，它必须完成给定的`Future`。 如果桥接启动方法是阻塞的，它必须使用 `executeBlocking` 构造，并完成给定的`Future`对象。

当服务发现停止时，网桥也会停止。 调用 `close` 方法，提供清理资源的机会，移除导入/导出的服务……此方法必须完成给定的 `Future` 以通知调用者完成。

请注意，与集群相比，只有一个成员需要注册网桥，因为所有成员都可以访问记录。

## 额外的桥接器

除了这个库支持的网桥之外，Vert.x 服务发现还提供了您可以在应用程序中使用的其他网桥。

### Consul 桥接器

这个发现桥将服务从 [Consul](https://consul.io/) 导入到 Vert.x 服务发现中。 网桥连接到 Consul 代理（服务器）并定期扫描服务：

- 新服务被导入
- 维护模式下的服务或已从 consul 中删除的服务将被移除

此桥使用 Consul 的 HTTP API。它不导出到Consul，也不支持服务修改。

服务类型是从 `tags` 推导出来的。 如果 `tag` 与已知服务类型匹配，则将使用此服务类型。 如果不是，该服务将作为 `unknown` 导入。 目前仅支持 `http-endpoint`。

#### 使用此桥接器

要使用此 Vert.x 发现桥，请将以下依赖项添加到构建描述符的 *dependencies* 部分：

- Maven (在你的 `pom.xml`):

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-service-discovery-bridge-consul</artifactId>
 <version>4.3.0</version>
</dependency>
```

- Gradle (在你的 `build.gradle` ):

```groovy
compile 'io.vertx:vertx-service-discovery-bridge-consul:4.3.0'
```

然后，在创建服务发现时，按如下方式注册此桥：

```java
ServiceDiscovery.create(vertx)
    .registerServiceImporter(new ConsulServiceImporter(),
        new JsonObject()
            .put("host", "localhost")
            .put("port", 8500)
            .put("scan-period", 2000));
```

您可以配置：

- 使用 `host` 属性的代理主机，默认为 `localhost`
- 使用 `port` 属性的代理端口，默认为 8500
- 使用 `acl_token` 属性的 acl 令牌，默认为 null
- 使用 `scan-period` 属性的扫描周期。 时间以毫秒为单位，默认为2000毫秒

### Kubernetes 桥接器

这个发现桥将服务从 Kubernetes（或 Openshift v3）导入到 Vert.x 服务发现中。 Kubernetes 服务映射到 `Record`。 此桥仅支持从 vert.x 中的 kubernetes 导入服务（而不是相反）。

`Record` 是从 Kubernetes 服务创建的。 服务类型是从 `service-type` 标签或服务公开的端口推导出来的。

#### 使用此桥接器

要使用此 Vert.x 发现桥，请将以下依赖项添加到构建描述符的 *dependencies* 部分：

- Maven (在你的 `pom.xml`):

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-service-discovery-bridge-kubernetes</artifactId>
 <version>4.3.0</version>
</dependency>
```

- Gradle (在你的 `build.gradle`):

```groovy
compile 'io.vertx:vertx-service-discovery-bridge-kubernetes:4.3.0'
```

然后，在创建服务发现时，按如下方式注册此网桥：

```java
JsonObject defaultConf = new JsonObject();
serviceDiscovery.registerServiceImporter(new KubernetesServiceImporter(), defaultConf);
```

#### 配置桥接器

使用以下方式配置网桥：

- oauth 令牌（默认使用 `/var/run/secrets/kubernetes.io/serviceaccount/token` 的内容）
- 搜索服务的命名空间（默认为 `default`）。

请注意，应用程序必须有权访问 Kubernetes，并且必须能够读取所选命名空间。

#### 服务到记录的映射

记录创建如下：

- 服务类型是从 `service.type` 标签推导出来的。 如果未设置此标签，则服务类型设置为 `unknown`
- 记录的名称是服务的名称
- 服务的标签映射到元数据
- 另外添加了：`kubernetes.uuid`、`kubernetes.namespace`、`kubernetes.name`
- 位置是从服务的*第一个**端口推导出来的

对于 HTTP 端点，如果服务的 `ssl` 标签设置为 `true`，则`ssl` (`https`) 属性设置为`true`。

#### 动态响应

网桥在 `start` 时导入所有服务，并在 `stop` 时删除它们。 在两者之间，它监视 Kubernetes 服务并添加新服务并删除已删除的服务。

#### 支持的类型

网桥使用`service-type`标签来感知类型。 此外，它还会检查服务的端口。 支持：

- 端口 80、443 和从 8080 到 9000：HTTP 端点
- 端口 5432 和 5433：JDBC 数据源 (PostGreSQL)
- 端口 3306 和 13306：JDBC 数据源 (MySQL)
- 端口 6379：Redis 数据源
- 端口 27017、27018 和 27019：MongoDB 数据源

如果存在，`service-type` 将覆盖基于端口的推论。

### Zookeeper 桥接器

这个发现桥将服务从 [Apache Zookeeper](https://zookeeper.apache.org/) 导入到 Vert.x 服务发现中。 桥使用 [Curator 扩展服务发现](https://curator.apache.org/curator-x-discovery/)。

服务描述被读取为 JSON 对象（合并在 Vert.x 服务记录元数据中）。 服务类型是通过阅读 `service-type` 从这个描述中推断出来的。

#### 使用此桥接器

要使用此 Vert.x 发现桥，请将以下依赖项添加到构建描述符的 *dependencies* 部分：

- Maven (在你的 `pom.xml`):

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-service-discovery-bridge-zookeeper</artifactId>
 <version>4.3.0</version>
</dependency>
```

- Gradle (在你的 `build.gradle`):

```groovy
compile 'io.vertx:vertx-service-discovery-bridge-zookeeper:4.3.0'
```

然后，在创建服务发现时，按如下方式注册此桥：

```java
ServiceDiscovery.create(vertx)
    .registerServiceImporter(new ZookeeperServiceImporter(),
        new JsonObject()
            .put("connection", "127.0.0.1:2181"));
```

只有 `connection` 配置是强制性的。 它是 Zookeeper 服务器的连接 *字符串*。

此外，您可以配置：

- `maxRetries`：连接尝试次数，默认为 3
- `baseSleepTimeBetweenRetries`：重试之间等待的毫秒数（指数退避策略）。 默认为 1000 毫秒。
- `basePath`：存储服务的 Zookeeper 路径。 默认为`/discovery`。
- `connectionTimeoutMs`：以毫秒为单位的连接超时。 默认为 1000。
- `canBeReadOnly` : 后端是否支持 *read-only* 模式（默认为 true）

```java
ServiceDiscovery.create(vertx)
    .registerServiceImporter(new ZookeeperServiceImporter(),
        new JsonObject()
            .put("connection", "127.0.0.1:2181")
            .put("maxRetries", 5)
            .put("baseSleepTimeBetweenRetries", 2000)
            .put("basePath", "/services")
    );
```

### Docker 链接桥

这个发现桥将服务从 Docker Links 导入到 Vert.x 服务发现中。 当您将 Docker 容器链接到另一个 Docker 容器时，Docker 会注入一组环境变量。 该网桥分析这些环境变量并为每个链接导入服务记录。 服务类型是从 `service.type` 标签推导出来的。 如果未设置，服务将作为 `unknown` 导入。 目前仅支持 `http-endpoint`。

由于链接是在容器启动时创建的，因此导入的记录是在桥启动时创建的，之后不会更改。

#### 使用此桥接器

要使用此 Vert.x 发现桥，请将以下依赖项添加到构建描述符的 *dependencies* 部分：

- Maven (在你的 `pom.xml`):

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-service-discovery-bridge-docker</artifactId>
 <version>4.3.0</version>
</dependency>
```

- Gradle (在你的 `build.gradle`):

```groovy
compile 'io.vertx:vertx-service-discovery-bridge-docker:4.3.0'
```

然后，在创建服务发现时，按如下方式注册此网桥：

```java
ServiceDiscovery.create(vertx)
    .registerServiceImporter(new DockerLinksServiceImporter(), new JsonObject());
```

网桥不需要任何进一步的配置。

## 其他后端

除了此库支持的后端之外，Vert.x 服务发现还提供了您可以在应用程序中使用的其他后端。

### Redis 后端

服务发现有一个使用 `ServiceDiscoveryBackend` SPI 的可插入后端。 这是一个基于 Redis 的 SPI 实现。

#### 使用 Redis 后端

要使用 Redis 后端，请将以下依赖项添加到构建描述符的 *dependencies* 部分：

- Maven (在你的 `pom.xml`):

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-service-discovery-backend-redis</artifactId>
 <version>4.3.0</version>
</dependency>
```

- Gradle (在你的 `build.gradle`):

```groovy
compile 'io.vertx:vertx-service-discovery-backend-redis:4.3.0'
```

请注意，您的 *classpath* 中只能有一个 SPI 实现。 如果没有，则使用默认后端。

#### 配置

后端基于 [vertx-redis-client](https://vertx.io/docs/vertx-redis-client/java)。 配置是客户端配置以及 `key` 指示记录存储在 Redis 上的哪个 *key* 中。

这是一个例子：

```java
ServiceDiscovery.create(vertx, new ServiceDiscoveryOptions()
    .setBackendConfiguration(
        new JsonObject()
            .put("connectionString", "redis://localhost:6379")
            .put("key", "records")
    ));
```

需要注意的是，后端配置是在 `setBackendConfiguration` 方法中传递的（如果使用 JSON，则为 `backendConfiguration` 条目）：

```java
ServiceDiscovery.create(vertx,
  new ServiceDiscoveryOptions(new JsonObject()
    .put("backendConfiguration",
      new JsonObject().put("connectionString", "redis://localhost:6379").put("key", "my-records")
)));
```
