package com.sohu.common


object PartitionMap {

  def main(args: Array[String]): Unit = {

    val m1 = Map("id" -> 0, "logSize" -> 0, "leader" -> "127.0.0.1:9093")
    val m2 = Map("id" -> 1, "logSize" -> 0, "leader" -> "127.0.0.1:9093")

    val partitionMap = scala.collection.mutable.ArrayBuffer(m1, m2)

    println("-->" + partitionMap.filter(p => (p.get("id").get.toString().toLong != 0)))

    val firstPartition = partitionMap.filter(_.get("id").get.toString().toDouble.toInt == 0).head
    //val leader = firstPartition.map(p => p.get("leader"))
    val leader = firstPartition.get("leader")
    println("leader:" + leader)

    val jm1 = Map("jmx_port" -> 9997, "timestamp" -> "1431489786576", "host" -> "10.16.10.197", "version" -> 1, "port" -> 8092)
    val jm2 = Map("jmx_port" -> 9997, "timestamp" -> "1431489786576", "host" -> "10.16.10.198", "version" -> 1, "port" -> 8092)
    val jm3 = Map("jmx_port" -> 9997, "timestamp" -> "1431489786576", "host" -> "10.16.10.200", "version" -> 1, "port" -> 8092)

    val brokersInfos = scala.collection.mutable.ArrayBuffer(jm1, jm2, jm3)

    val brokerInfoMaps = Map(1 -> jm1, 2 -> jm2, 3 -> jm3)

    //val brokersAddr = brokersInfo.map(bi => (bi._1, bi._2.get("host").get + ":" + bi._2.get("port").get.toString.toDouble.toInt))

    val brokersAddr = brokerInfoMaps.map(bi => {
      //                val map = Map[Int, String](bi._1 -> (bi._2.get("host").get + ":" + bi._2.get("port").get.toString.toDouble.toInt), (bi._1 + 10) -> (bi._2.get("host").get + ":" + bi._2.get("port").get.toString.toDouble.toInt))
      //                map

      (bi._1, bi._2.get("host").get + ":" + bi._2.get("port").get.toString.toDouble.toInt)
      //        if (bi._1 == 1) {
      //          (bi._1 + 10, bi._2.get("host").get + ":" + bi._2.get("jmx_port").get.toString.toDouble.toInt)
      //        }

    })

    println("\n--->brokersAddr:" + brokersAddr + "\n")

    val brokerJmxAddr = brokerInfoMaps.filter(p => p._1 == 1).map(bi => ("jmxAddr", bi._2.get("host").get + ":" + bi._2.get("port").get.toString.toDouble.toInt))

    val broker = brokerInfoMaps.map(bi => (bi._1, bi._2.get("host").get + ":" + bi._2.get("port").get.toString.toDouble.toInt))
      .++:(brokerInfoMaps.map(bi => (9999, bi._2.get("host").get + ":" + bi._2.get("jmx_port").get.toString.toDouble.toInt)))

    val partitionsWithoutLeaders = brokerInfoMaps.filterNot(b => b._2.headOption match {
      case Some(s) => true
      case _ => false
    })

    val br = broker.++:(partitionsWithoutLeaders)
    println("brokerInfoMaps:" + brokerInfoMaps)
    println("brokerAddr:" + brokersAddr + " , brokerJmxAddr:" + brokerJmxAddr)

    println("new brokersAddr : " + brokersAddr.++:(brokerJmxAddr))
    println("new brokersAddr : " + br)

    //brokersAddr.sortBy(pb => pb._1.toInt).map(pb => pb._2)

    //    println("brokerAddr(flatten):" + brokersAddr.flatten)
    //    println("brokerAddr:" + brokersAddr)
    //    println("brokerAddr:" + brokersAddr.flatMap(_.toMap))

    //    val xs = Map("a" -> List(11, 111), "b" -> List(22, 222)).flatMap(_._2)
    //    println("xs:" + xs)
    //
    //    val ys = Map("a" -> List(1 -> 11, 1 -> 111), "b" -> List(2 -> 22, 2 -> 222)).flatMap(_._2)
    //    println("ys:" + ys)

  }
}
