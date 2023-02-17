# Vert.x 健康检查

该组件提供了一个简单的检视健康状况的途径。 健康检查组件使用非常简单的措辞来表达应用程序的当前状况： *UP* 以及 *DOWN* 。 健康检查组件可以单独使用，也可以和 Vert.x Web 或者事件总线联合使用。

该组件提供一个 Vert.x Web handler 让您可以注册一些（检测）例程 用于检测应用程序的健康状况。 该 handler 计算出（健康状况的）最终状态并以 JSON 方式返回结果。

## 如何使用 Vert.x 健康检查

请注意，一般情况下您需要 Vert.x Web 模块来使用该组件。启用该组件只需要添加以下依赖：

- Maven （在您的 `pom.xml` 文件中）：

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-health-check</artifactId>
 <version>4.3.5</version>
</dependency>
```

- Gradle （在您的 `build.gradle` 文件中）：

```groovy
compile 'io.vertx:vertx-health-check:4.3.5'
```

### 创建健康检查对象。

最主要的对象是 `HealthChecks` 。 您可以通过以下方式创建该对象的实例：

```java
HealthChecks hc = HealthChecks.create(vertx);

hc.register(
  "my-procedure",
  promise -> promise.complete(Status.OK()));

// 注册时指定超时时间参数。如果未能在超时时间之内完成，则视为故障。
// timeout 参数的单位是毫秒。
hc.register(
  "my-procedure",
  2000,
  promise -> promise.complete(Status.OK()));
```

一旦您创建了这个对象，您就可以注册或者注销（检测）例程。详情请见后面的章节。

### 注册 Vert.x Web handler

您可以通过以下方式创建用于健康检查的 Vert.x Web handler ：

- 使用已有的 `HealthChecks` 对象实例
- 让 handler 为您创建一个新的对象实例。

```java
HealthCheckHandler healthCheckHandler1 = HealthCheckHandler.create(vertx);

HealthCheckHandler healthCheckHandler2 = HealthCheckHandler
  .createWithHealthChecks(HealthChecks.create(vertx));

Router router = Router.router(vertx);
// 向 router 添加路由规则
// 注册健康检查 handler
router.get("/health*").handler(healthCheckHandler1);
// 或者
router.get("/ping*").handler(healthCheckHandler2);
```

可以直接在 `HealthCheckHandler` 对象实例中注册例程。 此外，如果您已经预先创建了 `HealthChecks` 对象的实例， 您可以直接在该对象中注册例程。 在任何时刻都可以进行例程的注册和注销，即使在路由规则注册完成之后也可以：

```java
HealthCheckHandler healthCheckHandler = HealthCheckHandler.create(vertx);

// 注册例程
// 在路由规则注册完成之后，甚至是运行时，也可以完成该操作
healthCheckHandler.register("my-procedure-name", promise -> {
  // 进行检测 ....
  // 检测通过时执行
  promise.complete(Status.OK());
  // 如果发生故障则执行：
  promise.complete(Status.KO());
});

// 注册另一个指定了超时时间（2秒）的例程。 如果该例程未能在
// 指定的超时时间内完成，则视为故障。
healthCheckHandler.register(
  "my-procedure-name-with-timeout",
  2000,
  promise -> {
    // 进行检测 ....
    // 检测通过时执行
    promise.complete(Status.OK());
    // 如果发生故障则执行：
    promise.complete(Status.KO());
  });

router.get("/health").handler(healthCheckHandler);
```

## 例程（procedures）

此处的例程是指一个检查系统某个表征现象的函数，用于推断当前的健康状况。 它报告一个 `Status` 对象用以指示该项检测是否通过。 该函数将检测结果报告给它所对应的 `Promise` ，并且请注意该函数不可以阻塞这个 `Promise` 。

当您注册了一个例程，您需要对其命名，并且要指定一个函数（handler）来执行该项检测。

推断健康状况的规则如下：

- 如果对应的 promise 被标记为故障，则检测结果认定为 *KO*
- 如果对应的 promise 成功完成但是没有包含一个 `Status` 对象， 检测结果认定为 *OK*。
- 如果对应的 promise 成功完成且包含一个标记为 *OK* 的 `Status` 对象， 检测结果认定为 *OK*。
- 如果对应的 promise 成功完成且包含一个标记为 *KO* 的 `Status` 对象， 检测结果认定为 *KO*。

`Status` 对象可以提供额外的数据：

```java
HealthCheckHandler healthCheckHandler = HealthCheckHandler.create(vertx);

// Status 对象能以 JSON 形式提供额外的数据
healthCheckHandler.register("my-procedure-name", promise -> {
  promise.complete(Status.OK(new JsonObject().put("available-memory", "2mb")));
});

healthCheckHandler.register("my-second-procedure-name", promise -> {
  promise.complete(Status.KO(new JsonObject().put("load", 99)));
});

router.get("/health").handler(healthCheckHandler);
```

例程可以进行分组管理。 例程的名称里可以指定分组信息。 分组的例程按照树形结构进行组织， 并且树形结构被映射到 HTTP url 之上（如下所示）。

```java
HealthCheckHandler healthCheckHandler = HealthCheckHandler.create(vertx);

// 注册例程
// 例程可以进行分组，以例程名称中的 “/” 分隔符来判断组别
// 一个分组中也可以包含另一个分组
healthCheckHandler.register(
  "a-group/my-procedure-name",
  promise -> {
    //....
  });
healthCheckHandler.register(
  "a-group/a-second-group/my-second-procedure-name",
  promise -> {
    //....
  });

router.get("/health").handler(healthCheckHandler);
```

## HTTP 响应和 JSON 输出

启用 Vert.x web handler 之后，可以通过对外开放的 `HealthCheckHandler` 所对应的路由规则 以 HTTP GET 或者 POST （取决于您注册的路由规则）的方式获取总体健康检查信息。

如果没有注册任何例程， 则响应信息为 `204 - NO CONTENT` ， 表明系统状态为 *UP* 但是没有执行任何例程。 此时响应信息不包含任何有效数据。

如果注册了至少一个例程，该例程将被执行并计算出检测结果。 响应码包括下列几种：

- `200` : 一切正常
- `503` : 至少有一个例程报告了不健康状态
- `500` : 某个例程抛出了错误，或者未能及时报告状态

响应的内容是一个 JSON 文档，体现的是总体结果（`outcome`）。总体结果要么是 `UP` 要么是 `DOWN` 。 此外还给出了一个 `checks` 数组用以显示每个执行过的例程的结果。 如果某个例程报告了额外的数据，这些数据也会一并给出：

```json
{
"checks" : [
{
  "id" : "A",
  "status" : "UP"
},
{
  "id" : "B",
  "status" : "DOWN",
  "data" : {
    "some-data" : "some-value"
  }
}
],
"outcome" : "DOWN"
}
```

如果采用了分组/层级结构，则 `checks` 数组通过以下结构来描述：

```json
{
"checks" : [
{
  "id" : "my-group",
  "status" : "UP",
  "checks" : [
  {
    "id" : "check-2",
    "status" : "UP",
  },
  {
    "id" : "check-1",
    "status" : "UP"
  }]
}],
"outcome" : "UP"
}
```

如果一个例程抛出了错误，或者报告了故障（异常），该 JSON 文档会在 `data` 字段下给出 `cause` 字段。 如果某个例程未能及时上报结果，则结果将显示为 `Timeout` （超时）。

## 例程示例

此章节提供一些通用的健康检查示例

### SQL client

该例子用以报告一个数据库连接是否成功建立：

```java
handler.register("database",
  promise -> pool.getConnection(connection -> {
    if (connection.failed()) {
      promise.fail(connection.cause());
    } else {
      connection.result().close();
      promise.complete(Status.OK());
    }
  }));
```

### 服务可用性

该项检测用于报告某个服务（此处是指一个HTTP endpoint）在服务发现中是否可用：

```java
handler.register("my-service",
  promise ->
    HttpEndpoint.getClient(discovery, rec -> "my-service".equals(rec.getName()),
      client -> {
        if (client.failed()) {
          promise.fail(client.cause());
        } else {
          client.result().close();
          promise.complete(Status.OK());
        }
      }));
```

### 事件总线

该项检测用于报告某个事件总线上的某个消费者是否已经准备就绪。 在这个例子中，是一个简单的 ping/pong 应答协议，您也可以换成别的更为复杂的场景。 该项检测可以用于检查某个 verticle 是否已经准备就绪并且已在监听某个事件总线地址。

```java
handler.register("receiver",
  promise ->
    vertx.eventBus().request("health", "ping")
      .onSuccess(msg -> {
        promise.complete(Status.OK());
      })
      .onFailure(err -> {
        promise.complete(Status.KO());
      }));
```

## 身份认证

当使用 Vert.x web handler 时， 您可以传入一个 `AuthenticationProvider` 对象用来对请求进行身份认证。 详情请查阅 `Vert.x Auth` 。

Vert.x Web handler 创建一个 JSON 对象包含以下内容：

- 请求头
- 请求参数
- 表单参数（如果存在）
- JOSN 格式的内容（如果存在，并且请求的 content type 是 `application/json` ）

上述对象会被传入身份认证方式提供者来对请求进行身份认证。 如果认证失败，则会返回 `403 - FORBIDDEN` 响应。

## 在事件总线上开放健康检查功能

利用 Vert.x web handler 通过 HTTP 方式开放健康检查功能是十分便捷的，但是通过别的方式开放这些数据可以发挥更大的作用。 以下章节给出了如何在事件总线上开放健康检查数据的例子：

```java
vertx.eventBus().consumer("health",
  message -> healthChecks.checkStatus()
    .onSuccess(message::reply)
    .onFailure(err -> message.fail(0, err.getMessage())));
```

------

<<<<<< [完] >>>>>>

