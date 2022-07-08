# Vert.x Service Proxy Manual中文版

> 翻译: 白石(https://github.com/wjw465150/Vert.x-Core-Manual)

当您编写一个 Vert.x 应用程序时，您可能希望在某个地方隔离一个功能，并使其可供应用程序的其余部分使用。 这是服务代理的主要目的。 它允许您在事件总线上公开一个 *service*，因此，任何其他 Vert.x 组件只要知道发布服务的 *address* 就可以使用它。

*service* 使用包含遵循 *async 模式* 的方法的 Java 接口来描述。 在幕后，消息在事件总线上发送以调用服务并获取响应。 但为了便于使用，它会生成一个*代理*，您可以直接调用（使用服务接口中的 API）。

## 使用 Vert.x 服务代理

要 **使用** Vert.x 服务代理，请将以下依赖项添加到构建描述符的 *dependencies* 部分：

- Maven (在你的 `pom.xml`):

```xml
<dependency>
  <groupId>io.vertx</groupId>
  <artifactId>vertx-service-proxy</artifactId>
  <version>4.3.1</version>
</dependency>
```

- Gradle (在你的 `build.gradle` ):

```groovy
implementation 'io.vertx:vertx-service-proxy:4.3.1'
```

要 **实现** 服务代理，还要添加：

- Maven (在你的 `pom.xml`):

```xml
<dependency>
  <groupId>io.vertx</groupId>
  <artifactId>vertx-codegen</artifactId>
  <version>4.3.1</version>
  <scope>provided</scope>
</dependency>
```

- Gradle < 5 (在你的 `build.gradle` file):

```groovy
compileOnly 'io.vertx:vertx-codegen:4.3.1'
```

- Gradle >= 5 (在你的 `build.gradle` file):

```groovy
implementation 'io.vertx:vertx-codegen:4.3.1'
implementation 'io.vertx:vertx-service-proxy:4.3.1'

annotationProcessor 'io.vertx:vertx-codegen:4.3.1:processor'
annotationProcessor 'io.vertx:vertx-service-proxy:4.3.1'
```

>  **🏷注意:** 请注意，由于服务代理机制依赖于代码生成，因此对*服务接口*的修改需要重新编译源代码以重新生成代码。

要生成不同语言的代理，您需要为 Groovy 添加 *language* 依赖项，例如 `vertx-lang-groovy`。

## 服务代理简介

让我们看一下服务代理以及它们为何有用。 假设您在事件总线上公开了一个*数据库服务*，您应该执行以下操作：

```java
JsonObject message = new JsonObject();

message
  .put("collection", "mycollection")
  .put("document", new JsonObject().put("name", "tim"));

DeliveryOptions options = new DeliveryOptions().addHeader("action", "save");

vertx.eventBus()
  .request("database-service-address", message, options)
  .onSuccess(msg -> {
    // done
  }).onFailure(err -> {
  // failure
});
```

创建服务时，有一定数量的样板代码用于在事件总线上侦听传入消息，将它们路由到适当的方法并在事件总线上返回结果。

使用 Vert.x 服务代理，您可以避免编写所有样板代码并专注于编写服务。

您将服务编写为 Java 接口并使用 `@ProxyGen` 对其进行注解，例如：

```java
@ProxyGen
public interface SomeDatabaseService {

  // 几个工厂方法来创建实例和代理
  static SomeDatabaseService create(Vertx vertx) {
    return new SomeDatabaseServiceImpl(vertx);
  }

  static SomeDatabaseService createProxy(Vertx vertx, String address) {
    return new SomeDatabaseServiceVertxEBProxy(vertx, address);
  }

  // 实际服务操作在这里...
  void save(String collection, JsonObject document, Handler<AsyncResult<Void>> resultHandler);
}
```

您还需要在定义接口的包中（或上面）的某个位置有一个`package-info.java`文件。该包需要用`@ModuleGen`注释，以便 Vert.x CodeGen 可以识别您的接口并生成 适当的 EventBus 代理代码。

`package-info.java`文件内容

```java
@io.vertx.codegen.annotations.ModuleGen(groupPackage = "io.vertx.example", name = "services", useFutures = true)
package io.vertx.example;
```

有了这个接口，Vert.x将生成通过事件总线访问您的服务所需的所有样板代码，它还将为您的服务生成一个**客户端代理**，因此您的客户端可以为您的服务使用一个丰富的惯用API，而不必手动编写事件总线消息来发送。无论您的服务在事件总线的哪个位置(可能在另一台机器上)，客户端代理都可以工作。

这意味着您可以像这样与您的服务进行交互：

```java
SomeDatabaseService service = SomeDatabaseService.createProxy(vertx, "database-service-address");

// Save some data in the database - this time using the proxy
service.save(
  "mycollection",
  new JsonObject().put("name", "tim"),
  res2 -> {
    if (res2.succeeded()) {
      // done
    }
  });
```

你也可以将`@ProxyGen` 和语言API代码生成(`@VertxGen`)结合起来，以Vert.x支持的任何语言来创建服务存根——这意味着你可以只在Java中编写一次服务，然后通过一个习惯的其他语言API与它交互，而不管服务是在本地还是完全在事件总线的其他地方。为此，不要忘记在构建描述符中添加对其它语言的依赖:

```java
@ProxyGen // 生成服务代理
@VertxGen // 生成客户端
public interface SomeDatabaseService {
 // ...
}
```

> **💡提示:** 当`@VertxGen`注解存在时，Vert.x Java 注解处理器的代码生成将在构建时启用所有合适的其它语言绑定的代码生成器。要生成 其它语言的 绑定，我们需要添加对其它语言的依赖项。

## 异步接口

要由服务代理生成使用，*服务接口*必须遵守一些规则。 首先它应该遵循异步模式。 要返回结果，该方法应声明一个 `Future<ResultType>` 返回类型。 `ResultType` 可以是另一个代理（因此代理可以是其他代理的工厂）。

让我们看一个例子：

```java
@ProxyGen
public interface SomeDatabaseService {

 // 几个工厂方法来创建实例和代理
  static SomeDatabaseService create(Vertx vertx) {
    return new SomeDatabaseServiceImpl(vertx);
  }

  static SomeDatabaseService createProxy(Vertx vertx, String address) {
    return new SomeDatabaseServiceVertxEBProxy(vertx, address);
  }

  // 通知完成但没有结果的方法（void）
  Future<Void> save(String collection, JsonObject document);

  // 提供结果的方法（一个 json 对象）
  Future<JsonObject> findOne(String collection, JsonObject query);

  // 创建连接
  Future<MyDatabaseConnection> createConnection(String shoeSize);

}
```

和:

```java
@ProxyGen
@VertxGen
public interface MyDatabaseConnection {

  void insert(JsonObject someData);

  Future<Void> commit();

  @ProxyClose
  void close();
}
```

您还可以通过使用 `@ProxyClose` 注解来声明特定方法取消注册代理。 调用此方法时会释放代理实例。

下面描述了对*服务接口*的更多限制。

## 带有回调的异步接口

在 Vert.x 4.1 之前，服务异步接口是由回调定义的。

您仍然可以使用回调创建服务异步接口，使用此模块声明：

`package-info.java`文件内容

```java
@io.vertx.codegen.annotations.ModuleGen(groupPackage = "io.vertx.example", name = "services", useFutures = false)
package io.vertx.example;
```

> **🏷注意:** 为了向后兼容，`useFutures` 的默认值为 `false`，所以你也可以省略声明

带有回调的服务异步接口如下所示：

```java
@ProxyGen
public interface SomeDatabaseService {

  // 通知完成但没有结果的方法（void）
  void save(String collection, JsonObject document, Handler<AsyncResult<Void>> result);

  // 提供结果的方法（一个 json 对象）
  void findOne(String collection, JsonObject query, Handler<AsyncResult<JsonObject>> result);

  // 创建连接
  void createConnection(String shoeSize, Handler<AsyncResult<MyDatabaseConnection>> resultHandler);

}
```

返回类型必须是以下之一：

- `void`
- `@Fluent` 并返回对服务的引用（`this`）：

```java
@Fluent
SomeDatabaseService doSomething();
```

这是因为方法不能阻塞，如果服务是远程的，不可能立即返回结果而不阻塞。

## 安全

服务代理可以使用简单的拦截器执行基本的安全性。 必须提供身份验证提供程序，可以选择添加`Authorization`，在这种情况下，还必须存在`AuthorizationProvider`。 请注意，身份验证基于从 `auth-token` 标头中提取的令牌。

```java
SomeDatabaseService service = new SomeDatabaseServiceImpl();
// 注册处理程序
new ServiceBinder(vertx)
  .setAddress("database-service-address")
  // 保护传输中的消息
  .addInterceptor(
    new ServiceAuthInterceptor()
      // 令牌将使用 JWT 身份验证进行验证
      .setAuthenticationProvider(JWTAuth.create(vertx, new JWTAuthOptions()))
      // 可选地，我们也可以保护权限：

      // 一个 admin
      .addAuthorization(RoleBasedAuthorization.create("admin"))
      // 可以打印的
      .addAuthorization(PermissionBasedAuthorization.create("print"))

      // 授权被加载的地方，让我们从令牌中假设
      // 但如果需要，它们可以从数据库或文件中加载
      .setAuthorizationProvider(
        JWTAuthorization.create("permissions")))

  .register(SomeDatabaseService.class, service);
```

## 代码生成

带有`@ProxyGen`注解的服务会触发服务助手类的生成:

- 服务代理：编译时生成的代理，它使用 `EventBus` 通过消息与服务进行交互
- 服务处理程序：编译时生成的`EventBus`处理程序，它对代理发送的事件做出反应

生成的代理和处理程序以服务类命名，例如，如果服务名为`MyService`，则处理程序称为`MyServiceVertxProxyHandler`，代理称为`MyServiceVertxEBProxy`。

此外，Vert.x Core 提供了一个生成器，用于创建数据对象转换器，以简化服务代理中数据对象的使用。 这种转换器为在服务代理中使用数据对象所必需的`JsonObject`构造函数和`toJson()`方法提供了基础。

*codegen* 注解处理器在编译时生成这些类。 它是 Java 编译器的一项功能，因此*不需要额外的步骤*，只需正确配置您的构建：

只需将 `io.vertx:vertx-codegen:processor` 和 `io.vertx:vertx-service-proxy` 依赖项添加到您的构建中。

这里是 Maven 的配置示例：

```xml
<dependency>
  <groupId>io.vertx</groupId>
  <artifactId>vertx-codegen</artifactId>
  <version>4.3.1</version>
  <classifier>processor</classifier>
</dependency>
<dependency>
  <groupId>io.vertx</groupId>
  <artifactId>vertx-service-proxy</artifactId>
  <version>4.3.1</version>
</dependency>
```

这个特性也可以在 Gradle 中使用：

```groovy
implementation 'io.vertx:vertx-codegen:4.3.1'
implementation 'io.vertx:vertx-service-proxy:4.3.1'

annotationProcessor 'io.vertx:vertx-codegen:4.3.1:processor'
annotationProcessor 'io.vertx:vertx-service-proxy:4.3.1'
```

IDE 也通常为注释处理器提供支持。

codegen `processor` 分类器通过 `META-INF/services` 插件机制将服务代理注解处理器的自动配置添加到 jar 中。

如果您愿意，您也可以将它与常规 jar 一起使用，但您需要显式声明注解处理器，例如在 Maven 中：

```xml
<plugin>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <annotationProcessors>
      <annotationProcessor>io.vertx.codegen.CodeGenProcessor</annotationProcessor>
    </annotationProcessors>
  </configuration>
</plugin>
```

## 公开您的服务

一旦你有了你的*服务接口*，编译源代码来生成存根和代理。然后，你需要一些代码在事件总线上“注册”你的服务:

```java
SomeDatabaseService service = new SomeDatabaseServiceImpl();
// Register the handler
new ServiceBinder(vertx)
  .setAddress("database-service-address")
  .register(SomeDatabaseService.class, service);
```
> **💡提示:** 译者注: 为了提高处理速度,可以在同一个地址上重复注册异步服务.其实内部就是在相同的EvenBus地址上添加了新的consumer!

这可以在verticle里完成，也可以在代码中的任何地方完成。

一旦注册，服务就可以访问。如果您在集群上运行应用程序，那么任何主机都可以提供该服务。

要撤销您的服务，请使用 `unregister` 方法：

```java
ServiceBinder binder = new ServiceBinder(vertx);

// 创建服务实现的实例
SomeDatabaseService service = new SomeDatabaseServiceImpl();
// Register the handler
MessageConsumer<JsonObject> consumer = binder
  .setAddress("database-service-address")
  .register(SomeDatabaseService.class, service);

// ....

// 取消注册您的服务。
binder.unregister(consumer);
```

## 代理创建

现在服务已公开，您可能想要使用它。 为此，您需要创建一个代理。 可以使用 `ServiceProxyBuilder` 类创建代理：

```java
ServiceProxyBuilder builder = new ServiceProxyBuilder(vertx)
  .setAddress("database-service-address");

SomeDatabaseService service = builder.build(SomeDatabaseService.class);
// 或有 delivery 选项：
SomeDatabaseService service2 = builder.setOptions(options)
  .build(SomeDatabaseService.class);
```

第二种方法采用 `DeliveryOptions` 的实例，您可以在其中配置消息传递（例如超时）。

或者，您可以使用生成的代理类。 代理类名是 *service interface* 类名，后跟 `VertxEBProxy`。 例如，如果您的 *service interface* 命名为 `SomeDatabaseService`，则代理类命名为 `SomeDatabaseServiceVertxEBProxy`。

通常，*service interface* 包含一个`createProxy` 静态方法来创建代理。 

```java
@ProxyGen
public interface SomeDatabaseService {

// 创建代理的方法。
static SomeDatabaseService createProxy(Vertx vertx, String address) {
  return new SomeDatabaseServiceVertxEBProxy(vertx, address);
}

// ...
}
```

## 错误处理

服务方法可能会通过将包含 `ServiceException` 实例的失败 `Future` 传递给方法的 `Handler` 来向客户端返回错误。 `ServiceException` 包含一个 `int` 失败代码、一条消息和一个可选的 `JsonObject`，其中包含任何被认为对返回调用者很重要的额外信息。 为方便起见，`ServiceException.fail` 工厂方法可用于创建已包装在失败的`Future` 中的`ServiceException` 实例。 例如：

```java
public class SomeDatabaseServiceImpl implements SomeDatabaseService {

  private static final BAD_SHOE_SIZE = 42;
  private static final CONNECTION_FAILED = 43;

  // Create a connection
  public Future<MyDatabaseConnection> createConnection(String shoeSize) {
    if (!shoeSize.equals("9")) {
      return Future.failedFuture(ServiceException.fail(BAD_SHOE_SIZE, "The shoe size must be 9!",
        new JsonObject().put("shoeSize", shoeSize)));
     } else {
        return doDbConnection().recover(err -> Future.failedFuture(ServiceException.fail(CONNECTION_FAILED,  result.cause().getMessage())));
     }
  }
}
```

然后，客户端可以检查它从失败的 `Future` 接收到的 `Throwable` 是否是 `ServiceException`，如果是，请检查内部的特定错误代码。 它可以使用此信息来区分业务逻辑错误和系统错误（例如未向事件总线注册的服务），并准确确定发生了哪个业务逻辑错误。

```java
public Future<JsonObject> foo(String shoeSize) {
  SomeDatabaseService service = SomeDatabaseService.createProxy(vertx, SERVICE_ADDRESS);
  server.createConnection("8")
    .compose(connection -> {
      // 做成功的事。
      return doSuccessStuff(connection);
    })
    .recover(err -> {
      if (err instanceof ServiceException) {
        ServiceException exc = (ServiceException) err;
        if (exc.failureCode() == SomeDatabaseServiceImpl.BAD_SHOE_SIZE) {
          return Future.failedFuture(
            new InvalidInputError("You provided a bad shoe size: " +
              exc.getDebugInfo().getString("shoeSize")));
        } else if (exc.failureCode() == SomeDatabaseServiceImpl.CONNECTION) {
          return Future.failedFuture(new ConnectionError("Failed to connect to the DB"));
        }
      } else {
        // 必须是系统错误（例如，没有为代理注册服务）
        return Future.failedFuture(new SystemError("An unexpected error occurred: + " result.cause().getMessage()));
      }
  });
}
```

如果需要，服务实现也可以返回 `ServiceException` 的子类，只要为其注册了默认的 `MessageCodec`。 例如，给定以下 `ServiceException` 子类：

```java
class ShoeSizeException extends ServiceException {
  public static final BAD_SHOE_SIZE_ERROR = 42;

  private final String shoeSize;

  public ShoeSizeException(String shoeSize) {
    super(BAD_SHOE_SIZE_ERROR, "In invalid shoe size was received: " + shoeSize);
    this.shoeSize = shoeSize;
  }

  public String getShoeSize() {
    return extra;
  }

  public static <T> Future<T> fail(int failureCode, String message, String shoeSize) {
    return Future.failedFuture(new MyServiceException(failureCode, message, shoeSize));
  }
}
```

只要注册了一个默认的 `MessageCodec`，Service 实现就可以直接将自定义异常返回给调用者：

```java
public class SomeDatabaseServiceImpl implements SomeDatabaseService {
  public SomeDataBaseServiceImpl(Vertx vertx) {
    // 注册服务端。 如果使用本地事件总线，这就是所有需要的，因为代理端将共享同一个 Vertx 实例。
    SomeDatabaseService service = SomeDatabaseService.createProxy(vertx, SERVICE_ADDRESS);
    vertx.eventBus().registerDefaultCodec(ShoeSizeException.class, new ShoeSizeExceptionMessageCodec());
  }

  // 创建连接
  Future<MyDatabaseConnection> createConnection(String shoeSize) {
    if (!shoeSize.equals("9")) {
      return ShoeSizeException.fail(shoeSize);
    } else {
      // 在此处创建连接
      return Future.succeededFuture(myDbConnection);
    }
  }
}
```

最后，客户端现在可以检查自定义异常：

```java
public Future<JsonObject> foo(String shoeSize) {
  // 如果此代码在集群中的不同节点上运行，则 ShoeSizeExceptionMessageCodec 也需要在该节点上的 Vertx 实例中注册。
  SomeDatabaseService service = SomeDatabaseService.createProxy(vertx, SERVICE_ADDRESS);
  service.createConnection("8")
    .compose(connection -> {
      // 做成功的事。
      return doSuccessStuff(connection);
    })
    .recover(err -> {
      if (result.cause() instanceof ShoeSizeException) {
        ShoeSizeException exc = (ShoeSizeException) result.cause();
        return Future.failedFuture(
          new InvalidInputError("You provided a bad shoe size: " + exc.getShoeSize()));
      } else {
        // 必须是系统错误（例如，没有为代理注册服务）
        return Future.failedFuture(
          new SystemError("An unexpected error occurred: + " result.cause().getMessage())
        );
      }
  });
}
```

请注意，如果您正在集群 `Vertx` 实例，则需要将自定义异常的 `MessageCodec` 注册到集群中的每个 `Vertx` 实例。

## 服务接口限制

可以在服务方法中使用的类型和返回值有一些限制，因此它们很容易在事件总线消息上编组，因此它们可以异步使用。 他们是：

### 数据类型

让`JSON` = `JsonObject | JsonArray` 让 `PRIMITIVE` = 任何原始类型或包装原始类型

参数可以是以下任何一种：

- `JSON`
- `PRIMITIVE`
- `List<JSON>`
- `List<PRIMITIVE>`
- `Set<JSON>`
- `Set<PRIMITIVE>`
- `Map<String, JSON>`
- `Map<String, PRIMITIVE>`
- 任何 *Enum* 类型
- 任何使用 `@DataObject` 注解的类

异步结果模型化为:

- `Future<R>`
- `Handler<AsyncResult<R>>` 用于回调样式

`R` 可以是以下任何一种：

- `JSON`
- `PRIMITIVE`
- `List<JSON>`
- `List<PRIMITIVE>`
- `Set<JSON>`
- `Set<PRIMITIVE>`
- 任何 *Enum* 类型
- 任何使用 `@DataObject` 注解的类
- 其他代理

### 重载方法

不能有重载的服务方法。 （*即*多个同名，无论签名）。

## 通过事件总线调用服务的约定（无代理）

服务代理假定事件总线消息遵循某种格式，因此可以使用它们来调用服务。

当然，如果您不想这样做，您不必**必须**使用客户端代理来访问远程服务。 仅通过事件总线发送消息来与它们交互是完全可以接受的。

为了使服务以一致的方式进行交互，以下消息格式**必须用于**任何 Vert.x 服务。

格式非常简单：

- 应该有一个名为`action`的标题，它给出了要执行的操作的名称。
- 消息的主体应该是一个`JsonObject`，在对象中应该有一个字段用于操作所需的每个参数。

例如，调用一个名为 `save` 的操作，它需要一个字符串集合和一个 JsonObject 文档：

```
Headers:
   "action": "save"
Body:
   {
       "collection", "mycollection",
       "document", {
           "name": "tim"
       }
   }
```

无论是否使用服务代理来创建服务，都应使用上述约定，因为它允许与服务进行一致的交互。

在使用服务代理的情况下，`action`值应该映射到服务接口中的操作方法的名称，并且正文中的每个 `[key, value]` 应该映射到 `[arg_name, arg_value]` 动作方法。

对于返回值，服务应该使用 `message.reply(...)` 方法来发回一个返回值 - 这可以是事件总线支持的任何类型。 要发出失败信号，应该使用方法 `message.fail(...)`。

如果您使用服务代理，生成的代码将自动为您处理。
