# Vert.x EventBus Bridge Client中文版

> 翻译: 白石(https://github.com/wjw465150/Vert.x-Core-Manual)
>
> 项目地址: https://github.com/vert-x3/vertx-eventbus-bridge-clients

Vert.x EventBus Client 是一个 Java 客户端，允许应用程序通过 TCP 或 WebSocket 传输与 Vert.x EventBus 桥交互。 连接后，它允许：

- 将消息发送到 EventBus 的地址。
- 向 EventBus 的地址发送消息并期待回复。
- 将消息发布到 EventBus 的地址。
- 创建消费者并将其注册到相应地址上。
- 从相应地址注销消费者。

在底层，发送到服务器的数据包遵循 [Vert.x EventBus TCP 桥](https://vertx.io/docs/vertx-tcp-eventbus-bridge/java/) 中定义的协议。

> **📝注意:** 此客户端不依赖于 Vert.x，需要 Java 6 运行时，使其也可以嵌入到 Android 应用程序中。

## 使用 Vert.x Event Bus 客户端

要使用此项目，请将以下依赖项添加到构建描述符的 *dependencies* 部分：

- Maven (在你的 `pom.xml`):

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-eventbus-bridge-client</artifactId>
 <version>1.0.1</version>
</dependency>
<dependency>
 <groupId>com.google.code.gson</groupId>
 <artifactId>gson</artifactId>
 <version>${gson.version}</version>
</dependency>
```

- Gradle (在你的 `build.gradle`):

```groovy
compile 'io.vertx:vertx-eventbus-bridge-client:1.0.1'
compile 'com.google.code.gson:gson:${gson.version}'
```

## 创建 Vert.x EventBus 客户端

有两种创建 Vert.x EventBus 客户端的方法，具体取决于要与之交互的桥的类型：

### 创建 TCP 事件总线桥客户端

```java
EventBusClient tcpEventBusClient = EventBusClient.tcp();

// Create a bus client with specified host and port and TLS enabled
EventBusClientOptions options = new EventBusClientOptions()
  .setHost("127.0.0.1").setPort(7001)
  .setSsl(true)
  .setTrustStorePath("/path/to/store.jks")
  .setTrustStorePassword("change-it");
EventBusClient sslTcpEventBusClient = EventBusClient.tcp(options);
```

此示例可用于创建连接到 [Vert.x EventBus TCP 桥](https://vertx.io/docs/vertx-tcp-eventbus-bridge/java/) 的客户端。

使用默认选项，客户端将通过纯 TCP 连接到 `localhost:7000`。还可以将客户端配置为连接到不同的主机和端口，并为安全 TCP 连接启用 TLS/SSL。

### 创建 WebSocket SockJS 桥接客户端

```java
EventBusClient webSocketEventBusClient = EventBusClient.webSocket();

// Create a bus client with specified host and port, TLS enabled and WebSocket path.
EventBusClientOptions options = new EventBusClientOptions()
  .setHost("127.0.0.1").setPort(8043)
  .setSsl(true)
  .setTrustStorePath("/path/to/store.jks")
  .setTrustStorePassword("change-it")
  .setWebSocketPath("/eventbus/message")
  ;
EventBusClient sslWebSocketEventBusClient = EventBusClient.webSocket(options);
```

此示例可用于创建客户端以使用 WebSocket 连接到 [SockJS EventBus Bridge](https://vertx.io/docs/vertx-web/java/#_sockjs_event_bus_bridge)。

使用默认选项，客户端将使用 WebSocket 连接到 `http://localhost/eventbus/websocket`。还可以连接到不同的主机和端口，为安全 HTTP 连接启用 TLS/SSL。 `options.setWebSocketPath("/eventbus/message")` 可用于指定与 SockJS 桥中指定的路径对应的 WebSocket 路径。

## 与 EventBus 桥通信

不管连接的是什么类型的网桥，连接后，客户端可以通过以下方式与网桥通信：

> **📝注意:** 如果尚未建立连接，以下任何一种方法都会导致自动连接。

### 发送消息到 EventBus 的地址

消息可以发送到 EventBus 的地址。

```java
EventBusClient busClient = EventBusClient.tcp();

// 向总线发送消息，这会将客户端连接到服务器
busClient.send("newsfeed", "Breaking news: something great happened");
```

### 向 EventBus 的地址发送消息并期待回复

可以将消息发送到 EventBus 的地址，并指定预期的回复处理程序。

```java
busClient.request("newsfeed", "Breaking news: something great happened", new Handler<AsyncResult<Message<String>>>() {
  @Override
  public void handle(AsyncResult<Message<String>> reply) {
    System.out.println("We got the reply");
  }
});
```

### 发布消息到 EventBus 的地址

消息可以发布到 EventBus 的地址。

```java
busClient.publish("newsfeed", "Breaking news: something great happened");
```

### 创建消费者并将其注册到地址

您可以创建一个消费者并将其注册到 EventBus 的地址，当有任何消息发送到该地址时将调用它。

```java
busClient.consumer("newsfeed", new Handler<Message<String>>() {
  @Override
  public void handle(Message<String> message) {
    System.out.println("Received a news " + message.body());
  }
});
```

### 从其地址注销消费者

当消费者不再需要收听时，您可以从其地址中注销消费者。

```java
consumer.unregister();
```

## 关闭客户端

您可以关闭客户端以释放与桥接服务器的连接。

```java
busClient.closeHandler(new Handler<Void>() {
  @Override
  public void handle(Void event) {
    System.out.println("Bus Client Closed");
  }
});
// Closes the connection to the bridge server if it is open
busClient.close();
```

## JSON格式编码

客户端和桥接器以使用`JsonCodec`的实现编码的自定义 JSON 格式交换消息。 客户端提供了两个`JsonCodec`实现。

在不指定 JsonCodec 创建新的 EventBusClient 实例时，它首先尝试加载 `GsonCodec`，如果 Gson 不在类路径中，它会尝试加载 `JacksonCodec`，如果 FasterXML Jackson 数据绑定也不在类路径中 ，它无法创建客户端实例。

您也可以在创建新的`EventBusClient`实例时指定`JsonCodec`的自定义实例。

### GsonCodec

基于[Google Gson 项目](https://github.com/google/gson) 的`JsonCodec` 实现。 `com.google.code.gson:gson` 的依赖是可选的，您需要显式添加此依赖才能使用此实现。

### JacksonCodec

基于 [FasterXML Jackson 数据绑定](https://github.com/FasterXML/jackson-databind) 的 `JsonCodec` 实现。 `com.fasterxml.jackson.core:jackson-databind` 的依赖是可选的，您需要显式添加此依赖才能使用此实现。

## EventBus 客户端选项

Vert.x EventBus Client 中有 2 个主要选项。

### EventBusClientOptions

`EventBusClientOptions` 用于在创建期间配置 EventBusClient，它具有以下属性：

- `host`: String, 要连接的网桥主机地址，默认为“localhost”。
- `port`: int, 要连接的网桥端口，默认为`-1`，即 TCP 网桥为`7000`，WebSocket SockJS 网桥为`80`。
- `webSocketPath`: String, WebSocket客户端连接路径，默认为`/eventbus/websocket`。 它仅供 WebSocket EventBus Client 使用。
- `maxWebSocketFrameSize`: int, 最大 WebSocket 帧大小，默认为 65536。 它仅供 WebSocket EventBus Client 使用。
- `ssl`: boolean, 指示是否启用 SSL，默认为 `false`，表示未启用 SSL。
- `trustStorePath`: String, 信任库的路径。 它仅在 `ssl` 为true时使用。
- `trustStorePassword`: String, 信任库的密码。 它仅在 `ssl` 为true时使用。
- `trustStoreType`: String, 信任库类型，`jks`、`pfx`、`pem` 之一，默认为 `jks`。 它仅在 `ssl` 为true时使用。
- `verifyHost`: boolean, 如果启用主机名验证（用于 SSL/TLS），则默认为`true`。 它仅在 `ssl` 为true时使用。
- `trustAll`: boolean, 如果应该信任所有服务器 (SSL/TLS)，则默认为`false`。 它仅在 `ssl` 为true时使用。
- `pingInterval`: int, ping 间隔，以毫秒为单位，默认为`5000`毫秒。
- `autoReconnectInterval`: int, 自动重新连接尝试之间的暂停长度，以毫秒为单位，默认为`3000`毫秒。
- `maxAutoReconnectTries`: int, 自动重新连接尝试的最大次数，默认为`0`，表示没有限制。
- `connectTimeout`: int, 连接超时，以毫秒为单位，默认为`60000`毫秒。
- `idleTimeout`: int, 空闲超时，以毫秒为单位，默认为`0`，表示没有超时。
- `autoReconnect`: boolean, 是否启用自动重新连接，即使客户端不尝试发送消息，默认为`true`。
- `proxyHost`: String, 代理服务器地址。
- `proxyPort`: int, 代理服务器端口。
- `proxyUsername`: String, 如果代理需要身份验证，则为代理用户名。
- `proxyPassword`: String, 如果代理需要身份验证，则为代理密码。
- `proxyType`: ProxyType, `ProxyType.HTTP`、`ProxyType.SOCKS4`、`ProxyType.SOCKS5` 之一。

### DeliveryOptions

`DeliveryOptions` 用于向桥发送消息时，它具有以下属性：

- `timeout`: long, 发送超时，以毫秒为单位，默认为 `30 * 1000` 毫秒。 如果在超时时间内没有收到响应，则将调用处理程序失败。
- `headers`: Map, 发送到桥 EventBus 的标头。

------

<<<<<< [完] >>>>>>

