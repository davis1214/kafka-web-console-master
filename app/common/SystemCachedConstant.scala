package common

import java.io.FileInputStream
import java.io.InputStream
import java.util.Properties

/**
 * Created by davihe on 15-9-7.
 */
object SystemCachedConstant {
  val configPath = "conf/system.conf"
  var ins: InputStream = null
  var prop: Properties = null

  def getProperties(propStrs: String): String = {
    if (prop == null) {
      prop = new Properties
      try {
        prop.load(getInputStream())
      }
      catch {
        case e: Exception => e.printStackTrace
      }
    }
    prop.getProperty(propStrs)
  }

  def getInputStream(): InputStream = {
    if (ins == null) {
      val userPath = System.getProperty("user.dir")
      val topicPath = if (userPath.isEmpty) configPath else userPath + "/" + configPath
      ins = new FileInputStream(topicPath);
    }
    ins;
  }

  def main(args: Array[String]) {
    println("-->" + getProperties("weixin.http.url"))
    println("--.")
  }

}
