package actors

import common.Message
import java.util.Properties
import models.Settings
import java.util.concurrent.TimeUnit
import java.nio.charset.Charset

/**
 * Created by davihe on 15-9-10.
 */
private class AlarmExecutor extends Job {
  def execute(ctx: JobExecutionContext) {
    val actor = ctx.getJobDetail.getJobDataMap().get("alarmActor").asInstanceOf[ActorRef]
    actor ! Message.Purge
  }
}

class AlarmManager extends Actor {
  private var alarmPointsTask: Cancellable = null
  private val JobKey = "alarmMonitor"
  private[this] val props = new Properties()
  props.setProperty("org.quartz.scheduler.instanceName", context.self.path.name)
  props.setProperty("org.quartz.threadPool.threadCount", "1")
  props.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore")
  props.setProperty("org.quartz.scheduler.skipUpdateCheck", "true")
  val scheduler = new StdSchedulerFactory(props).getScheduler

  override def preStart() {
    registerConfig("conf/alarm-monitor.json")
    scheduler.start()
    schedule()
    self ! Message.FetchAlarmMonitorInfo
  }

  override def postStop() {
    scheduler.shutdown()
  }

  override def receive: Receive = {
    case Message.FetchAlarmMonitorInfo => {
      fetchAlarmMonitorInfoPoints()
      alarmPointsTask = Akka.system.scheduler.scheduleOnce(Duration.create(Settings.findByPurgeType(Settings.PurgeTypeAlarm.toString).get.FetchInterval.toLong, TimeUnit.SECONDS), self,
        Message.FetchAlarmMonitorInfo)
    }
    case Message.Purge => {
      Logger.warn(" Message.Purge in AlarmMonitorManager")
    }
    case _ => Logger.warn("AlarmMonitorManager: undesired messages received!")
  }

  private def schedule() {
    val jdm = new JobDataMap()
    jdm.put("alarmActor", self)
    val job = JobBuilder.newJob(classOf[AlarmExecutor]).withIdentity(JobKey).usingJobData(jdm).build()
    scheduler.scheduleJob(job, TriggerBuilder.newTrigger().startNow().forJob(job).withSchedule(CronScheduleBuilder.cronSchedule(Settings.findByPurgeType(Settings.PurgeTypeAlarm
      .toString).get.PurgeSchedule)).build())
  }

  private def fetchAlarmMonitorInfoPoints() {
    Logger.info("fetchAlarmMonitorInfoPoints in AlarmMonitorManager")
    if (!alarmMonitorConfig.isEmpty)
      JSON.parseFull(alarmMonitorConfig) match {
        case Some(m) =>
          val info = m.asInstanceOf[Map[String, Any]]
          val monitors = info.get("monitors").get.asInstanceOf[List[Map[String, Any]]]

          monitors.map(m => {
            val indexes = m.get("indexes").get.asInstanceOf[List[Map[String, String]]]
            m.get("monitor") match {
              case Some("brokers") => models.BrokerHistoryInfo.brokerMonitor(indexes)
              case Some("topics") => {
                indexes.map(i => {
                  models.TopicHistoryInfo.topicMonitor(i.get("topic").get, i.get("index").get, i.get("value").get)
                })
              }
              case Some("logLag") => {
                indexes.map(i => {
                  models.TopicHistoryInfo.logLagMonitor(i.get("value").get)
                })
              }
            }
          })

        case None => println("none")
      }
  }

  var alarmMonitorConfig = ""

  def registerConfig(configPath: String): Unit = {
    val userPath = System.getProperty("user.dir")
    val topicPath = if (userPath.isEmpty) configPath else userPath + "/" + configPath
    alarmMonitorConfig = Utils.readFileAsString(topicPath, Charset.defaultCharset())
  }
}