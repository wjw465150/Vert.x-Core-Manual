# SQL Client Templates中文版

> 翻译: 白石(https://github.com/wjw465150/Vert.x-Core-Manual)

SQL Client Templates 是一个小型库，旨在促进 SQL 查询的执行。

## 用法

要使用 SQL 客户端模板，请将以下依赖项添加到构建描述符的 *dependencies* 部分：

- Maven (在你的 `pom.xml`):

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-sql-client-templates</artifactId>
 <version>4.2.6</version>
</dependency>
```

- Gradle (在你的 `build.gradle`):

```groovy
dependencies {
 implementation 'io.vertx:vertx-sql-client-templates:4.2.6'
}
```

## 入门

这是使用 SQL 模板的最简单方法。

SQL 模板使用 *named* 参数，因此（默认情况下）将map作为参数源而不是元组。

SQL 模板生成（默认情况下）一个 `RowSet<Row>`，就像客户端`PreparedQuery`。 事实上，模板是 `PreparedQuery` 的一个瘦包装器。

```java
Map<String, Object> parameters = Collections.singletonMap("id", 1);

SqlTemplate
  .forQuery(client, "SELECT * FROM users WHERE id=#{id}")
  .execute(parameters)
  .onSuccess(users -> {
    users.forEach(row -> {
      System.out.println(row.getString("first_name") + " " + row.getString("last_name"));
    });
  });
```

当您需要执行插入或更新操作并且您不关心结果时，可以使用 `SqlTemplate.forUpdate` 代替：

```java
Map<String, Object> parameters = new HashMap<>();
parameters.put("id", 1);
parameters.put("firstName", "Dale");
parameters.put("lastName", "Cooper");

SqlTemplate
  .forUpdate(client, "INSERT INTO users VALUES (#{id},#{firstName},#{lastName})")
  .execute(parameters)
  .onSuccess(v -> {
    System.out.println("Successful update");
  });
```

## 模板语法

模板语法使用 `#{XXX}` 语法，其中 `XXX` 是有效的 [java 标识符](https://docs.oracle.com/javase/specs/jls/se8/html/jls-3.html#jls-3.8) 字符串（没有关键字限制）。

您可以使用反斜杠字符 `\` 转义任何 ` 字符，即 `\{foo}` 将被解释为没有 `foo` 参数的 `#{foo}` 字符串。

## 行映射

默认情况下，模板生成 `Row` 作为结果类型。

您可以提供自定义的 `RowMapper` 来实现行级映射：

```java
RowMapper<User> ROW_USER_MAPPER = row -> {
  User user = new User();
  user.id = row.getInteger("id");
  user.firstName = row.getString("firstName");
  user.lastName = row.getString("lastName");
  return user;
};
```

改为实现行级映射：

```java
SqlTemplate
  .forQuery(client, "SELECT * FROM users WHERE id=#{id}")
  .mapTo(ROW_USER_MAPPER)
  .execute(Collections.singletonMap("id", 1))
  .onSuccess(users -> {
    users.forEach(user -> {
      System.out.println(user.firstName + " " + user.lastName);
    });
  });
```

## 贫血的 JSON 行映射

贫血的 JSON 行映射是模板行列和 JSON 对象之间的简单映射，使用 `toJson`

```java
SqlTemplate
  .forQuery(client, "SELECT * FROM users WHERE id=#{id}")
  .mapTo(Row::toJson)
  .execute(Collections.singletonMap("id", 1))
  .onSuccess(users -> {
    users.forEach(user -> {
      System.out.println(user.encode());
    });
  });
```

## 参数映射

模板使用 `Map<String, Object>` 作为默认输入。

您可以提供自定义映射器：

```java
TupleMapper<User> PARAMETERS_USER_MAPPER = TupleMapper.mapper(user -> {
  Map<String, Object> parameters = new HashMap<>();
  parameters.put("id", user.id);
  parameters.put("firstName", user.firstName);
  parameters.put("lastName", user.lastName);
  return parameters;
});
```

来实现参数映射：

```java
User user = new User();
user.id = 1;
user.firstName = "Dale";
user.firstName = "Cooper";

SqlTemplate
  .forUpdate(client, "INSERT INTO users VALUES (#{id},#{firstName},#{lastName})")
  .mapFrom(PARAMETERS_USER_MAPPER)
  .execute(user)
  .onSuccess(res -> {
    System.out.println("User inserted");
  });
```

您还可以轻松地执行批处理：

```java
SqlTemplate
  .forUpdate(client, "INSERT INTO users VALUES (#{id},#{firstName},#{lastName})")
  .mapFrom(PARAMETERS_USER_MAPPER)
  .executeBatch(users)
  .onSuccess(res -> {
    System.out.println("Users inserted");
  });
```

## 贫血的JSON参数映射

贫血的JSON参数映射是模板参数和JSON对象之间的简单映射:

```java
JsonObject user = new JsonObject();
user.put("id", 1);
user.put("firstName", "Dale");
user.put("lastName", "Cooper");

SqlTemplate
  .forUpdate(client, "INSERT INTO users VALUES (#{id},#{firstName},#{lastName})")
  .mapFrom(TupleMapper.jsonObject())
  .execute(user)
  .onSuccess(res -> {
    System.out.println("User inserted");
  });
```

## 使用 Jackson 数据绑定进行映射

您可以使用 Jackson 数据绑定功能进行映射。

您需要将 Jackson 数据绑定依赖项添加到构建描述符的 *dependencies* 部分：

- Maven (在你的 `pom.xml`):

```xml
<dependency>
 <groupId>com.fasterxml.jackson.core</groupId>
 <artifactId>jackson-databind</artifactId>
 <version>${jackson.version}</version>
</dependency>
```

- Gradle (在你的 `build.gradle`):

```groovy
dependencies {
 compile 'com.fasterxml.jackson.core:jackson-databind:${jackson.version}'
}
```

行映射是通过使用行键/值对创建一个`JsonObject`，然后调用`mapTo`将其映射到任何带有 Jackson 数据绑定的 Java 类来实现的。

```java
SqlTemplate
  .forQuery(client, "SELECT * FROM users WHERE id=#{id}")
  .mapTo(User.class)
  .execute(Collections.singletonMap("id", 1))
  .onSuccess(users -> {
    users.forEach(user -> {
      System.out.println(user.firstName + " " + user.lastName);
    });
  });
```

同样，参数映射是通过使用`JsonObject.mapFrom`将对象映射到`JsonObject`，然后使用键/值对生成模板参数来实现的。

```java
User u = new User();
u.id = 1;

SqlTemplate
  .forUpdate(client, "INSERT INTO users VALUES (#{id},#{firstName},#{lastName})")
  .mapFrom(User.class)
  .execute(u)
  .onSuccess(res -> {
    System.out.println("User inserted");
  });
```

### Java Date/Time API 映射

您可以使用 *jackson-modules-java8* Jackson 扩展映射 `java.time` 类型。

您需要将 Jackson JSR 310 数据类型依赖项添加到构建描述符的 *dependencies* 部分：

- Maven (在你的 `pom.xml`):

```xml
<dependency>
 <groupId>com.fasterxml.jackson.datatype</groupId>
 <artifactId>jackson-datatype-jsr310</artifactId>
 <version>${jackson.version}</version>
</dependency>
```

- Gradle (在你的 `build.gradle`):

```groovy
dependencies {
 compile 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${jackson.version}'
}
```

然后你需要将时间模块注册到 Jackson `ObjectMapper`：

```java
ObjectMapper mapper = io.vertx.core.json.jackson.DatabindCodec.mapper();

mapper.registerModule(new JavaTimeModule());
```

您现在可以使用 `java.time`包下的类型，例如 `LocalDateTime`：

```java
public class LocalDateTimePojo {

 public LocalDateTime localDateTime;

}
```

## 使用 Vert.x 数据对象进行映射

SQL 客户端模板组件可以为 Vert.x 数据对象生成映射函数。

Vert.x 数据对象是一个简单的 Java bean 类，使用 `@DataObject` 进行注解。

```java
@DataObject
class UserDataObject {

  private long id;
  private String firstName;
  private String lastName;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }
}
```

### 代码生成

由`@RowMapped` 或`@ParametersMapped` 注释的任何数据对象都将触发相应映射器类的生成。

*codegen* 注解处理器在编译时生成这些类。 它是 Java 编译器的一项功能，因此*不需要额外的步骤*，只需正确配置您的构建：

只需将 `io.vertx:vertx-codegen:processor` 和 `io.vertx:vertx-sql-client-templates` 依赖项添加到您的构建中。

Here a configuration example for Maven:

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-codegen</artifactId>
 <version>4.2.6</version>
 <classifier>processor</classifier>
</dependency>
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-sql-client-templates</artifactId>
 <version>4.2.6</version>
</dependency>
```

这个特性也可以在 Gradle 中使用：

```
annotationProcessor "io.vertx:vertx-codegen:4.2.6:processor"
compile "io.vertx:vertx-sql-client-templates:4.2.6"
```

IDE 通常为注解处理器提供支持。

代码生成 `processor` 分类器通过 `META-INF/services` 插件机制将服务代理注解处理器的自动配置添加到 jar 中。

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

### 行映射

您可以通过使用`@RowMapped`注解数据对象来生成行映射器。

```java
@DataObject
@RowMapped
class UserDataObject {

  private long id;
  private String firstName;
  private String lastName;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }
}
```

默认情况下，每个列名都绑定在数据对象属性之后，例如 `userName` 属性绑定到 `userName` 列。

借助 `@Column` 注解，您可以使用自定义名称。

```java
@DataObject
@RowMapped
class UserDataObject {

  private long id;
  @Column(name = "first_name")
  private String firstName;
  @Column(name = "last_name")
  private String lastName;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }
}
```

您可以对字段、getter 或 setter 进行注解。

生成的映射器可用于执行行映射，如 [行映射章节](https://vertx.io/docs/vertx-sql-client-templates/java/#row_mapping_with_custom_mapper) 中所述。

```java
SqlTemplate
  .forQuery(client, "SELECT * FROM users WHERE id=#{id}")
  .mapTo(UserDataObjectRowMapper.INSTANCE)
  .execute(Collections.singletonMap("id", 1))
  .onSuccess(users -> {
    users.forEach(user -> {
      System.out.println(user.getFirstName() + " " + user.getLastName());
    });
  });
```

### 参数映射

您可以通过使用`@ParametersMapped`注解您的数据对象来生成参数映射器。

```java
@DataObject
@ParametersMapped
class UserDataObject {

  private long id;
  private String firstName;
  private String lastName;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }
}
```

默认情况下，每个参数都绑定在数据对象属性之后，例如 `userName` 属性绑定到 `userName` 参数。

借助 `@TemplateParameter` 注解，您可以使用自定义名称。

```java
@DataObject
@ParametersMapped
class UserDataObject {

  private long id;
  @TemplateParameter(name = "first_name")
  private String firstName;
  @TemplateParameter(name = "last_name")
  private String lastName;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }
}
```

您可以对字段、getter 或 setter 进行注解。

生成的映射器可用于执行参数映射，如 [参数映射章节](https://vertx.io/docs/vertx-sql-client-templates/java/#params_mapping_with_custom_function) 中所述。

```java
UserDataObject user = new UserDataObject().setId(1);

SqlTemplate
  .forQuery(client, "SELECT * FROM users WHERE id=#{id}")
  .mapFrom(UserDataObjectParamMapper.INSTANCE)
  .execute(user)
  .onSuccess(users -> {
    users.forEach(row -> {
      System.out.println(row.getString("firstName") + " " + row.getString("lastName"));
    });
  });
```

### Java 枚举类型映射

您可以在客户端支持时映射 Java 枚举类型（例如 Reactive PostgreSQL 客户端）。

通常 Java 枚举类型映射到字符串 / 数字和可能的自定义数据库枚举类型。

### 命名格式

默认模板对参数和列使用相同的大小写。 您可以覆盖 `Column` 和 `TemplateParameter` 注解中的默认名称，并使用您喜欢的格式。

你也可以在 `RowMapped` 和 `ParametersMapped` 注解中配置映射器的特定格式化情况:

```java
@DataObject
@RowMapped(formatter = SnakeCase.class)
@ParametersMapped(formatter = QualifiedCase.class)
class UserDataObject {
  // ...
}
```

可以使用以下情况：

- `CamelCase` : `FirstName`
- `LowerCamelCase` : `firstName` - 像骆驼大小写，但以小写字母开头，这是默认大小写
- `SnakeCase` : `first_name`
- `KebabCase` : `first-name`
- `QualifiedCase` : `first.name`
