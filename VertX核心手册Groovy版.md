# VertX核心手册 Groovy版

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

如果使用的是Maven或Gradle，请将以下依赖项添加到项目描述符的*dependencies*部分，以访问Vert.x Core API并启用Groovy支持：

- Maven (在您的`pom.xml`中):

```xml
<dependency>
  <groupId>io.vertx</groupId>
  <artifactId>vertx-core</artifactId>
  <version>3.8.2</version>
</dependency>

<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-lang-groovy</artifactId>
 <version>3.8.2</version>
</dependency>
```

- Gradle (在您的`build.gradle`文件中):

```groovy
compile "io.vertx:vertx-core:3.8.2"
compile "io.vertx:vertx-lang-groovy:3.8.2"
```

让我们讨论core中的不同概念和功能。

<a name="1___你流利的吗_"></a>
## 你流利的吗?
您可能已经注意到，在前面的示例中使用了**fluent** API。

在`fluent` API中，可以将多个方法调用链接在一起。例如:

```groovy
request.response().putHeader("Content-Type", "text/plain").write("some text").end()
```

这是整个Vert.x API的通用模式，因此请习惯使用它。

像这样的链接调用允许您编写稍微不那么冗长的代码。当然，如果您不喜欢fluent方法**我们不会强迫您**这样做，如果您愿意，您可以愉快地忽略它，并像这样编写您的代码:

```groovy
def response = request.response()
response.putHeader("Content-Type", "text/plain")
response.write("some text")
response.end()
```

<a name="2___别打给我们，我们会打给你的。"></a>
## 别打给我们，我们会打给你的。
Vert.x API在很大程度上是*事件驱动*的。 这意味着当您感兴趣的Vert.x中发生任何事情时，Vert.x会通过向您发送事件来呼叫您。

一些示例事件是：

- 计时器已触发
- 一些数据已经到达套接字
- 从磁盘读取了一些数据
- 发生异常
- HTTP服务器已收到请求

您可以通过向Vert.x API提供*handlers*来处理事件。 例如，要每秒接收一个计时器事件，您将执行以下操作：

```groovy
vertx.setPeriodic(1000, { id ->
  // This handler will get called every second
  println("timer fired!")
})
```

或接收HTTP请求：

```groovy
// Respond to each http request with "Hello World"
server.requestHandler({ request ->
  // This handler will be called every time an HTTP request is received at the server
  request.response().end("hello world!")
})
```

过了一段时间，当Vert.x有一个事件传递给您的处理程序时，Vert.x会**异步地**调用它。

这使我们想到了Vert.x中的一些重要概念：

<a name="3___不要阻塞我！"></a>
## 不要阻塞我！
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

<a name="4___反应器和多反应器"></a>
## 反应器和多反应器
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

<a name="5___黄金法则___不要阻塞事件循环"></a>
## 黄金法则 - 不要阻塞事件循环
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

<a name="6___运行阻塞的代码"></a>
## 运行阻塞的代码
在一个理想的世界中，不会有战争或饥饿，所有API都是异步编写的，小兔子会与小羊羔在阳光明媚的绿色草地上携手并进。

**但是……现实世界并非如此。 （您最近看过新闻吗？）**

事实是，很多（如果不是大多数的话）库，尤其是在JVM生态系统中，大多数库具有同步API，并且许多方法可能会阻塞。 JDBC API是一个很好的例子-它固有地是同步的，无论尝试多努力，Vert.x都无法在其上撒上魔术般的灰尘以使其异步。

我们不会在一夜之间将所有内容重写为异步的，因此我们需要为您提供一种在Vert.x应用程序中安全使用“传统”阻塞API的方法。

如前所述，您不能直接从事件循环中调用阻塞操作，因为这会阻止它执行任何其他有用的工作。怎么做呢?

这是通过调用`executeBlocking`来完成的，该代码同时指定了要执行的阻塞代码和在阻塞代码执行后异步调用的结果处理程序。

```groovy
vertx.executeBlocking({ promise ->
  // Call some blocking API that takes a significant amount of time to return
  def result = someAPI.blockingMethod("hello")
  promise.complete(result)
}, { res ->
  println("The result is: ${res.result()}")
})
```

------
> **警告:** 阻塞代码应阻塞一段合理的时间（例如: 不超过几秒钟）。 排除了长阻塞操作或轮询操作（在循环中以阻塞方式轮询事件的线程）。 当阻塞操作持续10秒钟以上时，阻塞线程检查器将在控制台上显示一条消息。 长阻塞操作应使用由应用程序管理的专用线程，该线程可以使用事件总线或`runOnContext`与verticles交互。
------

默认情况下，如果从同一上下文（例如，相同的Verticle实例）中多次调用了executeBlocking，则不同的executeBlocking将*串行*执行（即一个接一个）。

如果您不关心排序，您可以调用`executeBlocking`，将`false`指定为`ordered`的参数。在这种情况下，任何executeBlocking都可以在工作池上并行执行。

运行阻塞代码的另一种方法是使用[worker verticle](https://vertx.io/docs/vertx-core/groovy/#worker_verticles)

一个`worker verticle`总是用来自工作池的线程执行。

默认情况下，阻塞代码是在Vert.x工作池上执行的，该工作池使用setWorkerPoolSize配置。

可以出于其他目的创建其他池：

```groovy
def executor = vertx.createSharedWorkerExecutor("my-worker-pool")
executor.executeBlocking({ promise ->
  // Call some blocking API that takes a significant amount of time to return
  def result = someAPI.blockingMethod("hello")
  promise.complete(result)
}, { res ->
  println("The result is: ${res.result()}")
})
```

不需要`worker executor`时，必须将其关闭：

```groovy
executor.close()
```

当使用相同的名称创建多个工作线程时，他们将共享相同的池。 关闭所有使用该工作池的工作执行程序时，该工作池将被销毁。

在Verticle中创建executor时，当取消部署Verticle时，Vert.x会自动为您关闭它。

创建时可以配置Worker executors：

```groovy
//
// 10 threads max
def poolSize = 10

// 2 minutes
def maxExecuteTime = 2
def maxExecuteTimeUnit = TimeUnit.MINUTES

def executor = vertx.createSharedWorkerExecutor("my-worker-pool", poolSize, maxExecuteTime, maxExecuteTimeUnit)
```

------
> **注意:**  在创建工作池时设置配置
> 
------

<a name="7___异步协调"></a>
## 异步协调
多个异步结果的协调可以通过Vert.x的`futures`来实现。 它支持并发组合（并行运行多个异步操作）和顺序组合（链异步操作）。

<a name="8____并发组合"></a>
### 并发组合
`CompositeFuture.all`接受几个futures参数(最多6个)，返回一个在所有future都*succeeded*时“成功”的future，在至少一个future*failed*时“失败”的future:

```groovy
def httpServerFuture = Future.future({ promise ->
  httpServer.listen(promise)
})

def netServerFuture = Future.future({ promise ->
  netServer.listen(promise)
})

CompositeFuture.all(httpServerFuture, netServerFuture).setHandler({ ar ->
  if (ar.succeeded()) {
    // All servers started
  } else {
    // At least one server failed
  }
})
```

操作并发运行，在合成完成时调用附加到返回的future的`Handler`。当其中一个操作失败时(已传递的future中的一个被标记为失败)，结果的future 也被标记为失败。当所有操作都成功时，产生的future就成功地完成了。

或者，您可以传递一个futures列表(可能是空的):

```groovy
CompositeFuture.all([future1, future2, future3])
```

当`all`组合*等待*直到所有futures成功(或一个失败)时，`any`组合*等待*第一个成功的futures。 `CompositeFuture.any`接受几个future参数（最多6个），并返回一个future，当其中一个future成为成功时，则成功，而当所有Future都失败时，则失败:

```groovy
CompositeFuture.any(future1, future2).setHandler({ ar ->
  if (ar.succeeded()) {
    // At least one is succeeded
  } else {
    // All failed
  }
})
```

也可以使用future列表：

```groovy
CompositeFuture.any([f1, f2, f3])
```

`join`组合*等待*直到所有future完成，要么成功要么失败。`CompositeFuture.join`接受几个future参数(最多6个)，并返回一个future，当所有future都成功时，该future就成功；而当所有future都完成且其中至少一个失败时，则失败：

```groovy
CompositeFuture.join(future1, future2, future3).setHandler({ ar ->
  if (ar.succeeded()) {
    // All succeeded
  } else {
    // All completed and at least one failed
  }
})
```

也可以使用future列表：

```groovy
CompositeFuture.join([future1, future2, future3])
```

<a name="9____顺序组合"></a>
### 顺序组合
当`all`和`any`实现并发组合时，`compose`可用于链接futures(即顺序组合)。 

```groovy
def fs = vertx.fileSystem()

def fut1 = Future.future({ promise ->
  fs.createFile("/foo", promise)
})

def startFuture = fut1.compose({ v ->
  // When the file is created (fut1), execute this:
  return Future.future({ promise ->
    fs.writeFile("/foo", Buffer.buffer(), promise)
  })
}).compose({ v ->
  // When the file is written (fut2), execute this:
  return Future.future({ promise ->
    fs.move("/foo", "/bar", promise)
  })
})
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

<a name="10____编写Verticles"></a>
### 编写Verticles
在Groovy中创建verticles有两种选择：

一个普通的Groovy脚本, 或者一个实现`Verticle`接口或扩展`AbstractVerticle`类的Groovy类

例如，下一个片段是有效的Groovy的verticle片段：

```groovy
println "Hello from vertx"
```

在部署时，默认情况下，Vert.x执行脚本。 可选地，您的脚本可以提供`startVertx`和`stopVertx`方法。 这些方法在verticle开始和停止时分别被调用：

```groovy
void vertxStart() {
  println "starting"
}

void vertxStop() {
  println "stopping"
}
```

另外，您可以扩展AbstractVerticle类并实现start和stop方法：

```groovy
import io.vertx.core.AbstractVerticle;

public class HelloWorldHttpVerticle extends AbstractVerticle {

  public void start() {
   println("Starting")
  }

  public void stop() {
    println("Stopping")
  }
}
```

当Vert.x部署该verticle时，它将调用`start`方法，当该方法完成后，该verticle将被视为已启动。

您也可以选择覆盖stop方法。 Vert.x将在取消部署verticle时调用该方法，并且在该方法完成后，该verticle将被视为已停止。

<a name="11____从一个verticle访问vertx实例"></a>
### 从一个verticle访问vertx实例
无论您使用哪种方式来实现您的verticle，都可以使用`vertex`变量/字段访问vert.x实例。

在Groovy脚本中访问vert.x实例

```groovy
vertx.deployVerticle("another_verticle.rb")
```

访问Groovy类中的vert.x实例

```groovy
import io.vertx.lang.groovy.GroovyVerticle;

public class HelloWorldHttpVerticle extends GroovyVerticle {

  public void start() {
    vertx.deployVerticle("another_verticle.js")
  }
}
```

<a name="12____异步Verticle启动和停止"></a>
### 异步Verticle启动和停止
有时，您需要在启动verticle时做一些事情，而这需要一些时间，并且您不希望在这种情况发生之前就考虑将verticle部署。 例如，您可能想在start方法中部署其他verticle。

您不能阻止在开始方法中部署其他verticle，因为那样会破坏[黄金规则](https://vertx.io/docs/vertx-core/groovy/#golden_rule)。

那你怎么做呢？

做到这一点的方法是实现**异步**的start方法。 此版本的方法以Future为参数。当该方法返回时，将**不会**认为verticle已部署。

一段时间后，当您完成了所有需要做的事情（例如，启动其他verticle）后，就可以在Future上调用complete（或失败）以表明您已完成。 同样，也有stop方法的异步版本。 如果您要进行一些需要一些时间的verticle清理，则可以使用此方法。

当您的verticle被实现为脚本时，异步启动和停止的实现如下：

```groovy
import io.vertx.core.Future

void vertxStart(Future<Void> future) {
  println "starting"
  vertx.deployVerticle("v.rb", { res ->
    if (res.succeeded()) {
      future.complete()
    } else {
      future.fail()
    }
  })
}

void vertxStop(Future<Void> future) {
  println "stopping"
  future.complete()
}
```

如果您的verticle 扩展了`AbstractVerticle`，则将覆盖`start`和`stop`方法：

```groovy
import io.vertx.core.Future
import io.vertx.core.AbstractVerticle

public class HelloWorldHttpVerticle extends AbstractVerticle {

  public void start(Future<Void> future) {
    println "starting"
    vertx.deployVerticle("v.rb",
    { res ->
      if (res.succeeded()) {
        future.complete()
      } else {
        future.fail()
      }
    })
   }

  public void stop(Future<Void> future) {
   println("stopping")
   future.complete()
  }
}
```

------
> **注意:**  您无需通过verticle的stop方法手动取消部署由verticle开始的子verticle。 取消部署父级时，Vert.x会自动取消部署所有子verticle。
------

<a name="13____API与先前版本的更改"></a>
### API与先前版本的更改
用于Groovy的Vert.x已在Vert.x 3.4.x中进行了修订，并提供了针对先前API编写的Verticles的自动迁移路径。

Vert.x 3.5.0假定应用程序已迁移到新API。

<a name="14____Verticle类型"></a>
### Verticle类型
共有三种不同类型的verticle：

- Standard Verticles(标准Verticles)

  这些是最常见和最有用的类型-它们始终使用事件循环线程执行。 我们将在下一部分中对此进行更多讨论。

- Worker Verticles(工作Verticles)

  这些使用工作池中的线程运行。 一个实例永远不会由多个线程并发执行。

- Multi-threaded worker verticles(多线程工作Verticles)

  这些使用工作池中的线程运行。 一个实例可以由多个线程并发执行。

<a name="15____标准Verticles"></a>
### 标准Verticles
标准verticles在创建时会分配一个事件循环线程，并使用该事件循环调用start方法。 当您从事件循环调用任何其他在核心API上使用处理程序的方法时，Vert.x将保证这些处理程序在被调用时将在同一事件循环上执行。

这意味着我们可以保证您的verticle实例中的所有代码始终在同一事件循环上执行（只要您不创建自己的线程并调用它！）。

这意味着您可以将应用程序中的所有代码编写为单线程，让Vert.x来处理线程和可伸缩性。 不再需要担心同步和易失性，并且还避免了其他许多竞争情况和死锁的情况，这些情况在进行手工“传统”多线程应用程序开发时非常普遍。

<a name="16____工作Verticles"></a>
### 工作Verticles
`worker verticle`与标准verticle一样，但是它使用Vert.x的`worker thread pool`线程池中的线程执行，而不是使用事件循环。

Worker verticles旨在用于调用阻塞代码，因为它们不会阻止任何事件循环。

如果您不想使用`worker verticle`来运行阻塞代码，则还可以在事件循环上直接运行[inline blocking code](https://vertx.io/docs/vertx-core/groovy/#blocking_code) 。

如果要将一个verticle 部署为一个worker verticle，可以使用`setWorker`来完成。

```groovy
def options = [
  worker:true
]
vertx.deployVerticle("com.mycompany.MyOrderProcessorVerticle", options)
```

Vert.x永远不会由多个线程同时执行worker verticle实例，但可以在不同时间由不同线程执行。

<a name="17_____多线程工作Verticles"></a>
#### 多线程工作Verticles
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

```groovy
def options = [
  worker:true,
  instances:5,
  workerPoolName:"the-specific-pool",
  workerPoolSize:5
]
vertx.deployVerticle("com.mycompany.MyOrderProcessorVerticle", options)
```

另外，您可以创建一个常规的verticle并用多个`executeBlocking`将您的阻塞代码包装，并将`ordered`标志设置为`false`：

```groovy
vertx.eventBus().consumer("foo", { msg ->
  vertx.executeBlocking({ promise ->
    // Invoke blocking code with received message data
    promise.complete(someresult)
  }, false, { ar ->
    // Handle result, e.g. reply to the message
  })
})
```

<a name="18____以编程方式部署verticles"></a>
### 以编程方式部署verticles
您可以使用`deployVerticle`方法之一来部署一个verticle，指定一个Verticle名称，也可以传入已经创建的Verticle实例。

------
> **注意:**  部署Verticle实例仅限Java。
> 
------

```groovy
def myVerticle = new examples.CoreExamples.MyVerticle()
vertx.deployVerticle(myVerticle)
```

您还可以通过指定verticle **name**来部署顶点。

verticle名称用于查找特定的`VerticleFactory`，该实例将用于实例化实际的verticle实例。

不同的Version工厂可用于以不同的语言实例化Verticle，并且出于各种其他原因，例如加载服务以及在运行时从Maven获取Verticle。

这使您可以部署Vert.x支持的任何其他语言编写的Verticles。

这是部署一些不同类型的verticles的示例：

```groovy
// Deploy a Java verticle - the name is the fully qualified class name of the verticle class
vertx.deployVerticle("com.mycompany.MyOrderProcessorVerticle")

// Deploy a JavaScript verticle
vertx.deployVerticle("verticles/myverticle.js")

// Deploy a Ruby verticle verticle
vertx.deployVerticle("verticles/my_verticle.rb")
```

<a name="19____将verticle名称映射到verticle工厂的规则"></a>
### 将verticle名称映射到verticle工厂的规则
当使用名称部署verticle时，该名称用于选择将实例化该verticle的实际verticle工厂。

verticle名称可以有一个前缀-这是一个字符串，后跟一个冒号，如果存在的话将用于查找工厂，例如
```groovy
js:foo.js // Use the JavaScript verticle factory 

groovy:com.mycompany.SomeGroovyCompiledVerticle // Use the Groovy 

verticle factory service:com.mycompany:myorderservice // Uses the service verticle factory
```

如果没有前缀，Vert.x将查找后缀并使用后缀查找工厂，例如

`foo.js`将使用JavaScript verticle工厂`SomeScript.groovy`将使用Groovy verticle工厂

如果没有前缀或后缀，则Vert.x将假定它是Java完全限定的类名（FQCN），然后尝试实例化该名称。

<a name="20____Verticle工厂位于哪里_"></a>
### Verticle工厂位于哪里?
大多数Verticle工厂都从类路径加载并在Vert.x启动时注册。

如果愿意，您还可以使用`registerVerticleFactory`和`unregisterVerticleFactory`以编程方式注册和注销verticle工厂。

<a name="21____等待部署完成"></a>
### 等待部署完成
Verticle部署是异步的，可能会在部署调用返回后的一段时间内完成。

如果你想在部署完成时得到通知，你可以部署指定一个完成处理程序:

```groovy
vertx.deployVerticle("com.mycompany.MyOrderProcessorVerticle", { res ->
  if (res.succeeded()) {
    println("Deployment id is: ${res.result()}")
  } else {
    println("Deployment failed!")
  }
})
```

如果部署成功，将向完成处理程序传递包含部署ID字符串的结果。

如果希望取消部署，可以稍后使用此部署ID。

<a name="22____取消verticle部署"></a>
### 取消verticle部署
可以使用`undeploy`取消部署。

取消部署本身是异步的，因此，如果要在完成取消部署时收到通知，可以部署指定完成处理程序：

```groovy
vertx.undeploy(deploymentID, { res ->
  if (res.succeeded()) {
    println("Undeployed ok")
  } else {
    println("Undeploy failed!")
  }
})
```

<a name="23____指定verticle实例数"></a>
### 指定verticle实例数
使用verticle名称部署verticle时，可以指定要部署的verticle实例的数量：

```groovy
def options = [
  instances:16
]
vertx.deployVerticle("com.mycompany.MyOrderProcessorVerticle", options)
```

这对于轻松跨多个内核进行扩展很有用。 例如，您可能有一个要部署的Web服务器版本，并且在您的计算机上有多个核心，因此您想部署多个实例以利用所有核心。

<a name="24____将配置传递到verticle"></a>
### 将配置传递到verticle
可以将Map形式的配置在部署时传递给verticle：

```groovy
def config = [
  name:"tim",
  directory:"/blah"
]
def options = [ "config" : config ];
vertx.deployVerticle("com.mycompany.MyOrderProcessorVerticle", options);
```

然后可以通过Context使用此配置。该配置作为Map对象返回，因此您可以按以下方式检索数据：

```groovy
println vertx.getOrCreateContext().config()["name"]
```

------
> **NOTE:**  The configuration can also be a `JsonObject` object. 
> 
------

<a name="25____在Verticle中访问环境变量"></a>
### 在Verticle中访问环境变量
使用Java API可访问环境变量和系统属性：

```groovy
println System.getProperty("foo")
println System.getenv("HOME")
```

<a name="26____Verticle隔离组"></a>
### Verticle隔离组
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

```groovy
def options = [
  isolationGroup:"mygroup"
]
options.isolatedClasses = ["com.mycompany.myverticle.*", "com.mycompany.somepkg.SomeClass", "org.somelibrary.*"]
vertx.deployVerticle("com.mycompany.myverticle.VerticleClass", options)
```

<a name="27____高可用性"></a>
### 高可用性
可以在启用高可用性（HA）的情况下部署Verticles。 在这种情况下，当将一个verticle部署在突然死亡的vert.x实例上时，该verticle 将重新部署到集群中的另一个vert.x实例上。

要运行启用了高可用性的Verticle，只需附加`-ha`开关即可：

```groovy
vertx run my-verticle.js -ha
```

启用高可用性时，无需添加`-cluster`。

[高可用性和故障转移](https://vertx.io/docs/vertx-core/groovy/#_high_availability_and_fail_over)部分中有关高可用性功能和配置的更多详细信息。

<a name="28____从命令行运行Verticles"></a>
### 从命令行运行Verticles
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

<a name="29____导致Vert_x退出"></a>
### 导致Vert.x退出
Vert.x实例维护的线程不是守护程序线程，因此它们将阻止JVM退出。

如果你正在嵌入Vert.x，并且你已经完成了它，你可以调用`close`来关闭它。

这将关闭所有内部线程池并关闭其他资源，并允许JVM退出。

<a name="30____上下文对象"></a>
### 上下文对象
当Vert.x向处理程序提供事件或调用`Verticle`的start或stop方法时，执行将与`Context` 关联。 通常，上下文是**事件循环上下文**，并绑定到特定的事件循环线程。 因此，针对该上下文的执行始终在完全相同的事件循环线程上进行。 对于工作程序verticles和运行内联阻塞代码的情况，工作程序上下文将与执行关联，该上下文将使用工作程序线程池中的线程。

要获取上下文，请使用`getOrCreateContext`方法：

```groovy
def context = vertx.getOrCreateContext()
```

如果当前线程具有与之关联的上下文，则它将重用上下文对象。 如果不是，则创建新的上下文实例。 您可以测试检索到的上下文的*类型*：

```groovy
def context = vertx.getOrCreateContext()
if (context.isEventLoopContext()) {
  println("Context attached to Event Loop")
} else if (context.isWorkerContext()) {
  println("Context attached to Worker Thread")
} else if (context.isMultiThreadedWorkerContext()) {
  println("Context attached to Worker Thread - multi threaded worker")
} else if (!Context.isOnVertxThread()) {
  println("Context not attached to a thread managed by vert.x")
}
```

检索上下文对象后，可以在此上下文中异步运行代码。 换句话说，您提交的任务最终将在相同的上下文中运行，但是稍后：

```groovy
vertx.getOrCreateContext().runOnContext({ v ->
  println("This will be executed asynchronously in the same context")
})
```

当多个处理程序在同一上下文中运行时，它们可能希望共享数据。上下文对象提供了在上下文中存储和检索共享数据的方法。例如，它可以让你传递数据到一些动作运行`runOnContext`:

```groovy
def context = vertx.getOrCreateContext()
context.put("data", "hello")
context.runOnContext({ v ->
  def hello = context.get("data")
})
```

上下文对象还允许您使用`config`方法访问verticle配置。 检查[将配置传递到verticle位置](https://vertx.io/docs/vertx-core/groovy/#_passing_configuration_to_a_verticle)部分以获取有关此配置的更多详细信息。

<a name="31____执行定期和延迟的操作"></a>
### 执行定期和延迟的操作
在Vert.x中，很常见的是要延迟或定期执行操作。

在标准verticle中，您不能只是使线程休眠以引入延迟，因为这会阻塞事件循环线程。

而是使用Vert.x计时器。 计时器可以是**一次性**或**定期**。 我们将讨论两者

<a name="32_____单次计时器"></a>
#### 单次计时器
一次性计时器在一定的延迟(以毫秒为单位)之后调用事件处理程序。

使用`setTimer`方法传递延迟和处理程序后，设置要触发的计时器

```groovy
def timerID = vertx.setTimer(1000, { id ->
  println("And one second later this is printed")
})

println("First this is printed")
```

返回值是唯一的计时器ID，以后可用于取消计时器。 处理程序还传递了计时器ID。

<a name="33_____周期性的计时器"></a>
#### 周期性的计时器
您还可以使用`setPeriodic`将计时器设置为定期触发。

将会有一个与周期相等的初始延迟。

`setPeriodic`的返回值是唯一的计时器ID（长整数）。 如果需要取消计时器，可以稍后使用。

传递到计时器事件处理程序中的参数也是唯一的计时器ID：

请记住，计时器将定期触发。如果您的定期任务需要很长时间才能完成，那么计时器事件可能会连续运行，甚至更糟:堆积起来。

在这种情况下，您应该考虑改用`setTimer`。 任务完成后，您可以设置下一个计时器。

```groovy
def timerID = vertx.setPeriodic(1000, { id ->
  println("And every second this is printed")
})

println("First this is printed")
```

<a name="34_____取消计时器"></a>
#### 取消计时器
要取消定期计时器，请调用`cancelTimer`并指定计时器ID。 例如：

```groovy
vertx.cancelTimer(timerID)
```

<a name="35_____verticles中的自动清理"></a>
#### verticles中的自动清理
如果您是从verticle内部创建计时器，则取消部署verticles时，这些计时器将自动关闭。

<a name="36____Verticle工作池"></a>
### Verticle工作池
Verticles使用Vert.x工作池执行阻塞操作，即`executeBlocking`或工作verticle。

可以在部署选项中指定其他工作池：

```groovy
vertx.deployVerticle("the-verticle", [
  workerPoolName:"the-specific-pool"
])
```

<a name="37___事件总线"></a>
## 事件总线
`event bus(事件总线)`是Vert.x的**nervous system(经系统)**。

每个Vert.x实例都有一个事件总线实例，可以使用`eventBus`方法获得它。

事件总线允许您的应用程序的不同部分相互通信，而不管它们是用什么语言编写的，以及它们是在相同的Vert.x实例中还是在不同的Vert.x实例中。

它甚至可以桥接，以允许在浏览器中运行的客户端JavaScript在同一事件总线上进行通信。

事件总线构成了跨越多个服务器节点和多个浏览器的分布式对等消息传递系统。

事件总线支持发布/订阅，点对点和请求-响应消息传递。

事件总线API非常简单。 它基本上涉及注册处理程序，注销处理程序以及发送和发布消息。

首先是一些理论：

<a name="38____理论"></a>
### 理论
<a name="39_____地址"></a>
#### 地址
消息在事件总线上发送到**address(地址)**address**。

Vert.x不需要任何花哨的寻址方案。 在Vert.x中，地址只是一个字符串。 任何字符串均有效。 但是，明智的做法是使用某种方案，例如使用句点来分隔名称空间。

有效地址的一些示例是`europe.news.feed1`，`acme.games.pacman`，`sausages`和`X`。

<a name="40_____处理程序"></a>
#### 处理程序
消息由处理程序接收。 您在地址注册处理程序。

可以在同一地址注册许多不同的处理程序。

单个处理程序可以在许多不同的地址上注册。

<a name="41_____发布_订阅消息"></a>
#### 发布/订阅消息
事件总线支持**发布**消息。

消息被发布到一个地址。 发布意味着将消息传递给在该地址注册的所有处理程序。

这是熟悉的**发布/订阅**消息传递模式。

<a name="42_____点对点和请求响应消息传递"></a>
#### 点对点和请求响应消息传递
事件总线还支持**point-to-point(点对点)**消息传递。

消息被发送到一个地址。 然后，Vert.x会将它们路由到在该地址注册的处理程序之一。

如果在该地址注册了多个处理程序，那么将使用非严格的循环算法选择一个。

使用点对点消息传递，可以在发送消息时指定可选的应答处理程序。

当消息被接收方接收并处理后，接收方可以选择回复消息。如果这样做，将调用应答处理程序。

当发件人收到回复时，也可以回复它。 可以无限次重复此操作，并允许在两个不同的verticles之间建立对话框。

这是一种常见的消息传递模式，称为**请求-响应**模式。

<a name="43_____尽力递送"></a>
#### 尽力递送
Vert.x会尽力传递消息，并且不会有意识地将其丢弃。 这称为**best-effort(尽力而为)**交付。

但是，如果事件总线的全部或部分发生故障，则可能会丢失消息。

如果您的应用程序关心丢失的消息，则应将处理程序编码为幂等，而发送方应在恢复后重试。

<a name="44_____消息类型"></a>
#### 消息类型
开箱即用的Vert.x允许将任何原始/简单类型，字符串或`buffers(缓冲区)`作为消息发送。

但是，在Vert.x中以[JSON](https://json.org/)发送消息是一种惯例

JSON非常容易以Vert.x支持的所有语言创建，读取和解析，因此它已成为Vert.x的一种*通用语言*。

但是，如果您不想这么做，则不必强制使用JSON。

事件总线非常灵活，并且还支持通过事件总线发送任意对象。 您可以通过为要发送的对象定义一个“编解码器”来实现。

<a name="45____事件总线API"></a>
### 事件总线API
让我们进入API。

<a name="46_____获取事件总线"></a>
#### 获取事件总线
您可以获得对事件总线的引用，如下所示：

```groovy
def eb = vertx.eventBus()
```

每个Vert.x实例只有一个事件总线实例。

<a name="47_____注册处理程序"></a>
#### 注册处理程序
注册处理程序的最简单方法是使用`consumer`。 这是一个例子：

```groovy
def eb = vertx.eventBus()

eb.consumer("news.uk.sport", { message ->
  println("I have received a message: ${message.body()}")
})
```

当消息到达您的处理程序时，将调用您的处理程序，并传递`message`。

从调用返回给Consumer()的对象是`MessageConsumer`的实例。

此对象随后可用于注销处理程序，或将处理程序用作流。

或者，您可以使用`consumer`来返回一个没有设置任何处理程序的MessageConsumer，然后在此基础上设置处理程序。例如：

```groovy
def eb = vertx.eventBus()

def consumer = eb.consumer("news.uk.sport")
consumer.handler({ message ->
  println("I have received a message: ${message.body()}")
})
```

在集群事件总线上注册处理程序时，注册到集群的所有节点可能需要一些时间。

如果您希望在完成时得到通知，您可以在MessageConsumer对象上注册一个`completion handler(完成处理程序)`。

```groovy
consumer.completionHandler({ res ->
  if (res.succeeded()) {
    println("The handler registration has reached all nodes")
  } else {
    println("Registration failed!")
  }
})
```

<a name="48_____取消注册处理程序"></a>
#### 取消注册处理程序
要取消注册处理程序，请调用`unregister`。

如果您在集群事件总线上，则注销可能需要一些时间才能在节点上传播。 如果您想在完成时收到通知，请使用`unregister`。

```groovy
consumer.unregister({ res ->
  if (res.succeeded()) {
    println("The handler un-registration has reached all nodes")
  } else {
    println("Un-registration failed!")
  }
})
```

<a name="49_____发布消息"></a>
#### 发布消息
发布消息很简单。 只需使用`publish`指定发布地址即可。

```groovy
eventBus.publish("news.uk.sport", "Yay! Someone kicked a ball")
```

然后，该消息将传递给在地址`news.uk.sport`注册的所有处理程序。

<a name="50_____发送消息"></a>
#### 发送消息
发送消息将导致仅在接收消息的地址注册一个处理程序。这就是点对点消息传递模式。处理程序以非严格的循环方式选择。

您可以通过`send`发送信息。

```groovy
eventBus.send("news.uk.sport", "Yay! Someone kicked a ball")
```

<a name="51_____在消息上设置标题"></a>
#### 在消息上设置标题
通过事件总线发送的消息也可以包含*header*。 可以通过在发送或发布时设置选项来指定：

```groovy
def options = [
 headers: [
  "some-header" : "some-value"
 ]
]
vertx.eventBus().send("news.uk.sport", "Yay! Someone kicked a ball", options);
```

另一方面，消费者可以按以下方式检索标头：

```groovy
vertx.eventBus().consumer("news.uk.sport",  { e ->
  println e.headers()["some-header"];
});
```

<a name="52_____消息顺序"></a>
#### 消息顺序
Vert.x将按照从任何特定发件人发送的顺序将消息传递到任何特定处理程序。

<a name="53_____消息对象"></a>
#### 消息对象
您在消息处理程序中收到的对象是`Message`。

消息的`body`对应于已发送或发布的对象。

消息的头可与`headers`一起使用。

<a name="54_____确认消息_发送回复"></a>
#### 确认消息/发送回复

当使用`send`时，事件总线尝试将消息传递到在事件总线上注册的`MessageConsumer`。

在某些情况下，对于发件人来说，了解消费者何时收到邮件并使用**request-response**模式对其进行"处理"很有用。

为了确认消息已被处理，消费者可以通过调用`reply`来答复消息。

发生这种情况时，它会导致将答复发送回发送方，并且将使用该答复来调用答复处理程序。

一个例子可以清楚地说明这一点：

接受方：

```groovy
def consumer = eventBus.consumer("news.uk.sport")
consumer.handler({ message ->
  println("I have received a message: ${message.body()}")
  message.reply("how interesting!")
})
```

发送方：

```groovy
eventBus.request("news.uk.sport", "Yay! Someone kicked a ball across a patch of grass", { ar ->
  if (ar.succeeded()) {
    println("Received reply: ${ar.result().body()}")
  }
})
```

回复可以包含一个消息正文，其中可以包含有用的信息。

“处理”的实际含义是由应用程序定义的，并且完全取决于消息使用者的操作，而不是Vert.x事件总线本身知道或关心的事情。

一些例子：

- 一个简单的消息使用者，实现了返回一天中时间的服务，便会在回复正文中使用包含一天中时间的消息进行确认
- 实现持久队列的消息使用者，如果消息已成功持久存储在消息中，则可能会以`true`进行确认，否则将以`false`进行确认。
- 成功处理完订单后，处理订单的消息使用者可能会以`true`确认，因此可以将其从数据库中删除

<a name="55_____发送与超时"></a>
#### 发送与超时
当发送带有回复处理程序的消息时，您可以在`DeliveryOptions`中指定超时。

如果在此时间内未收到答复，则将以失败的方式调用答复处理程序。

默认超时为30秒。

<a name="56_____发送失败"></a>
#### 发送失败
消息发送可能由于其他原因而失败，包括：

- 没有可用于将消息发送到的处理程序
- 接收者已使用`fail`明确使消息失败

在所有情况下，将使用特定的故障调用应答处理程序。

<a name="57_____消息的编解码器"></a>
#### 消息的编解码器
如果定义并注册了`消息编解码器`，则可以在事件总线上发送任何您喜欢的对象。

消息编解码器具有名称，您可以在发送或发布消息时在`DeliveryOptions`中指定该名称：

```groovy
eventBus.registerCodec(myCodec)

def options = [
 codecName: myCodec.name()
]

eventBus.send("orders", new MyPOJO(), options)
```

如果您始终希望将相同的编解码器用于特定类型，则可以为其注册默认编解码器，则不必在传递选项中为每次发送指定编解码器：

```groovy
eventBus.registerDefaultCodec(MyPOJO.class, myCodec);

eventBus.send("orders", new MyPOJO());
```

您可以使用`unregisterCodec`取消注册消息编解码器。

消息编解码器不必总是编码和解码为相同的类型。 例如，您可以编写允许发送MyPOJO类的编解码器，但是当该消息发送到处理程序时，它将作为MyOtherPOJO类到达。

<a name="58_____集群事件总线"></a>
#### 集群事件总线
事件总线不仅存在于单个Vert.x实例中。 通过在网络上将不同的Vert.x实例群集在一起，它们可以形成单一的分布式事件总线。

<a name="59_____以编程方式建立集群"></a>
#### 以编程方式建立集群
如果您以编程方式创建Vert.x实例，则可以通过将Vert.x实例配置为集群来获得集群事件总线；

```groovy
def options = [:]
Vertx.clusteredVertx(options, { res ->
  if (res.succeeded()) {
    def vertx = res.result()
    def eventBus = vertx.eventBus()
    println("We now have a clustered event bus: ${eventBus}")
  } else {
    println("Failed: ${res.cause()}")
  }
})
```

您还应该确保在类路径上具有`ClusterManager`实现，例如Hazelcast集群管理器。

<a name="60_____在命令行上进行集群"></a>
#### 在命令行上进行集群
您可以使用以下命令行运行Vert.x集群

```bash
vertx run my-verticle.js -cluster
```

<a name="61____verticles自动清理"></a>
### verticles自动清理

如果您是从Verticle内部注册事件总线处理程序，则在取消部署Verticle时，这些处理程序将自动注销。

<a name="62___配置事件总线"></a>
## 配置事件总线
可以配置事件总线。当事件总线集群化时，它特别有用。在底层，事件总线使用TCP连接发送和接收消息，因此`EventBusOptions`允许您配置这些TCP连接的所有方面。由于事件总线充当服务器和客户机，所以配置接近于`NetClientOptions`和`NetServerOptions`。

```groovy
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
> 
------

事件总线配置需要在所有集群节点中保持一致。

`EventBusOptions`还允许您指定事件总线是否集群、端口和主机。

在容器中使用时，还可以配置公共主机和端口:

```groovy
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

<a name="63___JSON"></a>
## JSON
为了操作JSON对象，Vert.x提出了自己的`JsonObject`和`JsonArray`实现。这是因为，与其他一些语言不同，Java没有对[JSON](https://json.org/)的一级支持。

当使用Groovy开发vert.x应用程序时，您可以依赖于这两个类，或者使用([来自Groovy的JSON支持](http://www.groovy-lang.org/json.html))。本节解释如何使用Vert.x类。

------
> **注意:** 大多数采用JSON对象作为参数的vert.x方法在其Java版本中采用Map。
> 
------

<a name="64____JSON对象"></a>
### JSON对象
`JsonObject`类表示JSON对象。

JSON对象基本上只是一个具有字符串键的映射，并且值可以是JSON支持的类型之一（字符串，数字，布尔值）。

JSON对象还支持空值`null`。

<a name="65_____创建JSON对象"></a>
#### 创建JSON对象
可以使用默认构造函数创建空的JSON对象。

你可以创建一个JSON对象从一个字符串或g-string JSON表示形式:，如下所示：

```groovy
def object = new JsonObject("{\"foo\":\"bar\"}")
def object2 = new JsonObject("""
{
"foo": "bar"
}
""")
```

在Groovy中，从Map创建JSON对象也很方便：

```groovy
def map = [ "foo" : "bar" ]
def json = new JsonObject(map)
```

嵌套maps将转换为嵌套JSON对象。

<a name="66_____将条目放入JSON对象"></a>
#### 将条目放入JSON对象
使用`put`方法将值放入JSON对象。

可以使用流畅的API链接方法调用：

```groovy
def object = new JsonObject()
object.put("foo", "bar").put("num", 123).put("mybool", true)
```

<a name="67_____从JSON对象获取值"></a>
#### 从JSON对象获取值
您可以使用`getXXX`方法从JSON对象获取值，例如：

```groovy
dev val1 = jsonObject.getString("some-key")
def val2 = jsonObject.getInteger("some-other-key")
```

<a name="68_____将JSON对象编码为字符串"></a>
#### 将JSON对象编码为字符串
您可以使用`encode`将对象编码为String形式。 还有一个`encodePrettily`使输出漂亮（多行和缩进）。

<a name="69____JSON_数组"></a>
### JSON 数组
`JsonArray`类表示JSON数组。

JSON数组是一系列值（字符串，数字，布尔值）。

JSON数组也可以包含空值`null`。

<a name="70_____创建JSON数组"></a>
#### 创建JSON数组
可以使用默认构造函数创建空的JSON数组。

您可以从字符串JSON表示形式创建JSON数组，如下所示：

```groovy
def object = new JsonObject("""{foo:["bar", "baz"}""")
def object2 = new JsonObject(["foo": ["bar", "baz"]])
```

<a name="71_____将条目添加到JSON数组中"></a>
#### 将条目添加到JSON数组中
您可以使用`add`方法将条目添加到JSON数组中。

```groovy
def array = new JsonArray()
array.add("foo").add(123).add(false)
```

<a name="72_____从JSON数组获取值"></a>
#### 从JSON数组获取值
您可以使用`getXXX`方法从JSON数组中获取值，例如：

```groovy
def val = array.getString(0)
def intVal = array.getInteger(1)
def boolVal = array.getBoolean(2)
```

<a name="73_____将JSON数组编码为字符串"></a>
#### 将JSON数组编码为字符串
您可以使用`encode`将数组编码为String形式。 还有一个`encodePrettily`使输出漂亮（多行和缩进）。

<a name="74___Json指针"></a>
## Json指针
Vert.x提供了[来自RFC6901的Json指针](https://tools.ietf.org/html/rfc6901)的实现。 您可以将指针用于查询和编写。 您可以使用字符串，URI或手动添加路径来构建`JsonPointer`：

```groovy
// Build a pointer from a string
def pointer1 = JsonPointer.from("/hello/world")
// Build a pointer manually
def pointer2 = JsonPointer.create().append("hello").append("world")
```

实例化指针后，使用`queryJson`来查询JSON值。 您可以使用`writeJson`来更新Json值：

```groovy
// Query a JsonObject
def result1 = objectPointer.queryJson(jsonObject)
// Query a JsonArray
def result2 = arrayPointer.queryJson(jsonArray)
// Write starting from a JsonObject
objectPointer.writeJson(jsonObject, "new element")
// Write starting from a JsonObject
arrayPointer.writeJson(jsonArray, "new element")
```

您可以通过提供`JsonPointerIterator`的自定义实现，将Vert.x的Json指针与任何对象模型一起使用

<a name="75___缓冲区"></a>
## 缓冲区
大多数数据使用缓冲区在Vert.x内部混洗。

缓冲区是可以读取或写入的零个或多个字节的序列，并根据需要自动扩展以容纳写入其中的任何字节。 您也许可以将缓冲区视为智能字节数组。

<a name="76____创建缓冲区"></a>
### 创建缓冲区
可以使用静态的`Buffer.buffer`方法之一来创建缓冲区。

可以从字符串或字节数组初始化缓冲区，也可以创建空缓冲区。

以下是一些创建缓冲区的示例：

创建一个新的空缓冲区：

```groovy
def buff = Buffer.buffer()
```

从字符串创建缓冲区。 字符串将使用UTF-8在缓冲区中编码。

```groovy
def buff = Buffer.buffer("some string")
```

从字符串创建缓冲区：将使用指定的编码对字符串进行编码，例如：

```groovy
def buff = Buffer.buffer("some string", "UTF-16")
```

创建一个带有初始大小提示的缓冲区。 如果您知道缓冲区中将写入一定数量的数据，则可以创建缓冲区并指定此大小。 这使得缓冲区最初分配了那么多内存，并且比缓冲区在将数据写入缓冲区时自动多次调整大小的效率更高。

注意，以这种方式创建的缓冲区是**空的**。它不会创建一个满是0到指定大小的缓冲区。

```groovy
def buff = Buffer.buffer(10000)
```

<a name="77____写入缓冲区"></a>
### 写入缓冲区
有两种写入缓冲区的方法：追加和随机访问。 无论哪种情况，缓冲区将始终自动扩展以包含字节。 带有缓冲区的`IndexOutOfBoundsException`是不可能的。

<a name="78_____追加到缓冲区"></a>
#### 追加到缓冲区
要追加到缓冲区，可以使用`appendXXX`方法。 存在用于附加各种不同类型的附加方法。

`appendXXX`方法的返回值是缓冲区本身，因此可以将它们链接起来：

```groovy
def buff = Buffer.buffer()

buff.appendInt(123).appendString("hello\n")

socket.write(buff)
```

<a name="79_____随机存取缓冲区写入"></a>
#### 随机存取缓冲区写入
您也可以使用`setXXX`方法以特定的索引写入缓冲区。 存在用于各种不同数据类型的设置方法。 所有set方法都将索引作为第一个参数-这表示缓冲区中开始写入数据的位置。

缓冲区将始终根据需要扩展以容纳数据。

```groovy
def buff = Buffer.buffer()

buff.setInt(1000, 123)
buff.setString(0, "hello")
```

<a name="80____从缓冲区读取"></a>
### 从缓冲区读取
使用`getXXX`方法从缓冲区读取数据。 存在各种数据类型的Get方法。 这些方法的第一个参数是缓冲区中从何处获取数据的索引。

```groovy
def buff = Buffer.buffer()
for (def i = 0;i < buff.length();4) {
  println("int value at ${i} is ${buff.getInt(i)}")
}
```

<a name="81____使用无符号数字"></a>
### 使用无符号数字
可以使用`getUnsignedXXX`，`appendUnsignedXXX`和`setUnsignedXXX`方法从缓冲区读取无符号的数字或将其附加/设置到缓冲区。 在为网络协议实现编解码器时最有用的，该编解码器已优化以最小化带宽消耗。

在下面的例子中，值200被设置在指定的位置，只有一个字节:

```groovy
def buff = Buffer.buffer(128)
def pos = 15
buff.setUnsignedByte(pos, 200)
println(buff.getUnsignedByte(pos))
```

控制台显示“200”。

<a name="82____缓冲区长度"></a>
### 缓冲区长度
使用`length`获得缓冲区的长度。 缓冲区的长度是缓冲区的最大索引+ 1。

<a name="83____复制缓冲区"></a>
### 复制缓冲区
使用`copy`制作缓冲区的副本

<a name="84____切片缓冲区"></a>
### 切片缓冲区
切片缓冲区是一个新的缓冲区，它返回到原始缓冲区，即它不复制底层数据。使用`slice`创建一个切片缓冲区

<a name="85____缓冲区重用"></a>
### 缓冲区重用
将缓冲区写入套接字或其他类似位置后，将无法重复使用它们。

<a name="86___编写TCP服务器和客户端"></a>
## 编写TCP服务器和客户端
Vert.x允许您轻松编写不阻塞的TCP客户端和服务器。

<a name="87____创建一个TCP服务器"></a>
### 创建一个TCP服务器
使用所有默认选项创建TCP服务器的最简单方法如下：

```groovy
def server = vertx.createNetServer()
```

<a name="88____配置TCP服务器"></a>
### 配置TCP服务器
如果您不希望使用默认值，则可以通过在创建服务器时传入`NetServerOptions`实例来配置服务器：when creating it:

```groovy
def options = [
  port:4321
]
def server = vertx.createNetServer(options)
```

<a name="89____开始服务器监听"></a>
### 开始服务器监听
要告诉服务器侦听传入请求，可以使用`listen`替代方法之一。

要告诉服务器侦听选项中指定的主机和端口：

```groovy
def server = vertx.createNetServer()
server.listen()
```

或在调用中指定要监听的主机和端口，而忽略选项中配置的内容：

```groovy
def server = vertx.createNetServer()
server.listen(1234, "localhost")
```

默认主机为`0.0.0.0`，表示“监听所有可用地址”，默认端口为`0`，这是一个特殊值，指示服务器查找随机未使用的本地端口并使用该端口。

实际的绑定是异步的，因此服务器可能调用返回之后一段时间才真正在监听。

如果希望在服务器实际监听时收到通知，则可以为`listen`调用提供处理程序。 例如：

```groovy
def server = vertx.createNetServer()
server.listen(1234, "localhost", { res ->
  if (res.succeeded()) {
    println("Server is now listening!")
  } else {
    println("Failed to bind!")
  }
})
```

<a name="90____在随机端口上监听"></a>
### 在随机端口上监听
如果将`0`用作侦听端口，则服务器将找到一个未使用的随机端口进行侦听。

要找出服务器正在监听的真实端口，可以调用`actualPort`。

```groovy
def server = vertx.createNetServer()
server.listen(0, "localhost", { res ->
  if (res.succeeded()) {
    println("Server is now listening on actual port: ${server.actualPort()}")
  } else {
    println("Failed to bind!")
  }
})
```

<a name="91____收到传入连接的通知"></a>
### 收到传入连接的通知
要在建立连接时收到通知，您需要设置一个`connectHandler`：

```groovy
def server = vertx.createNetServer()
server.connectHandler({ socket ->
  // Handle the connection in here
})
```

建立连接后，将使用`NetSocket`实例调用处理程序。

这是实际连接的类似于套接字的接口，它允许您读取和写入数据以及执行其他各种操作，例如关闭套接字。

<a name="92____从套接字读取数据"></a>
### 从套接字读取数据
要从套接字读取数据，请在套接字上设置`handler`。

每次在套接字上接收到数据时，将使用`Buffer`实例调用此处理程序。

```groovy
def server = vertx.createNetServer()
server.connectHandler({ socket ->
  socket.handler({ buffer ->
    println("I received some bytes: ${buffer.length()}")
  })
})
```

<a name="93____将数据写入套接字"></a>
### 将数据写入套接字
您使用`write`之一写入套接字。

```groovy
// Write a buffer
def buffer = Buffer.buffer().appendFloat(12.34f).appendInt(123)
socket.write(buffer)

// Write a string in UTF-8 encoding
socket.write("some data")

// Write a string using the specified encoding
socket.write("some data", "UTF-16")
```

写操作是异步的，直到写调用返回后一段时间才可能发生。

<a name="94____关闭处理程序"></a>
### 关闭处理程序
如果您想在套接字关闭时收到通知，可以在其上设置一个`closeHandler`：

```groovy
socket.closeHandler({ v ->
  println("The socket has been closed")
})
```

<a name="95____处理异常"></a>
### 处理异常
您可以设置`exceptionHandler`来接收套接字上发生的任何异常。

您可以设置`exceptionHandler`来接收将连接传递给`connectHandler`之前发生的任何异常，例如在TLS握手期间。

<a name="96____事件总线写处理程序"></a>
### 事件总线写处理程序
每个套接字都会在事件总线上自动注册一个处理程序，并且在此处理程序中接收到任何缓冲区时，它将它们写入自身。

这使您能够将数据写入socket，socket可能位于完全不同的verticle，甚至可能位于不同的 Vert.x实例。将缓冲区发送到该处理程序的地址。

处理程序的地址是`writeHandlerID`

<a name="97____本地和远程地址"></a>
### 本地和远程地址
可以使用`localAddress`检索`NetSocket`的本地地址。

可以使用`remoteAddress`来检索`NetSocket`的远程地址（即连接另一端的地址）。

<a name="98____从类路径发送文件或资源"></a>
### 从类路径发送文件或资源
文件和类路径资源可以直接使用`sendFile`写入套接字。 这是一种非常有效的发送文件的方式，因为它可以由OS内核直接在操作系统支持的地方进行处理。

有关限制或禁用类路径解析的信息，请参阅有关[从类路径提供文件](https://vertx.io/docs/vertx-core/java/#classpath)的章节。

```groovy
socket.sendFile("myfile.dat")
```

<a name="99____流式套接字"></a>
### 流式套接字
`NetSocket`的实例同时也是`ReadStream`和`WriteStream`实例，因此它们可用于向其他读写流中泵送数据。

有关更多信息，请参见[流和泵](https://vertx.io/docs/vertx-core/java/#streams)一章。

<a name="100____将连接升级到SSL_TLS"></a>
### 将连接升级到SSL/TLS
非SSL/TLS连接可以使用`upgradeToSsl`升级到SSL/TLS。

必须为SSL/TLS配置服务器或客户端才能正常工作。 有关更多信息，请参见[SSL/TLS章节](https://vertx.io/docs/vertx-core/java/#ssl) 。

<a name="101____关闭TCP服务器"></a>
### 关闭TCP服务器
调用`close`关闭服务器。 关闭服务器将关闭所有打开的连接并释放所有服务器资源。

关闭实际上是异步的，并且可能直到调用返回后的一段时间才能完成。 如果您想在实际关闭完成时收到通知，则可以传入处理程序。

关闭完成后，将调用此处理程序。

```groovy
server.close({ res ->
  if (res.succeeded()) {
    println("Server is now closed")
  } else {
    println("close failed")
  }
})
```

<a name="102____自动清理的verticles"></a>
### 自动清理的verticles
如果您要从verticles中创建TCP服务器和客户端，则取消部署verticles时，这些服务器和客户端将自动关闭。

<a name="103____扩展_共享TCP服务器"></a>
### 扩展-共享TCP服务器
任何TCP服务器的处理程序始终在同一事件循环线程上执行。

这意味着，如果您在具有很多核心的服务器上运行，并且仅部署了一个实例，那么您的服务器上最多将使用一个核心。

为了利用服务器的更多核心，您将需要部署服务器的更多实例。

您可以在代码中以编程方式实例化更多实例：

```groovy
// Create a few instances so we can utilise cores

(0..<10).each { i ->
  def server = vertx.createNetServer()
  server.connectHandler({ socket ->
    socket.handler({ buffer ->
      // Just echo back the data
      socket.write(buffer)
    })
  })
  server.listen(1234, "localhost")
}
```

或者，如果您使用的是verticle，则可以通过在命令行上使用`-instances`选项来简单地部署服务器verticle的更多实例：

vertx 运行 `com.mycompany.MyVerticle -instances 10`

或以编程方式部署verticle

```groovy
def options = [
  instances:10
]
vertx.deployVerticle("com.mycompany.MyVerticle", options)
```

完成此操作后，您将发现echo服务器在功能上与以前相同，但是可以利用服务器上的所有内核，并且可以处理更多工作。

此时，您可能会问自己 **'如何让多个服务器监听同一主机和端口?当您尝试并部署多个实例时，您肯定会遇到端口冲突吗?'**

**Vert.x在这里做了一点魔术。**

当您在与现有服务器相同的主机和端口上部署另一台服务器时，实际上并不会尝试创建在同一主机/端口上侦听的新服务器。

相反，它在内部仅维护一台服务器，并且随着传入连接的到达，它将以循环方式将它们分配给任何连接处理程序。

因此，Vert.x TCP服务器可以扩展可用核心，而每个实例保持单线程。

<a name="104____创建一个TCP客户端"></a>
### 创建一个TCP客户端
使用所有默认选项创建TCP客户端的最简单方法如下：

```groovy
def client = vertx.createNetClient()
```

<a name="105____配置TCP客户端"></a>
### 配置TCP客户端
如果您不希望使用默认值，则可以通过在创建客户端时传入`NetClientOptions`实例来配置客户端：

```groovy
def options = [
  connectTimeout:10000
]
def client = vertx.createNetClient(options)
```

<a name="106____建立连接"></a>
### 建立连接
要与服务器建立连接，请使用`connect`，指定服务器的端口和主机以及将被调用的处理程序，连接成功时将包含`NetSocket`的结果，如果连接失败则返回错误。

```groovy
def options = [
  connectTimeout:10000
]
def client = vertx.createNetClient(options)
client.connect(4321, "localhost", { res ->
  if (res.succeeded()) {
    println("Connected!")
    def socket = res.result()
  } else {
    println("Failed to connect: ${res.cause().getMessage()}")
  }
})
```

<a name="107____配置连接尝试"></a>
### 配置连接尝试
可以将客户端配置为在无法连接的情况下自动重试连接服务器。 这是通过`setReconnectInterval`和`setReconnectAttempts`配置的。

------
> **注意:** 当前，如果连接失败，Vert.x将不会尝试重新连接，重新连接尝试和间隔仅适用于创建初始连接。
> 
------

```groovy
def options = [
  reconnectAttempts:10,
  reconnectInterval:500
]

def client = vertx.createNetClient(options)
```

默认情况下，禁用多次连接尝试。

<a name="108____记录网络活动"></a>
### 记录网络活动
出于调试目的，可以记录网络活动：

```groovy
def options = [
  logActivity:true
]

def server = vertx.createNetServer(options)
```

客户端

```groovy
def options = [
  logActivity:true
]

def client = vertx.createNetClient(options)
```

Netty用`DEBUG`级别和`io.netty.handler.logging.LoggingHandler`名称记录网络活动。 使用网络活动日志记录时，请记住以下几点：

- 记录不是由Vert.x记录执行，而是由Netty执行
- 这**不是**生产功能

您应该阅读[Netty日志记录](https://vertx.io/docs/vertx-core/java/#netty-logging)部分。

<a name="109____配置服务器和客户端以使用SSL_TLS"></a>
### 配置服务器和客户端以使用SSL/TLS
可以将TCP客户端和服务器配置为使用[传输层安全性](https://en.wikipedia.org/wiki/Transport_Layer_Security)-TLS的早期版本称为SSL。

无论是否使用SSL/TLS，服务器和客户端的API都是相同的，并且可以通过配置用于创建服务器或客户端的`NetClientOptions` 或 `NetServerOptions`实例来启用它们。

<a name="110_____在服务器上启用SSL_TLS"></a>
#### 在服务器上启用SSL/TLS
SSL/TLS通过`ssl`启用。

默认情况下，它是禁用的。

<a name="111_____指定服务器的密钥_证书"></a>
#### 指定服务器的密钥/证书
SSL/TLS服务器通常向客户端提供证书，以便向客户端验证其身份。

可以通过多种方式为服务器配置证书/密钥：

第一种方法是通过指定包含证书和私钥的Java密钥存储的位置。

可以使用JDK附带的[keytool](https://docs.oracle.com/javase/6/docs/technotes/tools/solaris/keytool.html)实用程序来管理Java密钥库。

还应提供密钥库的密码：

```groovy
def options = [
  ssl:true,
  keyStoreOptions:[
    path:"/path/to/your/server-keystore.jks",
    password:"password-of-your-keystore"
  ]
]
def server = vertx.createNetServer(options)
```

或者，您可以自己将密钥存储区作为缓冲区读取并直接提供：

```groovy
def myKeyStoreAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/server-keystore.jks")
def jksOptions = [
  value:myKeyStoreAsABuffer,
  password:"password-of-your-keystore"
]
def options = [
  ssl:true,
  keyStoreOptions:jksOptions
]
def server = vertx.createNetServer(options)
```

密钥/证书，格式为PKCS#12([http://en.wikipedia.org/wiki/PKCS_12](https://en.wikipedia.org/wiki/PKCS_12))，通常带有`.pfx`或`.p12`扩展名也可以以与JKS密钥库类似的方式加载：

```groovy
def options = [
  ssl:true,
  pfxKeyCertOptions:[
    path:"/path/to/your/server-keystore.pfx",
    password:"password-of-your-keystore"
  ]
]
def server = vertx.createNetServer(options)
```

还支持缓冲区配置：

```groovy
def myKeyStoreAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/server-keystore.pfx")
def pfxOptions = [
  value:myKeyStoreAsABuffer,
  password:"password-of-your-keystore"
]
def options = [
  ssl:true,
  pfxKeyCertOptions:pfxOptions
]
def server = vertx.createNetServer(options)
```

使用`.pem`文件分别提供服务器私钥和证书的另一种方法。

```groovy
def options = [
  ssl:true,
  pemKeyCertOptions:[
    keyPath:"/path/to/your/server-key.pem",
    certPath:"/path/to/your/server-cert.pem"
  ]
]
def server = vertx.createNetServer(options)
```

还支持缓冲区配置：

```groovy
def myKeyAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/server-key.pem")
def myCertAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/server-cert.pem")
def pemOptions = [
  keyValue:myKeyAsABuffer,
  certValue:myCertAsABuffer
]
def options = [
  ssl:true,
  pemKeyCertOptions:pemOptions
]
def server = vertx.createNetServer(options)
```

Vert.x支持从PKCS8 PEM文件中读取未加密的基于RSA和/或ECC的私钥。 还可以从PKCS1 PEM文件中读取基于RSA的私钥。 可以从包含[RFC 7468，第5节](https://tools.ietf.org/html/rfc7468#section-5)定义的证书文本编码的PEM文件中读取X.509证书。

------
> **警告:** 请记住，任何可以读取该文件的人都可以提取未加密的PKCS8或PKCS1 PEM文件中包含的密钥。 因此，请确保对此类PEM文件设置适当的访问限制，以防止滥用。
>
------

<a name="112_____指定服务器的信任"></a>
#### 指定服务器的信任
SSL/TLS服务器可以使用证书颁发机构来验证客户端的身份。

可以通过多种方式为服务器配置证书颁发机构：

可以使用JDK附带的[keytool](https://docs.oracle.com/javase/6/docs/technotes/tools/solaris/keytool.html)实用程序来管理Java信任库。

还应提供信任库的密码：

```groovy
def options = [
  ssl:true,
  clientAuth:"REQUIRED",
  trustStoreOptions:[
    path:"/path/to/your/truststore.jks",
    password:"password-of-your-truststore"
  ]
]
def server = vertx.createNetServer(options)
```

或者，您可以自己将信任库作为缓冲区读取，并直接提供该缓冲区：

```groovy
def myTrustStoreAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/truststore.jks")
def options = [
  ssl:true,
  clientAuth:"REQUIRED",
  trustStoreOptions:[
    value:myTrustStoreAsABuffer,
    password:"password-of-your-truststore"
  ]
]
def server = vertx.createNetServer(options)
```

PKCS#12格式([http://en.wikipedia.org/wiki/PKCS_12](https://en.wikipedia.org/wiki/PKCS_12))的证书颁发机构，通常带有`.pfx`或`.p12`扩展名也可以类似于JKS信任库的方式加载：

```groovy
def options = [
  ssl:true,
  clientAuth:"REQUIRED",
  pfxTrustOptions:[
    path:"/path/to/your/truststore.pfx",
    password:"password-of-your-truststore"
  ]
]
def server = vertx.createNetServer(options)
```

还支持缓冲区配置：

```groovy
def myTrustStoreAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/truststore.pfx")
def options = [
  ssl:true,
  clientAuth:"REQUIRED",
  pfxTrustOptions:[
    value:myTrustStoreAsABuffer,
    password:"password-of-your-truststore"
  ]
]
def server = vertx.createNetServer(options)
```

使用列表`.pem`文件提供服务器证书颁发机构的另一种方法。

```groovy
def options = [
  ssl:true,
  clientAuth:"REQUIRED",
  pemTrustOptions:[
    certPaths:[
      "/path/to/your/server-ca.pem"
    ]
  ]
]
def server = vertx.createNetServer(options)
```

还支持缓冲区配置：

```groovy
def myCaAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/server-ca.pfx")
def options = [
  ssl:true,
  clientAuth:"REQUIRED",
  pemTrustOptions:[
    certValues:[
      myCaAsABuffer
    ]
  ]
]
def server = vertx.createNetServer(options)
```

<a name="113_____在客户端上启用SSL_TLS"></a>
#### 在客户端上启用SSL/TLS
Net Client也可以轻松配置为使用SSL。 使用SSL和使用标准套接字时，它们具有完全相同的API。

要在NetClient上启用SSL，请调用函数`setSSL(true)`。

<a name="114_____客户端信任配置"></a>
#### 客户端信任配置
如果在客户端上将`trustAll`设置为true，则客户端将信任所有服务器证书。 连接仍将被加密，但是此模式容易受到“中间人”攻击。 即 您不确定要连接到谁。 请谨慎使用。 默认值为false。

```groovy
def options = [
  ssl:true,
  trustAll:true
]
def client = vertx.createNetClient(options)
```

如果未设置`trustAll`，则必须配置客户端信任存储，并且应包含客户端信任的服务器的证书。

默认情况下，在客户端上禁用主机验证。 要启用主机验证，请设置要在客户端上使用的算法（当前仅支持HTTPS和LDAPS）：

```groovy
def options = [
  ssl:true,
  hostnameVerificationAlgorithm:"HTTPS"
]
def client = vertx.createNetClient(options)
```

与服务器配置相同，可以通过以下几种方式配置客户端信任：

第一种方法是通过指定包含证书颁发机构的Java信任库的位置。

它只是一个标准的Java密钥库，与服务器端的密钥库相同。 客户信任库位置是通过使用`jks options`上的函数`path`来设置的。 如果服务器在连接过程中出示的证书不在客户端信任存储区中，则连接尝试将不会成功。

```groovy
def options = [
  ssl:true,
  trustStoreOptions:[
    path:"/path/to/your/truststore.jks",
    password:"password-of-your-truststore"
  ]
]
def client = vertx.createNetClient(options)
```

还支持缓冲区配置：

```groovy
def myTrustStoreAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/truststore.jks")
def options = [
  ssl:true,
  trustStoreOptions:[
    value:myTrustStoreAsABuffer,
    password:"password-of-your-truststore"
  ]
]
def client = vertx.createNetClient(options)
```

PKCS#12格式([http://en.wikipedia.org/wiki/PKCS_12](https://en.wikipedia.org/wiki/PKCS_12))的证书颁发机构，通常带有`.pfx`或`.p12`扩展名也可以类似于JKS信任库的方式加载：

```groovy
def options = [
  ssl:true,
  pfxTrustOptions:[
    path:"/path/to/your/truststore.pfx",
    password:"password-of-your-truststore"
  ]
]
def client = vertx.createNetClient(options)
```

还支持缓冲区配置：

```groovy
def myTrustStoreAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/truststore.pfx")
def options = [
  ssl:true,
  pfxTrustOptions:[
    value:myTrustStoreAsABuffer,
    password:"password-of-your-truststore"
  ]
]
def client = vertx.createNetClient(options)
```

使用列表`.pem`文件提供服务器证书颁发机构的另一种方法。

```groovy
def options = [
  ssl:true,
  pemTrustOptions:[
    certPaths:[
      "/path/to/your/ca-cert.pem"
    ]
  ]
]
def client = vertx.createNetClient(options)
```

还支持缓冲区配置：

```groovy
def myTrustStoreAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/ca-cert.pem")
def options = [
  ssl:true,
  pemTrustOptions:[
    certValues:[
      myTrustStoreAsABuffer
    ]
  ]
]
def client = vertx.createNetClient(options)
```

<a name="115_____指定客户端的密钥_证书"></a>
#### 指定客户端的密钥/证书
如果服务器要求客户端身份验证，则客户端在连接时必须向服务器出示自己的证书。 可以通过几种方式配置客户端：

第一种方法是通过指定包含密钥和证书的Java密钥存储的位置。 同样，它只是常规的Java密钥存储区。 客户端密钥库的位置是通过使用`jks options`上的函数`path`来设置的。

```groovy
def options = [
  ssl:true,
  keyStoreOptions:[
    path:"/path/to/your/client-keystore.jks",
    password:"password-of-your-keystore"
  ]
]
def client = vertx.createNetClient(options)
```

还支持缓冲区配置：

```groovy
def myKeyStoreAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/client-keystore.jks")
def jksOptions = [
  value:myKeyStoreAsABuffer,
  password:"password-of-your-keystore"
]
def options = [
  ssl:true,
  keyStoreOptions:jksOptions
]
def client = vertx.createNetClient(options)
```

密钥/证书，格式为PKCS#12([http://en.wikipedia.org/wiki/PKCS_12](https://en.wikipedia.org/wiki/PKCS_12))，通常带有`.pfx`或`.p12扩展名也可以以与JKS密钥库类似的方式加载：

```groovy
def options = [
  ssl:true,
  pfxKeyCertOptions:[
    path:"/path/to/your/client-keystore.pfx",
    password:"password-of-your-keystore"
  ]
]
def client = vertx.createNetClient(options)
```

还支持缓冲区配置：

```groovy
def myKeyStoreAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/client-keystore.pfx")
def pfxOptions = [
  value:myKeyStoreAsABuffer,
  password:"password-of-your-keystore"
]
def options = [
  ssl:true,
  pfxKeyCertOptions:pfxOptions
]
def client = vertx.createNetClient(options)
```

使用`.pem`文件分别提供服务器私钥和证书的另一种方法。

```groovy
def options = [
  ssl:true,
  pemKeyCertOptions:[
    keyPath:"/path/to/your/client-key.pem",
    certPath:"/path/to/your/client-cert.pem"
  ]
]
def client = vertx.createNetClient(options)
```

还支持缓冲区配置：

```groovy
def myKeyAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/client-key.pem")
def myCertAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/client-cert.pem")
def pemOptions = [
  keyValue:myKeyAsABuffer,
  certValue:myCertAsABuffer
]
def options = [
  ssl:true,
  pemKeyCertOptions:pemOptions
]
def client = vertx.createNetClient(options)
```

请记住，pem配置中，私钥未加密。

<a name="116_____用于测试和开发目的的自签名证书"></a>
#### 用于测试和开发目的的自签名证书

------
> **慎重:** 不要在生产设置中使用此功能，请注意，生成的密钥非常不安全。
>
------

通常，需要自签名证书，无论是用于单元/集成测试还是用于运行应用程序的开发版本。

`SelfSignedCertificate`可用于提供自签名的PEM证书助手，并提供`KeyCertOptions`和`TrustOptions`配置：

```groovy
def certificate = SelfSignedCertificate.create()

def serverOptions = [
  ssl:true,
  keyCertOptions:certificate.keyCertOptions(),
  trustOptions:certificate.trustOptions()
]

def server = vertx.createNetServer(serverOptions).connectHandler({ socket ->
  socket.write("Hello!").end()
}).listen(1234, "localhost")

def clientOptions = [
  ssl:true,
  keyCertOptions:certificate.keyCertOptions(),
  trustOptions:certificate.trustOptions()
]

def client = vertx.createNetClient(clientOptions)
client.connect(1234, "localhost", { ar ->
  if (ar.succeeded()) {
    ar.result().handler({ buffer ->
      println(buffer)
    })
  } else {
    System.err.println("Woops: ${ar.cause().getMessage()}")
  }
})
```

客户端也可以配置为信任所有证书：

```groovy
def clientOptions = [
  ssl:true,
  trustAll:true
]
```

请注意，自签名证书还适用于其他TCP协议（例如HTTPS）：

```groovy
def certificate = SelfSignedCertificate.create()

vertx.createHttpServer([
  ssl:true,
  keyCertOptions:certificate.keyCertOptions(),
  trustOptions:certificate.trustOptions()
]).requestHandler({ req ->
  req.response().end("Hello!")
}).listen(8080)
```

<a name="117_____吊销证书颁发机构"></a>
#### 吊销证书颁发机构
可以将信任配置为使用证书吊销列表（CRL）来处理应该不再受信任的吊销证书。 `crlPath`配置crl列表以使用：

```groovy
def options = [
  ssl:true,
  trustStoreOptions:trustOptions,
  crlPaths:[
    "/path/to/your/crl.pem"
  ]
]
def client = vertx.createNetClient(options)
```

还支持缓冲区配置：

```groovy
def myCrlAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/crl.pem")
def options = [
  ssl:true,
  trustStoreOptions:trustOptions,
  crlValues:[
    myCrlAsABuffer
  ]
]
def client = vertx.createNetClient(options)
```

<a name="118_____配置密码套件"></a>
#### 配置密码套件
默认情况下，TLS配置将使用运行Vert.x的JVM的Cipher套件。 可以使用以下一组启用密码来配置此密码套件：

```groovy
def options = [
  ssl:true,
  keyStoreOptions:keyStoreOptions,
  enabledCipherSuites:[
    "ECDHE-RSA-AES128-GCM-SHA256",
    "ECDHE-ECDSA-AES128-GCM-SHA256",
    "ECDHE-RSA-AES256-GCM-SHA384",
    "CDHE-ECDSA-AES256-GCM-SHA384"
  ]
]
def server = vertx.createNetServer(options)
```

密码套件可以在`NetServerOptions`或`NetClientOptions`配置中指定。

<a name="119_____配置TLS协议版本"></a>
#### 配置TLS协议版本
默认情况下，TLS配置将使用以下协议版本：SSLv2Hello，TLSv1，TLSv1.1和TLSv1.2。 可以通过显式添加启用的协议来配置协议版本：

```groovy
Code not translatable
```

协议版本可以在`NetServerOptions`或`NetClientOptions`配置中指定。

<a name="120_____SSL引擎"></a>
#### SSL引擎
可以将引擎实现配置为使用[OpenSSL](https://www.openssl.org/)而不是JDK实现。 与JDK引擎相比，OpenSSL提供了更好的性能和CPU使用率，并且具有JDK版本独立性。

使用的引擎选项是

- 设置时的`getSslEngineOptions`选项
- 否则`JdkSSLEngineOptions`

```groovy
// Use JDK SSL engine
def options = [
  ssl:true,
  keyStoreOptions:keyStoreOptions
]

// Use JDK SSL engine explicitly
options = [
  ssl:true,
  keyStoreOptions:keyStoreOptions,
  jdkSslEngineOptions:[:]
]

// Use OpenSSL engine
options = [
  ssl:true,
  keyStoreOptions:keyStoreOptions,
  openSslEngineOptions:[:]
]
```

<a name="121_____服务器名称指示_SNI_"></a>
#### 服务器名称指示(SNI)
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

```groovy
def keyCertOptions = [
  path:"keystore.jks",
  password:"wibble"
]

def netServer = vertx.createNetServer([
  keyStoreOptions:keyCertOptions,
  ssl:true,
  sni:true
])
```

可以将`PemKeyCertOptions`配置为保存多个条目：

```groovy
def keyCertOptions = [
  keyPaths:["default-key.pem", "host1-key.pem", "etc..."],
  certPaths:["default-cert.pem", "host2-key.pem", "etc..."]
]

def netServer = vertx.createNetServer([
  pemKeyCertOptions:keyCertOptions,
  ssl:true,
  sni:true
])
```

客户端隐式发送连接主机作为完全合格域名（FQDN）的SNI服务器名称。

您可以在连接套接字时提供一个明确的服务器名称

```groovy
def client = vertx.createNetClient([
  trustStoreOptions:trustOptions,
  ssl:true
])

// Connect to 'localhost' and present 'server.name' server name
client.connect(1234, "localhost", "server.name", { res ->
  if (res.succeeded()) {
    println("Connected!")
    def socket = res.result()
  } else {
    println("Failed to connect: ${res.cause().getMessage()}")
  }
})
```

它可以用于不同的目的：

- 提供与服务器主机不同的服务器名称
- 连接到IP时显示服务器名称
- 使用短名称时强制显示服务器名称

<a name="122_____应用层协议协商_ALPN_"></a>
#### 应用层协议协商(ALPN)
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

<a name="123______OpenSSL提供本机ALPN支持。"></a>
##### OpenSSL提供本机ALPN支持。

OpenSSL需要配置`setOpenSslEngineOptions`，并在类路径上使用[netty-tcnative](http://netty.io/wiki/forked-tomcat-native.html)jar。 根据tcnative的实现，使用tcnative可能需要在您的操作系统上安装OpenSSL。

<a name="124______Jetty_ALPN_support"></a>
##### Jetty-ALPN support
Jetty-ALPN是一个小jar，它覆盖了Java 8分发的一些类以支持ALPN。

JVM必须在其`bootclasspath`中以*alpn-boot-${version}.jar*启动：

```bash
-Xbootclasspath/p:/path/to/alpn-boot${version}.jar
```

其中${version}取决于JVM版本，例如 *OpenJDK 1.8.0u74* 的 *8.1.7.v20160121*。 完整列表可在[Jetty-ALPN页面](https://www.eclipse.org/jetty/documentation/current/alpn-chapter.html)上找到。

主要缺点是版本取决于JVM。

为了解决这个问题，可以使用*[Jetty ALPN agent](https://github.com/jetty-project/jetty-alpn-agent)*。 该代理是一个JVM代理，它将为运行它的JVM选择正确的ALPN版本：

```bash
-javaagent:/path/to/alpn/agent
```

<a name="125____使用代理进行客户端连接"></a>
### 使用代理进行客户端连接
`NetClient`支持HTTP/1.x *CONNECT*, *SOCKS4a* 或 *SOCKS5*代理。

通过设置包含代理类型，主机名，端口以及用户名和密码（可选）的`ProxyOptions`对象，可以在`NetClientOptions`中配置代理。

这是一个例子：

```groovy
def options = [
  proxyOptions:[
    type:"SOCKS5",
    host:"localhost",
    port:1080,
    username:"username",
    password:"secret"
  ]
]
def client = vertx.createNetClient(options)
```

DNS解析始终在代理服务器上完成，为了实现SOCKS4客户端的功能，必须在本地解析DNS地址。

<a name="126___编写HTTP服务器和客户端"></a>
## 编写HTTP服务器和客户端
Vert.x允许您轻松编写不阻塞的HTTP客户端和服务器。

Vert.x支持HTTP/1.0, HTTP/1.1 和 HTTP/2协议。

HTTP的基本API与HTTP/1.x和HTTP/2相同，特定的API功能可用于处理HTTP/2协议。

<a name="127____创建一个HTTP服务器"></a>
### 创建一个HTTP服务器
使用所有默认选项创建HTTP服务器的最简单方法如下：

```groovy
def server = vertx.createHttpServer()
```

<a name="128____配置HTTP服务器"></a>
### 配置HTTP服务器
如果您不希望使用默认值，则可以在创建服务器时通过传入`HttpServerOptions`实例来配置服务器：

```groovy
def options = [
  maxWebsocketFrameSize:1000000
]

def server = vertx.createHttpServer(options)
```

<a name="129____配置HTTP_2服务器"></a>
### 配置HTTP/2服务器
Vert.x通过TLS `h2`和TCP `h2c`支持HTTP/2。

- `h2` 在[应用层协议协商](https://en.wikipedia.org/wiki/Application-Layer_Protocol_Negotiation) (ALPN)协商的TLS上使用时，标识HTTP/2协议
- 当在TCP上以明文形式使用时，`h2c`标识HTTP/2协议，此类连接可以通过HTTP/1.1升级请求建立，也可以直接建立

要处理`h2`请求，必须将TLS和`setUseAlpn`一起启用：

```groovy
def options = [
  useAlpn:true,
  ssl:true,
  keyStoreOptions:[
    path:"/path/to/my/keystore"
  ]
]

def server = vertx.createHttpServer(options)
```

ALPN是TLS扩展，可在客户端和服务器开始交换数据之前协商协议。

不支持ALPN的客户端仍可以进行*经典* SSL握手。

ALPN通常会同意`h2`协议，尽管如果服务器或客户端决定使用http/1.1，则可以使用。

为了处理`h2c`请求，必须禁用TLS，服务器将任何要升级到HTTP/2的请求HTTP/1.1升级到HTTP/2。 它也将接受直接从`PRI * HTTP/2.0\r\nSM\r\n`前言开始的`h2c`连接。

------
> **警告:** 大多数浏览器均不支持`h2c`，因此，为网站服务时，应使用`h2`而不是`h2c`。
>
------

服务器接受HTTP/2连接时，会将其`initial settings(初始设置)`发送给客户端。 这些设置定义客户端如何使用连接，服务器的默认初始设置为：

- `getMaxConcurrentStreams`：HTTP/2 RFC建议的`100`
- 其他的默认HTTP/2设置值

------
> **注意:** Worker Verticles与HTTP/2不兼容
>
------

<a name="130____记录网络服务器活动"></a>
### 记录网络服务器活动
出于调试目的，可以记录网络活动。

```groovy
def options = [
  logActivity:true
]

def server = vertx.createHttpServer(options)
```

有关详细说明，请参见[记录网络活动](https://vertx.io/docs/vertx-core/java/#logging_network_activity)一章。

<a name="131____开始服务器监听"></a>
### 开始服务器监听
要告诉服务器侦听传入的请求，您可以使用一种`listen`方法。

要告诉服务器侦听选项中指定的主机和端口：

```groovy
def server = vertx.createHttpServer()
server.listen()
```

或在调用中指定要监听的主机和端口，而忽略选项中配置的内容：

```groovy
def server = vertx.createHttpServer()
server.listen(8080, "myhost.com")
```

默认主机为`0.0.0.0`，表示“监听所有可用地址”，默认端口为`80`。

实际的绑定是异步的，因此服务器可能调用返回之后一段时间才真正在监听。

如果希望在服务器实际监听时收到通知，则可以为`listen`调用提供处理程序。 例如：

```groovy
def server = vertx.createHttpServer()
server.listen(8080, "myhost.com", { res ->
  if (res.succeeded()) {
    println("Server is now listening!")
  } else {
    println("Failed to bind!")
  }
})
```

<a name="132____收到传入请求的通知"></a>
### 收到传入请求的通知
要在请求到达时得到通知，您需要设置`requestHandler`：

```groovy
def server = vertx.createHttpServer()
server.requestHandler({ request ->
  // Handle the request in here
})
```

<a name="133____处理请求"></a>
### 处理请求
当请求到达时，调用请求处理程序传递`HttpServerRequest`的实例。 该对象代表服务器端HTTP请求。

当请求的头被完全读取时，将调用处理程序。

如果请求包含正文，则该正文将在调用请求处理程序后的某个时间到达服务器。

服务器请求对象允许您检索`uri`, `path`, `params` 和 `headers`等。

每个服务器请求对象都与一个服务器响应对象相关联。 您可以使用`response`来获取对`HttpServerResponse`对象的引用。

这是服务器处理请求并以"hello world"回复的简单示例。

```groovy
vertx.createHttpServer().requestHandler({ request ->
  request.response().end("Hello world")
}).listen(8080)
```

<a name="134_____Request_版本"></a>
#### Request 版本
可以使用`version`检索请求中指定的HTTP版本。

<a name="135_____Request_方法"></a>
#### Request 方法
使用`method`检索请求的HTTP方法。 （即GET，POST，PUT，DELETE，HEAD，OPTIONS等）。

<a name="136_____Request_URI"></a>
#### Request URI
使用`uri`检索请求的URI。

注意，这是在HTTP请求中传递的实际URI，它几乎总是一个相对URI。

URI如[HTTP规范的5.1.2节-请求URI](https://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html)中所定义。

<a name="137_____equest_路径"></a>
#### equest 路径
使用`path`返回URI的路径部分

例如，如果请求URI为：

`a/b/c/page.html?param1=abc&param2=xyz`

那么路径将是

`/a/b/c/page.html`

<a name="138_____Request_查询"></a>
#### Request 查询
使用`query`返回URI的查询部分

例如，如果请求URI为：

`a/b/c/page.html?param1=abc&param2=xyz`

然后查询将是

`param1=abc&param2=xyz`

<a name="139_____Request_头"></a>
#### Request 头
使用`headers`返回HTTP请求的标题。

这将返回`MultiMap`的实例-类似于普通Map或Hash，但允许同一键具有多个值-这是因为HTTP允许具有相同键的多个标头值。

它还具有不区分大小写的键，这意味着您可以执行以下操作：

```groovy
def headers = request.headers()

// Get the User-Agent:
println("User agent is ${headers.get("user-agent")}")

// You can also do this and get the same result:
println("User agent is ${headers.get("User-Agent")}")
```

<a name="140_____Request_主机"></a>
#### Request 主机
使用`host`返回HTTP请求的主机。

对于HTTP/1.x请求，返回`host`头，对于HTTP/1请求，返回`:authority`伪头。

<a name="141_____Request_参数"></a>
#### Request 参数
使用`params`返回HTTP请求的参数。

就像`headers`一样，它会返回`MultiMap`的一个实例，因为可以有多个具有相同名称的参数。

请求参数在请求URI的路径之后发送。例如，如果URI是:

`/page.html?param1=abc&param2=xyz`

那么参数将包含以下内容:

```groovy
param1: 'abc'
param2: 'xyz
```

请注意，这些请求参数是从请求的URL中检索的。 如果您在`multi-part/form-data`请求的正文中提交的表单属性是作为HTML表单提交的一部分发送的，则它们不会出现在此处的参数中。

<a name="142_____Remote_地址"></a>
#### Remote 地址
可以使用`remoteAddress`检索请求的发送者的地址。

<a name="143_____绝对_URI"></a>
#### 绝对 URI
HTTP请求中传递的URI通常是相对的。 如果您希望检索与请求相对应的绝对URI，则可以使用`absoluteURI`获取它。

<a name="144_____End_handler"></a>
#### End handler
当整个请求（包括任何主体）都已被完全读取时，将调用请求的`endHandler`。

<a name="145_____从请求主体读取数据"></a>
#### 从请求主体读取数据
HTTP请求通常包含我们要读取的正文。 如前所述，仅当请求的标头到达时，请求处理程序才被调用，因此请求对象此时没有主体。

这是因为主体可能很大（例如，文件上传），而且我们通常不希望在将整个主体交给您之前将其缓存在内存中，因为那样可能会导致服务器耗尽可用内存。

要接收主体，您可以在请求上使用`handler`，每次请求主体的一部分到达时都会调用此函数。 这是一个例子：

```groovy
request.handler({ buffer ->
  println("I have received a chunk of the body of length ${buffer.length()}")
})
```

传递到处理程序中的对象是一个`Buffer`，当数据从网络到达时，可以多次调用该处理程序，具体取决于主体的大小。

在某些情况下（例如，如果主体很小），您将希望在内存中聚合整个主体，因此您可以自己进行聚合，如下所示：

```groovy
// Create an empty buffer
def totalBuffer = Buffer.buffer()

request.handler({ buffer ->
  println("I have received a chunk of the body of length ${buffer.length()}")
  totalBuffer.appendBuffer(buffer)
})

request.endHandler({ v ->
  println("Full body received, length = ${totalBuffer.length()}")
})
```

这是很常见的情况，Vert.x提供了一个`bodyHandler`来为您执行此操作。 接收到所有主体后，将调用一次主体处理程序：

```groovy
request.bodyHandler({ totalBuffer ->
  println("Full body received, length = ${totalBuffer.length()}")
})
```

<a name="146__________泵送请求"></a>
#### #### 泵送请求
请求对象是`ReadStream`，因此您可以将请求主体泵送到任何`WriteStream`实例。

有关详细说明，请参见[流和泵](https://vertx.io/docs/vertx-core/java/#streams)一章。

<a name="147_____处理HTML表单"></a>
#### 处理HTML表单
HTML表单可以以`application/x-www-form-urlencoded` 或 `multipart/form-data`的内容类型提交。

对于url编码的表单，表单属性是在url中编码的，就像普通的查询参数一样。

对于包含多个部分的表单，它们被编码在请求体中，因此在从连线读取整个表单体之前是不可用的。

包含多个部分的表单还可以包含文件上传。

如果你想检索一个多部分表单的属性，你应该告诉Vert。通过调用带有true的`setExpectMultipart`来读取任何主体之前，您希望接收到这样的表单，然后在读取整个主体之后，您应该使用`formAttributes`来检索实际属性:

```groovy
server.requestHandler({ request ->
  request.setExpectMultipart(true)
  request.endHandler({ v ->
    // The body has now been fully read, so retrieve the form attributes
    def formAttributes = request.formAttributes()
  })
})
```

<a name="148_____处理表单文件上传"></a>
#### 处理表单文件上传
Vert.x还可以处理以多部分请求正文编码的文件上传。

要接收文件上传，您告诉Vert.x期望采用多部分表单，并在请求上设置`uploadHandler`。

对于每次到达服务器的上传，都会调用一次此处理程序。

传递到处理程序中的对象是一个`HttpServerFileUpload`实例。

```groovy
server.requestHandler({ request ->
  request.setExpectMultipart(true)
  request.uploadHandler({ upload ->
    println("Got a file upload ${upload.name()}")
  })
})
```

文件上传可能很大，我们不会在单个缓冲区中提供整个上传，因为这可能会导致内存耗尽，相反，上传数据是分块接收的：

```groovy
request.uploadHandler({ upload ->
  upload.handler({ chunk ->
    println("Received a chunk of the upload of length ${chunk.length()}")
  })
})
```

上传对象是一个`ReadStream`，因此您可以将请求正文泵送到任何一个`WriteStream`实例。 有关详细说明，请参见[流和泵](https://vertx.io/docs/vertx-core/java/#streams)一章。

如果您只想将文件上传到磁盘上的某个地方，可以使用`streamToFileSystem`：

```groovy
request.uploadHandler({ upload ->
  upload.streamToFileSystem("myuploads_directory/${upload.filename()}")
})
```

------
> **警告:** 确保在生产系统中检查文件名，以避免恶意客户端将文件上传到文件系统上的任意位置。 有关更多信息，请参见[安全说明](https://vertx.io/docs/vertx-core/java/#_security_notes)。
>
------

<a name="149_____处理_cookies"></a>
#### 处理 cookies
使用`getCookie`按名称检索cookie，或使用`cookieMap`检索所有cookie。

要删除一个cookie，使用`removeCookie`。

要添加cookie，请使用`addCookie`。

当写入响应标头时，cookie集将自动写入响应中，以便浏览器可以存储它们。

Cookie由`Cookie`实例描述。这允许您检索名称、值、域、路径和其他常规cookie属性。

下面是一个查询和添加cookie的例子:

```groovy
def someCookie = request.getCookie("mycookie")
def cookieValue = someCookie.getValue()

// Do something with cookie...

// Add a cookie - this will get written back in the response automatically
request.response().addCookie(Cookie.cookie("othercookie", "somevalue"))
```

<a name="150_____处理压缩的主体"></a>
#### 处理压缩的主体
Vert.x可以处理由客户端使用*deflate* 或 *gzip*算法编码的压缩主体有效载荷。

要启用解压缩，请在创建服务器时在选项上设置`setDecompressionSupported`。

默认情况下解压是禁用的。

<a name="151_____接收自定义HTTP_2帧"></a>
#### 接收自定义HTTP/2帧
HTTP/2是一个框架协议，具有用于HTTP请求/响应模型的各种框架。该协议允许发送和接收其他类型的帧。

要接收自定义帧，您可以对请求使用`customFrameHandler`，它将在每次自定义帧到达时被调用。这里有一个例子:

```groovy
request.customFrameHandler({ frame ->

  println("Received a frame type=${frame.type()} payload${frame.payload().toString()}")
})
```

HTTP/2帧不受流控制—当接收到自定义帧时，无论请求是否暂停，都会立即调用帧处理程序

<a name="152_____非标准的HTTP方法"></a>
#### 非标准的HTTP方法
`OTHER` HTTP方法用于非标准方法，在本例中，`rawMethod`返回客户端发送的HTTP方法。

<a name="153____发送回响应"></a>
### 发送回响应
服务器响应对象是`HttpServerResponse`的一个实例，它是从带有`response`的请求中获得的。

您可以使用响应对象将响应写回到HTTP客户端。

<a name="154_____设置状态码和消息"></a>
#### 设置状态码和消息
响应的默认HTTP状态码是`200`，表示`OK`。

使用`setStatusCode`来设置不同的代码。

您还可以使用`setStatusMessage`指定自定义状态消息。

如果没有指定状态消息，则将使用与状态代码对应的默认消息。

------
> **注意:** 对于HTTP/2，状态不会出现在响应中，因为协议不会将消息传输到客户端
>
------

<a name="155_____编写HTTP响应"></a>
#### 编写HTTP响应
要将数据写入HTTP响应，需要使用`write`操作之一。

在响应结束之前，可以多次调用它们。 可以通过以下几种方式调用它们：

使用单个缓冲区：

```groovy
def response = request.response()
response.write(buffer)
```

用一个字符串。在这种情况下，字符串将使用UTF-8进行编码，并将结果写入线路。

```groovy
def response = request.response()
response.write("hello world!")
```

一个字符串和一个编码。在这种情况下，将使用指定的编码对字符串进行编码，并将结果写入线路。

```groovy
def response = request.response()
response.write("hello world!", "UTF-16")
```

写入响应是异步的，并且总是在写入队列后立即返回。

如果你只是写一个字符串或缓冲区到HTTP响应你可以写它并结束响应在一个单一的调用`end`

首次写入调用会导致将响应标头写入响应。 因此，如果您不使用HTTP分块，则必须在写响应之前设置`Content-Length`标头，否则将为时已晚。 如果您使用的是HTTP分块，则不必担心。

<a name="156_____结束HTTP响应"></a>
#### 结束HTTP响应
一旦你完成了HTTP响应，你应该`end`它。

这可以通过几种方式完成：

没有参数，响应就简单地结束了。

```groovy
def response = request.response()
response.write("hello world!")
response.end()
```

也可以使用字符串或缓冲区调用它，方法与调用`write`相同。在本例中，它与使用字符串或缓冲区调用write，然后调用没有参数的end是一样的。例如:

```groovy
def response = request.response()
response.end("hello world!")
```

<a name="157_____关闭基础连接"></a>
#### 关闭基础连接
您可以使用`close`关闭底层TCP连接。

响应结束时，Vert.x将自动关闭非保持活动连接。

默认情况下，Vert.x不会自动关闭保持活动连接。 如果要在空闲时间后关闭保持活动的连接，则可以配置`setIdleTimeout`。

HTTP/2连接在关闭响应之前发送一个{@literal GOAWAY}帧。

<a name="158_____设置响应头"></a>
#### 设置响应头
HTTP响应报头可以通过直接添加它们到`headers`响应：

```groovy
def response = request.response()
def headers = response.headers()
headers.set("content-type", "text/html")
headers.set("other-header", "wibble")
```

或者你可以使用`putHeader`

```groovy
def response = request.response()
response.putHeader("content-type", "text/html").putHeader("other-header", "wibble")
```

必须在写入响应主体的任何部分之前添加所有的响应头。

<a name="159_____分块的HTTP响应和trailers"></a>
#### 分块的HTTP响应和trailers
Vert.x支持[HTTP块传输编码](https://en.wikipedia.org/wiki/Chunked_transfer_encoding)。

这允许将HTTP响应主体分块编写，并且通常在将大型响应主体流式传输到客户端并且事先不知道总大小时使用。

您将HTTP响应置于分块模式，如下所示：

```groovy
def response = request.response()
response.setChunked(true)
```

默认为非分块。 在分块模式下，每次调用write方法之一将导致新的HTTP块被写出。

在分块模式下，还可以编写HTTP响应trailers。这些实际上写在响应的最后一部分。

------
> **注意:** 分块响应对HTTP/2流无效
>
------

要将trailers添加到响应中，请将其直接添加到`trailers`中。

```groovy
def response = request.response()
response.setChunked(true)
def trailers = response.trailers()
trailers.set("X-wibble", "woobble").set("X-quux", "flooble")
```

或使用`putTrailer`。

```groovy
def response = request.response()
response.setChunked(true)
response.putTrailer("X-wibble", "woobble").putTrailer("X-quux", "flooble")
```

<a name="160_____直接从磁盘或类路径提供文件"></a>
#### 直接从磁盘或类路径提供文件
如果您正在编写Web服务器，则从磁盘提供文件的一种方法是将其作为`AsyncFile`打开并将其泵送至HTTP响应。

或者，您可以使用`readFile`一次性加载它，然后直接将其写入响应中。

另外，Vert.x提供了一种方法，使您可以通过一次操作将磁盘或文件系统中的文件提供给HTTP响应。 在底层操作系统支持的情况下，这可能会导致OS直接将字节从文件传输到套接字，而根本不通过用户空间进行复制。

这是通过使用`sendFile`完成的，通常对于大文件来说效率更高，但是对于小文件来说可能会更慢。

这是一个非常简单的网络服务器，它使用sendFile为文件系统中的文件提供服务：

```groovy
vertx.createHttpServer().requestHandler({ request ->
  def file = ""
  if (request.path() == "/") {
    file = "index.html"
  } else if (!request.path().contains("..")) {
    file = request.path()
  }
  request.response().sendFile("web/${file}")
}).listen(8080)
```

发送文件是异步的，可能在调用返回后一段时间才会完成。如果你想在写文件时得到通知，你可以使用`sendFile`

请参阅有关[从类路径提供文件](https://vertx.io/docs/vertx-core/java/#classpath)的章节，以获取有关类路径解析或禁用它的限制。

------
> **注意:** 如果您在使用HTTPS时使用`sendFile`，它将通过用户空间进行复制，因为如果内核将数据直接从磁盘复制到套接字，则不会给我们提供任何加密的机会。
>
------

------
> **警告:** 如果您要直接使用Vert.x编写Web服务器，请注意用户不能利用该路径访问您要为其提供服务的目录或类路径之外的文件，而使用Vert.x Web可能更安全。
>
------

如果只需要服务文件的一部分，例如从给定的字节开始，则可以通过执行以下操作来实现：

```groovy
vertx.createHttpServer().requestHandler({ request ->
  def offset = 0
  try {
    offset = java.lang.Long.parseLong(request.getParam("start"))
  } catch(Exception e) {
    // error handling...
  }


  def end = java.lang.Long.MAX_VALUE
  try {
    end = java.lang.Long.parseLong(request.getParam("end"))
  } catch(Exception e) {
    // error handling...
  }


  request.response().sendFile("web/mybigfile.txt", offset, end)
}).listen(8080)
```

你不需要提供长度，如果你想发送一个文件从一个偏移到结束，在这种情况下，你可以做:

```groovy
vertx.createHttpServer().requestHandler({ request ->
  def offset = 0
  try {
    offset = java.lang.Long.parseLong(request.getParam("start"))
  } catch(Exception e) {
    // error handling...
  }


  request.response().sendFile("web/mybigfile.txt", offset)
}).listen(8080)
```

<a name="161_____Pumping_响应"></a>
#### Pumping 响应
服务器响应是一个`WriteStream`实例，因此您可以从任何`ReadStream`， 例如， `AsyncFile`，`NetSocket`，`WebSocket`或`HttpServerRequest`。

这是一个示例，该示例针对任何PUT方法在响应中回显请求正文。 它为主体使用泵，因此即使HTTP请求主体比任何时候都可容纳在内存中的容量大得多，它也将起作用：

```groovy
vertx.createHttpServer().requestHandler({ request ->
  def response = request.response()
  if (request.method() == HttpMethod.PUT) {
    response.setChunked(true)
    Pump.pump(request, response).start()
    request.endHandler({ v ->
      response.end()
    })
  } else {
    response.setStatusCode(400).end()
  }
}).listen(8080)
```

<a name="162_____编写HTTP_2帧"></a>
#### 编写HTTP/2帧
HTTP/2是带有`HTTP请求/响应模型`的各种框架的框架协议。 该协议允许发送和接收其他类型的帧。

要发送这样的帧，可以在响应中使用`writeCustomFrame`。 这是一个例子：

```groovy
def frameType = 40
def frameStatus = 10
def payload = Buffer.buffer("some data")

// Sending a frame to the client
response.writeCustomFrame(frameType, frameStatus, payload)
```

这些帧将立即发送，并且不受流控制-当发送此类帧时，可以在其他{@literal DATA}帧之前完成。

<a name="163_____流重置"></a>
#### 流重置
HTTP/1.x不允许对请求或响应流进行干净的重置，例如，当客户端上传服务器上已经存在的资源时，服务器需要接受整个响应。

HTTP/2支持在请求/响应期间的任何时间进行流重置：

```groovy
// Reset the stream
request.response().reset()
```

默认情况下发送`NO_ERROR` (0)错误代码，可以发送另一个代码:

```groovy
// Cancel the stream
request.response().reset(8)
```

HTTP / 2规范定义了可以使用的[错误代码](http://httpwg.org/specs/rfc7540.html#ErrorCodes)列表。

使用`request handler` 和 `response handler`将流重置事件通知给请求处理程序：

```groovy
request.response().exceptionHandler({ err ->
  if (err instanceof io.vertx.core.http.StreamResetException) {
    def reset = err
    println("Stream reset ${reset.getCode()}")
  }
})
```

<a name="164_____服务器推送"></a>
#### 服务器推送
服务器推送是HTTP/2的一项新功能，它可以为单个客户端请求并行发送多个响应。

服务器处理请求时，可以将请求/响应推送到客户端：

```groovy
def response = request.response()

// Push main.js to the client
response.push(HttpMethod.GET, "/main.js", { ar ->

  if (ar.succeeded()) {

    // The server is ready to push the response
    def pushedResponse = ar.result()

    // Send main.js response
    pushedResponse.putHeader("content-type", "application/json").end("alert(\"Push response hello\")")
  } else {
    println("Could not push client resource ${ar.cause()}")
  }
})

// Send the requested resource
response.sendFile("<html><head><script src=\"/main.js\"></script></head><body></body></html>")
```

当服务器准备好推送响应时，将调用推送响应处理程序，并且该处理程序可以发送响应。

推送响应处理程序可能会收到失败消息，例如，客户端可能会取消推送，因为它的缓存中已经有`main.js`，并且不再需要它。

必须在发起响应结束之前调用`push`方法，但是可以在之后写入被推送的响应。

<a name="165_____处理异常"></a>
#### 处理异常
您可以设置`exceptionHandler`来接收将连接传递给`requestHandler`或`websocketHandler`之前发生的任何异常，例如在TLS握手期间。

<a name="166____HTTP_压缩"></a>
### HTTP 压缩
Vert.x开箱即用地支持HTTP压缩。

这意味着您可以在将响应正文发送回客户端之前自动对其进行压缩。

如果客户端不支持HTTP压缩，则将响应发送回而不压缩主体。

这样可以处理支持HTTP压缩的客户端和不支持HTTP压缩的客户端。

要启用压缩，可以使用`setCompressionSupported`配置它。

默认情况下不启用压缩。

启用HTTP压缩后，服务器将检查客户端是否包含包含支持的压缩的`Accept-Encoding`标头。 常用的是deflate和gzip。 两者均受Vert.x支持。

如果找到这样的标头，则服务器将使用支持的压缩之一自动压缩响应的主体，并将其发送回客户端。

每当需要不加压缩就发送响应时，都可以将标头`content-encoding`设置为`identity`：

```groovy
// Disable compression and send an image
request.response().putHeader(io.vertx.core.http.HttpHeaders.CONTENT_ENCODING, io.vertx.core.http.HttpHeaders.IDENTITY).sendFile("/path/to/image.jpg")
```

请注意，压缩可能会减少网络流量，但会占用更多CPU资源。

为了解决后一个问题，Vert.x允许您调整gzip/deflate压缩算法固有的“compression level”参数。

压缩级别允许根据结果数据的压缩率和压缩/解压缩操作的计算成本来配置gzip/deflate算法。

压缩级别是一个从'1'到'9'的整数值，其中'1'表示较低的压缩率，但算法最快，而'9'表示可用的最大压缩率，但算法较慢。

使用高于1-2的压缩级别通常只能节省一些字节的大小-增益不是线性的，并且取决于要压缩的特定数据-但是对于CPU所需的CPU周期而言，它占用了不可交易的成本。 服务器在生成压缩响应数据时（请注意，目前Vert.x不支持压缩响应数据的任何形式的缓存，即使对于静态文件也是如此，因此压缩是在每个请求正文生成时即时完成的） 与在解码（扩大）接收到的响应时影响客户端的方式相同，级别越高，操作就越占用CPU资源。

默认情况下-如果通过`setCompressionSupported`启用了压缩-Vert.x将使用'6'作为压缩级别，但是该参数可以配置为使用`setCompressionLevel`处理任何情况。

<a name="167____创建一个HTTP客户端"></a>
### 创建一个HTTP客户端
您可以使用以下默认选项创建一个`HttpClient`实例：

```groovy
def client = vertx.createHttpClient()
```

如果要为客户端配置选项，请按以下方式创建它：

```groovy
def options = [
  keepAlive:false
]
def client = vertx.createHttpClient(options)
```

Vert.x通过TLS `h2` 和TCP `h2c`支持HTTP/2。

默认情况下，http客户端执行HTTP/1.1请求，要执行HTTP/2请求，必须将`setProtocolVersion`设置为`HTTP_2`。

对于`h2`请求，必须通过 *应用层协议协商* 启用TLS：

```groovy
def options = [
  protocolVersion:"HTTP_2",
  ssl:true,
  useAlpn:true,
  trustAll:true
]

def client = vertx.createHttpClient(options)
```

对于`h2c`请求，必须禁用TLS，客户端将执行HTTP/1.1请求并尝试升级到HTTP/2：

```groovy
def options = [
  protocolVersion:"HTTP_2"
]

def client = vertx.createHttpClient(options)
```

也可以直接建立`h2c`连接，即在`setHttp2ClearTextUpgrade`选项设置为false时开始连接:建立连接后，客户端将发送HTTP/2连接序言，并期望从服务器接收相同的序言。

http服务器可能不支持HTTP/2，当响应到达时，可以使用`version`检查实际版本。

当客户端连接到HTTP/2服务器时，它将“初始设置”发送到服务器。 这些设置定义服务器如何使用连接，客户端的默认初始设置是HTTP/2 RFC定义的默认值。

<a name="168____记录网络客户端活动"></a>
### 记录网络客户端活动
出于调试目的，可以记录网络活动。

```groovy
def options = [
  logActivity:true
]
def client = vertx.createHttpClient(options)
```

有关详细说明，请参见[记录网络活动](https://vertx.io/docs/vertx-core/java/#logging_network_activity) 一章。

<a name="169____发出请求"></a>
### 发出请求
http客户端非常灵活，可以通过多种方式发出请求。

通常，您想通过http客户端向同一主机/端口发出许多请求。 为避免每次发出请求时都重复主机/端口，可以为客户端配置默认主机/端口：

```groovy
// Set the default host
def options = [
  defaultHost:"wibble.com"
]
// Can also set default port if you want...
def client = vertx.createHttpClient(options)
client.getNow("/some-uri", { response ->
  println("Received response with status code ${response.statusCode()}")
})
```

另外，如果您发现使用同一客户端向不同的主机/端口发出大量请求，则只需在执行请求时指定主机/端口即可。

```groovy
def client = vertx.createHttpClient()

// Specify both port and host name
client.getNow(8080, "myserver.mycompany.com", "/some-uri", { response ->
  println("Received response with status code ${response.statusCode()}")
})

// This time use the default port 80 but specify the host name
client.getNow("foo.othercompany.com", "/other-uri", { response ->
  println("Received response with status code ${response.statusCode()}")
})
```

与客户端发出请求的所有不同方式都支持两种指定主机/端口的方法。

<a name="170_____没有请求正文的简单请求"></a>
#### 没有请求正文的简单请求
通常，您会希望在没有请求正文的情况下发出HTTP请求。 HTTP GET，OPTIONS和HEAD请求通常是这种情况。

使用Vert.x http客户端执行此操作的最简单方法是使用`Now`后缀的方法。 例如`getNow`。

这些方法创建http请求并将其通过单个方法调用发送，并允许您提供一个处理程序，该处理程序将在返回时与http响应一起调用。

```groovy
def client = vertx.createHttpClient()

// Send a GET request
client.getNow("/some-uri", { response ->
  println("Received response with status code ${response.statusCode()}")
})

// Send a GET request
client.headNow("/other-uri", { response ->
  println("Received response with status code ${response.statusCode()}")
})
```

<a name="171_____编写一般请求"></a>
#### 编写一般请求
在其他时间，直到运行时您才知道要发送的请求方法。 对于这种用例，我们提供了通用的请求方法，例如`request`，它允许您在运行时指定HTTP方法：

```groovy
def client = vertx.createHttpClient()

client.request(HttpMethod.GET, "some-uri", { response ->
  println("Received response with status code ${response.statusCode()}")
}).end()

client.request(HttpMethod.POST, "foo-uri", { response ->
  println("Received response with status code ${response.statusCode()}")
}).end("some-data")
```

<a name="172_____编写请求主体"></a>
#### 编写请求主体
有时，您可能希望写入具有正文的请求，或者在发送请求之前希望写入请求头。

为此，您可以调用一种特定的请求方法（例如`post`）或一种通用请求方法（例如`request`）。

这些方法不会立即发送请求，而是返回HttpClientRequest的实例，该实例可用于写入请求正文或写入标头。

以下是使用正文编写POST请求的一些示例：

```groovy
def client = vertx.createHttpClient()

def request = client.post("some-uri", { response ->
  println("Received response with status code ${response.statusCode()}")
})

// Now do stuff with the request
request.putHeader("content-length", "1000")
request.putHeader("content-type", "text/plain")
request.write(body)

// Make sure the request is ended when you're done with it
request.end()

// Or fluently:

client.post("some-uri", { response ->
  println("Received response with status code ${response.statusCode()}")
}).putHeader("content-length", "1000").putHeader("content-type", "text/plain").write(body).end()

// Or event more simply:

client.post("some-uri", { response ->
  println("Received response with status code ${response.statusCode()}")
}).putHeader("content-type", "text/plain").end(body)
```

存在使用UTF-8编码和任何特定编码写入字符串以及写入缓冲区的方法：

```groovy
// Write string encoded in UTF-8
request.write("some data")

// Write string encoded in specific encoding
request.write("some other data", "UTF-16")

// Write a buffer
def buffer = Buffer.buffer()
buffer.appendInt(123).appendLong(245L)
request.write(buffer)
```

如果您只是向HTTP请求写入单个字符串或缓冲区，则可以编写该字符串或缓冲区，并在一次对`end`函数的调用中结束该请求。

```groovy
// Write string and end the request (send it) in a single call
request.end("some simple data")

// Write buffer and end the request (send it) in a single call
def buffer = Buffer.buffer().appendDouble(12.34d).appendLong(432L)
request.end(buffer)
```

当您写入请求时，第一次调用`write`会导致请求标头被写到线路中。

实际的写入是异步的，并且可能要等到调用返回后的一段时间才能发生。

带有请求体的非分块HTTP请求需要提供一个`Content-Length`报头。

因此，如果您不使用chunked HTTP，那么您必须在写入请求之前设置`Content-Length`报头，否则就太晚了。

如果您正在调用采用字符串或缓冲区的`end`方法之一，则Vert.x将在写入请求主体之前自动计算并设置`Content-Length`标头。

如果您使用HTTP分块，则不需要`Content-Length`标头，因此不必预先计算大小。

<a name="173_____编写请求标头"></a>
#### 编写请求标头
您可以使用`headers` multi-map将标头写入请求，如下所示：

```groovy
// Write some headers using the headers() multimap

def headers = request.headers()
headers.set("content-type", "application/json").set("other-header", "foo")
```

标题是`MultiMap`的实例，它提供添加，设置和删除条目的操作。 Http标头为特定键允许多个值。

您也可以使用`putHeader`来写标题

```groovy
// Write some headers using the putHeader method

request.putHeader("content-type", "application/json").putHeader("other-header", "foo")
```

如果您希望将标头写入请求，则必须在写入请求正文的任何部分之前这样做。

<a name="174_____非标准HTTP方法"></a>
#### 非标准HTTP方法
HTTP方法的`OTHER`用于非标准方法，使用此方法时，必须使用`setRawMethod`来设置要发送到服务器的原始方法。

<a name="175_____结束HTTP请求"></a>
#### 结束HTTP请求
完成HTTP请求后，必须以`end`操作之一结束它。

结束请求将导致写入所有标头（如果尚未写入标头）并将请求标记为已完成。

请求可以通过几种方式结束。 没有参数，请求就简单地结束了：

```groovy
request.end()
```

或者可以在对`end`的调用中提供字符串或缓冲区。 这就像在调用不带参数的`end`之前用字符串或缓冲区调用`write`一样

```groovy
// End the request with a string
request.end("some-data")

// End it with a buffer
def buffer = Buffer.buffer().appendFloat(12.3f).appendInt(321)
request.end(buffer)
```

<a name="176_____分块的HTTP请求"></a>
#### 分块的HTTP请求
Vert.x支持[HTTP块传输编码](https://en.wikipedia.org/wiki/Chunked_transfer_encoding) 。

这允许将HTTP请求主体分块编写，并且通常在将大型请求主体流式传输到服务器（事先不知道其大小）时使用。

您可以使用`setChunked`将HTTP请求置于分块模式。

在分块模式下，每次写调用都将导致将一个新的分块写入线路。 在分块模式下，无需预先设置请求的`Content-Length`。

```groovy
request.setChunked(true)

// Write some chunks
(0..<10).each { i ->
  request.write("this-is-chunk-${i}")
}

request.end()
```

<a name="177_____请求超时"></a>
#### 请求超时
您可以使用`setTimeout`为特定的HTTP请求设置超时。

如果请求在超时时间内未返回任何数据，则将异常传递给异常处理程序（如果提供），并且该请求将被关闭。

<a name="178_____处理异常"></a>
#### 处理异常
您可以通过在`HttpClientRequest`实例上设置异常处理程序来处理与请求相对应的异常：

```groovy
def request = client.post("some-uri", { response ->
  println("Received response with status code ${response.statusCode()}")
})
request.exceptionHandler({ e ->
  println("Received exception: ${e.getMessage()}")
  e.printStackTrace()
})
```

这不会处理需要在`HttpClientResponse`代码中处理的非*2xx*响应：

```groovy
def request = client.post("some-uri", { response ->
  if (response.statusCode() == 200) {
    println("Everything fine")
    return
  }
  if (response.statusCode() == 500) {
    println("Unexpected behavior on the server side")
    return
  }
})
request.end()
```

------
> **重要:** `XXXNow`方法不能接收异常处理程序。
>
------

<a name="179_____在客户端请求上指定处理程序"></a>
#### 在客户端请求上指定处理程序
或者，除了在创建客户端请求对象的调用中提供响应处理程序之外，还不能在创建请求时提供处理程序，以后再使用`handler`在请求对象本身上进行设置，例如：

```groovy
def request = client.post("some-uri")
request.handler({ response ->
  println("Received response with status code ${response.statusCode()}")
})
```

<a name="180_____将请求用作流"></a>
#### 将请求用作流
`HttpClientRequest`实例也是一个`WriteStream`，这意味着您可以从任何`ReadStream`实例中将其泵入。

例如，可以将磁盘上的文件泵送到http请求正文，如下所示：

```groovy
request.setChunked(true)
def pump = Pump.pump(file, request)
file.endHandler({ v ->
  request.end()
})
pump.start()
```

<a name="181_____编写HTTP_2帧"></a>
#### 编写HTTP/2帧
HTTP/2是一个框架协议，具有用于HTTP请求/响应模型的各种框架。该协议允许发送和接收其他类型的帧。

要发送这样的帧，您可以对请求使用`write`。这里有一个例子:

```groovy
def frameType = 40
def frameStatus = 10
def payload = Buffer.buffer("some data")

// Sending a frame to the server
request.writeCustomFrame(frameType, frameStatus, payload)
```

<a name="182_____流重置"></a>
#### 流重置
HTTP/1.x不允许对请求或响应流进行干净的重置，例如，当客户机上传服务器上已经存在的资源时，服务器需要接受整个响应。

HTTP/2支持在请求/响应期间的任何时间进行流重置：

```groovy
request.reset()
```

默认情况下NO_ERROR(0)错误代码被发送，另一个代码可以被发送:

```groovy
request.reset(8)
```

HTTP/2规范定义了可以使用的[错误代码](http://httpwg.org/specs/rfc7540.html#ErrorCodes)列表。

使用`request handler`和`response handler`将流重置事件通知给请求处理程序：

```groovy
request.exceptionHandler({ err ->
  if (err instanceof io.vertx.core.http.StreamResetException) {
    def reset = err
    println("Stream reset ${reset.getCode()}")
  }
})
```

<a name="183____处理HTTP响应"></a>
### 处理HTTP响应
您会在请求方法中指定的处理程序中收到`HttpClientResponse`的实例，或者直接在`HttpClientRequest`对象上设置处理程序。

您可以使用`statusCode`和`statusMessage`查询响应的状态码和状态消息。

```groovy
client.getNow("some-uri", { response ->
  // the status code - e.g. 200 or 404
  println("Status code is ${response.statusCode()}")

  // the status message e.g. "OK" or "Not Found".
  println("Status message is ${response.statusMessage()}")
})
```

<a name="184_____将响应作为流使用"></a>
#### 将响应作为流使用
`HttpClientResponse`实例也是一个`ReadStream`，这意味着你可以把它泵到任何`WriteStream`实例。

<a name="185_____响应头和trailers_尾部_"></a>
#### 响应头和trailers(尾部)
Http响应可以包含标头。使用`headers`获取标头。

返回的对象是一个`MultiMap`，因为HTTP头文件可以包含单个键的多个值。

```groovy
def contentType = response.headers().get("content-type")
def contentLength = response.headers().get("content-lengh")
```

块状HTTP响应也可以包含trailers尾部-这些尾部在响应主体的最后一块发送。

您可以使用`trailers`来获取Trailers。 Trailers也是`MultiMap`。

<a name="186_____读取请求正文"></a>
#### 读取请求正文
当从连线读取响应的标头时，将调用响应处理程序。

如果响应有一个主体，那么它可能在读取标题之后的一段时间内以几部分的形式到达。我们不会在调用响应处理程序之前等待所有主体的到来，因为响应可能非常大，我们可能要等待很长时间，或者耗尽内存来处理较大的响应。

当响应体的一部分到达时，调用`handler`，并用`Buffer`表示响应体的一部分:

```groovy
client.getNow("some-uri", { response ->

  response.handler({ buffer ->
    println("Received a part of the response body: ${buffer}")
  })
})
```

如果您知道响应体不是非常大，并希望在处理它之前在内存中聚合它，您可以自己聚合它:

```groovy
client.getNow("some-uri", { response ->

  // Create an empty buffer
  def totalBuffer = Buffer.buffer()

  response.handler({ buffer ->
    println("Received a part of the response body: ${buffer.length()}")

    totalBuffer.appendBuffer(buffer)
  })

  response.endHandler({ v ->
    // Now all the body has been read
    println("Total response body length is ${totalBuffer.length()}")
  })
})
```

或者，您可以使用便捷的`bodyHandler`，当完全读取响应后，将在整个正文中调用它：

```groovy
client.getNow("some-uri", { response ->

  response.bodyHandler({ totalBuffer ->
    // Now all the body has been read
    println("Total response body length is ${totalBuffer.length()}")
  })
})
```

<a name="187_____响应结束处理程序"></a>
#### 响应结束处理程序
当读取了整个响应主体时，或者在读取标头之后立即调用响应`endHandler`，如果没有主体则调用响应处理程序。

<a name="188_____从响应中读取Cookie"></a>
#### 从响应中读取Cookie
您可以使用`cookies`来从响应中检索Cookie列表。

另外，您也可以在响应中自己解析`Set-Cookie`标头。

<a name="189_____30x重定向处理"></a>
#### 30x重定向处理
当客户端收到以下消息时，可以将客户端配置为遵循`Location`响应标头提供的HTTP重定向：

- 一个`301`、`302`、`307`或`308`状态码，以及HTTP GET或HEAD方法
- 一个`303`状态码，另外定向请求执行一个HTTP GET方法

这里有一个例子:

```groovy
client.get("some-uri", { response ->
  println("Received response with status code ${response.statusCode()}")
}).setFollowRedirects(true).end()
```

默认情况下，最大重定向数为`16`，可以通过`setMaxRedirects`进行更改。

```groovy
def client = vertx.createHttpClient([
  maxRedirects:32
])

client.get("some-uri", { response ->
  println("Received response with status code ${response.statusCode()}")
}).setFollowRedirects(true).end()
```

一种尺寸不能满足所有需求，并且默认重定向策略可能无法满足您的需求。

可以使用定制实现来更改默认重定向策略：

```groovy
client.redirectHandler({ response ->

  // Only follow 301 code
  if (response.statusCode() == 301 && response.getHeader("Location") != null) {

    // Compute the redirect URI
    def absoluteURI = this.resolveURI(response.request().absoluteURI(), response.getHeader("Location"))

    // Create a new ready to use request that the client will use
    return Future.succeededFuture(client.getAbs(absoluteURI))
  }

  // We don't redirect
  return null
})
```

该策略处理收到的原始`HttpClientResponse`，并返回`null`或`Future`。

- 当返回`null`时，原始响应被处理
- 当返回future时，请求将在成功完成后发送
- 当返回future时，在请求失败时调用在请求上设置的异常处理程序

返回的请求必须未发送，以便可以发送原始请求处理程序，并且客户端可以在之后发送它。

大多数原始请求设置将传播到新请求：

- 请求标头，除非您设置了一些标头（包括`SetHost`）
- 请求主体，除非返回的请求使用`GET`方法
- 响应处理程序
- 请求异常处理程序
- 请求超时

<a name="190_____100_继续处理"></a>
#### 100-继续处理
根据[HTTP 1.1规范](https://www.w3.org/Protocols/rfc2616/rfc2616-sec8.html) ，客户端可以设置标头`Expect: 100-Continue` ,并在发送请求标头之前发送请求正文的其余部分。

然后，服务器可以使用临时响应状态`Status: 100 (Continue)` 来响应，以向客户端表示可以发送主体的其余部分。

这里的想法是，它允许服务器在发送大量数据之前对请求进行授权和接受/拒绝。如果请求可能不被接受，则发送大量数据是对带宽的浪费，并且会使服务器无法读取它将丢弃的数据。

Vert.x允许您在客户端请求对象上设置`continueHandler`

如果服务器发回`Status: 100 (Continue)`响应以表示可以发送其余请求，则将调用此方法。

它与`[sendHead](https://vertx.io/docs/apidocs/io/vertx/core/http/HttpClientRequest.html#sendHead--)`结合使用，以发送请求的头部。

这是一个例子：

```groovy
def request = client.put("some-uri", { response ->
  println("Received response with status code ${response.statusCode()}")
})

request.putHeader("Expect", "100-Continue")

request.continueHandler({ v ->
  // OK to send rest of body
  request.write("Some data")
  request.write("Some more data")
  request.end()
})
```

在服务器端，可以将Vert.x http服务器配置为在收到`Expect: 100-Continue`标头时自动发送回100 Continue临时响应。

这是通过设置选项`setHandle100ContinueAutomatically`完成的。

如果您希望决定是否手动发送回继续响应，则应将此属性设置为`false`（默认值），然后可以检查标头并调用`writeContinue`以使客户端继续发送正文：

```groovy
httpServer.requestHandler({ request ->
  if (request.getHeader("Expect").equalsIgnoreCase("100-Continue")) {

    // Send a 100 continue response
    request.response().writeContinue()

    // The client should send the body when it receives the 100 response
    request.bodyHandler({ body ->
      // Do something with body
    })

    request.endHandler({ v ->
      request.response().end()
    })
  }
})
```

你也可以通过直接发送一个失败状态代码来拒绝请求:在这种情况下，要么忽略正文，要么关闭连接(100-Continue是一个性能提示，不能是逻辑协议约束):

```groovy
httpServer.requestHandler({ request ->
  if (request.getHeader("Expect").equalsIgnoreCase("100-Continue")) {

    //
    def rejectAndClose = true
    if (rejectAndClose) {

      // Reject with a failure code and close the connection
      // this is probably best with persistent connection
      request.response().setStatusCode(405).putHeader("Connection", "close").end()
    } else {

      // Reject with a failure code and ignore the body
      // this may be appropriate if the body is small
      request.response().setStatusCode(405).end()
    }
  }
})
```

<a name="191_____客户端推送"></a>
#### 客户端推送
服务器推送是HTTP/2的一个新特性，它支持为单个客户机请求并发发送多个响应。

可以在请求上设置推送处理程序，以接收服务器推送的请求/响应：

```groovy
def request = client.get("/index.html", { response ->
  // Process index.html response
})

// Set a push handler to be aware of any resource pushed by the server
request.pushHandler({ pushedRequest ->

  // A resource is pushed for this request
  println("Server pushed ${pushedRequest.path()}")

  // Set an handler for the response
  pushedRequest.handler({ pushedResponse ->
    println("The response for the pushed request")
  })
})

// End the request
request.end()
```

如果客户端不想接收推送的请求，则可以重置流：

```groovy
request.pushHandler({ pushedRequest ->
  if (pushedRequest.path() == "/main.js") {
    pushedRequest.reset()
  } else {
    // Handle it
  }
})
```

如果未设置任何处理程序，则推送的任何流都将由客户端通过流重置（“ 8”错误代码）自动取消。

<a name="192_____接收自定义HTTP_2帧"></a>
#### 接收自定义HTTP/2帧
HTTP/2是一个框架协议，具有用于HTTP请求/响应模型的各种框架。该协议允许发送和接收其他类型的帧。

要接收自定义帧，您可以对请求使用customFrameHandler，它将在每次自定义帧到达时被调用。这里有一个例子:

```groovy
response.customFrameHandler({ frame ->

  println("Received a frame type=${frame.type()} payload${frame.payload().toString()}")
})
```

<a name="193____在客户端上启用压缩"></a>
### 在客户端上启用压缩
http客户端开箱即用地支持HTTP压缩。

这意味着客户端可以让远程http服务器知道它支持压缩，并且能够处理压缩的响应主体。

http服务器可以随意使用一种受支持的压缩算法进行压缩，也可以将主体发送回而不进行任何压缩。 因此，这只是对它可能会随意忽略的Http服务器的提示。

为了告诉http服务器客户端支持哪种压缩，它将包括一个`Accept-Encoding`标头，并将其支持的压缩算法作为值。 支持多种压缩算法。 如果是Vert.x，将导致添加以下标头：
```javascript
Accept-Encoding: gzip, deflate
```
然后，服务器将从其中之一中进行选择。 您可以通过检查服务器发回的响应中的`Content-Encoding`标头来检测服务器是否压缩了正文。

如果响应的主体是通过gzip压缩的，则它将包含例如以下标头：
```javascript
Content-Encoding: gzip
```
要启用压缩，请在创建客户端时使用的选项上设置`setTryUseCompression`。

默认情况下禁用压缩。

<a name="194____HTTP_1_x_连接池_和_保持活动状态"></a>
### HTTP/1.x 连接池 和 保持活动状态
Http保持活动状态允许将HTTP连接用于多个请求。 当您向同一服务器发出多个请求时，可以更有效地使用连接。

对于HTTP / 1.x版本，http客户端支持连接池，从而允许您在请求之间重用连接。

为了使池工作，必须在配置客户端时在选项上使用`setKeepAlive`来保持活动。 默认值是true。

启用保持活动状态时。 Vert.x将向每个发送的HTTP / 1.0请求添加一个`Connection: Keep-Alive`标头。 保持活动状态时被禁用。 Vert.x将在每个发送的HTTP / 1.1请求中添加一个`Connection: Close`标头，以表明响应完成后将关闭连接。

使用`setMaxPoolSize`配置**每个服务器**池的最大连接数

在启用池的情况下发出请求时，如果少于为该服务器创建的最大连接数，Vert.x将创建一个新连接，否则，会将请求添加到队列中。

保持活动连接将在超时后由客户端自动关闭。 服务器可以使用`keep-alive`标头指定超时时间：

```javascript
keep-alive: timeout=30
```

您可以使用`setKeepAliveTimeout`设置默认超时-在此超时时间内未使用的所有连接都将关闭。 请注意，超时值以秒为单位，而不是毫秒。

<a name="195____HTTP_1_1_pipe_lining_流水线_"></a>
### HTTP/1.1 pipe-lining(流水线)
客户端还支持对连接的请求进行pipe-lining。

Pipe-lining表示在先前的响应返回之前，在同一连接上发送了另一个请求。 Pipe-lining不适用于所有请求。

要启用pipe-lining，必须使用`setPipelining`启用它。 默认情况下，pipe-lining是禁用的。

启用pipe-lining后，请求将被写入连接，而无需等待先前的响应返回。

单个连接上的pipe-lined请求数受`setPipeliningLimit`限制。 此选项定义发送到服务器等待响应的http请求的最大数量。 此限制可确保在与同一服务器的连接上分配客户端请求的公平性。

<a name="196____HTTP_2_多路复用"></a>
### HTTP/2 多路复用
HTTP/2提倡使用与服务器的单个连接，默认情况下，http客户端对每个服务器使用一个连接，到同一服务器的所有流都在同一连接上多路复用。

当客户端需要使用多个连接并使用缓冲池时，应使用`setHttp2MaxPoolSize`。

当需要限制每个连接的多路复用流的数量并使用连接池而不是单个连接时，可以使用`setHttp2MultiplexingLimit`。

```groovy
def clientOptions = [
  http2MultiplexingLimit:10,
  http2MaxPoolSize:3
]

// Uses up to 3 connections and up to 10 streams per connection
def client = vertx.createHttpClient(clientOptions)
```

连接的多路复用限制是在客户端上设置的一项设置，用于限制单个连接的流数。 如果服务器使用`SETTINGS_MAX_CONCURRENT_STREAMS`设置下限，则有效值甚至会更低。

客户端不会自动关闭HTTP/2连接。 要关闭它们，您可以调用`close`或关闭客户端实例。

另外，您也可以使用`setIdleTimeout`设置空闲超时-在此超时时间内未使用的任何连接都将关闭。 请注意，空闲超时值以秒为单位，而不是毫秒。

<a name="197____HTTP_连接"></a>
### HTTP 连接
`HttpConnection`提供了用于处理HTTP连接事件，生命周期和设置的API。

HTTP/2完全实现了`HttpConnection` API。

HTTP/1.x部分实现了`HttpConnection` API：仅实现了close操作，close处理程序和异常处理程序。 该协议不提供其他操作的语义。

<a name="198_____Server_连接"></a>
#### Server 连接
`connection`方法返回服务器上的请求连接:

```groovy
def connection = request.connection()
```

可以在服务器上设置连接处理程序，以通知任何传入的连接：

```groovy
def server = vertx.createHttpServer(http2Options)

server.connectionHandler({ connection ->
  println("A client connected")
})
```

<a name="199_____Client_连接"></a>
#### Client 连接
`connection`方法在客户端上返回请求连接：

```groovy
def connection = request.connection()
```

可以在请求上设置连接处理程序，以在发生连接时通知：

```groovy
request.connectionHandler({ connection ->
  println("Connected to the server")
})
```

<a name="200_____Connection_设置"></a>
#### Connection 设置
HTTP/2的配置由`Http2Settings`数据对象配置。

每个端点都必须遵守连接另一端发送的设置。

建立连接后，客户端和服务器将交换初始设置。 初始设置由客户端上的`setInitialSettings`和服务器上的`setInitialSettings`配置。

建立连接后，可以随时更改设置：

```groovy
connection.updateSettings([
  maxConcurrentStreams:100
])
```

由于远程端应在收到设置更新后进行确认，因此有可能向回调发送确认通知：

```groovy
connection.updateSettings([
  maxConcurrentStreams:100
], { ar ->
  if (ar.succeeded()) {
    println("The settings update has been acknowledged ")
  }
})
```

相反，当接收到新的远程设置时，将通知`remoteSettingsHandler`：

```groovy
connection.remoteSettingsHandler({ settings ->
  println("Received new settings")
})
```

------
> **注意:** 这仅适用于HTTP/2协议
>
------

<a name="201_____连接_ping"></a>
#### 连接 ping
HTTP/2连接`ping`对确定连接往返时间或检查连接有效性很有用：`ping`将`{@literal PING}`帧发送到远程端点：

```groovy
def data = Buffer.buffer()
(0..<8).each { i ->
  data.appendByte(i)
}
connection.ping(data, { pong ->
  println("Remote side replied")
})
```

当收到`{@literal PING}`帧时，Vert.x会自动发送确认，可以将处理程序设置为针对每次收到的ping通知：

```groovy
connection.pingHandler({ ping ->
  println("Got pinged by remote side")
})
```

只是通知处理程序，发送确认消息。 此类功能旨在实现基于HTTP/2的协议。

------
> **注意:** 这仅适用于HTTP/2协议
>
------

<a name="202_____连接关闭并消失"></a>
#### 连接关闭并消失
调用`shutdown`会将{@literal GOAWAY}帧发送到连接的远程端，要求它停止创建流：客户端将停止发出新请求，而服务器将停止推送响应。 发送{@literal GOAWAY}帧后，连接将等待一段时间（默认为30秒），直到所有当前流关闭并关闭连接为止：

```groovy
connection.shutdown()
```

`shutdownHandler`通知当所有流都已关闭时，连接尚未关闭。

可以只发送{@literal GOAWAY}帧，而关闭则主要区别在于它只会告诉连接的远程端停止创建新的流，而无需安排关闭连接：

```groovy
connection.goAway(0)
```

反过来，也可能在收到{@literal GOAWAY}时得到通知：

```groovy
connection.goAwayHandler({ goAway ->
  println("Received a go away frame")
})
```

当所有当前流都已关闭并且连接可以关闭时，将调用`shutdownHandler`：

```groovy
connection.goAway(0)
connection.shutdownHandler({ v ->

  // All streams are closed, close the connection
  connection.close()
})
```

当收到{@literal GOAWAY}时，这也适用。

------
> **注意:** this only applies to the HTTP/2 protocol
>
------

<a name="203_____连接关闭"></a>
#### 连接关闭
连接 `close` 关闭连接:

- 它关闭HTTP/1.x的套接字
- 如果关闭HTTP/2没有任何延迟，则在关闭连接之前仍将发送{@literal GOAWAY}帧。 

`closeHandler`通知连接何时关闭。

<a name="204____HttpClient_用法"></a>
### HttpClient 用法
HttpClient可以在Vertical或嵌入式中使用。

在Verticle中使用时，Verticle**应该使用其自己的客户端实例**。

通常，不应在不同的Vert.x上下文之间共享客户端，因为它可能导致意外行为。

例如，保持连接将在打开连接的请求的上下文中调用客户端处理程序，后续请求将使用相同的上下文。

发生这种情况时，Vert.x会检测到并记录警告：

```
Reusing a connection with a different context: an HttpClient is probably shared between different Verticles
```

HttpClient可以嵌入在非Vert.x线程中，例如单元测试或普通的Java`main`：客户端处理程序将由不同的Vert.x线程和上下文调用，此类上下文是根据需要创建的。 对于生产，不建议使用此用法。

<a name="205____Server_sharing"></a>
### Server sharing
Server 共享
当多个HTTP服务器在同一端口上侦听时，vert.x使用循环策略来协调请求处理。

让我们使用verticle创建一个HTTP服务器，如:`io.vertx.examples.http.sharing.HttpServerVerticle`

```groovy
vertx.createHttpServer().requestHandler({ request ->
  request.response().end("Hello from server ${this}")
}).listen(8080)
```

该服务正在8080端口上侦听。那么，当verticle被多次实例化时，例如：`vertx run io.vertx.examples.http.sharing.HttpServerVerticle -instances 2`，这是怎么回事？ 如果两个verticle都绑定到同一端口，则会收到套接字异常。幸运的是，vert.x正在为您处理这种情况。 当您在与现有服务器相同的主机和端口上部署另一台服务时，实际上并不会尝试创建在同一主机/端口上侦听的新服务器。 它仅绑定一次到套接字。 收到请求后，它将遵循循环策略来调用服务器处理程序。

现在，假设有一个客户端，例如：

```groovy
vertx.setPeriodic(100, { l ->
  vertx.createHttpClient().getNow(8080, "localhost", "/", { resp ->
    resp.bodyHandler({ body ->
      println(body.toString("ISO-8859-1"))
    })
  })
})
```

Vert.x将请求顺序地委派给其中一台服务器：

```
Hello from i.v.e.h.s.HttpServerVerticle@1
Hello from i.v.e.h.s.HttpServerVerticle@2
Hello from i.v.e.h.s.HttpServerVerticle@1
Hello from i.v.e.h.s.HttpServerVerticle@2
...
```

因此，服务器可以扩展可用内核，而每个Vert.x Verticle实例严格保持单线程运行，并且无需执行任何特殊技巧即可编写负载平衡器，从而在多核计算机上扩展服务器。

<a name="206____将HTTPS与Vert_x一起使用"></a>
### 将HTTPS与Vert.x一起使用
可以将Vert.x http服务器和客户端配置为以与网络服务器完全相同的方式使用HTTPS。

有关更多信息，请参见[配置网络服务器以使用SSL](https://vertx.io/docs/vertx-core/java/#ssl)。

还可以使用`RequestOptions`或通过使用`requestAbs`方法指定方案时启用/禁用SSL。

```groovy
client.getNow([
  host:"localhost",
  port:8080,
  uRI:"/",
  ssl:true
], { response ->
  println("Received response with status code ${response.statusCode()}")
})
```

`setSsl`设置用作默认客户端设置。

`setSsl`覆盖默认的客户端设置

- 将值设置为`false`将禁用SSL/TLS，即使将客户端配置为使用SSL/TLS
- 将值设置为`true`将启用SSL/TLS，即使将客户端配置为不使用SSL/TLS，实际的客户端SSL/TLS（例如信任，密钥/证书，密码，ALPN等）也将被重用

同样，`requestAbs`方案也将覆盖默认的客户端设置。

<a name="207_____服务器名称指示__SNI_"></a>
#### 服务器名称指示 (SNI)
可以将Vert.x http服务器配置为使用{@linkplain io.vertx.core.net 网络服务器}完全相同的方式来使用SNI。

Vert.x http客户端将在TLS握手期间将实际的主机名显示为*服务器名*。

<a name="208____WebSockets"></a>
### WebSockets
[WebSockets](https://en.wikipedia.org/wiki/WebSocket)是一种Web技术，它允许HTTP服务器和HTTP客户端（通常是浏览器）之间的全双工套接字式连接。

Vert.x在客户端和服务器端都支持WebSocket。

<a name="209_____服务器上的WebSocket"></a>
#### 服务器上的WebSocket
在服务器端有两种处理WebSocket的方法。

<a name="210______WebSocket处理程序"></a>
##### WebSocket处理程序
第一种方法涉及在服务器实例上提供一个`websocketHandler`。

当与服务器建立WebSocket连接时，将调用处理程序，并传入一个`ServerWebSocket`实例。

```groovy
server.websocketHandler({ websocket ->
  println("Connected!")
})
```

您可以通过调用`reject`选择拒绝WebSocket。

```groovy
server.websocketHandler({ websocket ->
  if (websocket.path() == "/myapi") {
    websocket.reject()
  } else {
    // Do something
  }
})
```

您可以通过调用带有`Future`的`setHandshake`来执行异步握手：

```groovy
server.websocketHandler({ websocket ->
  def promise = Promise.promise()
  websocket.setHandshake(promise)
  this.authenticate(websocket, { ar ->
    if (ar.succeeded()) {
      // Terminate the handshake with the status code 101 (Switching Protocol)
      // Reject the handshake with 401 (Unauthorized)
      promise.complete(ar.succeeded() ? 101 : 401)
    } else {
      // Will send a 500 error
      promise.fail(ar.cause())
    }
  })
})
```

------
> **注意:** 除非已设置WebSocket的握手，否则调用处理程序后，WebSocket将被自动接受
>
------

<a name="211______升级到WebSocket"></a>
##### 升级到WebSocket
处理WebSockets的第二种方法是处理从客户端发送的HTTP升级请求，并在服务器请求上调用`upgrade`。

```groovy
server.requestHandler({ request ->
  if (request.path() == "/myapi") {

    def websocket = request.upgrade()
    // Do something

  } else {
    // Reject
    request.response().setStatusCode(400).end()
  }
})
```

<a name="212______服务器WebSocket"></a>
##### 服务器WebSocket
`ServerWebSocket`实例使您能够检索WebSocket握手的HTTP请求的`headers`，`path`，`query`和`URI`。

<a name="213_____客户端上的WebSockets"></a>
#### 客户端上的WebSockets
Vert.x的`HttpClient`支持WebSockets。

您可以使用`webSocket`操作之一并提供处理程序将WebSocket连接到服务器。

建立连接后，将使用`WebSocket`实例调用处理程序：

```groovy
client.webSocket("/some-uri", { res ->
  if (res.succeeded()) {
    def ws = res.result()
    println("Connected!")
  }
})
```

<a name="214_____将消息写入WebSockets"></a>
#### 将消息写入WebSockets
如果您希望将单个WebSocket消息写入WebSocket，则可以使用`writeBinaryMessage`或`writeTextMessage`来实现：

```groovy
// Write a simple binary message
def buffer = Buffer.buffer().appendInt(123).appendFloat(1.23f)
websocket.writeBinaryMessage(buffer)

// Write a simple text message
def message = "hello"
websocket.writeTextMessage(message)
```

如果WebSocket消息大于使用`setMaxWebsocketFrameSize`配置的最大Websocket帧大小，则Vert.x会将其拆分为多个WebSocket帧，然后通过网络发送。

<a name="215_____将frames写入WebSocket"></a>
#### 将frames写入WebSocket
WebSocket消息可以由多个frames组成。 在这种情况下，第一帧是 *binary* 或 *text* 帧，然后是零个或多个 *continuation* 帧。

消息中的最后一帧被标记为*final*。

要发送包含多个frames的消息，您可以使用`WebSocketFrame.binaryFrame`，`WebSocketFrame.textFrame`或`WebSocketFrame.continuationFrame`创建frames，然后使用`writeFrame`将它们写入WebSocket。

这是二进制帧的示例：

```groovy
def frame1 = WebSocketFrame.binaryFrame(buffer1, false)
websocket.writeFrame(frame1)

def frame2 = WebSocketFrame.continuationFrame(buffer2, false)
websocket.writeFrame(frame2)

// Write the final frame
def frame3 = WebSocketFrame.continuationFrame(buffer2, true)
websocket.writeFrame(frame3)
```

在很多情况下，你只是想发送一个websocket消息，它由一个单一的final帧组成，所以我们提供了两个快捷方法来实现这一点，分别是`writeFinalBinaryFrame` 和 `writeFinalTextFrame`。

这里有一个例子:

```groovy
// Send a websocket messages consisting of a single final text frame:

websocket.writeFinalTextFrame("Geronimo!")

// Send a websocket messages consisting of a single final binary frame:

def buff = Buffer.buffer().appendInt(12).appendString("foo")

websocket.writeFinalBinaryFrame(buff)
```

<a name="216_____从WebSockets读取frames"></a>
#### 从WebSockets读取frames
要从WebSocket读取帧，请使用`frameHandler`。

当帧到达时，将使用`WebSocketFrame`实例调用帧处理程序，例如：

```groovy
websocket.frameHandler({ frame ->
  println("Received a frame of size!")
})
```

<a name="217_____关闭_WebSockets"></a>
#### 关闭 WebSockets
完成后，使用`close`关闭WebSocket连接。

<a name="218_____流式WebSocket"></a>
#### 流式WebSocket
WebSocket实例也是`ReadStream`和`WriteStream`实例，因此可以与泵一起使用。

当将WebSocket用作写入流或读取流时，它只能与WebSockets连接一起使用，该连接用于二进制帧，而二进制帧不会拆分为多个帧。

<a name="219____使用代理进行HTTP_HTTPS连接"></a>
### 使用代理进行HTTP/HTTPS连接
http客户端支持通过HTTP代理（例如Squid）或 *SOCKS4a* 或 *SOCKS5* 代理访问http/https URL。 CONNECT协议使用HTTP/1.x，但可以连接到HTTP/1.x和HTTP/2服务器。

http代理可能不支持连接到h2c（未加密的HTTP/2服务器），因为它们仅支持HTTP/1.1。

可以通过设置包含代理类型，主机名，端口以及用户名和密码（可选）的`ProxyOptions`对象在`HttpClientOptions`中配置代理。

这是使用HTTP代理的示例：

```groovy
def options = [
  proxyOptions:[
    type:"HTTP",
    host:"localhost",
    port:3128,
    username:"username",
    password:"secret"
  ]
]
def client = vertx.createHttpClient(options)
```

客户端连接到http URL时，它将连接到代理服务器并在HTTP请求中提供完整URL("GET http://www.somehost.com/path/file.html HTTP/1.1")。

客户端连接到https URL时，它要求代理使用CONNECT方法创建到远程主机的隧道。

对于SOCKS5代理：

```groovy
def options = [
  proxyOptions:[
    type:"SOCKS5",
    host:"localhost",
    port:1080,
    username:"username",
    password:"secret"
  ]
]
def client = vertx.createHttpClient(options)
```

DNS解析始终在代理服务器上完成，为了实现SOCKS4客户端的功能，必须在本地解析DNS地址。

<a name="220_____其他协议的处理"></a>
#### 其他协议的处理
如果代理支持，HTTP代理实现支持获取`ftp:// urls`，这在非代理getAbs请求中不可用。

```groovy
def options = [
  proxyOptions:[
    type:"HTTP"
  ]
]
def client = vertx.createHttpClient(options)
client.getAbs("ftp://ftp.gnu.org/gnu/", { response ->
  println("Received response with status code ${response.statusCode()}")
})
```

对其他协议的支持是不可用的，因为`java.net.URL`不支持它们(例如gopher://)。

<a name="221____verticles自动清理"></a>
### verticles自动清理
如果您是从Verticle内部创建http服务器和客户端，则取消部署Verticle时，这些服务器和客户端将自动关闭。

<a name="222___使用SharedData_API"></a>
## 使用SharedData API
顾名思义，`SharedData` API可让您安全地在以下之间共享数据：

- 应用程序的不同部分，或者
- 同一Vert.x实例中的不同应用程序，或
- Vert.x实例群集中的不同应用程序。

实际上，它提供：

- 同步 map（仅限本地）
- 异步 maps
- 异步 locks
- 异步 counters

------
> **重要:** 分布式数据结构的行为取决于您使用的集群管理器。 群集管理器及其配置定义了面对网络分区时的备份（复制）和行为。 请参考集群管理器文档以及基础框架手册。
>
------

<a name="223____Local_maps"></a>
### Local maps
本地 maps
`Local maps` 可让您在同一Vert.x实例中的不同事件循环（例如，不同的verticles）之间安全地共享数据。

它们仅允许将某些数据类型用作键和值：

- 不可变的类型（例如字符串，布尔值，…等），或
- 实现`Shareable`接口的类型（buffers，JSON数组，JSON对象或您自己的可共享对象）。

在后一种情况下，将先将键/值复制到map中。

这样，我们可以确保Vert.x应用程序中的不同线程之间没有对可变状态的共享访问。 您不必担心通过同步访问状态来保护该状态。

这是使用共享local map的示例：

```groovy
def sharedData = vertx.sharedData()

def map1 = sharedData.getLocalMap("mymap1")

map1.put("foo", "bar")

def map2 = sharedData.getLocalMap("mymap2")

map2.put("eek", Buffer.buffer().appendInt(123))

// Then... in another part of your application:

map1 = sharedData.getLocalMap("mymap1")

def val = map1.get("foo")

map2 = sharedData.getLocalMap("mymap2")

def buff = map2.get("eek")
```

<a name="224____异步_shared_maps"></a>
### 异步 shared maps
`Asynchronous shared maps`允许将数据放入map中，并在本地或从任何其他节点检索。

这使得它们对于将会话状态存储在托管Vert.x Web应用程序的服务器场中非常有用。

获取map是异步的，结果将在您指定的处理程序中返回给您。 这是一个例子：

```groovy
def sharedData = vertx.sharedData()

sharedData.getAsyncMap("mymap", { res ->
  if (res.succeeded()) {
    def map = res.result()
  } else {
    // Something went wrong!
  }
})
```

对Vert.x进行群集时，您可以在本地以及任何其他群集成员上访问放入map的数据。

------
> **重要:** 在集群模式下，asynchronous shared maps依赖于集群管理器提供的分布式数据结构。 请注意，asynchronous shared maps操作，在集群中的延迟可能比在本地模式下高得多。
>
------

如果您的应用程序不需要与其他所有节点共享数据，则可以检索仅限本地的local-only map：

```groovy
def sharedData = vertx.sharedData()

sharedData.getLocalAsyncMap("mymap", { res ->
  if (res.succeeded()) {
    // Local-only async map
    def map = res.result()
  } else {
    // Something went wrong!
  }
})
```

<a name="225_____将数据放入map"></a>
#### 将数据放入map
你用`put`把数据放到map上。

实际的放置是异步的，并且在完成后会通知处理程序：

```groovy
map.put("foo", "bar", { resPut ->
  if (resPut.succeeded()) {
    // Successfully put the value
  } else {
    // Something went wrong!
  }
})
```

<a name="226_____从map获取数据"></a>
#### 从map获取数据
您可以通过`get`从map中获取数据。

实际的get是异步的，并在一段时间后将结果通知处理程序：

```groovy
map.get("foo", { resGet ->
  if (resGet.succeeded()) {
    // Successfully got the value
    def val = resGet.result()
  } else {
    // Something went wrong!
  }
})
```

<a name="227______其它_map_操作"></a>
##### 其它 map 操作
您还可以从异步map中删除条目，清除它们并获取大小。

有关map操作的详细列表，请参见API文档。

<a name="228____Asynchronous_锁"></a>
### Asynchronous 锁
`Asynchronous locks(异步锁)`允许您在本地或整个集群中获取排他锁。 当您想随时执行某项操作或仅在群集的一个节点上访问资源时，此功能很有用。

异步锁具有异步API，这与大多数锁API不同，后者会阻塞调用线程，直到获得锁为止。

要获取锁，请使用`getLock`。 这不会阻止，但是当锁可用时，将使用`Lock`实例调用处理程序，表示您现在拥有该锁。

当您拥有该锁时，本地或群集上的其他任何调用者都将无法获得该锁。

锁定完成后，您可以调用`release`来释放它，以便另一个调用者可以获取它：

```groovy
def sharedData = vertx.sharedData()

sharedData.getLock("mylock", { res ->
  if (res.succeeded()) {
    // Got the lock!
    def lock = res.result()

    // 5 seconds later we release the lock so someone else can get it

    vertx.setTimer(5000, { tid ->
      lock.release()
    })

  } else {
    // Something went wrong
  }
})
```

您还可以获得带有超时的锁。如果在超时时间内未能获得锁，处理程序将调用一个失败:

```groovy
def sharedData = vertx.sharedData()

sharedData.getLockWithTimeout("mylock", 10000, { res ->
  if (res.succeeded()) {
    // Got the lock!
    def lock = res.result()

  } else {
    // Failed to get lock
  }
})
```

有关锁操作的详细列表，请参阅`API文档`。

------
> **重要:** 在集群模式下，异步锁依赖于集群管理器提供的分布式数据结构。 请注意，群集中的相对于异步共享锁操作的延迟可能比本地模式下的延迟高得多。
>
------

如果您的应用程序不需要与其他所有节点共享该锁，则可以检索仅本地锁：

```groovy
def sharedData = vertx.sharedData()

sharedData.getLocalLock("mylock", { res ->
  if (res.succeeded()) {
    // Local-only lock
    def lock = res.result()

    // 5 seconds later we release the lock so someone else can get it

    vertx.setTimer(5000, { tid ->
      lock.release()
    })

  } else {
    // Something went wrong
  }
})
```

<a name="229____异步计数器"></a>
### 异步计数器
在本地或跨应用程序的不同节点维护原子计数器通常很有用。

你可以通过`Counter`来完成。

您可以使用`getCounter`获取实例：

```groovy
def sharedData = vertx.sharedData()

sharedData.getCounter("mycounter", { res ->
  if (res.succeeded()) {
    def counter = res.result()
  } else {
    // Something went wrong!
  }
})
```

拥有实例后，您可以检索当前计数，以原子方式递增，递减并使用各种方法为其添加值。

有关计数器操作的详细列表，请参见`API文档`。

------
> **重要:** 在集群模式下，异步计数器依赖于集群管理器提供的分布式数据结构。 请注意，相对于异步共享计数器操作，在集群中的延迟可能比在本地模式下高得多。
>
------

如果您的应用程序不需要与其他节点共享计数器，则可以检索仅本地计数器：

```groovy
def sharedData = vertx.sharedData()

sharedData.getLocalCounter("mycounter", { res ->
  if (res.succeeded()) {
    // Local-only counter
    def counter = res.result()
  } else {
    // Something went wrong!
  }
})
```

<a name="230___在Vert_x中使用文件系统"></a>
## 在Vert.x中使用文件系统
Vert.x `FileSystem` 对象提供了许多操作文件系统的操作。

每个Vert.x实例只有一个文件系统对象，您可以通过`fileSystem`获取它。

提供了每个操作的阻塞版本和非阻塞版本。 非阻塞版本采用一个处理程序，当操作完成或发生错误时将调用该处理程序。

下面是一个文件的异步拷贝一个例子：

```groovy
def fs = vertx.fileSystem()

// Copy file from foo.txt to bar.txt
fs.copy("foo.txt", "bar.txt", { res ->
  if (res.succeeded()) {
    // Copied ok!
  } else {
    // Something went wrong
  }
})
```

阻塞版本名为`xxxBlocking`，并直接返回结果或引发异常。 在许多情况下，根据操作系统和文件系统的不同，某些可能会阻塞的操作会很快返回，这就是我们提供它们的原因，但是强烈建议您在使用它们之前测试它们返回特定应用程序需要花费多长时间。 从事件循环开始，以免破坏黄金法则。

这是使用阻塞API的复制：

```groovy
def fs = vertx.fileSystem()

// Copy file from foo.txt to bar.txt synchronously
fs.copyBlocking("foo.txt", "bar.txt")
```

存在许多操作来复制，移动，截断，chmod和许多其他文件操作。 我们不会在此处列出所有列表，请查看`API docs`以获取完整列表。

我们来看几个使用异步方法的示例：

```groovy
// Read a file
vertx.fileSystem().readFile("target/classes/readme.txt", { result ->
  if (result.succeeded()) {
    println(result.result())
  } else {
    System.err.println("Oh oh ...${result.cause()}")
  }
})

// Copy a file
vertx.fileSystem().copy("target/classes/readme.txt", "target/classes/readme2.txt", { result ->
  if (result.succeeded()) {
    println("File copied")
  } else {
    System.err.println("Oh oh ...${result.cause()}")
  }
})

// Write a file
vertx.fileSystem().writeFile("target/classes/hello.txt", Buffer.buffer("Hello"), { result ->
  if (result.succeeded()) {
    println("File written")
  } else {
    System.err.println("Oh oh ...${result.cause()}")
  }
})

// Check existence and delete
vertx.fileSystem().exists("target/classes/junk.txt", { result ->
  if (result.succeeded() && result.result()) {
    vertx.fileSystem().delete("target/classes/junk.txt", { r ->
      println("File deleted")
    })
  } else {
    System.err.println("Oh oh ... - cannot delete the file: ${result.cause()}")
  }
})
```

<a name="231____Asynchronous_文件"></a>
### Asynchronous 文件
Vert.x提供了异步文件抽象，使您可以操纵文件系统上的文件。

您可以如下打开`AsyncFile`：

```groovy
def options = [:]
fileSystem.open("myfile.txt", options, { res ->
  if (res.succeeded()) {
    def file = res.result()
  } else {
    // Something went wrong!
  }
})
```

`AsyncFile`实现了`ReadStream`和`WriteStream`，因此您可以将文件与其他流对象（例如，网络套接字，http请求和响应以及WebSocket）进行*pump(泵)*。

它们还允许您直接对其进行读写。

<a name="232_____随机读取"></a>
#### 随机读取
要将`AsyncFile`用于随机访问，请使用`read`方法。

该方法的参数为：

- `buffer`: 数据将被读入的缓冲区。
- `offset`: 放入读取数据的缓冲区的整数偏移量。
- `position`: 文件中从中读取数据的位置。
- `length`: 要读取的数据字节数
- `handler`: 结果处理程序

这是随机读取的示例：

```groovy
vertx.fileSystem().open("target/classes/hello.txt", [:], { result ->
  if (result.succeeded()) {
    def file = result.result()
    def buff = Buffer.buffer("foo")
    (0..<5).each { i ->
      file.write(buff, buff.length() * i, { ar ->
        if (ar.succeeded()) {
          println("Written ok!")
          // etc
        } else {
          System.err.println("Failed to write: ${ar.cause()}")
        }
      })
    }
  } else {
    System.err.println("Cannot open file ${result.cause()}")
  }
})
```

<a name="233_____打开选项"></a>
#### 打开选项
当打开`AsyncFile`时，您传递`OpenOptions`实例。 这些选项描述了文件访问的行为。 例如，您可以使用`setRead`，`setWrite`和`setPerms`方法配置文件权限。

如果打开的文件已经存在，则您也可以配置行为：`setCreateNew`和`setTruncateExisting`。

您也可以使用`setDeleteOnClose`标记关闭或关闭JVM时要删除的文件。

<a name="234_____数据刷新到下面的储存"></a>
#### 数据刷新到下面的储存
在`OpenOptions`中，您可以使用`setDsync`启用/禁用每次写入时内容的自动同步。 在这种情况下，您可以通过调用`flush`方法来手动清除OS缓存中的所有写入。

也可以使用处理程序来调用此方法，该处理程序将在刷新完成后调用。

<a name="235_____使用AsyncFile作为ReadStream和WriteStream"></a>
#### 使用AsyncFile作为ReadStream和WriteStream
`AsyncFile`实现`ReadStream`和`WriteStream`。 然后，您可以将它们与*pump*一起使用，以与其他读取和写入流之间来回传输数据。 例如，这会将内容复制到另一个`AsyncFile`中：

```groovy
def output = vertx.fileSystem().openBlocking("target/classes/plagiary.txt", [:])

vertx.fileSystem().open("target/classes/les_miserables.txt", [:], { result ->
  if (result.succeeded()) {
    def file = result.result()
    Pump.pump(file, output).start()
    file.endHandler({ r ->
      println("Copy done")
    })
  } else {
    System.err.println("Cannot open file ${result.cause()}")
  }
})
```

您也可以使用*pump*将文件内容写入HTTP响应，或更普遍的是在任何`WriteStream`中。

<a name="236_____从类路径访问文件"></a>
#### 从类路径访问文件
当vert.x在文件系统上找不到文件时，它将尝试从类路径中解析文件。 注意，类路径资源路径从不以`/`开头。

由于Java不提供对类路径资源的异步访问的事实，因此，当首次访问类路径资源并从那里异步提供服务时，该文件将被复制到工作线程中的文件系统中。 第二次访问同一资源时，来自文件系统的文件将直接从文件系统提供服务。 即使类路径资源发生变化（例如，在开发系统中），原始内容仍会提供。

可以在`setFileCachingEnabled`选项上设置这种缓存行为。 除非定义了系统属性`vertx.disableFileCaching`，否则该选项的默认值为`true`。

缓存文件的路径默认为`.vertx`，可以通过设置系统属性`vertx.cacheDirBase`进行自定义。

通过将系统属性`vertx.disableFileCPCPResolving`设置为`true`，可以在整个系统范围内禁用整个类路径解析功能。

------
> **注意:** 加载`io.vertx.core.file.FileSystemOptions`类时，将对这些系统属性进行求值一次，因此，应在加载此类之前设置这些属性，或在启动它时将其设置为JVM系统属性。
>
------

如果要禁用特定应用程序的类路径解析，但默认情况下在系统范围内将其保持启用状态，则可以通过`setClassPathResolvingEnabled`选项来启用。

<a name="237_____关闭一个AsyncFile"></a>
#### 关闭一个AsyncFile
要关闭`AsyncFile`，请调用`close`方法。 关闭是异步的，如果您希望在关闭完成时收到通知，则可以将处理函数指定为参数。

<a name="238___数据报套接字_UDP_"></a>
## 数据报套接字(UDP)
将用户数据报协议（UDP）与Vert.x结合使用很容易。

UDP是无连接传输，这基本上意味着您没有与远程对等方的持久连接。

相反，您可以发送和接收程序包，并且每个程序包中都包含远程地址。

除此之外，UDP使用起来不如TCP安全，这意味着根本不能保证发送的数据报包会收到它的端点。

唯一可以保证的是，它要么接收完整，要么完全接收不到。

另外，您通常不能发送比网络接口的MTU大小更大的数据，这是因为每个包将作为一个包发送。

但是请注意，即使包的大小小于MTU，它仍然可能失败。

失败的规模取决于操作系统等。所以经验法则是尽量发送小数据包。

由于UDP的特性，它最适合允许您丢弃数据包的应用程序(例如监视应用程序)。

其优点是与TCP相比，它的开销要小得多，而TCP可以由NetServer和NetClient来处理(参见上面)。

<a name="239____创建一个DatagramSocket"></a>
### 创建一个DatagramSocket
要使用UDP，你首先需要创建一个`DatagramSocket`。在这里，您是否只想发送数据或发送和接收数据并不重要。

```groovy
def socket = vertx.createDatagramSocket([:])
```

返回的`DatagramSocket`将不会绑定到特定的端口。如果您只想发送数据(如客户端)，这不是问题，但在下一节中会详细介绍。

<a name="240____发送_Datagram_packets"></a>
### 发送 Datagram packets
如前所述，用户数据报协议(User Datagram Protocol, UDP)以数据包的形式向远程对等方发送数据，但不以持久的方式连接到它们。

这意味着每个包可以发送到不同的远程对等点。

发送数据包很简单，如下图所示:

```groovy
def socket = vertx.createDatagramSocket([:])
def buffer = Buffer.buffer("content")
// Send a Buffer
socket.send(buffer, 1234, "10.0.0.1", { asyncResult ->
  println("Send succeeded? ${asyncResult.succeeded()}")
})
// Send a String
socket.send("A string used as content", 1234, "10.0.0.1", { asyncResult ->
  println("Send succeeded? ${asyncResult.succeeded()}")
})
```

<a name="241____接收_Datagram_packets"></a>
### 接收 Datagram packets
如果你想接收数据包，你需要绑定`DatagramSocket`通过调用`listen(…)`。

通过这种方式，您将能够接收发送到`DatagramSocket`侦听的地址和端口的`DatagramPacket`。

除此之外，您还需要设置一个`Handler`，它将为每个接收到的`DatagramPacket`调用。

`DatagramPacket`有以下方法:

- `sender`: 代表包的发送者的InetSocketAddress
- `data`: 保存接收到的数据的缓冲区。

因此，要监听特定的地址和端口，您需要做如下操作:

```groovy
def socket = vertx.createDatagramSocket([:])
socket.listen(1234, "0.0.0.0", { asyncResult ->
  if (asyncResult.succeeded()) {
    socket.handler({ packet ->
      // Do something with the packet
    })
  } else {
    println("Listen failed${asyncResult.cause()}")
  }
})
```

注意，即使{code AsyncResult}成功了，也只意味着它可能被写在网络堆栈上，而不能保证它曾经或将要到达远程对等点。

如果您需要这样的保证，那么您需要使用TCP，并在其上构建一些握手逻辑。

<a name="242____多播"></a>
### 多播
<a name="243_____发送多播数据包"></a>
#### 发送多播数据包
多播允许多个套接字接收相同的数据包。这是通过让套接字加入相同的多播组来实现的，然后您可以向该组发送数据包。

我们将在下一节讨论如何加入多播组并接收数据包。

发送多播包与发送正常的数据报包没有什么不同。不同之处在于，您将多播组地址传递给send方法。

这里显示的是:

```groovy
def socket = vertx.createDatagramSocket([:])
def buffer = Buffer.buffer("content")
// Send a Buffer to a multicast address
socket.send(buffer, 1234, "230.0.0.1", { asyncResult ->
  println("Send succeeded? ${asyncResult.succeeded()}")
})
```

所有加入组播组`230.0.0.1`的套接字都将收到数据包。

<a name="244______接收多播数据包"></a>
##### 接收多播数据包
如果您想接收特定多播组的数据包，您需要通过调用`listen(…)`绑定`DatagramSocket`以加入多播组。

这样，您将收到发送到`DatagramSocket`侦听的地址和端口的`DatagramPackets`，以及发送到`Multicast`组的数据报。

除此之外，您还需要为每个接收到的DatagramPacket设置一个处理程序。

`DatagramPacket`有以下方法:

- `sender()`: 代表包的发送者的InetSocketAddress
- `data()`: 保存接收到的数据的缓冲区。

因此，要监听特定的地址和端口，并接收多播组`230.0.0.1`的数据包，您需要做如下操作:

```groovy
def socket = vertx.createDatagramSocket([:])
socket.listen(1234, "0.0.0.0", { asyncResult ->
  if (asyncResult.succeeded()) {
    socket.handler({ packet ->
      // Do something with the packet
    })

    // join the multicast group
    socket.listenMulticastGroup("230.0.0.1", { asyncResult2 ->
      println("Listen succeeded? ${asyncResult2.succeeded()}")
    })
  } else {
    println("Listen failed${asyncResult.cause()}")
  }
})
```

<a name="245______取消监听_离开多播组"></a>
##### 取消监听/离开多播组
有时，您希望在有限的时间内接收多播组的数据包。

在这种情况下，您可以先开始收听它们，然后再取消收听。

如下所示:

```groovy
def socket = vertx.createDatagramSocket([:])
socket.listen(1234, "0.0.0.0", { asyncResult ->
  if (asyncResult.succeeded()) {
    socket.handler({ packet ->
      // Do something with the packet
    })

    // join the multicast group
    socket.listenMulticastGroup("230.0.0.1", { asyncResult2 ->
      if (asyncResult2.succeeded()) {
        // will now receive packets for group

        // do some work

        socket.unlistenMulticastGroup("230.0.0.1", { asyncResult3 ->
          println("Unlisten succeeded? ${asyncResult3.succeeded()}")
        })
      } else {
        println("Listen failed${asyncResult2.cause()}")
      }
    })
  } else {
    println("Listen failed${asyncResult.cause()}")
  }
})
```

<a name="246______阻塞多播"></a>
##### 阻塞多播
除了取消监听多播地址之外，还可以阻止特定发送方地址的多播。

注意，这只适用于某些操作系统和内核版本。因此，请检查操作系统文档是否受支持。

这是一个专家特性。

要阻止来自特定地址的多播，你可以在DatagramSocket上调用`blockMulticastGroup(…)`，如下图所示:

```groovy
def socket = vertx.createDatagramSocket([:])

// Some code

// This would block packets which are send from 10.0.0.2
socket.blockMulticastGroup("230.0.0.1", "10.0.0.2", { asyncResult ->
  println("block succeeded? ${asyncResult.succeeded()}")
})
```

<a name="247_____DatagramSocket_属性"></a>
#### DatagramSocket 属性
在创建`DatagramSocket`对象时，可以设置多个属性来更改`DatagramSocketOptions`对象的行为。这些都列在这里:

- `setSendBufferSize` 设置发送缓冲区的大小(以字节为单位)。
- `setReceiveBufferSize` 设置TCP接收缓冲区的大小(以字节为单位)。
- `setReuseAddress` 如果为true，则TIME_WAIT状态下的地址可以在关闭后重新使用。
- `setTrafficClass`
- `setBroadcast` 设置或清除SO_BROADCAST套接字选项。设置此选项后，可以将数据报(UDP)数据包发送到本地接口的广播地址。
- `setMulticastNetworkInterface` 设置或清除IP_MULTICAST_LOOP套接字选项。设置此选项后，还将在本地接口上接收多播包。
- `setMulticastTimeToLive` 设置IP_MULTICAST_TTL套接字选项。TTL表示“生存时间”，但是在这个上下文中，它指定了允许数据包通过的IP跳数，特别是对于多播流量。每一个转发包的路由器或网关都会降低TTL。如果TTL被路由器减少到0，它将不会被转发。

<a name="248_____DatagramSocket本地地址"></a>
#### DatagramSocket本地地址
你可以找到本地地址的套接字(即UDP套接字的这一边的地址)调用`localAddress`。这将只返回一个`InetSocketAddress`，如果你之前绑定了`DatagramSocket`和`listen(…)`，否则它将返回null。

<a name="249_____关闭DatagramSocket"></a>
#### 关闭DatagramSocket
您可以通过调用`close`方法来关闭套接字。这将关闭套接字并释放所有资源

<a name="250___DNS_客户端"></a>
## DNS 客户端
通常，您会遇到需要以异步方式获取DNS信息的情况。 不幸的是，Java虚拟机本身附带的API无法做到这一点。 因此，Vert.x提供了自己的用于DNS解析的API，该API是完全异步的。

要获取DnsClient实例，您将通过Vertx创建一个新实例。

```groovy
def client = vertx.createDnsClient(53, "10.0.0.1")
```

您还可以使用选项创建客户端并配置查询超时。

```groovy
def client = vertx.createDnsClient([
  port:53,
  host:"10.0.0.1",
  queryTimeout:10000
])
```

创建没有参数的客户端或省略服务器地址将使用内部用于非阻塞地址解析的服务器地址。

```groovy
def client1 = vertx.createDnsClient()

// Just the same but with a different query timeout
def client2 = vertx.createDnsClient([
  queryTimeout:10000
])
```

<a name="251____查找"></a>
### 查找
尝试查找给定名称的A (ipv4)或AAAA (ipv6)记录。返回的第一个将被使用，因此它的行为与在操作系统上使用“nslookup”时的行为相同。

查找“vertx.io”的A / AAAA记录。你通常会这样使用它:

```groovy
def client = vertx.createDnsClient(53, "9.9.9.9")
client.lookup("vertx.io", { ar ->
  if (ar.succeeded()) {
    println(ar.result())
  } else {
    println("Failed to resolve entry${ar.cause()}")
  }
})
```

<a name="252____查找4"></a>
### 查找4
尝试查找给定名称的A (ipv4)记录。返回的第一个将被使用，因此它的行为与在操作系统上使用“nslookup”时的行为相同。

查找“vertx.io”的A记录。你通常会这样使用它:

```groovy
def client = vertx.createDnsClient(53, "9.9.9.9")
client.lookup4("vertx.io", { ar ->
  if (ar.succeeded()) {
    println(ar.result())
  } else {
    println("Failed to resolve entry${ar.cause()}")
  }
})
```

<a name="253____查找6"></a>
### 查找6
尝试查找给定名称的AAAA (ipv6)记录。返回的第一个将被使用，因此它的行为与在操作系统上使用“nslookup”时的行为相同。

查找“vertx.io”的A记录。你通常会这样使用它:

```groovy
def client = vertx.createDnsClient(53, "9.9.9.9")
client.lookup6("vertx.io", { ar ->
  if (ar.succeeded()) {
    println(ar.result())
  } else {
    println("Failed to resolve entry${ar.cause()}")
  }
})
```

<a name="254____解析A"></a>
### 解析A
尝试解析给定名称的所有A（ipv4）记录。 这与在类似unix的操作系统上使用“dig”非常相似。

要查找“vertx.io”的所有A记录，通常需要执行以下操作：

```groovy
def client = vertx.createDnsClient(53, "9.9.9.9")
client.resolveA("vertx.io", { ar ->
  if (ar.succeeded()) {
    def records = ar.result()
    records.each { record ->
      println(record)
    }
  } else {
    println("Failed to resolve entry${ar.cause()}")
  }
})
```

<a name="255____解析AAAA"></a>
### 解析AAAA
尝试解析给定名称的所有AAAA（ipv6）记录。 这与在类似unix的操作系统上使用“dig”非常相似。

要查找“vertx.io”的所有AAAAA记录，通常需要执行以下操作：

```groovy
def client = vertx.createDnsClient(53, "9.9.9.9")
client.resolveAAAA("vertx.io", { ar ->
  if (ar.succeeded()) {
    def records = ar.result()
    records.each { record ->
      println(record)
    }
  } else {
    println("Failed to resolve entry${ar.cause()}")
  }
})
```

<a name="256____解析CNAME"></a>
### 解析CNAME
尝试解析给定名称的所有CNAME记录。 这与在类似unix的操作系统上使用“dig”非常相似。

要查找“vertx.io”的所有CNAME记录，通常需要执行以下操作：

```groovy
def client = vertx.createDnsClient(53, "9.9.9.9")
client.resolveCNAME("vertx.io", { ar ->
  if (ar.succeeded()) {
    def records = ar.result()
    records.each { record ->
      println(record)
    }
  } else {
    println("Failed to resolve entry${ar.cause()}")
  }
})
```

<a name="257____解析MX"></a>
### 解析MX
尝试解析给定名称的所有MX记录。 MX记录用于定义哪个邮件服务器接受给定域的电子邮件。

要查找“vertx.io”的所有MX记录，通常需要执行以下操作：

```groovy
def client = vertx.createDnsClient(53, "9.9.9.9")
client.resolveMX("vertx.io", { ar ->
  if (ar.succeeded()) {
    def records = ar.result()
    records.each { record ->
      println(record)
    }
  } else {
    println("Failed to resolve entry${ar.cause()}")
  }
})
```

请注意，列表将包含按优先级排序的`MxRecord`，这意味着优先级较小的MX记录在列表中排在首位。

`MxRecord`允许您通过提供方法来访问MX记录的优先级和名称，例如：

```groovy
record.priority()
record.name()
```

<a name="258____解析TXT"></a>
### 解析TXT
尝试解析给定名称的所有TXT记录。 TXT记录通常用于定义域的其他信息。

要解析“vertx.io”的所有TXT记录，可以使用以下几行：

```groovy
def client = vertx.createDnsClient(53, "9.9.9.9")
client.resolveTXT("vertx.io", { ar ->
  if (ar.succeeded()) {
    def records = ar.result()
    records.each { record ->
      println(record)
    }
  } else {
    println("Failed to resolve entry${ar.cause()}")
  }
})
```

<a name="259____解析NS"></a>
### 解析NS
尝试解析给定名称的所有NS记录。 NS记录指定哪个DNS服务器托管给定域的DNS信息。

要为“vertx.io”解析所有NS记录，可以使用以下方式：

```groovy
def client = vertx.createDnsClient(53, "9.9.9.9")
client.resolveNS("vertx.io", { ar ->
  if (ar.succeeded()) {
    def records = ar.result()
    records.each { record ->
      println(record)
    }
  } else {
    println("Failed to resolve entry${ar.cause()}")
  }
})
```

<a name="260____解析SRV"></a>
### 解析SRV
尝试解析给定名称的所有SRV记录。 SRV记录用于定义其他信息，例如服务的端口和主机名。 一些协议需要这些额外的信息。

要查找“vertx.io”的所有SRV记录，通常需要执行以下操作：

```groovy
def client = vertx.createDnsClient(53, "9.9.9.9")
client.resolveSRV("vertx.io", { ar ->
  if (ar.succeeded()) {
    def records = ar.result()
    records.each { record ->
      println(record)
    }
  } else {
    println("Failed to resolve entry${ar.cause()}")
  }
})
```

请注意，列表将包含按优先级排序的SrvRecords，这意味着优先级较小的SrvRecords在列表中排在首位。

`SrvRecord`允许您访问SRV记录本身包含的所有信息：

```groovy
record.priority()
record.name()
record.weight()
record.port()
record.protocol()
record.service()
record.target()
```

请参阅API文档以获取确切的详细信息。

<a name="261____解析PTR"></a>
### 解析PTR
尝试解析给定名称的PTR记录。 PTR记录将ip地址映射到名称。

要解析ipaddress 10.0.0.1的PTR记录，可以使用PTR概念"1.0.0.10.in-addr.arpa"

```groovy
def client = vertx.createDnsClient(53, "9.9.9.9")
client.resolvePTR("1.0.0.10.in-addr.arpa", { ar ->
  if (ar.succeeded()) {
    def record = ar.result()
    println(record)
  } else {
    println("Failed to resolve entry${ar.cause()}")
  }
})
```

<a name="262____解析Lookup"></a>
### 解析Lookup
尝试对IP地址进行反向查找。 这基本上与解析PTR记录相同，但是允许您仅传入IP地址而不是有效的PTR查询字符串。

要对ipaddress 10.0.0.1进行反向查找，请执行类似以下操作：

```groovy
def client = vertx.createDnsClient(53, "9.9.9.9")
client.reverseLookup("10.0.0.1", { ar ->
  if (ar.succeeded()) {
    def record = ar.result()
    println(record)
  } else {
    println("Failed to resolve entry${ar.cause()}")
  }
})
```

<a name="263____错误处理"></a>
### 错误处理
如前几节所述，DnsClient允许您传入一个处理程序，一旦查询完成，该处理程序将通过AsyncResult通知。 如果发生错误，将通过DnsException通知该错误，该错误将在`DnsResponseCode`中指出解析失败的原因。 此DnsResponseCode可用于更详细地检查原因。

可能的DnsResponseCodes为：

- `NOERROR` 找不到给定查询的记录
- `FORMERROR` 格式错误
- `SERVFAIL` 服务器故障
- `NXDOMAIN` 名称错误
- `NOTIMPL` DNS服务器没有实现
- `REFUSED` DNS服务器拒绝查询
- `YXDOMAIN` 域名不应该存在
- `YXRRSET` 资源记录不应该存在
- `NXRRSET` RRSET不存在
- `NOTZONE` 名称不在区域中
- `BADVERS` 版本的错误扩展机制
- `BADSIG` 签名错误
- `BADKEY` 坏的键
- `BADTIME` 时间戳错误

所有这些错误都是由DNS服务器本身“生成”的。

您可以从DnsException获得DnsResponseCode，例如：

```groovy
def client = vertx.createDnsClient(53, "8.8.8.8");
client.lookup("missing.vertx.io", { ar ->
if (ar.succeeded()) {
  def record = ar.result();
  println "record: " + record;
} else {
  def cause = ar.cause();
  if (cause instanceof DnsException) {
  def code = cause.code();
  println "Code : " + code
  // ...
  } else {
    println("Failed to resolve entry" + ar.cause());
  }
}
})
```

## 流
Vert.x中有几个对象，它们可以读取和写入项目。

在以前的版本中，`io.vertx.core.streams`包是专门处理`Buffer`对象的。 从现在开始，流不再与缓冲区耦合，并且可以与任何类型的对象一起使用。

在Vert.x中，写调用立即返回，写操作在内部排队。

不难看出，如果写入对象的速度快于将对象实际写入其底层资源的速度，那么写入队列可能会无限增长，最终导致内存耗尽。

为了解决此问题，Vert.x API中的某些对象提供了一种简单的流量控制（*back-pressure背压*）功能。

任何可*written-to*的流控制对象实现`WriteStream`，而任何可*read-from*的流控制对象实现`ReadStream`。

让我们举一个例子，我们想要从`ReadStream`读取数据，然后将数据写入`WriteStream`。

一个非常简单的例子是，从一个`NetSocket`读取数据，然后写回同一个`NetSocket`——因为`NetSocket`同时实现了`ReadStream`和`WriteStream`。注意，这在任何`ReadStream`和`WriteStream`兼容对象之间都可以工作，包括HTTP请求、HTTP响应、异步文件I/O、WebSockets等。

一种简单的方法是直接获取已读取的数据，并立即将其写入`NetSocket`:

```groovy
def server = vertx.createNetServer([
  port:1234,
  host:"localhost"
])
server.connectHandler({ sock ->
  sock.handler({ buffer ->
    // Write the data straight back
    sock.write(buffer)
  })
}).listen()
```

上面的例子有一个问题:如果从套接字读取数据的速度比将数据写回套接字的速度快，那么它将在`NetSocket`的写队列中累积，最终耗尽RAM。这可能会发生，例如，如果套接字另一端的客户端读取速度不够快，有效地对连接施加了被压。

由于`NetSocket`实现了`WriteStream`，我们可以在写入之前检查`WriteStream`是否已满:

```groovy
def server = vertx.createNetServer([
  port:1234,
  host:"localhost"
])
server.connectHandler({ sock ->
  sock.handler({ buffer ->
    if (!sock.writeQueueFull()) {
      sock.write(buffer)
    }
  })

}).listen()
```

这个示例不会耗尽RAM，但是如果写队列满了，我们将丢失数据。我们真正想做的是暂停`NetSocket`当写队列满了:

```groovy
def server = vertx.createNetServer([
  port:1234,
  host:"localhost"
])
server.connectHandler({ sock ->
  sock.handler({ buffer ->
    sock.write(buffer)
    if (sock.writeQueueFull()) {
      sock.pause()
    }
  })
}).listen()
```

我们快到了，但还没到。 现在，当文件已满时，`NetSocket`会暂停，但是当写队列处理了其积压后，我们还需要取消暂停它：

```groovy
def server = vertx.createNetServer([
  port:1234,
  host:"localhost"
])
server.connectHandler({ sock ->
  sock.handler({ buffer ->
    sock.write(buffer)
    if (sock.writeQueueFull()) {
      sock.pause()
      sock.drainHandler({ done ->
        sock.resume()
      })
    }
  })
}).listen()
```

我们终于得到它了。 当写队列准备好接受更多数据时，将调用`drainHandler`事件处理程序，这将恢复允许读取更多数据的`NetSocket`。

在编写Vert.x应用程序时，这样做很常见，因此我们添加了`pipeTo`方法，可以为您完成所有这些艰苦的工作。 您只需向其提供`WriteStream`并使用它：

```groovy
def server = vertx.createNetServer([
  port:1234,
  host:"localhost"
])
server.connectHandler({ sock ->
  sock.pipeTo(sock)
}).listen()
```

这与更详细的示例完全相同，此外它还处理流失败和终止：当管道成功完成或失败时，目标`WriteStream`将结束。

操作完成时会通知您：

```groovy
server.connectHandler({ sock ->

  // Pipe the socket providing an handler to be notified of the result
  sock.pipeTo(sock, { ar ->
    if (ar.succeeded()) {
      println("Pipe succeeded")
    } else {
      println("Pipe failed")
    }
  })
}).listen()
```

当处理异步目标时，您可以创建一个`Pipe`实例，该实例暂停源并在将源通过管道传输到目标时恢复它：

```groovy
server.connectHandler({ sock ->

  // Create a pipe to use asynchronously
  def pipe = sock.pipe()

  // Open a destination file
  fs.open("/path/to/file", [:], { ar ->
    if (ar.succeeded()) {
      def file = ar.result()

      // Pipe the socket to the file and close the file at the end
      pipe.to(file)
    } else {
      sock.close()
    }
  })
}).listen()
```

当需要中止传输时，需要关闭它：

```groovy
vertx.createHttpServer().requestHandler({ request ->

  // Create a pipe that to use asynchronously
  def pipe = request.pipe()

  // Open a destination file
  fs.open("/path/to/file", [:], { ar ->
    if (ar.succeeded()) {
      def file = ar.result()

      // Pipe the socket to the file and close the file at the end
      pipe.to(file)
    } else {
      // Close the pipe and resume the request, the body buffers will be discarded
      pipe.close()

      // Send an error response
      request.response().setStatusCode(500).end()
    }
  })
}).listen(8080)
```

当管道关闭时，将取消流处理程序的设置并恢复`ReadStream`。

如上所示，默认情况下，目的地总是在流完成时结束，您可以在管道对象上控制此行为：

- `endOnFailure` 控制故障发生时的行为
- `endOnSuccess` 控制读取流结束时的行为
- `endOnComplete` 控制所有情况下的行为

这是一个简短的示例：

```groovy
src.pipe().endOnSuccess(false).to(dst, { rs ->
  // Append some text and close the file
  dst.end(Buffer.buffer("done"))
})
```

现在，让我们更详细地了解`ReadStream`和`WriteStream`上的方法：

<a name="264____ReadStream"></a>
### ReadStream
`ReadStream` 被这些类实现 `HttpClientResponse`, `DatagramSocket`, `HttpClientRequest`, `HttpServerFileUpload`, `HttpServerRequest`, `MessageConsumer`, `NetSocket`, `WebSocket`, `TimeoutStream`, `AsyncFile`.

功能介绍:

- `handler`: 设置一个处理程序，该处理程序将从ReadStream接收项目。
- `pause`: 暂停处理程序。 暂停时，处理程序中不会收到任何内容。
- `resume`: 恢复处理程序。 如果有任何内容到达，处理程序将被调用。
- `exceptionHandler`: 如果ReadStream发生异常，将被调用。
- `endHandler`: 到达流结束时将被调用。 如果ReadStream表示一个文件，则达到EOF；如果是HTTP请求，则达到请求结束；或者，如果它是TCP套接字，则关闭连接。

<a name="265____WriteStream"></a>
### WriteStream

`WriteStream` 被这些类实现 `HttpClientRequest`, `HttpServerResponse`, `WebSocket`, `NetSocket`, `AsyncFile`, `MessageProducer`

功能介绍:

- `write`: 将对象写入WriteStream。 此方法将永远不会阻塞。 写入在内部排队，并异步写入基础资源。
- `setWriteQueueMaxSize`: 设置将写队列视为*full已满*的对象的数量，方法`writeQueueFull`返回`true`。 请注意，当写入队列被认为已满时，如果调用写入，则数据仍将被接受并排队。 实际数量取决于流的实现，对于`Buffer`，size表示实际写入的字节数，而不是缓冲区的number。
- `writeQueueFull`: 如果认为写入队列已满，则返回`true`。
- `exceptionHandler`: 如果`riteStream`发生异常，将被调用。
- `drainHandler`: 如果认为`WriteStream`不再满，则将调用处理程序。

<a name="266____Pump_水泵_"></a>
### Pump(水泵)
泵公开了管道API的子集，仅在流之间传输项目，它不处理传输操作的完成或失败。

```groovy
def server = vertx.createNetServer([
  port:1234,
  host:"localhost"
])
server.connectHandler({ sock ->
  Pump.pump(sock, sock).start()
}).listen()
```

------
> **重要:** 在Vert.x 3.7之前，`Pump`是提倡将读取流传输到写入流的API。 从3.7开始，pipe API取代了pump API。
>
------

Pump实例具有以下方法：

- `start`:开始 pump.
- `stop`: 停止pump。当泵启动时，它处于停止模式。
- `setWriteQueueMaxSize`: 与`WriteStream`上的`setWriteQueueMaxSize`具有相同的含义。

pump可以多次启动和停止。

首次创建pump时，它*不会*启动。 您需要调用`start()`方法来启动它。

<a name="267___记录解析器"></a>
## 记录解析器
记录解析器使您可以轻松地解析由字节序列或固定大小记录分隔的协议。 它将输入缓冲区的序列转换为按配置结构构造的缓冲区序列（固定大小或分隔的记录）。

例如，如果您有一个以`\n`分隔的简单ASCII文本协议，并且输入如下：

```
buffer1:HELLO\nHOW ARE Y
buffer2:OU?\nI AM
buffer3: DOING OK
buffer4:\n
```

记录解析器将产生

```
buffer1:HELLO
buffer2:HOW ARE YOU?
buffer3:I AM DOING OK
```

让我们看一下相关代码：

```groovy
def parser = RecordParser.newDelimited("\n", { h ->
  println(h.toString())
})

parser.handle(Buffer.buffer("HELLO\nHOW ARE Y"))
parser.handle(Buffer.buffer("OU?\nI AM"))
parser.handle(Buffer.buffer("DOING OK"))
parser.handle(Buffer.buffer("\n"))
```

您还可以生成固定大小的块，如下所示：

```groovy
RecordParser.newFixed(4, { h ->
  println(h.toString())
})
```

有关更多详细信息，请查看`RecordParser`类。

<a name="268___Json解析器"></a>
## Json解析器
您可以轻松地解析JSON结构，但这需要立即提供JSON内容，但在需要解析非常大的结构时可能不太方便。

非阻塞JSON解析器是一个事件驱动的解析器，能够处理非常大的结构。 它将输入缓冲区的序列转换为JSON解析事件的序列。

```
Code not translatable
```

解析器是非阻塞的，发出的事件由输入缓冲区驱动。

```groovy
def parser = JsonParser.newParser()

// start array event
// start object event
// "firstName":"Bob" event
parser.handle(Buffer.buffer("[{\"firstName\":\"Bob\","))

// "lastName":"Morane" event
// end object event
parser.handle(Buffer.buffer("\"lastName\":\"Morane\"},"))

// start object event
// "firstName":"Luke" event
// "lastName":"Lucky" event
// end object event
parser.handle(Buffer.buffer("{\"firstName\":\"Luke\",\"lastName\":\"Lucky\"}"))

// end array event
parser.handle(Buffer.buffer("]"))

// Always call end
parser.end()
```

事件驱动的解析提供了更多的控制权，但代价是要处理细粒度的事件，这有时会带来不便。 JSON解析器允许您在需要时将JSON结构作为值处理：

```
Code not translatable
```

可以在解析期间设置和取消设置值模式，从而允许您在细粒度事件或JSON对象值事件之间切换。

```
Code not translatable
```

你也可以对数组做同样的事情

```
Code not translatable
```

您还可以解码POJO

```groovy
parser.handler({ event ->
  // Handle each object
  // Get the field in which this object was parsed
  def id = event.fieldName()
  def user = event.mapTo(examples.ParseToolsExamples.User.class)
  println("User with id ${id} : ${user.firstName} ${user.lastName}")
})
```

每当解析器无法处理缓冲区时，除非您设置了异常处理程序，否则都会引发异常：

```groovy
def parser = JsonParser.newParser()

parser.exceptionHandler({ err ->
  // Catch any parsing or decoding error
})
```

解析器还解析json流：

- 串联的json流: `{"temperature":30}{"temperature":50}`
- 行分隔的json流: `{"an":"object"}\r\n3\r\n"a string"\r\nnull`

有关更多详细信息，请查看`JsonParser`类。

<a name="269___线程安全"></a>
## 线程安全
大多数Vert.x对象可以从不同线程安全访问。 *但是*当从创建它们的相同上下文访问它们时，性能会得到优化。

例如，如果您部署了一个创建了`NetServer`的Verticle，并在其处理程序中提供了`NetSocket`实例，那么最好始终从该Verticle的事件循环访问该套接字实例。

如果您坚持使用标准的Vert.x的verticle 部署模型，并避免在verticles之间共享对象，则无需考虑即可。

<a name="270___指标SPI"></a>
## 指标SPI
默认情况下，Vert.x不记录任何指标。 相反，它为其他人提供了一个SPI，可以将其添加到类路径中。 指标SPI是一项高级功能，允许实施者从Vert.x捕获事件以收集指标。 有关此的更多信息，请查阅API文档。

如果使用`setFactory`嵌入Vert.x，您还可以通过编程方式指定指标工厂。

<a name="271___OSGi"></a>
## OSGi
Vert.x Core打包为OSGi捆绑包，因此可以在任何OSGi R4.2 +环境中使用，例如Apache Felix或Eclipse Equinox。 捆绑包导出`io.vertx.core*`。

但是，捆绑软件对Jackson和Netty有一定的依赖性。 要解决vert.x核心捆绑的部署：

- Jackson Annotation [2.6.0,3)
- Jackson Core [2.6.2,3)
- Jackson Databind [2.6.2,3)
- Netty Buffer [4.0.31,5)
- Netty Codec [4.0.31,5)
- Netty Codec/Socks [4.0.31,5)
- Netty Codec/Common [4.0.31,5)
- Netty Codec/Handler [4.0.31,5)
- Netty Codec/Transport [4.0.31,5)

这是Apache Felix 5.2.0上的有效部署：

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

在Equinox上，您可能要禁用具有以下框架属性的`ContextFinder`：`eclipse.bundle.setTCCL=false`

<a name="272___vertx_命令行"></a>
## vertx'命令行
vertx命令用于从命令行与Vert.x交互。 主要用途是运行Vert.x的verticles。 为此，您需要下载并安装Vert.x发行版，并将安装的bin目录添加到环境变量`PATH`中。 还要确保您在`PATH`上有一个Java 8 JDK。

------
> **注意:** 需要JDK来支持Java代码的即时编译。
>
------

<a name="273____运行_verticles"></a>
### 运行 verticles
您可以使用`vertx run`直接从命令行运行原始Vert.x的verticles。 这是`run`  *command*的几个例子：

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
3. 部署一个 Ruby verticle
4. 部署一个已经编译好的Java Verticle。 类路径根目录是当前目录
5. 部署打包在Jar中的verticle，该jar必须位于类路径中
6. 编译并部署Java源代码的verticle

如在Java中所见，名称可以是verticle的完全限定的类名，也可以直接指定Java Source文件，然后Vert.x会为您编译它。

您还可以在verticle上添加要使用的语言实现的名称。 例如，如果verticle是已编译的Groovy类，则可以在其前面加上`groovy:`，以便Vert.x知道它是Groovy类而不是Java类。

```bash
vertx run groovy:io.vertx.example.MyGroovyVerticle
```

`vertx run`命令可以使用一些可选参数，它们是：

- `-options ` - 提供Vert.x选项。 `options`是表示Vert.x选项的JSON文件的名称，或者是JSON字符串。 这是可选的。
- `-conf ` - 为verticle提供一些配置。 `conf`是表示verticle配置或JSON字符串的JSON文件的名称。 这是可选的。
- `-cp ` - 搜索verticle和verticle使用的任何其他资源的路径。 默认为`.`（当前目录）。 如果您的Verticle引用了其他脚本，类或其他资源（例如jar文件），请确保它们在此路径上。 该路径可以包含多个路径条目，具体取决于操作系统，这些条目由`:`（冒号）或`;`（分号）分隔。 每个路径条目都可以是包含脚本的目录的绝对或相对路径，也可以是jar或zip文件的绝对或相对文件名。 示例路径可能是`-cp classes:lib/otherscripts:jars/myjar.jar:jars/otherjar.jar`。 始终使用路径引用您的Verticle所需的任何资源。 请勿将它们放在系统类路径上，因为这可能导致部署的verticles之间的隔离问题。
- `-instances ` - 要实例化的verticle的实例数。 每个verticle实例严格都是单线程的，因此要在可用核心上扩展应用程序，您可能需要部署多个实例。 如果省略，将部署一个实例。
- `-worker` - 此选项确定该verticle是否为工作者verticle 。
- `-cluster` - 此选项确定Vert.x实例是否将尝试与网络上的其他Vert.x实例形成集群。 集群Vert.x实例允许Vert.x与其他节点形成分布式事件总线。 默认值为`false`（不群群）。
- `-cluster-port` - 如果还指定了`cluster`选项，那么它将确定绑定哪个端口与其他Vert.x实例进行集群通信。 默认值为'0'-表示*选择一个随机端口*。 除非确实需要绑定到特定端口，否则通常无需指定此参数。
- `-cluster-host` - 如果还指定了`cluster`选项，那么它将确定绑定哪个主机地址与其他Vert.x实例进行集群通信。 默认情况下，它将尝试从可用接口中选择一个。 如果您有多个接口，并且想要使用特定的接口，请在此处指定。
- `-cluster-public-port` - 如果还指定了`cluster`选项，那么这将决定通告哪个端口与其他Vert.x实例进行集群通信。 默认值为`-1`，表示与集群端口相同。
- `-cluster-public-host` - 如果还指定了`cluster`选项，则这将确定要通告哪个主机地址以与其他Vert.x实例进行集群通信。 如果未指定，则Vert.x使用`cluster-host`的值。
- `-ha` - 如果指定，则将以高可用性（HA）部署的方式部署该Verticle。 有关更多详细信息，请参见相关部分
- `-quorum` - 与`-ha`结合使用。 它指定要使任何*HA deploymentIDs*处于活动状态的群集中的最小节点数。 预设为0。
- `-hagroup` - 与`-ha`结合使用。 它指定此节点将加入的HA组。 一个群集中可以有多个HA组。 节点将仅故障转移到同一组中的其他节点。 默认值为`__DEFAULT__`

您还可以使用`-Dkey=value`设置系统属性。

这里还有更多示例：

使用默认设置运行JavaScript verticle server.js

```bash
vertx run server.js
```

运行指定类路径的已经编译好的Java verticle的10个实例

```bash
vertx run com.acme.MyVerticle -cp "classes:lib/myjar.jar" -instances 10
```

通过源*file*运行Java verticle的10个实例

```bash
vertx run MyVerticle.java -instances 10
```

运行20个ruby工作verticle的实例

```bash
vertx run order_worker.rb -instances 20 -worker
```

在同一台机器上运行两个JavaScript的verticles，让它们彼此集群在一起，以及网络上的任何其他服务器

```bash
vertx run handler.js -cluster
vertx run sender.js -cluster
```

运行一个Ruby verticle传递一些配置

```
vertx run my_verticle.rb -conf my_verticle.conf
```

其中`my_verticle.conf`可能包含以下内容：

```json
{
"name": "foo",
"num_widgets": 46
}
```

可以通过核心API在verticle内部使用该配置。

使用vert.x的高可用性功能时，您可能需要创建vert.x的“bare”实例。 该实例在启动时不会部署任何Verticle，但是如果集群的另一个节点死亡，则会收到一个Verticle。 要创建一个*bare*实例，请启动：

```bash
vertx bare
```

根据您的集群配置，您可能必须附加`cluster-host`和`cluster-port`参数。

<a name="274____执行打包为fat_jar的Vert_x应用程序"></a>
### 执行打包为fat jar的Vert.x应用程序
*fat jar*是嵌入了全部依赖项的可执行jar。 这意味着您无需在执行jar的计算机上预安装Vert.x。 像任何可执行的Java jar一样，可以使用它来执行。

```bash
java -jar my-application-fat.jar
```

真正的Vert.x并没有什么特别的，您可以使用任何Java应用程序来完成

您可以创建自己的主类并在manifest中指定它，但是建议您将代码编写为verticles，并使用Vert.x `Launcher`类（`io.vertx.core.Launcher`）作为主类。这与在命令行上运行Vert.x时使用的主类相同，因此允许您指定命令行参数，例如`-instances`，以便更轻松地扩展应用程序。

要将verticle放置在这样的*fat jar*中，您必须具有*manifest*，并具有：

- `Main-Class` 设置成 `io.vertx.core.Launcher`
- `Main-Verticle` 指定主Verticle（完全限定的类名或脚本文件名）

您还可以提供传递给`vertx run`的常用命令行参数：

```bash
java -jar my-verticle-fat.jar -cluster -conf myconf.json
java -jar my-verticle-fat.jar -cluster -conf myconf.json -cp path/to/dir/conf/cluster_xml
```

------
> **注意:** 请参考示例存储库中的Maven/Gradle最简单的Maven/Gradle verticle示例，以获取将应用程序构建为fatjars的示例。
>
------

默认情况下，fat jar执行`run`命令。

<a name="275____显示Vert_x的版本"></a>
### 显示Vert.x的版本
要显示vert.x版本，只需启动：

```bash
vertx version
```

<a name="276____其他命令"></a>
### 其他命令
除了`run` 和 `version`之外，`vertx`命令行和`Launcher`还提供其他*command*：

您可以使用以下方法创建一个`bare`实例：

```bash
vertx bare
# or
java -jar my-verticle-fat.jar bare
```

您还可以使用以下方法在后台启动应用程序：

```bash
java -jar my-verticle-fat.jar start --vertx-id=my-app-name
```

如果未设置`my-app-name`，则会生成一个随机ID，并将其打印在命令提示符下。 您可以将`—-vertx-id`选项传递给`start`命令：

```bash
java -jar my-verticle-fat.jar start —-vertx-id=my-app-name -cluster
```

一旦在后台启动，您可以使用`stop`命令停止它：

```bash
java -jar my-verticle-fat.jar stop my-app-name
```

您还可以使用以下命令列出在后台启动的vert.x应用程序：

```bash
java -jar my-verticle-fat.jar list
```

也可以从`vertx`工具中获得`start`，`stop`和`list`命令。 `start`命令支持两个选项：

- `vertx-id` : 应用程序ID，如果未设置，则使用随机UUID
- `java-opts` : Java虚拟机选项，如果未设置，则使用JAVA_OPTS环境变量。
- `redirect-output` : 将产生的流程输出和错误流重定向到父流程流。

如果选项值包含空格，请不要忘了将值包装在`""`（双引号）之间。

当`start`命令产生一个新进程时，传递给JVM的Java选项不会传播，因此您必须使用`java-opts`来配置JVM (`-X`, `-D`…)。 如果您使用`CLASSPATH`环境变量，请确保它包含所有必需的jar（vertx-core，您的jar和所有依赖项）。

该命令集是可扩展的，请参阅[扩展vert.x启动器](https://vertx.io/docs/vertx-core/java/#_extending_the_vert_x_launcher)部分。

<a name="277____实时重新部署"></a>
### 实时重新部署
开发时，在文件更改后自动重新部署应用程序可能会很方便。 `vertx`命令行工具，更一般地讲，`Launcher`类提供此功能。 这里有些例子：

```bash
vertx run MyVerticle.groovy --redeploy="**/*.groovy" --launcher-class=io.vertx.core.Launcher
vertx run MyVerticle.groovy --redeploy="**/*.groovy,**/*.rb"  --launcher-class=io.vertx.core.Launcher
java io.vertx.core.Launcher run org.acme.MyVerticle --redeploy="**/*.class"  --launcher-class=io.vertx.core.Launcher -cp ...
```

重新部署过程的实现如下。 首先，您的应用程序将作为后台应用程序启动（使用`start`命令）。 匹配文件更改后，该过程将停止并重新启动应用程序。 这样可以避免泄漏，因为重新启动了进程。

要启用实时重新部署，请将`--redeploy`选项传递给`run`命令。 `--redeploy`表示要监视的文件集。 这个集合可以使用Ant样式的模式（带有`**`，`*`和`?`）。 您可以使用逗号（`,`）将它们分开来指定多个集合。 模式是相对于当前工作目录的。

传递给`run`命令的参数传递给应用程序。 可以使用`--java-opts`来配置Java虚拟机选项。 例如，要传递`conf`参数或系统属性，您需要使用：`--java-opts="-conf=my-conf.json -Dkey=value"`

`--launcher-class`选项通过*main*类确定应用程序是启动器。 通常是`Launcher`，但是您可以使用自己的*main*。

可以在您的IDE中使用重新部署功能：

- Eclipse - 使用`io.vertx.core.Launcher`类作为**main类**创建一个`Run`配置。 在“Program arguments”区域（在“Arguments”选项卡中），写成`run your-verticle-fully-qualified-name --redeploy=**/*.java --launcher-class=io.vertx.core.Launcher`。 您还可以添加其他参数。 当Eclipse在保存时逐步编译文件时，重新部署可以顺利进行。
- IntelliJ - 创建一个*Run*配置（*Application*），将*Main class*设置为`io.vertx.core.Launcher`。 在程序参数中写：`run your-verticle-fully-qualified-name --redeploy=**/*.class --launcher-class=io.vertx.core.Launcher`。 要触发重新部署，您需要*使*项目或模块显式（*Build*菜单→*Make project*）。

要调试您的应用程序，请将您的运行配置创建为远程应用程序，并使用`--java-opts`配置调试器。 但是，不要忘记在每次重新部署后都重新插入调试器，因为每次都会创建一个新进程。

您还可以在重新部署周期中挂钩构建过程：

```bash
java -jar target/my-fat-jar.jar --redeploy="**/*.java" --on-redeploy="mvn package"
java -jar build/libs/my-fat-jar.jar --redeploy="src/**/*.java" --on-redeploy='./gradlew shadowJar'
```

“on-redeploy”选项指定在应用程序关闭之后和重新启动之前调用的命令。 因此，如果构建工具更新了某些运行时构件，则可以将其挂钩。 例如，您可以启动`gulp`或`grunt`来更新您的资源。 不要忘记，将参数传递给应用程序需要`--java-opts`参数：

```bash
java -jar target/my-fat-jar.jar --redeploy="**/*.java" --on-redeploy="mvn package" --java-opts="-Dkey=val"
java -jar build/libs/my-fat-jar.jar --redeploy="src/**/*.java" --on-redeploy='./gradlew shadowJar' --java-opts="-Dkey=val"
```

重新部署功能还支持以下设置：

- `redeploy-scan-period` : 文件系统检查周期（以毫秒为单位），默认为250ms
- `redeploy-grace-period` : 2次重新部署之间等待的时间（以毫秒为单位），默认为1000ms
- `redeploy-termination-period` : 停止应用程序后（启动用户命令之前）要等待的时间。 这在Windows上非常有用，因为Windows不会立即终止该进程。 时间以毫秒为单位。 默认为0毫秒。

<a name="278___集群管理器"></a>
## 集群管理器
在Vert.x中，群集管理器用于各种功能，包括：

- 集群中Vert.x节点的发现和组成员关系
- 维护集群范围的主题订户列表（因此我们知道哪些节点对哪些事件总线地址感兴趣）
- 分布式Map支持
- 分布式锁
- 分布式计数器

群集管理器*不*处理事件总线节点间的传输，这是由Vert.x使用TCP连接直接完成的。

Vert.x发行版中使用的默认集群管理器是使用[Hazelcast](http://hazelcast.com/)的集群管理器，但是由于可以插入Vert.x集群管理器，因此可以很容易地用其他实现替换。

集群管理器必须实现接口`ClusterManager`。 Vert.x通过使用Java [Service Loader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) 功能来在运行时查找类路径上的`ClusterManager`集群管理器。

如果您在命令行上使用Vert.x，并且想要使用集群，则应确保Vert.x安装的`lib`目录包含您的集群管理器jar。

如果您从Maven或Gradle项目中使用Vert.x，则只需添加群集管理器jar作为项目的依赖项即可。

如果使用`setClusterManager`嵌入Vert.x，也可以通过编程方式指定集群管理器。

<a name="279___日志记录"></a>
## 日志记录
Vert.x使用其内置的日志记录API进行日志记录。 默认实现使用JDK（JUL）日志记录，因此不需要额外的日志记录依赖项。

<a name="280____配置_JUL_日志"></a>
### 配置 JUL 日志
可以通过提供一个名为`java.util.logging.config.file`的系统属性（其值为配置文件）来以普通的JUL方式指定JUL日志记录配置文件。 有关此内容和JUL配置文件的结构的更多信息，请查阅JUL日志记录文档。

Vert.x还提供了一种更便捷的方式来指定配置文件，而无需设置系统属性。 只需在类路径上提供一个名为`vertx-default-jul-logging.properties`的JUL配置文件（例如，在fatjar中），Vert.x将使用该文件来配置JUL。

<a name="281____使用其它的日志框架"></a>
### 使用其它的日志框架
如果您不希望Vert.x使用JUL进行自己的日志记录，则可以将其配置为使用其他日志记录框架，例如 Log4J或SLF4J。

为此，您应该设置一个名为`vertx.logger-delegate-factory-class-name`的系统属性，该属性带有实现`LogDelegateFactory`接口的Java类的名称。 我们提供了Log4J(版本1)、Log4J 2和SLF4J的预构建实现，类名为`io.vertx.core.logging.Log4jLogDelegateFactory`, `io.vertx.core.logging.Log4j2LogDelegateFactory` 和 `io.vertx.core.logging.SLF4JLogDelegateFactory`。如果要使用这些实现，则还应确保相关的Log4J或SLF4J jar在您的类路径中。

注意，Log4J 1提供的委托不支持参数化消息。Log4J 2的委托像SLF4J委托一样使用`{}`语法。JUL委托使用`{x}`语法。

<a name="282____Netty_日志"></a>
### Netty 日志
配置日志记录时，还应注意配置Netty日志记录。

Netty不依赖外部日志记录配置（例如系统属性），而是基于Netty类可见的日志记录库实现日志记录配置：

- 如果可见则使用`SLF4J`库
- 否则使用`Log4j`（如果可见）
- 否则使用`java.util.logging`

通过直接在`io.netty.util.internal.logger.internalloggerfactory`上设置Netty的内部日志实现，可以强制日志实现为特定的实现:

```groovy
// Force logging to Log4j
InternalLoggerFactory.setDefaultFactory(Log4JLoggerFactory.INSTANCE);
```

<a name="283____故障排除"></a>
### 故障排除
<a name="284_____启动时的SLF4J警告"></a>
#### 启动时的SLF4J警告
如果，当你开始你的应用程序，你看到以下消息:

```
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
```

这意味着您的类路径中有SLF4J-API，但没有实际的绑定。 用SLF4J记录的消息将被丢弃。 您应该将绑定添加到类路径。 检查`https://www.slf4j.org/manual.html#swapping`以选择绑定并进行配置。

请注意，Netty会查找SLF4-API jar，并默认使用它。

<a name="285_____对等连接被重置"></a>
#### 对等连接被重置
如果您的日志显示以下内容：

```
io.vertx.core.net.impl.ConnectionBase
SEVERE: java.io.IOException: Connection reset by peer
```

这意味着客户端正在重置HTTP连接，而不是关闭它。 此消息还表明您可能没有消耗完完整的有效负载（连接已被切断，然后才能够使用）。

<a name="286___高可用性和故障转移"></a>
## 高可用性和故障转移
Vert.x允许您在具有高可用性（HA）支持的情况下运行自己的verticles。 在这种情况下，当运行某个verticle的vert.x实例突然死亡时，该verticle将被迁移到另一个verticle实例。 vert.x实例必须位于同一集群中。

<a name="287____自动故障转移"></a>
### 自动故障转移
在启用*HA*的情况下运行vert.x时，如果运行某个verticle的vert.x实例发生故障或死亡，该verticle将自动重新部署到集群的另一个vert.x实例上。 我们称此为“verticle故障转移”。

要在启用*H *的情况下运行vert.x，只需在命令行中添加`-ha`标志即可：

```bash
vertx run my-verticle.js -ha
```

现在，要使HA正常工作，集群中需要多个Vert.x实例，因此，假设您已经启动了另一个Vert.x实例，例如：

```bash
vertx run my-other-verticle.js -ha
```

如果运行`my-verticle.js`的Vert.x实例现在死亡（您可以通过使用`kill -9`杀死该进程来测试），则运行`my-other-verticle.js`的Vert.x实例将自动部署`my-verticle.js`，因此，现在Vert.x实例正在同时运行两verticles。

------
> **注意:** 仅当第二个vert.x实例有权访问verticle文件（此处为`my-verticle.js`）时，才可以进行迁移。
>
------

------
> **重要:** 请注意，完全关闭Vert.x实例不会导致发生故障转移，例如`CTRL-C` 或 `kill -SIGINT`
>
------

您也可以启动 *裸* Vert.x实例--即最初没有运行任何verticle的实例，它们也将针对集群中的节点进行故障转移。 要启动一个裸实例，您只需执行以下操作：

```bash
vertx run -ha
```

当使用`-ha`开关时，您不需要提供`-cluster`开关，因为如果您想要HA，则假定为集群。

------
> **注意:** 根据您的集群配置，您可能需要自定义集群管理器配置（默认为Hazelcast）和/或添加`cluster-host`和`cluster-port`参数。
>
------

<a name="288____HA_组"></a>
### HA 组
当使用HA运行Vert.x实例时，您还可以可选地指定*HA组*。 HA组表示集群中节点的逻辑组。 只有具有相同HA组的节点才能彼此故障转移。 如果您未指定HA组，则使用默认组`__DEFAULT__`。

要指定HA组，请在运行verticle时使用`-group`开关，例如:

```bash
vertx run my-verticle.js -ha -hagroup my-group
```

让我们看一个例子：

在第一个终端中：

```bash
vertx run my-verticle.js -ha -hagroup g1
```

在第二个终端中，让我们使用相同的组运行另一个verticle：

```bash
vertx run my-other-verticle.js -ha -hagroup g1
```

最后，在第三个终端中，使用不同的组启动另一个verticle：

```bash
vertx run yet-another-verticle.js -ha -hagroup g2
```

如果我们杀死终端1中的实例，它将故障转移到终端2中的实例，而不是终端3中的实例，因为该实例具有不同的组。

如果我们在终端3杀死了该实例，则该实例将不会进行故障转移，因为该组中没有其他vert.x实例。

<a name="289____处理网络分区_Quora"></a>
### 处理网络分区-Quora
HA实现还支持法定人数。 quorum (法定人数)是为了允许在分布式系统中执行操作而必须获得的分布式事务的最小投票数。

启动Vert.x实例时，您可以指示它要求`quorum`，然后才能部署任何HA。 在这种情况下，quorum是集群中特定组的最小节点数。 通常，您将quorum大小选择为`Q = 1 + N/2`，其中`N`是组中的节点数。 如果集群中的节点少于`Q`个，则HA部署将取消。 如果重新达到法定人数，他们将重新部署。 这样可以防止网络分区，也就是“split brain(裂脑)”。

有关quora的更多信息，请参见[此处](https://en.wikipedia.org/wiki/Quorum_(distributed_computing))。

要使用quorum运行vert.x实例，请在命令行上指定`-quorum'，例如

在第一个终端中：

```bash
vertx run my-verticle.js -ha -quorum 3
```

此时，Vert.x实例将启动，但尚未部署模块（尚未），因为集群中只有一个节点，而不是3个。

在第二个终端中：

```bash
vertx run my-other-verticle.js -ha -quorum 3
```

此时，Vert.x实例将启动，但尚未部署模块，因为集群中只有两个节点，而不是3个。

在第三个终端中，您可以启动vert.x的另一个实例：

```bash
vertx run yet-another-verticle.js -ha -quorum 3
```

好极了！ --我们有三个节点，这是一个法定人数。 此时，模块将自动部署在所有实例上。

如果现在关闭或杀死其中一个节点，则模块将自动在其他节点上取消部署，因为不再有仲裁。

Quora也可以与ha组结合使用。 在这种情况下，将为每个特定组解决法定人数。

<a name="290___本地传输"></a>
## 本地传输
Vert.x可以与[本地传输](http://netty.io/wiki/native-transports.html)（如果可用）一起在BSD（OSX）和Linux上运行：

<a name="291____本地Linux传输"></a>
### 本地Linux传输
您需要在类路径中添加以下依赖项：

```xml
<dependency>
 <groupId>io.netty</groupId>
 <artifactId>netty-transport-native-epoll</artifactId>
 <version>4.1.15.Final</version>
 <classifier>linux-x86_64</classifier>
</dependency>
```

Linux上的Native可以为您提供额外的联网选项：

- `SO_REUSEPORT`
- `TCP_QUICKACK`
- `TCP_CORK`
- `TCP_FASTOPEN`

```groovy
// Available on Linux
vertx.createHttpServer([
  tcpFastOpen:fastOpen,
  tcpCork:cork,
  tcpQuickAck:quickAck,
  reusePort:reusePort
])
```

<a name="292____本地BSDLinuxNative"></a>
### 本地BSDLinuxNative
您需要在类路径中添加以下依赖项：

```xml
<dependency>
 <groupId>io.netty</groupId>
 <artifactId>netty-transport-native-kqueue</artifactId>
 <version>4.1.15.Final</version>
 <classifier>osx-x86_64</classifier>
</dependency>
```

支持MacOS Sierra及更高版本。

BSD上的Native可以为您提供其他网络选项：

- `SO_REUSEPORT`

```groovy
// Available on BSD
vertx.createHttpServer([
  reusePort:reusePort
])
```

<a name="293____域套接字"></a>
### 域套接字
本机为服务器提供域套接字支持:

```groovy
// Only available on BSD and Linux
vertx.createNetServer().connectHandler({ so ->
  // Handle application
}).listen(SocketAddress.domainSocketAddress("/var/tmp/myservice.sock"))
```

或者http:

```groovy
vertx.createHttpServer().requestHandler({ req ->
  // Handle application
}).listen(SocketAddress.domainSocketAddress("/var/tmp/myservice.sock"), { ar ->
  if (ar.succeeded()) {
    // Bound to socket
  } else {
    ar.cause().printStackTrace()
  }
})
```

以及客户端:

```groovy
def netClient = vertx.createNetClient()

// Only available on BSD and Linux
def addr = SocketAddress.domainSocketAddress("/var/tmp/myservice.sock")

// Connect to the server
netClient.connect(addr, { ar ->
  if (ar.succeeded()) {
    // Connected
  } else {
    ar.cause().printStackTrace()
  }
})
```

或者http:

```groovy
def httpClient = vertx.createHttpClient()

// Only available on BSD and Linux
def addr = SocketAddress.domainSocketAddress("/var/tmp/myservice.sock")

// Send request to the server
httpClient.request(HttpMethod.GET, addr, 8080, "localhost", "/", { resp ->
  // Process response
}).end()
```

<a name="294___安全提示"></a>
## 安全提示
Vert.x是一个工具包，而不是一个固执己见的框架，我们在其中强迫您以某种方式做事。 这为您提供了强大的开发能力，但随之而来的是巨大的责任。

与任何工具包一样，也可以编写不安全的应用程序，因此在开发应用程序时应特别小心，尤其是在公开（例如通过互联网）的应用程序中。

<a name="295____Web_应用程序"></a>
### Web 应用程序
如果编写网络应用程序，强烈建议您直接使用Vert.x-Web而不是Vert.x core来提供资源和处理文件上传。

Vert.x-Web规范了请求中的路径，以防止恶意客户端制作URL来访问Web根以外的资源。

同样，对于文件上载，Vert.x-Web提供了用于上载到磁盘上已知位置的功能，并且不依赖于客户端在上载中提供的文件名，可以将其设计成上载到磁盘上的其他位置。

Vert.x核心本身不提供此类检查，因此，作为开发人员，您可以自己实施这些检查。

<a name="296____集群事件总线流量"></a>
### 集群事件总线流量
在网络上不同Vert.x节点之间对事件总线进行集群时，流量将以非加密方式通过网络发送，因此，如果要发送的机密数据且Vert.x节点不在受信任的网络上，请不要使用此功能 。

<a name="297____标准安全最佳做法"></a>
### 标准安全最佳做法
无论是使用Vert.x或任何其他工具包编写的任何服务，都可能存在潜在的漏洞，因此请始终遵循最佳安全实践，尤其是当您的服务面向公众时。

例如，您应该始终在DMZ中使用具有有限权限的用户帐户运行它们，以限制服务受到损害时的损坏程度。

<a name="298___Vert_x命令行界面API"></a>
## Vert.x命令行界面API
Vert.x Core提供了一个API，用于解析传递给程序的命令行参数。 它还可以打印帮助消息，其中详细说明了命令行工具可用的选项。 即使这些功能与Vert.x核心主题相距甚远，该API仍可以在`Launcher`类中使用，您可以在*fat-jar*和`vertx`命令行工具中使用。 此外，它是多语言的（可以从任何受支持的语言中使用），并在Vert.x Shell中使用。

Vert.x CLI提供了一个模型来描述您的命令行界面，还提供了一个解析器。 该解析器支持不同类型的语法：

- POSIX类型的选项（例如: `tar -zxvf foo.tar.gz`）
- GNU类型的长选项（例如: `du --human-readable --max-depth=1`）
- Java类型的属性 (例如: `java -Djava.awt.headless=true -Djava.net.useSystemProxies=true Foo`)
- 附带值的短选项 (例如: `gcc -O2 foo.c`)
- 单连字符的长选项 (例如: `ant -projecthelp`)

使用CLI api的过程分为3个步骤：

1. 命令行界面的定义
2. 用户命令行解析
3. 查询/询问

<a name="299____定义阶段"></a>
### 定义阶段
每个命令行界面必须定义将要使用的选项和参数集。 它还需要一个名称。 CLI API使用`Option`和`Argument`类来描述选项和参数：

```groovy
def cli = CLI.create("copy").setSummary("A command line interface to copy files.").addOption([
  longName:"directory",
  shortName:"R",
  description:"enables directory support",
  flag:true
]).addArgument([
  index:0,
  description:"The source",
  argName:"source"
]).addArgument([
  index:1,
  description:"The destination",
  argName:"target"
])
```

如您所见，您可以使用`CLI.create`创建一个新的`CLI`。 传递的字符串是CLI的名称。 创建后，您可以设置摘要和描述。 摘要旨在简短（一行），而描述可以包含更多详细信息。 每个选项和参数也使用`addArgument`和`addOption`方法添加到CLI对象。

<a name="300_____选项"></a>
#### 选项
“ Option”是一个命令行参数，由用户命令行中的*key*标识。 选项必须至少具有长名或短名。 长名称通常使用`--`前缀，而短名称则使用单个`-`。 选项可以在用法中显示说明（请参阅下文）。 选项可以接收0、1或几个值。 接收到0个值的选项是一个`flag`，必须使用`setFlag`声明。 默认情况下，选项接收单个值，但是，您可以使用`setMultiValued`将选项配置为接收多个值：


```groovy
def cli = CLI.create("some-name").setSummary("A command line interface illustrating the options valuation.").addOption([
  longName:"flag",
  shortName:"f",
  flag:true,
  description:"a flag"
]).addOption([
  longName:"single",
  shortName:"s",
  description:"a single-valued option"
]).addOption([
  longName:"multiple",
  shortName:"m",
  multiValued:true,
  description:"a multi-valued option"
])
```

选项可以标记为强制性。 用户命令行中未设置的强制选项在解析过程中会引发异常：

```groovy
def cli = CLI.create("some-name").addOption([
  longName:"mandatory",
  required:true,
  description:"a mandatory option"
])
```

非强制选项可以具有*默认值*。 如果用户未在命令行中设置选项，则将使用此值：

```groovy
def cli = CLI.create("some-name").addOption([
  longName:"optional",
  defaultValue:"hello",
  description:"an optional option with a default value"
])
```

可以使用`setHidden`方法将选项“隐藏”。 用法中未列出“隐藏”选项，但仍可以在用户命令行中使用（对于高级用户）。

如果选项值限制为固定值，则可以设置不同的可接受选项：

```groovy
def cli = CLI.create("some-name").addOption([
  longName:"color",
  defaultValue:"green",
  choices:[
    "blue",
    "red",
    "green"
  ],
  description:"a color"
])
```

还可以从JSON格式实例化选项。

<a name="301_____参数"></a>
#### 参数
与选项不同，参数没有*key*，而是由*index*标识。 例如，在`java com.acme.Foo`中，`com.acme.Foo`是一个参数。

参数没有名称，使用基于0的索引进行标识。 第一个参数的索引为`0`：

```groovy
def cli = CLI.create("some-name").addArgument([
  index:0,
  description:"the first argument",
  argName:"arg1"
]).addArgument([
  index:1,
  description:"the second argument",
  argName:"arg2"
])
```

如果您未设置参数索引，它将使用声明顺序自动计算。

```groovy
def cli = CLI.create("some-name").addArgument([
  description:"the first argument",
  argName:"arg1"
]).addArgument([
  description:"the second argument",
  argName:"arg2"
])
```

`argName`是可选的，在用法消息中使用。

`Argument`可以有一下选项：

- 被`setHidden`隐藏
- 使用`setRequired`说明是强制性的
- 使用`setDefaultValue`设置默认值
- 使用`setMultiValued`接收多个值--只有最后一个参数可以是多值的。

还可以从JSON格式实例化选项。

<a name="302_____用法生成"></a>
#### 用法生成
一旦您的`CLI`实例被配置，您可以生成*usage*消息：

```groovy
def cli = CLI.create("copy").setSummary("A command line interface to copy files.").addOption([
  longName:"directory",
  shortName:"R",
  description:"enables directory support",
  flag:true
]).addArgument([
  index:0,
  description:"The source",
  argName:"source"
]).addArgument([
  index:0,
  description:"The destination",
  argName:"target"
])

def builder = new java.lang.StringBuilder()
cli.usage(builder)
```

它生成如下用法消息：

```bash
Usage: copy [-R] source target

A command line interface to copy files.

 -R,--directory   enables directory support
```

如果需要调整用法消息，请查看`UsageMessageFormatter`类。

<a name="303____解析阶段"></a>
### 解析阶段
配置完`CLI`实例后，您可以解析用户命令行以评估每个选项和参数：

```groovy
def commandLine = cli.parse(userCommandLineArguments)
```

`parse`方法返回一个包含值的`CommandLine`对象。 默认情况下，它会验证用户命令行，并检查是否已设置每个强制性选项和参数以及每个选项接收的值数量。 您可以通过传递`false`作为parse的第二个参数来禁用验证。 如果您想检查参数或选项是否存在，即使已解析的命令行无效，这也很有用。

您可以使用`isValid`检查`CommandLine`是否有效。

<a name="304____查询_讯问阶段"></a>
### 查询/讯问阶段
解析后，您可以从parse方法返回的CommandLine对象中检索选项和参数的值：

```groovy
def commandLine = cli.parse(userCommandLineArguments)
def opt = commandLine.getOptionValue("my-option")
def flag = commandLine.isFlagEnabled("my-flag")
def arg0 = commandLine.getArgumentValue(0)
```

您的选项之一可以被标记为“help”。 如果用户命令行启用了“help”选项，则验证不会失败，但是可以让您检查用户是否寻求帮助：

```groovy
def cli = CLI.create("test").addOption([
  longName:"help",
  shortName:"h",
  flag:true,
  help:true
]).addOption([
  longName:"mandatory",
  required:true
])

def line = cli.parse(java.util.Collections.singletonList("-h"))

// The parsing does not fail and let you do:
if (!line.isValid() && line.isAskingForHelp()) {
  def builder = new java.lang.StringBuilder()
  cli.usage(builder)
  stream.print(builder.toString())
}
```

<a name="305___vert_x启动器"></a>
## vert.x启动器
vert.x的`Launcher`在*fat jar*中用作主类，并由`vertx`命令行实用程序使用。 它执行一组*命令*，例如*run*，*bare*，*start* ...

<a name="306____扩展vert_x启动器"></a>
### 扩展vert.x启动器
您可以通过实现自己的`Command`（仅适用于Java）来扩展命令集：

```groovy
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

您还需要实现`CommandFactory`：

```groovy
public class HelloCommandFactory extends DefaultCommandFactory<HelloCommand> {
 public HelloCommandFactory() {
  super(HelloCommand.class);
 }
}
```

然后，创建`src/main/resources/META-INF/services/io.vertx.core.spi.launcher.CommandFactory`并添加一行指示工厂的全限定名称：

```groovy
io.vertx.core.launcher.example.HelloCommandFactory
```

构建包含命令的jar。 确保包含SPI文件(`META-INF/services/io.vertx.core.spi.launcher.CommandFactory`)。

然后，将包含命令的jar放入fat-jar的类路径（或将其包含在里面）或vert.x发行版的`lib`目录中，您将可以执行：

```bash
vertx hello vert.x
java -jar my-fat-jar.jar hello vert.x
```

<a name="307____在fat_jars中使用启动器"></a>
### 在fat jars中使用启动器
要在*fat-jar*中使用`Launcher`类，只需将*MANIFEST*的`Main-Class`设置为`io.vertx.core.Launcher`。 另外，将`Main-Verticle`  *MANIFEST*条目设置为主verticle的名称。

默认情况下，它执行`run`命令。 但是，您可以通过设置`Main-Command` *MANIFEST*条目来配置默认命令。 如果在没有命令的情况下启动*fat jar*，则使用默认命令。

<a name="308____对启动器进行子类化"></a>
### 对启动器进行子类化
您也可以创建`Launcher`的子类来启动您的应用程序。 该类已被设计为易于扩展。

`Launcher`子类可以：

- 在`beforeStartingVertx`中自定义vert.x配置
- 通过覆盖`afterStartingVertx`来检索由“run”或“bare”命令创建的vert.x实例。
- 使用`getMainVerticle`和`getDefaultCommand`配置默认的verticle和命令
- 使用`register`和`unregister`添加/删除命令

<a name="309____启动器和退出代码"></a>
### 启动器和退出代码
当您使用`Launcher`类作为主类时，它使用以下退出代码：

- `0` 如果过程顺利结束，或者抛出未捕获的错误
- `1` 对于通用错误
- `11` 如果Vert.x无法初始化
- `12` 如果无法启动、查找或停止衍生进程。这个错误代码由`start`和`stop`命令使用
- `14` 如果系统配置不符合系统要求（找不到java的shc）
- `15` 如果主verticle无法部署

<a name="310___配置Vert_x缓存"></a>
## 配置Vert.x缓存
当Vert.x需要从类路径中读取文件时（嵌入在一个 fat jar中，以类路径的jar形式或位于类路径中的文件），它将把它复制到缓存目录中。 这背后的原因很简单：从jar或从输入流读取文件是阻塞的。 因此，为避免每次都付出代价，Vert.x将该文件复制到其缓存目录，并在以后每次读取时从该目录读取该文件。 此行为可以配置。

首先，默认情况下，Vert.x使用`$CWD/.vertx`作为缓存目录。 它在此目录内创建一个唯一目录，以避免冲突。 这个位置可以通过使用`vertx.cacheDirBase`系统属性来配置。 例如，如果当前工作目录不可写（例如在不可变的容器上下文中），请使用以下命令启动应用程序：

```bash
vertx run my.Verticle -Dvertx.cacheDirBase=/tmp/vertx-cache
# or
java -jar my-fat.jar vertx.cacheDirBase=/tmp/vertx-cache
```

------
> **重要:** 该目录必须是“可写的”。
>
------

当您编辑HTML，CSS或JavaScript之类的资源时，此缓存机制可能很烦人，因为它仅提供文件的第一个版本（因此，如果重新加载页面，您将看不到编辑内容）。 为了避免这种情况，请使用`-Dvertx.disableFileCaching=true`启动您的应用程序。 有了这个设置，Vert.x仍然使用缓存，但总是使用原始源刷新缓存中存储的版本。因此，如果您编辑来自类路径的文件并刷新浏览器，那么Vert.x从类路径中读取它，将它复制到缓存目录并从那里提供服务。不要在生产中使用这种设置，它会扼杀你的表现。

最后，您可以使用`-Dvertx.disableFileCPResolving=true`完全禁用高速缓存。 此设置并非没有后果。 Vert.x将无法从类路径（仅从文件系统）读取任何文件。 使用此设置时要非常小心。

