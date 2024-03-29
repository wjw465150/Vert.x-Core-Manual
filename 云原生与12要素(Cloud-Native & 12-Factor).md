# 云原生与12要素(Cloud-Native & 12-Factor)

## 前言

**Cloud-Native:**

Java并不是为了Web而诞生，但似乎B/S架构让Java生机无限，Spring全家桶的助推也使得Java在Web更为强大，微服务体系Spring Cloud更是顺风顺水，不得不说的Spring应用的痛点就是:

- 启动过慢
- 内存占用偏高
- 对服务器资源占用较大

而且JVM的本身就难逃离内存的过度依赖.

随着容器化技术Docker、Kubernetes，让云原生似乎成为了未来的发展方向，**云原生(Cloud-Native)**这个概念最早由Pivotal公司的Matt Stine于2013年首次提出，提到云原生首先想到的关键词可能就是 **容器化**、**微服务**、**Lambda**，**服务网格** 等...

当然这些是必要元素，但是不代表拥有这些元素就是云原生应用，很多应用的部署只能是基于云来完成，比如私有云、公有云，这也是未来的趋势。

**云原生本质上不是部署，而是以什么方式来构建应用**，云原生的最终目的是为了提高开发效率，提升业务敏捷度、扩容性、可用性、资源利用率，降低成本。

Go语言作为一种云原生语言也体现出了强大的生命力，Java也在变化，2018年Java出现了大量轻量级微服务框架，来面对未来的云原生趋势，Eclipse基金会的Vert.x, Red Hat推出的Quarkus、Oracle的Helidon以及Spring Native都在快速发展，拥抱云原生。

**12-Factor:**

也称为“12要素”，是一套流行的应用程序开发原则。云原生架构中使用12-Factor作为设计准则。

12-Factor 的目标在于：

- 使用标准化流程自动配置，从而使新的开发者花费最少的学习成本加入项目中。
- 和底层操作系统之间尽可能的划清界限，在各个系统中提供最大的可移植性。
- 适合部署在现代的云计算平台，从而在服务器和系统管理方面节省资源。
- 将开发环境和生产环境的差异降至最低，并使用持续交付实施敏捷开发。
- 可以在工具、架构和开发流程不发生明显变化的前提下实现扩展。

12-Factor 可以适用于任意语言和后端服务（数据库、消息队列、缓存等）开发的应用程序，自然也适用于云原生。在构建云原生应用时，也需要考虑这十二个方面的内容。

## 1 基准代码

代码是程序的根本，有什么样的代码最终会表现为怎么样的程序软件。从源码到产品发布中间会经历多个环节，比如开发、编译、测试、构建、部署等，这些环节可能都有自己的不同的部署环境，而不同的环境相应的责任人关注于产品的不同阶段。比如，测试人员主要关注于测试的结果，而业务人员可能关注于生产环境的最终的部署结果。但不管是哪个环节，部署到怎么的环境中，他们所依赖的代码是一致的，即所谓的“一份基准代码（Codebase），多份部署（Deploy）”。

现代的代码管理，往往需要进行版本的管理。即便是个人的工作，采用版本管理工具进行管理，对于方便查找特定版本的内容，或者是回溯历史的修改内容都是极其必要。版本控制系统就是帮助人们协调工作的工具，它能够帮助我们和其他小组成员监测同一组文件，比如说软件源代码，升级过程中所做的变更，也就是说，它可以帮助我们轻松地将工作进行融合。

版本控制工具发展到现在已经有几十年了，简单地可以将其分为四代：

- 文件式版本控制系统，比如 SCCS、RCS；
- 树状版本控制系统—服务器模式，比如 CVS；
- 树状版本控制系统—双服务器模式，比如 Subversion；
- 树状版本控制系统—分布式模式，比如 Bazaar、Mercurial、Git。

目前，在企业中广泛采用服务器模式的版本控制系统，但越来越多的企业开始倾向于采用分布式模式版本控制系统。

读者如果对版本控制系统感兴趣，可以参阅笔者所著的《分布式系统常用技术及案例分析》中的“第7章分布式版本控制系统”内容。本书“10.3 代码管理”章节部分，还会继续深入探讨 Git 的使用。

## 2 依赖

应该明确声明应用程序依赖关系（Dpendency），这样，所有的依赖关系都可以从工件的存储库中获得，并且可以使用依赖管理器（例如 Apache Maven、Gradle）进行下载。

显式声明依赖的优点之一是为新进开发者简化了环境配置流程。新进开发者可以检出应用程序的基准代码，安装编程语言环境和它对应的依赖管理工具，只需通过一个构建命令来安装所有的依赖项，即可开始工作。

比如，项目组统一采用 Gradle 来进行依赖管理。那么可以使用 Gradle Wrapper。Gradle Wrapper 免去了用户在使用 Gradle 进行项目构建时需要安装 Gradle 的繁琐步骤。每个 Gradle Wrapper 都绑定到一个特定版本的 Gradle，所以当你第一次在给定 Gradle 版本下运行上面的命令之一时，它将下载相应的 Gradle 发布包，并使用它来执行构建。默认，Gradle Wrapper 的发布包是指向的官网的 Web 服务地址，相关配置记录在了 gradle-wrapper.properties 文件中。我们查看下 Sring Boot 提供的这个 Gradle Wrapper 的配置，参数“distributionUrl”就是用于指定发布包的位置。

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-4.5.1-bin.zip
```

而这个 gradle-wrapper.properties 文件是作为依赖项，而纳入代码存储库中的。

## 3 配置

相同的应用，在不同的部署环境（如预发布、生产环境、开发环境等等）下，可能有不同的配置内容。这其中包括：

- 数据库、Redis 以及其他后端服务的配置；
- 第三方服务的证书；
- 每份部署特有的配置，如域名等。

这些配置项不可能硬编码在代码中，因为我们必须要保证同一份基准代码（Codebase）能够多份部署。一种解决方法是使用配置文件，但不把它们纳入版本控制系统，就像 Rails 的 config/database.yml。这相对于在代码中硬编码常量已经是长足进步，但仍然有缺点：

- 不小心将配置文件签入了代码库；
- 配置文件的可能会分散在不同的目录，并有着不同的格式，不方便统一管理；
- 这些格式通常是语言或框架特定的，不具备通用性。

所以，推荐的做法是将应用的配置存储于环境变量中。好处在于：

- 环境变量可以非常方便地在不同的部署间做修改，却不动一行代码；
- 与配置文件不同，不小心把它们签入代码库的概率微乎其微；
- 与一些传统的解决配置问题的机制（比如 Java 的属性配置文件）相比，环境变量与语言和系统无关。

本书介绍了另外一种解决方案——集中化配置中心。通过配置中心来集中化管理各个环境的配置变量。配置中心的实现也是于具体语言和系统无关的。欲了解有关配置中心的内容，可以参阅本书“10.5 配置管理”章节的内容。

## 4 后端服务

后端服务（Backing Services）是指程序运行所需要的通过网络调用的各种服务，如数据库（MySQL，CouchDB），消息/队列系统（RabbitMQ，Beanstalkd），SMTP 邮件发送服务（Postfix），以及缓存系统（Memcached，Redis）。

这些后端服务，通常由部署应用程序的系统管理员一起管理。除了本地服务之外，应用程序有可能使用了第三方发布和管理的服务。示例包括 SMTP（例如 Postmark），数据收集服务（例如 New Relic 或 Loggly），数据存储服务（如 Amazon S3），以及使用 API 访问的服务（例如 Twitter、Google Maps 等等）。

12-Factor 应用不会区别对待本地或第三方服务。对应用程序而言，本地或第三方服务都是附加资源，都可以通过一个 URI 或是其他存储在配置中的服务定位或服务证书来获取数据。12-Factor 应用的任意部署，都应该可以在不进行任何代码改动的情况下，将本地 MySQL 数据库换成第三方服务（例如 Amazon RDS）。类似的，本地 SMTP 服务应该也可以和第三方 SMTP 服务（例如 Postmark ）互换。比如，在上述两个例子中，仅需修改配置中的资源地址。

每个不同的后端服务都是一份资源 。例如，一个 MySQL 数据库是一个资源，两个 MySQL 数据库（用来数据分区）就被当作是两个不同的资源。12-Factor 应用将这些数据库都视作附加资源，这些资源和它们附属的部署保持松耦合。

使用后端服务的好处在于，部署可以按需加载或卸载资源。例如，如果应用的数据库服务由于硬件问题出现异常，管理员可以从最近的备份中恢复一个数据库，卸载当前的数据库，然后加载新的数据库，整个过程都不需要修改代码。

## 5 构建、发布、运行

基准代码进行部署需要以下三个阶段：

- 构建阶段：是指将代码仓库转化为可执行包的过程。构建时会使用指定版本的代码，获取和打包依赖项，编译成二进制文件和资源文件。
- 发布阶段：会将构建的结果和当前部署所需配置相结合，并能够立刻在运行环境中投入使用。
- 运行阶段：是指针对选定的发布版本，在执行环境中启动一系列应用程序进程。

应用严格区分构建、发布、运行这三个步骤。举例来说，直接修改处于运行状态的代码是非常不可取的做法，因为这些修改很难再同步回构建步骤。

部署工具通常都提供了发布管理工具，在必要的时候还是可以退回至较旧的发布版本。

每一个发布版本必须对应一个唯一的发布 ID，例如可以使用发布时的时间戳（2011-04-06-20:32:17），亦或是一个增长的数字（v100）。发布的版本就像一本只能追加的账本，一旦发布就不可修改，任何的变动都应该产生一个新的发布版本。

新的代码在部署之前，需要开发人员触发构建操作。但是，运行阶段不一定需要人为触发，而是可以自动进行。如服务器重启，或是进程管理器重启了一个崩溃的进程。因此，运行阶段应该保持尽可能少的模块，这样假设半夜发生系统故障而开发人员又捉襟见肘也不会引起太大问题。构建阶段是可以相对复杂一些的，因为错误信息能够立刻展示在开发人员面前，从而得到妥善处理。

## 6 进程

12-Factor 应用推荐以一个或多个无状态进程运行应用。这里的“无状态”是与 REST 中的无状态是一个意思，即进程的执行不依赖于上一个进程的执行。

举例来说，内存区域或磁盘空间可以作为进程在做某种事务型操作时的缓存，例如下载一个很大的文件，对其操作并将结果写入数据库的过程。12-Factor 应用根本不用考虑这些缓存的内容是不是可以保留给之后的请求来使用，这是因为应用启动了多种类型的进程，将来的请求多半会由其他进程来服务。即使在只有一个进程的情形下，先前保存的数据（内存或文件系统中）也会因为重启（如代码部署、配置更改、或运行环境将进程调度至另一个物理区域执行）而丢失。

一些互联网应用依赖于“粘性 session”， 这是指将用户 session 中的数据缓存至某进程的内存中，并将同一用户的后续请求路由到同一个进程。粘性 session 是 12-Factor 极力反对的。Session 中的数据应该保存在诸如 Memcached 或 Redis 这样的带有过期时间的缓存中。

相比于有状态的应用而言，无状态具有更好的可扩展性。

## 7 端口绑定

传统的互联网应用有时会运行于服务器的容器之中。例如 PHP 经常作为 Apache HTTPD 的一个模块来运行，而 Java 应用往往会运行于 Tomcat 中。

12-Factor 应用完全具备自我加载的能力，而不依赖于任何网络服务器就可以创建一个面向网络的服务。互联网应用通过端口绑定（Port binding）来提供服务，并监听发送至该端口的请求。

举例来说，Java 程序完全能够内嵌一个 Tomcat 在程序中，从而自己就能启动并提供服务，省去了将 Java 应用部署到 Tomcat 中的繁琐过程。在这方面，Spring Boot 框架的布道者 Josh Long 有句名言“Make JAR not WAR”，即 Java 应用程序应该被打包为可以独立运行的 JAR 文件，而不是传统的 WAR 包。

以 Spring Boot 为例，构建一个具有内嵌容器的 Java 应用是非常简单的，只需要引入以下依赖：

```scss
// 依赖关系
dependencies {
 
    // 该依赖用于编译阶段
    compile('org.springframework.boot:spring-boot-starter-web')
 
}
```

这样，该 Spring Boot 应用就包含了内嵌 Tomcat 容器。

如果想使用其他容器，比如 Jetty、Undertow 等，只需要在依赖中加入相应 Servlet 容器的 Starter 就能实现默认容器的替换，比如：

- spring-boot-starter-jetty：使用 Jetty 作为内嵌容器，可以替换 spring-boot-starter-tomcat；
- spring-boot-starter-undertow：使用 Undertow 作为内嵌容器，可以替换 spring-boot-starter-tomcat。

可以使用 Spring Environment 属性配置常见的 Servlet 容器的相关设置。通常您将在 application.properties 文件中来定义属性。

常见的 Servlet 容器设置包括：

- 网络设置：监听 HTTP 请求的端口（server.port）、绑定到 server.address 的接口地址等；
- 会话设置：会话是否持久（server.session.persistence）、会话超时（server.session.timeout）、会话数据的位置（server.session.store-dir）和会话 cookie 配置（server.session.cookie.*）；
- 错误管理：错误页面的位置（server.error.path）等；
- SSL；
- HTTP 压缩。

Spring Boot 尽可能地尝试公开这些常见公用设置，但也会有一些特殊的配置。对于这些例外的情况，Spring Boot 提供了专用命名空间来对应特定于服务器的配置（比如 server.tomcat 和 server.undertow）。

## 8 并发

在 12-factor 应用中，进程是一等公民。由于进程之间不会共享状态，这意味着应用可以通过进程的扩展来实现并发。

类似于 unix 守护进程模型，开发人员可以运用这个模型去设计应用[架构](https://so.csdn.net/so/search?q=架构&spm=1001.2101.3001.7020)，将不同的工作分配给不同的进程。例如，HTTP 请求可以交给 web 进程来处理，而常驻的后台工作则交由 worker 进程负责。

在 Java 语言中，往往通过多线程的方式来实现程序的并发。线程允许在同一个进程中同时存在多个线程控制流。线程会共享进程范围内的资源，例如内存句柄和文件句柄，但每个线程都有各自的程序计数器、栈以及局部变量。线程还提供了一种直观的分解模式来充分利用操作系统中的硬件并行性，而在同一个程序中的多个线程也可以被同时调度到多个CPU上运行。

毫无疑问，多线程编程使得程序任务并发成为了可能。而并发控制主要是为了解决多个线程之间资源争夺等问题。并发一般发生在数据聚合的地方，只要有聚合，就有争夺发生，传统解决争夺的方式采取线程锁机制，这是强行对CPU管理线程进行人为干预，线程唤醒成本高，新的无锁并发策略来源于异步编程、非阻塞I/O等编程模型。

并发的使用并非没有风险。多线程并发会带来如下的问题：

- 安全性问题。在没有充足同步的情况下，多个线程中的操作执行顺序是不可预测的，甚至会产生奇怪的结果。线程间的通信主要是通过共享访问字段及其字段所引用的对象来实现的。这种形式的通信是非常有效的，但可能导致两种错误：线程干扰（thread interference）和内存一致性错误（memory consistency errors）。
- 活跃度问题。一个并行应用程序的及时执行能力被称为它的活跃度（liveness）。安全性的含义是“永远不发生糟糕的事情”，而活跃度则关注于另外一个目标，即“某件正确的事情最终会发生”。当某个操作无法继续执行下去，就会发生活跃度问题。在串行程序中，活跃度问题形式之一就是无意中造成的无限循环（死循环）。而在多线程程序中，常见的活跃度问题主要有死锁、饥饿以及活锁。
- 性能问题。在设计良好的并发应用程序中，线程能提升程序的性能，但无论如何，线程总是带来某种程度的运行时开销。而这种开销主要是在线程调度器临时关起活跃线程并转而运行另外一个线程的上下文切换操作（Context Switch）上，因为执行上下文切换，需要保存和恢复执行上下文，丢失局部性，并且CPU时间将更多地花在线程调度而不线程运行上。当线程共享数据时，必须使用同步机制，而这些机制往往会抑制某些编译器优化，使内存缓存区中的数据无效，以及增加贡献内存总线的同步流量。所以这些因素都会带来额外的性能开销。

## 9 易处理

12-Factor 应用的进程是易处理（Disposable）的，意味着它们可以瞬间启动或停止。比如，Spring Boot 应用，它可以无需依赖容器，而采用内嵌容器的方式来实现自启动。这有利于迅速部署变化的代码或配置，保障系统的可用性，并在系统负荷到来前，快速实现扩展。

进程应当追求最小启动时间。 理想状态下，进程从敲下命令到真正启动并等待请求的时间应该只需很短的时间。更少的启动时间提供了更敏捷的发布以及扩展过程，此外还增加了健壮性，因为进程管理器可以在授权情形下容易的将进程搬到新的物理机器上。

进程一旦接收终止信号（SIGTERM）就会优雅的终止。就网络进程而言，优雅终止是指停止监听服务的端口，即拒绝所有新的请求，并继续执行当前已接收的请求，然后退出。

对于 worker 进程来说，优雅终止是指将当前任务退回队列。例如，RabbitMQ 中，worker 可以发送一个 NACK 信号。Beanstalkd 中，任务终止并退回队列会在 worker 断开时自动触发。有锁机制的系统诸如 Delayed Job 则需要确定释放了系统资源。

## 10 开发环境与线上环境等价

我们期望一份基准代码可以部署到多个环境，但如果环境不一致，最终也可能导致运行程序的结果不一致。

比如，在开发环境，我们是采用了 MySQL 作为测试数据库，而在线上生产环境，则是采用了 Oracle。虽然，MySQL 和 Oracle 都遵循相同的 SQL 标准，但两者在很多语法上还是存在细微的差异。这些差异非常有可能导致两者的执行结果不一致，甚至某些 SQL 语句在开发环境能够正常执行，而在线上环境根本无法执行。这都给调试增加了复杂性，同时，也无法保障最终的测试效果。

所以，一个好的指导意见是，不同的环境尽量保持一样。开发环境、测试环境与线上环境设置成一样，更早发现测试问题，而不至于在生产环境才暴露出问题。

## 11 日志

在应用程序中打日志是一个好习惯。日志使得应用程序运行的动作变得透明。日志是在系统出现故障时，排查问题的有力帮手。

日志应该是事件流的汇总，将所有运行中进程和后端服务的输出流按照时间顺序收集起来。尽管在回溯问题时可能需要看很多行，日志最原始的格式确实是一个事件一行。日志没有确定开始和结束，但随着应用在运行会持续的增加。对于传统的 Java EE 应用程序而言，有许多框架和库可用于日志记录。Java Logging (JUL) 是 Java 自身所提供的现成选项。除此之外 Log4j、Logback 和 SLF4J 是其他一些流行的日志框架。

对于传统的单块架构而言，日志管理本身并不存在难点，毕竟所有的日志文件，都存储在应用所部属的主机上，获取日志文件或者搜索日志内容都比较简单。但在 Cloud Native 应用中，
情况则有非常大的不同。分布式系统，特别是微服务架构所带来的部署应用方式的重大转变，都使得微服务的日志管理面临很多新的挑战。一方面随着微服务实例的数量的增长，伴随而来的就是日志文件的递增。另一方面，日志被散落在各自的实例所部署的主机上，不方面整合和回溯。

在这种情况下，将日志进行集中化的管理变得意义重大。本书的“10.4 日志管理”章节内容，会对 Cloud Native 的日志集中化管理进行详细的探讨。

## 12 管理进程

开发人员经常希望执行一些管理或维护应用的一次性任务，例如：

- 运行数据移植（Django 中的 manage.py migrate, Rails 中的 rake db:migrate）。
- 运行一个控制台（也被称为 REPL shell），来执行一些代码或是针对线上数据库做一些检查。大多数语言都通过解释器提供了一个 REPL 工具（python 或 perl），或是其他命令（Ruby 使用 irb, Rails 使用 rails console）。
- 运行一些提交到代码仓库的一次性脚本。

一次性管理进程应该和正常的常驻进程使用同样的环境。这些管理进程和任何其他的进程一样使用相同的代码和配置，基于某个发布版本运行。后台管理代码应该随其他应用程序代码一起发布，从而避免同步问题。

所有进程类型应该使用同样的依赖隔离技术。例如，如果 Rub y的 web 进程使用了命令 bundle exec thin start，那么数据库移植应使用 bundle exec rake db:migrate。同样的，如果一个 Python 程序使用了 Virtualenv，则需要在运行 Tornado Web 服务器和任何 manage.py 管理进程时引入 bin/python。
