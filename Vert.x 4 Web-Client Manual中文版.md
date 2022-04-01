# Vert.x 4 Web-Client Manual中文版

Vert.x Web Client 是一个异步 HTTP 和 HTTP/2 客户端。

Web 客户端可以轻松地与 Web 服务器进行 HTTP 请求/响应交互，并提供高级功能，例如：

- Json body 编码/解码
- 请求/响应泵送
- 请求参数
- 统一的错误处理
- 表单提交

Web 客户端并没有弃用 Vert.x Core `HttpClient`，实际上它是基于这个客户端并继承了它的配置和强大的功能，如池、HTTP/2 支持、管道支持等……在以下情况下应该使用`HttpClient` 对 HTTP 请求/响应的细粒度控制是必要的。

Web 客户端不提供 WebSocket API，应使用 Vert.x Core `HttpClient`。 它目前也不处理 cookie。

## 使用 Web Client

要使用 Vert.x Web 客户端，请将以下依赖项添加到构建描述符的 *dependencies* 部分：

- Maven (在你的 `pom.xml`):

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-web-client</artifactId>
 <version>4.2.5</version>
</dependency>
```

- Gradle (i在你的 `build.gradle` ):

```groovy
dependencies {
 compile 'io.vertx:vertx-web-client:4.2.5'
}
```

## 重温 Vert.x Core HTTP 客户端

Vert.x Web 客户端使用来自 Vert.x 核心的 API，因此如果您还没有使用 Vert.x 核心，那么熟悉使用 `HttpClient` 的基本概念是非常值得的。

## 创建一个 Web Client

您使用默认选项创建一个 `WebClient` 实例，如下所示

```java
WebClient client = WebClient.create(vertx);
```

如果要为客户端配置选项，请按如下方式创建它

```java
WebClientOptions options = new WebClientOptions()
  .setUserAgent("My-App/1.2.3");
options.setKeepAlive(false);
WebClient client = WebClient.create(vertx, options);
```

Web 客户端选项继承 Http 客户端选项，因此您可以设置其中任何一个。

如果您的应用程序中已经有一个 HTTP 客户端，您也可以重用它

```java
WebClient client = WebClient.wrap(httpClient);
```

> **⚠重要:** 在大多数情况下，应在应用程序启动时创建一次 Web 客户端，然后重用。 否则你会失去很多好处，比如连接池，如果实例没有正确关闭，可能会泄漏资源。

## 发出请求

### 没有正文的简单请求

通常，您会希望发出没有请求正文的 HTTP 请求。 这通常是 HTTP GET、OPTIONS 和 HEAD 请求的情况

```java
WebClient client = WebClient.create(vertx);

// Send a GET request
client
  .get(8080, "myserver.mycompany.com", "/some-uri")
  .send()
  .onSuccess(response -> System.out
    .println("Received response with status code" + response.statusCode()))
  .onFailure(err ->
    System.out.println("Something went wrong " + err.getMessage()));

// Send a HEAD request
client
  .head(8080, "myserver.mycompany.com", "/some-uri")
  .send()
  .onSuccess(response -> System.out
    .println("Received response with status code" + response.statusCode()))
  .onFailure(err ->
    System.out.println("Something went wrong " + err.getMessage()));
```

您可以以流畅的方式将查询参数添加到请求 URI

```java
client
  .get(8080, "myserver.mycompany.com", "/some-uri")
  .addQueryParam("param", "param_value")
  .send()
  .onSuccess(response -> System.out
    .println("Received response with status code" + response.statusCode()))
  .onFailure(err ->
    System.out.println("Something went wrong " + err.getMessage()));
```

任何请求 URI 参数都将预先填充请求

```java
HttpRequest<Buffer> request = client
  .get(
    8080,
    "myserver.mycompany.com",
    "/some-uri?param1=param1_value&param2=param2_value");

// Add param3
request.addQueryParam("param3", "param3_value");

// Overwrite param2
request.setQueryParam("param2", "another_param2_value");
```

设置请求 URI 会丢弃现有的查询参数

```java
HttpRequest<Buffer> request = client
  .get(8080, "myserver.mycompany.com", "/some-uri");

// Add param1
request.addQueryParam("param1", "param1_value");

// Overwrite param1 and add param2
request.uri("/some-uri?param1=param1_value&param2=param2_value");
```

### 编写请求体

当您需要使用正文发出请求时，您使用相同的 API，然后调用期望正文发送的 `sendXXX` 方法。

使用 `sendBuffer` 发送缓冲区主体

```java
client
  .post(8080, "myserver.mycompany.com", "/some-uri")
  .sendBuffer(buffer)
  .onSuccess(res -> {
    // OK
  });
```

发送单个缓冲区很有用，但通常您不想完全加载内存中的内容，因为它可能太大，或者您想处理许多并发请求并且只想为每个请求使用最小值。 为此，Web 客户端可以使用 `sendStream` 方法发送 `ReadStream<Buffer>`（例如，`AsyncFile` 是 ReadStream<Buffer>`）

```java
client
  .post(8080, "myserver.mycompany.com", "/some-uri")
  .sendStream(stream)
  .onSuccess(res -> {
    // OK
  });
```

Web 客户端负责为您设置传输泵。 由于不知道流的长度，请求将使用分块传输编码。

当您知道流的大小时，应在提前设定 `content-length` 标头

```java
fs.open("content.txt", new OpenOptions(), fileRes -> {
  if (fileRes.succeeded()) {
    ReadStream<Buffer> fileStream = fileRes.result();

    String fileLen = "1024";

    // Send the file to the server using POST
    client
      .post(8080, "myserver.mycompany.com", "/some-uri")
      .putHeader("content-length", fileLen)
      .sendStream(fileStream)
      .onSuccess(res -> {
        // OK
      })
    ;
  }
});
```

这样POST就不会被分块。

#### Json 包体

通常你会想要发送 Json 正文请求，发送一个 `JsonObject` 使用 `sendJsonObject`

```java
client
  .post(8080, "myserver.mycompany.com", "/some-uri")
  .sendJsonObject(
    new JsonObject()
      .put("firstName", "Dale")
      .put("lastName", "Cooper"))
  .onSuccess(res -> {
    // OK
  });
```

在 Java、Groovy 或 Kotlin 中，您可以使用 `sendJson` 方法，该方法使用`Json.encode` 方法将 POJO（普通旧 Java 对象）映射到 Json 对象

```java
client
  .post(8080, "myserver.mycompany.com", "/some-uri")
  .sendJson(new User("Dale", "Cooper"))
  .onSuccess(res -> {
    // OK
  });
```

> **🏷注意:** `Json.encode` 使用 Jackson 映射器将对象编码为 Json。

#### 表单提交

您可以使用 `sendForm` 变体发送 http 表单提交正文。

```java
MultiMap form = MultiMap.caseInsensitiveMultiMap();
form.set("firstName", "Dale");
form.set("lastName", "Cooper");

// Submit the form as a form URL encoded body
client
  .post(8080, "myserver.mycompany.com", "/some-uri")
  .sendForm(form)
  .onSuccess(res -> {
    // OK
  });
```

默认情况下，表单使用 `application/x-www-form-urlencoded` 内容类型标头提交。 您可以将 `content-type` 标头设置为 `multipart/form-data`

```java
MultiMap form = MultiMap.caseInsensitiveMultiMap();
form.set("firstName", "Dale");
form.set("lastName", "Cooper");

// Submit the form as a multipart form body
client
  .post(8080, "myserver.mycompany.com", "/some-uri")
  .putHeader("content-type", "multipart/form-data")
  .sendForm(form)
  .onSuccess(res -> {
    // OK
  });
```

如果要上传文件和发送属性，可以创建一个`MultipartForm`并使用`sendMultipartForm`。

```java
MultipartForm form = MultipartForm.create()
  .attribute("imageDescription", "a very nice image")
  .binaryFileUpload(
    "imageFile",
    "image.jpg",
    "/path/to/image",
    "image/jpeg");

// Submit the form as a multipart form body
client
  .post(8080, "myserver.mycompany.com", "/some-uri")
  .sendMultipartForm(form)
  .onSuccess(res -> {
    // OK
  });
```

### 编写请求标头

您可以使用 headers multi-map 将标头写入请求，如下所示：

```java
HttpRequest<Buffer> request = client
  .get(8080, "myserver.mycompany.com", "/some-uri");

MultiMap headers = request.headers();
headers.set("content-type", "application/json");
headers.set("other-header", "foo");
```

标头是`MultiMap`的一个实例，它提供了添加、设置和删除条目的操作。 Http 标头允许特定键有多个值。

您还可以使用 putHeader 编写标头

```java
HttpRequest<Buffer> request = client
  .get(8080, "myserver.mycompany.com", "/some-uri");

request.putHeader("content-type", "application/json");
request.putHeader("other-header", "foo");
```

### 配置请求以添加身份验证。

可以通过设置正确的标头手动执行身份验证，或者使用我们预定义的方法（我们强烈建议启用 HTTPS，特别是对于经过身份验证的请求）：

在基本 HTTP 身份验证中，请求包含格式为`Authorization: Basic <credentials>`的标头字段，其中凭据是由冒号连接的 id 和密码的 base64 编码。

您可以配置请求以添加基本访问身份验证，如下所示：

```java
HttpRequest<Buffer> request = client
  .get(8080, "myserver.mycompany.com", "/some-uri")
  .authentication(new UsernamePasswordCredentials("myid", "mypassword"));
```

在 OAuth 2.0 中，请求包含格式为`Authorization: Bearer <bearerToken>`的标头字段，其中 BearerToken 是授权服务器发布的用于访问受保护资源的不记名令牌。

您可以配置请求以添加承载令牌身份验证，如下所示：

```java
HttpRequest<Buffer> request = client
  .get(8080, "myserver.mycompany.com", "/some-uri")
  .authentication(new TokenCredentials("myBearerToken"));
```

### 重用Request

`send` 方法可以安全地多次调用，使得配置和重用 `HttpRequest` 对象变得非常容易

```java
HttpRequest<Buffer> get = client
  .get(8080, "myserver.mycompany.com", "/some-uri");

get
  .send()
  .onSuccess(res -> {
    // OK
  });

// Same request again
get
  .send()
  .onSuccess(res -> {
    // OK
  });
```

不过要注意`HttpRequest`实例是可变的。因此，你应该在修改缓存实例之前调用`copy`方法。

```java
HttpRequest<Buffer> get = client
  .get(8080, "myserver.mycompany.com", "/some-uri");

get
  .send()
  .onSuccess(res -> {
    // OK
  });

// The "get" request instance remains unmodified
get
  .copy()
  .putHeader("a-header", "with-some-value")
  .send()
  .onSuccess(res -> {
    // OK
  });
```

### 超时

您可以使用 `timeout` 为特定的 http 请求设置超时。

```java
client
  .get(8080, "myserver.mycompany.com", "/some-uri")
  .timeout(5000)
  .send()
  .onSuccess(res -> {
    // OK
  })
  .onFailure(err -> {
    // Might be a timeout when cause is java.util.concurrent.TimeoutException
  });
```

如果请求在超时期限内没有返回任何数据，则会将异常传递给响应处理程序。

## 处理 http 响应

当 Web 客户端发送请求时，您总是处理单个异步结果 `HttpResponse`。

如果结果成功，则在接收到响应后发生回调

```java
client
  .get(8080, "myserver.mycompany.com", "/some-uri")
  .send()
  .onSuccess(res ->
    System.out.println("Received response with status code" + res.statusCode()))
  .onFailure(err ->
    System.out.println("Something went wrong " + err.getMessage()));
```

> **⚠小心:** 默认情况下，仅当网络级别发生错误时，Vert.x Web 客户端请求才会以错误结束。 换句话说，`404 Not Found`响应，或内容类型错误的响应，**不**被视为失败。 如果您希望 Web 客户端自动执行健全性检查，请使用 [response predicates](#response-predicates)。

> **☢警告:** 响应是完全buffered，使用 `BodyCodec.pipe` 将响应通过管道传输到写入流

### 解码响应

默认情况下，Web 客户端提供一个 http 响应体作为 `Buffer` 并且不应用任何解码。

可以使用 `BodyCodec` 实现自定义响应正文解码：

- 纯字符串
- Json 对象
- Json 映射 POJO
- `WriteStream`

body 编解码器可以将任意二进制数据流解码为特定对象实例，从而节省响应处理程序中的解码步骤。

使用 `BodyCodec.jsonObject` 来解码一个 Json 对象：

```java
client
  .get(8080, "myserver.mycompany.com", "/some-uri")
  .as(BodyCodec.jsonObject())
  .send()
  .onSuccess(res -> {
    JsonObject body = res.body();

    System.out.println(
      "Received response with status code" +
        res.statusCode() +
        " with body " +
        body);
  })
  .onFailure(err ->
    System.out.println("Something went wrong " + err.getMessage()));
```

在Java、Groovy或Kotlin中，定制的Json映射POJO可以被解码

```java
client
  .get(8080, "myserver.mycompany.com", "/some-uri")
  .as(BodyCodec.json(User.class))
  .send()
  .onSuccess(res -> {
    User user = res.body();

    System.out.println(
      "Received response with status code" +
        res.statusCode() +
        " with body " +
        user.getFirstName() +
        " " +
        user.getLastName());
  })
  .onFailure(err ->
    System.out.println("Something went wrong " + err.getMessage()));
```

当预期响应较大时，请使用`BodyCodec.pipe`。 此主体编解码器将响应主体缓冲区泵入`WriteStream`，并在异步结果响应中指示操作成功或失败

```java
client
  .get(8080, "myserver.mycompany.com", "/some-uri")
  .as(BodyCodec.pipe(writeStream))
  .send()
  .onSuccess(res ->
    System.out.println("Received response with status code" + res.statusCode()))
  .onFailure(err ->
    System.out.println("Something went wrong " + err.getMessage()));
```

经常看到 API 返回 JSON 对象流。 例如，Twitter API 可以提供推文提要。 要处理此用例，您可以使用 `BodyCodec.jsonStream`。 您传递一个 JSON 解析器，该解析器从 HTTP 响应发出读取的 JSON 流：

```java
JsonParser parser = JsonParser.newParser().objectValueMode();
parser.handler(event -> {
  JsonObject object = event.objectValue();
  System.out.println("Got " + object.encode());
});
client
  .get(8080, "myserver.mycompany.com", "/some-uri")
  .as(BodyCodec.jsonStream(parser))
  .send()
  .onSuccess(res ->
    System.out.println("Received response with status code" + res.statusCode()))
  .onFailure(err ->
    System.out.println("Something went wrong " + err.getMessage()));
```

最后，如果您对响应内容完全不感兴趣，`BodyCodec.none` 会简单地丢弃整个响应正文

```java
client
  .get(8080, "myserver.mycompany.com", "/some-uri")
  .as(BodyCodec.none())
  .send()
  .onSuccess(res ->
    System.out.println("Received response with status code" + res.statusCode()))
  .onFailure(err ->
    System.out.println("Something went wrong " + err.getMessage()));
```

当你事先不知道 http 响应的内容类型时，仍然可以使用 `bodyAsXXX()` 方法将响应解码为特定类型

```java
client
  .get(8080, "myserver.mycompany.com", "/some-uri")
  .send()
  .onSuccess(res -> {
    // Decode the body as a json object
    JsonObject body = res.bodyAsJsonObject();

    System.out.println(
      "Received response with status code" +
        res.statusCode() +
        " with body " +
        body);
  })
  .onFailure(err ->
    System.out.println("Something went wrong " + err.getMessage()));
```

> **☢警告:** 这仅对解码为缓冲区的响应有效。

<a name="response-predicates"></a>
### 响应谓词

默认情况下，仅当网络级别发生错误时，Vert.x Web 客户端请求才会以错误结束。

换句话说，您必须在收到响应后手动执行完整性检查：

```java
client
  .get(8080, "myserver.mycompany.com", "/some-uri")
  .send()
  .onSuccess(res -> {
    if (
      res.statusCode() == 200 &&
        res.getHeader("content-type").equals("application/json")) {
      // Decode the body as a json object
      JsonObject body = res.bodyAsJsonObject();

      System.out.println(
        "Received response with status code" +
          res.statusCode() +
          " with body " +
          body);
    } else {
      System.out.println("Something went wrong " + res.statusCode());
    }
  })
  .onFailure(err ->
    System.out.println("Something went wrong " + err.getMessage()));
```

您可以使用*响应谓词*来交换灵活性以获得清晰和简洁。

当响应与条件不匹配时，“响应谓词”可能会使请求失败。

Web 客户端带有一组现成可用的谓词：

```java
client
  .get(8080, "myserver.mycompany.com", "/some-uri")
  .expect(ResponsePredicate.SC_SUCCESS)
  .expect(ResponsePredicate.JSON)
  .send()
  .onSuccess(res -> {
    // Safely decode the body as a json object
    JsonObject body = res.bodyAsJsonObject();
    System.out.println(
      "Received response with status code" +
        res.statusCode() +
        " with body " +
        body);
  })
  .onFailure(err ->
    System.out.println("Something went wrong " + err.getMessage()));
```

当现有谓词不符合您的需求时，您还可以创建自定义谓词：

```java
Function<HttpResponse<Void>, ResponsePredicateResult> methodsPredicate =
  resp -> {
    String methods = resp.getHeader("Access-Control-Allow-Methods");
    if (methods != null) {
      if (methods.contains("POST")) {
        return ResponsePredicateResult.success();
      }
    }
    return ResponsePredicateResult.failure("Does not work");
  };

// Send pre-flight CORS request
client
  .request(
    HttpMethod.OPTIONS,
    8080,
    "myserver.mycompany.com",
    "/some-uri")
  .putHeader("Origin", "Server-b.com")
  .putHeader("Access-Control-Request-Method", "POST")
  .expect(methodsPredicate)
  .send()
  .onSuccess(res -> {
    // Process the POST request now
  })
  .onFailure(err ->
    System.out.println("Something went wrong " + err.getMessage()));
```

> **💡提示:** 响应谓词在收到响应正文**之前**进行评估。 因此，您无法在谓词测试函数中检查响应正文。

#### 预定义的谓词

为方便起见，Web 客户端为常见用例提供了一些谓词。

对于状态码，例如 `ResponsePredicate.SC_SUCCESS`。为了验证响应是否有一个`2xx`的代码，你也可以创建一个自定义的代码:

```java
client
  .get(8080, "myserver.mycompany.com", "/some-uri")
  .expect(ResponsePredicate.status(200, 202))
  .send()
  .onSuccess(res -> {
    // ....
  });
```

对于内容类型，例如 `ResponsePredicate.JSON` 来验证响应正文是否包含 JSON 数据，您还可以创建一个自定义的：

```java
client
  .get(8080, "myserver.mycompany.com", "/some-uri")
  .expect(ResponsePredicate.contentType("some/content-type"))
  .send()
  .onSuccess(res -> {
    // ....
  });
```

有关预定义谓词的完整列表，请参阅 `ResponsePredicate` 文档。

#### 创建自定义的失败

默认情况下，响应谓词（包括预定义的谓词）使用默认错误转换器，该转换器丢弃正文并传达简单消息。 您可以通过更改错误转换器来自定义异常类：

```java
ResponsePredicate predicate = ResponsePredicate.create(
  ResponsePredicate.SC_SUCCESS,
  result -> new MyCustomException(result.message()));
```

许多 Web API 在错误响应中提供详细信息。 例如，[Marvel API](https://developer.marvel.com/docs) 使用这种 JSON 对象格式：

```javascript
{
 "code": "InvalidCredentials",
 "message": "The passed API key is invalid."
}
```

为避免丢失此信息，可以在调用错误转换器之前等待完全接收到响应正文：

```java
ErrorConverter converter = ErrorConverter.createFullBody(result -> {

  // Invoked after the response body is fully received
  HttpResponse<Buffer> response = result.response();

  if (response
    .getHeader("content-type")
    .equals("application/json")) {

    // Error body is JSON data
    JsonObject body = response.bodyAsJsonObject();

    return new MyCustomException(
      body.getString("code"),
      body.getString("message"));
  }

  // Fallback to defaut message
  return new MyCustomException(result.message());
});

ResponsePredicate predicate = ResponsePredicate
  .create(ResponsePredicate.SC_SUCCESS, converter);
```

> **☢警告:** 在 Java 中创建异常会在捕获堆栈跟踪时产生性能成本，因此您可能希望创建不捕获堆栈跟踪的异常。 默认情况下，使用不捕获堆栈跟踪的异常来报告异常。

### 处理 30x 重定向

默认情况下，客户端遵循重定向，您可以在 `WebClientOptions` 中配置默认行为：

```java
WebClient client = WebClient
  .create(vertx, new WebClientOptions().setFollowRedirects(false));
```

客户端最多会遵循 16 个请求重定向，可以在相同的选项中更改：

```java
WebClient client = WebClient
  .create(vertx, new WebClientOptions().setMaxRedirects(5));
```

> **🏷注意:** 出于安全原因，客户端不会使用不同于 GET 或 HEAD 的方法跟踪请求的重定向

## HTTP 响应缓存

Vert.x Web 提供 HTTP 响应缓存工具； 要使用它，您需要创建一个`CachingWebClient`。

### 创建缓存 Web 客户端

```java
WebClient client = WebClient.create(vertx);
WebClient cachingWebClient = CachingWebClient.create(client);
```

### 配置缓存的内容

默认情况下，缓存 Web 客户端只会缓存来自状态码为`200`、`301`或`404`的`GET`方法的响应。 此外，默认情况下不会缓存包含`Vary`标头的响应。

这可以通过在客户端创建期间传递`CachingWebClientOptions`来配置。

```java
CachingWebClientOptions options = new CachingWebClientOptions()
  .addCachedMethod(HttpMethod.HEAD)
  .removeCachedStatusCode(301)
  .setEnableVaryCaching(true);

WebClient client = WebClient.create(vertx);
WebClient cachingWebClient = CachingWebClient.create(client, options);
```

在 `Cache-Control` 标头中包含 `private` 指令的响应不会被缓存，除非客户端也是 `WebClientSession`。 请参阅 [处理私有响应](#_handling_private_responses)。

### 提供一个外部存储

存储响应时，默认缓存客户端将使用本地`Map`。您可以提供自己的存储实现来存储响应。 为此，请实现 `CacheStore`，然后您可以在创建客户端时提供它。

```java
WebClient client = WebClient.create(vertx);
CacheStore store = new NoOpCacheStore(); // or any store you like
WebClient cachingWebClient = CachingWebClient.create(client, store);
```

<a name="_handling_private_responses"></a>
### 处理私有响应

要启用私有响应缓存，可以将 `CachingWebClient` 与 `WebClientSession` 结合使用。 完成后，公共响应，即在 `Cache-Control` 标头中带有 `public` 指令的响应，将被缓存在创建客户端的 `CacheStore` 中。 私有响应，那些在 `Cache-Control` 标头中带有 `private` 指令的响应，将与会话一起缓存，以确保缓存的响应不会泄露给其他用户（会话）。

要创建可以缓存私有响应的客户端，请将`CachingWebClient`传递给`WebClientSession`。

```java
WebClient client = WebClient.create(vertx);
WebClient cachingWebClient = CachingWebClient.create(client);
WebClient sessionClient = WebClientSession.create(cachingWebClient);
```

## 使用 HTTPS

Vert.x Web 客户端可以配置为使用 HTTPS，其方式与 Vert.x的 `HttpClient` 完全相同。

您可以指定每个请求的行为

```java
client
  .get(443, "myserver.mycompany.com", "/some-uri")
  .ssl(true)
  .send()
  .onSuccess(res ->
    System.out.println("Received response with status code" + res.statusCode()))
  .onFailure(err ->
    System.out.println("Something went wrong " + err.getMessage()));
```

或者使用带有绝对 URI 参数的 create 方法

```java
client
  .getAbs("https://myserver.mycompany.com:4043/some-uri")
  .send()
  .onSuccess(res ->
    System.out.println("Received response with status code" + res.statusCode()))
  .onFailure(err ->
    System.out.println("Something went wrong " + err.getMessage()));
```

## 会话管理

Vert.x web 提供了一个网络会话管理工具； 要使用它，您需要为每个用户（会话）创建一个 `WebClientSession` 并使用它来代替 `WebClient`。

### 创建一个 WebClientSession

您创建一个 `WebClientSession` 实例如下

```java
WebClient client = WebClient.create(vertx);
WebClientSession session = WebClientSession.create(client);
```

### 发出请求

创建后，可以使用 `WebClientSession` 代替 `WebClient` 来执行 HTTP(s) 请求并自动管理从您正在调用的服务器接收到的任何 cookie。

### 设置会话级别标头

您可以设置要添加到每个请求的任何会话级别标头，如下所示：

```java
WebClientSession session = WebClientSession.create(client);
session.addHeader("my-jwt-token", jwtToken);
```

然后将标头添加到每个请求中； 请注意，这些标头将被发送到所有主机； 如果您需要将不同的标头发送到不同的主机，则必须手动将它们添加到每个请求中，而不是添加到 `WebClientSession`。

## OAuth2 安全性

Vert.x web 提供了一个网络会话管理工具； 要使用它，您为每个用户（会话）创建一个 `OAuth2WebClient` 并使用它而不是 `WebClient`。

### 创建 Oauth2 客户端

您创建一个 `OAuth2WebClient` 实例如下

```java
WebClient client = WebClient.create(vertx);
OAuth2WebClient oauth2 = OAuth2WebClient.create(
    client,
    OAuth2Auth.create(vertx, new OAuth2Options(/* enter IdP config */)))

  // configure the initial credentials (needed to fetch if needed
  // the access_token
  .withCredentials(new TokenCredentials("some.jwt.token"));
```

客户端还可以利用 OpenId 服务发现来完全配置客户端，例如连接到真正的 keycloak 服务器，您可以这样做：

```java
KeycloakAuth.discover(
    vertx,
    new OAuth2Options().setSite("https://keycloakserver.com"))
  .onSuccess(oauth -> {
    OAuth2WebClient client = OAuth2WebClient.create(
        WebClient.create(vertx),
        oauth)
      // if your keycloak is configured for password_credentials_flow
      .withCredentials(
        new UsernamePasswordCredentials("bob", "s3cret"));
  });
```

### 发出请求

创建后，可以使用 `OAuth2WebClient` 代替 `WebClient` 来执行 HTTP(s) 请求并自动管理从您正在调用的服务器接收到的任何 cookie。

### 避免过期的令牌

您可以为每个请求设置令牌过期时间，如下所示:

```java
OAuth2WebClient client = OAuth2WebClient.create(
    baseClient,
    oAuth2Auth,
    new OAuth2WebClientOptions()
      .setLeeway(5));
```

如果要执行请求，则检查当前活动用户对象是否过期，并具有额外的给定余地。 这将允许客户端在需要时执行令牌刷新，而不是因错误而中止操作。

由于过期计算仍将在服务器端执行，因此请求仍可能因令牌过期而失败。 为了减少用户端的工作，可以将客户端配置为对返回状态码**401**（禁止）的请求执行**单**重试。当选项: `refreshTokenOnForbidden` 设置为 `true` 时，客户端将执行一个新的令牌请求，在将响应传递给用户处理程序/承诺之前重试原始请求。

```java
OAuth2WebClient client = OAuth2WebClient.create(
  baseClient,
  oAuth2Auth,
  new OAuth2WebClientOptions()
    // the client will attempt a single token request, if the request
    // if the status code of the response is 401
    // there will be only 1 attempt, so the second consecutive 401
    // will be passed down to your handler/promise
    .setRenewTokenOnForbidden(true));
```

## RxJava 3 API

RxJava `HttpRequest` 提供了原始 API 的 rx-ified 版本，`rxSend` 方法返回一个 `Single<HttpResponse<Buffer>>` 在订阅时发出 HTTP 请求，因此可以订阅 `Single` 多次。

```java
Single<HttpResponse<Buffer>> single = client
  .get(8080, "myserver.mycompany.com", "/some-uri")
  .rxSend();

// Send a request upon subscription of the Single
single.subscribe(response -> System.out.println("Received 1st response with status code" + response.statusCode()), error -> System.out.println("Something went wrong " + error.getMessage()));

// Send another request
single.subscribe(response -> System.out.println("Received 2nd response with status code" + response.statusCode()), error -> System.out.println("Something went wrong " + error.getMessage()));
```

得到的 `Single` 可以用 RxJava API 自然组合和链接

```java
Single<String> url = client
  .get(8080, "myserver.mycompany.com", "/some-uri")
  .rxSend()
  .map(HttpResponse::bodyAsString);

// Use the flatMap operator to make a request on the URL Single
url
  .flatMap(u -> client.getAbs(u).rxSend())
  .subscribe(response -> System.out.println("Received response with status code" + response.statusCode()), error -> System.out.println("Something went wrong " + error.getMessage()));
```

可以使用相同的 API

```java
Single<HttpResponse<JsonObject>> single = client
  .get(8080, "myserver.mycompany.com", "/some-uri")
  .putHeader("some-header", "header-value")
  .addQueryParam("some-param", "param value")
  .as(BodyCodec.jsonObject())
  .rxSend();
single.subscribe(resp -> {
  System.out.println(resp.statusCode());
  System.out.println(resp.body());
});
```

`rxSendStream` 应优先用于发送主体 `Flowable<Buffer>`。

```java
Flowable<Buffer> body = getPayload();

Single<HttpResponse<Buffer>> single = client
  .post(8080, "myserver.mycompany.com", "/some-uri")
  .rxSendStream(body);
single.subscribe(resp -> {
  System.out.println(resp.statusCode());
  System.out.println(resp.body());
});
```

订阅后，将订阅 `body` 并将其内容用于请求。

## 域套接字

从 3.7.1 开始，Web 客户端支持域套接字，例如，您可以与 [本地 Docker 守护进程](https://docs.docker.com/engine/reference/commandline/dockerd/) 交互。

为此，必须使用本机传输创建`Vertx`实例，您可以阅读清楚地解释它的 Vert.x 核心文档。

```java
SocketAddress serverAddress = SocketAddress
  .domainSocketAddress("/var/run/docker.sock");

// We still need to specify host and port so the request
// HTTP header will be localhost:8080
// otherwise it will be a malformed HTTP request
// the actual value does not matter much for this example
client
  .request(
    HttpMethod.GET,
    serverAddress,
    8080,
    "localhost",
    "/images/json")
  .expect(ResponsePredicate.SC_ACCEPTED)
  .as(BodyCodec.jsonObject())
  .send()
  .onSuccess(res ->
    System.out.println("Current Docker images" + res.body()))
  .onFailure(err ->
    System.out.println("Something went wrong " + err.getMessage()));
```
