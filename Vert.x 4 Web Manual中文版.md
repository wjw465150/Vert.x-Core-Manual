# Vert.x 4 Web Manual中文版

Vert.x-Web 是一组构建块，用于使用 Vert.x 构建 Web 应用程序。 将其视为构建现代、可扩展的 Web 应用程序的瑞士军刀。

Vert.x 核心提供了一组相当低级的功能来处理 HTTP，并且对于某些应用程序来说已经足够了。

Vert.x-Web 建立在 Vert.x 核心之上，为更轻松地构建真正的 Web 应用程序提供了更丰富的功能集。

它是 Vert.x 2.x 中 [Yoke](http://pmlopes.github.io/yoke/) 的继承者，灵感来自于 [Express](http://expressjs.com/) 等项目 Node.js 世界和 Ruby 世界中的 [Sinatra](http://www.sinatrarb.com/)。

Vert.x-Web 被设计为功能强大、非强制且完全可嵌入的。 您只需使用您想要的部分，仅此而已。 Vert.x-Web 不是容器。

您可以使用 Vert.x-Web 创建经典的服务器端 Web 应用程序、RESTful Web 应用程序、“实时”（服务器推送）Web 应用程序或您能想到的任何其他类型的 Web 应用程序。 Vert.x-Web 不在乎。 您可以选择自己喜欢的应用类型，而不是 Vert.x-Web。

Vert.x-Web 非常适合编写 RESTful HTTP 微服务，**但我们不会强迫**您编写这样的应用程序。

Vert.x-Web 的一些主要功能包括：

- 路由（基于方法、路径等）
- 路径的正则表达式模式匹配
- 从路径中提取参数
- 内容协商
- 请求包体处理
- 请求包体大小限制
- Multipart forms(多表格)
- 多部分文件上传
- 子路由
- 会话支持 - 本地（用于粘性会话）和集群（用于非粘性）
- CORS（跨源资源共享）支持
- 错误页面处理程序
- HTTP 基本/摘要式身份验证
- 基于重定向的身份验证
- 授权处理程序
- 基于 JWT/OAuth2 的授权
- 用户/角色/权限授权
- 网站图标处理
- 对服务器端渲染的模板支持，包括对以下开箱即用的模板引擎的支持：
  - Handlebars
  - Jade,
  - MVEL
  - Thymeleaf
  - Apache FreeMarker
  - Pebble
  - Rocker
- 响应时间处理程序
- 静态文件服务，包括缓存逻辑和目录列表。
- 请求超时支持
- SockJS 支持
- Event-bus 桥接器
- CSRF跨站请求伪造
- 虚拟主机

Vert.x-Web 中的大多数功能都是作为处理程序实现的，因此您始终可以编写自己的。 我们设想随着时间的推移会写出更多的东西。

我们将在本手册中讨论所有这些功能。

## 使用 Vert.x Web

要使用 vert.x web，请将以下依赖项添加到构建描述符的 *dependencies* 部分：

- Maven (在你的 `pom.xml`):

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-web</artifactId>
 <version>4.2.5</version>
</dependency>
```

- Gradle (在你的 `build.gradle`):

```groovy
dependencies {
 compile 'io.vertx:vertx-web:4.2.5'
}
```

### 开发模式

Vert.x Web 默认在生产模式下运行。 您可以通过将 `dev` 值分配给以下任一方式来切换开发模式：

- `VERTXWEB_ENVIRONMENT` 环境变量，或
- `vertxweb.environment` 系统属性

在开发模式下：

- 模板引擎缓存被禁用
- `ErrorHandler` 不显示异常详细信息
- `StaticHandler` 不处理缓存头
- GraphiQL 开发工具被禁用

## 重温 Vert.x Core 的 HTTP 服务器

Vert.x-Web 使用并公开来自 Vert.x 核心的 API，因此如果您还没有熟悉使用 Vert.x 核心编写 HTTP 服务器的基本概念，那么非常值得熟悉。

Vert.x 核心 HTTP 文档对此进行了详细介绍。

这是一个使用 Vert.x 核心编写的 `hello world web` 服务器。 此时不涉及 Vert.x-Web模块：

```java
HttpServer server = vertx.createHttpServer();

server.requestHandler(request -> {

  // This handler gets called for each request that arrives on the server
  HttpServerResponse response = request.response();
  response.putHeader("content-type", "text/plain");

  // Write to the response and end it
  response.end("Hello World!");
});

server.listen(8080);
```

我们创建一个 HTTP 服务器实例，并在其上设置一个请求处理程序。 只要请求到达服务器，就会调用请求处理程序。

发生这种情况时，我们只需将内容类型设置为 `text/plain`，并编写 `Hello World!` 并结束响应。

然后我们告诉服务器在端口“8080”（默认主机是“localhost”）进行监听。

你可以运行它，然后将浏览器指向 `http://localhost:8080` 以验证它是否按预期工作。

## 基本的 Vert.x-Web 概念

`Router` 是 Vert.x-Web 的核心概念之一。 它是一个维护零个或多个 `Routes` 的对象。

一个router 接受 HTTP 请求并找到该请求的第一个匹配路由，并将请求传递给该路由。

路由可以有一个与之关联的 *handler*，然后它会接收请求。 然后，您对请求*做某事*，然后结束它或将其传递给下一个匹配的处理程序。

这是一个简单的Router示例：

```java
HttpServer server = vertx.createHttpServer();

Router router = Router.router(vertx);

router.route().handler(ctx -> {

  // This handler will be called for every request
  HttpServerResponse response = ctx.response();
  response.putHeader("content-type", "text/plain");

  // Write to the response and end it
  response.end("Hello World from Vert.x-Web!");
});

server.requestHandler(router).listen(8080);
```

它与上一节中的 Vert.x Core HTTP 服务器 hello world 示例基本相同，但这次使用的是 Vert.x-Web。

我们像以前一样创建一个 HTTP 服务器，然后我们创建一个router。 完成后，我们将创建一个没有匹配条件的简单router，以便匹配到达服务器的*所有*请求。

然后我们为该router指定一个处理程序。 所有到达服务器的请求都会调用该处理程序。

传递给处理程序的对象是一个 `RoutingContext` - 它包含标准的 Vert.x `HttpServerRequest` 和 `HttpServerResponse` 以及其他各种使 Vert.x-Web 工作更简单的有用的东西。

对于每个被路由的请求，都有一个唯一的路由上下文实例，并且相同的实例被传递给该请求的所有处理程序。

设置处理程序后，我们设置 HTTP 服务器的请求处理程序以将所有传入请求传递给 `handle`。

所以，这就是基础。 现在我们将更详细地看一下：

## 处理请求并调用下一个处理程序

当 Vert.x-Web 决定将请求路由到匹配的路由时，它会调用路由的处理程序，传入 `RoutingContext` 的实例。 一个路由可以有不同的处理程序，你可以使用 `handler` 来追加

如果你没有在你的处理程序中结束响应，你应该调用 `next` 以便另一个匹配的路由可以处理请求（如果有的话）。

在处理程序完成执行之前，您不必调用 `next`。 如果您愿意，您可以稍后再执行此操作：

```java
Route route = router.route("/some/path/");
route.handler(ctx -> {

  HttpServerResponse response = ctx.response();
  // enable chunked responses because we will be adding data as
  // we execute over other handlers. This is only required once and
  // only if several handlers do output.
  response.setChunked(true);

  response.write("route1\n");

  // Call the next matching route after a 5 second delay
  ctx.vertx().setTimer(5000, tid -> ctx.next());
});

route.handler(ctx -> {

  HttpServerResponse response = ctx.response();
  response.write("route2\n");

  // Call the next matching route after a 5 second delay
  ctx.vertx().setTimer(5000, tid -> ctx.next());
});

route.handler(ctx -> {

  HttpServerResponse response = ctx.response();
  response.write("route3");

  // Now end the response
  ctx.response().end();
});
```

在上面的示例中，`route1` 被写入响应，然后 5 秒后 `route2` 被写入响应，然后 5 秒后 `route3` 被写入响应并结束响应。

请注意，这一切都是在没有任何线程阻塞的情况下发生的。

## 简单响应

处理程序非常强大，因为它们允许您构建非常复杂的应用程序。 对于简单的响应，例如，直接从 vert.x API 返回异步响应，router 包含一个处理程序的快捷方式，以确保：

1. 响应以 JSON 格式返回。
2. 如果处理处理程序发生错误，则返回相应的错误。
3. 如果序列化对 JSON 的响应出错，则返回相应的错误。

```java
router
  .get("/some/path")
  // this handler will ensure that the response is serialized to json
  // the content type is set to "application/json"
  .respond(
    ctx -> Future.succeededFuture(new JsonObject().put("hello", "world")));

router
  .get("/some/path")
  // this handler will ensure that the Pojo is serialized to json
  // the content type is set to "application/json"
  .respond(
    ctx -> Future.succeededFuture(new Pojo()));
```

但是，如果提供的函数调用 `write` 或 `end`，您也可以将其用于非 JSON 响应：

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
  // in this case, the handler ensures that the connection is ended
  .respond(
    ctx -> ctx
      .response()
        .setChunked(true)
        .write("Write some text..."));
```

## 使用阻塞处理程序

有时，您可能必须在处理程序中做一些事情，这可能会阻塞事件循环一段时间，例如 调用旧的阻塞 API 或进行一些密集计算。

您无法在普通处理程序中执行此操作，因此我们提供了在路由上设置阻塞处理程序的能力。

阻塞处理程序看起来就像一个普通处理程序，但它是由 Vert.x 使用来自工作池的线程而不是使用事件循环来调用的。

您使用 `blockingHandler` 在路由上设置阻塞处理程序。 这是一个例子：

```java
router.route().blockingHandler(ctx -> {

  // Do something that might take some time synchronously
  service.doSomethingThatBlocks();

  // Now call the next handler
  ctx.next();

});
```

默认情况下，在同一个上下文（例如同一个 Verticle 实例）上执行的任何阻塞处理程序都是 *ordered* - 这意味着在前一个完成之前不会执行下一个。 如果您不关心排序并且不介意并行执行的阻塞处理程序，您可以使用 `blockingHandler` 将指定 `ordered` 的阻塞处理程序设置为 false。

请注意，如果您需要处理来自阻塞处理程序的多部分表单数据，则必须首先使用非阻塞处理程序才能调用 `setExpectMultipart(true)`。 这是一个例子：

```java
router.post("/some/endpoint").handler(ctx -> {
  ctx.request().setExpectMultipart(true);
  ctx.next();
}).blockingHandler(ctx -> {
  // ... Do some blocking operation
});
```

## 按确切路径来路由

可以设置路由来匹配来自请求URI的路径。在这种情况下，它将匹配任何具有与指定路径相同路径的请求。

在以下示例中，将为请求`/some/path/`调用处理程序。 我们也忽略尾随斜杠，因此它也会被调用路径`/some/path`和`/some/path//`：

```java
Route route = router.route().path("/some/path/");

route.handler(ctx -> {
  // This handler will be called for the following request paths:

  // `/some/path/`
  // `/some/path//`
  //
  // but not:
  // `/some/path` the end slash in the path makes it strict
  // `/some/path/subdir`
});

// paths that do not end with slash are not strict
// this means that the trailing slash is optional
// and they match regardless
Route route2 = router.route().path("/some/path");

route2.handler(ctx -> {
  // This handler will be called for the following request paths:

  // `/some/path`
  // `/some/path/`
  // `/some/path//`
  //
  // but not:
  // `/some/path/subdir`
});
```

## 通过以某样东西开始的路径进行路由

通常，您希望路由以特定路径开头的所有请求。 您可以使用正则表达式来执行此操作，但一种简单的方法是在声明路由路径时在路径末尾使用星号`*`。

在以下示例中，将为任何具有以 `/some/path/` 开头的 URI 路径的请求调用处理程序。

例如 `/some/path/foo.html` 和 `/some/path/otherdir/blah.css` 都会匹配。

```java
Route route = router.route().path("/some/path/*");

route.handler(ctx -> {
  // This handler will be called for any path that starts with
  // `/some/path/`, e.g.

  // `/some/path/`
  // `/some/path/subdir`
  // `/some/path/subdir/blah.html`
  //
  // but **ALSO**:
  // `/some/path` the final slash is always optional with a wildcard to preserve
  //              compatibility with many client libraries.
  // but **NOT**:
  // `/some/patha`
  // `/some/patha/`
  // etc...
});
```

对于任何路径，也可以在创建路由时指定：

```java
Route route = router.route("/some/path/*");

route.handler(ctx -> {
  // This handler will be called same as previous example
});
```

## 获取路径参数

可以使用参数的占位符来匹配路径，然后在上下文 `pathParam` 中可用。

这是一个例子

```java
router
  .route(HttpMethod.POST, "/catalogue/products/:productType/:productID/")
  .handler(ctx -> {

    String productType = ctx.pathParam("productType");
    String productID = ctx.pathParam("productID");

    // Do something with them...
  });
```

占位符由 `:` 后跟参数名称组成。 参数名称由任何字母字符、数字字符或下划线组成。 在某些情况下，这有点受限，因此用户可以切换到包含 2 个额外字符 `-` 和 `$` 的扩展名称规则。 扩展参数规则作为系统属性启用：

```
-Dio.vertx.web.route.param.extended-pattern=true
```

在上面的示例中，如果向路径发出 POST 请求：`/catalogue/products/tools/drill123/`，则路由将匹配，`productType` 将收到值 `tools`，`productID` 将收到值`drill123`。

参数不需要是路径段。 例如，像下面这样的路径参数也是有效的：

```java
router
  .route(HttpMethod.GET, "/flights/:from-:to")
  .handler(ctx -> {
    // when handling requests to /flights/AMS-SFO will set:
    String from = ctx.pathParam("from"); // AMS
    String to = ctx.pathParam("to"); // SFO
    // remember that this will not work as expected when the parameter
    // naming pattern in use is not the "extended" one. That is because in
    // that case "-" is considered to be part of the variable name and
    // not a separator.
  });
```


## 使用正则表达式来路由

正则表达式也可用于匹配路由中的 URI 路径。

```java
Route route = router.route().pathRegex(".*foo");

route.handler(ctx -> {

  // This handler will be called for:

  // /some/path/foo
  // /foo
  // /foo/bar/wibble/foo
  // /bar/foo

  // But not:
  // /bar/wibble
});
```

或者，可以在创建路由时指定正则表达式：

```java
Route route = router.routeWithRegex(".*foo");

route.handler(ctx -> {

  // This handler will be called same as previous example

});
```

## 使用正则表达式捕获路径参数

您还可以在使用正则表达式时捕获路径参数，这是一个示例：

```java
Route route = router.routeWithRegex(".*foo");

// This regular expression matches paths that start with something like:
// "/foo/bar" - where the "foo" is captured into param0 and the "bar" is
// captured into param1
route.pathRegex("\\/([^\\/]+)\\/([^\\/]+)").handler(ctx -> {

  String productType = ctx.pathParam("param0");
  String productID = ctx.pathParam("param1");

  // Do something with them...
});
```

在上面的示例中，如果向路径发出请求：`/tools/drill123/`，则路由将匹配，`productType` 将收到值`tools`，`productID` 将收到值`drill123`。

捕获用带有捕获组的正则表达式表示（即用圆括号包围捕获）

## 使用命名捕获组

在某些情况下，使用 int 索引参数名称可能会很麻烦。 可以在正则表达式路径中使用命名的捕获组。

```java
router
  .routeWithRegex("\\/(?<productType>[^\\/]+)\\/(?<productID>[^\\/]+)")
  .handler(ctx -> {

    String productType = ctx.pathParam("productType");
    String productID = ctx.pathParam("productID");

    // Do something with them...
  });
```

在上面的示例中，命名捕获组映射到与组同名的路径参数。

此外，您仍然可以像使用普通组一样访问组参数（即 `params0、params1...`）

## 通过 HTTP的Method来路由

默认情况下，路由将匹配所有 HTTP Method。

如果您希望路由仅匹配特定的 HTTP Method，您可以使用 `method`

```java
Route route = router.route().method(HttpMethod.POST);

route.handler(ctx -> {

  // This handler will be called for any POST request

});
```

或者您可以在创建路由时使用路径指定它：

```java
Route route = router.route(HttpMethod.POST, "/some/path/");

route.handler(ctx -> {
  // This handler will be called for any POST request
  // to a URI path starting with /some/path/
});
```

如果要路由特定的 HTTP Method，还可以使用以 HTTP Method名称命名的方法，例如 `get`、`post` 和 `put`。 例如：

```java
router.get().handler(ctx -> {

  // Will be called for any GET request

});

router.get("/some/path/").handler(ctx -> {

  // Will be called for any GET request to a path
  // starting with /some/path

});

router.getWithRegex(".*foo").handler(ctx -> {

  // Will be called for any GET request to a path
  // ending with `foo`

});
```

如果你想指定一个路由将匹配多个 HTTP Method，你可以多次调用 `method`：

```java
Route route = router.route().method(HttpMethod.POST).method(HttpMethod.PUT);

route.handler(ctx -> {

  // This handler will be called for any POST or PUT request

});
```

如果您正在创建一个需要自定义 HTTP 谓词的应用程序，例如，“WebDav”服务器，那么您可以指定自定义谓词，例如：

```java
Route route = router.route()
  .method(HttpMethod.valueOf("MKCOL"))
  .handler(ctx -> {
    // This handler will be called for any MKCOL request
  });
```

> **🏷注意:** 重要的是要注意，诸如重新路由之类的功能将不接受自定义 HTTP Method，并且检查路由动词将产生枚举值`OTHER`而不是自定义名称。

## 路由顺序

默认情况下，路由按照添加到路由器的顺序进行匹配。

当请求到达时，路由器将遍历每个路由并检查它是否匹配，如果匹配，则将调用该路由的处理程序。

如果处理程序随后调用 `next`，则将调用下一个匹配路由（如果有）的处理程序。 等等。

这是一个例子来说明这一点：

```java
router
  .route("/some/path/")
  .handler(ctx -> {

    HttpServerResponse response = ctx.response();
    // enable chunked responses because we will be adding data as
    // we execute over other handlers. This is only required once and
    // only if several handlers do output.
    response.setChunked(true);

    response.write("route1\n");

    // Now call the next matching route
    ctx.next();
  });

router
  .route("/some/path/")
  .handler(ctx -> {

    HttpServerResponse response = ctx.response();
    response.write("route2\n");

    // Now call the next matching route
    ctx.next();
  });

router
  .route("/some/path/")
  .handler(ctx -> {

    HttpServerResponse response = ctx.response();
    response.write("route3");

    // Now end the response
    ctx.response().end();
  });
```

在上面的示例中，响应将包含：

```
route1
route2
route3
```

因为任何以`/some/path`开头的请求都按该顺序调用了路由。

如果你想覆盖路由的默认顺序，你可以使用 `order` 来实现，指定一个整数值。

路由在创建时被分配一个与它们添加到路由器的顺序相对应的顺序，第一个路由编号为`0`，第二个路由编号为`1`，依此类推。

通过指定路由的顺序，你可以覆盖默认的顺序。Order也可以是负数，例如，如果你想确保路由在路由号' 0 '之前被评估。

让我们更改 route2 的顺序，使其在 route1 之前运行：

```java
router
  .route("/some/path/")
  .order(1)
  .handler(ctx -> {

    HttpServerResponse response = ctx.response();
    response.write("route1\n");

    // Now call the next matching route
    ctx.next();
  });

router
  .route("/some/path/")
  .order(0)
  .handler(ctx -> {

    HttpServerResponse response = ctx.response();
    // enable chunked responses because we will be adding data as
    // we execute over other handlers. This is only required once and
    // only if several handlers do output.
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

then the response will now contain:

```
route2
route1
route3
```

如果两个匹配的路由具有相同的 order 值，那么它们将按照它们添加的顺序被调用。

您还可以使用 `last` 指定最后处理路由

> **🏷注意:** 路由顺序只能在配置处理程序之前指定！

## 基于请求的 MIME 类型的路由

您可以使用 `consumes` 指定路由将与匹配的请求 MIME 类型匹配。

在这种情况下，请求将包含一个 `content-type` 报头，指定请求正文的 MIME 类型。 这将与 `consumes` 中指定的值匹配。

基本上，`consumes` 描述了处理程序可以*消费*的 MIME 类型。

Matching can be done on exact MIME type matches:

```java
router.route()
  .consumes("text/html")
  .handler(ctx -> {

    // This handler will be called for any request with
    // content-type header set to `text/html`

  });
```

也可以指定多个精确匹配：

```java
router.route()
  .consumes("text/html")
  .consumes("text/plain")
  .handler(ctx -> {

    // This handler will be called for any request with
    // content-type header set to `text/html` or `text/plain`.

  });
```

支持匹配子类型的通配符：

```java
router.route()
  .consumes("text/*")
  .handler(ctx -> {

    // This handler will be called for any request
    // with top level type `text` e.g. content-type
    // header set to `text/html` or `text/plain`
    // will both match

  });
```

你还可以在顶级类型上进行匹配

```java
router.route()
  .consumes("*/json")
  .handler(ctx -> {

    // This handler will be called for any request with sub-type json
    // e.g. content-type header set to `text/json` or
    // `application/json` will both match

  });
```

如果您没有在消费者中指定 `/`，它将假定您的意思是子类型。

## 基于客户端可接受的 MIME 类型的路由

HTTP `accept` 报头用于表示客户端可以接受哪些 MIME 类型的响应。

一个 `accept` 报头可以有多个 MIME 类型，由 ',' 分隔。

MIME 类型还可以附加一个“q”值*，这表示如果有多个响应 MIME 类型与接受报头匹配，则应用权重。 q 值是一个介于 0 和 1.0 之间的数字。 如果省略，则默认为 1.0。

例如，以下 `accept` 报头表示客户端将仅接受 `text/plain` 的 MIME 类型：`Accept: text/plain`

在以下情况下，客户端将接受' text/plain '或' text/html '，不带任何偏好:  `Accept: text/plain, text/html`

下面的客户端将接受`text/plain` 或 `text/html`，但更喜欢 `text/html`，因为它有更高的`q`值(默认值是q=1.0) : `Accept: text/plain; q=0.9, text/html`.如果服务器可以同时提供 `text/plain` 和 `text/html` 它应该在这种情况下提供 `text/html`。

通过使用 `produces`，您可以定义路由产生的 MIME 类型，例如 以下处理程序产生一个 MIME 类型为 `application/json` 的响应。

```java
router.route()
  .produces("application/json")
  .handler(ctx -> {

    HttpServerResponse response = ctx.response();
    response.putHeader("content-type", "application/json");
    response.end(someJSON);

  });
```

在这种情况下，路由将匹配任何带有匹配 `application/json` 的 `accept` 报头的请求。

以下是一些匹配的 `accept` 报头示例：
```
Accept: application/json Accept: application/* Accept: application/json, text/html Accept: application/json;q=0.7, text/html;q=0.8, text/plain
```

您还可以将您的路线标记为产生多个 MIME 类型。 如果是这种情况，那么您可以使用 `getAcceptableContentType` 来找出被接受的实际 MIME 类型。

```java
router.route()
  .produces("application/json")
  .produces("text/html")
  .handler(ctx -> {

    HttpServerResponse response = ctx.response();

    // Get the actual MIME type acceptable
    String acceptableContentType = ctx.getAcceptableContentType();

    response.putHeader("content-type", acceptableContentType);
    response.end(whatever);
  });
```

在上面的示例中，如果您发送的请求带有以下 `accept` 报头：`Accept: application/json; q=0.7, text/html`

然后路由将匹配，并且 `acceptableContentType` 将包含 `text/html`，因为两者都是可接受的，但是有一个具有更高的 `q` 值。

## 基于VirtualHost的路由

您可以配置 `Route` 将匹配请求主机名。

根据 `Host` 标头检查请求是否匹配，并且模式允许使用 `**` 通配符，例如 `**.vertx.io` 或完整的域名作为 `www.vertx.io`。

```java
router.route().virtualHost("*.vertx.io").handler(ctx -> {
  // do something if the request is for *.vertx.io
});
```

## 组合路由条件

您可以通过多种不同方式组合上述所有路由条件，例如：

```java
router.route(HttpMethod.PUT, "myapi/orders")
  .consumes("application/json")
  .produces("application/json")
  .handler(ctx -> {

    // This would be match for any PUT method to paths starting
    // with "myapi/orders" with a content-type of "application/json"
    // and an accept header matching "application/json"

  });
```

## 启用和禁用路由

您可以使用 `disable` 禁用路由。 匹配时将忽略禁用的路由。

您可以使用 `enable` 重新启用禁用的路由

## Forward 支持

您的应用程序可能位于代理服务器之后，例如`HAProxy`。 在此设置下工作时，访问客户端连接详细信息将无法正确返回预期结果。 例如，客户端主机 IP 地址将是代理服务器 IP 地址，而不是客户端的 IP 地址。

为了获得正确的连接信息，已经标准化了一个特殊的标头`Forward`以包含正确的信息。 然而这个标准并不是很老，所以很多代理一直在使用其他通常以前缀开头的标头：`X-Forward`。 Vert.x web 允许使用和解析这些标头，但默认情况下不允许。

默认情况下禁用这些标头的原因是为了防止恶意应用程序伪造它们的来源并隐藏它们的真正来源。

如前所述，默认情况下禁用Forward，要启用您应该使用：

```java
router.allowForward(AllowForwardHeaders.FORWARD);

// we can now allow forward header parsing
// and in this case only the "X-Forward" headers will be considered
router.allowForward(AllowForwardHeaders.X_FORWARD);

// we can now allow forward header parsing
// and in this case both the "Forward" header and "X-Forward" headers
// will be considered, yet the values from "Forward" take precedence
// this means if case of a conflict (2 headers for the same value)
// the "Forward" value will be taken and the "X-Forward" ignored.
router.allowForward(AllowForwardHeaders.ALL);
```

相同的规则适用于显式禁用`Forward`报头解析：

```java
router.allowForward(AllowForwardHeaders.NONE);
```

想了解更多关于header格式的内容，请参考：

- https://tools.ietf.org/html/rfc7239#section-4
- https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Forwarded

在幕后，这个特性所做的是改变你的连接(HTTP或WebSocket)的以下值:

- protocol(协议)
- host name(主机名)
- host port(端口号)

## Context 数据

您可以使用 `RoutingContext` 中的上下文数据来维护您希望在请求的生命周期内在处理程序之间共享的任何数据。

这是一个示例，其中一个处理程序在上下文数据中设置了一些数据，随后的处理程序检索它：

您可以使用 `put` 来放置任何对象，并使用 `get` 从上下文数据中检索任何对象。

发送到路径 `/some/path/other` 的请求将匹配两个路由。

```java
router.get("/some/path").handler(ctx -> {

  ctx.put("foo", "bar");
  ctx.next();

});

router.get("/some/path/other").handler(ctx -> {

  String bar = ctx.get("foo");
  // Do something with bar
  ctx.response().end();

});
```

或者，您可以使用 `data` 访问整个上下文数据映射。

## 元数据

虽然上下文允许您在请求-响应生命周期中存储数据，但有时让运行时元数据可用很重要。 例如，构建 API 文档，或为给定路由保存特定配置。

元数据功能与上下文数据类似。 您可以访问持有的 `Map` 或使用在 `Router` 和 `Route` 接口上定义的专用 getter 和 setter：

```java
router
  .route("/metadata/route")
  .putMetadata("metadata-key", "123")
  .handler(ctx -> {
    Route route = ctx.currentRoute();
    String value = route.getMetadata("metadata-key"); // 123
    // will end the request with the value 123
    ctx.end(value);
  });
```

## 辅助函数

虽然路由上下文将允许您访问底层请求和响应对象，但有时如果提供一些快捷方式来帮助完成常见任务，它会更有效率。 上下文中存在一些辅助函数以促进此任务。

提供“附件”，附件是一个响应，它将触发浏览器在配置为处理特定 mime 类型的操作系统应用程序上打开响应。 假设您正在生成 PDF：

```java
ctx
  .attachment("weekly-report.pdf")
  .end(pdfBuffer);
```

执行重定向到不同的页面或主机。 一个示例是重定向到应用程序的 HTTPS 变体：

```java
ctx.redirect("https://securesite.com/");

// 对目标“back”有一个特殊处理。
// 在这种情况下，重定向会将用户发送到referrer url或“/”(如果没有referrer)。

ctx.redirect("back");
```

向客户端发送 JSON 响应：

```java
ctx.json(new JsonObject().put("hello", "vert.x"));
// also applies to arrays
ctx.json(new JsonArray().add("vertx").add("web"));
// or any object that will be converted according
// to the json encoder available at runtime.
ctx.json(someObject);
```

简单的内容类型检查：

```java
ctx.is("html"); // => true
ctx.is("text/html"); // => true

// When Content-Type is application/json
ctx.is("application/json"); // => true
ctx.is("html"); // => false
```

验证关于缓存头和 last modified/etag 的当前值的请求是否“新鲜”。

```java
ctx.lastModified("Wed, 13 Jul 2011 18:30:00 GMT");
// this will now be used to verify the freshness of the request
if (ctx.isFresh()) {
  // client cache value is fresh perhaps we
  // can stop and return 304?
}
```

还有一些其他简单的不言自明的快捷方式：

```java
ctx.etag("W/123456789");

// set the last modified value
ctx.lastModified("Wed, 13 Jul 2011 18:30:00 GMT");

// quickly end
ctx.end();
ctx.end("body");
ctx.end(buffer);
```

## 重新路由

到目前为止，所有路由机制都允许您以顺序方式处理您的请求，但是有时您可能想要返回。 由于上下文没有公开任何关于前一个或下一个处理程序的信息，主要是因为这些信息是动态的，所以有一种方法可以从当前路由器的开始处重新启动整个路由。

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

所以从代码中你可以看到，如果一个请求到达`/some/path`，如果首先向上下文添加一个值，然后移动到下一个处理程序，将请求重新路由到`/some/path/B`，后者终止 请求。

您可以基于新路径或基于新路径和方法重新路由。 但是请注意，基于方法的重新路由可能会引入安全问题，因为例如通常安全的 GET 请求可能会变成 DELETE。

在故障处理程序上也允许重新路由，但是由于重新路由的性质，当调用当前状态代码和故障原因时会重置。 为了重新路由的处理程序应该在需要时生成正确的状态代码，例如：

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

应该清楚的是，重新路由适用于`paths`，因此如果您需要在重新路由之间保留和/或添加状态，则应该使用`RoutingContext`对象。 例如，您想使用额外参数重新路由到新路径：

```java
router.get("/final-target").handler(ctx -> {
  // continue from here...
});

// (Will reroute to /final-target including the query string)
router.get().handler(ctx -> ctx.reroute("/final-target?variable=value"));

// A safer way would be to add the variable to the context
router.get().handler(ctx -> ctx
  .put("variable", "value")
  .reroute("/final-target"));
```

Reroute 也会重新解析查询参数。 请注意，以前的查询参数将被丢弃。 该方法还将默默地丢弃并忽略路径中的任何 html 片段。 这是为了在常规请求和重新路由之间保持重新路由的语义一致。

如果需要将更多信息传递给新请求，它应该使用在 HTTP 事务的整个生命周期中保留的上下文。

## 子路由器

有时，如果您有很多处理程序，将它们分成多个路由器是有意义的。 如果您想在不同的应用程序中重用一组处理程序，这也很有用，根植于不同的路径根。

为此，您可以将路由器安装在另一个路由器的*安装点*。 安装的路由器称为*子路由器*。 子路由器可以挂载其他子路由器，因此您可以根据需要拥有多个级别的子路由器。

让我们看一个简单的例子，一个子路由器挂载另一个路由器。

此子路由器将维护与简单虚构 REST API 对应的一组处理程序。 我们将把它安装在另一个路由器上。 未显示 REST API 的完整实现。

这是子路由器：

```java
Router restAPI = Router.router(vertx);

restAPI.get("/products/:productID").handler(ctx -> {

  // TODO Handle the lookup of the product....
  ctx.response().write(productJSON);

});

restAPI.put("/products/:productID").handler(ctx -> {

  // TODO Add a new product...
  ctx.response().end();

});

restAPI.delete("/products/:productID").handler(ctx -> {

  // TODO delete the product...
  ctx.response().end();

});
```

如果此路由器用作顶级路由器，则对 `/products/product1234` 之类的 url 的 GET/PUT/DELETE 请求将调用 API。

但是，假设我们已经有另一个路由器描述的网站：

```java
Router mainRouter = Router.router(vertx);

// Handle static resources
mainRouter.route("/static/*").handler(myStaticHandler);

mainRouter.route(".*\\.templ").handler(myTemplateHandler);
```

我们现在可以将子路由器安装在主路由器上，针对一个安装点，在这种情况下是`/productsAPI`

```java
mainRouter.mountSubRouter("/productsAPI", restAPI);
```

这意味着现在可以通过以下路径访问 REST API：`/productsAPI/products/product1234`。

在使用子路由器之前，必须满足一些规则：

- 路由路径必须以通配符结尾
- 允许使用参数，但不允许使用完整的正则表达式模式
- 在此调用之前或之后只能注册 1 个处理程序（但它们可以在同一路径的新路由对象上）
- 每个路径对象只有 1 个路由器

验证发生在将路由器添加到 http 服务器时。 这意味着由于子路由器的动态特性，您在构建期间不会出现任何验证错误。 它们取决于要验证的上下文。

## 本地化

Vert.x Web 解析 `Accept-Language` 标头并提供一些帮助方法来识别客户端的首选语言环境或按质量排序的首选语言环境列表。

```java
Route route = router.get("/localized").handler(ctx -> {
  // although it might seem strange by running a loop with a switch we
  // make sure that the locale order of preference is preserved when
  // replying in the users language.
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
  // we do not know the user language so lets just inform that back:
  ctx.response().end("Sorry we don't speak: " + ctx.preferredLanguage());
});
```

主方法`acceptableLanguages`将返回用户理解的区域设置的排序列表，如果你只对用户首选的区域设置感兴趣，那么`preferredLanguage`将返回列表的第一个元素，如果用户没有提供区域设置，则返回`null`。

## 路由匹配失败

如果没有任何路由匹配任何特定请求，Vert.x-Web 将根据匹配失败发出错误信号：

- 404 如果没有路由匹配路径
- 405 如果路由匹配路径但不匹配 HTTP 方法
- 406 如果路由匹配路径和方法，但无法提供内容类型匹配 `Accept` 标头的响应
- 415 如果路由匹配路径和方法但它不能接受`Content-type`
- 400 如果路由匹配路径和方法但它不能接受空的主体

您可以使用`errorHandler`手动管理这些失败。

## 错误处理

除了设置处理程序来处理请求之外，您还可以设置处理程序来处理路由中的故障。

失败处理程序使用与普通处理程序完全相同的路由匹配条件。

例如，您可以提供一个故障处理程序，它只处理某些路径或某些 HTTP 方法上的故障。

这允许您为应用程序的不同部分设置不同的故障处理程序。

这是一个示例失败处理程序，仅在将 GET 请求路由到以 `/somepath/` 开头的路径时发生的失败调用：

```java
Route route = router.get("/somepath/*");

route.failureHandler(ctx -> {

  // This will be called for failures that occur
  // when routing requests to paths starting with
  // '/somepath/'

});
```

如果处理程序抛出异常，或者处理程序调用 `fail` 指定 HTTP 状态代码以故意发出失败信号，则会发生失败路由。

如果从处理程序中捕获到异常，这将导致失败，并发出状态代码“500”信号。

在处理故障时，故障处理程序会传递路由上下文，该上下文还允许检索故障或故障代码，以便故障处理程序可以使用它来生成故障响应。

```java
Route route1 = router.get("/somepath/path1/");

route1.handler(ctx -> {

  // Let's say this throws a RuntimeException
  throw new RuntimeException("something happened!");

});

Route route2 = router.get("/somepath/path2");

route2.handler(ctx -> {

  // This one deliberately fails the request passing in the status code
  // E.g. 403 - Forbidden
  ctx.fail(403);

});

// Define a failure handler
// This will get called for any failures in the above handlers
Route route3 = router.get("/somepath/*");

route3.failureHandler(failureRoutingContext -> {

  int statusCode = failureRoutingContext.statusCode();

  // Status code will be 500 for the RuntimeException
  // or 403 for the other failure
  HttpServerResponse response = failureRoutingContext.response();
  response.setStatusCode(statusCode).end("Sorry! Not today");

});
```

对于在运行错误处理程序时发生错误的可能性，与状态消息头中不允许的字符的使用有关，则原始状态消息将从错误代码更改为默认消息。 这是为了保持 HTTP 协议的语义正常工作而不是在没有正确完成协议的情况下突然崩溃和关闭套接字的权衡。

## 请求Body处理

`BodyHandler` 允许您检索请求正文、限制正文大小和处理文件上传。

对于需要此功能的任何请求，您应该确保body处理程序位于匹配的路由上。

此处理程序的使用要求它尽快安装在路由器中，因为它需要安装处理程序以使用 HTTP 请求正文，并且必须在执行任何异步调用之前完成。

```java
router.route().handler(BodyHandler.create());
```

如果之前需要异步调用，则 `HttpServerRequest` 应暂停然后恢复，以便在主体处理程序准备好处理它们之前不会传递请求事件。

```java
router.route().handler(ctx -> {

  HttpServerRequest request = ctx.request();

  // Pause the request
  request.pause();

  someAsyncCall(result -> {

    // Resume the request
    request.resume();

    // And continue processing
    ctx.next();
  });
});

// This body handler will be called for all routes
router.route().handler(BodyHandler.create());
```

> **🏷注意:** 上传可能是 DDoS 攻击的来源，为了减少攻击面，建议对 `setBodyLimit` 设置合理的限制（例如：一般上传为 10mb 或 JSON 为 100kb）。

### 获取请求body

如果您知道请求正文是 JSON，那么您可以使用 `getBodyAsJson`，如果您知道它是一个字符串，您可以使用 `getBodyAsString`，或者将其作为缓冲区检索使用 `getBody`。

### 限制body大小

要限制请求正文的大小，请创建正文处理程序，然后使用 `setBodyLimit` 指定最大正文大小，以字节为单位。 这对于避免用非常大的实体耗尽内存很有用。

如果尝试发送大于最大大小的正文，则会发送 HTTP 状态代码 413 - `Request Entity Too Large`。

默认情况下没有正文限制。

### 合并From属性

默认情况下，body 处理程序会将任何表单属性合并到请求参数中。 如果你不想要这种行为，你可以使用 `setMergeFormAttributes` 禁用它。

### 处理文件上传

正文处理程序也用于处理多部分文件上传。

如果Body处理程序在请求的匹配路由上，则任何文件上传都将自动流式传输到上传目录，默认情况下是 `file-uploads`。

每个文件都将被赋予一个自动生成的文件名，并且文件上传将在路由上下文中通过 `fileUploads` 提供。

这是一个例子：

```java
router.route().handler(BodyHandler.create());

router.post("/some/path/uploads").handler(ctx -> {

  Set<FileUpload> uploads = ctx.fileUploads();
  // Do something with uploads....

});
```

每个文件上传都由一个 `FileUpload` 实例描述，它允许访问各种属性，例如名称、文件名和大小。

## 处理 cookie

Vert.x-Web 具有开箱即用的 cookie 支持。

### 操作 cookie

您可以使用 `getCookie` 按名称检索 cookie，或使用 `cookieMap` 检索整个集合。

要删除 cookie，请使用`removeCookie`。

要添加 cookie，请使用 `addCookie`。

写入响应标头时，cookie 集将自动写回响应中，以便浏览器可以存储它们。

Cookie 由`Cookie`实例描述。 这允许您检索名称、值、域、路径和其他正常的 cookie 属性。

以下是查询和添加 cookie 的示例：

```java
Cookie someCookie = ctx.request().getCookie("mycookie");
String cookieValue = someCookie.getValue();

// Do something with cookie...

// Add a cookie - this will get written back in the response automatically
ctx.response().addCookie(Cookie.cookie("othercookie", "somevalue"));
```

## 处理 sessions

Vert.x-Web 为会话提供开箱即用的支持。

会话在HTTP请求之间持续的时间相当于一个浏览器会话的长度，并为您提供了一个可以添加会话范围信息的地方，比如一个购物篮。

Vert.x-Web 使用会话 cookie 来识别会话。 会话 cookie 是临时的，当它关闭时会被您的浏览器删除。

我们不会将会话的实际数据放入会话 cookie - cookie 只是使用标识符来查找服务器上的实际会话。 标识符是使用安全随机生成的随机 UUID，因此它应该是有效不可猜测的。

Cookie 在 HTTP 请求和响应中通过网络传递，因此确保在使用会话时使用 HTTPS 始终是明智之举。 如果您尝试通过直接 HTTP 使用会话，Vert.x 会警告您。

要在您的应用程序中启用会话，您必须在应用程序逻辑之前的匹配路由上有一个 `SessionHandler`。

会话处理程序处理会话 cookie 的创建和会话的查找，因此您不必自己做这些。

将响应标头发送到客户端后，会话数据会自动保存到会话存储中。 但请注意，使用这种机制，不能保证数据在客户端收到响应之前完全持久化。 但有时需要这种保证。 在这种情况下，您可以强制刷新。 这将禁用自动保存过程，除非刷新操作失败。 这允许在完成响应之前控制状态，例如：

```java
ChainAuthHandler chain =
  ChainAuthHandler.any()
    .add(authNHandlerA)
    .add(ChainAuthHandler.all()
      .add(authNHandlerB)
      .add(authNHandlerC));

// secure your route
router.route("/secure/resource").handler(chain);
// your app
router.route("/secure/resource").handler(ctx -> {
  // do something...
});
```

Vert.x 会话处理程序状态默认使用 cookie 来存储会话 ID。 会话 ID 是一个唯一的字符串，用于在访问之间识别单个访问者。 但是，如果客户端的 Web 浏览器不支持 cookie 或访问者在 Web 浏览器的设置中禁用了 cookie，我们就不能在客户端的机器上存储会话 ID。 在这种情况下，将为每个请求创建新会话。 这种行为是无用的，因为我们无法记住两个请求之间某个访问者的信息。 我们可以说，默认情况下，如果浏览器不支持 cookie，会话将无法工作。

Vert.x Web 支持没有 cookie 的会话，称为“无 cookie”会话。 作为替代方案，Vert.x Web 可以在页面 URL 中嵌入会话 ID。 这样，所有页面链接都将包含会话 id 字符串。 当访问者点击其中一些链接时，它会从页面 URL 中读取会话 id，因此我们不需要 cookie 支持来获得功能会话。

要启用无 cookie 会话：

```java
router.route()
  .handler(SessionHandler.create(store).setCookieless(true));
```

重要的是要知道，在这种模式下，会话 ID 应该由应用程序传递给最终用户，通常是通过在 HTML 页面或脚本上呈现它。 有一些重要的规则。 会话 ID 由路径 `/optional/path/prefix/'('sessionId')'/path/suffix` 上的以下模式标识。

例如，给定路径：`http://localhost:2677/WebSite1/(S(3abhbgwjg33aqrt3uat2kh4d))/api/`，会话 ID 将为：`3abhbgwjg33aqrt3uat2kh4d`。

使用会话时的主要安全问题是恶意用户可能会发现其他人的会话 ID。 如果两个用户共享相同的会话 id，他们也共享相同的会话变量，并且网站将他们视为一个访问者。 如果会话用于任何私人或敏感数据，或者允许访问网站的受限区域，这可能会带来安全风险。 使用 cookie 时，可以使用 SSL 并通过将 cookie 标记为安全来保护会话 ID。 但是，在无 cookie 会话的情况下，会话 ID 是 URL 的一部分，并且更容易受到攻击。

### Session 存储

要创建会话处理程序，您需要有一个会话存储实例。 会话存储是保存应用程序实际会话的对象。

会话存储负责保存一个安全的伪随机数生成器，以保证安全的会话 ID。 此 PRNG 独立于存储，这意味着给定存储 A 的会话 id，由于它们具有不同的种子和状态，因此无法导出存储 B 的会话 id。

默认情况下，此 PRNG 使用混合模式，阻塞用于播种，非阻塞用于生成。 PRNG 还将每 5 分钟重新播种 64 位新熵。 但是，这都可以使用系统属性进行配置：

- io.vertx.ext.auth.prng.algorithm 例如：SHA1PRNG
- io.vertx.ext.auth.prng.seed.interval 例如：1000（每秒）
- io.vertx.ext.auth.prng.seed.bits 例如：128

大多数用户不需要配置这些值，除非您注意到应用程序的性能受到 PRNG 算法的影响。

Vert.x-Web 带有两个开箱即用的会话存储实现，如果您愿意，也可以编写自己的。

实现应遵循`ServiceLoader`约定，并且在运行时从类路径中可用的所有存储都将被公开。 当多个实现可用时，第一个可以实例化和配置成功的实现成为默认值。 如果没有可用，则默认值取决于创建 Vert.x 的模式。 如果集群模式可用，则集群会话存储是默认设置，否则本地存储是默认设置。

#### 本地 session 存储

使用此存储，会话本地存储在内存中，并且仅在此实例中可用。

如果您只有一个 Vert.x 实例，并且在应用程序中使用粘性会话，并且已将负载均衡器配置为始终将 HTTP 请求路由到同一个 Vert.x 实例，则此存储是合适的。

如果您不能确保您的请求都将在同一台服务器上终止，那么请不要使用此存储，因为您的请求可能会在不知道您的会话的服务器上结束。

本地会话存储是通过使用共享的本地map实现的，并且有一个清除过期会话的收割机。

可以使用带有键的 json 消息配置收割机间隔：`reaperInterval`。

以下是创建本地 `SessionStore` 的一些示例

```java
SessionStore store1 = LocalSessionStore.create(vertx);

// Create a local session store specifying the local shared map name to use
// This might be useful if you have more than one application in the same
// Vert.x instance and want to use different maps for different applications
SessionStore store2 = LocalSessionStore.create(
  vertx,
  "myapp3.sessionmap");

// Create a local session store specifying the local shared map name to use and
// setting the reaper interval for expired sessions to 10 seconds
SessionStore store3 = LocalSessionStore.create(
  vertx,
  "myapp3.sessionmap",
  10000);
```

#### 集群 session 存储

使用此存储，会话存储在分布式map中，可跨 Vert.x 集群访问。

如果您*不*使用粘性会话，则此存储是合适的，即您的负载均衡器正在将来自同一浏览器的不同请求分发到不同的服务器。

您的会话可从集群中使用此存储的任何节点访问。

要使用集群会话存储，您应该确保您的 Vert.x 实例是集群的。

以下是一些创建集群 `SessionStore` 的示例

```java
Vertx.clusteredVertx(new VertxOptions(), res -> {

  Vertx vertx = res.result();

  // Create a clustered session store using defaults
  SessionStore store1 = ClusteredSessionStore.create(vertx);

  // Create a clustered session store specifying the distributed map name to use
  // This might be useful if you have more than one application in the cluster
  // and want to use different maps for different applications
  SessionStore store2 = ClusteredSessionStore.create(
    vertx,
    "myclusteredapp3.sessionmap");
});
```

#### 其他 存储

还可以使用其他存储，可以通过将正确的jar导入项目来使用这些存储。这类存储的一个例子是cookie存储。这种存储的优点是它不需要后台或服务器端状态，这可能是有用的，它在一些情况下**但**所有的会话数据将被发送回客户端Cookie，所以如果你需要存储私人信息，这不应该被使用。

如果您使用的是粘性会话，则此存储是合适的，即您的负载均衡器将来自同一浏览器的不同请求分发到不同的服务器。

由于会话存储在 Cookie 中，这意味着会话也能在服务器崩溃后幸免于难。

第二个已知的实现是 Redis 会话存储。 这个存储就像普通的集群存储一样工作，但是就像它的名字所暗示的那样，它使用一个 redis 后端来保持会话数据的集中。

此外，还有 Infinispan 会话存储（详情如下）。

这些存储的坐标如下:

- groupId: `io.vertx`
- artifactId: `vertx-web-sstore-{cookie|redis|infinispan}`

##### Infinispan Web Session 存储

SessionStore的实现依赖于[Infinispan](https://infinispan.org/) Java客户端。

> **☢警告:** 此模块具有“技术预览”状态，这意味着API可以在不同版本之间更改。

###### 入门指南

要使用此模块，请将以下内容添加到 Maven POM 文件的 *dependencies* 部分：

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-web-sstore-infinispan</artifactId>
 <version>4.2.5</version>
</dependency>
```

或者，如果您使用 Gradle：

```groovy
compile 'io.vertx:vertx-web-sstore-infinispan:4.2.5'
```

###### 使用

如果此会话存储是您的依赖项中唯一的一个，则可以以通用方式对其进行初始化：

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

否则，显式使用 `InfinispanSessionStore` 类型：

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

配置条目

根条目是：

- `servers`：强制，服务器定义的 JSON 数组（见下文）
- `cacheName`：可选，用于存储会话数据的缓存名称（默认为 `vertx-web.sessions`）
- `retryTimeout`：可选，会话处理程序从存储中检索值时使用的重试超时值（以毫秒为单位）（默认为 `5000`）

服务器定义的条目是：

- `uri` : 可选，一个 [Hot Rod URI](https://infinispan.org/blog/2020/05/26/hotrod-uri/)
- `host`：可选（默认为 `localhost`）
- `port`：可选（默认为 `11222`）
- `clientIntelligence`：可选（`BASIC`、`TOPOLOGY_AWARE`、`HASH_DISTRIBUTION_AWARE`之一）
- `用户名`：必填
- `密码`：必填
- `realm`：可选（默认为 `default`）
- `saslMechanism`：可选（默认为`DIGEST-MD5`）
- `saslQop`：可选（`AUTH`、`AUTH_INT`、`AUTH_CONF`之一）

> **⚠重要:** 如果设置了`uri`条目，则忽略其他条目。

自定义 Infinispan 客户端

对于高级配置要求，您可以提供自定义的 [`RemoteCacheManager`](https://docs.jboss.org/infinispan/12.1/apidocs/org/infinispan/client/hotrod/RemoteCacheManager.html)：

```java
InfinispanSessionStore sessionStore = InfinispanSessionStore.create(vertx, config, remoteCacheManager);
```

### 创建会话处理程序

创建会话存储后，您可以创建会话处理程序，并将其添加到路由中。 您应该确保您的会话处理程序在您的应用程序处理程序之前被路由到。

这是一个例子：

```java
Router router = Router.router(vertx);

// Create a clustered session store using defaults
SessionStore store = ClusteredSessionStore.create(vertx);

SessionHandler sessionHandler = SessionHandler.create(store);

// the session handler controls the cookie used for the session
// this includes configuring, for example, the same site policy
// like this, for strict same site policy.
sessionHandler.setCookieSameSite(CookieSameSite.STRICT);

// Make sure all requests are routed through the session handler too
router.route().handler(sessionHandler);

// Now your application handlers
router.route("/somepath/blah/").handler(ctx -> {

  Session session = ctx.session();
  session.put("foo", "bar");
  // etc

});
```

会话处理程序将确保在会话存储中自动查找您的会话（或在不存在会话时创建），并在路由上下文到达您的应用程序处理程序之前对其进行设置。

### 使用 session

在您的处理程序中，您可以使用 `session` 访问会话实例。

你使用 `put` 将数据放入会话中，使用 `get` 从会话中获取数据，并使用 `remove` 从会话中删除数据。

会话中项目的键始终是字符串。 对于本地会话存储，值可以是任何类型，对于集群会话存储，它们可以是任何基本类型，或 `Buffer`、`JsonObject`、`JsonArray` 或可序列化对象，因为这些值必须跨集群。

这是一个操作会话数据的示例：

```java
router.route().handler(sessionHandler);

// Now your application handlers
router.route("/somepath/blah").handler(ctx -> {

  Session session = ctx.session();

  // Put some data from the session
  session.put("foo", "bar");

  // Retrieve some data from a session
  int age = session.get("age");

  // Remove some data from a session
  JsonObject obj = session.remove("myobj");

});
```

响应完成后，会话会自动写回存储。

您可以使用 `destroy` 手动销毁会话。 这将从上下文和会话存储中删除会话。 请注意，如果没有会话，将自动为来自浏览器的下一个请求创建一个新会话，该请求通过会话处理程序路由。

### Session 超时

如果会话超过超时时间未访问，会话将自动超时。 当会话超时时，它会从存储中删除。

当请求到达并查找会话以及响应完成并将会话存储回存储区时，会话会自动标记为已访问。

您还可以使用 `setAccessed` 手动将会话标记为已访问。

创建会话处理程序时可以配置会话超时。 默认超时为 30 分钟。

## 认证/授权

Vert.x 带有一些开箱即用的处理程序，用于处理身份验证和授权。 在 vert.x web 中，这两个词的含义是：

- **Authentication(身份验证)** - 告诉用户是谁
- **Authorization(授权)** - 告诉用户可以做什么

而**Authentication(身份验证)**是严格的，以一个众所周知的协议，例如:

- HTTP 基本身份验证
- HTTP 摘要认证
- OAuth2 身份验证
- …

vert.x 中的 **Authorization(授权)** 非常通用，无论之前的情况如何，都可以使用。 然而，在这两种情况下使用相同的提供程序模块也是可能的并且是一个有效的用例。

### 创建身份验证处理程序

要创建身份验证处理程序，您需要一个 `AuthenticationProvider` 的实例。 身份验证提供程序用于对用户进行身份验证。 Vert.x 在 vertx-auth 项目中提供了几个开箱即用的身份验证提供程序实例。 有关身份验证提供程序以及如何使用和配置它们的完整信息，请参阅身份验证文档。

这是一个给定身份验证提供程序创建基本身份验证处理程序的简单示例。

```java
router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

AuthenticationHandler basicAuthHandler = BasicAuthHandler.create(authProvider);
```

### 在您的应用程序中处理身份验证

假设您希望对以 `/private/` 开头的路径的所有请求都经过身份验证。 为此，请确保您的身份验证处理程序在这些路径上的应用程序处理程序之前：

```java
router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

AuthenticationHandler basicAuthHandler = BasicAuthHandler.create(authProvider);

// All requests to paths starting with '/private/' will be protected
router.route("/private/*").handler(basicAuthHandler);

router.route("/someotherpath").handler(ctx -> {

  // This will be public access - no login required

});

router.route("/private/somepath").handler(ctx -> {

  // This will require a login

  // This will have the value true
  boolean isAuthenticated = ctx.user() != null;

});
```

如果身份验证处理程序成功地对用户进行了身份验证，它将向 `RoutingContext` 中注入一个 `User` 对象，因此它可以在您的处理程序中使用：`user`。

如果您希望将 User 对象存储在会话中，以便在请求之间可用，这样您就不必对每个请求进行身份验证，那么您应该确保在身份验证处理程序之前有一个会话处理程序。

拥有用户对象后，您还可以以编程方式使用其上的方法来授权用户。

如果你想让用户退出，你可以在路由上下文中调用`clearUser`。

### HTTP Basic 身份验证

[HTTP 基本身份验证](http://en.wikipedia.org/wiki/Basic_access_authentication) 是一种适用于简单应用程序的简单身份验证方法。

使用基本身份验证，凭据在 HTTP 标头中通过网络未加密发送，因此您必须使用 HTTPS 而不是 HTTP 为您的应用程序提供服务。

使用基本身份验证，如果用户请求需要身份验证的资源，则基本身份验证处理程序将发送回带有`WWW-Authenticate`标头的`401`响应。 这会提示浏览器显示登录对话框并提示用户输入他们的用户名和密码。

再次向资源发出请求，这次设置了`Authorization`标头，其中包含以 Base64 编码的用户名和密码。

当基本身份验证处理程序收到此信息时，它会使用用户名和密码调用配置的 `AuthenticationProvider` 来验证用户。 如果身份验证成功，则允许请求的路由继续到应用程序处理程序，否则返回`403`响应以表示访问被拒绝。

### 重定向身份验证处理程序

通过重定向身份验证处理，如果用户尝试访问受保护的资源但未登录，则会将用户重定向到登录页面。

然后用户填写登录表单并提交。 这由对用户进行身份验证的服务器处理，如果通过身份验证，则将用户重定向回原始资源。

要使用重定向身份验证，您需要配置 `RedirectAuthHandler` 的实例，而不是基本身份验证处理程序。

您还需要设置处理程序来为您的实际登录页面提供服务，并设置处理程序来处理实际登录本身。 为了处理登录，我们为此提供了一个预构建的处理程序`FormLoginHandler`。

这是一个简单应用程序的示例，在默认重定向 url `/loginpage` 上使用重定向身份验证处理程序。

```java
router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

// All requests to paths starting with '/private/' will be protected
router
  .route("/private/*")
  .handler(RedirectAuthHandler.create(authProvider));

// Handle the actual login
// One of your pages must POST form login data
router.post("/login").handler(FormLoginHandler.create(authProvider));

// Set a static server to serve static resources, e.g. the login page
router.route().handler(StaticHandler.create());

router
  .route("/someotherpath")
  .handler(ctx -> {
    // This will be public access - no login required
  });

router
  .route("/private/somepath")
  .handler(ctx -> {

    // This will require a login

    // This will have the value true
    boolean isAuthenticated = ctx.user() != null;

  });
```

### JWT 认证

使用 JWT 身份验证可以通过权限保护资源，并且拒绝没有足够权限的用户访问。 您需要添加 `io.vertx:vertx-auth-jwt:4.2.5` 依赖项才能使用 `JWTAuthProvider`

要使用此处理程序，涉及 2 个步骤：

- 设置一个处理程序来发行令牌（或依赖第 3 方）
- 设置处理程序以过滤请求

请注意，这 2 个处理程序应仅在 HTTPS 上可用，否则将允许在传输中嗅探令牌，从而导致会话劫持攻击。

以下是有关如何发行代币的示例：

```java
Router router = Router.router(vertx);

JWTAuthOptions authConfig = new JWTAuthOptions()
  .setKeyStore(new KeyStoreOptions()
    .setType("jceks")
    .setPath("keystore.jceks")
    .setPassword("secret"));

JWTAuth jwt = JWTAuth.create(vertx, authConfig);

router.route("/login").handler(ctx -> {
  // this is an example, authentication should be done with another provider...
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

现在您的客户端有一个令牌，它所需要的是**对于所有**后续请求，HTTP 标头`Authorization` 填充：`Bearer <token>` 例如：

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
  // some handle code...
});
```

JWT 允许您将任何您喜欢的信息添加到令牌本身。 通过这样做，服务器中没有状态允许您在不需要集群会话数据的情况下扩展应用程序。 为了向令牌添加数据，在创建令牌期间只需将数据添加到 JsonObject 参数：

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

消费时也一样：

```java
Handler<RoutingContext> handler = ctx -> {
  String theSubject = ctx.user().principal().getString("sub");
  String someKey = ctx.user().principal().getString("someKey");
};
```

### 配置授权

到目前为止，所有示例都涵盖了身份验证。 与用户打交道时，授权是下一个合乎逻辑的步骤。 虽然身份验证确实特定于协议，但**授权**是独立的，所有信息都是从 `User` 对象中提取的。

在此之前，需要将授权加载到同一对象。 为了做到这一点，应该使用`AuthorizationHandler`。 授权处理程序将从给定的`AuthorizationProvider`加载所有已知的授权。

```java
router.route().handler(
  // create the handler that will perform the attestation
  AuthorizationHandler.create(
    // what to attest
    PermissionBasedAuthorization.create("can-do-work"))
    // where to lookup the authorizations for the user
    .addAuthorizationProvider(authProvider));
```

查找可以在多个源上执行，只要继续添加`addAuthorizationProvider(provider)`到处理程序。

这是一个配置应用程序的示例，以便应用程序的不同部分需要不同的权限。 请注意，权限的含义由您使用的基础身份验证提供程序确定。 例如。 有些可能支持基于角色/权限的模型，但有些可能使用另一种模型。

```java
router.route("/listproducts/*").handler(
  // create the handler that will perform the attestation
  AuthorizationHandler.create(
    // what to attest
    PermissionBasedAuthorization.create("list_products"))
    // where to lookup the authorizations for the user
    .addAuthorizationProvider(authProvider));

// Only "admin" has access to /private/settings
router.route("/private/settings/*").handler(
  // create the handler that will perform the attestation
  AuthorizationHandler.create(
    // what to attest
    RoleBasedAuthorization.create("admin"))
    .addAuthorizationProvider(authProvider));
```

### 链接多个身份验证处理程序

有时您希望在单个应用程序中支持多种身份验证机制。 为此，您可以使用`ChainAuthHandler`。 链身份验证处理程序将尝试在处理程序链上执行身份验证。

重要的是要知道某些处理程序需要特定的提供程序，例如：

- `JWTAuthHandler` 需要`JWTAuth`。
- `DigestAuthHandler` 需要`HtdigestAuth`。
- `OAuth2AuthHandler` 需要`OAuth2Auth`。
- `WebAuthnHandler` 需要`WebAuthn`。

因此，预计不会在所有处理程序之间共享提供程序。 在某些情况下，可以跨处理程序共享提供程序，例如：

- `BasicAuthHandler` 可以采用任何提供者。
- `RedirectAuthHandler` 可以采用任何提供者。

假设您要创建一个同时接受`HTTP Basic Authentication`和`Form Redirect`的应用程序。 您将开始将您的链配置为：

```java
ChainAuthHandler chain = ChainAuthHandler.any();

// add http basic auth handler to the chain
chain.add(BasicAuthHandler.create(provider));
// add form redirect auth handler to the chain
chain.add(RedirectAuthHandler.create(provider));

// secure your route
router.route("/secure/resource").handler(chain);
// your app
router.route("/secure/resource").handler(ctx -> {
  // do something...
});
```

因此，当用户发出没有 `Authorization` 标头的请求时，这意味着链将无法通过基本身份验证处理程序进行身份验证，并将尝试使用重定向处理程序进行身份验证。 由于重定向处理程序总是重定向，您将被发送到您在该处理程序中配置的登录表单。

就像在vertexweb中正常的路由一样，身份验证的改变是一个序列，所以如果你想使用HTTP基本身份验证而不是重定向回退到你的浏览器请求用户凭据，你所需要做的就是反转附加到链的顺序。

现在假设您发出请求，其中提供标头`Authorization`和值`Basic [token]`。 在这种情况下，基本身份验证处理程序将尝试进行身份验证，如果成功，则链将停止并且 vertx-web 将继续处理您的处理程序。 如果令牌无效，例如用户名/密码错误，则链将继续到以下条目。 在这种特定情况下，重定向身份验证处理程序。

复杂的链接也是可能的，例如，构建逻辑序列，例如：`HandlerA` OR (`HandlerB` AND `HandlerC`)。

```java
ChainAuthHandler chain =
  ChainAuthHandler.any()
    .add(authNHandlerA)
    .add(ChainAuthHandler.all()
      .add(authNHandlerB)
      .add(authNHandlerC));

// secure your route
router.route("/secure/resource").handler(chain);
// your app
router.route("/secure/resource").handler(ctx -> {
  // do something...
});
```

## 提供静态资源

Vert.x-Web 带有一个开箱即用的处理程序，用于提供静态 Web 资源，因此您可以非常轻松地编写静态 Web 服务器。

要提供静态资源，例如 `.html`、`.css`、`.js` 或任何其他静态资源，您可以使用 `StaticHandler` 的实例。

对静态处理程序处理的路径的任何请求都将导致文件从文件系统上的目录或类路径提供服务。 默认的静态文件目录是 `webroot` 但可以配置。

在下面的示例中，所有对以 `/static/` 开头的路径的请求都将从目录 `webroot` 获得服务：

```java
router.route("/static/*").handler(StaticHandler.create());
```

例如，如果有一个路径为 `/static/css/mystyles.css` 的请求，静态服务将在目录 `webroot/css/mystyle.css` 中查找文件。

它还将在类路径中查找名为 `webroot/css/mystyle.css` 的文件。 这意味着您可以将所有静态资源打包成一个 jar 文件（或 fatjar）并像这样分发它们。

当 Vert.x 第一次在类路径中找到资源时，它会提取它并将其缓存在磁盘上的临时目录中，因此它不必每次都这样做。

处理程序将处理范围感知请求。 当客户端向静态资源发出请求时，处理程序将通过在 `Accept-Ranges` 标头上声明单元来通知它可以处理范围感知请求。 进一步的请求包含具有正确单位和开始和结束索引的`Range`标头，然后将收到具有正确`Content-Range`标头的部分响应。

### 配置缓存

默认情况下，静态处理程序将设置缓存标头以使浏览器能够有效地缓存文件。

Vert.x-Web 设置标题 `cache-control`、`last-modified` 和 `date`。

`cache-control` 默认设置为 `max-age=86400`。 这相当于一天。 如果需要，可以使用 `setMaxAgeSeconds` 进行配置。

如果浏览器发送带有 `if-modified-since` 标头的 GET 或 HEAD 请求，并且该资源自该日期以来没有被修改，则返回 `304` 状态，告知浏览器使用其本地缓存的资源。

如果不需要处理缓存头，可以使用 `setCachingEnabled` 禁用它。

启用缓存处理时，Vert.x-Web 会将资源的最后修改日期缓存在内存中，这样可以避免每次检查实际最后修改日期时磁盘命中。

缓存中的条目有一个到期时间，在此之后，将再次检查磁盘上的文件并更新缓存条目。

如果您知道您的文件在磁盘上永远不会更改，那么缓存条目将有效地永不过期。 这是默认设置。

如果你知道当服务器运行时你的文件可能会在磁盘上更改，那么你可以使用`setFilesReadOnly`将只读文件设置为false。

要启用可以在任何时候缓存在内存中的最大条目数，您可以使用`setMaxCacheSize`。

要配置缓存条目的到期时间，您可以使用`setCacheEntryTimeout`。

### 配置index页面

对根路径 `/` 的任何请求都将导致索引页面被提供。 默认情况下，索引页面是`index.html`。 这可以使用 `setIndexPage` 进行配置。

### 更改web根目录

默认情况下，静态资源将从目录 `webroot` 提供。 要配置这个使用`setWebRoot`。

### 提供隐藏文件

默认情况下，服务将提供隐藏文件（以 `.` 开头的文件）。

如果您不想提供隐藏文件，您可以使用 `setIncludeHidden` 对其进行配置。

### 目录列表

服务器还可以执行目录列表。 默认情况下，目录列表被禁用。 要启用它，请使用`setDirectoryListing`。

启用目录列表时，返回的内容取决于 `accept` 标头中的内容类型。

对于 `text/html` 目录列表，用于渲染目录列表页面的模板可以通过 `setDirectoryTemplate` 进行配置。

### 禁用磁盘上的文件缓存

默认情况下，Vert.x 将从类路径提供的文件缓存到磁盘上当前工作目录中名为 `.vertx` 的目录的子目录中的文件中。 这主要在将服务部署为生产中的 fatjar 时非常有用，因为每次从类路径提供文件可能会很慢。

在开发中，这可能会导致问题，就像您在服务器运行时更新静态内容一样，将提供缓存的文件而不是更新的文件。

要禁用文件缓存，你可以提供你的vert.x选项，属性`fileResolverCachingEnabled`为`false`。为了向后兼容，它也会将该值默认为系统属性`vertx.disableFileCaching`。例如，你可以在IDE中设置一个运行配置，在运行你的主类时设置。

## CORS 处理

[跨域资源共享](http://en.wikipedia.org/wiki/Cross-origin_resource_sharing) 是一种允许从一个域请求资源并从另一个域提供资源的安全机制。

Vert.x-Web 包含一个处理程序`CorsHandler`，它为您处理 CORS 协议。

Here’s an example:

```java
router.route()
  .handler(
    CorsHandler.create("vertx\\.io")
      .allowedMethod(HttpMethod.GET));

router.route().handler(ctx -> {

  // Your app handlers

});
```

## 多租户

在某些情况下，您的应用程序需要处理的不仅仅是 1 个租户。 在这种情况下，提供了一个帮助处理程序来简化应用程序的设置。

如果租户由 HTTP 标头标识，例如`X-Tenant`，则创建处理程序非常简单：

```java
router.route().handler(MultiTenantHandler.create("X-Tenant"));
```

您现在应该注册应该为给定租户执行的处理程序：

```java
MultiTenantHandler.create("X-Tenant")
  .addTenantHandler("tenant-A", ctx -> {
    // do something for tenant A...
  })
  .addTenantHandler("tenant-B", ctx -> {
    // do something for tenant B...
  })
  // optionally
  .addDefaultHandler(ctx -> {
    // do something when no tenant matches...
  });
```

这对于安全情况很有用：

```java
OAuth2Auth gitHubAuthProvider = GithubAuth
  .create(vertx, "CLIENT_ID", "CLIENT_SECRET");

// create a oauth2 handler on our running server
// the second argument is the full url to the callback
// as you entered in your provider management console.
OAuth2AuthHandler githubOAuth2 = OAuth2AuthHandler.create(
  vertx,
  gitHubAuthProvider,
  "https://myserver.com/github-callback");

// setup the callback handler for receiving the GitHub callback
githubOAuth2.setupCallback(router.route("/github-callback"));

// create an OAuth2 provider, clientID and clientSecret
// should be requested to Google
OAuth2Auth googleAuthProvider = OAuth2Auth.create(vertx, new OAuth2Options()
  .setClientId("CLIENT_ID")
  .setClientSecret("CLIENT_SECRET")
  .setFlow(OAuth2FlowType.AUTH_CODE)
  .setSite("https://accounts.google.com")
  .setTokenPath("https://www.googleapis.com/oauth2/v3/token")
  .setAuthorizationPath("/o/oauth2/auth"));

// create a oauth2 handler on our domain: "http://localhost:8080"
OAuth2AuthHandler googleOAuth2 = OAuth2AuthHandler.create(
  vertx,
  googleAuthProvider,
  "https://myserver.com/google-callback");

// setup the callback handler for receiving the Google callback
googleOAuth2.setupCallback(router.route("/google-callback"));

// At this point the 2 callbacks endpoints are registered:

// /github-callback -> handle github Oauth2 callbacks
// /google-callback -> handle google Oauth2 callbacks

// As the callbacks are made by the IdPs there's no header
// to identify the source, hence the need of custom URLs

// However for out Application we can control it so later
// we can add the right handler for the right tenant

router.route().handler(
  MultiTenantHandler.create("X-Tenant")
    // tenants using github should go this way:
    .addTenantHandler("github", githubOAuth2)
    // tenants using google should go this way:
    .addTenantHandler("google", googleOAuth2)
    // all other should be forbidden
    .addDefaultHandler(ctx -> ctx.fail(401)));
```

租户 id 可以随时从上下文中读取，例如决定要加载哪个资源，或者要连接到哪个数据库：

```java
router.route().handler(ctx -> {
  // the default key is "tenant" as defined in
  // MultiTenantHandler.TENANT but this value can be
  // modified at creation time in the factory method
  String tenant = ctx.get(MultiTenantHandler.TENANT);

  switch(tenant) {
    case "google":
      // do something for google users
      break;
    case "github":
      // so something for github users
      break;
  }
});
```

多租户是一个强大的处理程序，它允许应用程序并行运行，但是它不提供执行的沙盒。它不应该被用作隔离，因为错误编写的应用程序可能会在租户之间泄漏状态。

## 模板

Vert.x-Web 通过包括对几种流行模板引擎的开箱即用支持来包括动态页面生成功能。 您也可以轻松添加自己的。

模板引擎由 `TemplateEngine` 描述。 为了渲染模板，使用了`render`。

使用模板最简单的方法不是直接调用模板引擎，而是使用`TemplateHandler`。 此处理程序根据 HTTP 请求中的路径为您调用模板引擎。

默认情况下，模板处理程序将在名为 `templates` 的目录中查找模板。 这可以配置。

默认情况下，处理程序将返回内容类型为 `text/html` 的渲染结果。 这也可以配置。

当您创建模板处理程序时，您会传入所需的模板引擎实例。 模板引擎没有嵌入在 vertx-web 中，所以你需要配置你的项目来访问它们。 为每个模板引擎提供了配置。

这里有些例子：

```java
TemplateEngine engine = HandlebarsTemplateEngine.create();
TemplateHandler handler = TemplateHandler.create(engine);

// This will route all GET requests starting with /dynamic/ to the template handler
// E.g. /dynamic/graph.hbs will look for a template in /templates/graph.hbs
router.get("/dynamic/*").handler(handler);

// Route all GET requests for resource ending in .hbs to the template handler
router.getWithRegex(".+\\.hbs").handler(handler);
```

### MVEL 模板引擎

要使用 MVEL，您需要将以下 *dependency* 添加到您的项目中：`io.vertx:vertx-web-templ-mvel:4.2.5`。 使用以下命令创建 MVEL 模板引擎的实例：`io.vertx.ext.web.templ.mvel.MVELTemplateEngine#create(io.vertx.core.Vertx)`

使用 MVEL 模板引擎时，如果文件名中没有指定扩展名，它将默认查找具有 `.templ` 扩展名的模板。

路由上下文 `RoutingContext` 在 MVEL 模板中作为 `context` 变量可用，这意味着您可以根据上下文中的任何内容呈现模板，包括请求、响应、会话或上下文数据。

这里有些例子：

```
The request path is @{context.request().path()}

The variable 'foo' from the session is @{context.session().get('foo')}

The value 'bar' from the context data is @{context.get('bar')}
```

有关如何编写 MVEL 模板，请参阅 [MVEL 模板文档](http://mvel.documentnode.com/#mvel-2.0-templating-guide)。

### Jade 模板引擎

要使用 Jade 模板引擎，您需要将以下 *dependency* 添加到您的项目中：`io.vertx:vertx-web-templ-jade:4.2.5`。 使用以下命令创建 Jade 模板引擎的实例：`io.vertx.ext.web.templ.jade.JadeTemplateEngine#create(io.vertx.core.Vertx)`。

使用 Jade 模板引擎时，如果文件名中没有指定扩展名，它将默认查找具有 `.jade` 扩展名的模板。

路由上下文 `RoutingContext` 在 Jade 模板中作为 `context` 变量可用，这意味着您可以根据上下文中的任何内容呈现模板，包括请求、响应、会话或上下文数据。

这里有些例子：

```
!!! 5
html
 head
   title= context.get('foo') + context.request().path()
 body
```

Please consult the [Jade4j documentation](https://github.com/neuland/jade4j) for how to write Jade templates.

### Handlebars 模板引擎

要使用 Handlebars，您需要将以下 *dependency* 添加到您的项目中：`io.vertx:vertx-web-templ-handlebars:4.2.5`。 使用以下命令创建 Handlebars 模板引擎的实例：`io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine#create(io.vertx.core.Vertx)`。

使用 Handlebars 模板引擎时，如果文件名中没有指定扩展名，它将默认查找具有 `.hbs` 扩展名的模板。

Handlebars模板不能调用对象中的任意方法，所以我们不能像使用其他模板引擎那样，把路由上下文传递给模板，然后让模板自检。

相反，模板中提供了上下文`data`。

如果您想访问其他数据，如请求路径、请求参数或会话数据，您应该在模板处理程序之前将其添加到处理程序中的上下文data。 例如：
```java
TemplateHandler handler = TemplateHandler.create(engine);

router.get("/dynamic").handler(ctx -> {

  ctx.put("request_path", ctx.request().path());
  ctx.put("session_data", ctx.session().data());

  ctx.next();
});

router.get("/dynamic/").handler(handler);
```

请参考【Handlebars Java 移植文档】（https://github.com/jknack/handlebars.java）了解如何编写handlebars 模板。

### Thymeleaf 模板引擎

要使用 Thymeleaf，您需要将以下 *dependency* 添加到您的项目中：`io.vertx:vertx-web-templ-thymeleaf:4.2.5`。 使用以下命令创建 Thymeleaf 模板引擎的实例：`io.vertx.ext.web.templ.thymeleaf.ThymeleafTemplateEngine#create(io.vertx.core.Vertx)`。

使用 Thymeleaf 模板引擎时，如果文件名中没有指定扩展名，它将默认查找具有 `.html` 扩展名的模板。

路由上下文 `RoutingContext` 在 Thymeleaf 模板中作为 `context` 变量可用，这意味着您可以根据上下文中的任何内容呈现模板，包括请求、响应、会话或上下文数据。

这里有些例子：

```
[snip]
<p th:text="${context.get('foo')}"></p>
<p th:text="${context.get('bar')}"></p>
<p th:text="${context.normalizedPath()}"></p>
<p th:text="${context.request().params().get('param1')}"></p>
<p th:text="${context.request().params().get('param2')}"></p>
[snip]
```

请查阅 [Thymeleaf 文档](http://www.thymeleaf.org/) 了解如何编写 Thymeleaf 模板。

### Apache FreeMarker 模板引擎

要使用 Apache FreeMarker，您需要将以下 *dependency* 添加到您的项目中：`io.vertx:vertx-web-templ-freemarker:4.2.5`。 使用：`io.vertx.ext.web.templ.Engine#create()` 创建 Apache FreeMarker 模板引擎的实例。

使用 Apache FreeMarker 模板引擎时，如果文件名中没有指定扩展名，它将默认查找具有 `.ftl` 扩展名的模板。

路由上下文 `RoutingContext` 在 Apache FreeMarker 模板中作为 `context` 变量可用，这意味着您可以根据上下文中的任何内容呈现模板，包括请求、响应、会话或上下文数据。

这里有些例子：

```
[snip]
<p th:text="${context.foo}"></p>
<p th:text="${context.bar}"></p>
<p th:text="${context.normalizedPath()}"></p>
<p th:text="${context.request().params().param1}"></p>
<p th:text="${context.request().params().param2}"></p>
[snip]
```

请参阅 [Apache FreeMarker 文档](http://www.freemarker.org/)，了解如何编写 Apache FreeMarker 模板。

### Pebble 模板引擎

要使用 Pebble，您需要将以下 *dependency* 添加到您的项目中：`io.vertx:vertx-web-templ-pebble:4.2.5`。 使用：`io.vertx.ext.web.templ.pebble.PebbleTemplateEngine#create(vertx)` 创建 Pebble 模板引擎的实例。

使用 Pebble 模板引擎时，如果文件名中没有指定扩展名，它将默认查找扩展名为 `.peb` 的模板。

路由上下文 `RoutingContext` 在 Pebble 模板中作为 `context` 变量可用，这意味着您可以根据上下文中的任何内容呈现模板，包括请求、响应、会话或上下文数据。

这里有些例子：

```
[snip]
<p th:text="{{context.foo}}"></p>
<p th:text="{{context.bar}}"></p>
<p th:text="{{context.normalizedPath()}}"></p>
<p th:text="{{context.request().params().param1}}"></p>
<p th:text="{{context.request().params().param2}}"></p>
[snip]
```

Please consult the [Pebble documentation](http://www.mitchellbosecke.com/pebble/home/) for how to write Pebble templates.

### Rocker 模板引擎

要使用 Rocker，请添加 `io.vertx:vertx-web-templ-rocker:4.2.5` 作为项目的依赖项。 然后，您可以使用 `io.vertx.ext.web.templ.rocker#create()` 创建一个 Rocker 模板引擎实例。

传递给 `render` 方法的 JSON 上下文对象的值随后会作为模板参数公开。 鉴于：

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

那么模板可以是如下的`somedir/TestRockerTemplate2.rocker.html`资源文件：

```
@import io.vertx.core.json.JsonObject
@args (JsonObject context, String foo, String bar)
Hello @foo and @bar
Request path is @context.getString("path")
```

### HTTL 模板引擎

要使用 HTTL，您需要将以下 *dependency* 添加到您的项目中：`io.vertx:vertx-web-templ-httl:4.2.5`。 使用以下命令创建 HTTL 模板引擎的实例：`io.vertx.ext.web.templ.httl.HTTLTemplateEngine#create(io.vertx.core.Vertx)`。

使用 HTML 模板引擎时，如果文件名中没有指定扩展名，它将默认查找带有 `.https` 扩展名的模板。

传递给 `render` 方法的 JSON 上下文对象的值随后会作为模板参数公开。 鉴于：

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

那么模板可以是如下的`somedir/test-httl-template1.httl`资源文件：

```
<!-- #set(String foo, String bar) -->
Hello ${foo} and ${bar}
```

关于如何编写 HTTL 模板，请参考 [HTTL 文档](https://httl.github.io/en/)。

### Rythm 模板引擎

要使用 Rythm，您需要将以下 *dependency* 添加到您的项目中：`io.vertx:vertx-web-templ-rythm:4.2.5`。 使用以下命令创建 Rythm 模板引擎的实例：`io.vertx.ext.web.templ.rythm.RythmTemplateEngine#create(io.vertx.core.Vertx)`。

使用 Rythm 模板引擎时，如果文件名中没有指定扩展名，它将默认查找具有 `.html` 扩展名的模板。

传递给 `render` 方法的 JSON 上下文对象的值随后会作为模板参数公开。 鉴于：

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

那么模板可以是如下的`somedir/test-rythm-template1.httl`资源文件：

```
<!-- #set(String foo, String bar) -->
Hello @foo and @bar
```

有关如何编写模板，请参阅 [RythmEngine 文档](http://www.rythmengine.org/)。

### 缓存

许多引擎支持缓存已编译的模板。 缓存存储在 vert.x 共享数据本地映射中，允许引擎以高效和安全的方式跨多个 Verticle 共享相同的缓存。

#### 禁用缓存

在开发过程中，您可能希望禁用模板缓存，以便在每次请求时重新评估模板。 为此，您需要将系统属性：`vertxweb.environment` 或环境变量`VERTXWEB_ENVIRONMENT` 设置为`dev` 或`development`。 默认情况下，缓存始终处于启用状态。

## 错误处理程序

您可以使用模板处理程序或其他方式呈现您自己的错误，但 Vert.x-Web 还包含一个开箱即用的“漂亮”错误处理程序，可以为您呈现错误页面。

处理程序是`ErrorHandler`。 要使用错误处理程序，只需将其设置为您想要覆盖的任何路径的故障处理程序。

## 请求记录器

Vert.x-Web 包含一个处理程序`LoggerHandler`，您可以使用它来记录 HTTP 请求。 您应该在任何可能使 `RoutingContext` 失败的处理程序之前安装此处理程序

默认情况下，请求被记录到 Vert.x 记录器，可以配置为使用 JUL 记录、log4j 或 SLF4J。

请参阅  `LoggerFormat`.

## 提供网站图标

Vert.x-Web 包含处理程序`FaviconHandler`，特别是用于提供 favicon。

Favicon 可以使用文件系统的路径指定，或者默认情况下 Vert.x-Web 将在类路径上查找名为 `favicon.ico` 的文件。 这意味着您将 favicon 捆绑在应用程序的 jar 中。

## 超时处理程序

Vert.x-Web 包含一个超时处理程序，如果处理时间过长，您可以使用它来超时请求。

这是使用 `TimeoutHandler` 的实例配置的。

如果在写入响应之前请求超时，则会将`503`响应返回给客户端。

这是一个使用超时处理程序的示例，它将在 5 秒后超时对以 `/foo` 开头的路径的所有请求：

```java
router.route("/foo/").handler(TimeoutHandler.create(5000));
```

## 响应时间处理程序

此处理程序设置标头`x-response-time`响应标头，其中包含从收到请求到写入响应标头的时间，以毫秒为单位，例如：

```
x-response-time: 1456ms
```

## 内容类型处理程序

`ResponseContentTypeHandler` 可以自动设置 `Content-Type` 标头。 假设我们正在构建一个 RESTful Web 应用程序。 我们需要在所有处理程序中设置内容类型：

```java
router
  .get("/api/books")
  .produces("application/json")
  .handler(ctx -> findBooks()
    .onSuccess(books -> ctx.response()
      .putHeader("Content-Type", "application/json")
      .end(toJson(books))).onFailure(ctx::fail));
```

如果 API 表面变得非常大，那么设置内容类型会变得很麻烦。 为避免这种情况，请将 `ResponseContentType Handler` 添加到相应的路由中：

```java
router.route("/api/*").handler(ResponseContentTypeHandler.create());
router
  .get("/api/books")
  .produces("application/json")
  .handler(ctx -> findBooks()
    .onSuccess(books -> ctx.response()
      .end(toJson(books))).onFailure(ctx::fail));
```

处理程序从 `getAcceptableContentType` 获取适当的内容类型。 因此，您可以轻松地共享相同的处理程序来生成不同类型的数据：

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

SockJS 是一个客户端 JavaScript 库和协议，它提供了一个简单的类似 WebSocket 的接口，允许您连接到 SockJS 服务器，而不管实际的浏览器或网络是否允许真正的 WebSocket。

它通过支持浏览器和服务器之间的各种不同传输，并在运行时根据浏览器和网络功能选择一种来实现这一点。

所有这一切对你来说都是透明的——你只是看到了*正常工作*的类似 WebSocket 的接口。

有关 SockJS 的更多信息，请参阅 [SockJS 网站](https://github.com/sockjs/sockjs-client)。

### SockJS 处理程序

Vert.x 提供了一个开箱即用的处理程序，称为`SockJSHandler`，用于在您的 Vert.x-Web 应用程序中使用 SockJS。

您应该使用 `SockJSHandler.create` 为每个 SockJS 应用程序创建一个处理程序。 您还可以在创建实例时指定配置选项。 配置选项是用一个 `SockJSHandlerOptions` 的实例来描述的。

```java
Router router = Router.router(vertx);

SockJSHandlerOptions options = new SockJSHandlerOptions()
  .setHeartbeatInterval(2000);

SockJSHandler sockJSHandler = SockJSHandler.create(vertx, options);
```

### 处理SockJS套接字

在服务器端，你在`SockJSHandler`上设置一个处理程序，每次从客户端建立SockJS连接时，这个处理程序都会被调用:

传递给处理程序的对象是一个`SockJSSocket`。 它有一个熟悉的类套接字接口，您可以像 `NetSocket` 或 `WebSocket` 一样对其进行读写。 它还实现了 `ReadStream` 和 `WriteStream`，因此您可以将其泵入和从其他读写流中提取。 当使用 `routingContext` 加载 SockJS 连接时，`RoutingContext` 可用于手动会话管理。 有了它，您可以管理通过 `webSession` 和 `webUser` 访问的用户和会话。

这是一个简单的 SockJS 处理程序的示例，它简单地回显它读取的任何数据：

```java
Router router = Router.router(vertx);

SockJSHandlerOptions options = new SockJSHandlerOptions()
  .setHeartbeatInterval(2000);

SockJSHandler sockJSHandler = SockJSHandler.create(vertx, options);

router.mountSubRouter("/myapp", sockJSHandler.socketHandler(sockJSSocket -> {

  // Just echo the data back
  sockJSSocket.handler(sockJSSocket::write);

}));
```

### 客户端

在客户端 JavaScript 中，您使用 SockJS 客户端库来建立连接。 为方便起见，该软件包可在 https://www.npmjs.com/package/sockjs-client 上找到。

这意味着您可以从捆绑程序或构建工具中引用它。 然而，如果你想获得一个直接在你的 `HTML` 文档上使用的 `CDN` 版本，首先你需要参考 sockjs 依赖：

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

[SockJS 网站](https://github.com/sockjs/sockjs-client) 上提供了使用 SockJS JavaScript 客户端的完整详细信息，但总的来说，您可以像这样使用它：

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
 return true; // in order to signal that the message has been processed
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

### 配置 SockJS 处理程序

可以使用 `SockJSHandlerOptions` 为处理程序配置各种选项。

> **🏷注意:** 默认情况下，配置不包含默认的 `Origin` 属性。 为防止来自 Web 浏览器的 Cross-Site WebSocket Hijacking 攻击，建议将此属性设置为应用程序面向 Internet 的来源。 这将强制检查 Web 套接字来源是否来自此应用程序。 这个检查很重要，因为 WebSocket 不受同源策略的限制，攻击者可以轻松地从恶意网页发起 WebSocket 请求，目标是 sockJS 桥的 `ws://` 或 `wss://` 端点 URL。

### 通过事件总线写入 SockJS 套接字

当一个`SockJSSocket`被创建时，它可以向事件总线注册一个事件处理程序。 该处理程序的地址由 `writeHandlerID` 给出。

默认情况下，事件处理程序未注册。 它必须在 `SockJSHandlerOptions` 中启用。

```java
Router router = Router.router(vertx);

SockJSHandlerOptions options = new SockJSHandlerOptions().setRegisterWriteHandler(true);

SockJSHandler sockJSHandler = SockJSHandler.create(vertx, options);

router.mountSubRouter("/myapp", sockJSHandler.socketHandler(sockJSSocket -> {

  // Retrieve the writeHandlerID and store it (e.g. in a local map)
  String writeHandlerID = sockJSSocket.writeHandlerID();

}));
```

> **🏷注意:** 默认情况下，处理程序仅在本地注册。 可以使用 `setLocalWriteHandler` 使其在集群范围内。

然后您可以通过事件总线将 `[Buffer](https://vertx.io/docs/apidocs/io/vertx/core/buffer/Buffer.html)` 写入 SockJS 套接字。

```java
eventBus.send(writeHandlerID, Buffer.buffer("foo"));
```

## SockJS 事件总线桥

Vert.x-Web 带有一个内置的 SockJS 套接字处理程序，称为事件总线桥，它有效地将服务器端 Vert.x 事件总线扩展到客户端 JavaScript。

这将创建一个分布式事件总线，它不仅跨越服务器端的多个 Vert.x 实例，还包括在浏览器中运行的客户端 JavaScript。

因此，我们可以创建一个包含许多浏览器和服务器的巨大分布式总线。 只要服务器已连接，浏览器就不必连接到同一服务器。

这是通过提供一个名为`vertx-eventbus.js`的简单客户端 JavaScript 库来完成的，该库提供了一个与服务器端 Vert.x 事件总线 API 非常相似的 API，它允许您向事件总线发送和发布消息 并注册处理程序以接收消息。

这个 JavaScript 库使用 JavaScript SockJS 客户端通过 SockJS 连接来隧道化事件总线流量，该连接终止于服务器端的`SockJSHandler`。

然后在 `SockJSHandler` 上安装一个特殊的 SockJS 套接字处理程序，它处理 SockJS 数据并将其与服务器端事件总线桥接。

要激活桥接，您只需在 SockJS 处理程序上调用 `bridge`。

```java
Router router = Router.router(vertx);

SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
SockJSBridgeOptions options = new SockJSBridgeOptions();
// mount the bridge on the router
router.mountSubRouter("/eventbus", sockJSHandler.bridge(options));
```

在客户端 JavaScript 中，您使用 [@vertx/eventbus-bridge-client.js](http://npmjs.com/package/@vertx/eventbus-bridge-client.js) 库来创建与事件总线的连接并 发送和接收消息。 该库在 [NPM](http://npmjs.com/package/@vertx/eventbus-bridge-client.js) 上可用，因此它可以很容易地与捆绑器或构建工具一起使用，但可以很容易地从 CDN（就像之前的 sockJS 示例）：

```html
<script src="https://unpkg.io/sockjs-client@1.5.0/dist/sockjs.min.js"></script>
<script src='https://unpkg.io/@vertx/eventbus-bridge-client.js@1.0.0-1/vertx-eventbus.js'></script>

<script>

var eb = new EventBus('http://localhost:8080/eventbus');

eb.onopen = () => {

 // set a handler to receive a message
 eb.registerHandler('some-address', (error, message) => {
   console.log('received a message: ' + JSON.stringify(message));
 });

 // send a message
 eb.send('some-address', {name: 'tim', age: 587});

}

</script>
```

该示例所做的第一件事是创建事件总线的实例

```javascript
var eb = new EventBus('http://localhost:8080/eventbus');
```

构造函数的参数是连接到事件总线的 URI。 由于我们创建了带有前缀`eventbus`的桥，我们将在那里连接。

在打开连接之前，您实际上无法对连接执行任何操作。 当它打开时，将调用 `onopen` 处理程序。

网桥支持自动重新连接，具有可配置的延迟和回退选项。

```javascript
var eb = new EventBus('http://localhost:8080/eventbus');
eb.enableReconnect(true);
eb.onopen = function() {}; // Set up handlers here, will be called on initial connection and all reconnections
eb.onreconnect = function() {}; // Optional, will only be called on reconnections

// Alternatively, pass in an options object
var options = {
   vertxbus_reconnect_attempts_max: Infinity, // Max reconnect attempts
   vertxbus_reconnect_delay_min: 1000, // Initial delay (in ms) before first reconnect attempt
   vertxbus_reconnect_delay_max: 5000, // Max delay (in ms) between reconnect attempts
   vertxbus_reconnect_exponent: 2, // Exponential backoff factor
   vertxbus_randomization_factor: 0.5 // Randomization factor between 0 and 1
};

var eb2 = new EventBus('http://localhost:8080/eventbus', options);
eb2.enableReconnect(true);
// Set up handlers...
```

### 保护桥接

如果你像上面的例子一样在没有保护它的情况下启动了一个桥，并试图通过它发送消息，你会发现消息神秘地消失了。 他们发生了什么？

对于大多数应用程序，您可能不希望客户端 JavaScript 能够将任何消息发送到服务器端的任何处理程序或所有其他浏览器。

例如，您可能在事件总线上有一个允许访问或删除数据的服务。 我们不希望行为不端或恶意的客户端能够删除您数据库中的所有数据！

此外，我们不一定希望任何客户端能够监听任何事件总线地址。

为了解决这个问题，SockJS 桥默认拒绝任何消息通过。 您可以告诉网桥哪些消息可以通过。 （始终允许通过的回复消息有一个例外）。

换句话说，网桥就像一种具有默认 *deny-all* 策略的防火墙。

配置网桥来告诉它应该通过什么消息很容易。

您可以使用调用 bridge 时传入的 `SockJSBridgeOptions` 指定要允许入站和出站流量的 *匹配*。

每个匹配项都是一个`PermittedOptions`对象：

- `setAddress`

  这表示消息发送到的确切地址。 如果您想允许基于确切地址的消息，请使用此字段。

- `setAddressRegex`

  这是一个将与地址匹配的正则表达式。 如果要允许基于正则表达式的消息，请使用此字段。 如果指定了`address`字段，则该字段将被忽略。

- `setMatch`

  这允许您根据其结构允许消息。 匹配中的任何字段必须以相同的值存在于消息中，才能被允许。 这目前仅适用于 JSON 消息。

如果消息是 *in-bound* （即从客户端 JavaScript 发送到服务器），则 Vert.x-Web 将查看所有允许的入站匹配。 如果有任何匹配，它将被允许通过。

如果消息在发送到客户端之前是*out-bound*（即从服务器发送到客户端 JavaScript），则 Vert.x-Web 将查看任何出站允许的匹配项。 如果有任何匹配，它将被允许通过。

实际匹配工作如下：

如果指定了 `address` 字段，则 `address` 必须与消息的地址*完全*匹配才能被视为匹配。

如果没有指定`address`字段并且指定了`addressRegex`字段，则`address_re`中的正则表达式必须与消息的地址匹配才能被视为匹配。

如果指定了 `match` 字段，则消息的结构也必须匹配。 通过查看匹配对象中的所有字段和值并检查它们是否都存在于实际消息正文中来构建匹配。

这是一个例子：

```java
Router router = Router.router(vertx);

SockJSHandler sockJSHandler = SockJSHandler.create(vertx);


// Let through any messages sent to 'demo.orderMgr' from the client
PermittedOptions inboundPermitted1 = new PermittedOptions()
  .setAddress("demo.orderMgr");

// Allow calls to the address 'demo.persistor' from the client as
// long as the messages have an action field with value 'find'
// and a collection field with value 'albums'
PermittedOptions inboundPermitted2 = new PermittedOptions()
  .setAddress("demo.persistor")
  .setMatch(new JsonObject().put("action", "find")
    .put("collection", "albums"));

// Allow through any message with a field `wibble` with value `foo`.
PermittedOptions inboundPermitted3 = new PermittedOptions()
  .setMatch(new JsonObject().put("wibble", "foo"));

// First let's define what we're going to allow from server -> client

// Let through any messages coming from address 'ticker.mystock'
PermittedOptions outboundPermitted1 = new PermittedOptions()
  .setAddress("ticker.mystock");

// Let through any messages from addresses starting with "news."
// (e.g. news.europe, news.usa, etc)
PermittedOptions outboundPermitted2 = new PermittedOptions()
  .setAddressRegex("news\\..+");

// Let's define what we're going to allow from client -> server
SockJSBridgeOptions options = new SockJSBridgeOptions().
  addInboundPermitted(inboundPermitted1).
  addInboundPermitted(inboundPermitted1).
  addInboundPermitted(inboundPermitted3).
  addOutboundPermitted(outboundPermitted1).
  addOutboundPermitted(outboundPermitted2);

// mount the bridge on the router
router.mountSubRouter("/eventbus", sockJSHandler.bridge(options));
```

### 消息需要授权

事件总线桥也可以配置为使用 Vert.x-Web 授权功能来要求对桥上的入站或出站消息进行授权。

为此，您可以在上一节中描述的匹配中添加额外的字段，以确定匹配所需的权限。

要声明登录用户需要特定权限才能访问允许消息，请使用`setRequiredAuthority`字段。

这是一个例子：

```java
PermittedOptions inboundPermitted = new PermittedOptions()
  .setAddress("demo.orderService");

// But only if the user is logged in and has the authority "place_orders"
inboundPermitted.setRequiredAuthority("place_orders");

SockJSBridgeOptions options = new SockJSBridgeOptions()
  .addInboundPermitted(inboundPermitted);
```

要使用户获得授权，他们必须首先登录，然后才能获得所需的权限。

要处理登录和实际身份验证，您可以配置普通的 Vert.x 身份验证处理程序。 例如：

```java
Router router = Router.router(vertx);

// Let through any messages sent to 'demo.orderService' from the client
PermittedOptions inboundPermitted = new PermittedOptions()
  .setAddress("demo.orderService");

// But only if the user is logged in and has the authority "place_orders"
inboundPermitted.setRequiredAuthority("place_orders");

SockJSHandler sockJSHandler = SockJSHandler.create(vertx);

// Now set up some basic auth handling:

router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

AuthenticationHandler basicAuthHandler = BasicAuthHandler.create(authProvider);

router.route("/eventbus/*").handler(basicAuthHandler);

// mount the bridge on the router
router.mountSubRouter(
  "/eventbus",
  sockJSHandler.bridge(new SockJSBridgeOptions()
    .addInboundPermitted(inboundPermitted)));
```

### 处理事件总线桥事件

如果你想在桥上发生事件时得到通知，你可以在调用 `bridge` 时提供一个处理程序。

每当桥上发生事件时，它将传递给处理程序。 该事件由`BridgeEvent`的一个实例描述。

事件可以是以下类型之一：

- SOCKET_CREATED

  此事件将在创建新的 SockJS 套接字时发生。

- SOCKET_IDLE

  当 SockJS 套接字空闲的时间比最初配置的时间长时，将发生此事件。

- SOCKET_PING

  当 SockJS 套接字的最后一个 ping 时间戳更新时，将发生此事件。

- SOCKET_CLOSED

  此事件将在 SockJS 套接字关闭时发生。

- SOCKET_ERROR

  当底层传输错误时会发生此事件。

- SEND

  当尝试从客户端向服务器发送消息时，将发生此事件。

- PUBLISH

  当尝试将消息从客户端发布到服务器时，将发生此事件。

- RECEIVE

  当尝试将消息从服务器传递到客户端时，将发生此事件。

- REGISTER

  当客户端尝试注册处理程序时，将发生此事件。

- UNREGISTER

  当客户端尝试取消注册处理程序时，将发生此事件。

该事件使您能够使用 `type` 检索类型并使用 `getRawMessage` 检查事件的原始消息。

原始消息是具有以下结构的 JSON 对象：

```json
{
 "type": "send"|"publish"|"receive"|"register"|"unregister",
 "address": the event bus address being sent/published/registered/unregistered
 "body": the body of the message
}
```

> **🏷注意:** `SOCKET_ERROR` 事件可能包含一条消息。 在这种情况下检查类型属性，可能会引入一种新的消息。 `err`消息。 这是出现套接字异常时生成的合成消息。 消息将遵循桥接协议，如下所示：

```json
{
 "type": "err",
 "failureType": "socketException",
 "message": "optionally a message from the exception being raised"
}
```

该事件也是 `Promise` 的一个实例。 处理完事件后，您可以使用 `true` 完成承诺以启用进一步处理。

如果您不希望处理该事件，您可以使用 `false` 完成承诺。 这是一个有用的功能，使您能够对通过网桥的消息进行自己的过滤，或者可能应用一些细粒度的授权或指标。

这是一个示例，如果它们包含单词“Armadillos”，我们会拒绝所有通过桥接的消息。

```java
Router router = Router.router(vertx);

// Let through any messages sent to 'demo.orderMgr' from the client
PermittedOptions inboundPermitted = new PermittedOptions()
  .setAddress("demo.someService");

SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
SockJSBridgeOptions options = new SockJSBridgeOptions()
  .addInboundPermitted(inboundPermitted);

// mount the bridge on the router
router
  .mountSubRouter("/eventbus", sockJSHandler
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

这是一个如何配置和处理 SOCKET_IDLE 桥接事件类型的示例。 注意 `setPingTimeout(5000)` 表示如果 ping 消息在 5 秒内没有从客户端到达，那么 SOCKET_IDLE 桥接事件将被触发。

```java
Router router = Router.router(vertx);

// Initialize SockJS handler
SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
SockJSBridgeOptions options = new SockJSBridgeOptions()
  .addInboundPermitted(inboundPermitted)
  .setPingTimeout(5000);

// mount the bridge on the router
router
  .mountSubRouter("/eventbus", sockJSHandler.bridge(options, be -> {
    if (be.type() == BridgeEventType.SOCKET_IDLE) {
      // Do some custom handling...
    }

    be.complete(true);
  }));
```

在客户端 JavaScript 中，您使用`vertx-eventbus.js`库来创建与事件总线的连接并发送和接收消息：

```html
<script src="https://unpkg.io/sockjs-client@1.5.0/dist/sockjs.min.js"></script>
<script src='https://unpkg.io/@vertx/eventbus-bridge-client.js@1.0.0-1/vertx-eventbus.js'></script>

<script>

var eb = new EventBus('http://localhost:8080/eventbus', {"vertxbus_ping_interval": 300000}); // sends ping every 5 minutes.

eb.onopen = function() {

// set a handler to receive a message
eb.registerHandler('some-address', function(error, message) {
  console.log('received a message: ' + JSON.stringify(message));
});

// send a message
eb.send('some-address', {name: 'tim', age: 587});
}

</script>
```

该示例所做的第一件事是创建事件总线的实例

```javascript
var eb = new EventBus('http://localhost:8080/eventbus', {"vertxbus_ping_interval": 300000});
```

构造函数的第二个参数告诉 sockjs 库每 5 分钟发送一次 ping 消息。 因为服务器被配置为每 5 秒 ping 一次 → `SOCKET_IDLE` 将在服务器上触发。

您还可以修改原始消息，例如 改变body。 对于从客户端流入的消息，您还可以在消息中添加标头，这是一个示例：

```java
Router router = Router.router(vertx);

// Let through any messages sent to 'demo.orderService' from the client
PermittedOptions inboundPermitted = new PermittedOptions()
  .setAddress("demo.orderService");

SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
SockJSBridgeOptions options = new SockJSBridgeOptions()
  .addInboundPermitted(inboundPermitted);

// mount the bridge on the router
router.mountSubRouter(
  "/eventbus",
  sockJSHandler.bridge(options, be -> {
    if (
      be.type() == BridgeEventType.PUBLISH ||
        be.type() == BridgeEventType.SEND) {

      // Add some headers
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

## CSRF 跨站请求伪造

CSRF 或有时也称为 XSRF 是一种技术，未经授权的站点可以通过该技术获取用户的私人数据。 Vert.x-Web 包含一个处理程序`CSRFHandler`，您可以使用它来防止跨站点请求伪造请求。

在此处理程序下的每个获取请求上，都会将 cookie 添加到具有唯一令牌的响应中。 然后，客户端应在标头中返回此令牌。 由于发送了 cookie，因此需要 cookie 处理程序也存在于router上。

在开发依赖用户代理执行`POST`操作的非单页应用程序时，不能在 HTML 表单上指定标头。 为了解决这个问题，当且仅当表单属性中不存在与标头相同名称的标头时，也会检查标头值，例如：

```html
<form action="/submit" method="POST">
<input type="hidden" name="X-XSRF-TOKEN" value="abracadabra">
</form>
```

用户有责任为表单字段填写正确的值。 喜欢使用纯 HTML 解决方案的用户可以通过从键`X-XSRF-TOKEN`或他们在`CSRFHandler`对象实例化期间选择的标头名称下的路由上下文中获取令牌值来填充此值。

```java
router.route().handler(CSRFHandler.create(vertx, "abracadabra"));
router.route().handler(ctx -> {

});
```

请注意，此处理程序是会话感知的。 如果有可用的会话，则可能会在`POST`操作期间省略表单参数或标头，因为它将从会话中读取。 这也意味着令牌只会在会话升级时重新生成。

请注意，为了额外的安全性，建议用户轮换签署令牌的密钥。 这可以通过替换处理程序或使用新配置重新启动应用程序来在线完成。 点击劫持仍可能影响应用程序。 如果这是一个关键应用程序，请考虑设置标题：`X-Frame-Options`，如：`https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Frame-Options`所述

### 使用 AJAX

通过 ajax 访问受保护的路由时，需要在请求中传递两个 csrf 令牌。 通常这是使用请求标头完成的，因为添加请求标头通常可以在中心位置轻松完成，而无需修改有效负载。

CSRF 令牌是从键值为`X-XSRF-TOKEN`的服务器端上下文中获取的（除非您指定了不同的名称）。 此令牌需要向客户端公开，通常是通过将其包含在初始页面内容中。 一种可能性是将其存储在 HTML的 `<meta>` 标记中，然后可以在 JavaScript 请求时检索值。

以下内容可以包含在您的视图中（下面的车把示例）：

```html
<meta name="csrf-token" content="${X-XSRF-TOKEN}">
```

以下是使用 Fetch API 使用页面上`<meta>` 标记的 CSRF 令牌发布到 `/process` 路由的示例：

```js
// Read the CSRF token from the <meta> tag
var token = document.querySelector('meta[name="csrf-token"]').getAttribute('content')

// Make a request using the Fetch API
fetch('/process', {
 credentials: 'same-origin', // <-- includes cookies in the request
 headers: {
   'X-XSRF-TOKEN': token // <-- is the csrf token as a header
 },
 method: 'POST',
 body: {
   key: 'value'
 }
})
```

## HSTS 处理器

HTTP 严格传输安全 (HSTS) 是一种 Web 安全策略机制，有助于保护网站免受协议降级攻击和 cookie 劫持等中间人攻击。 它允许 Web 服务器声明 Web 浏览器（或其他符合要求的用户代理）应仅使用 HTTPS 连接自动与其交互，该连接提供传输层安全性 (TLS/SSL)，这与单独使用不安全的 HTTP 不同。 HSTS 是一种 IETF 标准跟踪协议，在 RFC 6797 中指定。

此处理程序将在一个步骤中为您的应用程序配置正确的标头：

```java
router.route().handler(HSTSHandler.create());
```

## CSP 处理器

内容安全策略 (CSP) 是附加的安全层，有助于检测和缓解某些类型的攻击，包括跨站点脚本 (XSS) 和数据注入攻击。 这些攻击被用于从数据盗窃到站点破坏再到恶意软件分发的所有事情。

CSP 旨在完全向后兼容。 不支持它的浏览器仍然可以与实现它的服务器一起使用，反之亦然：不支持 CSP 的浏览器会简单地忽略它，照常运行，默认为 Web 内容的标准同源策略。 如果网站不提供 CSP 标头，浏览器同样会使用标准的同源策略。

```java
router.route().handler(
  CSPHandler.create()
    .addDirective("default-src", "*.trusted.com"));
```

## XFrame 处理器

`X-Frame-Options` HTTP 响应标头可用于指示是否应允许浏览器在`frame`、`iframe`、`embed` 或`object` 中呈现页面。 网站可以使用它来避免点击劫持攻击，方法是确保其内容不会嵌入到其他网站中。

仅当访问文档的用户使用支持`X-Frame-Options`的浏览器时，才提供附加的安全性。

如果您指定 `DENY`，当从其他站点加载时，不仅尝试在框架中加载页面会失败，从同一站点加载时尝试这样做也会失败。 另一方面，如果您指定`SAMEORIGIN`，您仍然可以在框架中使用该页面，只要将其包含在框架中的站点与提供该页面的站点相同。

此处理程序将在一个步骤中为您的应用程序配置正确的标头：

```java
router.route().handler(XFrameHandler.create(XFrameHandler.DENY));
```

## OAuth2AuthHandler 处理器

`OAuth2AuthHandler` 允许使用 OAuth2 协议快速设置安全路由。 此处理程序简化了 authCode 流程。 使用它来保护某些资源并使用 GitHub 进行身份验证的示例可以实现为：

```java
OAuth2Auth authProvider = GithubAuth
  .create(vertx, "CLIENT_ID", "CLIENT_SECRET");

// create a oauth2 handler on our running server
// the second argument is the full url to the
// callback as you entered in your provider management console.
OAuth2AuthHandler oauth2 = OAuth2AuthHandler
  .create(vertx, authProvider, "https://myserver.com/callback");

// setup the callback handler for receiving the GitHub callback
oauth2.setupCallback(router.route("/callback"));

// protect everything under /protected
router.route("/protected/*").handler(oauth2);
// mount some handler under the protected zone
router
  .route("/protected/somepage")
  .handler(ctx -> ctx.response().end("Welcome to the protected resource!"));

// welcome page
router
  .get("/")
  .handler(ctx -> ctx.response()
    .putHeader("content-type", "text/html")
    .end("Hello<br><a href=\"/protected/somepage\">Protected by Github</a>"));
```

OAuth2AuthHandler 将设置一个适当的回调 OAuth2 处理程序，因此用户不需要处理授权服务器响应的验证。 知道授权服务器响应仅有效一次是非常重要的，这意味着如果客户端发出回调 URL 的重新加载，它将被断言为无效请求，因为验证将失败。

经验法则是，一旦执行有效的回调，客户端就会重定向到受保护的资源。 此重定向还应创建会话 cookie（或其他会话机制），因此用户无需为每个请求进行身份验证。

由于 OAuth2 规范的性质，为了使用其他 OAuth2 提供程序，需要进行细微的更改，但 vertx-auth 为您提供了许多开箱即用的实现：

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

但是，如果您使用的是未列出的提供程序，您仍然可以使用基本 API 来执行此操作，如下所示：

```java
OAuth2Auth authProvider = OAuth2Auth.create(vertx, new OAuth2Options()
  .setClientId("CLIENT_ID")
  .setClientSecret("CLIENT_SECRET")
  .setFlow(OAuth2FlowType.AUTH_CODE)
  .setSite("https://accounts.google.com")
  .setTokenPath("https://www.googleapis.com/oauth2/v3/token")
  .setAuthorizationPath("/o/oauth2/auth"));

// create a oauth2 handler on our domain: "http://localhost:8080"
OAuth2AuthHandler oauth2 = OAuth2AuthHandler
  .create(vertx, authProvider, "http://localhost:8080");

// these are the scopes
oauth2.withScope("profile");

// setup the callback handler for receiving the Google callback
oauth2.setupCallback(router.get("/callback"));

// protect everything under /protected
router.route("/protected/*").handler(oauth2);
// mount some handler under the protected zone
router
  .route("/protected/somepage")
  .handler(ctx -> ctx.response().end("Welcome to the protected resource!"));

// welcome page
router
  .get("/")
  .handler(ctx -> ctx.response()
    .putHeader("content-type", "text/html")
    .end("Hello<br><a href=\"/protected/somepage\">Protected by Google</a>"));
```

您将需要手动提供提供商的所有详细信息，但最终结果是相同的。

处理程序会将您的应用程序固定到配置的回调 url。 用法很简单，为处理程序提供一个路由实例，所有设置都将为您完成。 在典型的用例中，您的提供商会询问您应用程序的回调 url 是什么，然后您输入一个 url，例如：`https://myserver.com/callback`。 这是处理程序的第二个参数，现在您只需要设置它。 为了使最终用户更容易，您需要做的就是调用 setupCallback 方法。

这是您将处理程序固定到服务器`https://myserver.com:8447/callback`的方式。 请注意，默认值的端口号不是必需的，http 为 80，https 为 443。

```java
OAuth2AuthHandler oauth2 = OAuth2AuthHandler
  .create(vertx, provider, "https://myserver.com:8447/callback");

// now allow the handler to setup the callback url for you
oauth2.setupCallback(router.route("/callback"));
```

在示例中，路由对象是由`Router.route()`内联创建的，但是如果您想完全控制调用处理程序的顺序（例如，您希望在链中尽快调用它），您 始终可以在之前创建路由对象并将其作为引用传递给此方法。

### 一个真实的例子

到目前为止，您已经学习了如何使用 Oauth2 处理程序，但是您会注意到对于每个请求，您都需要进行身份验证。 这是因为处理程序没有状态，并且示例中没有应用状态管理。

尽管对于面向 API 的端点建议没有状态，例如，对于面向用户的端点使用 JWT（我们稍后会介绍），我们可以将身份验证结果存储在会话中。 为此，我们需要一个类似于以下代码段的应用程序：

```java
OAuth2Auth authProvider =
  GithubAuth
    .create(vertx, "CLIENTID", "CLIENT SECRET");
// We need a user session handler too to make sure
// the user is stored in the session between requests
router.route()
  .handler(SessionHandler.create(LocalSessionStore.create(vertx)));
// we now protect the resource under the path "/protected"
router.route("/protected").handler(
  OAuth2AuthHandler.create(
    vertx,
      authProvider,
      "http://localhost:8080/callback")
    // we now configure the oauth2 handler, it will
    // setup the callback handler
    // as expected by your oauth2 provider.
    .setupCallback(router.route("/callback"))
    // for this resource we require that users have
    // the authority to retrieve the user emails
    .withScope("user:email")
);
// Entry point to the application, this will render
// a custom template.
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
// The protected resource
router.get("/protected").handler(ctx -> {
  // at this moment your user object should contain the info
  // from the Oauth2 response, since this is a protected resource
  // as specified above in the handler config the user object is never null
  User user = ctx.user();
  // just dump it to the client for demo purposes
  ctx.response().end(user.toString());
});
```

### 混合 OAuth2 和 JWT

一些提供商使用 JWT 令牌作为访问令牌，这是 [RFC6750](https://tools.ietf.org/html/rfc6750) 的一项功能，当想要混合基于客户端的身份验证和 API 授权时非常有用。 例如，假设您有一个应用程序提供了一些受保护的 HTML 文档，但您还希望它可供 API 使用。 在这种情况下，API 无法轻松执行 OAuth2 所需的重定向握手，但可以使用事先提供的 Token。

只要将提供程序配置为支持 JWT，这将由处理程序自动处理。

在现实生活中，这意味着您的 API 可以使用标头`Authorization`和值为`Bearer BASE64 ACCESS_TOKEN`来访问受保护的资源。

### WebAuthn

我们的网络生活依赖于一种过时而脆弱的密码观念。密码介于恶意用户和你的银行账户或社交媒体账户之间。密码很难维护;很难将它们存储在服务器上(密码会被窃取)。它们很难记住，或者不告诉别人(网络钓鱼攻击)。

但是还有更好的方法！ 一个无密码的世界，它是 W3C 和 FIDO 联盟在您的浏览器上运行的标准。

WebAuthn是一个API，它允许服务器使用公钥加密而不是密码来注册和认证用户，这个API在认证设备的帮助下以用户可访问的方式使用加密，例如yubikey令牌或你的手机。

该协议至少需要在router上安装第一个回调：

1. `/webauthn/response` 用于执行所有验证的回调
2. `/webauthn/login` 允许用户启动登录流程的端点（可选，但没有它将无法登录）
3. `/webauthn/register` 允许用户注册新标识符的端点（可选，如果数据已存储，则不需要此端点）

受保护应用程序的示例是：

```java
WebAuthn webAuthn = WebAuthn.create(
  vertx,
  new WebAuthnOptions()
    .setRelyingParty(new RelyingParty().setName("Vert.x WebAuthN Demo"))
    // What kind of authentication do you want? do you care?
    // # security keys
    .setAuthenticatorAttachment(AuthenticatorAttachment.CROSS_PLATFORM)
    // # fingerprint
    .setAuthenticatorAttachment(AuthenticatorAttachment.PLATFORM)
    .setUserVerification(UserVerification.REQUIRED))
  // where to load the credentials from?
  .authenticatorFetcher(fetcher)
  // update the state of an authenticator
  .authenticatorUpdater(updater);

// parse the BODY
router.post()
  .handler(BodyHandler.create());
// add a session handler
router.route()
  .handler(SessionHandler
    .create(LocalSessionStore.create(vertx)));

// security handler
WebAuthnHandler webAuthNHandler = WebAuthnHandler.create(webAuthn)
  .setOrigin("https://192.168.178.74.xip.io:8443")
  // required callback
  .setupCallback(router.post("/webauthn/response"))
  // optional register callback
  .setupCredentialsCreateCallback(router.post("/webauthn/register"))
  // optional login callback
  .setupCredentialsGetCallback(router.post("/webauthn/login"));

// secure the remaining routes
router.route().handler(webAuthNHandler);
```

应用程序在后端不安全，但需要在客户端执行一些代码。需要一些样板文件，以这两个函数为例:

```javascript
/**
* Converts PublicKeyCredential into serialised JSON
* @param  {Object} pubKeyCred
* @return {Object}            - JSON encoded publicKeyCredential
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
* Generate secure random buffer
* @param  {Number} len - Length of the buffer (default 32 bytes)
* @return {Uint8Array} - random string
*/
var generateRandomBuffer = (len) => {
 len = len || 32;

 let randomBuffer = new Uint8Array(len);
 window.crypto.getRandomValues(randomBuffer);

 return randomBuffer
};

/**
* Decodes arrayBuffer required fields.
*/
var preformatMakeCredReq = (makeCredReq) => {
 makeCredReq.challenge = base64url.decode(makeCredReq.challenge);
 makeCredReq.user.id = base64url.decode(makeCredReq.user.id);

 return makeCredReq
};

/**
* Decodes arrayBuffer required fields.
*/
var preformatGetAssertReq = (getAssert) => {
 getAssert.challenge = base64url.decode(getAssert.challenge);

 for (let allowCred of getAssert.allowCredentials) {
   allowCred.id = base64url.decode(allowCred.id)
 }

 return getAssert
};
```

这些函数将帮助您与服务器进行交互。仅此而已。让我们从登录用户开始:

```javascript
// using the functions defined before...
getGetAssertionChallenge({name: 'your-user-name'})
.then((response) => {
 // base64 must be decoded to a JavaScript Buffer
 let publicKey = preformatGetAssertReq(response);
 // the response is then passed to the browser
 // to generate an assertion by interacting with your token/phone/etc...
 return navigator.credentials.get({publicKey})
})
.then((response) => {
 // convert response buffers to base64 and json
 let getAssertionResponse = publicKeyCredentialToJSON(response);
 // send information to server
 return sendWebAuthnResponse(getAssertionResponse)
})
.then((response) => {
 // success!
 alert('Login success')
})
.catch((error) => alert(error));

// utility functions

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

上面的示例已经覆盖了 66% 的 API，覆盖了 3 个端点中的 2 个。 最后一个端点是用户注册。 用户注册是将新密钥注册到服务器凭证存储并映射到用户的过程，当然在客户端创建了一个私钥并与服务器相关联，但该密钥从未离开硬件令牌或您的手机安全 芯片。

注册用户并重用上面已经定义的大部分功能：

```javascript
/* Handle for register form submission */
getMakeCredentialsChallenge({name: 'myalias', displayName: 'Paulo Lopes'})
.then((response) => {
 // convert challenge & id to buffer and perform register
 let publicKey = preformatMakeCredReq(response);
 // create a new secure key pair
 return navigator.credentials.create({publicKey})
})
.then((response) => {
 // convert response from buffer to json
 let makeCredResponse = window.publicKeyCredentialToJSON(response);
 // send to server to confirm the user
 return sendWebAuthnResponse(makeCredResponse)
})
.then((response) => {
 alert('Registration completed')
})
.catch((error) => alert(error));

// utility functions

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

> **☢警告:** 由于 API 浏览器的安全特性，您将不允许您在纯文本 HTTP 上使用此 API。 所有请求都必须通过 HTTPS。

> **☢警告:** WebAuthN 需要带有有效 TLS 证书的 HTTPS，您也可以在开发过程中使用自签名证书。

### 一次性密码（多重身份验证）

Vert.x 还支持多因素身份验证。 使用 MFA 有两种选择：

- `HOTP` - 基于哈希的一次性密码
- `TOTP` - 基于时间的一次性密码

提供者之间的用法相同，因此存在一个处理程序，允许您在构造函数级别选择所需的模式。

这个处理程序的行为可以看作是：

如果当前请求中没有`User`，则假定之前没有执行过身份验证。 这意味着请求将立即以状态码 401 终止。

如果存在用户并且对象缺少具有匹配类型（`hotp`/`totp`）的属性`mfa`，则请求将被重定向到验证url（如果提供），否则将被终止。 这样的 url 应该提供一种输入代码的方法，例如：

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

用户输入有效代码后，请求将重定向到初始 url，如果不知道原始 url，则重定向到`/`。

当然，此流程假定已配置身份验证器应用程序或设备。 为了配置一个新的应用程序/设备，一个示例 HTML 页面可以是：

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

此示例中的重要一点是脚本向配置的注册回调发出`POST`请求。 如果请求中没有用户已通过身份验证，此回调将再次返回状态代码 401。 成功时返回一个带有 url 和一些额外元数据的 JSON 文档。 此 url 应用于配置身份验证器，通过在应用程序中手动输入或通过呈现 QR 码。 二维码的渲染可以在后端或前端完成。 为简单起见，此示例使用 google 图表 API 在浏览器上呈现它。

最后，这是您在 vert.x 应用程序中使用处理程序的方式：

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
  .handler(ctx -> {
    ctx.end("Super secret content");
  });
```

### 处理 HTTP 方法覆盖

许多公司和其他服务对它们允许外部世界使用的 REST HTTP 方法施加了限制。 有些人因为允许任何方法而松懈，大多数人受到限制，只允许一个小而体面的集合，有些人只允许 GET 和 POST。 此类限制的原因各不相同：浏览器或客户端限制或非常严格的公司防火墙。 只有 GET 和 POST 的 Web 服务不能很好地表达 REST 思想。 PUT、DELETE、OPTIONS 等对于指定对资源执行的操作非常有用。 为了解决这个问题，它创建了 `X-HTTP-METHOD-OVERRIDE` HTTP 标头作为解决方法。

通过使用 GET/POST 发送请求以及请求应该在 `X-HTTP-METHOD-OVERRIDE` HTTP 标头中真正处理的方法，服务器应该识别标头并重定向到适当的方法。

Vert.x 允许这样做，只需：

```java
router.route().handler(MethodOverrideHandler.create());

router.route(HttpMethod.GET, "/").handler(ctx -> {
  // do GET stuff...
});

router.route(HttpMethod.POST, "/").handler(ctx -> {
  // do POST stuff...
});
```

由于它将重定向请求，因此避免不必要地触发请求处理程序是明智的，因此最好将 MethodOverrideHandler 添加为第一个处理程序。

另外，请注意：这可能成为不怀好意的人的攻击媒介！

为了缓解这样的问题，MethodOverrideHandler 默认带有一个安全降级策略。 该政策规定 `X-HTTP-METHOD-OVERRIDE` 中包含的方法可以在以下情况下覆盖原始方法：

- 覆盖方法是幂等的； 或者
- 覆盖方法是安全的，并且要覆盖的方法不是幂等的； 或者
- 被覆盖的方法是不安全的。

虽然我们不推荐，但 Vert.x 不会强迫您做任何事情。 如果您希望允许任何覆盖，那么：

```java
router.route().handler(MethodOverrideHandler.create(false));

router.route(HttpMethod.GET, "/").handler(ctx -> {
  // do GET stuff...
});

router.route(HttpMethod.POST, "/").handler(ctx -> {
  // do POST stuff...
});
```
