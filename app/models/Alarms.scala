package models

import java.nio.charset.Charset

/**
 * Created by davihe on 15-9-11.
 */
object Alarms {

  implicit object AlarmsWrites extends Writes[Alarms] {
    def writes(alarm: Alarms) = {
      Json.obj(
        "monitorType" -> alarm.MonitorType,
        "monitorTopic" -> alarm.MonitorTopic,
        "monitorIndex" -> alarm.MonitorIndex,
        "monitorValue" -> alarm.MonitorValue
      )
    }
  }

  var alarmMonitorConfig = ""

  def registerConfig(configPath: String): Unit = {
    val userPath = System.getProperty("user.dir")
    val topicPath = if (userPath.isEmpty) configPath else userPath + "/" + configPath
    alarmMonitorConfig = Utils.readFileAsString(topicPath, Charset.defaultCharset())
  }

  def findAll: Seq[Alarms] = {
    registerConfig("conf/alarm-monitor.json")

    val list: MutableList[Alarms] = MutableList()

    JSON.parseFull(alarmMonitorConfig) match {
      case Some(m) =>
        val info = m.asInstanceOf[Map[String, Any]]
        val monitors = info.get("monitors").get.asInstanceOf[List[Map[String, Any]]]

        monitors.map(m => {
          val indexes = m.get("indexes").get.asInstanceOf[List[Map[String, String]]]
          m.get("monitor") match {
            case Some("brokers") => {
              indexes.map(i => {
                val alarms = new Alarms("BROKERS", "-", i.get("index").get, i.get("value").get)
                //println(alarms.toMap)
                list += alarms
              })
            }
            case Some("topics") => {
              indexes.map(i => {
                val alarms = new Alarms("TOPICS", i.get("topic").get, i.get("index").get, i.get("value").get)
                //println(alarms.toMap)
                list += alarms
              })
            }
            case Some("logLag") => {
              indexes.map(f = i => {
                val alarms = new Alarms("LOGLAGS", "ALL-TOPIC", i.get("index").get, i.get("value").get)
                //println(alarms.toMap)
                list += alarms
              })
            }
          }
        })
      case None => println("none")
    }

    list.toSeq
  }
}

case class Alarms(MonitorType: String, MonitorTopic: String, MonitorIndex: String, MonitorValue: String) {

  def toMap: Map[String, Any] = {
    val map = Map("MonitorType" -> MonitorType, "MonitorTopic" -> MonitorTopic, "MonitorIndex" -> MonitorIndex, "MonitorValue" -> MonitorValue)
    map
  }

}