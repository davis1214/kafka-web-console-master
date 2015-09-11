package com.sohu.thread

import util.Util
import Util._
import models.Zookeeper
import java.nio.charset.Charset
import common.TopicCachedConfig

/**
 * Created by davihe on 15-8-18.
 */
object TopicHistoryTest {

  def main(args: Array[String]): Unit = {
    try {
      //init

      val path = "conf/topic-info.json"
      TopicCachedConfig.registerConfig(path)

      val topic = TopicCachedConfig.cachedTopicMap.get("tvadfront-pc")
      println("topic>>:" + topic)
      test

      //test2
    } catch {
      case e: Exception => e.printStackTrace()
    }

    //test3
    // test4
    //test5
  }

  def test2() {
    /*val connectedZks = Seq(Zookeeper("127.0.0.1", "127.0.0.1", 2181, 0l, 1l, "", "", 142l))
    val zkClient = ZkClient("127.0.0.1:2181", 6000 milliseconds, 6000 milliseconds)(new JavaTimer)
    val zkConnections = Map("127.0.0.1" -> zkClient)*/

    val connectedZks = Seq(Zookeeper("10.16.10.99", "10.16.10.99", 2181, 0l, 1l, "kafka-0.8.1", "conf/topic-info.json", 142l))
    val zkClient = ZkClient("10.16.10.99:2181", 6000 milliseconds, 6000 milliseconds)(new JavaTimer)
    val zkConnections = Map("10.16.10.99" -> zkClient)

    val partitionOffsets = getPartitionOffsets("tvadfront-mobile", zkClient)
    //println("----->" + partitionOffsets)

    partitionOffsets.foreach(println)
  }

  var cachedTopicMap: Map[String, (String, Int)] = Map()

  //全局变量
  def init() {
    val path = "conf/topic-info.json"
    val filecontents = Utils.readFileAsString(path, Charset.defaultCharset())

    // cachedMap.+= ()

    JSON.parseFull(filecontents) match {
      case Some(m) =>
        val info = m.asInstanceOf[Map[String, Any]]
        val describe = info.get("describe").get
        //println("describe:" + describe)
        val topics = info.get("topics").get.asInstanceOf[List[Map[String, Any]]]
        //println("topics --> " + broker.get("host").get + ":" + broker.get("port").get)
        //println("topics --> " + topics)
        //topics.map(f => println("topic:" + f.get("topic") + " ,partition num:" + f.get("partitions") + " ,
        // " +  "zk path:" + f.get("zkpath")))

        topics.map(f =>
          cachedTopicMap += (f.get("topic").get.asInstanceOf[String] ->
            (f.get("zkpath").get.asInstanceOf[String], f.get("partitions").get.asInstanceOf[Double].toInt)))
      //println("cachedTopicMap:" + cachedTopicMap)

      case None => println("none")
    }

    //println("-->" + cachedTopicMap)

    val tuple = cachedTopicMap.get("tvadppv")
    println("topic:" + tuple.get._1 + " , partitions :" + tuple.get._2)

  }

  def test {
    connectedZookeepers {
      (topic, zk, zkClient) => {
        //TODO add try catch clause for test
        try {
          println("process topic :" + topic)
          val topicPath = "/" + zk.chroot + "/brokers/topics"
          val topicIndex = topicPath.split("/").length - 1

          val topicsAndPartitionsAndZookeeperAndLogSizes = for {
          // it's possible to have topics without partitions in Zookeeper
            allTopicNodes <- getZChildren(zkClient, topicPath + "/" + topic)
            allTopics = allTopicNodes.map(p => (p.path.split("/").filter(_ != "")(topicIndex), Seq[String]())).toMap
            partitions <- getZChildren(zkClient, topicPath + "/" + topic + "/partitions/*")
            topics = partitions.map(p => (p.path.split("/").filter(_ != "")(topicIndex), p.name)).groupBy(_._1).map(e => e._1 -> e._2.map(_._2))
            topicsAndPartitionsAndZookeeper = (allTopics ++ topics).map(e => Map("name" -> e._1, "partitions" -> e._2, "zookeeper" -> zk.name)).toSeq
            topicsAndPartitionsAndZookeeperAndLogSize <- createTopicsInfo(topicPath, zk, topicsAndPartitionsAndZookeeper, zkClient)
          } yield topicsAndPartitionsAndZookeeperAndLogSize

          topicsAndPartitionsAndZookeeperAndLogSizes.foreach(f => {
            //println("--->" + f.flatten)
            f.map(topiczk => {
              println("------->" + topiczk)
              val name = topiczk.get("name") //topicName
              val zookeeper = topiczk.get("zookeeper") //zkName config in zookeeper
              val logSize = topiczk.get("logSize")
              println("name:" + name + " ,zookeeper:" + zookeeper + " ,logSize:" + logSize)
            })
          })
        } catch {
          case e: Exception => e.printStackTrace()
        }
      }

    }

    println("已经处理完了！！")

    //Future.sequence(topicsZks).map(l => println("-->" + l.flatten))
  }

  def connectedZookeepers[A](block: (String, Zookeeper, ZkClient) => A): Unit = {
    //name  host  port  statusId  groupId chroot  id
    //127.0.0.1 127.0.0.1 2181  0 1   14

    val connectedZks = Seq(Zookeeper("10.16.10.99", "10.16.10.99", 2181, 0l, 1l, "kafka-0.8.1", "conf/topic-info.json", 142l))
    val zkClient = ZkClient("10.16.10.99:2181", 6000 milliseconds, 6000 milliseconds)(new JavaTimer)
    val zkConnections = Map("10.16.10.99" -> zkClient)


    zkConnections match {
      case _ if zkConnections.size > 0 => connectedZks.map {
        zk: Zookeeper => {

          val topicPath = "/" + zk.chroot + "/brokers/topics"
          val topicIndex = topicPath.split("/").length - 1

          println("topic path is " + topicPath)
          val allTopic = for {
            allTopicNodes <- getZChildren(zkConnections.get(zk.name).get, topicPath + "/*")
            allTopics = allTopicNodes.map(p => (p.path.split("/").filter(_ != "")(topicIndex), Seq[String]())).toMap
          } yield allTopics

          allTopic.foreach(t =>
            t match {
              case map: Map[String, List[Nothing]] => map.map(ss => {
                //println("to proc topic:" + ss._1)
                //TODO for test
                if (ss._1.equals("tvadfront-pc")) {
                  block(ss._1, zk, zkConnections.get(zk.name).get)
                }
              })
              case _ =>
            }
          )
        }
      }
      case _ => Seq.empty
    }
  }

  private def createTopicsInfo(topicPath: String, zk: Zookeeper, topics: Seq[Map[String, Object]], zkClient: ZkClient): Future[Seq[Map[String,
    Object]]] = {

    println("to create topic info")
    Future.sequence(topics.map {
      e =>
        for {
          partitionLeaders <- getPartitionLeaders(topicPath, e("name").toString, zkClient)
          //partitionsLogSize <- getPartitionOffsets(e("name").toString, zkClient)
          partitionsLogSize <- if (zk.topicConfig.isEmpty) getPartitionsLogSize(e("name").toString, partitionLeaders.dropRight(1).toSeq)
          else getPartitionOffsets(e("name").toString, zkClient)
          //partitionsLogSize <- getPartitionsLogSize(e("name").toString, partitionLeaders)
          partitions = partitionsLogSize.zipWithIndex.map(pls => Map("id" -> pls._2.toString, "logSize" -> pls._1.toString, "leader" -> partitionLeaders(pls._2)))
          logSizeSum = partitionsLogSize.foldLeft(0.0)(_ + _).toString
        } yield Map("name" -> e("name"), "partitions" -> partitions, "zookeeper" -> e("zookeeper"), "logSize" -> logSizeSum)
    })
  }

  def getPartitionLeaders(topicPath: String, topicName: String, zkClient: ZkClient): Future[Seq[String]] = {
    Logger.debug("Getting partition leaders for topic " + topicName)
    val len = topicPath.split("/").length

    return for {
      partitionStates <- getZChildren(zkClient, topicPath + "/" + topicName + "/partitions/*/state")
      //TODO spit path 改为 5 -> 6
      partitionsData <- Future.sequence(partitionStates.map(p => twitterToScalaFuture(p.getData().map(d => (p.path.split("/")(
        (len + 2)),
        new String(d.bytes))))))
      brokerIds = partitionsData.map(d => (d._1, scala.util.parsing.json.JSON.parseFull(d._2).get.asInstanceOf[Map[String, Any]].get("leader").get))
      brokers <- Future.sequence(brokerIds.map(bid => getZChildren(zkClient, "/brokers/ids/" + bid._2.toString.toDouble.toInt).map((bid._1, _))))
      partitionsWithLeaders = brokers.filter(_._2.headOption match {
        case Some(s) => true
        case _ => false
      })
      partitionsWithoutLeaders = brokers.filterNot(b => b._2.headOption match {
        case Some(s) => true
        case _ => Logger.warn("Partition " + b._1 + " in topic " + topicName + " has no leaders"); false
      })
      brokersData <- Future.sequence(partitionsWithLeaders.map(d => twitterToScalaFuture(d._2.head.getData().map((d._1, _)))))
      brokersInfo = brokersData.map(d => (d._1, scala.util.parsing.json.JSON.parseFull(new String(d._2.bytes)).get.asInstanceOf[Map[String, Any]]))
      brokersAddr = brokersInfo.map(bi => (bi._1, bi._2.get("host").get + ":" + bi._2.get("port").get.toString.toDouble.toInt))
      pidsAndBrokers = brokersAddr ++ partitionsWithoutLeaders.map(pid => (pid._1, ""))
    } yield pidsAndBrokers.sortBy(pb => pb._1.toInt).map(pb => pb._2)
  }

  def getPartitionOffsets(topicName: String, zkClient: ZkClient): Future[Seq[Double]] = {
    Logger.debug("Getting partition offsets for topic " + topicName)
    println("topic:" + topicName)
    val zkPath = TopicCachedConfig.cachedTopicMap.get(topicName).get._1
    println("zkPath:" + zkPath)

    return for {
      offsetsPartitionsNodes <- getZChildren(zkClient, zkPath + "/*")
      partitionOffsets <- Future.sequence(offsetsPartitionsNodes.map(p => twitterToScalaFuture(p.getData().map(d => (p.path.split
        ("/")(3).split("_")(1), getOffset(new String(d.bytes)).toDouble)))))
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

  def getPartitionsLogSize(topicName: String, partitionLeaders: Seq[String]): Future[Seq[Double]] = {
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
              case e => Logger.error("Could not connect to partition leader " + addr + ". Error message: " + e.getMessage);
                0L
            }
          }
          client.close()
          offset
      })
    } yield partitionsLogSize.map(_.toString.toDouble)
  }

  private def createPartitionInfo(consumerGroupAndPartitionOffsets: (String, Seq[Long]),
                                  partitionsLogSize: Seq[Long],
                                  owners: Seq[(String, Int, String)]): Seq[Map[String, String]] = {
    consumerGroupAndPartitionOffsets._2.zipWithIndex.map {
      case (pO, i) =>
        Map("id" -> i.toString, "offset" -> pO.toString, "lag" -> (partitionsLogSize(i) - pO).toString,
          "owner" -> {
            owners.find(o => (o._1 == consumerGroupAndPartitionOffsets._1) && (o._2 == i)) match {
              case Some(s) => s._3
              case None => ""
            }
          })
    }
  }

}
