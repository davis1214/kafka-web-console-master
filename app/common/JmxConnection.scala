package common

import javax.management.{ObjectName, MBeanServerConnection}
import javax.management.remote.{JMXServiceURL, JMXConnectorFactory}

/**
 * Created by davihe on 15-7-28.
 */
trait JmxConnection {

  def getJmxConn(host: Any, jmx_port: AnyVal): MBeanServerConnection = {
    val serviceAddress = new JMXServiceURL(JmxPropertyConstant.JMX_URL_WITH_UNCERTAIN_ADDR.replace(JmxPropertyConstant.JMX_ADDR_STRINGS, host + ":" + jmx_port))
    JMXConnectorFactory.connect(serviceAddress).getMBeanServerConnection
  }

  def getJmxConn(address: String): MBeanServerConnection = {
    val serviceAddress = new JMXServiceURL(JmxPropertyConstant.JMX_URL_WITH_UNCERTAIN_ADDR.replace(JmxPropertyConstant.JMX_ADDR_STRINGS, address))
    JMXConnectorFactory.connect(serviceAddress).getMBeanServerConnection
  }

  def getStringVauleByAttribute(conn: MBeanServerConnection, objectName: String, attribute: String): String = {
    conn.getAttribute(new ObjectName(objectName), attribute).toString
  }

  def getStringVauleByAttribute(conn: MBeanServerConnection, objectName: ObjectName, attribute: String): String = {
    conn.getAttribute(objectName, attribute).toString
  }

  def getLongVauleByAttribute(conn: MBeanServerConnection, objectName: String, attribute: String): Long = {
    conn.getAttribute(new ObjectName(objectName), attribute).toString.toLong
  }

}

