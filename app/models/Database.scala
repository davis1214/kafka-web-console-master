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

package models

import models.BrokerHistoryInfo.BrokerHistoryInfo
import models.TopicHistoryInfo.TopicHistoryInfo
import models.PartitionHistoryInfo.PartitionHistoryInfo

object Database extends Schema {
  val zookeepersTable = table[Zookeeper]("zookeepers")
  val groupsTable = table[Group]("groups")
  val statusTable = table[Status]("status")
  val offsetPointsTable = table[OffsetPoint]("offsetPoints")
  val offsetHistoryTable = table[OffsetHistory]("offsetHistory")
  val settingsTable = table[Settings]("settings")

  //TODO develop
  val brokerHistoryInfoTable = table[BrokerHistoryInfo]("brokerHistoryInfo")
  val topicHistoryInfoTable = table[TopicHistoryInfo]("topicHistoryInfo")

  //含有外键关系的
  val partitionHistoryInfoTable = table[PartitionHistoryInfo]("partitionHistoryInfo")

  val groupToZookeepers = oneToManyRelation(groupsTable, zookeepersTable).via((group, zk) => group.id === zk.groupId)
  val statusToZookeepers = oneToManyRelation(statusTable, zookeepersTable).via((status, zk) => status.id === zk.statusId)
  val offsetHistoryToOffsetPoints = oneToManyRelation(offsetHistoryTable, offsetPointsTable).via((offsetHistory, offsetPoint) => offsetHistory.id === offsetPoint.offsetHistoryId)
  val zookeeperToOffsetHistories = oneToManyRelation(zookeepersTable, offsetHistoryTable).via((zookeeper, offsetHistory) => zookeeper.id === offsetHistory.zookeeperId)

  on(this.brokerHistoryInfoTable) {
    brokerHistoryInfo =>
      declare(
        brokerHistoryInfo.id is(primaryKey, autoIncremented),
        brokerHistoryInfo.name is (unique)
      )
  }

  on(this.topicHistoryInfoTable) {
    topicHistoryInfo =>
      declare(
        topicHistoryInfo.id is(primaryKey, autoIncremented),
        topicHistoryInfo.name is (unique)
      )
  }

  on(this.partitionHistoryInfoTable) {
    partitionHistoryInfo =>
      declare(
        partitionHistoryInfo.id is(primaryKey, autoIncremented)
      )
  }

  on(this.settingsTable) {
    settingInfo =>
      declare(
        settingInfo.id is(primaryKey, autoIncremented)
      )
  }

  on(this.zookeepersTable) {
    zookeeper =>
      declare(
        zookeeper.id is(primaryKey, autoIncremented),
        zookeeper.name is (unique)
      )
  }

  on(this.groupsTable) {
    group =>
      declare(
        group.id is(primaryKey, autoIncremented),
        group.name is (unique)
      )
  }

  on(this.statusTable) {
    status =>
      declare(
        status.id is(primaryKey, autoIncremented),
        status.name is (unique)
      )
  }

  on(this.offsetPointsTable) {
    offsetPoint =>
      declare(
        offsetPoint.id is(primaryKey, autoIncremented)
      )
  }

  on(this.offsetHistoryTable) {
    offsetHistory =>
      declare(
        offsetHistory.id is(primaryKey, autoIncremented)
      )
  }

}
