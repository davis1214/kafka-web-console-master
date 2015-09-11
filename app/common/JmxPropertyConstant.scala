package common

/**
 * Created by davihe on 15-7-27. singleton
 */
object JmxPropertyConstant {

  val KAFKA_SERVER = "\"kafka.server\""
  val KAFKA_CONTROLLER = "\"kafka.controller\""


  val JMX_ADDR_STRINGS = "${jmxAddr}"
  val JMX_URL_WITH_UNCERTAIN_ADDR = "service:jmx:rmi:///jndi/rmi://${jmxAddr}/jmxrmi";

  val MONITOR_INDEX_KAFKA_TOPICS = List("BytesInPerSec", "BytesOutPerSec", "FailedFetchRequestsPerSec", "FailedProduceRequestsPerSec", "LogBytesAppendedPerSec", "MessagesInPerSec")
  val MONITOR_INDEX_KAFKA_BROKERS = List("\"AllTopicsBytesInPerSec\"", "\"AllTopicsBytesOutPerSec\"", "\"AllTopicsMessagesInPerSec\"")
  val MONITOR_INDEX_KAFKA_LEADERS = List("\"LeaderElectionRateAndTimeMs\"")

  val KAFKA_CONTROLLER_LEADER_ELECTION_RATE_AND_TIMEMS = "\"kafka.controller\":type=\"ControllerStats\",name=\"LeaderElectionRateAndTimeMs\""
  val JAVA_LANG_GARBAGE_COLLECTOR_CONCURRENTMARKSWEEP = "java.lang:type=GarbageCollector,name=ConcurrentMarkSweep"
  //("java.lang:type=OperatingSystem"), "SystemLoadAverage"
  val JAVA_LANG_OPERATING_SYSTEM = "java.lang:type=OperatingSystem"

  val COUNT = "Count"
  val COLLECT_COUNT = "CollectionCount"
  val COLLECT_TIME = "CollectionTime"
  val SYSTEM_LOAD_AVERAGE = "SystemLoadAverage"

}
