# Vert.x 4 JUnit 5 integration中文版

该模块为使用 JUnit 5 编写 Vert.x 测试提供集成和支持。

## 在你的构建中使用它

- `groupId`: `io.vertx`
- `artifactId`: `vertx-junit5`
- `version`: (当前 Vert.x 版本或快照)

## 为什么测试异步代码是不同的

测试异步操作需要比 JUnit 等测试工具提供的更多工具。 让我们考虑一个典型的 Vert.x 创建 HTTP 服务器，并将其放入 JUnit 测试中：

```java
@ExtendWith(VertxExtension.class)
class ATest {
  Vertx vertx = Vertx.vertx();

  @Test
  void start_server() {
    vertx.createHttpServer()
      .requestHandler(req -> req.response().end("Ok"))
      .listen(16969, ar -> {
        // (we can check here if the server started or not)
      });
  }
}
```

这里有一些问题，因为 `listen` 在尝试异步启动 HTTP 服务器时不会阻塞。 我们不能简单地假设服务器在 `listen` 调用返回时已正确启动。 还：

1. 传递给`listen`的回调将从 Vert.x 事件循环线程执行，该线程与运行 JUnit 测试的线程不同，并且
2. 在调用 `listen` 之后，测试退出并被认为通过，而 HTTP 服务器甚至可能还没有完成启动，并且
3. 由于 `listen` 回调在与执行测试的线程不同的线程上执行，因此 JUnit 运行程序无法捕获任何异常，例如由失败的断言引发的异常。

## 异步执行的测试上下文

这个模块的第一个贡献是一个 `VertxTestContext` 对象：

1. 允许等待其他线程中的操作以通知完成，并且
2. 支持接收断言失败以将测试标记为失败。

这是一个非常基本的用法：

```java
@ExtendWith(VertxExtension.class)
class BTest {
  Vertx vertx = Vertx.vertx();

  @Test
  void start_http_server() throws Throwable {
    VertxTestContext testContext = new VertxTestContext();

    vertx.createHttpServer()
      .requestHandler(req -> req.response().end())
      .listen(16969)
      .onComplete(testContext.succeedingThenComplete()); //(1)

    assertThat(testContext.awaitCompletion(5, TimeUnit.SECONDS)).isTrue(); //(2)
    if (testContext.failed()) {  //(3)
      throw testContext.causeOfFailure();
    }
  }
}
```

1. `succeedingThenComplete` 返回一个异步结果处理程序，预期会成功，然后使测试上下文通过。
2. `awaitCompletion` 具有`java.util.concurrent.CountDownLatch` 的语义，如果在测试通过之前等待延迟到期，则返回`false`。
3. 如果上下文捕获了一个（可能是异步的）错误，那么在完成后我们必须抛出失败异常以使测试失败。

## 使用任何断言库

该模块不对您应该使用的断言库做出任何假设。 您可以使用普通的 JUnit 断言、[AssertJ](http://joel-costigliola.github.io/assertj/) 等。

要在异步代码中进行断言并确保 `VertxTestContext` 被通知潜在的失败，您需要通过调用 `verify`、`succeeding` 或 `failing` 来包装它们：

```java
HttpClient client = vertx.createHttpClient();

client.request(HttpMethod.GET, 8080, "localhost", "/")
  .compose(req -> req.send().compose(HttpClientResponse::body))
  .onComplete(testContext.succeeding(buffer -> testContext.verify(() -> {
    assertThat(buffer.toString()).isEqualTo("Plop");
    testContext.completeNow();
  })));
```

`VertxTestContext` 中有用的方法如下：

- `completeNow` 和 `failNow` 通知成功或失败
- `succeedingThenComplete` 提供 `Handler<AsyncResult<T>>` 处理程序，期望成功然后完成测试上下文
- `failingThenComplete` 提供 `Handler<AsyncResult<T>>` 处理程序，该处理程序预期失败，然后完成测试上下文
- `succeeding` 提供 `Handler<AsyncResult<T>>` 处理程序，期望成功并将结果传递给另一个回调，回调抛出的任何异常都被视为测试失败
- `failing` 提供预期失败并将异常传递给另一个回调的 `Handler<AsyncResult<T>>` 处理程序，回调抛出的任何异常都被视为测试失败
- `verify` 来执行断言，从代码块抛出的任何异常都被认为是测试失败。

> **☢警告:** 与 `succeedingThenComplete` 和 `failingThenComplete` 不同，调用 `succeeding` 和 `failing` 方法只能使测试失败（例如，`succeeding` 得到失败的异步结果）。 要使测试通过，您仍然需要调用 `completeNow`，或使用如下所述的检查点。

## 有多个成功条件时的检查点

许多测试可以通过在执行的某个时间点调用`completeNow`来标记为通过。 话虽如此，在许多情况下，测试的成功取决于要验证的不同异步部分。

您可以使用检查点来标记一些要通过的执行点。 一个`Checkpoint`可能需要一个标记或多个标记。 当所有检查点都被标记后，相应的 `VertxTestContext` 使测试通过。

这是一个示例，其中 HTTP 服务器上的检查点正在启动，10 个 HTTP 请求得到响应，10 个 HTTP 客户端请求已经发出：

```java
Checkpoint serverStarted = testContext.checkpoint();
Checkpoint requestsServed = testContext.checkpoint(10);
Checkpoint responsesReceived = testContext.checkpoint(10);

vertx.createHttpServer()
  .requestHandler(req -> {
    req.response().end("Ok");
    requestsServed.flag();
  })
  .listen(8888)
  .onComplete(testContext.succeeding(httpServer -> {
    serverStarted.flag();

    HttpClient client = vertx.createHttpClient();
    for (int i = 0; i < 10; i++) {
      client.request(HttpMethod.GET, 8888, "localhost", "/")
        .compose(req -> req.send().compose(HttpClientResponse::body))
        .onComplete(testContext.succeeding(buffer -> testContext.verify(() -> {
          assertThat(buffer.toString()).isEqualTo("Ok");
          responsesReceived.flag();
        })));
    }
  }));
```

> **💡提示:** 检查点应该只从测试用例主线程创建，而不是从 Vert.x 异步事件回调。

## 与 JUnit 5 集成

与以前的版本相比，JUnit 5 提供了不同的模型。

### Test 方法

Vert.x 集成主要使用 `VertxExtension` 类，并使用 `Vertx` 和 `VertxTestContext` 实例的测试参数注入：

```java
@ExtendWith(VertxExtension.class)
class SomeTest {

  @Test
  void some_test(Vertx vertx, VertxTestContext testContext) {
    // (...)
  }
}
```

> **🏷注意:** `Vertx` 实例没有集群并且具有默认配置。 如果您需要其他东西，那么不要在该参数上使用注入并自己准备一个`Vertx`对象。

测试会自动包装在 `VertxTestContext` 实例生命周期中，因此您不需要自己插入 `awaitCompletion` 调用：

```java
@ExtendWith(VertxExtension.class)
class SomeTest {

  @Test
  void http_server_check_response(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(new HttpServerVerticle(), testContext.succeeding(id -> {
      HttpClient client = vertx.createHttpClient();
      client.request(HttpMethod.GET, 8080, "localhost", "/")
        .compose(req -> req.send().compose(HttpClientResponse::body))
        .onComplete(testContext.succeeding(buffer -> testContext.verify(() -> {
          assertThat(buffer.toString()).isEqualTo("Plop");
          testContext.completeNow();
        })));
    }));
  }
}
```

您可以将它与标准的 JUnit 注解一起使用，例如 `@RepeatedTest` 或生命周期回调注解：

```java
@ExtendWith(VertxExtension.class)
class SomeTest {

  // Deploy the verticle and execute the test methods when the verticle
  // is successfully deployed
  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(new HttpServerVerticle(), testContext.succeedingThenComplete());
  }

  // Repeat this test 3 times
  @RepeatedTest(3)
  void http_server_check_response(Vertx vertx, VertxTestContext testContext) {
    HttpClient client = vertx.createHttpClient();
    client.request(HttpMethod.GET, 8080, "localhost", "/")
      .compose(req -> req.send().compose(HttpClientResponse::body))
      .onComplete(testContext.succeeding(buffer -> testContext.verify(() -> {
        assertThat(buffer.toString()).isEqualTo("Plop");
        testContext.completeNow();
      })));
  }
}
```

也可以在测试类或方法上使用 `@Timeout` 注解自定义默认的 `VertxTestContext` 超时：

```java
@ExtendWith(VertxExtension.class)
class SomeTest {

  @Test
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void some_test(Vertx vertx, VertxTestContext context) {
    // (...)
  }
}
```

### 生命周期方法

JUnit 5 提供了几个用户定义的生命周期方法，用 `@BeforeAll`、`@BeforeEach`、`@AfterEach` 和 `@AfterAll` 注解。

这些方法可以请求注入 `Vertx` 实例。 通过这样做，他们很可能对 `Vertx` 实例执行异步操作，因此他们可以请求注入 `VertxTestContext` 实例以确保 JUnit 运行程序等待它们完成，并报告可能的错误。

这是一个例子：

```java
@ExtendWith(VertxExtension.class)
class LifecycleExampleTest {

  @BeforeEach
  @DisplayName("Deploy a verticle")
  void prepare(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(new SomeVerticle(), testContext.succeedingThenComplete());
  }

  @Test
  @DisplayName("A first test")
  void foo(Vertx vertx, VertxTestContext testContext) {
    // (...)
    testContext.completeNow();
  }

  @Test
  @DisplayName("A second test")
  void bar(Vertx vertx, VertxTestContext testContext) {
    // (...)
    testContext.completeNow();
  }

  @AfterEach
  @DisplayName("Check that the verticle is still there")
  void lastChecks(Vertx vertx) {
    assertThat(vertx.deploymentIDs())
      .isNotEmpty()
      .hasSize(1);
  }
}
```

#### `VertxTestContext` 对象的作用域

由于这些对象有助于等待异步操作完成，因此会为任何 `@Test`、`@BeforeAll`、`@BeforeEach`、`@AfterEach` 和 `@AfterAll` 方法创建一个新实例。

#### `Vertx` 对象的作用域

`Vertx` 对象的范围取决于 [JUnit 相对执行顺序](http://junit.org/junit5/docs/current/user-guide/#extensions-execution-order) 中的哪个生命周期方法首先需要一个 要创建的新实例。 一般来说，我们尊重 JUnit 扩展范围规则，但这里是规范。

1. 如果父测试上下文已经有一个 `Vertx` 实例，它会在子扩展测试上下文中被重用。
2. 注入 `@BeforeAll` 方法会创建一个新实例，该实例将在所有后续测试和生命周期方法中共享以供注入。
3. 注入没有父上下文的 `@BeforeEach` 或先前的 `@BeforeAll` 注入会创建一个与相应测试和 `AfterEach` 方法共享的新实例。
4. 如果在运行测试方法之前不存在任何实例，则会为该测试创建一个实例（并且仅针对该测试）。

#### 配置 `Vertx` 实例

默认情况下，使用 `Vertx` 的默认设置使用 `Vertx.vertx()` 创建 `Vertx` 对象。 但是，您可以配置 `VertxOptions` 以满足您的需求。 一个典型的用例是“为调试延长阻塞超时警告”。 要配置 `Vertx` 对象，您必须：

1. 使用 [json 格式](https://vertx.io/docs/apidocs/io/vertx/core/VertxOptions.html#VertxOptions-io.vertx.core.json.JsonObject-) 创建一个带有 `VertxOptions` 的 json 文件
2. 创建一个指向该文件的环境变量`vertx.parameter.filename`

延长超时的示例文件内容：

```json
{
 "blockedThreadCheckInterval" : 5,
 "blockedThreadCheckIntervalUnit" : "MINUTES",
 "maxEventLoopExecuteTime" : 360,
 "maxEventLoopExecuteTimeUnit" : "SECONDS"
}
```

满足这些条件后，将使用配置的选项创建 `Vertx` 对象

#### 关闭和移除 `Vertx` 对象

注入的 `Vertx` 对象会自动关闭并从其相应的范围中删除。

例如，如果为测试方法的范围创建了一个 `Vertx` 对象，它会在测试完成后关闭。 类似地，当它被`@BeforeEach` 方法创建时，它会在可能的`@AfterEach` 方法完成后被关闭。

#### 对相同生命周期事件的多个方法发出警告

JUnit 5允许为相同的生命周期事件存在多个方法。

例如，可以在同一个测试中定义 3 个 `@BeforeEach` 方法。 由于异步操作，这些方法的效果可能同时发生而不是顺序发生，这可能导致状态不一致。

这是 JUnit 5 而不是这个模块的问题。 如有疑问，您可能总是想知道为什么单一方法不能比许多方法更好。

## 支持其他参数类型

Vert.x JUnit 5 扩展是可扩展的：您可以通过 `VertxExtensionParameterProvider` 服务提供者接口添加更多类型。

如果你使用 RxJava，而不是`io.vertx.core.Vertx`，你可以注入：

- `io.vertx.rxjava3.core.Vertx`, 或者
- `io.vertx.reactivex.core.Vertx`, 或者
- `io.vertx.rxjava.core.Vertx`.

为此，请将相应的库添加到您的项目中：

- `io.vertx:vertx-junit5-rx-java3`, 或者
- `io.vertx:vertx-junit5-rx-java2`, 或者
- `io.vertx:vertx-junit5-rx-java`.

在 Reactiveerse 上，您可以在 `reactiverse-junit5-extensions` 项目中找到越来越多的 `vertx-junit5` 扩展集合，这些扩展与 Vert.x 堆栈集成：`https://github.com/reactiverse/reactiverse-junit5-extensions`。

## 参数排序

可能是一个参数类型必须放在另一个参数之前。 例如，`vertx-junit5-extensions` 项目中的 Web 客户端支持要求 `Vertx` 参数位于 `WebClient` 参数之前。 这是因为 `Vertx` 实例需要存在才能创建 `WebClient`。

期望参数提供者抛出有意义的异常，让用户知道可能的排序约束。

在任何情况下，最好先使用 `Vertx` 参数，然后按照手动创建它们的顺序排列下一个参数。

## 使用 `@MethodSource` 的参数化测试

您可以使用带有 vertx-junit5 的 `@MethodSource` 的参数化测试。 因此，您需要在方法定义中的 vertx 测试参数之前声明方法源参数。

```java
@ExtendWith(VertxExtension.class)
static class SomeTest {

  static Stream<Arguments> testData() {
    return Stream.of(
      Arguments.of("complex object1", 4),
      Arguments.of("complex object2", 0)
    );
  }

  @ParameterizedTest
  @MethodSource("testData")
  void test2(String obj, int count, Vertx vertx, VertxTestContext testContext) {
    // your test code
    testContext.completeNow();
  }
}
```

其他 `ArgumentSources` 也是如此。 参见[ParameterizedTest](https://junit.org/junit5/docs/current/api/org.junit.jupiter.params/org/junit/jupiter/params/ParameterizedTest.html)的API文档中的`Formal Parameter List`部分.

## 在 Vert.x 上下文中运行测试

默认情况下，调用测试方法的线程是 JUnit 线程。 `RunTestOnContext` 扩展可用于通过在 Vert.x 事件循环线程上运行测试方法来改变此行为。

> **⚠小心:** 请记住，在使用此扩展程序时，您不能阻止事件循环。

为此，扩展需要一个 `Vertx` 实例。 默认情况下，它会自动创建一个，但您可以提供配置选项或 supplier 方法。

测试运行时可以检索 `Vertx` 实例。

```java
@ExtendWith(VertxExtension.class)
class RunTestOnContextExampleTest {

  @RegisterExtension
  RunTestOnContext rtoc = new RunTestOnContext();

  Vertx vertx;

  @BeforeEach
  void prepare(VertxTestContext testContext) {
    vertx = rtoc.vertx();
    // Prepare something on a Vert.x event-loop thread
    // The thread changes with each test instance
    testContext.completeNow();
  }

  @Test
  void foo(VertxTestContext testContext) {
    // Test something on the same Vert.x event-loop thread
    // that called prepare
    testContext.completeNow();
  }

  @AfterEach
  void cleanUp(VertxTestContext testContext) {
    // Clean things up on the same Vert.x event-loop thread
    // that called prepare and foo
    testContext.completeNow();
  }
}
```

当用作`@RegisterExtension` 实例字段时，会为每个测试方法创建一个新的`Vertx` 对象和`Context`。 `@BeforeEach` 和 `@AfterEach` 方法在此上下文中执行。

当用作`@RegisterExtension` 静态字段时，会为所有测试方法创建一个`Vertx` 对象和`Context`。 `@BeforeAll` 和 `@AfterAll` 方法也在这个上下文中执行。

