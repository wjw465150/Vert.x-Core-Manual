# Hazelcast  集群管理器

Vert.x 基于 [Hazelcast](https://hazelcast.com/) 实现了一个集群管理器。

这是 Vert.x CLI默认的集群管理器。由于 Vert.x 集群管理器的可插拔性，可轻易切换至其它的集群管理器。

这个集群管理器由以下依赖引入：

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-hazelcast</artifactId>
 <version>4.3.5</version>
</dependency>
```

Vert.x 集群管理器包含以下几项功能：

- 发现并管理集群中的节点
- 管理集群的 EventBus 地址订阅清单（这样就可以轻松得知集群中的哪些节点订阅了哪些 EventBus 地址）
- 分布式 Map 支持
- 分布式锁
- 分布式计数器

<mark>**Vert.x 集群器 **并不** 处理节点之间的通信。在 Vert.x 中，集群节点间通信是直接由 TCP 连接处理的。**</mark>

## 使用集群管理器

如果通过命令行来使用 Vert.x，对应集群管理器的 `jar` 包（名为 `vertx-hazelcast-4.3.5.jar` ） 应该在 Vert.x 中安装路径的 `lib` 目录中。

如果在 Maven 或者 Gradle 工程中使用 Vert.x， 只需要在工程依赖中加上依赖： `io.vertx:vertx-hazelcast:4.3.5`。

如果（集群管理器的）jar 包在 classpath 中，Vert.x将自动检测到并将其作为集群管理器。 <mark>**需要注意的是，要确保 Vert.x 的 classpath 中没有其它的集群管理器实现， 否则会使用错误的集群管理器。**</mark>

编程内嵌 Vert.x 时，可以在创建 Vert.x 实例时， 通过编程的方式显式配置 Vert.x 集群管理器，例如：

```java
ClusterManager mgr = new HazelcastClusterManager();

VertxOptions options = new VertxOptions().setClusterManager(mgr);

Vertx.clusteredVertx(options, res -> {
  if (res.succeeded()) {
    Vertx vertx = res.result();
  } else {
    // 失败
  }
});
```

## 配置集群管理器

### 使用XML文件配置

通常情况下，集群管理器的相关配置默认是通过打包在jar中的配置文件 [`default-cluster.xml`](https://github.com/vert-x3/vertx-hazelcast/blob/master/src/main/resources/default-cluster.xml) 配置的。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ Copyright 2017 Red Hat, Inc.
  ~
  ~ Red Hat licenses this file to you under the Apache License, version 2.0
  ~ (the "License"); you may not use this file except in compliance with the
  ~ License.  You may obtain a copy of the License at:
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  ~ WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
  ~ License for the specific language governing permissions and limitations
  ~ under the License.
  -->

<hazelcast xmlns="http://www.hazelcast.com/schema/config"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://www.hazelcast.com/schema/config
           https://www.hazelcast.com/schema/config/hazelcast-config-4.2.xsd">

  <network>
    <join>
      <multicast/>
    </join>
  </network>

  <multimap name="__vertx.subs">
    <backup-count>1</backup-count>
    <value-collection-type>SET</value-collection-type>
  </multimap>

  <map name="__vertx.haInfo">
    <backup-count>1</backup-count>
  </map>

  <map name="__vertx.nodeInfo">
    <backup-count>1</backup-count>
  </map>

  <cp-subsystem>
    <cp-member-count>0</cp-member-count>
    <semaphores>
      <semaphore>
        <name>__vertx.*</name>
        <jdk-compatible>false</jdk-compatible>
        <initial-permits>1</initial-permits>
      </semaphore>
    </semaphores>
  </cp-subsystem>

</hazelcast>
```

如果要覆盖此配置，可以在 classpath 中添加一个 `cluster.xml` 文件。如果想在 fat jar 中内嵌 `cluster.xml` ，此文件必须在 fat jar 的根目录中。如果此文件是一个外部文件，则必须将其所在的 **目录** 添加至 classpath 中。举个例子，如果使用 Vert.x 的 *launcher* 启动应用，则 classpath 应该设置为：

```sh
# 如果 cluster.xml 在当前目录：
java -jar ... -cp . -cluster
vertx run MyVerticle -cp . -cluster

# 如果 cluster.xml 在 conf 目录：
java -jar ... -cp conf -cluster
```

还可以通过配置系统属性 `vertx.hazelcast.config` 来覆盖默认的配置文件：

```sh
# 指定一个外部文件为自定义配置文件
java -Dvertx.hazelcast.config=./config/my-cluster-config.xml -jar ... -cluster

# 从 classpath 中加载一个文件为自定义配置文件
java -Dvertx.hazelcast.config=classpath:my/package/config/my-cluster-config.xml -jar ... -cluster
```

如果 `vertx.hazelcast.config` 值不为空时，将用其覆盖 classpath 中所有的 `cluster.xml` 文件； 但是如果加载 `vertx.hazelcast.config` 系统配置失败时， 系统将选取 classpath 任意一个 `cluster.xml` ，甚至直接使用默认配置。

> **📌小心:** Vert.x 并不支持 `-Dhazelcast.config` 设置方式， 请不要使用

这里的 xml 是 Hazelcast 的配置文件， 可以在 Hazelcast 官网找到详细的配置文档。

### 通过编程配置

您也可以通过编程的形式配置集群管理器：

```java
Config hazelcastConfig = new Config();

// 设置相关的hazlcast配置，在这里省略掉，不再赘述

ClusterManager mgr = new HazelcastClusterManager(hazelcastConfig);

VertxOptions options = new VertxOptions().setClusterManager(mgr);

Vertx.clusteredVertx(options, res -> {
  if (res.succeeded()) {
    Vertx vertx = res.result();
  } else {
    // 失败！
  }
});
```

您也可以对已存在的XML配置进行修改。 比如修改集群名：

```java
Config hazelcastConfig = ConfigUtil.loadConfig();

hazelcastConfig.setClusterName("my-cluster-name");

ClusterManager mgr = new HazelcastClusterManager(hazelcastConfig);

VertxOptions options = new VertxOptions().setClusterManager(mgr);

Vertx.clusteredVertx(options, res -> {
  if (res.succeeded()) {
    Vertx vertx = res.result();
  } else {
    // 失败！
  }
});
```

`ConfigUtil#loadConfig` 方法会加载 Hazelcast 的XML配置文件，并将其转换为 `Config` 对象。 读取的XML配置文件来自：

1. `vertx.hazelcast.config` 系统配置指定的文件，若不存在则
2. classpath 内的 `cluster.xml` 文件，若不存在则
3. 默认的配置文件

### 发现配置

Hazelcast 支持几种不同的发现配置。 Hazelcast 默认配置使用多播，因此您必须在网络上启用多播才能正常工作。

关于如何配置不同的发现方式，请查阅 Hazelcast 文档。

### 通过系统配置改变本地地址及公共地址

有时，集群节点必须绑定到其他集群成员无法访问的地址。 例如，节点不在同一网络区域中，或在某些具有特定防火墙配置的云服务中时，可能会发生这种情况。

可以使用以下系统属性设置绑定的本地地址和公共地址（向其他成员发布的地址）：

```sh
-Dhazelcast.local.localAddress=172.16.5.131 -Dhazelcast.local.publicAddress=104.198.78.81
```

## 使用已存在的 Hazelcast 集群

可以向集群管理器传入 `HazelcastInstance` 来复用现有集群：

```java
ClusterManager mgr = new HazelcastClusterManager(hazelcastInstance);
VertxOptions options = new VertxOptions().setClusterManager(mgr);
Vertx.clusteredVertx(options, res -> {
  if (res.succeeded()) {
    Vertx vertx = res.result();
  } else {
    // 失败！
  }
});
```

在这种情况下，Vert.x不是 Hazelcast 集群的所有者，所以不要关闭 Vert.x 时关闭 Hazlecast 集群。

请注意，自定义 Hazelcast 实例需要以下配置：

```xml
<multimap name="__vertx.subs">
 <backup-count>1</backup-count>
 <value-collection-type>SET</value-collection-type>
</multimap>

<map name="__vertx.haInfo">
 <backup-count>1</backup-count>
</map>

<map name="__vertx.nodeInfo">
 <backup-count>1</backup-count>
</map>

<cp-subsystem>
 <cp-member-count>0</cp-member-count>
 <semaphores>
   <semaphore>
     <name>__vertx.*</name>
     <jdk-compatible>false</jdk-compatible>
     <initial-permits>1</initial-permits>
   </semaphore>
 </semaphores>
</cp-subsystem>
```

> **🔔重要:** 不支持 Hazelcast 客户端及智能客户端。

> **🔔重要:** 要确保 Hazelcast 集群 先于 Vert.x 集群启动，后于 Vert.x 集群关闭。 同时需要禁用 `shutdown hook` （参考上述的 xml 配置，或通过系统变量来实现）。

## 修改故障节点的超时配置

缺省情况下，Hazelcast 会移除集群中超过300秒没收到心跳的节点。 通过系统配置 `hazelcast.max.no.heartbeat.seconds` 可以修改这个超时时间，如:

```
-Dhazelcast.max.no.heartbeat.seconds=5
```

修改后，超过5秒没发出心跳的节点会被移出集群。

请参考 [Hazelcast 系统配置](https://docs.hazelcast.org/docs/latest/manual/html-single/#system-properties) 。

## 集群故障排除

如果默认的组播配置不能正常运行，通常有以下原因：

### 机器禁用组播

通常来说，OSX 默认禁用组播。 请自行Google一下如何启用组播。

### 使用错误的网络接口

如果机器上有多个网络接口（也有可能是在运行 VPN 的情况下）， 那么 Hazelcast 很有可能使用错误的网络接口。

为了确保 Hazelcast 使用正确的网络接口，在配置文件中将 `interface` 设置为指定IP地址。 同时确保 `enabled` 属性设置为 `true` 。例如：

```xml
<interfaces enabled="true">
 <interface>192.168.1.20</interface>
</interfaces>
```

### 使用VPN

VPN 软件工作时通常会创建虚拟网络接口，但往往不支持组播。 在 VPN 环境中，如果 Hazelcast 与 Vert.x 没有配置正确的话， 将会选择 VPN 创建的网络接口，而不是正确的网络接口。

所以，如果您的应用运行在 VPN 环境中，请参考上述章节， 设置正确的网络接口。

### 组播不可用

在某些情况下，因为特殊的运行环境，可能无法使用组播。 在这种情况下，应该配置其他网络传输，例如使用 TCP 套接字，或在亚马逊云 EC2 上使用AWS。

有关 Hazelcast 更多传输方式，以及如何配置它们， 请查询 Hazelcast 文档。

### 开启日志

在排除故障时，开启 Hazelcast 日志很有帮助，可以观察是否组成了集群。 使用默认的 JUL 日志时，在 classpath 中添加 `vertx-default-jul-logging.properties` 文件可开启 Hazelcast 日志。 这是一个标准 java.util.logging（JUL） 配置文件。 具体配置如下：

```properties
com.hazelcast.level=INFO
```

以及

```properties
java.util.logging.ConsoleHandler.level=INFO
java.util.logging.FileHandler.level=INFO
```

## Hazelcast 日志配置

Hazelcast 的日志默认采用 `JDK` 的实现（即 JUL）。 如果想切换至其他日志库，通过设置系统配置 `hazelcast.logging.type` 即可：

```properties
-Dhazelcast.logging.type=slf4j
```

详细文档请参考 [hazelcast 文档](http://docs.hazelcast.org/docs/3.6.1/manual/html-single/index.html#logging-configuration) 。

## 使用其他 Hazelcast 版本

当前的 Vert.x HazelcastClusterManager 使用的 Hazelcast 版本为 `4.2.5` 。 如果开发者想使用其他版本的 Hazelcast，需要做以下工作：

- 将目标版本的 Hazelcast 依赖添加至 classpath 中
- 如果是 fat jar 的形式，在构建工具中使用正确的版本

使用 Maven 时可参考下面代码：

```xml
<dependency>
 <groupId>com.hazelcast</groupId>
 <artifactId>hazelcast</artifactId>
 <version>ENTER_YOUR_VERSION_HERE</version>
</dependency>
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-hazelcast</artifactId>
 <version>4.3.5</version>
</dependency>
```

对于某些版本，您可能需要排除掉一些（冲突的）依赖。

对于 Gradle 可以使用下面代码:

```groovy
dependencies {
  compile ("io.vertx:vertx-hazelcast:4.3.5"){
    exclude group: 'com.hazelcast', module: 'hazelcast'
  }
  compile "com.hazelcast:hazelcast:ENTER_YOUR_VERSION_HERE"
}
```

## 配置 Kubernetes

Kubernetes 上的 Hazelcast 要配置为使用 [Hazelcast Kubernetes](https://github.com/hazelcast/hazelcast-kubernetes) 插件。

首先在项目中增加依赖： `io.vertx:vertx-hazelcast:${vertx.version}` 和 `com.hazelcast:hazelcast-kubernetes:${hazelcast-kubernetes.version}` 。 对于 Maven，参考下面代码：

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-hazelcast</artifactId>
 <version>${vertx.version}</version>
</dependency>
<dependency>
 <groupId>com.hazelcast</groupId>
 <artifactId>hazelcast-kubernetes</artifactId>
 <version>${hazelcast-kubernetes.version}</version>
</dependency>
```

> **📝注意:** 如果您使用了其他版本的 Hazelcast core 依赖，请确保兼容 Kubernetes discovery 插件。

然后在 Hazelcast 配置中配置 Kubernetes discovery 插件，可以通过自定义的 `cluster.xml` 文件进行配置，或通过编程方式配置（参考 [配置集群管理器](https://vertx-china.github.io/docs/vertx-hazelcast/java/#configcluster)）。

Kubernetes discovery 插件提供了两种可选的 [发现模式](https://github.com/hazelcast/hazelcast-kubernetes#understanding-discovery-modes)： *Kubernetes API* 和 *DNS Lookup* 。 关于这两种模式的利弊，请参阅该插件的项目网站。

在本文中，我们使用 *DNS Lookup* 发现模式。请修改/增加以下的配置：

```xml
<hazelcast>
 <properties>
   <property name="hazelcast.discovery.enabled">true</property> (1)
 </properties>

 <network>
   <join>
     <multicast enabled="false"/> (2)
     <tcp-ip enabled="false" />

     <discovery-strategies>
       <discovery-strategy enabled="true"> (3)
           class="com.hazelcast.kubernetes.HazelcastKubernetesDiscoveryStrategy">
         <properties>
           <property name="service-dns">MY-SERVICE-DNS-NAME</property> (4)
         </properties>
       </discovery-strategy>
     </discovery-strategies>
   </join>
 </network>
</hazelcast>
```

1. 启用 Discovery SPI
2. 停用其他发现模式
3. 启用 Kubernetes 插件
4. 服务DNS，通常以 `MY-SERVICE-NAME.MY-NAMESPACE.svc.cluster.local` 的形式出现，视乎 Kubernetes 的分布配置

`MY-SERVICE-DNS-NAME` 的取值必须是 Kubernetes 的一个 **无头** 服务（Headless Services），Hazelcast 将用其识别所有集群成员节点。 无头服务的创建配置可参考下面代码：

```yaml
apiVersion: v1
kind: Service
metadata:
 namespace: MY-NAMESPACE
 name: MY-SERVICE-NAME
spec:
 selector:
   component: MY-SERVICE-NAME (1)
 clusterIP: None
 ports:
 - name: hz-port-name
   port: 5701
   protocol: TCP
```

1. 按标签选择的集群成员

最终，属于集群的所有 Kubernetes 部署需要追加 `component` 标签：

```yaml
apiVersion: extensions/v1beta1
kind: Deployment
metadata:
 namespace: MY-NAMESPACE
spec:
 template:
   metadata:
     labels:
       component: MY-SERVICE-NAME
```

更多关于配置的详情请参考 [Hazelcast Kubernetes 插件页面](https://github.com/hazelcast/hazelcast-kubernetes)。

### 滚动更新

在滚动更新期间，建议逐一更换 Pod。

为此，我们必须将 Kubernetes 配置为：

- 不要同时启动多个新 Pod
- 在滚动更新过程中，不可用的 Pod 不能多于一个

```yaml
spec:
 strategy:
   type: Rolling
   rollingParams:
     updatePeriodSeconds: 10
     intervalSeconds: 20
     timeoutSeconds: 600
     maxUnavailable: 1 (1)
     maxSurge: 1 (2)
```

1. 在升级过程中允许 不可用的最大 Pod 数
2. 允许超过预期创建数量的最大 Pod 数（译者注：即，实际创建的 Pod 数量 ≤ 预期 Pod 数量 + maxSurge）

同样地，Pod 的准备情况探针（readiness probe）必须考虑集群状态。 请参阅 [集群管理](https://vertx-china.github.io/docs/vertx-hazelcast/java/#one-by-one) 章节，了解如何使用 [Vert.x 健康检查](https://vertx-china.github.io/docs/vertx-health-check/java/) 实现准备情况探针。

## 集群管理

Hazelcast 集群管理器的工作原理是将 Vert.x 节点作为 Hazelcast 集群的成员。 因此，Vert.x 使用 Hazelcast 集群管理器时，应遵循 Hazelcast 的管理准则。

首先介绍下分区数据和脑裂。

### 分区数据

每个 Vert.x 节点都包含部分集群数据，包括：EventBus 订阅，异步 Map，分布式计数器等等。

当有节点加入或离开集群时，Hazelcast 会迁移分区数据。 换句话说，它可以移动数据以适应新的集群拓扑。 此过程可能需要一些时间，具体取决于集群数据量和节点数量。

### 脑裂

在理想环境中，不会出现网络设备故障。 实际上，集群早晚会被分成多个小组，彼此之间不可见。

Hazelcast 能够将节点合并回单个集群。 但是，就像数据分区迁移一样，此过程可能需要一些时间。 在集群变回可用之前，某些 EventBus 的消费者可能无法获取到消息。 否则，重新部署故障的 Verticle 过程中无法保证高可用。

> **📝注意:** 很难（或者说基本不可能）区分脑裂和:长时间的GC暂停 （导致错过了心跳检查），部署新版本应用时，同时强制关闭了很多节点

### 建议

考虑到上面讨论的常见集群问题，建议遵循下述的最佳实践。

#### 优雅地关闭

应该避免强行停止集群成员节点（例如，对节点进程使用 `kill -9` ）。

当然，进程崩溃是不可避免的，但是优雅地关闭进程有助于其余节点更快地恢复稳定状态。

#### 逐个添加或移除节点

滚动更新新版本应用时，或扩大/缩小集群时，应该一个接一个地添加或移除节点。

逐个停止节点可避免集群误以为发生了脑裂。 逐个添加节点可以进行干净的增量数据分区迁移。

可以使用 [Vert.x 运行状况检查](https://vertx-china.github.io/docs/vertx-health-check/java/) 来验证集群安全性：

```java
Handler<Promise<Status>> procedure = ClusterHealthCheck.createProcedure(vertx);
HealthChecks checks = HealthChecks.create(vertx).register("cluster-health", procedure);
```

完成集群创建后，可以通过 [Vert.x Web](https://vertx-china.github.io/docs/vertx-web/java/) 路由 Handler 编写的HTTP程序进行健康检查：

```java
Router router = Router.router(vertx);
router.get("/readiness").handler(HealthCheckHandler.createWithHealthChecks(checks));
```

#### 使用轻量级成员（Lite Members）

为了尽量减少 Vert.x 集群适应新拓扑的时间，您可以使用外部数据节点，并将 Vert.x 节点标记为 [*轻量级成员*](https://docs.hazelcast.org/docs/latest/manual/html-single/#enabling-lite-members)。

*轻量级成员* 像普通成员一样加入 Hazelcast 集群，但是他们不拥有任何数据分区。 因此，添加或删除此类成员时，Hazelcast 不需要迁移数据分区。

> **🔔重要:** 您必须事先启动外部数据节点，因为 Hazelcast 不会只使用 *轻量级成员* 节点创建集群。 

启动外部数据节点可以使用 Hazelcast 分发启动脚本，或以编程方式进行。

可以在XML配置中将Vert.x节点标记为 *轻量级成员* 节点：

```xml
<lite-member enabled="true"/>
```

还可以通过编程实现：

```java
Config hazelcastConfig = ConfigUtil.loadConfig()
  .setLiteMember(true);

ClusterManager mgr = new HazelcastClusterManager(hazelcastConfig);

VertxOptions options = new VertxOptions().setClusterManager(mgr);

Vertx.clusteredVertx(options, res -> {
  if (res.succeeded()) {
    Vertx vertx = res.result();
  } else {
    // failed!
  }
});
```

------

<<<<<< [完] >>>>>>

