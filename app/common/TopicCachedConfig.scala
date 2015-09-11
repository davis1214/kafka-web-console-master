package common

import java.nio.charset.Charset

/**
 * Created by davihe on 15-8-19.
 */
object TopicCachedConfig {

  var cachedTopicMap: Map[String, (String, Int)] = Map()

  def registerConfig(configPath: String): Unit = {
    val userPath = System.getProperty("user.dir")
    val topicPath = if (userPath.isEmpty) configPath else userPath + "/" + configPath
    val content = Utils.readFileAsString(topicPath, Charset.defaultCharset())

    JSON.parseFull(content) match {
      case Some(m) =>
        val info = m.asInstanceOf[Map[String, Any]]
        val topics = info.get("topics").get.asInstanceOf[List[Map[String, Any]]]

        topics.map(f =>
          cachedTopicMap += (f.get("topic").get.asInstanceOf[String] ->
            (f.get("zkpath").get.asInstanceOf[String], f.get("partitions").get.asInstanceOf[Double].toInt)))

      case None => println("none")
    }

  }

}
