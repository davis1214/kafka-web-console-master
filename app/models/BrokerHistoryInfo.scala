package models

import models.Database._
import java.io.IOException
import util.WeixinUtil

/**
 * Created by davihe on 15-7-18.
 */
object BrokerHistoryInfo {

  implicit object BrokerHistoryInfoWrites extends Writes[BrokerHistoryInfo] {
    def writes(brokerHistoryInfo: BrokerHistoryInfo) = {
      Json.obj(
        "name" -> brokerHistoryInfo.name,
        "host" -> brokerHistoryInfo.host,
        "port" -> brokerHistoryInfo.port,
        "messagesIn" -> brokerHistoryInfo.messagesIn,
        "dataFlowIn" -> brokerHistoryInfo.dataFlowIn,
        "dataFlowOut" -> brokerHistoryInfo.dataFlowOut,
        "leaderElectionRate" -> brokerHistoryInfo.leaderElectionRate,
        "loadAverage" -> brokerHistoryInfo.loadAverage,
        "disIo" -> brokerHistoryInfo.disIo,
        "heapUageRate" -> brokerHistoryInfo.heapUageRate,
        "gcTime" -> brokerHistoryInfo.gcTime,
        "gcCount" -> brokerHistoryInfo.gcCount,
        "time" -> brokerHistoryInfo.time.toLocaleString
      )
    }
  }

  //UPDATE brokerHistoryInfo SET isCurrentTime = 0 WHERE isCurrentTime = 1 AND HOST = ? AND PORT = ? AND NAME = ?
  def update(brokerHistoryInfo: BrokerHistoryInfo) = inTransaction {
    try {
      brokerHistoryInfoTable.update(brokerHistoryInfo)
    } catch {
      case ex: IOException => Logger.error(ex.getMessage, ex)
      case ex: Exception => Logger.error(ex.getMessage, ex)
    }
  }

  def updateStatusByZkName(zookeeper: String, isCurrentTime: Long) = inTransaction {
    try {
      val brokerHistoryInfos = from(brokerHistoryInfoTable)(oH => where(oH.isCurrentTime === 1 and oH.name === zookeeper) select (oH)).toList

      brokerHistoryInfos.map(m => {
        m.isCurrentTime = isCurrentTime
        brokerHistoryInfoTable.update(m)
      })
    } catch {
      case ex: IOException => Logger.error(ex.getMessage, ex)
      case ex: Exception => Logger.error(ex.getMessage, ex)
    }
  }


  //UPDATE brokerHistoryInfo SET isCurrentTime = 0 WHERE isCurrentTime = 1 AND HOST = ? AND PORT = ? AND NAME = ?
  def updateByIsCurrentTime(name: String, host: String, port: String, isCurrentTime: Long) = inTransaction {
    try {
      val brokerHistoryInfos = from(brokerHistoryInfoTable)(oH => where(oH.isCurrentTime === isCurrentTime and oH.name === name
        and oH.host === host and oH.port === port) select (oH)).toList

      brokerHistoryInfos.map(m => {
        m.isCurrentTime = 0
        brokerHistoryInfoTable.update(m)
      })

    } catch {
      case ex: IOException => Logger.error(ex.getMessage, ex)
      case ex: Exception => Logger.error(ex.getMessage, ex)
    }
  }

  def insert(brokerHistoryInfo: BrokerHistoryInfo) = inTransaction {
    //println("-->" + brokerHistoryInfo.toMap)
    try {
      brokerHistoryInfo.isCurrentTime = 1
      brokerHistoryInfoTable.insert(brokerHistoryInfo)
    } catch {
      case ex: IOException => Logger.error(ex.getMessage, ex)
      case ex: Exception => Logger.error(ex.getMessage, ex)
    }
  }

  def truncate() = inTransaction {
    Session.currentSession.connection.createStatement().executeUpdate("TRUNCATE TABLE brokerHistoryInfo;")
  }

  def brokerMonitor(indexes: List[Map[String, String]]) = inTransaction {
    try {
      val brokerHistoryInfos = from(brokerHistoryInfoTable)(oH => where(oH.isCurrentTime === 1) select (oH)).toList
      brokerHistoryInfos.map(m => {
        indexes.map(i => {
          //println("monitor brokers with index :" + i.get("index") + " , " + i.get("value"))
          val monitorIndex = i.get("index").get
          val monitorValue = i.get("value").get

          val value = monitorIndex.toUpperCase match {
            case "MESSAGESIN" => m.messagesIn
            case "DATAFLOWIN" => m.dataFlowIn
            case "DATAFLOWOUT" => m.dataFlowOut
            case "LEADERELECTIONRATE" => m.leaderElectionRate
            case "LOADAVERAGE" => m.loadAverage
            case "DISIO" => m.disIo
            case "HEAPUAGERATE" => m.heapUageRate.substring(0, m.heapUageRate.length - 1)
            case "GCTIME" => m.gcTime
            case "GCCOUNT" => m.gcCount
          }

          if (value.toString.toDouble > monitorValue.toDouble) {
            val key = "broker " + m.host + " in configed-zk " + m.name + " " + monitorIndex
            val alarmContent = key + " is " + value + " higher than configed " + monitorValue
            WeixinUtil.sendMsg(key, alarmContent)
          }
        })
      })
    } catch {
      case ex: IOException => Logger.error(ex.getMessage, ex)
      case ex: Exception => Logger.error(ex.getMessage, ex)
    }
  }

  //根据状态查询数据 0:非当前时间，1：当前时间
  def findCurrentTimeBrokerInfo(isCurrentTime: Long) = inTransaction {
    from(brokerHistoryInfoTable)(oH => where(oH.isCurrentTime === isCurrentTime) select (oH) orderBy (oH.name asc)).toList
  }

  //包含broker的host、port、数据流量、流入速度、流出速度、leader 选举频率、cpu、disk io、memory，gc时间、gc次数 以及时间戳信息
  case class BrokerHistoryInfo(name: String, host: String, port: String, var messagesIn: String = "", var dataFlowIn: String = "",
                               var dataFlowOut: String = "", var leaderElectionRate: String = "", var loadAverage: String = "",
                               var disIo: String = "", var heapUageRate: String = "", var gcTime: String = "",
                               var gcCount: String = "", time: java.sql.Timestamp, var isCurrentTime: Long = 0)
    extends KeyedEntity[Long] {

    override val id: Long = 0

    override def toString = "%s:%s/%s".format(host, port, name)

    def toMap: Map[String, Any] = {

      val map = Map("name" -> name, "host" -> host, "port" -> port, "messagesIn" -> messagesIn, "dataFlowIn" -> dataFlowIn,
        "dataFlowOut" -> dataFlowOut, "leaderElectionRate" -> leaderElectionRate, "loadAverage" -> loadAverage, "disIo" -> disIo, "heapUageRate" -> heapUageRate,
        "gcTime" -> gcTime, "gcCount" -> gcCount, "time" -> time, "isCurrentTime" -> isCurrentTime)
      map
    }

    def this(name: String, host: String, port: String, time: java.sql.Timestamp) {
      this(name, host, port, messagesIn = "", dataFlowIn = "",
        dataFlowOut = "", leaderElectionRate = "", loadAverage = "", disIo = "", heapUageRate = "",
        gcTime = "", gcCount = "", time, isCurrentTime = 0)
    }
  }

}
