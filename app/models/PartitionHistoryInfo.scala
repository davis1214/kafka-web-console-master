package models

import models.Database._
import java.io.IOException

/**
 * Created by davihe on 15-7-21.
 */
object PartitionHistoryInfo {

  //topicHistoryId, partitionId,logSize,leader
  implicit object PartitionHistoryInfoWrites extends Writes[PartitionHistoryInfo] {
    def writes(partitionHistoryInfo: PartitionHistoryInfo) = {
      Json.obj(
        "topicHistoryId" -> partitionHistoryInfo.topicHistoryId,
        "partitionId" -> partitionHistoryInfo.partitionId,
        "logSize" -> partitionHistoryInfo.logSize,
        "logLag" -> partitionHistoryInfo.logLag,
        "leader" -> partitionHistoryInfo.leader
      )
    }
  }

  def insert(partitionHistoryInfo: PartitionHistoryInfo) = inTransaction {
    try {
      val t = partitionHistoryInfoTable.insert(partitionHistoryInfo)
    } catch {
      case ex: IOException => println("io exception:" + ex.getMessage + " --> " + ex.printStackTrace())
      case ex: Exception => println("exception:" + ex.getMessage + " --> " + ex.printStackTrace())
    }
  }

  //topicHistoryId, partitionId,logSize,leader
  case class PartitionHistoryInfo(var topicHistoryId: Long, partitionId: Long = 0, var logSize: Double = 0,
                                  var logLag: Double = 0, var leader: String)
    extends KeyedEntity[Long] {

    override val id: Long = 0

    override def toString = "%d:%d/%s".format(topicHistoryId, logSize, leader)

    def toMap: Map[String, Any] = {
      val map = Map("topicHistoryId" -> topicHistoryId, "partitionId" -> partitionId, "logSize" -> logSize, "logLag" -> logLag, "leader" -> leader)
      map
    }

    def this(topicHistoryId: Long, partitionId: Long) {
      this(topicHistoryId, partitionId, logSize = 0, logLag = 0, leader = "")
    }

    def this(partitionId: Long, logSize: Double, logLag: Double, leader: String) {
      this(topicHistoryId = 0, partitionId, logSize, logLag, leader)
    }
  }

}
