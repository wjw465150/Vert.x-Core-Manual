# Vert.x Kafka 客户端

该组件提供了 Kafka 客户端， 可以用与给 [Apache Kafka](https://kafka.apache.org/) 集群发送信息，或从中读取信息。

作为消费者，接口提供了订阅主题分区，并异步地 接收消息，或将消息作为流进行处理（甚至可以做到中止或重启数据流）的方法。

作为生产者，接口提供了流式向主题分区发送消息的方法。

## 使用 Vert.x 的 Kafka 客户端

为了使用该组件， 需要在您的构建描述文件中的依赖配置中添加如下内容：

- Maven (在您的 `pom.xml`):

```xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-kafka-client</artifactId>
 <version>4.3.5</version>
</dependency>
```

- Gradle (在您的 `build.gradle` 文件中):

```groovy
compile io.vertx:vertx-kafka-client:4.3.5
```

## 创建 kafka 客户端

创建 kafka 的消费者和生产者的方式非常详细，它们都基于原生的 kafka 的客户端库工作。

在创建时，需要进行很多配置，这些配置可以参考 Apache Kafka 文档， 参见 [消费者](https://kafka.apache.org/documentation/#newconsumerconfigs) 和 [生产者](https://kafka.apache.org/documentation/#producerconfigs).

为了方便配置， 您可以将参数放置在一个 Map 容器中，并在调用 `KafkaConsumer` 和 `KafkaProducer` 的静态创建方法时传入。

```java
Map<String, String> config = new HashMap<>();
config.put("bootstrap.servers", "localhost:9092");
config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
config.put("group.id", "my_group");
config.put("auto.offset.reset", "earliest");
config.put("enable.auto.commit", "false");

// 使用消费者和 Apache Kafka 交互
KafkaConsumer<String, String> consumer = KafkaConsumer.create(vertx, config);
```

在以上代码中，我们传入了一个 Map 容器实例作为创建 `KafkaConsumer` 对象实例时 的参数，这样可以指定要连接的 kafka 节点列表（这里只有一个）的地址和 每个接收到的消息的键和内容的反序列化器。

创建 kafka 生产者地方法也大致相同。

```java
Map<String, String> config = new HashMap<>();
config.put("bootstrap.servers", "localhost:9092");
config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
config.put("acks", "1");

// 使用生产者和 Apache Kafka 交互
KafkaProducer<String, String> producer = KafkaProducer.create(vertx, config);
```

> **📝注意:** 创建 `KafkaConsumer` 的事件循环将是处理其消息的事件循环。 例如，如果您希望在 Verticle 的事件循环上处理消息，请在 Verticle 的 start 方法中创建 Kafka Consumer。

## 加入一个消费者群组并从主题中接收消息

要开始从 kafka 的主题中接收消息， 消费者需要使用 `subscribe` 方法去 作为一个消费者群组（群组在创建时的属性设置里指定）的一员去订阅一组主题。

您也可以使用 `subscribe` 方法去 指定一个正则表达式，并订阅所有匹配该正则表达式的主题。

为了注册一个处理器去处理接收到的消息，您需要使用 `handler` 方法。

```java
consumer.handler(record -> {
  System.out.println("Processing key=" + record.key() + ",value=" + record.value() +
    ",partition=" + record.partition() + ",offset=" + record.offset());
});

// 订阅一组主题
Set<String> topics = new HashSet<>();
topics.add("topic1");
topics.add("topic2");
topics.add("topic3");
consumer.subscribe(topics);

// 或使用正则表达式
Pattern pattern = Pattern.compile("topic\\d");
consumer.subscribe(pattern);

// 或仅订阅一个主题
consumer.subscribe("a-single-topic");
```

您可以在调用 `subscribe()` 方法的前后注册消息处理器； 直到您调用了该方法并注册了消息处理器后，消息才会 开始被消费。 举个例子，您可以先调用 `subscribe()` 方法，再调用 `seek()` 方法，最后调用 `handler()` 方法 ，这样您可以在一个特定的偏移处开始消费消息。

消息处理器也可以在订阅时注册，这样您就可以获取订阅的结果并当操作完成时 收到通知。

```java
consumer.handler(record -> {
  System.out.println("Processing key=" + record.key() + ",value=" + record.value() +
    ",partition=" + record.partition() + ",offset=" + record.offset());
});

// 订阅一组主题
Set<String> topics = new HashSet<>();
topics.add("topic1");
topics.add("topic2");
topics.add("topic3");
consumer
  .subscribe(topics)
  .onSuccess(v ->
    System.out.println("subscribed")
  ).onFailure(cause ->
    System.out.println("Could not subscribe " + cause.getMessage())
  );

// 或仅订阅一个主题
consumer
  .subscribe("a-single-topic")
  .onSuccess(v ->
    System.out.println("subscribed")
  ).onFailure(cause ->
    System.out.println("Could not subscribe " + cause.getMessage())
  );
```

通过使用消费者群组，Kafka 集群会将同一个消费者群组下的其他消费者正在使用的分区 分配给该消费者， 因此分区可以在消费者群组中传播。

Kafka 集群会在消费者离开集群时（此时原消费者的分区可以分配给其他消费者）或 新的消费者加入集群时（新消费者的需要申请分区来读取）重新平衡分区。

您可以给 `KafkaConsumer` 注册一个处理器，这样 会在 kafka 集群给消费者分配或撤回主题分区时收到通知，使用 `partitionsRevokedHandler` 和 `partitionsAssignedHandler` 方法注册该处理器。

```java
consumer.handler(record -> {
  System.out.println("Processing key=" + record.key() + ",value=" + record.value() +
    ",partition=" + record.partition() + ",offset=" + record.offset());
});

// 注册主题分区撤回和分配的处理器
consumer.partitionsAssignedHandler(topicPartitions -> {
  System.out.println("Partitions assigned");
  for (TopicPartition topicPartition : topicPartitions) {
    System.out.println(topicPartition.getTopic() + " " + topicPartition.getPartition());
  }
});

consumer.partitionsRevokedHandler(topicPartitions -> {
  System.out.println("Partitions revoked");
  for (TopicPartition topicPartition : topicPartitions) {
    System.out.println(topicPartition.getTopic() + " " + topicPartition.getPartition());
  }
});

// 订阅主题
consumer
  .subscribe("test")
  .onSuccess(v ->
    System.out.println("subscribed")
  ).onFailure(cause ->
    System.out.println("Could not subscribe " + cause.getMessage())
  );
```

在加入一个消费者群组接收消息后， 消费者可以选择使用 `unsubscribe` 方法 离开群组，这样就不会再收到消息

```java
consumer.unsubscribe();
```

您还可以设置一个处理器来处理退出的结果

```java
consumer
  .unsubscribe()
  .onSuccess(v ->
    System.out.println("Consumer unsubscribed")
  );
```

## 请求指定主题分区以接收消息

在接收消息时，除了加入消费者群组， 消费者也可以主动请求一个 特定的主题分区。 当消费者并不在一个消费者群组内， 那么应用就不能 依赖 kafka 的重平衡特性。

您可以使用 `assign` 方法 去请求特定的分区。

```java
consumer.handler(record -> {
  System.out.println("key=" + record.key() + ",value=" + record.value() +
    ",partition=" + record.partition() + ",offset=" + record.offset());
});

//
Set<TopicPartition> topicPartitions = new HashSet<>();
topicPartitions.add(new TopicPartition()
  .setTopic("test")
  .setPartition(0));

// 请求分配特定的分区
consumer
  .assign(topicPartitions)
  .onSuccess(v -> System.out.println("Partition assigned"))
  // 成功后会从该分区获取消息
  .compose(v -> consumer.assignment())
  .onSuccess(partitions -> {
    for (TopicPartition topicPartition : partitions) {
      System.out.println(topicPartition.getTopic() + " " + topicPartition.getPartition());
    }
  });
```

使用 `subscribe()` 方法时， 您可以在调用 `assign()` 方法之前或之后注册接收消息处理器； 因为消息只会在两个方法都生效后才会被消费。 举个例子，您可以先调用 `assign()` 方法， 再调用 `seek()` 方法，最后调用 `handler()` 方法， 这样您就可以只消费特定分区的指定偏移之后的消息。

调用 `assignment` 可以让您 获取当前分配的消息分区。

## 通过显式请求获取消息

为了从 Kafka 接收消息，除了使用客户端内部自带的请求机制外， 客户端可以订阅 主题， 并且不注册消息处理器，并使用 `poll` 方法获取消息。

通过这种方式， 用户的应用可以在其需要时才执行请求以获取消息， 举个例子。

```java
consumer
  .subscribe("test")
  .onSuccess(v -> {
    System.out.println("Consumer subscribed");

    // 每秒请求一次
    vertx.setPeriodic(1000, timerId ->
      consumer
        .poll(Duration.ofMillis(100))
        .onSuccess(records -> {
          for (int i = 0; i < records.size(); i++) {
            KafkaConsumerRecord<String, String> record = records.recordAt(i);
            System.out.println("key=" + record.key() + ",value=" + record.value() +
              ",partition=" + record.partition() + ",offset=" + record.offset());
          }
        })
        .onFailure(cause -> {
          System.out.println("Something went wrong when polling " + cause.toString());
          cause.printStackTrace();

          // 当发生错误时停止请求
          vertx.cancelTimer(timerId);
        })
    );
});
```

订阅成功后， 应用启动了一个定时器来执行请求并且 周期性地从 kafka 获取消息。

## 改变订阅或主题分区的分配

您可以在开始消费消息之后修改订阅的主题或主题分区的分配，只需要 重新调用 `subscribe()` 方法或 `assign()` 方法。

请记住，由于 kafka 客户端的内部存在消息缓存， 因此很有可能在您 调用 `subscribe()` 方法或 `assign()` 方法 *之后* ，原先的消息处理器仍然 收到了旧的主题或分区的消息。 但是如果您使用了批处理器就不会发生这种情况： 一旦重新调用订阅或修改方法的完成回调被触发， 那么客户端就只会收到新的主题或分区的消息。

## 获取主题分区信息

您可以调用 `partitionsFor` 方法来获取 特定主题的分区信息。

```java
consumer
  .partitionsFor("test")
  .onSuccess(partitions -> {
    for (PartitionInfo partitionInfo : partitions) {
      System.out.println(partitionInfo);
    }
  });
```

您也可以调用 `listTopics` 方法获取所有当前主题的 分区信息。

```java
consumer
  .listTopics()
  .onSuccess(partitionsTopicMap ->
    partitionsTopicMap.forEach((topic, partitions) -> {
      System.out.println("topic = " + topic);
      System.out.println("partitions = " + partitions);
    })
  );
```

## 手动提交偏移

Apache Kafka 的消费者一般会处理最后一个读取的消息的偏移。

一般情况下，kafka 的客户端会自动地在每次从主题分区获取一批消息 后通过提交操作处理。 配置参数 `enable.auto.commit` 会在客户端被创建时设置 为 `true` 。

手动提交偏移，可以使用 `commit` 方法。 这样可以确保 *至少一次* 提交偏移前消息已经被 处理了。

```java
consumer.commit().onSuccess(v ->
  System.out.println("Last read message offset committed")
);
```

## 在消息分区内查询

Apache Kafka 可以保存一段时间内的消息数据，并且消费者可以在消息分区内查询 并获取任意一条消息。

您可以使用 `seek` 方法来改变读取时的偏移，并移动到 特定的位置

```java
TopicPartition topicPartition = new TopicPartition()
  .setTopic("test")
  .setPartition(0);

// 移动特定的偏移
consumer
  .seek(topicPartition, 10)
  .onSuccess(v -> System.out.println("Seeking done"));
```

当消费者需要从开始处重新获取消息时，可以使用 `seekToBeginning`

```java
TopicPartition topicPartition = new TopicPartition()
  .setTopic("test")
  .setPartition(0);

// 移动偏移到分区开头
consumer
  .seekToBeginning(Collections.singleton(topicPartition))
  .onSuccess(v -> System.out.println("Seeking done"));
```

最后，`seekToEnd` 可以用于将偏移移动到分区的结尾

```java
TopicPartition topicPartition = new TopicPartition()
  .setTopic("test")
  .setPartition(0);

// 移动偏移到分区末尾
consumer
  .seekToEnd(Collections.singleton(topicPartition))
  .onSuccess(v -> System.out.println("Seeking done"));
```

请记住，由于 kafka 客户端的内部存在消息缓存， 因此很有可能在您 调用完 `seek*()` 方法 *之后* 原有的消息处理器仍在获取原先 偏移处的消息。 但是如果您使用了批处理器就不会发生这种情况： 一旦 `seek*()` 的完成回调被触发， 消息处理器就只会接收到新的偏移处的消息。

## 查询偏移

您可以使用在 Kafka 0.10.1.1 引入的 beginningOffsets 接口来获取指定分区的 第一个偏移。 与 `seekToBeginning` 方法不同的是， 该接口并不会改变当前客户端的偏移。

```java
Set<TopicPartition> topicPartitions = new HashSet<>();
TopicPartition topicPartition = new TopicPartition().setTopic("test").setPartition(0);
topicPartitions.add(topicPartition);

consumer
  .beginningOffsets(topicPartitions)
  .onSuccess(results ->
    results.forEach((topic, beginningOffset) ->
      System.out.println(
        "Beginning offset for topic=" + topic.getTopic() + ", partition=" +
          topic.getPartition() + ", beginningOffset=" + beginningOffset
      )
    )
  );

// 方便地获取一个分区的偏移
consumer
  .beginningOffsets(topicPartition)
  .onSuccess(beginningOffset ->
    System.out.println(
      "Beginning offset for topic=" + topicPartition.getTopic() + ", partition=" +
        topicPartition.getPartition() + ", beginningOffset=" + beginningOffset
    )
  );
```

您可以使用在 Kafka 0.10.1.1 引入的 endOffsets 接口来获取指定分区的 结尾偏移。 与 `seekToEnd` 方法不同的是， 该接口并不会改变当前客户端的偏移。

```java
Set<TopicPartition> topicPartitions = new HashSet<>();
TopicPartition topicPartition = new TopicPartition().setTopic("test").setPartition(0);
topicPartitions.add(topicPartition);

consumer.endOffsets(topicPartitions)
  .onSuccess(results ->
    results.forEach((topic, beginningOffset) ->
      System.out.println(
        "End offset for topic=" + topic.getTopic() + ", partition=" +
          topic.getPartition() + ", beginningOffset=" + beginningOffset
      )
    )
  );

// 方便地获取一个分区的偏移
consumer
  .endOffsets(topicPartition)
  .onSuccess(endOffset ->
    System.out.println(
      "End offset for topic=" + topicPartition.getTopic() + ", partition=" +
        topicPartition.getPartition() + ", endOffset=" + endOffset
    )
);
```

您可以使用在 Kafka 0.10.1.1 引入的 endOffsets 接口来根据时间戳获取指定分区的 偏移。查询参数是一个 unix 时间戳，而返回的结果是满足 摄入时间 >= 给定时间条件的最小偏移。

```java
Map<TopicPartition, Long> topicPartitionsWithTimestamps = new HashMap<>();
TopicPartition topicPartition = new TopicPartition().setTopic("test").setPartition(0);

// 我们想知道 60 秒前摄入消息的偏移
long timestamp = (System.currentTimeMillis() - 60000);

topicPartitionsWithTimestamps.put(topicPartition, timestamp);
consumer
  .offsetsForTimes(topicPartitionsWithTimestamps)
  .onSuccess(results ->
    results.forEach((topic, offset) ->
      System.out.println(
        "Offset for topic=" + topic.getTopic() +
        ", partition=" + topic.getPartition() + "\n" +
        ", timestamp=" + timestamp + ", offset=" + offset.getOffset() +
        ", offsetTimestamp=" + offset.getTimestamp()
      )
    )
);

// 方便地获取一个分区的偏移
consumer.offsetsForTimes(topicPartition, timestamp).onSuccess(offsetAndTimestamp ->
  System.out.println(
    "Offset for topic=" + topicPartition.getTopic() +
    ", partition=" + topicPartition.getPartition() + "\n" +
    ", timestamp=" + timestamp + ", offset=" + offsetAndTimestamp.getOffset() +
    ", offsetTimestamp=" + offsetAndTimestamp.getTimestamp()
  )
);
```

## 消息流控制

kafka 的消费者可以控制消息的流入，并且暂停 / 重启从一个主题中读取消息的操作。当消费者需要 更多时间去处理当前消息时，它可以暂停消息流，它也可以重启消息流 去继续处理消息。

为了这么做，您可以使用 `pause` 方法和 `resume` 方法。

在对特定的主题分区调用了暂停和重启方法后，消息处理器有可能仍然会从 已经暂停了的主题分区接收消息，即使是在 `pause()` 方法的完成回调 *已经被调用之后* 。 如果您使用了批处理器，一旦您调用 `pause()` 方法的完成回调被调用， 消费者就只能从未被暂停的主题分区 接收消息。

```java
TopicPartition topicPartition = new TopicPartition()
  .setTopic("test")
  .setPartition(0);

// 注册消息处理器
consumer.handler(record -> {
  System.out.println("key=" + record.key() + ",value=" + record.value() +
    ",partition=" + record.partition() + ",offset=" + record.offset());

  // 在接收消息的偏移到达 5 之后暂停 / 重启分区 0 的消息流
  if ((record.partition() == 0) && (record.offset() == 5)) {

    // pause the read operations
    consumer.pause(topicPartition)
      .onSuccess(v -> System.out.println("Paused"))
      .onSuccess(v -> vertx.setTimer(5000, timeId ->
        // 重启读操作
        consumer.resume(topicPartition)
      ));
  }
});
```

## 关闭消费者

调用 close 方法来关闭消费者。 关闭消费者会关闭其所持有的所有连接并释放它所有的消费者资源。

close 方法是异步的并且在方法返回时可能还未完成。 如果您想在关闭完成后 收到通知，那么可以向其传递一个回调处理器。

该回调处理器会在关闭操作完全完成后被调用。

```java
consumer
  .close()
  .onSuccess(v -> System.out.println("Consumer is now closed"))
  .onFailure(cause -> System.out.println("Close failed: " + cause));
```

## 向主题发送消息

您可以使用 `write` 方法去发送消息 (记录) 给主题。

最简单的发送消息的方法是指定目标主题和相对应的值， 忽略它的键 和分区，这种情况下消息会以轮流循环的方式呗发送给该主题的所有分区。

```java
for (int i = 0; i < 5; i++) {

  // 只设置主题和消息内容的情况下，消息会被循环轮流发送给目的主题的所有分区
  KafkaProducerRecord<String, String> record =
    KafkaProducerRecord.create("test", "message_" + i);

  producer.write(record);
}
```

在发送消息成功时，您可以接收到该消息在 kafka 中的元数据，例如它的主题，目标分区和它在存储中的偏移。

```java
for (int i = 0; i < 5; i++) {

  // 只设置主题和消息内容的情况下，消息会被循环轮流发送给目的主题的所有分区
  KafkaProducerRecord<String, String> record =
    KafkaProducerRecord.create("test", "message_" + i);

  producer.send(record).onSuccess(recordMetadata ->
    System.out.println(
      "Message " + record.value() + " written on topic=" + recordMetadata.getTopic() +
      ", partition=" + recordMetadata.getPartition() +
      ", offset=" + recordMetadata.getOffset()
    )
  );
}
```

当您需要指定消息发送的分区时，您需要指定它的分区标识符或 消息的键。

```java
for (int i = 0; i < 10; i++) {

  // 指定分区
  KafkaProducerRecord<String, String> record =
    KafkaProducerRecord.create("test", null, "message_" + i, 0);

  producer.write(record);
}
```

由于消息的生产者使用键的哈希计算对应的主题分区，您可以利用这一点保证拥有相同键的所有 消息都按照顺序被发送给一个相同的分区。

```java
for (int i = 0; i < 10; i++) {

  // 根据奇偶性设置消息的键
  int key = i % 2;

  // 指定一个消息的键，所有键相同的消息会被发给同一个分区
  KafkaProducerRecord<String, String> record =
    KafkaProducerRecord.create("test", String.valueOf(key), "message_" + i);

  producer.write(record);
}
```

> <mark>**请记住:**</mark>可共用的生产者通过 `createShared` 方法的第一次调用创建，并且它的配置在此时被设置， 可共用的生产者使用时必须确保配置相同。

## 公用生产者

有时您需要在多个 verticle 或上下文（context）中共享同一个生产者。

使用 `KafkaProducer.createShared` 方法 返回一个可以被安全地共用的 producer。

```java
KafkaProducer<String, String> producer1 = KafkaProducer.createShared(vertx, "the-producer", config);

// 之后您可以关闭它
producer1.close();
```

通过该方法返回的生产者会共享相同的资源（线程，连接） 。

当您使用完毕该生产者后，可以简单地关闭它。 当所有共用的生产者被关闭后，所有的资源 也会被释放。

## 关闭生产者

调用 close 方法来关闭生产者。关闭生产者会关闭其打开的连接并释放其所占有的所有资源。

关闭是异步进行的，因此在调用返回时生产者可能还没有完全关闭。 如果您想在 关闭完成时收到通知，那么您可以传入一个回调。

这个回调会在生产者被完全关闭后调用。

```java
producer
  .close()
  .onSuccess(v -> System.out.println("Producer is now closed"))
  .onFailure(cause -> System.out.println("Close failed: " + cause));
```

## 获取主题分片信息

您可以调用 `partitionsFor` 方法来获取 指定主题的分片信息：

```java
producer
  .partitionsFor("test")
  .onSuccess(partitions ->
    partitions.forEach(System.out::println)
  );
```

## 处理错误

kafka 客户端（消费者或生产者）和 kafka 集群间的异常处理 (例如连接超时) 需要用到 `exceptionHandler` 方法或 `exceptionHandler` 方法

```java
consumer.exceptionHandler(e -> {
  System.out.println("Error = " + e.getMessage());
});
```

## verticle 的自动清理

如果您是在 verticle 中创建 kafka 的消费者和生产者的，那么这些消费者和生产者会在 该 verticle 被取消部署时被自动清理。

## 使用 Vert.x 的序列化器 / 反序列化器

Vert.x 的 Kafka 客户端的实现自带了对 Buffer 数据类型， json 对象 和 json 对象数组的序列化器和反序列化器的包装。

使用消费者时您可以直接接收 Buffer 数据类型

```java
Map<String, String> config = new HashMap<>();
config.put("bootstrap.servers", "localhost:9092");
config.put("key.deserializer", "io.vertx.kafka.client.serialization.BufferDeserializer");
config.put("value.deserializer", "io.vertx.kafka.client.serialization.BufferDeserializer");
config.put("group.id", "my_group");
config.put("auto.offset.reset", "earliest");
config.put("enable.auto.commit", "false");

// 创建一个可以反序列化 json 对象的消费者
config = new HashMap<>();
config.put("bootstrap.servers", "localhost:9092");
config.put("key.deserializer", "io.vertx.kafka.client.serialization.JsonObjectDeserializer");
config.put("value.deserializer", "io.vertx.kafka.client.serialization.JsonObjectDeserializer");
config.put("group.id", "my_group");
config.put("auto.offset.reset", "earliest");
config.put("enable.auto.commit", "false");

// 创建一个可以反序列化 json 对象数组的消费者
config = new HashMap<>();
config.put("bootstrap.servers", "localhost:9092");
config.put("key.deserializer", "io.vertx.kafka.client.serialization.JsonArrayDeserializer");
config.put("value.deserializer", "io.vertx.kafka.client.serialization.JsonArrayDeserializer");
config.put("group.id", "my_group");
config.put("auto.offset.reset", "earliest");
config.put("enable.auto.commit", "false");
```

在生产者端，您也可以这么做

```java
Map<String, String> config = new HashMap<>();
config.put("bootstrap.servers", "localhost:9092");
config.put("key.serializer", "io.vertx.kafka.client.serialization.BufferSerializer");
config.put("value.serializer", "io.vertx.kafka.client.serialization.BufferSerializer");
config.put("acks", "1");

// 创建一个可以序列化 json 对象的生产者
config = new HashMap<>();
config.put("bootstrap.servers", "localhost:9092");
config.put("key.serializer", "io.vertx.kafka.client.serialization.JsonObjectSerializer");
config.put("value.serializer", "io.vertx.kafka.client.serialization.JsonObjectSerializer");
config.put("acks", "1");

// 创建一个可以序列化 json 对象数组的生产者
config = new HashMap<>();
config.put("bootstrap.servers", "localhost:9092");
config.put("key.serializer", "io.vertx.kafka.client.serialization.JsonArraySerializer");
config.put("value.serializer", "io.vertx.kafka.client.serialization.JsonArraySerializer");
config.put("acks", "1");
```

您可以在创建时直接指定序列化器/反序列化器：

对于消费者

```java
Map<String, String> config = new HashMap<>();
config.put("bootstrap.servers", "localhost:9092");
config.put("group.id", "my_group");
config.put("auto.offset.reset", "earliest");
config.put("enable.auto.commit", "false");

// 创建一个可以反序列化 Buffer 数据类型的消费者
KafkaConsumer<Buffer, Buffer> bufferConsumer = KafkaConsumer.create(vertx, config, Buffer.class, Buffer.class);

// 创建一个可以反序列化 json 对象的消费者
KafkaConsumer<JsonObject, JsonObject> jsonObjectConsumer = KafkaConsumer.create(vertx, config, JsonObject.class, JsonObject.class);

// 创建一个可以反序列化 json 对象数组的消费者
KafkaConsumer<JsonArray, JsonArray> jsonArrayConsumer = KafkaConsumer.create(vertx, config, JsonArray.class, JsonArray.class);
```

而对于生产者

```java
Map<String, String> config = new HashMap<>();
config.put("bootstrap.servers", "localhost:9092");
config.put("acks", "1");

// 创建一个可以序列化 Buffer 数据类型的生产者
KafkaProducer<Buffer, Buffer> bufferProducer = KafkaProducer.create(vertx, config, Buffer.class, Buffer.class);

// 创建一个可以序列化 json 对象的生产者
KafkaProducer<JsonObject, JsonObject> jsonObjectProducer = KafkaProducer.create(vertx, config, JsonObject.class, JsonObject.class);

// 创建一个可以序列化 json 对象数组的生产者
KafkaProducer<JsonArray, JsonArray> jsonArrayProducer = KafkaProducer.create(vertx, config, JsonArray.class, JsonArray.class);
```

## RxJava 3 接口

Kafka 客户端提供了在原有 API 基础上的响应式接口

```java
Observable<KafkaConsumerRecord<String, Long>> observable = consumer.toObservable();

observable
  .map(record -> record.value())
  .buffer(256)
  .map(
  list -> list.stream().mapToDouble(n -> n).average()
).subscribe(val -> {

  // 获取平均值

});
```

## 自动追踪传播

当您配置 Vert.x 开启追踪时 (参见 `setTracingOptions`)， 追踪可以通过 Kafka 的消息自动传播。

Kafka 的生产者会在写入消息时自动添加一个 Span 去追踪，追踪的上下文通过 Kafka 消息头部传递。并且消费者会在收到消息后根据消息头部信息重建 Span。

参考以下信息 [OpenTracing semantic convention](https://github.com/opentracing/specification/blob/master/semantic_conventions.md), Span 的标签包括：

- `span.kind`，类型是 `consumer` 或 `producer`
- `peer.address` 可以使用 `setTracePeerAddress` 配置。如果没有设置，那么会使用配置中的 Kafka 服务器地址
- `peer.hostname` 通过解析 `peer.address` 得到
- `peer.port` 通过解析 `peer.address` 得到
- `peer.service` 一直是 always `kafka`
- `message_bus.destination`, 会设置为 kafka 消息的主题

# Vert.x Kafka 管理客户端

该组件提供了对 Kafka 管理客户端接口的 Vert.x 风格的包装。 Kafka 管理客户端用于创建，修改和删除主题。 除此之外，它还可以用于管理 ACL (Access Control Lists)，消费者群组和更多信息。

## 创建 Kafka 管理客户端

创建 Kafka 管理客户端的方法与使用原生 Kafka 客户端库十分相似。

您需要配置一些属性，这些属性可以参见官方的 Apache Kafka 文档， 参考此链接:https://kafka.apache.org/documentation/#adminclientconfigs[admin].

为了传递配置信息，您可以使用一个 Map 来保存属性值， 并在调用 `KafkaAdminClient` 提供的静态创建方法传入。

```java
Properties config = new Properties();
config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

KafkaAdminClient adminClient = KafkaAdminClient.create(vertx, config);
```

## 使用 Kafka 管理客户端

### 获取主题列表

您可以调用 `listTopics` 方法来获取集群中的主题列表。 该方法唯一需要的参数是一个回调函数用于处理返回的主题列表。

```java
adminClient.listTopics().onSuccess(topics ->
    System.out.println("Topics= " + topics)
);
```

### 主题描述

您可以调用 `describeTopics` 方法来获取集群中的主题描述。 主题描述是指获取该主题相关的元数据，例如分片数量，副本，领导，同步中的副本等。 方法需要的参数是您要获取的主题列表和用于处理结果的回调， 回调会通过一个 Map 来返回查询结果，Map 的键是主题名称，Map 的内容是 `TopicDescription`。

```java
adminClient.describeTopics(Collections.singletonList("my-topic")).onSuccess(topics -> {
  TopicDescription topicDescription = topics.get("first-topic");

  System.out.println("Topic name=" + topicDescription.getName() +
      " isInternal= " + topicDescription.isInternal() +
      " partitions= " + topicDescription.getPartitions().size());

  for (TopicPartitionInfo topicPartitionInfo : topicDescription.getPartitions()) {
    System.out.println("Partition id= " + topicPartitionInfo.getPartition() +
      " leaderId= " + topicPartitionInfo.getLeader().getId() +
      " replicas= " + topicPartitionInfo.getReplicas() +
      " isr= " + topicPartitionInfo.getIsr());
  }
});
```

### 创建主题

您可以调用 `createTopics` 方法在集群中创建主题， 方法需要的参数是您要创建的主题列表和处理结果的回调。 要创建的主题需要通过 `NewTopic` 类来指定名称 分区的数量和复制因子。 也可以指定副本的分配，将副本映射给每个 Kafka 消息中介的 id，而不是仅设置 分片的数量和复制银子 (在这种情况下该值会被设置为 -1)。

```java
adminClient.createTopics(Collections.singletonList(new NewTopic("testCreateTopic", 1, (short)1)))
  .onSuccess(v -> {
    // 成功创建主题
  })
  .onFailure(cause -> {
    // 创建主题时出错
  });
```

### 删除主题

您可以调用 `deleteTopics` 方法来删除集群中的主题。 方法需要的参数是要删除的主题列表和处理结果的回调。

```java
adminClient.deleteTopics(Collections.singletonList("topicToDelete"))
  .onSuccess(v -> {
    // 成功删除主题
  })
  .onFailure(cause -> {
    // 删除主题时出错
  });
```

### 配置描述

您可以调用 `describeConfigs` 方法来获取资源配置的数据。 资源配置的描述是指获取集群中一些资源的信息，例如主题和消息中介。 方法需要的参数是您想要获取的资源列表和用于处理结果的回调。 资源配置通过一个类型为 `ConfigResource` 的集合描述。每个配置的数据 保存在 `Config` 类的 `ConfigEntry` 键值对类型中

```java
adminClient.describeConfigs(Collections.singletonList(
  new ConfigResource(org.apache.kafka.common.config.ConfigResource.Type.TOPIC, "my-topic"))).onSuccess(configs -> {
  // 检查配置
});
```

### 选择配置

您可以调用 `alterConfigs` 方法来选取集群的资源配置。 选取资源配置是指更新集群资源的配置信息，例如主题和分区。 方法需要的参数是您想更新的配置的资源列表和用于处理结果的回调。 您可以在一次方法调用中选取更新多个不同资源的数据。需要的参数是 `ConfigResource` 作为资源参数与 `Config` 作为配置一一对应。

```java
ConfigResource resource = new ConfigResource(org.apache.kafka.common.config.ConfigResource.Type.TOPIC, "my-topic");
// 创建更新该主题 retention.ms 的配置项
ConfigEntry retentionEntry = new ConfigEntry(TopicConfig.RETENTION_MS_CONFIG, "51000");
Map<ConfigResource, Config> updateConfig = new HashMap<>();
updateConfig.put(resource, new Config(Collections.singletonList(retentionEntry)));
adminClient.alterConfigs(updateConfig)
  .onSuccess(v -> {
    // 成功更新配种
  })
  .onFailure(cause -> {
    // 配置更新时出错
  });
```

### 消费者群组列表

您可以调用 `listConsumerGroups` 方法来获取集群中的消费者群组列表。 方法需要的参数只有用于处理消费者群组列表结果的回调。

```java
adminClient.listConsumerGroups().onSuccess(consumerGroups ->
  System.out.println("ConsumerGroups= " + consumerGroups)
);
```

### 消费者群组的描述

您可以调用 `describeConsumerGroups` 来获取消费者群组的描述。 消费者群组的描述是指获取消费者群组的相关信息，例如成员，相关的 id，主题订阅，分区分配等。 需要的参数是要获取描述的消费者群组列表和用于处理结果的回调， 回调会通过一个 Map 来返回查询结果，Map 的键是消费者群组的名称，Map 的值类型是 `MemberDescription` 。

```java
adminClient.describeTopics(Collections.singletonList("my-topic")).onSuccess(topics -> {
  TopicDescription topicDescription = topics.get("first-topic");

  System.out.println("Topic name=" + topicDescription.getName() +
      " isInternal= " + topicDescription.isInternal() +
      " partitions= " + topicDescription.getPartitions().size());

  for (TopicPartitionInfo topicPartitionInfo : topicDescription.getPartitions()) {
    System.out.println("Partition id= " + topicPartitionInfo.getPartition() +
      " leaderId= " + topicPartitionInfo.getLeader().getId() +
      " replicas= " + topicPartitionInfo.getReplicas() +
      " isr= " + topicPartitionInfo.getIsr());
  }
});
```
