/*
 * Copyright 2014 Claude Mamo
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package actors

import models._
import util.Util
import Util._
import common.Message
import java.util.concurrent.TimeUnit
import java.sql.Timestamp
import java.util.{Properties, Date}

private class Executor() extends Job {
  def execute(ctx: JobExecutionContext) {
    val actor = ctx.getJobDetail.getJobDataMap().get("actor").asInstanceOf[ActorRef]
    actor ! Message.Purge
  }
}

class OffsetHistoryManager extends Actor {

  private var fetchOffsetPointsTask: Cancellable = null
  private val JobKey = "purge"
  private[this] val props = new Properties()

  props.setProperty("org.quartz.scheduler.instanceName", context.self.path.name)
  props.setProperty("org.quartz.threadPool.threadCount", "1")
  props.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore")
  props.setProperty("org.quartz.scheduler.skipUpdateCheck", "true")
  val scheduler = new StdSchedulerFactory(props).getScheduler

  override def preStart() {
    scheduler.start()
    schedule()
    self ! Message.FetchOffsets
  }

  override def postStop() {
    scheduler.shutdown()
  }

  override def receive: Receive = {
    case Message.FetchOffsets => {
      fetchOffsetPoints()
      fetchOffsetPointsTask = Akka.system.scheduler.scheduleOnce(Duration.create(Settings.findByPurgeType(Settings.PurgeTypeOther.toString).get.FetchInterval.toLong,
        TimeUnit.SECONDS), self, Message.FetchOffsets)
    }
    case Message.SettingsUpdateNotification => {
      scheduler.deleteJob(new JobKey(JobKey))
      schedule()
      fetchOffsetPointsTask.cancel()
      Akka.system.scheduler.scheduleOnce(Duration.create(Settings.findByPurgeType(Settings.PurgeTypeOther.toString).get.FetchInterval.toLong, TimeUnit.SECONDS), self, Message.FetchOffsets)
    }
    case Message.Purge => {
      OffsetPoint.truncate()
      OffsetHistory.truncate()
    }
    case _ =>
  }

  private def getOffsetHistory(zk: Zookeeper, topic: (String, Seq[String])): OffsetHistory = {
    OffsetHistory.findByZookeeperIdAndTopic(zk.id, topic._1) match {
      case None => OffsetHistory.insert(OffsetHistory(zk.id, topic._1))
      case Some(oH) => oH
    }
  }

  private def persistOffsetPoint(partitionOffsets: Map[String, Seq[Long]], offsetHistory: OffsetHistory, partitionsLogSize: Seq[Long]) {
    val timestamp = new Timestamp(new Date().getTime)
    for (e <- partitionOffsets) {
      for ((p, i) <- e._2.zipWithIndex) {
        OffsetPoint.insert(OffsetPoint(e._1, timestamp, offsetHistory.id, i, p, partitionsLogSize(i)))
      }

    }
  }

  private def schedule() {
    val jdm = new JobDataMap()
    jdm.put("actor", self)
    val job = JobBuilder.newJob(classOf[Executor]).withIdentity(JobKey).usingJobData(jdm).build()
    scheduler.scheduleJob(job, TriggerBuilder.newTrigger().startNow().forJob(job).withSchedule(CronScheduleBuilder.cronSchedule(Settings.findByPurgeType(Settings.PurgeTypeTopic
      .toString).get.PurgeSchedule)).build())
  }

  private def fetchOffsetPoints() {
    connectedZookeepers {
      (zk, zkClient) =>
        for {
          topics <- getTopics(zkClient)
          topic = topics.map {
            t =>
              for {
                partitionLeaders <- getPartitionLeaders(t._1, zkClient)
                partitionsLogSize <- getPartitionsLogSize(t._1, partitionLeaders)
                partitionOffsets <- getPartitionOffsets(t._1, zkClient)
              } yield persistOffsetPoint(partitionOffsets, getOffsetHistory(zk, t), partitionsLogSize)
          }
        } yield None
    }
  }
}
