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

import util.Util
import Util._
import models.Zookeeper

object Broker extends Controller {

  implicit object BrokerWrites extends Writes[Seq[(String, Map[String, Any])]] {
    def writes(l: Seq[(String, Map[String, Any])]) = {
      val brokers = l.map {
        i =>

          val fields = i._2.map {
            kv =>
              kv._2 match {
                case v: Double => (kv._1, v.toInt.toString)
                case _ => (kv._1, kv._2.toString())
              }
          }

          fields + ("zookeeper" -> i._1)
      }
      Json.toJson(brokers)
    }
  }

  def index = Action {
    //val brokers = connectedZookeepers { (zk, zkClient) => getBrokers(zk, zkClient)}
    //Future.sequence(brokers).map(l => Ok(Json.toJson(l.flatten)))

    Ok(Json.toJson(models.BrokerHistoryInfo.findCurrentTimeBrokerInfo(1)))
  }

  private def getBrokers(zk: Zookeeper, zkClient: ZkClient): Future[Seq[(String, Map[String, Any])]] = {
    return for {
      brokerIds <- getZChildren(zkClient, "/brokers/ids/*")
      brokers <- Future.sequence(brokerIds.map(brokerId => twitterToScalaFuture(brokerId.getData())))
    } yield brokers.map(b => (zk.name, scala.util.parsing.json.JSON.parseFull(new String(b.bytes)).get.asInstanceOf[Map[String, Any]]))
  }

}
