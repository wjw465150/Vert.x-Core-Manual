# Context_API_简介

## `io.vertx.core.Context`

The execution context of a [`Handler`](https://vertx.io/docs/apidocs/io/vertx/core/Handler.html) execution.
处理程序的执行上下文.

When Vert.x provides an event to a handler or calls the start or stop methods of a `Verticle`, the execution is associated with a `Context`.
当Vert.x向处理程序提供事件或调用 `Verticle` 的start或stop方法时，执行与 `Context`关联。

Usually a context is an *event-loop context* and is tied to a specific event loop thread. So executions for that context always occur on that exact same event loop thread.
通常，上下文是*事件循环上下文*，并绑定到特定的事件循环线程。因此，该上下文的执行始终发生在完全相同的事件循环线程上。

In the case of worker verticles and running inline blocking code a worker context will be associated with the execution which will use a thread from the worker thread pool.
在工作线程和运行内联阻塞代码的情况下，工作线程上下文将与执行相关联，该执行将使用工作线程池中的线程。

When a handler is set by a thread associated with a specific context, the Vert.x will guarantee that when that handler is executed, that execution will be associated with the same context.
当处理程序由与特定上下文关联的线程设置时，Vert.x将保证当执行该处理程序时，该执行将与相同的上下文关联。

If a handler is set by a thread not associated with a context (i.e. a non Vert.x thread). Then a new context will be created for that handler.
如果处理程序是由与上下文不关联的线程设置的（即非Vert.x线程）。 然后将为该处理程序创建一个新的上下文。

In other words, a context is propagated.
换句话说，上下文被传播。

This means that when a verticle is deployed, any handlers it sets will be associated with the same context - the context of the verticle.
这意味着，当部署一个 verticle 时，其设置的任何处理程序都将与相同的上下文相关联 -  verticle 的上下文。

This means (in the case of a standard verticle) that the verticle code will always be executed with the exact same thread, so you don't have to worry about multi-threaded acccess to the verticle state and you can code your application as single threaded.
这意味着(在标准verticle的情况下), verticle代码将始终使用完全相同的线程执行，因此您不必担心对verticle状态的多线程访问，并且可以将应用程序编码为单线程。

This class also allows arbitrary data to be `put(java.lang.Object, java.lang.Object)` and `get(java.lang.Object)` on the context so it can be shared easily amongst different handlers of, for example, a verticle instance.
这个类还允许将任意数据  `put(java.lang。Object, java.lang.Object)` 和 `get(java.lang.Object)` 放在上下文上，因此它可以在不同的处理程序之间轻松共享，例如，一个verticle实例。

This class also provides `runOnContext(io.vertx.core.Handler)` which allows an action to be executed asynchronously using the same context.
这个类还提供了 `runOnContext(io. vertex .core. handler)`  ，它允许使用相同的上下文异步执行操作。
