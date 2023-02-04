# 使用Websockets和Vert.x进行实时竞价

>  翻译: 白石(https://github.com/wjw465150/Vert.x-Core-Manual)
>
> 原文地址: https://vertx.io/blog/real-time-bidding-with-websockets-and-vert-x/

在过去的几年中，用户对网络应用程序的期望发生了变化。在拍卖竞价过程中，用户不再需要按下刷新按钮来检查价格是否变化或拍卖是否结束。这使得竞标变得困难且不那么有趣。相反，他们希望在应用程序中实时看到更新。

在本文中，我想展示如何创建一个提供实时出价的简单应用程序。 我们将使用 WebSockets、[SockJS](https://github.com/sockjs/sockjs-client) 和 Vert.x。

我们将创建一个用于快速出价的前端，它与用 Java 编写并基于 Vert.x 的微服务进行通信。

## Websocket 是什么？

WebSocket 是异步、双向、全双工协议，它通过单个 TCP 连接提供通信通道。 通过 [WebSocket API](http://www.w3.org/TR/websockets/)，它提供了网站和远程服务器之间的双向通信。

WebSockets 解决了许多阻止 HTTP 协议适用于现代实时应用程序的问题。 不再需要像轮询这样的解决方法，这简化了应用程序架构。 WebSockets 不需要打开多个 HTTP 连接，它们减少了不必要的网络流量并减少了延迟。

## Websocket API 与 SockJS

遗憾的是，并非所有 Web 浏览器都支持 WebSocket。 但是，当 WebSockets 不可用时，有些库会提供回退。一个这样的库是 [SockJS](https://github.com/sockjs/sockjs-client)。 SockJS 从尝试使用 WebSocket 协议开始。但是，如果这不可能，它会使用[各种特定于浏览器的传输协议](https://github.com/sockjs/sockjs-client#supported-transports-by-browser-html-served-from-http-or-https)。 SockJS 是一个库，旨在在所有现代浏览器和不支持 WebSocket 协议的环境中工作，例如在限制性公司代理后面。 SockJS 提供了一个类似于标准 WebSocket API 的 API。

## 快速出价的前端

拍卖网页包含投标表格和一些简单的 JavaScript，它从服务中加载当前价格，打开到 SockJS 服务器的事件总线连接并提供投标。 我们出价的示例网页的 HTML 源代码可能如下所示：

```html
<h3>Auction 1</h3>
<div id="error_message"></div>
<form>
    Current price:
    <span id="current_price"></span>
    <div>
        <label for="my_bid_value">Your offer:</label>
        <input id="my_bid_value" type="text">
        <input type="button" onclick="bid();" value="Bid">
    </div>
    <div>
        Feed:
        <textarea id="feed" rows="4" cols="50" readonly></textarea>
    </div>
</form>
```

我们使用 `vertx-eventbus.js` 库来创建到事件总线的连接。 `vertx-eventbus.js` 库是 Vert.x 发行版的一部分。 `vertx-eventbus.js` 在内部使用 SockJS 库将数据发送到 SockJS 服务器。在下面的代码片段中，我们创建了一个事件总线实例。构造函数的参数是连接到事件总线的 URI。然后我们注册监听地址 `auction.<auction_id>` 的处理程序。每个客户端都可以在多个地址注册，例如 在拍卖 1234 中出价时，他们会在地址`auction.1234`等上注册。当数据到达处理程序时，我们会更改当前价格和拍卖网页上的出价提要。

```javascript
function registerHandlerForUpdateCurrentPriceAndFeed() {
    var eventBus = new EventBus('http://localhost:8080/eventbus');
    eventBus.onopen = function () {
        eventBus.registerHandler('auction.' + auction_id, function (error, message) {
            document.getElementById('current_price').innerHTML = JSON.parse(message.body).price;
            document.getElementById('feed').value += 'New offer: ' + JSON.parse(message.body).price + '\n';
        });
    }
};
```

任何尝试出价的用户都会向服务生成一个 PATCH Ajax 请求，其中包含有关拍卖中新出价的信息（请参阅下面的`bid()`函数）。在服务器端，我们将事件总线上的此信息发布给注册到某个地址的所有客户端。如果您收到`200 (OK)`以外的 HTTP 响应状态代码，则会在网页上显示一条错误消息。

```javascript
function bid() {
    var newPrice = document.getElementById('my_bid_value').value;

    var xmlhttp = (window.XMLHttpRequest) ? new XMLHttpRequest() : new ActiveXObject("Microsoft.XMLHTTP");
    xmlhttp.onreadystatechange = function () {
        if (xmlhttp.readyState == 4) {
            if (xmlhttp.status != 200) {
                document.getElementById('error_message').innerHTML = 'Sorry, something went wrong.';
            }
        }
    };
    xmlhttp.open("PATCH", "http://localhost:8080/api/auctions/" + auction_id);
    xmlhttp.setRequestHeader("Content-Type", "application/json");
    xmlhttp.send(JSON.stringify({price: newPrice}));
};
```

## 拍卖 服务

SockJS 客户端需要服务器端部分。现在我们要创建一个轻量级的 RESTful 拍卖服务。我们将以 JSON 格式发送和检索数据。让我们从创建一个 Verticle 开始。首先我们需要继承自 [`AbstractVerticle`](https://vertx.io/docs/apidocs/io/vertx/core/AbstractVerticle.html) 类并覆盖 `start` 方法。每个 Verticle 实例都有一个名为`vertx`的成员变量。这提供了对 Vert.x 核心 API 的访问。例如，要创建一个 HTTP 服务器，您可以在 `vertx` 实例上调用 `createHttpServer` 方法。要告诉服务器在端口 8080 上侦听传入请求，您可以使用 `listen` 方法。

我们需要一个带有路由的router。 router 接受 HTTP 请求并找到第一个匹配的路由。 路由可以有一个与之关联的处理程序，它接收请求（例如，匹配路径 `/eventbus/*` 的路由与 `eventBusHandler` 相关联）。

我们可以对请求做一些事情，然后结束它或将它传递给下一个匹配的处理程序。

如果您有很多处理程序，则将它们拆分为多个路由器是有意义的。

您可以通过在另一个路由器的挂载点挂载一个路由器来完成此操作（参见下面代码片段中与 `/api` 挂载点相对应的 `auctionApiRouter`）。

这是一个示例verticle：

```java
public class AuctionServiceVerticle extends AbstractVerticle {

    @Override
    public void start() {
        Router router = Router.router(vertx);

        router.route("/eventbus/*").handler(eventBusHandler());
        router.mountSubRouter("/api", auctionApiRouter());
        router.route().failureHandler(errorHandler());
        router.route().handler(staticHandler());

        vertx.createHttpServer().requestHandler(router::accept).listen(8080);
    }

    //…
}
```

现在我们将更详细地看一下。 我们将讨论 Verticle 中使用的 Vert.x 功能：错误处理程序、SockJS 处理程序、Body处理程序、共享数据、静态处理程序和基于方法、路径等的路由。

### 错误处理器

除了设置处理程序来处理请求外，您还可以为路由中的失败设置处理程序。 如果处理程序抛出异常，或者如果处理程序调用 [`fail`](https://vertx.io/docs/apidocs/io/vertx/ext/web/RoutingContext.html#fail-int-)方法。 为了呈现错误页面，我们使用 Vert.x 提供的错误处理程序：

```java
private ErrorHandler errorHandler() {
    return ErrorHandler.create();
}
```

### SockJS 处理程序

Vert.x 为 SockJS 处理程序提供了事件总线桥，它将服务器端 Vert.x 事件总线扩展到客户端 JavaScript。

配置网桥以告诉它哪些消息应该通过很容易。您可以使用 [`BridgeOptions`](https://vertx.io/docs/apidocs/io/vertx/ext/web/handler/sockjs/BridgeOptions.html) 指定允许哪些匹配项用于入站和出站流量 .如果消息是出站的，在将其从服务器发送到客户端 JavaScript 之前，Vert.x 将查看所有出站允许的匹配项。在下面的代码片段中，我们允许来自以“拍卖”开头的地址的任何消息。 并以数字结尾（例如 `auction.1`、`auction.100` 等）。

如果你想在桥上发生事件时得到通知，你可以在调用桥时提供一个处理程序。例如，创建新的 SockJS 套接字时将发生 SOCKET_CREATED 事件。该事件是 [`Future`](https://vertx.io/docs/apidocs/io/vertx/core/Future.html) 的实例。完成事件处理后，您可以使用“true”完成未来以启用进一步处理。

要启动桥，只需在 SockJS 处理程序上调用 `bridge` 方法：

```java
private SockJSHandler eventBusHandler() {
    BridgeOptions options = new BridgeOptions()
            .addOutboundPermitted(new PermittedOptions().setAddressRegex("auction\\.[0-9]+"));
    return SockJSHandler.create(vertx).bridge(options, event -> {
         if (event.type() == BridgeEventType.SOCKET_CREATED) {
            logger.info("A socket was created");
        }
        event.complete(true);
    });
}
```

### Body 处理器

BodyHandler 允许您检索请求正文、限制正文大小并处理文件上传。对于需要此功能的任何请求，Body处理程序应该在匹配的路由上。我们在投标过程中需要 BodyHandler（PATCH 方法请求 `/auctions/<auction_id>` 包含请求正文，其中包含有关拍卖中新报价的信息）。创建一个新的Body处理程序很简单：

```java
BodyHandler.create();
```

如果请求体是JSON格式，可以通过[`getBodyAsJson`](https://vertx.io/docs/apidocs/io/vertx/ext/web/RoutingContext.html#getBodyAsJson--)方法获取。

### 共享数据

共享数据包含允许您在同一 Vert.x 实例中或跨 Vert.x 实例集群的不同应用程序之间安全地共享数据的功能。 共享数据包括本地共享Map、分布式集群范围Map、异步集群范围锁和异步集群范围计数器。

为了简化应用程序，我们使用本地共享Map来保存有关拍卖的信息。 本地共享Map允许您在同一 Vert.x 实例中的不同 Verticle 之间共享数据。 以下是在拍卖服务中使用共享本地Map的示例：

```java
public class AuctionRepository {

    //…

    public Optional<Auction> getById(String auctionId) {
        LocalMap<String, String> auctionSharedData = this.sharedData.getLocalMap(auctionId);

        return Optional.of(auctionSharedData)
            .filter(m -> !m.isEmpty())
            .map(this::convertToAuction);
    }

    public void save(Auction auction) {
        LocalMap<String, String> auctionSharedData = this.sharedData.getLocalMap(auction.getId());

        auctionSharedData.put("id", auction.getId());
        auctionSharedData.put("price", auction.getPrice());
    }

    //…
}
```

如果您想将拍卖数据存储在数据库中，Vert.x 提供了一些不同的异步客户端来访问各种数据存储（MongoDB、Redis 或 JDBC 客户端）。

### 拍卖 API

Vert.x 允许您根据请求路径上的模式匹配将 HTTP 请求路由到不同的处理程序。 它还使您能够从路径中提取值并将它们用作请求中的参数。 每个 HTTP 方法都存在相应的方法。 第一个匹配的将收到请求。 此功能在开发 REST 样式的 Web 应用程序时特别有用。

要从路径中提取参数，您可以使用冒号字符来表示参数的名称。 正则表达式也可用于提取更复杂的匹配项。 通过模式匹配提取的任何参数都将添加到请求参数映射中。

[`Consumes`](https://vertx.io/docs/apidocs/io/vertx/ext/web/Route.html#consumes-java.lang.String-) 描述了处理程序可以使用哪些 MIME 类型。通过使用 [`produces`](https://vertx.io/docs/apidocs/io/vertx/ext/web/Route.html#produces-java.lang.String-) 您可以定义路由生成的 MIME 类型。在下面的代码中，路由将匹配任何带有匹配` application/json` 的 `content-type` 标头和 `accept` 标头的请求。

让我们看一个挂载在主路由器上的子路由器的例子，它是在 Verticle 的 `start` 方法中创建的：

```java
private Router auctionApiRouter() {
    AuctionRepository repository = new AuctionRepository(vertx.sharedData());
    AuctionValidator validator = new AuctionValidator(repository);
    AuctionHandler handler = new AuctionHandler(repository, validator);

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    router.route().consumes("application/json");
    router.route().produces("application/json");

    router.get("/auctions/:id").handler(handler::handleGetAuction);
    router.patch("/auctions/:id").handler(handler::handleChangeAuctionPrice);

    return router;
}
```

GET 请求返回拍卖数据，而 PATCH 方法请求允许您在拍卖中出价。让我们关注更有趣的方法，即 `handleChangeAuctionPrice`。用最简单的术语来说，该方法可能看起来像这样：

```java
public void handleChangeAuctionPrice(RoutingContext context) {
    String auctionId = context.request().getParam("id");
    Auction auction = new Auction(
        auctionId,
        new BigDecimal(context.getBodyAsJson().getString("price"))
    );

    this.repository.save(auction);
    context.vertx().eventBus().publish("auction." + auctionId, context.getBodyAsString());

    context.response()
        .setStatusCode(200)
        .end();
}
```

对 `/auctions/1` 的 `PATCH` 请求将导致变量 `auctionId` 获得值 1。我们在拍卖中保存一个新的报价，然后在事件总线上将这个信息发布给所有在客户端JavaScript地址上注册的客户端。完成 HTTP 响应后，您必须对其调用 `end` 函数。

### 静态 处理器

Vert.x 提供了处理静态网络资源的处理程序。提供静态文件的默认目录是 `webroot`，但可以对其进行配置。默认情况下，静态处理程序将设置缓存标头以使浏览器能够缓存文件。可以使用 [`setCachingEnabled`](https://vertx.io/docs/apidocs/io/vertx/ext/web/handler/StaticHandler.html#setCachingEnabled-boolean-) 方法禁用设置缓存标头。要从拍卖服务提供拍卖 HTML 页面、JS 文件（和其他静态文件），您可以创建一个静态处理程序，如下所示：

```java
private StaticHandler staticHandler() {
    return StaticHandler.create()
        .setCachingEnabled(false);
}
```

## 我们来Run一下！

[github](https://github.com/mwarc/simple-realtime-auctions-vertx3-example) 上提供了完整的应用程序代码。

克隆存储库并运行 `./gradlew run` 。

打开一个或多个浏览器并将它们指向“http://localhost:8080”。现在您可以在拍卖中出价：

![](使用Websockets和Vert.x进行实时竞价.assets/image-20230202111820409.png)

## 总结

本文概述了一个允许实时出价的简单应用程序。 我们创建了一个用 Java 编写并基于 Vert.x 的轻量级、高性能和可扩展的微服务。 我们讨论了 Vert.x 提供的内容，其中包括分布式事件总线和优雅的 API，可让您立即创建应用程序。



------------

<<<<<< [完]  >>>>>>
