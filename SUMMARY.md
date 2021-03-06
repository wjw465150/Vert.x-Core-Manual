# Summary

-----
* [前言](README.md)

-----
* [ VertX核心手册Java版](VertX核心手册Java版)
  * [开始创建Vert.x](VertX核心手册Java版.md#1___开始创建Vert_x)
    * [创建Vertx对象时指定选项](VertX核心手册Java版.md#2____创建Vertx对象时指定选项)
    * [创建集群的Vert.x对象](VertX核心手册Java版.md#3____创建集群的Vert_x对象)      
  * [你是链式的吗?](VertX核心手册Java版.md#4___你是链式的吗_)
  * [别打给我们，我们会打给你的。](VertX核心手册Java版.md#5___别打给我们，我们会打给你的。)
  * [不要阻塞我！](VertX核心手册Java版.md#6___不要阻塞我！)
  * [反应器和多反应器](VertX核心手册Java版.md#7___反应器和多反应器)
  * [黄金法则 - 不要阻塞事件循环](VertX核心手册Java版.md#8___黄金法则___不要阻塞事件循环)  
  * [运行阻塞的代码](VertX核心手册Java版.md#9___运行阻塞的代码)
  * [异步协调](VertX核心手册Java版.md#10___异步协调)
    * [并发组合](VertX核心手册Java版.md#11____并发组合)
    * [顺序组合](VertX核心手册Java版.md#12____顺序组合)
  * [Verticles](VertX核心手册Java版.md#13___Verticles)
    * [编写Verticles](VertX核心手册Java版.md#14____编写Verticles)
    * [异步Verticle启动和停止](VertX核心手册Java版.md#15____异步Verticle启动和停止)
    * [Verticle类型](VertX核心手册Java版.md#16____Verticle类型)
    * [Standard verticles](VertX核心手册Java版.md#17____Standard_verticles)
      * [多线程工作Verticles](VertX核心手册Java版.md#18_____多线程工作Verticles)
    * [以编程方式部署verticles](VertX核心手册Java版.md#19____以编程方式部署verticles)
    * [将verticle名称映射到verticle工厂的规则](VertX核心手册Java版.md#20____将verticle名称映射到verticle工厂的规则)
    * [Verticle工厂位于哪里?](VertX核心手册Java版.md#21____Verticle工厂位于哪里_)
    * [等待部署完成](VertX核心手册Java版.md#22____等待部署完成)
    * [取消verticle部署](VertX核心手册Java版.md#23____取消verticle部署)
    * [指定verticle实例数](VertX核心手册Java版.md#24____指定verticle实例数)
    * [将配置传递到verticle](VertX核心手册Java版.md#25____将配置传递到verticle)
    * [在Verticle中访问环境变量](VertX核心手册Java版.md#26____在Verticle中访问环境变量)
    * [Verticle隔离组](VertX核心手册Java版.md#27____Verticle隔离组)
    * [高可用性](VertX核心手册Java版.md#28____高可用性)
    * [从命令行运行Verticles](VertX核心手册Java版.md#29____从命令行运行Verticles)
    * [导致Vert.x退出](VertX核心手册Java版.md#30____导致Vert_x退出)
    * [上下文对象](VertX核心手册Java版.md#31____上下文对象)
    * [执行定期和延迟的操作](VertX核心手册Java版.md#32____执行定期和延迟的操作)
      * [单次计时器](VertX核心手册Java版.md#33_____单次计时器)
      * [周期性的计时器](VertX核心手册Java版.md#34_____周期性的计时器)
      * [取消计时器](VertX核心手册Java版.md#35_____取消计时器)
      * [verticles中的自动清理](VertX核心手册Java版.md#36_____verticles中的自动清理)
    * [Verticle工作池](VertX核心手册Java版.md#37____Verticle工作池)
  * [事件总线](VertX核心手册Java版.md#38___事件总线)
    * [理论](VertX核心手册Java版.md#39____理论)
      * [地址](VertX核心手册Java版.md#40_____地址)
      * [处理程序](VertX核心手册Java版.md#41_____处理程序)
      * [发布/订阅消息](VertX核心手册Java版.md#42_____发布_订阅消息)
      * [点对点和请求响应消息传递](VertX核心手册Java版.md#43_____点对点和请求响应消息传递)
      * [尽力递送](VertX核心手册Java版.md#44_____尽力递送)
      * [消息类型](VertX核心手册Java版.md#45_____消息类型)
    * [事件总线API](VertX核心手册Java版.md#46____事件总线API)
      * [获取事件总线](VertX核心手册Java版.md#47_____获取事件总线)
      * [注册处理程序](VertX核心手册Java版.md#48_____注册处理程序)
      * [取消注册处理程序](VertX核心手册Java版.md#49_____取消注册处理程序)
      * [发布消息](VertX核心手册Java版.md#50_____发布消息)
      * [发送消息](VertX核心手册Java版.md#51_____发送消息)
      * [在消息上设置标题](VertX核心手册Java版.md#52_____在消息上设置标题)
      * [消息顺序](VertX核心手册Java版.md#53_____消息顺序)
      * [消息对象](VertX核心手册Java版.md#54_____消息对象)
      * [确认消息/发送回复](VertX核心手册Java版.md#55_____确认消息_发送回复)
      * [发送与超时](VertX核心手册Java版.md#56_____发送与超时)
      * [发送失败](VertX核心手册Java版.md#57_____发送失败)
      * [消息的编解码器](VertX核心手册Java版.md#58_____消息的编解码器)
      * [集群事件总线](VertX核心手册Java版.md#59_____集群事件总线)
      * [以编程方式建立集群](VertX核心手册Java版.md#60_____以编程方式建立集群)
      * [在命令行上进行集群](VertX核心手册Java版.md#61_____在命令行上进行集群)
  * [配置事件总线](VertX核心手册Java版.md#62___配置事件总线)
  * [JSON](VertX核心手册Java版.md#63___JSON)
    * [JSON对象](VertX核心手册Java版.md#64____JSON对象)
      * [创建JSON对象](VertX核心手册Java版.md#65_____创建JSON对象)
      * [将条目放入JSON对象](VertX核心手册Java版.md#66_____将条目放入JSON对象)
      * [从JSON对象获取值](VertX核心手册Java版.md#67_____从JSON对象获取值)
      * [JSON对象和Java对象之间的映射](VertX核心手册Java版.md#68_____JSON对象和Java对象之间的映射)
      * [将JSON对象编码为字符串](VertX核心手册Java版.md#69_____将JSON对象编码为字符串)
    * [JSON 数组](VertX核心手册Java版.md#70____JSON_数组)
      * [创建JSON数组](VertX核心手册Java版.md#71_____创建JSON数组)
      * [将条目添加到JSON数组中](VertX核心手册Java版.md#72_____将条目添加到JSON数组中)
      * [从JSON数组获取值](VertX核心手册Java版.md#73_____从JSON数组获取值)
      * [将JSON数组编码为字符串](VertX核心手册Java版.md#74_____将JSON数组编码为字符串)
      * [创建任意JSON](VertX核心手册Java版.md#75_____创建任意JSON)
  * [JSON指针](VertX核心手册Java版.md#76___JSON指针)
  * [缓冲区](VertX核心手册Java版.md#77___缓冲区)
    * [创建缓冲区](VertX核心手册Java版.md#78____创建缓冲区)
    * [写入缓冲区](VertX核心手册Java版.md#79____写入缓冲区)
      * [追加到缓冲区](VertX核心手册Java版.md#80_____追加到缓冲区)
      * [随机存取缓冲区写入](VertX核心手册Java版.md#81_____随机存取缓冲区写入)
    * [从缓冲区读取](VertX核心手册Java版.md#82____从缓冲区读取)
    * [使用无符号数字](VertX核心手册Java版.md#83____使用无符号数字)
    * [缓冲区长度](VertX核心手册Java版.md#84____缓冲区长度)
    * [复制缓冲区](VertX核心手册Java版.md#85____复制缓冲区)
    * [切片缓冲区](VertX核心手册Java版.md#86____切片缓冲区)
    * [缓冲区重用](VertX核心手册Java版.md#87____缓冲区重用)
  * [编写TCP服务器和客户端](VertX核心手册Java版.md#88___编写TCP服务器和客户端)
    * [创建一个TCP服务器](VertX核心手册Java版.md#89____创建一个TCP服务器)
    * [配置TCP服务器](VertX核心手册Java版.md#90____配置TCP服务器)
    * [开始服务器监听](VertX核心手册Java版.md#91____开始服务器监听)
    * [在随机端口上监听](VertX核心手册Java版.md#92____在随机端口上监听)
    * [收到传入连接的通知](VertX核心手册Java版.md#93____收到传入连接的通知)
    * [从套接字读取数据](VertX核心手册Java版.md#94____从套接字读取数据)
    * [将数据写入套接字](VertX核心手册Java版.md#95____将数据写入套接字)
    * [关闭处理程序](VertX核心手册Java版.md#96____关闭处理程序)
    * [处理异常](VertX核心手册Java版.md#97____处理异常)
    * [事件总线写处理程序](VertX核心手册Java版.md#98____事件总线写处理程序)
    * [本地和远程地址](VertX核心手册Java版.md#99____本地和远程地址)
    * [从类路径发送文件或资源](VertX核心手册Java版.md#100____从类路径发送文件或资源)
    * [流式套接字](VertX核心手册Java版.md#101____流式套接字)
    * [将连接升级到SSL/TLS](VertX核心手册Java版.md#102____将连接升级到SSL_TLS)
    * [关闭TCP服务器](VertX核心手册Java版.md#103____关闭TCP服务器)
    * [自动清理的verticles](VertX核心手册Java版.md#104____自动清理的verticles)
    * [扩展-共享TCP服务器](VertX核心手册Java版.md#105____扩展_共享TCP服务器)
    * [创建一个TCP客户端](VertX核心手册Java版.md#106____创建一个TCP客户端)
    * [配置TCP客户端](VertX核心手册Java版.md#107____配置TCP客户端)
    * [建立连接](VertX核心手册Java版.md#108____建立连接)
    * [配置连接尝试](VertX核心手册Java版.md#109____配置连接尝试)
    * [记录网络活动](VertX核心手册Java版.md#110____记录网络活动)
    * [配置服务器和客户端以使用SSL/TLS](VertX核心手册Java版.md#111____配置服务器和客户端以使用SSL_TLS)
      * [在服务器上启用SSL/TLS](VertX核心手册Java版.md#112_____在服务器上启用SSL_TLS)
      * [指定服务器的密钥/证书](VertX核心手册Java版.md#113_____指定服务器的密钥_证书)
      * [指定服务器的信任](VertX核心手册Java版.md#114_____指定服务器的信任)
      * [在客户端上启用SSL/TLS](VertX核心手册Java版.md#115_____在客户端上启用SSL_TLS)
      * [客户端信任配置](VertX核心手册Java版.md#116_____客户端信任配置)
      * [指定客户端的密钥/证书](VertX核心手册Java版.md#117_____指定客户端的密钥_证书)
      * [用于测试和开发目的的自签名证书](VertX核心手册Java版.md#118_____用于测试和开发目的的自签名证书)
      * [吊销证书颁发机构](VertX核心手册Java版.md#119_____吊销证书颁发机构)
      * [配置密码套件](VertX核心手册Java版.md#120_____配置密码套件)
      * [配置TLS协议版本](VertX核心手册Java版.md#121_____配置TLS协议版本)
      * [SSL引擎](VertX核心手册Java版.md#122_____SSL引擎)
      * [服务器名称指示(SNI)](VertX核心手册Java版.md#123_____服务器名称指示_SNI_)
      * [应用层协议协商(ALPN)](VertX核心手册Java版.md#124_____应用层协议协商_ALPN_)
        * [OpenSSL ALPN支持](VertX核心手册Java版.md#125______OpenSSL_ALPN支持)
        * [Jetty-ALPN支持](VertX核心手册Java版.md#126______Jetty_ALPN支持)
    * [使用代理进行客户端连接](VertX核心手册Java版.md#127____使用代理进行客户端连接)
  * [编写HTTP服务器和客户端](VertX核心手册Java版.md#128___编写HTTP服务器和客户端)
    * [创建一个HTTP服务器](VertX核心手册Java版.md#129____创建一个HTTP服务器)
    * [配置HTTP服务器](VertX核心手册Java版.md#130____配置HTTP服务器)
    * [配置HTTP/2服务器](VertX核心手册Java版.md#131____配置HTTP_2服务器)
    * [记录网络服务器活动](VertX核心手册Java版.md#132____记录网络服务器活动)
    * [开始服务器监听](VertX核心手册Java版.md#133____开始服务器监听)
    * [收到传入请求的通知](VertX核心手册Java版.md#134____收到传入请求的通知)
    * [处理请求](VertX核心手册Java版.md#135____处理请求)
      * [Request 版本](VertX核心手册Java版.md#136_____Request_版本)
      * [Request 方法](VertX核心手册Java版.md#137_____Request_方法)
      * [Request URI](VertX核心手册Java版.md#138_____Request_URI)
      * [Request 路径](VertX核心手册Java版.md#139_____Request_路径)
      * [Request query](VertX核心手册Java版.md#140_____Request_query)
      * [Request 头](VertX核心手册Java版.md#141_____Request_头)
      * [Request host](VertX核心手册Java版.md#142_____Request_host)
      * [Request 参数](VertX核心手册Java版.md#143_____Request_参数)
      * [Remote 地址](VertX核心手册Java版.md#144_____Remote_地址)
      * [绝对 URI](VertX核心手册Java版.md#145_____绝对_URI)
      * [End handler](VertX核心手册Java版.md#146_____End_handler)
      * [从请求主体读取数据](VertX核心手册Java版.md#147_____从请求主体读取数据)
      * [泵送请求](VertX核心手册Java版.md#148_____泵送请求)
      * [处理HTML表单](VertX核心手册Java版.md#149_____处理HTML表单)
      * [处理表单文件上传](VertX核心手册Java版.md#150_____处理表单文件上传)
      * [处理 cookies](VertX核心手册Java版.md#151_____处理_cookies)
      * [处理压缩的主体](VertX核心手册Java版.md#152_____处理压缩的主体)
      * [接收自定义HTTP/2帧](VertX核心手册Java版.md#153_____接收自定义HTTP_2帧)
      * [非标准的HTTP方法](VertX核心手册Java版.md#154_____非标准的HTTP方法)
    * [发送回响应](VertX核心手册Java版.md#155____发送回响应)
      * [设置状态码和消息](VertX核心手册Java版.md#156_____设置状态码和消息)
      * [编写HTTP响应](VertX核心手册Java版.md#157_____编写HTTP响应)
      * [结束HTTP响应](VertX核心手册Java版.md#158_____结束HTTP响应)
      * [关闭基础连接](VertX核心手册Java版.md#159_____关闭基础连接)
      * [设置响应头](VertX核心手册Java版.md#160_____设置响应头)
      * [分块的HTTP响应和trailers](VertX核心手册Java版.md#161_____分块的HTTP响应和trailers)
      * [直接从磁盘或类路径提供文件](VertX核心手册Java版.md#162_____直接从磁盘或类路径提供文件)
      * [Pumping 响应](VertX核心手册Java版.md#163_____Pumping_响应)
      * [编写HTTP/2帧](VertX核心手册Java版.md#164_____编写HTTP_2帧)
      * [流重置](VertX核心手册Java版.md#165_____流重置)
      * [服务器推送](VertX核心手册Java版.md#166_____服务器推送)
      * [处理异常](VertX核心手册Java版.md#167_____处理异常)
    * [HTTP 压缩](VertX核心手册Java版.md#168____HTTP_压缩)
    * [创建一个HTTP客户端](VertX核心手册Java版.md#169____创建一个HTTP客户端)
    * [记录网络客户端活动](VertX核心手册Java版.md#170____记录网络客户端活动)
    * [发出请求](VertX核心手册Java版.md#171____发出请求)
      * [没有请求正文的简单请求](VertX核心手册Java版.md#172_____没有请求正文的简单请求)
      * [编写一般请求](VertX核心手册Java版.md#173_____编写一般请求)
      * [编写请求主体](VertX核心手册Java版.md#174_____编写请求主体)
      * [编写请求标头](VertX核心手册Java版.md#175_____编写请求标头)
      * [非标准HTTP方法](VertX核心手册Java版.md#176_____非标准HTTP方法)
      * [结束HTTP请求](VertX核心手册Java版.md#177_____结束HTTP请求)
      * [分块的HTTP请求](VertX核心手册Java版.md#178_____分块的HTTP请求)
      * [请求超时](VertX核心手册Java版.md#179_____请求超时)
      * [处理异常](VertX核心手册Java版.md#180_____处理异常)
      * [在客户端请求上指定处理程序](VertX核心手册Java版.md#181_____在客户端请求上指定处理程序)
      * [将请求用作流](VertX核心手册Java版.md#182_____将请求用作流)
      * [编写HTTP/2帧](VertX核心手册Java版.md#183_____编写HTTP_2帧)
      * [流重置](VertX核心手册Java版.md#184_____流重置)
    * [处理HTTP响应](VertX核心手册Java版.md#185____处理HTTP响应)
      * [将响应作为流使用](VertX核心手册Java版.md#186_____将响应作为流使用)
      * [响应头和trailers(尾部)](VertX核心手册Java版.md#187_____响应头和trailers_尾部_)
      * [读取请求正文](VertX核心手册Java版.md#188_____读取请求正文)
      * [响应结束处理程序](VertX核心手册Java版.md#189_____响应结束处理程序)
      * [从响应中读取Cookie](VertX核心手册Java版.md#190_____从响应中读取Cookie)
      * [30x重定向处理](VertX核心手册Java版.md#191_____30x重定向处理)
      * [100-继续处理](VertX核心手册Java版.md#192_____100_继续处理)
      * [客户端推送](VertX核心手册Java版.md#193_____客户端推送)
      * [接收自定义HTTP/2帧](VertX核心手册Java版.md#194_____接收自定义HTTP_2帧)
    * [在客户端上启用压缩](VertX核心手册Java版.md#195____在客户端上启用压缩)
    * [HTTP/1.x 连接池 和 保持活动状态](VertX核心手册Java版.md#196____HTTP_1_x_连接池_和_保持活动状态)
    * [HTTP/1.1 pipe-lining(流水线)](VertX核心手册Java版.md#197____HTTP_1_1_pipe_lining_流水线_)
    * [HTTP/2 多路复用](VertX核心手册Java版.md#198____HTTP_2_多路复用)
    * [HTTP 连接](VertX核心手册Java版.md#199____HTTP_连接)
      * [Server 连接](VertX核心手册Java版.md#200_____Server_连接)
      * [Client 连接](VertX核心手册Java版.md#201_____Client_连接)
      * [Connection 设置](VertX核心手册Java版.md#202_____Connection_设置)
      * [连接 ping](VertX核心手册Java版.md#203_____连接_ping)
      * [连接关闭并消失](VertX核心手册Java版.md#204_____连接关闭并消失)
      * [连接关闭](VertX核心手册Java版.md#205_____连接关闭)
    * [HttpClient 用法](VertX核心手册Java版.md#206____HttpClient_用法)
    * [Server 共享](VertX核心手册Java版.md#207____Server_共享)
    * [将HTTPS与Vert.x一起使用](VertX核心手册Java版.md#208____将HTTPS与Vert_x一起使用)
      * [服务器名称指示 (SNI)](VertX核心手册Java版.md#209_____服务器名称指示__SNI_)
    * [WebSockets](VertX核心手册Java版.md#210____WebSockets)
      * [服务器上的WebSocket](VertX核心手册Java版.md#211_____服务器上的WebSocket)
        * [WebSocket处理程序](VertX核心手册Java版.md#212______WebSocket处理程序)
        * [升级到WebSocket](VertX核心手册Java版.md#213______升级到WebSocket)
        * [服务器WebSocket](VertX核心手册Java版.md#214______服务器WebSocket)
      * [客户端上的WebSockets](VertX核心手册Java版.md#215_____客户端上的WebSockets)
      * [将消息写入WebSockets](VertX核心手册Java版.md#216_____将消息写入WebSockets)
      * [将frames写入WebSocket](VertX核心手册Java版.md#217_____将frames写入WebSocket)
      * [从WebSockets读取frames](VertX核心手册Java版.md#218_____从WebSockets读取frames)
      * [关闭 WebSockets](VertX核心手册Java版.md#219_____关闭_WebSockets)
      * [流式WebSocket](VertX核心手册Java版.md#220_____流式WebSocket)
    * [使用代理进行HTTP/HTTPS连接](VertX核心手册Java版.md#221____使用代理进行HTTP_HTTPS连接)
      * [其他协议的处理](VertX核心手册Java版.md#222_____其他协议的处理)
    * [verticles自动清理](VertX核心手册Java版.md#223____verticles自动清理)
  * [使用SharedData API](VertX核心手册Java版.md#224___使用SharedData_API)
    * [本地 maps](VertX核心手册Java版.md#225____本地_maps)
    * [异步 shared maps](VertX核心手册Java版.md#226____异步_shared_maps)
      * [将数据放入map](VertX核心手册Java版.md#227_____将数据放入map)
      * [从map获取数据](VertX核心手册Java版.md#228_____从map获取数据)
        * [其它 map 操作](VertX核心手册Java版.md#229______其它_map_操作)
    * [Asynchronous 锁](VertX核心手册Java版.md#230____Asynchronous_锁)
    * [异步计数器](VertX核心手册Java版.md#231____异步计数器)
  * [在Vert.x中使用文件系统](VertX核心手册Java版.md#232___在Vert_x中使用文件系统)
    * [Asynchronous 文件](VertX核心手册Java版.md#233____Asynchronous_文件)
      * [随机存取写入](VertX核心手册Java版.md#234_____随机存取写入)
      * [随机读取](VertX核心手册Java版.md#235_____随机读取)
      * [打开选项](VertX核心手册Java版.md#236_____打开选项)
      * [数据刷新到下面的储存](VertX核心手册Java版.md#237_____数据刷新到下面的储存)
      * [使用AsyncFile作为ReadStream和WriteStream](VertX核心手册Java版.md#238_____使用AsyncFile作为ReadStream和WriteStream)
      * [从类路径访问文件](VertX核心手册Java版.md#239_____从类路径访问文件)
      * [关闭一个AsyncFile](VertX核心手册Java版.md#240_____关闭一个AsyncFile)
  * [数据报套接字(UDP)](VertX核心手册Java版.md#241___数据报套接字_UDP_)
    * [创建一个DatagramSocket](VertX核心手册Java版.md#242____创建一个DatagramSocket)
    * [发送 Datagram packets](VertX核心手册Java版.md#243____发送_Datagram_packets)
    * [接收 Datagram packets](VertX核心手册Java版.md#244____接收_Datagram_packets)
    * [多播](VertX核心手册Java版.md#245____多播)
      * [发送多播数据包](VertX核心手册Java版.md#246_____发送多播数据包)
        * [接收多播数据包](VertX核心手册Java版.md#247______接收多播数据包)
        * [取消收听/离开多播组](VertX核心手册Java版.md#248______取消收听_离开多播组)
        * [阻塞多播](VertX核心手册Java版.md#249______阻塞多播)
      * [DatagramSocket 属性](VertX核心手册Java版.md#250_____DatagramSocket_属性)
      * [DatagramSocket本地地址](VertX核心手册Java版.md#251_____DatagramSocket本地地址)
      * [关闭DatagramSocket](VertX核心手册Java版.md#252_____关闭DatagramSocket)
  * [DNS 客户端](VertX核心手册Java版.md#253___DNS_客户端)
    * [查找](VertX核心手册Java版.md#254____查找)
    * [查找4](VertX核心手册Java版.md#255____查找4)
    * [查找6](VertX核心手册Java版.md#256____查找6)
    * [解析A](VertX核心手册Java版.md#257____解析A)
    * [解析AAAA](VertX核心手册Java版.md#258____解析AAAA)
    * [解析CNAME](VertX核心手册Java版.md#259____解析CNAME)
    * [解析MX](VertX核心手册Java版.md#260____解析MX)
    * [解析TXT](VertX核心手册Java版.md#261____解析TXT)
    * [解析NS](VertX核心手册Java版.md#262____解析NS)
    * [解析SRV](VertX核心手册Java版.md#263____解析SRV)
    * [解析PTR](VertX核心手册Java版.md#264____解析PTR)
    * [解析Lookup](VertX核心手册Java版.md#265____解析Lookup)
    * [错误处理](VertX核心手册Java版.md#266____错误处理)
    * [ReadStream](VertX核心手册Java版.md#267____ReadStream)
    * [WriteStream](VertX核心手册Java版.md#268____WriteStream)
    * [Pump(水泵)](VertX核心手册Java版.md#269____Pump_水泵_)
  * [记录解析器](VertX核心手册Java版.md#270___记录解析器)
  * [Json解析器](VertX核心手册Java版.md#271___Json解析器)
  * [线程安全](VertX核心手册Java版.md#272___线程安全)
  * [指标SPI](VertX核心手册Java版.md#273___指标SPI)
  * [OSGi](VertX核心手册Java版.md#274___OSGi)
  * ['vertx'命令行](VertX核心手册Java版.md#275____vertx_命令行)
    * [运行 verticles](VertX核心手册Java版.md#276____运行_verticles)
    * [执行打包为fat jar的Vert.x应用程序](VertX核心手册Java版.md#277____执行打包为fat_jar的Vert_x应用程序)
    * [显示Vert.x的版本](VertX核心手册Java版.md#278____显示Vert_x的版本)
    * [其他命令](VertX核心手册Java版.md#279____其他命令)
    * [实时重新部署](VertX核心手册Java版.md#280____实时重新部署)
  * [集群管理器](VertX核心手册Java版.md#281___集群管理器)
  * [日志记录](VertX核心手册Java版.md#282___日志记录)
    * [配置 JUL 日志](VertX核心手册Java版.md#283____配置_JUL_日志)
    * [使用其它的日志框架](VertX核心手册Java版.md#284____使用其它的日志框架)
    * [Netty 日志](VertX核心手册Java版.md#285____Netty_日志)
    * [故障排除](VertX核心手册Java版.md#286____故障排除)
      * [启动时的SLF4J警告](VertX核心手册Java版.md#287_____启动时的SLF4J警告)
      * [对等连接重置](VertX核心手册Java版.md#288_____对等连接重置)
  * [主机名解析](VertX核心手册Java版.md#289___主机名解析)
    * [故障转移](VertX核心手册Java版.md#290____故障转移)
    * [服务器列表轮换](VertX核心手册Java版.md#291____服务器列表轮换)
    * [主机映射](VertX核心手册Java版.md#292____主机映射)
    * [搜索域](VertX核心手册Java版.md#293____搜索域)
  * [高可用性和故障转移](VertX核心手册Java版.md#294___高可用性和故障转移)
    * [自动故障转移](VertX核心手册Java版.md#295____自动故障转移)
    * [HA 组](VertX核心手册Java版.md#296____HA_组)
    * [处理网络分区-Quora](VertX核心手册Java版.md#297____处理网络分区_Quora)
  * [本地传输](VertX核心手册Java版.md#298___本地传输)
    * [本地Linux传输](VertX核心手册Java版.md#299____本地Linux传输)
    * [本地BSDLinuxNative](VertX核心手册Java版.md#300____本地BSDLinuxNative)
    * [域套接字](VertX核心手册Java版.md#301____域套接字)
  * [安全提示](VertX核心手册Java版.md#302___安全提示)
    * [Web 应用程序](VertX核心手册Java版.md#303____Web_应用程序)
    * [集群事件总线流量](VertX核心手册Java版.md#304____集群事件总线流量)
    * [标准安全最佳做法](VertX核心手册Java版.md#305____标准安全最佳做法)
  * [Vert.x命令行界面API](VertX核心手册Java版.md#306___Vert_x命令行界面API)
    * [定义阶段](VertX核心手册Java版.md#307____定义阶段)
      * [选项](VertX核心手册Java版.md#308_____选项)
      * [参数](VertX核心手册Java版.md#309_____参数)
      * [用法生成](VertX核心手册Java版.md#310_____用法生成)
    * [解析阶段](VertX核心手册Java版.md#311____解析阶段)
    * [查询/讯问阶段](VertX核心手册Java版.md#312____查询_讯问阶段)
    * [类型化选项和参数](VertX核心手册Java版.md#313____类型化选项和参数)
    * [使用注解](VertX核心手册Java版.md#314____使用注解)
  * [vert.x启动器](VertX核心手册Java版.md#315___vert_x启动器)
    * [扩展vert.x启动器](VertX核心手册Java版.md#316____扩展vert_x启动器)
    * [在fat jars中使用启动器](VertX核心手册Java版.md#317____在fat_jars中使用启动器)
    * [对启动器进行子类化](VertX核心手册Java版.md#318____对启动器进行子类化)
    * [启动器和退出代码](VertX核心手册Java版.md#319____启动器和退出代码)
  * [配置Vert.x缓存](VertX核心手册Java版.md#320___配置Vert_x缓存)
