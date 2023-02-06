# Vert.x 4 Web-Server Manual中文版

> 翻译: 白石(https://github.com/wjw465150/Vert.x-Core-Manual)

Vert.x-Web是基于Vert.x的，用于构建Web应用程序的一系列构建模块。 可以将其视为一把构建现代的， 可扩展的Web应用的瑞士军刀。

Vert.x Core 提供了一系列相对底层的功能用于操作HTTP， 对于一部分应用是足够的。

Vert.x Web 基于 Vert.x Core 提供了一系列更丰富的功能， 以便更容易地开发实际的 Web 应用。

它继承了 Vert.x 2.x 里的 [Yoke](http://pmlopes.github.io/yoke/) 的特点，灵感来自于 Node.js 的框架 [Express](http://expressjs.com/) 和 Ruby 的框架 [Sinatra](http://www.sinatrarb.com/) 等等。

Vert.x Web 的设计是强大的，非侵入式的, 并且是完全可插拔的。您可以只使用您需要的部分。 Vert.x Web 不是一个容器。

您可以使用 Vert.x Web 来构建经典的服务端 Web 应用， RESTful 应用， 实时的（服务端推送) Web 应用, 或任何您所能想到的 Web 应用类型。 应用类型的选择取决于您的喜好，而不是 Vert.x Web。

Vert.x Web 非常适合编写 RESTful HTTP 微服务， **但我们不强制** 您必须把应用实现成这样。

Vert.x Web 的一部分关键特性有：

- 路由(基于方法,路径等)
- 基于正则表达式的路径匹配
- 从路径中提取参数
- 内容协商
- 处理消息体
- 消息体的长度限制
- Multipart 表单
- Multipart 文件上传
- 子路由
- 支持本地会话和集群会话
- 支持 CORS(跨域资源共享)
- 错误页面处理器
- HTTP基本/摘要认证
- 基于重定向的认证
- 授权处理器
- 基于 JWT 的授权
- 用户/角色/权限授权
- 网页图标处理器
- 支持服务端模板渲染，包括以下开箱即用的模板引擎:
  - Handlebars
  - Jade
  - MVEL
  - Thymeleaf
  - Apache FreeMarker
  - Pebble
  - Rocker
- 响应时间处理器
- 静态文件服务，包括缓存逻辑以及目录监听
- 支持请求超时
- 支持 SockJS
- 桥接 Event-bus
- CSRF 跨域请求伪造
- 虚拟主机

Vert.x Web 的大部分特性是使用Handler实现的， 而且您随时可以实现您自己的处理器。 我们预计随着时间的推移会有更多的处理器被实现。

我们会在本手册里讨论所有上述的特性。

## 使用 Vert.x Web

在使用 Vert.x Web 之前，需要为您的构建工具在描述文件中添加 *dependencies* 依赖项：

- Maven (在您的 `pom.xml` 文件中):

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-web</artifactId>
 <version>4.3.5</version>
</dependency>
```

- Gradle (在您的 `build.gradle` 文件中)：

```groovy
dependencies {
 compile 'io.vertx:vertx-web:4.3.5'
}
```

### 开发模式

Vert.x Web 默认使用生产模式。 您可以通过设置 `dev` 值到下面的其中一个来切换开发模式：

- `VERTXWEB_ENVIRONMENT` 环境变量，或
- `vertxweb.environment` 系统属性

在开发模式：

- 模板引擎缓存被禁用
- `ErrorHandler` 不显示异常详细信息
- `StaticHandler` 不处理缓存头
- GraphQL开发工具被禁用

## 回顾 Vert.x Core 的 HTTP 服务端

Vert.x Web 使用并暴露了 Vert.x Core 的 API， 所以熟悉基于 Vert.x Core 编写 HTTP 服务端的基本概念是很有价值的。

Vert.x core HTTP文档对此进行了详细介绍。

这是一个用 Vert.x core 编写的 Hello World Web服务。暂不涉及Vert.x-Web：

```java
HttpServer server = vertx.createHttpServer();

server.requestHandler(request -> {

  // 所有的请求都会调用这个处理器处理
  HttpServerResponse response = request.response();
  response.putHeader("content-type", "text/plain");

  // 写入响应并结束处理
  response.end("Hello World!");
});

server.listen(8080);
```

我们创建了一个 HTTP 服务器实例，并设置了一个请求处理器。 所有的请求都会调用这个处理器处理。

当请求到达时，我们设置响应的 Content Type 为 `text/plain` ， 并写入了 `Hello World!` 然后结束了处理。

之后，我们告诉服务器监听 `8080` 端口(默认的主机名是 `localhost` )。

您可以执行这段代码，并打开浏览器访问 `http://localhost:8080` 来验证它是否如预期一样工作。

## Vert.x Web 的基本概念

高屋建瓴 (Here’s the 10000 foot view)：

`路由器 Router` 是 Vert.x Web 的核心概念之一。 它是一个维护了零或多个 `路由 Routes` 的对象。

路由器将传入的 `[Http服务器端请求 HttpServerRequest](https://vertx-china.github.io/docs/apidocs/io/vertx/core/http/HttpServerRequest.html)`暂停，以确保请求正文或任何协议升级不会丢失。 其次，它将找到匹配该请求的第一个路由，并将请求传递给该路由。

`Route` 可以持有一个与之关联的 *处理器 handler* 用于接收请求。 您可以通过该处理器对请求 *做一些处理*, 然后结束响应或者把请求传递给下一个匹配的处理器。

以下是一个简单的路由示例：

```java
HttpServer server = vertx.createHttpServer();

Router router = Router.router(vertx);

router.route().handler(ctx -> {

  // 所有的请求都会调用这个处理器处理
  HttpServerResponse response = ctx.response();
  response.putHeader("content-type", "text/plain");

  // 写入响应并结束处理
  response.end("Hello World from Vert.x-Web!");
});

server.requestHandler(router).listen(8080);
```

它做了和上文使用 Vert.x Core 实现的 hello world HTTP 服务基本相同的事情， 只是这一次换成了 Vert.x Web。

我们像以前一样创建一个HTTP服务器，然后我们创建一个 router。当我们完成这些之后， 我们创建一个简单的没有匹配条件的 route，它能够匹配 *全部* 到来的请求。

然后，我们为该路由指定一个处理器。该处理器将处理所有到来的请求。

传递给处理器的对象是 `RoutingContext` - 它包含标准的 Vert.x `HttpServerRequest` 和 `HttpServerResponse` 还有其他各种有用的东西，让使用Vert.x-Web变得更加简单。

每个被路由的请求， 都有一个唯一的路由上下文实例， 并将这个实例传递给该请求的所有处理器。

当我们建立了处理器之后，我们设置 HTTP 服务器的请求处理器， 使所有的请求都通过 `handle` 处理。

这些是最基本的，下面我们来看一下更多的细节：

## 处理请求并调用下一个处理器

当 Vert.x Web 决定路由一个请求到匹配的route， 它会调用对应处理器并将一个 `RoutingContext` 实例传递给它.route可以具有不同的处理器， 您可以叠加使用 `handler`

如果您不在处理器里结束这个响应，您需要调用 `next` 方法让其他匹配的 route 来处理请求(如果有)。

您不需要在处理器执行完毕时调用 `next` 。 您可以在之后需要的时间点调用它：

```java
Route route = router.route("/some/path/");
route.handler(ctx -> {

  HttpServerResponse response = ctx.response();
  // 开启分块响应，
  // 因为我们将在执行其他处理器时添加数据
  // 仅当有多个处理器输出时
  response.setChunked(true);

  response.write("route1\n");

  // 延迟5秒后调用下一匹配route
  ctx.vertx().setTimer(5000, tid -> ctx.next());
});

route.handler(ctx -> {

  HttpServerResponse response = ctx.response();
  response.write("route2\n");

  // 延迟5秒后调用下一匹配route
  ctx.vertx().setTimer(5000, tid -> ctx.next());
});

route.handler(ctx -> {

  HttpServerResponse response = ctx.response();
  response.write("route3");

  // 现在结束响应
  ctx.response().end();
});
```

在上述的例子中， `route1` 向响应里写入了数据， 5秒之后 `route2` 向响应里写入了数据， 再5秒之后 `route3` 向响应里写入了数据并结束了响应。

>**📝注意:** 所有发生的这些没有线程阻塞。

## 简单的响应

处理器非常强大， 因为它们允许您构建非常复杂的应用程序。 为了保证简单的响应， 例如直接从vert.x API返回异步响应， router 包含一个快捷的处理器：

1. 响应返回JSON。
2. 如果处理过程中发生错误，一个适当的错误会返回。
3. 如果序列化JSON中发生错误，一个适当的错误会返回。

```java
router
  .get("/some/path")
  // 这个处理器将保证这个响应会被序列化成json
  // content type被设置成 "application/json"
  .respond(
    ctx -> Future.succeededFuture(new JsonObject().put("hello", "world")));

router
  .get("/some/path")
  // 这个处理器将保证这个Pojo会被序列化成json
  // content type被设置成 "application/json"
  .respond(
    ctx -> Future.succeededFuture(new Pojo()));
```

不过， 如果提供的函数支持调用 `write` 或 `end` ， 您还可以将其用于非JSON响应。

```java
router
  .get("/some/path")
  .respond(
    ctx -> ctx
      .response()
      .putHeader("Content-Type", "text/plain")
      .end("hello world!"));

router
  .get("/some/path")
  // 在这种情况下，处理器确保连接被终止
  .respond(
    ctx -> ctx
      .response()
      .setChunked(true)
      .write("Write some text..."));
```

## 使用阻塞式处理器

某些时候您可能需要在处理器里执行一些需要阻塞 Event Loop 的操作， 比如调用某个传统的阻塞式 API 或者执行密集计算。

您不能在普通的处理器里执行这些操作， 因此我们提供了将route设置成阻塞式处理器的功能。

阻塞式处理器和普通处理器很像， 区别是 Vert.x 会使用 Worker Pool 中的线程而不是 Event Loop 线程来处理请求。

您可以使用 `blockingHandler` 方法来建立阻塞式处理器。 以下是例子：

```java
router.route().blockingHandler(ctx -> {

  // 执行某些同步的耗时操作
  service.doSomethingThatBlocks();

  // 调用下一个处理器
  ctx.next();

});
```

默认情况下，在同一个 Context (例如同一个 Verticle 实例) 上执行的所有阻塞式处理器是顺序的， 也就意味着只有一个处理器执行完了才会继续执行下一个。 如果您不关心执行的顺序， 并且不介意阻塞式处理器以并行的方式执行， 您可以在使用 `blockingHandler` 时，设置阻塞式处理器的 `ordered` 为 false。

> **📝注意:** 如果您需要在一个阻塞处理器中处理一个 multipart 类型的表单数据， 您需要首先使用一个非阻塞的处理器来调用 `setExpectMultipart(true)` 。以下是例子：
> 
> ```java
> router.post("/some/endpoint").handler(ctx -> {
>   ctx.request().setExpectMultipart(true);
>   ctx.next();
> }).blockingHandler(ctx -> {
>   // ... 执行某些阻塞操作
> });
> ```

## 基于精确路径的路由

可以将 `Route` 设置为根据需要所匹配的 URI。 在这种情况下它只会匹配路径一致的请求。

在下面这个例子中，处理器会被路径为 `/some/path/` 的请求调用。 我们会忽略结尾的 `/` ， 所以路径 `/some/path` 或者 `/some/path//` 的请求也是匹配的

```java
Route route = router.route().path("/some/path/");

route.handler(ctx -> {
  // 这个处理器会被以下路径的请求调用：

  // `/some/path/`
  // `/some/path//`
  //
  // 但不包括:
  // `/some/path` 路径末尾的斜线会被严格限制
  // `/some/path/subdir`
});

// 路径结尾没有斜线的不会被严格限制
// 这意味着结尾的斜线是可选的
// 无论怎样都会匹配
Route route2 = router.route().path("/some/path");

route2.handler(ctx -> {
  // 这个处理器会被以下路径的请求调用：

  // `/some/path`
  // `/some/path/`
  // `/some/path//`
  //
  // 但不包括:
  // `/some/path/subdir`
});
```

## 基于路径前缀的路由

您经常需要为所有以某些路径开始的请求设置 `Route` 。 您可以使用正则表达式来实现， 但更简单的方式是在声明 `Route` 的路径时使用一个 `*` 作为结尾。

在下面的例子中处理器会匹配所有 URI 以 `/some/path` 开头的请求。

例如 `/some/path/foo.html` 和 `/some/path/otherdir/blah.css` 都会匹配。

```java
Route route = router.route().path("/some/path/*");

route.handler(ctx -> {
  // 这个处理器处理会被所有以
  // `/some/path/` 开头的请求调用， 例如：

  // `/some/path/`
  // `/some/path/subdir`
  // `/some/path/subdir/blah.html`
  //
  // 但同时：
  // `/some/path` 最终的斜杆总是可选的并配有通配符，
  //              以保持与许多客户端库的兼容性。
  // 但 **不包括**：
  // `/some/patha`
  // `/some/patha/`
  // 等等……
});
```

也可以在创建 `Route` 的时候指定任意的路径：

```java
Route route = router.route("/some/path/*");

route.handler(ctx -> {
  // 这个处理器的调用规则和上面的例子一样
});
```

## 捕捉路径参数

可以通过占位符声明路径参数并在处理请求时通过 `pathParam` 。 方法获取

以下是例子

```java
router
  .route(HttpMethod.POST, "/catalogue/products/:productType/:productID/")
  .handler(ctx -> {

    String productType = ctx.pathParam("productType");
    String productID = ctx.pathParam("productID");

    // 执行某些操作...
  });
```

占位符由 `:` 和参数名构成。 参数名由字母，数字和下划线构成。 在某些情况下，这会受到一定限制，因而用户可以切换至包括2个额外字符“-”和“ $”的扩展名称规则。 扩展参数规则可用如下系统属性启用：

```
-Dio.vertx.web.route.param.extended-pattern=true
```

在上述例子中， 如果一个 POST 请求的路径为 `/catalogue/products/tools/drill123/` ， 那么会匹配这个 `Route` ， 并且会接收参数 `productType` 的值为 `tools` ，参数 `productID` 的值为 `drill123` 。

参数并不一定是路径段。例如，以下路径参数同样有效：

```java
router
  .route(HttpMethod.GET, "/flights/:from-:to")
  .handler(ctx -> {
    // 在处理发送至/flights/AMS-SFO的请求时，将会设置：
    String from = ctx.pathParam("from"); // AMS
    String to = ctx.pathParam("to"); // SFO
    // 记住一点，如果不切换至参数命名的 “extend/扩展” 模式的话，
    // 这将不会起作用。
    // 因为在那种情况下，“-” 符号并不被认为是分隔符，
    // 而是参数名的一部分。
  });
```

> **📝注意:**您也可以将 * 捕获为路径参数 * 。

## 基于正则表达式的路由

同样也可用正则表达式匹配路由的 URI 路径。

```java
Route route = router.route().pathRegex(".*foo");

route.handler(ctx -> {

  // 以下路径的请求都会调用这个处理器：

  // /some/path/foo
  // /foo
  // /foo/bar/wibble/foo
  // /bar/foo

  // 但不包括：
  // /bar/wibble
});
```

或者在创建 route 时指定正则表达式：

```java
Route route = router.routeWithRegex(".*foo");

route.handler(ctx -> {

  // 这个路由器的调用规则和上面的例子一样

});
```

## 通过正则表达式捕捉路径参数

您也可以通过正则表达式声明捕捉路径参数，以下是例子：

```java
Route route = router.routeWithRegex(".*foo");

// 这个正则表达式可以匹配路径类似于：
// `/foo/bar` 的请求
// `foo` 可以通过参数 param0 获取，`bar` 可以通过参数 param1 获取
route.pathRegex("\\/([^\\/]+)\\/([^\\/]+)").handler(ctx -> {

  String productType = ctx.pathParam("param0");
  String productID = ctx.pathParam("param1");

  // 执行某些操作……
});
```

在上述的例子中，如果一个请求的路径为 `/tools/drill123/`，那么会匹配这个 `route`， 并且会接收到参数 `productType` 的值为 `tools`，参数 `productID` 的值为 `drill123`。

捕捉（译者注：这里指的是捕捉参数这一行为）在正则表达式中用捕捉组表示（即用圆括号括住捕捉）

## 使用命名的捕捉组

使用序号参数名在某些场景下可能会比较麻烦。 亦可在正则表达式路径中使用命名的捕捉组。

```java
router
  .routeWithRegex("\\/(?<productType>[^\\/]+)\\/(?<productID>[^\\/]+)")
  .handler(ctx -> {

    String productType = ctx.pathParam("productType");
    String productID = ctx.pathParam("productID");

    // 执行某些操作……
  });
```

在上述的例子中，命名捕捉组将路径参数映射到同名的捕捉组中。

此外，您仍可以使用普通捕捉组访问组参数（例如：`params0, params1…`）

## 基于 HTTP 方法的路由

Route 默认会匹配所有的 HTTP 方法。

如果您只想让 route 匹配特定的 HTTP 方法，那么您可以使用 `method`

```java
Route route = router.route().method(HttpMethod.POST);

route.handler(ctx -> {

  // 所有的 POST 请求都会调用这个处理器

});
```

或者您可以在创建 Route 时和路径一起指定：

```java
Route route = router.route(HttpMethod.POST, "/some/path/");

route.handler(ctx -> {
  // 所有路径为 `/some/path/`
  // 的 POST 请求都会调用这个处理器
});
```

如果您想让 Route 指定 HTTP 方法，您也可以使用对应的 `get`， `post` 以及 `put` 等方法。 例如：

```java
router.get().handler(ctx -> {

  // 所有 GET 请求都会调用这个处理器

});

router.get("/some/path/").handler(ctx -> {

  // 所有路径以 `/some/path/` 开始的
  // GET 请求都会调用这个处理器

});

router.getWithRegex(".*foo").handler(ctx -> {

  // 所有路径以 `foo` 结尾的
  // GET 请求都会调用这个处理器

});
```

如果您想要让 route 匹配不止一个 HTTP 方法， 您可多次调用 `method` 方法：

```java
Route route = router.route().method(HttpMethod.POST).method(HttpMethod.PUT);

route.handler(ctx -> {

  // 所有 GET 或 POST 请求都会调用这个处理器

});
```

如果您的应用程序需要自定义 HTTP 动词，例如， `基于Web的分布式编写和版本控制（WebDAV）` 服务器中， 您可这样自定义动词：

```java
Route route = router.route()
  .method(HttpMethod.valueOf("MKCOL"))
  .handler(ctx -> {
    // 所有 MKCOL 请求都会调用这个处理器
  });
```

> **📝注意:** 请务必留意，像 rerouting 等特性不接受自定义 http 方法， 这些操作在检测到自定义动词时将会使用 `OTHER` 值以替代自定义名。

## 路由顺序

默认情况下Route按照其加入到Router的顺序进行匹配

路由器会逐级检查每条Route否匹配 如果匹配的话，该Route的handler将被调用。

如果这个handler接下来会调用 `next` 方法 则下一个匹配的路由(如果有的话)的handler将被调用。等等。

这里有一个例子来说明

```java
router
  .route("/some/path/")
  .handler(ctx -> {

    HttpServerResponse response = ctx.response();
    //启动response的分块响应功能，
    //因为我们将在多个handler中将添加数据
    //只需要一次，并且只在多个处理程序进行输出时才需要。
    response.setChunked(true);

    response.write("route1\n");

    //现在我们将调用下一个匹配的Route
    ctx.next();
  });

router
  .route("/some/path/")
  .handler(ctx -> {

    HttpServerResponse response = ctx.response();
    response.write("route2\n");

    //现在我们将调用下一个匹配的Route
    ctx.next();
  });

router
  .route("/some/path/")
  .handler(ctx -> {

    HttpServerResponse response = ctx.response();
    response.write("route3");

    // 现在我们结束响应
    ctx.response().end();
  });
```

在上面的例子里面，响应将包含这些内容

```
route1
route2
route3
```

对于任何以' /some/path '开头的请求，路由都是按照这个顺序被调用的

如果您想要覆盖默认的Route顺序，您可以使用 `order` 指定一个整数类型的值

Route在创建时被分配的顺序与它们被添加到Router的顺序相对应 第一个Route编号为 `0`，第二个Route编号为 `1`，以此类推。

通过给Route指定order您可以覆盖默认值，order可以为负值，举个例子 如果想要确保一个Route在order为 `0` 的Route之前执行则可以这样做

让我们更改route2的order值让他在route1之前执行

```java
router
  .route("/some/path/")
  .order(1)
  .handler(ctx -> {

    HttpServerResponse response = ctx.response();
    response.write("route1\n");

    // 现在调用下一个匹配的Route
    ctx.next();
  });

router
  .route("/some/path/")
  .order(0)
  .handler(ctx -> {

    HttpServerResponse response = ctx.response();
    // 启动response的分块响应功能，
    // 因为我们将在多个handler中将添加数据
    // 只需要一次，并且只在多个处理程序进行输出时才需要。
    response.setChunked(true);

    response.write("route2\n");

    // Now call the next matching route
    ctx.next();
  });

router
  .route("/some/path/")
  .order(2)
  .handler(ctx -> {

    HttpServerResponse response = ctx.response();
    response.write("route3");

    // Now end the response
    ctx.response().end();
  });
```

然后响应将包含以下内容

```
route2
route1
route3
```

如果两个匹配的Route都具有相同的order值，则按照他们添加的顺序被调用。

您也可以指定一个路由最后调用，参考 `last`

> **📝注意:** 只能在配置handler之前指定路由顺序！

## 基于请求MIME类型的路由

通过使用 `consumes`，您可以指定route将与的哪种请求MIME类型相匹配。

在这种情况下，请求将包含一个 `content-type` 请求头，指定请求体的MIME类型 这将匹配 `consumes` 指定的值。

基本上，`consumes` 用于描述这个handler将可以 *处理* 哪些MIME类型

匹配可以在精确的MIME类型匹配上进行：

```java
router.route()
  .consumes("text/html")
  .handler(ctx -> {

    //这个handler将会被
    //content-type 请求头设置为`text/html`的任意请求调用

  });
```

也可以指定多个精确的匹配：

```java
router.route()
  .consumes("text/html")
  .consumes("text/plain")
  .handler(ctx -> {

    // 这个handler将会被
    // content-type 请求头设置为`text/html`或者`text/plain`的任意请求调用

  });
```

支持子类型通配符的匹配：

```java
router.route()
  .consumes("text/*")
  .handler(ctx -> {

    //这个handler将会被
    //顶级类型为`text` 例如
    //content-type被设置为`text/html` 或者 `text/plain`的任意请求
    //匹配

  });
```

而且您也可以匹配顶级类型：

```java
router.route()
  .consumes("*/json")
  .handler(ctx -> {

    //这个handler将会被子类型为json的任意请求调用
    //例如content-type请求头设置为`text/json`或者
    // `application/json` 都会匹配

  });
```

如果您没有在consumer中指定一个 `/` ，它会假定您指的是子类型

## 基于客户端可接收的MIME类型的路由

HTTP `accept` 请求头用于表示客户端可以接受响应的MIME类型

一个 `accept` 请求头可以包含多个MIME类型，其之间用 ‘,’ 分割

MIME类型还可以附加一个 `q` 值，这表示如果有多个响应MIME类型与接受请求头匹配，则指定一个权重 q值是0到1.0之间的数字。 如果省略，则默认为1.0。

举个例子，下面的 `accept` 请求头则指定客户端将只能接收 `text/plain` 的MIME类型数据：

Accept: text/plain

客户端将接受 `text/plain` 或 `text/html` ，没有优先级：

Accept: text/plain, text/html

客户端将接受 `text/plain` 或 `text/html` ，但因为 `text/html` 有一个更高的 `q` 值(默认q=1.0)，所以客户端会优先接收 `text/html` ：

Accept: text/plain; q=0.9, text/html

如果服务端可以同时提供 text/plain 和 text/html，在这个例子里面它应当提供 text/html。

通过使用 `produces` 您可以决定Route可以产生那个(哪些) MIME 类型 例如 下面这个handler会产生一个MIME类型为 `application/json` 的响应。

```java
router.route()
  .produces("application/json")
  .handler(ctx -> {

    HttpServerResponse response = ctx.response();
    response.putHeader("content-type", "application/json");
    response.end(someJSON);

  });
```

在这种情况下，Route将匹配带有 `accept` 请求头且匹配 `application/json` 的任何请求。

这有一些 `accept` 请求头将如何匹配的例子：

Accept: application/json Accept: application/* Accept: application/json, text/html Accept: application/json;q=0.7, text/html;q=0.8, text/plain

您还可以将您的路由标记为生成多个MIME类型。如果是这样，那么使用 `getAcceptableContentType` 找出实际被接收的MIME类型。

```java
router.route()
  .produces("application/json")
  .produces("text/html")
  .handler(ctx -> {

    HttpServerResponse response = ctx.response();

    // 获取真正可接受的MIME类型
    String acceptableContentType = ctx.getAcceptableContentType();

    response.putHeader("content-type", acceptableContentType);
    response.end(whatever);
  });
```

在上面的例子中，如果您发送了一个带有以下 `accept` 请求头的请求：

Accept: application/json; q=0.7, text/html

然后路由将匹配，`acceptableContentType` 将包含 `text/html` 两个都是可以接受的但是它有更高的 `q` 值。

## 基于VirtualHost的路由

您可以配置一个 `Route` 将与请求主机名匹配

请求会根据 `Host` 请求头进行匹配检查，这种模式允许使用 `` **通配符 举个例子 ``**`.vertx.io` 或像 `www.vertx.io` 的完整域名。

```java
router.route().virtualHost("*.vertx.io").handler(ctx -> {
  //如果请求符合 *.vertx.io，则做一些事情
});
```

## 组合多个路由条件

您可以用不同方式组合上面所有的路由条件，举个例子：

```java
router.route(HttpMethod.PUT, "myapi/orders")
  .consumes("application/json")
  .produces("application/json")
  .handler(ctx -> {

    // 这个将匹配PUT方法，
    // 请求路径以"myapi/orders"开头且 content-type为"application/json"
    // 和 accept请求头为"application/json"的任意请求

  });
```

## 启用或者关闭Route

您可以通过 `disable` 关闭一个Route。一个被关闭的Route将会在匹配过程中被忽略。

您可以通过使用 `enable` 重新启用一个被关闭的Route。

## 对Forward的支持

您的应用可能由Proxy服务器所代理，比如 `HAProxy`。在此设置下工作时，获取客户端连接细节将不能正确返回预期结果 举个例子， 客户端的主机ip地址可能是代理服务器的ip地址，而不是实际客户端的ip地址

为了获取正确的连接信息，一个特殊的请求头 `Forward` 已经被标准化，以包括正确的 信息。虽然这个标准不是很老，但是很多代理已经使用其他请求头，通常 以前缀 `X-Forward` 开头。Vert.x web允许使用和解析这些请求头信息，但不是默认的。

这些请求头在默认情况下禁用的原因是为了防止恶意应用程序伪造它们的来源 和隐藏他们真正的来源。

如前所述，默认情况下forward是禁用的，要启用，您应该使用：

```java
router.allowForward(AllowForwardHeaders.FORWARD);

// 您现在可以允许解析forward请求头了
// 在这种情况下，只会考虑"X-Forward"请求头
router.allowForward(AllowForwardHeaders.X_FORWARD);

//我们现在可以允许forward请求头解析
//在这个例子中是"Forward"请求头和"X-Forward"请求头
//将被解析，但是来自"Forward"的值优先
//这意味着如果发生冲突(2个请求头的值相同)
//接收"Forward"值，忽略"X-Forward"。
router.allowForward(AllowForwardHeaders.ALL);
```

同样的规则也适用于显式禁用请求头的解析:

```java
router.allowForward(AllowForwardHeaders.NONE);
```

要阅读更多关于请求头格式的格式，请查看

- https://tools.ietf.org/html/rfc7239#section-4
- https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Forwarded

在幕后，这个特性所做的是改变您的连接(HTTP或WebSocket)的以下值：

- protocol
- host name
- host port

## 上下文数据

您可以使用 `RoutingContext` 保存任何 在请求生命周期内您想在多个handler之间共享的数据

下面是一个例子，其中一个handler在上下文数据中设置一些数据，然后一个后续的处理程序获取它：

您可以使用 `put` 添加任何对象， 然后使用 `get` 获取任何来自于上下文的对象

一个发送到 `/some/path/other` 的请求将匹配这两个Route。

```java
router.get("/some/path").handler(ctx -> {

  ctx.put("foo", "bar");
  ctx.next();

});

router.get("/some/path/other").handler(ctx -> {

  String bar = ctx.get("foo");
  // 用bar对象做一些事情
  ctx.response().end();

});
```

您也可以使用 `data` 获取全部的上下文数据 map

## 元数据

虽然上下文数据已经允许您在请求-响应生命周期内存储数据，但有时重要的是能获取 运行时的元数据。例如，为了构建 API 文档，或保存一个给定的路由规则 的特定配置。

元数据的作用方式与上下文数据类似。您可以通过其 `Map` 类型接口或使用 `Router` 和 `Route` 接口上的特定 getter 和 setter 来获取数据

```java
router
  .route("/metadata/route")
  .putMetadata("metadata-key", "123")
  .handler(ctx -> {
    Route route = ctx.currentRoute();
    String value = route.getMetadata("metadata-key"); // 123
    // 结束请求，并返回 123
    ctx.end(value);
  });
```

## 帮手函数

虽然路由上下文将允许您获取基础请求和响应对象， 但有时如果有一些捷径可以帮助您完成常见的任务，您的工作效率会更高。 有几个帮手存在于上下文中可以便于完成这项任务。

提供一个“附件”，附件是一种响应，它将触发浏览器打开配置为处理特定MIME类型的操作系统应用程序。 假设您正在生成一个PDF文件：

```java
ctx
  .attachment("weekly-report.pdf")
  .end(pdfBuffer);
```

执行重定向到另一个页面或主机。一个例子是重定向到应用程序的HTTPS变体：

```java
ctx.redirect("https://securesite.com/");

//对于目标为“back”有一个特殊的处理。
//在这种情况下，重定向会将用户发送到
//referrer url或 "/"如果没有referrer。

ctx.redirect("back");
```

向客户端发送一个JSON响应：

```java
ctx.json(new JsonObject().put("hello", "vert.x"));
// 也可以用于数组
ctx.json(new JsonArray().add("vertx").add("web"));
//或者用于任意对象
//其将根据运行时的json编码器进行转化
ctx.json(someObject);
```

常规的content-type校验：

```java
ctx.is("html"); // => true
ctx.is("text/html"); // => true

//当content-type为application/json时
ctx.is("application/json"); // => true
ctx.is("html"); // => false
```

验证有关缓存头和last modified/etag的当前值的请求是否"新鲜"

```java
ctx.lastModified("Wed, 13 Jul 2011 18:30:00 GMT");
// 现在将使用它来验证请求的新鲜度
if (ctx.isFresh()) {
  //客户端缓存值是新鲜的，
  //也许我们可以停止并返回304？
}
```

和其他一些简单的无需解释的快捷方式

```java
ctx.etag("W/123456789");

// 设置last modified 的值
ctx.lastModified("Wed, 13 Jul 2011 18:30:00 GMT");

// 便捷结束响应
ctx.end();
ctx.end("body");
ctx.end(buffer);
```

## 重新路由

到目前为止，所有路由机制都允许您以顺序的方式处理请求， 但是有时您可能希望后退。由于上下文没有公开有关上一个或下一个handler的任何信息， 主要是因为此信息是动态的， 因此有一种方法可以从当前路由器的开头重新启动整个路由。

```java
router.get("/some/path").handler(ctx -> {

  ctx.put("foo", "bar");
  ctx.next();

});

router
  .get("/some/path/B")
  .handler(ctx -> ctx.response().end());

router
  .get("/some/path")
  .handler(ctx -> ctx.reroute("/some/path/B"));
```

因此，从代码中您可以看到，如果请求首先到达 `/some/path` 且最先向上下文中添加一个值， 然后移至下一个handler，该处理程序将请求重新路由至 `/some/path/B`，从而终止请求。

您可以基于新路径或基于新路径和方法重新路由。 但是请注意，基于方法的重新路由可能会引入安全性问题，因为例如通常安全的GET请求可能会变为DELETE。

失败处理程序上也允许重新路由，但是由于重新路由的性质，当被调用时，当前状态代码和失败原因将会重置 为了在需要时重新路由的处理程序应生成正确的状态代码， 例如：

```java
router.get("/my-pretty-notfound-handler").handler(ctx -> ctx.response()
  .setStatusCode(404)
  .end("NOT FOUND fancy html here!!!"));

router.get().failureHandler(ctx -> {
  if (ctx.statusCode() == 404) {
    ctx.reroute("/my-pretty-notfound-handler");
  } else {
    ctx.next();
  }
});
```

应当清楚的是，重新路由可以在 `路径` 上使用，因此，如果您需要在重新路由之间保留或添加状态，则应使用 `RoutingContext` 对象。 例如，您想使用额外的参数重新路由到新路径：

```java
router.get("/final-target").handler(ctx -> {
  // 在这里做一些事情
});

// 将会带着查询字符串重定向到 /final-target
router.get().handler(ctx -> ctx.reroute("/final-target?variable=value"));

// 一个更安全的方法是将变量添加至上下文中
router.get().handler(ctx -> ctx
  .put("variable", "value")
  .reroute("/final-target"));
```

重新路由也会重新解析查询参数。请注意，先前的查询参数将被丢弃。 该方法还将静默地丢弃并忽略路径中的任何html片段。 这是为了使重新路由的语义在常规请求和重新路由之间保持一致。

如果需要将更多信息传递给新请求， 则应使用在HTTP事务的整个生命周期中保留的上下文。

## 子路由器

有时，如果您有很多handler，则可以将它们拆分为多个Router。 如果要在不同路径根的不同应用程序中重用一组handler，这也很有用。

为此，您可以将Router挂载在另一个Router *挂载点* 上。安装的Router称为 *子路由器*。 子路由器可以挂载其他子路由器，因此您可以根据需要拥有多个级别的子路由器。

让我们看一个简单的子路由挂载在其他路由上面的例子

该子路由器将维护简单的虚构REST API对应的handler。我们将其挂载在另一个路由器上。 其未显示REST API的完整实现。

这是一个子路由器

```java
Router restAPI = Router.router(vertx);

restAPI.get("/products/:productID").handler(ctx -> {

  // TODO 处理产品查找
  ctx.response().write(productJSON);

});

restAPI.put("/products/:productID").handler(ctx -> {

  // TODO 添加一个新产品
  ctx.response().end();

});

restAPI.delete("/products/:productID").handler(ctx -> {

  // TODO 删除一个产品
  ctx.response().end();

});
```

如果将此路由器用作顶级路由器， 则对诸如 `/products/product1234` 之类的url的GET/PUT/DELETE请求将调用该API。

但是，假设我们已经有另一个路由器描述的网站：

```java
Router mainRouter = Router.router(vertx);

// 处理静态资源
mainRouter.route("/static/*").handler(myStaticHandler);

mainRouter.route(".*\\.templ").handler(myTemplateHandler);
```

现在，我们可以将子路由器挂载在主路由器上，挂载点在本例中为 `/productsAPI`。

```java
mainRouter.route("/productsAPI/*")
  .subRouter(restAPI);
```

这意味着现在可以通过以下路径访问REST API：`/productsAPI/products/product1234`。

在使用子路由器之前，必须满足一些规则

- 路由路径必须以通配符结尾。
- 允许使用参数，但不能完全的使用正则表达式模式。
- 在此调用之前或之后，只能注册1个处理程序（但它们可以在同一路径的新路由对象上注册）
- 每个路径对象仅1个路由器

验证是在将路由器添加到http服务器时进行的。这意味着由于子路由器的动态特性，在构建期间无法获得任何验证错误。 它们取决于要验证的上下文。

## 本地化

Vert.x Web解析 `Accept-Language` 请求头， 并提供一些帮助方法来确定哪个是客户端的首选语言环境或按质量排序的首选语言环境列表。

```java
Route route = router.get("/localized").handler(ctx -> {
  // 尽管通过switch运行循环可能看起来很奇怪，
  // 但我们可以确保在使用用户语言进行响应时，
  // 保留了语言环境的优先顺序。
  for (LanguageHeader language : ctx.acceptableLanguages()) {
    switch (language.tag()) {
      case "en":
        ctx.response().end("Hello!");
        return;
      case "fr":
        ctx.response().end("Bonjour!");
        return;
      case "pt":
        ctx.response().end("Olá!");
        return;
      case "es":
        ctx.response().end("Hola!");
        return;
    }
  }
  // 我们不知道用户使用的语言，因此请告知
  ctx.response().end("Sorry we don't speak: " + ctx.preferredLanguage());
});
```

主要方法 `acceptableLanguages` 将返回用户理解的语言环境的有序列表， 如果您只对用户偏爱的语言环境感兴趣，则使用这个帮手方法 `preferredLanguage` 其将返回列表的第1个元素， 如果用户未提供语言环境，则返回"null"。

## 路由匹配失败

如果没有路由符合任何特定请求，则Vert.x-Web将根据匹配失败发出错误消息

- 404 没有匹配的路径
- 405 路径匹配但是请求方法不匹配
- 406 路径匹配，请求方法匹配但是它无法提供内容类型与 `Accept` 请求头匹配的响应
- 415 路径匹配，请求方法匹配但是它不能接受 `Content-type`
- 400 路径匹配，请求方法匹配但是它接收空方法体

您可以使用 `errorHandler` 手动管理这些错误。

## 错误处理

除了设置处理程序以处理请求之外，您还可以设置处理程序以处理路由过程中的错误

Failure 处理器与普通处理器（handler）有完全相同的路由匹配条件

例如，您可以提供一个错误处理器，该处理程序仅处理某些路径或某些HTTP方法上的错误。

这使您可以为应用程序的不同部分设置不同的错误处理器。

这是一个示例错误处理器，仅在将GET请求路由到以 `/somepath/` 开头的路径时发生的失败时 才会调用该错误处理器：

```java
Route route = router.get("/somepath/*");

route.failureHandler(ctx -> {

  // 以 '/somepath/'
  // 开头的路径时发生的错误时
  // 这个将会被调用

});
```

如果handler引发异常，或者如果handler调用 `fail` 并指定HTTP状态代码来故意发出失败信号， 则会触发错误处理路由。

如果从handler中捕获到异常，则将导致失败，并发出状态代码 `500`。

处理错误时，将向故障处理器传递路由上下文，该路由上下文还允许获取故障或故障代码， 以便错误处理器可以使用它来生成失败响应。

```java
Route route1 = router.get("/somepath/path1/");

route1.handler(ctx -> {

  // 让我们抛出一个RuntimeException
  throw new RuntimeException("something happened!");

});

Route route2 = router.get("/somepath/path2");

route2.handler(ctx -> {

  // 这是一个故意使请求传递状态码的错误
  // 比如 403-访问被拒绝
  ctx.fail(403);

});

// 定义一个错误处理器
// 它将会被上面handler里面发生的任何异常触发
Route route3 = router.get("/somepath/*");

route3.failureHandler(failureRoutingContext -> {

  int statusCode = failureRoutingContext.statusCode();

  // RuntimeException的状态码将为500
  // 或403，表示其他失败
  HttpServerResponse response = failureRoutingContext.response();
  response.setStatusCode(statusCode).end("Sorry! Not today");

});
```

如果在错误处理器执行时在状态消息头中出现非法字符，则发生错误， 那么原始状态消息将从错误代码更改为默认消息。 这是保持HTTP协议语义正常工作的一种折衷， 而不是在没有正确完成协议的情况下突然崩溃并关闭套接字。

## 请求体处理

`请求体处理器 BodyHandler` 允许您获取请求体， 限制请求体大小以及处理文件上传。

您应该确保对于任何需要此功能的请求，请求体处理器都能正确匹配路由。

为使用该处理器应尽快将其安装。 处理器安装之后，便会恢复处理 `Http服务器端请求 HttpServerRequest`。

```java
router.route().handler(BodyHandler.create());
```

| 注意 | 上传可能是DDoS攻击的来源，为了减少攻击面，建议 设置合适的 `setBodyLimit` (例如 10MB的上传限制 或者 100KB的json大小限制). |
| ---- | ------------------------------------------------------------ |
|      |                                                              |

### 获取请求体

如果您知道请求体是个JSON，然后您可以使用 `getBodyAsJson` ， 如果您知道他是个字符串，您可以使用 `getBodyAsString`， 或者使用 `getBody` 获取buffer

### 限制请求体大小

为了限制请求体大小，创建请求体处理器然后使用 `setBodyLimit` 指定最大请求体大小 这对于防止过大请求体导致耗尽内存很有用

如果尝试发送大于最大大小的请求体， 则会发送HTTP状态代码413- `Request Entity Too Large`。

默认情况下没有最大请求体大小限制

### 合并表单属性

默认情况下，请求体处理器会将所有表单属性合并到请求参数中。 如果您不想这样做，您可以通过 `setMergeFormAttributes` 关闭这个功能

### 处理文件上传

请求体处理器还用于处理多部分文件的上传。

如果请求体处理器处于与请求匹配的路由上， 则任何文件上传将自动流式传输到文件上传目录，该目录默认情况下为 `file-uploads`。

每一个文件都会自动生成一个文件名，而且文件上传将通过 `fileUploads` 在路由上下文中可用。

这是一个例子：

```java
router.route().handler(BodyHandler.create());

router.post("/some/path/uploads").handler(ctx -> {

  List<FileUpload> uploads = ctx.fileUploads();
  // 使用uploads做一些事情

});
```

每个文件上传均由一个 `FileUpload` 实例描述， 该实例允许访问各种属性，例如名称，文件名和大小。

## 处理cookie

Vert.x-Web 有开箱即用的cookie支持

### 操作 cookies

您可以使用 `getCookie` 按名获取一个cookie 或者使用 `cookieMap` 获取整个set集合。

使用 `removeCookie` 移除一个cookie。

使用 `addCookie`，添加一个cookie。

当写入响应头后，这组Cookie会自动写回到响应中， 以便浏览器可以存储它们。

cookie被 `Cookie` 实例所描述。 它允许您获取名称、 值、域、路径和其他cookie属性

这是一个查询并添加cookie的例子：

```java
Cookie someCookie = ctx.request().getCookie("mycookie");
String cookieValue = someCookie.getValue();

// 使用cookie做一些事情

// 添加一个cookie——它将自动写回到响应中
ctx.response().addCookie(Cookie.cookie("othercookie", "somevalue"));
```

## 处理session

Vert.x-Web 提供了开箱即用的 session 支持

session 存活在在浏览器会话周期的 HTTP 请求之间， 它给予了您一个可以储存 session 作用域信息的地方，比如购物车

Vert.x-Web 使用 session cookie 来识别 session 这个 session cookie 是临时的，而且当其关闭的时候您的浏览器会将其删除

我们并不会将您 session 中的真实数据放到 session cookie 中——cookie 只是简单的使用标记符在服务器上寻找实际的 session 这个标记符是一个使用安全随机数生成的随机 UUID 所以它应该实际上是不可被推测出来的

cookie 在 HTTP 请求和响应中通过网络传递，因此明智的做法是在使用会话时始终使用 HTTPS 协议。 如果您尝试通过直接 HTTP 使用会话，则 Vert.x 会警告您。

为了启用您应用程序中的 session， 您必须在应用程序逻辑之前的匹配路由上具有一个 `SessionHandler`

这个 session 处理器用于处理 session cookie 的生成和寻找对应的 session 所以您无需自己去做这些事情

在响应头发回给客户端之后，session 中的数据会自动地保存在 session 储存器中 但是请注意，因为这个机制， 它并不保证这个数据在客户端收到响应之前完全保留 在这个场景中您可以强制刷新一下 除非刷新操作失败，否则这将禁用自动保存过程。 这样可以在完成响应之前控制状态，例如：

```java
ChainAuthHandler chain =
  ChainAuthHandler.any()
    .add(authNHandlerA)
    .add(ChainAuthHandler.all()
      .add(authNHandlerB)
      .add(authNHandlerC));

// 保护您的路由
router.route("/secure/resource").handler(chain);
// 您的应用
router.route("/secure/resource").handler(ctx -> {
  // do something...
});
```

默认情况下，Vert.x session处理器的状态使用cookie存储session ID。session ID是一个唯一的字符串，用于识别两次访问之间的单个访问者 。 但是，如果客户端的网络浏览器不支持cookie或访问者已在网络浏览器的设置中禁用了cookie，则我们无法在客户端的计算机上存储session ID。 在这种情况下，将为每个请求创建新的会话。 这种行为是无用的，因为我们无法记住两个请求之间特定访问者的信息。 我们可以说，默认情况下，如果浏览器不支持cookie，则session将无法工作。

Vert.x Web支持不使用cookie的session，称为"无cookie"session。 作为替代，Vert.x Web可以将session ID嵌入页面URL内。这样，所有页面链接都将包含session ID字符串。 当访问者单击其中的某些链接时，它将从页面URL读取session ID，因此我们不需要cookie支持即可进行功能性session。

启动无cookies session

```java
router.route()
  .handler(SessionHandler.create(store).setCookieless(true));
```

知道在这种情况下session ID会被应用传递给最终的用户这一点非常重要，通常来讲通过把他渲染到 HTML 页面或者脚本上 有一些非常重要的规则，session ID 会由 `/optional/path/prefix'('sessionId')'/path/suffix` 上的以下模式标识。

举个例子，给出一个路径 `http://localhost:2677/WebSite1/(S(3abhbgwjg33aqrt3uat2kh4d))/api/` , session ID在这种情况下，是 `3abhbgwjg33aqrt3uat2kh4d`

如果两个用户共享一个相同的 session ID， 他们也将共享同样的 session 变量，而且网站会将其认为是同一个访问者。 如果 session 被用于储存私密或敏感的数据，或者允许访问网站的受限区域，这将是一个安全危机， 当使用 cookie 时，session ID可以通过 SSL 和标记 cookie 为 secure 进行保护。 但是在无 cookie session的情况下，session id 是 URL 的一部分，而且这非常容易受到攻击

### session储存

创建一个session处理器，您需要一个session储存器实例。 这个session储存器是一个可以为您的应用储存实际session的对象

session存储器负责保存安全的伪随机数生成器，以保证安全的session ID。 此PRNG独立于储存器，这意味着从储存器A获得的会话ID不能获取储存器B的会话ID， 因为它们具有不同的种子和状态。

默认情况下，PRNG使用混合模式，生成种子的时候会阻塞，生成时并不阻塞 PRNG也将每5分钟重新设置64位新的熵。而且这也可以通过系统属性进行设置

- io.vertx.ext.auth.prng.algorithm 比如: SHA1PRNG
- io.vertx.ext.auth.prng.seed.interval 比如: 1000 (every second)
- io.vertx.ext.auth.prng.seed.bits 比如: 128

除非您注意到PRNG算法会影响应用程序的性能， 否则大多数用户都不需要配置这些值。

Vert.x-Web具有两个开箱即用的sesion存储实现，如果您愿意，也可以编写自己的会话存储

这些实现应遵循 `ServiceLoader` 约定， 所有从类路径下运行时可以用的储存都将被暴露出来 当有多个实现可用时，第一个可以实例化并成功配置的实现将成为默认设置。 如果没有可用的，则默认值取决于创建Vert.x的模式。 如果集群模式可用，则默认配置为为集群储存，否则为本地存储。

#### 本地session储存

通过这个储存器，session可以在内存中本地化储存，而且只在这个实例中可用

如果只有一个Vert.x实例正在应用程序中使用粘性session， 并且已将负载均衡器配置为始终将HTTP请求路由到同一Vert.x实例，则此存储是合适的。

如果您不能确保所有请求都将在同一服务器上终止，请不要使用此存储 因为服务器可能会在不知道对应session的情况下，终结您的请求

本地session储存器通过shared local map实现，而且会由回收器清理过期的session

回收间隔将可以用json信息进行设置，它所对应的key值为 `reaperInterval` .

下面是一个创建本地 `session储存器` 的例子

```java
SessionStore store1 = LocalSessionStore.create(vertx);

// 创建一个指定local shared map名的本地session储存
// 如果您有多个应用在同一个Vert.x 实例中而且您想使用为不同的应用不同的map，
// 这将非常有用
SessionStore store2 = LocalSessionStore.create(
  vertx,
  "myapp3.sessionmap");

// 创建一个本地session储存器，
// 其制定了local shared map名和设置了10s的清理周期用于清理过期session
SessionStore store3 = LocalSessionStore.create(
  vertx,
  "myapp3.sessionmap",
  10000);
```

#### 集群session储存器

通过这个储存器，session可以储存在分布式map中，其可以跨Vert.x集群使用

如果您 *不* 使用粘性会话，即您的负载均衡器将来自同一浏览器的不同请求分发到不同的服务器 ，则此存储是合适的。

您的session在集群的任意一个节点只要通过这个储存器都是可用的

使用集群session储存器，您需要保证您的Vert.x实例是集群的

这有一些创建集群 `sessionstore` 的例子

```java
Vertx.clusteredVertx(new VertxOptions(), res -> {

  Vertx vertx = res.result();

  // 使用默认配置创建集群session储存
  SessionStore store1 = ClusteredSessionStore.create(vertx);

  // 指分布式map的名字创建集群session存储器
  // 如果您有多个应用在同一个集群中而且您想使用为不同的应用不同的map，
  // 这将非常有用
  SessionStore store2 = ClusteredSessionStore.create(
    vertx,
    "myclusteredapp3.sessionmap");
});
```

#### 其他储存方式

其他储存方式也是可行的,这些储存可通过导入正确的jar包到您的项目里面使用。 一个这种储存的例子就是cookie储存器。 此存储的优点是不需要后端或服务器端状态，这在某些情况下很有用， **但是** 所有会话数据都将通过Cookie发送回客户端， 因此， 如果您需要存储私有信息，则不应使用。

如果您使用粘性会话，则此存储是合适的， 即您的负载均衡器将来自同一浏览器的不同请求分发到不同的服务器。

由于会话存储在Cookie中，因此会话也可以在服务器崩溃后幸免

第二个已知的实现是Redis session储存器。 该储存器的工作方式与普通集群存储区相同，然而顾名思义，它使用 redis 作为后端将会话数据集中存储。

此外，亦提供 Infinispan 会话存储（详情如下）。

这些储存器可在这些坐标上可用：

- groupId: `io.vertx`
- artifactId: `vertx-web-sstore-{cookie|redis|infinispan}`

##### Infinispan 网络会话存储

依赖于 [Infinispan](https://infinispan.org/) Java 客户端的一种 `SessionStore` 实现。

> **😎警告:** 此模块具有 *Tech Preview* 状态，这意味着 API 可能会在后续版本中更改。

###### 由此开始

要使用此模块，请将以下内容添加到 Maven POM 文件的 *dependencies* 部分中：

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-web-sstore-infinispan</artifactId>
 <version>4.3.5</version>
</dependency>
```

或者，您使用的是 Gradle：

```groovy
compile 'io.vertx:vertx-web-sstore-infinispan:4.3.5'
```

###### 使用

如果这个会话存储是您所有依赖项中唯一的一项，那么您可以用一种通用的方式将其初始化：

```java
JsonObject config = new JsonObject()
  .put("servers", new JsonArray()
    .add(new JsonObject()
      .put("host", "server1.datagrid.mycorp.int")
      .put("username", "foo")
      .put("password", "bar"))
    .add(new JsonObject()
      .put("host", "server2.datagrid.mycorp.int")
      .put("username", "foo")
      .put("password", "bar"))
  );
SessionStore store = SessionStore.create(vertx, config);
SessionHandler sessionHandler = SessionHandler.create(store);
router.route().handler(sessionHandler);
```

或者，使用明确的 `InfinispanSessionStore` 类型。

```java
JsonObject config = new JsonObject()
  .put("servers", new JsonArray()
    .add(new JsonObject()
      .put("host", "server1.datagrid.mycorp.int")
      .put("username", "foo")
      .put("password", "bar"))
    .add(new JsonObject()
      .put("host", "server2.datagrid.mycorp.int")
      .put("username", "foo")
      .put("password", "bar"))
  );
InfinispanSessionStore store = InfinispanSessionStore.create(vertx, config);
SessionHandler sessionHandler = SessionHandler.create(store);
router.route().handler(sessionHandler);
```

###### 配置

配置条目（entry）

根条目是：

- `servers`：强制/必填的，服务器定义的 JSON 数组（见下文）
- `cacheName`：可选的，用于存储会话数据的缓存名称（默认为 `vertx-web.sessions`）
- `retryTimeout`：可选的，会话处理程序从存储中检索值时所使用的重试超时时间（以毫秒为单位）（`5000`）

用于服务器定义的条目是：

- `uri` ：可选的，一个 [Hot Rod URI](https://infinispan.org/blog/2020/05/26/hotrod-uri/)
- `host`：可选的（默认为 `localhost`）
- `port`：可选的（默认为 `11222`）
- `clientIntelligence`：可选的（以下值之一 `BASIC`，`TOPOLOGY_AWARE`，`HASH_DISTRIBUTION_AWARE`）
- `username`：强制的
- `password`：强制的
- `realm`：可选的（默认为 `default`）
- `saslMechanism`：可选的（默认为 `DIGEST-MD5`）
- `saslQop`：可选的（以下值之一 `AUTH`，`AUTH_INT`，`AUTH_CONF`）

> **🔔重要:** 如果设置了 `uri` 条目，则其他条目将被忽略。

自定义 Infinispan 客户端

对于高级配置要求， 您可以提供自定义的 [`RemoteCacheManager`](https://docs.jboss.org/infinispan/12.1/apidocs/org/infinispan/client/hotrod/RemoteCacheManager.html)：

```java
InfinispanSessionStore sessionStore = InfinispanSessionStore.create(vertx, config, remoteCacheManager);
```

### 创建session处理器

一旦您创建好session储存器，您就可以开始创建session处理器了，并且把他添加到Route中。 那您应该确保将会话处理程序路由到应用程序处理程序之前。

这里有个例子

```java
Router router = Router.router(vertx);

//使用默认配置创建一个集群session储存器
SessionStore store = ClusteredSessionStore.create(vertx);

SessionHandler sessionHandler = SessionHandler.create(store);

// session处理器控制用于session的cookie
// 举个例子，它可以包含同站策略（译者注：即samesite policy）的配置
// 比如这个，使用严格模式的同站策略
sessionHandler.setCookieSameSite(CookieSameSite.STRICT);

// 确保所有请求都可以路由经过这个session处理器
router.route().handler(sessionHandler);

// 现在您的应用程序可以开始处理了
router.route("/somepath/blah/").handler(ctx -> {

  Session session = ctx.session();
  session.put("foo", "bar");
  // 等等

});
```

session处理器会确保您的session会从session储存器中被自动地找出来(或者当session不存在时创建一个)， 然后在到达您的应用程序处理器之前将其放置在路由上下文中

默认情况下，会话（session）处理程序将始终将会话的 cookie 添加到 HTTP 响应中， 即便会话未被您的应用程序所访问。 如需仅在使用会话时创建会话 cookie，请使用 `sessionHandler.setLazySession(true)`。

### 使用session

在您的处理器中，您可以通过 `session` 获取到 session 实例

您可以通过 `put` 将数据放到 session 中， 您可以通过 `get` 从 session 中获取数据 同时您也可以通过 `remove` 从 session 中移除数据。

session 中对象的键往往是字符串类型。 对于本地 session 储存器，其值可以是任何类型，对于集群 session 储存器中它可以是任何基础类型或 `Buffer`, `JsonObject`, `JsonArray` 亦或者一个可序列化的类型，因为这些值为了在集群间传递必须进行序列化。

这是一个操作session中数据的例子

```java
router.route().handler(sessionHandler);

//现在是您的程序在处理
router.route("/somepath/blah").handler(ctx -> {

  Session session = ctx.session();

  // 放置一些数据到session中
  session.put("foo", "bar");

  // 从session获取数据
  int age = session.get("age");

  // 从session中移除数据
  JsonObject obj = session.remove("myobj");

});
```

在响应完成之后 session 会被自动写回到储存器中

您通过 `destroy` 手动销毁 session 它会将 session 从上下文和 session 储存器中移除 请注意，如果没有 session，则将为通过 session 处理器的下一个来自浏览器的请求自动创建一个新会话。

### session 超时

如果session的未访问时间超过超时时间，则session将自动超时。 当一个session超时时，它将会被从储存中移除

当请求到达，session被查找以及当响应完成且会话被存储回存储器中时， session将被自动标记为已访问。

您也可以使用 `setAccessed` 手动为session打上已访问标记

当创建session处理器时可以设置session的超时时间，其默认值为30分钟

## 认证/授权

Verx.x带有一些现成的处理器，用于处理身份验证和授权 在Vert.x web中，这两个词的含义是：

- **身份认证** - 表明用户是谁
- **授权** - 表明用户可以做什么

而 **身份认证** 是严格到一个众所周知的协议，比如

- HTTP Basic Authentication
- HTTP Digest Authentication
- OAuth2 Authentication
- …

**授权** 在vert.x中是相当通用的，无论优先级如何都可以使用 然而，在这两种情况下使用相同的提供者模块也是可能的，也是有效的用例。

### 创建一个身份认证处理器

创建一个验证处理器您需要一个 `AuthenticationProvider` 的实例。 身份认证提供者被用于认证用户身份。Vert.x在vertx-auth项目里面提供了几个开箱即用的身份认证提供者实例， 有关身份认证提供者以及如何使用和配置它们的完整信息， 请查阅身份认证文档。

这里有一些关于创建提供身份认证的basic auth处理器的例子。

```java
router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

AuthenticationHandler basicAuthHandler = BasicAuthHandler.create(authProvider);
```

### 在您的程序中处理身份认证

假设您希望对以 `/private/` 开头的路径的所有请求都经过身份认证。 为此，请确保在这些路径上，身份认证处理程序位于应用程序处理程序之前：

```java
router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

AuthenticationHandler basicAuthHandler = BasicAuthHandler.create(authProvider);

// 所有以'/private/'路径开头的请求都会被保护
router.route("/private/*").handler(basicAuthHandler);

router.route("/someotherpath").handler(ctx -> {

  // 这里是公开访问的地方 —— 不需要登录

});

router.route("/private/somepath").handler(ctx -> {

  // 这里需要登录

  // 这里的值为true
  boolean isAuthenticated = ctx.user() != null;

});
```

如果身份认证处理器成功认证了用户，它将注入一个 `User` 对象到 `RoutingContext` 中，因此它可以在您的处理器中通过 `user` 获取到。

如果希望将User对象存储在session中，以便在请求之间可用，因此不必在每个请求上进行身份验证， 则应确保在身份认证处理器之前具有session处理器。

一旦您拥有了您的user对象，您还可以以编程方式使用其上的方法来授权用户

如果您想要注销一个用户您可以调用 在路由上下文中调用 `clearUser`

### HTTP Basic Authentication

[HTTP Basic Authentication](http://en.wikipedia.org/wiki/Basic_access_authentication) 是一种适用于简单应用程序的简单身份认证方法。

使用basic authentication时，凭据将以非加密方式通过HTTP标头通过网络发送， 因此，必须使用HTTPS而不是HTTP为应用程序提供服务。

使用basic authentication时，如果用户请求需要身份认证的资源， 则basic auth处理器将发送带有标头 `WWW-Authenticate` 的 `401` 响应。 这会提示浏览器显示登录对话框，并提示用户输入用户名和密码。

用户会再次向资源发出请求，这次设置了 `Authorization` 标头， 其中包含以Base64编码的用户名和密码。

当basic auth处理器收到这个信息时，它会用用户名和密码调用配置好的 `AuthenticationProvider` 来认证用户。如果身份认证成功， 则允许请求路由继续到应用程序处理器， 否则返回 `403` 响应以表示拒绝访问。

### 重定向身份认证

使用重定向身份认证处理的情况下，如果用户尝试访问受保护的资源并且未登录， 则将用户重定向到登录页面

然后，用户填写登录表单并提交。 这由服务器进行处理，服务器对用户进行身份认证，如果通过身份认证，则将用户重定向回原始资源。

要使用重定向身份验证，您可以配置 `RedirectAuthHandler` 实例， 而不是basic authentication处理器。

您还需要设置处理器以处理于您的实际登录页面，以及一个处理器来处理实际登录本身。 为了处理登录，我们为此提供了一个预先构建的处理程序 `FormLoginHandler` 。

这里有一个简单app的例子，在默认重定向url `/loginpage` 上使用重定向认证处理器

```java
router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

// 所有以'/private/'路径开头的请求都会被保护
router
  .route("/private/*")
  .handler(RedirectAuthHandler.create(authProvider));

// 处理实际的登录请求
// 您其中一个页面必须要POST表单登陆数据
router.post("/login").handler(FormLoginHandler.create(authProvider));

// 设置静态服务器处理静态资源
router.route().handler(StaticHandler.create());

router
  .route("/someotherpath")
  .handler(ctx -> {
    //  这里是公开访问的地方 —— 不需要登录
  });

router
  .route("/private/somepath")
  .handler(ctx -> {

    // 这里需要登录

    // 这里的值是true
    boolean isAuthenticated = ctx.user() != null;

  });
```

### JWT 认证

使用JWT身份认证可以通过权限保护资源，并且拒绝没有足够权限的用户访问。 您需要添加 `io.vertx：vertx-auth-jwt：4.3.5` 依赖项才能使用 `JWTAuthProvider`。

使用这个处理器需要包含两步

- 设置处理器以发放token（或依赖第三方）
- 设置处理器过滤请求

请注意，这两个处理器仅应在HTTPS上可用，如果不这样做，会导致允许嗅探正在传输的token， 从而导致会话劫持攻击。

这是一个如何发放token的例子：

```java
Router router = Router.router(vertx);

JWTAuthOptions authConfig = new JWTAuthOptions()
  .setKeyStore(new KeyStoreOptions()
    .setType("jceks")
    .setPath("keystore.jceks")
    .setPassword("secret"));

JWTAuth jwt = JWTAuth.create(vertx, authConfig);

router.route("/login").handler(ctx -> {
 //这是个例子，身份认证应当使用其他的提供者
  if (
    "paulo".equals(ctx.request().getParam("username")) &&
      "secret".equals(ctx.request().getParam("password"))) {
    ctx.response()
      .end(jwt.generateToken(new JsonObject().put("sub", "paulo")));
  } else {
    ctx.fail(401);
  }
});
```

现在您的客户端拥有token，那么对于 **所有** 后续请求，HTTP标头 `Authorization` 都填充有：`Bearer <token>`，例如：

```java
Router router = Router.router(vertx);

JWTAuthOptions authConfig = new JWTAuthOptions()
  .setKeyStore(new KeyStoreOptions()
    .setType("jceks")
    .setPath("keystore.jceks")
    .setPassword("secret"));

JWTAuth authProvider = JWTAuth.create(vertx, authConfig);

router.route("/protected/*").handler(JWTAuthHandler.create(authProvider));

router.route("/protected/somepage").handler(ctx -> {
  // 一些处理器代码
});
```

JWT允许您将所需的任何信息添加到token本身。 这样一来，服务器中就不存在状态，这样就可以允许您扩展应用程序而无需集群的会话数据。 为了将数据添加到token，在token创建期间，只需将数据添加到JsonObject参数即可：

```java
JWTAuthOptions authConfig = new JWTAuthOptions()
  .setKeyStore(new KeyStoreOptions()
    .setType("jceks")
    .setPath("keystore.jceks")
    .setPassword("secret"));

JWTAuth authProvider = JWTAuth.create(vertx, authConfig);

authProvider
  .generateToken(
    new JsonObject()
      .put("sub", "paulo")
      .put("someKey", "some value"),
    new JWTOptions());
```

和消费时一样：

```java
Handler<RoutingContext> handler = ctx -> {
  String theSubject = ctx.user().principal().getString("sub");
  String someKey = ctx.user().principal().getString("someKey");
};
```

### 配置授权

到目前为止，所有示例都涉及身份认证。与用户打交道时，授权是下一个合乎逻辑的步骤。 虽然身份认证确实是特定于协议的，但授权是独立的， 所有信息均从 `User` 对象中提取。

在此之前，需要将授权加载到该同一对象。为了做到这一点，应该使用 `AuthorizationHandler`。 授权处理器将从给定的 `AuthorizationProvider` 中加载所有已知的授权

```java
router.route().handler(
  // 创建将执行认证的处理器
  AuthorizationHandler.create(
    // 认证什么
    PermissionBasedAuthorization.create("can-do-work"))
    // 在哪里查找用户的授权
    .addAuthorizationProvider(authProvider));
```

可以在1个以上的源上执行查找，只需继续向处理程序中添加 `addAuthorizationProvider(provider)` 即可。

这是一个配置应用程序的示例，因为该应用程序的不同部分需要不同的权限。 请注意，权限的含义由您使用的底层身份验证处理器确定。 例如。一些可能支持基于角色权限的模型，但是其他可能使用其他模型。

```java
router.route("/listproducts/*").handler(
  // 创建将执行认证的处理器
  AuthorizationHandler.create(
    // 认证什么
    PermissionBasedAuthorization.create("list_products"))
    // 在哪里查找用户的授权
    .addAuthorizationProvider(authProvider));

// 只有 "admin"可以访问 /private/settings
router.route("/private/settings/*").handler(
  // 创建将执行认证的处理器
  AuthorizationHandler.create(
    // 认证什么
    RoleBasedAuthorization.create("admin"))
    .addAuthorizationProvider(authProvider));
```

### 链接多个身份验证处理器

有时您想在单个应用程序中支持多种身份认证机制。 为此， 您可以使用 `ChainAuthHandler` 。 链式身份认证处理器将尝试对一系列处理程序执行身份认证。

重要的是要知道某些处理器需要特定的提供者，例如：

- `JWTAuthHandler` 需要 `JWTAuth` 。
- `DigestAuthHandler` 需要 `HtdigestAuth` 。
- `OAuth2AuthHandler` 需要 `OAuth2Auth` 。
- `WebAuthnHandler` 需要 `WebAuthn` 。

因此，不希望在所有处理程序之间共享提供者。 在某些情况下，可以跨处理程序共享提供者，例如：

- `BasicAuthHandler` 可以接受任何提供者。
- `RedirectAuthHandler` 可以接受任何提供者。

假设您要创建一个接受 `HTTP Basic Authentication` 和 `Form Redirect` 的应用程序。 您将链配置为：

```java
ChainAuthHandler chain = ChainAuthHandler.any();

// 将http basic认证处理器添加到处理器链路中
chain.add(BasicAuthHandler.create(provider));
// 将表单重定向认证处理器添加到处理器链路中
chain.add(RedirectAuthHandler.create(provider));

// 保护您的路由
router.route("/secure/resource").handler(chain);
// 您的app
router.route("/secure/resource").handler(ctx -> {
  // 做一些事情
});
```

因此，当用户发出没有 `Authorization` 标头的请求时， 这意味着该链将无法使用basic auth处理器进行身份验证，并将尝试使用重定向处理器进行身份证。 由于重定向处理器始终会将您重定向到发送您在处理器中配置的发送登录表单那里

就像vertx-web中的常规路由一样，身份认证链是一个序列。 因此，如果您希望回退到浏览器中，使用HTTP Basic身份认证而不是重定向来询问用户凭据， 那么您所需要做的就是反转加入到处理器链路的顺序。

现在假设您在请求中向请求头 `Authorization` 提供了值 `Basic [token]`。 在这种情况下，basic auth处理器将尝试进行身份认证，如果成功，处理器链路将停止，并且vertx-web将继续处理您的处理程序。 如果token无效，例如用户名密码错误，则链路将继续执行至下面的节点。 在此特定情况就是重定向身份验证处理程序。

复杂的处理器链路也是可行的，举个例子，创建一个逻辑序列比如：`HandlerA` 或 （ `HandlerB` 和 `HandlerC` ）。

```java
ChainAuthHandler chain =
  ChainAuthHandler.any()
    .add(authNHandlerA)
    .add(ChainAuthHandler.all()
      .add(authNHandlerB)
      .add(authNHandlerC));

// 保护您的代码
router.route("/secure/resource").handler(chain);
// 您的app
router.route("/secure/resource").handler(ctx -> {
  // 做一些事情
});
```

## 处理静态资源

Vert.x-Web带有开箱即用的处理器，用于处理静态Web资源， 因此您可以非常轻松地编写静态Web服务器。

您需要一个 `StaticHandler` 实例处理静态资源，比如 `.html`, `.css`, `.js` 或者其他任意静态资源。

对静态处理器处理的路径的任何请求都由文件系统上的目录或类路径提供文件。 默认静态文件目录为 `webroot`，但可以配置。

在下面的示例中，所有对以 `/static/` 开头的路径的请求都将从目录 `webroot` 得到响应：

```java
router.route("/static/*").handler(StaticHandler.create());
```

例如，如果请求路径为 `static/css/mystyles.css`， 则静态服务将在目录 `webroot/css/mystyle.css` 中查找文件。

它还会在类路径中寻找一个名为 `webroot/css/mystyle.css` 的文件。这意味着您可以将所有静态资源打包到一个jar文件（或fatjar）中， 然后像这样分发它们。

当Vert.x首次在类路径中找到资源时，它将提取该资源并将其缓存在磁盘上的临时目录中 ，因此不必每次都这样做。

处理器会处理范围感知的请求。当客户端向静态资源发出请求时， 处理程序将通过在 `Accept-Ranges` 标头上声明该单元来通知其可以处理范围感知的请求。 包含带有正确单位以及起始索引和结束索引的 `Range` 标头的进一步请求将收到带有正确 `Content-Range` 标头的部分响应。

### 配置缓存

默认情况下，静态处理程序将设置缓存头，以使浏览器能够有效地缓存文件。

Vert.x-Web 设置 `cache-control`,`last-modified`, 和 `date` 头

`cache-control` 默认情况下被设置为 `max-age=86400`。这相当于一天 如果您需要，这也可以通过 `setMaxAgeSeconds` 配置。

如果浏览器发送带有 `if-modified-since` 标头的GET或HEAD请求，并且自该日期以来资源没有被修改， 则返回 `304` 状态码，指示浏览器使用其本地缓存的资源。

如果不需要处理缓存头，则可以使用 `setCachingEnabled` 将其禁用。

启用缓存处理后，Vert.x-Web将在内存中缓存资源的上次修改日期， 这样可以避免每次访问磁盘时都要检查实际的上次修改日期。

缓存中的条目有一个到期时间，在此时间之后， 将再次检查磁盘上的文件并更新缓存中的条目

如果您知道文件永远不会在磁盘上更改，那么缓存条目将永远不会过期。 这是默认值。

如果您知道文件在服务器运行时可能会在磁盘上更改， 则可以通过 `setFilesReadOnly` 设置文件只读属性为false。

要启用一次可以在内存中缓存的最大条目数，可以使用 `setMaxCacheSize` 。

可以通过 `setCacheEntryTimeout` 配置缓存项的过期时间。

### 配置主页

对根路径 `/` 的任何请求都将导致主页得到处理。默认情况下主页是 `index.html` 它可以通过 `setIndexPage`.配置

### 更改web root

默认情况下，静态资源将从目录 `webroot` 提供。可以通过 `setWebRoot` 进行配置这个。

### 处理隐藏文件

默认情况下，服务器将提供隐藏文件（以 `.` 开头的文件）。

如果您不希望提供隐藏文件，则可以使用以下命令对其进行配置 `setIncludeHidden`。

### 目录列表

服务器还可以执行目录列表。默认情况下，目录列表处于禁用状态，可以使用 `setDirectoryListing` 启用他

当目录列表被启用时，返回的内容取决于 `accept` 标头中的内容类型。

对于 `text/html` 目录列表，可以使用 `setDirectoryTemplate` 配置用于呈现目录列表页面的模板

### 关闭在磁盘上的文件缓存

默认情况下，Vert.x将把从类路径提供的文件缓存到当前工作目录中名为 `.vertx` 的目录的子目录中的磁盘上的文件中。 当在生产环境中将服务部署为Fatjar时，这尤其有用， 因为每次生产时从类路径提供文件的速度都会很慢。

在开发中，这可能会引起问题，就像在服务器运行时更新静态内容一样， 只会提供缓存的文件而不是实际更新的文件

要禁用文件缓存，您可以将vert.x选项的属性 `fileResolverCachingEnabled` 设置为false。 为了向后兼容，该值还将默认为系统属性 `vertx.disableFileCaching` 的值。 例如。您可以在IDE中设置运行配置，以便在运行主类时进行设置。

## 跨域处理

[Cross Origin Resource Sharing](http://en.wikipedia.org/wiki/Cross-origin_resource_sharing)是一种安全的机制， 用于允许从一个域请求资源并从另一个域提供资源。

Vert.x-Web包含一个 `CorsHandler` ，用于帮您处理CORS协议

这是一个例子

```java
router.route()
  .handler(
    CorsHandler.create("vertx\\.io")
      .allowedMethod(HttpMethod.GET));

router.route().handler(ctx -> {

  // 您的app处理器

});
```

## 多租户

在某些情况下，您的应用程序需要处理的不仅仅是一个租户。 在这种情况下，将提供一个助手处理器，以简化应用程序的设置。

如果租户是通过HTTP标头（例如 `X-Tenant` ）标识的， 则创建处理程序非常简单：

```java
router.route().handler(MultiTenantHandler.create("X-Tenant"));
```

现在，您应该注册为给定租户执行的处理器：

```java
MultiTenantHandler.create("X-Tenant")
  .addTenantHandler("tenant-A", ctx -> {
    // 为租户A做一些事情
  })
  .addTenantHandler("tenant-B", ctx -> {
    // 为租户B做一些事情
  })
  // optionally
  .addDefaultHandler(ctx -> {
    // 当没有租户匹配时，做一些事情
  });
```

这对于安全情况很有用：

```java
OAuth2Auth gitHubAuthProvider = GithubAuth
  .create(vertx, "CLIENT_ID", "CLIENT_SECRET");

// 在我们运行的服务器上创建一个oauth2处理器
// 第二个参数是回调的完整网址
// 和您在提供者管理平台输入的一致
OAuth2AuthHandler githubOAuth2 = OAuth2AuthHandler.create(
  vertx,
  gitHubAuthProvider,
  "https://myserver.com/github-callback");

// 设置回调处理程序以接收GitHub回调
githubOAuth2.setupCallback(router.route("/github-callback"));

// 创建一个OAuth2提供者，ClintID和ClientSecret
// 应该向Google请求
OAuth2Auth googleAuthProvider = OAuth2Auth.create(vertx, new OAuth2Options()
  .setClientId("CLIENT_ID")
  .setClientSecret("CLIENT_SECRET")
  .setSite("https://accounts.google.com")
  .setTokenPath("https://www.googleapis.com/oauth2/v3/token")
  .setAuthorizationPath("/o/oauth2/auth"));

// 在我们的"http://localhost:8080"域上创建oauth2处理器
OAuth2AuthHandler googleOAuth2 = OAuth2AuthHandler.create(
  vertx,
  googleAuthProvider,
  "https://myserver.com/google-callback");

// 设置回调处理程序以接收Google回调
googleOAuth2.setupCallback(router.route("/google-callback"));

// 此时，两个回调端点已注册：

// /github-callback -> 处理 github Oauth2 回调
// /google-callback -> 处理 google Oauth2 回调

// 由于回调是由IdP进行的，因此没有标头来标识源，
// 因此需要自定义URL

// 但是对于我们的应用程序，我们可以对其进行控制，
// 因此稍后我们可以为合适的租户添加合适的处理程序

router.route().handler(
  MultiTenantHandler.create("X-Tenant")
    // 使用github的租户走这个方法
    .addTenantHandler("github", githubOAuth2)
    // 使用google的租户走这个方法
    .addTenantHandler("google", googleOAuth2)
    // 其余的都会被禁止
    .addDefaultHandler(ctx -> ctx.fail(401)));
```

可以随时从上下文中读取租户ID，例如， 确定要加载的资源或要连接的数据库：

```java
router.route().handler(ctx -> {
  // 默认键是 "tenant"，
  // 其是在MultiTenantHandler.TENANT中定义的，
  // 但此值可以在创建时在工厂方法中进行修改
  String tenant = ctx.get(MultiTenantHandler.TENANT);

  switch (tenant) {
    case "google":
      // 为google用户做一些事情
      break;
    case "github":
      // 为github用户做一些事情
      break;
  }
});
```

多租户是一个功能强大的处理器，将允许应用程序并排运行，但是它不提供沙箱执行。 不应将其用作隔离，因为错误编写的应用程序可能会在租户之间泄漏状态。

## 模板

Vert.x Web 为若干流行的模板引擎提供了开箱即用的支持，通过这种方式来提供生成动态页面的能力。 您也可以很容易地添加您自己的实现。

`TemplateEngine` 定义了使用模板引擎的接口。 当渲染模板时会调用 `render` 方法。

最简单的使用模板的方式不是直接调用模板引擎，而是使用模板处理器 `TemplateHandler` 。 这个处理器会根据 HTTP 请求的路径来调用模板引擎。

缺省情况下，模板处理器会在 `templates` 目录中查找模板文件。这是可以配置的。

该处理器会返回渲染的结果，并默认设置 Content-Type 消息头为 `text/html` 。这也是可以配置的。

您需要在创建模板处理器时提供您想要使用的模板引擎实例。 Vert.x Web 并未嵌入模板引擎的实现，您需要配置项目来访问它们。 Vert.x Web 提供了每一种模板引擎的配置。

以下是例子：

```java
TemplateEngine engine = HandlebarsTemplateEngine.create();
TemplateHandler handler = TemplateHandler.create(engine);

// 这会将所有以 `/dynamic` 开头的 GET 请求路由到模板处理器上
// 例如 /dynamic/graph.hbs 会查找模板 /templates/graph.hbs
router.get("/dynamic/*").handler(handler);

// 将所有以 `.hbs` 结尾的 GET 请求路由到模板处理器上
router.getWithRegex(".+\\.hbs").handler(handler);
```

### MVEL 模版引擎

您需要在项目中添加以下 *依赖* 以使用 MVEL 模板引擎： `io.vertx:vertx-web-templ-mvel:4.3.5`。 并通过此方法以创建 MVEL 模板引擎实例： `io.vertx.ext.web.templ.mvel.MVELTemplateEngine#create(io.vertx.core.Vertx)`。

在使用 MVEL 模板引擎时，如果不指定模板文件的扩展名， 则默认会查找扩展名为 `.templ` 的文件。

在 MVEL 模板中可以通过 `context` 上下文变量来访问路由上下文 `RoutingContext` 对象。 这意味着您可使用任何基于上下文里的信息来渲染模板， 包括请求、响应、会话或者上下文数据。

以下是例子：

```
The request path is @{context.request().path()}

The variable 'foo' from the session is @{context.session().get('foo')}

The value 'bar' from the context data is @{context.get('bar')}
```

关于如何编写 MVEL 模板， 请参考 [MVEL 模板文档](http://mvel.documentnode.com/#mvel-2.0-templating-guide)。

### Jade 模版引擎（译者注：Jade 已更名为 Pug)

您需要在项目中添加以下 *依赖* 以使用 Jade 模板引擎： `io.vertx:vertx-web-templ-jade:4.3.5`。 并通过此方法以创建 Jade 模板引擎实例： `io.vertx.ext.web.templ.jade.JadeTemplateEngine#create(io.vertx.core.Vertx)`。

在使用 Jade 模板引擎时，如果不指定模板文件的扩展名， 则默认会查找扩展名为 `.jade` 的文件。

在 Jade 模板中可以通过 `context` 上下文变量来访问路由上下文 `RoutingContext` 对象。 这意味着您可使用任何基于上下文里的信息来渲染模板， 包括请求、响应、会话或者上下文数据。

以下是例子：

```
!!! 5
html
 head
   title= context.get('foo') + context.request().path()
 body
```

关于如何编写 Jade 模板， 请参考 [Jade4j 文档](https://github.com/neuland/jade4j)。

### Handlebars 模板引擎

您需要在项目中添加以下 *依赖* 以使用 Handlebars 模板引擎： `io.vertx:vertx-web-templ-handlebars:4.3.5`。 并通过此方法以创建 Handlebars 模板引擎实例： `io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine#create(io.vertx.core.Vertx)`。

在使用 Handlebars 模板引擎时，如果不指定模板文件的扩展名， 则默认会查找扩展名为 `.hbs` 的文件。

Handlebars 不允许在模板中随意地调用对象的方法， 因此我们不能像对待其他模板引擎一样将RoutingContext传递到引擎里并让模板来识别它。

替代方案是，可以使用模版中的上下文 `data` 对象。

如果您要访问某些RoutingContext里不存在的信息， 比如请求的路径、请求参数或者会话等，您需要在模板处理器执行之前将他们添加到上下文data里，例如：

```java
TemplateHandler handler = TemplateHandler.create(engine);

router.get("/dynamic").handler(ctx -> {

  ctx.put("request_path", ctx.request().path());
  ctx.put("session_data", ctx.session().data());

  ctx.next();
});

router.get("/dynamic/").handler(handler);
```

关于如何编写 Handlebars 模板， 请参考 [Handlebars Java 文档](https://github.com/jknack/handlebars.java)。

### Thymeleaf 模板引擎

您需要在项目中添加以下 *依赖* 以使用 Thymeleaf 模板引擎： `io.vertx:vertx-web-templ-thymeleaf:4.3.5`。 并通过此方法以创建 Thymeleaf 模板引擎实例： `io.vertx.ext.web.templ.thymeleaf.ThymeleafTemplateEngine#create(io.vertx.core.Vertx)`。

在使用 Thymeleaf 模板引擎时，如果不指定模板文件的扩展名， 则默认会查找扩展名为 `.html` 的文件。

在 Thymeleaf 模板中可以通过 `context` 上下文变量来访问路由上下文 `RoutingContext` 对象。 这意味着您可使用任何基于上下文里的信息来渲染模板， 包括请求、响应、会话或者上下文数据。

以下是例子：

```
[snip]
<p th:text="${context.get('foo')}"></p>
<p th:text="${context.get('bar')}"></p>
<p th:text="${context.normalizedPath()}"></p>
<p th:text="${context.request().params().get('param1')}"></p>
<p th:text="${context.request().params().get('param2')}"></p>
[snip]
```

关于如何编写 Thymeleaf 模板， 请参考 [Thymeleaf 文档](http://www.thymeleaf.org/)。

### Apache FreeMarker 模版引擎

您需要在项目中添加以下 *依赖* 以使用 Apache FreeMarker ： `io.vertx:vertx-web-templ-freemarker:4.3.5`。 并通过此方法以创建 Apache FreeMarker 模板引擎实例： `io.vertx.ext.web.templ.Engine#create()`。

在使用 Apache FreeMarker 模板引擎时，如果不指定模板文件的扩展名， 则默认会查找扩展名为 `.ftl` 的文件。

在 Apache FreeMarker 模板中可以通过 `context` 上下文变量来访问路由上下文 `RoutingContext` 对象。 这意味着您可使用任何基于上下文里的信息来渲染模板， 包括请求、响应、会话或者上下文数据。

以下是例子：

```
[snip]
<p th:text="${context.foo}"></p>
<p th:text="${context.bar}"></p>
<p th:text="${context.normalizedPath()}"></p>
<p th:text="${context.request().params().param1}"></p>
<p th:text="${context.request().params().param2}"></p>
[snip]
```

关于如何编写 Apache FreeMarker 模板， 请参考 [Apache FreeMarker 文档](http://www.freemarker.org/)。

### Pebble 模版引擎

您需要在项目中添加以下 *依赖* 以使用 Pebble ： `io.vertx:vertx-web-templ-pebble:4.3.5`。 并通过此方法以创建 Pebble 模板引擎实例： `io.vertx.ext.web.templ.pebble.PebbleTemplateEngine#create(vertx)`。

在使用 Pebble 模板引擎时，如果不指定模板文件的扩展名， 则默认会查找扩展名为 `.ped` 的文件。

在 Pebble 模板中可以通过 `context` 上下文变量来访问路由上下文 `RoutingContext` 对象。 这意味着您可使用任何基于上下文里的信息来渲染模板， 包括请求、响应、会话或者上下文数据。

以下是例子：

```
[snip]
<p th:text="{{context.foo}}"></p>
<p th:text="{{context.bar}}"></p>
<p th:text="{{context.normalizedPath()}}"></p>
<p th:text="{{context.request().params().param1}}"></p>
<p th:text="{{context.request().params().param2}}"></p>
[snip]
```

关于如何编写 Pebble 模板， 请参考 [Pebble 文档](http://www.mitchellbosecke.com/pebble/home/)。

### Rocker 模版引擎

您需要在项目中添加以下 *依赖* 以使用 Rocker：`io.vertx:vertx-web-templ-rocker:4.3.5 并通过此方法以创建 Rocker 模板引擎实例：`io.vertx.ext.web.templ.rocker#create()`。

Rocker会将JSON上下文对象的值传递给 `render` 方法作为模版的参数。 假定已知：

```
[snip]
final JsonObject context = new JsonObject()
 .put("foo", "badger")
 .put("bar", "fox")
 .put("context", new JsonObject().put("path", "/foo/bar"));

engine.render(context, "somedir/TestRockerTemplate2", render -> {
 // (...)
});
[snip]
```

那么相对应的模版文件 `somedir/TestRockerTemplate2.rocker.html` 可写作：

```
@import io.vertx.core.json.JsonObject
@args (JsonObject context, String foo, String bar)
Hello @foo and @bar
Request path is @context.getString("path")
```

### HTTL 模版引擎

您需要在项目中添加以下 *依赖* 以使用 HTTL ： `io.vertx:vertx-web-templ-httl:4.3.5`。 并通过此方法以创建 HTTL 模板引擎实例： `io.vertx.ext.web.templ.httl.HTTLTemplateEngine#create(io.vertx.core.Vertx)`。

在使用 HTTL 模板引擎时，如果不指定模板文件的扩展名， 则默认会查找扩展名为 `.httl` 的文件。

HTTL会将JSON上下文对象的值传递给 `render` 方法作为模版的参数。 假定已知：

```
[snip]
TemplateEngine engine = HTTLTemplateEngine.create(vertx);
final JsonObject context = new JsonObject()
 .put("foo", "badger")
 .put("bar", "fox");

engine.render(context, "somedir/test-httl-template1.httl", render -> {
 // (...)
});
[snip]
```

那么相对应的模版文件 `somedir/test-httl-template1.httl` 可写作：

```
<!-- #set(String foo, String bar) -->
Hello ${foo} and ${bar}
```

关于如何编写 HTTL 模板， 请参考 [HTTL 文档](https://httl.github.io/en/)。

### Rythm 模版引擎

您需要在项目中添加以下 *依赖* 以使用 Rythm ： `io.vertx:vertx-web-templ-rythm:4.3.5`。 并通过此方法以创建 Rythm 模板引擎实例： `io.vertx.ext.web.templ.rythm.RythmTemplateEngine#create(io.vertx.core.Vertx)`。

在使用 Rythm 模板引擎时，如果不指定模板文件的扩展名， 则默认会查找扩展名为 `.html` 的文件。

Rythm会将JSON上下文对象的值传递给 `render` 方法作为模版的参数。 假定已知：

```
[snip]
TemplateEngine engine = RythmTemplateEngine.create(vertx);
final JsonObject context = new JsonObject()
 .put("foo", "badger")
 .put("bar", "fox");

engine.render(context, "somedir/test-rythm-template1.html", render -> {
 // (...)
});
[snip]
```

那么相对应的模版文件 `somedir/test-rythm-template1.httl` 可写作：

```
<!-- #set(String foo, String bar) -->
Hello @foo and @bar
```

关于如何编写 HTTL 模板，请参考 [RythmEngine 文档](http://www.rythmengine.org/)。

### 缓存

许多引擎支持将编译好的模版存入缓存。该缓存存放在Vert.x的可分享的数据local map里。 这样引擎便可在多个verticle中安全高效地使用该缓存。

#### 禁用缓存

在开发时，为了让每一次请求可以读取最新的模板，您可能希望禁用模板的缓存。 您可通过设置系统变量：`vertxweb.environment` 或环境变量 `VERTXWEB_ENVIRONMENT` 为 `dev` 或 `development` 将其禁用。缓存默认是启用的。

## 错误处理器

您可使用模版处理器自行渲染错误页面， 但是Vert.x-Web同样为您提供了开箱即用且“好看的”错误处理器，可为您渲染错误页面。

该处理器是 `ErrorHandler`。 要使用该错误处理器，仅需要将其设置为您希望覆盖的错误路径的失败处理器即可（译者注：例如router.route("/*").failureHandler(ErrorHandler.create(vertx))）。

## 请求日志

Vert.x-Web通过内置处理器 `LoggerHandler` 来记录请求日志。 您需在挂载任何可能导致 `RoutingContext` 失败的处理器之前挂载该处理器。

默认情况下，请求日志将会被记录到Vert.x logger中，亦可通过更改配置使用JUL logging, log4j 或 SLF4J记录。

详见 `LoggerFormat`。

## 提供网页图标

Vert.x-Web通过内置处理器 `FaviconHandler` 以提供网页图标。

图标可以指定为文件系统上的某个路径，否则 Vert.x Web 默认会在 classpath 上寻找名为 `favicon.ico` 的文件。 这意味着您可以将图标打包到包含您应用的 jar 包里。

## 超时处理器

Vert.x-Web内置一个超时处理器以处理超时请求。

可通过 `TimeoutHandler` 配置。

如果一个请求超时，则会给客户端返回一个 503 的响应。

下面的例子设置了一个超时处理器。对于所有以 `/foo` 路径开头的请求， 都会在执行时间超过 5 秒之后自动超时。

```java
router.route("/foo/").handler(TimeoutHandler.create(5000));
```

## 响应时间处理器

该处理器会将从接收到请求到写入响应的消息头之间的毫秒数写入到响应的 `x-response-time` 里， 例如：

x-response-time: 1456ms

## 内容类型（Content type）处理器

`ResponseContentTypeHandler` 会自动设置响应的 `Content-Type` 消息头。 假设我们要构建一个 RESTful 的 Web 应用，我们需要在所有处理器里设置消息类型：

```java
router
  .get("/api/books")
  .produces("application/json")
  .handler(ctx -> findBooks()
    .onSuccess(books -> ctx.response()
      .putHeader("Content-Type", "application/json")
      .end(toJson(books))).onFailure(ctx::fail));
```

随着 API 接口数量的增长，设置内容类型会变得很麻烦。 可以通过在相应的 Route 上添加 `ResponseContentTypeHandler` 来避免这个问题：

```java
router.route("/api/*").handler(ResponseContentTypeHandler.create());
router
  .get("/api/books")
  .produces("application/json")
  .handler(ctx -> findBooks()
    .onSuccess(books -> ctx.response()
      .end(toJson(books))).onFailure(ctx::fail));
```

处理器会通过 `getAcceptableContentType` 方法来选择适当的内容类型。 因此，您可以很容易地使用同一个处理器以提供不同类型的数据：

```java
router.route("/api/*").handler(ResponseContentTypeHandler.create());

router
  .get("/api/books")
  .produces("text/xml")
  .produces("application/json")
  .handler(ctx -> findBooks()
    .onSuccess(books -> {
      if (ctx.getAcceptableContentType().equals("text/xml")) {
        ctx.response().end(toXML(books));
      } else {
        ctx.response().end(toJson(books));
      }
    })
    .onFailure(ctx::fail));
```

## SockJS

SockJS 是一个客户端的 JavaScript 库以及协议，它提供了类似 WebSocket 的接口以方便您与 SockJS 服务器创建连接， 而无需您关心浏览器或网络是否允许真正的 WebSocket。

它提供了若干不同的传输方式， 并在运行时根据浏览器和网络的兼容性来选择使用哪种传输方式处理。

然而这一切对您而言是透明的，您只需要简单地使用类似 WebSocket 的接口 *即可*。

请参阅 [SockJS 网站](https://github.com/sockjs/sockjs-client)以获取更多关于SockJS的信息。

### SockJS 处理器

Vert.x 提供了一个开箱即用的处理器 `SockJSHandler` 以便您在 Vert.x-Web 应用中使用 SockJS。

您需要通过 `SockJSHandler.create` 方法为每一个 SockJS 的应用创建处理器。 您也可以在创建处理器时通过 `SockJSHandlerOptions` 对象来指定配置选项。

```java
Router router = Router.router(vertx);

SockJSHandlerOptions options = new SockJSHandlerOptions()
  .setHeartbeatInterval(2000);

SockJSHandler sockJSHandler = SockJSHandler.create(vertx, options);
```

### 处理 SockJS 套接字

您可以在服务器端设置一个 SockJS 处理器， 这个处理器会在客户端创建连接时被调用：

传递给处理器的是 `SockJSSocket` 对象。 这是一个类似套接字的接口，您可以像使用 `NetSocket` 或 `WebSocket` 那样通过它来读写数据。它实现了 `ReadStream` 和 `WriteStream` 接口，因此您可以将它套用（pump）到其他读写流上。 若 SockJS 连接使用 `routingContext` 加载， 那么便可在手动管理会话（session）时访问 `RoutingContext`。 由此您可以通过 `webSession` 和 `webUser` 管理用户和会话。

下面的例子中的 SockJS 处理器直接使用了它读取到的数据进行回写：

```java
Router router = Router.router(vertx);

SockJSHandlerOptions options = new SockJSHandlerOptions()
  .setHeartbeatInterval(2000);

SockJSHandler sockJSHandler = SockJSHandler.create(vertx, options);

router.route("/myapp/*")
  .subRouter(sockJSHandler.socketHandler(sockJSSocket -> {

  // 将数据回写
  sockJSSocket.handler(sockJSSocket::write);

}));
```

### 客户端

在客户端 JavaScript 环境里您需要通过 SockJS 的客户端库来建立连接。 这是SockJS 客户端的地址 https://www.npmjs.com/package/sockjs-client。

您可直接在捆绑软件或构建工具中直接引用它。 或者您想在 `HTML` 文档中直接使用 `CDN` 版本，那么首先需要引入 sockjs 的依赖：

```html
<html>
<head>
 <script src="https://unpkg.io/sockjs-client@1.5.0/dist/sockjs.min.js"></script>
</head>
<body>
 ...
</body>
</html>
```

完整的使用细节可参阅 [SockJS 网站](https://github.com/sockjs/sockjs-client)， 但简而言之可像这样使用：

```js
var sock = new SockJS('http://mydomain.com/myapp');

sock.onopen = function() {
 console.log('open');
};

sock.onmessage = function(e) {
 console.log('message', e.data);
};

sock.onevent = function(event, message) {
 console.log('event: %o, message:%o', event, message);
 return true; // 为了标记消息已被处理了
};

sock.onunhandled = function(json) {
 console.log('this message has no address:', json);
};

sock.onclose = function() {
 console.log('close');
};

sock.send('test');

sock.close();
```

### 配置 SockJS 处理器

可使用 `SockJSHandlerOptions` 为处理器配置各种选项。

> **📝注意:** 默认情况下， 配置中并不包含一个默认的 `Origin` 属性。为了防御浏览器发送的 WebSocket 跨域劫持攻击，我们建议您将 `Origin` 属性设置为您应用的 网络源。这会强制服务器检查 WebSocket 的 `Origin` 以验证连接是否来自您的应用。这项检查非常重要，因为 浏览器的同源策略并不限制 WebSocket 连接，因此一个攻击者可以轻松地在一个 恶意网页上创建一个请求并连接您服务器上 sockJS 桥接器提供的 `ws://` 或 `wss://` 接口。

### 通过 event bus 写入 SockJS 套接字

在创建 `SockJSSocket` 的时候，可为其注册一个 event bus 上的事件处理器。 该处理器的地址就是 `writeHandlerID` 。

默认情况下，不允许注册事件处理器。 需要通过 `SockJSHandlerOptions` 以启用该设置。

```java
Router router = Router.router(vertx);

SockJSHandlerOptions options = new SockJSHandlerOptions()
  .setRegisterWriteHandler(true);

SockJSHandler sockJSHandler = SockJSHandler.create(vertx, options);

router.route("/myapp/*")
  .subRouter(sockJSHandler.socketHandler(sockJSSocket -> {

  // 获取 writeHandlerID 并将其存放 (例如放在本地 map 里)
  String writeHandlerID = sockJSSocket.writeHandlerID();

}));
```

> **📝注意:** 默认情况下，处理器仅在本地注册。 集群可通过配置 `setLocalWriteHandler` 为false启用。

随后将数据写入 `Buffer` 便可发送给 SockJS 套接字。

```java
eventBus.send(writeHandlerID, Buffer.buffer("foo"));
```

## SockJS 桥接 Event Bus

Vert.x Web 提供了一个内置的被称为事件总线桥（event bus bridge）的 SockJS 套接字处理器。 该处理器有效地将服务器端的 Vert.x 的事件总线延伸到客户端的 JavaScript 运行环境里。

这将创建一个分布式的事件总线。 该 event bus 不仅可以在服务器端多个 Vert.x 实例中使用，还可以通过运行在浏览器里的 JavaScript 访问。

由此，我们可以建立起一个连接多个浏览器和服务器群的庞大的分布式 event bus。 浏览器只需与服务器集群建立连接，无需每次都与固定的某个服务器建立连接。

这些是通过 Vert.x 提供的一个简单的客户端 JavaScript 库 `vertx-eventbus.js` 来实现的。 它提供了一系列与服务器端的 Vert.x event-bus 极为类似的 API。 通过这些 API 您可以发送或发布消息，或注册处理器来接收消息。

该 JavaScript 库使用了 JavaScript 的 SockJS 客户端，与另外一端的 `SockJS 处理器` 建立起 SockJS 连接， 并将事件总线上的流量通过管道（tunnel）传送至该客户端。

一个特殊的 SockJS 套接字处理器因此被安装到 `SockJS 处理器` 上， 而该处理器将会处理 SockJS 的数据，并将建立起与服务器端的事件总线的连接桥。

启用该连接桥您只需要在 SockJS 处理器中调用 `bridge`。

```java
Router router = Router.router(vertx);

SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
SockJSBridgeOptions options = new SockJSBridgeOptions();
// 将连接桥挂载到路由器上
router
  .route("/eventbus/*")
  .subRouter(sockJSHandler.bridge(options));
```

在客户端的 JavaScript 中，您可以使用 [@vertx/eventbus-bridge-client.js](http://npmjs.com/package/@vertx/eventbus-bridge-client.js) 库以创建跟事件总线的连接，并以此发送和接收消息。 该库可在 [NPM](http://npmjs.com/package/@vertx/eventbus-bridge-client.js) 上找到。 您可直接在捆绑软件或构建工具中直接引用它，但同时它亦可以在 CDN 中使用 （就像之前的 sockJS 例子）：

```html
<script src="https://unpkg.io/sockjs-client@1.5.0/dist/sockjs.min.js"></script>
<script src='https://unpkg.io/@vertx/eventbus-bridge-client.js@1.0.0-1/vertx-eventbus.js'></script>

<script>

var eb = new EventBus('http://localhost:8080/eventbus');

eb.onopen = () => {

 // 设置一个处理器以接收消息
 eb.registerHandler('some-address', (error, message) => {
   console.log('received a message: ' + JSON.stringify(message));
 });

 // 发送消息
 eb.send('some-address', {name: 'tim', age: 587});

}

</script>
```

该例子中首先创建了一个 event bus 实例

```javascript
var eb = new EventBus('http://localhost:8080/eventbus');
```

构造器中的参数是连接 event bus 的URI。 因为我们建立的连接桥是以 `eventbus` 为前缀，所以我们将会成功建立连接。

您在连接开启之前无法做任何事。当连接开启时 `onopen` 处理器将会被调用。

连接桥支持自动重连，可设置延迟和退避选项。

```javascript
var eb = new EventBus('http://localhost:8080/eventbus');
eb.enableReconnect(true);
eb.onopen = function() {}; // 在此处设立处理器，每次建立连接或重连时候调用
eb.onreconnect = function() {}; // 可选，仅在重连时被调用

// 或者，传入一个 options 对象
var options = {
   vertxbus_reconnect_attempts_max: Infinity, // 重连尝试最多次数
   vertxbus_reconnect_delay_min: 1000, // 在第一次尝试重连之前的初始延迟（单位为毫秒）
   vertxbus_reconnect_delay_max: 5000, // 尝试重连之间的最大延迟（单位为毫秒）
   vertxbus_reconnect_exponent: 2, // 指数退避因子
   vertxbus_randomization_factor: 0.5 // 介于0和1之间的随机因子
};

var eb2 = new EventBus('http://localhost:8080/eventbus', options);
eb2.enableReconnect(true);
// 创建处理器……
```

### 守护连接桥

如果您像上面的例子一样建立连接桥但未开启守护机制，此时您试图通过该桥发送消息， 您会发现消息神秘地失踪了。发生了什么？

对于大多数的应用，您恐怕不希望客户端的 JavaScript 代码可以发送任何消息到任意服务端处理器或其他所有浏览器上。

例如，您可能在事件总线上注册了一个服务，用于访问或删除数据。 我们并不希望出现恶意的行为或有害的客户端能够利用该服务删除数据库中所有的数据！

此外，我们恐怕也不希望任意一个客户端都能监听任意一个事件总线地址。

为了解决这个问题，SockJs连接桥默认会拒绝所有的消息。 您需要告诉连接桥哪些消息是可以通过的。（例外情况是，所有的回复消息都是可以通过的）。

换句话说，连接桥的行为就像是配置了缺省策略为 *全部拒绝* 策略的防火墙。

为连接桥配置哪些消息可以通过是很简单的一件事。

您可以通过调用连接桥时传入的 `SockJSBridgeOptions` 来配置 *匹配* 规则以指定哪些输入和输出的流量是允许通过的。

每一个匹配规则对应一个 `PermittedOptions` 对象：

- `setAddress`

  该配置规则精确地定义了消息可以被发送到哪些地址。 如您需要通过精确地址来控制消息的话，使用该选项。

- `setAddressRegex`

  该配置规则通过正则表达式来定义消息可以被发送到哪些地址。如您需要通过正则表达式来控制消息的话，请使用这个选项。 如果指定了 `address` ，则该选项会被忽略。

- `setMatch`

  该配置规则通过消息的结构来控制消息是否可被发送。该配置中定义的每一个字段必须在消息中存在，且值一致。 目前仅适用于 JSON 格式的消息。

对于一个 *输入* 的消息（例如通过客户端 JavaScript 发送到服务器） 当消息抵达时，Vert.x Web 会检查每一条输入许可。如果存在匹配规则，则消息可以通过。

对于一个 *输出* 的消息（例如通过服务器端发送给客户端 JavaScript） 当消息发送时，Vert.x Web 会检查每一条输出许可。如果存在匹配，则消息可以通过。

实际的匹配过程如下：

如果指定了 `address` 字段，并且消息的目标地址与 `address` *精确* 匹配， 则匹配成功。

如果没有指定 `address` 但指定了 `addressRegex` 字段，并且消息的目标地址匹配了 `address_re` 里的正则表达式， 则匹配成功。

如果指定了 `match` 字段，则消息的结构也必须匹配。 消息需包含有 match 对象中的所有键值对，方能匹配成功。

以下是例子：

```java
Router router = Router.router(vertx);

SockJSHandler sockJSHandler = SockJSHandler.create(vertx);


// 允许客户端向地址 `demo.orderMgr` 发送消息
PermittedOptions inboundPermitted1 = new PermittedOptions()
  .setAddress("demo.orderMgr");

// 允许客户端向地址 `demo.persistor` 发送
// 包含有 `action` 的值为 `find`、
// `collecton` 的值为 `albums` 的消息。
PermittedOptions inboundPermitted2 = new PermittedOptions()
  .setAddress("demo.persistor")
  .setMatch(new JsonObject().put("action", "find")
    .put("collection", "albums"));

// 允许 `wibble` 值为 `foo` 的消息。
PermittedOptions inboundPermitted3 = new PermittedOptions()
  .setMatch(new JsonObject().put("wibble", "foo"));

// 让我们定义 服务端 -> 客户端 发送消息匹配规则

// 允许向客户端发送地址为 `ticker.mystock` 的消息
PermittedOptions outboundPermitted1 = new PermittedOptions()
  .setAddress("ticker.mystock");

// 允许向客户端发送地址以 `news.` 开头的消息
//（例如 news.europe, news.usa, 等）
PermittedOptions outboundPermitted2 = new PermittedOptions()
  .setAddressRegex("news\\..+");

// 让我们定义 客户端 -> 客户端 发送消息匹配规则
SockJSBridgeOptions options = new SockJSBridgeOptions().
  addInboundPermitted(inboundPermitted1).
  addInboundPermitted(inboundPermitted1).
  addInboundPermitted(inboundPermitted3).
  addOutboundPermitted(outboundPermitted1).
  addOutboundPermitted(outboundPermitted2);

// 将连接桥挂载到路由器上
router
  .route("/eventbus/*")
  .subRouter(sockJSHandler.bridge(options));
```

### 消息授权

连接桥可使用 Vert.x Web 的授权功能以配置消息的访问权限， 同时支持输入和输出的消息。

为此，您可通过向上文所述的匹配规则中加入额外的字段 以指定匹配需要哪些权限。

通过 `setRequiredAuthority` 方法来指定对于登录用户，需要具有哪些权限才允许访问这个消息。

以下是例子：

```java
PermittedOptions inboundPermitted = new PermittedOptions()
  .setAddress("demo.orderService");

// 仅限用户已登录并且拥有权限 `place_orders`
inboundPermitted.setRequiredAuthority("place_orders");

SockJSBridgeOptions options = new SockJSBridgeOptions()
  .addInboundPermitted(inboundPermitted);
```

用户需要登录，并被授权才能够访问消息。

因此，您需要配置一个 Vert.x 认证处理器来处理登录和授权。例如：

```java
Router router = Router.router(vertx);

// 允许客户端向 `demo.orderService` 发送消息
PermittedOptions inboundPermitted = new PermittedOptions()
  .setAddress("demo.orderService");

// 仅限用户已登录并且拥有权限 `place_orders`
inboundPermitted.setRequiredAuthority("place_orders");

SockJSHandler sockJSHandler = SockJSHandler.create(vertx);

// 设置基础认证处理器：

router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

AuthenticationHandler basicAuthHandler = BasicAuthHandler.create(authProvider);

router.route("/eventbus/*").handler(basicAuthHandler);

// 将连接桥挂载到路由器上
router
  .route("/eventbus/*")
  .subRouter(sockJSHandler.bridge(new SockJSBridgeOptions()
    .addInboundPermitted(inboundPermitted)));
```

### 处理事件总线桥事件

如果您希望在桥时上发生事件时收到通知，可以提供一个处理器在调用 `bridge` 时调用。

每当桥上发生事件时，它将被传递给处理程序。这个事件通过一个 `BridgeEvent` 实例进行描述。

事件可以是下面的类型中的一种：

- SOCKET_CREATED

  创建新的 SockJS 套接字时，将发生此事件.

- SOCKET_IDLE

  当 SockJS 套接字闲置的时间比最初配置的时间长时，将发生此事件.

- SOCKET_PING

  当为 SockJS 套接字更新最后的 ping 时间戳时，将发生此事件.

- SOCKET_CLOSED

  关闭 SockJS 套接字时，将发生此事件.

- SOCKET_ERROR

  当底层传输出错时，将发生此事件.

- SEND

  尝试将消息从客户端发送到服务器时，将发生此事件.

- PUBLISH

  尝试将消息从客户端发布到服务器时，将发生此事件.

- RECEIVE

  尝试将消息从服务器传递到客户端时，将发生此事件.

- REGISTER

  当客户端尝试注册处理程序时，将发生此事件.

- UNREGISTER

  当客户端尝试注销处理程序时，将发生此事件.

event可以通过 `type` 让您获取到类型， 并通过 `getRawMessage` 检查事件的原始消息。

原始消息是具有以下结构的JSON对象：

```
{
 "type": "send"|"publish"|"receive"|"register"|"unregister",
 "address": 发送/发布/注册/注销的信息总线地址
 "body": 消息体
}
```

注意：`SOCKET_ERROR` 事件可能会包含消息。在这种情况下检查type属性，可能会引入一种新的 消息。一个 `err` 消息。这是当一个套接字异常时会生成的一个合成消息。该消息将会 遵循桥接数据传输格式，并像下面的示例一样：

```
{
 "type": "err",
 "failureType": "socketException",
 "message": "可选的，来自被引发的异常的消息"
}
```

事件也是 `Promise` 的一个实例。 处理完事件后，您可以使用 `true` 来完成promise，以启用进一步的处理。

如果您不希望处理该事件，则可以使用 `false` 来完成promise。您可以自己过滤桥上面传递的信息， 这是一个很有用的特性， 或者应用一些细粒度的授权或指标。

这是一个示例，其中我们拒绝所有流经桥的消息（如果其中包含单词"Armadillos"）。

```java
Router router = Router.router(vertx);

// 让从客户端发送到 "demo.orderMgr"的任何消息通过
PermittedOptions inboundPermitted = new PermittedOptions()
  .setAddress("demo.someService");

SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
SockJSBridgeOptions options = new SockJSBridgeOptions()
  .addInboundPermitted(inboundPermitted);

// 将桥挂载到路由上
router
  .route("/eventbus/*")
  .subRouter(sockJSHandler
    .bridge(options, be -> {
      if (be.type() == BridgeEventType.PUBLISH ||
        be.type() == BridgeEventType.RECEIVE) {

          if (be.getRawMessage().getString("body").equals("armadillos")) {
            // Reject it
            be.complete(false);
            return;
          }
        }
        be.complete(true);
      }));
```

这是如何配置和处理SOCKET_IDLE桥事件类型的示例。 注意 `setPingTimeout(5000)`，它代表如果ping消息在5秒钟内没有从客户端到达， 则将触发SOCKET_IDLE桥事件。

```java
Router router = Router.router(vertx);

// 初始化 SockJSHandler 处理器
SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
SockJSBridgeOptions options = new SockJSBridgeOptions()
  .addInboundPermitted(inboundPermitted)
  .setPingTimeout(5000);

// 挂载桥到路由上
router
  .route("/eventbus/*")
  .subRouter(sockJSHandler.bridge(options, be -> {
    if (be.type() == BridgeEventType.SOCKET_IDLE) {
      // 做一些自定义处理
    }

      be.complete(true);
    }));
```

在客户端JavaScript中，您可以使用 `vertx-eventbus.js` 库创建与事件总线的连接并发送和接收消息：

```html
<script src="https://unpkg.io/sockjs-client@1.5.0/dist/sockjs.min.js"></script>
<script src='https://unpkg.io/@vertx/eventbus-bridge-client.js@1.0.0-1/vertx-eventbus.js'></script>

<script>

var eb = new EventBus('http://localhost:8080/eventbus', {"vertxbus_ping_interval": 300000}); // sends ping every 5 minutes.

eb.onopen = function() {

// 设置接收消息的处理器
eb.registerHandler('some-address', function(error, message) {
  console.log('received a message: ' + JSON.stringify(message));
});

// 发送信息
eb.send('some-address', {name: 'tim', age: 587});
}

</script>
```

该示例所做的第一件事是创建事件总线的实例

```javascript
var eb = new EventBus('http://localhost:8080/eventbus', {"vertxbus_ping_interval": 300000});
```

构造函数的第二个参数告诉sockjs库每5分钟发送一次ping消息。 由于服务器已配置为每5秒执行一次ping操作，因此会在服务器上触发 `SOCKET_IDLE`

您还可以修改原始消息，例如改变请求体。 对于从客户端传入的消息，您还可以在消息中添加头，下面是一个示例：

```java
Router router = Router.router(vertx);

// 让从客户端发送到 'demo.orderService' 的任何消息通过
PermittedOptions inboundPermitted = new PermittedOptions()
  .setAddress("demo.orderService");

SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
SockJSBridgeOptions options = new SockJSBridgeOptions()
  .addInboundPermitted(inboundPermitted);

// 将连接桥挂载到路由器上
router
  .route("/eventbus/*")
  .subRouter(sockJSHandler.bridge(options, be -> {
    if (
      be.type() == BridgeEventType.PUBLISH ||
        be.type() == BridgeEventType.SEND) {

      // 添加一些头
      JsonObject headers = new JsonObject()
        .put("header1", "val")
        .put("header2", "val2");

        JsonObject rawMessage = be.getRawMessage();
        rawMessage.put("headers", headers);
        be.setRawMessage(rawMessage);
      }
      be.complete(true);
    }));
```

## CSRF跨站请求伪造

CSRF或有时也称为XSRF是一种技术，未经授权的站点可以通过该技术获取用户的私人数据 Vert.x-Web包含了一个 `CSRFHandler`， 您可以使用它防止跨站伪造请求

在此处理程序的每个get请求上，都会使用唯一token将cookie添加到响应中。 然后，希望客户端在标头中返回此token。 由于发送了cookie，因此要求cookie处理程序也存在于路由上。

在开发依赖于User-Agent来执行 `POST` 操作的非单页应用程序时， 无法在HTML表单上指定标头。 为了解决此问题，还将并且仅当在表单属性中不存在与标头同名的标头时， 才检查标头值，例如：

```html
<form action="/submit" method="POST">
<input type="hidden" name="X-XSRF-TOKEN" value="abracadabra">
</form>
```

用户有责任为表单字段填写正确的值。 倾向于使用仅HTML解决方案的用户可以通过从路由上下文中获取键为 `X-XSRF-TOKEN` 或在实例化 `CSRFHandler` 对象时选择的标头名称下来填充此值。

```java
router.route().handler(CSRFHandler.create(vertx, "abracadabra"));
router.route().handler(ctx -> {

});
```

请注意，此处理程序是session感知的。 如果有可用的session，则在 `POST` 操作期间可能会省略表单参数或头，因为将从会话中读取该参数或头。 这也意味着token将仅在session升级时重新生成。

请注意，为提高安全性，建议用户旋转对token进行签名的密钥 。 可以通过替换处理器或使用新配置重新启动应用程序来在线完成此操作。 点击劫持仍然可能影响应用程序。 如果这是关键应用程序，请考虑设置标头 `X-Frame-Options` ，如以下描述中所述： https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Frame-Options

### 使用 AJAX

当通过ajax访问受保护的路由时，两个csrf token都需要在请求中传递。 通常这是使用请求标头完成的，因为添加请求标头通常可以轻松地在中心位置完成， 而无需修改有效负载。

CSRF token是从服务器端上下文中的键 `X-XSRF-TOKEN` 下获得的（除非您指定其他名称）。 通常需要通过将令牌包含在初始页面内容中来将此token公开给客户端。 一种可能性是将其存储在HTML <meta>标记中， 然后在其中可以通过JavaScript在请求时检索值。

您的视图中可以包含以下内容（下面的示例）：

```html
<meta name="csrf-token" content="${X-XSRF-TOKEN}">
```

以下是使用Fetch API将页面 <meta> 标记中的CSRF token通过POST请求/process路由的例子:

```js
// 获取<meta>标签中的CSRF token
var token = document.querySelector('meta[name="csrf-token"]').getAttribute('content')

// 使用Fetch API发送请求
fetch('/process', {
 credentials: 'same-origin', // <-- 在请求中包含cookie
 headers: {
   'X-XSRF-TOKEN': token // <-- 将CSRF token放到header中
 },
 method: 'POST',
 body: {
   key: 'value'
 }
})
```

## HSTS处理器

HTTP严格传输安全性（HSTS）是一种Web安全策略机制， 可帮助保护网站免受中间人攻击，例如协议降级攻击和cookie劫持。 它允许Web服务器声明Web浏览器（或其他符合要求的用户代理）应仅使用提供传输层安全性 （TLS/SSL）的HTTPS连接自动与其进行交互， 这与单独使用不安全的HTTP不同。HSTS是IETF标准的跟踪协议， 在RFC 6797中进行了确定。

该处理器只需一步即可为您的应用程序配置正确的标头

```java
router.route().handler(HSTSHandler.create());
```

## CSP处理器

内容安全策略（CSP）是安全性的附加层，有助于检测和缓解某些类型的攻击， 包括跨站点脚本（XSS）和数据注入攻击。 这些攻击可用于从数据盗窃， 站点攻击到恶意软件分发的各种方面

CSP设计为完全向后兼容。 不支持它的浏览器仍然可以与实现它的服务器一起使用，反之亦然： 不支持CSP的浏览器只是忽略它，照常运行，默认为Web内容的标准同源策略。 如果该站点不提供CSP标头， 则浏览器同样会使用标准的同源策略。

```java
router.route().handler(
  CSPHandler.create()
    .addDirective("default-src", "*.trusted.com"));
```

## XFrame处理器

`X-Frame-Options` HTTP响应标头可用于指示是否应允许浏览器在 `frame` ，`iframe`，`embed` 或 `object` 中呈现页面。 网站可以通过确保其内容未嵌入其他网站来避免点击劫持攻击。

仅当访问文档的用户使用支持 `X-Frame-Options` 的浏览器时， 才提供附加的安全性。

如果指定 `DENY`，则从其他站点加载时，不仅在fram中加载页面失败，而且从同一站点加载时，也会失败。 另一方面，如果您指定 `SAMEORIGIN` ，则只要frame中包含该页面的站点与提供该页面的站点相同， 您仍可以在frame中使用该页面。

此处理器将一步为您的应用程序配置正确的标头

```java
router.route().handler(XFrameHandler.create(XFrameHandler.DENY));
```

## OAuth2Auth处理器

`OAuth2AuthHandler` 允许使用OAuth2协议快速设置安全路由。该处理器简化了authCode流。 一个使用它来保护某些资源并通过GitHub进行身份验证的示例是这样的：

```java
OAuth2Auth authProvider = GithubAuth
  .create(vertx, "CLIENT_ID", "CLIENT_SECRET");

// 在您运行的服务器上创建oauth2处理器
// the second argument is the full url to the 第二个参数是一个完全的url
//  这个url用于回调，和您在提供者管理控制台输入的一致
OAuth2AuthHandler oauth2 = OAuth2AuthHandler
  .create(vertx, authProvider, "https://myserver.com/callback");

// 为接收GitHb回调设置回调处理器
oauth2.setupCallback(router.route("/callback"));

// 保护 /protected 下的一切资源
router.route("/protected/*").handler(oauth2);
// mount some handler under the protected zone
router
  .route("/protected/somepage")
  .handler(ctx -> ctx.response().end("Welcome to the protected resource!"));

// 欢迎页面
router
  .get("/")
  .handler(ctx -> ctx.response()
    .putHeader("content-type", "text/html")
    .end("Hello<br><a href=\"/protected/somepage\">Protected by Github</a>"));
```

OAuth2AuthHandler将设置适当的回调OAuth2处理器， 因此用户不需要处理授权服务器响应的验证 。 知道授权服务器响应仅有效一次非常重要， 这意味着如果客户端发出重新加载回调URL的请求，则它将被断言为无效请求， 因为验证将失效

一条经验法则是，一旦执行了有效的回调，就将客户端重定向到受保护的资源 此重定向还应该创建一个session cookie（或其他session机制）， 因此不需要用户为每个请求进行身份验证。

由于OAuth2规范的性质，使用其他OAuth2提供者需要进行一些细微的更改 但是vertx-auth为您提供了许多现成的实现：

- Azure Active Directory `AzureADAuth`
- Box.com `BoxAuth`
- Dropbox `DropboxAuth`
- Facebook `FacebookAuth`
- Foursquare `FoursquareAuth`
- Github `GithubAuth`
- Google `GoogleAuth`
- Instagram `InstagramAuth`
- Keycloak `KeycloakAuth`
- LinkedIn `LinkedInAuth`
- Mailchimp `MailchimpAuth`
- Salesforce `SalesforceAuth`
- Shopify `ShopifyAuth`
- Soundcloud `SoundcloudAuth`
- Stripe `StripeAuth`
- Twitter `TwitterAuth`

但是，如果您使用的是未列出的提供者，则仍然可以使用基本API来做到这一点，如下所示：

```java
OAuth2Auth authProvider = OAuth2Auth.create(vertx, new OAuth2Options()
  .setClientId("CLIENT_ID")
  .setClientSecret("CLIENT_SECRET")
  .setSite("https://accounts.google.com")
  .setTokenPath("https://www.googleapis.com/oauth2/v3/token")
  .setAuthorizationPath("/o/oauth2/auth"));

// 在我们的"http://localhost:8080"域上创建oauth2处理器
OAuth2AuthHandler oauth2 = OAuth2AuthHandler
  .create(vertx, authProvider, "http://localhost:8080");

// 这是它的范围
oauth2.withScope("profile");

// 为Google回调设置回调处理器
oauth2.setupCallback(router.get("/callback"));

// 保护 /protected 下的一切资源
router.route("/protected/*").handler(oauth2);
// 在受保护的区域挂载一些处理器
router
  .route("/protected/somepage")
  .handler(ctx -> ctx.response().end("Welcome to the protected resource!"));

// 欢迎页
router
  .get("/")
  .handler(ctx -> ctx.response()
    .putHeader("content-type", "text/html")
    .end("Hello<br><a href=\"/protected/somepage\">Protected by Google</a>"));
```

您将需要手动提供提供商的所有详细信息，但最终结果是相同的。

处理器会将您的应用程序固定为配置的回调URL。 用法很简单，只需为处理程序提供一个路由实例，所有设置便会为您完成。 在典型的用例中，您的提供者将询问您应用程序的回调URL是什么，然后您输入一个URL，例如：`https://myserver.comcallback`。 这是处理程序的第二个参数，现在您只需要设置它即可 。 为了使最终用户更轻松，您所需要做的就是调用setupCallback方法。

这是将处理器固定到服务器 `https://myserver.com:8447/callback` 的方式。 请注意， 端口号并不是强制为默认值，http的默认端口号是80，https的默认端口号是443。

```java
OAuth2AuthHandler oauth2 = OAuth2AuthHandler
  .create(vertx, provider, "https://myserver.com:8447/callback");

// 现在允许处理器为您设置回调url
oauth2.setupCallback(router.route("/callback"));
```

在该示例中，路由对象是由 `Router.route()` 内联创建的， 但是，如果您想完全控制处理程序的调用顺序（例如，希望在链中尽快调用它）， 则可以可以始终在此之前创建Route对象，并将其的引用传递给这个方法

### 一个真实世界的例子

到目前为止，您已经学习了如何使用Oauth2处理器，但是您会注意到，对于每个请求，您都需要进行身份验证。 这是因为处理器没有状态，并且在示例中没有应用状态管理。

尽管建议对面向API的端点不使用任何状态， 例如，对于面向用户的端点使用JWT（我们将在后面介绍），我们可以将身份验证结果存储在session中。 为此，我们需要一个类似于以下代码段的应用程序：

```java
OAuth2Auth authProvider =
  GithubAuth
    .create(vertx, "CLIENTID", "CLIENT SECRET");
// 我们也需要一个用户session处理器
// 其用来确保用户数据可以在请求之间储存在session中
router.route()
  .handler(SessionHandler.create(LocalSessionStore.create(vertx)));
// 我们现在保护 "/protected" 下的一切资源
router.route("/protected").handler(
  OAuth2AuthHandler.create(
      vertx,
      authProvider,
      "http://localhost:8080/callback")
    // 我们现在配置oauth2处理器
    // 设置回调处理器
    // 回调处理器必须符合oauth2提供者的规范
    .setupCallback(router.route("/callback"))
    // 对于这个资源，
    // 我们要求用户具有查看用户电子邮件的权限
    .withScope("user:email")
);
// 应用程序的入口点
// 其会渲染一个自定义的模板
router.get("/").handler(ctx -> ctx.response()
  .putHeader("Content-Type", "text/html")
  .end(
    "<html>\n" +
      "  <body>\n" +
      "    <p>\n" +
      "      Well, hello there!\n" +
      "    </p>\n" +
      "    <p>\n" +
      "      We're going to the protected resource, if there is no\n" +
      "      user in the session we will talk to the GitHub API. Ready?\n" +
      "      <a href=\"/protected\">Click here</a> to begin!</a>\n" +
      "    </p>\n" +
      "    <p>\n" +
      "      <b>If that link doesn't work</b>, remember to provide your\n" +
      "      own <a href=\"https://github.com/settings/applications/new\">\n" +
      "      Client ID</a>!\n" +
      "    </p>\n" +
      "  </body>\n" +
      "</html>"));
// 受保护的资源
router.get("/protected").handler(ctx -> {
  // 此时，您的user对象应包含
  // 从Oauth2响应中的信息,因为这是受保护的资源
  // 如上面在处理程序配置中指定的，用户对象永远不会为null
  User user = ctx.user();
  // 只是将其转储到客户端以进行演示
  ctx.response().end(user.toString());
});
```

### 将OAuth2和JWT混合使用

一些提供者将JWT token用作访问token，这是 [RFC6750](https://tools.ietf.org/html/rfc6750) 的功能， 当要混合基于客户端的身份验证和API授权时，此功能非常有用。 例如， 假如您有一个提供一些受保护的HTML文档的应用程序，但您还希望它可供API使用。 在这种情况下，API无法轻松执行OAuth2所需的重定向握手， 但可以使用事先提供的token。

只要提供者被配置为支持JWT，这个处理器就会自动处理此问题。

在现实生活中，这意味着您的API可以使用标头 `Authorization` ， 其值为 `Bearer BASE64 ACCESS_TOKEN` ，来访问受保护的资源。

### WebAuthn

我们的网络信息存在依赖于过时而且脆弱的密码概念的问题。 密码是介于恶意用户和您的银行帐户或社交媒体帐户之间的东西。 密码很难维护。很难将它们存储在服务器上 （密码被盗）。他们很难记住，或者不告诉别人（网络钓鱼攻击）。

但是现在有一个更好的方法！一个无需密码的世界，它是浏览器上运行的W3C和FIDO Alliance的标准

WebAuthn是一种API，它允许服务器使用公钥加密（而不是密码）来注册和验证用户 ， 这种API使用用户可访问的方法借助身份认证设备， 比如yubikey token或者您的手机，进行加密

该协议至少要求将第一个回调挂载在路由上：

1. `/webauthn/response` 用于执行所有验证的回调
2. `/webauthn/login` 允许用户启动登录流程的端点（可选，但如果没有它，它将无法登录）
3. `/webauthn/register` 允许用户注册新的身份标识的端点（可选，如果数据已经存储，则不需要该端点）

这是一个受保护的应用程序的示例：

```java
WebAuthn webAuthn = WebAuthn.create(
  vertx,
  new WebAuthnOptions()
    .setRelyingParty(new RelyingParty().setName("Vert.x WebAuthN Demo"))
    // 哪种认证类型是您想要的，是您关心的？
    // # 密钥
    .setAuthenticatorAttachment(AuthenticatorAttachment.CROSS_PLATFORM)
    // # 指纹
    .setAuthenticatorAttachment(AuthenticatorAttachment.PLATFORM)
    .setUserVerification(UserVerification.REQUIRED))
  // 从哪里加载证书？
  .authenticatorFetcher(fetcher)
  // 更新认证状态
  .authenticatorUpdater(updater);

// 解析请求体
router.post()
  .handler(BodyHandler.create());
// 添加session处理器
router.route()
  .handler(SessionHandler
    .create(LocalSessionStore.create(vertx)));

// 安全处理器
WebAuthnHandler webAuthNHandler = WebAuthnHandler.create(webAuthn)
  .setOrigin("https://192.168.178.74.xip.io:8443")
  // 需要回调
  .setupCallback(router.post("/webauthn/response"))
  // 可选的注册回调
  .setupCredentialsCreateCallback(router.post("/webauthn/register"))
  // 可选的登录回调
  .setupCredentialsGetCallback(router.post("/webauthn/login"));

// 保护其余的Route
router.route().handler(webAuthNHandler);
```

该应用程序在后端不安全，但是需要在客户端执行一些代码 。 需要一些具有以下两个功能的样板

```javascript
/**
* 将公钥证书转化为可序列化的JSON
* @param  {Object} pubKeyCred 公钥证书
* @return {Object}            - 编码为JSON的公钥证书
*/
var publicKeyCredentialToJSON = (pubKeyCred) => {
 if (pubKeyCred instanceof Array) {
   let arr = [];
   for (let i of pubKeyCred) { arr.push(publicKeyCredentialToJSON(i)) }

   return arr
 }

 if (pubKeyCred instanceof ArrayBuffer) {
   return base64url.encode(pubKeyCred)
 }

 if (pubKeyCred instanceof Object) {
   let obj = {};

   for (let key in pubKeyCred) {
     obj[key] = publicKeyCredentialToJSON(pubKeyCred[key])
   }

   return obj
 }

 return pubKeyCred
};

/**
* 生成安全随机数的Buffer
* @param  {Number} len - buffer的长度（默认为32byte）
* @return {Uint8Array} - 随机字符串
*/
var generateRandomBuffer = (len) => {
 len = len || 32;

 let randomBuffer = new Uint8Array(len);
 window.crypto.getRandomValues(randomBuffer);

 return randomBuffer
};

/**
* 解码arrayBuffer必填字段
*/
var preformatMakeCredReq = (makeCredReq) => {
 makeCredReq.challenge = base64url.decode(makeCredReq.challenge);
 makeCredReq.user.id = base64url.decode(makeCredReq.user.id);

 return makeCredReq
};

/**
* 解码arrayBuffer必填字段
*/
var preformatGetAssertReq = (getAssert) => {
 getAssert.challenge = base64url.decode(getAssert.challenge);

 for (let allowCred of getAssert.allowCredentials) {
   allowCred.id = base64url.decode(allowCred.id)
 }

 return getAssert
};
```

这些功能将帮助您与服务器进行交互。话不多说。让我们从登录用户开始：

```javascript
// 使用之前定义的函数
getGetAssertionChallenge({name: 'your-user-name'})
.then((response) => {
 // base64必须解码为JavaScript Buffer
 let publicKey = preformatGetAssertReq(response);
 // 这个响应之后会传给浏览器
 // 用于通过于您的token/手机等互动产生断言
 return navigator.credentials.get({publicKey})
})
.then((response) => {
 // 将响应的buffer转为base64和json
 let getAssertionResponse = publicKeyCredentialToJSON(response);
 // 向服务器传送数据
 return sendWebAuthnResponse(getAssertionResponse)
})
.then((response) => {
 // 成功
 alert('Login success')
})
.catch((error) => alert(error));

// 工具函数

let sendWebAuthnResponse = (body) => {
 return fetch('/webauthn/response', {
   method: 'POST',
   credentials: 'include',
   headers: {
     'Content-Type': 'application/json'
   },
   body: JSON.stringify(body)
 })
   .then(response => {
     if (!response.ok) {
       throw new Error(`Server responded with error: ${response.statusText}`);
     }
     return response;
   })
};

let getGetAssertionChallenge = (formBody) => {
 return fetch('/webauthn/login', {
   method: 'POST',
   credentials: 'include',
   headers: {
     'Content-Type': 'application/json'
   },
   body: JSON.stringify(formBody)
 })
   .then(response => {
     if (!response.ok) {
       throw new Error(`Server responded with error: ${response.statusText}`);
     }
     return response;
   })
   .then((response) => response.json())
};
```

上面的示例已经涵盖了66％的API，涵盖了3个端点中的2个。 最后的端点是用户注册。用户注册是将新密钥注册到服务器凭证存储中并映射到用户的过程， 当然，在客户端上创建了私钥并将其与服务器相关联， 但是此密钥从未离开硬件令牌或手机安全芯片。

要注册用户并重用上面已经定义的大多数功能，请执行以下操作：

```javascript
/* 处理注册表 */
getMakeCredentialsChallenge({name: 'myalias', displayName: 'Paulo Lopes'})
.then((response) => {
 // 将询问和id转为buffer并执行注册
 let publicKey = preformatMakeCredReq(response);
 //  创建一个新的密钥对
 return navigator.credentials.create({publicKey})
})
.then((response) => {
 // 将响应从buffer转为json
 let makeCredResponse = window.publicKeyCredentialToJSON(response);
 //  传送给服务器以辨认用户身份
 return sendWebAuthnResponse(makeCredResponse)
})
.then((response) => {
 alert('Registration completed')
})
.catch((error) => alert(error));

// 工具函数

let getMakeCredentialsChallenge = (formBody) => {
 return fetch('/webauthn/register', {
   method: 'POST',
   credentials: 'include',
   headers: {
     'Content-Type': 'application/json'
   },
   body: JSON.stringify(formBody)
 })
   .then(response => {
     if (!response.ok) {
       throw new Error(`Server responded with error: ${response.statusText}`);
     }
     return response;
   })
   .then((response) => response.json())
};
```

> **😎警告:** 由于 API 的安全性，浏览器将不允许您在纯文本 HTTP 上使用此 API。 所有请求都必须通过 HTTPS。

> **😎警告:** WebAuthN 需要具有有效 TLS 证书的 HTTPS， 您也可以在开发过程中使用自签名证书。

### 一次性密码 (多重因子身份验证)

Vert.x 也支持多重因子身份验证（译者注：简称 MFA）。有两种使用 MFA 的方式：

- `HOTP` - 基于哈希的一次性密码
- `TOTP` - 基于时间的一次性密码

这些使用 MFA 的方式用法相同，因此您可以在构造方法级别 选择一个您需要的使用方式。

这个处理器的工作方式如下：

如果当前请求中不包含 `User` ，那么处理器会假定用户之前没有经过 身份认证。这意味着请求将立即终止，并且返回的 HTTP 状态码为 401。

如果当前请求中包含用户，但对象中缺少具有匹配类型(`hotp`/`totp`)的 `mfa` 属性，请求会 被重定向至身份验证 url (如果提供了的话)，否则请求也会被立即终止。这个 url 应该提供一种方式 以供用户输入验证码，例如：

```html
<html>
<head>
 <meta charset="UTF-8">
 <title>OTP Authenticator Verification Example Page</title>
</head>
<body>
<form action="/otp/verify" method="post" enctype="multipart/form-data">
 <div>
   <label>Code:</label>
   <input type="text" name="code"/><br/>
 </div>
 <div>
   <input type="submit" value="Submit"/>
 </div>
</form>
</body>
</html>
```

用户输入完验证码后，请求会被重定向至初始的 url，如果不知道初始 url 的话，请求会被重定向至 `/`。

当然以上流程假定身份验证程序或设备已经配置好了。为了配置一个新的 应用或设备，一个示例 HTML 网页代码如下：

```html
<html>
<head>
 <title>OTP Authenticator Registration Example Page</title>
</head>
<body>
 <p>Scan this QR Code in Google Authenticator</p>
 <img id="qrcode">
 <p>- or enter this key manually -</p>
 <span id="url"></span>

 <script>
 const key = document.getElementById('url');
 const qrcode = document.getElementById('qrcode');

 fetch(
   '/otp/register',
   {
     method: 'POST',
     headers: {
       'Accept': 'application/json'
     }
   })
   .then(res => {
     if (res.status === 200) {
       return res;
     }
     throw new Error(res.statusText);
   })
   .catch(err => console.error(err))
   .then(res => res.json())
   .then(json => {
     key.innerText = json.url;
     qrcode.src =
       'https://chart.googleapis.com/chart?chs=166x166&chld=L|0&cht=qr&chl=' +
       encodeURIComponent(json.url);
   });
 </script>
</body>
</html>
```

以上示例代码中的重点是脚本会发送一个 `POST` 请求给配置好的注册回调。 如果请求中用户仍然没有通过身份验证，那么注册回调将再次返回 HTTP 状态码 401。而如果 身份验证成功，程序会返回一段 JSON 数据，其包含有一个 url，和一些额外的元数据。这个 url 用于配置身份验证器， 验证的方式可以是手动输入数据，也可以是在显示一个二维码。要显示的二维码可以在前端渲染， 也可以在后端渲染。简单起见，以上示例使用了 google charts API 来在 浏览器上渲染二维码

最后，这就是您在 vert.x 应用程序中使用处理程序的方式：

```java
router.post()
  .handler(BodyHandler.create());
// add a session handler (OTP requires state)
router.route()
  .handler(SessionHandler
    .create(LocalSessionStore.create(vertx))
    .setCookieSameSite(CookieSameSite.STRICT));

// add the first authentication mode, for example HTTP Basic Authentication
router.route()
  .handler(basicAuthHandler);

final OtpAuthHandler otp = OtpAuthHandler
  .create(TotpAuth.create()
    .authenticatorFetcher(authr -> {
      // fetch authenticators from a database
      // ...
      return Future.succeededFuture(new io.vertx.ext.auth.otp.Authenticator());
    })
    .authenticatorUpdater(authr -> {
      // update or insert authenticators from a database
      // ...
      return Future.succeededFuture();
    }));

otp
  // the issuer for the application
  .issuer("Vert.x Demo")
  // handle code verification responses
  .verifyUrl("/verify-otp.html")
  // handle registration of authenticators
  .setupRegisterCallback(router.post("/otp/register"))
  // handle verification of authenticators
  .setupCallback(router.post("/otp/verify"));

// secure the rest of the routes
router.route()
  .handler(otp);

// To view protected details, user must be authenticated and
// using 2nd factor authentication
router.get("/protected")
  .handler(ctx -> ctx.end("Super secret content"));
```

### 处理HTTP请求方法覆盖

许多公司和其他服务都限制了它们允许外部使用的REST HTTP方法。有些不太严密的可以允许使用任意的方法，大多数仅允许一个很小但不错的集合，有些则只允许GET和POST。造成这种限制的原因各不相同：浏览器或客户端限制或非常严格的公司防火墙。仅具有GET和POST的Web服务不能很好地表达REST的思想。PUT，DELETE，OPTIONS等对于指定对资源执行的操作非常有用。为了解决这个问题，它创建了X-HTTP-METHOD-OVERRIDE HTTP标头作为解决方法。

通过使用GET/POST发送请求以及该请求应在X-HTTP-METHOD-OVERRIDE HTTP标头内设置真正的请求方法，服务器应识别该标头并重定向到适当的方法

Vert.x允许这样做，只需：

```java
router.route().handler(MethodOverrideHandler.create());

router.route(HttpMethod.GET, "/").handler(ctx -> {
  // 做 GET 相关操作……
});

router.route(HttpMethod.POST, "/").handler(ctx -> {
  // 做 POST 相关操作……
});
```

由于它将重定向请求，因此明智的做法是避免不必要地触发请求处理器，因此最好将MethodOverrideHandler添加为第一个处理器。

另外，请注意：这可能会成为不怀好意的人的攻击对象！

为了缓解这种问题，默认情况下，MethodOverrideHandler附带了一个安全降级策略。这项策略规定，在以下情况下，X-HTTP-METHOD-OVERRIDE中包含的方法可以覆盖原始方法：

- 覆盖方法是幂等的; 或者
- 覆盖方法是安全的，并且要覆盖的方法不是幂等的; 或者
- 被覆盖的方法不安全.

尽管我们不建议这样做，但Vert.x不会强迫您采取任何措施。如果您希望允许任何覆盖，则：

```java
router.route().handler(MethodOverrideHandler.create(false));

router.route(HttpMethod.GET, "/").handler(ctx -> {
  // 做 GET 相关操作……
});

router.route(HttpMethod.POST, "/").handler(ctx -> {
  // 做 POST 相关操作……
});
```

------

<<<<<< [完] >>>>>>

