# JWT 认证与授权

该组件包含了一个现成的 JWT 实现，要使用这个项目， 将下面的依赖添加到构建描述符里的 *dependencies* 部分

- Maven（在您的 `pom.xml`）：

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-auth-jwt</artifactId>
 <version>4.3.5</version>
</dependency>
```

- Gradle（在您的 `build.gradle` 文件）：

```groovy
compile 'io.vertx:vertx-auth-jwt:4.3.5'
```

JSON Web 令牌是一种简单的以明文（通常是URL）发送信息的方法， 其内容可被验证为是可信的。 像下面的这些场景 JWT 是非常适用的：

- 在单点登录方案中，需要一个单独的身份验证服务器， 然后该服务器可以以被信任的方式发送用户信息。
- 无状态的API 服务，非常适合单页应用。
- 等等…

在决定使用 JWT 之前, 需要重点注意的是 JWT 并不加密 payload, 而是对它签名。 您不应该使用 JWT 发送任何机密信息，相反应该发送的是非私密的，但需要被验证的信息。 举个例子，使用 JWT 发送一个签名过的用户 id 来表明这个用户已经登录了的做法非常棒！ 相反发送一个用户的密码的做法则是非常非常错误的。

JWT 主要的优点有：

- 它允许您验证令牌的真实性。
- 它有一个 JSON 结构，可以包含任何您所需的任意数量的数据。
- 它是无状态的。

------

**作者注解:** JWT令牌有三部分:

- Header
  包含用于对令牌签名的算法，用于声明类型`typ`和加密算法`alg`,该内容使用base64加密

- Body

  主要为json数据，该数据经过Base64URl编码，包含声明

  - 标准声明（建议使用）
    jti: jwt的唯一身份标识，主要用来作为一次性token,从而回避重放攻击。
    iss（issuer）：JWT 签发方。
    iat（issued at time）：JWT 签发时间。
    sub（subject）：JWT 主题。
    aud（audience）：JWT 接收方。
    exp（expiration time）：JWT 的过期时间。是一个数字的组合，这是因为这个字符串使用的是 Unix 的时间
    nbf（not before time）：JWT 生效时间，早于该定义的时间的 JWT 不能被接受处理。
    jti（JWT ID）：JWT 唯一标识。
    
  - 公有声明
    该部分可添加任何信息
  - 私有声明
    客户端与服务端共同定义的声明

- Signature
  根据算法，签名包含使用私钥签名的正文签名

------

您可使用 `JWTAuth` 创建一个验证器的实例。 并指定一个格式为 JSON 的配置文件。

这是创建一个 JWT 身份验证器的示例代码：

```java
JWTAuthOptions config = new JWTAuthOptions()
  .setKeyStore(new KeyStoreOptions()
    .setPath("keystore.jceks")
    .setPassword("secret"));

AuthenticationProvider provider = JWTAuth.create(vertx, config);
```

JWT 用法的典型流程是，您的应用程序中有一个接口负责颁发令牌，这个接口应在 SSL 模式下运行， 接口在通过用户名和密码验证完请求用户之后， 您应当这样做：

```java
JWTAuthOptions config = new JWTAuthOptions()
  .setKeyStore(new KeyStoreOptions()
    .setPath("keystore.jceks")
    .setPassword("secret"));

JWTAuth provider = JWTAuth.create(vertx, config);

// 验证用户的用户名和密码之后
// 通过端点生成签名令牌
if ("paulo".equals(username) && "super_secret".equals(password)) {
  String token = provider.generateToken(
    new JsonObject().put("sub", "paulo"), new JWTOptions());

  // 现在，对于任何对受保护资源的请求，您应该
  // 检查他们的HTTP头中Authorization字符串：
  // Authorization: Bearer <token>
}
```

### 加载秘钥

秘钥可以通过三种不同的方式载入:

- 使用 secrets（对称秘钥）
- 使用 OpenSSL 生成的 `pem` 格式文件（公钥）
- 使用 Java Keystore 文件（对称加密公钥）

#### 使用对称秘钥

JWT的默认签名方法称为 `HS256`。 `HS` 默认表示为 `HMAC 加密 使用 SHA256`。

这便是最简单的加载秘钥方式。您只需要将 secret 与第三方共享，举个例子 假设 secret 是：`keyboard cat` 那么您可将 Auth 配置为：

```java
JWTAuth provider = JWTAuth.create(vertx, new JWTAuthOptions()
  .addPubSecKey(new PubSecKeyOptions()
    .setAlgorithm("HS256")
    .setBuffer("keyboard cat")));

String token = provider.generateToken(new JsonObject());
```

在这种情况下 secret 将被设置为公钥，因为这是双方都知道的令牌， 您可配置 PubSec 密钥为对称的。

#### 使用 RSA 秘钥

这部分不是 OpenSSL 文档，建议阅读 OpenSSL 文档了解命令的使用。 我们将介绍如何生成最通用的密钥以及如何与 JWT auth 一起使用。

想象一下，您想使用非常常见的 `RS256` 加密算法来保护您的资源。 与您想象的相反， 256 不是秘钥长度而是哈希算法的签名长度。 任何 RSA 秘钥都可以和 JWT 加密算法一期使用。 这是信息表：

| "alg" 参数值 | 数字签名算法                        |
| ------------ | ----------------------------------- |
| *RS256*      | **RSASSA-PKCS1-v1_5 using SHA-256** |
| *RS384*      | **RSASSA-PKCS1-v1_5 using SHA-384** |
| *RS512*      | **RSASSA-PKCS1-v1_5 using SHA-512** |

如果您想生成一个2048位的 RSA 密钥对，那么您应该 （请记住 **不要** 添加密码， 否则 JWT auth 将无法载入秘钥文件）：

```shell
openssl genrsa -out private.pem 2048
```

如您看到类似的文件内容，那么恭喜您，秘钥文件正确的生成了：

```
-----BEGIN RSA PRIVATE KEY-----
MIIEowIBAAKCAQEAxPSbCQY5mBKFDIn1kggvWb4ChjrctqD4nFnJOJk4mpuZ/u3h
...
e4k0yN3F1J1DVlqYWJxaIMzxavQsi9Hz4p2JgyaZMDGB6kGixkMo
-----END RSA PRIVATE KEY-----
```

标准的 JDK 是无法读取该文件的，所以我们 **必须** 将其转换成 PKCS8 标准格式：

```
openssl pkcs8 -topk8 -inform PEM -in private.pem -out private_key.pem -nocrypt
```

现在类似原始文件的新文件 `private_key.pem` 里包含了：

```
-----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDE9JsJBjmYEoUM
...
0fPinYmDJpkwMYHqQaLGQyg=
-----END PRIVATE KEY-----
```

如您只验证令牌（只需要 private_key.pem 文件）那么您需要签发令牌， 故而您需要一个公钥。在这种情况下您需要从私钥文件中提取公钥文件：

```
openssl rsa -in private.pem -outform PEM -pubout -out public.pem
```

您会见到类似以下内容的文件：

```
-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxPSbCQY5mBKFDIn1kggv
...
qwIDAQAB
-----END PUBLIC KEY-----
```

现在可以校验令牌有效性了：

```java
JWTAuth provider = JWTAuth.create(vertx, new JWTAuthOptions()
  .addPubSecKey(new PubSecKeyOptions()
    .setAlgorithm("RS256")
    .setBuffer(
      "-----BEGIN PUBLIC KEY-----\n" +
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxPSbCQY5mBKFDIn1kggv\n" +
        "Wb4ChjrctqD4nFnJOJk4mpuZ/u3h2ZgeKJJkJv8+5oFO6vsEwF7/TqKXp0XDp6IH\n" +
        "byaOSWdkl535rCYR5AxDSjwnuSXsSp54pvB+fEEFDPFF81GHixepIbqXCB+BnCTg\n" +
        "N65BqwNn/1Vgqv6+H3nweNlbTv8e/scEgbg6ZYcsnBBB9kYLp69FSwNWpvPmd60e\n" +
        "3DWyIo3WCUmKlQgjHL4PHLKYwwKgOHG/aNl4hN4/wqTixCAHe6KdLnehLn71x+Z0\n" +
        "SyXbWooftefpJP1wMbwlCpH3ikBzVIfHKLWT9QIOVoRgchPU3WAsZv/ePgl5i8Co\n" +
        "qwIDAQAB\n" +
        "-----END PUBLIC KEY-----"))
  .addPubSecKey(new PubSecKeyOptions()
    .setAlgorithm("RS256")
    .setBuffer(
      "-----BEGIN PRIVATE KEY-----\n" +
        "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDE9JsJBjmYEoUM\n" +
        "ifWSCC9ZvgKGOty2oPicWck4mTiam5n+7eHZmB4okmQm/z7mgU7q+wTAXv9Oopen\n" +
        "RcOnogdvJo5JZ2SXnfmsJhHkDENKPCe5JexKnnim8H58QQUM8UXzUYeLF6khupcI\n" +
        "H4GcJOA3rkGrA2f/VWCq/r4fefB42VtO/x7+xwSBuDplhyycEEH2Rgunr0VLA1am\n" +
        "8+Z3rR7cNbIijdYJSYqVCCMcvg8cspjDAqA4cb9o2XiE3j/CpOLEIAd7op0ud6Eu\n" +
        "fvXH5nRLJdtaih+15+kk/XAxvCUKkfeKQHNUh8cotZP1Ag5WhGByE9TdYCxm/94+\n" +
        "CXmLwKirAgMBAAECggEAeQ+M+BgOcK35gAKQoklLqZLEhHNL1SnOhnQd3h84DrhU\n" +
        "CMF5UEFTUEbjLqE3rYGP25mdiw0ZSuFf7B5SrAhJH4YIcZAO4a7ll23zE0SCW+/r\n" +
        "zr9DpX4Q1TP/2yowC4uGHpBfixxpBmVljkWnai20cCU5Ef/O/cAh4hkhDcHrEKwb\n" +
        "m9nymKQt06YnvpCMKoHDdqzfB3eByoAKuGxo/sbi5LDpWalCabcg7w+WKIEU1PHb\n" +
        "Qi+RiDf3TzbQ6TYhAEH2rKM9JHbp02TO/r3QOoqHMITW6FKYvfiVFN+voS5zzAO3\n" +
        "c5X4I+ICNzm+mnt8wElV1B6nO2hFg2PE9uVnlgB2GQKBgQD8xkjNhERaT7f78gBl\n" +
        "ch15DRDH0m1rz84PKRznoPrSEY/HlWddlGkn0sTnbVYKXVTvNytKSmznRZ7fSTJB\n" +
        "2IhQV7+I0jeb7pyLllF5PdSQqKTk6oCeL8h8eDPN7awZ731zff1AGgJ3DJXlRTh/\n" +
        "O6zj9nI8llvGzP30274I2/+cdwKBgQDHd/twbiHZZTDexYewP0ufQDtZP1Nk54fj\n" +
        "EpkEuoTdEPymRoq7xo+Lqj5ewhAtVKQuz6aH4BeEtSCHhxy8OFLDBdoGCEd/WBpD\n" +
        "f+82sfmGk+FxLyYkLxHCxsZdOb93zkUXPCoCrvNRaUFO1qq5Dk8eftGCdC3iETHE\n" +
        "6h5avxHGbQKBgQCLHQVMNhL4MQ9slU8qhZc627n0fxbBUuhw54uE3s+rdQbQLKVq\n" +
        "lxcYV6MOStojciIgVRh6FmPBFEvPTxVdr7G1pdU/k5IPO07kc6H7O9AUnPvDEFwg\n" +
        "suN/vRelqbwhufAs85XBBY99vWtxdpsVSt5nx2YvegCgdIj/jUAU2B7hGQKBgEgV\n" +
        "sCRdaJYr35FiSTsEZMvUZp5GKFka4xzIp8vxq/pIHUXp0FEz3MRYbdnIwBfhssPH\n" +
        "/yKzdUxcOLlBtry+jgo0nyn26/+1Uyh5n3VgtBBSePJyW5JQAFcnhqBCMlOVk5pl\n" +
        "/7igiQYux486PNBLv4QByK0gV0SPejDzeqzIyB+xAoGAe5if7DAAKhH0r2M8vTkm\n" +
        "JvbCFjwuvhjuI+A8AuS8zw634BHne2a1Fkvc8c3d9VDbqsHCtv2tVkxkKXPjVvtB\n" +
        "DtzuwUbp6ebF+jOfPK0LDuJoTdTdiNjIcXJ7iTTI3cXUnUNWWphYnFogzPFq9CyL\n" +
        "0fPinYmDJpkwMYHqQaLGQyg=\n" +
        "-----END PRIVATE KEY-----")
  ));

String token = provider.generateToken(
  new JsonObject().put("some", "token-data"),
  new JWTOptions().setAlgorithm("RS256"));
```

#### 使用 EC 秘钥

我们还支持椭圆曲线加密算法，但是在默认 JDK 上使用有一定限制

用法和 RSA 加密算法极其相似，首先您需要创建一个公钥：

```shell
openssl ecparam -name secp256r1 -genkey -out private.pem
```

然后您会看到类似以下内容的文件了：

```
-----BEGIN EC PARAMETERS-----
BggqhkjOPQMBBw==
-----END EC PARAMETERS-----
-----BEGIN EC PRIVATE KEY-----
MHcCAQEEIMZGaqZDTHL+IzFYEWLIYITXpGzOJuiQxR2VNGheq7ShoAoGCCqGSM49
AwEHoUQDQgAEG1O9LCrP6hg3Y9q68+LF0q48UcOkwVKE1ax0b56wjVusf3qnuFO2
/+XHKKhtzEavvFMeXRQ+ZVEqM0yGNb04qw==
-----END EC PRIVATE KEY-----
```

但是 JDK 更倾向于使用PKCS8格式，我们必须将其转换：

```shell
openssl pkcs8 -topk8 -nocrypt -in private.pem -out private_key.pem
```

然后会看到类似内容的文件：

```
-----BEGIN PRIVATE KEY-----
MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgxkZqpkNMcv4jMVgR
YshghNekbM4m6JDFHZU0aF6rtKGhRANCAAQbU70sKs/qGDdj2rrz4sXSrjxRw6TB
UoTVrHRvnrCNW6x/eqe4U7b/5ccoqG3MRq+8Ux5dFD5lUSozTIY1vTir
-----END PRIVATE KEY-----
```

使用私钥您可生成令牌：

```java
JWTAuth provider = JWTAuth.create(vertx, new JWTAuthOptions()
  .addPubSecKey(new PubSecKeyOptions()
    .setAlgorithm("ES256")
    .setBuffer(
      "-----BEGIN PRIVATE KEY-----\n" +
        "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgeRyEfU1NSHPTCuC9\n" +
        "rwLZMukaWCH2Fk6q5w+XBYrKtLihRANCAAStpUnwKmSvBM9EI+W5QN3ALpvz6bh0\n" +
        "SPCXyz5KfQZQuSj4f3l+xNERDUDaygIUdLjBXf/bc15ur2iZjcq4r0Mr\n" +
        "-----END PRIVATE KEY-----\n")
  ));

String token = provider.generateToken(
  new JsonObject(),
  new JWTOptions().setAlgorithm("ES256"));
```

为了验证令牌您还需要一个公钥：

```shell
openssl ec -in private.pem -pubout -out public.pem
```

现在您可用它进行全部操作了：

```java
JWTAuth provider = JWTAuth.create(vertx, new JWTAuthOptions()
  .addPubSecKey(new PubSecKeyOptions()
    .setAlgorithm("ES256")
    .setBuffer(
      "-----BEGIN PUBLIC KEY-----\n" +
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEraVJ8CpkrwTPRCPluUDdwC6b8+m4\n" +
        "dEjwl8s+Sn0GULko+H95fsTREQ1A2soCFHS4wV3/23Nebq9omY3KuK9DKw==\n" +
        "-----END PUBLIC KEY-----"))
  .addPubSecKey(new PubSecKeyOptions()
    .setAlgorithm("ES256")
    .setBuffer(
      "-----BEGIN PRIVATE KEY-----\n" +
        "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgeRyEfU1NSHPTCuC9\n" +
        "rwLZMukaWCH2Fk6q5w+XBYrKtLihRANCAAStpUnwKmSvBM9EI+W5QN3ALpvz6bh0\n" +
        "SPCXyz5KfQZQuSj4f3l+xNERDUDaygIUdLjBXf/bc15ur2iZjcq4r0Mr")
  ));

String token = provider.generateToken(
  new JsonObject(),
  new JWTOptions().setAlgorithm("ES256"));
```

#### JWT keystore 文件

如果您更倾向于使用 Java Keystores 格式的秘钥文件，那么您也可如此做。

身份认证器需要在 classpath 或文件路径上加载一个秘钥库，以供 `javax.crypto.Mac` 或 `java.security.Signature` 生成或认证令牌。

默认情况下，该实现将查找以下别名，并不是所有加密算法都有别名。 就比如 `HS256` 是存在的：

```
`HS256`:: HMAC 使用SHA-256哈希算法
`HS384`:: HMAC 使用SHA-384哈希算法
`HS512`:: HMAC 使用SHA-512哈希算法
`RS256`:: RSASSA 使用SHA-256哈希算法
`RS384`:: RSASSA 使用SHA-384哈希算法
`RS512`:: RSASSA 使用SHA-512哈希算法
`ES256`:: ECDSA 使用P-256曲线和SHA-256哈希算法
`ES384`:: ECDSA 使用P-384曲线和SHA-384哈希算法
`ES512`:: ECDSA 使用P-521曲线和SHA-512哈希算法
```

如果未提供密钥库，则实现将回退到不安全模式，并且不会验证签名， 这对于通过外部手段对 payload 签名会很有用。

存储于 keystore 里的密钥对始终包含证书。 证书的有效性在加载时就进行了测试， 如果证书已过期或无效， 则不会加载证书。

给定别名将在所有的密钥算法中匹配最合适的。 例如 `RS256` 算法是不允许的，`EC` 算法或 `RSA` 算法是允许的， 注意 `RSA` 具体为 `SHA1WithRSA` 而不是 `SHA256WithRSA`。

##### 生成新的Keystore格式秘钥

生成秘钥文件需要唯一的工具是 `keytool`， 您通过以下方式指定算法：

```
keytool -genseckey -keystore keystore.jceks -storetype jceks -storepass secret -keyalg HMacSHA256 -keysize 2048 -alias HS256 -keypass secret
keytool -genseckey -keystore keystore.jceks -storetype jceks -storepass secret -keyalg HMacSHA384 -keysize 2048 -alias HS384 -keypass secret
keytool -genseckey -keystore keystore.jceks -storetype jceks -storepass secret -keyalg HMacSHA512 -keysize 2048 -alias HS512 -keypass secret
keytool -genkey -keystore keystore.jceks -storetype jceks -storepass secret -keyalg RSA -keysize 2048 -alias RS256 -keypass secret -sigalg SHA256withRSA -dname "CN=,OU=,O=,L=,ST=,C=" -validity 360
keytool -genkey -keystore keystore.jceks -storetype jceks -storepass secret -keyalg RSA -keysize 2048 -alias RS384 -keypass secret -sigalg SHA384withRSA -dname "CN=,OU=,O=,L=,ST=,C=" -validity 360
keytool -genkey -keystore keystore.jceks -storetype jceks -storepass secret -keyalg RSA -keysize 2048 -alias RS512 -keypass secret -sigalg SHA512withRSA -dname "CN=,OU=,O=,L=,ST=,C=" -validity 360
keytool -genkeypair -keystore keystore.jceks -storetype jceks -storepass secret -keyalg EC -keysize 256 -alias ES256 -keypass secret -sigalg SHA256withECDSA -dname "CN=,OU=,O=,L=,ST=,C=" -validity 360
keytool -genkeypair -keystore keystore.jceks -storetype jceks -storepass secret -keyalg EC -keysize 384 -alias ES384 -keypass secret -sigalg SHA384withECDSA -dname "CN=,OU=,O=,L=,ST=,C=" -validity 360
keytool -genkeypair -keystore keystore.jceks -storetype jceks -storepass secret -keyalg EC -keysize 521 -alias ES512 -keypass secret -sigalg SHA512withECDSA -dname "CN=,OU=,O=,L=,ST=,C=" -validity 360
```

更多有关 keystores 的信息以及如何使用 `PKCS12` 格式秘钥 (默认：Java版本 >=9) 请参阅通用模块的文档。

### 读取令牌

如果由第三方发布 JWT 令牌，而您没有私钥， 在这种情况下您的公钥必须是 PEM 格式的。

```java
JWTAuthOptions config = new JWTAuthOptions()
  .addPubSecKey(new PubSecKeyOptions()
    .setAlgorithm("RS256")
    .setBuffer("BASE64-ENCODED-PUBLIC_KEY"));

AuthenticationProvider provider = JWTAuth.create(vertx, config);
```

## AuthN/AuthZ 和 JWT

通常在开发微服务时，您希望应用程序能够调用一些 API 。 而这些 API 并不打算暴露给一般用户， 因而我们应该在架构中移除所有对 API 调用方进行身份验证的交互部分的内容。

在这种情况下，可以使用 HTTP 作为调用 API 的协议， 并且 HTTP 协议已经定义了应该用于传递授权信息的标头 `Authorization` 。在大多数情况下令牌将以承载令牌（bearer tokens）的形式发送， 例如：`Authorization: Bearer some+base64+string`。

### 鉴权/身份验证 (AuthN)

对于此验证器，如果令牌通过签名检查并且令牌未过期， 则对用户进行身份验证。因此，必须保证私钥安全不被泄露，并且不要在项目中复制粘贴， 因为这将是一个安全漏洞。

```java
jwtAuth.authenticate(new JsonObject().put("token", "BASE64-ENCODED-STRING"))
  .onSuccess(user -> System.out.println("User: " + user.principal()))
  .onFailure(err -> {
    // 失败!
  });
```

简而言之，验证服务正在检查以下几件事：

- 令牌签名是否有效
- `exp`, `iat`, `nbf`, `audience`, `issuer` 等字段是否满足配置要求
  > iss（issuer）：JWT 签发方。
  > iat（issued at time）：JWT 签发时间。
  > sub（subject）：JWT 主题。
  > aud（audience）：JWT 接收方。
  > exp（expiration time）：JWT 的过期时间。是一个数字的组合，这是因为这个字符串使用的是 Unix 的时间
  > nbf（not before time）：JWT 生效时间，早于该定义的时间的 JWT 不能被接受处理。
  > jti（JWT ID）：JWT 唯一标识。

如果所有这些都有效，则令牌被认为是正确的，并返回一个用户对象。

尽管字段 `exp`，`iat`、`nbf` 是简单的时间戳校验，但只有 `exp` 可以被配置成忽略：

```java
jwtAuth.authenticate(
  new JsonObject()
    .put("token", "BASE64-ENCODED-STRING")
    .put("options", new JsonObject()
      .put("ignoreExpiration", true)))
  .onSuccess(user -> System.out.println("User: " + user.principal()))
  .onFailure(err -> {
    // 失败!
  });
```

为了验证 `aud` 字段需要像以上用例一样传递选项：

```java
jwtAuth.authenticate(
  new JsonObject()
    .put("token", "BASE64-ENCODED-STRING")
    .put("options", new JsonObject()
      .put("audience", new JsonArray().add("paulo@server.com"))))
  .onSuccess(user -> System.out.println("User: " + user.principal()))
  .onFailure(err -> {
    // 失败!
  });
```

验证 issuer 字段：

```java
jwtAuth.authenticate(
  new JsonObject()
    .put("token", "BASE64-ENCODED-STRING")
    .put("options", new JsonObject()
      .put("issuer", "mycorp.com")))
  .onSuccess(user -> System.out.println("User: " + user.principal()))
  .onFailure(err -> {
    // Failed!
  });
```

### 授权 (AuthZ)

一旦令牌被解析并且有效，我们就可以使用它来执行授权任务。最简单的方法是验证用户是否具有特定权限。 授权将遵循通用的 `AuthorizationProvider` API。 选择相应的验证服务 API 来产生、验证令牌。

目前有两个工厂类：

- `JWTAuthorization` 根据权限声明确定权限。
- `MicroProfileAuthorization` 令牌根据 <a href="https://www.eclipse.org/community/eclipse_newsletter/2017/september/article2.php">MP JWT spec</a>.

典型的用法是使用验证器从用户对象中提取权限并执行证明：

```java
AuthorizationProvider authz = MicroProfileAuthorization.create();

authz.getAuthorizations(user)
  .onSuccess(v -> {
    // 现在我们可以根据需要执行检查
    if (PermissionBasedAuthorization.create("create-report").match(user)) {
      // 是的，用户可以创建报告
    }
  });
```

默认情况下验证器会检查 `permissions` 键，但是和其他验证器一样, 可以通过使用 `:` 分隔符将概念拓展到角色，因此可以用 `role:authority` 查找令牌。

JWT 是个相当自由的格式，并没有强制规定，所以可以将 `permissions` 配置成其他内容， 例如，甚至可以在这样的路径下查找：

```java
JsonObject config = new JsonObject()
  .put("public-key", "BASE64-ENCODED-PUBLIC_KEY")
  // 因为我们正在使用 keycloak JWT 因此我们需要
  // 在令牌中设置许可声明
  .put("permissionsClaimKey", "realm_access/roles");

AuthenticationProvider provider =
  JWTAuth.create(vertx, new JWTAuthOptions(config));
```

所以在此示例中，我们将 JWT 配置为使用 Keycloak 令牌格式。在这种情况下 `realm_access/roles` 路径下的 claims 会被检查 而不是 `permissions`。

### 校验令牌

方法 `authenticate` 被调用时，令牌将根据初始化期间提供的 `JWTOptions` 进行 验证。验证步骤如下:

1. `ignoreExpiration` (默认关闭) 是关闭的的情况下, 校验令牌的有效期, 将检查字段: `exp`, `iat` 和 `nbf`。 由于各端时间存在一定偏差, 可以配置 `leeway`宽限日期， 应对时间超出而失效的情况。
2. 如果配置了 `audience`, 那么根据配置检查令牌中的 `aud` 属性， 所以令牌中必须有属性。
3. 如果配置了 `issuer` ，那么令牌的 `iss` 属性会被检查。

这些验证完成后，将返回 JWTUser 对象，该对象包含了配置中对权限声明密文的引用， 这个值在后面验证时会用到。 该值对应于权限检查会用的 json 路径。

### 自定义 Token 生成

以相同的方式验证令牌，生成是在初始化期间进行初始配置的。

生成令牌时，可以提供一个可选的额外参数来控制令牌的生成， 这是一个 `JWTOptions` 对象。 可以使用 `algorithm` 属性来配置令牌签名算法（默认为：HS256）。 在这种情况下，将执行与该算法相对应的密钥的查找并将其用于签名。

令牌的 `headers` 属性可以添加额外的信息或者与默认选项合并。

有时我们发行的令牌会没有时间戳（例如：在测试、开发过程中），在这种情况下 `noTimestamp` 属性应该被设置成 ture (默认为 false)。 这将表示着令牌中没有 `iat` 字段。

令牌的过期时间由 `expiresInSeconds` 属性控制，默认情况下不会过期。 然后可以配置其他控制字段 `audience`，`issuer` 以及 `subject` 并将其加入到令牌元数据中。

最后对令牌以正确的格式进行编码并签名。
