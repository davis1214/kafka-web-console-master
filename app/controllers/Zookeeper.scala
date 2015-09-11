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

package controllers

import common.{Message, Registry}
import Registry.PropertyConstants

object Zookeeper extends Controller {

  val zookeeperForm = Forms.tuple(
    "name" -> Forms.text,
    "host" -> Forms.text,
    "port" -> Forms.number,
    "group" -> Forms.text,
    "chroot" -> optional(Forms.text),
    "topicConfig" -> optional(Forms.text)
  )

  def index(group: String) = Action {

    if (group.toUpperCase() == "ALL") {
      Ok(Json.toJson(models.Zookeeper.findAll.toSeq))
    }
    else {
      models.Group.findByName(group.toUpperCase) match {
        case Some(z) => Ok(Json.toJson(z.zookeepers))
        case _ => Ok(Json.toJson(List[String]()))
      }
    }
  }

  def create() = Action {
    implicit request =>
      val result = Form(zookeeperForm).bindFromRequest.fold(
        formFailure => BadRequest,
        formSuccess => {

          val name: String = formSuccess._1
          val host: String = formSuccess._2
          val port: Int = formSuccess._3
          val group: String = formSuccess._4
          val chroot: String = formSuccess._5 match {
            case Some(s) => s
            case _ => ""
          }
          val topicConfig: String = formSuccess._6 match {
            case Some(s) => s
            case _ => ""
          }

          //println("----------------------------------->topicConfig:" + topicConfig + " , formSuccess: " + formSuccess.toString())
          val zk = models.Zookeeper.insert(models.Zookeeper(name, host, port, models.Group.findByName(group.toUpperCase).get.id,
            models.Status.Disconnected.id, chroot, topicConfig))

          Akka.system.actorSelection("akka://application/user/router") ! Message.Connect(zk)
          Created
        }
      )

      result
  }

  def delete(name: String) = Action {
    val zk = models.Zookeeper.findByName(name).get
    models.Zookeeper.delete(models.Zookeeper.findById(zk.id).get)

    //更新brokerhistoryinfo、topichistoryinfo对应zkname的状态为无效
    models.BrokerHistoryInfo.updateStatusByZkName(name, 0)
    models.TopicHistoryInfo.updateStatusByZkName(name, 0)

    Akka.system.actorSelection("akka://application/user/router") ! Message.Disconnect(zk)
    NoContent
  }

  def feed() = WebSocket.using[String] {
    implicit request =>
      val in = Iteratee.ignore[String]
      val out = Registry.lookupObject(PropertyConstants.BroadcastChannel) match {
        case Some(broadcastChannel: (_, _)) => broadcastChannel._1.asInstanceOf[Enumerator[String]]
        case _ => Enumerator.empty[String]
      }

      (in, out)
  }
}