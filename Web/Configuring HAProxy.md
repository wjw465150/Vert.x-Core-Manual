# Configuring HAProxy

## option forwardfor

Enable insertion of the `X-Forwarded-For` header to requests sent to servers

```
option forwardfor [ except <network> ] [ header <name> ] [ if-none ]
```

**Arguments :**

```
<network> is an optional argument used to disable this option for sources matching <network>
<name>    an optional argument to specify a different "X-Forwarded-For" header name.
```

Since HAProxy works in reverse-proxy mode, the servers see its IP address astheir client address. This is sometimes annoying when the client's IP addressis expected in server logs. To solve this problem, the well-known HTTP header"X-Forwarded-For" may be added by HAProxy to all requests sent to the server.This header contains a value representing the client's IP address. Since thisheader is always appended at the end of the existing header list, the servermust be configured to always use the last occurrence of this header only. Seethe server's manual to find how to enable use of this standard header. Notethat only the last occurrence of the header must be used, since it is reallypossible that the client has already brought one.

The keyword "header" may be used to supply a different header name to replacethe default "X-Forwarded-For". This can be useful where you might alreadyhave a "X-Forwarded-For" header from a different application (e.g. stunnel),and you need preserve it. Also if your backend server doesn't use the"X-Forwarded-For" header and requires different one (e.g. Zeus Web Serversrequire "X-Cluster-Client-IP").

Sometimes, a same HAProxy instance may be shared between a direct clientaccess and a reverse-proxy access (for instance when an SSL reverse-proxy isused to decrypt HTTPS traffic). It is possible to disable the addition of theheader for a known source address or network by adding the "except" keywordfollowed by the network address. In this case, any source IP matching thenetwork will not cause an addition of this header. Most common uses are withprivate networks or 127.0.0.1. IPv4 and IPv6 are both supported.

Alternatively, the keyword "if-none" states that the header will only beadded if it is not present. This should only be used in perfectly trustedenvironment, as this might cause a security issue if headers reaching HAProxyare under the control of the end-user.

This option may be specified either in the frontend or in the backend. If atleast one of them uses it, the header will be added. Note that the backend'ssetting of the header subargument takes precedence over the frontend's ifboth are defined. In the case of the "if-none" argument, if at least one ofthe frontend or the backend does not specify it, it wants the addition to bemandatory, so it wins.

**Example :**

```
# Public HTTP address also used by stunnel on the same machine
frontend www
    mode http
    option forwardfor except 127.0.0.1  # stunnel already adds the header

# Those servers want the IP Address in X-Client
backend www
    mode http
    option forwardfor header X-Client
```

## option http-keep-alive

Enable or disable HTTP keep-alive from client to server

**Arguments :** none

By default HAProxy operates in keep-alive mode with regards to persistentconnections: for each connection it processes each request and response, andleaves the connection idle on both sides between the end of a response andthe start of a new request. This mode may be changed by several options suchas "option http-server-close" or "option httpclose". This option allows toset back the keep-alive mode, which can be useful when another mode was usedin a defaults section.

Setting "option http-keep-alive" enables HTTP keep-alive mode on the client-and server- sides. This provides the lowest latency on the client side (slownetwork) and the fastest session reuse on the server side at the expenseof maintaining idle connections to the servers. In general, it is possiblewith this option to achieve approximately twice the request rate that the"http-server-close" option achieves on small objects. There are mainly twosituations where this option may be useful :

  - when the server is non-HTTP compliant and authenticates the connection
    instead of requests (e.g. NTLM authentication)

  - when the cost of establishing the connection to the server is significant
    compared to the cost of retrieving the associated object from the server.

This last case can happen when the server is a fast static server of cache.In this case, the server will need to be properly tuned to support high enoughconnection counts because connections will last until the client sends anotherrequest.

If the client request has to go to another backend or another server due tocontent switching or the load balancing algorithm, the idle connection willimmediately be closed and a new one re-opened. Option "prefer-last-server" isavailable to try optimize server selection so that if the server currentlyattached to an idle connection is usable, it will be used.

At the moment, logs will not indicate whether requests came from the samesession or not. The accept date reported in the logs corresponds to the endof the previous request, and the request time corresponds to the time spentwaiting for a new request. The keep-alive request time is still bound to thetimeout defined by "timeout http-keep-alive" or "timeout http-request" ifnot set.

This option disables and replaces any previous "option httpclose" or "optionhttp-server-close". When backend and frontend options differ, all of these 4 options have precedence over "option http-keep-alive".

## option httpclose

Enable or disable HTTP connection closing

**Arguments :** none

When running with "option http-server-close" or "option httpclose", HAProxyadds a "Connection: close" header to the request forwarded to the server.Unfortunately, when some servers see this header, they automatically refrainfrom using the chunked encoding for responses of unknown length, while thisis totally unrelated. The immediate effect is that this prevents HAProxy frommaintaining the client connection alive. A second effect is that a client ora cache could receive an incomplete response without being aware of it, andconsider the response complete.

By setting "option http-pretend-keepalive", HAProxy will make the serverbelieve it will keep the connection alive. The server will then not fall backto the abnormal undesired above. When HAProxy gets the whole response, itwill close the connection with the server just as it would do with the"option httpclose". That way the client gets a normal response and theconnection is correctly closed on the server side.

It is recommended not to enable this option by default, because most serverswill more efficiently close the connection themselves after the last packet,and release its buffers slightly earlier. Also, the added packet on thenetwork could slightly reduce the overall peak performance. However it isworth noting that when this option is enabled, HAProxy will have slightlyless work to do. So if HAProxy is the bottleneck on the whole architecture,enabling this option might save a few CPU cycles.

This option may be set in backend and listen sections. Using it in a frontendsection will be ignored and a warning will be reported during startup. It isa backend related option, so there is no real reason to set it on afrontend. This option may be combined with "option httpclose", which willcause keepalive to be announced to the server and close to be announced tothe client. This practice is discouraged though.

If this option has been enabled in a "defaults" section, it can be disabledin a specific instance by prepending the "no" keyword before it.

## option http-server-close

Enable or disable HTTP connection closing on the server side

**Arguments :** none

By default HAProxy operates in keep-alive mode with regards to persistentconnections: for each connection it processes each request and response, andleaves the connection idle on both sides between the end of a response andthe start of a new request. This mode may be changed by several options suchas "option http-server-close" or "option httpclose". Setting "optionhttp-server-close" enables HTTP connection-close mode on the server sidewhile keeping the ability to support HTTP keep-alive and pipelining on theclient side. This provides the lowest latency on the client side (slownetwork) and the fastest session reuse on the server side to save serverresources, similarly to "option httpclose".  It also permits non-keepalivecapable servers to be served in keep-alive mode to the clients if theyconform to the requirements of RFC7230. Please note that some servers do notalways conform to those requirements when they see "Connection: close" in therequest. The effect will be that keep-alive will never be used. A workaroundconsists in enabling "option http-pretend-keepalive".

At the moment, logs will not indicate whether requests came from the samesession or not. The accept date reported in the logs corresponds to the endof the previous request, and the request time corresponds to the time spentwaiting for a new request. The keep-alive request time is still bound to thetimeout defined by "timeout http-keep-alive" or "timeout http-request" ifnot set.

This option may be set both in a frontend and in a backend. It is enabled ifat least one of the frontend or backend holding a connection has it enabled.It disables and replaces any previous "option httpclose" or "optionhttp-keep-alive". Please check section 4 ("Proxies") to see how this optioncombines with others when frontend and backend options differ.

If this option has been enabled in a "defaults" section, it can be disabledin a specific instance by prepending the "no" keyword before it.

## option http-no-delay

Instruct the system to favor low interactive delays over performance in HTTP

**Arguments :** none

In HTTP, each payload is unidirectional and has no notion of interactivity.Any agent is expected to queue data somewhat for a reasonably low delay.There are some very rare server-to-server applications that abuse the HTTPprotocol and expect the payload phase to be highly interactive, with manyinterleaved data chunks in both directions within a single request. This isabsolutely not supported by the HTTP specification and will not work acrossmost proxies or servers. When such applications attempt to do this throughHAProxy, it works but they will experience high delays due to the networkoptimizations which favor performance by instructing the system to wait forenough data to be available in order to only send full packets. Typicaldelays are around 200 ms per round trip. Note that this only happens withabnormal uses. Normal uses such as CONNECT requests nor WebSockets are notaffected.

When "option http-no-delay" is present in either the frontend or the backendused by a connection, all such optimizations will be disabled in order tomake the exchanges as fast as possible. Of course this offers no guarantee onthe functionality, as it may break at any other place. But if it works viaHAProxy, it will work as fast as possible. This option should never be usedby default, and should never be used at all unless such a buggy applicationis discovered. The impact of using this option is an increase of bandwidthusage and CPU usage, which may significantly lower performance in highlatency environments.

## option redispatch `<interval>`

当连接失败时，启用或禁用会话重分发

**Arguments :**

```
<interval> 可选的整数值，用于控制重试连接时重新发送的频率。 正值 P 表示在每 P 次重试时都需要重新分发，负值 N 表示在最后一次重试之前的第 N 次重试时需要重新分发。 例如，默认值 -1 保留在上次重试时重新调度的历史行为，正值 1 表示每次重试时重新调度，正值 3 表示每三次重试时重新调度。 您可以禁用值为 0 的重新调度。
```

在HTTP模式下，如果cookie指定的服务器宕机了，客户端可能会坚持使用它，因为他们无法刷新cookie，所以他们将无法再访问该服务。

指定“option redispatch”将允许代理打破基于持久性的cookie或一致散列，并将它们重新分发到工作的服务器。

活动服务器是从可用服务器列表的子集中选择的。未关闭或正在维护的活动服务器(即，其健康状况未被检查或已被检查为“up”)，将按以下顺序选择:
  1. 任何活动的非备份服务器(如果有的话)，或者,
  2. 如果未设置“allbackups”选项，则列表中的第一个备份服务器或
  3. 如果设置了“allbackups”选项，则任何备份服务器。

当重试发生时，HAProxy尝试选择另一个服务器而不是最后一个服务器。从当前服务器列表中选择新服务器。

有时，如果在重试之间更新列表(例如，如果多次重试并且持续的时间超过了检查服务器是否关闭所需的时间，则将其从列表中删除并回落到备份服务器列表中)，则连接可能会重定向到备份服务器。

它还允许在多重连接失败的情况下重试连接到另一个服务器。当然，这需要将“retries”设置为非零值。

如果此选项在“defaults”部分启用，则可以在特定实例中通过在其前面添加“no”关键字禁用它。
