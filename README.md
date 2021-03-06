# kafka-web-console-master
 kafka-web-console

 项目说明：
 Kafka Web Console是用Scala语言编写的Java web程序，用于监控Apache Kafka。源代码的地址在https://github.com/claudemamo/kafka-web-console中。
 项目采用scala（2.10） + play framework（2.2）开发，涉及到了kafka、zookeeper、squeryl(scala ORM for SQL databases,0.9.5)、mysql、jmx等。项目管理采用sbt（0.13.0）。

第一阶段
    二次开发：
    1、页面部分增加了kafka监控的信息指标，涉及到的指标信息，见文档《katfka监控方案》；
    2、后台读取partition size的改为从zk读取。
    3、调整适配我们现在kafka2hdfs对partition的维护；
    4、数据持久化改为mysql的方式，并且增加了对topic、partition、kafka broker历史信息的保存；
    5、优化程序。处理topic信息的地方由批处理改为一次处理一个topic；
    6、zk配置的地方，增加topic-config。根据当前的配置信息，处理对应zk配置的topic，同时可以解决对单个zk节点访问过多造成的压力问题。

    接下来：
    1、页面部分，时间折行问题；
    2、增加告警监控管理；
    3、根据情况考虑是否增加权限功能；
    4、定时任务，改成细粒度的方式（比如一个定时配置对应一个功能项）
    5、重构、优化；
    6、优化项目，去掉或者合并不用的类或者文件；
    7、需要多测试，发现解决其中的问题，包括性能、稳定性、安全等。
    8、增加参数指标说明页

第二阶段
    1、去掉topic group组件中topic feed功能（影响性能）；
    2、topic、parition中增加logLag指控指标，标示消费者消费的延迟大小（写入的offset减去消费的logsize）；
    3、topic中增加LogBytesAppendedPerSec指标,标示每秒增加的日志大小；
    4、增加告警监控的功能（文件配置+微信报警端口），并且可根据alarm.message.send.interval配置短信发送的频率；
    5、定时任务，改成细粒度的方式（比如一个定时配置对应一个功能项）；
    6、setting功能去掉。避免因为误操作导致的对zk、kafka集群的性能问题；
    7、将原来页面settings的功能改为配置信息展示，包括原来的任务配置信息和告警配置信息。参数调整需要手动维护。


以下是对比的开源kafka监控：
             	作者	开源版本地址	                                  开发语言	  技术框架	     功能
 offset monitor		    https://github.com/quantifind/KafkaOffsetMonitor	scala	                 传统的java web项目	topic及partition对应的offset大小。UI展示一般
 kafka manager	 yahoo	https://github.com/yahoo/kafka-manager		        scala    playframework	 操作流程复杂。页面的展示繁琐
 kafka-web-console		https://github.com/claudemamo/kafka-web-console		scala	 playframework   操作简单，页面友好。源代码较为清晰，二次开发容易些。

其他说明信息请参考doc目录下的文档
