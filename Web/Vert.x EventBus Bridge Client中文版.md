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

To use this project, add the following dependency to the *dependencies* section of your build descriptor:

- Maven (in your `pom.xml`):

```
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

- Gradle (in your `build.gradle` file):

```
compile 'io.vertx:vertx-eventbus-bridge-client:1.0.1'
compile 'com.google.code.gson:gson:${gson.version}'
```

## Creating the Vert.x EventBus Client

There are 2 ways to create a Vert.x EventBus client, depending on what type of the bridge to interact with:

### Create a TCP event bus bridge client

```
EventBusClient tcpEventBusClient = EventBusClient.tcp();

// Create a bus client with specified host and port and TLS enabled
EventBusClientOptions options = new EventBusClientOptions()
  .setHost("127.0.0.1").setPort(7001)
  .setSsl(true)
  .setTrustStorePath("/path/to/store.jks")
  .setTrustStorePassword("change-it");
EventBusClient sslTcpEventBusClient = EventBusClient.tcp(options);
```

This example can be used to create a client to connect to a [Vert.x EventBus TCP bridge](https://vertx.io/docs/vertx-tcp-eventbus-bridge/java/).

With default options, the client will connect to `localhost:7000` via plain TCP. It is also possible to configure the client to connect to different host and port with TLS/SSL enabled for security TCP connections.

### Create a WebSocket SockJS bridge client

```
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

This example can be used to create a client to connect to a [SockJS EventBus Bridge](https://vertx.io/docs/vertx-web/java/#_sockjs_event_bus_bridge) using WebSocket.

With default options, the client will connect to `http://localhost/eventbus/websocket` using WebSocket. It is also possible to connect to different host and port with TLS/SSL enabled for security HTTP connections. `options.setWebSocketPath("/eventbus/message")` can be used to specify the WebSocket path corresponding to the one specified in the SockJS bridge.

## Communicate with the EventBus Bridge

No matter what type of the bridge it connects to, after connection, the client can be used to communicate with the bridge in the following ways:

> **📝注意:** Any of the following method leads to connect automatically if no connection established yet.

### Sending messages to an address of the EventBus

Messages can be sent to an address of the EventBus.

```
EventBusClient busClient = EventBusClient.tcp();

// Send a message to the bus, this will connect the client to the server
busClient.send("newsfeed", "Breaking news: something great happened");
```

### Sending messages to an address of the EventBus and expect a reply

Messages can be sent to an address of the EventBus with an expected reply handler specified.

```
busClient.request("newsfeed", "Breaking news: something great happened", new Handler<AsyncResult<Message<String>>>() {
  @Override
  public void handle(AsyncResult<Message<String>> reply) {
    System.out.println("We got the reply");
  }
});
```

### Publishing messages to an address of the EventBus

Messages can be published to an address of the EventBus.

```
busClient.publish("newsfeed", "Breaking news: something great happened");
```

### Creating a consumer and register it against an address

You can create a consumer and register it on an address of the EventBus, it will be called when there are any messages sent to that address.

```
busClient.consumer("newsfeed", new Handler<Message<String>>() {
  @Override
  public void handle(Message<String> message) {
    System.out.println("Received a news " + message.body());
  }
});
```

### Unregistering a consumer from its address

You can unregister the consumer from its address when it does not need to listen on that anymore.

```
consumer.unregister();
```

## Closing the Client

You can close the client to release the connection to the bridge server.

```
busClient.closeHandler(new Handler<Void>() {
  @Override
  public void handle(Void event) {
    System.out.println("Bus Client Closed");
  }
});
// Closes the connection to the bridge server if it is open
busClient.close();
```

## JSON format encoding

The client and the bridge exchange messages in a custom JSON format encoded using an implementation of `JsonCodec`. The client ships two `JsonCodec` implementations.

When creating a new `EventBusClient` instance without specifying `JsonCodec`, it tries to load `GsonCodec` first, if Gson is not in the classpath, it tries to load `JacksonCodec`, if FasterXML Jackson data-bind is not in classpath either, it fails to create the client instance.

You can specify a custom instance of `JsonCodec` when creating a new `EventBusClient` instance as well.

### GsonCodec

The `JsonCodec` implementation based on [Google Gson project](https://github.com/google/gson). The dependency of `com.google.code.gson:gson` is optional, you need to add this dependency explicitly to use this implementation.

### JacksonCodec

The `JsonCodec` implementation based on [FasterXML Jackson databind](https://github.com/FasterXML/jackson-databind). The dependency of `com.fasterxml.jackson.core:jackson-databind` is optional, you need to add this dependency explicitly to use this implementation.

## EventBus Client Options

There are 2 main options in Vert.x EventBus Client.

### EventBusClientOptions

The `EventBusClientOptions` is used to configure the EventBusClient during creation, it has the following properties:

- `host`: String, the host of the bridge to connect to, defaults to `localhost`.
- `port`: int, the port of the bridge to connect to, defaults to `-1`, which means `7000` for TCP bridge and `80` for WebSocket SockJS bridge.
- `webSocketPath`: String, the path connect the WebSocket client to, defaults to `/eventbus/websocket`. It is used only by the WebSocket EventBus Client.
- `maxWebSocketFrameSize`: int, the maximum WebSocket frame size, defaults to `65536`. It is used only by the WebSocket EventBus Client.
- `ssl`: boolean, indicates if SSL is enabled, defaults to `false`, which means SSL is not enabled.
- `trustStorePath`: String, the path of the trust store. It is used only when `ssl` is true.
- `trustStorePassword`: String, the password of the trust store. It is used only when `ssl` is true.
- `trustStoreType`: String, the trust store type, one of `jks`, `pfx`, `pem`, defaults to `jks`. It is used only when `ssl` is true.
- `verifyHost`: boolean, if hostname verification (for SSL/TLS) is enabled, defaults to `true`. It is used only when `ssl` is true.
- `trustAll`: boolean, if all servers (SSL/TLS) should be trusted, defaults to `false`. It is used only when `ssl` is true.
- `pingInterval`: int, ping interval, in milliseconds, defaults to `5000` ms.
- `autoReconnectInterval`: int, the length of the pause between auto reconnect tries, in milliseconds, defaults to `3000` ms.
- `maxAutoReconnectTries`: int, the maximum number of auto reconnect tries, defaults to `0`, which means no limit.
- `connectTimeout`: int, the connect timeout, in milliseconds, defaults to `60000` ms.
- `idleTimeout`: int, the idle timeout, in milliseconds, defaults to `0` which means no timeout.
- `autoReconnect`: boolean, whether auto reconnects is enabled, even if the client does not try to send a message, defaults to `true`.
- `proxyHost`: String, the proxy host.
- `proxyPort`: int, the proxy port.
- `proxyUsername`: String, the proxy username if the proxy requires authentication.
- `proxyPassword`: String, the proxy password if the proxy requires authentication.
- `proxyType`: ProxyType, one of `ProxyType.HTTP`, `ProxyType.SOCKS4`, `ProxyType.SOCKS5`.

### DeliveryOptions

`DeliveryOptions` is used when sending messages to the bridge, it has following properties:

- `timeout`: long, the send timeout, in milliseconds, defaults to `30 * 1000` ms. If there is no response received within the timeout the handler will be called with a failure.
- `headers`: Map, the headers sent to the bridge EventBus.
