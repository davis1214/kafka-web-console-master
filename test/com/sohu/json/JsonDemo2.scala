/**
 *
 */
package com.sohu.json

import java.nio.charset.Charset

/**
 * @author davihe
 *
 */
object JsonDemos extends Logging {

  //TODO 全局变量 用法
  val path = "./conf/topic-info.json"

  val filecontents = Utils.readFileAsString(path, Charset.defaultCharset())

  def main(args: Array[String]): Unit = {
    //test1

    // test2

    val ss = "/opt/kafka-web-console/target/universal/kafka-web-console-2.1.0-SNAPSHOT/lib/default.kafka-web-console-2.1.0-SNAPSHOT.jar"

    // println(ss.substring(0,ss.split("/").length - 2))
    // println(    ss.split("/").slice(0,ss.split("/").length  - 2).toString)
    //test3
    println("1-->" + getClass.getResource(""))
    println("2-->" + System.getProperty("user.dir"))
    println("3-->" + getClass.getProtectionDomain.getCodeSource.getLocation.getPath)
    println("4-->" + java.net.URLDecoder.decode(getClass.getProtectionDomain.getCodeSource.getLocation.getFile, "UTF-8"))
  }

  def test3() {

    println("----->" + ClassLoader.getSystemResource("").getPath)

    val path = getClass.getResource("/")
    println("path :" + path)

    println("-->" + JsonDemos.getClass.getResource("/"))
    //将数据封装成的样子
    var cachedTopicMap: Map[String, (String, Int)] = Map()
    //从zk读取 offset的值  Map[topic,Tuple(zkpath,partitions)]

    JSON.parseFull(filecontents) match {
      case Some(m) =>
        val info = m.asInstanceOf[Map[String, Any]]
        val describe = info.get("describe").get
        println("describe:" + describe)

        val topics = info.get("topics").get.asInstanceOf[List[Map[String, Any]]]

        //println("topics --> " + broker.get("host").get + ":" + broker.get("port").get)
        println("topics --> " + topics)
        topics.map(f => println("topic:" + f.get("topic") + " ,partition num:" + f.get("partitions") + " , zk path:" + f.get("zkpath")))

        topics.map(f =>
          cachedTopicMap += (f.get("topic").get.asInstanceOf[String] ->
            (f.get("zkpath").get.asInstanceOf[String], f.get("partitions").get.asInstanceOf[Double].toInt)))
        println("cachedTopicMap:" + cachedTopicMap)

      case None => println("none")
    }
    println("-->" + cachedTopicMap)
  }

  def test2() {
    val zkoffset = "{\"offset\":17112027822,\"partition\":2,\"broker\":{\"host\":\"10.16.43.149\",\"port\":8092}," +
      "\"topic\":\"newtvadpv\"}"

    def getOffset(): String = {
      var offset = JSON.parseFull(zkoffset) match {
        case Some(m) => {
          val controllerInfo = m.asInstanceOf[Map[String, Any]]
          val offsets = controllerInfo.get("offset").get.toString //Double类型
          //println("offset:" + offsets)
          offsets
        }
        case None => "0"
      }
      offset
    }

    println("offset: " + getOffset())

  }

  def test1 {
    val a = 2899089861232323223l
    println("a:" + a)

    println("filecontents--->:" + filecontents)

    val controllerInfoString = "{\"controller_epoch\":23,\"leader\":1,\"version\":1,\"leader_epoch\":16,\"isr\":[1,2,3]}"

    println("controllerInfoString : " + controllerInfoString)

    JSON.parseFull(controllerInfoString) match {
      case Some(m) =>
        val controllerInfo = m.asInstanceOf[Map[String, Any]]
        val brokerid = controllerInfo.get("controller_epoch").get //Double类型
        this.logIdent = "[Controller " + brokerid + "]: "
        println("brokerid:" + brokerid)

      case None => throw new KafkaException("Failed to parse the controller info json [%s].".format(controllerInfoString))
    }

    /*    Json.fatal(filecontents)
    Json.trace(filecontents)*/

    //将数据封装成的样子
    var cachedTopicMap: Map[String, (String, Int)] = Map()
    //从zk读取 offset的值  Map[topic,Tuple(zkpath,partitions)]

    JSON.parseFull(filecontents) match {
      case Some(m) =>
        val info = m.asInstanceOf[Map[String, Any]]
        val describe = info.get("describe").get
        println("describe:" + describe)

        val topics = info.get("topics").get.asInstanceOf[List[Map[String, Any]]]

        //println("topics --> " + broker.get("host").get + ":" + broker.get("port").get)
        println("topics --> " + topics)
        topics.map(f => println("topic:" + f.get("topic") + " ,partition num:" + f.get("partitions") + " , zk path:" + f.get("zkpath")))

        topics.map(f =>
          cachedTopicMap += (f.get("topic").get.asInstanceOf[String] ->
            (f.get("zkpath").get.asInstanceOf[String], f.get("partitions").get.asInstanceOf[Double].toInt)))
        println("cachedTopicMap:" + cachedTopicMap)

      case None => println("none")
    }
    println("-->" + cachedTopicMap)
  }
}
