package actors

import common._
import java.util.Properties
import models.Zookeeper
import java.util.concurrent.TimeUnit
import util.Util
import Util._
import jmx.JmxDomain
import common.Registry.PropertyConstants
import models.PartitionHistoryInfo.PartitionHistoryInfo
import models.TopicHistoryInfo.TopicHistoryInfo

import models.Settings

/**
 * Created by davihe on 15-7-15.
 */
private class TopicExecutor() extends Job {
  def execute(ctx: JobExecutionContext) {
    val actor = ctx.getJobDetail.getJobDataMap().get("topicActor").asInstanceOf[ActorRef]
    actor ! Message.Purge
  }
}

class TopicHistoryManager extends Actor with JmxConnection {
  private var topicHisotryPointsTask: Cancellable = null
  private val JobKey = "topicMonitor"
  private[this] val props = new Properties()
  props.setProperty("org.quartz.scheduler.instanceName", context.self.path.name)
  props.setProperty("org.quartz.threadPool.threadCount", "1")
  props.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore")
  props.setProperty("org.quartz.scheduler.skipUpdateCheck", "true")
  val scheduler = new StdSchedulerFactory(props).getScheduler

  override def preStart() {
    scheduler.start()
    schedule()
    self ! Message.FetchTopicMonitorInfo
  }

  override def postStop() {
    scheduler.shutdown()
  }

  override def receive: Receive = {
    case Message.FetchTopicMonitorInfo => {
      fetchTopicMonitorInfoPoints()

      topicHisotryPointsTask = Akka.system.scheduler.scheduleOnce(Duration.create(Settings.findByPurgeType(Settings.PurgeTypeTopic.toString).get.FetchInterval.toLong,
        TimeUnit.SECONDS), self, Message.FetchTopicMonitorInfo)
    }
    case Message.Purge => {
      Logger.warn(" Message.Purge in BrokerMonitorManager")
    }
    case _ => Logger.warn("TopicHistoryManger : undesired messages received!")
  }

  private def schedule() {
    val jdm = new JobDataMap()
    jdm.put("topicActor", self)
    val job = JobBuilder.newJob(classOf[BrokerExecutor]).withIdentity(JobKey).usingJobData(jdm).build()
    scheduler.scheduleJob(job, TriggerBuilder.newTrigger().startNow().forJob(job).withSchedule(CronScheduleBuilder.cronSchedule(Settings.findByPurgeType(Settings.PurgeTypeTopic
      .toString).get.PurgeSchedule)).build())
  }

  private def fetchTopicMonitorInfoPoints() {
    Logger.info("fetchTopicMonitorInfoPoints in TopicHistoryManger")

    try {
      connectedZookeeper {
        (topic, zk, zkClient) => {
          Logger.debug("to proc topic " + topic)

          val topicsAndPartitionsAndZookeeperAndLogSizes = for {
            allTopicNodes <- getZChildren(zkClient, "/brokers/topics/" + topic)
            allTopics = allTopicNodes.map(p => (p.path.split("/").filter(_ != "")(2), Seq[String]())).toMap
            partitions <- getZChildren(zkClient, "/brokers/topics/" + topic + "/partitions/*")

            topics = partitions.map(p => (p.path.split("/").filter(_ != "")(2), p.name)).groupBy(_._1).map(e => e._1 -> e._2.map(_._2))
            topicsAndPartitionsAndZookeeper = (allTopics ++ topics).map(e => Map("name" -> e._1, "partitions" -> e._2, "zookeeper" -> zk.name)).toSeq
            topicsAndPartitionsAndZookeeperAndLogSize <- createTopicsInfo(topicsAndPartitionsAndZookeeper, zk, zkClient)
          } yield topicsAndPartitionsAndZookeeperAndLogSize

          topicsAndPartitionsAndZookeeperAndLogSizes.foreach(topiczks => {
            topiczks.map(topiczk => {
              Logger.debug("process topic " + topiczk)
              val name = topiczk.get("name") //topicName
              val zookeeper = topiczk.get("zookeeper") //zkName config in zookeeper
              val logSize = topiczk.get("logSize")
              val consumedLogSize = topiczk.get("consumedLogSize")

              Logger.debug("name:" + name + " ,zookeeper:" + zookeeper + " ,logSize:" + logSize)
              val topicHistoryInfo: models.TopicHistoryInfo.TopicHistoryInfo = getTopicHistoryInfo(name, zookeeper, topiczk, logSize, consumedLogSize)
              models.TopicHistoryInfo.updateByIsCurrentTime(name.get.toString, zookeeper.get.toString, 1)
              val topicHistoryInfoId = models.TopicHistoryInfo.insert(topicHistoryInfo)

              //partitionHistoryInfo
              topiczk.get("partitions").get match {
                case partition: scala.collection.mutable.ArrayBuffer[Map[String, Any]] => {
                  partition.map(pm => {
                    //Map(id -> 1, logSize -> 385483, leader -> 127.0.0.1:9093)
                    //TODO 增加logLag字段
                    val logLag = pm.get("logSize").get.toString.toDouble - pm.get("consumedLogSize").get.toString.toDouble
                    val partitionHistoryInfo = new PartitionHistoryInfo(topicHistoryId = topicHistoryInfoId, partitionId = pm.get("id").get.toString.toLong,
                      logSize = pm.get("logSize").get.toString.toDouble, logLag, leader = pm.get("leader").get.toString)
                    models.PartitionHistoryInfo.insert(partitionHistoryInfo)
                  })
                }
                case _ =>
              }
            })
          })
        }
      }
    } catch {
      case e: Exception => Logger.error(e.getMessage, e)
    }
  }

  def connectedZookeeper[A](block: (String, Zookeeper, ZkClient) => A): Unit = {
    val connectedZks = models.Zookeeper.findByStatusId(models.Status.Connected.id)

    val zkConnections: Map[String, ZkClient] = Registry.lookupObject(PropertyConstants.ZookeeperConnections) match {
      case Some(s: Map[_, _]) if connectedZks.size > 0 => s.asInstanceOf[Map[String, ZkClient]]
      case _ => Map()
    }

    //TODO 一次处理一个topic信息
    zkConnections match {
      case _ if zkConnections.size > 0 => connectedZks.map {
        zk: Zookeeper => {
          val topicPath = "/brokers/topics" //"/"+zk.chroot + "/brokers/topics"
          val topicIndex = topicPath.split("/").length - 1

          if (!zk.topicConfig.isEmpty) TopicCachedConfig.registerConfig(zk.topicConfig)

          println("topic path is " + topicPath)
          val allTopic = for {
            allTopicNodes <- getZChildren(zkConnections.get(zk.name).get, topicPath + "/*")
            allTopics = allTopicNodes.map(p => (p.path.split("/").filter(_ != "")(topicIndex), Seq[String]())).toMap
          } yield allTopics

          // val allTopic = TopicCachedConfig.cachedTopicMap.keySet
          allTopic.foreach(allt =>
            allt match {
              case map: Map[String, List[Nothing]] => map.map(topic => {
                if (!zk.topicConfig.isEmpty) {
                  if (TopicCachedConfig.cachedTopicMap.keySet.contains(topic._1.toString))
                    block(topic._1, zk, zkConnections.get(zk.name).get)
                } else {
                  block(topic._1, zk, zkConnections.get(zk.name).get)
                }
              })
              case _ =>
            }
          )
        }
      }
      case _ =>
    }
  }

  def getTopicHistoryInfo(name: Option[Object], zookeeper: Option[Object], topiczk: Map[String, Object], logSize: Option[Object],
                          consumedLogSize: Option[Object]): models.TopicHistoryInfo.TopicHistoryInfo = {
    val datetime = new java.sql.Timestamp(System.currentTimeMillis())
    val topicHistoryInfo = new TopicHistoryInfo(name.get.toString, zookeeper.get.toString, datetime)

    val topicInfos = getTopicInfos(name.get.toString, topiczk.get("jmxAddr").get.toString)
    //Logger.debug("topicInfos-->" + topicInfos)

    topicHistoryInfo.logSize = logSize.get.toString.toDouble
    topicHistoryInfo.logLag = logSize.get.toString.toDouble - consumedLogSize.get.toString.toDouble
    topicHistoryInfo.bytesInPerSec = topicInfos(0).toString.toDouble
    topicHistoryInfo.bytesOutPerSec = topicInfos(1).toString.toDouble
    topicHistoryInfo.failedFetchRequestsPerSec = topicInfos(2).toString.toDouble
    topicHistoryInfo.failedProduceRequestsPerSec = topicInfos(3).toString.toDouble
    topicHistoryInfo.logBytesAppendedPerSec = topicInfos(4).toString.toDouble
    topicHistoryInfo.messagesInPerSec = topicInfos(5).toString.toDouble
    topicHistoryInfo
  }

  //TODO 增加consumedLogSize
  //implicit def stringToDouble(x:String) : Double = x.toDouble
  private def createTopicsInfo(topics: Seq[Map[String, Object]], zk: Zookeeper, zkClient: ZkClient): Future[Seq[Map[String, Object]]] = {
    val topicConfig = zk.topicConfig
    Future.sequence(topics.map {
      e =>
        for {
          partitionLeaders <- getPartitionLeadersForTopicManager(e("name").toString, zkClient)
          partitionsLogSize <- if (topicConfig.isEmpty) getPartitionsLogSize(e("name").toString, partitionLeaders.dropRight(1).toSeq)
          else getPartitionOffsets(zk, e("name").toString, partitionLeaders.dropRight(1).toSeq)

          consumedPartitions = partitionsLogSize._2.zipWithIndex.map(pls => pls._1.toString)
          partitions = partitionsLogSize._1.zipWithIndex.map(pls => Map("id" -> pls._2.toString, "logSize" -> pls._1.toString,
            "consumedLogSize" -> consumedPartitions(pls._2),
            "leader" -> partitionLeaders.dropRight(1).seq(pls._2)))
          jmxAddr = partitionLeaders.last
          logSizeSum = partitionsLogSize._1.foldLeft(0.0)(_ + _).toString
          consumedLogSizeSum = partitionsLogSize._2.foldLeft(0.0)(_ + _).toString
        } yield Map("name" -> e("name"), "partitions" -> partitions, "jmxAddr" -> jmxAddr, "zookeeper" -> e("zookeeper"), "logSize" -> logSizeSum,
          "consumedLogSize" -> consumedLogSizeSum)
    })
  }

  def getPartitionsLogSize(topicName: String, partitionLeaders: Seq[String]): Future[(Seq[Double], Seq[Double])] = {
    Logger.debug("Getting partition log sizes for topic " + topicName + " from partition leaders " + partitionLeaders.mkString(", "))
    return for {
      clients <- Future.sequence(partitionLeaders.map(addr => Future((addr, Kafka.newRichClient(addr)))))
      partitionsLogSize <- Future.sequence(clients.zipWithIndex.map {
        tu =>
          val addr = tu._1._1
          val client = tu._1._2
          var offset = Future(0L)

          if (!addr.isEmpty) {
            offset = twitterToScalaFuture(client.offset(topicName, tu._2, OffsetRequest.LatestTime)).map(_.offsets.head).recover {
              case e => Logger.error("Could not connect to partition leader " + addr + ". Error message: " + e.getMessage); 0L
            }
          }
          client.close()
          offset
      })
    } yield (partitionsLogSize.map(_.toString.toDouble), partitionsLogSize.map(_.toString.toDouble))
  }

  def getPartitionOffsets(zk: Zookeeper, topicName: String, partitionLeaders: Seq[String]): Future[(Seq[Double], Seq[Double])] = {
    Logger.debug("Getting partition offsets for topic " + topicName)

    val zkPath = TopicCachedConfig.cachedTopicMap.get(topicName) match {
      case Some(t) => t._1
      case None => ""
    }
    val zkAddr = zk.host + ":" + zk.port
    val zkClient = ZkClient(zkAddr, 6000 milliseconds, 6000 milliseconds)(new JavaTimer)

    return for {
      offsetsPartitionsNodes <- getZChildren(zkClient, zkPath + "/*")
      clients <- Future.sequence(partitionLeaders.map(addr => Future((addr, Kafka.newRichClient(addr)))))

      partitionConsumedOffsets <- Future.sequence(offsetsPartitionsNodes.map(p => twitterToScalaFuture(p.getData().map(d =>
        (p.path.split("/")(3).split("_")(1), getOffset(new String(d.bytes)).toDouble)))))
      partitionsLogSize <- Future.sequence(clients.zipWithIndex.map {
        tu =>
          val addr = tu._1._1
          val client = tu._1._2
          var offset = Future(0L)

          if (!addr.isEmpty) {
            offset = twitterToScalaFuture(client.offset(topicName, tu._2, OffsetRequest.LatestTime)).map(_.offsets.head).recover {
              case e => Logger.error("Could not connect to partition leader " + addr + ". Error message: " + e.getMessage); 0L
            }
          }
          client.close()
          offset
      })

      sortedPartitionConsumedOffsets = partitionConsumedOffsets.sortBy(r => r._1.toInt).map(p => p._2)
    } yield (partitionsLogSize.map(_.toString.toDouble), sortedPartitionConsumedOffsets)
  }

  def getPartitionOffsets_old(zk: Zookeeper, topicName: String): Future[Seq[Double]] = {
    Logger.debug("Getting partition offsets for topic " + topicName)

    val zkPath = TopicCachedConfig.cachedTopicMap.get(topicName) match {
      case Some(t) => t._1
      case None => ""
    }
    val zkAddr = zk.host + ":" + zk.port
    val zkClient = ZkClient(zkAddr, 6000 milliseconds, 6000 milliseconds)(new JavaTimer)

    return for {
      offsetsPartitionsNodes <- getZChildren(zkClient, zkPath + "/*")
      partitionOffsets <- Future.sequence(offsetsPartitionsNodes.map(p => twitterToScalaFuture(p.getData().map(d =>
        (p.path.split("/")(3).split("_")(1), getOffset(new String(d.bytes)).toDouble)))))
      sortedPartitionOffsets = partitionOffsets.sortBy(r => r._1.toInt).map(p => p._2)
    } yield sortedPartitionOffsets
  }

  def getOffset(zkOffsetData: String): String = {
    var offset = JSON.parseFull(zkOffsetData) match {
      case Some(m) => {
        val controllerInfo = m.asInstanceOf[Map[String, Any]]
        val offsets = controllerInfo.get("offset").get.toString
        offsets
      }
      case None => "0"
    }
    offset
  }

  // get topic infos by partition num 0 through jxm
  def getTopicInfos(name: String, jmxAddr: String): List[Any] = {
    Logger.debug("get topic " + name + " infos from jmx " + jmxAddr)

    val conn = getJmxConn(jmxAddr)
    val jmxServerDomain = new JmxDomain(JmxPropertyConstant.KAFKA_SERVER, conn)

    //BytesInPerSec,BytesOutPerSec,FailedFetchRequestsPerSec,FailedProduceRequestsPerSec,LogBytesAppendedPerSec,MessagesInPerSec
    val topicInfos = JmxPropertyConstant.MONITOR_INDEX_KAFKA_TOPICS.map {
      m => {
        jmxServerDomain.mbean("name", "\"" + name + "-" + m + "\"").map {
          m => m.attribute("Count").map {
            _.value
          }
        } match {
          case Some(v) => v.get
          case None => "0"
        }
      }
    }
    topicInfos
  }

  case class TopicMonitor(val monitorIndex: String, val indexValue: String, val monitorUnit: String, val eventType: String)

}
