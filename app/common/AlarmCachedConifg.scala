package common


/**
 * Created by davihe on 15-9-9.
 */
object AlarmCachedConifg {
  //缓存告警的信息
  val cachedAlarms: Map[String, Long] = Map()
  val interval = SystemCachedConstant.getProperties("alarm.message.send.interval").toLong

  def registerAlarm(key: String, value: Long): Boolean = {
    val toSendAlarm = cachedAlarms.get(key) match {
      case None => {
        Logger.info("alarm " + key + " not ocurred")
        cachedAlarms += (key -> value)
        true
      }
      case Some(last) => {
        val lastTime = last.asInstanceOf[Long]
        if ((value - lastTime) > interval * 1000) {
          cachedAlarms.remove(key)
          true
        } else
          false
      }
    }
    //println(cachedAlarms)
    //Logger.debug("send alarm [" + key + "] " + toSendAlarm)
    toSendAlarm
  }

}
