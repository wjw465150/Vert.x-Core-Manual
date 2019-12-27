# VertX核心手册 Java版 {#VertX_Core_Manual_Java}

Vert.x的核心是一组Java API，我们称为**Vert.x Core**

[Repository](https://github.com/eclipse/vert.x).

Vert.x核心为以下事情提供功能：

- 编写TCP客户端和服务器
- 编写HTTP客户端和服务器，包括对WebSockets的支持
- 事件总线
- 共享数据 - —本地映射和集群分布式映射
- 周期性和延迟动作
- 部署和取消部署Verticles
- 数据报套接字
- DNS客户端
- 文件系统访问
- 高可用性
- 本地传输
- 集群

核心功能相当低级-在这里找不到数据库访问，授权或高级Web功能之类的东西-在**Vert.x ext**（扩展）中可以找到。

Vert.x核心小巧轻便。 您只需要使用所需的零件。 它也可以完全嵌入到您现有的应用程序中-我们不强迫您以特殊的方式构建应用程序，以便您可以使用Vert.x。

您可以使用Vert.x支持的任何其他语言的core。 但这很酷-我们不会强迫您直接从JavaScript或Ruby中使用Java API-毕竟，不同的语言具有不同的约定和惯用语，而在Ruby上强制使用Java惯用语是很奇怪的 开发人员（例如）。 相反，我们会为每种语言自动生成等效于核心Java API的“惯用语言”。

从现在开始，我们将仅使用**core**一词来指代Vert.x核心。

如果使用的是Maven或Gradle，请将以下依赖项添加到项目描述符的*dependencies*部分，以访问Vert.x Core API：

- Maven (在您的`pom.xml`中):

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-core</artifactId>
 <version>3.8.3</version>
</dependency>
```

- Gradle (在您的`build.gradle`文件中):

```java
dependencies {
 compile 'io.vertx:vertx-core:3.8.3'
}
```

让我们讨论core中的不同概念和功能。

## 开始创建Vert.x {#In_the_beginning_there_was_Vert_x}
除非您可以与`Vertx`对象进行通讯，否则您在Vert.x-land上不能做很多事情！

它是Vert.x的控制中心，是您执行几乎所有操作的方法，包括创建客户端和服务器，获取对事件总线的引用，设置计时器以及许多其他功能。

那么如何获得实例？

如果要嵌入Vert.x，则只需按以下步骤创建实例：

```java
Vertx vertx = Vertx.vertx();
```

------
> **注意:**  大多数应用程序只需要一个Vert.x实例，但是如果您需要隔离事件总线或不同组的服务器和客户端，则可以创建多个Vert.x实例。
------

### 创建Vertx对象时指定选项 {#Specifying_options_when_creating_a_Vertx_object}
创建Vert.x对象时，如果默认值不适合您，您还可以指定选项：

```java
Vertx vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(40));
```

`VertxOptions`对象有许多设置，允许您配置集群、高可用性、池大小和各种其他设置。

### 创建集群的Vert.x对象 {#Creating_a_clustered_Vert_x_object}
如果您要创建**集群Vert.x**（请参阅[事件总线]](https://vertx.io/docs/vertx-core/java/#event_bus)上的部分，以获得关于集群事件总线的更多信息），然后您将通常使用异步变量来创建Vertx对象。

这是因为群集中的不同Vert.x实例通常需要花费一些时间（可能是几秒钟）来组合在一起。 在此期间，我们不想阻塞调用线程，因此我们将结果异步地提供给您。

## 你是链式的吗? {#Are_you_fluent_}
您可能已经注意到，在前面的示例中使用了**fluent** API。

在`fluent` API中，可以将多个方法调用链接在一起。例如:

```java
request.response().putHeader("Content-Type", "text/plain").write("some text").end();
```

这是整个Vert.x API的通用模式，因此请习惯使用它。

像这样的链接调用允许您编写稍微不那么冗长的代码。当然，如果您不喜欢fluent方法**我们不会强迫您**这样做，如果您愿意，您可以愉快地忽略它，并像这样编写您的代码:

```java
HttpServerResponse response = request.response();
response.putHeader("Content-Type", "text/plain");
response.write("some text");
response.end();
```

## 别打给我们，我们会打给你的。 {#Don_t_call_us__we_ll_call_you_}
Vert.x API在很大程度上是*事件驱动*的。 这意味着当您感兴趣的Vert.x中发生任何事情时，Vert.x会通过向您发送事件来呼叫您。

一些示例事件是：

- 计时器已触发
- 一些数据已经到达套接字
- 从磁盘读取了一些数据
- 发生异常
- HTTP服务器已收到请求

您可以通过向Vert.x API提供*handlers*来处理事件。 例如，要每秒接收一个计时器事件，您将执行以下操作：

```java
vertx.setPeriodic(1000, id -> {
  // This handler will get called every second
  System.out.println("timer fired!");
});
```

或接收HTTP请求：

```java
server.requestHandler(request -> {
  // This handler will be called every time an HTTP request is received at the server
  request.response().end("hello world!");
});
```

过了一段时间，当Vert.x有一个事件传递给您的处理程序时，Vert.x会**异步地**调用它。

这使我们想到了Vert.x中的一些重要概念：

## 不要阻塞我！ {#Don_t_block_me_}
除了极少数例外（即某些文件系统操作以“同步”结尾）外，Vert.x中的所有API均不会阻塞调用线程。

如果可以立即提供结果，则将立即返回结果，否则通常会在一段时间后提供处理程序以接收事件。

因为没有Vert.x API会阻塞线程，这意味着您可以使Vert.x仅使用少量线程来处理大量并发。

使用传统的阻止API，在以下情况下，调用线程可能会阻止：

- 从套接字读取数据
- 将数据写入磁盘
- 向收件人发送消息并等待回复。
- … 许多其他情况

在上述所有情况下，当您的线程正在等待结果时，它无能为力-它实际上是无用的。

这意味着，如果要使用阻塞API进行大量并发操作，则需要大量线程来防止应用程序停止运行。

线程在其所需的内存（例如用于堆栈）和上下文切换方面都有开销。

对于许多现代应用程序所需的并发级别，阻塞方法根本无法扩展。

## 反应器和多反应器 {#Reactor_and_Multi_Reactor}
我们之前提到过Vert.x API是事件驱动的 - Vert.x在事件可用时将事件传递给处理程序。

在大多数情况下，Vert.x使用称为**event loop(事件循环)**的线程调用处理程序。

由于Vert.x或您的应用程序中没有任何内容，因此事件循环可以轻松地在事件到达时依次将事件传递给不同的处理程序。

由于没有任何阻塞，因此事件循环可能会在短时间内交付大量事件。 例如，单个事件循环可以非常快速地处理数千个HTTP请求。

我们称此为[Reactor Pattern(反应器模式)](https://en.wikipedia.org/wiki/Reactor_pattern).

你可能已经听说过这一点 - 例如Node.js的实现这种模式。

在标准的反应器实现中，有一个**single event loop(单个事件循环)**线程，该线程在一个循环中运行，将所有事件到达时的所有事件传递给所有处理程序。

单线程的问题是它只能在一个单一的核心上运行，所以如果你想让你的单线程反应器应用程序(例如你的Node.js应用程序)在你的多核服务器上扩展，你必须启动和管理许多不同的进程。

Vert.x在这里的工作方式有所不同。 每个Vertx实例都维护多个事件循环，而不是单个事件循环。 默认情况下，我们根据计算机上可用内核的数量选择数量，但是可以覆盖该数量。

这意味着与Node.js不同，单个Vertx进程可以在整个服务器上扩展。

我们将此模式称为**Multi-Reactor Pattern(多反应器模式)**，以将其与单线程反应器模式区分开。

------
> **注意:** 尽管Vertx实例维护多个事件循环，但任何特定的处理程序都不会并发执行，并且在大多数情况下总是使用**完全相同的事件循环**调用。(除了 [worker verticles](https://vertx.io/docs/vertx-core/groovy/#worker_verticles))。
> 
------

## 黄金法则 - 不要阻塞事件循环 {#The_Golden_Rule___Don_t_Block_the_Event_Loop}
我们已经知道Vert.x api是非阻塞的，不会阻塞事件循环，但是如果您在处理程序中阻塞事件循环**您自己**，那就没有多大帮助。

如果这样做，则在阻塞期间该事件循环将无法执行任何其他操作。如果您阻塞了Vertx实例中的所有事件循环，那么您的应用程序将完全停止!

所以不要这样做! **我已经警告过你了**。

阻塞的例子包括:

- Thread.sleep()
- 等待锁
- 等待互斥或监视器(例如: synchronized 段)
- 进行长时间的数据库操作并等待结果
- 进行复杂的计算需要花费大量时间。
- 循环运行

如果以上任何一种情况使事件循环在**相当长的时间内**停止执行任何其他操作，那么您应该立即转到naughty步骤，并等待进一步的指示。

那么，什么是**相当长的时间内**?

一根绳子有多长?这实际上取决于您的应用程序和所需的并发数量。

如果您有一个单独的事件循环，并且希望每秒处理10000个http请求，那么很明显，每个请求的处理时间不能超过0.1毫秒，因此您不能阻塞超过0.1毫秒的时间。

**这道数学题不难，留给读者作为练习。**

如果您的应用程序没有响应，这可能是您正在阻塞某个事件循环的信号。为了帮助您诊断这些问题，如果Vert.x检测到某个事件循环有一段时间没有返回，它将自动记录警告。如果您在日志中看到类似的警告，那么您应该进行调查。

```
Thread vertx-eventloop-thread-3 has been blocked for 20458 ms
```

Vert.x还将提供堆栈跟踪，以精确定位阻塞发生的位置。

如果您想关闭这些警告或更改设置，您可以在创建Vertx对象之前在`VertxOptions`对象中这样做。

## 运行阻塞的代码 {#Running_blocking_code}
在一个理想的世界中，不会有战争或饥饿，所有API都是异步编写的，小兔子会与小羊羔在阳光明媚的绿色草地上携手并进。

**但是……现实世界并非如此。 （您最近看过新闻吗？）**

事实是，很多（如果不是大多数的话）库，尤其是在JVM生态系统中，大多数库具有同步API，并且许多方法可能会阻塞。 JDBC API是一个很好的例子-它固有地是同步的，无论尝试多努力，Vert.x都无法在其上撒上魔术般的灰尘以使其异步。

我们不会在一夜之间将所有内容重写为异步的，因此我们需要为您提供一种在Vert.x应用程序中安全使用“传统”阻塞API的方法。

如前所述，您不能直接从事件循环中调用阻塞操作，因为这会阻止它执行任何其他有用的工作。怎么做呢?

这是通过调用`executeBlocking`来完成的，该代码同时指定了要执行的阻塞代码和在阻塞代码执行后异步调用的结果处理程序。

```java
vertx.executeBlocking(promise -> {
  // Call some blocking API that takes a significant amount of time to return
  String result = someAPI.blockingMethod("hello");
  promise.complete(result);
}, res -> {
  System.out.println("The result is: " + res.result());
});
```

------
> **警告:** 阻塞代码应阻塞一段合理的时间（例如: 不超过几秒钟）。 排除了长阻塞操作或轮询操作（在循环中以阻塞方式轮询事件的线程）。 当阻塞操作持续10秒钟以上时，阻塞线程检查器将在控制台上显示一条消息。 长阻塞操作应使用由应用程序管理的专用线程，该线程可以使用事件总线或`runOnContext`与verticles交互。
> 
------

默认情况下，如果从同一上下文（例如，相同的Verticle实例）中多次调用了executeBlocking，则不同的executeBlocking将*串行*执行（即一个接一个）。

如果您不关心排序，您可以调用`executeBlocking`，将`false`指定为`ordered`的参数。在这种情况下，任何executeBlocking都可以在工作池上并行执行。

运行阻塞代码的另一种方法是使用[worker verticle](https://vertx.io/docs/vertx-core/groovy/#worker_verticles)

一个`worker verticle`总是用来自工作池的线程执行。

默认情况下，阻塞代码是在Vert.x工作池上执行的，该工作池使用setWorkerPoolSize配置。

可以出于其他目的创建其他池：

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

不需要`worker executor`时，必须将其关闭：

```java
executor.close();
```

当使用相同的名称创建多个工作线程时，他们将共享相同的池。 关闭所有使用该工作池的工作执行程序时，该工作池将被销毁。

在Verticle中创建executor时，当取消部署Verticle时，Vert.x会自动为您关闭它。

创建时可以配置Worker executors：

```java
int poolSize = 10;

// 2 minutes
long maxExecuteTime = 2;
TimeUnit maxExecuteTimeUnit = TimeUnit.MINUTES;

WorkerExecutor executor = vertx.createSharedWorkerExecutor("my-worker-pool", poolSize, maxExecuteTime, maxExecuteTimeUnit);
```

------
> **注意:**  在创建工作池时设置配置
> 
------

## 异步协调 {#Async_coordination}
多个异步结果的协调可以通过Vert.x的`futures`来实现。 它支持并发组合（并行运行多个异步操作）和顺序组合（链异步操作）。

### 并发组合 {#Concurrent_composition}
`CompositeFuture.all`接受几个futures参数(最多6个)，返回一个在所有future都*succeeded*时“成功”的future，在至少一个future*failed*时“失败”的future:

```java
Future<HttpServer> httpServerFuture = Future.future(promise -> httpServer.listen(promise));

Future<NetServer> netServerFuture = Future.future(promise -> netServer.listen(promise));

CompositeFuture.all(httpServerFuture, netServerFuture).setHandler(ar -> {
  if (ar.succeeded()) {
    // All servers started
  } else {
    // At least one server failed
  }
});
```

操作并发运行，在合成完成时调用附加到返回的future的`Handler`。当其中一个操作失败时(已传递的future中的一个被标记为失败)，结果的future 也被标记为失败。当所有操作都成功时，产生的future就成功地完成了。

或者，您可以传递一个futures列表(可能是空的):

```java
CompositeFuture.all(Arrays.asList(future1, future2, future3));
```

当`all`组合*等待*直到所有futures成功(或一个失败)时，`any`组合*等待*第一个成功的futures。 `CompositeFuture.any`接受几个future参数（最多6个），并返回一个future，当其中一个future成为成功时，则成功，而当所有Future都失败时，则失败:

```java
CompositeFuture.any(future1, future2).setHandler(ar -> {
  if (ar.succeeded()) {
    // At least one is succeeded
  } else {
    // All failed
  }
});
```

也可以使用future列表：

```java
CompositeFuture.any(Arrays.asList(f1, f2, f3));
```

`join`组合*等待*直到所有future完成，要么成功要么失败。`CompositeFuture.join`接受几个future参数(最多6个)，并返回一个future，当所有future都成功时，该future就成功；而当所有future都完成且其中至少一个失败时，则失败：

```java
CompositeFuture.join(future1, future2, future3).setHandler(ar -> {
  if (ar.succeeded()) {
    // All succeeded
  } else {
    // All completed and at least one failed
  }
});
```

也可以使用future列表：

```java
CompositeFuture.join(Arrays.asList(future1, future2, future3));
```

### 顺序组合 {#Sequential_composition}
当`all`和`any`实现并发组合时，`compose`可用于链接futures(即顺序组合)。 

```java
FileSystem fs = vertx.fileSystem();

Future<Void> fut1 = Future.future(promise -> fs.createFile("/foo", promise));

Future<Void> startFuture = fut1
  .compose(v -> {
  // When the file is created (fut1), execute this:
  return Future.<Void>future(promise -> fs.writeFile("/foo", Buffer.buffer(), promise));
})
  .compose(v -> {
  // When the file is written (fut2), execute this:
  return Future.future(promise -> fs.move("/foo", "/bar", promise));
});
```

在这个例子中，3个操作被链接:

1. 创建一个文件(' fut1 ')
2. 文件中写入了一些内容(' fut2 ')
3. 文件被移动(' startFuture ')

当这三个步骤成功时，最后一个future(“startFuture”)就成功了。但是，如果其中一个步骤失败，则最终的将来也失败了。

这个示例使用:

- `compose`: 当前的future完成时，运行给定的函数，该函数将返回一个future。 当返回的future完成时，它完成了composition。
- `compose`: 当当前的future完成时，运行给定的处理程序以完成给定的`future`（下一个）。

在第二种情况下，`Handler`应完成`next`future 以报告其成功或失败。

## Verticles {Vertx的模块}

Vert.x提供了一个简单，可扩展的，类似于*actor-like*的部署和并发模型，您可以使用它来节省编写自己的代码的时间。

**此模型是完全可选的，如果您不愿意，Vert.x不会强迫您以这种方式创建应用程序。**.

该模型并不声称是严格的actor-model模型实现，但确实具有相似之处，尤其是在并发，扩展和部署方面。

要使用此模型，您需要将代码编写为一组**verticles**。

Verticles 由Vert.x部署和运行的代码块。 一个Vert.x实例默认维护N个事件循环线程（其中N默认为core * 2）。 可以使用Vert.x支持的任何语言来编写Verticles，并且单个应用程序可以包括以多种语言编写的Verticles 。

您可以在[Actor Model](https://en.wikipedia.org/wiki/Actor_model)中将某个verticle想像为一个演员。

应用程序通常由同时在同一Vert.x实例中运行的许多verticle实例组成。 不同的verticle实例通过在[事件总线](https://vertx.io/docs/vertx-core/groovy/#event_bus)上发送消息来相互通信。

### 编写Verticles {#Writing_Verticles}
Verticle 类必须实现`Verticle`接口。

如果愿意，他们可以直接实现它，但通常扩展抽象类`AbstractVerticle`会更简单。

这是一个verticle的例子:

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

通常，您将像上面的示例一样覆盖`start`方法。

当Vert.x部署该verticle时，它将调用`start`方法，当该方法完成后，该verticle将被视为已启动。

您也可以选择覆盖stop方法。 Vert.x将在取消部署verticle时调用该方法，并且在该方法完成后，该verticle将被视为已停止。

### 异步Verticle启动和停止 {#Asynchronous_Verticle_start_and_stop}
有时，您需要在启动Verticle时做一些事情，而这需要一些时间，并且您不希望在这种情况发生之前就考虑将Verticle部署。 例如，您可能想使用start方法启动HTTP服务器，并传播服务器`listen`方法的异步结果。

您不能阻止在启动方法中绑定HTTP服务器，那样会破坏[黄金规则](https://vertx.io/docs/vertx-core/java/#golden_rule)。

那你怎么做呢？

做到这一点的方法是实现**异步**的start方法。 此版本的方法以Future为参数。当该方法返回时，将**不会**认为verticle已部署。

一段时间之后，在您完成了所有需要做的事情(例如启动HTTP服务器)之后，您可以在Future上调用complete(或fail)来表示您已经完成了。

这里有一个例子:

```java
public class MyVerticle extends AbstractVerticle {

 private HttpServer server;

 public void start(Future<Void> startFuture) {
   server = vertx.createHttpServer().requestHandler(req -> {
     req.response()
       .putHeader("content-type", "text/plain")
       .end("Hello from Vert.x!");
     });

   // Now bind the server:
   server.listen(8080, res -> {
     if (res.succeeded()) {
       startFuture.complete();
     } else {
       startFuture.fail(res.cause());
     }
   });
 }
}
```

类似地，stop方法也有一个异步版本。如果你想做一些需要时间的verticle清理，你可以使用这个。

```java
public class MyVerticle extends AbstractVerticle {

 public void start() {
   // Do something
 }

 public void stop(Future<Void> stopFuture) {
   obj.doSomethingThatTakesTime(res -> {
     if (res.succeeded()) {
       stopFuture.complete();
     } else {
       stopFuture.fail();
     }
   });
 }
}
```

------
> 信息: 您无需通过verticle的stop方法手动停止由verticle启动的HTTP服务器。当verticle取消部署时Vert.x会自动停止所有正在运行的服务器。
> 
------

### Verticle类型 {#Verticle_Types}
共有三种不同类型的verticle：

- Standard Verticles(标准Verticles)

  这些是最常见和最有用的类型-它们始终使用事件循环线程执行。 我们将在下一部分中对此进行更多讨论。

- Worker Verticles(工作Verticles)

  这些使用工作池中的线程运行。 一个实例永远不会由多个线程并发执行。

- Multi-threaded worker verticles(多线程工作Verticles)

  这些使用工作池中的线程运行。 一个实例可以由多个线程并发执行。

### Standard verticles {#标准Verticles}
标准verticles在创建时会分配一个事件循环线程，并使用该事件循环调用start方法。 当您从事件循环调用任何其他在核心API上使用处理程序的方法时，Vert.x将保证这些处理程序在被调用时将在同一事件循环上执行。

这意味着我们可以保证您的verticle实例中的所有代码始终在同一事件循环上执行（只要您不创建自己的线程并调用它！）。

这意味着您可以将应用程序中的所有代码编写为单线程，让Vert.x来处理线程和可伸缩性。 不再需要担心同步和易失性，并且还避免了其他许多竞争情况和死锁的情况，这些情况在进行手工“传统”多线程应用程序开发时非常普遍。

`worker verticle`与标准verticle一样，但是它使用Vert.x的`worker thread pool`线程池中的线程执行，而不是使用事件循环。

Worker verticles旨在用于调用阻塞代码，因为它们不会阻止任何事件循环。

如果您不想使用`worker verticle`来运行阻塞代码，则还可以在事件循环上直接运行[inline blocking code](https://vertx.io/docs/vertx-core/groovy/#blocking_code) 。

如果要将一个verticle 部署为一个worker verticle，可以使用`setWorker`来完成。

```java
DeploymentOptions options = new DeploymentOptions().setWorker(true);
vertx.deployVerticle("com.mycompany.MyOrderProcessorVerticle", options);
```

Vert.x永远不会由多个线程同时执行worker verticle实例，但可以在不同时间由不同线程执行。

#### 多线程工作Verticles {#Multi_threaded_worker_verticles}
多线程worker verticle与普通worker verticle一样，但是可以由不同的线程同时执行。

------
> **慎重:**  多线程worker verticles是一项高级功能，大多数应用程序将不需要它们。
> 
------

由于这些verticles的并发性，您必须非常小心，使用标准Java技术进行多线程编程，使verticles保持一致状态。

设计了多线程worker verticles，仅用于以阻塞方式同时使用`EventBus`消息。

------
> **警告:**  无法在多线程worker verticle中创建Vert.x客户端和服务器（TCP，HTTP等）。 如果您不小心尝试，将引发异常。
> 
------

从本质上讲，多线程worker verticles仅避免用户部署worker verticle实例的数量与worker pool中线程的数量一样多。 因此，例如，您可以在` DeploymentOptions`中提供worker pool名称/大小并相应地设置实例数：

```java
DeploymentOptions options = new DeploymentOptions()
  .setWorker(true)
  .setInstances(5) // matches the worker pool size below
  .setWorkerPoolName("the-specific-pool")
  .setWorkerPoolSize(5);
vertx.deployVerticle("com.mycompany.MyOrderProcessorVerticle", options);
```

另外，您可以创建一个常规的verticle并用多个`executeBlocking`将您的阻塞代码包装，并将`ordered`标志设置为`false`：

```java
vertx.eventBus().consumer("foo", msg -> {
  vertx.executeBlocking(promise -> {
    // Invoke blocking code with received message data
    promise.complete(someresult);
  }, false, ar -> { // ordered == false
    // Handle result, e.g. reply to the message
  });
});
```

### 以编程方式部署verticles {#Deploying_verticles_programmatically}
您可以使用`deployVerticle`方法之一来部署一个verticle，指定一个Verticle名称，也可以传入已经创建的Verticle实例。

------
> **注意:**  部署Verticle实例仅限Java。
> 
------

```java
Verticle myVerticle = new MyVerticle();
vertx.deployVerticle(myVerticle);
```

您还可以通过指定verticle **name**来部署顶点。

verticle名称用于查找特定的`VerticleFactory`，该实例将用于实例化实际的verticle实例。

不同的Version工厂可用于以不同的语言实例化Verticle，并且出于各种其他原因，例如加载服务以及在运行时从Maven获取Verticle。

这使您可以部署Vert.x支持的任何其他语言编写的Verticles。

这是部署一些不同类型的verticles的示例：

```java
vertx.deployVerticle("com.mycompany.MyOrderProcessorVerticle");

// Deploy a JavaScript verticle
vertx.deployVerticle("verticles/myverticle.js");

// Deploy a Ruby verticle verticle
vertx.deployVerticle("verticles/my_verticle.rb");
```

### 将verticle名称映射到verticle工厂的规则 {#Rules_for_mapping_a_verticle_name_to_a_verticle_factory}
当使用名称部署verticle时，该名称用于选择将实例化该verticle的实际verticle工厂。

verticle名称可以有一个前缀-这是一个字符串，后跟一个冒号，如果存在的话将用于查找工厂，例如
```java
js:foo.js // Use the JavaScript verticle factory 

groovy:com.mycompany.SomeGroovyCompiledVerticle // Use the Groovy 

verticle factory service:com.mycompany:myorderservice // Uses the service verticle factory
```
如果没有前缀，Vert.x将查找后缀并使用后缀查找工厂，例如

`foo.js`将使用JavaScript verticle工厂`SomeScript.groovy`将使用Groovy verticle工厂

如果没有前缀或后缀，则Vert.x将假定它是Java完全限定的类名（FQCN），然后尝试实例化该名称。

### Verticle工厂位于哪里? {#How_are_Verticle_Factories_located_}
大多数Verticle工厂都从类路径加载并在Vert.x启动时注册。

如果愿意，您还可以使用`registerVerticleFactory`和`unregisterVerticleFactory`以编程方式注册和注销verticle工厂。

### 等待部署完成 {#Waiting_for_deployment_to_complete}
Verticle部署是异步的，可能会在部署调用返回后的一段时间内完成。

如果你想在部署完成时得到通知，你可以部署指定一个完成处理程序:

```java
vertx.deployVerticle("com.mycompany.MyOrderProcessorVerticle", res -> {
  if (res.succeeded()) {
    System.out.println("Deployment id is: " + res.result());
  } else {
    System.out.println("Deployment failed!");
  }
});
```

如果部署成功，将向完成处理程序传递包含部署ID字符串的结果。

如果希望取消部署，可以稍后使用此部署ID。

### 取消verticle部署 {#Undeploying_verticle_deployments}
可以使用`undeploy`取消部署。

取消部署本身是异步的，因此，如果要在完成取消部署时收到通知，可以部署指定完成处理程序：

```java
vertx.undeploy(deploymentID, res -> {
  if (res.succeeded()) {
    System.out.println("Undeployed ok");
  } else {
    System.out.println("Undeploy failed!");
  }
});
```

### 指定verticle实例数 {#Specifying_number_of_verticle_instances}
使用verticle名称部署verticle时，可以指定要部署的verticle实例的数量：

```java
DeploymentOptions options = new DeploymentOptions().setInstances(16);
vertx.deployVerticle("com.mycompany.MyOrderProcessorVerticle", options);
```

这对于轻松跨多个内核进行扩展很有用。 例如，您可能有一个要部署的Web服务器版本，并且在您的计算机上有多个核心，因此您想部署多个实例以利用所有核心。

### 将配置传递到verticle {#Passing_configuration_to_a_verticle}
可以在部署时将JSON形式的配置传递给verticle：

```java
JsonObject config = new JsonObject().put("name", "tim").put("directory", "/blah");
DeploymentOptions options = new DeploymentOptions().setConfig(config);
vertx.deployVerticle("com.mycompany.MyOrderProcessorVerticle", options);
```

然后可以通过`Context`对象或直接使用`config`方法来使用此配置。 该配置作为JSON对象返回，因此您可以按以下方式检索数据：

```java
System.out.println("Configuration: " + config().getString("name"));
```

### 在Verticle中访问环境变量 {#Accessing_environment_variables_in_a_Verticle}
使用Java API可访问环境变量和系统属性：

```java
System.getProperty("prop");
System.getenv("HOME");
```

### Verticle隔离组 {#Verticle_Isolation_Groups}
默认情况下，Vert.x具有*flat classpath*。 也就是说，当Vert.x部署verticle时，它会使用当前的类加载器进行部署-不会创建新的类加载器。 在大多数情况下，这是最简单，最清晰和最明智的做法。

但是，在某些情况下，您可能希望部署一个Verticle，以便该Verticle的类与您的应用程序中的其他类隔离。

例如，如果要在同一Vert.x实例中部署具有相同类名的两个不同版本的verticle，或者您有两个使用相同jar库的不同版本的不同verticle，则可能是这种情况。

使用隔离组时，您可以使用`setIsolatedClasses`提供要隔离的类名称的列表-条目可以是完全限定的类名称，例如`com.mycompany.myproject.engine.MyClass`，也可以是通配符， 将匹配包和任何子包中的任何类，例如 `com.mycompany.myproject.*`将匹配`com.mycompany.myproject`包中的任何类或任何子包。

请注意，*仅* 匹配的类将被隔离-其他任何类将由当前类加载器加载。

`setExtraClasspath`也可以提供额外的类路径条目，因此，如果您要加载主类路径上尚不存在的类或资源，则可以添加它。

------
> **警告:**  谨慎使用此功能。类装入器可能是一堆蠕虫，并且会使调试变得困难。
> 
------

这是一个使用隔离组隔离verticle部署的示例。

```java
DeploymentOptions options = new DeploymentOptions().setIsolationGroup("mygroup");
options.setIsolatedClasses(Arrays.asList("com.mycompany.myverticle.*",
                   "com.mycompany.somepkg.SomeClass", "org.somelibrary.*"));
vertx.deployVerticle("com.mycompany.myverticle.VerticleClass", options);
```

### High Availability {#High_Availability}
可以在启用高可用性（HA）的情况下部署Verticles。 在这种情况下，当将一个verticle部署在突然死亡的vert.x实例上时，该verticle 将重新部署到集群中的另一个vert.x实例上。

要运行启用了高可用性的Verticle，只需附加`-ha`开关即可：

```java
vertx run my-verticle.js -ha
```

启用高可用性时，无需添加`-cluster`。

[高可用性和故障转移](https://vertx.io/docs/vertx-core/groovy/#_high_availability_and_fail_over)部分中有关高可用性功能和配置的更多详细信息。

### 从命令行运行Verticles {#Running_Verticles_from_the_command_line}
您可以在Maven或Gradle项目中直接使用Vert.x，方法是向Vert.x核心库添加一个依赖项，然后从那里开始。

但是，如果愿意，您也可以直接从命令行运行Vert.x的verticles。

为此，您需要下载并安装Vert.x发行版，并将安装的bin目录添加到环境变量PATH中。 还要确保您在`PATH`上有一个Java 8 JDK。

------
> **注意:**  需要JDK来支持Java代码的即时编译。
> 
------

现在，您可以使用`vertx run`命令运行verticles。 这里有些例子：

```bash
# Run a JavaScript verticle
vertx run my_verticle.js

# Run a Ruby verticle
vertx run a_n_other_verticle.rb

# Run a Groovy script verticle, clustered
vertx run FooVerticle.groovy -cluster
```

您甚至可以运行Java源代码verticles，而无需先编译它们！

```bash
vertx run SomeJavaSourceFile.java
```

Vert.x将在运行之前即时编译Java源文件。 这对于快速制作verticles原型非常有用，并且对于演示非常有用。 无需先设置Maven或Gradle构建就可以开始！

有关在命令行上执行`vertx`时可用的各种选项的完整信息，请在命令行中键入`vertx`。

### 导致Vert.x退出 {#Causing_Vert_x_to_exit}
Vert.x实例维护的线程不是守护程序线程，因此它们将阻止JVM退出。

如果你正在嵌入Vert.x，并且你已经完成了它，你可以调用`close`来关闭它。

这将关闭所有内部线程池并关闭其他资源，并允许JVM退出。

### 上下文对象 {#The_Context_object}
当Vert.x向处理程序提供事件或调用`Verticle`的start或stop方法时，执行将与`Context` 关联。 通常，上下文是**事件循环上下文**，并绑定到特定的事件循环线程。 因此，针对该上下文的执行始终在完全相同的事件循环线程上进行。 对于工作程序verticles和运行内联阻塞代码的情况，工作程序上下文将与执行关联，该上下文将使用工作程序线程池中的线程。

要获取上下文，请使用`getOrCreateContext`方法：

```java
Context context = vertx.getOrCreateContext();
```

如果当前线程具有与之关联的上下文，则它将重用上下文对象。 如果不是，则创建新的上下文实例。 您可以测试检索到的上下文的*类型*：

```java
Context context = vertx.getOrCreateContext();
if (context.isEventLoopContext()) {
  System.out.println("Context attached to Event Loop");
} else if (context.isWorkerContext()) {
  System.out.println("Context attached to Worker Thread");
} else if (context.isMultiThreadedWorkerContext()) {
  System.out.println("Context attached to Worker Thread - multi threaded worker");
} else if (! Context.isOnVertxThread()) {
  System.out.println("Context not attached to a thread managed by vert.x");
}
```

检索上下文对象后，可以在此上下文中异步运行代码。 换句话说，您提交的任务最终将在相同的上下文中运行，但是稍后：

```java
vertx.getOrCreateContext().runOnContext( (v) -> {
  System.out.println("This will be executed asynchronously in the same context");
});
```

当多个处理程序在同一上下文中运行时，它们可能希望共享数据。上下文对象提供了在上下文中存储和检索共享数据的方法。例如，它可以让你传递数据到一些动作运行`runOnContext`:

```java
final Context context = vertx.getOrCreateContext();
context.put("data", "hello");
context.runOnContext((v) -> {
  String hello = context.get("data");
});
```

上下文对象还允许您使用`config`方法访问verticle配置。 检查[将配置传递到verticle位置](https://vertx.io/docs/vertx-core/groovy/#_passing_configuration_to_a_verticle)部分以获取有关此配置的更多详细信息。

### 执行定期和延迟的操作 {#Executing_periodic_and_delayed_actions}
在Vert.x中，很常见的是要延迟或定期执行操作。

在标准verticle中，您不能只是使线程休眠以引入延迟，因为这会阻塞事件循环线程。

而是使用Vert.x计时器。 计时器可以是**一次性**或**定期**。 我们将讨论两者

#### 单次计时器 {#One_shot_Timers}
一次性计时器在一定的延迟(以毫秒为单位)之后调用事件处理程序。

使用`setTimer`方法传递延迟和处理程序后，设置要触发的计时器

```java
long timerID = vertx.setTimer(1000, id -> {
  System.out.println("And one second later this is printed");
});

System.out.println("First this is printed");
```

返回值是唯一的计时器ID，以后可用于取消计时器。 处理程序还传递了计时器ID。

#### 周期性的计时器 {#Periodic_Timers}
您还可以使用`setPeriodic`将计时器设置为定期触发。

将会有一个与周期相等的初始延迟。

`setPeriodic`的返回值是唯一的计时器ID（长整数）。 如果需要取消计时器，可以稍后使用。

传递到计时器事件处理程序中的参数也是唯一的计时器ID：

请记住，计时器将定期触发。如果您的定期任务需要很长时间才能完成，那么计时器事件可能会连续运行，甚至更糟:堆积起来。

在这种情况下，您应该考虑改用`setTimer`。 任务完成后，您可以设置下一个计时器。

```java
long timerID = vertx.setPeriodic(1000, id -> {
  System.out.println("And every second this is printed");
});

System.out.println("First this is printed");
```

#### 取消计时器 {#Cancelling_timers}
要取消定期计时器，请调用`cancelTimer`并指定计时器ID。 例如：

```java
vertx.cancelTimer(timerID);
```

#### verticles中的自动清理 {#Automatic_clean_up_in_verticles}
如果您是从verticle内部创建计时器，则取消部署verticles时，这些计时器将自动关闭。

### Verticle工作池 {#Verticle_worker_pool}
Verticles使用Vert.x工作池执行阻塞操作，即`executeBlocking`或工作verticle。

可以在部署选项中指定其他工作池：

```java
vertx.deployVerticle("the-verticle", new DeploymentOptions().setWorkerPoolName("the-specific-pool"));
```

## 事件总线 {#The_Event_Bus}
`event bus(事件总线)`是Vert.x的**nervous system(经系统)**。

每个Vert.x实例都有一个事件总线实例，可以使用`eventBus`方法获得它。

事件总线允许您的应用程序的不同部分相互通信，而不管它们是用什么语言编写的，以及它们是在相同的Vert.x实例中还是在不同的Vert.x实例中。

它甚至可以桥接，以允许在浏览器中运行的客户端JavaScript在同一事件总线上进行通信。

事件总线构成了跨越多个服务器节点和多个浏览器的分布式对等消息传递系统。

事件总线支持发布/订阅，点对点和请求-响应消息传递。

事件总线API非常简单。 它基本上涉及注册处理程序，注销处理程序以及发送和发布消息。

首先是一些理论：

### 理论 {#The_Theory}
#### 地址 {#Addressing}
消息在事件总线上发送到**address(地址)**address**。

Vert.x不需要任何花哨的寻址方案。 在Vert.x中，地址只是一个字符串。 任何字符串均有效。 但是，明智的做法是使用某种方案，例如使用句点来分隔名称空间。

有效地址的一些示例是`europe.news.feed1`，`acme.games.pacman`，`sausages`和`X`。

#### 处理程序 {#Handlers}
消息由处理程序接收。 您在地址注册处理程序。

可以在同一地址注册许多不同的处理程序。

单个处理程序可以在许多不同的地址上注册。

#### 发布/订阅消息 {#Publish___subscribe_messaging}
事件总线支持**发布**消息。

消息被发布到一个地址。 发布意味着将消息传递给在该地址注册的所有处理程序。

这是熟悉的**发布/订阅**消息传递模式。

#### 点对点和请求响应消息传递 {#Point_to_point_and_Request_Response_messaging}
事件总线还支持**point-to-point(点对点)**消息传递。

消息被发送到一个地址。 然后，Vert.x会将它们路由到在该地址注册的处理程序之一。

如果在该地址注册了多个处理程序，那么将使用非严格的循环算法选择一个。

使用点对点消息传递，可以在发送消息时指定可选的应答处理程序。

当消息被接收方接收并处理后，接收方可以选择回复消息。如果这样做，将调用应答处理程序。

当发件人收到回复时，也可以回复它。 可以无限次重复此操作，并允许在两个不同的verticles之间建立对话框。

这是一种常见的消息传递模式，称为**请求-响应**模式。

#### 尽力递送 {#Best_effort_delivery}
Vert.x会尽力传递消息，并且不会有意识地将其丢弃。 这称为**best-effort(尽力而为)**交付。

但是，如果事件总线的全部或部分发生故障，则可能会丢失消息。

如果您的应用程序关心丢失的消息，则应将处理程序编码为幂等，而发送方应在恢复后重试。

#### 消息类型 {#Types_of_messages}
开箱即用的Vert.x允许将任何原始/简单类型，字符串或`buffers(缓冲区)`作为消息发送。

但是，在Vert.x中以[JSON](https://json.org/)发送消息是一种惯例

JSON非常容易以Vert.x支持的所有语言创建，读取和解析，因此它已成为Vert.x的一种*通用语言*。

但是，如果您不想这么做，则不必强制使用JSON。

事件总线非常灵活，并且还支持通过事件总线发送任意对象。 您可以通过为要发送的对象定义一个“编解码器”来实现。

### 事件总线API {#The_Event_Bus_API}
让我们进入API。

#### 获取事件总线 {#Getting_the_event_bus}
您可以获得对事件总线的引用，如下所示：

```java
EventBus eb = vertx.eventBus();
```

每个Vert.x实例只有一个事件总线实例。

#### 注册处理程序 {#Registering_Handlers}
注册处理程序的最简单方法是使用`consumer`。 这是一个例子：

```java
EventBus eb = vertx.eventBus();

eb.consumer("news.uk.sport", message -> {
  System.out.println("I have received a message: " + message.body());
});
```

当消息到达您的处理程序时，将调用您的处理程序，并传递`message`。

从调用返回给Consumer()的对象是`MessageConsumer`的实例。

此对象随后可用于注销处理程序，或将处理程序用作流。

或者，您可以使用`consumer`来返回一个没有设置任何处理程序的MessageConsumer，然后在此基础上设置处理程序。例如：

```java
EventBus eb = vertx.eventBus();

MessageConsumer<String> consumer = eb.consumer("news.uk.sport");
consumer.handler(message -> {
  System.out.println("I have received a message: " + message.body());
});
```

在集群事件总线上注册处理程序时，注册到集群的所有节点可能需要一些时间。

如果您希望在完成时得到通知，您可以在MessageConsumer对象上注册一个`completion handler(完成处理程序)`。

```java
consumer.completionHandler(res -> {
  if (res.succeeded()) {
    System.out.println("The handler registration has reached all nodes");
  } else {
    System.out.println("Registration failed!");
  }
});
```

#### 取消注册处理程序 {#Un_registering_Handlers}
要取消注册处理程序，请调用`unregister`。

如果您在集群事件总线上，则注销可能需要一些时间才能在节点上传播。 如果您想在完成时收到通知，请使用`unregister`。

```java
consumer.unregister(res -> {
  if (res.succeeded()) {
    System.out.println("The handler un-registration has reached all nodes");
  } else {
    System.out.println("Un-registration failed!");
  }
});
```

#### 发布消息 {#Publishing_messages}
发布消息很简单。 只需使用`publish`指定发布地址即可。

```java
eventBus.publish("news.uk.sport", "Yay! Someone kicked a ball");
```

然后，该消息将传递给在地址`news.uk.sport`注册的所有处理程序。

#### 发送消息 {#Sending_messages}
发送消息将导致仅在接收消息的地址注册一个处理程序。这就是点对点消息传递模式。处理程序以非严格的循环方式选择。

您可以通过`send`发送信息。

```java
eventBus.send("news.uk.sport", "Yay! Someone kicked a ball");
```

#### 在消息上设置标题 {#Setting_headers_on_messages}
通过事件总线发送的消息也可以包含*header*。 可以通过在发送或发布时设置选项来指定：

```java
DeliveryOptions options = new DeliveryOptions();
options.addHeader("some-header", "some-value");
eventBus.send("news.uk.sport", "Yay! Someone kicked a ball", options);
```

#### 消息顺序 {#Message_ordering}
Vert.x将按照从任何特定发件人发送的顺序将消息传递到任何特定处理程序。

#### 消息对象 {#The_Message_object}
您在消息处理程序中收到的对象是`Message`。

消息的`body`对应于已发送或发布的对象。

消息的头可与`headers`一起使用。

#### 确认消息/发送回复 {#Acknowledging_messages___sending_replies}
当使用`send`时，事件总线尝试将消息传递到在事件总线上注册的`MessageConsumer`。

在某些情况下，对于发件人来说，了解消费者何时收到邮件并使用**request-response**模式对其进行"处理"很有用。

为了确认消息已被处理，消费者可以通过调用`reply`来答复消息。

发生这种情况时，它会导致将答复发送回发送方，并且将使用该答复来调用答复处理程序。

一个例子可以清楚地说明这一点：

接受方：

```java
MessageConsumer<String> consumer = eventBus.consumer("news.uk.sport");
consumer.handler(message -> {
  System.out.println("I have received a message: " + message.body());
  message.reply("how interesting!");
});
```

发送方：

```java
eventBus.request("news.uk.sport", "Yay! Someone kicked a ball across a patch of grass", ar -> {
  if (ar.succeeded()) {
    System.out.println("Received reply: " + ar.result().body());
  }
});
```

回复可以包含一个消息正文，其中可以包含有用的信息。

“处理”的实际含义是由应用程序定义的，并且完全取决于消息使用者的操作，而不是Vert.x事件总线本身知道或关心的事情。

一些例子：

- 一个简单的消息使用者，实现了返回一天中时间的服务，便会在回复正文中使用包含一天中时间的消息进行确认
- 实现持久队列的消息使用者，如果消息已成功持久存储在消息中，则可能会以`true`进行确认，否则将以`false`进行确认。
- 成功处理完订单后，处理订单的消息使用者可能会以`true`确认，因此可以将其从数据库中删除

#### 发送与超时 {#Sending_with_timeouts}
当发送带有回复处理程序的消息时，您可以在`DeliveryOptions`中指定超时。

如果在此时间内未收到答复，则将以失败的方式调用答复处理程序。

默认超时为30秒。

#### 发送失败 {#Send_Failures}
消息发送可能由于其他原因而失败，包括：

- 没有可用于将消息发送到的处理程序
- 接收者已使用`fail`明确使消息失败

在所有情况下，将使用特定的故障调用应答处理程序。

#### 消息的编解码器 {#Message_Codecs}
如果定义并注册了`消息编解码器`，则可以在事件总线上发送任何您喜欢的对象。

消息编解码器具有名称，您可以在发送或发布消息时在`DeliveryOptions`中指定该名称：

```java
eventBus.registerCodec(myCodec);

DeliveryOptions options = new DeliveryOptions().setCodecName(myCodec.name());

eventBus.send("orders", new MyPOJO(), options);
```

如果您始终希望将相同的编解码器用于特定类型，则可以为其注册默认编解码器，则不必在传递选项中为每次发送指定编解码器：

```java
eventBus.registerDefaultCodec(MyPOJO.class, myCodec);

eventBus.send("orders", new MyPOJO());
```

您可以使用`unregisterCodec`取消注册消息编解码器。

消息编解码器不必总是编码和解码为相同的类型。 例如，您可以编写允许发送MyPOJO类的编解码器，但是当该消息发送到处理程序时，它将作为MyOtherPOJO类到达。

#### 集群事件总线 {#Clustered_Event_Bus}
事件总线不仅存在于单个Vert.x实例中。 通过在网络上将不同的Vert.x实例群集在一起，它们可以形成单一的分布式事件总线。

#### 以编程方式建立集群 {#Clustering_programmatically}
如果您以编程方式创建Vert.x实例，则可以通过将Vert.x实例配置为集群来获得集群事件总线；

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

您还应该确保在类路径上具有`ClusterManager`实现，例如Hazelcast集群管理器。

#### 在命令行上进行集群 {#Clustering_on_the_command_line}
您可以使用以下命令行运行Vert.x集群

```java
vertx run my-verticle.js -cluster
```

### Automatic clean-up in verticles {verticles自动清理}

如果您是从Verticle内部注册事件总线处理程序，则在取消部署Verticle时，这些处理程序将自动注销。

## 配置事件总线 {#Configuring_the_event_bus}
可以配置事件总线。当事件总线集群化时，它特别有用。在底层，事件总线使用TCP连接发送和接收消息，因此`EventBusOptions`允许您配置这些TCP连接的所有方面。由于事件总线充当服务器和客户机，所以配置接近`NetClientOptions`和`NetServerOptions。

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

上一片段描述了如何将SSL连接用于事件总线，而不是普通的TCP连接。

------
> **警告:** 要在集群模式下实施安全性，您**必须**配置集群管理器以使用加密或实施安全性。有关详细信息，请参阅集群管理器的文档。
------

事件总线配置需要在所有集群节点中保持一致。

`EventBusOptions`还允许您指定事件总线是否集群、端口和主机。

When used in containers, you can also configure the public host and port:

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

## JSON {#JSON}
与其他一些语言不同，Java不支持[JSON](https://json.org/)，因此我们提供了两个类来简化Vert.x应用程序中的JSON处理。

### JSON对象 {#JSON_objects}
`JsonObject`类表示JSON对象。

JSON对象基本上只是一个具有字符串键的映射，并且值可以是JSON支持的类型之一（字符串，数字，布尔值）。

JSON对象还支持空值。

#### 创建JSON对象 {#Creating_JSON_objects}
可以使用默认构造函数创建空的JSON对象。

您可以通过字符串JSON表示形式创建JSON对象，如下所示：

```java
String jsonString = "{\"foo\":\"bar\"}";
JsonObject object = new JsonObject(jsonString);
```

您可以从map创建JSON对象，如下所示：

```java
Map<String, Object> map = new HashMap<>();
map.put("foo", "bar");
map.put("xyz", 3);
JsonObject object = new JsonObject(map);
```

#### 将条目放入JSON对象 {#Putting_entries_into_a_JSON_object}
使用`put`方法将值放入JSON对象。

可以使用流畅的API链接方法调用：

```java
JsonObject object = new JsonObject();
object.put("foo", "bar").put("num", 123).put("mybool", true);
```

#### 从JSON对象获取值 {#Getting_values_from_a_JSON_object}
您可以使用`getXXX`方法从JSON对象获取值，例如：

```java
String val = jsonObject.getString("some-key");
int intVal = jsonObject.getInteger("some-other-key");
```

#### JSON对象和Java对象之间的映射 {#Mapping_between_JSON_objects_and_Java_objects}
您可以从Java对象的字段创建一个JSON对象，如下所示:

您可以实例化Java对象并从JSON对象填充其字段，如下所示：

```java
request.bodyHandler(buff -> {
  JsonObject jsonObject = buff.toJsonObject();
  User javaObject = jsonObject.mapTo(User.class);
});
```

请注意，以上两个映射说明均使用Jackson的`ObjectMapper#convertValue()`进行映射。 有关字段和构造函数可见性的影响，对跨对象引用的序列化和反序列化的警告等信息，请参阅Jackson文档。

但是，在最简单的情况下，如果Java类的所有字段都是公共的（或具有公共的getters/setters），并且存在公共的默认构造函数（或没有定义的构造函数），则`mapFrom`和`mapTo`应该都将成功。

只要对象图是非循环的，被引用的对象就可以在嵌套JSON对象之间进行传递序列化/反序列化。

#### 将JSON对象编码为字符串 {#Encoding_a_JSON_object_to_a_String}
使用`encode`将对象编码为字符串形式。

### JSON 数组 {#JSON_arrays}
`JsonArray`类表示JSON数组。

JSON数组是一系列值（字符串，数字，布尔值）。

JSON数组也可以包含空值。

#### 创建JSON数组 {#Creating_JSON_arrays}
可以使用默认构造函数创建空的JSON数组。

您可以从字符串JSON表示形式创建JSON数组，如下所示：

```java
String jsonString = "[\"foo\",\"bar\"]";
JsonArray array = new JsonArray(jsonString);
```

#### 将条目添加到JSON数组中 {#Adding_entries_into_a_JSON_array}
您可以使用`add`方法将条目添加到JSON数组中。

```java
JsonArray array = new JsonArray();
array.add("foo").add(123).add(false);
```

#### 从JSON数组获取值 {#Getting_values_from_a_JSON_array}
您可以使用`getXXX`方法从JSON数组中获取值，例如：

```java
String val = array.getString(0);
Integer intVal = array.getInteger(1);
Boolean boolVal = array.getBoolean(2);
```

#### 将JSON数组编码为字符串 {#Encoding_a_JSON_array_to_a_String}
您可以使用`encode`将数组编码为String形式。

#### 创建任意JSON {#Creating_arbitrary_JSON}
创建JSON对象和数组假定您正在使用有效的字符串表示形式。

如果不确定字符串的有效性，则应改用`Json.decodeValue`。

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

## JSON指针 {#Json_Pointers}
Vert.x提供了[来自RFC6901的Json指针](https://tools.ietf.org/html/rfc6901)的实现。 您可以将指针用于查询和编写。 您可以使用字符串，URI或手动添加路径来构建`JsonPointer`：

```java
JsonPointer pointer1 = JsonPointer.from("/hello/world");
// Build a pointer manually
JsonPointer pointer2 = JsonPointer.create()
  .append("hello")
  .append("world");
```

实例化指针后，使用`queryJson`来查询JSON值。 您可以使用`writeJson`来更新Json值：

```java
Object result1 = objectPointer.queryJson(jsonObject);
// Query a JsonArray
Object result2 = arrayPointer.queryJson(jsonArray);
// Write starting from a JsonObject
objectPointer.writeJson(jsonObject, "new element");
// Write starting from a JsonObject
arrayPointer.writeJson(jsonArray, "new element");
```

您可以通过提供`JsonPointerIterator`的自定义实现，将Vert.x的Json指针与任何对象模型一起使用

## 缓冲区 {#Buffers}
大多数数据使用缓冲区在Vert.x内部混洗。

缓冲区是可以读取或写入的零个或多个字节的序列，并根据需要自动扩展以容纳写入其中的任何字节。 您也许可以将缓冲区视为智能字节数组。

### 创建缓冲区 {#Creating_buffers}
可以使用静态的`Buffer.buffer`方法之一来创建缓冲区。

可以从字符串或字节数组初始化缓冲区，也可以创建空缓冲区。

以下是一些创建缓冲区的示例：

创建一个新的空缓冲区：

```java
Buffer buff = Buffer.buffer();
```

从字符串创建缓冲区。 字符串将使用UTF-8在缓冲区中编码。

```java
Buffer buff = Buffer.buffer("some string");
```

从字符串创建缓冲区：将使用指定的编码对字符串进行编码，例如：

```java
Buffer buff = Buffer.buffer("some string", "UTF-16");
```

从`byte[]`创建缓冲区

```java
byte[] bytes = new byte[] {1, 3, 5};
Buffer buff = Buffer.buffer(bytes);
```

创建一个带有初始大小提示的缓冲区。 如果您知道缓冲区中将写入一定数量的数据，则可以创建缓冲区并指定此大小。 这使得缓冲区最初分配了那么多内存，并且比缓冲区在将数据写入缓冲区时自动多次调整大小的效率更高。

注意，以这种方式创建的缓冲区是**空的**。它不会创建一个满是0到指定大小的缓冲区。

```java
Buffer buff = Buffer.buffer(10000);
```

### 写入缓冲区 {#Writing_to_a_Buffer}
有两种写入缓冲区的方法：追加和随机访问。 无论哪种情况，缓冲区将始终自动扩展以包含字节。 带有缓冲区的`IndexOutOfBoundsException`是不可能的。

#### 追加到缓冲区 {#Appending_to_a_Buffer}
要追加到缓冲区，可以使用`appendXXX`方法。 存在用于附加各种不同类型的附加方法。

 `appendXXX`方法的返回值是缓冲区本身，因此可以将它们链接起来：

```java
Buffer buff = Buffer.buffer();

buff.appendInt(123).appendString("hello\n");

socket.write(buff);
```

#### 随机存取缓冲区写入 {#Random_access_buffer_writes}
您也可以使用`setXXX`方法以特定的索引写入缓冲区。 存在用于各种不同数据类型的设置方法。 所有set方法都将索引作为第一个参数-这表示缓冲区中开始写入数据的位置。

缓冲区将始终根据需要扩展以容纳数据。

```java
Buffer buff = Buffer.buffer();

buff.setInt(1000, 123);
buff.setString(0, "hello");
```

### 从缓冲区读取 {#Reading_from_a_Buffer}
使用`getXXX`方法从缓冲区读取数据。 存在各种数据类型的Get方法。 这些方法的第一个参数是缓冲区中从何处获取数据的索引。

```java
Buffer buff = Buffer.buffer();
for (int i = 0; i < buff.length(); i += 4) {
  System.out.println("int value at " + i + " is " + buff.getInt(i));
}
```

### 使用无符号数字 {#Working_with_unsigned_numbers}
可以使用`getUnsignedXXX`，`appendUnsignedXXX`和`setUnsignedXXX`方法从缓冲区读取无符号的数字或将其附加/设置到缓冲区。 在为网络协议实现编解码器时最有用的，该编解码器已优化以最小化带宽消耗。

在下面的例子中，值200被设置在指定的位置，只有一个字节:

```java
Buffer buff = Buffer.buffer(128);
int pos = 15;
buff.setUnsignedByte(pos, (short) 200);
System.out.println(buff.getUnsignedByte(pos));
```

控制台显示“200”。

### 缓冲区长度 {#Buffer_length}
使用`length`获得缓冲区的长度。 缓冲区的长度是缓冲区的最大索引+ 1。

### 复制缓冲区 {#Copying_buffers}
使用`copy`制作缓冲区的副本

### 切片缓冲区 {#Slicing_buffers}
切片缓冲区是一个新的缓冲区，它返回到原始缓冲区，即它不复制底层数据。使用`slice`创建一个切片缓冲区

### 缓冲区重用 {#Buffer_re_use}
将缓冲区写入套接字或其他类似位置后，将无法重复使用它们。

## 编写TCP服务器和客户端 {#Writing_TCP_servers_and_clients}
Vert.x允许您轻松编写不阻塞的TCP客户端和服务器。

### 创建一个TCP服务器 {#Creating_a_TCP_server}
使用所有默认选项创建TCP服务器的最简单方法如下：

```java
NetServer server = vertx.createNetServer();
```

### 配置TCP服务器 {#Configuring_a_TCP_server}
如果您不希望使用默认值，则可以通过在创建服务器时传入`NetServerOptions`实例来配置服务器：

```java
NetServerOptions options = new NetServerOptions().setPort(4321);
NetServer server = vertx.createNetServer(options);
```

### 开始服务器监听 {#Start_the_Server_Listening}
要告诉服务器侦听传入请求，可以使用`listen`替代方法之一。

要告诉服务器侦听选项中指定的主机和端口：

```java
NetServer server = vertx.createNetServer();
server.listen();
```

或在调用中指定要监听的主机和端口，而忽略选项中配置的内容：

```java
NetServer server = vertx.createNetServer();
server.listen(1234, "localhost");
```

默认主机为`0.0.0.0`，表示“监听所有可用地址”，默认端口为`0`，这是一个特殊值，指示服务器查找随机未使用的本地端口并使用该端口。

实际的绑定是异步的，因此服务器可能调用返回之后一段时间才真正在监听。

如果希望在服务器实际监听时收到通知，则可以为`listen`调用提供处理程序。 例如：

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

### 在随机端口上监听 {#Listening_on_a_random_port}
如果将`0`用作侦听端口，则服务器将找到一个未使用的随机端口进行侦听。

要找出服务器正在监听的真实端口，可以调用`actualPort`。

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

### 收到传入连接的通知 {#Getting_notified_of_incoming_connections}
要在建立连接时收到通知，您需要设置一个`connectHandler`：

```java
NetServer server = vertx.createNetServer();
server.connectHandler(socket -> {
  // Handle the connection in here
});
```

建立连接后，将使用`NetSocket`实例调用处理程序。

这是实际连接的类似于套接字的接口，它允许您读取和写入数据以及执行其他各种操作，例如关闭套接字。

### 从套接字读取数据 {#Reading_data_from_the_socket}
要从套接字读取数据，请在套接字上设置`handler`。

每次在套接字上接收到数据时，将使用`Buffer`实例调用此处理程序。

```java
NetServer server = vertx.createNetServer();
server.connectHandler(socket -> {
  socket.handler(buffer -> {
    System.out.println("I received some bytes: " + buffer.length());
  });
});
```

### 将数据写入套接字 {#Writing_data_to_a_socket}
您使用`write`之一写入套接字。

```java
Buffer buffer = Buffer.buffer().appendFloat(12.34f).appendInt(123);
socket.write(buffer);

// Write a string in UTF-8 encoding
socket.write("some data");

// Write a string using the specified encoding
socket.write("some data", "UTF-16");
```

写操作是异步的，直到写调用返回后一段时间才可能发生。

### 关闭处理程序 {#Closed_handler}
如果您想在套接字关闭时收到通知，可以在其上设置一个`closeHandler`：

```java
socket.closeHandler(v -> {
  System.out.println("The socket has been closed");
});
```

### 处理异常 {#Handling_exceptions}
您可以设置`exceptionHandler`来接收套接字上发生的任何异常。

您可以设置`exceptionHandler`来接收将连接传递给`connectHandler`之前发生的任何异常，例如在TLS握手期间。

### 事件总线写处理程序 {#Event_bus_write_handler}
每个套接字都会在事件总线上自动注册一个处理程序，并且在此处理程序中接收到任何缓冲区时，它将它们写入自身。

这使您能够将数据写入socket，socket可能位于完全不同的verticle，甚至可能位于不同的 Vert.x实例。将缓冲区发送到该处理程序的地址。

处理程序的地址是`writeHandlerID`

### 本地和远程地址 {#Local_and_remote_addresses}
可以使用`localAddress`检索`NetSocket`的本地地址。

可以使用`remoteAddress`来检索`NetSocket`的远程地址（即连接另一端的地址）。

### 从类路径发送文件或资源 {#Sending_files_or_resources_from_the_classpath}
文件和类路径资源可以直接使用`sendFile`写入套接字。 这是一种非常有效的发送文件的方式，因为它可以由OS内核直接在操作系统支持的地方进行处理。

有关限制或禁用类路径解析的信息，请参阅有关[从类路径提供文件](https://vertx.io/docs/vertx-core/java/#classpath)的章节。

```java
socket.sendFile("myfile.dat");
```

### 流式套接字 {#Streaming_sockets}
`NetSocket`的实例同时也是`ReadStream`和`WriteStream`实例，因此它们可用于向其他读写流中泵送数据。

有关更多信息，请参见[流和泵](https://vertx.io/docs/vertx-core/java/#streams)一章。

### 将连接升级到SSL/TLS {#Upgrading_connections_to_SSL_TLS}
非SSL/TLS连接可以使用`upgradeToSsl`升级到SSL/TLS。

必须为SSL/TLS配置服务器或客户端才能正常工作。 有关更多信息，请参见[SSL/TLS章节](https://vertx.io/docs/vertx-core/java/#ssl) 。

### 关闭TCP服务器 {#Closing_a_TCP_Server}
调用`close`关闭服务器。 关闭服务器将关闭所有打开的连接并释放所有服务器资源。

关闭实际上是异步的，并且可能直到调用返回后的一段时间才能完成。 如果您想在实际关闭完成时收到通知，则可以传入处理程序。

关闭完成后，将调用此处理程序。

```java
server.close(res -> {
  if (res.succeeded()) {
    System.out.println("Server is now closed");
  } else {
    System.out.println("close failed");
  }
});
```

### 自动清理的verticles {#Automatic_clean_up_in_verticles}
如果您要从verticles中创建TCP服务器和客户端，则取消部署verticles时，这些服务器和客户端将自动关闭。

### 扩展-共享TCP服务器 {#Scaling___sharing_TCP_servers}
任何TCP服务器的处理程序始终在同一事件循环线程上执行。

这意味着，如果您在具有很多核心的服务器上运行，并且仅部署了一个实例，那么您的服务器上最多将使用一个核心。

为了利用服务器的更多核心，您将需要部署服务器的更多实例。

您可以在代码中以编程方式实例化更多实例：

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

或者，如果您使用的是verticle，则可以通过在命令行上使用`-instances`选项来简单地部署服务器verticle的更多实例：

vertx 运行 `com.mycompany.MyVerticle -instances 10`

或以编程方式部署verticle

```java
DeploymentOptions options = new DeploymentOptions().setInstances(10);
vertx.deployVerticle("com.mycompany.MyVerticle", options);
```

完成此操作后，您将发现echo服务器在功能上与以前相同，但是可以利用服务器上的所有内核，并且可以处理更多工作。

此时，您可能会问自己 **'如何让多个服务器监听同一主机和端口?当您尝试并部署多个实例时，您肯定会遇到端口冲突吗?'**

**Vert.x在这里做了一点魔术。**

当您在与现有服务器相同的主机和端口上部署另一台服务器时，实际上并不会尝试创建在同一主机/端口上侦听的新服务器。

相反，它在内部仅维护一台服务器，并且随着传入连接的到达，它将以循环方式将它们分配给任何连接处理程序。

因此，Vert.x TCP服务器可以扩展可用核心，而每个实例保持单线程。

### 创建一个TCP客户端 {#Creating_a_TCP_client}
使用所有默认选项创建TCP客户端的最简单方法如下：

```java
NetClient client = vertx.createNetClient();
```

### 配置TCP客户端 {#Configuring_a_TCP_client}
如果您不希望使用默认值，则可以通过在创建客户端时传入`NetClientOptions`实例来配置客户端：

```java
NetClientOptions options = new NetClientOptions().setConnectTimeout(10000);
NetClient client = vertx.createNetClient(options);
```

### 建立连接 {#Making_connections}
要与服务器建立连接，请使用`connect`，指定服务器的端口和主机以及将被调用的处理程序，连接成功时将包含`NetSocket`的结果，如果连接失败则返回错误。

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

### 配置连接尝试 {#Configuring_connection_attempts}
可以将客户端配置为在无法连接的情况下自动重试连接服务器。 这是通过`setReconnectInterval`和`setReconnectAttempts`配置的。

------
> **注意:** 当前，如果连接失败，Vert.x将不会尝试重新连接，重新连接尝试和间隔仅适用于创建初始连接。
------

```java
NetClientOptions options = new NetClientOptions().
  setReconnectAttempts(10).
  setReconnectInterval(500);

NetClient client = vertx.createNetClient(options);
```

默认情况下，禁用多次连接尝试。

### 记录网络活动 {#Logging_network_activity}
出于调试目的，可以记录网络活动：

```java
NetServerOptions options = new NetServerOptions().setLogActivity(true);

NetServer server = vertx.createNetServer(options);
```

客户端

```java
NetClientOptions options = new NetClientOptions().setLogActivity(true);

NetClient client = vertx.createNetClient(options);
```

Netty用`DEBUG`级别和`io.netty.handler.logging.LoggingHandler`名称记录网络活动。 使用网络活动日志记录时，请记住以下几点：

- 记录不是由Vert.x记录执行，而是由Netty执行
- 这**不是**生产功能

您应该阅读[Netty日志记录](https://vertx.io/docs/vertx-core/java/#netty-logging)部分。

### 配置服务器和客户端以使用SSL/TLS {#Configuring_servers_and_clients_to_work_with_SSL_TLS}
可以将TCP客户端和服务器配置为使用[传输层安全性](https://en.wikipedia.org/wiki/Transport_Layer_Security)-TLS的早期版本称为SSL。

无论是否使用SSL/TLS，服务器和客户端的API都是相同的，并且可以通过配置用于创建服务器或客户端的`NetClientOptions` 或 `NetServerOptions`实例来启用它们。

#### 在服务器上启用SSL/TLS {#Enabling_SSL_TLS_on_the_server}
SSL/TLS通过`ssl`启用。

默认情况下，它是禁用的。

#### 指定服务器的密钥/证书 {#Specifying_key_certificate_for_the_server}
SSL/TLS服务器通常向客户端提供证书，以便向客户端验证其身份。

可以通过多种方式为服务器配置证书/密钥：

第一种方法是通过指定包含证书和私钥的Java密钥存储的位置。

可以使用JDK附带的[keytool](https://docs.oracle.com/javase/6/docs/technotes/tools/solaris/keytool.html)实用程序来管理Java密钥库。

还应提供密钥库的密码：

```java
NetServerOptions options = new NetServerOptions().setSsl(true).setKeyStoreOptions(
  new JksOptions().
    setPath("/path/to/your/server-keystore.jks").
    setPassword("password-of-your-keystore")
);
NetServer server = vertx.createNetServer(options);
```

或者，您可以自己将密钥存储区作为缓冲区读取并直接提供：

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

密钥/证书，格式为PKCS#12([http://en.wikipedia.org/wiki/PKCS_12](https://en.wikipedia.org/wiki/PKCS_12))，通常带有`.pfx`或`.p12`扩展名也可以以与JKS密钥库类似的方式加载：

```java
NetServerOptions options = new NetServerOptions().setSsl(true).setPfxKeyCertOptions(
  new PfxOptions().
    setPath("/path/to/your/server-keystore.pfx").
    setPassword("password-of-your-keystore")
);
NetServer server = vertx.createNetServer(options);
```

还支持缓冲区配置：

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

使用`.pem`文件分别提供服务器私钥和证书的另一种方法。

```java
NetServerOptions options = new NetServerOptions().setSsl(true).setPemKeyCertOptions(
  new PemKeyCertOptions().
    setKeyPath("/path/to/your/server-key.pem").
    setCertPath("/path/to/your/server-cert.pem")
);
NetServer server = vertx.createNetServer(options);
```

还支持缓冲区配置：

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

Vert.x支持从PKCS8 PEM文件中读取未加密的基于RSA和/或ECC的私钥。 还可以从PKCS1 PEM文件中读取基于RSA的私钥。 可以从包含[RFC 7468，第5节](https://tools.ietf.org/html/rfc7468#section-5)定义的证书文本编码的PEM文件中读取X.509证书。

------
> **警告:** 请记住，任何可以读取该文件的人都可以提取未加密的PKCS8或PKCS1 PEM文件中包含的密钥。 因此，请确保对此类PEM文件设置适当的访问限制，以防止滥用。
------

#### 指定服务器的信任 {#Specifying_trust_for_the_server}
SSL/TLS服务器可以使用证书颁发机构来验证客户端的身份。

可以通过多种方式为服务器配置证书颁发机构：

可以使用JDK附带的[keytool](https://docs.oracle.com/javase/6/docs/technotes/tools/solaris/keytool.html)实用程序来管理Java信任库。

还应提供信任库的密码：

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

或者，您可以自己将信任库作为缓冲区读取，并直接提供该缓冲区：

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

PKCS#2格式([http://en.wikipedia.org/wiki/PKCS_12](https://en.wikipedia.org/wiki/PKCS_12))的证书颁发机构，通常带有`.pfx`或`.p12`扩展名也可以类似于JKS信任库的方式加载：

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

还支持缓冲区配置：

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

使用列表`.pem`文件提供服务器证书颁发机构的另一种方法。

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

还支持缓冲区配置：

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

#### 在客户端上启用SSL/TLS {#Enabling_SSL_TLS_on_the_client}
Net Client也可以轻松配置为使用SSL。 使用SSL和使用标准套接字时，它们具有完全相同的API。

要在NetClient上启用SSL，请调用函数`setSSL(true)`。

#### 客户端信任配置 {#Client_trust_configuration}
如果在客户端上将`trustAll`设置为true，则客户端将信任所有服务器证书。 连接仍将被加密，但是此模式容易受到“中间人”攻击。 即 您不确定要连接到谁。 请谨慎使用。 默认值为false。

```java
NetClientOptions options = new NetClientOptions().
  setSsl(true).
  setTrustAll(true);
NetClient client = vertx.createNetClient(options);
```

如果未设置`trustAll`，则必须配置客户端信任存储，并且应包含客户端信任的服务器的证书。

默认情况下，在客户端上禁用主机验证。 要启用主机验证，请设置要在客户端上使用的算法（当前仅支持HTTPS和LDAPS）：

```java
NetClientOptions options = new NetClientOptions().
  setSsl(true).
  setHostnameVerificationAlgorithm("HTTPS");
NetClient client = vertx.createNetClient(options);
```

与服务器配置相同，可以通过以下几种方式配置客户端信任：

第一种方法是通过指定包含证书颁发机构的Java信任库的位置。

它只是一个标准的Java密钥库，与服务器端的密钥库相同。 客户信任库位置是通过使用`jks options`上的函数`path`来设置的。 如果服务器在连接过程中出示的证书不在客户端信任存储区中，则连接尝试将不会成功。

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

还支持缓冲区配置：

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

PKCS#12格式([http://en.wikipedia.org/wiki/PKCS_12](https://en.wikipedia.org/wiki/PKCS_12))的证书颁发机构，通常带有`.pfx`或`.p12`扩展名也可以类似于JKS信任库的方式加载：

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

还支持缓冲区配置：

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

使用列表`.pem`文件提供服务器证书颁发机构的另一种方法。

```java
NetClientOptions options = new NetClientOptions().
  setSsl(true).
  setPemTrustOptions(
    new PemTrustOptions().
      addCertPath("/path/to/your/ca-cert.pem")
  );
NetClient client = vertx.createNetClient(options);
```

还支持缓冲区配置：

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

#### 指定客户端的密钥/证书 {#Specifying_key_certificate_for_the_client}
如果服务器要求客户端身份验证，则客户端在连接时必须向服务器出示自己的证书。 可以通过几种方式配置客户端：

第一种方法是通过指定包含密钥和证书的Java密钥存储的位置。 同样，它只是常规的Java密钥存储区。 客户端密钥库的位置是通过使用`jks options`上的函数`path`来设置的。

```java
NetClientOptions options = new NetClientOptions().setSsl(true).setKeyStoreOptions(
  new JksOptions().
    setPath("/path/to/your/client-keystore.jks").
    setPassword("password-of-your-keystore")
);
NetClient client = vertx.createNetClient(options);
```

还支持缓冲区配置：

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

密钥/证书，格式为PKCS#12([http://en.wikipedia.org/wiki/PKCS_12](https://en.wikipedia.org/wiki/PKCS_12))，通常带有`.pfx`或`.p12扩展名也可以以与JKS密钥库类似的方式加载：

```java
NetClientOptions options = new NetClientOptions().setSsl(true).setPfxKeyCertOptions(
  new PfxOptions().
    setPath("/path/to/your/client-keystore.pfx").
    setPassword("password-of-your-keystore")
);
NetClient client = vertx.createNetClient(options);
```

还支持缓冲区配置：

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

使用`.pem`文件分别提供服务器私钥和证书的另一种方法。

```java
NetClientOptions options = new NetClientOptions().setSsl(true).setPemKeyCertOptions(
  new PemKeyCertOptions().
    setKeyPath("/path/to/your/client-key.pem").
    setCertPath("/path/to/your/client-cert.pem")
);
NetClient client = vertx.createNetClient(options);
```

还支持缓冲区配置：

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

请记住，pem配置中，私钥未加密。

#### 用于测试和开发目的的自签名证书 {#Self_signed_certificates_for_testing_and_development_purposes}

------
> **慎重:** 不要在生产设置中使用此功能，请注意，生成的密钥非常不安全。
------

通常，需要自签名证书，无论是用于单元/集成测试还是用于运行应用程序的开发版本。

`SelfSignedCertificate`可用于提供自签名的PEM证书助手，并提供`KeyCertOptions`和`TrustOptions`配置：

```java
SelfSignedCertificate certificate = SelfSignedCertificate.create();

NetServerOptions serverOptions = new NetServerOptions()
  .setSsl(true)
  .setKeyCertOptions(certificate.keyCertOptions())
  .setTrustOptions(certificate.trustOptions());

NetServer server = vertx.createNetServer(serverOptions)
  .connectHandler(socket -> socket.write("Hello!").end())
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

客户端也可以配置为信任所有证书：

```java
NetClientOptions clientOptions = new NetClientOptions()
  .setSsl(true)
  .setTrustAll(true);
```

请注意，自签名证书还适用于其他TCP协议（例如HTTPS）：

```java
SelfSignedCertificate certificate = SelfSignedCertificate.create();

vertx.createHttpServer(new HttpServerOptions()
  .setSsl(true)
  .setKeyCertOptions(certificate.keyCertOptions())
  .setTrustOptions(certificate.trustOptions()))
  .requestHandler(req -> req.response().end("Hello!"))
  .listen(8080);
```

#### 吊销证书颁发机构 {#Revoking_certificate_authorities}
可以将信任配置为使用证书吊销列表（CRL）来处理应该不再受信任的吊销证书。 `crlPath`配置crl列表以使用：

```java
NetClientOptions options = new NetClientOptions().
  setSsl(true).
  setTrustStoreOptions(trustOptions).
  addCrlPath("/path/to/your/crl.pem");
NetClient client = vertx.createNetClient(options);
```

还支持缓冲区配置：

```java
Buffer myCrlAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/crl.pem");
NetClientOptions options = new NetClientOptions().
  setSsl(true).
  setTrustStoreOptions(trustOptions).
  addCrlValue(myCrlAsABuffer);
NetClient client = vertx.createNetClient(options);
```

#### 配置密码套件 {#Configuring_the_Cipher_suite}
默认情况下，TLS配置将使用运行Vert.x的JVM的Cipher套件。 可以使用以下一组启用密码来配置此密码套件：

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

密码套件可以在`NetServerOptions`或`NetClientOptions`配置中指定。

#### 配置TLS协议版本 {#Configuring_TLS_protocol_versions}
默认情况下，TLS配置将使用以下协议版本：SSLv2Hello，TLSv1，TLSv1.1和TLSv1.2。 可以通过显式添加启用的协议来配置协议版本：

```java
NetServerOptions options = new NetServerOptions().
  setSsl(true).
  setKeyStoreOptions(keyStoreOptions).
  removeEnabledSecureTransportProtocol("TLSv1").
  addEnabledSecureTransportProtocol("TLSv1.3");
NetServer server = vertx.createNetServer(options);
```

协议版本可以在`NetServerOptions`或`NetClientOptions`配置中指定。

#### SSL引擎 {#SSL_engine}
可以将引擎实现配置为使用[OpenSSL](https://www.openssl.org/)而不是JDK实现。 与JDK引擎相比，OpenSSL提供了更好的性能和CPU使用率，并且具有JDK版本独立性。

使用的引擎选项是

- 设置时的`getSslEngineOptions`选项
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

#### 服务器名称指示(SNI) {#Server_Name_Indication__SNI_}
服务器名称指示（SNI）是TLS扩展，客户端通过该扩展名指定尝试连接的主机名：在TLS握手期间，客户端提供服务器名称，服务器可以使用该名称来响应该服务器名称的特定证书而不是 默认部署的证书。 如果服务器要求客户端身份验证，则服务器可以根据指定的服务器名称使用特定的受信任CA证书。

当SNI处于活动状态时，服务器使用

- 证书CN或SAN DNS（带有DNS的主题备用名称）进行完全匹配，例如`www.example.com`
- 证书CN或SAN DNS证书以匹配通配符名称，例如`*.example.com`
- 否则，当客户端不提供服务器名称或提供的服务器名称时，第一个证书无法匹配

当服务器另外要求客户端身份验证时：

- 如果使用`JksOptions`来设置信任选项(`options`)，则将与信任库别名完全匹配
- 否则，可用的CA证书的使用方式与没有SNI的使用方式相同

您可以通过将`setSni`设置为`true`并在服务器上配置多个密钥/证书对来在服务器上启用SNI。

Java KeyStore文件或PKCS12文件可以开箱即用地存储多个密钥/证书对。

```java
JksOptions keyCertOptions = new JksOptions().setPath("keystore.jks").setPassword("wibble");

NetServer netServer = vertx.createNetServer(new NetServerOptions()
    .setKeyStoreOptions(keyCertOptions)
    .setSsl(true)
    .setSni(true)
);
```

可以将`PemKeyCertOptions`配置为保存多个条目：

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

客户端隐式发送连接主机作为完全合格域名（FQDN）的SNI服务器名称。

您可以在连接套接字时提供一个明确的服务器名称

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

它可以用于不同的目的：

- 提供与服务器主机不同的服务器名称
- 连接到IP时显示服务器名称
- 使用短名称时强制显示服务器名称

#### 应用层协议协商(ALPN) {#Application_Layer_Protocol_Negotiation__ALPN_}
应用程序层协议协商（ALPN）是用于应用程序层协议协商的TLS扩展。 它由HTTP/2使用：在TLS握手期间，客户端会提供其接受的应用程序协议列表，服务器将以其支持的协议进行响应。

如果您使用的是Java 9，那很好，您可以直接使用HTTP/2，而无需执行其他步骤。

Java 8不支持现成的ALPN，因此应通过其他方式启用ALPN：

- *OpenSSL* 支持
- *Jetty-ALPN* 支持

使用的引擎选项是

- 设置时的`getSslEngineOptions`选项
- 当ALPN可用于JDK时，将使用`JdkSSLEngineOptions`
- ALPN可用于OpenSSL时使用`OpenSSLEngineOptions`
- 否则失败

##### OpenSSL ALPN支持 {#OpenSSL_ALPN_support}
OpenSSL提供本机ALPN支持。

OpenSSL需要配置`setOpenSslEngineOptions`，并在类路径上使用[netty-tcnative](http://netty.io/wiki/forked-tomcat-native.html)jar。 根据tcnative的实现，使用tcnative可能需要在您的操作系统上安装OpenSSL。

##### Jetty-ALPN支持 {#Jetty_ALPN_support}
Jetty-ALPN是一个小jar，它覆盖了Java 8分发的一些类以支持ALPN。

JVM必须在其`bootclasspath`中以*alpn-boot-${version}.jar*启动：

```java
-Xbootclasspath/p:/path/to/alpn-boot${version}.jar
```

其中${version}取决于JVM版本，例如 *OpenJDK 1.8.0u74* 的 *8.1.7.v20160121*。 完整列表可在[Jetty-ALPN页面](https://www.eclipse.org/jetty/documentation/current/alpn-chapter.html)上找到。

主要缺点是版本取决于JVM。

为了解决这个问题，可以使用*[Jetty ALPN agent](https://github.com/jetty-project/jetty-alpn-agent)*。 该代理是一个JVM代理，它将为运行它的JVM选择正确的ALPN版本：

```java
-javaagent:/path/to/alpn/agent
```

### 使用代理进行客户端连接 {#Using_a_proxy_for_client_connections}
`NetClient`支持HTTP/1.x *CONNECT*, *SOCKS4a* 或 *SOCKS5*代理。

通过设置包含代理类型，主机名，端口以及用户名和密码（可选）的`ProxyOptions`对象，可以在`NetClientOptions`中配置代理。

这是一个例子：

```java
NetClientOptions options = new NetClientOptions()
  .setProxyOptions(new ProxyOptions().setType(ProxyType.SOCKS5)
    .setHost("localhost").setPort(1080)
    .setUsername("username").setPassword("secret"));
NetClient client = vertx.createNetClient(options);
```

DNS解析始终在代理服务器上完成，为了实现SOCKS4客户端的功能，必须在本地解析DNS地址。

## 编写HTTP服务器和客户端 {#Writing_HTTP_servers_and_clients}
Vert.x允许您轻松编写不阻塞的HTTP客户端和服务器。

Vert.x支持HTTP/1.0, HTTP/1.1 和 HTTP/2协议。

HTTP的基本API与HTTP/1.x和HTTP/2相同，特定的API功能可用于处理HTTP/2协议。

### 创建一个HTTP服务器 {#Creating_an_HTTP_Server}
使用所有默认选项创建HTTP服务器的最简单方法如下：

```java
HttpServer server = vertx.createHttpServer();
```

### 配置HTTP服务器 {#Configuring_an_HTTP_server}
如果您不希望使用默认值，则可以在创建服务器时通过传入`HttpServerOptions`实例来配置服务器：

```java
HttpServerOptions options = new HttpServerOptions().setMaxWebsocketFrameSize(1000000);

HttpServer server = vertx.createHttpServer(options);
```

### 配置HTTP/2服务器 {#Configuring_an_HTTP_2_server}
Vert.x通过TLS `h2`和TCP `h2c`支持HTTP/2。

- `h2` 在[应用层协议协商](https://en.wikipedia.org/wiki/Application-Layer_Protocol_Negotiation) (ALPN)协商的TLS上使用时，标识HTTP/2协议
- 当在TCP上以明文形式使用时，`h2c`标识HTTP/2协议，此类连接可以通过HTTP/1.1升级请求建立，也可以直接建立

要处理`h2`请求，必须将TLS和`setUseAlpn`一起启用：

```java
HttpServerOptions options = new HttpServerOptions()
    .setUseAlpn(true)
    .setSsl(true)
    .setKeyStoreOptions(new JksOptions().setPath("/path/to/my/keystore"));

HttpServer server = vertx.createHttpServer(options);
```

ALPN是TLS扩展，可在客户端和服务器开始交换数据之前协商协议。

不支持ALPN的客户端仍可以进行*经典* SSL握手。

ALPN通常会同意`h2`协议，尽管如果服务器或客户端决定使用http/1.1，则可以使用。

为了处理`h2c`请求，必须禁用TLS，服务器将任何要升级到HTTP/2的请求HTTP/1.1升级到HTTP/2。 它也将接受直接从`PRI * HTTP/2.0\r\nSM\r\n`前言开始的`h2c`连接。

------
> **警告:** 大多数浏览器均不支持`h2c`，因此，为网站服务时，应使用`h2`而不是`h2c`。
------

服务器接受HTTP/2连接时，会将其`initial settings(初始设置)`发送给客户端。 这些设置定义客户端如何使用连接，服务器的默认初始设置为：

- `getMaxConcurrentStreams`：HTTP/2 RFC建议的`100`
- 其他的默认HTTP/2设置值

------
> **注意:** Worker Verticles与HTTP/2不兼容
------

### 记录网络服务器活动 {#Logging_network_server_activity}
出于调试目的，可以记录网络活动。

```java
HttpServerOptions options = new HttpServerOptions().setLogActivity(true);

HttpServer server = vertx.createHttpServer(options);
```

有关详细说明，请参见[记录网络活动](https://vertx.io/docs/vertx-core/java/#logging_network_activity)一章。

### 开始服务器监听 {#Start_the_Server_Listening}
要告诉服务器侦听传入的请求，您可以使用一种`listen`方法。

要告诉服务器侦听选项中指定的主机和端口：

```java
HttpServer server = vertx.createHttpServer();
server.listen();
```

或在调用中指定要监听的主机和端口，而忽略选项中配置的内容：

```java
HttpServer server = vertx.createHttpServer();
server.listen(8080, "myhost.com");
```

默认主机为`0.0.0.0`，表示“监听所有可用地址”，默认端口为`80`。

实际的绑定是异步的，因此服务器可能调用返回之后一段时间才真正在监听。

如果希望在服务器实际监听时收到通知，则可以为`listen`调用提供处理程序。 例如：

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

### 收到传入请求的通知 {#Getting_notified_of_incoming_requests}
要在请求到达时得到通知，您需要设置`requestHandler`：

```java
HttpServer server = vertx.createHttpServer();
server.requestHandler(request -> {
  // Handle the request in here
});
```

### 处理请求 {#Handling_requests}
当请求到达时，调用请求处理程序传递`HttpServerRequest`的实例。 该对象代表服务器端HTTP请求。

当请求的头被完全读取时，将调用处理程序。

如果请求包含正文，则该正文将在调用请求处理程序后的某个时间到达服务器。

服务器请求对象允许您检索`uri`, `path`, `params` 和 `headers`等。

每个服务器请求对象都与一个服务器响应对象相关联。 您可以使用`response`来获取对`HttpServerResponse`对象的引用。

这是服务器处理请求并以“ hello world”回复的简单示例。

```java
vertx.createHttpServer().requestHandler(request -> {
  request.response().end("Hello world");
}).listen(8080);
```

#### Request 版本 {#Request_version}
可以使用`version`检索请求中指定的HTTP版本。

#### Request 方法 {#Request_method}
使用`method`检索请求的HTTP方法。 （即GET，POST，PUT，DELETE，HEAD，OPTIONS等）。

#### Request URI {#Request_URI}
使用`uri`检索请求的URI。

注意，这是在HTTP请求中传递的实际URI，它几乎总是一个相对URI。

URI如[HTTP规范的5.1.2节-请求URI](https://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html)中所定义。

#### Request 路径 {#Request_path}
使用`path`返回URI的路径部分

例如，如果请求URI为：

`a/b/c/page.html?param1=abc&param2=xyz`

那么路径将是

`/a/b/c/page.html`

#### Request query {#Request_query}
使用`query`返回URI的查询部分

例如，如果请求URI为：

`a/b/c/page.html?param1=abc&param2=xyz`

然后查询将是

`param1=abc&param2=xyz`

#### Request 头 {#Request_headers}
使用`headers`返回HTTP请求的标题。

这将返回`MultiMap`的实例-类似于普通Map或Hash，但允许同一键具有多个值-这是因为HTTP允许具有相同键的多个标头值。

它还具有不区分大小写的键，这意味着您可以执行以下操作：

```java
MultiMap headers = request.headers();

// Get the User-Agent:
System.out.println("User agent is " + headers.get("user-agent"));

// You can also do this and get the same result:
System.out.println("User agent is " + headers.get("User-Agent"));
```

#### Request host {#Request_host}
使用`host`返回HTTP请求的主机。

对于HTTP/1.x请求，返回`host`头，对于HTTP/1请求，返回`:authority`伪头。

#### Request 参数 {#Request_parameters}
使用`params`返回HTTP请求的参数。

就像`headers`一样，它会返回`MultiMap`的一个实例，因为可以有多个具有相同名称的参数。

请求参数在请求URI的路径之后发送。例如，如果URI是:

`/page.html?param1=abc&param2=xyz`

那么参数将包含以下内容:

```
param1: 'abc'
param2: 'xyz
```

请注意，这些请求参数是从请求的URL中检索的。 如果您在`multi-part/form-data`请求的正文中提交的表单属性是作为HTML表单提交的一部分发送的，则它们不会出现在此处的参数中。

#### Remote 地址 {#Remote_address}
可以使用`remoteAddress`检索请求的发送者的地址。

#### 绝对 URI {#Absolute_URI}
HTTP请求中传递的URI通常是相对的。 如果您希望检索与请求相对应的绝对URI，则可以使用`absoluteURI`获取它。

#### End handler {#End_handler}
当整个请求（包括任何主体）都已被完全读取时，将调用请求的`endHandler`。

#### 从请求主体读取数据 {#Reading_Data_from_the_Request_Body}
HTTP请求通常包含我们要读取的正文。 如前所述，仅当请求的标头到达时，请求处理程序才被调用，因此请求对象此时没有主体。

这是因为主体可能很大（例如，文件上传），而且我们通常不希望在将整个主体交给您之前将其缓存在内存中，因为那样可能会导致服务器耗尽可用内存。

要接收主体，您可以在请求上使用`handler`，每次请求主体的一部分到达时都会调用此函数。 这是一个例子：

```java
request.handler(buffer -> {
  System.out.println("I have received a chunk of the body of length " + buffer.length());
});
```

传递到处理程序中的对象是一个`Buffer`，当数据从网络到达时，可以多次调用该处理程序，具体取决于主体的大小。

在某些情况下（例如，如果主体很小），您将希望在内存中聚合整个主体，因此您可以自己进行聚合，如下所示：

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

这是很常见的情况，Vert.x提供了一个`bodyHandler`来为您执行此操作。 接收到所有主体后，将调用一次主体处理程序：

```java
request.bodyHandler(totalBuffer -> {
  System.out.println("Full body received, length = " + totalBuffer.length());
});
```

#### 泵送请求 {#Pumping_requests}
请求对象是`ReadStream`，因此您可以将请求主体泵送到任何`WriteStream`实例。

有关详细说明，请参见[流和泵](https://vertx.io/docs/vertx-core/java/#streams)一章。

#### 处理HTML表单 {#Handling_HTML_forms}
HTML表单可以以`application/x-www-form-urlencoded` 或 `multipart/form-data`的内容类型提交。

对于url编码的表单，表单属性是在url中编码的，就像普通的查询参数一样。

对于包含多个部分的表单，它们被编码在请求体中，因此在从连线读取整个表单体之前是不可用的。

包含多个部分的表单还可以包含文件上传。

如果你想检索一个多部分表单的属性，你应该告诉Vert。通过调用带有true的`setExpectMultipart`来读取任何主体之前，您希望接收到这样的表单，然后在读取整个主体之后，您应该使用`formAttributes`来检索实际属性:

```java
server.requestHandler(request -> {
  request.setExpectMultipart(true);
  request.endHandler(v -> {
    // The body has now been fully read, so retrieve the form attributes
    MultiMap formAttributes = request.formAttributes();
  });
});
```

#### 处理表单文件上传 {#Handling_form_file_uploads}
Vert.x还可以处理以多部分请求正文编码的文件上传。

要接收文件上传，您告诉Vert.x期望采用多部分表单，并在请求上设置`uploadHandler`。

对于每次到达服务器的上传，都会调用一次此处理程序。

传递到处理程序中的对象是一个`HttpServerFileUpload`实例。

```java
server.requestHandler(request -> {
  request.setExpectMultipart(true);
  request.uploadHandler(upload -> {
    System.out.println("Got a file upload " + upload.name());
  });
});
```

文件上传可能很大，我们不会在单个缓冲区中提供整个上传，因为这可能会导致内存耗尽，相反，上传数据是分块接收的：

```java
request.uploadHandler(upload -> {
  upload.handler(chunk -> {
    System.out.println("Received a chunk of the upload of length " + chunk.length());
  });
});
```

上传对象是一个ReadStream，因此您可以将请求正文泵送到任何一个`WriteStream`实例。 有关详细说明，请参见[流和泵](https://vertx.io/docs/vertx-core/java/#streams)一章。

如果您只想将文件上传到磁盘上的某个地方，可以使用`streamToFileSystem`：

```java
request.uploadHandler(upload -> {
  upload.streamToFileSystem("myuploads_directory/" + upload.filename());
});
```

------
> **警告:** 确保在生产系统中检查文件名，以避免恶意客户端将文件上传到文件系统上的任意位置。 有关更多信息，请参见[安全说明](https://vertx.io/docs/vertx-core/java/#_security_notes)。
------

#### 处理 cookies {#Handling_cookies}
使用`getCookie`按名称检索cookie，或使用`cookieMap`检索所有cookie。

要删除一个cookie，使用`removeCookie`。

要添加cookie，请使用`addCookie`。

当写入响应标头时，cookie集将自动写入响应中，以便浏览器可以存储它们。

Cookie由`Cookie`实例描述。这允许您检索名称、值、域、路径和其他常规cookie属性。

下面是一个查询和添加cookie的例子:

```java
Cookie someCookie = request.getCookie("mycookie");
String cookieValue = someCookie.getValue();

// Do something with cookie...

// Add a cookie - this will get written back in the response automatically
request.response().addCookie(Cookie.cookie("othercookie", "somevalue"));
```

#### 处理压缩的主体 {#Handling_compressed_body}
Vert.x可以处理由客户端使用*deflate* 或 *gzip*算法编码的压缩主体有效载荷。

要启用解压缩，请在创建服务器时在选项上设置`setDecompressionSupported`。

默认情况下解压是禁用的。

#### 接收自定义HTTP/2帧 {#Receiving_custom_HTTP_2_frames}
HTTP/2是一个框架协议，具有用于HTTP请求/响应模型的各种框架。该协议允许发送和接收其他类型的帧。

要接收自定义帧，您可以对请求使用`customFrameHandler`，它将在每次自定义帧到达时被调用。这里有一个例子:

```java
request.customFrameHandler(frame -> {

  System.out.println("Received a frame type=" + frame.type() +
      " payload" + frame.payload().toString());
});
```

HTTP/2帧不受流控制—当接收到自定义帧时，无论请求是否暂停，都会立即调用帧处理程序

#### 非标准的HTTP方法 {#Non_standard_HTTP_methods}
`OTHER` HTTP方法用于非标准方法，在本例中，`rawMethod`返回客户端发送的HTTP方法。

### 发送回响应 {#Sending_back_responses}
服务器响应对象是`HttpServerResponse`的一个实例，它是从带有`response`的请求中获得的。

您可以使用响应对象将响应写回到HTTP客户端。

#### 设置状态码和消息 {#Setting_status_code_and_message}
响应的默认HTTP状态码是`200`，表示`OK`。

使用`setStatusCode`来设置不同的代码。

您还可以使用`setStatusMessage`指定自定义状态消息。

如果没有指定状态消息，则将使用与状态代码对应的默认消息。

------
> **注意:** 对于HTTP/2，状态不会出现在响应中，因为协议不会将消息传输到客户端
------

#### 编写HTTP响应 {#Writing_HTTP_responses}
要将数据写入HTTP响应，需要使用`write`操作之一。

在响应结束之前，可以多次调用它们。 可以通过以下几种方式调用它们：

使用单个缓冲区：

```java
HttpServerResponse response = request.response();
response.write(buffer);
```

用一个字符串。在这种情况下，字符串将使用UTF-8进行编码，并将结果写入线路。

```java
HttpServerResponse response = request.response();
response.write("hello world!");
```

一个字符串和一个编码。在这种情况下，将使用指定的编码对字符串进行编码，并将结果写入线路。

```java
HttpServerResponse response = request.response();
response.write("hello world!", "UTF-16");
```

写入响应是异步的，并且总是在写入队列后立即返回。

如果你只是写一个字符串或缓冲区到HTTP响应你可以写它并结束响应在一个单一的调用`end`

首次写入调用会导致将响应标头写入响应。 因此，如果您不使用HTTP分块，则必须在写响应之前设置`Content-Length`标头，否则将为时已晚。 如果您使用的是HTTP分块，则不必担心。

#### 结束HTTP响应 {#Ending_HTTP_responses}
一旦你完成了HTTP响应，你应该`end`它。

这可以通过几种方式完成：

没有参数，响应就简单地结束了。

```java
HttpServerResponse response = request.response();
response.write("hello world!");
response.end();
```

也可以使用字符串或缓冲区调用它，方法与调用`write`相同。在本例中，它与使用字符串或缓冲区调用write，然后调用没有参数的end是一样的。例如:

```java
HttpServerResponse response = request.response();
response.end("hello world!");
```

#### 关闭基础连接 {#Closing_the_underlying_connection}
您可以使用`close`关闭底层TCP连接。

响应结束时，Vert.x将自动关闭非保持活动连接。

默认情况下，Vert.x不会自动关闭保持活动连接。 如果要在空闲时间后关闭保持活动的连接，则可以配置`setIdleTimeout`。

HTTP/2连接在关闭响应之前发送一个{@literal GOAWAY}帧。

#### 设置响应头 {#Setting_response_headers}
HTTP响应报头可以通过直接添加它们到`headers`响应：

```java
HttpServerResponse response = request.response();
MultiMap headers = response.headers();
headers.set("content-type", "text/html");
headers.set("other-header", "wibble");
```

或者你可以使用`putHeader`

```java
HttpServerResponse response = request.response();
response.putHeader("content-type", "text/html").putHeader("other-header", "wibble");
```

必须在写入响应主体的任何部分之前添加所有的响应头。

#### 分块的HTTP响应和trailers {#Chunked_HTTP_responses_and_trailers}
Vert.x支持[HTTP块传输编码](https://en.wikipedia.org/wiki/Chunked_transfer_encoding)。

这允许将HTTP响应主体分块编写，并且通常在将大型响应主体流式传输到客户端并且事先不知道总大小时使用。

您将HTTP响应置于分块模式，如下所示：

```java
HttpServerResponse response = request.response();
response.setChunked(true);
```

默认为非分块。 在分块模式下，每次调用write方法之一将导致新的HTTP块被写出。

在分块模式下，还可以编写HTTP响应trailers。这些实际上写在响应的最后一部分。

------
> **注意:** 分块响应对HTTP/2流无效
------

要将trailers添加到响应中，请将其直接添加到`trailers`中。

```java
HttpServerResponse response = request.response();
response.setChunked(true);
MultiMap trailers = response.trailers();
trailers.set("X-wibble", "woobble").set("X-quux", "flooble");
```

或使用`putTrailer`。

```java
HttpServerResponse response = request.response();
response.setChunked(true);
response.putTrailer("X-wibble", "woobble").putTrailer("X-quux", "flooble");
```

#### 直接从磁盘或类路径提供文件 {#Serving_files_directly_from_disk_or_the_classpath}
如果您正在编写Web服务器，则从磁盘提供文件的一种方法是将其作为`AsyncFile`打开并将其泵送至HTTP响应。

或者，您可以使用`readFile`一次性加载它，然后直接将其写入响应中。

另外，Vert.x提供了一种方法，使您可以通过一次操作将磁盘或文件系统中的文件提供给HTTP响应。 在底层操作系统支持的情况下，这可能会导致OS直接将字节从文件传输到套接字，而根本不通过用户空间进行复制。

这是通过使用`sendFile`完成的，通常对于大文件来说效率更高，但是对于小文件来说可能会更慢。

这是一个非常简单的网络服务器，它使用sendFile为文件系统中的文件提供服务：

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

发送文件是异步的，可能在调用返回后一段时间才会完成。如果你想在写文件时得到通知，你可以使用`sendFile`

请参阅有关[从类路径提供文件](https://vertx.io/docs/vertx-core/java/#classpath)的章节，以获取有关类路径解析或禁用它的限制。

------
> **注意:** 如果您在使用HTTPS时使用`sendFile`，它将通过用户空间进行复制，因为如果内核将数据直接从磁盘复制到套接字，则不会给我们提供任何加密的机会。
------

------
> **警告:** 如果您要直接使用Vert.x编写Web服务器，请注意用户不能利用该路径访问您要为其提供服务的目录或类路径之外的文件，而使用Vert.x Web可能更安全。
------

如果只需要服务文件的一部分，例如从给定的字节开始，则可以通过执行以下操作来实现：

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

你不需要提供长度，如果你想发送一个文件从一个偏移到结束，在这种情况下，你可以做:

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

#### Pumping 响应 {#Pumping_responses}
服务器响应是一个`WriteStream`实例，因此您可以从任何`ReadStream`， 例如， `AsyncFile`，`NetSocket`，`WebSocket`或`HttpServerRequest`。

这是一个示例，该示例针对任何PUT方法在响应中回显请求正文。 它为主体使用泵，因此即使HTTP请求主体比任何时候都可容纳在内存中的容量大得多，它也将起作用：

```java
vertx.createHttpServer().requestHandler(request -> {
  HttpServerResponse response = request.response();
  if (request.method() == HttpMethod.PUT) {
    response.setChunked(true);
    Pump.pump(request, response).start();
    request.endHandler(v -> response.end());
  } else {
    response.setStatusCode(400).end();
  }
}).listen(8080);
```

#### 编写HTTP/2帧 {#Writing_HTTP_2_frames}
HTTP/2是带有`HTTP请求/响应模型`的各种框架的框架协议。 该协议允许发送和接收其他类型的帧。

要发送这样的帧，可以在响应中使用`writeCustomFrame`。 这是一个例子：

```java
int frameType = 40;
int frameStatus = 10;
Buffer payload = Buffer.buffer("some data");

// Sending a frame to the client
response.writeCustomFrame(frameType, frameStatus, payload);
```

这些帧将立即发送，并且不受流控制-当发送此类帧时，可以在其他{@literal DATA}帧之前完成。

#### 流重置 {#Stream_reset}
HTTP/1.x不允许对请求或响应流进行干净的重置，例如，当客户端上传服务器上已经存在的资源时，服务器需要接受整个响应。

HTTP/2支持在请求/响应期间的任何时间进行流重置：

```java
request.response().reset();
```

默认情况下发送`NO_ERROR` (0)错误代码，可以发送另一个代码:

```java
request.response().reset(8);
```

HTTP / 2规范定义了可以使用的[错误代码](http://httpwg.org/specs/rfc7540.html#ErrorCodes)列表。

使用`request handler` 和 `response handler`将流重置事件通知给请求处理程序：

```java
request.response().exceptionHandler(err -> {
  if (err instanceof StreamResetException) {
    StreamResetException reset = (StreamResetException) err;
    System.out.println("Stream reset " + reset.getCode());
  }
});
```

#### 服务器推送 {#Server_push}
服务器推送是HTTP/2的一项新功能，它可以为单个客户端请求并行发送多个响应。

服务器处理请求时，可以将请求/响应推送到客户端：

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

当服务器准备好推送响应时，将调用推送响应处理程序，并且该处理程序可以发送响应。

推送响应处理程序可能会收到失败消息，例如，客户端可能会取消推送，因为它的缓存中已经有`main.js`，并且不再需要它。

必须在发起响应结束之前调用`push`方法，但是可以在之后写入被推送的响应。

#### 处理异常 {#Handling_exceptions}
您可以设置`exceptionHandler`来接收将连接传递给`requestHandler`或`websocketHandler`之前发生的任何异常，例如在TLS握手期间。

### HTTP 压缩 {#HTTP_Compression}
Vert.x开箱即用地支持HTTP压缩。

这意味着您可以在将响应正文发送回客户端之前自动对其进行压缩。

如果客户端不支持HTTP压缩，则将响应发送回而不压缩主体。

这样可以处理支持HTTP压缩的客户端和不支持HTTP压缩的客户端。

要启用压缩，可以使用`setCompressionSupported`配置它。

默认情况下不启用压缩。

启用HTTP压缩后，服务器将检查客户端是否包含包含支持的压缩的`Accept-Encoding`标头。 常用的是deflate和gzip。 两者均受Vert.x支持。

如果找到这样的标头，则服务器将使用支持的压缩之一自动压缩响应的主体，并将其发送回客户端。

每当需要不加压缩就发送响应时，都可以将标头`content-encoding`设置为`identity`：

```java
request.response()
  .putHeader(HttpHeaders.CONTENT_ENCODING, HttpHeaders.IDENTITY)
  .sendFile("/path/to/image.jpg");
```

请注意，压缩可能会减少网络流量，但会占用更多CPU资源。

为了解决后一个问题，Vert.x允许您调整gzip/deflate压缩算法固有的“compression level”参数。

压缩级别允许根据结果数据的压缩率和压缩/解压缩操作的计算成本来配置gzip/deflate算法。

压缩级别是一个从'1'到'9'的整数值，其中'1'表示较低的压缩率，但算法最快，而'9'表示可用的最大压缩率，但算法较慢。

使用高于1-2的压缩级别通常只能节省一些字节的大小-增益不是线性的，并且取决于要压缩的特定数据-但是对于CPU所需的CPU周期而言，它占用了不可交易的成本。 服务器在生成压缩响应数据时（请注意，目前Vert.x不支持压缩响应数据的任何形式的缓存，即使对于静态文件也是如此，因此压缩是在每个请求正文生成时即时完成的） 与在解码（扩大）接收到的响应时影响客户端的方式相同，级别越高，操作就越占用CPU资源。

默认情况下-如果通过`setCompressionSupported`启用了压缩-Vert.x将使用'6'作为压缩级别，但是该参数可以配置为使用`setCompressionLevel`处理任何情况。

### 创建一个HTTP客户端 {#Creating_an_HTTP_client}
您可以使用以下默认选项创建一个`HttpClient`实例：

```java
HttpClient client = vertx.createHttpClient();
```

如果要为客户端配置选项，请按以下方式创建它：

```java
HttpClientOptions options = new HttpClientOptions().setKeepAlive(false);
HttpClient client = vertx.createHttpClient(options);
```

Vert.x通过TLS `h2` 和TCP `h2c`支持HTTP/2。

默认情况下，http客户端执行HTTP/1.1请求，要执行HTTP/2请求，必须将`setProtocolVersion`设置为`HTTP_2`。

对于`h2`请求，必须通过 *应用层协议协商* 启用TLS：

```java
HttpClientOptions options = new HttpClientOptions().
    setProtocolVersion(HttpVersion.HTTP_2).
    setSsl(true).
    setUseAlpn(true).
    setTrustAll(true);

HttpClient client = vertx.createHttpClient(options);
```

For `h2c` requests, TLS must be disabled, the client will do an HTTP/1.1 requests and try an upgrade to HTTP/2:

```java
HttpClientOptions options = new HttpClientOptions().setProtocolVersion(HttpVersion.HTTP_2);

HttpClient client = vertx.createHttpClient(options);
```

也可以直接建立`h2c`连接，即在`setHttp2ClearTextUpgrade`选项设置为false时开始连接:建立连接后，客户端将发送HTTP/2连接序言，并期望从服务器接收相同的序言。

http服务器可能不支持HTTP/2，当响应到达时，可以使用`version`检查实际版本。

当客户端连接到HTTP/2服务器时，它将“初始设置”发送到服务器。 这些设置定义服务器如何使用连接，客户端的默认初始设置是HTTP/2 RFC定义的默认值。

### 记录网络客户端活动 {#Logging_network_client_activity}
出于调试目的，可以记录网络活动。

```java
HttpClientOptions options = new HttpClientOptions().setLogActivity(true);
HttpClient client = vertx.createHttpClient(options);
```

有关详细说明，请参见[记录网络活动](https://vertx.io/docs/vertx-core/java/#logging_network_activity) 一章。

### 发出请求 {#Making_requests}
http客户端非常灵活，可以通过多种方式发出请求。

通常，您想通过http客户端向同一主机/端口发出许多请求。 为避免每次发出请求时都重复主机/端口，可以为客户端配置默认主机/端口：

```java
HttpClientOptions options = new HttpClientOptions().setDefaultHost("wibble.com");
// Can also set default port if you want...
HttpClient client = vertx.createHttpClient(options);
client.getNow("/some-uri", response -> {
  System.out.println("Received response with status code " + response.statusCode());
});
```

另外，如果您发现使用同一客户端向不同的主机/端口发出大量请求，则只需在执行请求时指定主机/端口即可。

```java
HttpClient client = vertx.createHttpClient();

// Specify both port and host name
client.getNow(8080, "myserver.mycompany.com", "/some-uri", response -> {
  System.out.println("Received response with status code " + response.statusCode());
});

// This time use the default port 80 but specify the host name
client.getNow("foo.othercompany.com", "/other-uri", response -> {
  System.out.println("Received response with status code " + response.statusCode());
});
```

与客户端发出请求的所有不同方式都支持两种指定主机/端口的方法。

#### 没有请求正文的简单请求 {#Simple_requests_with_no_request_body}
通常，您会希望在没有请求正文的情况下发出HTTP请求。 HTTP GET，OPTIONS和HEAD请求通常是这种情况。

使用Vert.x http客户端执行此操作的最简单方法是使用`Now`后缀的方法。 例如`getNow`。

这些方法创建http请求并将其通过单个方法调用发送，并允许您提供一个处理程序，该处理程序将在返回时与http响应一起调用。

```java
HttpClient client = vertx.createHttpClient();

// Send a GET request
client.getNow("/some-uri", response -> {
  System.out.println("Received response with status code " + response.statusCode());
});

// Send a GET request
client.headNow("/other-uri", response -> {
  System.out.println("Received response with status code " + response.statusCode());
});
```

#### 编写一般请求 {#Writing_general_requests}
在其他时间，直到运行时您才知道要发送的请求方法。 对于这种用例，我们提供了通用的请求方法，例如`request`，它允许您在运行时指定HTTP方法：

```java
HttpClient client = vertx.createHttpClient();

client.request(HttpMethod.GET, "some-uri", response -> {
  System.out.println("Received response with status code " + response.statusCode());
}).end();

client.request(HttpMethod.POST, "foo-uri", response -> {
  System.out.println("Received response with status code " + response.statusCode());
}).end("some-data");
```

#### 编写请求主体 {#Writing_request_bodies}
有时，您可能希望写入具有正文的请求，或者在发送请求之前希望写入请求头。

为此，您可以调用一种特定的请求方法（例如`post`）或一种通用请求方法（例如`request`）。

这些方法不会立即发送请求，而是返回HttpClientRequest的实例，该实例可用于写入请求正文或写入标头。

以下是使用正文编写POST请求的一些示例：

```java
HttpClient client = vertx.createHttpClient();

HttpClientRequest request = client.post("some-uri", response -> {
  System.out.println("Received response with status code " + response.statusCode());
});

// Now do stuff with the request
request.putHeader("content-length", "1000");
request.putHeader("content-type", "text/plain");
request.write(body);

// Make sure the request is ended when you're done with it
request.end();

// Or fluently:

client.post("some-uri", response -> {
  System.out.println("Received response with status code " + response.statusCode());
}).putHeader("content-length", "1000").putHeader("content-type", "text/plain").write(body).end();

// Or event more simply:

client.post("some-uri", response -> {
  System.out.println("Received response with status code " + response.statusCode());
}).putHeader("content-type", "text/plain").end(body);
```

存在使用UTF-8编码和任何特定编码写入字符串以及写入缓冲区的方法：

```java
request.write("some data");

// Write string encoded in specific encoding
request.write("some other data", "UTF-16");

// Write a buffer
Buffer buffer = Buffer.buffer();
buffer.appendInt(123).appendLong(245l);
request.write(buffer);
```

如果您只是向HTTP请求写入单个字符串或缓冲区，则可以编写该字符串或缓冲区，并在一次对`end`函数的调用中结束该请求。

```java
request.end("some simple data");

// Write buffer and end the request (send it) in a single call
Buffer buffer = Buffer.buffer().appendDouble(12.34d).appendLong(432l);
request.end(buffer);
```

当您写入请求时，第一次调用`write`会导致请求标头被写到线路中。

实际的写入是异步的，并且可能要等到调用返回后的一段时间才能发生。

带有请求体的非分块HTTP请求需要提供一个`Content-Length`报头。

因此，如果您不使用chunked HTTP，那么您必须在写入请求之前设置`Content-Length`报头，否则就太晚了。

如果您正在调用采用字符串或缓冲区的`end`方法之一，则Vert.x将在写入请求主体之前自动计算并设置`Content-Length`标头。

如果您使用HTTP分块，则不需要`Content-Length`标头，因此不必预先计算大小。

#### 编写请求标头 {#Writing_request_headers}
您可以使用`headers` multi-map将标头写入请求，如下所示：

```java
MultiMap headers = request.headers();
headers.set("content-type", "application/json").set("other-header", "foo");
```

标题是`MultiMap`的实例，它提供添加，设置和删除条目的操作。 Http标头为特定键允许多个值。

您也可以使用`putHeader`来写标题

```java
request.putHeader("content-type", "application/json").putHeader("other-header", "foo");
```

如果您希望将标头写入请求，则必须在写入请求正文的任何部分之前这样做。

#### 非标准HTTP方法 {#Non_standard_HTTP_methods}
HTTP方法的`OTHER`用于非标准方法，使用此方法时，必须使用`setRawMethod`来设置要发送到服务器的原始方法。

#### 结束HTTP请求 {#Ending_HTTP_requests}
完成HTTP请求后，必须以`end`操作之一结束它。

结束请求将导致写入所有标头（如果尚未写入标头）并将请求标记为已完成。

请求可以通过几种方式结束。 没有参数，请求就简单地结束了：

```java
request.end();
```

或者可以在对`end`的调用中提供字符串或缓冲区。 这就像在调用不带参数的`end`之前用字符串或缓冲区调用`write`一样

```java
request.end("some-data");

// End it with a buffer
Buffer buffer = Buffer.buffer().appendFloat(12.3f).appendInt(321);
request.end(buffer);
```

#### 分块的HTTP请求 {#Chunked_HTTP_requests}
Vert.x支持[HTTP块传输编码](https://en.wikipedia.org/wiki/Chunked_transfer_encoding) 。

这允许将HTTP请求主体分块编写，并且通常在将大型请求主体流式传输到服务器（事先不知道其大小）时使用。

您可以使用`setChunked`将HTTP请求置于分块模式。

在分块模式下，每次写调用都将导致将一个新的分块写入线路。 在分块模式下，无需预先设置请求的`Content-Length`。

```java
request.setChunked(true);

// Write some chunks
for (int i = 0; i < 10; i++) {
  request.write("this-is-chunk-" + i);
}

request.end();
```

#### 请求超时 {#Request_timeouts}
您可以使用`setTimeout`为特定的HTTP请求设置超时。

如果请求在超时时间内未返回任何数据，则将异常传递给异常处理程序（如果提供），并且该请求将被关闭。

#### 处理异常 {#Handling_exceptions}
您可以通过在`HttpClientRequest`实例上设置异常处理程序来处理与请求相对应的异常：

```java
HttpClientRequest request = client.post("some-uri", response -> {
  System.out.println("Received response with status code " + response.statusCode());
});
request.exceptionHandler(e -> {
  System.out.println("Received exception: " + e.getMessage());
  e.printStackTrace();
});
```

这不会处理需要在`HttpClientResponse`代码中处理的非*2xx*响应：

```java
HttpClientRequest request = client.post("some-uri", response -> {
  if (response.statusCode() == 200) {
    System.out.println("Everything fine");
    return;
  }
  if (response.statusCode() == 500) {
    System.out.println("Unexpected behavior on the server side");
    return;
  }
});
request.end();
```

------
> **重要:** `XXXNow`方法不能接收异常处理程序。
------

#### 在客户端请求上指定处理程序 {#Specifying_a_handler_on_the_client_request}
或者，除了在创建客户端请求对象的调用中提供响应处理程序之外，还不能在创建请求时提供处理程序，以后再使用`handler`在请求对象本身上进行设置，例如：

```java
HttpClientRequest request = client.post("some-uri");
request.handler(response -> {
  System.out.println("Received response with status code " + response.statusCode());
});
```

#### 将请求用作流 {#Using_the_request_as_a_stream}
`HttpClientRequest`实例也是一个`WriteStream`，这意味着您可以从任何`ReadStream`实例中将其泵入。

例如，可以将磁盘上的文件泵送到http请求正文，如下所示：

```java
request.setChunked(true);
Pump pump = Pump.pump(file, request);
file.endHandler(v -> request.end());
pump.start();
```

#### 编写HTTP/2帧 {#Writing_HTTP_2_frames}
HTTP/2是一个框架协议，具有用于HTTP请求/响应模型的各种框架。该协议允许发送和接收其他类型的帧。

要发送这样的帧，您可以对请求使用`write`。这里有一个例子:

```java
int frameType = 40;
int frameStatus = 10;
Buffer payload = Buffer.buffer("some data");

// Sending a frame to the server
request.writeCustomFrame(frameType, frameStatus, payload);
```

#### 流重置 {#Stream_reset}
HTTP/1.x不允许对请求或响应流进行干净的重置，例如，当客户机上传服务器上已经存在的资源时，服务器需要接受整个响应。

HTTP/2支持在请求/响应期间的任何时间进行流重置：

```java
request.reset();
```

默认情况下NO_ERROR(0)错误代码被发送，另一个代码可以被发送:

```java
request.reset(8);
```

HTTP/2规范定义了可以使用的[错误代码](http://httpwg.org/specs/rfc7540.html#ErrorCodes)列表。

使用`request handler`和`response handler`将流重置事件通知给请求处理程序：

```java
request.exceptionHandler(err -> {
  if (err instanceof StreamResetException) {
    StreamResetException reset = (StreamResetException) err;
    System.out.println("Stream reset " + reset.getCode());
  }
});
```

### 处理HTTP响应 {#Handling_HTTP_responses}
您会在请求方法中指定的处理程序中收到`HttpClientResponse`的实例，或者直接在`HttpClientRequest`对象上设置处理程序。

您可以使用`statusCode`和`statusMessage`查询响应的状态码和状态消息。

```java
client.getNow("some-uri", response -> {
  // the status code - e.g. 200 or 404
  System.out.println("Status code is " + response.statusCode());

  // the status message e.g. "OK" or "Not Found".
  System.out.println("Status message is " + response.statusMessage());
});
```

#### Using the response as a stream {#Using_the_response_as_a_stream}
The `HttpClientResponse` instance is also a `ReadStream` which means you can pump it to any `WriteStream` instance.

#### Response headers and trailers {#Response_headers_and_trailers}
Http responses can contain headers. Use `headers` to get the headers.

The object returned is a `MultiMap` as HTTP headers can contain multiple values for single keys.

```java
String contentType = response.headers().get("content-type");
String contentLength = response.headers().get("content-lengh");
```

Chunked HTTP responses can also contain trailers - these are sent in the last chunk of the response body.

You use `trailers` to get the trailers. Trailers are also a `MultiMap`.

#### Reading the request body {#Reading_the_request_body}
The response handler is called when the headers of the response have been read from the wire.

If the response has a body this might arrive in several pieces some time after the headers have been read. We don’t wait for all the body to arrive before calling the response handler as the response could be very large and we might be waiting a long time, or run out of memory for large responses.

As parts of the response body arrive, the `handler` is called with a `Buffer` representing the piece of the body:

```java
client.getNow("some-uri", response -> {

  response.handler(buffer -> {
    System.out.println("Received a part of the response body: " + buffer);
  });
});
```

If you know the response body is not very large and want to aggregate it all in memory before handling it, you can either aggregate it yourself:

```java
client.getNow("some-uri", response -> {

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
});
```

Or you can use the convenience `bodyHandler` which is called with the entire body when the response has been fully read:

```java
client.getNow("some-uri", response -> {

  response.bodyHandler(totalBuffer -> {
    // Now all the body has been read
    System.out.println("Total response body length is " + totalBuffer.length());
  });
});
```

#### Response end handler {#Response_end_handler}
The response `endHandler` is called when the entire response body has been read or immediately after the headers have been read and the response handler has been called if there is no body.

#### Reading cookies from the response {#Reading_cookies_from_the_response}
You can retrieve the list of cookies from a response using `cookies`.

Alternatively you can just parse the `Set-Cookie` headers yourself in the response.

#### 30x redirection handling {#x_redirection_handling}
The client can be configured to follow HTTP redirections provided by the `Location` response header when the client receives:

- a `301`, `302`, `307` or `308` status code along with a HTTP GET or HEAD method
- a `303` status code, in addition the directed request perform an HTTP GET methodn

Here’s an example:

```java
client.get("some-uri", response -> {
  System.out.println("Received response with status code " + response.statusCode());
}).setFollowRedirects(true).end();
```

The maximum redirects is `16` by default and can be changed with `setMaxRedirects`.

```java
HttpClient client = vertx.createHttpClient(
    new HttpClientOptions()
        .setMaxRedirects(32));

client.get("some-uri", response -> {
  System.out.println("Received response with status code " + response.statusCode());
}).setFollowRedirects(true).end();
```

One size does not fit all and the default redirection policy may not be adapted to your needs.

The default redirection policy can changed with a custom implementation:

```java
client.redirectHandler(response -> {

  // Only follow 301 code
  if (response.statusCode() == 301 && response.getHeader("Location") != null) {

    // Compute the redirect URI
    String absoluteURI = resolveURI(response.request().absoluteURI(), response.getHeader("Location"));

    // Create a new ready to use request that the client will use
    return Future.succeededFuture(client.getAbs(absoluteURI));
  }

  // We don't redirect
  return null;
});
```

The policy handles the original `HttpClientResponse` received and returns either `null` or a `Future`.

- when `null` is returned, the original response is processed
- when a future is returned, the request will be sent on its successful completion
- when a future is returned, the exception handler set on the request is called on its failure

The returned request must be unsent so the original request handlers can be sent and the client can send it after.

Most of the original request settings will be propagated to the new request:

- request headers, unless if you have set some headers (including `setHost`)
- request body unless the returned request uses a `GET` method
- response handler
- request exception handler
- request timeout

#### 100-Continue handling {#Continue_handling}
According to the [HTTP 1.1 specification](https://www.w3.org/Protocols/rfc2616/rfc2616-sec8.html) a client can set a header `Expect: 100-Continue` and send the request header before sending the rest of the request body.

The server can then respond with an interim response status `Status: 100 (Continue)` to signify to the client that it is ok to send the rest of the body.

The idea here is it allows the server to authorise and accept/reject the request before large amounts of data are sent. Sending large amounts of data if the request might not be accepted is a waste of bandwidth and ties up the server in reading data that it will just discard.

Vert.x allows you to set a `continueHandler` on the client request object

This will be called if the server sends back a `Status: 100 (Continue)` response to signify that it is ok to send the rest of the request.

This is used in conjunction with `[sendHead](https://vertx.io/docs/apidocs/io/vertx/core/http/HttpClientRequest.html#sendHead--)`to send the head of the request.

Here’s an example:

```java
HttpClientRequest request = client.put("some-uri", response -> {
  System.out.println("Received response with status code " + response.statusCode());
});

request.putHeader("Expect", "100-Continue");

request.continueHandler(v -> {
  // OK to send rest of body
  request.write("Some data");
  request.write("Some more data");
  request.end();
});
```

On the server side a Vert.x http server can be configured to automatically send back 100 Continue interim responses when it receives an `Expect: 100-Continue` header.

This is done by setting the option `setHandle100ContinueAutomatically`.

If you’d prefer to decide whether to send back continue responses manually, then this property should be set to `false` (the default), then you can inspect the headers and call `writeContinue` to have the client continue sending the body:

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

You can also reject the request by sending back a failure status code directly: in this case the body should either be ignored or the connection should be closed (100-Continue is a performance hint and cannot be a logical protocol constraint):

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

#### Client push {#Client_push}
Server push is a new feature of HTTP/2 that enables sending multiple responses in parallel for a single client request.

A push handler can be set on a request to receive the request/response pushed by the server:

```java
HttpClientRequest request = client.get("/index.html", response -> {
  // Process index.html response
});

// Set a push handler to be aware of any resource pushed by the server
request.pushHandler(pushedRequest -> {

  // A resource is pushed for this request
  System.out.println("Server pushed " + pushedRequest.path());

  // Set an handler for the response
  pushedRequest.handler(pushedResponse -> {
    System.out.println("The response for the pushed request");
  });
});

// End the request
request.end();
```

If the client does not want to receive a pushed request, it can reset the stream:

```java
request.pushHandler(pushedRequest -> {
  if (pushedRequest.path().equals("/main.js")) {
    pushedRequest.reset();
  } else {
    // Handle it
  }
});
```

When no handler is set, any stream pushed will be automatically cancelled by the client with a stream reset (`8` error code).

#### Receiving custom HTTP/2 frames {#Receiving_custom_HTTP_2_frames}
HTTP/2 is a framed protocol with various frames for the HTTP request/response model. The protocol allows other kind of frames to be sent and received.

To receive custom frames, you can use the customFrameHandler on the request, this will get called every time a custom frame arrives. Here’s an example:

```java
response.customFrameHandler(frame -> {

  System.out.println("Received a frame type=" + frame.type() +
      " payload" + frame.payload().toString());
});
```

### Enabling compression on the client {#Enabling_compression_on_the_client}
The http client comes with support for HTTP Compression out of the box.

This means the client can let the remote http server know that it supports compression, and will be able to handle compressed response bodies.

An http server is free to either compress with one of the supported compression algorithms or to send the body back without compressing it at all. So this is only a hint for the Http server which it may ignore at will.

To tell the http server which compression is supported by the client it will include an `Accept-Encoding` header with the supported compression algorithm as value. Multiple compression algorithms are supported. In case of Vert.x this will result in the following header added:

Accept-Encoding: gzip, deflate

The server will choose then from one of these. You can detect if a server ompressed the body by checking for the `Content-Encoding` header in the response sent back from it.

If the body of the response was compressed via gzip it will include for example the following header:

Content-Encoding: gzip

To enable compression set `setTryUseCompression` on the options used when creating the client.

By default compression is disabled.

### HTTP/1.x pooling and keep alive {#HTTP_1_x_pooling_and_keep_alive}
Http keep alive allows http connections to be used for more than one request. This can be a more efficient use of connections when you’re making multiple requests to the same server.

For HTTP/1.x versions, the http client supports pooling of connections, allowing you to reuse connections between requests.

For pooling to work, keep alive must be true using `setKeepAlive` on the options used when configuring the client. The default value is true.

When keep alive is enabled. Vert.x will add a `Connection: Keep-Alive` header to each HTTP/1.0 request sent. When keep alive is disabled. Vert.x will add a `Connection: Close` header to each HTTP/1.1 request sent to signal that the connection will be closed after completion of the response.

The maximum number of connections to pool **for each server** is configured using `setMaxPoolSize`

When making a request with pooling enabled, Vert.x will create a new connection if there are less than the maximum number of connections already created for that server, otherwise it will add the request to a queue.

Keep alive connections will be closed by the client automatically after a timeout. The timeout can be specified by the server using the `keep-alive` header:

```
keep-alive: timeout=30
```

You can set the default timeout using `setKeepAliveTimeout` - any connections not used within this timeout will be closed. Please note the timeout value is in seconds not milliseconds.

### HTTP/1.1 pipe-lining {#HTTP_1_1_pipe_lining}
The client also supports pipe-lining of requests on a connection.

Pipe-lining means another request is sent on the same connection before the response from the preceding one has returned. Pipe-lining is not appropriate for all requests.

To enable pipe-lining, it must be enabled using `setPipelining`. By default pipe-lining is disabled.

When pipe-lining is enabled requests will be written to connections without waiting for previous responses to return.

The number of pipe-lined requests over a single connection is limited by `setPipeliningLimit`. This option defines the maximum number of http requests sent to the server awaiting for a response. This limit ensures the fairness of the distribution of the client requests over the connections to the same server.

### HTTP/2 multiplexing {#HTTP_2_multiplexing}
HTTP/2 advocates to use a single connection to a server, by default the http client uses a single connection for each server, all the streams to the same server are multiplexed over the same connection.

When the clients needs to use more than a single connection and use pooling, the `setHttp2MaxPoolSize` shall be used.

When it is desirable to limit the number of multiplexed streams per connection and use a connection pool instead of a single connection, `setHttp2MultiplexingLimit` can be used.

```
HttpClientOptions clientOptions = new HttpClientOptions().
    setHttp2MultiplexingLimit(10).
    setHttp2MaxPoolSize(3);

// Uses up to 3 connections and up to 10 streams per connection
HttpClient client = vertx.createHttpClient(clientOptions);
```

The multiplexing limit for a connection is a setting set on the client that limits the number of streams of a single connection. The effective value can be even lower if the server sets a lower limit with the `SETTINGS_MAX_CONCURRENT_STREAMS` setting.

HTTP/2 connections will not be closed by the client automatically. To close them you can call `close` or close the client instance.

Alternatively you can set idle timeout using `setIdleTimeout` - any connections not used within this timeout will be closed. Please note the idle timeout value is in seconds not milliseconds.

### HTTP connections {#HTTP_connections}
The `HttpConnection` offers the API for dealing with HTTP connection events, lifecycle and settings.

HTTP/2 implements fully the `HttpConnection` API.

HTTP/1.x implements partially the `HttpConnection` API: only the close operation, the close handler and exception handler are implemented. This protocol does not provide semantics for the other operations.

#### Server connections {#Server_connections}
The `connection` method returns the request connection on the server:

```
HttpConnection connection = request.connection();
```

A connection handler can be set on the server to be notified of any incoming connection:

```
HttpServer server = vertx.createHttpServer(http2Options);

server.connectionHandler(connection -> {
  System.out.println("A client connected");
});
```

#### Client connections {#Client_connections}
The `connection` method returns the request connection on the client:

```
HttpConnection connection = request.connection();
```

A connection handler can be set on the request to be notified when the connection happens:

```
request.connectionHandler(connection -> {
  System.out.println("Connected to the server");
});
```

#### Connection settings {#Connection_settings}
The configuration of an HTTP/2 is configured by the `Http2Settings` data object.

Each endpoint must respect the settings sent by the other side of the connection.

When a connection is established, the client and the server exchange initial settings. Initial settings are configured by `setInitialSettings` on the client and `setInitialSettings` on the server.

The settings can be changed at any time after the connection is established:

```
connection.updateSettings(new Http2Settings().setMaxConcurrentStreams(100));
```

As the remote side should acknowledge on reception of the settings update, it’s possible to give a callback to be notified of the acknowledgment:

```
connection.updateSettings(new Http2Settings().setMaxConcurrentStreams(100), ar -> {
  if (ar.succeeded()) {
    System.out.println("The settings update has been acknowledged ");
  }
});
```

Conversely the `remoteSettingsHandler` is notified when the new remote settings are received:

```
connection.remoteSettingsHandler(settings -> {
  System.out.println("Received new settings");
});
```

| NOTE | this only applies to the HTTP/2 protocol |
| ---- | ---------------------------------------- |
|      |                                          |

#### Connection ping {#Connection_ping}
HTTP/2 connection ping is useful for determining the connection round-trip time or check the connection validity: `ping` sends a {@literal PING} frame to the remote endpoint:

```
Buffer data = Buffer.buffer();
for (byte i = 0;i < 8;i++) {
  data.appendByte(i);
}
connection.ping(data, pong -> {
  System.out.println("Remote side replied");
});
```

Vert.x will send automatically an acknowledgement when a {@literal PING} frame is received, an handler can be set to be notified for each ping received:

```
connection.pingHandler(ping -> {
  System.out.println("Got pinged by remote side");
});
```

The handler is just notified, the acknowledgement is sent whatsoever. Such feature is aimed for implementing protocols on top of HTTP/2.

| NOTE | this only applies to the HTTP/2 protocol |
| ---- | ---------------------------------------- |
|      |                                          |

#### Connection shutdown and go away {#Connection_shutdown_and_go_away}
Calling `shutdown` will send a {@literal GOAWAY} frame to the remote side of the connection, asking it to stop creating streams: a client will stop doing new requests and a server will stop pushing responses. After the {@literal GOAWAY} frame is sent, the connection waits some time (30 seconds by default) until all current streams closed and close the connection:

```
connection.shutdown();
```

The `shutdownHandler` notifies when all streams have been closed, the connection is not yet closed.

It’s possible to just send a {@literal GOAWAY} frame, the main difference with a shutdown is that it will just tell the remote side of the connection to stop creating new streams without scheduling a connection close:

```
connection.goAway(0);
```

Conversely, it is also possible to be notified when {@literal GOAWAY} are received:

```
connection.goAwayHandler(goAway -> {
  System.out.println("Received a go away frame");
});
```

The `shutdownHandler` will be called when all current streams have been closed and the connection can be closed:

```
connection.goAway(0);
connection.shutdownHandler(v -> {

  // All streams are closed, close the connection
  connection.close();
});
```

This applies also when a {@literal GOAWAY} is received.

| NOTE | this only applies to the HTTP/2 protocol |
| ---- | ---------------------------------------- |
|      |                                          |

#### Connection close {#Connection_close}
Connection `close` closes the connection:

- it closes the socket for HTTP/1.x
- a shutdown with no delay for HTTP/2, the {@literal GOAWAY} frame will still be sent before the connection is closed. *

The `closeHandler` notifies when a connection is closed.

### HttpClient usage {#HttpClient_usage}
The HttpClient can be used in a Verticle or embedded.

When used in a Verticle, the Verticle **should use its own client instance**.

More generally a client should not be shared between different Vert.x contexts as it can lead to unexpected behavior.

For example a keep-alive connection will call the client handlers on the context of the request that opened the connection, subsequent requests will use the same context.

When this happen Vert.x detects it and log a warn:

```
Reusing a connection with a different context: an HttpClient is probably shared between different Verticles
```

The HttpClient can be embedded in a non Vert.x thread like a unit test or a plain java `main`: the client handlers will be called by different Vert.x threads and contexts, such contexts are created as needed. For production this usage is not recommended.

### Server sharing {#Server_sharing}
When several HTTP servers listen on the same port, vert.x orchestrates the request handling using a round-robin strategy.

Let’s take a verticle creating a HTTP server such as:

io.vertx.examples.http.sharing.HttpServerVerticle

```
vertx.createHttpServer().requestHandler(request -> {
  request.response().end("Hello from server " + this);
}).listen(8080);
```

This service is listening on the port 8080. So, when this verticle is instantiated multiple times as with: `vertx run io.vertx.examples.http.sharing.HttpServerVerticle -instances 2`, what’s happening ? If both verticles would bind to the same port, you would receive a socket exception. Fortunately, vert.x is handling this case for you. When you deploy another server on the same host and port as an existing server it doesn’t actually try and create a new server listening on the same host/port. It binds only once to the socket. When receiving a request it calls the server handlers following a round robin strategy.

Let’s now imagine a client such as:

```
vertx.setPeriodic(100, (l) -> {
  vertx.createHttpClient().getNow(8080, "localhost", "/", resp -> {
    resp.bodyHandler(body -> {
      System.out.println(body.toString("ISO-8859-1"));
    });
  });
});
```

Vert.x delegates the requests to one of the server sequentially:

```
Hello from i.v.e.h.s.HttpServerVerticle@1
Hello from i.v.e.h.s.HttpServerVerticle@2
Hello from i.v.e.h.s.HttpServerVerticle@1
Hello from i.v.e.h.s.HttpServerVerticle@2
...
```

Consequently the servers can scale over available cores while each Vert.x verticle instance remains strictly single threaded, and you don’t have to do any special tricks like writing load-balancers in order to scale your server on your multi-core machine.

### Using HTTPS with Vert.x {#Using_HTTPS_with_Vert_x}
Vert.x http servers and clients can be configured to use HTTPS in exactly the same way as net servers.

Please see [configuring net servers to use SSL](https://vertx.io/docs/vertx-core/java/#ssl) for more information.

SSL can also be enabled/disabled per request with `RequestOptions` or when specifying a scheme with `requestAbs` method.

```
client.getNow(new RequestOptions()
    .setHost("localhost")
    .setPort(8080)
    .setURI("/")
    .setSsl(true), response -> {
  System.out.println("Received response with status code " + response.statusCode());
});
```

The `setSsl` setting acts as the default client setting.

The `setSsl` overrides the default client setting

- setting the value to `false` will disable SSL/TLS even if the client is configured to use SSL/TLS
- setting the value to `true` will enable SSL/TLS even if the client is configured to not use SSL/TLS, the actual client SSL/TLS (such as trust, key/certificate, ciphers, ALPN, …) will be reused

Likewise `requestAbs` scheme also overrides the default client setting.

#### Server Name Indication (SNI) {#Server_Name_Indication__SNI_}
Vert.x http servers can be configured to use SNI in exactly the same way as {@linkplain io.vertx.core.net net servers}.

Vert.x http client will present the actual hostname as *server name* during the TLS handshake.

### WebSockets {#WebSockets}
[WebSockets](https://en.wikipedia.org/wiki/WebSocket) are a web technology that allows a full duplex socket-like connection between HTTP servers and HTTP clients (typically browsers).

Vert.x supports WebSockets on both the client and server-side.

#### WebSockets on the server {#WebSockets_on_the_server}
There are two ways of handling WebSockets on the server side.

##### WebSocket handler {#WebSocket_handler}
The first way involves providing a `websocketHandler` on the server instance.

When a WebSocket connection is made to the server, the handler will be called, passing in an instance of `ServerWebSocket`.

```
server.websocketHandler(websocket -> {
  System.out.println("Connected!");
});
```

You can choose to reject the WebSocket by calling `reject`.

```
server.websocketHandler(websocket -> {
  if (websocket.path().equals("/myapi")) {
    websocket.reject();
  } else {
    // Do something
  }
});
```

You can perform an asynchronous handshake by calling `setHandshake` with a `Future`:

```
server.websocketHandler(websocket -> {
  Promise<Integer> promise = Promise.promise();
  websocket.setHandshake(promise.future());
  authenticate(websocket.headers(), ar -> {
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

| NOTE | the WebSocket will be automatically accepted after the handler is called unless the WebSocket’s handshake has been set |
| ---- | ------------------------------------------------------------ |
|      |                                                              |

##### Upgrading to WebSocket {#Upgrading_to_WebSocket}
The second way of handling WebSockets is to handle the HTTP Upgrade request that was sent from the client, and call `upgrade` on the server request.

```
server.requestHandler(request -> {
  if (request.path().equals("/myapi")) {

    ServerWebSocket websocket = request.upgrade();
    // Do something

  } else {
    // Reject
    request.response().setStatusCode(400).end();
  }
});
```

##### The server WebSocket {#The_server_WebSocket}
The `ServerWebSocket` instance enables you to retrieve the `headers`, `path`, `query` and `URI` of the HTTP request of the WebSocket handshake.

#### WebSockets on the client {#WebSockets_on_the_client}
The Vert.x `HttpClient` supports WebSockets.

You can connect a WebSocket to a server using one of the `webSocket` operations and providing a handler.

The handler will be called with an instance of `WebSocket` when the connection has been made:

```
client.webSocket("/some-uri", res -> {
  if (res.succeeded()) {
    WebSocket ws = res.result();
    System.out.println("Connected!");
  }
});
```

#### Writing messages to WebSockets {#Writing_messages_to_WebSockets}
If you wish to write a single WebSocket message to the WebSocket you can do this with `writeBinaryMessage` or `writeTextMessage` :

```
Buffer buffer = Buffer.buffer().appendInt(123).appendFloat(1.23f);
websocket.writeBinaryMessage(buffer);

// Write a simple text message
String message = "hello";
websocket.writeTextMessage(message);
```

If the WebSocket message is larger than the maximum websocket frame size as configured with `setMaxWebsocketFrameSize` then Vert.x will split it into multiple WebSocket frames before sending it on the wire.

#### Writing frames to WebSockets {#Writing_frames_to_WebSockets}
A WebSocket message can be composed of multiple frames. In this case the first frame is either a *binary* or *text* frame followed by zero or more *continuation* frames.

The last frame in the message is marked as *final*.

To send a message consisting of multiple frames you create frames using `WebSocketFrame.binaryFrame` , `WebSocketFrame.textFrame` or `WebSocketFrame.continuationFrame` and write them to the WebSocket using `writeFrame`.

Here’s an example for binary frames:

```
WebSocketFrame frame1 = WebSocketFrame.binaryFrame(buffer1, false);
websocket.writeFrame(frame1);

WebSocketFrame frame2 = WebSocketFrame.continuationFrame(buffer2, false);
websocket.writeFrame(frame2);

// Write the final frame
WebSocketFrame frame3 = WebSocketFrame.continuationFrame(buffer2, true);
websocket.writeFrame(frame3);
```

In many cases you just want to send a websocket message that consists of a single final frame, so we provide a couple of shortcut methods to do that with `writeFinalBinaryFrame` and `writeFinalTextFrame`.

Here’s an example:

```
websocket.writeFinalTextFrame("Geronimo!");

// Send a websocket messages consisting of a single final binary frame:

Buffer buff = Buffer.buffer().appendInt(12).appendString("foo");

websocket.writeFinalBinaryFrame(buff);
```

#### Reading frames from WebSockets {#Reading_frames_from_WebSockets}
To read frames from a WebSocket you use the `frameHandler`.

The frame handler will be called with instances of `WebSocketFrame` when a frame arrives, for example:

```
websocket.frameHandler(frame -> {
  System.out.println("Received a frame of size!");
});
```

#### Closing WebSockets {#Closing_WebSockets}
Use `close` to close the WebSocket connection when you have finished with it.

#### Streaming WebSockets {#Streaming_WebSockets}
The `WebSocket` instance is also a `ReadStream` and a `WriteStream` so it can be used with pumps.

When using a WebSocket as a write stream or a read stream it can only be used with WebSockets connections that are used with binary frames that are no split over multiple frames.

### Using a proxy for HTTP/HTTPS connections {#Using_a_proxy_for_HTTP_HTTPS_connections}
The http client supports accessing http/https URLs via a HTTP proxy (e.g. Squid) or *SOCKS4a* or *SOCKS5* proxy. The CONNECT protocol uses HTTP/1.x but can connect to HTTP/1.x and HTTP/2 servers.

Connecting to h2c (unencrypted HTTP/2 servers) is likely not supported by http proxies since they will support HTTP/1.1 only.

The proxy can be configured in the `HttpClientOptions` by setting a `ProxyOptions` object containing proxy type, hostname, port and optionally username and password.

Here’s an example of using an HTTP proxy:

```
HttpClientOptions options = new HttpClientOptions()
    .setProxyOptions(new ProxyOptions().setType(ProxyType.HTTP)
        .setHost("localhost").setPort(3128)
        .setUsername("username").setPassword("secret"));
HttpClient client = vertx.createHttpClient(options);
```

When the client connects to an http URL, it connects to the proxy server and provides the full URL in the HTTP request ("GET http://www.somehost.com/path/file.html HTTP/1.1").

When the client connects to an https URL, it asks the proxy to create a tunnel to the remote host with the CONNECT method.

For a SOCKS5 proxy:

```
HttpClientOptions options = new HttpClientOptions()
    .setProxyOptions(new ProxyOptions().setType(ProxyType.SOCKS5)
        .setHost("localhost").setPort(1080)
        .setUsername("username").setPassword("secret"));
HttpClient client = vertx.createHttpClient(options);
```

The DNS resolution is always done on the proxy server, to achieve the functionality of a SOCKS4 client, it is necessary to resolve the DNS address locally.

#### Handling of other protocols {#Handling_of_other_protocols}
The HTTP proxy implementation supports getting ftp:// urls if the proxy supports that, which isn’t available in non-proxy getAbs requests.

```
HttpClientOptions options = new HttpClientOptions()
    .setProxyOptions(new ProxyOptions().setType(ProxyType.HTTP));
HttpClient client = vertx.createHttpClient(options);
client.getAbs("ftp://ftp.gnu.org/gnu/", response -> {
  System.out.println("Received response with status code " + response.statusCode());
});
```

Support for other protocols is not available since java.net.URL does not support them (gopher:// for example).

### Automatic clean-up in verticles {#Automatic_clean_up_in_verticles}
If you’re creating http servers and clients from inside verticles, those servers and clients will be automatically closed when the verticle is undeployed.

## Using the SharedData API {#Using_the_SharedData_API}
As its name suggests, the `SharedData` API allows you to safely share data between:

- different parts of your application, or
- different applications in the same Vert.x instance, or
- different applications across a cluster of Vert.x instances.

In practice, it provides:

- synchronous maps (local-only)
- asynchronous maps
- asynchronous locks
- asynchronous counters

| IMPORTANT | The behavior of the distributed data structure depends on the cluster manager you use. Backup (replication) and behavior when a network partition is faced are defined by the cluster manager and its configuration. Please refer to the cluster manager documentation as well as to the underlying framework manual. |
| --------- | ------------------------------------------------------------ |
|           |                                                              |

### Local maps {#Local_maps}
`Local maps` allow you to share data safely between different event loops (e.g. different verticles) in the same Vert.x instance.

They only allow certain data types to be used as keys and values:

- immutable types (e.g. strings, booleans, … etc), or
- types implementing the `Shareable` interface (buffers, JSON arrays, JSON objects, or your own shareable objects).

In the latter case the key/value will be copied before putting it into the map.

This way we can ensure there is no *shared access to mutable state* between different threads in your Vert.x application. And you won’t have to worry about protecting that state by synchronising access to it.

Here’s an example of using a shared local map:

```
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

### Asynchronous shared maps {#Asynchronous_shared_maps}
`Asynchronous shared maps` allow data to be put in the map and retrieved locally or from any other node.

This makes them really useful for things like storing session state in a farm of servers hosting a Vert.x Web application.

Getting the map is asynchronous and the result is returned to you in the handler that you specify. Here’s an example:

```
SharedData sharedData = vertx.sharedData();

sharedData.<String, String>getAsyncMap("mymap", res -> {
  if (res.succeeded()) {
    AsyncMap<String, String> map = res.result();
  } else {
    // Something went wrong!
  }
});
```

When Vert.x is clustered, data that you put into the map is accessible locally as well as on any of the other cluster members.

| IMPORTANT | In clustered mode, asynchronous shared maps rely on distributed data structures provided by the cluster manager. Beware that the latency relative to asynchronous shared map operations can be much higher in clustered than in local mode. |
| --------- | ------------------------------------------------------------ |
|           |                                                              |

If your application doesn’t need data to be shared with every other node, you can retrieve a local-only map:

```
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

#### Putting data in a map {#Putting_data_in_a_map}
You put data in a map with `put`.

The actual put is asynchronous and the handler is notified once it is complete:

```
map.put("foo", "bar", resPut -> {
  if (resPut.succeeded()) {
    // Successfully put the value
  } else {
    // Something went wrong!
  }
});
```

#### Getting data from a map {#Getting_data_from_a_map}
You get data from a map with `get`.

The actual get is asynchronous and the handler is notified with the result some time later:

```
map.get("foo", resGet -> {
  if (resGet.succeeded()) {
    // Successfully got the value
    Object val = resGet.result();
  } else {
    // Something went wrong!
  }
});
```

##### Other map operations {#Other_map_operations}
You can also remove entries from an asynchronous map, clear them and get the size.

See the `API docs` for a detailed list of map operations.

### Asynchronous locks {#Asynchronous_locks}
`Asynchronous locks` allow you to obtain exclusive locks locally or across the cluster. This is useful when you want to do something or access a resource on only one node of a cluster at any one time.

Asynchronous locks have an asynchronous API unlike most lock APIs which block the calling thread until the lock is obtained.

To obtain a lock use `getLock`. This won’t block, but when the lock is available, the handler will be called with an instance of `Lock`, signalling that you now own the lock.

While you own the lock, no other caller, locally or on the cluster, will be able to obtain the lock.

When you’ve finished with the lock, you call `release` to release it, so another caller can obtain it:

```
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

You can also get a lock with a timeout. If it fails to obtain the lock within the timeout the handler will be called with a failure:

```
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

See the `API docs` for a detailed list of lock operations.

| IMPORTANT | In clustered mode, asynchronous locks rely on distributed data structures provided by the cluster manager. Beware that the latency relative to asynchronous shared lock operations can be much higher in clustered than in local mode. |
| --------- | ------------------------------------------------------------ |
|           |                                                              |

If your application doesn’t need the lock to be shared with every other node, you can retrieve a local-only lock:

```
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

### Asynchronous counters {#Asynchronous_counters}
It’s often useful to maintain an atomic counter locally or across the different nodes of your application.

You can do this with `Counter`.

You obtain an instance with `getCounter`:

```
SharedData sharedData = vertx.sharedData();

sharedData.getCounter("mycounter", res -> {
  if (res.succeeded()) {
    Counter counter = res.result();
  } else {
    // Something went wrong!
  }
});
```

Once you have an instance you can retrieve the current count, atomically increment it, decrement and add a value to it using the various methods.

See the `API docs` for a detailed list of counter operations.

| IMPORTANT | In clustered mode, asynchronous counters rely on distributed data structures provided by the cluster manager. Beware that the latency relative to asynchronous shared counter operations can be much higher in clustered than in local mode. |
| --------- | ------------------------------------------------------------ |
|           |                                                              |

If your application doesn’t need the counter to be shared with every other node, you can retrieve a local-only counter:

```
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

## Using the file system with Vert.x {#Using_the_file_system_with_Vert_x}
The Vert.x `FileSystem` object provides many operations for manipulating the file system.

There is one file system object per Vert.x instance, and you obtain it with `fileSystem`.

A blocking and a non blocking version of each operation is provided. The non blocking versions take a handler which is called when the operation completes or an error occurs.

Here’s an example of an asynchronous copy of a file:

```
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

The blocking versions are named `xxxBlocking` and return the results or throw exceptions directly. In many cases, depending on the operating system and file system, some of the potentially blocking operations can return quickly, which is why we provide them, but it’s highly recommended that you test how long they take to return in your particular application before using them from an event loop, so as not to break the Golden Rule.

Here’s the copy using the blocking API:

```
FileSystem fs = vertx.fileSystem();

// Copy file from foo.txt to bar.txt synchronously
fs.copyBlocking("foo.txt", "bar.txt");
```

Many operations exist to copy, move, truncate, chmod and many other file operations. We won’t list them all here, please consult the `API docs` for the full list.

Let’s see a couple of examples using asynchronous methods:

```
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

### Asynchronous files {#Asynchronous_files}
Vert.x provides an asynchronous file abstraction that allows you to manipulate a file on the file system.

You open an `AsyncFile` as follows:

```
OpenOptions options = new OpenOptions();
fileSystem.open("myfile.txt", options, res -> {
  if (res.succeeded()) {
    AsyncFile file = res.result();
  } else {
    // Something went wrong!
  }
});
```

`AsyncFile` implements `ReadStream` and `WriteStream` so you can *pump* files to and from other stream objects such as net sockets, http requests and responses, and WebSockets.

They also allow you to read and write directly to them.

#### Random access writes {#Random_access_writes}
To use an `AsyncFile` for random access writing you use the `write` method.

The parameters to the method are:

- `buffer`: the buffer to write.
- `position`: an integer position in the file where to write the buffer. If the position is greater or equal to the size of the file, the file will be enlarged to accommodate the offset.
- `handler`: the result handler

Here is an example of random access writes:

```
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

#### Random access reads {#Random_access_reads}
To use an `AsyncFile` for random access reads you use the `read` method.

The parameters to the method are:

- `buffer`: the buffer into which the data will be read.
- `offset`: an integer offset into the buffer where the read data will be placed.
- `position`: the position in the file where to read data from.
- `length`: the number of bytes of data to read
- `handler`: the result handler

Here’s an example of random access reads:

```
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

#### Opening Options {#Opening_Options}
When opening an `AsyncFile`, you pass an `OpenOptions` instance. These options describe the behavior of the file access. For instance, you can configure the file permissions with the `setRead`, `setWrite` and `setPerms` methods.

You can also configure the behavior if the open file already exists with `setCreateNew` and `setTruncateExisting`.

You can also mark the file to be deleted on close or when the JVM is shutdown with `setDeleteOnClose`.

#### Flushing data to underlying storage. {#Flushing_data_to_underlying_storage_}
In the `OpenOptions`, you can enable/disable the automatic synchronisation of the content on every write using `setDsync`. In that case, you can manually flush any writes from the OS cache by calling the `flush` method.

This method can also be called with an handler which will be called when the flush is complete.

#### Using AsyncFile as ReadStream and WriteStream {#Using_AsyncFile_as_ReadStream_and_WriteStream}
`AsyncFile` implements `ReadStream` and `WriteStream`. You can then use them with a *pump* to pump data to and from other read and write streams. For example, this would copy the content to another `AsyncFile`:

```
final AsyncFile output = vertx.fileSystem().openBlocking("target/classes/plagiary.txt", new OpenOptions());

vertx.fileSystem().open("target/classes/les_miserables.txt", new OpenOptions(), result -> {
  if (result.succeeded()) {
    AsyncFile file = result.result();
    Pump.pump(file, output).start();
    file.endHandler((r) -> {
      System.out.println("Copy done");
    });
  } else {
    System.err.println("Cannot open file " + result.cause());
  }
});
```

You can also use the *pump* to write file content into HTTP responses, or more generally in any `WriteStream`.

#### Accessing files from the classpath {#Accessing_files_from_the_classpath}
When vert.x cannot find the file on the filesystem it tries to resolve the file from the class path. Note that classpath resource paths never start with a `/`.

Due to the fact that Java does not offer async access to classpath resources, the file is copied to the filesystem in a worker thread when the classpath resource is accessed the very first time and served from there asynchronously. When the same resource is accessed a second time, the file from the filesystem is served directly from the filesystem. The original content is served even if the classpath resource changes (e.g. in a development system).

This caching behaviour can be set on the `setFileCachingEnabled` option. The default value of this option is `true` unless the system property `vertx.disableFileCaching` is defined.

The path where the files are cached is `.vertx` by default and can be customized by setting the system property `vertx.cacheDirBase`.

The whole classpath resolving feature can be disabled system-wide by setting the system property `vertx.disableFileCPResolving` to `true`.

| NOTE | these system properties are evaluated once when the the `io.vertx.core.file.FileSystemOptions` class is loaded, so these properties should be set before loading this class or as a JVM system property when launching it. |
| ---- | ------------------------------------------------------------ |
|      |                                                              |

If you want to disable classpath resolving for a particular application but keep it enabled by default system-wide, you can do so via the `setClassPathResolvingEnabled` option.

#### Closing an AsyncFile {#Closing_an_AsyncFile}
To close an `AsyncFile` call the `close` method. Closing is asynchronous and if you want to be notified when the close has been completed you can specify a handler function as an argument.

## Datagram sockets (UDP) {#Datagram_sockets__UDP_}
Using User Datagram Protocol (UDP) with Vert.x is a piece of cake.

UDP is a connection-less transport which basically means you have no persistent connection to a remote peer.

Instead you can send and receive packages and the remote address is contained in each of them.

Beside this UDP is not as safe as TCP to use, which means there are no guarantees that a send Datagram packet will receive it’s endpoint at all.

The only guarantee is that it will either receive complete or not at all.

Also you usually can’t send data which is bigger then the MTU size of your network interface, this is because each packet will be send as one packet.

But be aware even if the packet size is smaller then the MTU it may still fail.

At which size it will fail depends on the Operating System etc. So rule of thumb is to try to send small packets.

Because of the nature of UDP it is best fit for Applications where you are allowed to drop packets (like for example a monitoring application).

The benefits are that it has a lot less overhead compared to TCP, which can be handled by the NetServer and NetClient (see above).

### Creating a DatagramSocket {#Creating_a_DatagramSocket}
To use UDP you first need t create a `DatagramSocket`. It does not matter here if you only want to send data or send and receive.

```
DatagramSocket socket = vertx.createDatagramSocket(new DatagramSocketOptions());
```

The returned `DatagramSocket` will not be bound to a specific port. This is not a problem if you only want to send data (like a client), but more on this in the next section.

### Sending Datagram packets {#Sending_Datagram_packets}
As mentioned before, User Datagram Protocol (UDP) sends data in packets to remote peers but is not connected to them in a persistent fashion.

This means each packet can be sent to a different remote peer.

Sending packets is as easy as shown here:

```
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

### Receiving Datagram packets {#Receiving_Datagram_packets}
If you want to receive packets you need to bind the `DatagramSocket` by calling `listen(…)}` on it.

This way you will be able to receive `DatagramPacket`s that were sent to the address and port on which the `DatagramSocket` listens.

Beside this you also want to set a `Handler` which will be called for each received `DatagramPacket`.

The `DatagramPacket` has the following methods:

- `sender`: The InetSocketAddress which represent the sender of the packet
- `data`: The Buffer which holds the data which was received.

So to listen on a specific address and port you would do something like shown here:

```
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

Be aware that even if the {code AsyncResult} is successed it only means it might be written on the network stack, but gives no guarantee that it ever reached or will reach the remote peer at all.

If you need such a guarantee then you want to use TCP with some handshaking logic build on top.

### Multicast {#Multicast}
#### Sending Multicast packets {#Sending_Multicast_packets}
Multicast allows multiple sockets to receive the same packets. This works by having the sockets join the same multicast group to which you can then send packets.

We will look at how you can join a Multicast Group and receive packets in the next section.

Sending multicast packets is not different than sending normal Datagram packets. The difference is that you pass in a multicast group address to the send method.

This is show here:

```
DatagramSocket socket = vertx.createDatagramSocket(new DatagramSocketOptions());
Buffer buffer = Buffer.buffer("content");
// Send a Buffer to a multicast address
socket.send(buffer, 1234, "230.0.0.1", asyncResult -> {
  System.out.println("Send succeeded? " + asyncResult.succeeded());
});
```

All sockets that have joined the multicast group 230.0.0.1 will receive the packet.

##### Receiving Multicast packets {#Receiving_Multicast_packets}
If you want to receive packets for specific Multicast group you need to bind the `DatagramSocket` by calling `listen(…)` on it to join the Multicast group.

This way you will receive DatagramPackets that were sent to the address and port on which the `DatagramSocket` listens and also to those sent to the Multicast group.

Beside this you also want to set a Handler which will be called for each received DatagramPacket.

The `DatagramPacket` has the following methods:

- `sender()`: The InetSocketAddress which represent the sender of the packet
- `data()`: The Buffer which holds the data which was received.

So to listen on a specific address and port and also receive packets for the Multicast group 230.0.0.1 you would do something like shown here:

```
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

##### Unlisten / leave a Multicast group {#Unlisten___leave_a_Multicast_group}
There are sometimes situations where you want to receive packets for a Multicast group for a limited time.

In this situations you can first start to listen for them and then later unlisten.

This is shown here:

```
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

##### Blocking multicast {#Blocking_multicast}
Beside unlisten a Multicast address it’s also possible to just block multicast for a specific sender address.

Be aware this only work on some Operating Systems and kernel versions. So please check the Operating System documentation if it’s supported.

This an expert feature.

To block multicast from a specific address you can call `blockMulticastGroup(…)` on the DatagramSocket like shown here:

```
DatagramSocket socket = vertx.createDatagramSocket(new DatagramSocketOptions());

// Some code

// This would block packets which are send from 10.0.0.2
socket.blockMulticastGroup("230.0.0.1", "10.0.0.2", asyncResult -> {
  System.out.println("block succeeded? " + asyncResult.succeeded());
});
```

#### DatagramSocket properties {#DatagramSocket_properties}
When creating a `DatagramSocket` there are multiple properties you can set to change it’s behaviour with the `DatagramSocketOptions` object. Those are listed here:

- `setSendBufferSize` Sets the send buffer size in bytes.
- `setReceiveBufferSize` Sets the TCP receive buffer size in bytes.
- `setReuseAddress` If true then addresses in TIME_WAIT state can be reused after they have been closed.
- `setTrafficClass`
- `setBroadcast` Sets or clears the SO_BROADCAST socket option. When this option is set, Datagram (UDP) packets may be sent to a local interface’s broadcast address.
- `setMulticastNetworkInterface` Sets or clears the IP_MULTICAST_LOOP socket option. When this option is set, multicast packets will also be received on the local interface.
- `setMulticastTimeToLive` Sets the IP_MULTICAST_TTL socket option. TTL stands for "Time to Live," but in this context it specifies the number of IP hops that a packet is allowed to go through, specifically for multicast traffic. Each router or gateway that forwards a packet decrements the TTL. If the TTL is decremented to 0 by a router, it will not be forwarded.

#### DatagramSocket Local Address {#DatagramSocket_Local_Address}
You can find out the local address of the socket (i.e. the address of this side of the UDP Socket) by calling `localAddress`. This will only return an `InetSocketAddress` if you bound the `DatagramSocket` with `listen(…)` before, otherwise it will return null.

#### Closing a DatagramSocket {#Closing_a_DatagramSocket}
You can close a socket by invoking the `close` method. This will close the socket and release all resources

## DNS client {#DNS_client}
Often you will find yourself in situations where you need to obtain DNS informations in an asynchronous fashion. Unfortunally this is not possible with the API that is shipped with the Java Virtual Machine itself. Because of this Vert.x offers it’s own API for DNS resolution which is fully asynchronous.

To obtain a DnsClient instance you will create a new via the Vertx instance.

```
DnsClient client = vertx.createDnsClient(53, "10.0.0.1");
```

You can also create the client with options and configure the query timeout.

```
DnsClient client = vertx.createDnsClient(new DnsClientOptions()
  .setPort(53)
  .setHost("10.0.0.1")
  .setQueryTimeout(10000)
);
```

Creating the client with no arguments or omitting the server address will use the address of the server used internally for non blocking address resolution.

```
DnsClient client1 = vertx.createDnsClient();

// Just the same but with a different query timeout
DnsClient client2 = vertx.createDnsClient(new DnsClientOptions().setQueryTimeout(10000));
```

### lookup {#lookup}
Try to lookup the A (ipv4) or AAAA (ipv6) record for a given name. The first which is returned will be used, so it behaves the same way as you may be used from when using "nslookup" on your operation system.

To lookup the A / AAAA record for "vertx.io" you would typically use it like:

```
DnsClient client = vertx.createDnsClient(53, "9.9.9.9");
client.lookup("vertx.io", ar -> {
  if (ar.succeeded()) {
    System.out.println(ar.result());
  } else {
    System.out.println("Failed to resolve entry" + ar.cause());
  }
});
```

### lookup4 {#lookup4}
Try to lookup the A (ipv4) record for a given name. The first which is returned will be used, so it behaves the same way as you may be used from when using "nslookup" on your operation system.

To lookup the A record for "vertx.io" you would typically use it like:

```
DnsClient client = vertx.createDnsClient(53, "9.9.9.9");
client.lookup4("vertx.io", ar -> {
  if (ar.succeeded()) {
    System.out.println(ar.result());
  } else {
    System.out.println("Failed to resolve entry" + ar.cause());
  }
});
```

### lookup6 {#lookup6}
Try to lookup the AAAA (ipv6) record for a given name. The first which is returned will be used, so it behaves the same way as you may be used from when using "nslookup" on your operation system.

To lookup the A record for "vertx.io" you would typically use it like:

```
DnsClient client = vertx.createDnsClient(53, "9.9.9.9");
client.lookup6("vertx.io", ar -> {
  if (ar.succeeded()) {
    System.out.println(ar.result());
  } else {
    System.out.println("Failed to resolve entry" + ar.cause());
  }
});
```

### resolveA {#resolveA}
Try to resolve all A (ipv4) records for a given name. This is quite similar to using "dig" on unix like operation systems.

To lookup all the A records for "vertx.io" you would typically do:

```
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

### resolveAAAA {#resolveAAAA}
Try to resolve all AAAA (ipv6) records for a given name. This is quite similar to using "dig" on unix like operation systems.

To lookup all the AAAAA records for "vertx.io" you would typically do:

```
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

### resolveCNAME {#resolveCNAME}
Try to resolve all CNAME records for a given name. This is quite similar to using "dig" on unix like operation systems.

To lookup all the CNAME records for "vertx.io" you would typically do:

```
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

### resolveMX {#resolveMX}
Try to resolve all MX records for a given name. The MX records are used to define which Mail-Server accepts emails for a given domain.

To lookup all the MX records for "vertx.io" you would typically do:

```
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

Be aware that the List will contain the `MxRecord` sorted by the priority of them, which means MX records with smaller priority coming first in the List.

The `MxRecord` allows you to access the priority and the name of the MX record by offer methods for it like:

```
record.priority();
record.name();
```

### resolveTXT {#resolveTXT}
Try to resolve all TXT records for a given name. TXT records are often used to define extra informations for a domain.

To resolve all the TXT records for "vertx.io" you could use something along these lines:

```
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

### resolveNS {#resolveNS}
Try to resolve all NS records for a given name. The NS records specify which DNS Server hosts the DNS informations for a given domain.

To resolve all the NS records for "vertx.io" you could use something along these lines:

```
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

### resolveSRV {#resolveSRV}
Try to resolve all SRV records for a given name. The SRV records are used to define extra informations like port and hostname of services. Some protocols need this extra informations.

To lookup all the SRV records for "vertx.io" you would typically do:

```
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

Be aware that the List will contain the SrvRecords sorted by the priority of them, which means SrvRecords with smaller priority coming first in the List.

The `SrvRecord` allows you to access all informations contained in the SRV record itself:

```
record.priority();
record.name();
record.weight();
record.port();
record.protocol();
record.service();
record.target();
```

Please refer to the API docs for the exact details.

### resolvePTR {#resolvePTR}
Try to resolve the PTR record for a given name. The PTR record maps an ipaddress to a name.

To resolve the PTR record for the ipaddress 10.0.0.1 you would use the PTR notion of "1.0.0.10.in-addr.arpa"

```
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

### reverseLookup {#reverseLookup}
Try to do a reverse lookup for an ipaddress. This is basically the same as resolve a PTR record, but allows you to just pass in the ipaddress and not a valid PTR query string.

To do a reverse lookup for the ipaddress 10.0.0.1 do something similar like this:

```
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

### Error handling {#Error_handling}
As you saw in previous sections the DnsClient allows you to pass in a Handler which will be notified with an AsyncResult once the query was complete. In case of an error it will be notified with a DnsException which will hole a `DnsResponseCode` that indicate why the resolution failed. This DnsResponseCode can be used to inspect the cause in more detail.

Possible DnsResponseCodes are:

- `NOERROR` No record was found for a given query
- `FORMERROR` Format error
- `SERVFAIL` Server failure
- `NXDOMAIN` Name error
- `NOTIMPL` Not implemented by DNS Server
- `REFUSED` DNS Server refused the query
- `YXDOMAIN` Domain name should not exist
- `YXRRSET` Resource record should not exist
- `NXRRSET` RRSET does not exist
- `NOTZONE` Name not in zone
- `BADVERS` Bad extension mechanism for version
- `BADSIG` Bad signature
- `BADKEY` Bad key
- `BADTIME` Bad timestamp

All of those errors are "generated" by the DNS Server itself.

You can obtain the DnsResponseCode from the DnsException like:

```
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

## Streams {#Streams}
There are several objects in Vert.x that allow items to be read from and written.

In previous versions the `io.vertx.core.streams` package was manipulating `Buffer` objects exclusively. From now, streams are not coupled to buffers anymore and they work with any kind of objects.

In Vert.x, write calls return immediately, and writes are queued internally.

It’s not hard to see that if you write to an object faster than it can actually write the data to its underlying resource, then the write queue can grow unbounded - eventually resulting in memory exhaustion.

To solve this problem a simple flow control (*back-pressure*) capability is provided by some objects in the Vert.x API.

Any flow control aware object that can be *written-to* implements `WriteStream`, while any flow control object that can be *read-from* is said to implement `ReadStream`.

Let’s take an example where we want to read from a `ReadStream` then write the data to a `WriteStream`.

A very simple example would be reading from a `NetSocket` then writing back to the same `NetSocket` - since `NetSocket` implements both `ReadStream` and `WriteStream`. Note that this works between any `ReadStream` and `WriteStream` compliant object, including HTTP requests, HTTP responses, async files I/O, WebSockets, etc.

A naive way to do this would be to directly take the data that has been read and immediately write it to the `NetSocket`:

```
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

There is a problem with the example above: if data is read from the socket faster than it can be written back to the socket, it will build up in the write queue of the `NetSocket`, eventually running out of RAM. This might happen, for example if the client at the other end of the socket wasn’t reading fast enough, effectively putting back-pressure on the connection.

Since `NetSocket` implements `WriteStream`, we can check if the `WriteStream` is full before writing to it:

```
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

This example won’t run out of RAM but we’ll end up losing data if the write queue gets full. What we really want to do is pause the `NetSocket` when the write queue is full:

```
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

We’re almost there, but not quite. The `NetSocket` now gets paused when the file is full, but we also need to unpause it when the write queue has processed its backlog:

```
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

And there we have it. The `drainHandler` event handler will get called when the write queue is ready to accept more data, this resumes the `NetSocket` that allows more data to be read.

Wanting to do this is quite common while writing Vert.x applications, so we added the `pipeTo` method that does all of this hard work for you. You just feed it the `WriteStream` and use it:

```
NetServer server = vertx.createNetServer(
  new NetServerOptions().setPort(1234).setHost("localhost")
);
server.connectHandler(sock -> {
  sock.pipeTo(sock);
}).listen();
```

This does exactly the same thing as the more verbose example, plus it handles stream failures and termination: the destination `WriteStream` is ended when the pipe completes with success or a failure.

You can be notified when the operation completes:

```
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

When you deal with an asynchronous destination, you can create a `Pipe` instance that pauses the source and resumes it when the source is piped to the destination:

```
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

When you need to abort the transfer, you need to close it:

```
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

When the pipe is closed, the streams handlers are unset and the `ReadStream` resumed.

As seen above, by default the destination is always ended when the stream completes, you can control this behavior on the pipe object:

- `endOnFailure` controls the behavior when a failure happens
- `endOnSuccess` controls the behavior when the read stream ends
- `endOnComplete` controls the behavior in all cases

Here is a short example:

```
src.pipe()
  .endOnSuccess(false)
  .to(dst, rs -> {
    // Append some text and close the file
    dst.end(Buffer.buffer("done"));
});
```

Let’s now look at the methods on `ReadStream` and `WriteStream` in more detail:

### ReadStream {#ReadStream}
`ReadStream` is implemented by `HttpClientResponse`, `DatagramSocket`, `HttpClientRequest`, `HttpServerFileUpload`, `HttpServerRequest`, `MessageConsumer`, `NetSocket`, `WebSocket`, `TimeoutStream`, `AsyncFile`.

Functions:

- `handler`: set a handler which will receive items from the ReadStream.
- `pause`: pause the handler. When paused no items will be received in the handler.
- `resume`: resume the handler. The handler will be called if any item arrives.
- `exceptionHandler`: Will be called if an exception occurs on the ReadStream.
- `endHandler`: Will be called when end of stream is reached. This might be when EOF is reached if the ReadStream represents a file, or when end of request is reached if it’s an HTTP request, or when the connection is closed if it’s a TCP socket.

### WriteStream {#WriteStream}
```
WriteStream` is implemented by `HttpClientRequest`, `HttpServerResponse` `WebSocket`, `NetSocket`, `AsyncFile`, and `MessageProducer
```

Functions:

- `write`: write an object to the WriteStream. This method will never block. Writes are queued internally and asynchronously written to the underlying resource.
- `setWriteQueueMaxSize`: set the number of object at which the write queue is considered *full*, and the method `writeQueueFull` returns `true`. Note that, when the write queue is considered full, if write is called the data will still be accepted and queued. The actual number depends on the stream implementation, for `Buffer` the size represents the actual number of bytes written and not the number of buffers.
- `writeQueueFull`: returns `true` if the write queue is considered full.
- `exceptionHandler`: Will be called if an exception occurs on the `WriteStream`.
- `drainHandler`: The handler will be called if the `WriteStream` is considered no longer full.

### Pump {#Pump}
The pump exposes a subset of the pipe API and only transfers the items between streams, it does not handle the completion or failure of the transfer operation.

```
NetServer server = vertx.createNetServer(
  new NetServerOptions().setPort(1234).setHost("localhost")
);
server.connectHandler(sock -> {
  Pump.pump(sock, sock).start();
}).listen();
```

| IMPORTANT | Before Vert.x 3.7 the `Pump` was the advocated API for transferring a read stream to a write stream. Since 3.7 the pipe API supersedes the pump API. |
| --------- | ------------------------------------------------------------ |
|           |                                                              |

Instances of Pump have the following methods:

- `start`: Start the pump.
- `stop`: Stops the pump. When the pump starts it is in stopped mode.
- `setWriteQueueMaxSize`: This has the same meaning as `setWriteQueueMaxSize` on the `WriteStream`.

A pump can be started and stopped multiple times.

When a pump is first created it is *not* started. You need to call the `start()` method to start it.

## Record Parser {#Record_Parser}
The record parser allows you to easily parse protocols which are delimited by a sequence of bytes, or fixed size records. It transforms a sequence of input buffer to a sequence of buffer structured as configured (either fixed size or separated records).

For example, if you have a simple ASCII text protocol delimited by '\n' and the input is the following:

```
buffer1:HELLO\nHOW ARE Y
buffer2:OU?\nI AM
buffer3: DOING OK
buffer4:\n
```

The record parser would produce

```
buffer1:HELLO
buffer2:HOW ARE YOU?
buffer3:I AM DOING OK
```

Let’s see the associated code:

```
final RecordParser parser = RecordParser.newDelimited("\n", h -> {
  System.out.println(h.toString());
});

parser.handle(Buffer.buffer("HELLO\nHOW ARE Y"));
parser.handle(Buffer.buffer("OU?\nI AM"));
parser.handle(Buffer.buffer("DOING OK"));
parser.handle(Buffer.buffer("\n"));
```

You can also produce fixed sized chunks as follows:

```
RecordParser.newFixed(4, h -> {
  System.out.println(h.toString());
});
```

For more details, check out the `RecordParser` class.

## Json Parser {#Json_Parser}
You can easily parse JSON structures but that requires to provide the JSON content at once, but it may not be convenient when you need to parse very large structures.

The non-blocking JSON parser is an event driven parser able to deal with very large structures. It transforms a sequence of input buffer to a sequence of JSON parse events.

```
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

The parser is non-blocking and emitted events are driven by the input buffers.

```
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

Event driven parsing provides more control but comes at the price of dealing with fine grained events, which can be inconvenient sometimes. The JSON parser allows you to handle JSON structures as values when it is desired:

```
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

The value mode can be set and unset during the parsing allowing you to switch between fine grained events or JSON object value events.

```
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

You can do the same with arrays as well

```
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

You can also decode POJOs

```
parser.handler(event -> {
  // Handle each object
  // Get the field in which this object was parsed
  String id = event.fieldName();
  User user = event.mapTo(User.class);
  System.out.println("User with id " + id + " : " + user.firstName + " " + user.lastName);
});
```

Whenever the parser fails to process a buffer, an exception will be thrown unless you set an exception handler:

```
JsonParser parser = JsonParser.newParser();

parser.exceptionHandler(err -> {
  // Catch any parsing or decoding error
});
```

The parser also parses json streams:

- concatenated json streams: `{"temperature":30}{"temperature":50}`
- line delimited json streams: `{"an":"object"}\r\n3\r\n"a string"\r\nnull`

For more details, check out the `JsonParser` class.

## Thread safety {#Thread_safety}
Most Vert.x objects are safe to access from different threads. *However* performance is optimised when they are accessed from the same context they were created from.

For example if you have deployed a verticle which creates a `NetServer` which provides `NetSocket` instances in it’s handler, then it’s best to always access that socket instance from the event loop of the verticle.

If you stick to the standard Vert.x verticle deployment model and avoid sharing objects between verticles then this should be the case without you having to think about it.

## Metrics SPI {#Metrics_SPI}
By default Vert.x does not record any metrics. Instead it provides an SPI for others to implement which can be added to the classpath. The metrics SPI is an advanced feature which allows implementers to capture events from Vert.x in order to gather metrics. For more information on this, please consult the `API Documentation`.

You can also specify a metrics factory programmatically if embedding Vert.x using `setFactory`.

## OSGi {#OSGi}
Vert.x Core is packaged as an OSGi bundle, so can be used in any OSGi R4.2+ environment such as Apache Felix or Eclipse Equinox. The bundle exports `io.vertx.core*`.

However, the bundle has some dependencies on Jackson and Netty. To get the vert.x core bundle resolved deploy:

- Jackson Annotation [2.6.0,3)
- Jackson Core [2.6.2,3)
- Jackson Databind [2.6.2,3)
- Netty Buffer [4.0.31,5)
- Netty Codec [4.0.31,5)
- Netty Codec/Socks [4.0.31,5)
- Netty Codec/Common [4.0.31,5)
- Netty Codec/Handler [4.0.31,5)
- Netty Codec/Transport [4.0.31,5)

Here is a working deployment on Apache Felix 5.2.0:

```
14|Active     |    1|Jackson-annotations (2.6.0)
15|Active     |    1|Jackson-core (2.6.2)
16|Active     |    1|jackson-databind (2.6.2)
18|Active     |    1|Netty/Buffer (4.0.31.Final)
19|Active     |    1|Netty/Codec (4.0.31.Final)
20|Active     |    1|Netty/Codec/HTTP (4.0.31.Final)
21|Active     |    1|Netty/Codec/Socks (4.0.31.Final)
22|Active     |    1|Netty/Common (4.0.31.Final)
23|Active     |    1|Netty/Handler (4.0.31.Final)
24|Active     |    1|Netty/Transport (4.0.31.Final)
25|Active     |    1|Netty/Transport/SCTP (4.0.31.Final)
26|Active     |    1|Vert.x Core (3.1.0)
```

On Equinox, you may want to disable the `ContextFinder` with the following framework property: `eclipse.bundle.setTCCL=false`

## The 'vertx' command line {#The__vertx__command_line}
The `vertx` command is used to interact with Vert.x from the command line. It’s main use is to run Vert.x verticles. To do this you need to download and install a Vert.x distribution, and add the `bin` directory of the installation to your `PATH` environment variable. Also make sure you have a Java 8 JDK on your `PATH`.

| NOTE | The JDK is required to support on the fly compilation of Java code. |
| ---- | ------------------------------------------------------------ |
|      |                                                              |

### Run verticles {#Run_verticles}
You can run raw Vert.x verticles directly from the command line using `vertx run`. Here is a couple of examples of the `run` *command*:

```
vertx run my-verticle.js                                 (1)
vertx run my-verticle.groovy                             (2)
vertx run my-verticle.rb                                 (3)

vertx run io.vertx.example.MyVerticle                    (4)
vertx run io.vertx.example.MVerticle -cp my-verticle.jar (5)

vertx run MyVerticle.java                                (6)
```

1. Deploys a JavaScript verticle
2. Deploys a Groovy verticle
3. Deploys a Ruby verticle
4. Deploys an already compiled Java verticle. Classpath root is the current directory
5. Deploys a verticle packaged in a Jar, the jar need to be in the classpath
6. Compiles the Java source and deploys it

As you can see in the case of Java, the name can either be the fully qualified class name of the verticle, or you can specify the Java Source file directly and Vert.x compiles it for you.

You can also prefix the verticle with the name of the language implementation to use. For example if the verticle is a compiled Groovy class, you prefix it with `groovy:` so that Vert.x knows it’s a Groovy class not a Java class.

```
vertx run groovy:io.vertx.example.MyGroovyVerticle
```

The `vertx run` command can take a few optional parameters, they are:

- `-options ` - Provides the Vert.x options. `options` is the name of a JSON file that represents the Vert.x options, or a JSON string. This is optional.
- `-conf ` - Provides some configuration to the verticle. `config` is the name of a JSON file that represents the configuration for the verticle, or a JSON string. This is optional.
- `-cp ` - The path on which to search for the verticle and any other resources used by the verticle. This defaults to `.` (current directory). If your verticle references other scripts, classes or other resources (e.g. jar files) then make sure these are on this path. The path can contain multiple path entries separated by `:` (colon) or `;` (semi-colon) depending on the operating system. Each path entry can be an absolute or relative path to a directory containing scripts, or absolute or relative filenames for jar or zip files. An example path might be `-cp classes:lib/otherscripts:jars/myjar.jar:jars/otherjar.jar`. Always use the path to reference any resources that your verticle requires. Do **not** put them on the system classpath as this can cause isolation issues between deployed verticles.
- `-instances ` - The number of instances of the verticle to instantiate. Each verticle instance is strictly single threaded so to scale your application across available cores you might want to deploy more than one instance. If omitted a single instance will be deployed.
- `-worker` - This option determines whether the verticle is a worker verticle or not.
- `-cluster` - This option determines whether the Vert.x instance will attempt to form a cluster with other Vert.x instances on the network. Clustering Vert.x instances allows Vert.x to form a distributed event bus with other nodes. Default is `false` (not clustered).
- `-cluster-port` - If the `cluster` option has also been specified then this determines which port will be bound for cluster communication with other Vert.x instances. Default is `0` - which means '*choose a free random port*'. You don’t usually need to specify this parameter unless you really need to bind to a specific port.
- `-cluster-host` - If the `cluster` option has also been specified then this determines which host address will be bound for cluster communication with other Vert.x instances. By default it will try and pick one from the available interfaces. If you have more than one interface and you want to use a specific one, specify it here.
- `-cluster-public-port` - If the `cluster` option has also been specified then this determines which port will be advertised for cluster communication with other Vert.x instances. Default is `-1`, which means same as `cluster-port`.
- `-cluster-public-host` - If the `cluster` option has also been specified then this determines which host address will be advertised for cluster communication with other Vert.x instances. If not specified, Vert.x uses the value of `cluster-host`.
- `-ha` - if specified the verticle will be deployed as high availability (HA) deployment. See related section for more details
- `-quorum` - used in conjunction with `-ha`. It specifies the minimum number of nodes in the cluster for any *HA deploymentIDs* to be active. Defaults to 0.
- `-hagroup` - used in conjunction with `-ha`. It specifies the HA group this node will join. There can be multiple HA groups in a cluster. Nodes will only failover to other nodes in the same group. The default value is ` __DEFAULT__`

You can also set system properties using: `-Dkey=value`.

Here are some more examples:

Run a JavaScript verticle server.js with default settings

```
vertx run server.js
```

Run 10 instances of a pre-compiled Java verticle specifying classpath

```
vertx run com.acme.MyVerticle -cp "classes:lib/myjar.jar" -instances 10
```

Run 10 instances of a Java verticle by source *file*

```
vertx run MyVerticle.java -instances 10
```

Run 20 instances of a ruby worker verticle

```
vertx run order_worker.rb -instances 20 -worker
```

Run two JavaScript verticles on the same machine and let them cluster together with each other and any other servers on the network

```
vertx run handler.js -cluster
vertx run sender.js -cluster
```

Run a Ruby verticle passing it some config

```
vertx run my_verticle.rb -conf my_verticle.conf
```

Where `my_verticle.conf` might contain something like:

```
{
"name": "foo",
"num_widgets": 46
}
```

The config will be available inside the verticle via the core API.

When using the high-availability feature of vert.x you may want to create a *bare* instance of vert.x. This instance does not deploy any verticles when launched, but will receive a verticle if another node of the cluster dies. To create a *bare* instance, launch:

```
vertx bare
```

Depending on your cluster configuration, you may have to append the `cluster-host` and `cluster-port` parameters.

### Executing a Vert.x application packaged as a fat jar {#Executing_a_Vert_x_application_packaged_as_a_fat_jar}
A *fat jar* is an executable jar embedding its dependencies. This means you don’t have to have Vert.x pre-installed on the machine on which you execute the jar. Like any executable Java jar it can be executed with.

```
java -jar my-application-fat.jar
```

There is nothing really Vert.x specific about this, you could do this with any Java application

You can either create your own main class and specify that in the manifest, but it’s recommended that you write your code as verticles and use the Vert.x `Launcher` class (`io.vertx.core.Launcher`) as your main class. This is the same main class used when running Vert.x at the command line and therefore allows you to specify command line arguments, such as `-instances` in order to scale your application more easily.

To deploy your verticle in a *fatjar* like this you must have a *manifest* with:

- `Main-Class` set to `io.vertx.core.Launcher`
- `Main-Verticle` specifying the main verticle (fully qualified class name or script file name)

You can also provide the usual command line arguments that you would pass to `vertx run`:

```
java -jar my-verticle-fat.jar -cluster -conf myconf.json
java -jar my-verticle-fat.jar -cluster -conf myconf.json -cp path/to/dir/conf/cluster_xml
```

| NOTE | Please consult the Maven/Gradle simplest and Maven/Gradle verticle examples in the examples repository for examples of building applications as fatjars. |
| ---- | ------------------------------------------------------------ |
|      |                                                              |

A fat jar executes the `run` command, by default.

### Displaying version of Vert.x {#Displaying_version_of_Vert_x}
To display the vert.x version, just launch:

```
vertx version
```

### Other commands {#Other_commands}
The `vertx` command line and the `Launcher` also provide other *commands* in addition to `run` and `version`:

You can create a `bare` instance using:

```
vertx bare
# or
java -jar my-verticle-fat.jar bare
```

You can also start an application in background using:

```
java -jar my-verticle-fat.jar start --vertx-id=my-app-name
```

If `my-app-name` is not set, a random id will be generated, and printed on the command prompt. You can pass `run` options to the `start` command:

```
java -jar my-verticle-fat.jar start —-vertx-id=my-app-name -cluster
```

Once launched in background, you can stop it with the `stop` command:

```
java -jar my-verticle-fat.jar stop my-app-name
```

You can also list the vert.x application launched in background using:

```
java -jar my-verticle-fat.jar list
```

The `start`, `stop` and `list` command are also available from the `vertx` tool. The start` command supports a couple of options:

- `vertx-id` : the application id, uses a random UUID if not set
- `java-opts` : the Java Virtual Machine options, uses the `JAVA_OPTS` environment variable if not set.
- `redirect-output` : redirect the spawned process output and error streams to the parent process streams.

If option values contain spaces, don’t forget to wrap the value between `""` (double-quotes).

As the `start` command spawns a new process, the java options passed to the JVM are not propagated, so you **must** use `java-opts` to configure the JVM (`-X`, `-D`…). If you use the `CLASSPATH` environment variable, be sure it contains all the required jars (vertx-core, your jars and all the dependencies).

The set of commands is extensible, refer to the [Extending the vert.x Launcher](https://vertx.io/docs/vertx-core/java/#_extending_the_vert_x_launcher) section.

### Live Redeploy {#Live_Redeploy}
When developing it may be convenient to automatically redeploy your application upon file changes. The `vertx` command line tool and more generally the `Launcher` class offers this feature. Here are some examples:

```
vertx run MyVerticle.groovy --redeploy="**&#47;*.groovy" --launcher-class=io.vertx.core.Launcher
vertx run MyVerticle.groovy --redeploy="**&#47;*.groovy,**&#47;*.rb"  --launcher-class=io.vertx.core.Launcher
java io.vertx.core.Launcher run org.acme.MyVerticle --redeploy="**&#47;*.class"  --launcher-class=io.vertx.core
.Launcher -cp ...
```

The redeployment process is implemented as follows. First your application is launched as a background application (with the `start` command). On matching file changes, the process is stopped and the application is restarted. This avoids leaks, as the process is restarted.

To enable the live redeploy, pass the `--redeploy` option to the `run` command. The `--redeploy` indicates the set of file to *watch*. This set can use Ant-style patterns (with `**`, `*` and `?`). You can specify several sets by separating them using a comma (`,`). Patterns are relative to the current working directory.

Parameters passed to the `run` command are passed to the application. Java Virtual Machine options can be configured using `--java-opts`. For instance, to pass the the `conf` parameter or a system property, you need to use: `--java-opts="-conf=my-conf.json -Dkey=value"`

The `--launcher-class` option determine with with *main* class the application is launcher. It’s generally `Launcher`, but you have use you own *main*.

The redeploy feature can be used in your IDE:

- Eclipse - create a *Run* configuration, using the `io.vertx.core.Launcher` class a *main class*. In the *Program arguments* area (in the *Arguments* tab), write `run your-verticle-fully-qualified-name --redeploy=**/*.java --launcher-class=io.vertx.core.Launcher`. You can also add other parameters. The redeployment works smoothly as Eclipse incrementally compiles your files on save.
- IntelliJ - create a *Run* configuration (*Application*), set the *Main class* to `io.vertx.core.Launcher`. In the Program arguments write: `run your-verticle-fully-qualified-name --redeploy=**/*.class --launcher-class=io.vertx.core.Launcher`. To trigger the redeployment, you need to *make* the project or the module explicitly (*Build* menu → *Make project*).

To debug your application, create your run configuration as a remote application and configure the debugger using `--java-opts`. However, don’t forget to re-plug the debugger after every redeployment as a new process is created every time.

You can also hook your build process in the redeploy cycle:

```
java -jar target/my-fat-jar.jar --redeploy="**&#47;*.java" --on-redeploy="mvn package"
java -jar build/libs/my-fat-jar.jar --redeploy="src&#47;**&#47;*.java" --on-redeploy='./gradlew shadowJar'
```

The "on-redeploy" option specifies a command invoked after the shutdown of the application and before the restart. So you can hook your build tool if it updates some runtime artifacts. For instance, you can launch `gulp` or `grunt` to update your resources. Don’t forget that passing parameters to your application requires the `--java-opts` param:

```
java -jar target/my-fat-jar.jar --redeploy="**&#47;*.java" --on-redeploy="mvn package" --java-opts="-Dkey=val"
java -jar build/libs/my-fat-jar.jar --redeploy="src&#47;**&#47;*.java" --on-redeploy='./gradlew shadowJar' --java-opts="-Dkey=val"
```

The redeploy feature also supports the following settings:

- `redeploy-scan-period` : the file system check period (in milliseconds), 250ms by default
- `redeploy-grace-period` : the amount of time (in milliseconds) to wait between 2 re-deployments, 1000ms by default
- `redeploy-termination-period` : the amount of time to wait after having stopped the application (before launching user command). This is useful on Windows, where the process is not killed immediately. The time is given in milliseconds. 0 ms by default.

## Cluster Managers {#Cluster_Managers}
In Vert.x a cluster manager is used for various functions including:

- Discovery and group membership of Vert.x nodes in a cluster
- Maintaining cluster wide topic subscriber lists (so we know which nodes are interested in which event bus addresses)
- Distributed Map support
- Distributed Locks
- Distributed Counters

Cluster managers *do not* handle the event bus inter-node transport, this is done directly by Vert.x with TCP connections.

The default cluster manager used in the Vert.x distributions is one that uses [Hazelcast](http://hazelcast.com/) but this can be easily replaced by a different implementation as Vert.x cluster managers are pluggable.

A cluster manager must implement the interface `ClusterManager`. Vert.x locates cluster managers at run-time by using the Java [Service Loader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) functionality to locate instances of `ClusterManager` on the classpath.

If you are using Vert.x at the command line and you want to use clustering you should make sure the `lib` directory of the Vert.x installation contains your cluster manager jar.

If you are using Vert.x from a Maven or Gradle project just add the cluster manager jar as a dependency of your project.

You can also specify cluster managers programmatically if embedding Vert.x using `setClusterManager`.

## Logging {#Logging}
Vert.x logs using it’s in-built logging API. The default implementation uses the JDK (JUL) logging so no extra logging dependencies are needed.

### Configuring JUL logging {#Configuring_JUL_logging}
A JUL logging configuration file can be specified in the normal JUL way by providing a system property called: `java.util.logging.config.file` with the value being your configuration file. For more information on this and the structure of a JUL config file please consult the JUL logging documentation.

Vert.x also provides a slightly more convenient way to specify a configuration file without having to set a system property. Just provide a JUL config file with the name `vertx-default-jul-logging.properties` on your classpath (e.g. inside your fatjar) and Vert.x will use that to configure JUL.

### Using another logging framework {#Using_another_logging_framework}
If you don’t want Vert.x to use JUL for it’s own logging you can configure it to use another logging framework, e.g. Log4J or SLF4J.

To do this you should set a system property called `vertx.logger-delegate-factory-class-name` with the name of a Java class which implements the interface `LogDelegateFactory`. We provide pre-built implementations for Log4J (version 1), Log4J 2 and SLF4J with the class names `io.vertx.core.logging.Log4jLogDelegateFactory`, `io.vertx.core.logging.Log4j2LogDelegateFactory` and `io.vertx.core.logging.SLF4JLogDelegateFactory` respectively. If you want to use these implementations you should also make sure the relevant Log4J or SLF4J jars are on your classpath.

Notice that, the provided delegate for Log4J 1 does not support parameterized message. The delegate for Log4J 2 uses the `{}` syntax like the SLF4J delegate. JUL delegate uses the `{x}` syntax.

### Netty logging {#Netty_logging}
When configuring logging, you should care about configuring Netty logging as well.

Netty does not rely on external logging configuration (e.g system properties) and instead implements a logging configuration based on the logging libraries visible from the Netty classes:

- use `SLF4J` library if it is visible
- otherwise use `Log4j` if it is visible
- otherwise fallback `java.util.logging`

The logger implementation can be forced to a specific implementation by setting Netty’s internal logger implementation directly on `io.netty.util.internal.logging.InternalLoggerFactory`:

```
// Force logging to Log4j
InternalLoggerFactory.setDefaultFactory(Log4JLoggerFactory.INSTANCE);
```

### Troubleshooting {#Troubleshooting}
#### SLF4J warning at startup {#SLF4J_warning_at_startup}
If, when you start your application, you see the following message:

```
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
```

It means that you have SLF4J-API in your classpath but no actual binding. Messages logged with SLF4J will be dropped. You should add a binding to your classpath. Check https://www.slf4j.org/manual.html#swapping to pick a binding and configure it.

Be aware that Netty looks for the SLF4-API jar and uses it by default.

#### Connection reset by peer {#Connection_reset_by_peer}
If your logs show a bunch of:

```
io.vertx.core.net.impl.ConnectionBase
SEVERE: java.io.IOException: Connection reset by peer
```

It means that the client is resetting the HTTP connection instead of closing it. This message also indicates that you may have not consumed the complete payload (the connection was cut before you were able to).

## Host name resolution {#Host_name_resolution}
Vert.x uses an an address resolver for resolving host name into IP addresses instead of the JVM built-in blocking resolver.

An host name resolves to an IP address using:

- the *hosts* file of the operating system
- otherwise DNS queries against a list of servers

By default it will use the list of the system DNS server addresses from the environment, if that list cannot be retrieved it will use Google’s public DNS servers `"8.8.8.8"` and `"8.8.4.4"`.

DNS servers can be also configured when creating a `Vertx` instance:

```
Vertx vertx = Vertx.vertx(new VertxOptions().
    setAddressResolverOptions(
        new AddressResolverOptions().
            addServer("192.168.0.1").
            addServer("192.168.0.2:40000"))
);
```

The default port of a DNS server is `53`, when a server uses a different port, this port can be set using a colon delimiter: `192.168.0.2:40000`.

| NOTE | sometimes it can be desirable to use the JVM built-in resolver, the JVM system property *-Dvertx.disableDnsResolver=true* activates this behavior |
| ---- | ------------------------------------------------------------ |
|      |                                                              |

### Failover {#Failover}
When a server does not reply in a timely manner, the resolver will try the next one from the list, the search is limited by `setMaxQueries` (the default value is `4` queries).

A DNS query is considered as failed when the resolver has not received a correct answer within `getQueryTimeout` milliseconds (the default value is `5` seconds).

### Server list rotation {#Server_list_rotation}
By default the dns server selection uses the first one, the remaining servers are used for failover.

You can configure `setRotateServers` to `true` to let the resolver perform a round-robin selection instead. It spreads the query load among the servers and avoids all lookup to hit the first server of the list.

Failover still applies and will use the next server in the list.

### Hosts mapping {#Hosts_mapping}
The *hosts* file of the operating system is used to perform an hostname lookup for an ipaddress.

An alternative *hosts* file can be used instead:

```
Vertx vertx = Vertx.vertx(new VertxOptions().
    setAddressResolverOptions(
        new AddressResolverOptions().
            setHostsPath("/path/to/hosts"))
);
```

### Search domains {#Search_domains}
By default the resolver will use the system DNS search domains from the environment. Alternatively an explicit search domain list can be provided:

```
Vertx vertx = Vertx.vertx(new VertxOptions().
    setAddressResolverOptions(
        new AddressResolverOptions().addSearchDomain("foo.com").addSearchDomain("bar.com"))
);
```

When a search domain list is used, the threshold for the number of dots is `1` or loaded from `/etc/resolv.conf` on Linux, it can be configured to a specific value with `setNdots`.

## High Availability and Fail-Over {#High_Availability_and_Fail_Over}
Vert.x allows you to run your verticles with high availability (HA) support. In that case, when a vert.x instance running a verticle dies abruptly, the verticle is migrated to another vertx instance. The vert.x instances must be in the same cluster.

### Automatic failover {#Automatic_failover}
When vert.x runs with *HA* enabled, if a vert.x instance where a verticle runs fails or dies, the verticle is redeployed automatically on another vert.x instance of the cluster. We call this *verticle fail-over*.

To run vert.x with the *HA* enabled, just add the `-ha` flag to the command line:

```
vertx run my-verticle.js -ha
```

Now for HA to work, you need more than one Vert.x instances in the cluster, so let’s say you have another Vert.x instance that you have already started, for example:

```
vertx run my-other-verticle.js -ha
```

If the Vert.x instance that is running `my-verticle.js` now dies (you can test this by killing the process with `kill -9`), the Vert.x instance that is running `my-other-verticle.js` will automatic deploy `my-verticle .js` so now that Vert.x instance is running both verticles.

| NOTE | the migration is only possible if the second vert.x instance has access to the verticle file (here `my-verticle.js`). |
| ---- | ------------------------------------------------------------ |
|      |                                                              |

| IMPORTANT | Please note that cleanly closing a Vert.x instance will not cause failover to occur, e.g. `CTRL-C` or `kill -SIGINT` |
| --------- | ------------------------------------------------------------ |
|           |                                                              |

You can also start *bare* Vert.x instances - i.e. instances that are not initially running any verticles, they will also failover for nodes in the cluster. To start a bare instance you simply do:

```
vertx run -ha
```

When using the `-ha` switch you do not need to provide the `-cluster` switch, as a cluster is assumed if you want HA.

| NOTE | depending on your cluster configuration, you may need to customize the cluster manager configuration (Hazelcast by default), and/or add the `cluster-host` and `cluster-port` parameters. |
| ---- | ------------------------------------------------------------ |
|      |                                                              |

### HA groups {#HA_groups}
When running a Vert.x instance with HA you can also optional specify a *HA group*. A HA group denotes a logical group of nodes in the cluster. Only nodes with the same HA group will failover onto one another. If you don’t specify a HA group the default group `__DEFAULT__` is used.

To specify an HA group you use the `-hagroup` switch when running the verticle, e.g.

```
vertx run my-verticle.js -ha -hagroup my-group
```

Let’s look at an example:

In a first terminal:

```
vertx run my-verticle.js -ha -hagroup g1
```

In a second terminal, let’s run another verticle using the same group:

```
vertx run my-other-verticle.js -ha -hagroup g1
```

Finally, in a third terminal, launch another verticle using a different group:

```
vertx run yet-another-verticle.js -ha -hagroup g2
```

If we kill the instance in terminal 1, it will fail over to the instance in terminal 2, not the instance in terminal 3 as that has a different group.

If we kill the instance in terminal 3, it won’t get failed over as there is no other vert.x instance in that group.

### Dealing with network partitions - Quora {#Dealing_with_network_partitions___Quora}
The HA implementation also supports quora. A quorum is the minimum number of votes that a distributed transaction has to obtain in order to be allowed to perform an operation in a distributed system.

When starting a Vert.x instance you can instruct it that it requires a `quorum` before any HA deployments will be deployed. In this context, a quorum is a minimum number of nodes for a particular group in the cluster. Typically you chose your quorum size to `Q = 1 + N/2` where `N` is the number of nodes in the group. If there are less than `Q` nodes in the cluster the HA deployments will undeploy. They will redeploy again if/when a quorum is re-attained. By doing this you can prevent against network partitions, a.k.a. *split brain*.

There is more information on quora [here](https://en.wikipedia.org/wiki/Quorum_(distributed_computing)).

To run vert.x instances with a quorum you specify `-quorum` on the command line, e.g.

In a first terminal:

```
vertx run my-verticle.js -ha -quorum 3
```

At this point the Vert.x instance will start but not deploy the module (yet) because there is only one node in the cluster, not 3.

In a second terminal:

```
vertx run my-other-verticle.js -ha -quorum 3
```

At this point the Vert.x instance will start but not deploy the module (yet) because there are only two nodes in the cluster, not 3.

In a third console, you can start another instance of vert.x:

```
vertx run yet-another-verticle.js -ha -quorum 3
```

Yay! - we have three nodes, that’s a quorum. At this point the modules will automatically deploy on all instances.

If we now close or kill one of the nodes the modules will automatically undeploy on the other nodes, as there is no longer a quorum.

Quora can also be used in conjunction with ha groups. In that case, quora are resolved for each particular group.

## Native transports {#Native_transports}
Vert.x can run with [native transports](http://netty.io/wiki/native-transports.html) (when available) on BSD (OSX) and Linux:

```
Vertx vertx = Vertx.vertx(new VertxOptions().
  setPreferNativeTransport(true)
);

// True when native is available
boolean usingNative = vertx.isNativeTransportEnabled();
System.out.println("Running with native: " + usingNative);
```

| NOTE | preferring native transport will not prevent the application to execute (for example if a JAR is missing). If your application requires native transport, you need to check `isNativeTransportEnabled`. |
| ---- | ------------------------------------------------------------ |
|      |                                                              |

### Native Linux Transport {#Native_Linux_Transport}
You need to add the following dependency in your classpath:

```
<dependency>
 <groupId>io.netty</groupId>
 <artifactId>netty-transport-native-epoll</artifactId>
 <version>4.1.15.Final</version>
 <classifier>linux-x86_64</classifier>
</dependency>
```

Native on Linux gives you extra networking options:

- `SO_REUSEPORT`
- `TCP_QUICKACK`
- `TCP_CORK`
- `TCP_FASTOPEN`

```
vertx.createHttpServer(new HttpServerOptions()
  .setTcpFastOpen(fastOpen)
  .setTcpCork(cork)
  .setTcpQuickAck(quickAck)
  .setReusePort(reusePort)
);
```

### Native BSD Transport {#Native_BSD_Transport}
You need to add the following dependency in your classpath:

```
<dependency>
 <groupId>io.netty</groupId>
 <artifactId>netty-transport-native-kqueue</artifactId>
 <version>4.1.15.Final</version>
 <classifier>osx-x86_64</classifier>
</dependency>
```

MacOS Sierra and above are supported.

Native on BSD gives you extra networking options:

- `SO_REUSEPORT`

```
vertx.createHttpServer(new HttpServerOptions().setReusePort(reusePort));
```

### Domain sockets {#Domain_sockets}
Natives provide domain sockets support for servers:

```
vertx.createNetServer().connectHandler(so -> {
  // Handle application
}).listen(SocketAddress.domainSocketAddress("/var/tmp/myservice.sock"));
```

or for http:

```
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

As well as clients:

```
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

or for http:

```
HttpClient httpClient = vertx.createHttpClient();

// Only available on BSD and Linux
SocketAddress addr = SocketAddress.domainSocketAddress("/var/tmp/myservice.sock");

// Send request to the server
httpClient.request(HttpMethod.GET, addr, 8080, "localhost", "/", resp -> {
  // Process response
}).end();
```

## Security notes {#Security_notes}
Vert.x is a toolkit, not an opinionated framework where we force you to do things in a certain way. This gives you great power as a developer but with that comes great responsibility.

As with any toolkit, it’s possible to write insecure applications, so you should always be careful when developing your application especially if it’s exposed to the public (e.g. over the internet).

### Web applications {#Web_applications}
If writing a web application it’s highly recommended that you use Vert.x-Web instead of Vert.x core directly for serving resources and handling file uploads.

Vert.x-Web normalises the path in requests to prevent malicious clients from crafting URLs to access resources outside of the web root.

Similarly for file uploads Vert.x-Web provides functionality for uploading to a known place on disk and does not rely on the filename provided by the client in the upload which could be crafted to upload to a different place on disk.

Vert.x core itself does not provide such checks so it would be up to you as a developer to implement them yourself.

### Clustered event bus traffic {#Clustered_event_bus_traffic}
When clustering the event bus between different Vert.x nodes on a network, the traffic is sent un-encrypted across the wire, so do not use this if you have confidential data to send and your Vert.x nodes are not on a trusted network.

### Standard security best practices {#Standard_security_best_practices}
Any service can have potentially vulnerabilities whether it’s written using Vert.x or any other toolkit so always follow security best practice, especially if your service is public facing.

For example you should always run them in a DMZ and with an user account that has limited rights in order to limit the extent of damage in case the service was compromised.

## Vert.x Command Line Interface API {#Vert_x_Command_Line_Interface_API}
Vert.x Core provides an API for parsing command line arguments passed to programs. It’s also able to print help messages detailing the options available for a command line tool. Even if such features are far from the Vert.x core topics, this API is used in the `Launcher` class that you can use in *fat-jar* and in the `vertx` command line tools. In addition, it’s polyglot (can be used from any supported language) and is used in Vert.x Shell.

Vert.x CLI provides a model to describe your command line interface, but also a parser. This parser supports different types of syntax:

- POSIX like options (ie. `tar -zxvf foo.tar.gz`)
- GNU like long options (ie. `du --human-readable --max-depth=1`)
- Java like properties (ie. `java -Djava.awt.headless=true -Djava.net.useSystemProxies=true Foo`)
- Short options with value attached (ie. `gcc -O2 foo.c`)
- Long options with single hyphen (ie. `ant -projecthelp`)

Using the CLI api is a 3-steps process:

1. The definition of the command line interface
2. The parsing of the user command line
3. The query / interrogation

### Definition Stage {#Definition_Stage}
Each command line interface must define the set of options and arguments that will be used. It also requires a name. The CLI API uses the `Option` and `Argument` classes to describe options and arguments:

```
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

As you can see, you can create a new `CLI` using `CLI.create`. The passed string is the name of the CLI. Once created you can set the summary and description. The summary is intended to be short (one line), while the description can contain more details. Each option and argument are also added on the `CLI` object using the `addArgument` and `addOption` methods.

#### Options {#Options}
An `Option` is a command line parameter identified by a *key* present in the user command line. Options must have at least a long name or a short name. Long name are generally used using a `--` prefix, while short names are used with a single `-`. Options can get a description displayed in the usage (see below). Options can receive 0, 1 or several values. An option receiving 0 values is a `flag`, and must be declared using `setFlag`. By default, options receive a single value, however, you can configure the option to receive several values using `setMultiValued`:

```
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

Options can be marked as mandatory. A mandatory option not set in the user command line throws an exception during the parsing:

```
CLI cli = CLI.create("some-name")
    .addOption(new Option()
        .setLongName("mandatory")
        .setRequired(true)
        .setDescription("a mandatory option"));
```

Non-mandatory options can have a *default value*. This value would be used if the user does not set the option in the command line:

```
CLI cli = CLI.create("some-name")
    .addOption(new Option()
        .setLongName("optional")
        .setDefaultValue("hello")
        .setDescription("an optional option with a default value"));
```

An option can be *hidden* using the `setHidden` method. Hidden option are not listed in the usage, but can still be used in the user command line (for power-users).

If the option value is contrained to a fixed set, you can set the different acceptable choices:

```
CLI cli = CLI.create("some-name")
    .addOption(new Option()
        .setLongName("color")
        .setDefaultValue("green")
        .addChoice("blue").addChoice("red").addChoice("green")
        .setDescription("a color"));
```

Options can also be instantiated from their JSON form.

#### Arguments {#Arguments}
Unlike options, arguments do not have a *key* and are identified by their *index*. For example, in `java com.acme.Foo`, `com.acme.Foo` is an argument.

Arguments do not have a name, there are identified using a 0-based index. The first parameter has the index `0`:

```
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

If you don’t set the argument indexes, it computes it automatically by using the declaration order.

```
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

The `argName` is optional and used in the usage message.

As options, `Argument` can:

- be hidden using `setHidden`
- be mandatory using `setRequired`
- have a default value using `setDefaultValue`
- receive several values using `setMultiValued` - only the last argument can be multi-valued.

Arguments can also be instantiated from their JSON form.

#### Usage generation {#Usage_generation}
Once your `CLI` instance is configured, you can generate the *usage* message:

```
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

It generates an usage message like this one:

```
Usage: copy [-R] source target

A command line interface to copy files.

 -R,--directory   enables directory support
```

If you need to tune the usage message, check the `UsageMessageFormatter` class.

### Parsing Stage {#Parsing_Stage}
Once your `CLI` instance is configured, you can parse the user command line to evaluate each option and argument:

```
CommandLine commandLine = cli.parse(userCommandLineArguments);
```

The `parse` method returns a `CommandLine` object containing the values. By default, it validates the user command line and checks that each mandatory options and arguments have been set as well as the number of values received by each option. You can disable the validation by passing `false` as second parameter of `parse`. This is useful if you want to check an argument or option is present even if the parsed command line is invalid.

You can check whether or not the `CommandLine` is valid using `isValid`.

### Query / Interrogation Stage {#Query___Interrogation_Stage}
Once parsed, you can retrieve the values of the options and arguments from the `CommandLine` object returned by the `parse` method:

```
CommandLine commandLine = cli.parse(userCommandLineArguments);
String opt = commandLine.getOptionValue("my-option");
boolean flag = commandLine.isFlagEnabled("my-flag");
String arg0 = commandLine.getArgumentValue(0);
```

One of your option can have been marked as "help". If a user command line enabled a "help" option, the validation won’t failed, but give you the opportunity to check if the user asks for help:

```
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

### Typed options and arguments {#Typed_options_and_arguments}
The described `Option` and `Argument` classes are *untyped*, meaning that the only get String values. `TypedOption` and `TypedArgument` let you specify a *type*, so the (String) raw value is converted to the specified type.

Instead of `Option` and `Argument`, use `TypedOption` and `TypedArgument` in the `CLI` definition:

```
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
        .setIndex(0)
        .setDescription("The destination")
        .setArgName("target"));
```

Then you can retrieve the converted values as follows:

```
CommandLine commandLine = cli.parse(userCommandLineArguments);
boolean flag = commandLine.getOptionValue("R");
File source = commandLine.getArgumentValue("source");
File target = commandLine.getArgumentValue("target");
```

The vert.x CLI is able to convert to classes:

- having a constructor with a single `String` argument, such as `File` or `JsonObject`
- with a static `from` or `fromString` method
- with a static `valueOf` method, such as primitive types and enumeration

In addition, you can implement your own `Converter` and instruct the CLI to use this converter:

```
CLI cli = CLI.create("some-name")
    .addOption(new TypedOption<Person>()
        .setType(Person.class)
        .setConverter(new PersonConverter())
        .setLongName("person"));
```

For booleans, the boolean values are evaluated to `true`: `on`, `yes`, `1`, `true`.

If one of your option has an `enum` as type, it computes the set of choices automatically.

### Using annotations {#Using_annotations}
You can also define your CLI using annotations. Definition is done using annotation on the class and on *setter* methods:

```
&#64;Name("some-name")
&#64;Summary("some short summary.")
&#64;Description("some long description")
public class AnnotatedCli {

 private boolean flag;
 private String name;
 private String arg;

&#64;Option(shortName = "f", flag = true)
public void setFlag(boolean flag) {
  this.flag = flag;
}

&#64;Option(longName = "name")
public void setName(String name) {
  this.name = name;
}

&#64;Argument(index = 0)
public void setArg(String arg) {
 this.arg = arg;
}
}
```

Once annotated, you can define the `CLI` and inject the values using:

```
CLI cli = CLI.create(AnnotatedCli.class);
CommandLine commandLine = cli.parse(userCommandLineArguments);
AnnotatedCli instance = new AnnotatedCli();
CLIConfigurator.inject(commandLine, instance);
```

## The vert.x Launcher {#The_vert_x_Launcher}
The vert.x `Launcher` is used in *fat jar* as main class, and by the `vertx` command line utility. It executes a set of *commands* such as *run*, *bare*, *start*…

### Extending the vert.x Launcher {#Extending_the_vert_x_Launcher}
You can extend the set of command by implementing your own `Command` (in Java only):

```
&#64;Name("my-command")
&#64;Summary("A simple hello command.")
public class MyCommand extends DefaultCommand {

 private String name;

 &#64;Option(longName = "name", required = true)
 public void setName(String n) {
   this.name = n;
 }

 &#64;Override
 public void run() throws CLIException {
   System.out.println("Hello " + name);
 }
}
```

You also need an implementation of `CommandFactory`:

```
public class HelloCommandFactory extends DefaultCommandFactory<HelloCommand> {
 public HelloCommandFactory() {
  super(HelloCommand.class);
 }
}
```

Then, create the `src/main/resources/META-INF/services/io.vertx.core.spi.launcher.CommandFactory` and add a line indicating the fully qualified name of the factory:

```
io.vertx.core.launcher.example.HelloCommandFactory
```

Builds the jar containing the command. Be sure to includes the SPI file (`META-INF/services/io.vertx.core.spi.launcher.CommandFactory`).

Then, place the jar containing the command into the classpath of your fat-jar (or include it inside) or in the `lib` directory of your vert.x distribution, and you would be able to execute:

```
vertx hello vert.x
java -jar my-fat-jar.jar hello vert.x
```

### Using the Launcher in fat jars {#Using_the_Launcher_in_fat_jars}
To use the `Launcher` class in a *fat-jar* just set the `Main-Class` of the *MANIFEST* to `io.vertx.core.Launcher`. In addition, set the `Main-Verticle` *MANIFEST* entry to the name of your main verticle.

By default, it executed the `run` command. However, you can configure the default command by setting the `Main-Command` *MANIFEST* entry. The default command is used if the *fat jar* is launched without a command.

### Sub-classing the Launcher {#Sub_classing_the_Launcher}
You can also create a sub-class of `Launcher` to start your application. The class has been designed to be easily extensible.

A `Launcher` sub-class can:

- customize the vert.x configuration in `beforeStartingVertx`
- retrieve the vert.x instance created by the "run" or "bare" command by overriding `afterStartingVertx`
- configure the default verticle and command with `getMainVerticle` and `getDefaultCommand`
- add / remove commands using `register` and `unregister`

### Launcher and exit code {#Launcher_and_exit_code}
When you use the `Launcher` class as main class, it uses the following exit code:

- `0` if the process ends smoothly, or if an uncaught error is thrown
- `1` for general purpose error
- `11` if Vert.x cannot be initialized
- `12` if a spawn process cannot be started, found or stopped. This error code is used by the `start` and `stop` command
- `14` if the system configuration is not meeting the system requirement (shc as java not found)
- `15` if the main verticle cannot be deployed

## Configuring Vert.x cache {#Configuring_Vert_x_cache}
When Vert.x needs to read a file from the classpath (embedded in a fat jar, in a jar form the classpath or a file that is on the classpath), it copies it to a cache directory. The reason behind this is simple: reading a file from a jar or from an input stream is blocking. So to avoid to pay the price every time, Vert.x copies the file to its cache directory and reads it from there every subsequent read. This behavior can be configured.

First, by default, Vert.x uses `$CWD/.vertx` as cache directory. It creates a unique directory inside this one to avoid conflicts. This location can be configured by using the `vertx.cacheDirBase` system property. For instance if the current working directory is not writable (such as in an immutable container context), launch your application with:

```
vertx run my.Verticle -Dvertx.cacheDirBase=/tmp/vertx-cache
# or
java -jar my-fat.jar vertx.cacheDirBase=/tmp/vertx-cache
```

| IMPORTANT | the directory must be **writable**. |
| --------- | ----------------------------------- |
|           |                                     |

When you are editing resources such as HTML, CSS or JavaScript, this cache mechanism can be annoying as it serves only the first version of the file (and so you won’t see your edits if you reload your page). To avoid this behavior, launch your application with `-Dvertx.disableFileCaching=true`. With this setting, Vert.x still uses the cache, but always refresh the version stored in the cache with the original source. So if you edit a file served from the classpath and refresh your browser, Vert.x reads it from the classpath, copies it to the cache directory and serves it from there. Do not use this setting in production, it can kill your performances.

Finally, you can disable completely the cache by using `-Dvertx.disableFileCPResolving=true`. This setting is not without consequences. Vert.x would be unable to read any files from the classpath (only from the file system). Be very careful when using this settings.

Last updated 2019-10-17 18:44:38 CEST

