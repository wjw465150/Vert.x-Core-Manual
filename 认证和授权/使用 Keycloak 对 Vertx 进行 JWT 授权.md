# 使用 Keycloak 对 Vertx 进行 JWT 授权

> 原文: https://vertx.io/blog/jwt-authorization-for-vert-x-with-keycloak/

在这篇博文中，您将了解：

- JWT foun­da­tions
- 如何使用 JWT 授权保护路由
- 如何从 JWT 编码令牌中提取**声明(claims)**
- 如何将RBAC应用于Keycloak Realm roles

## 再次问好

嘿，你好！在我的上一篇博客文章[Easy SSO for Vert.x with Keycloak](https://vertx.io/blog/easy-sso-for-vert-x-with-keycloak/) 中，我们学习了如何使用Keycloak和OpenID connect为Vert.x Web应用程序配置单点登录。这一次，我们将看到如何使用 Vert.x 的 [JWT 授权](https://vertx.io/docs/vertx-web/java/#_jwt_authorisation)支持 用 Keycloak 来保护应用程序。

## Keycloak 设置

为了保护我们的 Vert.x 应用程序，我们需要使用 Keycloak 服务器来获取 JWT 令牌。虽然 [Keycloak有一个很好的入门指南](https://www.keycloak.org/docs/latest/getting_started/)，但我想让把所有东西放在一起更容易一些，因此我准备了一个本地的Keycloak docker容器，如[此处](https://github.com/thomasdarimont/vertx-playground/tree/master/jwt-service-vertx#start-keycloak-with-the-vertx-realm)所述，它配备了所有必需的配置，您可以轻松开始。

预配置的 Keycloak 领域`vertx`包含一个`vertx-service` OpenID 连接客户端，用于我们的 Vert.x 应用程序和一组用于测试的用户。 为了简化测试，`vertx-service`配置为在 Keycloak 中启用了`Direct Access Grant`，从而支持 OAuth2 资源所有者密码凭证授予 (ROPC) 流程。

要使用预配置的领域启动 Keycloak，只需使用以下命令启动 docker 容器：

```bash
docker run \
  -it \
  --name vertx-keycloak \
  --rm \
  -e KEYCLOAK_USER=admin \
  -e KEYCLOAK_PASSWORD=admin \
  -e KEYCLOAK_IMPORT=/tmp/vertx-realm.json \
  -v $PWD/vertx-realm.json:/tmp/vertx-realm.json \
  -v $PWD/data:/opt/jboss/keycloak/standalone/data \
  -p 8080:8080 \
  quay.io/keycloak/keycloak:11.0.2
```

## Vert.x App Vert.x 应用程序

The ex­am­ple app con­sists of a sin­gle `Verticle`, that runs on `http://localhost:3000` and pro­vides a few routes with pro­tected re­sources. [You can find the com­plete ex­am­ple here](https://github.com/thomasdarimont/vertx-playground/tree/master/jwt-service-vertx/src/main/java/demo/MainVerticle.java).
示例应用由单个 `Verticle` 组成，该 `Verticle` 在 `http://localhost:3000` 上运行，并提供一些具有受保护资源的路由。 您可以在此处找到完整的示例。

Our web app con­tains the fol­low­ing pro­tected routes with han­dlers:
我们的 Web 应用程序包含以下带有处理程序的受保护路由：

- `/api/greet` - The greet­ing re­source, which re­turns a greet­ing mes­sage, only au­then­ti­cated users can ac­cess this re­source.
  `/api/greet` - 返回问候消息的问候语资源，只有经过身份验证的用户才能访问此资源。
- `/api/user` - The user re­source, which re­turns some in­for­ma­tion about the user, only users with role `user` can ac­cess this re­source.
  `/api/user` - 用户资源，返回有关用户的一些信息，只有角色为 `user` 的用户才能访问此资源。
- `/api/admin` - The user re­source, which re­turns some in­for­ma­tion about the admin, only users with role `admin` can ac­cess this re­source.
  `/api/admin` - 用户资源，返回有关管理员的一些信息，只有角色为 `admin` 的用户才能访问此资源。

This ex­am­ple is built with Vert.x ver­sion 3.9.3.
此示例使用 Vert.x 版本 3.9.3 构建。

### Running the app in the console 在控制台运行应用

To run the app, we need to build it first:
要运行应用程序，我们需要先构建它：

```bash
cd jwt-service-vertx
mvn clean package
```

This cre­ates a jar, which we can run:
这将创建一个 jar，我们可以运行它：

```bash
java -jar target/*.jar
```

Note, that we need to start Key­cloak first, since our app fetches the con­fig­u­ra­tion from Key­cloak on startup.
请注意，我们需要先启动Keycloak，因为我们的应用程序在启动时从Keycloak获取配置。

### Running the app in the IDE 在 IDE 中运行应用程序

We can also run the app di­rectly from your favourite IDE like In­tel­liJ Idea or Eclipse. To run the app from an IDE, we need to cre­ate a launch con­fig­u­ra­tion and use the main class `io.vertx.core.Launcher`. Then set the the pro­gram ar­gu­ments to `run demo.MainVerticle` and use the class­path of the `jwt-service-vertx` mod­ule. With that in place we should be able to run the app.
我们还可以直接从您喜欢的IDE运行该应用程序，例如IntelliJ Idea或Eclipse。要从 IDE 运行应用程序，我们需要创建一个启动配置并使用主类 `io.vertx.core.Launcher` .然后将程序参数设置为 `run demo.MainVerticle` 并使用 `jwt-service-vertx` 模块的类路径。有了这个，我们应该能够运行该应用程序。

## JWT Authorization 智威汤逊授权

### JWT Foundations 智威汤逊基金会

[JSON Web Token (JWT)](https://tools.ietf.org/html/rfc7519) is an open stan­dard to se­curely ex­change in­for­ma­tion be­tween two par­ties in the form of [Base64URL](https://base64.guru/standards/base64url) en­coded JSON ob­jects. A stan­dard JWT is just a string which com­prises three base64url en­coded parts header, pay­load and a sig­na­ture, which are sep­a­rated by a ”`.`” char­ac­ter. There are other vari­ants of JWT that can have more parts.
JSON Web 令牌 （JWT） 是一种开放标准，用于以 Base64URL 编码的 JSON 对象的形式在双方之间安全地交换信息。标准的JWT只是一个字符串，它由三个base64url编码的部分标头，有效负载和一个签名组成，它们由“ `.` ”字符分隔。JWT 还有其他变体可以拥有更多部件。

An ex­am­ple JWT can look like this:
示例 JWT 可能如下所示：

```text
eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJjN00xX2hkWjAtWDNyZTl1dmZLSFRDUWRxYXJQYnBMblVJMHltdkF0U1RzIn0.eyJleHAiOjE2MDEzMTg0MjIsImlhdCI6MTYwMTMxODEyMiwianRpIjoiNzYzNWY1YTEtZjFkNy00NTdkLWI4NjktYWQ0OTIzNTJmNGQyIiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL2F1dGgvcmVhbG1zL3ZlcnR4IiwiYXVkIjoiYWNjb3VudCIsInN1YiI6IjI3YjNmYWMwLTlhZWMtNDQyMS04MWNmLWQ0YjAyNDI4ZjkwMSIsInR5cCI6IkJlYXJlciIsImF6cCI6InZlcnR4LXNlcnZpY2UiLCJzZXNzaW9uX3N0YXRlIjoiNjg3MDgyMTMtNDBiNy00NThhLWFlZTEtMzlkNmY5ZGEwN2FkIiwiYWNyIjoiMSIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJvZmZsaW5lX2FjY2VzcyIsInVtYV9hdXRob3JpemF0aW9uIiwidXNlciJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoiZW1haWwgcHJvZmlsZSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJuYW1lIjoiVGhlbyBUZXN0ZXIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ0ZXN0ZXIiLCJnaXZlbl9uYW1lIjoiVGhlbyIsImZhbWlseV9uYW1lIjoiVGVzdGVyIiwiZW1haWwiOiJ0b20rdGVzdGVyQGxvY2FsaG9zdCJ9.NN1ZGE3f3LHE0u7T6Vfq5yPMKoZ6SmrUxoFopAXZm5wVgMOsJHB8BgHQTDm7u0oTVU0ZHlKH2-o11RKK7Mz0mLqMy2EPdkGY9Bqtj5LZ8oTp8FaVqY1g5Fr5veXYpOMbc2fke-e2hG8sAfSjWz1Mq9BUhJ7HdK7TTIte12pub2nbUs4APYystJWx49cYmUwZ-5c9X295V-NX9UksuMSzFItZ4cACVKi68m9lkR4RuNQKFTuLvWsorz9yRx884e4cnoT_JmfSfYBIl31FfnQzUtCjluUzuD9jVXc_vgC7num_0AreOZiUzpglb8UjKXjswTHF-v_nEIaq7YmM5WKpeg
```

The header and pay­load sec­tions con­tain in­for­ma­tion as a JSON ob­ject, whereas the sig­na­ture is just a plain string. JSON ob­jects con­tain key value pairs which are called `claims`.
标头和有效负载部分包含作为 JSON 对象的信息，而签名只是一个纯字符串。JSON 对象包含称为 `claims` 的键值对。

The claims in­for­ma­tion can be ver­i­fied and trusted be­cause it is dig­i­tally signed with the pri­vate key from a pub­lic/pri­vate key-pair. The sig­na­ture can later be ver­i­fied with a cor­re­spond­ing pub­lic key. The iden­ti­fier of the pub­lic/pri­vate key-pair used to sign a JWT can be con­tained in a spe­cial claim called `kid` (key iden­ti­fier) in the header sec­tion of the JWT.
声明信息可以验证和信任，因为它是使用公钥/私钥对中的私钥进行数字签名的。稍后可以使用相应的公钥验证签名。用于对 JWT 进行签名的公钥/私钥对的标识符可以包含在 JWT 标头部分中名为 `kid` （密钥标识符）的特殊声明中。

An ex­am­ple for a JWT header that ref­er­ences a pub­lic/pri­vate key-pair looks like this:
引用公钥/私钥对的 JWT 标头示例如下所示：

```json
{
  "alg": "RS256",
  "typ": "JWT",
  "kid": "c7M1_hdZ0-X3re9uvfKHTCQdqarPbpLnUI0ymvAtSTs"
}
```

It is quite com­mon to use JWTs to con­vey in­for­ma­tion about au­then­ti­ca­tion (user iden­tity) and au­tho­riza­tion (scopes, user roles, per­mis­sions and other claims). OpenID providers such as [Key­cloak](https://www.keycloak.org/) sup­port is­su­ing OAuth2 ac­cess to­kens after au­then­ti­ca­tion for users to clients in the form of JWTs. An ac­cess token can then be used to ac­cess other ser­vices or APIs on be­half of the user. The server pro­vid­ing those ser­vices or APIs is often called `resource server`.
使用 JWT 传达有关身份验证（用户身份）和授权（范围、用户角色、权限和其他声明）的信息是很常见的。OpenID 提供程序（如 Keycloak）支持在用户身份验证后以 JWT 的形式向客户端颁发 OAuth2 访问令牌。然后，可以使用访问令牌代表用户访问其他服务或 API。提供这些服务或 API 的服务器通常称为 `resource server` 。

An ex­am­ple JWT pay­load gen­er­ated by Key­cloak looks like this:
Keycloak 生成的 JWT 有效负载示例如下所示：

```json
{
  "exp": 1601318422,
  "iat": 1601318122,
  "jti": "7635f5a1-f1d7-457d-b869-ad492352f4d2",
  "iss": "http://localhost:8080/auth/realms/vertx",
  "aud": "account",
  "sub": "27b3fac0-9aec-4421-81cf-d4b02428f901",
  "typ": "Bearer",
  "azp": "vertx-service",
  "session_state": "68708213-40b7-458a-aee1-39d6f9da07ad",
  "acr": "1",
  "realm_access": {
    "roles": [
      "offline_access",
      "uma_authorization",
      "user"
    ]
  },
  "scope": "email profile",
  "email_verified": true,
  "name": "Theo Tester",
  "preferred_username": "tester",
  "given_name": "Theo",
  "family_name": "Tester",
  "email": "tom+tester@localhost"
}
```

If a `resource server` re­ceives a re­quest with such an ac­cess token, it needs to ver­ify and in­spect the token be­fore it can trust its con­tent. To ver­ify the token, the `resource server` needs to ob­tain the `public key` to check the token sig­na­ture. This `public key` can ei­ther be con­fig­ured sta­t­i­cally or fetched dy­nam­i­cally from the OpenID Provider by lever­ag­ing the `kid` in­for­ma­tion from the JWT header sec­tion. Note that most `OpenID providers`, such as Key­cloak, pro­vide a ded­i­cated end­point for dy­namic pub­lic key lookups, e.g. `http://localhost:8080/auth/realms/vertx/protocol/openid-connect/certs`. A stan­dard for pro­vid­ing pub­lic key in­for­ma­tion is [JSON Web Key Set (JWKS)](https://tools.ietf.org/html/rfc7517). The JWKS in­for­ma­tion is usu­ally cached by the re­source server to avoid the over­head of fetch­ing JWKS for every re­quest.
如果 `resource server` 收到具有此类访问令牌的请求，则需要先验证并检查令牌，然后才能信任其内容。要验证令牌， `resource server` 需要获取 `public key` 来检查令牌签名。此 `public key` 可以静态配置，也可以利用 JWT 标头部分中的 `kid` 信息从 OpenID 提供程序动态获取。请注意，大多数 `OpenID providers` （例如 Keycloak）为动态公钥查找提供了专用端点，例如 `http://localhost:8080/auth/realms/vertx/protocol/openid-connect/certs` .提供公钥信息的标准是 JSON Web 密钥集 （JWKS）。JWKS 信息通常由资源服务器缓存，以避免为每个请求获取 JWKS 的开销。

An ex­am­ple re­sponse for Key­cloak’s JWKS end­point looks like this:
Keycloak 的 JWKS 端点的示例响应如下所示：

```json
{
   "keys":[
      {
         "kid":"c7M1_hdZ0-X3re9uvfKHTCQdqarPbpLnUI0ymvAtSTs",
         "kty":"RSA",
         "alg":"RS256",
         "use":"sig",
         "n":"iFuX2bAXA99Yrv6YEvpV9tjS52krP5UJ7lFL02Zl83PPV6PiLIWKTqF71bfTKnVDxO421xAsBw9f6dlgoyxxY1H_bzJQQryQkry7DA7tI_SnKVsehLgeF-tCcjRF_MF1kM14F1A5Zsu6oYIkMZvgJIRM-ejtz3aUcdnLcTvpPrmfvj7KwRgNsfm6Q-kO0-OAf6m6LaRvaC5VpTIRoVxXNhSIiGKuZ4d05Yk0-HdOR0D0sfOujYzleJmTGBEIAmdWpZqUXiSWbzmpw8mJmacFTP9v8lsTUYZrXc69xm5fHaNJ6PO_E-IKiPKT7OeoM2l3HIK76a4azVL1Ewbv1UtMFw",
         "e":"AQAB",
         "x5c":[
            "MIICmTCCAYECBgFwplKOujANBgkqhkiG9w0BAQsFADAQMQ4wDAYDVQQDDAV2ZXJ0eDAeFw0yMDAzMDQxNjExMzNaFw0zMDAzMDQxNjEzMTNaMBAxDjAMBgNVBAMMBXZlcnR4MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiFuX2bAXA99Yrv6YEvpV9tjS52krP5UJ7lFL02Zl83PPV6PiLIWKTqF71bfTKnVDxO421xAsBw9f6dlgoyxxY1H/bzJQQryQkry7DA7tI/SnKVsehLgeF+tCcjRF/MF1kM14F1A5Zsu6oYIkMZvgJIRM+ejtz3aUcdnLcTvpPrmfvj7KwRgNsfm6Q+kO0+OAf6m6LaRvaC5VpTIRoVxXNhSIiGKuZ4d05Yk0+HdOR0D0sfOujYzleJmTGBEIAmdWpZqUXiSWbzmpw8mJmacFTP9v8lsTUYZrXc69xm5fHaNJ6PO/E+IKiPKT7OeoM2l3HIK76a4azVL1Ewbv1UtMFwIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQBxcXiTtGoo4/eMNwhagYH8QpK1n7fxgzn4mkESU3wD+rnPOAh/xFmx5c3aq8X+8W2z7oopO86ZBSQ8HfbzViBP0uwvf7s7E6Q8FOqrUNv0Kj308A7hF1IOqOhCJE2nABIWJduYz5dWZN434Q9El30L1eOYTtjBUmCdP7/CM+1bvxIT+CYrWmjI9zCMJxhuixmLffppsLCjGtNgFBemjQyCrLxpEGCfy8QGb4pTY/XaHuJ7k6ZaQkVeTbeDzaZbHc9zT5qgf6w4Gp7y+uPZdAsasrwiqm3YBtyBfaK42luk09nHpV6PRKpftnyLVPwlQiJAW6ZMckvDwmnDst70msnb"
         ],
         "x5t":"MVYTXCx5cUQ8lT1ymIDDRYO7_ZI",
         "x5t#S256":"yBDVTlfR0e7cv3HxbbkfvGKVs5W1VQtFs7haE_js3DY"
      }
   ]
}
```

The `keys` array con­tains the JWKS struc­ture with the pub­lic key in­for­ma­tion that be­longs to the pub­lic/pri­vate key-pair which was used to sign the JWT ac­cess token from above. Note the match­ing `kid` claim from our ear­lier JWT header ex­am­ple.
`keys` 数组包含 JWKS 结构，其中包含属于公钥/私钥对的公钥信息，公钥/私钥对用于从上面对 JWT 访问令牌进行签名。请注意前面的 JWT 标头示例中匹配的 `kid` 声明。

Now that we have the ap­pro­pri­ate pub­lic key, we can use the in­for­ma­tion from the JWT header to val­i­date the sig­na­ture of the JWT ac­cess token. If the sig­na­ture is valid, we can go on and check ad­di­tional claims from the pay­load sec­tion of the JWT, such as ex­pi­ra­tion, al­lowed is­suer and au­di­ence etc.
现在我们有了适当的公钥，我们可以使用 JWT 标头中的信息来验证 JWT 访问令牌的签名。如果签名有效，我们可以继续检查 JWT 有效负载部分的其他声明，例如过期、允许的发行者和受众等。

Now that we have the nec­es­sary build­ing blocks in place, we can fi­nally look at how to con­fig­ure JWT au­tho­riza­tion in Vert.x.
现在我们已经有了必要的构建块，我们终于可以看看如何在 Vert.x 中配置 JWT 授权了。

### JWT Authorization in Vert.x Vert.x 中的 JWT 授权

Set­ting up JWT au­tho­riza­tion in Vert.x is quite easy. First we need to add the `vertx-auth-jwt` mod­ule as a de­pen­dency to our project.
在 Vert.x 中设置 JWT 授权非常简单。首先，我们需要将 `vertx-auth-jwt` 模块作为依赖项添加到项目中。

```xml
<dependency>
    <groupId>io.vertx</groupId>
    <artifactId>vertx-auth-jwt</artifactId>
</dependency>
```

In our ex­am­ple, the whole JWT au­tho­riza­tion setup hap­pens in the method `setupJwtAuth`.
在我们的示例中，整个 JWT 授权设置发生在方法 `setupJwtAuth` 中。

We use a `WebClient` to dy­nam­i­cally fetch the pub­lic key in­for­ma­tion from the `/protocol/openid-connect/certs` JWKS end­point rel­a­tive to our Key­cloak is­suer URL. After that, we con­fig­ure a `JWTAuth` in­stance and cus­tomize the JWT val­i­da­tion via `JWTOptions` and `JWTAuthOptions`. Note that we use Key­cloak’s realm roles for role based au­tho­riza­tion via the `JWTAuthOptions#setPermissionsClaimKey(..)` method.
我们使用 `WebClient` 从 `/protocol/openid-connect/certs` JWKS 端点动态获取相对于我们的 Keycloak 颁发者 URL 的公钥信息。之后，我们配置一个 `JWTAuth` 实例并通过 `JWTOptions` 和 `JWTAuthOptions` 自定义 JWT 验证。请注意，我们通过 `JWTAuthOptions#setPermissionsClaimKey(..)` 方法使用 Keycloak 的领域角色进行基于角色的授权。

```java
private Future<Startup> setupJwtAuth(Startup startup) {

    var jwtConfig = startup.config.getJsonObject("jwt");
    var issuer = jwtConfig.getString("issuer");
    var issuerUri = URI.create(issuer);

    // derive JWKS uri from Keycloak issuer URI
    var jwksUri = URI.create(jwtConfig.getString("jwksUri", String.format("%s://%s:%d%s",
            issuerUri.getScheme(), issuerUri.getHost(), issuerUri.getPort(), issuerUri.getPath() + "/protocol/openid-connect/certs")));

    var promise = Promise.<JWTAuth>promise();

    // fetch JWKS from `/certs` endpoint
    webClient.get(jwksUri.getPort(), jwksUri.getHost(), jwksUri.getPath())
            .as(BodyCodec.jsonObject())
            .send(ar -> {

                if (!ar.succeeded()) {
                    startup.bootstrap.fail(String.format("Could not fetch JWKS from URI: %s", jwksUri));
                    return;
                }

                var response = ar.result();

                var jwksResponse = response.body();
                var keys = jwksResponse.getJsonArray("keys");

                // Configure JWT validation options
                var jwtOptions = new JWTOptions();
                jwtOptions.setIssuer(issuer);

                // extract JWKS from keys array
                var jwks = ((List<Object>) keys.getList()).stream()
                        .map(o -> new JsonObject((Map<String, Object>) o))
                        .collect(Collectors.toList());

                // configure JWTAuth
                var jwtAuthOptions = new JWTAuthOptions();
                jwtAuthOptions.setJwks(jwks);
                jwtAuthOptions.setJWTOptions(jwtOptions);
                jwtAuthOptions.setPermissionsClaimKey(jwtConfig.getString("permissionClaimsKey", "realm_access/roles"));

                JWTAuth jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);
                promise.complete(jwtAuth);
            });

    return promise.future().compose(auth -> {
        jwtAuth = auth;
        return Future.succeededFuture(startup);
    });
}
```

### Protecting routes with JWTAuthHandler 使用 JWTA 处理程序保护路由

Now that our `JWTAuth` is con­fig­ured, we can use the `JWTAuthHandler` in the `setupRouter` method to apply JWT au­tho­riza­tion to all routes match­ing the path pat­tern `/api/*`. The `JWTAuthHandler` val­i­dates re­ceived JWTs and per­forms ad­di­tional checks like ex­pi­ra­tion and al­lowed is­suers. With that in place, we con­fig­ure our ac­tual routes in `setupRoutes`.
现在我们的 `JWTAuth` 配置好了，我们可以在 `setupRouter` 方法中使用 `JWTAuthHandler` 将 JWT 授权应用于与路径模式 `/api/*` 匹配的所有路由。 `JWTAuthHandler` 验证收到的 JWT 并执行其他检查，例如过期和允许的颁发者。有了这个，我们在 `setupRoutes` 中配置了我们的实际路由。

```java
private Future<Startup> setupRouter(Startup startup) {

    router = Router.router(vertx);

    router.route("/api/*").handler(JWTAuthHandler.create(jwtAuth));

    return Future.succeededFuture(startup);
}

private Future<Startup> setupRoutes(Startup startup) {

    router.get("/api/greet").handler(this::handleGreet);
    router.get("/api/user").handler(this::handleUserData);
    router.get("/api/admin").handler(this::handleAdminData);

    return Future.succeededFuture(startup);
}
```

### Extracting user information from JWTUser 从 JWTUser 提取用户信息

To ac­cess user in­for­ma­tion in our `handleGreet` method, we cast the re­sult of the `io.vertx.ext.web.RoutingContext#user` method to `JWTUser` which al­lows us to ac­cess token claim in­for­ma­tion via the `io.vertx.ext.auth.jwt.impl.JWTUser#principal` JSON ob­ject.
为了访问 `handleGreet` 方法中的用户信息，我们将 `io.vertx.ext.web.RoutingContext#user` 方法的结果强制转换为 `JWTUser` ，这允许我们通过 `io.vertx.ext.auth.jwt.impl.JWTUser#principal` JSON 对象访问令牌声明信息。

If we’d like to use the JWT ac­cess token for other ser­vice calls, we could ex­tract the token from the `Authorization` header.
如果我们想将 JWT 访问令牌用于其他服务调用，我们可以从 `Authorization` 标头中提取令牌。

```java
private void handleGreet(RoutingContext ctx) {

    var jwtUser = (JWTUser) ctx.user();
    var username = jwtUser.principal().getString("preferred_username");
    var userId = jwtUser.principal().getString("sub");

    var accessToken = ctx.request().getHeader(HttpHeaders.AUTHORIZATION).substring("Bearer ".length());
    // Use accessToken for down-stream calls if needed...

    ctx.request().response().end(String.format("Hi %s (%s) %s%n", username, userId, Instant.now()));
}
```

### Obtaining an Access Token from Keycloak for user `tester` 从密钥斗篷获取用户 @1 的访问令牌#

To test our ap­pli­ca­tion we can use the fol­low­ing `curl` com­mands in a bash like shell to ob­tain an JWT ac­cess token to call one of our end­points as the user `tester` with the role `user`.
为了测试我们的应用程序，我们可以在类似 bash 的 shell 中使用以下 `curl` 命令来获取 JWT 访问令牌，以用户 `tester` 的身份调用我们的一个端点，角色为 `user` 。

Note that this ex­am­ple uses the cli tool [jq](https://stedolan.github.io/jq/) for JSON pro­cess­ing.
请注意，此示例使用 cli 工具 jq 进行 JSON 处理。

```bash
KC_USERNAME=tester
KC_PASSWORD=test
KC_CLIENT=vertx-service
KC_CLIENT_SECRET=ecb85cc5-f90d-4a03-8fac-24dcde57f40c
KC_REALM=vertx
KC_URL=http://localhost:8080/auth
KC_RESPONSE=$(curl  -k \
        -d "username=$KC_USERNAME" \
        -d "password=$KC_PASSWORD" \
        -d 'grant_type=password' \
        -d "client_id=$KC_CLIENT" \
        -d "client_secret=$KC_CLIENT_SECRET" \
        "$KC_URL/realms/$KC_REALM/protocol/openid-connect/token" \
    | jq .)

KC_ACCESS_TOKEN=$(echo $KC_RESPONSE| jq -r .access_token)
echo $KC_ACCESS_TOKEN
```

Here we use the JWT ac­cess token in the `Authorization` header with the `Bearer` pre­fix to call our `greet` route:
在这里，我们使用 `Authorization` 标头中的 JWT 访问令牌和 `Bearer` 前缀来调用我们的 `greet` 路由：

```bash
curl --silent -H "Authorization: Bearer $KC_ACCESS_TOKEN" http://localhost:3000/api/greet
```

Ex­am­ple out­put: 示例输出：

```bash
Hi tester (27b3fac0-9aec-4421-81cf-d4b02428f901) 2020-09-28T21:03:59.254230700Z
```

### Applying Role-based Access-Control with JWTUser 使用 JWTUser 应用基于角色的访问控制

To lever­age sup­port for role based ac­cess con­trol (RBAC) we can use the `io.vertx.ext.auth.User#isAuthorised` method to check whether the cur­rent user has the re­quired role. If the role is present we re­turn some data about the user, oth­er­wise we send a re­sponse with sta­tus code 403 and a `forbidden` error mes­sage.
若要利用对基于角色的访问控制 （RBAC） 的支持，可以使用 `io.vertx.ext.auth.User#isAuthorised` 方法来检查当前用户是否具有所需的角色。如果角色存在，我们将返回有关用户的一些数据，否则我们将发送状态代码为 403 和 `forbidden` 错误消息的响应。

```java
private void handleUserData(RoutingContext ctx) {

    var jwtUser = (JWTUser) ctx.user();
    var username = jwtUser.principal().getString("preferred_username");
    var userId = jwtUser.principal().getString("sub");

    jwtUser.isAuthorized("user", res -> {

        if (!res.succeeded() || !res.result()) {
            toJsonResponse(ctx).setStatusCode(403).end("{\"error\": \"forbidden\"}");
            return;
        }

        JsonObject data = new JsonObject()
                .put("type", "user")
                .put("username", username)
                .put("userId", userId)
                .put("timestamp", Instant.now());

        toJsonResponse(ctx).end(data.toString());
    });
}

private void handleAdminData(RoutingContext ctx) {

    var jwtUser = (JWTUser) ctx.user();
    var username = jwtUser.principal().getString("preferred_username");
    var userId = jwtUser.principal().getString("sub");

    jwtUser.isAuthorized("admin", res -> {

        if (!res.succeeded() || !res.result()) {
            toJsonResponse(ctx).setStatusCode(403).end("{\"error\": \"forbidden\"}");
            return;
        }

        JsonObject data = new JsonObject()
                .put("type", "admin")
                .put("username", username)
                .put("userId", userId)
                .put("timestamp", Instant.now());

        toJsonResponse(ctx).end(data.toString());
    });
}
curl --silent -H "Authorization: Bearer $KC_ACCESS_TOKEN" http://localhost:3000/api/user
```

Out­put: 输出：

```json
{"type":"user","username":"tester","userId":"27b3fac0-9aec-4421-81cf-d4b02428f901","timestamp":"2020-09-28T21:07:49.340950300Z"}
curl --silent -H "Authorization: Bearer $KC_ACCESS_TOKEN" http://localhost:3000/api/admin
```

Out­put: 输出：

```json
{"error": "forbidden"}
```

### Obtaining an Access Token from Keycloak for user `vadmin` 从密钥斗篷获取用户 @1 的访问令牌#

To check ac­cess with an `admin` role, we ob­tain a new token for the user `vadmin` which has the roles `admin` and `user`.
要使用 `admin` 角色检查访问权限，我们为用户 `vadmin` 获取一个新令牌，该令牌具有角色 `admin` 和 `user` 。

```bash
KC_USERNAME=vadmin
KC_PASSWORD=test
KC_CLIENT=vertx-service
KC_CLIENT_SECRET=ecb85cc5-f90d-4a03-8fac-24dcde57f40c
KC_REALM=vertx
KC_URL=http://localhost:8080/auth
KC_RESPONSE=$(curl  -k \
        -d "username=$KC_USERNAME" \
        -d "password=$KC_PASSWORD" \
        -d 'grant_type=password' \
        -d "client_id=$KC_CLIENT" \
        -d "client_secret=$KC_CLIENT_SECRET" \
        "$KC_URL/realms/$KC_REALM/protocol/openid-connect/token" \
    | jq .)

KC_ACCESS_TOKEN=$(echo $KC_RESPONSE| jq -r .access_token)
echo $KC_ACCESS_TOKEN
curl --silent -H "Authorization: Bearer $KC_ACCESS_TOKEN" http://localhost:3000/api/user
```

Out­put: 输出：

```json
{"type":"user","username":"vadmin","userId":"75090eac-36ff-4cd8-847d-fc2941bc024e","timestamp":"2020-09-28T21:13:05.099393900Z"}
curl --silent -H "Authorization: Bearer $KC_ACCESS_TOKEN" http://localhost:3000/api/admin
```

Out­put: 输出：

```json
{"type":"admin","username":"vadmin","userId":"75090eac-36ff-4cd8-847d-fc2941bc024e","timestamp":"2020-09-28T21:13:34.945276500Z"}
```

### Conclusion 结论

We learned how to con­fig­ure a Vert.x ap­pli­ca­tion with JWT au­tho­riza­tion pow­ered by Key­cloak. Al­though the con­fig­u­ra­tion is quite com­plete al­ready, there are still some parts that can be im­proved, like the dy­namic JWKS fetch­ing on public-key pair ro­ta­tion as well as ex­trac­tion of nested roles.
我们学习了如何使用由Keycloak提供支持的JWT授权来配置Vert.x应用程序。尽管配置已经相当完整，但仍有一些部分可以改进，例如公钥对轮换的动态 JWKS 获取以及嵌套角色的提取。

Nev­er­the­less this is a good start­ing point for se­cur­ing your own Vert.x ser­vices with JWT and Key­cloak.
尽管如此，这是使用 JWT 和 Keycloak 保护您自己的 Vert.x 服务的良好起点。

You can check out the [com­plete ex­am­ple in keycloak-vertx Ex­am­ples Repo](https://github.com/thomasdarimont/vertx-playground/tree/master/jwt-service-vertx).
您可以在 keycloak-vertx 示例存储库中查看完整示例。

Thank you for your time, stay tuned for more up­dates! If you want to learn more about Key­cloak, feel free to reach out to me. You can find me via [thomas­da­ri­mont on twit­ter](https://twitter.com/thomasdarimont).
感谢您抽出宝贵时间，请继续关注更多更新！如果您想了解有关Keycloak的更多信息，请随时与我联系。你可以通过twitter上的thomasdarimont找到我。

Happy Hack­ing! 祝黑客愉快！
