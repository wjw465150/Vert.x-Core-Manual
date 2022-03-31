# Vert.x 4 Core Manual中文版

> 翻译: 白石(https://github.com/wjw465150/Vert.x-Core-Manual)

Vert.x的Core是一组Java API,我们称为**Vert.x Core**

[Repository](https://github.com/eclipse/vert.x).

Vert.x核心为以下事情提供功能:

- 编写TCP客户端和服务器
- 编写HTTP客户端和服务器,包括对WebSockets的支持
- 事件总线
- 共享数据 - -本地映射和集群分布式映射
- 周期性和延迟动作
- 部署和取消部署Verticles
- 数据报套接字
- DNS客户端
- 文件系统访问
- 高可用性
- 本地传输
- 集群

核心功能相当低级-在这里找不到数据库访问,授权或高级Web功能之类的东西-在**Vert.x ext**(扩展)中可以找到.

Vert.x 内核小巧轻便. 你只需使用你想要的部分. 它还可以完全嵌入到您现有的应用程序中--我们不会强迫您以特殊的方式构建应用程序,以便您可以使用 Vert.x.

您可以使用Vert.x支持的任何其他语言的core. 但这很酷-我们不会强迫您直接从JavaScript或Ruby中使用Java API-毕竟,不同的语言具有不同的约定和惯用语,而在Ruby上强制使用Java惯用语是很奇怪的 开发人员(例如). 相反,我们会为每种语言自动生成等效于核心Java API的"惯用语言".

从现在开始,我们将仅使用**core**一词来指代Vert.x核心.

如果使用的是Maven或Gradle,请将以下依赖项添加到项目描述符的*dependencies*部分,以访问Vert.x Core API:

- Maven (在您的`pom.xml`中):

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-core</artifactId>
 <version>4.2.4</version>
</dependency>
```

- Gradle (在您的`build.gradle`文件中):

```groovy
dependencies {
 compile 'io.vertx:vertx-core:4.2.4'
}
```

让我们讨论core中的不同概念和功能.

## 开始创建Vert.x

除非您可以与 `Vertx` 对象通信,否则您在 Vert.x天地中无法做太多事情!

它是 Vert.x 的控制中心,是你做几乎所有事情的方式,包括创建客户端和服务器,获取对事件总线的引用,设置计时器以及许多其他事情.

那么如何获取实例呢?

如果您要嵌入 Vert.x,那么您只需创建一个实例,如下所示:

```java
Vertx vertx = Vertx.vertx();
```

> **🏷注意:** 大多数应用程序只需要一个 Vert.x 实例,但如果您需要隔离事件总线或不同的服务器组和客户端,则可以创建多个 Vert.x 实例.

### 创建Vert.x对象时指定选项

创建Vert.x对象时,如果默认值不适合您,您还可以指定选项:

```java
Vertx vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(40));
```

`VertxOptions`对象有许多设置,允许您配置集群,高可用性,池大小和各种其他设置.

### 创建集群的Vert.x对象

如果您要创建**集群Vert.x**(请参阅[事件总线](#event_bus)上的部分,以获得关于集群事件总线的更多信息),然后您将通常使用异步变量来创建Vert.x对象.

这是因为集群中的不同Vert.x实例通常需要花费一些时间(可能是几秒钟)来组合在一起. 在此期间,我们不想阻塞调用线程,因此我们将结果异步地提供给您.

## 你是fluent的吗?

您可能已经注意到,在前面的示例中使用了**fluent** API.

在`fluent` API中,可以将多个方法调用链接在一起.例如:

```java
request.response().putHeader("Content-Type", "text/plain").end("some text");
```

这是整个Vert.x API的通用模式,因此请习惯使用它.

像这样的链接调用允许您编写稍微不那么冗长的代码.当然,如果您不喜欢fluent方法**我们不会强迫您**这样做,如果您愿意,您可以愉快地忽略它,并像这样编写您的代码:

```java
HttpServerResponse response = request.response();
response.putHeader("Content-Type", "text/plain");
response.write("some text");
response.end();
```

## 别Call我们,我们会Call你.

Vert.x API在很大程度上是*事件驱动*的. 这意味着当您感兴趣的Vert.x中发生任何事情时,Vert.x会通过向您发送事件来呼叫您.

一些示例事件是:

- 计时器已触发
- 一些数据已经到达套接字
- 从磁盘读取了一些数据
- 发生异常
- HTTP服务器已收到请求

您可以通过向Vert.x API提供*handlers*来处理事件. 例如,要每秒接收一个计时器事件,您将执行以下操作:

```java
vertx.setPeriodic(1000, id -> {
  // This handler will get called every second
  System.out.println("timer fired!");
});
```

或接收HTTP请求:

```java
server.requestHandler(request -> {
  // This handler will be called every time an HTTP request is received at the server
  request.response().end("hello world!");
});
```

过了一段时间,当Vert.x有一个事件传递给您的处理器时,Vert.x会**异步地**调用它.

这使我们想到了Vert.x中的一些重要概念:

## 不要阻塞我!

除了极少数例外(即某些以"Sync"结尾的文件系统操作),Vert.x 中的任何 API 都不会阻塞调用线程.

如果可以立即提供结果,则将立即返回结果,否则通常会在一段时间后提供处理器以接收事件.

因为没有Vert.x API会阻塞线程,这意味着您可以使Vert.x仅使用少量线程来处理大量并发.

使用传统的阻止API,在以下情况下,调用线程可能会阻止:

- 从套接字读取数据
- 将数据写入磁盘
- 向收件人发送消息并等待回复.
- … 许多其他情况

在上述所有情况下,当您的线程正在等待结果时,它无能为力-它实际上是无用的.

这意味着,如果要使用阻塞API进行大量并发操作,则需要大量线程来防止应用程序停止运行.

线程在其所需的内存(例如用于堆栈)和上下文切换方面都有开销.

对于许多现代应用程序所需的并发级别,阻塞方法根本无法扩展.

## Reactor & 多Multi-Reactor

我们之前提到过Vert.x API是事件驱动的 - Vert.x在事件可用时将事件传递给处理器.

在大多数情况下,Vert.x使用称为**event loop(事件循环)**的线程调用处理器.

由于 Vert.x 或您的应用程序块中没有任何内容,事件循环可以愉快地运行,在事件到达时连续将事件传递给不同的处理器.

因为没有阻塞,事件循环可能会在短时间内传递大量事件. 例如,单个事件循环可以非常快速地处理数千个 HTTP 请求.

我们称之为 [Reactor Pattern(反应堆模式)](https://en.wikipedia.org/wiki/Reactor_pattern).

你可能以前听说过这个 - 例如 Node.js 实现了这个模式.

在标准的反应器实现中,有一个**single event loop(单个事件循环)**线程,该线程在一个循环中运行,将所有事件到达时的所有事件传递给所有处理器.

单线程的问题是它在任何时候都只能在单核上运行,所以如果你想让你的单线程反应器应用程序(例如你的 Node.js 应用程序)在你的多核服务器上扩展,你必须启动并管理许多不同的进程.

Vert.x 在这里的工作方式不同. 每个 Vert.x 实例都维护**几个事件循环**,而不是单个事件循环. 默认情况下,我们根据机器上可用内核的数量来选择,但这可以被覆盖.

这意味着与 Node.js 不同,单个 Vertx 进程可以跨服务器扩展.

我们将此模式称为**Multi-Reactor Pattern(多反应器模式)**,以将其与单线程反应器模式区分开.

> **🏷注意:** 尽管Vertx实例维护多个事件循环,但任何特定的处理器永远不会并发执行,并且在大多数情况下(除了 [worker verticles](#Worker_verticles))总是使用**完全相同的事件循环**调用.

<a name="golden_rule"></a>

## 黄金法则 - 不要阻塞事件循环

我们已经知道 Vert.x API 是非阻塞的,不会阻塞事件循环,但如果您在处理器中自己阻塞事件循环,这并没有多大帮助.

如果这样做,则在阻塞期间该事件循环将无法执行任何其他操作.如果您阻塞了Vertx实例中的所有事件循环,那么您的应用程序将完全停止!

所以不要这样做! **我已经警告过你了**.

阻塞的例子包括:

- Thread.sleep()
- 等待锁
- 等待互斥或监视器(例如: synchronized 段)
- 进行长时间的数据库操作并等待结果
- 进行复杂的计算需要花费大量时间.
- 循环运行

如果以上任何一种情况使事件循环在**相当长的时间内**停止执行任何其他操作,那么您应该立即转到naughty(不妥当)的步骤,并等待进一步的指示.

那么,什么是**相当长的时间内**?

一个时间片到底是多长?这实际上取决于您的应用程序和所需的并发数量.

如果您有一个单独的事件循环,并且希望每秒处理10000个http请求,那么很明显,每个请求的处理时间不能超过0.1毫秒,因此您不能阻塞超过0.1毫秒的时间.

**这道数学题不难,留给读者作为练习.**

如果您的应用程序没有响应,则可能表明您在某处阻塞了事件循环. 为了帮助您诊断此类问题,如果 Vert.x 检测到事件循环有一段时间没有返回,它会自动记录警告. 如果您在日志中看到此类警告,那么您应该进行调查.

```
Thread vertx-eventloop-thread-3 has been blocked for 20458 ms
```

Vert.x还将提供堆栈跟踪,以精确定位阻塞发生的位置.

如果您想关闭这些警告或更改设置,您可以在创建 Vert.x 对象之前在 `VertxOptions` 对象中来改变缺省设置.

## Future的结果

Vert.x 4 使用futures来表示异步结果.

任何异步方法都会为调用结果返回一个`Future`对象:一个*success* 或者 *failure*.

您不能直接与future的结果进行交互,而是需要设置一个处理器,当future完成并且结果可用时将调用该处理器,就像任何其他类型的事件一样.

```java
FileSystem fs = vertx.fileSystem();

Future<FileProps> future = fs.props("/my_file.txt");

future.onComplete((AsyncResult<FileProps> ar) -> {
  if (ar.succeeded()) {
    FileProps props = ar.result();
    System.out.println("File size = " + props.size());
  } else {
    System.out.println("Failure: " + ar.cause().getMessage());
  }
});

```

> **🏷注意:** Vert.x 3 仅提供了一个回调模型. 为了轻松迁移到 Vert.x 4,我们决定每个异步方法也有一个回调版本. 上面的 `props` 方法还有一个 `props` 版本,其中回调作为方法参数.

## Future的composition(组合)

`compose` 可用于futures的链式调用:

- 在当前future成功时,应用给定的函数,返回一个future.当返回的future完成时,composition成功.
- 在当前future失败时,composition失败

```java
FileSystem fs = vertx.fileSystem();

Future<Void> future = fs
  .createFile("/foo")
  .compose(v -> {
    // When the file is created (fut1), execute this:
    return fs.writeFile("/foo", Buffer.buffer());
  })
  .compose(v -> {
    // When the file is written (fut2), execute this:
    return fs.move("/foo", "/bar");
  });
```

在此示例中,3 个操作链接在一起:

1. 创建一个文件
2. 数据写入此文件
3. 文件被移动

当这 3 个步骤成功时,最终的`future`将成功. 但是,如果其中一个步骤失败,最终的`future`将失败.

除此之外,`Future` 提供了更多:`map`,`recover`,`otherwise` 甚至是`compose` 的别名`flatMap`

## Future的coordination(协调)

使用 Vert.x的 `futures` 可以实现多个future的协调. 它支持并发组合(并行运行多个异步操作)和顺序组合(链式异步操作).

`CompositeFuture.all` 接受多个future作为参数(最多 6 个),并在所有future*成功*时返回*成功*,当至少一个future失败时返回*失败*:

```java
Future<HttpServer> httpServerFuture = httpServer.listen();

Future<NetServer> netServerFuture = netServer.listen();

CompositeFuture.all(httpServerFuture, netServerFuture).onComplete(ar -> {
  if (ar.succeeded()) {
    // All servers started
  } else {
    // At least one server failed
  }
});
```

这些操作同时运行,附加到返回的future的"处理器"在组合完成时被调用. 当其中一项操作失败(通过的future之一被标记为失败)时,生成的future也被标记为失败. 当所有操作都成功时,由此产生的future就成功完成了.

或者,您可以传递future列表(可能为空):

```java
CompositeFuture.all(Arrays.asList(future1, future2, future3));
```

虽然 `all` 组合 * 等待* 直到所有future都成功(或一个失败),但 `any` 组合 * 等待* 第一个成功的future.

`CompositeFuture.any` 接受多个future作为参数(最多 6 个)并返回一个future,当其中一个future成功时成功,当所有future都失败时失败:

```java
CompositeFuture.any(future1, future2).onComplete(ar -> {
  if (ar.succeeded()) {
    // At least one is succeeded
  } else {
    // All failed
  }
});
```

也可以使用future列表:

```java
CompositeFuture.any(Arrays.asList(f1, f2, f3));
```

`join` 组合 * 等待 * 直到所有future都完成,无论是成功还是失败. `CompositeFuture.join` 接受多个future作为参数(最多 6 个)并返回一个future,当所有future都成功时成功,当所有future,都完成且至少其中一个失败时失败:

```java
CompositeFuture.join(future1, future2, future3).onComplete(ar -> {
  if (ar.succeeded()) {
    // All succeeded
  } else {
    // All completed and at least one failed
  }
});
```

也可以使用future列表:

```java
CompositeFuture.join(Arrays.asList(future1, future2, future3));
```

### 与JDK自带的CompletionStage的互操作性

Vert.x `Future` API 提供兼容性 *from* 和 *to* `CompletionStage`,CompletionStage是用于可组合异步操作的 JDK 接口.

我们可以使用 `toCompletionStage` 方法从 Vert.x `Future` 转到 `CompletionStage`,如下所示:

```java
Future<String> future = vertx.createDnsClient().lookup("vertx.io");
future.toCompletionStage().whenComplete((ip, err) -> {
  if (err != null) {
    System.err.println("Could not resolve vertx.io");
    err.printStackTrace();
  } else {
    System.out.println("vertx.io => " + ip);
  }
});
```

相反,我们可以使用 `Future.fromCompletionStage` 从 `CompletionStage` 转到 Vert.x的 `Future`. 有 2 种变体:

1. 第一个变体只接受一个`CompletionStage`并从解析`CompletionStage`实例的线程调用`Future`方法
2. 第二个变体采用额外的 `Context` 参数来调用 Vert.x 上下文中的 `Future` 方法.

> **⚠重要:**  在大多数情况下,带有 `CompletionStage` 和 `Context` 的变体是您想要用来尊重 Vert.x 线程模型的变体,因为 Vert.x `Future` 更有可能与 Vert.x 的代码,库和客户端一起使用.

下面是一个从 `CompletionStage` 到 Vert.x `Future` 并在上下文中调度的示例:

```java
Future.fromCompletionStage(completionStage, vertx.getOrCreateContext())
  .flatMap(str -> {
    String key = UUID.randomUUID().toString();
    return storeInDb(key, str);
  })
  .onSuccess(str -> {
    System.out.println("We have a result: " + str);
  })
  .onFailure(err -> {
    System.err.println("We have a problem");
    err.printStackTrace();
  });
```

## Verticles

Vert.x 带有一个简单的,可扩展的,*actor-like* 部署和开箱即用的并发模型,您可以使用它来节省您自己编写的代码.

**此模型完全是可选的,如果您不想这样做,Vert.x 不会强迫您以这种方式创建应用程序.**

该模型并不声称是严格的`actor-model`实现,但它确实具有相似之处,尤其是在并发性,扩展性和部署方面.

要使用此模型,您将代码编写为一组 **verticles**.

Verticle 是由 Vert.x 部署和运行的代码块. 一个 Vert.x 实例默认维护 N 个事件循环线程(其中 N 默认为 core*2). Verticle 可以用 Vert.x 支持的任何语言编写,单个应用程序可以包含用多种语言编写的 Verticle.

您可以将verticle视为有点像 [Actor Model] (https://en.wikipedia.org/wiki/Actor_model) 中的演员.

一个应用程序通常由同时在同一个 Vert.x 实例中运行的多个 Verticle 实例组成. 不同的verticle实例通过在[event bus](#event_bus)上发送消息相互通信.

### 编写Verticles

Verticle类必须实现`Verticle`接口.

如果您愿意,他们可以直接实现它,但通常扩展抽象类`AbstractVerticle`更简单.

这是一个verticle的示例:

```java
public class MyVerticle extends AbstractVerticle {

 // Called when verticle is deployed
 public void start() {
 }

 // Optional - called when verticle is undeployed
 public void stop() {
 }

}
```

通常你会像上面的例子一样覆盖 start 方法.

当 Vert.x 部署 Verticle 时,它会调用 start 方法,当该方法完成时,Verticle 将被视为已启动.

您还可以选择覆盖 stop 方法. 这将在 Vert.x 取消部署时调用,并且当方法完成时,Verticle 将被视为已停止.

### 异步的Verticle start 和 stop

有时你想在你的 verticle 启动中做一些需要一些时间的事情,并且你不希望在这种情况发生之前考虑部署 verticle. 例如,您可能希望在 start 方法中启动 HTTP 服务器并传播服务器`listen`方法的异步结果.

你不能用阻塞的方式等待HTTP服务器绑定在你的start方法中,因为这将打破[黄金法则](#golden_rule).

那么该怎么能呢?

实现的方法是实现**异步的**start方法.这个版本的方法以Future作为参数.当该方法返回时,将**不**被认为已经被部署.

一段时间后,在你完成了你需要做的所有事情(例如启动 HTTP 服务器)之后,你可以在 Future 上调用 complete(或 fail)来表示你已经完成了.

这是一个例子:

```java
public class MyVerticle extends AbstractVerticle {

 private HttpServer server;

 public void start(Promise<Void> startPromise) {
   server = vertx.createHttpServer().requestHandler(req -> {
     req.response()
       .putHeader("content-type", "text/plain")
       .end("Hello from Vert.x!");
     });

   // Now bind the server:
   server.listen(8080, res -> {
     if (res.succeeded()) {
       startPromise.complete();
     } else {
       startPromise.fail(res.cause());
     }
   });
 }
}
```

同样,也有一个异步版本的 stop 方法. 如果你想做一些需要一些时间的verticle 清理,你可以使用它.

```java
public class MyVerticle extends AbstractVerticle {

 public void start() {
   // Do something
 }

 public void stop(Promise<Void> stopPromise) {
   obj.doSomethingThatTakesTime(res -> {
     if (res.succeeded()) {
       stopPromise.complete();
     } else {
       stopPromise.fail();
     }
   });
 }
}
```

> **💡提示:** 您不需要在 verticle 的 stop 方法中手动停止由 verticle 启动的 HTTP 服务器. 当 Verticle 取消部署时,Vert.x 将自动停止任何正在运行的服务器.

### Verticle 类型

有两种不同类型的verticles:

- 标准 Verticles

  这些是最常见和最有用的类型-它们总是使用事件循环线程执行. 我们将在下一节中对此进行更多讨论.

- 工作 Verticles

  这些使用工作池中的线程运行. 一个Verticle实例永远不会被多个线程同时执行.

### 标准 verticles

标准 Verticle 在创建时被分配一个事件循环线程,并使用该事件循环调用 `start` 方法. 当您从事件循环调用任何其他在核心 API 上获取处理器的方法时,Vert.x 将保证这些处理器在被调用时将在同一个事件循环上执行.

这意味着我们可以保证您的 Verticle 实例中的所有代码始终在同一个事件循环上执行(只要您不创建自己的线程并调用它!).

这意味着您可以将应用程序中的所有代码编写为单线程,并让 Vert.x 来管理线程和伸缩性. 你不再担心**同步**和**易失性**,并且您还可以避免在进行手动"传统"多线程应用程序开发时如此普遍的许多其他竞争条件和死锁情况.

<a name="Worker_verticles"></a>

### 工作 verticles

一个worker verticle就像一个标准的verticle,但它是使用Vert.x工作线程池中的一个线程来执行的,而不是使用一个事件循环.

Worker Verticle 是为调用阻塞代码而设计的,因为它们不会阻塞任何事件循环.

如果你不想使用 worker verticle 来运行阻塞代码,你也可以在事件循环中直接运行 [inline blocking code](#Running_blocking_code).

如果你想将一个verticle部署为一个worker verticle,你可以使用`setWorker`来实现.

```java
DeploymentOptions options = new DeploymentOptions().setWorker(true);
vertx.deployVerticle("com.mycompany.MyOrderProcessorVerticle", options);
```

Worker verticle实例**永远不会**被Vert.x的多个线程并发执行,但可以在不同的时间由不同的线程执行.

### 以编程方式部署Verticle

您可以使用 `deployVerticle` 方法部署一个verticle,指定一个verticle名称,或者你可以传入一个你已经创建的verticle实例.

> **🏷注意:** 只有Java支持**instances**的方式来部署Verticle.

```java
Verticle myVerticle = new MyVerticle();
vertx.deployVerticle(myVerticle);
```

您还可以通过指定 Verticle **name** 来部署 Verticle.

verticle 名称用于查找特定的 `VerticleFactory`,用于实例化实际的 verticle 实例.

不同的verticle工厂可用于以不同的语言实例化verticle以及出于各种其他原因,例如在运行时加载服务和从 Maven 获取verticles.

这允许您部署以任何语言编写的 Vert.x 支持的任何其他语言的 Verticle.

下面是部署一些不同语言写的 Verticle 的示例:

```java
vertx.deployVerticle("com.mycompany.MyOrderProcessorVerticle");

// Deploy a JavaScript verticle
vertx.deployVerticle("verticles/myverticle.js");

// Deploy a Ruby verticle verticle
vertx.deployVerticle("verticles/my_verticle.rb");
```

### 将 Verticle 名称映射到 Verticle 工厂的规则

使用名称部署 verticle(s) 时,该名称用于选择将实例化 verticle(s) 的实际的verticle工厂.

Verticle名称可以有一个前缀 - 它是一个后跟冒号的字符串,如果存在将用于查找工厂,例如:

`js:foo.js` // 使用 JavaScript verticle 工厂
`groovy:com.mycompany.SomeGroovyCompiledVerticle` // 使用 Groovy verticle 工厂
`service:com.mycompany:myorderservice` // 使用 service verticle 工厂

如果不存在前缀,Vert.x 将查找后缀并使用它来查找工厂,例如:

`foo.js` // 也将使用 JavaScript verticle 工厂
`SomeScript.groovy` // 将使用 Groovy verticle 工厂

如果不存在前缀或后缀,Vert.x 将假定它是 Java 完全限定类名 (FQCN) 并尝试实例化它.

### Verticle工厂如何定位?

大多数 Verticle 工厂都是从类路径加载并在 Vert.x 启动时注册的.

如果您愿意,还可以使用 `registerVerticleFactory` 和 `unregisterVerticleFactory` 以编程方式注册和注销 Verticle 工厂.

### 等待部署完成

Verticle 部署是异步的,可能会在部署调用返回后的一段时间内完成.

如果您想在部署完成时收到通知,您可以部署时指定完成处理器:

```java
vertx.deployVerticle("com.mycompany.MyOrderProcessorVerticle", res -> {
  if (res.succeeded()) {
    System.out.println("Deployment id is: " + res.result());
  } else {
    System.out.println("Deployment failed!");
  }
});
```

如果部署成功,完成处理器将传递一个包含部署 ID 字符串的结果.

如果您想取消部署,以后可以使用此部署 ID.

### 取消已经部署的verticle

可以使用 `undeploy` 取消部署.

取消部署本身是异步的,因此如果您想在取消部署完成时收到通知,您可以undeploy时指定完成处理器:

```java
vertx.undeploy(deploymentID, res -> {
  if (res.succeeded()) {
    System.out.println("Undeployed ok");
  } else {
    System.out.println("Undeploy failed!");
  }
});
```

### 指定verticle实例的数量

使用verticle名称部署verticle时,您可以指定要部署的verticle实例的数量:

```java
DeploymentOptions options = new DeploymentOptions().setInstances(16);
vertx.deployVerticle("com.mycompany.MyOrderProcessorVerticle", options);
```

这对于跨多个内核轻松扩展很有用. 例如,您可能在一个有很多核的服务器上部署Web服务Verticle,因此您希望部署多个实例以利用所有核心.

<a name="passing_configuration_to_a_verticle"></a>
### 将配置传递给verticle

JSON形式的配置可以在部署时传递给一个verticle对象:

```java
JsonObject config = new JsonObject().put("name", "tim").put("directory", "/blah");
DeploymentOptions options = new DeploymentOptions().setConfig(config);
vertx.deployVerticle("com.mycompany.MyOrderProcessorVerticle", options);
```

然后可以通过 `Context` 对象或直接使用 `config` 方法获得此配置. 配置作为 JSON 对象返回,因此您可以按如下方式检索数据:

```java
System.out.println("Configuration: " + config().getString("name"));
```

### 访问 Verticle 中的环境变量

可以使用 Java API 访问环境变量和系统属性:

```java
System.getProperty("prop");
System.getenv("HOME");
```

### 高可用性

可以在启用高可用性 (HA) 的情况下部署 Verticles. 在这种情况下,当一个 verticle 部署在一个突然死亡的 vert.x 实例上时,该 verticle 会重新部署到集群中的另一个 vert.x 实例上.

要运行启用了高可用性的 Verticle,只需附加 `-ha` 开关:

```bash
vertx run my-verticle.js -ha
```

启用高可用性时,无需添加 `-cluster`.

[高可用性和故障转移](#high_availability_and_fail_over) 部分中有关高可用性功能和配置的更多详细信息。

### 从命令行运行 Verticles

你可以在你的Maven或Gradle项目中直接使用Vert.x,只要添加一个Vert.x核心库的依赖,然后从那里入手.

但是,如果您愿意,也可以直接从命令行运行 Vert.x 的 verticles.

为此,您需要下载并安装 Vert.x 发行版(`https://repo1.maven.org/maven2/io/vertx/vertx-stack-manager/4.2.4/`),并将安装的 `bin` 目录添加到 `PATH` 环境变量中. 还要确保你的 `PATH` 上有一个 Java JDK.

Vert.x 支持Java版本从 8 到 17.

> **🏷注意:** 需要JDK来支持Java代码的动态编译.

您现在可以使用 `vertex run` 命令运行verticles . 这里有些例子:

```bash
# Run a JavaScript verticle
vertx run my_verticle.js

# Run a Ruby verticle
vertx run a_n_other_verticle.rb

# Run a Groovy script verticle, clustered
vertx run FooVerticle.groovy -cluster
```

您甚至可以在不先编译它们的情况下运行Verticle的Java源代码 !

```bash
vertx run SomeJavaSourceFile.java
```

Vert.x 将在运行之前即时编译 Java 源文件. 这对于快速制作 Verticle 原型非常有用,非常适合演示. 无需先设置 Maven 或 Gradle 构建即可开始!

有关在命令行上执行 `vertx` 时可用的各种选项的完整信息,请在命令行中键入 `vertx`.

### 使 Vert.x 退出

Vert.x 实例维护的线程不是守护线程,因此它们会阻止 JVM 退出.

如果你正在嵌入 Vert.x 并且你已经完成了它,你可以调用 `close` 来关闭它.

这将关闭所有内部线程池并关闭其他资源,并允许 JVM 退出.

### Context(上下文) 对象

当Vert.x提供一个事件处理器或调用`Verticle`的start或stop方法时,与`Context`相关联. 通常一个上下文是一个**事件循环上下文**并且绑定到一个特定的事件循环线程. 因此,对该上下文的执行总是发生在完全相同的事件循环线程上. 在工作线程和运行内联阻塞代码的情况下,工作上下文将与使用工作线程池中的线程的执行环境相关联.

要获取上下文,请使用 `getOrCreateContext` 方法:

```java
Context context = vertx.getOrCreateContext();
```

如果当前线程有与之关联的上下文,它会重用上下文对象. 如果没有创建一个新的上下文实例. 您可以测试您检索到的上下文的 *type*:

```java
Context context = vertx.getOrCreateContext();
if (context.isEventLoopContext()) {
  System.out.println("Context attached to Event Loop");
} else if (context.isWorkerContext()) {
  System.out.println("Context attached to Worker Thread");
} else if (! Context.isOnVertxThread()) {
  System.out.println("Context not attached to a thread managed by vert.x");
}
```

检索到上下文对象后,您可以在此上下文中异步运行代码. 换句话说,您提交的任务最终将在相同的上下文中运行(有可能会在稍后的时间里):

```java
vertx.getOrCreateContext().runOnContext( (v) -> {
  System.out.println("This will be executed asynchronously in the same context");
});
```

当多个handlers在同一上下文中运行时,它们可能希望共享数据. 上下文对象提供了存储和检索在上下文中共享的数据的方法. 例如,它允许您将数据传递给使用 `runOnContext` 运行的某些操作:

```java
final Context context = vertx.getOrCreateContext();
context.put("data", "hello");
context.runOnContext((v) -> {
  String hello = context.get("data");
});
```

上下文对象还允许您使用 `config` 方法访问 Verticle 配置. 查看[将配置传递给verticle](#passing_configuration_to_a_verticle) 部分以了解有关此配置的更多详细信息.

### 执行定期和延迟的操作

在 Vert.x 中,想要在延迟后或定期执行操作是很常见的.

在标准 Verticle 中,你不能仅仅让线程休眠来引入延迟,因为这会阻塞事件循环线程.

相反,您使用 Vert.x 计时器. 计时器可以是**一次性**或**周期性**. 两者我们都讨论

#### 一次性计时器

单次计时器在一定延迟后调用事件处理器,以毫秒为单位.

一旦你使用传入延迟和处理器的`setTimer`方法来设置定时器触发

```java
long timerID = vertx.setTimer(1000, id -> {
  System.out.println("And one second later this is printed");
});

System.out.println("First this is printed");
```

返回值是一个唯一的计时器 ID,稍后可用于取消计时器. 处理器还传递了计时器 ID.

#### 周期性计时器

您还可以使用 `setPeriodic` 设置一个定期触发的计时器.

将有一个与周期相等的初始延迟.

`setPeriodic` 的返回值是一个唯一的计时器 ID(long). 如果需要取消计时器,这可以在以后使用.

传递给定时器事件处理器的参数也是唯一的定时器 id:

请记住,计时器将定期触发. 如果您的周期性处理需要很长时间才能完成,您的计时器事件可能会连续运行,甚至更糟:堆积.

在这种情况下,您应该考虑改用 `setTimer`. 任务完成后,您可以再设置下一个计时器.

```java
long timerID = vertx.setPeriodic(1000, id -> {
  System.out.println("And every second this is printed");
});

System.out.println("First this is printed");
```

#### 取消计时器

要取消定期计时器,请调用指定计时器 id 的`cancelTimer`方法. 例如:

```java
vertx.cancelTimer(timerID);
```

#### Verticle中的自动清理

如果您从 Verticle 内部创建计时器,这些计时器将在 Verticle 取消部署时自动关闭.

<a name="worker_verticles"></a>
### Verticle 工作池

Verticle 使用 Vert.x 工作池来执行阻塞操作,即 `executeBlocking` 或 工作Verticle.

可以在部署选项中指定不同的工作池:

```java
vertx.deployVerticle("the-verticle", new DeploymentOptions().setWorkerPoolName("the-specific-pool"));
```

<a name="event_bus"></a>

## The Event Bus(事件总线)

`event bus` 是 Vert.x 的**神经系统**.

每个 Vert.x 实例都有一个事件总线实例,它是使用 `eventBus` 方法获得的.

事件总线允许应用程序的不同部分相互通信,无论它们是用什么语言编写的,也不管它们是在同一个 Vert.x 实例中,还是在不同的 Vert.x 实例中.

它甚至可以桥接以允许在浏览器中运行的JavaScript客户端在同一事件总线上进行通信.

事件总线形成了一个跨越多个服务器节点和多个浏览器的分布式peer-to-peer(端到端)的消息传递系统.

事件总线支持发布/订阅,点对点和请求-响应消息传递.

事件总线 API 非常简单. 它主要涉及注册处理器,取消注册处理器以及发送和发布消息.

首先是一些理论:

### 理论

#### Addressing(寻址)

消息通过事件总线发送到的目的**地址**.

Vert.x 不做任何花哨的寻址方案. 在 Vert.x 中,地址只是一个字符串. 任何字符串都是有效的. 然而,使用某种方案是明智的,*例如*使用句点来划分命名空间.

有效地址的一些示例包括 `europe.news.feed1`,`acme.games.pacman`,`sausages` 和 `X`.

#### Handlers(处理器)

消息由处理器接收. 你在一个地址上注册一个处理器.

许多不同的处理器可以在同一地址上注册.

单个处理器可以在许多不同的地址上注册.

#### 发布/订阅消息

事件总线支持**发布**消息.

消息被发布到一个地址. 发布意味着将消息传递给在该地址注册的所有处理器.

这是熟悉的 **publish/subscribe** 消息传递模式.

#### 点对点 和 请求-响应 消息

事件总线还支持 **point-to-point** 消息传递.

消息被发送到一个地址. 然后 Vert.x 会将它们路由到在该地址注册的处理器之一.

如果在该地址注册了多个处理器,则将使用非严格的循环算法选择一个.

使用点对点消息传递时,可以在发送消息时指定可选的回复处理器.

当消息被接收方接收并得到处理后,接收方可以选择是否回复该消息.如果它们这样做,将调用回复处理器.

当发送方收到回复时,也可以对其进行回复.这可以无限重复,并允许在两个不同的verticles之间建立对话.

这是一种常见的消息传递模式,称为 **request-response** 模式.

#### Best-effort delivery(尽最大努力交付)

Vert.x 尽最大努力传递消息,不会有意识地丢弃它们. 这称为**best-effort**交付.

但是,如果事件总线的全部或部分发生故障,则消息可能会丢失.

如果应用程序关心丢失的消息,则应该将处理器编写为幂等的,并将发送程序编写为在恢复后重试.

#### 消息类型

开箱即用的 Vert.x 允许任何原始/简单类型,字符串或`buffers`作为消息发送.

但是,在 Vert.x 中,将消息发送为 [JSON](https://json.org/) 是一种惯例

JSON 在 Vert.x 支持的所有语言中都非常容易创建,读取和解析,因此它已成为 Vert.x 的一种*通用语*.

但是,如果您不想使用 JSON,则不必强制使用.

事件总线非常灵活,还支持通过事件总线发送任意对象. 您可以通过为要发送的对象定义"编解码器"来做到这一点.

### 事件总线API

让我们进入事件总线API.

#### 获取事件总线

您将获得对事件总线的引用,如下所示:

```java
EventBus eb = vertx.eventBus();
```

每个 Vert.x 实例都有一个事件总线实例.

#### 注册处理器

注册处理器的最简单方法是使用`consumer`. 这是一个例子:

```java
EventBus eb = vertx.eventBus();

eb.consumer("news.uk.sport", message -> {
  System.out.println("I have received a message: " + message.body());
});
```

当消息到达您的处理器时,您的处理器将被调用,并传入"消息".

调用 consumer() 返回的对象是 `MessageConsumer` 的一个实例.

该对象随后可用于取消注册处理器,或将处理器用作流.

或者,您可以使用 `consumer` 返回没有设置处理器的 MessageConsumer,然后在其上设置处理器. 例如:

```java
EventBus eb = vertx.eventBus();

MessageConsumer<String> consumer = eb.consumer("news.uk.sport");
consumer.handler(message -> {
  System.out.println("I have received a message: " + message.body());
});
```

在集群事件总线上注册处理器时,注册可能需要一些时间才能到达集群的所有节点.

如果您想在完成时收到通知,您可以在 MessageConsumer 对象上注册一个"完成处理器".

```java
consumer.completionHandler(res -> {
  if (res.succeeded()) {
    System.out.println("The handler registration has reached all nodes");
  } else {
    System.out.println("Registration failed!");
  }
});
```

#### 取消注册处理器

要取消注册处理器,请调用 `unregister`.

如果您在集群事件总线上,则取消注册可能需要一些时间才能在节点间传播. 如果您想在完成时收到通知,请使用"`unregister`.

```java
consumer.unregister(res -> {
  if (res.succeeded()) {
    System.out.println("The handler un-registration has reached all nodes");
  } else {
    System.out.println("Un-registration failed!");
  }
});
```

#### 发布消息

发布消息很简单. 只需使用 `publish` 指定将其发布到的地址.

```java
eventBus.publish("news.uk.sport", "Yay! Someone kicked a ball");
```

然后,该消息将被传递给针对地址"news.uk.sport" 注册的**所有**处理器.

#### 发送消息

发送消息将导致只有一个在接收消息的地址注册的处理器接收到. 这是点对点消息传递模式. 处理器以非严格的循环方式选择.

您可以使用 `send` 发送消息.

```java
eventBus.send("news.uk.sport", "Yay! Someone kicked a ball");
```

#### 在消息上设置标题

通过事件总线发送的消息也可以包含标头. 这可以通过在发送或发布时提供 `DeliveryOptions` 来指定:

```java
DeliveryOptions options = new DeliveryOptions();
options.addHeader("some-header", "some-value");
eventBus.send("news.uk.sport", "Yay! Someone kicked a ball", options);
```

#### 消息顺序

Vert.x 将按照从任何特定发送者发送消息的相同顺序将消息传递给任何特定处理器.

#### 消息对象

您在消息处理器中收到的对象是`Message`.

消息的`body` 对应于发送或发布的对象.

消息的标头可通过 `headers` 获得.

#### 确认消息/发送回复

当使用 `send` 时,事件总线会尝试将消息传递给在事件总线上注册的 `MessageConsumer`.

在某些情况下,发送方知道用户何时收到消息并使用**请求-响应**模式"处理"消息是有用的.

要确认消息已被处理,消费者可以通过调用 `reply` 来回复消息.

当发生这种情况时,它将导致将应答发送回发送方,并使用应答调用应答处理器.

一个例子可以清楚地说明这一点:

发件人:

```java
eventBus.request("news.uk.sport", "Yay! Someone kicked a ball across a patch of grass", ar -> {
  if (ar.succeeded()) {
    System.out.println("Received reply: " + ar.result().body());
  }
});
```

收件人:

```java
MessageConsumer<String> consumer = eventBus.consumer("news.uk.sport");
consumer.handler(message -> {
  System.out.println("I have received a message: " + message.body());
  message.reply("how interesting!");
});
```

`io.vertx.core.eventbus.Message.reply()` 可以包含一个有用信息的消息体.

"processing(处理)"的实际含义是应用程序定义的,并且完全取决于消息消费者所做的事情,而不是 Vert.x 事件总线本身知道或关心的事情.

一些例子:

- 一个简单的消息消费者实现了一个返回当天时间的服务,它将在应答主体中使用一个包含当天时间的消息进行确认
- 一个实现了持久队列的消息消费者,如果消息成功地保存在存储中,可以用`true`来确认,如果没有,可以用`false`来确认.
- 处理订单的消息消费者可能会在订单被成功处理后以`true`确认订单,以便将其从数据库中删除

#### 带超时的发送

当发送带有回复处理器的消息时,您可以在 `DeliveryOptions` 中指定超时.

如果在该时间内未收到回复,则将调用回复处理器并失败.

默认超时为 30 秒.

#### 发送失败

消息发送可能因其他原因而失败,包括:

- 没有可用于将消息发送到的处理器
- 收件人使用`fail`显式地使消息失败

在所有情况下,都会以特定的失败调用回复处理器.

#### 消息编解码器

如果您为它定义和注册一个"消息编解码器",您可以通过事件总线发送您喜欢的任何对象.

消息编解码器有一个名称,您在发送或发布消息时在 `DeliveryOptions` 中指定该名称:

```java
eventBus.registerCodec(myCodec);

DeliveryOptions options = new DeliveryOptions().setCodecName(myCodec.name());

eventBus.send("orders", new MyPOJO(), options);
```

如果您总是希望将相同的编解码器用于特定类型,那么您可以为其注册一个默认编解码器,那么您不必在每次发送时在交付选项中指定编解码器:

```java
eventBus.registerDefaultCodec(MyPOJO.class, myCodec);

eventBus.send("orders", new MyPOJO());
```

您可以使用 `unregisterCodec` 取消注册消息编解码器.

消息编解码器并不总是必须以相同的类型进行编码和解码. 例如,您可以编写允许发送 MyPOJO 类的编解码器,但是当该消息发送到处理器时,它会作为 MyOtherPOJO 类到达.

#### 集群的事件总线

事件总线不仅仅存在于单个 Vert.x 实例中. 通过在您的网络上将不同的 Vert.x 实例聚集在一起,它们可以形成一个单一的分布式事件总线.

#### 以编程方式的集群

如果您以编程方式创建 Vert.x 实例,您可以通过将 Vert.x 实例配置为集群来获得集群事件总线;

```java
VertxOptions options = new VertxOptions();
Vertx.clusteredVertx(options, res -> {
  if (res.succeeded()) {
    Vertx vertx = res.result();
    EventBus eventBus = vertx.eventBus();
    System.out.println("We now have a clustered event bus: " + eventBus);
  } else {
    System.out.println("Failed: " + res.cause());
  }
});
```

您还应该确保在您的类路径上有一个`ClusterManager`实现,例如 Hazelcast 集群管理器.

#### 在命令行上进行集群

可以在命令行中集群运行Vert.x

```bash
vertx run my-verticle.js -cluster
```

### 自动清理verticles

如果您从 verticles 内部注册事件总线处理器,这些处理器将在 verticle 取消部署时自动取消注册.

## 配置事件总线

可以配置事件总线. 当事件总线被集群时,它特别有用. 在底层,事件总线使用 TCP 连接来发送和接收消息,因此`EventBusOptions` 让您可以配置这些 TCP 连接的所有方面. 由于事件总线充当服务器和客户端,因此配置接近于 `NetClientOptions` 和 `NetServerOptions`.

```java
VertxOptions options = new VertxOptions()
    .setEventBusOptions(new EventBusOptions()
        .setSsl(true)
        .setKeyStoreOptions(new JksOptions().setPath("keystore.jks").setPassword("wibble"))
        .setTrustStoreOptions(new JksOptions().setPath("keystore.jks").setPassword("wibble"))
        .setClientAuth(ClientAuth.REQUIRED)
    );

Vertx.clusteredVertx(options, res -> {
  if (res.succeeded()) {
    Vertx vertx = res.result();
    EventBus eventBus = vertx.eventBus();
    System.out.println("We now have a clustered event bus: " + eventBus);
  } else {
    System.out.println("Failed: " + res.cause());
  }
});
```

前面的代码片段描述了如何将 SSL 连接用于事件总线,而不是普通的 TCP 连接.

> **☢警告:** 要在集群模式下强制执行安全性,您**必须**配置集群管理器以使用加密或强制执行安全性. 有关详细信息,请参阅集群管理器的文档.

事件总线配置需要在所有集群节点中保持一致.

`EventBusOptions`还允许你指定事件总线是否集群,端口和主机.

在容器中使用时,还可以配置公共主机和端口:

```java
VertxOptions options = new VertxOptions()
    .setEventBusOptions(new EventBusOptions()
        .setClusterPublicHost("whatever")
        .setClusterPublicPort(1234)
    );

Vertx.clusteredVertx(options, res -> {
  if (res.succeeded()) {
    Vertx vertx = res.result();
    EventBus eventBus = vertx.eventBus();
    System.out.println("We now have a clustered event bus: " + eventBus);
  } else {
    System.out.println("Failed: " + res.cause());
  }
});
```

## JSON

与其他一些语言不同,Java 没有对 [JSON](https://json.org/) 一流的支持,因此我们提供了两个类来使您在 Vert.x 应用程序中处理 JSON 更容易一些.

### JSON 对象

`JsonObject` 类表示 JSON 对象.

JSON 对象基本上只是一个Map,它具有字符串键,值可以是 JSON 支持的类型之一(字符串,数字,布尔值).

JSON 对象也支持空值.

#### 创建JSON对象

可以使用默认构造函数创建空 JSON 对象.

您可以从字符串 JSON 表示创建 JSON 对象,如下所示:

```java
String jsonString = "{\"foo\":\"bar\"}";
JsonObject object = new JsonObject(jsonString);
```

您可以从Map创建 JSON 对象,如下所示:

```java
Map<String, Object> map = new HashMap<>();
map.put("foo", "bar");
map.put("xyz", 3);
JsonObject object = new JsonObject(map);
```

#### 将条目放入JSON对象

使用 `put` 方法将值放入 JSON 对象.

由于流畅的API,方法调用可以被链接:

```java
JsonObject object = new JsonObject();
object.put("foo", "bar").put("num", 123).put("mybool", true);
```

#### 从JSON对象中获取值

您可以使用 `getXXX` 方法从 JSON 对象获取值,例如:

```java
String val = jsonObject.getString("some-key");
int intVal = jsonObject.getInteger("some-other-key");
```

#### JSON对象和Java对象之间的映射

您可以从 Java 对象的字段创建 JSON 对象,如下所示:

您可以实例化 Java 对象并从 JSON 对象中填充其字段,如下所示:

```java
request.bodyHandler(buff -> {
  JsonObject jsonObject = buff.toJsonObject();
  User javaObject = jsonObject.mapTo(User.class);
});
```

请注意,以上两个映射方向都使用 Jackson 的 `ObjectMapper#convertValue()` 来执行映射. 有关字段和构造函数可见性的影响,跨对象引用的序列化和反序列化的警告等信息,请参阅 Jackson 文档.

然而,在最简单的情况下,如果 Java 类的所有字段都是公共的(或具有公共的 getter/setter),并且存在公共默认构造函数(或没有定义的构造函数),则 `mapFrom` 和 `mapTo` 都应该成功.

只要对象图是无循环的,引用的对象将被传递地序列化/反序列化到嵌套的JSON对象.

#### 将JSON对象编码为字符串

您使用 `encode` 将对象编码为字符串形式.

### JSON 数组

`JsonArray` 类表示 JSON 数组.

JSON 数组是一系列值(字符串,数字,布尔值).

JSON 数组也可以包含空值.

#### 创建 JSON 数组

可以使用默认构造函数创建空的 JSON 数组.

您可以从字符串 JSON 表示创建 JSON 数组,如下所示:

```java
String jsonString = "[\"foo\",\"bar\"]";
JsonArray array = new JsonArray(jsonString);
```

#### 将条目添加到 JSON 数组中

您可以使用 `add` 方法将条目添加到 JSON 数组.

```java
JsonArray array = new JsonArray();
array.add("foo").add(123).add(false);
```

#### 从 JSON 数组中获取值

您可以使用 `getXXX` 方法从 JSON 数组中获取值,例如:

```java
String val = array.getString(0);
Integer intVal = array.getInteger(1);
Boolean boolVal = array.getBoolean(2);
```

#### 将 JSON 数组编码为字符串

您使用 `encode` 将数组编码为字符串形式.

#### 创建任意 JSON

创建 JSON 对象和数组假定您使用的是有效的字符串表示.

当您不确定字符串的有效性时,您应该使用 `Json.decodeValue`

```java
Object object = Json.decodeValue(arbitraryJson);
if (object instanceof JsonObject) {
  // That's a valid json object
} else if (object instanceof JsonArray) {
  // That's a valid json array
} else if (object instanceof String) {
  // That's valid string
} else {
  // etc...
}
```

## Json 指针

Vert.x 提供了 [来自 RFC6901 的 Json 指针](https://tools.ietf.org/html/rfc6901) 的实现. 您可以将指针用于查询和写入. 您可以使用字符串,URI 或手动附加路径来构建您的`JsonPointer`:

```java
JsonPointer pointer1 = JsonPointer.from("/hello/world");
// Build a pointer manually
JsonPointer pointer2 = JsonPointer.create()
  .append("hello")
  .append("world");
```

实例化指针后,使用 `queryJson` 查询 JSON 值. 您可以使用 `writeJson` 更新 Json 值:

```java
Object result1 = objectPointer.queryJson(jsonObject);
// Query a JsonArray
Object result2 = arrayPointer.queryJson(jsonArray);
// Write starting from a JsonObject
objectPointer.writeJson(jsonObject, "new element");
// Write starting from a JsonObject
arrayPointer.writeJson(jsonArray, "new element");
```

通过提供 `JsonPointerIterator` 的自定义实现,您可以将 Vert.x Json 指针 与任何对象模型一起使用

## Buffers(缓冲区)

大多数数据在 Vert.x 中使用缓冲区进行混洗.

缓冲区是可以读取或写入的零个或多个字节的序列,并根据需要自动扩展以容纳写入其中的任何字节. 您也许可以将缓冲区视为智能字节数组.

### 创建 buffers

缓冲区可以使用静态 `Buffer.buffer` 方法之一创建.

缓冲区可以从字符串或字节数组初始化,也可以创建一个空缓冲区.

以下是创建缓冲区的一些示例:

创建一个新的空缓冲区:

```java
Buffer buff = Buffer.buffer();
```

从字符串创建缓冲区. 字符串将使用 `UTF-8` 在缓冲区中编码.

```java
Buffer buff = Buffer.buffer("some string");
```

从字符串创建缓冲区:字符串将使用指定的编码进行编码,例如:

```java
Buffer buff = Buffer.buffer("some string", "UTF-16");
```

从 `byte[]` 创建一个缓冲区

```java
byte[] bytes = new byte[] {1, 3, 5};
Buffer buff = Buffer.buffer(bytes);
```

创建一个带有初始大小提示的缓冲区. 如果您知道缓冲区将写入一定数量的数据,则可以创建缓冲区并指定此大小. 这使得缓冲区最初分配那么多内存,并且比缓冲区在数据写入时自动调整大小多次更有效.

请注意,以这种方式创建的缓冲区**是空的**. 它不会创建一个由零填充到指定大小的缓冲区.

```java
Buffer buff = Buffer.buffer(10000);
```

### 写入 Buffer

有两种写入缓冲区的方法:追加和随机访问. 在任何一种情况下,缓冲区都将始终自动扩展以包含字节. 使用缓冲区无法获得 `IndexOutOfBoundsException`异常.

#### 追加到 Buffer

要添加到缓冲区,你可以使用`appendXXX`方法.存在用于追加各种不同类型的Append方法.

`appendXXX` 方法的返回值是缓冲区本身,因此可以将它们链接起来:

```java
Buffer buff = Buffer.buffer();

buff.appendInt(123).appendString("hello\n");

socket.write(buff);
```

#### 随机存取Buffer写入

你也可以通过使用`setXXX`方法在特定的索引处写入缓冲区.Set方法适用于各种不同的数据类型.所有set方法都以索引作为第一个参数--它表示缓冲区中开始写入数据的位置.

缓冲区将始终根据需要扩展以容纳数据.

```java
Buffer buff = Buffer.buffer();

buff.setInt(1000, 123);
buff.setString(0, "hello");
```

### 从Buffer读取

使用`getXXX`方法从缓冲区读取数据.存在各种数据类型的Get方法.这些方法的第一个参数是用于获取数据的缓冲区中的索引.

```java
Buffer buff = Buffer.buffer();
for (int i = 0; i < buff.length(); i += 4) {
  System.out.println("int value at " + i + " is " + buff.getInt(i));
}
```

### 处理无符号数字

无符号数可以用`getUnsignedXXX`, `appendUnsignedXXX`和`setUnsignedXXX`方法从缓冲区读取或追加/设置.在为网络协议实现编解码器以最小化带宽消耗时,这是非常有用的.

在下面的例子中,值200被设置在指定的位置,只有一个字节:

```java
Buffer buff = Buffer.buffer(128);
int pos = 15;
buff.setUnsignedByte(pos, (short) 200);
System.out.println(buff.getUnsignedByte(pos));
```

控制台显示"200".

### Buffer 长度

使用`length`获取缓冲区的长度.缓冲区的长度是该缓冲区中索引最大的字节的索引+ 1.

### 复制 buffers

使用 `copy` 复制缓冲区

### Slicing(切片) buffers

切片缓冲区是一个新缓冲区,它指向与原Buffer相同的内存位置(即它不复制底层数据),且仅包含裁剪的元素. 使用 `slice` 创建切片缓冲区.
> **🏷注意:** 修改返回的切片缓冲区或原缓冲区的内容会影响彼此的内容,同时它们维护单独的索引和标记.

### Buffer 重用

将缓冲区写入套接字或其他类似位置后,它们无法重复使用.

## 编写 TCP 服务器和客户端

Vert.x 允许您轻松编写非阻塞 TCP 客户端和服务器.

### 创建 TCP 服务器

使用所有默认选项创建 TCP 服务器的最简单方法如下:

```java
NetServer server = vertx.createNetServer();
```

### 配置 TCP 服务器

如果您不想要默认值,可以通过在创建服务器时传入一个 `NetServerOptions` 实例来配置服务器:

```java
NetServerOptions options = new NetServerOptions().setPort(4321);
NetServer server = vertx.createNetServer(options);
```

### 启动服务器监听

要告诉服务器监听传入的请求,您可以使用`listen`选项之一.

告诉服务器监听选项中指定的主机和端口:

```java
NetServer server = vertx.createNetServer();
server.listen();
```

或者在监听的调用中指定主机和端口,忽略选项中配置的内容:

```java
NetServer server = vertx.createNetServer();
server.listen(1234, "localhost");
```

默认主机是`0.0.0.0`,表示"监听所有可用地址,默认端口是`0`,这是一个特殊值,指示服务器随机查找未使用的本地端口并使用它.

实际的绑定是异步的,因此服务器可能直到**在**监听调用返回后的某个时间才真正监听.

如果您想在服务器实际监听时收到通知,您可以为`listen`调用提供一个处理器. 例如:

```java
NetServer server = vertx.createNetServer();
server.listen(1234, "localhost", res -> {
  if (res.succeeded()) {
    System.out.println("Server is now listening!");
  } else {
    System.out.println("Failed to bind!");
  }
});
```

### 监听随机端口

如果使用`0`作为监听端口,服务器将找到一个未使用的随机端口进行监听.

要找出服务器正在侦听的真实端口,您可以调用`actualPort`.

```java
NetServer server = vertx.createNetServer();
server.listen(0, "localhost", res -> {
  if (res.succeeded()) {
    System.out.println("Server is now listening on actual port: " + server.actualPort());
  } else {
    System.out.println("Failed to bind!");
  }
});
```

### 收到传入连接的通知

要在建立连接时收到通知,您需要设置一个 `connectHandler`:

```java
NetServer server = vertx.createNetServer();
server.connectHandler(socket -> {
  // Handle the connection in here
});
```

建立连接后,将使用`NetSocket`实例调用处理器.

这是一个与实际连接类似的套接字接口,允许您读取和写入数据以及执行各种其他操作,例如关闭套接字.

### 从套接字读取数据

要从套接字读取数据,请在套接字上设置`handler` .

每次在套接字上接收到数据时,都会传递 `Buffer` 的实例调用此处理器.

```java
NetServer server = vertx.createNetServer();
server.connectHandler(socket -> {
  socket.handler(buffer -> {
    System.out.println("I received some bytes: " + buffer.length());
  });
});
```

### 将数据写入套接字

您使用 `write` 之一写入套接字.

```java
Buffer buffer = Buffer.buffer().appendFloat(12.34f).appendInt(123);
socket.write(buffer);

// Write a string in UTF-8 encoding
socket.write("some data");

// Write a string using the specified encoding
socket.write("some data", "UTF-16");
```

写操作是异步的,可能要等到 `write` 调用返回后一段时间才会发生.

### 关闭处理器

如果您想在套接字关闭时收到通知,可以在其上设置一个 `closeHandler`:

```java
socket.closeHandler(v -> {
  System.out.println("The socket has been closed");
});
```

### 处理异常

您可以设置一个 `exceptionHandler` 来接收套接字上发生的任何异常.

您可以设置一个 `exceptionHandler` 来接收在连接传递给 `connectHandler` 之前发生的任何异常,例如在 TLS 握手期间.

### 事件总线写处理器

每个套接字都会自动在事件总线上注册一个处理器,并且当在此处理器中接收到任何缓冲区时,它会将它们写入自己. 这些是未在集群上路由的本地订阅.

这使您可以通过将缓冲区发送到该处理器的地址来将数据写入可能位于完全不同的verticle中的套接字.

处理器的地址由 `writeHandlerID` 给出

### 本地和远程地址

可以使用 `localAddress` 检索`NetSocket` 的本地地址.

可以使用`remoteAddress`检索`NetSocket`的远程地址(即连接另一端的地址).

### 从classpath(类路径)发送文件或资源

文件和类路径资源可以使用 `sendFile` 直接写入套接字. 这可能是一种非常有效的文件发送方式,因为它可以在操作系统支持的情况下由操作系统内核直接处理.

有关类路径解析的限制或禁用它,请参阅关于 [从类路径提供文件](#classpath) 的章节.

```java
socket.sendFile("myfile.dat");
```

### 流式套接字

`NetSocket` 的实例也是 `ReadStream` 和 `WriteStream` 实例,因此它们可用于将数据传送到或读取来自其他的读写流.

有关详细信息,请参阅有关 [streams(流)](#streams) 的章节.

### 升级到 SSL/TLS 的连接

可以使用`upgradeToSsl`将非 SSL/TLS 连接升级到 SSL/TLS.

必须为服务器或客户端配置 SSL/TLS 才能正常工作. 请参阅 [关于 SSL/TLS 的章节](#ssl) 了解更多信息.

### 关闭 TCP 服务器

调用 `close` 关闭服务器. 关闭服务器会关闭所有打开的连接并释放所有服务器资源.

关闭实际上是异步的,可能要等到调用返回后一段时间才能完成. 如果您想在实际关闭完成时收到通知,那么您可以传入一个处理器.

当关闭完全完成时,将调用此处理器.

```java
server.close(res -> {
  if (res.succeeded()) {
    System.out.println("Server is now closed");
  } else {
    System.out.println("close failed");
  }
});
```

### 自动清理 verticles

如果您从 Verticle 内部创建 TCP 服务器和客户端,则在取消部署 Verticle 时,这些服务器和客户端将自动关闭.

### 伸缩性 - 共享 TCP 服务器

任何 TCP 服务器的处理器总是在同一个事件循环线程上执行.

这意味着,如果您在具有大量内核的服务器上运行,并且您只部署了这个实例,那么您的服务器上最多会使用一个内核.

为了利用服务器的更多核心,您需要部署更多服务器实例.

您可以在代码中以编程方式实例化更多实例:

```java
for (int i = 0; i < 10; i++) {
  NetServer server = vertx.createNetServer();
  server.connectHandler(socket -> {
    socket.handler(buffer -> {
      // Just echo back the data
      socket.write(buffer);
    });
  });
  server.listen(1234, "localhost");
}
```

或者,如果您使用的是 verticles,您可以使用命令行上的 `-instances` 选项简单地部署更多服务器verticle实例:

`vertx run com.mycompany.MyVerticle -instances 10`

或者在以编程方式部署您的verticle时:

```java
DeploymentOptions options = new DeploymentOptions().setInstances(10);
vertx.deployVerticle("com.mycompany.MyVerticle", options);
```

完成此操作后,您会发现 echo 服务器在功能上与以前相同,但您服务器上的所有内核都可以使用,并且可以处理更多工作.

此时您可能会问自己: **"如何让多个服务器在同一主机和端口上侦听? 当您尝试部署多个实例时,您肯定会遇到端口冲突吗?"**

**Vert.x 在这里做了一点魔法.**

当您在与现有服务器相同的主机和端口上部署另一台服务器时,它实际上并没有尝试创建一个在同一主机/端口上侦听的新服务器.

相反,它在内部只维护一个服务器,并且当传入连接到达时,它以循环方式将它们分发给任何连接处理器.

因此,Vert.x TCP 服务器可以扩展可用内核,同时每个实例保持单线程.

### 创建 TCP 客户端

使用所有默认选项创建 TCP 客户端的最简单方法如下:

```java
NetClient client = vertx.createNetClient();
```

### 配置TCP客户端

如果您不想要默认值,可以在创建客户端时通过传入 `NetClientOptions` 实例来配置客户端:

```java
NetClientOptions options = new NetClientOptions().setConnectTimeout(10000);
NetClient client = vertx.createNetClient(options);
```

### 建立连接

要与服务器建立连接,请使用`connect`,指定服务器的端口和主机以及一个处理器,当连接成功或连接失败时,将调用包含`NetSocket`的结果的处理器.

```java
NetClientOptions options = new NetClientOptions().setConnectTimeout(10000);
NetClient client = vertx.createNetClient(options);
client.connect(4321, "localhost", res -> {
  if (res.succeeded()) {
    System.out.println("Connected!");
    NetSocket socket = res.result();
  } else {
    System.out.println("Failed to connect: " + res.cause().getMessage());
  }
});
```

### 配置连接尝试

客户端可以配置为在无法连接时自动重试连接到服务器. 这是用 `setReconnectInterval` 和 `setReconnectAttempts` 配置的.

> **🏷注意:** 目前,如果连接失败,Vert.x 不会尝试重新连接,重新连接尝试和间隔仅适用于创建初始连接.

```java
NetClientOptions options = new NetClientOptions().
  setReconnectAttempts(10).
  setReconnectInterval(500);

NetClient client = vertx.createNetClient(options);
```

默认情况下,禁用多次连接尝试.

<a name="logging_network_activity"></a>
### 记录网络活动

出于调试目的,可以记录网络活动:

```java
NetServerOptions options = new NetServerOptions().setLogActivity(true);

NetServer server = vertx.createNetServer(options);
```

对于客户端

```java
NetClientOptions options = new NetClientOptions().setLogActivity(true);

NetClient client = vertx.createNetClient(options);
```

Netty 使用 `DEBUG` 级别和 `io.netty.handler.logging.LoggingHandler` 名称记录网络活动. 使用网络活动日志记录时,需要牢记以下几点:

- 日志不是由 Vert.x 日志执行的,而是由 Netty 执行的
- 这**不是**一个产品特性

You should read the [Netty logging](#netty-logging) section.

<a name="ssl"></a>
<a name="ssl"></a>
### 配置服务器和客户端以使用 SSL/TLS

TCP 客户端和服务器可以配置为使用 [传输层安全性](https://en.wikipedia.org/wiki/Transport_Layer_Security) - TLS 的早期版本被称为 SSL.

无论是否使用 SSL/TLS,服务器和客户端的 API 都是相同的,并且通过配置用于创建服务器或客户端的 `NetClientOptions` 或 `NetServerOptions` 实例来启用它.

#### 在服务器上启用 SSL/TLS

SSL/TLS 通过 `ssl` 启用.

默认情况下它被禁用.

#### 为服务器指定密钥/证书

SSL/TLS 服务器通常向客户端提供证书,以便向客户端验证其身份.

可以通过多种方式为服务器配置证书/密钥:

第一种方法是指定包含证书和私钥的 Java 密钥库的位置.

Java 密钥库可以使用 JDK 附带的 [keytool](https://docs.oracle.com/javase/6/docs/technotes/tools/solaris/keytool.html) 实用程序进行管理.

还应提供密钥库的密码:

```java
NetServerOptions options = new NetServerOptions().setSsl(true).setKeyStoreOptions(
  new JksOptions().
    setPath("/path/to/your/server-keystore.jks").
    setPassword("password-of-your-keystore")
);
NetServer server = vertx.createNetServer(options);
```

或者,你可以自己读取密钥存储作为一个缓冲区,并直接提供:

```java
Buffer myKeyStoreAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/server-keystore.jks");
JksOptions jksOptions = new JksOptions().
  setValue(myKeyStoreAsABuffer).
  setPassword("password-of-your-keystore");
NetServerOptions options = new NetServerOptions().
  setSsl(true).
  setKeyStoreOptions(jksOptions);
NetServer server = vertx.createNetServer(options);
```

`PKCS#12` 格式的密钥/证书([http://en.wikipedia.org/wiki/PKCS_12](https://en.wikipedia.org/wiki/PKCS_12)),通常使用 `.pfx` 或者 `.p12` 扩展名也可以以与 JKS 密钥存储类似的方式加载:

```java
NetServerOptions options = new NetServerOptions().setSsl(true).setPfxKeyCertOptions(
  new PfxOptions().
    setPath("/path/to/your/server-keystore.pfx").
    setPassword("password-of-your-keystore")
);
NetServer server = vertx.createNetServer(options);
```

还支持缓冲区配置:

```java
Buffer myKeyStoreAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/server-keystore.pfx");
PfxOptions pfxOptions = new PfxOptions().
  setValue(myKeyStoreAsABuffer).
  setPassword("password-of-your-keystore");
NetServerOptions options = new NetServerOptions().
  setSsl(true).
  setPfxKeyCertOptions(pfxOptions);
NetServer server = vertx.createNetServer(options);
```

另一种提供服务器私钥和证书的方式,是单独使用`.pem`文件.

```java
NetServerOptions options = new NetServerOptions().setSsl(true).setPemKeyCertOptions(
  new PemKeyCertOptions().
    setKeyPath("/path/to/your/server-key.pem").
    setCertPath("/path/to/your/server-cert.pem")
);
NetServer server = vertx.createNetServer(options);
```

还支持缓冲区配置:

```java
Buffer myKeyAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/server-key.pem");
Buffer myCertAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/server-cert.pem");
PemKeyCertOptions pemOptions = new PemKeyCertOptions().
  setKeyValue(myKeyAsABuffer).
  setCertValue(myCertAsABuffer);
NetServerOptions options = new NetServerOptions().
  setSsl(true).
  setPemKeyCertOptions(pemOptions);
NetServer server = vertx.createNetServer(options);
```

Vert.x 支持从 PKCS8 PEM 文件中读取未加密的 RSA 和/或基于 ECC 的私钥. 基于 RSA 的私钥也可以从 PKCS1 PEM 文件中读取. X.509 证书可以从 PEM 文件中读取,该文件包含 [RFC 7468,第 5 节](https://tools.ietf.org/html/rfc7468#section-5) 定义的证书文本编码.

> **☢警告:** 请记住,任何可以读取文件的人都可以提取未加密的 PKCS8 或 PKCS1 PEM 文件中包含的密钥. 因此,请确保对此类 PEM 文件设置适当的访问限制,以防止滥用.

最后,您还可以加载通用 Java 密钥库,这对于使用其他 KeyStore 实现(如 Bouncy Castle)很有用:

```java
NetServerOptions options = new NetServerOptions().setSsl(true).setKeyCertOptions(
  new KeyStoreOptions().
    setType("BKS").
    setPath("/path/to/your/server-keystore.bks").
    setPassword("password-of-your-keystore")
);
NetServer server = vertx.createNetServer(options);
```

#### 为服务器指定信任

SSL/TLS 服务器可以使用证书颁发机构来验证客户端的身份.

可以通过多种方式为服务器配置证书颁发机构:

Java 信任库可以使用 JDK 附带的 [keytool](https://docs.oracle.com/javase/6/docs/technotes/tools/solaris/keytool.html) 实用程序进行管理.

还需提供信任库的密码:

```java
NetServerOptions options = new NetServerOptions().
  setSsl(true).
  setClientAuth(ClientAuth.REQUIRED).
  setTrustStoreOptions(
    new JksOptions().
      setPath("/path/to/your/truststore.jks").
      setPassword("password-of-your-truststore")
  );
NetServer server = vertx.createNetServer(options);
```

或者,您可以自己读取信任存储作为缓冲区并直接提供:

```java
Buffer myTrustStoreAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/truststore.jks");
NetServerOptions options = new NetServerOptions().
  setSsl(true).
  setClientAuth(ClientAuth.REQUIRED).
  setTrustStoreOptions(
    new JksOptions().
      setValue(myTrustStoreAsABuffer).
      setPassword("password-of-your-truststore")
  );
NetServer server = vertx.createNetServer(options);
```

`PKCS#12` 格式的证书颁发机构([http://en.wikipedia.org/wiki/PKCS_12](https://en.wikipedia.org/wiki/PKCS_12)),通常使用 `.pfx` 或 `.p12` 扩展名也可以以与 JKS 信任存储类似的方式加载:

```java
NetServerOptions options = new NetServerOptions().
  setSsl(true).
  setClientAuth(ClientAuth.REQUIRED).
  setPfxTrustOptions(
    new PfxOptions().
      setPath("/path/to/your/truststore.pfx").
      setPassword("password-of-your-truststore")
  );
NetServer server = vertx.createNetServer(options);
```

还支持缓冲区配置:

```java
Buffer myTrustStoreAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/truststore.pfx");
NetServerOptions options = new NetServerOptions().
  setSsl(true).
  setClientAuth(ClientAuth.REQUIRED).
  setPfxTrustOptions(
    new PfxOptions().
      setValue(myTrustStoreAsABuffer).
      setPassword("password-of-your-truststore")
  );
NetServer server = vertx.createNetServer(options);
```

使用列表`.pem`文件提供服务器证书授权的另一种方法.

```java
NetServerOptions options = new NetServerOptions().
  setSsl(true).
  setClientAuth(ClientAuth.REQUIRED).
  setPemTrustOptions(
    new PemTrustOptions().
      addCertPath("/path/to/your/server-ca.pem")
  );
NetServer server = vertx.createNetServer(options);
```

还支持缓冲区配置:

```java
Buffer myCaAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/server-ca.pfx");
NetServerOptions options = new NetServerOptions().
  setSsl(true).
  setClientAuth(ClientAuth.REQUIRED).
  setPemTrustOptions(
    new PemTrustOptions().
      addCertValue(myCaAsABuffer)
  );
NetServer server = vertx.createNetServer(options);
```

#### 在客户端上启用 SSL/TLS

Net 客户端也可以轻松配置为使用 SSL. 它们在使用 SSL 时与使用标准套接字时具有完全相同的 API.

要在 NetClient 上启用 SSL,调用函数 `setSSL(true)`.

#### 客户端信任配置

如果客户端上的`trustALL`设置为 `true`,则客户端将信任所有服务器证书. 连接仍将被加密,但这种模式容易受到"中间人"攻击. IE. 你不能确定你在连接谁. 请谨慎使用. 默认值为`false`.

```java
NetClientOptions options = new NetClientOptions().
  setSsl(true).
  setTrustAll(true);
NetClient client = vertx.createNetClient(options);
```

如果未设置`trustAll`,则必须配置客户端信任库,并且应包含客户端信任的服务器的证书.

默认情况下,客户端上禁用主机验证. 要启用主机验证,请将算法设置为在您的客户端上使用(目前仅支持 HTTPS 和 LDAPS):

```java
NetClientOptions options = new NetClientOptions().
  setSsl(true).
  setHostnameVerificationAlgorithm("HTTPS");
NetClient client = vertx.createNetClient(options);
```

与服务器配置类似,客户端信任可以通过多种方式配置:

第一种方法是指定包含证书颁发机构的 Java 信任库的位置.

它只是一个标准的 Java 密钥库,与服务器端的密钥库相同. 客户端信任存储位置是通过使用 `jks options` 上的函数 `path` 设置的. 如果服务器在连接期间提供不在客户端信任库中的证书,则连接尝试将不会成功.

```java
NetClientOptions options = new NetClientOptions().
  setSsl(true).
  setTrustStoreOptions(
    new JksOptions().
      setPath("/path/to/your/truststore.jks").
      setPassword("password-of-your-truststore")
  );
NetClient client = vertx.createNetClient(options);
```

还支持缓冲区配置:

```java
Buffer myTrustStoreAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/truststore.jks");
NetClientOptions options = new NetClientOptions().
  setSsl(true).
  setTrustStoreOptions(
    new JksOptions().
      setValue(myTrustStoreAsABuffer).
      setPassword("password-of-your-truststore")
  );
NetClient client = vertx.createNetClient(options);
```

`PKCS#12` 格式的证书颁发机构([http://en.wikipedia.org/wiki/PKCS_12](https://en.wikipedia.org/wiki/PKCS_12)),通常使用 `.pfx` 或 `.p12` 扩展名也可以以与 JKS 信任存储类似的方式加载:

```java
NetClientOptions options = new NetClientOptions().
  setSsl(true).
  setPfxTrustOptions(
    new PfxOptions().
      setPath("/path/to/your/truststore.pfx").
      setPassword("password-of-your-truststore")
  );
NetClient client = vertx.createNetClient(options);
```

还支持缓冲区配置:

```java
Buffer myTrustStoreAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/truststore.pfx");
NetClientOptions options = new NetClientOptions().
  setSsl(true).
  setPfxTrustOptions(
    new PfxOptions().
      setValue(myTrustStoreAsABuffer).
      setPassword("password-of-your-truststore")
  );
NetClient client = vertx.createNetClient(options);
```

使用列表`.pem`文件提供服务器证书授权的另一种方法.

```java
NetClientOptions options = new NetClientOptions().
  setSsl(true).
  setPemTrustOptions(
    new PemTrustOptions().
      addCertPath("/path/to/your/ca-cert.pem")
  );
NetClient client = vertx.createNetClient(options);
```

还支持缓冲区配置:

```java
Buffer myTrustStoreAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/ca-cert.pem");
NetClientOptions options = new NetClientOptions().
  setSsl(true).
  setPemTrustOptions(
    new PemTrustOptions().
      addCertValue(myTrustStoreAsABuffer)
  );
NetClient client = vertx.createNetClient(options);
```

#### 为客户端指定密钥/证书

如果服务器需要客户端身份验证,则客户端在连接时必须向服务器出示自己的证书. 客户端可以通过多种方式进行配置:

第一种方法是指定包含密钥和证书的 Java 密钥库的位置. 同样,它只是一个普通的 Java 密钥库. 客户端密钥库位置是通过使用 `jks options` 上的函数 `path` 设置的.

```java
NetClientOptions options = new NetClientOptions().setSsl(true).setKeyStoreOptions(
  new JksOptions().
    setPath("/path/to/your/client-keystore.jks").
    setPassword("password-of-your-keystore")
);
NetClient client = vertx.createNetClient(options);
```

还支持缓冲区配置:

```java
Buffer myKeyStoreAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/client-keystore.jks");
JksOptions jksOptions = new JksOptions().
  setValue(myKeyStoreAsABuffer).
  setPassword("password-of-your-keystore");
NetClientOptions options = new NetClientOptions().
  setSsl(true).
  setKeyStoreOptions(jksOptions);
NetClient client = vertx.createNetClient(options);
```

`PKCS#12` 格式的密钥/证书([http://en.wikipedia.org/wiki/PKCS_12](https://en.wikipedia.org/wiki/PKCS_12)),通常使用 `.pfx` 或者 `.p12` 扩展名也可以以与 JKS 密钥存储类似的方式加载:

```java
NetClientOptions options = new NetClientOptions().setSsl(true).setPfxKeyCertOptions(
  new PfxOptions().
    setPath("/path/to/your/client-keystore.pfx").
    setPassword("password-of-your-keystore")
);
NetClient client = vertx.createNetClient(options);
```

还支持缓冲区配置:

```java
Buffer myKeyStoreAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/client-keystore.pfx");
PfxOptions pfxOptions = new PfxOptions().
  setValue(myKeyStoreAsABuffer).
  setPassword("password-of-your-keystore");
NetClientOptions options = new NetClientOptions().
  setSsl(true).
  setPfxKeyCertOptions(pfxOptions);
NetClient client = vertx.createNetClient(options);
```

另一种使用`.pem`文件分别提供服务器私钥和证书的方法.

```java
NetClientOptions options = new NetClientOptions().setSsl(true).setPemKeyCertOptions(
  new PemKeyCertOptions().
    setKeyPath("/path/to/your/client-key.pem").
    setCertPath("/path/to/your/client-cert.pem")
);
NetClient client = vertx.createNetClient(options);
```

还支持缓冲区配置:

```java
Buffer myKeyAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/client-key.pem");
Buffer myCertAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/client-cert.pem");
PemKeyCertOptions pemOptions = new PemKeyCertOptions().
  setKeyValue(myKeyAsABuffer).
  setCertValue(myCertAsABuffer);
NetClientOptions options = new NetClientOptions().
  setSsl(true).
  setPemKeyCertOptions(pemOptions);
NetClient client = vertx.createNetClient(options);
```

请记住,在pem配置中,私钥是不加密的.

#### 用于测试和开发目的的自签名证书

> **⚠小心:** 不要在生产设置中使用它,并注意生成的密钥非常不安全.

通常需要自签名证书,无论是用于单元/集成测试还是运行应用程序的开发版本.

`SelfSignedCertificate`可以用来提供自签名的PEM证书助手,并提供`KeyCertOptions` 和 `TrustOptions`配置:

```java
SelfSignedCertificate certificate = SelfSignedCertificate.create();

NetServerOptions serverOptions = new NetServerOptions()
  .setSsl(true)
  .setKeyCertOptions(certificate.keyCertOptions())
  .setTrustOptions(certificate.trustOptions());

vertx.createNetServer(serverOptions)
  .connectHandler(socket -> socket.end(Buffer.buffer("Hello!")))
  .listen(1234, "localhost");

NetClientOptions clientOptions = new NetClientOptions()
  .setSsl(true)
  .setKeyCertOptions(certificate.keyCertOptions())
  .setTrustOptions(certificate.trustOptions());

NetClient client = vertx.createNetClient(clientOptions);
client.connect(1234, "localhost", ar -> {
  if (ar.succeeded()) {
    ar.result().handler(buffer -> System.out.println(buffer));
  } else {
    System.err.println("Woops: " + ar.cause().getMessage());
  }
});
```

客户端也可以配置为信任所有证书:

```java
NetClientOptions clientOptions = new NetClientOptions()
  .setSsl(true)
  .setTrustAll(true);
```

请注意,自签名证书也适用于 HTTPS 等其他 TCP 协议:

```java
SelfSignedCertificate certificate = SelfSignedCertificate.create();

vertx.createHttpServer(new HttpServerOptions()
  .setSsl(true)
  .setKeyCertOptions(certificate.keyCertOptions())
  .setTrustOptions(certificate.trustOptions()))
  .requestHandler(req -> req.response().end("Hello!"))
  .listen(8080);
```

#### 撤销证书颁发机构

可以将信任配置为使用证书吊销列表 (CRL) 来处理不应再受信任的吊销证书. `crlPath` 配置 crl 列表以使用:

```java
NetClientOptions options = new NetClientOptions().
  setSsl(true).
  setTrustStoreOptions(trustOptions).
  addCrlPath("/path/to/your/crl.pem");
NetClient client = vertx.createNetClient(options);
```

还支持缓冲区配置:

```java
Buffer myCrlAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/crl.pem");
NetClientOptions options = new NetClientOptions().
  setSsl(true).
  setTrustStoreOptions(trustOptions).
  addCrlValue(myCrlAsABuffer);
NetClient client = vertx.createNetClient(options);
```

#### 配置加密套件

默认情况下,TLS配置将使用SSL引擎的Cipher套件列表:

- 使用 `JdkSSLEngineOptions` 时的 JDK SSL 引擎
- 使用 `OpenSSLEngineOptions` 时的 OpenSSL 引擎

此Cipher套件可以使用一组启用的密码进行配置:

```java
NetServerOptions options = new NetServerOptions().
  setSsl(true).
  setKeyStoreOptions(keyStoreOptions).
  addEnabledCipherSuite("ECDHE-RSA-AES128-GCM-SHA256").
  addEnabledCipherSuite("ECDHE-ECDSA-AES128-GCM-SHA256").
  addEnabledCipherSuite("ECDHE-RSA-AES256-GCM-SHA384").
  addEnabledCipherSuite("CDHE-ECDSA-AES256-GCM-SHA384");
NetServer server = vertx.createNetServer(options);
```

当启用的密码套件被定义(即非空)时,它优先于 SSL 引擎的默认密码套件.

密码套件可以在 `NetServerOptions` 或 `NetClientOptions` 配置中指定.

#### 配置 TLS 协议版本

默认情况下,TLS配置将使用如下协议版本:`SSLv2Hello`,`TLSv1`,T`LSv1.1`和`TLSv1.2`.协议版本可以通过显式添加启用的协议来配置:

```java
NetServerOptions options = new NetServerOptions().
  setSsl(true).
  setKeyStoreOptions(keyStoreOptions).
  removeEnabledSecureTransportProtocol("TLSv1").
  addEnabledSecureTransportProtocol("TLSv1.3");
NetServer server = vertx.createNetServer(options);
```

协议版本可以在 `NetServerOptions` 或 `NetClientOptions` 配置中指定.

#### SSL 引擎

引擎实现可以配置为使用 [OpenSSL](https://www.openssl.org/) 而不是 JDK 实现. OpenSSL 提供比 JDK 引擎更好的性能和 CPU 使用率,以及 JDK 版本独立性.

要使用的引擎选项是

- 设置时的 `getSslEngineOptions` 选项
- 否则`JdkSSLEngineOptions`

```java
NetServerOptions options = new NetServerOptions().
  setSsl(true).
  setKeyStoreOptions(keyStoreOptions);

// Use JDK SSL engine explicitly
options = new NetServerOptions().
  setSsl(true).
  setKeyStoreOptions(keyStoreOptions).
  setJdkSslEngineOptions(new JdkSSLEngineOptions());

// Use OpenSSL engine
options = new NetServerOptions().
  setSsl(true).
  setKeyStoreOptions(keyStoreOptions).
  setOpenSslEngineOptions(new OpenSSLEngineOptions());
```

#### 服务器名称指示 (SNI)

服务器名称指示 (SNI) 是 TLS 扩展,客户端通过它指定尝试连接的主机名:在 TLS 握手期间,客户端提供服务器名称,服务器可以使用它来响应此服务器名称的特定证书,而不是 默认部署的证书. 如果服务器需要客户端身份验证,则服务器可以使用特定的受信任 CA 证书,具体取决于指定的服务器名称.

当 SNI 处于活动状态时,服务器使用

- 证书 CN 或 SAN DNS(主题备用名称与 DNS)进行完全匹配,例如`www.example.com`
- 证书 CN 或 SAN DNS 证书以匹配通配符名称,例如 `*.example.com`
- 否则客户端不提供服务器名称或提供的服务器名称无法匹配时的第一个证书

当服务器另外需要客户端认证时:

- 如果 `JksOptions` 用于设置信任选项(`options`),则与信任库别名完全匹配
- 否则,可用的 CA 证书的使用方式与没有 SNI 的方式相同

您可以通过将`setSni`设置为`true`并为服务器配置多个密钥/证书对来启用服务器上的 SNI.

Java KeyStore 文件或 PKCS12 文件可以存储多个开箱即用的密钥/证书对.

```java
JksOptions keyCertOptions = new JksOptions().setPath("keystore.jks").setPassword("wibble");

NetServer netServer = vertx.createNetServer(new NetServerOptions()
    .setKeyStoreOptions(keyCertOptions)
    .setSsl(true)
    .setSni(true)
);
```

`PemKeyCertOptions` 可以配置为保存多个条目:

```java
PemKeyCertOptions keyCertOptions = new PemKeyCertOptions()
    .setKeyPaths(Arrays.asList("default-key.pem", "host1-key.pem", "etc..."))
    .setCertPaths(Arrays.asList("default-cert.pem", "host2-key.pem", "etc...")
    );

NetServer netServer = vertx.createNetServer(new NetServerOptions()
    .setPemKeyCertOptions(keyCertOptions)
    .setSsl(true)
    .setSni(true)
);
```

客户端将连接主机作为完全限定域名 (FQDN) 的 SNI 服务器名称隐式发送.

您可以在连接套接字时提供明确的服务器名称

```java
NetClient client = vertx.createNetClient(new NetClientOptions()
    .setTrustStoreOptions(trustOptions)
    .setSsl(true)
);

// Connect to 'localhost' and present 'server.name' server name
client.connect(1234, "localhost", "server.name", res -> {
  if (res.succeeded()) {
    System.out.println("Connected!");
    NetSocket socket = res.result();
  } else {
    System.out.println("Failed to connect: " + res.cause().getMessage());
  }
});
```

它可以用于不同的目的:

- 提供与服务器主机不同的服务器名称
- 在连接到 IP 时显示服务器名称
- 使用短名称时强制显示服务器名称

#### 应用层协议协商 (ALPN)

应用层协议协商 (ALPN) 是应用层协议协商的 TLS 扩展. 它被 HTTP/2 使用:在 TLS 握手期间,客户端给出它接受的应用程序协议列表,服务器用它支持的协议进行响应.

Java TLS 支持 ALPN(最新版本的 Java 8).

##### OpenSSL ALPN 支持

OpenSSL 还支持(本机)ALPN.

OpenSSL 需要配置 `setOpenSslEngineOptions` 并在类路径上使用 [netty-tcnative](http://netty.io/wiki/forked-tomcat-native.html) jar. 使用 tcnative 可能需要在您的操作系统上安装 OpenSSL,具体取决于 tcnative 实现.

### 使用代理进行客户端连接

`NetClient` 支持 HTTP/1.x *CONNECT*,*SOCKS4a* 或 *SOCKS5* 代理.

可以通过设置包含代理类型,主机名,端口以及可选的用户名和密码的 `ProxyOptions` 对象在 `NetClientOptions` 中配置代理.

这是一个例子:

```java
NetClientOptions options = new NetClientOptions()
  .setProxyOptions(new ProxyOptions().setType(ProxyType.SOCKS5)
    .setHost("localhost").setPort(1080)
    .setUsername("username").setPassword("secret"));
NetClient client = vertx.createNetClient(options);
```

DNS解析总是在代理服务器上完成,要实现SOCKS4客户端的功能,需要在本地解析DNS地址.

您可以使用 `setNonProxyHosts` 来配置绕过代理的主机列表. 列表接受 `*` 通配符来匹配域:

```java
NetClientOptions options = new NetClientOptions()
  .setProxyOptions(new ProxyOptions().setType(ProxyType.SOCKS5)
    .setHost("localhost").setPort(1080)
    .setUsername("username").setPassword("secret"))
  .addNonProxyHost("*.foo.com")
  .addNonProxyHost("localhost");
NetClient client = vertx.createNetClient(options);
```

### 使用 HA 代理协议

[HA PROXY 协议](https://www.haproxy.org/download/1.8/doc/proxy-protocol.txt) 提供了一种方便的方法来安全地传输连接信息,例如跨多个 NAT 或 TCP 代理层的客户端地址 .

可以通过设置选项 `setUseProxyProtocol` 并在类路径中添加以下依赖项来启用 HA PROXY 协议:

```xml
<dependency>
 <groupId>io.netty</groupId>
 <artifactId>netty-codec-haproxy</artifactId>
 <!--<version>Should align with netty version that Vert.x uses</version>-->
</dependency>
NetServerOptions options = new NetServerOptions().setUseProxyProtocol(true);
NetServer server = vertx.createNetServer(options);
server.connectHandler(so -> {
  // Print the actual client address provided by the HA proxy protocol instead of the proxy address
  System.out.println(so.remoteAddress());

  // Print the address of the proxy
  System.out.println(so.localAddress());
});
```

## 编写 HTTP 服务器和客户端

Vert.x 允许您轻松编写非阻塞 HTTP 客户端和服务器.

Vert.x 支持 HTTP/1.0,HTTP/1.1 和 HTTP/2 协议.

HTTP 的基本 API 与 HTTP/1.x 和 HTTP/2 相同,特定的 API 功能可用于处理 HTTP/2 协议.

### 创建 HTTP 服务器

使用所有默认选项创建 HTTP 服务器的最简单方法如下:

```java
HttpServer server = vertx.createHttpServer();
```

### 配置 HTTP 服务器

如果您不想要默认值,可以通过在创建服务器时传入一个 `HttpServerOptions` 实例来配置服务器:

```java
HttpServerOptions options = new HttpServerOptions().setMaxWebSocketFrameSize(1000000);

HttpServer server = vertx.createHttpServer(options);
```

### 配置 HTTP/2 服务器

Vert.x 通过 TLS `h2` 和 TCP `h2c` 支持 HTTP/2.

- `h2` 标识 HTTP/2 协议在通过 [应用层协议协商](https://en.wikipedia.org/wiki/Application-Layer_Protocol_Negotiation) (ALPN) 协商的 TLS 上使用
- `h2c` 在 TCP 上以明文形式使用时标识 HTTP/2 协议,此类连接是通过 HTTP/1.1 升级请求或直接建立的

要处理 `h2` 请求,TLS 必须与 `setUseAlpn` 一起启用:

```java
HttpServerOptions options = new HttpServerOptions()
    .setUseAlpn(true)
    .setSsl(true)
    .setKeyStoreOptions(new JksOptions().setPath("/path/to/my/keystore"));

HttpServer server = vertx.createHttpServer(options);
```

ALPN 是一种 TLS 扩展,它在客户端和服务器开始交换数据之前协商协议.

不支持 ALPN 的客户端仍然可以进行*经典* SSL 握手.

ALPN 通常会同意 `h2` 协议,尽管如果服务器或客户端决定使用 `http/1.1` 可以使用.

要处理 `h2c` 请求,必须禁用 TLS,服务器将升级到 HTTP/2 任何想要升级到 HTTP/2 的 HTTP/1.1 请求. 它还将接受以 `PRI * HTTP/2.0\r\nSM\r\n` 前言开头的直接 `h2c` 连接.

> **☢警告:** 大多数浏览器不支持 `h2c`,因此对于服务网站,您应该使用 `h2` 而不是 `h2c`.

当服务器接受 HTTP/2 连接时,它会将其"初始设置"发送给客户端. 这些设置定义了客户端如何使用连接,服务器的默认初始设置是:

- `getMaxConcurrentStreams`:HTTP/2 RFC 推荐的 `100`
- 其他的默认 HTTP/2 设置值

### 记录网络服务器活动

出于调试目的,可以记录网络活动.

```java
HttpServerOptions options = new HttpServerOptions().setLogActivity(true);

HttpServer server = vertx.createHttpServer(options);
```

有关详细说明,请参阅有关 [记录网络活动](#logging_network_activity) 的章节.

### 启动服务器监听

要告诉服务器监听传入的请求,您可以使用`listen`选项之一.

告诉服务器监听选项中指定的主机和端口:

```java
HttpServer server = vertx.createHttpServer();
server.listen();
```

或者在监听的调用中指定主机和端口,忽略选项中配置的内容:

```java
HttpServer server = vertx.createHttpServer();
server.listen(8080, "myhost.com");
```

默认主机是`0.0.0.0`,意思是"监听所有可用地址",默认端口是`80`.

实际的绑定是异步的,因此服务器可能直到**在**调用监听返回后的某个时间才真正在监听.

如果您想在服务器实际监听时收到通知,您可以为`listen`调用提供一个处理器. 例如:

```java
HttpServer server = vertx.createHttpServer();
server.listen(8080, "myhost.com", res -> {
  if (res.succeeded()) {
    System.out.println("Server is now listening!");
  } else {
    System.out.println("Failed to bind!");
  }
});
```

### 收到传入请求的通知

要在请求到达时收到通知,您需要设置一个`requestHandler`:

```java
HttpServer server = vertx.createHttpServer();
server.requestHandler(request -> {
  // Handle the request in here
});
```

### 处理请求

当请求到达时,调用请求处理器并传入一个 `HttpServerRequest` 的实例. 此对象表示服务器端 HTTP 请求.

当请求的标头已被完全读取时,将调用处理器.

如果请求包含正文,则该正文将在调用请求处理器后的某个时间到达服务器.

服务器 request  对象允许您检索 `uri`,`path`,`params` 和 `headers` 等.

每个服务器请求对象都与一个服务器响应对象相关联. 您使用 `response` 来获取对 `HttpServerResponse` 对象的引用.

这是一个服务器处理请求并用"hello world"回复它的简单示例.

```java
vertx.createHttpServer().requestHandler(request -> {
  request.response().end("Hello world");
}).listen(8080);
```

#### Request version(版本)

可以使用 `version` 检索请求中指定的 HTTP 版本

#### Request method(方法)

使用 `method` 检索请求的 HTTP 方法. (即是否是 GET,POST,PUT,DELETE,HEAD,OPTIONS 等).

#### Request URI

使用 `uri` 检索请求的 URI.

请注意,这是在 HTTP 请求中传递的实际 URI,它几乎总是一个相对 URI.

URI 定义在 [HTTP 规范的第 5.1.2 节 - Request-URI](https://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html)

#### Request path(路径)

使用 `path` 返回 URI 的路径部分

例如,如果请求 URI 是 `a/b/c/page.html?param1=abc&param2=xyz`

那么路径将是`/a/b/c/page.html`

#### Request query(查询)

使用 `query` 返回 URI 的查询部分

For example, if the request URI was `a/b/c/page.html?param1=abc&param2=xyz`

Then the query would be `param1=abc&param2=xyz`

#### Request headers(标头)

使用 `headers` 返回 HTTP 请求的标头.

这将返回一个 `MultiMap` 的实例--它类似于一个普通的 Map 或 Hash,但允许同一个键有多个值--这是因为 HTTP 允许具有相同键的多个标头值.

它还具有不区分大小写的键,这意味着您可以执行以下操作:

```java
MultiMap headers = request.headers();

// Get the User-Agent:
System.out.println("User agent is " + headers.get("user-agent"));

// You can also do this and get the same result:
System.out.println("User agent is " + headers.get("User-Agent"));
```

#### Request host(主机)

使用 `host` 返回 HTTP 请求的主机.

对于 HTTP/1.x 请求,返回 `host` 标头,对于 HTTP/1 请求,返回 `:authority` 伪标头.

#### Request parameters(参数)

使用 `params` 返回 HTTP 请求的参数.

就像 `headers` 一样,这会返回 `MultiMap` 的一个实例,因为可以有多个具有相同名称的参数.

请求参数在路径之后的请求 URI 上发送. 例如,如果 URI 是 `/page.html?param1=abc&param2=xyz`

然后参数将包含以下内容:

```
param1: 'abc'
param2: 'xyz
```

请注意,这些请求参数是从请求的 URL 中检索的. 如果您的表单属性已作为提交 HTML 表单的一部分发送到"multi-part/form-data"请求的正文中,那么它们将不会出现在此处的参数中.

#### 远程地址

可以使用 `remoteAddress` 检索请求发送者的地址.

#### 绝对 URI

在 HTTP 请求中传递的 URI 通常是相对的. 如果你想获取请求对应的绝对 URI,你可以通过 `absoluteURI` 获取

#### End handler(结束 处理器)

当整个请求(包括任何正文)已被完全读取时,将调用请求的 `endHandler`.

#### 从 Request Body 中读取数据

通常一个 HTTP 请求包含我们想要读取的正文. 如前所述,当只有请求的标头到达时调用请求处理器,因此请求对象此时没有正文.

这是因为主体可能非常大(例如文件上传),我们通常不希望在将其交给您之前将整个主体缓冲在内存中,因为这可能会导致服务器耗尽可用内存.

要接收正文,您可以在请求上使用`handler`,每次请求正文的一部分到达时都会调用它. 这是一个例子:

```java
request.handler(buffer -> {
  System.out.println("I have received a chunk of the body of length " + buffer.length());
});
```

传入处理器的对象是一个 `Buffer`,当数据从网络到达时,可以多次调用处理器,具体取决于主体的大小.

在某些情况下(例如,如果主体很小),您可能希望在内存中聚合整个主体,因此您可以自己进行聚合,如下所示:

```java
Buffer totalBuffer = Buffer.buffer();

request.handler(buffer -> {
  System.out.println("I have received a chunk of the body of length " + buffer.length());
  totalBuffer.appendBuffer(buffer);
});

request.endHandler(v -> {
  System.out.println("Full body received, length = " + totalBuffer.length());
});
```

这是很常见的情况,Vert.x 提供了一个 `bodyHandler` 来为您执行此操作. 收到所有正文后,将调用一次正文处理器:

```java
request.bodyHandler(totalBuffer -> {
  System.out.println("Full body received, length = " + totalBuffer.length());
});
```

#### Streaming requests(流式传输请求)

请求对象是一个`ReadStream`,因此您可以将请求主体通过管道传输到任何`WriteStream`实例.

有关详细说明,请参阅有关 [streams](#streams) 的章节.

#### 处理 HTML 表单

HTML 表单可以使用 `application/x-www-form-urlencoded` 或 `multipart/form-data` 的内容类型提交.

对于 url 编码的表单,表单属性在 url 中编码,就像普通的查询参数一样.

对于多部分表单,它们在request body中编码,因此在从连接读取整个request body之前是不可用的.

多部分表单(Multi-part forms)还可以包含文件上传.

如果您想检索多部分表单的属性,您应该告诉 Vert.x 通过调用传递`true`来调用`setExpectMultipart`,在读取任何body之前,你应该收到这样一个表单,然后在读取整个body之后,你应该使用`formAttributes`检索实际的属性:

```java
server.requestHandler(request -> {
  request.setExpectMultipart(true);
  request.endHandler(v -> {
    // The body has now been fully read, so retrieve the form attributes
    MultiMap formAttributes = request.formAttributes();
  });
});
```

表单属性的最大大小为`8192`字节. 当客户端提交属性大小大于此值的表单时,文件上传会在 `HttpServerRequest` 异常处理器上触发异常. 您可以使用 `setMaxFormAttributeSize` 设置不同的最大尺寸.

#### 处理表单文件上传

Vert.x 还可以处理在多部分请求正文中编码的文件上传.

要接收文件上传,你告诉 Vert.x 期待一个多部分的表单并在请求上设置一个`uploadHandler`.

对于到达服务器的每个上传,都会调用一次此处理器.

传递给处理器的对象是一个 `HttpServerFileUpload` 实例.

```java
server.requestHandler(request -> {
  request.setExpectMultipart(true);
  request.uploadHandler(upload -> {
    System.out.println("Got a file upload " + upload.name());
  });
});
```

文件上传可能很大,我们不会在单个缓冲区中提供整个上传,因为这可能会导致内存耗尽,而是以块的形式接收上传数据:

```java
request.uploadHandler(upload -> {
  upload.handler(chunk -> {
    System.out.println("Received a chunk of the upload of length " + chunk.length());
  });
});
```

上传对象是一个`ReadStream`,因此您可以将请求正文通过管道传输到任何`WriteStream`实例. 有关详细说明,请参阅有关 [streams](#streams) 的章节.

如果你只是想将文件上传到磁盘的某个地方,你可以使用`streamToFileSystem`:

```java
request.uploadHandler(upload -> {
  upload.streamToFileSystem("myuploads_directory/" + upload.filename());
});
```

> **☢警告:** 确保检查生产系统中的文件名,以避免恶意客户端将文件上传到文件系统上的任意位置. 有关详细信息,请参阅 [安全说明](#_security_notes).

#### 处理 cookie

您可以使用 `getCookie` 按名称检索 cookie,或使用 `cookieMap` 检索所有 cookie.

要删除 cookie,请使用`removeCookie`.

要添加 cookie,请使用 `addCookie`.

当响应飙头被写入时,这组cookie将被自动写回响应中,以便浏览器可以存储它们.

Cookie 由`Cookie`实例描述. 这允许您检索名称,值,域,路径和其他正常的 cookie 属性.

相同站点 Cookie 让服务器要求 cookie 不应与跨站点(站点由可注册域定义)请求一起发送,这提供了一些针对跨站点请求伪造攻击的保护. 这种 cookie 是使用 setter 启用的:`setSameSite`.

相同的站点 cookie 可以具有 3 个值之一:

- None - 浏览器将发送带有跨站点请求和同站点请求的 cookie.
- Strict - 浏览器只会为相同站点的请求(来自设置 cookie 的站点的请求)发送 cookie. 如果请求来自与当前位置的 URL 不同的 URL,则不会包含任何带有 Strict 属性标记的 cookie.
- Lax - 同站点 cookie 在跨站点子请求(例如加载图像或框架的调用)中被保留,但会在用户从外部站点导航到 URL 时发送; 例如,通过点击链接.

以下是查询和添加 cookie 的示例:

```java
Cookie someCookie = request.getCookie("mycookie");
String cookieValue = someCookie.getValue();

// Do something with cookie...

// Add a cookie - this will get written back in the response automatically
request.response().addCookie(Cookie.cookie("othercookie", "somevalue"));
```

#### Handling compressed body(处理压缩体)

Vert.x 可以处理由客户端使用 *deflate* 或 *gzip* 算法编码的压缩body有效负载.

要启用解压,在创建服务器时在选项上设置' setDecompressionSupported '.

默认情况下,解压缩是禁用的.

#### 接收自定义 HTTP/2 帧

HTTP/2 是一种框架协议,具有用于 HTTP 请求/响应模型的各种框架. 该协议允许发送和接收其他类型的帧.

要接收自定义帧,您可以在请求中使用`customFrameHandler`,每次自定义帧到达时都会调用它. 这是一个例子:

```java
request.customFrameHandler(frame -> {

  System.out.println("Received a frame type=" + frame.type() +
      " payload" + frame.payload().toString());
});
```

HTTP/2 帧不受流控制 - 当接收到自定义帧时,无论请求是否暂停,都会立即调用帧处理器

### 发回响应

服务器响应对象是 `HttpServerResponse` 的一个实例,是从带有 `response` 的请求中获取的.

您使用响应对象将响应写回 HTTP 客户端.

#### 设置状态码和消息

响应的默认 HTTP 状态代码是`200`,表示`OK`.

使用 `setStatusCode` 设置不同的代码.

您还可以使用 `setStatusMessage` 指定自定义状态消息.

如果您不指定状态消息,将使用与状态代码对应的默认消息.

> **🏷注意:** 对于 HTTP/2,响应中不会出现状态,因为协议不会将消息传输到客户端

#### 编写 HTTP 响应

要将数据写入 HTTP 响应,请使用`write`操作之一.

这些可以在响应结束之前多次调用. 可以通过以下几种方式调用它们:

使用单个缓冲区:

```java
HttpServerResponse response = request.response();
response.write(buffer);
```

用一个字符串. 在这种情况下,字符串将使用 UTF-8 编码并将结果写入网络.

```java
HttpServerResponse response = request.response();
response.write("hello world!");
```

带有字符串和编码. 在这种情况下,字符串将使用指定的编码进行编码,并将结果写入网络.

```java
HttpServerResponse response = request.response();
response.write("hello world!", "UTF-16");
```

写入响应是异步的,并且总是在写入队列后立即返回.

如果您只是将单个字符串或缓冲区写入 HTTP 响应,您可以编写它并在一次调用 `end` 时结束响应

第一次调用 `write` 导致将响应标头写入响应. 因此,如果您不使用 HTTP 分块,则必须在写入响应之前设置`Content-Length`标头,否则为时已晚. 如果您使用的是 HTTP 分块,则不必担心.

#### 结束 HTTP 响应

一旦你完成了 HTTP 响应,你应该`end`它.

这可以通过多种方式完成:

没有参数,响应就简单地结束了.

```java
HttpServerResponse response = request.response();
response.write("hello world!");
response.end();
```

它也可以用字符串或缓冲区调用,就像调用 `write` 一样. 在这种情况下,它与使用字符串或缓冲区调用 `write` 之后调用不带参数的 `end` 相同. 例如:

```java
HttpServerResponse response = request.response();
response.end("hello world!");
```

#### 关闭底层连接

您可以使用 `close` 关闭底层 TCP 连接.

响应结束时,Vert.x 将自动关闭非`keep-alive`连接.

默认情况下,Vert.x 不会自动关闭 Keep-alive 连接. 如果您希望在空闲时间后关闭保持活动连接,则配置`setIdleTimeout`.

HTTP/2 连接在关闭响应之前发送一个 `{@literal GOAWAY}` 帧.

#### 设置响应头

HTTP响应头可以通过直接添加到`headers`的方式添加到响应中:

```java
HttpServerResponse response = request.response();
MultiMap headers = response.headers();
headers.set("content-type", "text/html");
headers.set("other-header", "wibble");
```

你也可以用`putHeader`:

```java
HttpServerResponse response = request.response();
response.putHeader("content-type", "text/html").putHeader("other-header", "wibble");
```

> **🏷注意:** 必须在写入响应正文的任何部分之前添加所有标头.

#### 分块的 HTTP 响应和尾部

Vert.x 支持 [HTTP 分块传输编码](https://en.wikipedia.org/wiki/Chunked_transfer_encoding).

这允许将 HTTP 响应正文以块的形式写入,并且通常在将大型响应正文流式传输到客户端并且事先不知道总大小时使用.

您将 HTTP 响应置于分块模式,如下所示:

```java
HttpServerResponse response = request.response();
response.setChunked(true);
```

默认为非分块. 在分块模式下,每次调用`write`方法之一都会导致一个新的 HTTP 块被写出.

在分块模式下,您还可以将 HTTP 响应尾部写入响应. 这些实际上是写在响应的最后一块中.

> **🏷注意:** 分块响应对 HTTP/2 流没有影响

要将尾部添加到响应中,请将它们直接添加到`trailers`中.

```java
HttpServerResponse response = request.response();
response.setChunked(true);
MultiMap trailers = response.trailers();
trailers.set("X-wibble", "woobble").set("X-quux", "flooble");
```

或者使用 `putTrailer`.

```java
HttpServerResponse response = request.response();
response.setChunked(true);
response.putTrailer("X-wibble", "woobble").putTrailer("X-quux", "flooble");
```

#### 直接从磁盘或类路径提供文件

如果您正在编写 Web 服务器,从磁盘提供文件的一种方法是将其作为`AsyncFile`打开并将其通过管道传输到 HTTP 响应.

或者你可以使用`readFile`一次性加载它并将其直接写入响应.

或者,Vert.x 提供了一种方法,允许您在一次操作中将文件从磁盘或文件系统提供给 HTTP 响应. 在底层操作系统支持的情况下,这可能会导致操作系统直接将字节从文件传输到套接字,而根本不会通过用户空间进行复制.

这是通过使用 `sendFile` 完成的,通常对于大文件更有效,但对于小文件可能会更慢.

这是一个非常简单的 Web 服务器,它使用 sendFile 从文件系统提供文件:

```java
vertx.createHttpServer().requestHandler(request -> {
  String file = "";
  if (request.path().equals("/")) {
    file = "index.html";
  } else if (!request.path().contains("..")) {
    file = request.path();
  }
  request.response().sendFile("web/" + file);
}).listen(8080);
```

发送文件是异步的,可能要等到调用返回一段时间后才能完成. 如果您想在文件写入时收到通知,可以使用`sendFile`

有关类路径解析或禁用它的限制,请参阅关于 [从类路径提供文件](#classpath) 的章节.

> **🏷注意:** 如果您在使用 HTTPS 时使用`sendFile`,它将通过用户空间进行复制,因为如果内核直接将数据从磁盘复制到套接字,它不会给我们应用任何加密的机会.

> **☢警告:** 如果您打算直接使用 Vert.x 编写 Web 服务器,请注意用户不能利用路径访问您要为其提供服务的目录或类路径之外的文件.使用 Vert.x Web 模块可能更安全.

当需要只提供文件的一部分时,例如从给定字节开始,您可以通过执行以下操作来实现:

```java
vertx.createHttpServer().requestHandler(request -> {
  long offset = 0;
  try {
    offset = Long.parseLong(request.getParam("start"));
  } catch (NumberFormatException e) {
    // error handling...
  }

  long end = Long.MAX_VALUE;
  try {
    end = Long.parseLong(request.getParam("end"));
  } catch (NumberFormatException e) {
    // error handling...
  }

  request.response().sendFile("web/mybigfile.txt", offset, end);
}).listen(8080);
```

如果要从偏移量开始发送文件直到结束,则不需要提供长度,在这种情况下,您可以这样做:

```java
vertx.createHttpServer().requestHandler(request -> {
  long offset = 0;
  try {
    offset = Long.parseLong(request.getParam("start"));
  } catch (NumberFormatException e) {
    // error handling...
  }

  request.response().sendFile("web/mybigfile.txt", offset);
}).listen(8080);
```

#### 管道响应

服务器响应是一个`WriteStream`,因此您可以从任何`ReadStream`通过管道传输到它,例如 `AsyncFile`,`NetSocket`,`WebSocket` 或 `HttpServerRequest`.

这是一个示例,它在任何 PUT 方法的响应中回显请求正文. 它使用管道作为正文,因此即使 HTTP 请求正文大于任何时候都可以放入内存中,它也可以工作:

```java
vertx.createHttpServer().requestHandler(request -> {
  HttpServerResponse response = request.response();
  if (request.method() == HttpMethod.PUT) {
    response.setChunked(true);
    request.pipeTo(response);
  } else {
    response.setStatusCode(400).end();
  }
}).listen(8080);
```

您还可以使用 `send` 方法发送 `ReadStream`.

发送流是一种管道操作,但是由于这是 `HttpServerResponse` 的一种方法,它也会在未设置 `content-length` 时负责对响应进行分块.

```java
vertx.createHttpServer().requestHandler(request -> {
  HttpServerResponse response = request.response();
  if (request.method() == HttpMethod.PUT) {
    response.send(request);
  } else {
    response.setStatusCode(400).end();
  }
}).listen(8080);
```

#### 编写 HTTP/2 帧

HTTP/2 是一种框架协议,具有用于 HTTP 请求/响应模型的各种框架. 该协议允许发送和接收其他类型的帧.

要发送此类帧,您可以在响应中使用 `writeCustomFrame`. 下面一个例子:

```java
int frameType = 40;
int frameStatus = 10;
Buffer payload = Buffer.buffer("some data");

// Sending a frame to the client
response.writeCustomFrame(frameType, frameStatus, payload);
```

这些帧被立即发送并且不受流控制 - 当这样的帧被发送到那里时,它可能在其他 `{@literal DATA}` 帧之前完成.

#### 流重置

HTTP/1.x 不允许彻底重置请求或响应流,例如,当客户端上传服务器上已经存在的资源时,服务器需要接受整个响应.

HTTP/2 支持在请求/响应期间随时重置流:

```java
request.response().reset();
```

默认情况下,会发送 `NO_ERROR` (0) 错误代码,也可以发送另一个代码:

```java
request.response().reset(8);
```

HTTP/2 规范定义了可以使用的 [错误代码](http://httpwg.org/specs/rfc7540.html#ErrorCodes) 列表.

请求处理器通过 `request handler` 和 `response handler` 收到流重置事件的通知:

```java
request.response().exceptionHandler(err -> {
  if (err instanceof StreamResetException) {
    StreamResetException reset = (StreamResetException) err;
    System.out.println("Stream reset " + reset.getCode());
  }
});
```

#### 服务端推送

服务器推送是 HTTP/2 的一项新功能,可以为单个客户端请求并行发送多个响应.

当服务器处理请求时,它可以向客户端推送请求/响应:

```java
HttpServerResponse response = request.response();

// Push main.js to the client
response.push(HttpMethod.GET, "/main.js", ar -> {

  if (ar.succeeded()) {

    // The server is ready to push the response
    HttpServerResponse pushedResponse = ar.result();

    // Send main.js response
    pushedResponse.
        putHeader("content-type", "application/json").
        end("alert(\"Push response hello\")");
  } else {
    System.out.println("Could not push client resource " + ar.cause());
  }
});

// Send the requested resource
response.sendFile("<html><head><script src=\"/main.js\"></script></head><body></body></html>");
```

当服务器准备好推送响应时,将调用推送响应处理器并且处理器可以发送响应.

推送响应处理器可能会收到失败,例如客户端可能会取消推送,因为它的缓存中已经有 `main.js` 并且不再需要它.

> **🏷注意:** `push` 方法必须在发起响应结束之前调用,但是推送的响应可以写在之后.

#### 处理异常

您可以设置 `exceptionHandler` 来接收在连接传递给 `requestHandler` 或 `webSocketHandler` 之前发生的任何异常,例如 在 TLS 握手期间.

#### 处理无效请求

Vert.x 将处理无效的 HTTP 请求并提供一个默认处理器来适当地处理常见情况,例如 当请求标头太长时,它会以`REQUEST_HEADER_FIELDS_TOO_LARGE`响应.

您可以设置自己的 `invalidRequestHandler` 来处理无效请求. 您的实现可以处理特定情况并将其他情况委托给`HttpServerRequest.DEFAULT_INVALID_REQUEST_HANDLER`.

### HTTP 压缩

Vert.x 支持开箱即用的 HTTP 压缩.

这意味着您可以在将响应发送回客户端之前自动压缩响应的正文.

如果客户端不支持 HTTP 压缩,则在不压缩正文的情况下发回响应.

这允许同时处理支持 HTTP 压缩和不支持它的客户端.

要启用压缩,可以使用 `setCompressionSupported` 对其进行配置.

默认情况下,未启用压缩.

启用 HTTP 压缩后,服务器将检查客户端是否包含包含支持的压缩的`Accept-Encoding`标头. 常用的有deflate和gzip. Vert.x 支持两者.

如果找到这样的标头,服务器将使用支持的压缩之一自动压缩响应的主体并将其发送回客户端.

每当需要在不压缩的情况下发送响应时,您可以将标头 `content-encoding` 设置为 `identity`:

```java
request.response()
  .putHeader(HttpHeaders.CONTENT_ENCODING, HttpHeaders.IDENTITY)
  .sendFile("/path/to/image.jpg");
```

请注意,压缩可能能够减少网络流量,但会占用更多 CPU.

为了解决后一个问题,Vert.x 允许您调整 gzip/deflate 压缩算法的原生"压缩级别"参数.

压缩级别允许根据结果数据的压缩率和压缩/解压缩操作的计算成本来配置 gzip/deflate 算法.

压缩级别是一个整数值,范围从"1"到"9",其中"1"表示压缩率较低但算法最快,"9"表示可用压缩率最大但算法较慢.

使用高于 1-2 的压缩级别通常只允许在大小上节省一些字节 - 增益不是线性的,并且取决于要压缩的特定数据 - 但就 服务器同时生成压缩响应数据(请注意,目前 Vert.x 不支持任何形式的压缩响应数据缓存,即使对于静态文件也是如此,因此压缩是在每次请求正文生成时即时完成的)和 与解码(膨胀)接收到的响应时影响客户端的方式相同,级别越高,操作就越占用 CPU.

默认情况下 - 如果通过 `setCompressionSupported` 启用压缩 - Vert.x 将使用 '6' 作为压缩级别,但可以使用 `setCompressionLevel` 配置参数以解决任何情况.

### 创建 HTTP 客户端

您使用以下默认选项创建一个 `HttpClient` 实例:

```java
HttpClient client = vertx.createHttpClient();
```

如果要为客户端配置选项,请按如下方式创建它:

```java
HttpClientOptions options = new HttpClientOptions().setKeepAlive(false);
HttpClient client = vertx.createHttpClient(options);
```

Vert.x 通过 TLS `h2` 和 TCP `h2c` 支持 HTTP/2.

默认情况下,http 客户端执行 HTTP/1.1 请求,要执行 HTTP/2 请求,`setProtocolVersion` 必须设置为 `HTTP_2`.

对于 `h2` 请求,必须使用 *Application-Layer Protocol Negotiation* 启用 TLS:

```java
HttpClientOptions options = new HttpClientOptions().
    setProtocolVersion(HttpVersion.HTTP_2).
    setSsl(true).
    setUseAlpn(true).
    setTrustAll(true);

HttpClient client = vertx.createHttpClient(options);
```

对于' h2c '请求,TLS必须禁用,客户端将执行HTTP/1.1请求,并尝试升级到HTTP/2:

```java
HttpClientOptions options = new HttpClientOptions().setProtocolVersion(HttpVersion.HTTP_2);

HttpClient client = vertx.createHttpClient(options);
```

`h2c`连接也可以直接建立,也就是说,连接是在预先知道的情况下开始的,当`setHttp2ClearTextUpgrade`选项设置为`false`时:在连接建立后,客户端将发送HTTP/2连接序言,并期望从服务器接收到相同的序言.

http 服务器可能不支持 HTTP/2,实际版本可以在响应到达时使用 `version` 来检查.

当客户端连接到 HTTP/2 服务器时,它会将其"初始设置"发送到服务器. 这些设置定义了服务器如何使用连接,客户端的默认初始设置是 HTTP/2 RFC 定义的默认值.

### 记录网络客户端活动

出于调试目的,可以记录网络活动.

```java
HttpClientOptions options = new HttpClientOptions().setLogActivity(true);
HttpClient client = vertx.createHttpClient(options);
```

有关详细说明,请参阅有关 [记录网络活动](#logging_network_activity) 的章节.

### 发出请求

http 客户端非常灵活,您可以通过多种方式使用它发出请求.

发出请求的第一步是获取到远程服务器的 HTTP 连接:

```java
client.request(HttpMethod.GET,8080, "myserver.mycompany.com", "/some-uri", ar1 -> {
  if (ar1.succeeded()) {
    // Connected to the server
  }
});
```

客户端将连接到远程服务器或重用客户端连接池中的可用连接.

#### 默认主机和端口

通常,您希望使用 http 客户端向同一个主机/端口发出许多请求. 为了避免每次发出请求时都重复主机/端口,您可以使用默认主机/端口配置客户端:

```java
HttpClientOptions options = new HttpClientOptions().setDefaultHost("wibble.com");

// Can also set default port if you want...
HttpClient client = vertx.createHttpClient(options);
client.request(HttpMethod.GET, "/some-uri", ar1 -> {
  if (ar1.succeeded()) {
    HttpClientRequest request = ar1.result();
    request.send(ar2 -> {
      if (ar2.succeeded()) {
        HttpClientResponse response = ar2.result();
        System.out.println("Received response with status code " + response.statusCode());
      }
    });
  }
});
```

#### 写请求标头

您可以使用 `HttpHeaders` 将标头写入请求,如下所示:

```java
HttpClient client = vertx.createHttpClient();

// Write some headers using the headers multi-map
MultiMap headers = HttpHeaders.set("content-type", "application/json").set("other-header", "foo");

client.request(HttpMethod.GET, "some-uri", ar1 -> {
  if (ar1.succeeded()) {
    if (ar1.succeeded()) {
      HttpClientRequest request = ar1.result();
      request.headers().addAll(headers);
      request.send(ar2 -> {
        HttpClientResponse response = ar2.result();
        System.out.println("Received response with status code " + response.statusCode());
      });
    }
  }
});
```

标头是`MultiMap`的一个实例,它提供了添加,设置和删除条目的操作. Http 标头允许特定键有多个值.

You can also write headers using `putHeader`

```java
request.putHeader("content-type", "application/json")
       .putHeader("other-header", "foo");
```

> **🏷注意:** 如果您希望将标头写入请求,则必须在写入请求正文的任何部分之前执行此操作.

#### 写请求和处理响应

`HttpClientRequest` 的 `request` 方法连接到远程服务器或重用现有连接. 获取到的请求实例预先填充了一些数据,例如主机或请求 URI,但您需要将此请求发送到服务器.

您可以调用 `send` 来发送 HTTP `GET` 等请求并处理异步 `HttpClientResponse`.

```java
client.request(HttpMethod.GET,8080, "myserver.mycompany.com", "/some-uri", ar1 -> {
  if (ar1.succeeded()) {
    HttpClientRequest request = ar1.result();

    // Send the request and process the response
    request.send(ar -> {
      if (ar.succeeded()) {
        HttpClientResponse response = ar.result();
        System.out.println("Received response with status code " + response.statusCode());
      } else {
        System.out.println("Something went wrong " + ar.cause().getMessage());
      }
    });
  }
});
```

您还可以使用body发送请求.

带有字符串的 `send`,如果之前没有设置,则会为您设置 `Content-Length` 标头.

```java
client.request(HttpMethod.GET,8080, "myserver.mycompany.com", "/some-uri", ar1 -> {
  if (ar1.succeeded()) {
    HttpClientRequest request = ar1.result();

    // Send the request and process the response
    request.send("Hello World", ar -> {
      if (ar.succeeded()) {
        HttpClientResponse response = ar.result();
        System.out.println("Received response with status code " + response.statusCode());
      } else {
        System.out.println("Something went wrong " + ar.cause().getMessage());
      }
    });
  }
});
```

带有缓冲区的 `send`,如果之前没有设置 `Content-Length` 标头,将为您设置.

```java
request.send(Buffer.buffer("Hello World"), ar -> {
  if (ar.succeeded()) {
    HttpClientResponse response = ar.result();
    System.out.println("Received response with status code " + response.statusCode());
  } else {
    System.out.println("Something went wrong " + ar.cause().getMessage());
  }
});
```

带有流的 `send`,如果先前未设置 `Content-Length` 标头,则使用分块的 `Content-Encoding` 发送请求.

```java
request
  .putHeader(HttpHeaders.CONTENT_LENGTH, "1000")
  .send(stream, ar -> {
  if (ar.succeeded()) {
    HttpClientResponse response = ar.result();
    System.out.println("Received response with status code " + response.statusCode());
  } else {
    System.out.println("Something went wrong " + ar.cause().getMessage());
  }
});
```

#### Streaming Request body(流式请求正文)

`send` 方法一次发送请求.

有时您会希望对如何编写请求bodys进行低级别控制.

`HttpClientRequest` 可用于编写请求body.

以下是一些使用body编写 `POST` 请求的示例:

```java
HttpClient client = vertx.createHttpClient();

client.request(HttpMethod.POST, "some-uri")
  .onSuccess(request -> {
    request.response().onSuccess(response -> {
      System.out.println("Received response with status code " + response.statusCode());
    });

    // Now do stuff with the request
    request.putHeader("content-length", "1000");
    request.putHeader("content-type", "text/plain");
    request.write(body);

    // Make sure the request is ended when you're done with it
    request.end();
});

// Or fluently:

client.request(HttpMethod.POST, "some-uri")
  .onSuccess(request -> {
    request
      .response(ar -> {
        if (ar.succeeded()) {
          HttpClientResponse response = ar.result();
          System.out.println("Received response with status code " + response.statusCode());
        }
      })
      .putHeader("content-length", "1000")
      .putHeader("content-type", "text/plain")
      .end(body);
});
```

存在的方法可以用UTF-8编码和任何特定编码编写字符串,并写入缓冲区:

```java
request.write("some data");

// Write string encoded in specific encoding
request.write("some other data", "UTF-16");

// Write a buffer
Buffer buffer = Buffer.buffer();
buffer.appendInt(123).appendLong(245l);
request.write(buffer);
```

如果您只是将单个字符串或缓冲区写入 HTTP 请求,您可以编写它并在对 `end` 函数的一次调用中结束请求.

```java
request.end("some simple data");

// Write buffer and end the request (send it) in a single call
Buffer buffer = Buffer.buffer().appendDouble(12.34d).appendLong(432l);
request.end(buffer);
```

当您写入请求时,第一次调用 write 将导致请求标头被写入网络.

实际的写入是异步的,可能要等到调用返回后一段时间才会发生.

带有请求正文的非分块 HTTP 请求需要提供`Content-Length`标头.

因此,如果您不使用分块 HTTP,那么您必须在写入请求之前设置`Content-Length`标头,否则为时已晚.

如果您正在调用采用字符串或缓冲区的 `end` 方法之一,那么 Vert.x 将在写入请求正文之前自动计算并设置 `Content-Length` 标头.

如果您使用 HTTP 分块,则不需要`Content-Length`标头,因此您不必预先计算大小.

#### 结束流式 HTTP 请求

完成 HTTP 请求后,您必须使用 `end` 操作之一结束它.

结束请求会导致写入任何标头,如果它们尚未被写入并且请求被标记为完成.

可以通过多种方式结束请求. 没有参数,请求就简单地结束了:

```java
request.end();
```

或者可以在对 `end` 的调用中提供字符串或缓冲区. 这就像在不带参数调用 `end` 之前用字符串或缓冲区调用 `write`

```java
request.end("some-data");

// End it with a buffer
Buffer buffer = Buffer.buffer().appendFloat(12.3f).appendInt(321);
request.end(buffer);
```

#### 将请求用作流

`HttpClientRequest` 实例也是 `WriteStream` 实例.

您可以从任何 `ReadStream` 实例通过管道传输到它.

例如,您可以将磁盘上的文件通过管道传输到 http 请求正文,如下所示:

```java
request.setChunked(true);
file.pipeTo(request);
```

#### 分块的 HTTP 请求

Vert.x 支持 [HTTP Chunked Transfer Encoding](https://en.wikipedia.org/wiki/Chunked_transfer_encoding) 请求.

这允许将 HTTP 请求正文以块的形式写入,并且通常在将大型请求正文流式传输到服务器时使用,其大小事先不知道.

您使用 `setChunked` 将 HTTP 请求置于分块模式.

在分块模式下,每次调用 write 都会导致一个新的块被写入网络. 在分块模式下,无需预先设置请求的`Content-Length`.

```java
request.setChunked(true);

// Write some chunks
for (int i = 0; i < 10; i++) {
  request.write("this-is-chunk-" + i);
}

request.end();
```

#### 请求超时

您可以使用 `setTimeout` 为特定的 http 请求设置超时.

如果请求在超时期限内未返回任何数据,则会将异常传递给异常处理器(如果提供)并关闭请求.

#### 写 HTTP/2 帧

HTTP/2 是一种框架协议,具有用于 HTTP 请求/响应模型的各种框架. 该协议允许发送和接收其他类型的帧.

要发送此类帧,您可以在请求中使用`write`. 这是一个例子:

```java
int frameType = 40;
int frameStatus = 10;
Buffer payload = Buffer.buffer("some data");

// Sending a frame to the server
request.writeCustomFrame(frameType, frameStatus, payload);
```

#### 流重置

HTTP/1.x 不允许彻底重置请求或响应流,例如,当客户端上传服务器上已经存在的资源时,服务器需要接受整个响应.

HTTP/2 支持在请求/响应期间随时重置流:

```java
request.reset();
```

默认情况下发送 NO_ERROR (0) 错误代码,可以发送另一个代码:

```java
request.reset(8);
```

HTTP/2 规范定义了可以使用的 [错误代码](http://httpwg.org/specs/rfc7540.html#ErrorCodes) 列表.

请求处理器通过 `request handler` 和 `response handler` 收到流重置事件的通知:

```java
request.exceptionHandler(err -> {
  if (err instanceof StreamResetException) {
    StreamResetException reset = (StreamResetException) err;
    System.out.println("Stream reset " + reset.getCode());
  }
});
```

### 处理 HTTP 响应

您可以在请求方法中指定的处理器中接收 `HttpClientResponse` 的实例,或者直接在 `HttpClientRequest` 对象上设置处理器.

您可以使用 `statusCode` 和 `statusMessage` 查询响应的状态码和状态消息.

```java
request.send(ar2 -> {
  if (ar2.succeeded()) {

    HttpClientResponse response = ar2.result();

    // the status code - e.g. 200 or 404
    System.out.println("Status code is " + response.statusCode());

    // the status message e.g. "OK" or "Not Found".
    System.out.println("Status message is " + response.statusMessage());
  }
});

// Similar to above, set a completion handler and end the request
request
  .response(ar2 -> {
    if (ar2.succeeded()) {

      HttpClientResponse response = ar2.result();

      // the status code - e.g. 200 or 404
      System.out.println("Status code is " + response.statusCode());

      // the status message e.g. "OK" or "Not Found".
      System.out.println("Status message is " + response.statusMessage());
    }
  })
  .end();
```

#### 将响应用作流

`HttpClientResponse` 实例也是一个 `ReadStream`,这意味着您可以将其通过管道传输到任何 `WriteStream` 实例.

#### 响应标头和尾

Http 响应可以包含标头. 使用 `headers` 获取.

返回的对象是`MultiMap`,因为 HTTP 标头可以包含单个键的多个值.

```java
String contentType = response.headers().get("content-type");
String contentLength = response.headers().get("content-lengh");
```

分块的 HTTP 响应也可以包含尾部 - 这些在响应正文的最后一个块中发送.

您使用 `trailers` 来获取尾部. 尾部也是一个"MultiMap".

#### 读取请求body

当响应的标头已从网络中读取时,将调用响应处理器.

如果响应有正文,则可能会在读取标头后的一段时间内分几部分到达. 在调用响应处理器之前,我们不会等待所有body到达,因为响应可能非常大,我们可能会等待很长时间,或者内存不足以获取大量响应.

当响应body的一部分到达时,使用代表body 部分的`Buffer`调用`handler` :

```java
client.request(HttpMethod.GET, "some-uri", ar1 -> {

  if (ar1.succeeded()) {
    HttpClientRequest request = ar1.result();
    request.send(ar2 -> {
      HttpClientResponse response = ar2.result();
      response.handler(buffer -> {
        System.out.println("Received a part of the response body: " + buffer);
      });
    });
  }
});
```

如果您知道响应body不是很大并且想在处理它之前将其全部聚合到内存中,您可以自己聚合它:

```java
request.send(ar2 -> {

  if (ar2.succeeded()) {

    HttpClientResponse response = ar2.result();

    // Create an empty buffer
    Buffer totalBuffer = Buffer.buffer();

    response.handler(buffer -> {
      System.out.println("Received a part of the response body: " + buffer.length());

      totalBuffer.appendBuffer(buffer);
    });

    response.endHandler(v -> {
      // Now all the body has been read
      System.out.println("Total response body length is " + totalBuffer.length());
    });
  }
});
```

或者,您可以使用方便的 `body`,当响应被完全读取时,它会与整个body一起调用:

```java
request.send(ar1 -> {

  if (ar1.succeeded()) {
    HttpClientResponse response = ar1.result();
    response.body(ar2 -> {

      if (ar2.succeeded()) {
        Buffer body = ar2.result();
        // Now all the body has been read
        System.out.println("Total response body length is " + body.length());
      }
    });
  }
});
```

#### 响应结束处理器

响应`endHandler`是在整个响应body已经被读取时调用的,或者在消息头已经被读取后立即调用,并且如果没有响应body,则调用响应处理器.

#### 请求和响应组合

客户端接口非常简单,遵循以下模式:

1. `request` 一个连接
2. `send` 或者 `write`/`end` 请求到服务器
3. 处理`HttpClientResponse`的开始部分
4. 处理响应事件

你可以使用Vert.x的`future`的组合方法来简化你的代码,但是API是事件驱动的,你需要理解它,否则你可能会遇到数据竞争(即丢失事件导致数据损坏).

> **🏷注意:** [Vert.x Web Client](https://vertx.io/docs/vertx-web-client/java/) 是一种更高级别的 API 替代方案(实际上它是建立在此客户端之上的),如果这个客户端对于你的用例来说级别太低了,你可以考虑一下"Vert.x Web Client".

客户端 API 故意不返回 `Future<HttpClientResponse>`,因为当在事件循环之外设置完成处理器时,在 future 上设置完成处理器可能会很不稳定的.

```java
Future<HttpClientResponse> get = client.get("some-uri");

// 假设我们有一个客户端,它返回一个future
// 响应假设它不在事件循环中
// 为了这个例子,引入了一个潜在的数据竞争
Thread.sleep(100);

get.onSuccess(response -> {

  // 响应事件可能已经发生
  response.body(ar -> {

  });
});
```

将 `HttpClientRequest` 使用限制在 Verticle 中是最简单的解决方案,因为 Verticle 将确保按顺序处理事件以避免竞争.

```java
vertx.deployVerticle(() -> new AbstractVerticle() {
 @Override
 public void start() {

   HttpClient client = vertx.createHttpClient();

   Future<HttpClientRequest> future = client.request(HttpMethod.GET, "some-uri");
 }
}, new DeploymentOptions());
```

当您可能在 Verticle 之外与客户端交互时,只要不延迟响应事件,您就可以安全地执行组合,例如直接在事件循环上处理响应.

```java
Future<JsonObject> future = client
  .request(HttpMethod.GET, "some-uri")
  .compose(request -> request
    .send()
    .compose(response -> {
      // Process the response on the event-loop which guarantees no races
      if (response.statusCode() == 200 &&
          response.getHeader(HttpHeaders.CONTENT_TYPE).equals("application/json")) {
        return response
          .body()
          .map(buffer -> buffer.toJsonObject());
      } else {
        return Future.failedFuture("Incorrect HTTP response");
      }
    }));

// Listen to the composed final json result
future.onSuccess(json -> {
  System.out.println("Received json result " + json);
}).onFailure(err -> {
  System.out.println("Something went wrong " + err.getMessage());
});
```

如果您需要延迟响应处理,则需要`pause`响应或使用`pipe`,这在涉及另一个异步操作时可能是必要的.

```java
Future<Void> future = client
  .request(HttpMethod.GET, "some-uri")
  .compose(request -> request
    .send()
    .compose(response -> {
      // Process the response on the event-loop which guarantees no races
      if (response.statusCode() == 200) {

        // Create a pipe, this pauses the response
        Pipe<Buffer> pipe = response.pipe();

        // Write the file on the disk
        return fileSystem
          .open("/some/large/file", new OpenOptions().setWrite(true))
          .onFailure(err -> pipe.close())
          .compose(file -> pipe.to(file));
      } else {
        return Future.failedFuture("Incorrect HTTP response");
      }
    }));
```

#### 从响应中读取 cookie

您可以使用 `cookies` 从响应中检索 cookie 列表.

或者,您可以自己在响应中解析 `Set-Cookie` 标头.

#### 30x 重定向处理

客户端可以配置为当客户端接收到`Location`响应头提供的HTTP重定向:

- `301`,`302`,`307` 或 `308` 状态码以及 HTTP GET 或 HEAD 方法
- 一个`303`状态码,此外定向请求执行一个 HTTP GET 方法

这是一个例子:

```java
client.request(HttpMethod.GET, "some-uri", ar1 -> {
  if (ar1.succeeded()) {

    HttpClientRequest request = ar1.result();
    request.setFollowRedirects(true);
    request.send(ar2 -> {
      if (ar2.succeeded()) {

        HttpClientResponse response = ar2.result();
        System.out.println("Received response with status code " + response.statusCode());
      }
    });
  }
});
```

默认情况下,最大重定向为 `16` 次,可以使用 `setMaxRedirects` 进行更改.

```java
HttpClient client = vertx.createHttpClient(
    new HttpClientOptions()
        .setMaxRedirects(32));

client.request(HttpMethod.GET, "some-uri", ar1 -> {
  if (ar1.succeeded()) {

    HttpClientRequest request = ar1.result();
    request.setFollowRedirects(true);
    request.send(ar2 -> {
      if (ar2.succeeded()) {

        HttpClientResponse response = ar2.result();
        System.out.println("Received response with status code " + response.statusCode());
      }
    });
  }
});
```

一种尺寸无法满足所有需求,默认重定向策略可能无法满足您的需求.

默认重定向策略可以通过自定义实现进行更改:

```java
client.redirectHandler(response -> {

  // Only follow 301 code
  if (response.statusCode() == 301 && response.getHeader("Location") != null) {

    // Compute the redirect URI
    String absoluteURI = resolveURI(response.request().absoluteURI(), response.getHeader("Location"));

    // Create a new ready to use request that the client will use
    return Future.succeededFuture(new RequestOptions().setAbsoluteURI(absoluteURI));
  }

  // We don't redirect
  return null;
});
```

该策略处理收到的原始 `HttpClientResponse` 并返回 `null` 或 `Future<HttpClientRequest>`.

- 返回 `null` 时,处理原始响应
- 当future返回时,请求将在成功完成时发送
- 当返回一个future 时,在请求上设置的异常处理器在其失败时被调用

返回的请求必须未发送,以便可以发送原始请求处理器,然后客户端可以发送它.

大多数原始请求设置将传播到新请求:

- 请求标头,除非您设置了一些标头
- 请求body `GET` 方法
- 响应处理器
- 请求异常处理器
- 请求超时

#### 100-继续处理

根据[HTTP 1.1规范](https://www.w3.org/Protocols/rfc2616/rfc2616-sec8.html),客户端可以设置一个报头`Expect: 100-Continue`,并在发送其余的请求body之前发送请求报头.

然后,服务器可以以临时响应状态`Status: 100 (Continue)`进行响应,以向客户端表示可以发送body的其余部分.

这里的想法是它允许服务器在发送大量数据之前授权和接受/拒绝请求. 如果请求可能不被接受,则发送大量数据会浪费带宽,并且会占用服务器读取它将丢弃的数据.

Vert.x允许你在客户端请求对象上设置一个`continueHandler`

如果服务器发回`Status: 100 (Continue)`响应以表示可以发送请求的其余部分,则会调用此方法.

这与 `[sendHead](https://vertx.io/docs/apidocs/io/vertx/core/http/HttpClientRequest.html#sendHead--)` 结合使用以发送请求的头部.

这是一个例子:

```java
client.request(HttpMethod.PUT, "some-uri")
  .onSuccess(request -> {
    request.response().onSuccess(response -> {
      System.out.println("Received response with status code " + response.statusCode());
    });

    request.putHeader("Expect", "100-Continue");

    request.continueHandler(v -> {
      // OK to send rest of body
      request.write("Some data");
      request.write("Some more data");
      request.end();
    });

    request.sendHead();
});
```

在服务器端,可以将 Vert.x http 服务器配置为在收到 `Expect: 100-Continue` 标头时自动发回 100 Continue 临时响应.

这是通过设置选项`setHandle100ContinueAutomatically`来完成的.

如果您更愿意手动决定是否发回继续响应,则应将此属性设置为 `false`(默认值),然后您可以检查标头并调用 `writeContinue` 让客户端继续发送正文:

```java
httpServer.requestHandler(request -> {
  if (request.getHeader("Expect").equalsIgnoreCase("100-Continue")) {

    // Send a 100 continue response
    request.response().writeContinue();

    // The client should send the body when it receives the 100 response
    request.bodyHandler(body -> {
      // Do something with body
    });

    request.endHandler(v -> {
      request.response().end();
    });
  }
});
```

您还可以通过直接发回失败状态代码来拒绝请求:在这种情况下,应该忽略主体或关闭连接(100-Continue 是性能提示,不能成为逻辑协议约束):

```java
httpServer.requestHandler(request -> {
  if (request.getHeader("Expect").equalsIgnoreCase("100-Continue")) {

    //
    boolean rejectAndClose = true;
    if (rejectAndClose) {

      // Reject with a failure code and close the connection
      // this is probably best with persistent connection
      request.response()
          .setStatusCode(405)
          .putHeader("Connection", "close")
          .end();
    } else {

      // Reject with a failure code and ignore the body
      // this may be appropriate if the body is small
      request.response()
          .setStatusCode(405)
          .end();
    }
  }
});
```

#### 创建 HTTP 隧道

HTTP 隧道可以使用 `connect` 创建:

```java
client.request(HttpMethod.CONNECT, "some-uri")
  .onSuccess(request -> {

    // Connect to the server
    request.connect(ar -> {
      if (ar.succeeded()) {
        HttpClientResponse response = ar.result();

        if (response.statusCode() != 200) {
          // Connect failed for some reason
        } else {
          // Tunnel created, raw buffers are transmitted on the wire
          NetSocket socket = response.netSocket();
        }
      }
    });
});
```

处理器将在收到 HTTP 响应标头后调用,套接字将准备好进行隧道传输,并将发送和接收缓冲区.

`connect` 的工作方式类似于 `send`,但它重新配置传输以交换原始缓冲区.

#### Client push(客户端推送)

服务器推送是 HTTP/2 的一项新功能,可以为单个客户端请求并行发送多个响应.

可以在请求上设置推送处理器以接收服务器推送的请求/响应:

```java
client.request(HttpMethod.GET, "/index.html")
  .onSuccess(request -> {

    request
      .response().onComplete(response -> {
        // Process index.html response
      });

    // Set a push handler to be aware of any resource pushed by the server
    request.pushHandler(pushedRequest -> {

      // A resource is pushed for this request
      System.out.println("Server pushed " + pushedRequest.path());

      // Set an handler for the response
      pushedRequest.response().onComplete(pushedResponse -> {
        System.out.println("The response for the pushed request");
      });
    });

    // End the request
    request.end();
});
```

如果客户端不想接收推送的请求,它可以重置流:

```java
request.pushHandler(pushedRequest -> {
  if (pushedRequest.path().equals("/main.js")) {
    pushedRequest.reset();
  } else {
    // Handle it
  }
});
```

当未设置处理器时,客户端将自动取消推送的任何流并重置流(`8` 错误代码).

#### 接收自定义 HTTP/2 帧

HTTP/2 是一种框架协议,具有用于 HTTP 请求/响应模型的各种框架. 该协议允许发送和接收其他类型的帧.

要接收自定义帧,您可以在请求上使用 `customFrameHandler`,每次自定义帧到达时都会调用它. 这是一个例子:

```java
response.customFrameHandler(frame -> {

  System.out.println("Received a frame type=" + frame.type() +
      " payload" + frame.payload().toString());
});
```

### 在客户端启用压缩

http 客户端支持开箱即用的 HTTP 压缩.

这意味着客户端可以让远程 http 服务器知道它支持压缩,并且能够处理压缩的响应Body.

http 服务器可以自由地使用一种受支持的压缩算法进行压缩,也可以在根本不压缩的情况下将正文发回. 所以这只是对 Http 服务器的一个提示,它可以随意忽略.

为了告诉 http 服务器客户端支持哪种压缩,它将包含一个 `Accept-Encoding` 标头,其中包含支持的压缩算法作为值. 支持多种压缩算法. 在 Vert.x 的情况下,这将导致添加以下标头:

```
Accept-Encoding: gzip, deflate
```

然后服务器将从其中之一中进行选择. 您可以通过检查从服务器发回的响应中的`Content-Encoding`标头来检测服务器是否压缩了正文.

如果响应的主体是通过 gzip 压缩的,它将包括例如以下标头:

```
Content-Encoding: gzip

```

在创建客户端时使用的选项上启用压缩集`setTryUseCompression`.

默认情况下禁用压缩.

### HTTP/1.x 池化并保持活动状态

Http keep alive 允许将 http 连接用于多个请求. 当您向同一服务器发出多个请求时,这可以更有效地使用连接.

对于 HTTP/1.x 版本,http 客户端支持连接池,允许您在请求之间重用连接.

为了使池工作,保持活动必须在配置客户端时使用的选项上使用`setKeepAlive`为真. 默认值是true.

当启用保活时. Vert.x 将为每个发送的 HTTP/1.0 请求添加一个 `Connection: Keep-Alive` 标头. 当保持活动被禁用时. Vert.x 将在发送的每个 HTTP/1.1 请求中添加一个 `Connection: Close` 标头,以表明在响应完成后连接将关闭.

使用 `setMaxPoolSize` 配置 ** 每个服务器** 的最大连接池数

在启用池的情况下发出请求时,如果已为该服务器创建的连接数少于最大连接数,Vert.x 将创建一个新连接,否则会将请求添加到队列中.

超时后,客户端将自动关闭保持活动连接. 服务器可以使用 `keep-alive` 标头指定超时:

```
keep-alive: timeout=30
```

您可以使用 `setKeepAliveTimeout` 设置默认超时 - 在此超时内未使用的任何连接都将被关闭. 请注意超时值是秒而不是毫秒.

### HTTP/1.1管道

客户端还支持连接上请求的管道连接.

管道连接意味着在前一个请求的响应返回之前,在同一个连接上发送另一个请求.管道并不适用于所有请求.

要启用管道,必须使用`setPipelining`来启用.缺省情况下,管道是禁用的.

当启用了管道连接时,请求将被写入到连接中,而无需等待之前的响应返回.

在单个连接上的管道请求的数量被`setPipeliningLimit`限制.这个选项定义了发送到服务器等待响应的最大http请求数.这个限制确保了客户端请求在到同一服务器的连接上的分配的公平性.

### HTTP/2 多路复用

HTTP/2 提倡使用单个连接到服务器,默认情况下,http 客户端为每个服务器使用单个连接,到同一服务器的所有流都在同一连接上多路复用.

当客户端需要使用多个连接并使用池时,应使用`setHttp2MaxPoolSize`.

当希望限制每个连接的多路复用流数量并使用连接池而不是单个连接时,可以使用`setHttp2MultiplexingLimit`.

```java
HttpClientOptions clientOptions = new HttpClientOptions().
    setHttp2MultiplexingLimit(10).
    setHttp2MaxPoolSize(3);

// Uses up to 3 connections and up to 10 streams per connection
HttpClient client = vertx.createHttpClient(clientOptions);
```

连接的多路复用限制是在客户端上设置的设置,用于限制单个连接的流数. 如果服务器使用 `SETTINGS_MAX_CONCURRENT_STREAMS` 设置设置下限,则有效值可能会更低.

HTTP/2 连接不会被客户端自动关闭. 要关闭它们,您可以调用 `close` 或关闭客户端实例.

或者,您可以使用 `setIdleTimeout` 设置空闲超时 - 在此超时内未使用的任何连接都将被关闭. 请注意,空闲超时值以秒为单位而不是毫秒.

### HTTP 连接

`HttpConnection` 提供了处理 HTTP 连接事件,生命周期和设置的 API.

HTTP/2 完全实现了 `HttpConnection` API.

HTTP/1.x 部分实现了 `HttpConnection` API:仅实现了关闭操作,关闭处理器和异常处理器. 该协议不为其他操作提供语义.

#### 服务器 连接

`connection` 方法返回服务器上的请求连接:

```java
HttpConnection connection = request.connection();
```

可以在服务器上设置连接处理器,以通知任何传入连接:

```java
HttpServer server = vertx.createHttpServer(http2Options);

server.connectionHandler(connection -> {
  System.out.println("A client connected");
});
```

#### 客户端 连接

`connection` 方法返回客户端上的请求连接:

```java
HttpConnection connection = request.connection();
```

可以在客户端上设置连接处理器,以便在建立连接时收到通知:

```java
client.connectionHandler(connection -> {
  System.out.println("Connected to the server");
});
```

#### 连接 设置

HTTP/2 的配置由 `Http2Settings` 数据对象配置.

每个端点都必须遵守连接另一端发送的设置.

建立连接后,客户端和服务器会交换初始设置. 初始设置由客户端上的`setInitialSettings`和服务器上的`setInitialSettings`配置.

建立连接后可以随时更改设置:

```java
connection.updateSettings(new Http2Settings().setMaxConcurrentStreams(100));
```

由于远程端应在收到设置更新时进行确认,因此可以提供回调以通知确认:

```java
connection.updateSettings(new Http2Settings().setMaxConcurrentStreams(100), ar -> {
  if (ar.succeeded()) {
    System.out.println("The settings update has been acknowledged ");
  }
});
```

相反,当接收到新的远程设置时,会通知 `remoteSettingsHandler`:

```java
connection.remoteSettingsHandler(settings -> {
  System.out.println("Received new settings");
});
```

> **🏷注意:** this only applies to the HTTP/2 protocol

#### 连接 ping

HTTP/2 连接 ping 可用于确定连接往返时间或检查连接有效性:`ping` 向远程端点发送 `{@literal PING}` 帧:

```java
Buffer data = Buffer.buffer();
for (byte i = 0;i < 8;i++) {
  data.appendByte(i);
}
connection.ping(data, pong -> {
  System.out.println("Remote side replied");
});
```

Vert.x 将在收到 `{@literal PING}` 帧时自动发送确认,可以将处理器设置为收到每个 ping 通知:

```java
connection.pingHandler(ping -> {
  System.out.println("Got pinged by remote side");
});
```

处理器只是收到通知,无论如何都会发送确认. 此类功能旨在在 HTTP/2 之上实现协议.

> **🏷注意:** 这仅适用于 HTTP/2 协议

#### 连接关闭并离开

调用 `shutdown` 将向连接的远程端发送一个` {@literal GOAWAY}` 帧,要求它停止创建流:客户端将停止执行新请求,服务器将停止推送响应. 发送 `{@literal GOAWAY}` 帧后,连接会等待一段时间(默认为 30 秒),直到所有当前流关闭并关闭连接:

```java
connection.shutdown();
```

`shutdownHandler` 通知所有流已关闭时,连接尚未关闭.

可以只发送一个 `{@literal GOAWAY}` 帧,与关闭的主要区别在于它只会告诉连接的远程端停止创建新流而不安排关闭连接:

```java
connection.goAway(0);
```

相反,也可以在收到 `{@literal GOAWAY}` 时得到通知:

```java
connection.goAwayHandler(goAway -> {
  System.out.println("Received a go away frame");
});
```

当所有当前流都已关闭并且可以关闭连接时,将调用`shutdownHandler`:

```java
connection.goAway(0);
connection.shutdownHandler(v -> {

  // All streams are closed, close the connection
  connection.close();
});
```

这也适用于收到 `{@literal GOAWAY}` 时.

> **🏷注意:** 这仅适用于 HTTP/2 协议

#### 连接 关闭

连接调用 `close`关闭连接:

- 它关闭 HTTP/1.x 的套接字
- 对于 HTTP/2 没有延迟的关闭,{@literal GOAWAY} 帧仍将在连接关闭之前发送. 

当连接关闭时,`closeHandler` 会发出通知.

### 客户端 共享

您可以在多个 Verticle 或同一 Verticle 的实例之间共享一个 HTTP 客户端. 此类客户端应在 Verticle 之外创建,否则在取消部署创建它的 Verticle 时它将关闭

```java
HttpClient client = vertx.createHttpClient(new HttpClientOptions().setShared(true));
vertx.deployVerticle(() -> new AbstractVerticle() {
  @Override
  public void start() throws Exception {
    // Use the client
  }
}, new DeploymentOptions().setInstances(4));
```

您还可以在每个 Verticle 中创建一个共享的 HTTP 客户端:

```java
vertx.deployVerticle(() -> new AbstractVerticle() {
  HttpClient client;
  @Override
  public void start() {
    // Get or create a shared client
    // this actually creates a lease to the client
    // when the verticle is undeployed, the lease will be released automaticaly
    client = vertx.createHttpClient(new HttpClientOptions().setShared(true).setName("my-client"));
  }
}, new DeploymentOptions().setInstances(4));
```

第一次创建共享客户端时,它将创建并返回一个客户端. 后续调用将重用该客户端并为该客户端创建租约. 在处理完所有租约后,客户端将关闭.

默认情况下,客户端在需要创建 TCP 连接时会重用当前的事件循环. 因此,HTTP 客户端将随机使用 Verticle 的事件循环,以安全的方式使用它.

您可以分配多个事件循环,客户端将独立于使用它的客户端使用

```java
vertx.deployVerticle(() -> new AbstractVerticle() {
  HttpClient client;
  @Override
  public void start() {
    // The client creates and use two event-loops for 4 instances
    client = vertx.createHttpClient(new HttpClientOptions().setPoolEventLoopSize(2).setShared(true).setName("my-client"));
  }
}, new DeploymentOptions().setInstances(4));
```

### 服务端 共享

当多个 HTTP 服务器在同一个端口上侦听时,vert.x 使用循环策略编排请求处理.

让我们以 Verticle 创建一个 HTTP 服务器为例:

io.vertx.examples.http.sharing.HttpServerVerticle

```java
vertx.createHttpServer().requestHandler(request -> {
  request.response().end("Hello from server " + this);
}).listen(8080);
```

该服务正在监听 8080 端口.那么,当这个 Verticle 被多次实例化时:`vertx run io.vertx.examples.http.sharing.HttpServerVerticle -instances 2`,发生了什么? 如果两个 Verticle 都绑定到同一个端口,你会收到一个套接字异常. 幸运的是,vert.x 正在为您处理这种情况. 当您在与现有服务器相同的主机和端口上部署另一台服务器时,它实际上并没有尝试创建一个在同一主机/端口上侦听的新服务器. 它只绑定一次到套接字. 当收到请求时,它会按照循环策略调用服务器处理器.

现在让我们想象一个客户端,例如:

```java
vertx.setPeriodic(100, (l) -> {
  vertx.createHttpClient().request(HttpMethod.GET, 8080, "localhost", "/", ar1 -> {
    if (ar1.succeeded()) {
      HttpClientRequest request = ar1.result();
      request.send(ar2 -> {
        if (ar2.succeeded()) {
          HttpClientResponse resp = ar2.result();
          resp.bodyHandler(body -> {
            System.out.println(body.toString("ISO-8859-1"));
          });
        }
      });
    }
  });
});
```

Vert.x 按顺序将请求委托给其中一台服务器:

```
Hello from i.v.e.h.s.HttpServerVerticle@1
Hello from i.v.e.h.s.HttpServerVerticle@2
Hello from i.v.e.h.s.HttpServerVerticle@1
Hello from i.v.e.h.s.HttpServerVerticle@2
...
```

因此,服务器可以在可用内核上扩展,而每个 Vert.x verticle 实例都保持严格的单线程,并且您无需执行任何特殊技巧,例如编写负载平衡器即可在多核机器上扩展服务器.

你可以使用一个负的端口值绑定一个共享的随机端口,第一个绑定将随机选择一个端口,相同端口值的后续绑定将共享这个随机端口.

`io.vertx.examples.http.sharing.HttpServerVerticle`例子:

```java
vertx.createHttpServer().requestHandler(request -> {
  request.response().end("Hello from server " + this);
}).listen(-1);
```

### 在 Vert.x 中使用 HTTPS

Vert.x http 服务器和客户端可以配置为使用 HTTPS,其方式与网络服务器完全相同.

请参阅 [配置网络服务器以使用 SSL](#ssl) 了解更多信息.

SSL 也可以使用 `RequestOptions` 或在使用 `setAbsoluteURI` 方法指定方案时启用/禁用每个请求.

```java
client.request(new RequestOptions()
    .setHost("localhost")
    .setPort(8080)
    .setURI("/")
    .setSsl(true), ar1 -> {
  if (ar1.succeeded()) {
    HttpClientRequest request = ar1.result();
    request.send(ar2 -> {
      if (ar2.succeeded()) {
        HttpClientResponse response = ar2.result();
        System.out.println("Received response with status code " + response.statusCode());
      }
    });
  }
});
```

`setSsl` 设置充当默认客户端设置.

`setSsl` 覆盖默认客户端设置

- 即使将客户端配置为使用 SSL/TLS,将值设置为 `false` 也会禁用 SSL/TLS
- 将值设置为 `true` 将启用 SSL/TLS,即使客户端配置为不使用 SSL/TLS,实际客户端 SSL/TLS(例如信任,密钥/证书,密码,ALPN 等)将被重用

同样,`setAbsoluteURI` 方案也覆盖了默认的客户端设置.

#### 服务器名称指示 (SNI)

Vert.x http 服务器可以配置为使用与 {@linkplain io.vertx.core.net net servers} 完全相同的方式使用 SNI.

Vert.x http 客户端将在 TLS 握手期间将实际主机名显示为 *server name*.

### WebSockets

[WebSockets](https://en.wikipedia.org/wiki/WebSocket) 是一种 Web 技术,它允许在 HTTP 服务器和 HTTP 客户端(通常是浏览器)之间建立类似全双工套接字的连接.

Vert.x supports WebSockets on both the client and server-side.

#### 服务器上的 WebSockets

在服务器端有两种处理 WebSocket 的方法.

##### WebSocket 处理器

第一种方法涉及在服务器实例上提供一个`webSocketHandler`.

当与服务器建立 WebSocket 连接时,将调用处理器,并传入 `ServerWebSocket` 的实例.

```java
server.webSocketHandler(webSocket -> {
  System.out.println("Connected!");
});
```

您可以通过调用 `reject` 来选择拒绝 WebSocket.

```java
server.webSocketHandler(webSocket -> {
  if (webSocket.path().equals("/myapi")) {
    webSocket.reject();
  } else {
    // Do something
  }
});
```

您可以通过调用带有 `Future` 的 `setHandshake` 来执行异步握手:

```java
server.webSocketHandler(webSocket -> {
  Promise<Integer> promise = Promise.promise();
  webSocket.setHandshake(promise.future());
  authenticate(webSocket.headers(), ar -> {
    if (ar.succeeded()) {
      // Terminate the handshake with the status code 101 (Switching Protocol)
      // Reject the handshake with 401 (Unauthorized)
      promise.complete(ar.succeeded() ? 101 : 401);
    } else {
      // Will send a 500 error
      promise.fail(ar.cause());
    }
  });
});
```

> **🏷注意:** 除非WebSocket的握手被设置好了,否则WebSocket会在处理器被调用后被自动接受

##### 升级到 WebSocket

处理 WebSocket 的第二种方法是处理从客户端发送的 HTTP 升级请求,并在服务器请求上调用 `toWebSocket`.

```java
server.requestHandler(request -> {
  if (request.path().equals("/myapi")) {

    Future<ServerWebSocket> fut = request.toWebSocket();
    fut.onSuccess(ws -> {
      // Do something
    });

  } else {
    // Reject
    request.response().setStatusCode(400).end();
  }
});
```

##### 服务器端的 WebSocket

`ServerWebSocket` 实例使您能够检索 WebSocket 握手的 HTTP 请求的 `headers`,`path`,`query` 和 `URI`.

#### 客户端的 WebSocket

Vert.x的 `HttpClient` 支持 WebSockets.

您可以使用 `webSocket` 操作之一并提供处理器将 WebSocket 连接到服务器.

建立连接后,将使用 `WebSocket` 的实例调用处理器:

```java
client.webSocket("/some-uri", res -> {
  if (res.succeeded()) {
    WebSocket ws = res.result();
    System.out.println("Connected!");
  }
});
```

默认情况下,客户端将 `origin` 标头设置为服务器主机,例如 [http://www.example.com](http://www.example.com/). 有些服务器会拒绝这样的请求,你可以配置客户端不设置这个头.

```java
WebSocketConnectOptions options = new WebSocketConnectOptions()
  .setHost(host)
  .setPort(port)
  .setURI(requestUri)
  .setAllowOriginHeader(false);
client.webSocket(options, res -> {
  if (res.succeeded()) {
    WebSocket ws = res.result();
    System.out.println("Connected!");
  }
});
```

您还可以设置不同的标题:

```java
WebSocketConnectOptions options = new WebSocketConnectOptions()
  .setHost(host)
  .setPort(port)
  .setURI(requestUri)
  .addHeader(HttpHeaders.ORIGIN, origin);
client.webSocket(options, res -> {
  if (res.succeeded()) {
    WebSocket ws = res.result();
    System.out.println("Connected!");
  }
});
```

> **🏷注意:** 旧版本的 WebSocket 协议使用 `sec-websocket-origin`

#### 将消息写入 WebSocket

如果您希望将单个 WebSocket 消息写入 WebSocket,您可以使用 `writeBinaryMessage` 或 `writeTextMessage` 执行此操作:

```java
Buffer buffer = Buffer.buffer().appendInt(123).appendFloat(1.23f);
webSocket.writeBinaryMessage(buffer);

// Write a simple text message
String message = "hello";
webSocket.writeTextMessage(message);
```

如果 WebSocket 消息大于使用 `setMaxWebSocketFrameSize` 配置的最大 WebSocket 帧大小,则 Vert.x 将在将其在线发送之前将其拆分为多个 WebSocket 帧.

#### 将帧写入 WebSocket

一个 WebSocket 消息可以由多个帧组成. 在这种情况下,第一帧是 *binary* 或 *text* 帧,后跟零个或多个 *continuation* 帧.

消息中的最后一帧被标记为 *final*.

要发送由多个帧组成的消息,您可以使用 `WebSocketFrame.binaryFrame` , `WebSocketFrame.textFrame` 或 `WebSocketFrame.continuationFrame` 创建帧,然后使用 `writeFrame` 将它们写入 WebSocket.

Here's an example for binary frames:

```java
WebSocketFrame frame1 = WebSocketFrame.binaryFrame(buffer1, false);
webSocket.writeFrame(frame1);

WebSocketFrame frame2 = WebSocketFrame.continuationFrame(buffer2, false);
webSocket.writeFrame(frame2);

// Write the final frame
WebSocketFrame frame3 = WebSocketFrame.continuationFrame(buffer2, true);
webSocket.writeFrame(frame3);
```

在许多情况下,您只想发送一个包含单个最终帧的 WebSocket 消息,因此我们提供了一些快捷方法来执行此操作,使用 `writeFinalBinaryFrame` 和 `writeFinalTextFrame`.

这是一个例子:

```java
webSocket.writeFinalTextFrame("Geronimo!");

// Send a WebSocket message consisting of a single final binary frame:

Buffer buff = Buffer.buffer().appendInt(12).appendString("foo");

webSocket.writeFinalBinaryFrame(buff);
```

#### 从 WebSocket 读取帧

要从 WebSocket 读取帧,请使用`frameHandler`.

当帧到达时,将使用 `WebSocketFrame` 实例调用帧处理器,例如:

```java
webSocket.frameHandler(frame -> {
  System.out.println("Received a frame of size!");
});
```

#### 关闭 WebSockets

完成后使用 `close` 关闭 WebSocket 连接.

#### 管道 WebSockets

`WebSocket` 实例也是 `ReadStream` 和 `WriteStream`,因此它可以与管道一起使用.

将 WebSocket 用作写入流或读取流时,它只能与 WebSockets 连接一起使用,这些连接与不拆分为多个帧的二进制帧一起使用.

#### 事件总线处理器

每个WebSocket会自动在事件总线上注册两个处理器,当这个处理器接收到任何数据时,它会将它们写入自身.这些是不在集群上路由的本地订阅.

这使您能够将数据写入 WebSocket,该 WebSocket 可能在一个完全不同的 Verticle 中,将数据发送到该处理器的地址.

处理器的地址由 `binaryHandlerID` 和 `textHandlerID` 给出.

### 为 HTTP/HTTPS 连接使用代理

http 客户端支持通过 HTTP 代理(例如 Squid)或 *SOCKS4a* 或 *SOCKS5* 代理访问 http/https URL. CONNECT 协议使用 HTTP/1.x,但可以连接到 HTTP/1.x 和 HTTP/2 服务器.

http 代理可能不支持连接到 h2c(未加密的 HTTP/2 服务器),因为它们仅支持 HTTP/1.1.

可以通过设置包含代理类型,主机名,端口以及可选的用户名和密码的 `ProxyOptions` 对象在 `HttpClientOptions` 中配置代理.

这是使用 HTTP 代理的示例:

```java
HttpClientOptions options = new HttpClientOptions()
    .setProxyOptions(new ProxyOptions().setType(ProxyType.HTTP)
        .setHost("localhost").setPort(3128)
        .setUsername("username").setPassword("secret"));
HttpClient client = vertx.createHttpClient(options);
```

当客户端连接到一个 http URL 时,它会连接到代理服务器并在 HTTP 请求中提供完整的 URL("GET http://www.somehost.com/path/file.html HTTP/1.1").

当客户端连接到 https URL 时,它要求代理使用 CONNECT 方法创建到远程主机的隧道.

对于 SOCKS5 代理:

```java
HttpClientOptions options = new HttpClientOptions()
    .setProxyOptions(new ProxyOptions().setType(ProxyType.SOCKS5)
        .setHost("localhost").setPort(1080)
        .setUsername("username").setPassword("secret"));
HttpClient client = vertx.createHttpClient(options);
```

DNS解析总是在代理服务器上完成,要实现SOCKS4客户端的功能,需要在本地解析DNS地址.

代理选项也可以根据请求设置:

```java
client.request(new RequestOptions()
  .setHost("example.com")
  .setProxyOptions(proxyOptions))
  .compose(request -> request
    .send()
    .compose(HttpClientResponse::body))
  .onSuccess(body -> {
    System.out.println("Received response");
  });
```

> **🏷注意:** 给定的主机应始终使用相同的代理选项:由于 HTTP 请求是池化的,因此在建立连接时使用每个请求的代理选项

您可以使用 `setNonProxyHosts` 来配置绕过代理的主机列表. 列表接受 `*` 通配符来匹配域:

```java
HttpClientOptions options = new HttpClientOptions()
  .setProxyOptions(new ProxyOptions().setType(ProxyType.SOCKS5)
    .setHost("localhost").setPort(1080)
    .setUsername("username").setPassword("secret"))
  .addNonProxyHost("*.foo.com")
  .addNonProxyHost("localhost");
HttpClient client = vertx.createHttpClient(options);
```

#### 处理其他协议

如果代理支持,HTTP 代理实现支持获取 ftp:// url.

当 HTTP 请求 URI 包含完整的 URL 时,客户端将不会计算完整的 HTTP url,而是使用请求 URI 中指定的完整 URL:

```java
HttpClientOptions options = new HttpClientOptions()
    .setProxyOptions(new ProxyOptions().setType(ProxyType.HTTP));
HttpClient client = vertx.createHttpClient(options);
client.request(HttpMethod.GET, "ftp://ftp.gnu.org/gnu/", ar -> {
  if (ar.succeeded()) {
    HttpClientRequest request = ar.result();
    request.send(ar2 -> {
      if (ar2.succeeded()) {
        HttpClientResponse response = ar2.result();
        System.out.println("Received response with status code " + response.statusCode());
      }
    });
  }
});
```

### 使用 HA PROXY  协议

[HA PROXY 协议](https://www.haproxy.org/download/1.8/doc/proxy-protocol.txt) 提供了一种方便的方式来安全地跨多个 NAT 或 TCP 代理传输客户端地址等连接信息 .

可以通过设置选项 `setUseProxyProtocol` 并在类路径中添加以下依赖项来启用 HA PROXY 协议:

```xml
<dependency>
 <groupId>io.netty</groupId>
 <artifactId>netty-codec-haproxy</artifactId>
 <!--<version>Should align with netty version that Vert.x uses</version>-->
</dependency>
```

```java
HttpServerOptions options = new HttpServerOptions()
  .setUseProxyProtocol(true);

HttpServer server = vertx.createHttpServer(options);
server.requestHandler(request -> {
  // Print the actual client address provided by the HA proxy protocol instead of the proxy address
  System.out.println(request.remoteAddress());

  // Print the address of the proxy
  System.out.println(request.localAddress());
});
```

### Verticle中的自动清理

如果您从 Verticle 内部创建 http 服务器和客户端,这些服务器和客户端将在 verticle 取消部署时自动关闭.

## 使用 SharedData API

顾名思义,`SharedData` API 允许您在以下之间安全地共享数据:

- 应用程序的不同部分,或
- 同一个 Vert.x 实例中的不同应用程序,或
- 跨 Vert.x 实例集群的不同应用程序.

实际上,它提供了:

- 同步 maps(仅限本地)
- 异步 maps
- 异步 locks
- 异步 counters

> **⚠重要:** 分布式数据结构的行为取决于您使用的集群管理器. 面对网络分区时的备份(复制)和行为由集群管理器及其配置定义. 请参阅集群管理器文档以及底层框架手册.

### 本地 maps

`Local maps` 允许您在同一个 Vert.x 实例中的不同事件循环(例如不同的 Verticle)之间安全地共享数据.

它们只允许将某些数据类型用作键和值:

- 不可变类型(例如字符串,布尔值等),或
- 实现了`Shareable`接口的类型(buffers,JSON 数组,JSON 对象或您自己的可共享对象).

在后一种情况下,键/值将在放入map之前被复制.

通过这种方式,我们可以确保在您的 Vert.x 应用程序的不同线程之间没有*对可变状态的共享访问*. 而且您不必担心通过同步访问来保护该状态.

这是使用共享本地map的示例:

```java
SharedData sharedData = vertx.sharedData();

LocalMap<String, String> map1 = sharedData.getLocalMap("mymap1");

map1.put("foo", "bar"); // Strings are immutable so no need to copy

LocalMap<String, Buffer> map2 = sharedData.getLocalMap("mymap2");

map2.put("eek", Buffer.buffer().appendInt(123)); // This buffer will be copied before adding to map

// Then... in another part of your application:

map1 = sharedData.getLocalMap("mymap1");

String val = map1.get("foo");

map2 = sharedData.getLocalMap("mymap2");

Buffer buff = map2.get("eek");
```

### 异步共享 maps

`异步共享地图`允许将数据放入map在本地或从任何其他节点检索.

这使得它们对于诸如将会话状态存储在托管 Vert.x Web 应用程序的服务器场中非常有用.

获取map是异步的,结果会在您指定的处理器中返回给您. 这是一个例子:

```java
SharedData sharedData = vertx.sharedData();

sharedData.<String, String>getAsyncMap("mymap", res -> {
  if (res.succeeded()) {
    AsyncMap<String, String> map = res.result();
  } else {
    // Something went wrong!
  }
});
```

当 Vert.x 被集群时,您放入map的数据可以在本地访问,也可以在任何其他集群成员上访问.

> **⚠重要:** 在集群模式下,异步共享map依赖于集群管理器提供的分布式数据结构. 请注意,集群模式下与异步共享map操作相关的延迟可能比本地模式下高得多.

如果您的应用程序不需要与其他所有节点共享数据,您可以检索一个仅限本地的map:

```java
SharedData sharedData = vertx.sharedData();

sharedData.<String, String>getLocalAsyncMap("mymap", res -> {
  if (res.succeeded()) {
    // Local-only async map
    AsyncMap<String, String> map = res.result();
  } else {
    // Something went wrong!
  }
});
```

#### 将数据放入map

你用 `put` 把数据放到一个map中.

实际的 put 是异步的,一旦完成就会通知处理器:

```java
map.put("foo", "bar", resPut -> {
  if (resPut.succeeded()) {
    // Successfully put the value
  } else {
    // Something went wrong!
  }
});
```

#### 从map中获取数据

您可以使用 `get` 从map中获取数据.

实际的 get 是异步的,处理器会在一段时间后收到结果通知:

```java
map.get("foo", resGet -> {
  if (resGet.succeeded()) {
    // Successfully got the value
    Object val = resGet.result();
  } else {
    // Something went wrong!
  }
});
```

##### 其他map操作

您还可以从异步map中删除条目,清除它们并获取大小.

有关map操作的详细列表,请参阅`API 文档`.

### 异步 locks

`异步锁`允许您在本地或跨集群获得排他锁. 当您想要在任何时候只在集群的一个节点上做某事或访问资源时,这很有用.

与大多数锁 API 不同,异步锁有一个异步 API,后者会阻塞调用线程,直到获得锁为止.

要获得锁,请使用`getLock`. 这不会阻塞,但是当锁可用时,将使用 `Lock` 的实例调用处理器,表明您现在拥有锁.

当您拥有锁时,本地或集群上的其他调用者将无法获得锁.

当你完成锁后,你调用`release`来释放它,所以另一个调用者可以获得它:

```java
SharedData sharedData = vertx.sharedData();

sharedData.getLock("mylock", res -> {
  if (res.succeeded()) {
    // Got the lock!
    Lock lock = res.result();

    // 5 seconds later we release the lock so someone else can get it

    vertx.setTimer(5000, tid -> lock.release());

  } else {
    // Something went wrong
  }
});
```

您还可以通过超时获得锁. 如果在超时时间内未能获得锁,则处理器将被调用失败:

```java
SharedData sharedData = vertx.sharedData();

sharedData.getLockWithTimeout("mylock", 10000, res -> {
  if (res.succeeded()) {
    // Got the lock!
    Lock lock = res.result();

  } else {
    // Failed to get lock
  }
});
```

有关lock操作的详细列表,请参阅`API 文档`.

> **⚠重要:** 在集群模式下,异步锁依赖于集群管理器提供的分布式数据结构. 请注意,集群模式下与异步共享锁操作相关的延迟可能比本地模式下高得多.

如果您的应用程序不需要与其他所有节点共享锁,您可以检索本地锁:

```java
SharedData sharedData = vertx.sharedData();

sharedData.getLocalLock("mylock", res -> {
  if (res.succeeded()) {
    // Local-only lock
    Lock lock = res.result();

    // 5 seconds later we release the lock so someone else can get it

    vertx.setTimer(5000, tid -> lock.release());

  } else {
    // Something went wrong
  }
});
```

### 异步 counters

在本地或跨应用程序的不同节点维护原子计数器通常很有用.

您可以使用 `Counter` 执行此操作.

您使用 `getCounter` 获得一个实例:

```java
SharedData sharedData = vertx.sharedData();

sharedData.getCounter("mycounter", res -> {
  if (res.succeeded()) {
    Counter counter = res.result();
  } else {
    // Something went wrong!
  }
});
```

一旦你有一个实例,你可以检索当前计数,原子地增加它,减少它并使用各种方法向它添加一个值.

有关计数器操作的详细列表,请参阅`API 文档`.

> **⚠重要:** 在集群模式下,异步计数器依赖于集群管理器提供的分布式数据结构. 请注意,集群模式下与异步共享计数器操作相关的延迟可能比本地模式下高得多.

If your application doesn't need the counter to be shared with every other node, you can retrieve a local-only counter:

```java
SharedData sharedData = vertx.sharedData();

sharedData.getLocalCounter("mycounter", res -> {
  if (res.succeeded()) {
    // Local-only counter
    Counter counter = res.result();
  } else {
    // Something went wrong!
  }
});
```

## 在 Vert.x 中使用文件系统

Vert.x `FileSystem` 对象提供了许多操作文件系统的操作.

每个 Vert.x 实例有一个文件系统对象,您可以通过 `fileSystem` 方法来获得它.

提供了每个操作的阻塞和非阻塞版本. 非阻塞版本采用一个处理器,当操作完成或发生错误时调用该处理器.

下面是一个文件的异步副本示例:

```java
FileSystem fs = vertx.fileSystem();

// Copy file from foo.txt to bar.txt
fs.copy("foo.txt", "bar.txt", res -> {
  if (res.succeeded()) {
    // Copied ok!
  } else {
    // Something went wrong
  }
});
```

阻塞版本被命名为 `xxxBlocking` 并直接返回结果或抛出异常. 在许多情况下,根据操作系统和文件系统,一些潜在的阻塞操作可能会很快返回,这就是我们提供它们的原因,但强烈建议您在从事件循环中使用它们之前测试它们在特定应用程序中返回所需的时间,以免违反黄金法则.

这是使用阻塞 API 的`copy`:

```java
FileSystem fs = vertx.fileSystem();

// Copy file from foo.txt to bar.txt synchronously
fs.copyBlocking("foo.txt", "bar.txt");
```

存在许多操作来 复制,移动,截断,chmod 和许多其他文件操作. 我们不会在这里全部列出,请查阅 `API docs` 获取完整列表.

让我们看几个使用异步方法的例子:

```java
vertx.fileSystem().readFile("target/classes/readme.txt", result -> {
  if (result.succeeded()) {
    System.out.println(result.result());
  } else {
    System.err.println("Oh oh ..." + result.cause());
  }
});

// Copy a file
vertx.fileSystem().copy("target/classes/readme.txt", "target/classes/readme2.txt", result -> {
  if (result.succeeded()) {
    System.out.println("File copied");
  } else {
    System.err.println("Oh oh ..." + result.cause());
  }
});

// Write a file
vertx.fileSystem().writeFile("target/classes/hello.txt", Buffer.buffer("Hello"), result -> {
  if (result.succeeded()) {
    System.out.println("File written");
  } else {
    System.err.println("Oh oh ..." + result.cause());
  }
});

// Check existence and delete
vertx.fileSystem().exists("target/classes/junk.txt", result -> {
  if (result.succeeded() && result.result()) {
    vertx.fileSystem().delete("target/classes/junk.txt", r -> {
      System.out.println("File deleted");
    });
  } else {
    System.err.println("Oh oh ... - cannot delete the file: " + result.cause());
  }
});
```

### 异步文件

Vert.x 提供了一种异步文件抽象,允许您操作文件系统上的文件.

You open an `AsyncFile` as follows:

```java
OpenOptions options = new OpenOptions();
fileSystem.open("myfile.txt", options, res -> {
  if (res.succeeded()) {
    AsyncFile file = res.result();
  } else {
    // Something went wrong!
  }
});
```

`AsyncFile` 实现了 `ReadStream` 和 `WriteStream`,因此您可以 *pipe* 文件与其他流对象(例如网络套接字,http 请求和响应以及 WebSockets)之间进行传输.

它们还允许您直接读取和写入它们.

#### 随机存取写

要使用 `AsyncFile` 进行随机存取写入,请使用 `write` 方法.

该方法的参数是:

- `buffer`: 要写入的缓冲区.
- `position`: 文件中写入缓冲区的整数位置. 如果位置大于或等于文件的大小,文件将被放大以适应偏移量.
- `handler`: 结果处理器

这是随机存取写入的示例:

```java
vertx.fileSystem().open("target/classes/hello.txt", new OpenOptions(), result -> {
  if (result.succeeded()) {
    AsyncFile file = result.result();
    Buffer buff = Buffer.buffer("foo");
    for (int i = 0; i < 5; i++) {
      file.write(buff, buff.length() * i, ar -> {
        if (ar.succeeded()) {
          System.out.println("Written ok!");
          // etc
        } else {
          System.err.println("Failed to write: " + ar.cause());
        }
      });
    }
  } else {
    System.err.println("Cannot open file " + result.cause());
  }
});
```

#### 随机存取读

要使用 `AsyncFile` 进行随机存取读,请使用 `read` 方法.

该方法的参数是:

- `buffer`: 数据将被读取到的缓冲区.
- `offset`: 将放置读取数据的缓冲区的整数偏移量.
- `position`: 文件中从中读取数据的位置.
- `length`: 要读取的数据字节数
- `handler`: 结果处理器

这是随机访问读取的示例:

```java
vertx.fileSystem().open("target/classes/les_miserables.txt", new OpenOptions(), result -> {
  if (result.succeeded()) {
    AsyncFile file = result.result();
    Buffer buff = Buffer.buffer(1000);
    for (int i = 0; i < 10; i++) {
      file.read(buff, i * 100, i * 100, 100, ar -> {
        if (ar.succeeded()) {
          System.out.println("Read ok!");
        } else {
          System.err.println("Failed to write: " + ar.cause());
        }
      });
    }
  } else {
    System.err.println("Cannot open file " + result.cause());
  }
});
```

#### 打开选项

当打开一个 `AsyncFile` 时,你传递了一个 `OpenOptions` 实例. 这些选项描述文件访问的行为. 例如,您可以使用 `setRead`,`setWrite` 和 `setPerms` 方法配置文件权限.

如果打开的文件已经存在,您还可以使用 `setCreateNew` 和 `setTruncateExisting` 配置行为.

您还可以使用 `setDeleteOnClose` 标记要在关闭或关闭 JVM 时删除的文件.

#### 将数据刷新到底层存储.

在 `OpenOptions` 中,您可以使用 `setDsync` 在每次写入时启用/禁用内容的自动同步. 在这种情况下,您可以通过调用 `flush` 方法手动刷新操作系统缓存中的任何写入.

此方法也可以通过一个处理器来调用,该处理器将在刷新完成时调用.

#### 使用 AsyncFile 作为 ReadStream 和 WriteStream

`AsyncFile` 实现了 `ReadStream` 和 `WriteStream`. 然后,您可以将它们与 *pipe* 一起使用,将数据通过管道传输到其他读写流. 例如,这会将内容复制到另一个 `AsyncFile`:

```java
final AsyncFile output = vertx.fileSystem().openBlocking("target/classes/plagiary.txt", new OpenOptions());

vertx.fileSystem().open("target/classes/les_miserables.txt", new OpenOptions(), result -> {
  if (result.succeeded()) {
    AsyncFile file = result.result();
    file.pipeTo(output)
      .onComplete(v -> {
        System.out.println("Copy done");
      });
  } else {
    System.err.println("Cannot open file " + result.cause());
  }
});
```

您还可以使用 *pipe* 将文件内容写入 HTTP 响应,或者更普遍地写入任何 `WriteStream`.

<a name="classpath"></a>
#### [从classpath(类路径)访问文件](#classpath)

当vert.x无法在文件系统中找到该文件时,它将尝试从类路径解析该文件.请注意,类路径资源路径不能以`/`开头.

由于Java没有提供对类路径资源的异步访问,当第一次访问类路径资源时,文件会被复制到工作线程中的文件系统中,并从那里异步地提供服务.当第二次访问相同的资源时,文件系统中的文件将直接从文件系统中提供.即使类路径资源发生了变化(例如在开发系统中),也只会最初内容被提供.

此缓存行为可以在 `setFileCachingEnabled` 选项上设置. 此选项的默认值为 `true`,除非定义了系统属性 `vertx.disableFileCaching`.

文件缓存的路径默认是`/tmp/vertx-cache-UUID`,可以通过设置系统属性`vertx.cacheDirBase`来自定义. 使用此属性时,它应该指向进程可读/可写位置中的目录前缀,例如:`-Dvertx.cacheDirBase=/tmp/my-vertx-cache`(请注意,没有 UUID).

每个 vert.x 进程都会附加它自己的 UUID,以便保持缓存独立于在同一台机器上运行的不同应用程序.

通过将系统属性 `vertx.disableFileCPResolving` 设置为 `true`,可以在系统范围内禁用整个类路径解析功能.

> **🏷注意:** 这些系统属性会在加载 `io.vertx.core.file.FileSystemOptions` 类时评估一次,因此应在加载此类之前设置这些属性,或者在启动它时将其设置为 JVM 系统属性.

如果你想在特定的应用程序中禁用类路径解析,但是在系统范围内默认启用它,你可以通过`VertxOptions.getFileSystemOptions().setClassPathResolvingEnabled()`选项来实现.

#### 关闭 AsyncFile

要关闭 `AsyncFile`,请调用 `close` 方法. 关闭是异步的,如果您想在关闭完成时收到通知,您可以指定一个处理函数作为参数.

## 数据报套接字 (UDP)

在 Vert.x 中使用用户数据报协议 (UDP) 是小菜一碟.

UDP 是一种无连接传输,这基本上意味着您没有与远程对等方的持久连接.

相反,您可以发送和接收包,并且每个包中都包含远程地址.

除此之外,UDP 使用起来不如 TCP 安全,这意味着根本无法保证发送数据报数据包会收到它的端点.

唯一的保证是它要么会收到完整的,要么根本不会收到.

此外,您通常不能发送大于网络接口 MTU 大小的数据,这是因为每个数据包将作为一个数据包发送.

但是,请注意,即使数据包大小小于 MTU,它仍可能失败.

失败的大小取决于操作系统等.所以经验法则是尝试发送小数据包.

由于 UDP 的性质,它最适合允许您丢弃数据包的应用程序(例如监控应用程序).

好处是与 TCP 相比,它的开销要少得多,可以由 NetServer 和 NetClient 处理(见上文).

### 创建数据报套接字

要使用 UDP,您首先需要创建一个 `DatagramSocket`. 如果您只想发送数据或发送和接收,这并不重要.

```java
DatagramSocket socket = vertx.createDatagramSocket(new DatagramSocketOptions());
```

返回的 `DatagramSocket` 不会绑定到特定端口. 如果您只想发送数据(如客户端),这不是问题,但下一节将详细介绍.

### 发送数据报包

如前所述,用户数据报协议 (UDP) 以数据包的形式将数据发送到远程对等点,但不会以持久的方式连接到它们.

这意味着每个数据包都可以发送到不同的远程对等方.

发送数据包很简单,如下所示:

```java
DatagramSocket socket = vertx.createDatagramSocket(new DatagramSocketOptions());
Buffer buffer = Buffer.buffer("content");
// Send a Buffer
socket.send(buffer, 1234, "10.0.0.1", asyncResult -> {
  System.out.println("Send succeeded? " + asyncResult.succeeded());
});
// Send a String
socket.send("A string used as content", 1234, "10.0.0.1", asyncResult -> {
  System.out.println("Send succeeded? " + asyncResult.succeeded());
});
```

### 接收数据报包

如果你想接收数据包,你需要通过调用`listen(…)}`来绑定`DatagramSocket`.

这样,您将能够接收发送到`DatagramSocket`侦听的地址和端口的`DatagramPacket`.

除此之外,您还想设置一个 `Handler`,它将为每个收到的 `DatagramPacket` 调用.

`DatagramPacket` 有以下方法:

- `sender`:代表数据包发送者的 InetSocketAddress
- `data`:保存接收到的数据的缓冲区.

因此,要监听特定地址和端口,您可以执行如下所示的操作:

```java
DatagramSocket socket = vertx.createDatagramSocket(new DatagramSocketOptions());
socket.listen(1234, "0.0.0.0", asyncResult -> {
  if (asyncResult.succeeded()) {
    socket.handler(packet -> {
      // Do something with the packet
    });
  } else {
    System.out.println("Listen failed" + asyncResult.cause());
  }
});
```

> **🏷注意:** 请注意,即使 {code AsyncResult} 成功,它也仅意味着它可能被写入网络堆栈,但不能保证它曾经到达或将到达远程对等点.

如果您需要这样的保证,那么您希望使用 TCP 并在顶部构建一些握手逻辑.

### Multicast(组播)

#### 发送组播数据包

组播允许多个套接字接收相同的数据包. 这是通过让套接字加入同一个多播组来实现的,然后您可以将数据包发送到该组.

我们将在下一节中介绍如何加入多播组并接收数据包.

发送多播数据包与发送普通数据报数据包没有区别. 不同之处在于您将多播组地址传递给 send 方法.

如下所示:

```java
DatagramSocket socket = vertx.createDatagramSocket(new DatagramSocketOptions());
Buffer buffer = Buffer.buffer("content");
// Send a Buffer to a multicast address
socket.send(buffer, 1234, "230.0.0.1", asyncResult -> {
  System.out.println("Send succeeded? " + asyncResult.succeeded());
});
```

所有已加入多播组 `230.0.0.1` 的套接字都将收到该数据包.

##### 接收组播数据包

如果你想接收特定组播组的数据包,你需要通过调用`listen(…)`来绑定`DatagramSocket`来加入组播组.

这样,您将收到发送到`DatagramSocket`侦听的地址和端口的数据报包,以及发送到多播组的数据包.

除此之外,您还想设置一个处理器,它将为每个接收到的 DatagramPacket 调用.

`DatagramPacket` 有以下方法:

- `sender()`:代表数据包发送者的 InetSocketAddress
- `data()`:保存接收到的数据的缓冲区.

因此,要监听特定地址和端口并接收多播组 `230.0.0.1` 的数据包,您可以执行如下所示的操作:

```java
DatagramSocket socket = vertx.createDatagramSocket(new DatagramSocketOptions());
socket.listen(1234, "0.0.0.0", asyncResult -> {
  if (asyncResult.succeeded()) {
    socket.handler(packet -> {
      // Do something with the packet
    });

    // join the multicast group
    socket.listenMulticastGroup("230.0.0.1", asyncResult2 -> {
        System.out.println("Listen succeeded? " + asyncResult2.succeeded());
    });
  } else {
    System.out.println("Listen failed" + asyncResult.cause());
  }
});
```

##### 取消监听/离开多播组

有时您希望在有限的时间内接收多播组的数据包.

在这种情况下,您可以先开始聆听它们,然后再不听.

如下所示:

```java
DatagramSocket socket = vertx.createDatagramSocket(new DatagramSocketOptions());
socket.listen(1234, "0.0.0.0", asyncResult -> {
    if (asyncResult.succeeded()) {
      socket.handler(packet -> {
        // Do something with the packet
      });

      // join the multicast group
      socket.listenMulticastGroup("230.0.0.1", asyncResult2 -> {
          if (asyncResult2.succeeded()) {
            // will now receive packets for group

            // do some work

            socket.unlistenMulticastGroup("230.0.0.1", asyncResult3 -> {
              System.out.println("Unlisten succeeded? " + asyncResult3.succeeded());
            });
          } else {
            System.out.println("Listen failed" + asyncResult2.cause());
          }
      });
    } else {
      System.out.println("Listen failed" + asyncResult.cause());
    }
});
```

##### 阻止多播

除了取消侦听多播地址之外,还可以仅阻止特定发件人地址的多播.

请注意,这只适用于某些操作系统和内核版本. 因此,如果支持,请查看操作系统文档.

这是一个专家特性.

要阻止来自特定地址的多播,您可以在 DatagramSocket 上调用 `blockMulticastGroup(...)`,如下所示:

```java
DatagramSocket socket = vertx.createDatagramSocket(new DatagramSocketOptions());

// Some code

// This would block packets which are send from 10.0.0.2
socket.blockMulticastGroup("230.0.0.1", "10.0.0.2", asyncResult -> {
  System.out.println("block succeeded? " + asyncResult.succeeded());
});
```

#### DatagramSocket 属性

创建 `DatagramSocket` 时,您可以设置多个属性以使用 `DatagramSocketOptions` 对象更改其行为. 这些都在这里列出:

- `setSendBufferSize` 以字节为单位设置发送缓冲区大小.
- `setReceiveBufferSize` 以字节为单位设置 TCP 接收缓冲区大小.
- `setReuseAddress` 如果为真,那么处于 TIME_WAIT 状态的地址可以在关闭后重新使用.
- `setTrafficClass`
- `setBroadcast` 设置或清除 SO_BROADCAST 套接字选项. 设置此选项后,数据报 (UDP) 数据包可能会发送到本地接口的广播地址.
- `setMulticastNetworkInterface` 设置或清除 IP_MULTICAST_LOOP 套接字选项. 当设置此选项时,多播数据包也将在本地接口上接收.
- `setMulticastTimeToLive` 设置 IP_MULTICAST_TTL 套接字选项. TTL 代表"生存时间",但在此上下文中,它指定允许数据包通过的 IP 跃点数,特别是对于多播流量. 每个转发数据包的路由器或网关都会递减 TTL. 如果路由器将 TTL 减为 0,则不会转发.

#### DatagramSocket 本地地址

您可以通过调用 `localAddress` 找到套接字的本地地址(即 UDP 套接字这一端的地址). 如果您之前使用 `listen(...)` 绑定了 `DatagramSocket`,这只会返回 `InetSocketAddress`,否则它将返回 null.

#### 关闭 DatagramSocket

您可以通过调用 `close` 方法来关闭套接字. 这将关闭套接字并释放所有资源

## DNS 客户端

通常,您会发现自己需要以异步方式获取 DNS 信息. 不幸的是,Java 虚拟机本身附带的 API 无法做到这一点. 因此,Vert.x 提供了自己的完全异步的 DNS 解析 API.

要获得一个 Dns 客户端实例,您将通过 Vertex 实例创建一个新实例.

```java
DnsClient client = vertx.createDnsClient(53, "10.0.0.1");
```

You can also create the client with options and configure the query timeout.

```java
DnsClient client = vertx.createDnsClient(new DnsClientOptions()
  .setPort(53)
  .setHost("10.0.0.1")
  .setQueryTimeout(10000)
);
```

创建不带参数的客户端或省略服务器地址将使用内部用于非阻塞地址解析的服务器地址.

```java
DnsClient client1 = vertx.createDnsClient();

// Just the same but with a different query timeout
DnsClient client2 = vertx.createDnsClient(new DnsClientOptions().setQueryTimeout(10000));
```

### 查找

尝试查找给定名称的 A (ipv4) 或 AAAA (ipv6) 记录. 返回的第一个将被使用,因此它的行为方式与您在操作系统上使用"nslookup"时可能使用的方式相同.

要查找"vertx.io"的 A / AAAA 记录,您通常会像这样使用它:

```java
DnsClient client = vertx.createDnsClient(53, "9.9.9.9");
client.lookup("vertx.io", ar -> {
  if (ar.succeeded()) {
    System.out.println(ar.result());
  } else {
    System.out.println("Failed to resolve entry" + ar.cause());
  }
});
```

### lookup4

尝试查找给定名称的 A (ipv4) 记录. 返回的第一个将被使用,因此它的行为方式与您在操作系统上使用"nslookup"时可能使用的方式相同.

要查找"vertx.io"的 A 记录,您通常会像这样使用它:

```java
DnsClient client = vertx.createDnsClient(53, "9.9.9.9");
client.lookup4("vertx.io", ar -> {
  if (ar.succeeded()) {
    System.out.println(ar.result());
  } else {
    System.out.println("Failed to resolve entry" + ar.cause());
  }
});
```

### lookup6

尝试查找给定名称的 AAAA (ipv6) 记录. 返回的第一个将被使用,因此它的行为方式与您在操作系统上使用"nslookup"时可能使用的方式相同.

To lookup the A record for "vertx.io" you would typically use it like:

```java
DnsClient client = vertx.createDnsClient(53, "9.9.9.9");
client.lookup6("vertx.io", ar -> {
  if (ar.succeeded()) {
    System.out.println(ar.result());
  } else {
    System.out.println("Failed to resolve entry" + ar.cause());
  }
});
```

### resolveA

尝试解析给定名称的所有 A (ipv4) 记录. 这与在类似 unix 的操作系统上使用"dig"非常相似.

要查找"vertx.io"的所有 A 记录,您通常会这样做:

```java
DnsClient client = vertx.createDnsClient(53, "9.9.9.9");
client.resolveA("vertx.io", ar -> {
  if (ar.succeeded()) {
    List<String> records = ar.result();
    for (String record : records) {
      System.out.println(record);
    }
  } else {
    System.out.println("Failed to resolve entry" + ar.cause());
  }
});
```

### resolveAAAA

尝试解析给定名称的所有 AAAA (ipv6) 记录. 这与在类似 unix 的操作系统上使用"dig"非常相似.

要查找"vertx.io"的所有 AAAAA 记录,您通常会执行以下操作:

```java
DnsClient client = vertx.createDnsClient(53, "9.9.9.9");
client.resolveAAAA("vertx.io", ar -> {
  if (ar.succeeded()) {
    List<String> records = ar.result();
    for (String record : records) {
      System.out.println(record);
    }
  } else {
    System.out.println("Failed to resolve entry" + ar.cause());
  }
});
```

### resolveCNAME

尝试解析给定名称的所有 CNAME 记录. 这与在类似 unix 的操作系统上使用"dig"非常相似.

要查找"vertx.io"的所有 CNAME 记录,您通常会执行以下操作:

```java
DnsClient client = vertx.createDnsClient(53, "9.9.9.9");
client.resolveCNAME("vertx.io", ar -> {
  if (ar.succeeded()) {
    List<String> records = ar.result();
    for (String record : records) {
      System.out.println(record);
    }
  } else {
    System.out.println("Failed to resolve entry" + ar.cause());
  }
});
```

### resolveMX

尝试解析给定名称的所有 MX 记录. MX 记录用于定义哪个邮件服务器接受给定域的电子邮件.

要查找"vertx.io"的所有 MX 记录,您通常会执行以下操作:

```java
DnsClient client = vertx.createDnsClient(53, "9.9.9.9");
client.resolveMX("vertx.io", ar -> {
  if (ar.succeeded()) {
    List<MxRecord> records = ar.result();
    for (MxRecord record: records) {
      System.out.println(record);
    }
  } else {
    System.out.println("Failed to resolve entry" + ar.cause());
  }
});
```

请注意,列表将包含按优先级排序的 `MxRecord`,这意味着优先级较小的 MX 记录在列表中排在第一位.

`MxRecord` 允许您通过提供方法访问 MX 记录的优先级和名称,例如:

```java
record.priority();
record.name();
```

### resolveTXT

尝试解析给定名称的所有 TXT 记录. TXT 记录通常用于定义域的额外信息.

要解析"vertx.io"的所有 TXT 记录,您可以使用以下内容:

```java
DnsClient client = vertx.createDnsClient(53, "9.9.9.9");
client.resolveTXT("vertx.io", ar -> {
  if (ar.succeeded()) {
    List<String> records = ar.result();
    for (String record: records) {
      System.out.println(record);
    }
  } else {
    System.out.println("Failed to resolve entry" + ar.cause());
  }
});
```

### resolveNS

尝试解析给定名称的所有 NS 记录. NS 记录指定哪个 DNS 服务器托管给定域的 DNS 信息.

要解析"vertx.io"的所有 NS 记录,您可以使用以下内容:

```java
DnsClient client = vertx.createDnsClient(53, "9.9.9.9");
client.resolveNS("vertx.io", ar -> {
  if (ar.succeeded()) {
    List<String> records = ar.result();
    for (String record: records) {
      System.out.println(record);
    }
  } else {
    System.out.println("Failed to resolve entry" + ar.cause());
  }
});
```

### resolveSRV

尝试解析给定名称的所有 SRV 记录. SRV 记录用于定义服务的端口和主机名等额外信息. 一些协议需要这些额外信息.

要查找"vertx.io"的所有 SRV 记录,您通常会执行以下操作:

```java
DnsClient client = vertx.createDnsClient(53, "9.9.9.9");
client.resolveSRV("vertx.io", ar -> {
  if (ar.succeeded()) {
    List<SrvRecord> records = ar.result();
    for (SrvRecord record: records) {
      System.out.println(record);
    }
  } else {
    System.out.println("Failed to resolve entry" + ar.cause());
  }
});
```

请注意,列表将包含按优先级排序的 Srv 记录,这意味着优先级较小的 Srv 记录在列表中排在首位.

`SrvRecord` 允许您访问 SRV 记录本身中包含的所有信息:

```java
record.priority();
record.name();
record.weight();
record.port();
record.protocol();
record.service();
record.target();
```

Please refer to the API docs for the exact details.

### resolvePTR

尝试解析给定名称的 PTR 记录. PTR 记录将 ipaddress 映射到名称.

要解析 ipaddress 10.0.0.1 的 PTR 记录,您将使用"1.0.0.10.in-addr.arpa"的 PTR 概念

```java
DnsClient client = vertx.createDnsClient(53, "9.9.9.9");
client.resolvePTR("1.0.0.10.in-addr.arpa", ar -> {
  if (ar.succeeded()) {
    String record = ar.result();
    System.out.println(record);
  } else {
    System.out.println("Failed to resolve entry" + ar.cause());
  }
});
```

### reverseLookup

尝试对 IP 地址进行反向查找. 这与解析 PTR 记录基本相同,但只允许您传入 ip 地址而不是有效的 PTR 查询字符串.

要对 ipaddress 10.0.0.1 进行反向查找,请执行以下类似操作:

```java
DnsClient client = vertx.createDnsClient(53, "9.9.9.9");
client.reverseLookup("10.0.0.1", ar -> {
  if (ar.succeeded()) {
    String record = ar.result();
    System.out.println(record);
  } else {
    System.out.println("Failed to resolve entry" + ar.cause());
  }
});
```

### 错误处理

正如您在前面的部分中看到的,DnsClient 允许您传入一个处理器,一旦查询完成,该处理器将通过 AsyncResult 得到通知. 如果出现错误,将通过 DnsException 通知它,该异常将导致一个"DnsResponseCode",指示解析失败的原因. 此 DnsResponseCode 可用于更详细地检查原因.

可能的 DnsResponseCodes 是:

- `NOERROR` 没有找到给定查询的记录
- `FORMERROR` 格式错误
- `SERVFAIL` 服务器故障
- `NXDOMAIN` 名称错误
- `NOTIMPL` 未由 DNS 服务器实现
- `REFUSED` DNS 服务器拒绝查询
- `YXDOMAIN` 域名不应该存在
- `YXRRSET` 资源记录不应该存在
- `NXRRSET` RRSET 不存在
- `NOTZONE` 名称不在区域中
- `BADVERS` 版本的错误扩展机制
- `BADSIG` 错误签名
- `BADKEY` 坏键
- `BADTIME` 错误的时间戳

所有这些错误都是由 DNS 服务器本身"生成"的.

您可以从 DnsException 中获取 DnsResponseCode,例如:

```java
DnsClient client = vertx.createDnsClient(53, "10.0.0.1");
client.lookup("nonexisting.vert.xio", ar -> {
  if (ar.succeeded()) {
    String record = ar.result();
    System.out.println(record);
  } else {
    Throwable cause = ar.cause();
    if (cause instanceof DnsException) {
      DnsException exception = (DnsException) cause;
      DnsResponseCode code = exception.code();
      // ...
    } else {
      System.out.println("Failed to resolve entry" + ar.cause());
    }
  }
});
```

<a name="streams"></a>
## 流

在Vertx中有几个对象允许对项目进行读写.

在 Vert.x 中,写入调用立即返回,写入在内部排队.

不难看出,如果您写入对象的速度比它实际将数据写入其底层资源的速度快,那么写入队列可能会无限增长 - 最终导致内存耗尽.

To solve this problem aVert.x API 中的一些对象提供了简单的流量控制(*背压*)功能.

任何可以*写入*的流控制感知对象都实现了`WriteStream`,而任何可以*读取*的流控制对象都被称为实现了`ReadStream`.

让我们举一个例子,我们想从 `ReadStream` 中读取数据,然后将数据写入 `WriteStream`.

一个非常简单的例子是从 `NetSocket` 读取然后写回同一个 `NetSocket` - 因为 `NetSocket` 实现了 `ReadStream` 和 `WriteStream`. 请注意,这适用于任何符合 `ReadStream` 和 `WriteStream` 的对象,包括 HTTP 请求,HTTP 响应,异步文件 I/O,WebSocket 等.

一种简单的方法是直接获取已读取的数据,并立即将其写入`NetSocket`:

```java
NetServer server = vertx.createNetServer(
    new NetServerOptions().setPort(1234).setHost("localhost")
);
server.connectHandler(sock -> {
  sock.handler(buffer -> {
    // Write the data straight back
    sock.write(buffer);
  });
}).listen();
```

上面的例子有一个问题:如果从套接字读取数据的速度快于将数据写回套接字的速度,它将在 `NetSocket` 的写入队列中堆积,最终耗尽 RAM. 这可能会发生,例如,如果套接字另一端的客户端读取速度不够快,从而有效地对连接施加背压.

由于 `NetSocket` 实现了 `WriteStream`,我们可以在写入之前检查 `WriteStream` 是否已满:

```java
NetServer server = vertx.createNetServer(
    new NetServerOptions().setPort(1234).setHost("localhost")
);
server.connectHandler(sock -> {
  sock.handler(buffer -> {
    if (!sock.writeQueueFull()) {
      sock.write(buffer);
    }
  });

}).listen();
```

此示例不会耗尽 RAM,但如果写入队列已满,我们最终会丢失数据. 我们真正想做的是在写队列满时暂停`NetSocket`:

```java
NetServer server = vertx.createNetServer(
    new NetServerOptions().setPort(1234).setHost("localhost")
);
server.connectHandler(sock -> {
  sock.handler(buffer -> {
    sock.write(buffer);
    if (sock.writeQueueFull()) {
      sock.pause();
    }
  });
}).listen();
```

我们快到了,但还不完全. `NetSocket` 现在在文件已满时暂停,但我们还需要在写入队列处理完其积压后取消暂停:

```java
NetServer server = vertx.createNetServer(
    new NetServerOptions().setPort(1234).setHost("localhost")
);
server.connectHandler(sock -> {
  sock.handler(buffer -> {
    sock.write(buffer);
    if (sock.writeQueueFull()) {
      sock.pause();
      sock.drainHandler(done -> {
        sock.resume();
      });
    }
  });
}).listen();
```

我们终于得到它了. 当写入队列准备好接受更多数据时,会调用 `drainHandler` 事件处理器,这会恢复允许读取更多数据的 `NetSocket`.

在编写 Vert.x 应用程序时想要这样做是很常见的,所以我们添加了 `pipeTo` 方法来为您完成所有这些艰苦的工作. 你只需给它 `WriteStream` 并使用它:

```java
NetServer server = vertx.createNetServer(
  new NetServerOptions().setPort(1234).setHost("localhost")
);
server.connectHandler(sock -> {
  sock.pipeTo(sock);
}).listen();
```

这与更详细的示例完全相同,并且它处理流失败和终止:当管道以成功或失败完成时结束目标`WriteStream`.

操作完成时可以通知您:

```java
server.connectHandler(sock -> {

  // Pipe the socket providing an handler to be notified of the result
  sock.pipeTo(sock, ar -> {
    if (ar.succeeded()) {
      System.out.println("Pipe succeeded");
    } else {
      System.out.println("Pipe failed");
    }
  });
}).listen();
```

当您处理异步目标时,您可以创建一个 `Pipe` 实例,该实例暂停源并在源通过管道传输到目标时恢复它:

```java
server.connectHandler(sock -> {

  // Create a pipe to use asynchronously
  Pipe<Buffer> pipe = sock.pipe();

  // Open a destination file
  fs.open("/path/to/file", new OpenOptions(), ar -> {
    if (ar.succeeded()) {
      AsyncFile file = ar.result();

      // Pipe the socket to the file and close the file at the end
      pipe.to(file);
    } else {
      sock.close();
    }
  });
}).listen();
```

当您需要中止传输时,您需要将其关闭:

```java
vertx.createHttpServer()
  .requestHandler(request -> {

    // Create a pipe that to use asynchronously
    Pipe<Buffer> pipe = request.pipe();

    // Open a destination file
    fs.open("/path/to/file", new OpenOptions(), ar -> {
      if (ar.succeeded()) {
        AsyncFile file = ar.result();

        // Pipe the socket to the file and close the file at the end
        pipe.to(file);
      } else {
        // Close the pipe and resume the request, the body buffers will be discarded
        pipe.close();

        // Send an error response
        request.response().setStatusCode(500).end();
      }
    });
  }).listen(8080);
```

当管道关闭时,流处理器将被取消设置并恢复`ReadStream`.

如上所示,默认情况下,目标总是在流完成时结束,您可以在管道对象上控制此行为:

- `endOnFailure` 控制失败发生时的行为
- `endOnSuccess` 控制读取流结束时的行为
- `endOnComplete` 控制所有情况下的行为

这是一个简短的例子:

```java
src.pipe()
  .endOnSuccess(false)
  .to(dst, rs -> {
    // Append some text and close the file
    dst.end(Buffer.buffer("done"));
});
```

现在让我们更详细地看一下 `ReadStream` 和 `WriteStream` 上的方法:

### ReadStream

`ReadStream` 由`HttpClientResponse`,`DatagramSocket`,`HttpClientRequest`,`HttpServerFileUpload`,`HttpServerRequest`,`MessageConsumer`,`NetSocket`,`WebSocket`,`TimeoutStream`,`AsyncFile`实现.

- `handler`:设置一个从 ReadStream 接收项目的处理器.
- `pause`:暂停流. 暂停时,处理器中不会收到任何项目.
- `fetch`:从流中获取指定数量的项目. 如果有任何项目到达,将调用处理器. 获取是累积的.
- `resume`:恢复流. 如果有任何项目到达,将调用处理器. 恢复相当于获取 `Long.MAX_VALUE` 项.
- `exceptionHandler`:当 ReadStream 发生异常时调用.
- `endHandler`: 当到达流的末尾时调用. 如果 ReadStream 表示文件,则可能是到达 EOF 时,或者如果是 HTTP 请求,则可能是到达请求结束时,或者如果是 TCP 套接字,则可能是连接关闭时.

读取流处于 *flowing* 或 *fetch* 模式

- 最初,流处于 <i>flowing</i> 模式
- 当流处于 *flowing* 模式时,元素被传递给处理器
- 当流处于 *fetch* 模式时,只会将请求的元素数量传递给处理器

`pause`,`resume` 和 `fetch` 改变模式

- `resume()` 设置 *flowing* 模式
- `pause()` 设置 *fetch* 模式并将需求重置为 `0`
- `fetch(long)` 请求特定数量的元素并将其添加到实际需求中

### WriteStream

`WriteStream` 由 `HttpClientRequest`,`HttpServerResponse` `WebSocket`,`NetSocket` 和 `AsyncFile` 实现.

方法:

- `write`:将对象写入 WriteStream. 这种方法永远不会阻塞. 写入在内部排队并异步写入底层资源.
- `setWriteQueueMaxSize`:设置写入队列被认为*满*的对象数量,方法`writeQueueFull`返回`true`. 请注意,当认为写入队列已满时,如果调用 write,数据仍将被接受并排队. 实际数量取决于流的实现,对于 `Buffer`,大小表示实际写入的字节数,而不是缓冲区的数量.
- `writeQueueFull`:如果认为写入队列已满,则返回 `true`.
- `exceptionHandler`:如果`WriteStream`发生异常,将被调用.
- `drainHandler`:如果`WriteStream`被认为不再满,处理器将被调用.

## Record Parser(记录解析器)

记录解析器允许您轻松解析由字节序列或固定大小记录分隔的协议. 它将输入缓冲区序列转换为按配置结构化的缓冲区序列(固定大小或分隔记录).

例如,如果您有一个由 '\n' 分隔的简单 ASCII 文本协议,并且输入如下:

```
buffer1:HELLO\nHOW ARE Y
buffer2:OU?\nI AM
buffer3: DOING OK
buffer4:\n
```

记录解析器会产生

```
buffer1:HELLO
buffer2:HOW ARE YOU?
buffer3:I AM DOING OK
```

让我们看看相关的代码:

```java
final RecordParser parser = RecordParser.newDelimited("\n", h -> {
  System.out.println(h.toString());
});

parser.handle(Buffer.buffer("HELLO\nHOW ARE Y"));
parser.handle(Buffer.buffer("OU?\nI AM"));
parser.handle(Buffer.buffer("DOING OK"));
parser.handle(Buffer.buffer("\n"));
```

您还可以生成固定大小的块,如下所示:

```java
RecordParser.newFixed(4, h -> {
  System.out.println(h.toString());
});
```

有关更多详细信息,请查看 `RecordParser` 类.

## Json Parser

您可以轻松地解析 JSON 结构,但这需要一次提供 JSON 内容,但是当您需要解析非常大的结构时可能不方便.

非阻塞 JSON 解析器是一个事件驱动的解析器,能够处理非常大的结构. 它将输入缓冲区序列转换为 JSON 解析事件序列.

```java
JsonParser parser = JsonParser.newParser();

// Set handlers for various events
parser.handler(event -> {
  switch (event.type()) {
    case START_OBJECT:
      // Start an objet
      break;
    case END_OBJECT:
      // End an objet
      break;
    case START_ARRAY:
      // Start an array
      break;
    case END_ARRAY:
      // End an array
      break;
    case VALUE:
      // Handle a value
      String field = event.fieldName();
      if (field != null) {
        // In an object
      } else {
        // In an array or top level
        if (event.isString()) {

        } else {
          // ...
        }
      }
      break;
  }
});
```

解析器是非阻塞的,发出的事件由输入缓冲区驱动.

```java
JsonParser parser = JsonParser.newParser();

// start array event
// start object event
// "firstName":"Bob" event
parser.handle(Buffer.buffer("[{\"firstName\":\"Bob\","));

// "lastName":"Morane" event
// end object event
parser.handle(Buffer.buffer("\"lastName\":\"Morane\"},"));

// start object event
// "firstName":"Luke" event
// "lastName":"Lucky" event
// end object event
parser.handle(Buffer.buffer("{\"firstName\":\"Luke\",\"lastName\":\"Lucky\"}"));

// end array event
parser.handle(Buffer.buffer("]"));

// Always call end
parser.end();
```

事件驱动的解析提供了更多的控制,但代价是处理细粒度的事件,这有时会很不方便. JSON 解析器允许您在需要时将 JSON 结构作为值处理:

```java
JsonParser parser = JsonParser.newParser();

parser.objectValueMode();

parser.handler(event -> {
  switch (event.type()) {
    case START_ARRAY:
      // Start the array
      break;
    case END_ARRAY:
      // End the array
      break;
    case VALUE:
      // Handle each object
      break;
  }
});

parser.handle(Buffer.buffer("[{\"firstName\":\"Bob\"},\"lastName\":\"Morane\"),...]"));
parser.end();
```

在解析期间可以设置和取消设置值模式,允许您在细粒度事件或 JSON 对象值事件之间切换.

```java
JsonParser parser = JsonParser.newParser();

parser.handler(event -> {
  // Start the object

  switch (event.type()) {
    case START_OBJECT:
      // Set object value mode to handle each entry, from now on the parser won't emit start object events
      parser.objectValueMode();
      break;
    case VALUE:
      // Handle each object
      // Get the field in which this object was parsed
      String id = event.fieldName();
      System.out.println("User with id " + id + " : " + event.value());
      break;
    case END_OBJECT:
      // Set the object event mode so the parser emits start/end object events again
      parser.objectEventMode();
      break;
  }
});

parser.handle(Buffer.buffer("{\"39877483847\":{\"firstName\":\"Bob\"},\"lastName\":\"Morane\"),...}"));
parser.end();
```

你也可以对数组做同样的事情

```java
JsonParser parser = JsonParser.newParser();

parser.handler(event -> {
  // Start the object

  switch (event.type()) {
    case START_OBJECT:
      // Set array value mode to handle each entry, from now on the parser won't emit start array events
      parser.arrayValueMode();
      break;
    case VALUE:
      // Handle each array
      // Get the field in which this object was parsed
      System.out.println("Value : " + event.value());
      break;
    case END_OBJECT:
      // Set the array event mode so the parser emits start/end object events again
      parser.arrayEventMode();
      break;
  }
});

parser.handle(Buffer.buffer("[0,1,2,3,4,...]"));
parser.end();
```

您还可以解码 POJO

```java
parser.handler(event -> {
  // Handle each object
  // Get the field in which this object was parsed
  String id = event.fieldName();
  User user = event.mapTo(User.class);
  System.out.println("User with id " + id + " : " + user.firstName + " " + user.lastName);
});
```

每当解析器无法处理缓冲区时,除非您设置异常处理器,否则将引发异常:

```java
JsonParser parser = JsonParser.newParser();

parser.exceptionHandler(err -> {
  // Catch any parsing or decoding error
});
```

解析器还解析 json 流:

- 连接的 json 流: `{"temperature":30}{"temperature":50}`
- 行分隔的 json 流: `{"an":"object"}\r\n3\r\n"a string"\r\nnull`

有关更多详细信息,请查看 `JsonParser` 类.

## 线程安全

大多数 Vert.x 对象可以安全地从不同的线程访问. *然而*当它们从创建它们的相同上下文中访问时,性能得到了优化.

例如,如果您部署了一个 verticle,它创建了一个在其处理器中提供 `NetSocket` 实例的 `NetServer`,那么最好始终从 verticle 的事件循环中访问该套接字实例.

如果你坚持标准的 Vert.x Verticle 部署模型并避免在 Verticle 之间共享对象,那么这应该是你不必考虑的情况.

<a name="Running_blocking_code"></a>
## 运行阻塞代码

在一个完美的世界中,不会有战争或饥饿,所有api都是异步编写的,兔子会和小羊羔手牵手跳过阳光明媚的绿色草地.

**但现实世界不是这样的.(你最近看新闻了吗?)**

事实是,即使不是大多数库,也有很多库,特别是在 JVM 生态系统中,都有同步 API,并且许多方法可能会阻塞. 一个很好的例子是 JDBC API - 它本质上是同步的,无论它多么努力,Vert.x 都无法在其上撒上魔法粉以使其异步.

我们不会在一夜之间将所有内容重写为异步,因此我们需要为您提供一种在 Vert.x 应用程序中安全地使用"传统"阻塞 API 的方法.

如前所述,您不能直接从事件循环调用阻塞操作,因为这会阻止它执行任何其他有用的工作. 那么你怎么能做到这一点呢?

这是通过调用 `executeBlocking` 来完成的,指定要执行的阻塞代码和在执行阻塞代码时异步回调的结果处理器.

```java
vertx.executeBlocking(promise -> {
  // Call some blocking API that takes a significant amount of time to return
  String result = someAPI.blockingMethod("hello");
  promise.complete(result);
}, res -> {
  System.out.println("The result is: " + res.result());
});
```

> **☢警告:** 阻塞代码应该阻塞一段合理的时间(即不超过几秒钟).长时间的阻塞操作或轮询操作(即以阻塞方式循环轮询事件的线程)要被坚决杜绝.当阻塞操作持续超过 10 秒时,阻塞线程检查器将在控制台上打印一条消息. 长阻塞操作应该使用由应用程序管理的专用线程,该线程可以使用 `事件总线` 或 `runOnContext`与verticles交互

默认情况下,如果从同一个上下文(例如,同一个verticle实例)多次调用executeBlocking,那么不同的executeBlocking将*连续*执行(即一个接一个).

如果你不关心次序,你可以调用 `executeBlocking` 指定 `false` 作为 `ordered` 的参数. 在这种情况下,任何 executeBlocking 都可以在工作池上并行执行.

运行阻塞代码的另一种方法是使用 [工作 verticle](#worker_verticles)

`worker verticle`总是使用工作池中的线程执行.

默认情况下,阻塞代码在 Vert.x 工作池上执行,可以使用 `setWorkerPoolSize` 方法来配置工作池的大小.

可以为不同目的创建额外的池:

```java
WorkerExecutor executor = vertx.createSharedWorkerExecutor("my-worker-pool");
executor.executeBlocking(promise -> {
  // Call some blocking API that takes a significant amount of time to return
  String result = someAPI.blockingMethod("hello");
  promise.complete(result);
}, res -> {
  System.out.println("The result is: " + res.result());
});
```

不再需要时,必须关闭 worker executor:

```java
executor.close();
```

当多个worker以相同的名称创建时,它们将共享相同的池.当使用该工作池的所有工作执行程序都关闭时,该工作池将被销毁.

在 Verticle 中创建 executor 时,Vert.x 会在 Verticle 取消部署时自动为您关闭它.

Worker executors可以在创建时配置:

```java
int poolSize = 10;

// 2 minutes
long maxExecuteTime = 2;
TimeUnit maxExecuteTimeUnit = TimeUnit.MINUTES;

WorkerExecutor executor = vertx.createSharedWorkerExecutor("my-worker-pool", poolSize, maxExecuteTime, maxExecuteTimeUnit);
```

> **🏷注意:** 配置是在创建工作池时设置的

## 度量指标 SPI

默认情况下,Vert.x 不记录任何指标. 相反,它提供了一个 SPI 供其他人实现,可以将其添加到类路径中. 指标 SPI 是一项高级功能,它允许实施者从 Vert.x 捕获事件以收集指标. 有关这方面的更多信息,请参阅"API 文档".

如果使用' setFactory '嵌入Vert.x,你也可以通过编程方式指定一个度量工厂.

## `vertx` 命令行

`vertx` 命令用于从命令行与 Vert.x 交互. 它的主要用途是运行 Vert.x verticles. 为此,您需要下载并安装 Vert.x 发行版,并将安装的 `bin` 目录添加到 `PATH` 环境变量中. 还要确保你的 `PATH` 上有一个 Java JDK.

Vert.x 支持 Java 8 到 17.

> **🏷注意:** 需要JDK来支持 Java 代码的动态编译.

### 运行 verticles

您可以使用 `vertx run` 直接从命令行运行原始 Vert.x verticles. 以下是 `run` *command* 的几个示例:

```bash
vertx run my-verticle.js                                 (1)
vertx run my-verticle.groovy                             (2)
vertx run my-verticle.rb                                 (3)

vertx run io.vertx.example.MyVerticle                    (4)
vertx run io.vertx.example.MVerticle -cp my-verticle.jar (5)

vertx run MyVerticle.java                                (6)
```

1. 部署一个 JavaScript verticle
2. 部署一个 Groovy verticle
3. 部署一个Ruby Verticle
4. 部署一个已经编译好的Java Verticle. 类路径根是当前目录
5. 部署一个打包在jar中的verticle,jar需要在classpath中
6. 编译Java源码并部署
正如您在 Java 的情况下看到的,名称可以是 Verticle 的完全限定类名称,也可以直接指定 Java 源文件,Vert.x 会为您编译它.

您还可以在 Verticle 前面加上要使用的语言实现的名称. 例如,如果 Verticle 是一个已编译的 Groovy 类,则在它前面加上 `groovy:` 以便 Vert.x 知道它是一个 Groovy 类而不是 Java 类.

```bash
vertx run groovy:io.vertx.example.MyGroovyVerticle
```

`vertx run` 命令可以带几个可选参数,它们是:

- `-options <options>` - 提供 Vert.x 选项. `options` 是表示 Vert.x 选项的 JSON 文件的名称,或 JSON 字符串. 这是可选的.
- `-conf <config>` - 为verticle提供一些配置. `config` 是一个 JSON 文件的名称,它代表了 Verticle 的配置,或者一个 JSON 字符串. 这是可选的.
- `-cp <path>` - 搜索 Verticle 和 Verticle 使用的任何其他资源的路径. 这默认为 `.` (当前目录). 如果您的 Verticle 引用了其他脚本,类或其他资源(例如 jar 文件),请确保它们在此路径上. 路径可以包含多个路径条目,由 `:`(冒号)或 `;`(分号)分隔,具体取决于操作系统. 每个路径条目可以是包含脚本的目录的绝对或相对路径,或者是 jar 或 zip 文件的绝对或相对文件名.一个示例路径可能是 `-cp classes:lib/otherscripts:jars/myjar.jar:jars/otherjar.jar`. 始终使用路径来引用您的 Verticle 所需的任何资源. 不要**不要**将它们放在系统类路径中,因为这可能会导致部署的 Verticle 之间出现隔离问题.
- `-instances <instances>` - 要实例化的verticle的实例数. 每个 Verticle 实例都是严格的单线程的,因此要跨可用内核扩展您的应用程序,您可能需要部署多个实例. 如果省略,将部署单个实例.
- `-worker` - 这个选项决定了这个verticle是否是一个worker Verticle.
- `-cluster` - 此选项确定 Vert.x 实例是否会尝试与网络上的其他 Vert.x 实例形成集群. 集群 Vert.x 实例允许 Vert.x 与其他节点形成分布式事件总线. 默认为 `false` (未集群).
- `-cluster-port` - 如果还指定了 `cluster` 选项,那么这将确定哪个端口将绑定到与其他 Vert.x 实例的集群通信. 默认为"0"--这意味着"*选择一个免费的随机端口*". 通常不需要指定此参数,除非您确实需要绑定到特定端口.
- `-cluster-host` - 如果还指定了 `cluster` 选项,那么这将确定哪个主机地址将绑定到与其他 Vert.x 实例的集群通信. 如果未设置,则集群事件总线会尝试绑定到与底层集群管理器相同的主机. 作为最后的手段,将在可用的网络接口中选择一个地址.
- `-cluster-public-port` - 如果还指定了 `cluster` 选项,那么这将确定哪个端口将被通告用于与其他 Vert.x 实例的集群通信. 默认为 `-1`,与 `cluster-port` 的含义相同.
- `-cluster-public-host` - 如果还指定了 `cluster` 选项,那么这将确定哪个主机地址将被通告用于与其他 Vert.x 实例的集群通信. 如果未指定,Vert.x 使用 `cluster-host` 的值.
- `-ha` - 如果指定,verticle 将被部署为高可用性 (HA) 部署. 有关详细信息,请参阅相关部分
- `-quorum` - 与 `-ha` 一起使用. 它指定集群中任何 *HA 部署 ID* 处于活动状态的最小节点数. 默认为 0.
- `-hagroup` - 与 `-ha` 一起使用. 它指定此节点将加入的 HA 组. 一个集群中可以有多个 HA 组. 节点只会故障转移到同一组中的其他节点. 默认值为`__DEFAULT__`

您还可以使用:`-Dkey=value` 设置系统属性.

以下是更多示例:

使用默认设置运行 JavaScript verticle server.js

```bash
vertx run server.js
```

运行 10 个指定类路径的预编译 Java verticle 实例

```bash
vertx run com.acme.MyVerticle -cp "classes:lib/myjar.jar" -instances 10
```

通过源 *file* 运行 10 个 Java verticle 实例

```bash
vertx run MyVerticle.java -instances 10
```

运行 20 个 ruby worker verticle 实例

```bash
vertx run order_worker.rb -instances 20 -worker
```

在同一台机器上运行两个 JavaScript verticles 并让它们彼此集群在一起以及网络上的任何其他服务器

```bash
vertx run handler.js -cluster
vertx run sender.js -cluster
```

运行一个 Ruby verticle 传递一些配置

```bash
vertx run my_verticle.rb -conf my_verticle.conf
```

其中 `my_verticle.conf` 可能包含以下内容:

```json
{
"name": "foo",
"num_widgets": 46
}
```

配置将通过核心 API 在 Verticle 内可用.

使用 vert.x 的高可用性功能时,您可能需要创建一个 *bare* vert.x 实例. 此实例在启动时不会部署任何 Verticle,但如果集群的另一个节点死亡,它将接收一个 Verticle. 要创建 *bare* 实例,请启动:

```bash
vertx bare
```

根据您的集群配置,您可能需要附加 `cluster-host` 和 `cluster-port` 参数.

### 执行打包为 fat jar 的 Vert.x 应用程序

*fat jar* 是嵌入其依赖项的可执行 jar. 这意味着您不必在执行 jar 的机器上预先安装 Vert.x. 像任何可执行的 Java jar 一样,它可以被执行.

```bash
java -jar my-application-fat.jar
```

Vert.x 对此并没有什么特别之处,您可以使用任何 Java 应用程序来执行此操作

您可以创建自己的主类并在清单中指定,但建议您将代码编写为 verticles 并使用 Vert.x `Launcher` 类 (`io.vertx.core.Launcher`) 作为主类 . 这与在命令行运行 Vert.x 时使用的主类相同,因此允许您指定命令行参数,例如 `-instances` 以便更轻松地扩展应用程序.

要像这样在 *fatjar* 中部署您的 Verticle,您必须有一个 *manifest*,其中包含:

- `Main-Class` 设置成 `io.vertx.core.Launcher`
- `Main-Verticle` 指定主verticle(完全限定的类名或脚本文件名)

您还可以提供将传递给 `vertx run` 的常用命令行参数:

```bash
java -jar my-verticle-fat.jar -cluster -conf myconf.json
java -jar my-verticle-fat.jar -cluster -conf myconf.json -cp path/to/dir/conf/cluster_xml
```

> **🏷注意:** 请参考示例存储库中的 Maven/Gradle 最简单和 Maven/Gradle verticle 示例,以获取将应用程序构建为 fatjar 的示例.

默认情况下,fat jar 会执行 `run` 命令.

### 显示Vert.x的版本

要显示 vert.x 版本,只需启动:

```bash
vertx version
```

### 其他命令

除了 `run` 和 `version` 之外,`vertx` 命令行和 `Launcher` 还提供了其他 *命令*:

您可以使用以下方法创建一个`bare(裸)`实例:

```bash
vertx bare
# or
java -jar my-verticle-fat.jar bare
```

您还可以使用以下方法在后台启动应用程序:

```bash
java -jar my-verticle-fat.jar start --vertx-id=my-app-name
```

如果 `my-app-name` 未设置,将生成一个随机 id,并打印在命令提示符上. 您可以将 `run` 选项传递给 `start` 命令:

```bash
java -jar my-verticle-fat.jar start --vertx-id=my-app-name -cluster
```

在后台启动后,您可以使用 `stop` 命令停止它:

```bash
java -jar my-verticle-fat.jar stop my-app-name
```

您还可以使用以下命令列出在后台启动的 vert.x 应用程序:

```bash
java -jar my-verticle-fat.jar list
```

`start`,`stop` 和 `list` 命令也可以从 `vertx` 工具获得. `start` 命令支持几个选项:

- `vertx-id` : 应用程序 ID,如果未设置,则使用随机 UUID
- `java-opts` : Java 虚拟机选项,如果未设置,则使用 `JAVA_OPTS` 环境变量.
- `redirect-output` : 将生成的进程输出和错误流重定向到父进程流.

如果选项值包含空格,请不要忘记将值包含在 `""`(双引号)之间.

由于 `start` 命令生成一个新进程,传递给 JVM 的 java 选项不会传播,因此您必须**使用 `java-opts` 来配置 JVM(`-X`,`-D`... ). 如果您使用 `CLASSPATH` 环境变量,请确保它包含所有必需的 jars(vertx-core,您的 jars 和所有依赖项).

该命令集是可扩展的,请参阅 [Extending the vert.x Launcher](#_extending_the_vert_x_launcher) 部分.

### 实时重新部署(热部署)

在开发时,在文件更改时自动重新部署应用程序可能会很方便. `vertx` 命令行工具和更普遍的 `Launcher` 类提供了此功能. 这里有些例子:

```bash
vertx run MyVerticle.groovy --redeploy="**/*.groovy" --launcher-class=io.vertx.core.Launcher
vertx run MyVerticle.groovy --redeploy="**/*.groovy,**/*.rb"  --launcher-class=io.vertx.core.Launcher
java io.vertx.core.Launcher run org.acme.MyVerticle --redeploy="**/*.class"  --launcher-class=io.vertx.core
.Launcher -cp ...
```

重新部署过程执行如下. 首先,您的应用程序作为后台应用程序启动(使用 `start` 命令). 在匹配文件更改时,该进程将停止并重新启动应用程序. 这样可以避免泄漏,因为该进程会重新启动.

要启用实时重新部署,请将 `--redeploy` 选项传递给 `run` 命令. `--redeploy` 指示要 *watch* 的文件集. 该集合可以使用 Ant 样式的模式(使用 `**`,`*` 和 `?`). 您可以通过使用逗号 (`,`) 分隔它们来指定多个集合. 模式是相对于当前工作目录的.

传递给`run` 命令的参数被传递给应用程序. Java 虚拟机选项可以使用 `--java-opts` 进行配置. 例如,要传递 `conf` 参数或系统属性,您需要使用: `--java-opts="-conf=my-conf.json -Dkey=value"`

`--launcher-class` 选项与 *main* 类一起确定应用程序是启动器. 它通常是`Launcher`,但你也可以使用你自己的*main*.

可以在您的 IDE 中使用重新部署功能:

- Eclipse - 创建一个 *Run* 配置,使用 `io.vertx.core.Launcher` 类作为 *main class*. 在 *Program arguments* 区域(在 *Arguments* 选项卡中),编写 `run your-verticle-fully-qualified-name --redeploy=**/*.java --launcher-class=io.vertx.core.Launcher`. 您还可以添加其他参数. 随着 Eclipse 在保存时增量编译您的文件,重新部署工作顺利进行.
- IntelliJ - 创建一个 *Run* 配置 (*Application*),将 *Main class* 设置为 `io.vertx.core.Launcher`. 在程序参数中写入:`run your-verticle-fully-qualified-name --redeploy=**/*.class --launcher-class=io.vertx.core.Launcher`. 要触发重新部署,您需要显式地 *make* 项目或模块(*Build* 菜单 → *Make project*).

要调试您的应用程序,请将您的运行配置创建为远程应用程序并使用 --java-opts 配置调试器. 但是,不要忘记在每次重新部署后重新插入调试器,因为每次都会创建一个新进程.

您还可以在重新部署周期中挂钩您的构建过程:

```
java -jar target/my-fat-jar.jar --redeploy="**/*.java" --on-redeploy="mvn package"
java -jar build/libs/my-fat-jar.jar --redeploy="src/**/*.java" --on-redeploy='./gradlew shadowJar'
```

"on-redeploy"选项指定在应用程序关闭之后和重新启动之前调用的命令. 因此,如果它更新了一些运行时工件,您可以挂钩您的构建工具. 例如,您可以启动 `gulp` 或 `grunt` 来更新您的资源. 不要忘记向应用程序传递参数需要 `--java-opts` 参数:

```
java -jar target/my-fat-jar.jar --redeploy="**/*.java" --on-redeploy="mvn package" --java-opts="-Dkey=val"
java -jar build/libs/my-fat-jar.jar --redeploy="src/**/*.java" --on-redeploy='./gradlew shadowJar' --java-opts="-Dkey=val"
```

重新部署功能还支持以下设置:

- `redeploy-scan-period` : 文件系统检查周期(毫秒),默认250ms
- `redeploy-grace-period` : 在 2 次重新部署之间等待的时间(以毫秒为单位),默认为 1000 毫秒
- `redeploy-termination-period` : 停止应用程序后(启动用户命令之前)等待的时间. 这在 Windows 上很有用,在 Windows 中进程不会立即被终止. 时间以毫秒为单位. 默认为 0 毫秒.

## 集群管理器

在 Vert.x 中,集群管理器用于各种功能,包括:

- 集群中 Vert.x 节点的发现和组成员身份
- 维护集群范围的主题订阅者列表(因此我们知道哪些节点对哪些事件总线地址感兴趣)
- 分布式Map支持
- 分布式锁
- 分布式计数器

集群管理器*不*处理节点间的事件总线传输,这是由 Vert.x 通过 TCP 连接直接完成的.

Vert.x 发行版中使用的默认集群管理器是使用 [Hazelcast](http://hazelcast.com/) 的集群管理器,但由于 Vert.x 集群管理器是可插拔的,因此可以很容易地用不同的实现替换它.

集群管理器必须实现接口`ClusterManager`. Vert.x 在运行时通过使用 Java [Service Loader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) 功能定位集群管理器来定位 类路径上的`ClusterManager`.

如果您在命令行中使用 Vert.x 并且想要使用集群,则应确保 Vert.x 安装的 `lib` 目录包含集群管理器 jar.

如果您在 Maven 或 Gradle 项目中使用 Vert.x,只需将集群管理器 jar 添加为项目的依赖项.

如果使用 `setClusterManager` 嵌入 Vert.x,您还可以通过编程方式指定集群管理器.

## 日志记录

Vert.x 使用其内部日志 API 进行日志记录,并支持各种日志后端.

日志后端选择如下:

1. 由 `vertx.logger-delegate-factory-class-name` 系统属性表示的后端(如果存在),或者,
2. 当 `vertx-default-jul-logging.properties` 文件位于类路径中时的 JDK 日志记录,或者,
3. 类路径中存在的后端,按以下优先顺序:
   1. SLF4J
   2. Log4J
   3. Log4J2

否则 Vert.x 默认使用 JDK 日志记录.

### 使用系统属性进行配置

将 `vertx.logger-delegate-factory-class-name` 系统属性设置为:

- `io.vertx.core.logging.SLF4JLogDelegateFactory` 给 SLF4J 或者,
- `io.vertx.core.logging.Log4j2LogDelegateFactory` 给 Log4J2 或者,
- `io.vertx.core.logging.JULLogDelegateFactory` 给 JDK logging

### 自动配置

当没有设置 `vertx.logger-delegate-factory-class-name` 系统属性时,Vert.x 将尝试找到最合适的日志记录器:

- 在具有实际实现的类路径上可用时使用 SLF4J(即 `LoggerFactory.getILoggerFactory()` 不是 `NOPLoggerFactory` 的实例)
- 否则在类路径上可用时使用 Log4j2
- 否则使用 JUL

### 配置 JUL 日志记录

通过提供名为`java.util.logging.config.file`的系统属性,其值是您的配置文件,可以以正常的 JUL 方式指定 JUL 日志记录配置文件. 有关此内容和 JUL 配置文件结构的更多信息,请参阅 JDK 日志记录文档.

Vert.x 还提供了一种更方便的方式来指定配置文件,而无需设置系统属性. 只需在你的类路径(例如在你的 fatjar 中)提供一个名为 `vertx-default-jul-logging.properties` 的 JUL 配置文件,Vert.x 将使用它来配置 JUL.

### Netty 日志记录

Netty 不依赖于外部日志配置(例如系统属性). 相反,它基于 Netty 类中可见的日志库实现了日志配置:

- 使用 `SLF4J` 库,如果它是可见的
- 否则使用 `Log4j` 如果它是可见的
- 否则使用 `Log4j2` 如果它是可见的
- 否则回退到 `java.util.logging`

> **🏷注意:** 你们中的鹰眼可能已经注意到 Vert.x 遵循相同的优先顺序.

通过直接在 io.netty.util.internal.logging.InternalLoggerFactory 上设置 Netty 的内部日志记录器实现,可以将日志记录器实现强制为特定实现:

```java
// Force logging to Log4j 2
InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);
```

### 故障排除

#### 启动时的 SLF4J 警告

如果在启动应用程序时看到以下消息:

```
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
```

这意味着您的类路径中有 SLF4J-API 但没有实际绑定. 使用 SLF4J 记录的消息将被丢弃. 您应该将绑定添加到您的类路径. 检查 `https://www.slf4j.org/manual.html#swapping` 以选择绑定并进行配置.

请注意,Netty 会查找 SLF4-API jar 并默认使用它.

#### 对等点重置连接

如果您的日志显示一堆:

```
io.vertx.core.net.impl.ConnectionBase
SEVERE: java.io.IOException: Connection reset by peer
```

这意味着客户端正在重置 HTTP 连接而不是关闭它. 此消息还表明您可能尚未使用完整的有效负载(在您能够使用之前连接已断开).

## 主机名解析

Vert.x 使用地址解析器将主机名解析为 IP 地址,而不是 JVM 内置的阻塞解析器.

主机名使用以下方式解析为 IP 地址:

- 操作系统的 *hosts* 文件
- 否则对服务器列表进行 DNS 查询

默认情况下,它将使用环境中的系统 DNS 服务器地址列表,如果无法检索该列表,它将使用 Google 的公共 DNS 服务器`8.8.8.8`和`8.8.4.4`.

创建 `Vertx` 实例时也可以配置 DNS 服务器:

```java
Vertx vertx = Vertx.vertx(new VertxOptions().
    setAddressResolverOptions(
        new AddressResolverOptions().
            addServer("192.168.0.1").
            addServer("192.168.0.2:40000"))
);
```

DNS 服务器的默认端口是`53`,当服务器使用不同的端口时,可以使用冒号分隔符设置该端口:`192.168.0.2:40000`.

> **🏷注意:** 有时可能需要使用 JVM 内置解析器,JVM 系统属性 `-Dvertx.disableDnsResolver=true` 会激活此行为

### 故障转移

当服务器没有及时回复时,解析器将尝试列表中的下一个,搜索受限于`setMaxQueries`(默认值为`4`个查询).

当解析器在 `getQueryTimeout` 毫秒(默认值为 `5` 秒)内未收到正确答案时,DNS 查询被视为失败.

### 服务器列表轮换

默认情况下,dns 服务器选择使用第一个,其余服务器用于故障转移.

您可以将 `setRotateServers` 配置为 `true` 以让解析器执行循环选择. 它在服务器之间分散查询负载,并避免所有查找命中列表的第一个服务器.

故障转移仍然适用,并将使用列表中的下一个服务器.

### 主机映射

操作系统的 *hosts* 文件用于对 ipaddress 执行主机名查找.

可以使用替代的 *hosts* 文件:

```java
Vertx vertx = Vertx.vertx(new VertxOptions().
    setAddressResolverOptions(
        new AddressResolverOptions().
            setHostsPath("/path/to/hosts"))
);
```

### 搜索域

默认情况下,解析器将使用环境中的系统 DNS 搜索域. 或者,可以提供显式搜索域列表:

```java
Vertx vertx = Vertx.vertx(new VertxOptions().
    setAddressResolverOptions(
        new AddressResolverOptions().addSearchDomain("foo.com").addSearchDomain("bar.com"))
);
```

When a search domain list is used, the threshold for the number of dots is `1` or loaded from `/etc/resolv.conf` on Linux, it can be configured to a specific value with `setNdots`.

### MacOS 配置

MacOS 有一个特定的原生扩展,可以基于 <a href="https://opensource.apple.com/tarballs/mDNSResponder/">Apple 的开源 mDNSResponder</a> 获取系统的名称服务器配置. 当此扩展不存在时,Netty 会记录以下警告.

```
[main] WARN io.netty.resolver.dns.DnsServerAddressStreamProviders - Can not find io.netty.resolver.dns.macos.MacOSDnsServerAddressStreamProvider in the classpath, fallback to system defaults. This may result in incorrect DNS resolutions on MacOS.
```

这个扩展不是必需的,因为它的缺失不会阻止 Vert.x 的执行,但是是**推荐的**.

您可以使用将其添加到您的类路径来改进集成并删除警告.

```xml
<profile>
 <id>mac</id>
 <activation>
   <os>
     <family>mac</family>
   </os>
 </activation>
 <dependencies>
   <dependency>
     <groupId>io.netty</groupId>
     <artifactId>netty-resolver-dns-native-macos</artifactId>
     <classifier>osx-x86_64</classifier>
     <!--<version>Should align with netty version that Vert.x uses</version>-->
   </dependency>
 </dependencies>
</profile>
```

<a name="high_availability_and_fail_over"></a>
## 高可用性和故障转移

Vert.x 允许您运行具有高可用性 (HA) 支持的 Verticle. 在这种情况下,当运行 verticle 的 vert.x 实例突然死亡时,verticle 会迁移到另一个 vertx 实例. vert.x 实例必须在同一个集群中.

### 自动故障转移

当 vert.x 在启用 *HA* 的情况下运行时,如果一个 verticle 运行的 vert.x 实例失败或死亡,则该 verticle 会自动重新部署到集群的另一个 vert.x 实例上. 我们称之为*verticle 故障转移*.

要在启用 *HA* 的情况下运行 vert.x,只需将 `-ha` 标志添加到命令行:

```bash
vertx run my-verticle.js -ha
```

现在要使 HA 工作,您需要集群中的多个 Vert.x 实例,所以假设您已经启动了另一个 Vert.x 实例,例如:

```bash
vertx run my-other-verticle.js -ha
```

如果正在运行 `my-verticle.js` 的 Vert.x 实例现在死掉了(您可以通过使用 `kill -9` 终止进程来测试它),运行 `my-other-verticle.js` 的 Vert.x 实例 将自动部署 `my-verticle .js` 所以现在 Vert.x 实例正在运行两个 verticles.

> **🏷注意:** 仅当第二个 vert.x 实例可以访问 verticle 文件(此处为 `my-verticle.js`)时,才能进行迁移.

> **⚠重要:** 请注意,干净地关闭 Vert.x 实例不会导致发生故障转移,例如 `CTRL-C` 或 `kill -SIGINT`

您还可以启动 *bare*  Vert.x 实例 - 即最初不运行任何 Verticle 的实例,它们还将为集群中的节点进行故障转移. 要启动一个裸实例,您只需执行以下操作:

```bash
vertx run -ha
```

使用 `-ha` 开关时,您不需要提供 `-cluster` 开关,因为如果您需要 HA,则假定为集群.

> **🏷注意:** 根据您的集群配置,您可能需要自定义集群管理器配置(默认为 Hazelcast),和/或添加 `cluster-host` 和 `cluster-port` 参数.

### HA 组

使用 HA 运行 Vert.x 实例时,您还可以选择指定 *HA 组*. HA 组表示集群中的一个逻辑节点组. 只有具有相同 HA 组的节点才会故障转移到另一个节点上. 如果您未指定 HA 组,则使用默认组 `__DEFAULT__`.

要指定 HA 组,您可以在运行 verticle 时使用 `-hagroup` 开关,例如

```bash
vertx run my-verticle.js -ha -hagroup my-group
```

让我们看一个例子:

在第一个终端:

```bash
vertx run my-verticle.js -ha -hagroup g1
```

在第二个终端中,让我们使用同一组运行另一个 Verticle:

```bash
vertx run my-other-verticle.js -ha -hagroup g1
```

最后,在第三个终端中,使用不同的组启动另一个 Verticle:

```bash
vertx run yet-another-verticle.js -ha -hagroup g2
```

如果我们杀死终端 1 中的实例,它将故障转移到终端 2 中的实例,而不是终端 3 中的实例,因为它具有不同的组.

如果我们在终端 3 中终止实例,它不会发生故障转移,因为该组中没有其他 vert.x 实例.

### 处理网络分区 - Quora(法定人数)

HA 实现也支持 quora. 法定人数是分布式事务为了被允许在分布式系统中执行操作而必须获得的最小投票数.

启动 Vert.x 实例时,您可以指示它在部署任何 HA 部署之前需要一个 `quorum`. 在这种情况下,仲裁是集群中特定组的最小节点数. 通常,您将仲裁大小选择为`Q = 1 + N/2`,其中`N`是组中的节点数. 如果集群中的节点数少于"Q"个,HA 部署将取消部署. 如果/当重新达到法定人数时,他们将再次重新部署. 通过这样做,您可以防止网络分区,也就是*裂脑*.

有更多关于 quora 的信息 [这里](https://en.wikipedia.org/wiki/Quorum_(distributed_computing)).

要使用仲裁运行 vert.x 实例,请在命令行上指定 `-quorum`,例如

在第一个终端:

```bash
vertx run my-verticle.js -ha -quorum 3
```

此时 Vert.x 实例将启动但尚未部署模块,因为集群中只有一个节点,而不是 3 个.

在第二个终端:

```bash
vertx run my-other-verticle.js -ha -quorum 3
```

此时 Vert.x 实例将启动但尚未部署模块,因为集群中只有两个节点,而不是 3 个.

在第三个控制台中,您可以启动另一个 vert.x 实例:

```bash
vertx run yet-another-verticle.js -ha -quorum 3
```

耶! - 我们有三个节点,此时符合法定人数了. 此时,模块将自动部署在所有实例上.

如果我们现在关闭或杀死其中一个节点,模块将自动取消部署在其他节点上,因为不再有仲裁.

Quora 也可以与 ha 组一起使用. 在这种情况下,会为每个特定组解析 quora.

## 本机传输

Vert.x 可以在 BSD (OSX) 和 Linux 上使用 [native transports](http://netty.io/wiki/native-transports.html)(如果可用)运行:

```java
Vertx vertx = Vertx.vertx(new VertxOptions().
  setPreferNativeTransport(true)
);

// True when native is available
boolean usingNative = vertx.isNativeTransportEnabled();
System.out.println("Running with native: " + usingNative);
```

> **🏷注意:** 首选本机传输不会阻止应用程序执行(例如,如果缺少 JAR). 如果您的应用程序需要本地传输,则需要检查 `isNativeTransportEnabled`.

### 本机 Linux 传输

您需要在类路径中添加以下依赖项:

```xml
<dependency>
 <groupId>io.netty</groupId>
 <artifactId>netty-transport-native-epoll</artifactId>
 <classifier>linux-x86_64</classifier>
 <!--<version>Should align with netty version that Vert.x uses</version>-->
</dependency>
```

Linux 上的 Native 为您提供了额外的网络选项:

- `SO_REUSEPORT`
- `TCP_QUICKACK`
- `TCP_CORK`
- `TCP_FASTOPEN`

```java
vertx.createHttpServer(new HttpServerOptions()
  .setTcpFastOpen(fastOpen)
  .setTcpCork(cork)
  .setTcpQuickAck(quickAck)
  .setReusePort(reusePort)
);
```

### 本机 BSD 传输

您需要在类路径中添加以下依赖项:

```xml
<dependency>
 <groupId>io.netty</groupId>
 <artifactId>netty-transport-native-kqueue</artifactId>
 <classifier>osx-x86_64</classifier>
 <!--<version>Should align with netty version that Vert.x uses</version>-->
</dependency>
```

支持 MacOS Sierra 及更高版本.

BSD 上的本机为您提供了额外的网络选项:

- `SO_REUSEPORT`

```java
vertx.createHttpServer(new HttpServerOptions().setReusePort(reusePort));
```

### 域套接字

Natives 为服务器提供域套接字支持:

```java
vertx.createNetServer().connectHandler(so -> {
  // Handle application
}).listen(SocketAddress.domainSocketAddress("/var/tmp/myservice.sock"));
```

或 http:

```java
vertx.createHttpServer().requestHandler(req -> {
  // Handle application
}).listen(SocketAddress.domainSocketAddress("/var/tmp/myservice.sock"), ar -> {
  if (ar.succeeded()) {
    // Bound to socket
  } else {
    ar.cause().printStackTrace();
  }
});
```

以及客户端:

```java
NetClient netClient = vertx.createNetClient();

// Only available on BSD and Linux
SocketAddress addr = SocketAddress.domainSocketAddress("/var/tmp/myservice.sock");

// Connect to the server
netClient.connect(addr, ar -> {
  if (ar.succeeded()) {
    // Connected
  } else {
    ar.cause().printStackTrace();
  }
});
```

或 http:

```java
HttpClient httpClient = vertx.createHttpClient();

// Only available on BSD and Linux
SocketAddress addr = SocketAddress.domainSocketAddress("/var/tmp/myservice.sock");

// Send request to the server
httpClient.request(new RequestOptions()
  .setServer(addr)
  .setHost("localhost")
  .setPort(8080)
  .setURI("/"))
  .onSuccess(request -> {
    request.send().onComplete(response -> {
      // Process response
    });
  });
```
<a name="_security_notes"></a>
## 安全说明

Vert.x 是一个工具包,而不是一个固执己见的框架,我们强迫你以某种方式做事. 这为您作为开发人员提供了强大的力量,但随之而来的是巨大的责任.

与任何工具包一样,可能会编写不安全的应用程序,因此在开发应用程序时应始终小心谨慎,尤其是当它向公众公开时(例如通过互联网).

### Web 应用程序

如果编写 Web 应用程序,强烈建议您直接使用 Vert.x-Web 而不是 Vert.x core来提供资源和处理文件上传.

Vert.x-Web 规范化请求中的路径,以防止恶意客户端制作 URL 以访问 Web 根目录之外的资源.

同样,对于文件上传,Vert.x-Web 提供了上传到磁盘上已知位置的功能,并且不依赖于客户端在上传中提供的文件名,该文件名可以被精心设计为上传到磁盘上的不同位置.

Vert.x 核心本身不提供此类检查,因此作为开发人员,您可以自己实现它们.

### 集群事件总线通信

在网络上的不同 Vert.x 节点之间对事件总线进行集群时,通过线路发送的流量未加密,因此如果您有机密数据要发送并且您的 Vert.x 节点不在受信任的网络上,请不要使用它 .

### 标准安全最佳实践

无论是使用 Vert.x 还是任何其他工具包编写的任何服务都可能存在潜在漏洞,因此请始终遵循安全最佳实践,尤其是在您的服务面向公众的情况下.

例如,您应该始终在 DMZ 中使用具有有限权限的用户帐户运行它们,以便在服务受到损害时限制损坏程度.

## Vert.x 命令行接口 API

Vert.x Core 提供了一个 API 用于解析传递给程序的命令行参数. 它还能够打印帮助消息,详细说明可用于命令行工具的选项. 即使这些功能与 Vert.x 的核心主题相去甚远,这个 API 也可以在 `Launcher` 类中使用,您可以在 *fat-jar* 和 `vertx` 命令行工具中使用它. 此外,它是多语言的(可以从任何受支持的语言中使用)并且在 Vert.x Shell 中使用.

Vert.x CLI 提供了一个模型来描述你的命令行界面,同时也提供了一个解析器. 此解析器支持不同类型的语法:

- 类似 POSIX 的选项 (例如. `tar -zxvf foo.tar.gz`)
- 类似 GNU 的长选项 (例如. `du --human-readable --max-depth=1`)
- 类似 Java 的选项 (例如. `java -Djava.awt.headless=true -Djava.net.useSystemProxies=true Foo`)
- 有附加值的短选项 (例如. `gcc -O2 foo.c`)
- 带有单连字符的长选项 (例如. `ant -projecthelp`)

使用 CLI api 是一个 3 个步骤的过程:

1.命令行界面的定义
2.用户命令行的解析
3.查询/审讯

### 定义阶段

每个命令行界面都必须定义将使用的一组选项和参数. 它还需要一个名字. CLI API 使用 `Option` 和 `Argument` 类来描述选项和参数:

```java
CLI cli = CLI.create("copy")
    .setSummary("A command line interface to copy files.")
    .addOption(new Option()
        .setLongName("directory")
        .setShortName("R")
        .setDescription("enables directory support")
        .setFlag(true))
    .addArgument(new Argument()
        .setIndex(0)
        .setDescription("The source")
        .setArgName("source"))
    .addArgument(new Argument()
        .setIndex(1)
        .setDescription("The destination")
        .setArgName("target"));
```

如您所见,您可以使用 `CLI.create` 创建一个新的 `CLI`. 传递的字符串是 CLI 的名称. 创建后,您可以设置摘要和描述. 摘要旨在简短(一行),而描述可以包含更多细节. 每个选项和参数也使用 `addArgument` 和 `addOption` 方法添加到 `CLI` 对象上.

#### 选项

`Option` 是一个命令行参数,由用户命令行中的 *key* 标识. 选项必须至少有一个长名称或一个短名称. 长名称通常使用 `--` 前缀,而短名称使用单个 `-`.名称区分大小写; 但是,如果没有找到完全匹配,将在 [查询/询问阶段](#query_interrogation_stage) 期间使用不区分大小写的名称匹配. 选项可以获得使用中显示的描述(见下文). 选项可以接收 0,1 或多个值. 接收 0 值的选项是一个 `flag`,必须使用 `setFlag` 声明. 默认情况下,选项接收单个值,但是,您可以使用 `setMultiValued` 配置选项以接收多个值:

```java
CLI cli = CLI.create("some-name")
    .setSummary("A command line interface illustrating the options valuation.")
    .addOption(new Option()
        .setLongName("flag").setShortName("f").setFlag(true).setDescription("a flag"))
    .addOption(new Option()
        .setLongName("single").setShortName("s").setDescription("a single-valued option"))
    .addOption(new Option()
        .setLongName("multiple").setShortName("m").setMultiValued(true)
        .setDescription("a multi-valued option"));
```

选项可以标记为必填项. 未在用户命令行中设置的强制选项在解析期间会引发异常:

```java
CLI cli = CLI.create("some-name")
    .addOption(new Option()
        .setLongName("mandatory")
        .setRequired(true)
        .setDescription("a mandatory option"));
```

非强制性选项可以有一个*默认值*. 如果用户未在命令行中设置选项,则将使用此值:

```java
CLI cli = CLI.create("some-name")
    .addOption(new Option()
        .setLongName("optional")
        .setDefaultValue("hello")
        .setDescription("an optional option with a default value"));
```

使用 `setHidden` 方法可以*隐藏* 选项. 隐藏选项未在用法中列出,但仍可在用户命令行中使用(对于高级用户).

如果选项值被限制在一个固定的集合中,你可以设置不同的可接受的选择:

```java
CLI cli = CLI.create("some-name")
    .addOption(new Option()
        .setLongName("color")
        .setDefaultValue("green")
        .addChoice("blue").addChoice("red").addChoice("green")
        .setDescription("a color"));
```

选项也可以从JSON中实例化.

#### 参数

与选项不同,参数没有 *key* 并且由它们的 *index* 标识. 例如,在 `java com.acme.Foo` 中,`com.acme.Foo` 是一个参数.

参数没有名称,使用从 0 开始的索引来标识. 第一个参数的索引为`0`:

```java
CLI cli = CLI.create("some-name")
    .addArgument(new Argument()
        .setIndex(0)
        .setDescription("the first argument")
        .setArgName("arg1"))
    .addArgument(new Argument()
        .setIndex(1)
        .setDescription("the second argument")
        .setArgName("arg2"));
```

如果您不设置参数索引,它会使用声明顺序自动计算它.

```java
CLI cli = CLI.create("some-name")
    // will have the index 0
    .addArgument(new Argument()
        .setDescription("the first argument")
        .setArgName("arg1"))
    // will have the index 1
    .addArgument(new Argument()
        .setDescription("the second argument")
        .setArgName("arg2"));
```

`argName`是可选的,在用法消息中使用.

作为选项,`Argument` 可以:

- 使用 `setHidden` 隐藏
- 必须使用 `setRequired`
- 使用 `setDefaultValue` 有一个默认值
- 使用 `setMultiValued` 接收多个值 - 只有最后一个参数可以是多值的.

参数也可以从JSON中实例化.

#### 生成 Usage 消息

配置好 `CLI` 实例后,您可以生成 *usage* 消息:

```java
CLI cli = CLI.create("copy")
    .setSummary("A command line interface to copy files.")
    .addOption(new Option()
        .setLongName("directory")
        .setShortName("R")
        .setDescription("enables directory support")
        .setFlag(true))
    .addArgument(new Argument()
        .setIndex(0)
        .setDescription("The source")
        .setArgName("source"))
    .addArgument(new Argument()
        .setIndex(0)
        .setDescription("The destination")
        .setArgName("target"));

StringBuilder builder = new StringBuilder();
cli.usage(builder);
```

它会生成这样一条使用消息:

```
Usage: copy [-R] source target

A command line interface to copy files.

 -R,--directory   enables directory support
```

如果您需要调整使用消息,请检查 `UsageMessageFormatter` 类.

### 解析阶段

一旦配置了`CLI`实例,就可以解析用户命令行来评估每个选项和参数:

```java
CommandLine commandLine = cli.parse(userCommandLineArguments);
```

`parse` 方法返回一个包含值的`CommandLine` 对象. 默认情况下,它会验证用户命令行并检查每个强制选项和参数是否已设置以及每个选项接收的值的数量. 您可以通过传递 `false` 作为 `parse` 的第二个参数来禁用验证. 如果您想检查参数或选项是否存在,即使解析的命令行无效,这也很有用.

您可以使用 `isValid` 检查 `CommandLine` 是否有效.

<a name="query_interrogation_stage"></a>
### 查询/询问阶段

解析后,您可以从 `parse` 方法返回的 `CommandLine` 对象中检索选项和参数的值:

```java
CommandLine commandLine = cli.parse(userCommandLineArguments);
String opt = commandLine.getOptionValue("my-option");
boolean flag = commandLine.isFlagEnabled("my-flag");
String arg0 = commandLine.getArgumentValue(0);
```

您的选项之一可以标记为"帮助". 如果用户命令行启用了"帮助"选项,验证不会失败,但您有机会检查用户是否请求帮助:

```java
CLI cli = CLI.create("test")
    .addOption(
        new Option().setLongName("help").setShortName("h").setFlag(true).setHelp(true))
    .addOption(
        new Option().setLongName("mandatory").setRequired(true));

CommandLine line = cli.parse(Collections.singletonList("-h"));

// The parsing does not fail and let you do:
if (!line.isValid() && line.isAskingForHelp()) {
  StringBuilder builder = new StringBuilder();
  cli.usage(builder);
  stream.print(builder.toString());
}
```

### 类型化选项和参数

所描述的 `Option` 和 `Argument` 类是 *untyped* 的,这意味着只能获取 String 值. `TypedOption` 和 `TypedArgument` 让您指定 *type*,因此 (String) 原始值转换为指定类型.

在 `CLI` 定义中使用 `TypedOption` 和 `TypedArgument` 代替 `Option` 和 `Argument`:

```java
CLI cli = CLI.create("copy")
    .setSummary("A command line interface to copy files.")
    .addOption(new TypedOption<Boolean>()
        .setType(Boolean.class)
        .setLongName("directory")
        .setShortName("R")
        .setDescription("enables directory support")
        .setFlag(true))
    .addArgument(new TypedArgument<File>()
        .setType(File.class)
        .setIndex(0)
        .setDescription("The source")
        .setArgName("source"))
    .addArgument(new TypedArgument<File>()
        .setType(File.class)
        .setIndex(1)
        .setDescription("The destination")
        .setArgName("target"));
```

然后您可以按如下方式检索转换后的值:

```java
CommandLine commandLine = cli.parse(userCommandLineArguments);
boolean flag = commandLine.getOptionValue("R");
File source = commandLine.getArgumentValue("source");
File target = commandLine.getArgumentValue("target");
```

vert.x CLI 能够转换为类:

- 有一个带有单个 `String` 参数的构造函数,例如 `File` 或 `JsonObject`
- 使用静态 `from` 或 `fromString` 方法
- 使用静态的 `valueOf` 方法,例如原始类型和枚举

此外,您可以实现自己的 `Converter` 并指示 CLI 使用此转换器:

```java
CLI cli = CLI.create("some-name")
    .addOption(new TypedOption<Person>()
        .setType(Person.class)
        .setConverter(new PersonConverter())
        .setLongName("person"));
```

对于布尔值,`on`,`yes`,`1`,`true`被评估为 `true`.

如果您的一个选项有一个"枚举"类型,它会自动计算一组选项.

### 使用注解

您还可以使用注解定义 CLI. 定义是使用类和 *setter* 方法上的注解完成的:

```java
@Name("some-name")
@Summary("some short summary.")
@Description("some long description")
public class AnnotatedCli {

 private boolean flag;
 private String name;
 private String arg;

@Option(shortName = "f", flag = true)
public void setFlag(boolean flag) {
  this.flag = flag;
}

@Option(longName = "name")
public void setName(String name) {
  this.name = name;
}

@Argument(index = 0)
public void setArg(String arg) {
 this.arg = arg;
}
}
```

注解后,您可以定义`CLI`并使用以下方法注入值:

```java
CLI cli = CLI.create(AnnotatedCli.class);
CommandLine commandLine = cli.parse(userCommandLineArguments);
AnnotatedCli instance = new AnnotatedCli();
CLIConfigurator.inject(commandLine, instance);
```

## vert.x 的启动器(Launcher)

vert.x `Launcher` 在 *fat jar* 中用作主类,并由 `vertx` 命令行实用程序使用. 它执行一组*commands*,例如*run*,*bare*,*start*…

<a name="_extending_the_vert_x_launcher"></a>
### 扩展 vert.x 启动器

您可以通过实现自己的`Command`来扩展命令集(仅限 Java):

```java
@Name("my-command")
@Summary("A simple hello command.")
public class MyCommand extends DefaultCommand {

 private String name;

 @Option(longName = "name", required = true)
 public void setName(String n) {
   this.name = n;
 }

 @Override
 public void run() throws CLIException {
   System.out.println("Hello " + name);
 }
}
```

你还需要一个 `CommandFactory` 的实现:

```java
public class HelloCommandFactory extends DefaultCommandFactory<HelloCommand> {
 public HelloCommandFactory() {
  super(HelloCommand.class);
 }
}
```

然后,创建 `src/main/resources/META-INF/services/io.vertx.core.spi.launcher.CommandFactory` 并添加一行指示工厂的完全限定名称:

```
io.vertx.core.launcher.example.HelloCommandFactory
```

构建包含命令的 jar. 确保包含 SPI 文件(`META-INF/services/io.vertx.core.spi.launcher.CommandFactory`).

然后,将包含命令的 jar 放入 fat-jar 的类路径(或将其包含在其中)或 vert.x 发行版的 `lib` 目录中,您将能够执行:

```bash
vertx hello vert.x
java -jar my-fat-jar.jar hello vert.x
```

### 在fat jars中使用启动器

要在 *fat-jar* 中使用 `Launcher` 类,只需将 *MANIFEST* 的 `Main-Class` 设置为 `io.vertx.core.Launcher`. 此外,将 `Main-Verticle` 条目设置为您的主 Verticle 的名称.

默认情况下,它执行 `run` 命令. 但是,您可以通过设置 `Main-Command` *MANIFEST* 条目来配置默认命令. 如果 *fat jar* 在没有命令的情况下启动,则使用默认命令.

### 子类化启动器

您还可以创建一个 `Launcher` 的子类来启动您的应用程序. 该类被设计为易于扩展.

`Launcher` 子类可以:

- 在 `beforeStartingVertx` 中自定义 vert.x 配置
- 通过覆盖 `afterStartingVertx` 检索由"run"或"bare"命令创建的 vert.x 实例
- 使用 `getMainVerticle` 和 `getDefaultCommand` 配置默认verticle和命令
- 使用 `register` 和 `unregister` 添加/删除命令

### 启动器和退出代码

当您使用 `Launcher` 类作为主类时,它使用以下退出代码:

- `0` 如果进程顺利结束,或者如果抛出未捕获的错误
- `1` 表示通用错误
- `11` 如果 Vert.x 无法初始化
- `12` 如果生成过程无法启动,找到或停止. `start` 和 `stop` 命令使用此错误代码
- `14` 如果系统配置不满足系统要求(shc as java not found)
- `15` 如果无法部署主verticle

## 配置 Vert.x 缓存

当 Vert.x 需要从类路径中读取文件时(嵌入到 fat jar 中,以 jar 形式的类路径或类路径上的文件),它会将其复制到缓存目录中. 这背后的原因很简单:从 jar 或输入流中读取文件是阻塞的. 因此,为了避免每次都付出代价,Vert.x 将文件复制到其缓存目录并在每次后续读取时从那里读取. 可以配置此行为.

首先,默认情况下,Vert.x 使用 `$CWD/.vertx` 作为缓存目录. 它在此目录中创建一个唯一目录以避免冲突. 可以使用 `vertx.cacheDirBase` 系统属性来配置此位置. 例如,如果当前工作目录不可写(例如在不可变的容器上下文中),请使用以下命令启动您的应用程序:

```bash
vertx run my.Verticle -Dvertx.cacheDirBase=/tmp/vertx-cache
# or
java -jar my-fat.jar vertx.cacheDirBase=/tmp/vertx-cache
```

> **⚠重要:** 此目录必须是**可写的**.

当您编辑 HTML,CSS 或 JavaScript 等资源时,这种缓存机制可能很烦人,因为它只提供文件的第一个版本(因此,如果您重新加载页面,您将看不到您的编辑).
要避免这种行为,请使用 `-Dvertx.disableFileCaching=true` 启动您的应用程序.使用此设置,Vert.x仍然使用缓存,但总是用原始源刷新缓存中存储的版本.因此,如果编辑从类路径提供的文件并刷新浏览器,Vert.x将从类路径读取该文件,将其复制到缓存目录并从那里提供该文件.不要在生产中使用此设置,它会扼杀性能.

最后,您可以使用 `-Dvertx.disableFileCPResolving=true` 完全禁用缓存. 这种设置并非没有后果. Vert.x 将无法从类路径中读取任何文件(只能从文件系统中读取). 使用此设置时要非常小心.


