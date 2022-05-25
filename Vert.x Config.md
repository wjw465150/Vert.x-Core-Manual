# Vert.x Config

Vert.x Config 提供了一种配置 Vert.x 应用程序的方法。 它：

- 提供多种配置语法（JSON、properties、Yaml（扩展）、Hocon（扩展）...
- 提供多种配置存储，例如文件、目录、HTTP、git（扩展）、Redis（扩展）、系统属性和环境属性。
- 让您定义处理顺序和重载
- 支持运行时重新配置

## 概念

该库的结构围绕：

**Config Retriever** 由 Vert.x 应用程序实例化和使用。 它配置了一组配置存储**Configuration store**定义了读取配置数据的位置以及格式（默认为 JSON）

配置以 JSON 对象的形式检索。

## 使用 Config Retriever

要使用Config Retriever，请将以下依赖项添加到构建描述符的 *dependencies* 部分：

- Maven (在你的 `pom.xml`):

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-config</artifactId>
 <version>4.3.0</version>
</dependency>
```

- Gradle (在你的 `build.gradle`):

```groovy
compile 'io.vertx:vertx-config:4.3.0'
```

完成后，您首先需要实例化 `ConfigRetriever`：

```java
ConfigRetriever retriever = ConfigRetriever.create(vertx);
```

默认情况下，Config Retriever使用以下存储(按此顺序):

- Vert.x verticle 的 `config()`
- 系统属性
- 环境变量
- 一个 `conf/config.json` 文件。 可以使用 `vertx-config-path` 系统属性或 `VERTX_CONFIG_PATH` 环境变量覆盖此路径。

您可以配置自己的存储：

```java
ConfigStoreOptions httpStore = new ConfigStoreOptions()
  .setType("http")
  .setConfig(new JsonObject()
    .put("host", "localhost").put("port", 8080).put("path", "/conf"));

ConfigStoreOptions fileStore = new ConfigStoreOptions()
  .setType("file")
  .setConfig(new JsonObject().put("path", "my-config.json"));

ConfigStoreOptions sysPropsStore = new ConfigStoreOptions().setType("sys");


ConfigRetrieverOptions options = new ConfigRetrieverOptions()
  .addStore(httpStore).addStore(fileStore).addStore(sysPropsStore);

ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
```

下面提供了有关重载规则和可用存储的更多详细信息。 每个存储都可以标记为`optional`。 如果在从可选存储中检索（或处理）配置时发现失败，则会记录失败，但处理不会失败。 而是返回一个空的 JSON 对象 (`{}`)。 要将存储标记为可选，请使用 `optional` 属性：

```java
ConfigStoreOptions fileStore = new ConfigStoreOptions()
  .setType("file")
  .setOptional(true)
  .setConfig(new JsonObject().put("path", "my-config.json"));
ConfigStoreOptions sysPropsStore = new ConfigStoreOptions().setType("sys");

ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(fileStore).addStore(sysPropsStore);

ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
```

一旦你有了 Config Retriever 的实例，*retrieve* 配置如下：

```java
retriever.getConfig(ar -> {
  if (ar.failed()) {
    // Failed to retrieve the configuration
  } else {
    JsonObject config = ar.result();
  }
});
```

## 重载规则

配置存储的声明顺序很重要，因为它定义了重载。 对于冲突键，到达 *last* 的配置存储会重载先前配置存储提供的值。 让我们举个例子。 我们有 2 个配置存储：

- `A` 提供 `{a:value, b:1}`
- `B` 提供 `{a:value2, c:2}`

按此顺序 (A, B) 声明，生成的配置将是：`{a:value2, b:1, c:2}`。

如果你以相反的顺序（B，A）声明它们，你会得到：`{a:value, b:1, c:2}`。

## 使用获取配置

获取配置允许：

- 配置 verticles，
- 配置端口、客户端、位置等，
- 配置 Vert.x 本身

本节给出了一些使用示例。

### 配置单个 verticle

下面的例子可以放在一个verticle的`start`方法中。 它检索配置（使用默认存储），并使用配置内容配置 HTTP 服务器。

```java
ConfigRetriever retriever = ConfigRetriever.create(vertx);
retriever.getConfig(json -> {
  JsonObject result = json.result();

  vertx.createHttpServer()
    .requestHandler(req -> result.getString("message"))
    .listen(result.getInteger("port"));
});
```

### 配置一组 verticles

以下示例使用 `verticles.json` 文件中包含的配置配置 2 个 Verticle：

```java
ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions()
  .addStore(new ConfigStoreOptions().setType("file").setConfig(new JsonObject().put("path", "verticles.json"))));

retriever.getConfig(json -> {
  JsonObject a = json.result().getJsonObject("a");
  JsonObject b = json.result().getJsonObject("b");
  vertx.deployVerticle(GreetingVerticle.class.getName(), new DeploymentOptions().setConfig(a));
  vertx.deployVerticle(GreetingVerticle.class.getName(), new DeploymentOptions().setConfig(b));
});
```

### 配置 Vert.x 本身

您也可以直接配置 Vert.x。 为此，您需要一个用于检索配置的临时 Vert.x 实例。 然后创建实际实例：

```java
Vertx vertx = Vertx.vertx();
// Create the config retriever
ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions()
  .addStore(new ConfigStoreOptions().setType("file").setConfig(new JsonObject().put("path", "vertx.json"))));

// Retrieve the configuration
retriever.getConfig(json -> {
  JsonObject result = json.result();
  // Close the vert.x instance, we don't need it anymore.
  vertx.close();

  // Create a new Vert.x instance using the retrieve configuration
  VertxOptions options = new VertxOptions(result);
  Vertx newVertx = Vertx.vertx(options);

  // Deploy your verticle
  newVertx.deployVerticle(GreetingVerticle.class.getName(), new DeploymentOptions().setConfig(result.getJsonObject("a")));
});
```

### 将配置更改传播到事件总线

Vert.x Config 会在配置更改时通知您。 如果你想对这个事件做出反应，你需要自己实现反应。 例如，您可以取消部署和重新部署 verticle 或在事件总线上发送新配置。 以下示例显示了后一种情况。 它在事件总线上发送新配置。 有兴趣的verticles可以监听这个地址并自行更新：

```java
ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions()
  .addStore(new ConfigStoreOptions().setType("file").setConfig(new JsonObject().put("path", "verticles.json"))));

retriever.getConfig(json -> {
  //...
});

retriever.listen(change -> {
  JsonObject json = change.getNewConfiguration();
  vertx.eventBus().publish("new-configuration", json);
});
```

## 可用的配置存储

Config Retriever 提供了一组配置存储和格式。 更多可以作为扩展使用，您也可以实现自己的。

### 配置结构

每个声明的数据存储都必须指定 `type`。 它还可以定义`format`。 如果未设置，则使用 JSON。

一些配置存储需要额外的配置（比如路径……）。 此配置使用 `setConfig` 作为 Json 对象传递

### File

此配置存储只是从文件中读取配置。 它支持所有支持的格式。

```java
ConfigStoreOptions file = new ConfigStoreOptions()
  .setType("file")
  .setFormat("properties")
  .setConfig(new JsonObject().put("path", "path-to-file.properties"));
```

`path` 配置是必需的。

### JSON

JSON 配置存储按原样提供给定的 JSON 配置。

```java
ConfigStoreOptions json = new ConfigStoreOptions()
  .setType("json")
  .setConfig(new JsonObject().put("key", "value"));
```

此配置存储唯一支持的格式是 JSON。

### 环境变量

此配置存储将环境变量转换为贡献给全局配置的 JSON 对象。

```java
ConfigStoreOptions env = new ConfigStoreOptions()
  .setType("env");
```

此配置存储不支持 `format` 配置。 默认情况下，将检索到的值转换为 JSON 兼容结构（数字、字符串、布尔值、JSON 对象和 JSON 数组）。 为避免这种转换，请配置 `raw-data` 属性：

```java
ConfigStoreOptions env = new ConfigStoreOptions()
  .setType("env")
  .setConfig(new JsonObject().put("raw-data", true));
```

您可以配置 `raw-data` 属性（默认为 `false`）。 如果 `raw-data` 为 `true`，则不会尝试转换值，您将能够使用 `config.getString(key)` 获取原始值。 它在处理大整数时很有用。

如果您想要选择要导入的键集，请使用`keys`属性。它过滤掉所有未选中的键。键必须单独列出:

```java
ConfigStoreOptions env = new ConfigStoreOptions()
  .setType("env")
  .setConfig(new JsonObject().put("keys", new JsonArray().add("SERVICE1_HOST").add("SERVICE2_HOST")));
```

### 系统属性

此配置存储将系统属性转换为贡献给全局配置的 JSON 对象。

```java
ConfigStoreOptions sys = new ConfigStoreOptions()
  .setType("sys")
  .setConfig(new JsonObject().put("cache", false));
```

此配置存储不支持 `format` 配置。

您可以配置 `cache` 属性（默认为 `true`）让您决定是否在第一次访问时缓存系统属性并且不重新加载它们。

您还可以配置 `raw-data` 属性（默认为 `false`）。 如果 `raw-data` 为 `true`，则不会尝试转换值，您将能够使用 `config.getString(key)` 获取原始值。 它在处理大整数时很有用。

此外，还有 `hierarchical` 属性（默认为 `false`）。 如果 `hierarchical` 为 `true`，系统属性将被解析为嵌套的 JSON 对象，使用点分隔的属性名称作为 JSON 对象中的路径。

例子:

```java
ConfigStoreOptions sysHierarchical = new ConfigStoreOptions()
  .setType("sys")
  .setConfig(new JsonObject().put("hierarchical", true));
java -Dserver.host=localhost -Dserver.port=8080 -jar your-application.jar
```

这会将系统属性读取为等效于的 JSON 对象

```json
{
 "server": {
   "host": "localhost",
   "port": 8080
 }
}
```

### HTTP

此配置存储从 HTTP 位置检索配置。 它可以使用任何支持的格式。

```java
ConfigStoreOptions http = new ConfigStoreOptions()
  .setType("http")
  .setConfig(new JsonObject()
    .put("host", "localhost")
    .put("port", 8080)
    .put("path", "/A"));
```

它使用存储配置创建一个 Vert.x HTTP 客户端（请参阅下一个片段）。 简化配置； 您还可以使用 `host`、`port` 和 `path` 属性配置 `host`、`port` 和 `path`。 您还可以使用 `headers` 属性配置可选的 HTTP 请求标头，使用 `timeout` 属性检索配置的超时（以毫秒为单位，默认为 3000），如果使用 `followRedirects` 属性进行重定向（默认为 false）。

```java
ConfigStoreOptions http = new ConfigStoreOptions()
  .setType("http")
  .setConfig(new JsonObject()
    .put("defaultHost", "localhost")
    .put("defaultPort", 8080)
    .put("ssl", true)
    .put("path", "/A")
    .put("headers", new JsonObject().put("Accept", "application/json")));
```

### 事件总线

此事件总线配置存储从事件总线接收配置。 此存储允许您在本地和分布式组件之间分发配置。

```java
ConfigStoreOptions eb = new ConfigStoreOptions()
  .setType("event-bus")
  .setConfig(new JsonObject()
    .put("address", "address-getting-the-conf")
  );
```

此配置存储支持任何格式。

### 目录

此配置存储类似于 `file` 配置存储，但不是读取单个文件，而是从目录中读取多个文件。

此配置存储配置需要：

- `path` - 文件所在的根目录
- 至少一个 `fileset` - 选择文件的对象
- 对于 properties 文件，您可以指示是否要使用 `raw-data` 属性禁用类型转换

每个 `fileset` 包含：

- a `pattern` : 一个 Ant 风格的文件选择模式。 该模式应用于当前工作目录中文件的相对路径。
- 一个可选的 `format` 指示文件的格式（每个文件集可以使用不同的格式，但文件集中的文件必须共享相同的格式）。

```java
ConfigStoreOptions dir = new ConfigStoreOptions()
  .setType("directory")
  .setConfig(new JsonObject().put("path", "config")
    .put("filesets", new JsonArray()
      .add(new JsonObject().put("pattern", "dir/*json"))
      .add(new JsonObject().put("pattern", "dir/*.properties")
        .put("format", "properties"))
    ));

ConfigStoreOptions dirWithRawData = new ConfigStoreOptions()
  .setType("directory")
  .setConfig(new JsonObject().put("path", "config")
    .put("filesets", new JsonArray()
      .add(new JsonObject().put("pattern", "dir/*json"))
      .add(new JsonObject().put("pattern", "dir/*.properties")
        .put("format", "properties").put("raw-data", true))
    ));
```

### 属性文件和原始数据

Vert.x Config 可以读取属性文件。 读取此类文件时，您可以传递 `raw-data` 属性以指示 Vert.x 不尝试转换值。 它在处理大整数时很有用。 可以使用 `config.getString(key)` 检索值。

```java
ConfigStoreOptions propertyWithRawData = new ConfigStoreOptions()
  .setFormat("properties")
  .setType("file")
  .setConfig(new JsonObject().put("path", "raw.properties").put("raw-data", true)
  );
```

某些属性配置本质上可能是分层的。 在读取这样的文件时，您可以传递 `hierarchical` 属性以指示 Vert.x 将配置转换为 json 对象，同时保持此层次结构，这与之前的扁平结构方法不同。

例子:

```properties
server.host=localhost
server.port=8080
multiple.values=1,2,3
```

获取值:

```java
ConfigStoreOptions propertyWithHierarchical = new ConfigStoreOptions()
  .setFormat("properties")
  .setType("file")
  .setConfig(new JsonObject().put("path", "hierarchical.properties").put("hierarchical", true)
  );
ConfigRetrieverOptions options = new ConfigRetrieverOptions()
  .addStore(propertyWithHierarchical);

ConfigRetriever configRetriever = ConfigRetriever.create(Vertx.vertx(), options);

configRetriever.configStream().handler(config -> {
  String host = config.getJsonObject("server").getString("host");
  Integer port = config.getJsonObject("server").getInteger("port");
  JsonArray multiple = config.getJsonObject("multiple").getJsonArray("values");
  for (int i = 0; i < multiple.size(); i++) {
    Integer value = multiple.getInteger(i);
  }
});
```

## 监听配置更改

Configuration Retriever会定期检索配置，如果结果与当前不同，则可以重新配置您的应用程序。 默认情况下，配置每 5 秒重新加载一次。

```java
ConfigRetrieverOptions options = new ConfigRetrieverOptions()
  .setScanPeriod(2000)
  .addStore(store1)
  .addStore(store2);

ConfigRetriever retriever = ConfigRetriever.create(Vertx.vertx(), options);
retriever.getConfig(json -> {
  // Initial retrieval of the configuration
});

retriever.listen(change -> {
  // Previous configuration
  JsonObject previous = change.getPreviousConfiguration();
  // New configuration
  JsonObject conf = change.getNewConfiguration();
});
```

## 检索最后检索到的配置

您可以使用以下命令检索最后检索到的配置，而无需“等待”检索：

```java
JsonObject last = retriever.getCachedConfig();
```

## 将配置作为流读取

`ConfigRetriever` 提供了一种访问配置流的方法。 它是 `JsonObject` 的 `ReadStream`。 通过注册正确的处理程序集，您会收到通知：

- 检索到新配置时
- 当检索配置时发生错误
- 当配置检索器关闭时（调用 `endHandler`）。

```java
ConfigRetrieverOptions options = new ConfigRetrieverOptions()
  .setScanPeriod(2000)
  .addStore(store1)
  .addStore(store2);

ConfigRetriever retriever = ConfigRetriever.create(Vertx.vertx(), options);
retriever.configStream()
  .endHandler(v -> {
    // retriever closed
  })
  .exceptionHandler(t -> {
    // an error has been caught while retrieving the configuration
  })
  .handler(conf -> {
    // the configuration
  });
```

## 处理配置

您可以配置一个可以验证和更新配置的*处理器*。 这是使用 `setConfigurationProcessor` 方法完成的。

处理器不能返回 `null`。它获取检索到的配置并返回处理过的配置。如果处理器没有更新配置，它必须返回输入配置。处理器可以抛出异常(例如用于验证目的)。

## 检索配置作为Future

`ConfigRetriever` 提供了一种将配置检索作为 `Future` 的方法：

```java
Future<JsonObject> future = retriever.getConfig();
future.onComplete(ar -> {
  if (ar.failed()) {
    // Failed to retrieve the configuration
  } else {
    JsonObject config = ar.result();
  }
});
```

## 扩展配置检索器

您可以通过实现来扩展配置：

- `ConfigProcessor` SPI 添加对格式的支持
- `ConfigStoreFactory` SPI 添加对配置存储的支持（从中检索配置数据的位置）

## 其它的格式

除了此库支持的开箱即用格式之外，Vert.x Config 还提供了您可以在应用程序中使用的其他格式。

### Hocon 配置格式

Hocon 配置格式扩展了 Vert.x 配置检索器并提供了对 HOCON(https://github.com/typesafehub/config/blob/master/HOCON.md) 格式的支持。

它支持包括，json，属性，宏…

#### 使用 Hocon 配置格式

要使用 Hocon 配置格式，请将以下依赖项添加到构建描述符的 *dependencies* 部分：

- Maven (在你的  `pom.xml`):

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-config-hocon</artifactId>
 <version>4.3.0</version>
</dependency>
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-config</artifactId>
 <version>4.3.0</version>
</dependency>
```

- Gradle (在你的 `build.gradle`):

```groovy
compile 'io.vertx:vertx-config:4.3.0'
compile 'io.vertx:vertx-config-hocon:4.3.0'
```

#### 配置存储以使用 HOCON

添加到类路径或依赖项后，您需要配置 `ConfigRetriever` 以使用此格式：

```java
ConfigStoreOptions store = new ConfigStoreOptions()
  .setType("file")
  .setFormat("hocon")
  .setConfig(new JsonObject()
    .put("path", "my-config.conf")
  );

ConfigRetriever retriever = ConfigRetriever.create(vertx,
    new ConfigRetrieverOptions().addStore(store));
```

您只需将 `format` 设置为 `hocon`。

### Yaml 配置格式

Yaml 配置格式扩展了 Vert.x 配置检索器并提供对 Yaml 配置格式格式的支持。

#### 使用 Yaml 配置格式

要使用 Yaml 配置格式，请将以下依赖项添加到构建描述符的 *dependencies* 部分：

- Maven (在你的 `pom.xml`):

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-config-yaml</artifactId>
 <version>4.3.0</version>
</dependency>
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-config</artifactId>
 <version>4.3.0</version>
</dependency>
```

- Gradle (在你的 `build.gradle`):

```groovy
compile 'io.vertx:vertx-config:4.3.0'
compile 'io.vertx:vertx-config-yaml:4.3.0'
```

#### 将存储配置为使用 YAML

添加到类路径或依赖项后，您需要配置 `ConfigRetriever` 以使用此格式：

```java
ConfigStoreOptions store = new ConfigStoreOptions()
  .setType("file")
  .setFormat("yaml")
  .setConfig(new JsonObject()
    .put("path", "my-config.yaml")
  );

ConfigRetriever retriever = ConfigRetriever.create(vertx,
    new ConfigRetrieverOptions().addStore(store));
```

您只需将 `format` 设置为 `yaml`。

## 其他的存储

除了这个库支持的开箱即用存储之外，Vert.x Config 还提供了可以在应用程序中使用的其他存储。

### Git 配置存储

Git 配置存储是 Vert.x 配置检索器的扩展，用于从 Git 存储库中检索配置。

#### 使用 Git 配置存储

要使用 Git 配置，请将以下依赖项添加到构建描述符的 *dependencies* 部分：

- Maven (在你的 `pom.xml`):

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-config-git</artifactId>
 <version>4.3.0</version>
</dependency>
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-config</artifactId>
 <version>4.3.0</version>
</dependency>
```

- Gradle (在你的 `build.gradle`):

```groovy
compile 'io.vertx:vertx-config:4.3.0'
compile 'io.vertx:vertx-config-git:4.3.0'
```

#### 配置存储

添加到类路径或依赖项后，您需要配置 `ConfigRetriever` 以使用此存储：

```java
ConfigStoreOptions git = new ConfigStoreOptions()
    .setType("git")
    .setConfig(new JsonObject()
        .put("url", "https://github.com/cescoffier/vertx-config-test.git")
        .put("path", "local")
        .put("filesets",
            new JsonArray().add(new JsonObject().put("pattern", "*.json"))));

ConfigRetriever retriever = ConfigRetriever.create(vertx,
    new ConfigRetrieverOptions().addStore(git));
```

配置要求：

- 存储库的 `url`
- 存储库被克隆的 `path`（本地目录）
- 私有存储库的`user` （默认不进行身份验证）
- 用户的`password`
- 私有存储库的 `idRsaKeyPath` 需要 ssh uri
- 至少 `fileset` 指示要读取的文件集（与目录配置存储相同的行为）。

您还可以配置要使用的`branch`（默认为`master`）和`remote`存储库的名称（默认为`origin`）。

#### 它是如何工作的

如果本地 `path` 不存在，配置存储会将存储库克隆到此目录中。 然后它读取与不同文件集匹配的文件。

如果本地`path`存在，它会尝试更新它（如果需要，它会切换分支））。 如果更新失败，则配置检索失败。

定期更新存储库以检查配置是否已更新。

### Kubernetes ConfigMap 存储

Kubernetes ConfigMap 存储 扩展了 Vert.x 配置检索器并提供支持 Kubernetes Config Map 和 Secrets。 因此，通过读取配置map或secrets来检索配置。

#### 使用 Kubernetes ConfigMap 存储

要使用 Kubernetes ConfigMap 存储，请将以下依赖项添加到构建描述符的 *dependencies* 部分：

- Maven (在你的 `pom.xml`):

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-config-kubernetes-configmap</artifactId>
 <version>4.3.0</version>
</dependency>
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-config</artifactId>
 <version>4.3.0</version>
</dependency>
```

- Gradle (在你的 `build.gradle`):

```groovy
compile 'io.vertx:vertx-config:4.3.0'
compile 'io.vertx:vertx-config-kubernetes-configmap:4.3.0'
```

#### 配置存储

添加到类路径或依赖项后，您需要配置 `ConfigRetriever` 以使用此存储：

```java
ConfigStoreOptions store = new ConfigStoreOptions()
    .setType("configmap")
    .setConfig(new JsonObject()
        .put("namespace", "my-project-namespace")
        .put("name", "configmap-name")
    );

ConfigRetriever retriever = ConfigRetriever.create(vertx,
    new ConfigRetrieverOptions().addStore(store));
```

您需要配置存储以找到正确的 configmap。 这是通过以下方式完成的：

- `namespace` - 项目命名空间，默认为 `default`。 如果设置了 `KUBERNETES_NAMESPACE` ENV 变量，它将使用此值。
- `name` - 配置map的名称
- `optional` - 配置map是否是可选的（默认为`true`）

如果配置map由多个元素组成，您可以使用 `key` 参数来判断读取哪个 `key`

应用程序必须具有读取配置map的权限。

要从secret中读取数据，只需将 `secret` 属性配置为 `true`：

```java
ConfigStoreOptions store = new ConfigStoreOptions()
    .setType("configmap")
    .setConfig(new JsonObject()
        .put("namespace", "my-project-namespace")
        .put("name", "my-secret")
        .put("secret", true)
    );

ConfigRetriever retriever = ConfigRetriever.create(vertx,
    new ConfigRetrieverOptions().addStore(store));
```

如果配置map不可用，则将一个空的 JSON 对象作为配置块传递。 要禁用此行为并显式失败，您可以将 `optional` 配置设置为 `false`。

### Redis 配置存储

Redis 配置存储扩展了 Vert.x 配置检索器，并提供了从 Redis 服务器检索配置的方法。

#### 使用 Redis 配置存储

要使用 Redis 配置存储，请将以下依赖项添加到构建描述符的 *dependencies* 部分：

- Maven (在你的 `pom.xml`):

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-config-redis</artifactId>
 <version>4.3.0</version>
</dependency>
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-config</artifactId>
 <version>4.3.0</version>
</dependency>
```

- Gradle (在你的 `build.gradle`):

```groovy
compile 'io.vertx:vertx-config:4.3.0'
compile 'io.vertx:vertx-config-redis:4.3.0'
```

#### 配置存储

添加到类路径或依赖项后，您需要配置 `ConfigRetriever` 以使用此存储：

```java
ConfigStoreOptions store = new ConfigStoreOptions()
    .setType("redis")
    .setConfig(new JsonObject()
        .put("host", "localhost")
        .put("port", 6379)
        .put("key", "my-configuration")
    );

ConfigRetriever retriever = ConfigRetriever.create(vertx,
    new ConfigRetrieverOptions().addStore(store));
```

存储配置用于创建 `Redis` 的实例。 查看 Vert.x Redis 客户端的文档以获取更多详细信息。

此外，您可以设置 `key` 指示存储配置的 *field* 存储位置。 默认使用`configuration`。

创建的 Redis 客户端使用 `HGETALL` 配置检索配置。

### Zookeeper 配置存储

Zookeeper 配置存储扩展了 Vert.x 配置检索器，并提供了从 Zookeeper 服务器检索配置的方法。 它使用 Apache Curator 作为客户端。

#### 使用 Zookeeper 配置存储

要使用 Zookeeper 配置存储，请将以下依赖项添加到构建描述符的 *dependencies* 部分：

- Maven (在你的 `pom.xml`):

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-config-zookeeper</artifactId>
 <version>4.3.0</version>
</dependency>
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-config</artifactId>
 <version>4.3.0</version>
</dependency>
```

- Gradle (在你的 `build.gradle`):

```groovy
compile 'io.vertx:vertx-config:4.3.0'
compile 'io.vertx:vertx-config-zookeeper:4.3.0'
```

#### 配置存储

添加到类路径或依赖项后，您需要配置 `ConfigRetriever` 以使用此存储：

```java
ConfigStoreOptions store = new ConfigStoreOptions()
    .setType("zookeeper")
    .setConfig(new JsonObject()
        .put("connection", "localhost:2181")
        .put("path", "/path/to/my/conf")
    );

ConfigRetriever retriever = ConfigRetriever.create(vertx,
    new ConfigRetrieverOptions().addStore(store));
```

存储配置用于配置 Apache Curator 客户端和包含该配置的 Zookeeper 节点的 *path*。 请注意，配置的格式可以是 JSON，也可以是任何受支持的格式。

配置需要 `connection` 属性指示 Zookeeper 服务器的连接 *字符串*，以及 `path` 属性指示包含配置的节点的路径。

此外，您还可以配置：

- `maxRetries`：连接尝试次数，默认为 3
- `baseSleepTimeBetweenRetries`：重试之间等待的毫秒数（指数退避策略）。 默认为 1000 毫秒。

### Consul 配置存储

Consul 配置存储扩展了 Vert.x 配置检索器，并提供了从 [Consul](https://www.consul.io/) 检索配置的方法。

#### 使用 Consul 配置存储

要使用 Consul 配置存储，请将以下依赖项添加到构建描述符的 *dependencies* 部分：

- Maven (在你的 `pom.xml`):

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-config-consul</artifactId>
 <version>4.3.0</version>
</dependency>
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-config</artifactId>
 <version>4.3.0</version>
</dependency>
```

- Gradle (在你的 `build.gradle`):

```groovy
compile 'io.vertx:vertx-config:4.3.0'
compile 'io.vertx:vertx-config-consul:4.3.0'
```

#### 配置存储

添加到类路径或依赖项后，您需要配置 `ConfigRetriever` 以使用此存储：

```java
ConfigStoreOptions store = new ConfigStoreOptions()
    .setType("consul")
    .setConfig(new JsonObject()
      .put("host", "localhost")
      .put("port", 8500)
      .put("prefix", "foo")
      .put("raw-data", false)
    );

ConfigRetriever retriever = ConfigRetriever.create(vertx,
    new ConfigRetrieverOptions().addStore(store));
```

存储配置用于创建 `ConsulClient` 的实例。 查看 Vert.x Consul Client 的文档以获取更多详细信息。 这是特定于 Consul 配置存储的参数：

- `prefix`

  构建配置树时不会考虑的前缀。 默认为空。

- `delimiter`

  用于拆分 Consul 存储中的键以获得配置树中的级别的符号。 默认为“/”。

- `raw-data`

  如果 `raw-data` 为 `true`，则不会尝试转换值，并且您将能够使用 `config.getString(key)` 获取原始值。 默认是`true`。

### Spring 配置服务器存储

Spring Config Server Store 扩展了 Vert.x 配置检索器，并提供了一种从 Spring Server 检索配置的方法。

#### 使用 Spring  配置服务器存储

要使用 Spring 配置服务器存储，请将以下依赖项添加到构建描述符的 *dependencies* 部分：

- Maven (在你的 `pom.xml`):

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-config-spring-config-server</artifactId>
 <version>4.3.0</version>
</dependency>
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-config</artifactId>
 <version>4.3.0</version>
</dependency>
```

- Gradle (在你的 `build.gradle`):

```groovy
compile 'io.vertx:vertx-config:4.3.0'
compile 'io.vertx:vertx-config-spring-config-server:4.3.0'
```

#### 配置存储

添加到类路径或依赖项后，您需要配置 `ConfigRetriever` 以使用此存储：

```java
ConfigStoreOptions store = new ConfigStoreOptions()
    .setType("spring-config-server")
    .setConfig(new JsonObject().put("url", "http://localhost:8888/foo/development"));

ConfigRetriever retriever = ConfigRetriever.create(vertx,
    new ConfigRetrieverOptions().addStore(store));
```

可配置的属性有：

- `url` - 获取配置的`url`（强制），支持两种格式：
  - `/{application}/{environment}` 使用分离的 propertySources 产生响应
  - `/{application}-{environment}.json` 以 JSON 格式生成响应，具有唯一字段和解析的 Spring 占位符
- `timeout` - 检索配置的超时时间（以毫秒为单位），默认为 3000
- `user` - `用户名`（默认没有认证）
- `password` - `密码`
- `httpClientConfiguration` - 底层HTTP客户端的配置

### Vault 配置存储

Vault Store 扩展了 Vert.x 配置检索器并为 Vault (https://www.vaultproject.io/) 提供支持。 因此，从 Vault 中检索配置（秘密）。

> 此存储支持的secrets 引擎是 Vault Key/Value 版本 1 和版本 2 引擎 (https://www.vaultproject.io/docs/secrets/kv/index.html)。 不支持其他secrets 引擎。

#### 使用 Vault 配置存储

要使用 Vault 配置存储，请将以下依赖项添加到构建描述符的 *dependencies* 部分：

- Maven (在你的 `pom.xml`):

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-config-vault</artifactId>
 <version>4.3.0</version>
</dependency>
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-config</artifactId>
 <version>4.3.0</version>
</dependency>
```

- Gradle (在你的 `build.gradle`):

```groovy
compile 'io.vertx:vertx-config:4.3.0'
compile 'io.vertx:vertx-config-vault:4.3.0'
```

#### 配置存储

添加到类路径或依赖项后，您需要配置 `ConfigRetriever` 以使用此存储：

```java
ConfigStoreOptions store = new ConfigStoreOptions()
  .setType("vault")
  .setConfig(config);

ConfigRetriever retriever = ConfigRetriever.create(vertx,
  new ConfigRetrieverOptions().addStore(store));
```

要使用 Vault 配置存储，请将 `type` 设置为 `vault`。 配置以 Json 形式提供。 它配置对 Vault 的访问、身份验证和要检索的密钥的路径：

```java
JsonObject vault_config = new JsonObject()
  .put("host", "127.0.0.1") // The host name
  .put("port", 8200) // The port
  .put("ssl", true); // Whether or not SSL is used (disabled by default)

// Certificates
PemKeyCertOptions certs = new PemKeyCertOptions()
  .addCertPath("target/vault/config/ssl/client-cert.pem")
  .addKeyPath("target/vault/config/ssl/client-privatekey.pem");
vault_config.put("pemKeyCertOptions", certs.toJson());

// Truststore
JksOptions jks = new JksOptions()
  .setPath("target/vault/config/ssl/truststore.jks");
vault_config.put("trustStoreOptions", jks.toJson());

// Path to the secret to read.
vault_config.put("path", "secret/my-secret");

ConfigStoreOptions store = new ConfigStoreOptions()
  .setType("vault")
  .setConfig(vault_config);

ConfigRetriever retriever = ConfigRetriever.create(vertx,
  new ConfigRetrieverOptions().addStore(store));}
```

`vault_config` 对象可以包含 HTTP 客户端/Web 客户端配置，例如信任存储、超时、证书、端口和主机。 `path` 和 `host` 条目是强制性的。 `path` 表示要检索的stores`host` 是 Vault 服务器的主机名。 默认情况下使用端口 8200。 默认情况下禁用 SSL，但您应该为生产设置启用它。

然后，您需要使用以下方法之一来配置要使用的令牌或身份验证机制。

#### 使用现有令牌

如果您知道要使用的令牌，请在配置中设置 `token` 条目：

```java
JsonObject vault_config = new JsonObject();

// ...

// Path to the secret to read.
vault_config.put("path", "secret/my-secret");

// The token
vault_config.put("token", token);

ConfigStoreOptions store = new ConfigStoreOptions()
  .setType("vault")
  .setConfig(vault_config);

ConfigRetriever retriever = ConfigRetriever.create(vertx,
  new ConfigRetrieverOptions().addStore(store));
```

您可以使用根令牌，但不建议这样做。 当令牌被撤销时，对secret 的访问被阻止。 如果令牌是可更新的，则令牌在到期时会被更新。

#### 生成令牌

如果你有一个令牌允许你生成新的令牌，你可以请求生成令牌:

```java
JsonObject vault_config = new JsonObject();

// ...

// Path to the secret to read.
vault_config.put("path", "secret/my-secret");

// Configure the token generation

// Configure the token request (https://www.vaultproject.io/docs/auth/token.html)
JsonObject tokenRequest = new JsonObject()
  .put("ttl", "1h")
  .put("noDefault", true)

  // The token to use to request the generation (parts of the tokenRequest object)
  .put("token", token);

vault_config.put("auth-backend", "token") // Indicate the auth backend to use
  .put("renew-window", 5000L) // Renew error margin in ms
  .put("token-request", tokenRequest); // Pass the token generation configuration

ConfigStoreOptions store = new ConfigStoreOptions()
  .setType("vault")
  .setConfig(vault_config);

ConfigRetriever retriever = ConfigRetriever.create(vertx,
  new ConfigRetrieverOptions().addStore(store));
```

使用此方法时，根配置中必须不提供令牌，用于请求生成的令牌在嵌套 JSON 结构中传递。 如果生成的令牌是可更新的，它将在到期时自动更新。 `renew-window` 是添加到令牌有效性以更新它的时间窗口。 如果生成的令牌被撤销，则对secret的访问被阻止。

#### 使用证书

您可以使用 TLS 证书作为身份验证机制。 所以，你不需要知道令牌，令牌是自动生成的。

```java
JsonObject vault_config = new JsonObject();

// ...

PemKeyCertOptions certs = new PemKeyCertOptions()
  .addCertPath("target/vault/config/ssl/client-cert.pem")
  .addKeyPath("target/vault/config/ssl/client-privatekey.pem");
vault_config.put("pemKeyCertOptions", certs.toJson());

PemTrustOptions trust = new PemTrustOptions()
  .addCertPath("target/vault/config/ssl/cert.pem");
vault_config.put("pemTrustStoreOptions", trust.toJson());

JksOptions jks = new JksOptions()
  .setPath("target/vault/config/ssl/truststore.jks");
vault_config.put("trustStoreOptions", jks.toJson());

vault_config.put("auth-backend", "cert");

// Path to the secret to read.
vault_config.put("path", "secret/my-secret");

ConfigStoreOptions store = new ConfigStoreOptions()
  .setType("vault")
  .setConfig(vault_config);

ConfigRetriever retriever = ConfigRetriever.create(vertx,
  new ConfigRetrieverOptions().addStore(store));
```

检查 HTTP 客户端和 Web 客户端配置以传递证书。 如果生成的令牌是可更新的，它将被更新。 如果不是，则商店尝试再次进行身份验证。

#### 使用 AppRole

当 Vault 知道您的应用程序并且您拥有 `appRoleId` 和 `secretId` 时，将使用 `AppRole`。 您不需要令牌，令牌会自动生成：

```java
JsonObject vault_config = new JsonObject();

// ...

vault_config
  .put("auth-backend", "approle") // Set the auth-backend to approle
  .put("approle", new JsonObject()  // Configure the role id and secret it
    .put("role-id", appRoleId).put("secret-id", secretId)
  );

// Path to the secret to read.
vault_config.put("path", "secret/my-secret");

ConfigStoreOptions store = new ConfigStoreOptions()
  .setType("vault")
  .setConfig(vault_config);

ConfigRetriever retriever = ConfigRetriever.create(vertx,
  new ConfigRetrieverOptions().addStore(store));
```

If the generated token is renewable, it will be renewed. If not, the store attempts to authenticate again.

#### 使用 用户名和密码

当用户/应用程序使用用户名/密码进行身份验证时，将使用 `userpass` 身份验证后端。 您不需要令牌，因为令牌是在身份验证过程中生成的：

```java
JsonObject vault_config = new JsonObject();

// ...

vault_config
  .put("auth-backend", "userpass") // Set the auth-backend to userpass
  .put("user-credentials", new JsonObject()
    .put("username", username).put("password", password)
  );

// Path to the secret to read.
vault_config.put("path", "secret/my-secret");

ConfigStoreOptions store = new ConfigStoreOptions()
  .setType("vault")
  .setConfig(vault_config);

ConfigRetriever retriever = ConfigRetriever.create(vertx,
  new ConfigRetrieverOptions().addStore(store));
```

如果生成的令牌是可更新的，它将被更新。 如果不是，则商店尝试再次进行身份验证。

