package models

import models.Database._
import java.io.IOException
import util.WeixinUtil

/**
 * Created by davihe on 15-8-21.
 */
object TopicHistoryInfo {

  // name,zookeeper,logSize, bytesInPerSec, bytesOutPerSec,failedFetchRequestsPerSec,failedProduceRequestsPerSec,
  // logBytesAppendedPerSec,messagesInPerSec
  implicit object TopicHistoryInfoWrites extends Writes[TopicHistoryInfo] {
    def writes(topicHistoryInfo: TopicHistoryInfo) = {

      val partitionHistoryInfos = findPartitionHistoryInfosByTopicHisotoryId(topicHistoryInfo.id)

      Json.obj(
        "name" -> topicHistoryInfo.name,
        "zookeeper" -> topicHistoryInfo.zookeeper,
        "logSize" -> topicHistoryInfo.logSize,
        "logLag" -> topicHistoryInfo.logLag,
        "bytesInPerSec" -> topicHistoryInfo.bytesInPerSec,
        "bytesOutPerSec" -> topicHistoryInfo.bytesOutPerSec,
        "failedFetchRequestsPerSec" -> topicHistoryInfo.failedFetchRequestsPerSec,
        "failedProduceRequestsPerSec" -> topicHistoryInfo.failedProduceRequestsPerSec,
        "logBytesAppendedPerSec" -> topicHistoryInfo.logBytesAppendedPerSec,
        "messagesInPerSec" -> topicHistoryInfo.messagesInPerSec,
        "time" -> topicHistoryInfo.time.toLocaleString,
        "partitions" -> partitionHistoryInfos
      )
    }
  }

  //ArrayBuffer(PartitionHistoryInfo(0,0,462580,127.0.0.1:9093), PartitionHistoryInfo(0,1,385483,127.0.0.1:9093))
  def insert(topicHistoryInfo: TopicHistoryInfo): Long = inTransaction {
    topicHistoryInfo.isCurrentTime = 1
    val t = topicHistoryInfoTable.insert(topicHistoryInfo)
    t.id
  }

  //UPDATE topicHistoryInfo SET isCurrentTime = 0 WHERE isCurrentTime = 1 AND zookeeper = ? AND NAME = ?
  def updateByIsCurrentTime(name: String, zookeeper: String, isCurrentTime: Long) = inTransaction {
    try {
      val topicHistoryInfos = from(topicHistoryInfoTable)(oH => where(oH.isCurrentTime === isCurrentTime and oH.name === name
        and oH.zookeeper === zookeeper) select (oH)).toList

      topicHistoryInfos.map(m => {
        m.isCurrentTime = 0
        topicHistoryInfoTable.update(m)
      })

    } catch {
      case ex: IOException => Logger.error(ex.getMessage, ex)
      case ex: Exception => Logger.error(ex.getMessage, ex)
    }
  }

  def updateStatusByZkName(zookeeper: String, isCurrentTime: Long) = inTransaction {
    try {
      val topicHistoryInfos = from(topicHistoryInfoTable)(oH => where(oH.isCurrentTime === 1 and oH.zookeeper === zookeeper) select (oH)).toList
      topicHistoryInfos.map(m => {
        m.isCurrentTime = isCurrentTime
        topicHistoryInfoTable.update(m)
      })
    } catch {
      case ex: IOException => Logger.error(ex.getMessage, ex)
      case ex: Exception => Logger.error(ex.getMessage, ex)
    }
  }

  def findCurrentTimeTopicInfo(isCurrentTime: Long) = inTransaction {
    from(topicHistoryInfoTable)(oH => where(oH.isCurrentTime === isCurrentTime) select (oH) orderBy (oH.zookeeper asc)).toList
  }

  def findPartitionHistoryInfosByTopicHisotoryId(topicHisotoryId: Long) = inTransaction {
    from(partitionHistoryInfoTable)(oH => where(oH.topicHistoryId === topicHisotoryId) select (oH)).toList
  }

  def logLagMonitor(monitorValue: String) = inTransaction {
    try {
      val topicHistoryInfos = from(topicHistoryInfoTable)(oH => where(oH.isCurrentTime === 1) select (oH)).toList
      topicHistoryInfos.map(topicHistory => {
        //延迟的百分率不好计算，暂时用大于某个数值
        val logLag = topicHistory.logLag
        if (logLag > monitorValue.toDouble) {
          val key = "topic " + topicHistory.name + " in configed-zk " + topicHistory.zookeeper
          val alarmContent = key + " log lags " + logLag + " messages"
          WeixinUtil.sendMsg("logLag " + key, alarmContent)
        }
      })
    } catch {
      case ex: IOException => Logger.error(ex.getMessage, ex)
      case ex: Exception => Logger.error(ex.getMessage, ex)
    }
  }

  def topicMonitor(topic: String, monitorIndex: String, monitorValue: String) = inTransaction {
    try {
      val topicHistoryInfos = from(topicHistoryInfoTable)(oH => where(oH.isCurrentTime === 1 and oH.name === topic) select (oH)).toList
      topicHistoryInfos.map(m => {
        val value: Double = monitorIndex.toUpperCase match {
          case "BYTESINPERSEC" => m.bytesInPerSec
          case "BYTESOUTPERSEC" => m.bytesOutPerSec
          case "MESSAGESINPERSEC" => m.messagesInPerSec
          case "LOGBYTESAPPENDEDPERSEC" => m.logBytesAppendedPerSec
          case "FAILEDFETCHREQUESTSPERSEC" => m.failedFetchRequestsPerSec
          case "FAILEDPRODUCEREQUESTSPERSEC" => m.failedProduceRequestsPerSec
        }
        if (value > monitorValue.toDouble) {
          val key = "topic " + topic + " in configed-zk " + m.zookeeper + " " + monitorIndex
          val alarmContent = key + " is " + value + " higher than configed " + monitorValue
          WeixinUtil.sendMsg(key, alarmContent)
        }
      })
    } catch {
      case ex: IOException => Logger.error(ex.getMessage, ex)
      case ex: Exception => Logger.error(ex.getMessage, ex)
    }
  }

  //name,zookeeper,logSize, bytesInPerSec, bytesOutPerSec,failedFetchRequestsPerSec,failedProduceRequestsPerSec,
  // logBytesAppendedPerSec,messagesInPerSec
  case class TopicHistoryInfo(name: String, zookeeper: String, var logSize: Double, var logLag: Double, var bytesInPerSec: Double = 0,
                              var bytesOutPerSec: Double = 0, var failedFetchRequestsPerSec: Double = 0,
                              var failedProduceRequestsPerSec: Double = 0, var logBytesAppendedPerSec: Double = 0,
                              var messagesInPerSec: Double = 0, time: java.sql.Timestamp, var isCurrentTime: Long = 0)
    extends KeyedEntity[Long] {

    override val id: Long = 0

    override def toString = "%s:%s/%f".format(name, zookeeper, time)

    def toMap: Map[String, Any] = {
      val map = Map("name" -> name, "zookeeper" -> zookeeper, "logSize" -> logSize, "logLag" -> logLag, "bytesInPerSec" -> bytesInPerSec, "bytesOutPerSec" -> bytesOutPerSec,
        "failedFetchRequestsPerSec" -> failedFetchRequestsPerSec, "failedProduceRequestsPerSec" -> failedProduceRequestsPerSec,
        "logBytesAppendedPerSec" -> logBytesAppendedPerSec, "messagesInPerSec" -> messagesInPerSec, "time" -> time, "isCurrentTime" -> isCurrentTime)

      map
    }

    def this(name: String, zookeeper: String, time: java.sql.Timestamp) {
      this(name, zookeeper, logSize = 0, logLag = 0, bytesInPerSec = 0, bytesOutPerSec = 0, failedFetchRequestsPerSec = 0,
        failedProduceRequestsPerSec = 0, logBytesAppendedPerSec = 0, messagesInPerSec = 0, time, isCurrentTime = 0)
    }
  }

}
