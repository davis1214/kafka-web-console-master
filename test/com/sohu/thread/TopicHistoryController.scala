package com.sohu.thread

import util.Util
import Util._
import models.Zookeeper


object TopicHistoryController {

  def main(args: Array[String]): Unit = {
    test
    //test_old
    //test2
    //test3
    // test4
    //test5
  }

  def test5 {
    val connectedZks = Seq(Zookeeper("127.0.0.1", "127.0.0.1", 2181, 0l, 1l, "", "14"))
    val zkClient = ZkClient("127.0.0.1:2181", 6000 milliseconds, 6000 milliseconds)(new JavaTimer)
    val zkConnections = Map("127.0.0.1" -> zkClient)

    connectedZks.map(zk => {
      for (topics <- getTopics(zkConnections.get(zk.name).get)) yield blocks(topics.keySet)
    })

    /*   zkConnections match {
         case _ if zkConnections.size > 0 => connectedZks.map{zk => {
           for( topics  <- getTopics(zkConnections.get(zk.name).get)) yield blocks(topics.keySet)
          // block("newtvadpv",zk, zkConnections.get(zk.name).get)}
         }
         case _ => Seq.empty
       }
   */
  }

  def blocks(topics: Set[String]): Unit = {
    println("topics -->" + topics)
  }

  def test4 {
    val connectedZks = Seq(Zookeeper("127.0.0.1", "127.0.0.1", 2181, 0l, 1l, "", "14"))
    val zkClient = ZkClient("127.0.0.1:2181", 6000 milliseconds, 6000 milliseconds)(new JavaTimer)
    val zkConnections = Map("127.0.0.1" -> zkClient)

    connectedZks.map(zk => {
      val allTopic = for {
        allTopicNodes <- getZChildren(zkConnections.get(zk.name).get, "/brokers/topics/*")
        allTopics = allTopicNodes.map(p => (p.path.split("/").filter(_ != "")(2), Seq[String]())).toMap
      } yield allTopics

      allTopic.foreach(t =>
        t match {
          case map: Map[String, List[Nothing]] => map.map(ss => {
            println("topic:" + ss._1)

            // block( ss._1,zk, zkConnections.get(zk.name).get)
          })
          case _ =>
        }
      )
    })


    /*zkConnections match {
      case _ if zkConnections.size > 0 =>
        connectedZks.map(zk => block("newtvadpv",zk, zkConnections.get(zk.name).get)).toSeq
      case _ => Seq.empty
    }*/

  }

  def test3 {
    val zkClient = ZkClient("127.0.0.1:2181", 6000 milliseconds, 6000 milliseconds)(new JavaTimer)
    val allTopicNodes: Future[Seq[ZNode]] = getZChildren(zkClient, "/brokers/topics/*")

    allTopicNodes.map(f => f.map(p =>
      println("name:" + p.name + ", path:" + p.path)
    ))
  }

  def test2 {
    val zkClient = ZkClient("127.0.0.1:2181", 6000 milliseconds, 6000 milliseconds)(new JavaTimer)
    val allTopicNodes: Future[Seq[ZNode]] = getZChildren(zkClient, "/brokers/topics/*")

    allTopicNodes.map(f => f.map(p =>
      println("name:" + p.name + ", path:" + p.path)
    ))

    // allTopicNodes.map(f => println("----->" + f.map(x => x.name)))
    //Future.sequence(children) .map(f=>println("--------->"+f.head.toString()))
    val allTopics = allTopicNodes.map(x => x.map(p => (p.path.split("/").filter(_ != "")(2), Seq[String]())))
    println("allTopics:" + allTopics)

    val partitions = getZChildren(zkClient, "/brokers/topi cs/*/partitions/*")
    val topics = partitions.map(x => x.map(p => (p.path.split("/").filter(_ != "")(2), p.name)))
    println("topics:" + topics.map(x => println(x)))
  }

  def test_old {
    connectedZookeepers {
      //TODO 增加topic name
      (topic, zk, zkClient) => {
        println("zk:" + zk.host)
        //1、调整为按topic处理；2、当前zkstr如果需要读取配置，则单独处理
        val allTopicNodes: Future[Seq[ZNode]] = getZChildren(zkClient, "/brokers/topics/*")
        val topicsAndPartitionsAndZookeeperAndLogSize = allTopicNodes.map(f => f.map(p => {
          println("proc zNode(" + p.name + ") -> " + p.path)
          for {
            allTopicNodes <- getZChildren(zkClient, "/brokers/topics/" + p.name)
            allTopics = allTopicNodes.map(p => (p.path.split("/").filter(_ != "")(2), Seq[String]())).toMap
            partitions <- getZChildren(zkClient, "/brokers/topics/" + p.name + "/partitions/*")
            topics = partitions.map(p => (p.path.split("/").filter(_ != "")(2), p.name)).groupBy(_._1).map(e => e._1 -> e._2.map(_._2))
            topicsAndPartitionsAndZookeeper = (allTopics ++ topics).map(e => Map("name" -> e._1, "partitions" -> e._2, "zookeeper" -> zk.name)).toSeq
            topicsAndPartitionsAndZookeeperAndLogSize <- createTopicsInfo(topicsAndPartitionsAndZookeeper, zkClient)
          } yield topicsAndPartitionsAndZookeeperAndLogSize
        }
        ))
      }
    }

    //TODO for print
    //Future.sequence(topicsZks).map(l => println("-->" + l.flatten))
  }

  def test {
    connectedZookeepers {
      (topic, zk, zkClient) => {
        println("process topic :" + topic)
        val topicsAndPartitionsAndZookeeperAndLogSizes = for {
        // it's possible to have topics without partitions in Zookeeper
          allTopicNodes <- getZChildren(zkClient, "/brokers/topics/" + topic)
          allTopics = allTopicNodes.map(p => (p.path.split("/").filter(_ != "")(2), Seq[String]())).toMap
          partitions <- getZChildren(zkClient, "/brokers/topics/" + topic + "/partitions/*")

          topics = partitions.map(p => (p.path.split("/").filter(_ != "")(2), p.name)).groupBy(_._1).map(e => e._1 -> e._2.map(_._2))

          topicsAndPartitionsAndZookeeper = (allTopics ++ topics).map(e => Map("name" -> e._1, "partitions" -> e._2, "zookeeper" -> zk.name)).toSeq

          topicsAndPartitionsAndZookeeperAndLogSize <- createTopicsInfo(topicsAndPartitionsAndZookeeper, zkClient)

        } yield topicsAndPartitionsAndZookeeperAndLogSize

        topicsAndPartitionsAndZookeeperAndLogSizes.foreach(f => {
          println("--->" + f.flatten)

          f.map(topiczk => {
            println("------->" + topiczk)
            val name = topiczk.get("name") //topicName
            val zookeeper = topiczk.get("zookeeper") //zkName config in zookeeper
            val logSize = topiczk.get("logSize")
            println("name:" + name + " ,zookeeper:" + zookeeper + " ,logSize:" + logSize)
          })
          /*f.flatten.map(topiczk=>{
            println(topiczk)
          })*/
        })
      }
    }

    println("已经处理完了！！")

    //Future.sequence(topicsZks).map(l => println("-->" + l.flatten))
  }

  def connectedZookeepers[A](block: (String, Zookeeper, ZkClient) => A): Unit = {
    //name  host  port  statusId  groupId chroot  id
    //127.0.0.1 127.0.0.1 2181  0 1   14

    val connectedZks = Seq(Zookeeper("127.0.0.1", "127.0.0.1", 2181, 0l, 1l, "", "14"))
    val zkClient = ZkClient("127.0.0.1:2181", 6000 milliseconds, 6000 milliseconds)(new JavaTimer)
    val zkConnections = Map("127.0.0.1" -> zkClient)

    zkConnections match {
      case _ if zkConnections.size > 0 => connectedZks.map {
        zk: Zookeeper => {
          val allTopic = for {
            allTopicNodes <- getZChildren(zkConnections.get(zk.name).get, "/brokers/topics/*")
            allTopics = allTopicNodes.map(p => (p.path.split("/").filter(_ != "")(2), Seq[String]())).toMap
          } yield allTopics

          allTopic.foreach(t =>
            t match {
              case map: Map[String, List[Nothing]] => map.map(ss => {
                println("topic:" + ss._1)
                block(ss._1, zk, zkConnections.get(zk.name).get)
              })
              case _ =>
            }
          )
        }
      }
      case _ => Seq.empty
    }
  }

  private def createTopicsInfo(topics: Seq[Map[String, Object]], zkClient: ZkClient): Future[Seq[Map[String, Object]]] = {
    Future.sequence(topics.map {
      e =>
        for {
          partitionLeaders <- getPartitionLeaders(e("name").toString, zkClient)
          partitionsLogSize <- getPartitionsLogSize(e("name").toString, partitionLeaders)
          partitions = partitionsLogSize.zipWithIndex.map(pls => Map("id" -> pls._2.toString, "logSize" -> pls._1.toString, "leader" -> partitionLeaders(pls._2)))
          logSizeSum = partitionsLogSize.foldLeft(0.0)(_ + _).toInt.toString
        } yield Map("name" -> e("name"), "partitions" -> partitions, "zookeeper" -> e("zookeeper"), "logSize" -> logSizeSum)

    })
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
